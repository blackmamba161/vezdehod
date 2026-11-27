package local.blackmamba.vpn;

import android.net.Network;
import android.net.DnsResolver;
import android.os.CancellationSignal;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Resolve proxy endpoints on the physical Android network, not inside the Go subprocess.
 * Only the generated shared Xray configuration is touched; provider graphs keep their own DNS.
 * Keep endpoint hostnames intact for SNI, certificate validation and HTTP authority. */
final class XrayBootstrap {
 private XrayBootstrap() {}

 private static Set<String> targets(JSONObject o){
  Set<String> hosts=new LinkedHashSet<>();JSONObject s=o.optJSONObject("settings");if(s==null)return hosts;
  add(hosts,s.optString("address"));
  for(String key:new String[]{"vnext","servers"}){JSONArray arr=s.optJSONArray(key);for(int i=0;arr!=null&&i<arr.length();i++){JSONObject ep=arr.optJSONObject(i);if(ep!=null)add(hosts,ep.optString("address"));}}
  return hosts;
 }
 private static void add(Set<String> hosts,String name){if(!name.isEmpty()&&!name.contains(":")&&!name.matches("[0-9.]+"))hosts.add(name.toLowerCase(Locale.ROOT));}
 private static boolean eligible(JSONObject o){JSONObject st=o.optJSONObject("streamSettings"),so=st==null?null:st.optJSONObject("sockopt");return so==null||(!so.has("dialerProxy")&&!so.has("domainStrategy"));}

 static String runtime(String config,Network network)throws Exception{
  if(network==null)return config;
  JSONObject graph=new JSONObject(config);if(graph.has("dns"))return config;
  JSONArray out=graph.getJSONArray("outbounds");Set<String> names=new LinkedHashSet<>();
  for(int i=0;i<out.length();i++){JSONObject o=out.getJSONObject(i);if(eligible(o))names.addAll(targets(o));}
  if(names.isEmpty())return config;
  long started=android.os.SystemClock.elapsedRealtime();Map<String,List<String>> answers=new ConcurrentHashMap<>();
  // Use the Android resolver for the captured network, including its Private DNS policy.
  // Keep A and AAAA independent: an unanswered family must not discard a usable answer.
  CountDownLatch done=new CountDownLatch(names.size()*2);Semaphore slots=new Semaphore(32);List<CancellationSignal> pending=new ArrayList<>();
  long end=started+6000;
  try{
   outer:for(int type:new int[]{DnsResolver.TYPE_A,DnsResolver.TYPE_AAAA})for(String name:names){
    long left=end-android.os.SystemClock.elapsedRealtime();if(left<=0||!slots.tryAcquire(left,TimeUnit.MILLISECONDS))break outer;
    CancellationSignal signal=new CancellationSignal();pending.add(signal);
    try{DnsResolver.getInstance().query(network,name,type,DnsResolver.FLAG_NO_RETRY,Runnable::run,signal,new DnsResolver.Callback<List<InetAddress>>(){
     private void finish(){slots.release();done.countDown();}
     public void onAnswer(List<InetAddress> ips,int rcode){try{if(!signal.isCanceled()&&rcode==0&&!ips.isEmpty())answers.compute(name,(ignored,old)->{List<String> merged=old==null?new ArrayList<>():new ArrayList<>(old);for(InetAddress ip:ips)if(!merged.contains(ip.getHostAddress()))merged.add(ip.getHostAddress());return merged;});}finally{finish();}}
     public void onError(DnsResolver.DnsException error){finish();}
    });}catch(RuntimeException unavailable){slots.release();done.countDown();}
   }
   done.await(Math.max(0,end-android.os.SystemClock.elapsedRealtime()),TimeUnit.MILLISECONDS);
  }finally{for(CancellationSignal signal:pending)signal.cancel();}
  ProbeTrace.counts("bootstrap",answers.size(),names.size(),android.os.SystemClock.elapsedRealtime()-started);
  return apply(graph,new HashMap<>(answers))?graph.toString():config;
 }
 static boolean apply(JSONObject graph,Map<String,List<String>> answers)throws JSONException{
  if(answers.isEmpty()||graph.has("dns"))return false;
  JSONArray out=graph.getJSONArray("outbounds");JSONObject hosts=new JSONObject();
  for(int i=0;i<out.length();i++){JSONObject o=out.getJSONObject(i);Set<String> names=targets(o);if(!eligible(o)||names.isEmpty()||!answers.keySet().containsAll(names))continue;
   for(String name:names)hosts.put("full:"+name,new JSONArray(answers.get(name)));
   JSONObject st=o.optJSONObject("streamSettings");if(st==null)o.put("streamSettings",st=new JSONObject());
   JSONObject sock=st.optJSONObject("sockopt");if(sock==null)st.put("sockopt",sock=new JSONObject());sock.put("domainStrategy","UseIP");
  }
  if(hosts.length()==0)return false;graph.put("dns",new JSONObject().put("hosts",hosts));return true;
 }
}
