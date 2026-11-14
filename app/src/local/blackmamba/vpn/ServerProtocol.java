package local.blackmamba.vpn;
import org.json.*;
import java.util.*;

/** Protocols are derived from connection definitions, never from a default badge or server name. */
public final class ServerProtocol {
 private static final Set<String> AUXILIARY=Compat.set("direct","block","freedom","blackhole","loopback","dns","selector","urltest");
 private static void add(Set<String> types,String type){
  type=type.toLowerCase(Locale.ROOT).trim();
  if(type.equals("hy2"))type="hysteria2";
  if(type.equals("ss"))type="shadowsocks";
  if(!type.isEmpty()&&!AUXILIARY.contains(type))types.add(type);
 }
 public static String value(JSONObject outbound){
  if(outbound==null)return "";
  Set<String> types=new LinkedHashSet<>();String type=outbound.optString("type");
  if(type.equals("xray-group")){
   JSONObject config=outbound.optJSONObject("config");JSONArray members=config==null?null:config.optJSONArray("outbounds");
   for(int i=0;members!=null&&i<members.length();i++){JSONObject member=members.optJSONObject(i);if(member!=null)add(types,member.optString("protocol"));}
  }else if(type.equals("xray")||type.equals("xhttp")){
   JSONObject config=outbound.optJSONObject("xray");if(config!=null)add(types,config.optString("protocol"));
  }else add(types,outbound.optString("protocol",type));
  return String.join(" + ",types);
 }
 public static String label(JSONObject node){String protocols=value(node.optJSONObject("outbound"));return protocols.isEmpty()?"—":protocols.toUpperCase(Locale.ROOT);}
 /** Compact display only; transport details never change imported protocol IDs. */
 public static String details(JSONObject node){
  JSONObject outbound=node.optJSONObject("outbound");if(outbound==null)return "—";
  Map<String,Set<String>> entries=new LinkedHashMap<>();String type=outbound.optString("type");
  if(type.equals("xray-group")){
   JSONObject config=outbound.optJSONObject("config");JSONArray members=config==null?null:config.optJSONArray("outbounds");
   for(int i=0;members!=null&&i<members.length();i++)detail(entries,members.optJSONObject(i),true);
  }else if(type.equals("xray")||type.equals("xhttp"))detail(entries,outbound.optJSONObject("xray"),true);
  else detail(entries,outbound,false);
  List<String> labels=new ArrayList<>();
  for(Map.Entry<String,Set<String>> entry:entries.entrySet())labels.add(entry.getKey().toUpperCase(Locale.ROOT)+(entry.getValue().isEmpty()?"":" · "+String.join(" / ",entry.getValue())));
  return labels.isEmpty()?"—":String.join(" + ",labels);
 }
 private static void detail(Map<String,Set<String>> entries,JSONObject outbound,boolean xray){
  if(outbound==null)return;
  Set<String> protocols=new LinkedHashSet<>();add(protocols,outbound.optString("protocol",outbound.optString("type")));
  if(protocols.isEmpty())return;String protocol=protocols.iterator().next();
  Set<String> transports=entries.computeIfAbsent(protocol,k->new LinkedHashSet<>());
  JSONObject settings=outbound.optJSONObject(xray?"streamSettings":"transport");
  String transport=settings==null?"":settings.optString(xray?"network":"type");
  if(Compat.blank(transport)&&Compat.set("vless","vmess","trojan","socks","http","anytls").contains(protocol))transport="tcp";
  if(Compat.blank(transport))return;
  switch(transport.toLowerCase(Locale.ROOT)){
   case "raw":case "tcp":transport="TCP";break;
   case "grpc":transport="gRPC";break;
   case "ws":transport="WebSocket";break;
   case "httpupgrade":transport="HTTPUpgrade";break;
   default:transport=transport.toUpperCase(Locale.ROOT);
  }
  transports.add(transport);
 }
 private ServerProtocol(){}
}
