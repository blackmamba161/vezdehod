package routing_test

import (
 "context"
 stdjson "encoding/json"
 "net"
 "os"
 "runtime"
 "sync"
 "testing"
 "time"
 mdns "github.com/miekg/dns"
 box "github.com/sagernet/sing-box"
 "github.com/sagernet/sing-box/adapter"
 "github.com/sagernet/sing-box/experimental/libbox"
 "github.com/sagernet/sing-box/include"
 "github.com/sagernet/sing-box/option"
 "github.com/sagernet/sing/common/json"
 "github.com/sagernet/sing/service"
)

func TestOfflineDNSLoadTerminatesWithoutTaskGrowth(t *testing.T){
 raw,err:=os.ReadFile(os.Getenv("VEZDEHOD_TEST_CONFIG"));if err!=nil{t.Fatal(err)}
 var config map[string]any;if err=stdjson.Unmarshal(raw,&config);err!=nil{t.Fatal(err)}
 blackhole,err:=net.ListenPacket("udp","127.0.0.1:0");if err!=nil{t.Fatal(err)};defer blackhole.Close()
 config["log"]=map[string]any{"disabled":true}
 dns:=config["dns"].(map[string]any);for _,value:=range dns["servers"].([]any){server:=value.(map[string]any);if server["tag"]=="system"{server["address"]="udp://"+blackhole.LocalAddr().String()}}
 raw,err=stdjson.Marshal(config);if err!=nil{t.Fatal(err)}
 ctx:=service.ContextWithDefaultRegistry(include.Context(context.Background()))
 options,err:=json.UnmarshalExtendedContext[option.Options](ctx,raw);if err!=nil{t.Fatal(err)}
 engine,err:=box.New(box.Options{Context:ctx,Options:options});if err!=nil{t.Fatal(err)};defer engine.Close();if err=engine.Start();err!=nil{t.Fatal(err)}
 libbox.MobileMemoryBudget(64*1024*1024)
 manager:=service.FromContext[adapter.DNSTransportManager](ctx);transport,ok:=manager.Transport("local");if !ok{t.Fatal("Local route missing")}
 router:=service.FromContext[adapter.DNSRouter](ctx);runtime.GC();before:=runtime.NumGoroutine();var baseline runtime.MemStats;runtime.ReadMemStats(&baseline)
 started:=time.Now()
 for wave:=0;wave<10;wave++{
  var workers sync.WaitGroup;workers.Add(16)
  for i:=0;i<16;i++{go func(){defer workers.Done();queryCtx,cancel:=context.WithTimeout(ctx,30*time.Millisecond);defer cancel();q:=new(mdns.Msg).SetQuestion("example.org.",mdns.TypeA);_,err:=router.Exchange(queryCtx,q,adapter.DNSQueryOptions{Transport:transport,DisableCache:true});if err==nil{t.Error("Unresponsive DNS accepted")}}()}
  workers.Wait()
 }
 time.Sleep(200*time.Millisecond);runtime.GC();var after runtime.MemStats;runtime.ReadMemStats(&after)
 if elapsed:=time.Since(started);elapsed>5*time.Second{t.Fatalf("Offline requests did not terminate: %v",elapsed)}
 if count:=runtime.NumGoroutine();count>before+16{t.Fatalf("DNS tasks retained: before=%d after=%d",before,count)}
 if after.HeapAlloc>baseline.HeapAlloc+32*1024*1024{t.Fatalf("DNS retained over 32 MiB after cancellation: before=%d after=%d",baseline.HeapAlloc,after.HeapAlloc)}
 t.Logf("160 blackholed DNS queries terminated; goroutines %d -> %d; heap %d -> %d; %v",before,runtime.NumGoroutine(),baseline.HeapAlloc,after.HeapAlloc,time.Since(started))
}
