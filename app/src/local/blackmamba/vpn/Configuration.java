package local.blackmamba.vpn;
import org.json.*;import java.util.*;import static local.blackmamba.vpn.Store.*;

public final class Configuration {
 public static final class Result {public String singbox,xray;public final Map<String,String> groups=new LinkedHashMap<>();}
 public static Result build(JSONObject data,String network,Map<String,Integer> ports,String password)throws Exception{
  Preferences.validate("mtu",data.optInt("mtu",data.optInt("mtu",1400)));DnsSettings.validate(data);
  boolean ipv6=data.optBoolean("ipv6",true)&&data.optBoolean("networkIpv6",true);
  JSONArray nodes=data.getJSONArray("servers");if(nodes.length()==0)throw new Exception(L10n.s(R.string.msg_85973145cb));
  String active=data.optString(data.optBoolean("auto",true)?network:data.optString("manual","wifi"));if(find(nodes,active)==null)throw new Exception(L10n.s(R.string.msg_fa9ffb3f84));
  Result res=new Result();JSONArray out=new JSONArray(),tags=new JSONArray(),xi=new JSONArray(),xo=new JSONArray(),xr=new JSONArray();
  for(int i=0;i<nodes.length();i++){JSONObject n=nodes.getJSONObject(i),o=new JSONObject(n.getJSONObject("outbound").toString());String id=n.getString("id");tags.put(id);
   if(o.getString("type").equals("xray-group")){Integer port=ports.get(id);if(port==null)throw new Exception("Missing group port");res.groups.put(id,XrayProfile.runtime(o.getJSONObject("config"),port,password));o=obj("type","socks","server","127.0.0.1","server_port",port,"version","5","username","vpn","password",password);}
   else if(Arrays.asList("xhttp","xray").contains(o.getString("type"))){Integer port=ports.get(id);if(port==null)throw new Exception(L10n.s(R.string.msg_27a403dd36));JSONObject x=o.getJSONObject("xray");x.put("tag",id);xo.put(x);xi.put(obj("tag","in-"+id,"listen","127.0.0.1","port",port,"protocol","socks","settings",obj("auth","password","accounts",arr(obj("user","vpn","pass",password)),"udp",true)));xr.put(obj("type","field","inboundTag",arr("in-"+id),"outboundTag",id));o=obj("type","socks","server","127.0.0.1","server_port",port,"version","5","username","vpn","password",password);}
   o.put("tag",id);out.put(o);
  }
  out.put(obj("type","selector","tag","auto","outbounds",tags,"default",active,"interrupt_exist_connections",true));out.put(obj("type","direct","tag","direct"));
  JSONArray rules=arr(obj("action","sniff"),obj("protocol","dns","action","hijack-dns"));if(!ipv6)rules.put(obj("ip_version",6,"action","reject"));JSONArray dnsRules=new JSONArray();
  if(data.optBoolean("adBlock",false)){rules.put(AdBlock.rejection(data));dnsRules.put(AdBlock.rejection(data));}
  JSONArray rr=data.getJSONArray("rules");for(int i=0;i<rr.length();i++){JSONObject r=rr.getJSONObject(i);if(!r.optBoolean("enabled",true))continue;String type=r.optString("type"),v=r.optString("value"),target=r.optString("target","auto");if(type.equals("unsupported"))continue;
   String field=Map.of("app","package_name","domain","domain","domain_suffix","domain_suffix","cidr","ip_cidr").get(type);if(field==null)throw new Exception(L10n.s(R.string.msg_2846df10fa));JSONObject rule=obj(field,arr(v));String net=r.optString("network","all");if(!net.equals("all"))rule.put("network_type",arr(net));if(target.equals("block"))rule.put("action","reject");else{String tag=target.replace("server:","");if(!tag.equals("auto")&&!tag.equals("direct")&&find(nodes,tag)==null)throw new Exception(L10n.s(R.string.msg_4dfebb89a6)+r.optString("label",v));rule.put("action","route");rule.put("outbound",tag);}rules.put(rule);
   if(type.equals("app")||type.equals("domain")||type.equals("domain_suffix")){JSONObject d=obj(field,arr(v));if(target.equals("block"))d.put("action","reject");else d.put("server",target.equals("direct")?"local":target.equals("auto")?"remote":"dns-node-"+target.replace("server:",""));if(!net.equals("all"))d.put("network_type",arr(net));dnsRules.put(d);}
  }
  String fin=data.optString("defaultTarget","direct").replace("server:","");if(fin.equals("block")){rules.put(obj("action","reject"));dnsRules.put(obj("action","reject"));fin="direct";}
  JSONObject config=obj("log",obj("level","error","timestamp",false),"dns",DnsSettings.build(data,nodes,active,dnsRules,fin,ipv6),"inbounds",arr(obj("type","tun","tag","tun-in","address",ipv6?arr("172.19.0.1/30","fdfe:dcba:9876::1/126"):arr("172.19.0.1/30"),"mtu",data.optInt("mtu",1400),"auto_route",true,"stack",data.optString("stack","gvisor"))),"outbounds",out,"route",obj("rules",rules,"final",fin,"auto_detect_interface",true,"default_domain_resolver","bootstrap"),"experimental",obj("clash_api",obj()));
  AdBlock.attach(config,data);
  res.singbox=config.toString();if(xi.length()>0)res.xray=obj("log",obj("loglevel","none"),"inbounds",xi,"outbounds",xo,"routing",obj("rules",xr)).toString();return res;
 }
}
