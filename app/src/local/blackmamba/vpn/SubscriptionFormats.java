package local.blackmamba.vpn;

import org.json.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import static local.blackmamba.vpn.Store.*;

/** Imports server definitions only. Provider routes, listeners and scripts never replace app settings. */
public final class SubscriptionFormats {
 public interface LinkParser {JSONObject parse(String link,String profile)throws Exception;}
 public static final class Result {
  public final JSONArray nodes=new JSONArray(),warnings=new JSONArray();public String format=L10n.s(R.string.msg_e8d9cd7b4e);public int skipped=0;
 }
 private final Result result=new Result();private final String profile;private final LinkParser links;private final Set<String> ids=new HashSet<>();
 private SubscriptionFormats(String profile,LinkParser links){this.profile=profile;this.links=links;}
 public static Result parse(String source,String profile,LinkParser links)throws Exception {
  SubscriptionFormats parser=new SubscriptionFormats(profile,links);parser.read(source,0);
  if(parser.result.nodes.length()==0)throw new Exception(parser.result.skipped>0?L10n.s(R.string.msg_9794aef624)+parser.result.warnings.optString(0):L10n.s(R.string.msg_e7e42a2acf));return parser.result;
 }
 void read(String source,int depth)throws Exception {
  if(depth>2)throw new Exception(L10n.s(R.string.msg_999986e004));String raw=source.replace("\uFEFF","").trim();if(raw.length()>4*1024*1024)throw new Exception(L10n.s(R.string.msg_6c21351bc9));
  if(raw.isEmpty())throw new Exception(L10n.s(R.string.msg_6153e34d0c));
  if(raw.startsWith("<"))throw new Exception(L10n.s(R.string.msg_0432186247));
  if(raw.startsWith("{")||raw.startsWith("[")){
   Object parsed;try{parsed=new JSONTokener(raw).nextValue();}catch(Exception e){throw new Exception(L10n.s(R.string.msg_3c0e1e7d43));}result.format="JSON";json(parsed,"");return;
  }
  if(raw.matches("(?s)[A-Za-z0-9+/=_\\-\\s]+")){
   try{String compact=raw.replaceAll("\\s","");String decoded=new String(java.util.Base64.getDecoder().decode(compact.replace('-','+').replace('_','/')),StandardCharsets.UTF_8);read(decoded,depth+1);if(result.format.equals(L10n.s(R.string.msg_e8d9cd7b4e)))result.format="Base64";return;}catch(IllegalArgumentException ignored){}
  }
  if(raw.contains("proxies:")||raw.startsWith("---")||raw.startsWith("- name:")){
   try{LoaderOptions options=new LoaderOptions();options.setMaxAliasesForCollections(20);options.setNestingDepthLimit(30);options.setCodePointLimit(4*1024*1024);options.setAllowDuplicateKeys(false);Object loaded=new Yaml(new SafeConstructor(options)).load(raw);Object json=loaded instanceof Map?new JSONObject((Map<?,?>)loaded):loaded instanceof List?new JSONArray((List<?>)loaded):null;result.format="Clash / Mihomo";json(json,"");return;}catch(org.yaml.snakeyaml.error.YAMLException e){throw new Exception(L10n.s(R.string.msg_c14abc64af));}
  }
  result.format=L10n.s(R.string.msg_e8d9cd7b4e);int index=0;for(String line:raw.split("\\r?\\n")){line=line.trim();if(line.isEmpty()||line.startsWith("#"))continue;index++;try{if(!line.matches("^[A-Za-z0-9+.-]+://.*"))throw new Exception(L10n.s(R.string.msg_79be6ca6bc));JSONObject node=links.parse(line,profile);if(node!=null)add(node);}catch(Exception e){skip(L10n.s(R.string.msg_30d9e094e4)+index+": "+safeReason(e));}}
 }
 void json(Object value,String title)throws Exception {
  if(value instanceof JSONArray){JSONArray array=(JSONArray)value;for(int i=0;i<array.length();i++){Object part=array.get(i);if(part instanceof JSONObject)json(part,title);else if(part instanceof String){try{add(links.parse((String)part,profile));}catch(Exception e){skip(L10n.s(R.string.msg_54d59eb38a)+(i+1)+": "+safeReason(e));}}else skip(L10n.s(R.string.msg_8eeacd07c0)+(i+1)+L10n.s(R.string.msg_f702b574da));}return;}
  if(!(value instanceof JSONObject))throw new Exception(L10n.s(R.string.msg_e18fd43031));JSONObject object=(JSONObject)value;
  if(object.has("proxies")){int before=result.nodes.length();result.format="Clash / Mihomo";JSONArray proxies=object.optJSONArray("proxies");if(proxies==null)throw new Exception(L10n.s(R.string.msg_48db32c372));for(int i=0;i<proxies.length();i++){final int index=i;attempt(()->clash(proxies.getJSONObject(index)),L10n.s(R.string.msg_1fe1adcef3)+(i+1));}attachDns(object,before);return;}
  if(XrayProfile.grouped(object)){result.format="Xray JSON";attempt(()->{JSONObject graph=XrayProfile.preserve(object);String name=object.optString("remarks",object.optString("name",title));JSONObject outbound=obj("type","xray-group","config",graph);JSONObject node=wrap(name,ServerProtocol.value(outbound),outbound);JSONObject meta=object.optJSONObject("meta");if(meta!=null)node.put("description",meta.optString("serverDescription"));JSONArray aliases=new JSONArray(),out=object.getJSONArray("outbounds");for(int i=0;i<out.length();i++)try{JSONObject member=outbound(out.getJSONObject(i),name,true);if(member!=null)aliases.put(member.getString("id"));}catch(Exception ignored){}node.put("legacyIds",aliases);return node;},L10n.s(R.string.msg_917e05143a));return;}
  if(object.has("outbounds")){int before=result.nodes.length();JSONArray out=object.optJSONArray("outbounds");if(out==null)throw new Exception(L10n.s(R.string.msg_4505a9f9d0));String name=object.optString("remarks",object.optString("name",title));for(int i=0;i<out.length();i++){final int index=i;attempt(()->outbound(out.getJSONObject(index),name,out.length()>1),L10n.s(R.string.msg_1fe1adcef3)+(i+1));}attachDns(object,before);return;}
  if(object.has("protocol")){attempt(()->outbound(object,title,false),L10n.s(R.string.msg_917e05143a));return;}
  if(object.has("type")&&object.has("server")){attempt(()->object.has("port")?clash(object):outbound(object,title,false),L10n.s(R.string.msg_917e05143a));return;}
  throw new Exception(L10n.s(R.string.msg_e271fb3ca1));
 }
 void attachDns(JSONObject document,int start)throws Exception{JSONArray dns=DnsSettings.provider(document);if(dns.length()>0)for(int i=start;i<result.nodes.length();i++)result.nodes.getJSONObject(i).put("providerDns",new JSONArray(dns.toString()));}
 interface NodeAction{JSONObject run()throws Exception;}
 void attempt(NodeAction action,String label){try{JSONObject node=action.run();if(node!=null)add(node);}catch(Exception e){skip(label+": "+safeReason(e));}}
 void skip(String reason){result.skipped++;if(result.warnings.length()<8)result.warnings.put(reason);}
 static String safeReason(Exception e){String s=e.getMessage();if(s==null||s.contains("://")||s.length()>140||e instanceof JSONException)return L10n.s(R.string.msg_3d734741a7);return s;}
 void add(JSONObject node)throws Exception {if(node==null)return;if(result.nodes.length()>=512)throw new Exception(L10n.s(R.string.msg_7131aafc7b));String id=node.getString("id");if(ids.add(id))result.nodes.put(node);}
 JSONObject wrap(String name,String protocol,JSONObject outbound)throws Exception {
  String identity=canonical(outbound);byte[] digest=MessageDigest.getInstance("SHA-256").digest((profile+"|"+identity).getBytes(StandardCharsets.UTF_8));StringBuilder id=new StringBuilder("n");for(int i=0;i<12;i++)id.append(String.format("%02x",digest[i]));
  if(name.isBlank())name=protocol.toUpperCase(Locale.ROOT)+" · "+(result.nodes.length()+1);return obj("id",id.toString(),"profile",profile,"name",name,"protocol",protocol,"outbound",outbound);
 }
 static String canonical(Object value)throws JSONException{if(value instanceof JSONObject){JSONObject object=(JSONObject)value;List<String> keys=new ArrayList<>();Iterator<String> it=object.keys();while(it.hasNext())keys.add(it.next());Collections.sort(keys);StringBuilder s=new StringBuilder("{");for(String key:keys)s.append(JSONObject.quote(key)).append(':').append(canonical(object.get(key))).append(',');return s.append('}').toString();}if(value instanceof JSONArray){StringBuilder s=new StringBuilder("[");JSONArray a=(JSONArray)value;for(int i=0;i<a.length();i++)s.append(canonical(a.get(i))).append(',');return s.append(']').toString();}return value instanceof String?JSONObject.quote((String)value):String.valueOf(value);}
 static void endpoint(String host,int port)throws Exception{if(host.isBlank()||port<1||port>65535)throw new Exception(L10n.s(R.string.msg_210439cffd));}
 JSONObject outbound(JSONObject source,String group,boolean multiple)throws Exception {
  JSONObject o=new JSONObject(source.toString());String type=o.optString("protocol",o.optString("type"));
  if(Arrays.asList("freedom","blackhole","loopback","dns","direct","block","selector","urltest").contains(type))return null;
  String tag=o.optString("tag"),name=o.optString("remarks",o.optString("name",tag));if(!group.isBlank())name=multiple&&!tag.isBlank()?group+" · "+tag:group;
  o.remove("tag");o.remove("name");o.remove("remarks");
  if(o.has("protocol")){
   result.format="Xray JSON";if(!Arrays.asList("vless","vmess","trojan","shadowsocks","socks","http").contains(type))throw new Exception(L10n.s(R.string.msg_6f8352205b)+type);
   JSONObject stream=o.optJSONObject("streamSettings"),sock=stream==null?null:stream.optJSONObject("sockopt");
   if(o.has("proxySettings")||sock!=null&&sock.has("dialerProxy"))throw new Exception(L10n.s(R.string.msg_b329e9ab1b));
   JSONObject settings=o.getJSONObject("settings");JSONArray servers=settings.optJSONArray(type.equals("vless")||type.equals("vmess")?"vnext":"servers");if(servers==null||servers.length()==0)throw new Exception(L10n.s(R.string.msg_67acde8ec5));for(int i=0;i<servers.length();i++)endpoint(servers.getJSONObject(i).optString("address"),servers.getJSONObject(i).optInt("port"));
   return wrap(name,type,obj("type","xray","xray",o));
  }
  result.format="sing-box JSON";if(!Arrays.asList("vless","vmess","trojan","shadowsocks","hysteria","hysteria2","tuic","socks","http","anytls").contains(type))throw new Exception(L10n.s(R.string.msg_6f8352205b)+type);
  if(o.has("detour"))throw new Exception(L10n.s(R.string.msg_b329e9ab1b));endpoint(o.optString("server"),o.optInt("server_port"));return wrap(name,type,o);
 }
 JSONObject clash(JSONObject p)throws Exception {
  String type=p.getString("type");if(type.equals("direct")||type.equals("reject"))return null;
  if(!Arrays.asList("vless","vmess","trojan","ss","hysteria2","hy2","tuic","socks5","http").contains(type))throw new Exception(L10n.s(R.string.msg_6f8352205b)+type);
  if(p.has("dialer-proxy")||p.has("smux")||p.has("plugin"))throw new Exception(L10n.s(R.string.msg_09e323f866));
  endpoint(p.optString("server"),p.optInt("port"));String actual=type.equals("ss")?"shadowsocks":type.equals("hy2")?"hysteria2":type.equals("socks5")?"socks":type;
  JSONObject o=obj("type",actual,"server",p.getString("server"),"server_port",p.getInt("port"));
  if(type.equals("vless")||type.equals("vmess")||type.equals("tuic"))o.put("uuid",p.getString("uuid"));
  if(type.equals("vmess"))o.put("security",p.optString("cipher","auto")).put("alter_id",p.optInt("alterId",0));
  if(type.equals("vless")&&!p.optString("flow").isEmpty())o.put("flow",p.getString("flow"));
  if(Arrays.asList("ss","trojan","hysteria2","hy2","tuic").contains(type))o.put("password",p.getString("password"));
  if(type.equals("ss"))o.put("method",p.getString("cipher"));
  if(type.equals("socks5")||type.equals("http")){if(type.equals("socks5"))o.put("version","5");for(String k:new String[]{"username","password"})if(p.has(k))o.put(k,p.get(k));}
  boolean tls=p.optBoolean("tls")||Arrays.asList("trojan","hysteria2","hy2","tuic").contains(type)||p.has("reality-opts");
  if(tls){JSONObject t=obj("enabled",true,"server_name",p.optString("servername",p.optString("sni",p.getString("server"))));if(p.has("skip-cert-verify"))t.put("insecure",p.optBoolean("skip-cert-verify"));if(p.has("alpn"))t.put("alpn",p.getJSONArray("alpn"));if(p.has("client-fingerprint"))t.put("utls",obj("enabled",true,"fingerprint",p.getString("client-fingerprint")));if(p.has("reality-opts")){JSONObject r=p.getJSONObject("reality-opts");t.put("reality",obj("enabled",true,"public_key",r.getString("public-key"),"short_id",r.optString("short-id")));}o.put("tls",t);}
  if(type.equals("hysteria2")||type.equals("hy2")){if(p.has("obfs"))o.put("obfs",obj("type",p.getString("obfs"),"password",p.optString("obfs-password")));}
  if(type.equals("tuic")){if(p.has("congestion-controller"))o.put("congestion_control",p.get("congestion-controller"));if(p.has("udp-relay-mode"))o.put("udp_relay_mode",p.get("udp-relay-mode"));}
  String net=p.optString("network","tcp");
  if(net.equals("ws")){JSONObject w=p.optJSONObject("ws-opts");if(w==null)w=new JSONObject();JSONObject t=obj("type","ws","path",w.optString("path","/"));if(w.has("headers"))t.put("headers",w.getJSONObject("headers"));if(w.has("max-early-data"))t.put("max_early_data",w.get("max-early-data"));if(w.has("early-data-header-name"))t.put("early_data_header_name",w.get("early-data-header-name"));o.put("transport",t);}
  else if(net.equals("grpc")){JSONObject g=p.optJSONObject("grpc-opts");o.put("transport",obj("type","grpc","service_name",g==null?"":g.optString("grpc-service-name")));}
  else if(net.equals("h2")){JSONObject h=p.optJSONObject("h2-opts");if(h==null)h=new JSONObject();JSONObject t=obj("type","http","path",h.optString("path","/"));if(h.has("host"))t.put("host",h.getJSONArray("host"));o.put("transport",t);}
  else if(!net.equals("tcp")&&!net.equals("raw"))throw new Exception(L10n.s(R.string.msg_0203e5e1bf)+net+L10n.s(R.string.msg_aaecad4a8c));
  if(p.has("packet-encoding"))o.put("packet_encoding",p.get("packet-encoding"));return wrap(p.optString("name"),actual,o);
 }
}
