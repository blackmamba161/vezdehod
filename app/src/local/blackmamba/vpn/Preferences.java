package local.blackmamba.vpn;
import org.json.*;
import java.net.URI;
import static local.blackmamba.vpn.Store.*;

/** Settings shared by UI and core. Defaults preserve the installed configuration. */
public final class Preferences {
 public static JSONObject defaults() throws JSONException {return obj(
  "adBlock",false,"adBlockAllow","","theme","dark","accent","blue","textScale",100,
  "showSpeed",true,"showPing",true,"showServer",true,"showTraffic",false,"showExpiry",true,
  "boot",false,"reconnect",true,"retrySeconds",15,"fallbackWifi","","fallbackCellular","",
  "dnsMode","auto","dnsBackup",DnsSettings.CLOUDFLARE,"dnsDirect","","dnsBootstrap","","remoteDns",DnsSettings.GOOGLE,"ipv6",true,"mtu",1400,"stack","gvisor");}
 public static JSONObject networkDefaults()throws JSONException{return obj("dnsMode","auto","dnsBackup",DnsSettings.CLOUDFLARE,"dnsDirect","","dnsBootstrap","","remoteDns",DnsSettings.GOOGLE,"ipv6",true,"mtu",1400,"stack","gvisor");}
 public static void validate(String key,Object value)throws Exception {
  if(key.equals("adBlockAllow"))AdBlock.exceptions(value.toString());
  if(key.equals("mtu")&&(!(value instanceof Number)||((Number)value).intValue()<1280||((Number)value).intValue()>1500))throw new Exception(L10n.s(R.string.msg_681ba374e5));
  if(key.equals("retrySeconds")&&(!(value instanceof Number)||((Number)value).intValue()<5||((Number)value).intValue()>120))throw new Exception(L10n.s(R.string.msg_e13fa5e7d6));
  if(key.equals("remoteDns"))DnsSettings.address(value.toString());
  if(key.equals("dnsMode")&&!java.util.Arrays.asList("auto","manual").contains(value.toString()))throw new Exception(L10n.s(R.string.dns_invalid));
  if(java.util.Arrays.asList("dnsBackup","dnsDirect","dnsBootstrap").contains(key)&&!Compat.blank(value.toString()))DnsSettings.address(value.toString());
 }
 public static String fallbackKey(String network){return network.equals("cellular")?"fallbackCellular":"fallbackWifi";}
}
