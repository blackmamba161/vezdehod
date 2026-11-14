package routing_test

import (
 "context"
 "os"
 "testing"
 box "github.com/sagernet/sing-box"
 "github.com/sagernet/sing-box/include"
 "github.com/sagernet/sing-box/option"
 "github.com/sagernet/sing/common/json"
)
func TestGeneratedDNSConfigurationStarts(t *testing.T){
 path:=os.Getenv("VEZDEHOD_TEST_CONFIG");if path==""{t.Fatal("VEZDEHOD_TEST_CONFIG required")}
 content,err:=os.ReadFile(path);if err!=nil{t.Fatal(err)};ctx:=include.Context(context.Background())
 options,err:=json.UnmarshalExtendedContext[option.Options](ctx,content);if err!=nil{t.Fatal(err)}
 engine,err:=box.New(box.Options{Context:ctx,Options:options});if err!=nil{t.Fatal(err)};defer engine.Close()
 if err=engine.Start();err!=nil{t.Fatal(err)}
 t.Log("Generated DNS transports, dependencies, rules and routes start in the real engine without TUN")
}
