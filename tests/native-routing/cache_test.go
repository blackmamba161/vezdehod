package routing_test

import (
 "context"
 "fmt"
 "path/filepath"
 "testing"
 box "github.com/sagernet/sing-box"
 "github.com/sagernet/sing-box/include"
 "github.com/sagernet/sing-box/option"
 "github.com/sagernet/sing-box/protocol/group"
 _ "github.com/sagernet/sing-box/experimental/cachefile"
 "github.com/sagernet/sing/common/json"
)
func start(t *testing.T,path string)(*box.Box,*group.Selector){
 t.Helper();ctx:=include.Context(context.Background())
 raw:=fmt.Sprintf(`{"log":{"disabled":true},"outbounds":[{"type":"direct","tag":"wanted"},{"type":"socks","tag":"stale","server":"127.0.0.1","server_port":1},{"type":"selector","tag":"auto","outbounds":["wanted","stale"],"default":"wanted"}],"route":{"final":"auto"},"experimental":{"cache_file":{"enabled":true,"path":%q}}}`,path)
 options,err:=json.UnmarshalExtendedContext[option.Options](ctx,[]byte(raw));if err!=nil{t.Fatal(err)}
 b,err:=box.New(box.Options{Context:ctx,Options:options});if err!=nil{t.Fatal(err)};if err=b.Start();err!=nil{b.Close();t.Fatal(err)}
 o,ok:=b.Outbound().Outbound("auto");if !ok{t.Fatal("missing selector")};return b,o.(*group.Selector)
}
func TestPersistedSelectorMustBeOverridden(t *testing.T){
 path:=filepath.Join(t.TempDir(),"cache.db")
 first,s:=start(t,path);if !s.SelectOutbound("stale"){t.Fatal("seed selection failed")};if err:=first.Close();err!=nil{t.Fatal(err)}
 second,s:=start(t,path);defer second.Close()
 if s.Now()!="stale"{t.Fatal("regression did not reproduce native cached selection")}
 if !s.SelectOutbound("wanted")||s.Now()!="wanted"{t.Fatal("explicit app selection did not replace stale route")}
 t.Log("reproduced: cached stale proxy overrides configured default; explicit selection repairs route")
}
func TestIndependentProbeCacheWhileVpnRuns(t *testing.T){
 dir:=t.TempDir();vpn,_:=start(t,filepath.Join(dir,"vpn.db"));defer vpn.Close()
 probe,_:=start(t,filepath.Join(dir,"probe.db"));defer probe.Close()
 t.Log("VPN and probe native engines both start with independent cache files")
}
