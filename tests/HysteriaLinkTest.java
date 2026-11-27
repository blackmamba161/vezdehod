package local.blackmamba.vpn;
import org.json.JSONObject;

public class HysteriaLinkTest {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  JSONObject o=HysteriaLink.outbound("test.invalid",443,"auth","sni.invalid","h3","salamander","fixture","");
  JSONObject t=o.getJSONObject("tls");check(t.getBoolean("enabled")&&!t.has("utls")&&!t.optBoolean("insecure"));
  check("sni.invalid".equals(t.getString("server_name"))&&"h3".equals(t.getJSONArray("alpn").getString(0)));
  check("fixture".equals(o.getJSONObject("obfs").getString("password")));
  String pin="ab".repeat(32);
  o=HysteriaLink.outbound("test.invalid",443,"auth","sni.invalid","","salamander","fixture",pin);
  JSONObject st=o.getJSONObject("xray").getJSONObject("streamSettings");check(pin.equals(st.getJSONObject("tlsSettings").getString("pinnedPeerCertSha256")));
  check("auth".equals(st.getJSONObject("hysteriaSettings").getString("auth"))&&st.has("finalmask"));
  try{HysteriaLink.outbound("test.invalid",443,"auth","sni.invalid","","","","broken");throw new AssertionError();}catch(IllegalArgumentException expected){}
  System.out.println("PASS Hysteria2 standard QUIC TLS, SNI, ALPN, obfs and certificate pin");
 }
}
