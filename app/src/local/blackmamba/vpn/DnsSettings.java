package local.blackmamba.vpn;
import org.json.*;import java.net.*;import java.util.*;import static local.blackmamba.vpn.Store.*;
/** DNS policy is separate from provider graphs and from remembered manual values. */
public final class DnsSettings {
 static final String GOOGLE="https://dns.google/dns-query",CLOUDFLARE="https://cloudflare-dns.com/dns-query";
 static final String[] KEYS={"dnsMode","remoteDns","dnsBackup","dnsDirect","dnsBootstrap"};
 static JSONObject defaults()throws JSONException{return obj("dnsMode","auto","remoteDns",GOOGLE,"dnsBackup",CLOUDFLARE,"dnsDirect","","dnsBootstrap","");}
 static boolean manual(JSONObject data){return data.optString("dnsMode","auto").equals("manual");}
 static String address(String raw)throws Exception{
  String text=raw.trim();if(text.isEmpty())throw new Exception(L10n.s(R.string.dns_invalid));
  if(text.chars().anyMatch(Character::isWhitespace))throw new Exception(L10n.s(R.string.dns_invalid));
  if(!text.contains("://")){if(!ip(text))throw new Exception(L10n.s(R.string.dns_invalid));return text;}
  URI u=new URI(text);String scheme=u.getScheme(),host=u.getHost();
  if(!Arrays.asList("https","tls","udp","tcp").contains(scheme)||host==null||u.getUserInfo()!=null||u.getFragment()!=null||u.getPort()<-1||u.getPort()==0||u.getPort()>65535)throw new Exception(L10n.s(R.string.dns_invalid));
  if(!scheme.equals("https")&&((u.getPath()!=null&&!u.getPath().isEmpty())||u.getQuery()!=null))throw new Exception(L10n.s(R.string.dns_invalid));
  return text;
 }
 static boolean ip(String host){if(host.matches("[0-9.]+")){String[] a=host.split("\\.",-1);if(a.length!=4)return false;for(String n:a)try{if(n.isEmpty()||Integer.parseInt(n)>255)return false;}catch(Exception e){return false;}return true;}if(host.contains(":"))try{return InetAddress.getByName(host.replace("[","").replace("]","")) instanceof Inet6Address;}catch(Exception ignored){}return false;}
 static void validate(JSONObject d)throws Exception{
  if(!Arrays.asList("auto","manual").contains(d.optString("dnsMode","auto")))throw new Exception(L10n.s(R.string.dns_invalid));
  if(!manual(d))return;address(d.optString("remoteDns",GOOGLE));for(String k:new String[]{"dnsBackup","dnsDirect","dnsBootstrap"})if(!d.optString(k).isBlank())address(d.optString(k));
 }
 static JSONArray provider(JSONObject document)throws JSONException{
  JSONArray result=new JSONArray();JSONObject dns=document.optJSONObject("dns");if(dns==null)return result;
  String finalTag=dns.optString("final");JSONArray servers=dns.optJSONArray("servers");if(servers==null)servers=dns.optJSONArray("nameserver");if(servers==null)return result;
  // Scoped resolvers and detours have provider-specific semantics: retain those inside the original graph.
  for(int i=0;i<servers.length()&&result.length()<2;i++)try{Object value=servers.get(i);if(!finalTag.isEmpty()&&(!(value instanceof JSONObject)||!finalTag.equals(((JSONObject)value).optString("tag"))))continue;String raw="";
   if(value instanceof String)raw=(String)value;
   else if(value instanceof JSONObject){JSONObject o=(JSONObject)value;if(o.has("domains")||o.has("expectIPs")||o.has("detour"))continue;raw=o.optString("address");if(!raw.isEmpty()&&o.has("port")){if(raw.contains("://"))continue;raw="udp://"+(raw.contains(":")?"["+raw+"]":raw)+":"+o.getInt("port");}if(raw.isEmpty()){String t=o.optString("type"),h=o.optString("server");if(t.equals("https"))raw="https://"+h+(o.has("server_port")?":"+o.getInt("server_port"):"")+o.optString("path","/dns-query");else if(t.equals("tls"))raw="tls://"+h+(o.has("server_port")?":"+o.getInt("server_port"):"");}}
   String normalized=address(raw);boolean exists=false;for(int j=0;j<result.length();j++)exists|=normalized.equals(result.getString(j));if(!exists)result.put(normalized);
  }catch(Exception ignored){}return result;
 }
 static JSONArray providerFor(JSONObject node)throws JSONException {if(node==null)return new JSONArray();JSONArray p=node.optJSONArray("providerDns");if(p!=null)return p;JSONObject o=node.optJSONObject("outbound");return o!=null&&o.optJSONObject("config")!=null?provider(o.getJSONObject("config")):new JSONArray();}
 static String[] remote(JSONObject d,JSONObject node)throws Exception{
  if(manual(d))return new String[]{address(d.optString("remoteDns",GOOGLE)),d.optString("dnsBackup").isBlank()?"":address(d.optString("dnsBackup"))};
  JSONArray p=providerFor(node);String primary=p.length()>0?p.getString(0):GOOGLE,backup=p.length()>1?p.getString(1):primary.equals(GOOGLE)?CLOUDFLARE:GOOGLE;return new String[]{primary,backup};
 }
 static boolean changed(JSONObject before,JSONObject after){for(String key:KEYS)if(!before.optString(key).equals(after.optString(key)))return true;return false;}
 static boolean effectiveChanged(JSONObject before,JSONObject after){if(manual(before)!=manual(after))return true;return manual(after)&&changed(before,after);}
 static String summary(JSONObject d){return L10n.s(manual(d)?R.string.dns_manual:R.string.dns_auto);}
 static void endpoint(JSONArray servers,String tag,String raw,String detour)throws Exception{
  String a=address(raw);JSONObject o=obj("tag",tag,"address",a,"detour",detour);
  String host=a.contains("://")?new URI(a).getHost():a;
  if(!ip(host))o.put("address_resolver","bootstrap");servers.put(o);
 }
 static void pair(JSONArray servers,String tag,String primary,String backup,String detour)throws Exception{
  endpoint(servers,tag+"-primary",primary,detour);JSONArray names=arr(tag+"-primary");
  if(!backup.isEmpty()&&!backup.equals(primary)){endpoint(servers,tag+"-backup",backup,detour);names.put(tag+"-backup");}
  servers.put(obj("type","vezdehod-fallback","tag",tag,"servers",names));
 }
 static JSONObject build(JSONObject d,JSONArray nodes,String active,JSONArray rules,String fin,boolean ipv6)throws Exception{
  validate(d);JSONArray servers=new JSONArray();servers.put(obj("tag","system","address","local","detour","direct"));
  String bootstrap=manual(d)?d.optString("dnsBootstrap"):"";
  if(bootstrap.isEmpty())servers.put(obj("type","vezdehod-fallback","tag","bootstrap","servers",arr("system","bootstrap-public")));
  else {JSONObject b=obj("tag","bootstrap-custom","address",address(bootstrap),"detour","direct");String h=bootstrap.contains("://")?new URI(bootstrap).getHost():bootstrap;if(!ip(h))b.put("address_resolver","system");servers.put(b);servers.put(obj("type","vezdehod-fallback","tag","bootstrap","servers",arr("bootstrap-custom")));}
  servers.put(obj("tag","bootstrap-public","address","9.9.9.9","detour","direct"));
  String direct=manual(d)?d.optString("dnsDirect"):"";
  if(direct.isEmpty())servers.put(obj("type","vezdehod-fallback","tag","local","servers",arr("system","bootstrap-public")));else pair(servers,"local",direct,"","direct");
  JSONObject selections=new JSONObject();Map<String,String> policies=new LinkedHashMap<>();
  for(int i=0;i<nodes.length();i++){JSONObject node=nodes.getJSONObject(i);String[] rem=remote(d,node);String key=Arrays.toString(rem),tag=policies.get(key);if(tag==null){tag="remote-policy-"+policies.size();policies.put(key,tag);pair(servers,tag,rem[0],rem[1],"auto");}selections.put(node.getString("id"),tag);}
  servers.put(obj("type","vezdehod-selector","tag","remote","selector","auto","routes",selections));
  Set<String> specific=new LinkedHashSet<>();for(int i=0;i<rules.length();i++){String tag=rules.getJSONObject(i).optString("server");if(tag.startsWith("dns-node-"))specific.add(tag.substring(9));}
  if(!fin.equals("auto")&&!fin.equals("direct"))specific.add(fin);
  for(String id:specific){JSONObject node=find(nodes,id);if(node==null)throw new Exception("DNS route references missing server");String[] r=remote(d,node);pair(servers,"dns-node-"+id,r[0],r[1],id);}
  return obj("servers",servers,"rules",rules,"final",fin.equals("direct")?"local":fin.equals("auto")?"remote":"dns-node-"+fin,"strategy",ipv6?"prefer_ipv4":"ipv4_only","independent_cache",true);
 }
}
