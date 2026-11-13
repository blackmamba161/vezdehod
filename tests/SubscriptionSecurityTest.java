import org.json.*;import local.blackmamba.vpn.SubscriptionSecurity;
public class SubscriptionSecurityTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static JSONObject node(String name,JSONObject out)throws Exception{return new JSONObject().put("name",name).put("outbound",out);}
 public static void main(String[] args)throws Exception{
  JSONObject secure=node("Secure",new JSONObject("{type:vless,tls:{enabled:true,insecure:false}}"));check(SubscriptionSecurity.flags(secure)==0,"Secure TLS flagged");
  JSONObject unsafe=node("Unsafe",new JSONObject("{type:vless,tls:{enabled:true,insecure:true}}"));check(SubscriptionSecurity.flags(unsafe)==1,"Disabled verification missed");
  JSONObject plain=node("Plain",new JSONObject("{type:http,server:'example.invalid',server_port:8080}"));check(SubscriptionSecurity.flags(plain)==2,"Plain HTTP missed");
  JSONObject reality=node("Reality",new JSONObject("{type:xray,xray:{protocol:vless,streamSettings:{security:reality}}}"));check(SubscriptionSecurity.flags(reality)==0,"Reality flagged");
  JSONObject group=node("Group",new JSONObject("{type:'xray-group',config:{outbounds:[{protocol:vless,streamSettings:{security:tls,tlsSettings:{allowInsecure:true}}}]}}"));check(SubscriptionSecurity.flags(group)==1,"Group certificate bypass missed");
  JSONArray a=new JSONArray().put(unsafe).put(plain),b=new JSONArray().put(plain).put(unsafe);check(SubscriptionSecurity.fingerprint(a).equals(SubscriptionSecurity.fingerprint(b)),"Reorder forces new consent");check(SubscriptionSecurity.fingerprint(new JSONArray().put(secure)).isEmpty(),"Safe import needs consent");
  String before=group.toString();SubscriptionSecurity.flags(group);check(before.equals(group.toString()),"Provider settings modified");
  System.out.println("PASS TLS/Reality, certificate bypass, plaintext, nested Xray, stable consent fingerprint; provider settings unchanged");
 }
}
