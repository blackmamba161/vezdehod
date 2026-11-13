import local.blackmamba.vpn.*;
import org.json.*;
import java.nio.file.*;
import java.util.*;

public class SubscriptionFormatsTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static SubscriptionFormats.Result parse(String text)throws Exception{return SubscriptionFormats.parse(text,"test-profile",(link,p)->{if(!link.startsWith("vless://"))throw new Exception("неизвестный протокол");return new JSONObject().put("id",link).put("profile",p).put("name","URI fixture").put("outbound",new JSONObject());});}
 public static void main(String[] args)throws Exception{
  String xray="{\"remarks\":\"Test\",\"outbounds\":[{\"tag\":\"proxy\",\"protocol\":\"vless\",\"settings\":{\"vnext\":[{\"address\":\"example.com\",\"port\":443,\"users\":[{\"id\":\"00000000-0000-4000-8000-000000000001\",\"encryption\":\"none\"}]}]},\"streamSettings\":{\"network\":\"tcp\",\"security\":\"tls\",\"tlsSettings\":{\"serverName\":\"example.com\"}}},{\"protocol\":\"freedom\"}]}";
  SubscriptionFormats.Result r=parse(xray);check(r.nodes.length()==1&&r.skipped==0,"Xray plumbing imported");check(r.nodes.getJSONObject(0).getJSONObject("outbound").getString("type").equals("xray"),"Xray fields converted lossily");
  check(parse("["+xray+","+xray+"]").nodes.length()==1,"Duplicate JSON nodes");
  String sing="{\"outbounds\":[{\"type\":\"trojan\",\"tag\":\"test\",\"server\":\"example.com\",\"server_port\":443,\"password\":\"fixture\",\"tls\":{\"enabled\":true}},{\"type\":\"direct\"}]}";check(parse(sing).nodes.length()==1,"sing-box JSON");
  String yaml="proxies:\n  - name: Test\n    type: vless\n    server: example.com\n    port: 443\n    uuid: 00000000-0000-4000-8000-000000000001\n    tls: true\n    client-fingerprint: chrome\n    reality-opts:\n      public-key: test-key\n      short-id: aabb\n    network: ws\n    ws-opts:\n      path: /socket\n      headers:\n        Host: example.com\n";
  r=parse(yaml);JSONObject o=r.nodes.getJSONObject(0).getJSONObject("outbound");check(o.getJSONObject("tls").getJSONObject("reality").getString("short_id").equals("aabb"),"Reality settings lost");check(o.getJSONObject("transport").getJSONObject("headers").getString("Host").equals("example.com"),"WS header lost");
  check(parse(Base64.getEncoder().encodeToString(xray.getBytes(java.nio.charset.StandardCharsets.UTF_8))).nodes.length()==1,"Base64 JSON");check(parse("vless://one\nunknown://two").nodes.length()==1,"Mixed supported list");check(parse("vless://one\nunknown://two").skipped==1,"Missing partial-import warning");
  for(String bad:new String[]{"<html>login</html>","{}","proxies: !!javax.script.ScriptEngineManager {}","proxies: [ {type: vless, name: missing} ]","proxies: []"}){boolean failed=false;try{parse(bad);}catch(Exception expected){failed=true;}check(failed,"Malformed/unsafe source accepted");}
  System.out.println("PASS Xray JSON/list, sing-box JSON, Clash YAML, Base64, dedupe, Reality/WS retention, partial warnings, malformed input and YAML tag rejection");
  if(args.length>0){r=parse(Files.readString(Path.of(args[0])));System.out.println("Provided subscription: format="+r.format+", nodes="+r.nodes.length()+", skipped="+r.skipped);JSONObject data=new JSONObject().put("servers",r.nodes).put("rules",new JSONArray()).put("wifi",r.nodes.getJSONObject(0).getString("id")).put("cellular",r.nodes.getJSONObject(0).getString("id")).put("auto",true).put("defaultTarget","direct");Map<String,Integer> ports=new HashMap<>();for(int i=0;i<r.nodes.length();i++)ports.put(r.nodes.getJSONObject(i).getString("id"),23000+i);Configuration.Result config=Configuration.build(data,"wifi",ports,"test-only");Path parent=Path.of(args[0]).getParent();Files.writeString(parent.resolve("xray-check.json"),config.xray);Files.writeString(parent.resolve("singbox-check.json"),config.singbox);}
 }
}
