package local.blackmamba.vpn;
import org.json.*;import java.nio.file.*;import java.util.*;import static local.blackmamba.vpn.Store.*;
public final class DnsPolicyTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static JSONObject byTag(JSONArray a,String tag)throws Exception{for(int i=0;i<a.length();i++)if(a.getJSONObject(i).optString("tag").equals(tag))return a.getJSONObject(i);throw new AssertionError("Missing "+tag);}
 public static void main(String[] args)throws Exception{
  for(String value:new String[]{"1.1.1.1","2001:4860:4860::8888","https://dns.google/dns-query","tls://dns.google","udp://1.1.1.1:53","tcp://[2606:4700:4700::1111]:53"})check(DnsSettings.address(value).equals(value),"Valid address rejected");
  for(String value:new String[]{"","google.com","256.1.1.1","http://dns.google/dns-query","https://user:pass@dns.google/query","tls://dns.google/path","udp://1.1.1.1:0","tcp://1.1.1.1:65536","https://dns.google/#secret","https://dns.google/\nquery"}){boolean rejected=false;try{DnsSettings.address(value);}catch(Exception e){rejected=true;}check(rejected,"Invalid DNS accepted: "+value);}
  JSONObject d=DnsSettings.defaults();String remembered="tls://dns.google";d.put("remoteDns",remembered);check(DnsSettings.remote(d,null)[0].equals(DnsSettings.GOOGLE),"Auto uses remembered manual override");
  JSONObject manual=new JSONObject(d.toString()).put("dnsMode","manual");check(DnsSettings.remote(manual,null)[0].equals(remembered),"Manual value lost");check(DnsSettings.effectiveChanged(d,manual),"Mode switch ignored");
  JSONObject other=new JSONObject(d.toString()).put("remoteDns","1.1.1.1");check(!DnsSettings.effectiveChanged(d,other),"Inactive manual edits restart tunnel");check(!DnsSettings.changed(d,new JSONObject(d.toString())),"Identical save restarts tunnel");
  JSONArray imported=DnsSettings.provider(obj("dns",obj("servers",arr(obj("address","9.9.9.9","domains",arr("example.org")),"https://dns.quad9.net/dns-query",obj("type","https","server","dns.google","path","/dns-query")))));
  check(imported.length()==2&&imported.getString(0).equals("https://dns.quad9.net/dns-query"),"Scoped DNS imported globally");
  check(DnsSettings.provider(obj("dns",obj("final","remote","servers",arr(obj("tag","bootstrap","address","9.9.9.9"),obj("tag","remote","address",DnsSettings.GOOGLE))))).getString(0).equals(DnsSettings.GOOGLE),"Provider final DNS ignored");
  check(DnsSettings.provider(obj("dns",obj("servers",arr(obj("address","1.1.1.1","port",5353))))).getString(0).equals("udp://1.1.1.1:5353"),"Provider DNS port lost");
  JSONArray nodes=arr(obj("id","one","outbound",obj("type","socks","server","192.0.2.1","server_port",1080),"providerDns",imported),obj("id","two","outbound",obj("type","socks","server","192.0.2.2","server_port",1080)));
  d.put("servers",nodes).put("wifi","one").put("cellular","two").put("auto",true).put("defaultTarget","direct").put("rules",arr(obj("type","domain_suffix","value","youtube.com","target","auto"),obj("type","domain","value","example.net","target","server:two"),obj("type","app","value","org.telegram.messenger","target","auto"),obj("type","domain","value","example.org","target","direct")));
  String original=d.toString();JSONObject config=new JSONObject(Configuration.build(d,"wifi",Map.of(),"").singbox);JSONObject dns=config.getJSONObject("dns");JSONArray servers=dns.getJSONArray("servers"),rules=dns.getJSONArray("rules");
  check(byTag(servers,"remote-policy-0-primary").getString("address").equals(imported.getString(0)),"Provider DNS lost");
  for(String tag:new String[]{"remote-policy-0-primary","remote-policy-0-backup"})check(byTag(servers,tag).getString("detour").equals("auto"),"VPN DNS leaks directly");
  check(!byTag(servers,"remote").getJSONObject("routes").getString("one").equals(byTag(servers,"remote").getJSONObject("routes").getString("two")),"Different subscription DNS merged");
  check(byTag(servers,"dns-node-two-primary").getString("detour").equals("two"),"Explicit rule DNS uses wrong server");
  check(rules.getJSONObject(2).has("package_name"),"App DNS route missing");check(rules.getJSONObject(3).getString("server").equals("local"),"Direct rule DNS proxied");
  check(dns.getString("final").equals("local"),"Rest-of-traffic DNS mismatch");check(config.getJSONObject("route").getString("default_domain_resolver").equals("bootstrap"),"Server resolution loops through VPN");
  check(byTag(servers,"bootstrap-public").getString("detour").equals("direct"),"Bootstrap recursion");check(d.toString().equals(original),"Policy mutated saved state");
  // Host startup test needs no Android package resolver or TUN.
  config.put("inbounds",new JSONArray());for(int i=rules.length()-1;i>=0;i--)if(rules.getJSONObject(i).has("package_name"))rules.remove(i);
  JSONArray routes=config.getJSONObject("route").getJSONArray("rules");for(int i=routes.length()-1;i>=0;i--)if(routes.getJSONObject(i).has("package_name"))routes.remove(i);
  Files.writeString(Path.of(args[0]),config.toString());
  d.put("defaultTarget","block");dns=new JSONObject(Configuration.build(d,"wifi",Map.of(),"").singbox).getJSONObject("dns");rules=dns.getJSONArray("rules");check(rules.getJSONObject(rules.length()-1).getString("action").equals("reject"),"Blocked default leaks DNS");
  String clash="proxies:\n  - name: Test\n    type: socks5\n    server: 192.0.2.1\n    port: 1080\ndns:\n  nameserver:\n    - https://dns.google/dns-query\n";
  SubscriptionFormats.Result parsed=SubscriptionFormats.parse(clash,"fixture",(u,p)->null);check(parsed.nodes.getJSONObject(0).getJSONArray("providerDns").getString(0).equals(DnsSettings.GOOGLE),"Clash DNS not retained");
  System.out.println("PASS DNS address validation, auto/manual retention, provider import, route isolation, blocked DNS, immutable settings");
 }
}
