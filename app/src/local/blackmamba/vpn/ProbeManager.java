package local.blackmamba.vpn;
import android.content.Context;import android.net.*;import io.nekohasekai.libbox.*;import org.json.*;import java.io.*;import java.net.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import static local.blackmamba.vpn.Store.*;
/** A bounded batch independent of the live VPN. Every temporary engine is closed in finally. */
final class ProbeManager {
 static final ProbeManager INSTANCE=new ProbeManager();private final ExecutorService coordinator=Executors.newSingleThreadExecutor();volatile Batch current;volatile Network resultNetwork;volatile String error="";
 static final class Batch {final Map<String,String> errors=new ConcurrentHashMap<>();final Set<String> pending=ConcurrentHashMap.newKeySet();volatile int total;final AtomicInteger completed=new AtomicInteger();final AtomicBoolean canceled=new AtomicBoolean();final Network network;volatile BoxService box;volatile XrayProcesses processes;volatile ExecutorService workers;volatile Configuration.Result built;Batch(int total,Network n){this.total=total;network=n;}}
 boolean running(){return current!=null;}boolean pending(String id){Batch b=current;return b!=null&&b.pending.contains(id);}String progress(){Batch b=current;return b==null?"":b.completed.get()+"/"+b.total;}
 synchronized void checkNetwork(Context c){Network next=physical(c);if(resultNetwork!=null&&!resultNetwork.equals(next)){cancel();TunnelService.pings.clear();resultNetwork=next;}}
 synchronized void cancel(){Batch b=current;if(b==null)return;b.canceled.set(true);b.pending.clear();if(b.box!=null)try{b.box.mobileCancel();}catch(Exception ignored){}if(b.workers!=null)b.workers.shutdownNow();}
 static Network physical(Context context){ConnectivityManager cm=context.getSystemService(ConnectivityManager.class);Network best=null;int score=-1;for(Network n:cm.getAllNetworks()){NetworkCapabilities c=cm.getNetworkCapabilities(n);if(c==null||c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)||!c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))continue;int s=(c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)?100:0)+(c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?20:10);if(s>score){score=s;best=n;}}return best;}
 /* Servers tapped while a batch runs wait here and start as soon as it finishes; their radars show "pending" meanwhile. */
 private final Map<String,JSONObject> queued=new LinkedHashMap<>();
 /** A batch is not extensible (its config is fixed), so extra taps queue up and run right after it. */
 synchronized void start(Context context,JSONArray source)throws Exception{
  if(source.length()==0)return;Context c=context.getApplicationContext();Network n=physical(c);if(n==null)throw new IOException(L10n.s(R.string.msg_f0a50c59fc));
  Batch live=current;
  if(live!=null){
   /* Same network: queue. Different network: the old batch is stale, cancel it and start fresh. */
   if(live.network.equals(n)&&!live.canceled.get()){for(int i=0;i<source.length();i++){JSONObject node=source.getJSONObject(i);String id=node.getString("id");if(live.pending.contains(id))continue;queued.put(id,node);live.pending.add(id);live.total++;TunnelService.pings.remove(id);}return;}
   cancel();
  }
  launch(c,n,source);
 }
 private void launch(Context c,Network n,JSONArray source)throws Exception{JSONArray nodes=new JSONArray(source.toString());Batch b=new Batch(nodes.length(),n);for(int i=0;i<nodes.length();i++){String id=nodes.getJSONObject(i).getString("id");b.pending.add(id);TunnelService.pings.remove(id);}error="";resultNetwork=n;current=b;coordinator.execute(()->run(c,b,nodes));}
 /** Schedule one node on a running batch. The caller has already counted it in. */
 void submit(Context c,Batch b,JSONObject node){b.workers.submit(()->probe(c,b,node));}
 /** Probe a single node on a live batch; every exit path clears its pending flag. */
 void probe(Context c,Batch b,JSONObject node){String id=node.optString("id");int ping=-1;Configuration.Result built=b.built;try{if(b.canceled.get())return;if(!b.network.equals(physical(c)))throw new IOException("Network changed");b.processes.ensure(id);long deadline=System.currentTimeMillis()+8000;while(!b.canceled.get()){long left=deadline-System.currentTimeMillis();if(left<=0)break;try{ping=b.box.mobileDelayFor(id,(int)Math.min(6000,left));b.errors.remove(id);break;}catch(Exception e){b.errors.put(id,TunnelService.safeError(e));if(!built.groups.containsKey(id)||System.currentTimeMillis()>=deadline)throw e;Thread.sleep(150);}}}catch(Exception problem){b.errors.put(id,TunnelService.safeError(problem));NetworkErrors.record("probe",id,problem.toString());}finally{if(!b.canceled.get()&&b.network.equals(physical(c)))TunnelService.pings.put(id,ping);b.pending.remove(id);b.completed.incrementAndGet();b.processes.release(id);}}
 void run(Context c,Batch b,JSONArray nodes){List<ServerSocket> held=new ArrayList<>();File probeCache=new File(c.getCacheDir(),"probe-"+UUID.randomUUID()+".db");try{
  if(b.canceled.get())return;String first=nodes.getJSONObject(0).getString("id"),password=UUID.randomUUID().toString();Map<String,Integer> ports=new HashMap<>();for(int i=0;i<nodes.length();i++){String id=nodes.getJSONObject(i).getString("id");ServerSocket ss=new ServerSocket(0,0,InetAddress.getLoopbackAddress());held.add(ss);ports.put(id,ss.getLocalPort());}
  JSONObject data=obj("servers",nodes,"rules",new JSONArray(),"wifi",first,"cellular",first,"auto",true,"defaultTarget","direct");Configuration.Result built=Configuration.build(data,"wifi",ports,password);JSONObject config=new JSONObject(built.singbox);config.put("inbounds",new JSONArray());config.put("experimental",obj("cache_file",obj("enabled",true,"path",probeCache.getAbsolutePath())));config.put("dns",obj("servers",arr(obj("tag","local","address","local","detour","direct")),"final","local","strategy","prefer_ipv4"));config.put("route",obj("final",first,"auto_detect_interface",true,"default_domain_resolver","local"));
  SetupOptions setup=new SetupOptions();setup.setBasePath(c.getFilesDir().getPath());setup.setWorkingPath(c.getFilesDir().getPath());setup.setTempPath(c.getCacheDir().getPath());setup.setFixAndroidStack(true);Libbox.setup(setup);CoreRuntime.configure(c);
  for(ServerSocket ss:held)ss.close();held.clear();Map<String,String> definitions=new LinkedHashMap<>(built.groups);if(built.xray!=null)definitions.put("__single",XrayBootstrap.runtime(built.xray,b.network));b.processes=new XrayProcesses(c,definitions);b.processes.ensure("__single");b.box=Libbox.newService(config.toString(),new ProbePlatform(c,b.network));b.box.start();if(b.canceled.get())return;
  b.built=built;b.workers=Executors.newFixedThreadPool(4);for(int i=0;i<nodes.length();i++)submit(c,b,nodes.getJSONObject(i));
  b.workers.shutdown();while(!b.workers.awaitTermination(200,TimeUnit.MILLISECONDS)){if(b.canceled.get()||!b.network.equals(physical(c))){b.canceled.set(true);b.workers.shutdownNow();break;}}
 }catch(Exception e){if(!b.canceled.get()){error=TunnelService.safeError(e);NetworkErrors.record("probe-start","",e.toString());}}finally{
  if(ProbeTrace.enabled()){int good=0;for(int i=0;i<nodes.length();i++){JSONObject n=nodes.optJSONObject(i);Integer ms=n==null?null:TunnelService.pings.get(n.optString("id"));if(ms!=null&&ms>=0)good++;}ProbeTrace.counts("batch",good,nodes.length(),0);}
  if(b.workers!=null)b.workers.shutdownNow();if(b.box!=null)try{b.box.close();}catch(Exception ignored){}if(b.processes!=null)b.processes.close();probeCache.delete();for(ServerSocket ss:held)try{ss.close();}catch(Exception ignored){}b.pending.clear();
  /* Drain the queue: anything tapped during this batch starts now as its own batch. */
  JSONArray next=new JSONArray();Network keep=b.network;synchronized(this){if(current==b)current=null;if(!queued.isEmpty()&&keep!=null&&keep.equals(physical(c))){for(JSONObject node:queued.values())next.put(node);queued.clear();}}
  if(next.length()>0)try{launch(c,keep,next);}catch(Exception ignored){}
 }}
}
