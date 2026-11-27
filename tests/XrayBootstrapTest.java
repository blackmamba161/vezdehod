package local.blackmamba.vpn;

import java.util.*;
import org.json.*;

public class XrayBootstrapTest {
 static void check(boolean condition){if(!condition)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  String host="vpn.example.invalid";
  String original="{\"outbounds\":[{\"protocol\":\"vless\",\"settings\":{\"vnext\":[{\"address\":\"VPN.example.invalid\",\"port\":443}]},\"streamSettings\":{\"network\":\"xhttp\",\"security\":\"tls\",\"tlsSettings\":{\"serverName\":\"sni.invalid\",\"pinnedPeerCertSha256\":\"test-pin\"},\"xhttpSettings\":{\"host\":\"authority.invalid\",\"path\":\"/test\"},\"sockopt\":{\"tcpNoDelay\":true}}}]}";
  Map<String,List<String>> answers=Map.of(host,List.of("192.0.2.1","2001:db8::1"));
  JSONObject g=new JSONObject(original);check(!XrayBootstrap.apply(g,Collections.emptyMap()));check(g.similar(new JSONObject(original)));
  check(XrayBootstrap.apply(g,answers));check(g.getJSONObject("dns").getJSONObject("hosts").getJSONArray("full:"+host).length()==2);
  JSONObject so=g.getJSONArray("outbounds").getJSONObject(0).getJSONObject("streamSettings").getJSONObject("sockopt");check(so.getString("domainStrategy").equals("UseIP"));
  so.remove("domainStrategy");g.remove("dns");check(g.similar(new JSONObject(original)));
  for(String key:new String[]{"dialerProxy","domainStrategy"}){g=new JSONObject(original);g.getJSONArray("outbounds").getJSONObject(0).getJSONObject("streamSettings").getJSONObject("sockopt").put(key,"provided");check(!XrayBootstrap.apply(g,answers));}
  g=new JSONObject(original).put("dns",new JSONObject());check(!XrayBootstrap.apply(g,answers));
  g=new JSONObject(original.replace("VPN.example.invalid","192.0.2.1"));check(!XrayBootstrap.apply(g,answers));
  System.out.println("PASS Android bootstrap: endpoint-only DNS, preserved names/TLS/Host and provider overrides");
 }
}
