package local.blackmamba.vpn;
import android.content.Context;import android.net.Network;import io.nekohasekai.libbox.*;import org.json.*;import java.io.*;import java.net.*;import java.util.*;import static local.blackmamba.vpn.Store.*;
/** Isolated, on-demand DNS verification. Never establishes or replaces the Android VPN. */
final class DnsChecker {
 private static final java.util.concurrent.atomic.AtomicBoolean running=new java.util.concurrent.atomic.AtomicBoolean();
 static String check(Context c,JSONObject data)throws Exception {
  if(!running.compareAndSet(false,true))throw new IOException(L10n.s(R.string.dns_checking));
  try{return run(c,new JSONObject(data.toString()));}finally{running.set(false);}
 }
 private static String run(Context c,JSONObject data)throws Exception {
  Network net=ProbeManager.physical(c);if(net==null)throw new IOException(L10n.s(R.string.msg_f0a50c59fc));
  android.net.NetworkCapabilities nc=c.getSystemService(android.net.ConnectivityManager.class).getNetworkCapabilities(net);String network=nc!=null&&nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)?"cellular":"wifi";
  data.put("networkIpv6",TunnelService.supportsIpv6(c.getSystemService(android.net.ConnectivityManager.class).getLinkProperties(net)));
  String selected=data.optString(data.optBoolean("auto",true)?network:data.optString("manual","wifi"));
  File cache=new File(c.getCacheDir(),"dns-check-"+UUID.randomUUID()+".db");BoxService box=null;XrayProcesses processes=null;List<ServerSocket> held=new ArrayList<>();
  try{
   Map<String,Integer> ports=new HashMap<>();JSONArray nodes=data.getJSONArray("servers");for(int i=0;i<nodes.length();i++){ServerSocket socket=new ServerSocket(0,0,InetAddress.getByName("127.0.0.1"));held.add(socket);ports.put(nodes.getJSONObject(i).getString("id"),socket.getLocalPort());}
   Configuration.Result built=Configuration.build(data,network,ports,UUID.randomUUID().toString());JSONObject config=new JSONObject(built.singbox);config.put("inbounds",new JSONArray());config.put("experimental",obj("cache_file",obj("enabled",true,"path",cache.getAbsolutePath())));
   SetupOptions setup=new SetupOptions();setup.setBasePath(c.getFilesDir().getPath());setup.setWorkingPath(c.getFilesDir().getPath());setup.setTempPath(c.getCacheDir().getPath());setup.setFixAndroidStack(true);Libbox.setup(setup);
   for(ServerSocket socket:held)socket.close();held.clear();Map<String,String> graphs=new LinkedHashMap<>(built.groups);if(built.xray!=null)graphs.put("__single",built.xray);processes=new XrayProcesses(c,graphs);processes.ensure("__single");processes.ensure(selected);
   JSONArray rules=data.optJSONArray("rules");for(int i=0;rules!=null&&i<rules.length();i++)if(rules.getJSONObject(i).optBoolean("enabled",true))processes.ensure(rules.getJSONObject(i).optString("target").replace("server:",""));processes.ensure(data.optString("defaultTarget").replace("server:",""));
   box=Libbox.newService(config.toString(),new ProbePlatform(c,net));box.start();box.mobileSelect(selected);int dns=box.mobileDNSCheck();
   if(!net.equals(ProbeManager.physical(c)))throw new IOException(L10n.s(R.string.dns_network_changed));
   return L10n.s(R.string.dns_check_ok)+" · "+dns+" ms";
  }finally{if(box!=null)try{box.close();}catch(Exception ignored){}if(processes!=null)processes.close();for(ServerSocket socket:held)try{socket.close();}catch(Exception ignored){}cache.delete();}
 }
}
