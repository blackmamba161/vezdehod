import org.json.*;
import local.blackmamba.vpn.SubscriptionRefresh;

public class SubscriptionRefreshTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception {
  JSONArray a=new JSONArray("[{id:a,name:Helsinki,profile:p,outbound:{type:vless,server:'192.0.2.1',server_port:443,tls:{enabled:true,alpn:[h2,http]}}},{id:b,name:Other,profile:q,outbound:{type:trojan,password:test}}]");
  JSONArray b=new JSONArray("[{name:Renamed,id:b,profile:q,outbound:{password:test,type:trojan}},{id:a,name:Changed,profile:p,outbound:{tls:{alpn:[h2,http],enabled:true},server_port:443,server:'192.0.2.1',type:vless}}]");
  check(SubscriptionRefresh.sameConnections(a,b),"Display/order/JSON key changes interrupt VPN");
  b.getJSONObject(1).getJSONObject("outbound").put("server","192.0.2.2");check(!SubscriptionRefresh.sameConnections(a,b),"Changed endpoint ignored");
  b=new JSONArray(a.toString());b.getJSONObject(1).getJSONObject("outbound").put("password","rotated");check(!SubscriptionRefresh.sameConnections(a,b),"Credential rotation ignored");
  b=new JSONArray(a.toString());b.getJSONObject(0).getJSONObject("outbound").getJSONObject("tls").put("alpn",new JSONArray("[http,h2]"));check(!SubscriptionRefresh.sameConnections(a,b),"Ordered transport options ignored");
  b=new JSONArray(a.toString());b.remove(1);check(!SubscriptionRefresh.sameConnections(a,b),"Server deletion ignored");
  b=new JSONArray(a.toString());b.getJSONObject(1).put("id","c");check(!SubscriptionRefresh.sameConnections(a,b),"Identity change ignored");
  b=new JSONArray(a.toString());b.getJSONObject(0).put("providerDns",new JSONArray().put("https://dns.google/dns-query"));check(!SubscriptionRefresh.sameConnections(a,b),"Changed provider DNS ignored");
  check(SubscriptionRefresh.sameConnections(new JSONArray(),new JSONArray()),"Empty metadata update interrupts VPN");
  System.out.println("PASS unchanged/reordered metadata preserves tunnel; endpoint, credentials, ordered options, deletion and IDs require reload");
 }
}
