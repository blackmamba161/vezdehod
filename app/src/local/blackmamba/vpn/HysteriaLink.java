package local.blackmamba.vpn;

import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONObject;
import static local.blackmamba.vpn.Store.*;

final class HysteriaLink {
 private HysteriaLink() {}
 static JSONObject outbound(String host,int port,String auth,String sni,String alpn,String obfs,String obfsPassword,String pin)throws Exception {
  // QUIC uses standard TLS, not the stream-oriented uTLS ClientHello wrapper.
  JSONArray protocols=alpn.isEmpty()?null:new JSONArray(Arrays.asList(alpn.split(",")));
  if(pin.isEmpty()){
   JSONObject tls=obj("enabled",true,"server_name",sni);
   if(protocols!=null)tls.put("alpn",protocols);
   JSONObject node=obj("type","hysteria2","server",host,"server_port",port,"password",auth,"tls",tls);
   if(!obfs.isEmpty())node.put("obfs",obj("type",obfs,"password",obfsPassword));
   return node;
  }
  // sing-box 1.12 has no certificate digest option. Use the bundled Xray's
  // Hysteria2 transport for pinned links; never discard the provider's pin.
  String digest=pin.replace(":","");
  if(!digest.matches("[0-9a-fA-F]{64}"))throw new IllegalArgumentException("Invalid Hysteria2 pinSHA256");
  JSONObject tls=obj("serverName",sni,"pinnedPeerCertSha256",digest);
  if(protocols!=null)tls.put("alpn",protocols);
  JSONObject stream=obj("network","hysteria","security","tls","tlsSettings",tls,"hysteriaSettings",obj("version",2,"auth",auth));
  if(!obfs.isEmpty()){
   if(!obfs.equals("salamander")||obfsPassword.isEmpty())throw new IllegalArgumentException("Unsupported Hysteria2 obfs");
   stream.put("finalmask",obj("udp",arr(obj("type","salamander","settings",obj("password",obfsPassword)))));
  }
  return obj("type","xray","xray",obj("protocol","hysteria","settings",obj("version",2,"address",host,"port",port),"streamSettings",stream));
 }
}
