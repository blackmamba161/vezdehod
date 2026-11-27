package local.blackmamba.vpn;
import org.json.*;import android.net.Uri;import android.util.Base64;import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.*;
import static local.blackmamba.vpn.Store.*;

public final class Subscriptions {
 static String decoded(String s){return new String(Base64.decode(s.replace('-','+').replace('_','/'),Base64.DEFAULT),StandardCharsets.UTF_8);}
 static String qp(Uri u,String k,String def){String v=u.getQueryParameter(k);return v==null?def:v;}
 static String id(String s)throws Exception{byte[] b=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder("n");for(int i=0;i<12;i++)x.append(String.format("%02x",b[i]));return x.toString();}
 public static JSONArray parse(String body,String profile)throws Exception{return SubscriptionFormats.parse(body,profile,Subscriptions::node).nodes;}

 static JSONObject node(String link,String profile)throws Exception{
  Uri u=Uri.parse(link);String type=u.getScheme(),name=Uri.decode(u.getEncodedFragment()==null?"":u.getEncodedFragment());JSONObject o;String protocol=type;
  if("vmess".equals(type)){String payload=link.substring(8).split("#")[0];JSONObject v=new JSONObject(decoded(payload));name=v.optString("ps","VMess");o=obj("type","vmess","server",v.getString("add"),"server_port",Integer.parseInt(v.getString("port")),"uuid",v.getString("id"),"security",v.optString("scy","auto"),"alter_id",v.optInt("aid",0));if("tls".equals(v.optString("tls")))o.put("tls",obj("enabled",true,"server_name",v.optString("sni",v.optString("host",v.getString("add"))),"utls",obj("enabled",true,"fingerprint","chrome")));transport(o,v.optString("net","tcp"),v.optString("path","/"),v.optString("host",""),v.optString("path",""));}
  else if("ss".equals(type)){String host=u.getHost(),info=u.getUserInfo();int port=u.getPort();if(info==null){String s=decoded(link.substring(5).split("#")[0]);u=Uri.parse("ss://"+s);host=u.getHost();info=u.getUserInfo();port=u.getPort();}if(!info.contains(":"))info=decoded(info);String[] a=info.split(":",2);o=obj("type","shadowsocks","server",host,"server_port",port,"method",a[0],"password",a[1]);if(u.getQueryParameter("plugin")!=null)throw new Exception("Shadowsocks plugin");}
  else {
   if(!Arrays.asList("vless","trojan","hysteria2","hy2").contains(type))throw new Exception(L10n.s(R.string.msg_3fed10dcd2)+type);
   String host=u.getHost();int port=u.getPort()>0?u.getPort():443;if(host==null)throw new Exception(L10n.s(R.string.msg_0ed6fd3f45));
   if(host.equals("0.0.0.0")||host.equals("127.0.0.1")||host.equals("app.deprecated")||port==1)return null;
   String info=Uri.decode(u.getEncodedUserInfo());o=obj("type",type.equals("hy2")?"hysteria2":type,"server",host,"server_port",port);
   if(type.equals("hy2")||type.equals("hysteria2")){
    o=HysteriaLink.outbound(host,port,info,qp(u,"sni",qp(u,"serverName",host)),qp(u,"alpn",""),qp(u,"obfs",""),qp(u,"obfs-password",""),qp(u,"pinSHA256",""));
   }else{
   if(type.equals("vless")){o.put("uuid",info);String flow=qp(u,"flow","");if(!flow.isEmpty())o.put("flow",flow);}else o.put("password",info);
   String sec=qp(u,"security",type.equals("vless")?"none":"tls"),net=qp(u,"type","tcp");
   /* REALITY over plain TCP goes through Xray: its REALITY client keeps the post-quantum key share that some providers require, while the sing-box one strips it. */
   boolean xrayTcp=type.equals("vless")&&net.equals("tcp")&&(sec.equals("reality")||!qp(u,"encryption","none").equals("none"));
   if(sec.equals("tls")||sec.equals("reality")){JSONObject tls=obj("enabled",true,"server_name",qp(u,"sni",qp(u,"serverName",host)),"utls",obj("enabled",true,"fingerprint",qp(u,"fp","chrome")));String alpn=qp(u,"alpn","");if(!alpn.isEmpty())tls.put("alpn",new JSONArray(Arrays.asList(alpn.split(","))));if(sec.equals("reality"))tls.put("reality",obj("enabled",true,"public_key",qp(u,"pbk",""),"short_id",qp(u,"sid","")));o.put("tls",tls);}
   if(net.equals("xhttp")||xrayTcp){JSONObject user=obj("id",info,"encryption",qp(u,"encryption","none"));String flow=qp(u,"flow","");if(!flow.isEmpty())user.put("flow",flow);JSONObject settings=obj("address",host,"port",port,"users",arr(user));JSONObject stream=obj("network",net,"security",sec);if(net.equals("xhttp"))stream.put("xhttpSettings",obj("path",qp(u,"path","/"),"host",qp(u,"host",""),"mode",qp(u,"mode","auto")));
    String extra=qp(u,"extra","");if(net.equals("xhttp")&&!extra.isEmpty()){JSONObject ex=new JSONObject(extra),xh=stream.getJSONObject("xhttpSettings");Iterator<String> keys=ex.keys();while(keys.hasNext()){String k=keys.next();xh.put(k,ex.get(k));}}
    JSONObject tls=obj("serverName",qp(u,"sni",qp(u,"serverName",host)),"fingerprint",qp(u,"fp","chrome"),"allowInsecure",false);String alpn=qp(u,"alpn","");if(!alpn.isEmpty())tls.put("alpn",new JSONArray(Arrays.asList(alpn.split(","))));if(sec.equals("tls"))stream.put("tlsSettings",tls);
    if(sec.equals("reality")){stream.remove("tlsSettings");stream.put("realitySettings",obj("serverName",qp(u,"sni",host),"fingerprint",qp(u,"fp","chrome"),"publicKey",qp(u,"pbk",""),"shortId",qp(u,"sid",""),"spiderX",qp(u,"spx","")));}
    o=obj("type",xrayTcp?"xray":"xhttp","xray",obj("protocol","vless","settings",obj("vnext",arr(settings)),"streamSettings",stream));
   }else transport(o,net,qp(u,"path","/"),qp(u,"host",""),qp(u,"serviceName",""));
   }
  }
  if(Compat.blank(name))name=protocol+" · "+o.optString("server","XHTTP");return obj("id",id(profile+"|"+link),"profile",profile,"name",name,"protocol",protocol,"outbound",o);
 }
 static void transport(JSONObject o,String type,String path,String host,String service)throws Exception {switch(type){case "tcp":case "raw":break;case "ws":JSONObject t=obj("type","ws","path",path);if(!host.isEmpty())t.put("headers",obj("Host",host));o.put("transport",t);break;case "grpc":o.put("transport",obj("type","grpc","service_name",service));break;case "http":case "h2":o.put("transport",obj("type","http","path",path,"host",arr(host)));break;case "httpupgrade":o.put("transport",obj("type","httpupgrade","path",path,"host",host));break;default:throw new Exception(L10n.s(R.string.msg_0203e5e1bf)+type);}}
 public static JSONObject fetch(JSONObject p,android.content.Context context)throws Exception {
  File noBackupDir=p.optString("type","subscription").equals("subscription")&&Store.get(context).sendsSubscriptionHwid()?context.getNoBackupFilesDir():null;
  JSONObject result=fetch(p,noBackupDir);SubscriptionValidator.validate(context,result);return result;
 }
 public static JSONObject fetch(JSONObject p)throws Exception {return fetch(p,(File)null);}
 private static JSONObject fetch(JSONObject p,File noBackupDir)throws Exception {
  String source=p.getString("source"),body=source,usage="",providerTitle="";
  if(p.optString("type","subscription").equals("subscription")){
   SubscriptionDownload downloaded=SubscriptionDownload.fetch(new URL(source),p.optString("userAgent","v2rayNG/1.10"),noBackupDir,url->(HttpURLConnection)url.openConnection());
   body=downloaded.body;usage=downloaded.usage;providerTitle=downloaded.title;
  }
  SubscriptionFormats.Result parsed=SubscriptionFormats.parse(body,p.getString("id"),Subscriptions::node);if(parsed.skipped>0&&p.has("updated"))throw new Exception(L10n.s(R.string.msg_8478b68ef9));JSONArray nodes=parsed.nodes;JSONObject updated=new JSONObject(p.toString());if(providerTitle!=null&&!Compat.blank(providerTitle)&&p.optString("type").equals("subscription")&&p.optString("name").equals(new URI(source).getHost())){try{String title=providerTitle.startsWith("base64:")?decoded(providerTitle.substring(7)):providerTitle;title=title.replaceAll("[\\p{Cntrl}]","").trim();if(!title.isEmpty())updated.put("name",title.substring(0,Math.min(80,title.length())));}catch(Exception ignored){}}SubscriptionMetadata.apply(updated,usage);updated.put("updated",System.currentTimeMillis()).put("format",parsed.format).put("importSkipped",parsed.skipped);return obj("profile",updated,"servers",nodes,"warnings",parsed.warnings,"skipped",parsed.skipped,"format",parsed.format);
 }
}
