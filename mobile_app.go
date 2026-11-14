package libbox

import (
 "bytes"
 "context"
 "encoding/json"
 "fmt"
 "io"
 "net"
 "net/http"
 "time"
 "strings"
 "github.com/sagernet/sing-box/adapter"
 "github.com/sagernet/sing/service"
 dnsmsg "github.com/miekg/dns"
 "github.com/sagernet/sing-box/experimental/clashapi"
 "github.com/sagernet/sing-box/protocol/group"
 M "github.com/sagernet/sing/common/metadata"
)
func (s *BoxService) MobileStats() string {
 c,ok:=s.clashServer.(*clashapi.Server);if !ok{return `{}`}
 up,down:=c.TrafficManager().Total();return fmt.Sprintf(`{"up":%d,"down":%d}`,up,down)
}
func (s *BoxService) MobileSelect(tag string) error {
 o,ok:=s.instance.Outbound().Outbound("auto");if !ok{return fmt.Errorf("selector unavailable")}
 g,ok:=o.(*group.Selector);if !ok{return fmt.Errorf("server unavailable")};previous:=g.Now();if !g.SelectOutbound(tag){return fmt.Errorf("server unavailable")};if previous!=tag{service.FromContext[adapter.DNSRouter](s.ctx).ResetNetwork()};return nil
}
func (s *BoxService) MobileDelay(tag string) (int32,error) { return s.MobileDelayFor(tag,6000) }
func (s *BoxService) MobileDelayFor(tag string, milliseconds int32) (int32,error) {
 o,ok:=s.instance.Outbound().Outbound(tag);if !ok{return 0,fmt.Errorf("server unavailable")}
 ctx,cancel:=context.WithTimeout(s.ctx,time.Duration(milliseconds)*time.Millisecond);defer cancel()
 type outcome struct{ms int32;err error};answers:=make(chan outcome,2)
 for _,endpoint:=range []string{"https://www.gstatic.com/generate_204","https://cp.cloudflare.com/generate_204"}{go func(endpoint string){
  tr:=&http.Transport{DialContext:func(ctx context.Context,network,address string)(net.Conn,error){return o.DialContext(ctx,network,M.ParseSocksaddr(address))},DisableKeepAlives:true,TLSHandshakeTimeout:4*time.Second}
  defer tr.CloseIdleConnections();client:=&http.Client{Transport:tr,Timeout:6*time.Second,CheckRedirect:func(*http.Request,[]*http.Request)error{return http.ErrUseLastResponse}}
  req,_:=http.NewRequestWithContext(ctx,"GET",endpoint,nil);start:=time.Now();resp,err:=client.Do(req)
  if err==nil{resp.Body.Close();if resp.StatusCode!=204{err=fmt.Errorf("probe HTTP %d",resp.StatusCode)}}
  answers<-outcome{int32(time.Since(start)/time.Millisecond),err}
 }(endpoint)}
 var last error
 for i:=0;i<2;i++{select{case r:=<-answers:if r.err==nil{return r.ms,nil};last=r.err;case <-ctx.Done():return -1,ctx.Err()}}
 return -1,last
}
func (s *BoxService) MobileSpeed(tag string) (string,error) {
 o,ok:=s.instance.Outbound().Outbound(tag);if !ok{return "",fmt.Errorf("server unavailable")}
 ctx,cancel:=context.WithTimeout(s.ctx,40*time.Second);defer cancel()
 tr:=&http.Transport{DialContext:func(ctx context.Context,network,address string)(net.Conn,error){return o.DialContext(ctx,network,M.ParseSocksaddr(address))},DisableKeepAlives:true}
 defer tr.CloseIdleConnections();client:=&http.Client{Transport:tr,Timeout:20*time.Second}
 req,_:=http.NewRequestWithContext(ctx,"GET","https://speed.cloudflare.com/__down?bytes=8000000",nil)
 start:=time.Now();resp,err:=client.Do(req);if err!=nil{return "",err};if resp.StatusCode!=200{resp.Body.Close();return "",fmt.Errorf("speed endpoint: HTTP %d",resp.StatusCode)}
 n,err:=io.Copy(io.Discard,io.LimitReader(resp.Body,8000001));resp.Body.Close();if err!=nil{return "",err};down:=float64(n)*8/time.Since(start).Seconds()/1e6
 req,_=http.NewRequestWithContext(ctx,"POST","https://speed.cloudflare.com/__up",bytes.NewReader(make([]byte,2000000)))
 start=time.Now();resp,err=client.Do(req);if err!=nil{return "",err};io.Copy(io.Discard,io.LimitReader(resp.Body,4096));resp.Body.Close();if resp.StatusCode>=400{return "",fmt.Errorf("upload endpoint: HTTP %d",resp.StatusCode)}
 up:=float64(2000000)*8/time.Since(start).Seconds()/1e6
 p,err:=s.MobileDelay(tag);if err!=nil{p=-1}
 b,_:=json.Marshal(map[string]any{"down":down,"up":up,"ping":p,"bytes":n+2000000});return string(b),nil
}

func (s *BoxService) MobileProbe(tag string) (string,error) {
 o,ok:=s.instance.Outbound().Outbound(tag);if !ok{return "",fmt.Errorf("server unavailable")}
 ctx,cancel:=context.WithTimeout(s.ctx,20*time.Second);defer cancel()
 tr:=&http.Transport{DialContext:func(ctx context.Context,network,address string)(net.Conn,error){return o.DialContext(ctx,network,M.ParseSocksaddr(address))},DisableKeepAlives:true}
 defer tr.CloseIdleConnections();client:=&http.Client{Transport:tr,Timeout:12*time.Second}
 req,_:=http.NewRequestWithContext(ctx,"GET","https://1.1.1.1/cdn-cgi/trace",nil)
 resp,err:=client.Do(req);if err!=nil{return "",err};defer resp.Body.Close();if resp.StatusCode!=200{return "",fmt.Errorf("IP endpoint: HTTP %d",resp.StatusCode)}
 body,err:=io.ReadAll(io.LimitReader(resp.Body,8192));if err!=nil{return "",err};ip:="";for _,line:=range strings.Split(string(body),"\n"){if strings.HasPrefix(line,"ip="){ip=strings.TrimSpace(strings.TrimPrefix(line,"ip="))}}
 if net.ParseIP(ip)==nil{return "",fmt.Errorf("IP endpoint returned no valid address")}
 ping,err:=s.MobileDelay(tag);if err!=nil{return "",err};b,_:=json.Marshal(map[string]any{"ip":ip,"ping":ping});return string(b),nil
}

// MobileDNSCheck uses the configured DNS router without accepting a cached result.
func (s *BoxService) MobileDNSCheck() (int32,error) {
 ctx,cancel:=context.WithTimeout(s.ctx,12*time.Second);defer cancel()
 router:=service.FromContext[adapter.DNSRouter](s.ctx);start:=time.Now()
 manager:=service.FromContext[adapter.DNSTransportManager](s.ctx)
 for _,check:=range []struct{tag,host string}{{"remote","www.youtube.com."},{"local","example.com."}}{
  transport,ok:=manager.Transport(check.tag);if !ok{return -1,fmt.Errorf("DNS route missing")}
  q:=new(dnsmsg.Msg);q.SetQuestion(check.host,dnsmsg.TypeA)
  r,err:=router.Exchange(ctx,q,adapter.DNSQueryOptions{Transport:transport,DisableCache:true})
  if err!=nil{return -1,fmt.Errorf("DNS query failed: %w",err)}
  if r==nil||r.Rcode!=dnsmsg.RcodeSuccess||len(r.Answer)==0{return -1,fmt.Errorf("DNS returned no address")}
 }
 return int32(time.Since(start)/time.Millisecond),nil
}
