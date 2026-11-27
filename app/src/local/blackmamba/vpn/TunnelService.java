package local.blackmamba.vpn;
import android.app.Notification;
import android.app.*;import android.content.*;import android.content.pm.*;import android.net.*;import android.os.*;import io.nekohasekai.libbox.*;import org.json.*;import java.net.*;import java.io.*;import java.util.*;import java.util.concurrent.*;import static local.blackmamba.vpn.Store.*;

public class TunnelService extends VpnService implements PlatformInterface {
 public static volatile TunnelService instance;public static volatile String status="off",error="",network="wifi",server="";public static volatile String health=L10n.s(R.string.msg_5f496335d3);public static volatile int lastPing=-1;public static final Map<String,Integer> pings=new ConcurrentHashMap<>();public static volatile double down=0,up=0;public static volatile long totalDown=0,totalUp=0;private volatile Configuration.Plan connectedScope=null;
 private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();private final ExecutorService tests=Executors.newFixedThreadPool(2);private final ExecutorService pingWorker=Executors.newFixedThreadPool(2);private final Map<String,List<TestResult>> pendingPings=new HashMap<>();private final Set<String> refreshing=ConcurrentHashMap.newKeySet();private final Map<String,Long> refreshAttempt=new ConcurrentHashMap<>();private final ConnectionDeadline connectionDeadline=new ConnectionDeadline();private final ExecutorService aborter=Executors.newSingleThreadExecutor();private final Object tunLock=new Object();private volatile BoxService box;private ParcelFileDescriptor tun;private java.lang.Process xray;private XrayProcesses groups;private volatile Network underlying;private boolean coreIpv6;SocketProtector socketProtector;private InterfaceUpdateListener listener;private ConnectivityManager cm;private boolean registered=false;private volatile boolean wanted=false;private volatile long epoch=0;private long retryAt=0,lastHealth=0;private long scannedEpoch=-1;private boolean checkingHealth=false;private final FailoverPolicy failover=new FailoverPolicy();private String primary="";private long lastTime=0,lastUp=0,lastDown=0;private String password=UUID.randomUUID().toString();private final Map<String,Integer> ports=new HashMap<>();private ScheduledFuture<?> refresh;private int deadChecks=0;private volatile VpnRecovery recovery;
 private final java.util.concurrent.atomic.AtomicBoolean networkQueued=new java.util.concurrent.atomic.AtomicBoolean();private final java.util.concurrent.atomic.AtomicBoolean reloadQueued=new java.util.concurrent.atomic.AtomicBoolean();
 private final ExecutorService healthChecks=Executors.newFixedThreadPool(2);
 static volatile java.util.function.Function<String,Integer> testHealthReply;
 private final java.util.concurrent.atomic.AtomicBoolean stopping=new java.util.concurrent.atomic.AtomicBoolean();
 private volatile boolean shutdownComplete;
 private volatile long stopStarted;
 private static final String STOP_LOG="VezdehodStop";
 boolean isStopping(){return stopping.get();}
 private final ConnectivityManager.NetworkCallback callback=new ConnectivityManager.NetworkCallback(){public void onAvailable(Network n){scheduleNetwork();}public void onLost(Network n){scheduleNetwork();}public void onCapabilitiesChanged(Network n,NetworkCapabilities c){scheduleNetwork();}public void onLinkPropertiesChanged(Network n,LinkProperties p){scheduleNetwork();}};
 public static void start(Context c){c.startForegroundService(new Intent(c,TunnelService.class).setAction("start"));}
 public static void stop(Context c){TunnelService current=instance;if(current!=null)current.requestStop();}
 public static void reload(){TunnelService s=instance;if(s==null||!s.reloadQueued.compareAndSet(false,true))return;try{s.worker.execute(()->{s.reloadQueued.set(false);if(s.wanted)s.startCore();});}catch(RejectedExecutionException ignored){s.reloadQueued.set(false);}}
 @Override public void onCreate(){super.onCreate();instance=this;cm=getSystemService(ConnectivityManager.class);try{socketProtector=new SocketProtector(this,()->underlying);}catch(IOException e){throw new IllegalStateException("Cannot create outbound socket protection",e);}getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("vpn",L10n.s(R.string.msg_643b39ad20),NotificationManager.IMPORTANCE_LOW));}
 @Override public void onConfigurationChanged(android.content.res.Configuration configuration){super.onConfigurationChanged(configuration);L10n.invalidate();getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("vpn",L10n.s(R.string.msg_643b39ad20),NotificationManager.IMPORTANCE_LOW));if(wanted)notifyStatus(currentStatusText());}
 private SharedPreferences sessionPreferences(){return getSharedPreferences("vpn-session",MODE_PRIVATE);}
 @Override public int onStartCommand(Intent i,int flags,int startId){
  if(i!=null&&"stop".equals(i.getAction())){requestStop();return START_NOT_STICKY;}
  if(isStopping())return START_NOT_STICKY;
  boolean bare=i!=null&&"test-bare-tun".equals(i.getAction())&&(getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0;
  if(i==null&&(!sessionPreferences().getBoolean("requested",false)||!Store.get(this).data.optBoolean("reconnect",true)||prepare(this)!=null)){
   stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(startId);return START_NOT_STICKY;
  }
  if(!bare)sessionPreferences().edit().putBoolean("requested",true).apply();
  notifyStatus(wanted?currentStatusText():L10n.s(R.string.msg_40b27edf9a));
  worker.execute(()->{
   if(wanted||isStopping())return;wanted=true;if(isStopping()){wanted=false;return;}
   try{
    if(!registered){cm.registerNetworkCallback(new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build(),callback);registered=true;}
    updateNetwork();if(bare){startBareTun();return;}startCore();
    if(wanted)refresh=worker.scheduleWithFixedDelay(()->{stats();autoRefresh();maintenance();},1,1,TimeUnit.SECONDS);
   }catch(Exception e){if(wanted)fail(e);}
  });
  // Android may restore a user-started foreground VPN after process eviction, but not after a user stop.
  return bare?START_NOT_STICKY:START_STICKY;
 }
 private void startBareTun()throws Exception{
  Builder builder=new Builder().setSession("TUN shutdown test").setMtu(1400).setMetered(network.equals("cellular"));
  builder.addAddress("172.19.0.1",30).addRoute("192.0.2.0",24).addAllowedApplication(getPackageName());
  synchronized(tunLock){if(!wanted)return;tun=builder.establish();if(tun==null)throw new IOException("Could not establish test TUN");}
  status="connected";
 }
 ParcelFileDescriptor keepTestTunOpen()throws IOException{
  if((getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)==0)throw new SecurityException("Test build required");
  synchronized(tunLock){if(tun==null)throw new IOException("Test TUN is not open");return ParcelFileDescriptor.dup(tun.getFileDescriptor());}
 }
 VpnRecovery directRecovery(){VpnRecovery state=recovery;return state!=null&&wanted&&status.equals("connected")&&server.equals("direct")?state:null;}
 String currentStatusText(){return switch(status){
  case "connected"->label();case "waiting"->L10n.s(R.string.msg_a6ee13c1ee);
  case "retrying"->L10n.s(R.string.connection_retry_pending);case "error"->L10n.s(R.string.msg_4ec4a7927c);
  default->L10n.s(R.string.msg_aa409ed202);
 };}
 void notifyStatus(String text){
  Intent open=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,TunnelService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification.Builder n=new Notification.Builder(this,"vpn").setSmallIcon(R.drawable.ic_vpn).setColor(0xff39aaf8).setContentTitle(L10n.s(R.string.app_name)).setContentText(text).setContentIntent(pi).setSound(null).setOngoing(true).setOnlyAlertOnce(true).addAction(new Notification.Action.Builder(null,L10n.s(R.string.msg_94d07a2171),stop).build());
  VpnRecovery direct=directRecovery();
  // System UI timers outlive the app process on some Android builds. Keep the live timer on the home screen only.
  if(direct!=null)n.setContentTitle(L10n.s(R.string.connection_temporary_unavailable)).setContentText(L10n.s(R.string.connection_direct)).setShowWhen(false);
  startForeground(1,n.build());
 }
 void scheduleNetwork(){if(!networkQueued.compareAndSet(false,true))return;try{worker.execute(()->{networkQueued.set(false);if(!wanted)return;try{Network previous=underlying;updateNetwork();if(box!=null){if(coreIpv6!=effectiveIpv6()){EventLog.add(this,L10n.s(R.string.msg_683f4493ca));startCore();return;}String selected=Store.get(this).selected(network);if(!Objects.equals(previous,underlying)||!selected.equals(primary)){epoch++;cancelDelays();pings.clear();primary=selected;deadChecks=0;recovery=null;if(groups!=null)groups.ensure(selected);box.mobileSelect(selected);server=selected;health=L10n.s(R.string.msg_5f496335d3);lastPing=-1;failover.reset();checkingHealth=false;lastHealth=0;status=underlying==null?"waiting":"connecting";error="";box.resetNetwork();lastTime=0;EventLog.add(this,L10n.s(R.string.msg_b3be92a610)+(network.equals("wifi")?"Wi-Fi":L10n.s(R.string.msg_9dfa170d49)));}if(underlying!=null){if(status.equals("waiting")){status="connecting";lastHealth=0;}notifyStatus(currentStatusText());}else{status="waiting";health=L10n.s(R.string.msg_f0a50c59fc);notifyStatus(L10n.s(R.string.msg_a6ee13c1ee));}}}catch(Exception e){fail(e);}});}catch(RejectedExecutionException ignored){networkQueued.set(false);}}
 void updateNetwork() throws Exception {Network best=null;int bestScore=-1;for(Network n:cm.getAllNetworks()){NetworkCapabilities c=cm.getNetworkCapabilities(n);if(c==null||!c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)||c.hasTransport(NetworkCapabilities.TRANSPORT_VPN))continue;int score=(c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)?100:0)+(c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?20:10);if(score>bestScore){bestScore=score;best=n;}}Network previous=underlying;underlying=best;if(best!=null){NetworkCapabilities c=cm.getNetworkCapabilities(best);network=c!=null&&c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?"cellular":"wifi";}if(!Objects.equals(previous,best))setUnderlyingNetworks(best==null?null:new Network[]{best});updateListener();}
 // Only advertise IPv6 to clients when the underlying network can route it.
 // Otherwise gVisor accepts the TCP handshake before the direct IPv6 dial fails,
 // preventing Chromium's Happy Eyeballs from falling back to IPv4.
 boolean nativeIpv6(){return supportsIpv6(underlying==null?null:cm.getLinkProperties(underlying));}
 boolean effectiveIpv6(){return Store.get(this).data.optBoolean("ipv6",true)&&nativeIpv6();}
 static boolean supportsIpv6(LinkProperties p){
  if(p==null)return false;boolean address=false,route=false;
  for(LinkAddress a:p.getLinkAddresses())if(a.getAddress() instanceof Inet6Address&&!a.getAddress().isLinkLocalAddress()&&!a.getAddress().isLoopbackAddress()&&!a.getAddress().isAnyLocalAddress())address=true;
  for(RouteInfo r:p.getRoutes())if(r.isDefaultRoute()&&r.getDestination().getAddress() instanceof Inet6Address)route=true;
  return address&&route;
 }
 String label(){if(server.equals("direct"))return (network.equals("wifi")?"Wi-Fi":L10n.s(R.string.msg_6679cdceb0))+" · "+L10n.s(R.string.msg_8db805fb1a);JSONObject n=Store.find(Store.get(this).data.optJSONArray("servers"),server);return (network.equals("wifi")?"Wi-Fi":L10n.s(R.string.msg_6679cdceb0))+" · "+(n==null?"VPN":n.optString("name"));}
 synchronized void startCore(){if(!wanted)return;connectionDeadline.arm(20000,this::connectionTimedOut);status="connecting";error="";notifyStatus(L10n.s(R.string.msg_40b27edf9a));try{closeCore();JSONObject d=Store.get(this).snapshot();String selected=d.optString(d.optBoolean("auto",true)?network:d.optString("manual","wifi"));d.put("networkIpv6",nativeIpv6());d.put("gvisorSupported",Libbox.mobileGvisorSupported());coreIpv6=d.optBoolean("ipv6",true)&&d.getBoolean("networkIpv6");ports.clear();JSONArray ns=d.getJSONArray("servers");List<ServerSocket> reserved=new ArrayList<>();try{for(int i=0;i<ns.length();i++){JSONObject n=ns.getJSONObject(i);if(Arrays.asList("xhttp","xray","xray-group").contains(n.getJSONObject("outbound").getString("type"))){ServerSocket ss=new ServerSocket(0,0,InetAddress.getLoopbackAddress());reserved.add(ss);ports.put(n.getString("id"),ss.getLocalPort());}}}finally{for(ServerSocket ss:reserved)ss.close();}Configuration.Result config=Configuration.build(d,network,ports,password);
  SetupOptions options=new SetupOptions();options.setBasePath(getFilesDir().getPath());options.setWorkingPath(getFilesDir().getPath());options.setTempPath(getCacheDir().getPath());options.setFixAndroidStack(true);Libbox.setup(options);CoreRuntime.configure(this);Libbox.checkConfig(config.singbox);
  if(config.xray!=null){File f=new File(getFilesDir(),"xray.json");RuntimeSecrets.write(f,config.xray);ProcessBuilder process=new ProcessBuilder(getApplicationInfo().nativeLibraryDir+"/libxray.so","run","-config",f.getPath()).redirectErrorStream(true).redirectOutput(new File("/dev/null"));socketProtector.apply(process);xray=process.start();Thread.sleep(300);if(!xray.isAlive())throw new Exception(L10n.s(R.string.msg_87fe71dbf1));}
  if(!wanted)throw new InterruptedException("Connection cancelled");groups=new XrayProcesses(this,config.groups);groups.ensure(selected);JSONArray routeRules=d.optJSONArray("rules");for(int ri=0;routeRules!=null&&ri<routeRules.length();ri++){JSONObject r=routeRules.getJSONObject(ri);if(r.optBoolean("enabled",true))groups.ensure(r.optString("target").replace("server:",""));}groups.ensure(d.optString("defaultTarget").replace("server:",""));
  if(!wanted)throw new InterruptedException("Connection cancelled");box=Libbox.newService(config.singbox,this);if(!wanted){box.mobileCancel();throw new InterruptedException("Connection cancelled");}box.start();if(!wanted)throw new InterruptedException("Connection cancelled");/* Native selector restores cache before its configured default: enforce the app selection. */box.mobileSelect(selected);server=selected;primary=server;failover.reset();lastHealth=0;health=L10n.s(R.string.msg_5f496335d3);lastPing=-1;retryAt=0;deadChecks=0;recovery=null;EventLog.add(this,L10n.s(R.string.msg_b84eb59edf));status=underlying==null?"waiting":"connecting";lastTime=0;notifyStatus(underlying==null?L10n.s(R.string.msg_a6ee13c1ee):L10n.s(R.string.msg_aa409ed202));
  // The engine startup deadline is separate from the two bounded health checks.
  String backup=d.optString(Preferences.fallbackKey(d.optBoolean("auto",true)?network:d.optString("manual","wifi")));
  if(wanted&&underlying!=null&&!backup.equals(selected)&&find(ns,backup)!=null)connectionDeadline.arm(20000,this::connectionTimedOut);
 }catch(Exception e){if(wanted)fail(e);else closeCore();}}
 int connectionPing(BoxService current,XrayProcesses processes,String id)throws Exception{return connectionPing(current,processes,id,id);}
 int connectionPing(BoxService current,XrayProcesses processes,String id,String route)throws Exception{
  java.util.function.Function<String,Integer> fixture=testHealthReply;
  if(fixture!=null&&(getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0){Integer reply=fixture.apply(id);if(reply!=null)return reply;}
  if(processes!=null)processes.ensure(id);
  if(processes==null||!processes.configs.containsKey(id))return current.mobileDelay(route);
  long until=SystemClock.elapsedRealtime()+8000;Exception last=null;
  while(SystemClock.elapsedRealtime()<until){try{int ping=current.mobileDelayFor(route,(int)Math.min(6000,Math.max(1,until-SystemClock.elapsedRealtime())));if(ping>=0)return ping;}catch(Exception e){last=e;}Thread.sleep(150);}
  throw last==null?new IOException("Connection check failed"):last;
 }
 void connectionResult(int ping){
  connectionDeadline.clear();lastPing=ping;pings.put(server,ping);
  String next=underlying==null?"waiting":ping>=0?"connected":"error";
  String previous=status;status=next;
  error=next.equals("error")?L10n.s(R.string.connection_unavailable):"";
  health=ping>=0?L10n.s(R.string.msg_e66c7d3fa3):L10n.s(R.string.msg_a2ca43d182);
  if(!previous.equals(next))notifyStatus(next.equals("connected")?label():next.equals("waiting")?L10n.s(R.string.msg_a6ee13c1ee):L10n.s(R.string.msg_4ec4a7927c));
 }
 /** Switch the live selector to "direct" without touching the TUN; DNS follows via the vezdehod-selector route. */
 void failOpen(BoxService current){
  try{if(groups!=null)groups.ensure("direct");current.mobileSelect("direct");server="direct";recovery=VpnRecovery.waitFrom(SystemClock.elapsedRealtime());error="";health=L10n.s(R.string.msg_ecd3610b6d);status="connected";EventLog.add(this,L10n.s(R.string.msg_6268037bd9));notifyStatus(label());}
  catch(Exception e){fail(e);}
 }
 synchronized void closeCore(){
  epoch++;checkingHealth=false;cancelDelays();pings.clear();recovery=null;deadChecks=0;connectedScope=null;
  long before=SystemClock.elapsedRealtime();if(box!=null){try{box.close();}catch(Exception ignored){}box=null;}
  long nativeDone=SystemClock.elapsedRealtime();closeTun();
  if(xray!=null){xray.destroy();try{if(!xray.waitFor(2,TimeUnit.SECONDS))xray.destroyForcibly();}catch(Exception ignored){}xray=null;}
  if(groups!=null){groups.close();groups=null;}
  if(isStopping())android.util.Log.i(STOP_LOG,"core="+(nativeDone-before)+" ms, processes="+(SystemClock.elapsedRealtime()-nativeDone)+" ms, tun handles="+tunHandleCount());
  new File(getFilesDir(),"xray.json").delete();down=up=0;totalDown=totalUp=0;server="";lastPing=-1;
 }
 private static int tunHandleCount(){
  File dir=new File("/proc/self/fd");String[] names=dir.list();if(names==null)return -1;int handles=0;
  for(String name:names)try{String path=android.system.Os.readlink(new File(dir,name).getPath());if(path.equals("/dev/tun")||path.equals("/dev/net/tun"))handles++;}catch(android.system.ErrnoException ignored){}
  return handles;
 }
 void fail(Exception e){connectionDeadline.clear();error=safeError(e);status="error";closeCore();health=L10n.s(R.string.msg_8d41645d5d);retryAt=SystemClock.elapsedRealtime()+Store.get(this).data.optInt("retrySeconds",15)*1000L;EventLog.add(this,L10n.s(R.string.msg_15da4eee64));notifyStatus(L10n.s(R.string.msg_4ec4a7927c));}
 public static String safeError(Exception e){String m=e.getMessage();if(m==null)m=e.getClass().getSimpleName();return m.replaceAll("(?i)(vless|vmess|trojan|ss|https?)://[^\\s]+",L10n.s(R.string.msg_a24fd4542e)).replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F-]{27,}",L10n.s(R.string.msg_1c64a8bede));}
 void closeTun(){synchronized(tunLock){if(tun!=null){try{tun.close();}catch(Exception ignored){}tun=null;}}}
 void abortCore(){closeTun();BoxService current=box;if(current!=null)try{aborter.execute(()->{try{current.mobileCancel();}catch(Exception ignored){}});}catch(RejectedExecutionException ignored){}}
 void connectionTimedOut(){if(!wanted||!status.equals("connecting"))return;wanted=false;epoch++;error=L10n.s(R.string.connection_timeout);status="error";abortCore();notifyStatus(L10n.s(R.string.msg_4ec4a7927c));try{worker.execute(this::closeCore);}catch(RejectedExecutionException ignored){}}
 void requestStop(){if(!stopping.compareAndSet(false,true))return;sessionPreferences().edit().putBoolean("requested",false).apply();stopStarted=SystemClock.elapsedRealtime();android.util.Log.i(STOP_LOG,"stop requested, tun handles="+tunHandleCount());wanted=false;epoch++;connectionDeadline.clear();status="stopping";error="";abortCore();try{worker.execute(this::shutdown);}catch(RejectedExecutionException ignored){}}
 void shutdown(){
  android.util.Log.i(STOP_LOG,"worker entered after "+(SystemClock.elapsedRealtime()-stopStarted)+" ms");wanted=false;connectionDeadline.clear();
  EventLog.add(this,L10n.s(R.string.msg_c0b5a5c3bd));health=L10n.s(R.string.msg_5f496335d3);if(refresh!=null)refresh.cancel(false);
  if(registered){cm.unregisterNetworkCallback(callback);registered=false;}
  closeCore();shutdownComplete=true;
  Network stale=null;
  try{if(!isAlwaysOn()&&!isLockdownEnabled())for(Network n:cm.getAllNetworks())if(hasOwnTunnel(n)){stale=n;break;}}
  catch(RuntimeException e){android.util.Log.w(STOP_LOG,"VPN state unavailable: "+e.getClass().getSimpleName());}
  if(stale==null){finishShutdown(null);return;}
  Network lingering=stale;worker.schedule(()->finishShutdown(lingering),350,TimeUnit.MILLISECONDS);
 }
 private boolean hasOwnTunnel(Network n){
  NetworkCapabilities capabilities=cm.getNetworkCapabilities(n);
  if(capabilities==null||!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN))return false;
  int owner=capabilities.getOwnerUid();if(owner!=-1&&owner!=android.os.Process.myUid())return false;
  LinkProperties links=cm.getLinkProperties(n);if(links==null)return false;
  String ifName=links.getInterfaceName();if(ifName==null)return false;
  // ConnectivityManager can briefly report a VPN that the kernel has already brought down.
  try{java.net.NetworkInterface iface=java.net.NetworkInterface.getByName(ifName);if(iface==null||!iface.isUp())return false;}catch(SocketException ignored){}
  for(LinkAddress address:links.getLinkAddresses())if("172.19.0.1".equals(address.getAddress().getHostAddress()))return true;
  return false;
 }
 void finishShutdown(Network lingering){
  try{
   if(lingering!=null&&!isAlwaysOn()&&!isLockdownEnabled()&&hasOwnTunnel(lingering)&&prepare(this)==null){
    // Public VpnService handover resets the old system interface. The replacement has no Internet routes or VPN DNS.
    try(ParcelFileDescriptor empty=new Builder().setSession(L10n.s(R.string.app_name)).addAddress("192.0.2.1",32)
      .allowFamily(android.system.OsConstants.AF_INET6).addAllowedApplication(getPackageName()).establish()){
     if(empty!=null)android.util.Log.i(STOP_LOG,"stale interface reset through VPN handover");
    }
   }
  }catch(Exception e){android.util.Log.w(STOP_LOG,"VPN handover unavailable: "+e.getClass().getSimpleName());}
  finally{stopForeground(STOP_FOREGROUND_REMOVE);android.util.Log.i(STOP_LOG,"stopSelf after "+(SystemClock.elapsedRealtime()-stopStarted)+" ms");stopSelf();}
 }
 @Override public void onRevoke(){requestStop();}
 @Override public void onDestroy(){connectionDeadline.close();if(socketProtector!=null)socketProtector.close();wanted=false;abortCore();aborter.shutdown();if(registered){cm.unregisterNetworkCallback(callback);registered=false;}if(!shutdownComplete)worker.execute(this::closeCore);worker.shutdown();healthChecks.shutdownNow();tests.shutdownNow();pingWorker.shutdownNow();cancelDelays();error="";status="off";instance=null;if(stopStarted!=0)android.util.Log.i(STOP_LOG,"destroyed after "+(SystemClock.elapsedRealtime()-stopStarted)+" ms");super.onDestroy();}
 synchronized void stats(){if(box==null)return;try{JSONObject s=new JSONObject(box.mobileStats());long now=SystemClock.elapsedRealtime(),u=s.optLong("up"),d=s.optLong("down");if(lastTime>0){up=Math.max(0,u-lastUp)*.008/(now-lastTime);down=Math.max(0,d-lastDown)*.008/(now-lastTime);}lastTime=now;lastUp=u;lastDown=d;totalUp=u;totalDown=d;if(groups!=null)groups.check();if(xray!=null&&!xray.isAlive())throw new Exception(L10n.s(R.string.msg_28fc95b5ac));}catch(Exception e){fail(e);}}
 private long checked=0;void autoRefresh(){long now=System.currentTimeMillis();if(now-checked<60000)return;checked=now;try{JSONArray p=Store.get(this).snapshot().getJSONArray("profiles");for(int i=0;i<p.length();i++){JSONObject x=p.getJSONObject(i);String id=x.getString("id");if(!x.optBoolean("auto",true)||!x.optString("type").equals("subscription")||now-x.optLong("updated")<=3600000||now-refreshAttempt.getOrDefault(id,0L)<300000||!refreshing.add(id))continue;refreshAttempt.put(id,now);tests.execute(()->{try{JSONObject r=Subscriptions.fetch(x,this);if(Store.get(this).putProfileIfCurrent(x,r.getJSONObject("profile"),r.getJSONArray("servers")))reload();}catch(Exception ignored){}finally{refreshing.remove(id);}});}}catch(Exception ignored){}}
 void maintenance(){
   if(!wanted)return;long now=SystemClock.elapsedRealtime();JSONObject data=Store.get(this).data;
   if(box==null){if(underlying!=null&&data.optBoolean("reconnect",true)&&now>=retryAt){retryAt=now+data.optInt("retrySeconds",15)*1000L;EventLog.add(this,L10n.s(R.string.msg_9b36be9334));startCore();}return;}
   /* The system app list is fixed while the TUN lives: when Wi-Fi <-> cellular flips the plan may change, so rebuild the tunnel. */
   if(connectedScope!=null&&!connectedScope.equals(Configuration.plan(data,network))){EventLog.add(this,L10n.s(R.string.msg_c28130e4b1));startCore();return;}
   String net=data.optBoolean("auto",true)?network:data.optString("manual","wifi");String reserve=data.optString(Preferences.fallbackKey(net));
   final boolean hasReserve=!reserve.isEmpty()&&!reserve.equals(primary)&&find(data.optJSONArray("servers"),reserve)!=null;final boolean canFailOpen=data.optBoolean("failOpen",true);
   if(underlying==null||checkingHealth)return;
   final boolean recovering=recovery!=null;
   if(recovering){if(!recovery.isDue(now))return;recovery=VpnRecovery.CHECKING;notifyStatus(currentStatusText());}
   else{long interval=status.equals("error")||status.equals("retrying")?10000:30000;if(lastHealth!=0&&now-lastHealth<interval)return;}
   lastHealth=now;checkingHealth=true;final BoxService current=box;final XrayProcesses processes=groups;final long token=epoch;
   final String activeServer=server,mainServer=primary;final boolean usingReserve=!activeServer.equals(mainServer)&&!activeServer.equals("direct"),connecting=status.equals("connecting");
   healthChecks.execute(()->{
    int measured=-1,alternative=-1;
    if(recovering){
      /* Try the primary first, then the reserve; either can restore VPN routing. */
      try{measured=connectionPing(current,processes,mainServer);}catch(Exception e){NetworkErrors.record("health",mainServer,e.toString());}
    }else{
     try{measured=connectionPing(current,processes,activeServer,"auto");}catch(Exception e){NetworkErrors.record("health",activeServer,e.toString());}
     }
     String other=usingReserve?mainServer:reserve;
     if(hasReserve&&(usingReserve||measured<0))try{alternative=connectionPing(current,processes,other);}catch(Exception e){NetworkErrors.record("health-reserve",other,e.toString());}
    final int ping=measured,candidate=alternative;
    try{worker.execute(()->{
     if(!wanted||epoch!=token||box!=current)return;checkingHealth=false;
     pings.put(recovering?mainServer:activeServer,ping);
     try{
      if(recovering){
        FailoverPolicy.Decision choice=FailoverPolicy.recoverFromDirect(ping>=0,candidate>=0);
        if(choice!=FailoverPolicy.Decision.KEEP){
         boolean onReserve=choice==FailoverPolicy.Decision.RESERVE;String selected=onReserve?reserve:mainServer;
         if(groups!=null)groups.ensure(selected);current.mobileSelect(selected);server=selected;recovery=null;deadChecks=0;failover.reset();
         connectionResult(onReserve?candidate:ping);EventLog.add(this,L10n.s(onReserve?R.string.msg_af26aede0a:R.string.msg_b4f1b04c34));notifyStatus(label());
        }
       else{recovery=VpnRecovery.waitFrom(SystemClock.elapsedRealtime());notifyStatus(currentStatusText());}
       return;
      }
      int effective=ping;
       if(hasReserve){FailoverPolicy.Decision decision=failover.observe(usingReserve,ping>=0,candidate>=0,connecting);
       if(decision!=FailoverPolicy.Decision.KEEP){String selected=decision==FailoverPolicy.Decision.PRIMARY?mainServer:reserve;
        if(groups!=null)groups.ensure(selected);current.mobileSelect(selected);server=selected;epoch++;cancelDelays();effective=candidate;deadChecks=0;
        EventLog.add(this,decision==FailoverPolicy.Decision.PRIMARY?L10n.s(R.string.msg_ec7a0b7421):L10n.s(R.string.msg_af26aede0a));
       }
      }
      /* Two consecutive dead checks with no working reserve: fall open to direct instead of dropping the phone's connectivity. */
      if(canFailOpen&&ping<0&&candidate<0){
       if(++deadChecks>=2){failOpen(current);return;}
        retryHealth();return;
       }
       deadChecks=0;
       if(effective<0&&candidate>=0){retryHealth();return;}
       connectionResult(effective);
     }catch(Exception e){fail(e);}
    });}catch(RejectedExecutionException ignored){}
   });
  }
 void retryHealth(){connectionDeadline.clear();lastPing=-1;health=L10n.s(R.string.connection_retry_pending);error="";status="retrying";notifyStatus(currentStatusText());}
 public void probe(TestResult callback){probeServer(server,callback);}
 public void probeServer(String id,TestResult callback){tests.execute(()->{try{BoxService current=box;long token=epoch;if(current==null)throw new Exception(L10n.s(R.string.msg_bc15101b19));if(groups!=null)groups.ensure(id);JSONObject r=new JSONObject(current.mobileProbe(id));if(epoch!=token)throw new Exception(L10n.s(R.string.msg_f96dbc41fe));if(id.equals(server)){lastPing=r.optInt("ping",-1);health=L10n.s(R.string.msg_e66c7d3fa3);}EventLog.add(this,L10n.s(R.string.msg_d2d78125b3));callback.done(r,null);}catch(Exception e){if(id.equals(server))health=L10n.s(R.string.msg_80f6d88155);EventLog.add(this,L10n.s(R.string.msg_c050b742cc));callback.done(null,safeError(e));}});}
 public interface TestResult{void done(JSONObject value,String err);}
 void scanPings(){if(box==null||underlying==null||!status.equals("connected")||scannedEpoch==epoch)return;scannedEpoch=epoch;JSONArray nodes=Store.get(this).data.optJSONArray("servers");for(int i=0;i<nodes.length();i++){String id=nodes.optJSONObject(i).optString("id");if(!id.equals(server))delay(id,(v,e)->{});}}
 public void delay(String id,TestResult callback){final BoxService current=box;final long token=epoch;final String key=token+":"+id;
  if(current==null){callback.done(null,L10n.s(R.string.msg_bc15101b19));return;}
  synchronized(pendingPings){List<TestResult> pending=pendingPings.get(key);if(pending!=null){pending.add(callback);return;}pending=new ArrayList<>();pending.add(callback);pendingPings.put(key,pending);}
  try{pingWorker.execute(()->{int result=-1;String error=null;try{if(current!=box||token!=epoch)throw new Exception(L10n.s(R.string.msg_d14be38b43));if(groups!=null)groups.ensure(id);result=current.mobileDelay(id);}catch(Exception e){error=safeError(e);}if(token==epoch&&current==box&&current!=null){pings.put(id,result);if(id.equals(server))lastPing=result;}else error=L10n.s(R.string.msg_d14be38b43);JSONObject value=null;try{if(error==null)value=obj("ping",result);}catch(Exception ignored){}finishDelay(key,value,error);});}catch(RejectedExecutionException e){finishDelay(key,null,L10n.s(R.string.msg_c0b5a5c3bd));}
 }
 void finishDelay(String key,JSONObject value,String error){List<TestResult> callbacks;synchronized(pendingPings){callbacks=pendingPings.remove(key);}if(callbacks!=null)for(TestResult callback:callbacks)try{callback.done(value,error);}catch(Exception ignored){}}
 void cancelDelays(){List<String> keys;synchronized(pendingPings){keys=new ArrayList<>(pendingPings.keySet());}for(String key:keys)finishDelay(key,null,L10n.s(R.string.msg_d14be38b43));}
 public void speed(TestResult callback){tests.execute(()->{try{BoxService current=box;if(current==null)throw new Exception(L10n.s(R.string.msg_bc15101b19));long token=epoch;String id=server;String r=current.mobileSpeed(id);if(token!=epoch||!id.equals(server))throw new Exception(L10n.s(R.string.msg_044b7bf56c));callback.done(new JSONObject(r),null);}catch(Exception e){callback.done(null,safeError(e));}});}
 @Override public int openTun(TunOptions o)throws Exception{if(prepare(this)!=null)throw new Exception(L10n.s(R.string.msg_cda6f16c5c));Builder b=new Builder().setSession(L10n.s(R.string.app_name)).setMtu(o.getMTU()).setMetered(network.equals("cellular"));RoutePrefixIterator a=o.getInet4Address();while(a.hasNext()){RoutePrefix p=a.next();b.addAddress(p.address(),p.prefix());}a=o.getInet6Address();while(a.hasNext()){RoutePrefix p=a.next();b.addAddress(p.address(),p.prefix());}b.addRoute("0.0.0.0",0).addRoute("::",0).addDnsServer("172.19.0.2");applyScope(b);b.setConfigureIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE));synchronized(tunLock){if(!wanted)throw new IOException("Connection cancelled");tun=b.establish();if(tun==null)throw new Exception(L10n.s(R.string.msg_a57d71f512));return tun.getFd();}}
  /* System-level split tunnelling: the OS routes these apps around the TUN, so they see the plain Wi-Fi/cellular network instead of a VPN. */
 void applyScope(Builder b)throws Exception{JSONObject d=Store.get(this).data;connectedScope=d.optBoolean("appScope",Preferences.DEFAULT_APP_SCOPE)?Configuration.plan(d,network):null;if(connectedScope==null)return;PackageManager pm=getPackageManager();Set<String> installed=new HashSet<>();for(ApplicationInfo ai:pm.getInstalledApplications(0))installed.add(ai.packageName);int missing=0;for(String pkg:connectedScope.packages){if(installed.contains(pkg)){if(connectedScope.allow)b.addAllowedApplication(pkg);else b.addDisallowedApplication(pkg);}else missing++;}if(missing>0)EventLog.add(this,L10n.s(R.string.msg_dd22010599)+missing);}
 @Override public boolean usePlatformAutoDetectInterfaceControl(){return true;}@Override public void autoDetectInterfaceControl(int fd)throws Exception{Network captured=underlying;if(captured==null)throw new IOException("Physical network unavailable");if(!protect(fd))throw new Exception(L10n.s(R.string.msg_f132898dfe));try(ParcelFileDescriptor duplicate=ParcelFileDescriptor.fromFd(fd)){captured.bindSocket(duplicate.getFileDescriptor());}}
 @Override public boolean useProcFS(){return false;}
 @Override public int findConnectionOwner(int proto,String src,int sp,String dst,int dp)throws Exception{return cm.getConnectionOwnerUid(proto,new InetSocketAddress(InetAddress.getByName(src),sp),new InetSocketAddress(InetAddress.getByName(dst),dp));}
 @Override public String packageNameByUid(int uid){String[] p=getPackageManager().getPackagesForUid(uid);return p==null||p.length==0?"":p[0];}
 @Override public int uidByPackageName(String name)throws Exception{return getPackageManager().getApplicationInfo(name,0).uid;}
 @Override public void startDefaultInterfaceMonitor(InterfaceUpdateListener l)throws Exception{listener=l;updateListener();}@Override public void closeDefaultInterfaceMonitor(InterfaceUpdateListener l){listener=null;}
 void updateListener()throws Exception{InterfaceUpdateListener l=listener;if(l==null)return;LinkProperties p=underlying==null?null:cm.getLinkProperties(underlying);String name=p==null?"":p.getInterfaceName();java.net.NetworkInterface ni=name==null||name.isEmpty()?null:java.net.NetworkInterface.getByName(name);l.updateDefaultInterface(name==null?"":name,ni==null?-1:ni.getIndex(),network.equals("cellular"),false);}
 static StringIterator strings(List<String> list){return new StringIterator(){Iterator<String> it=list.iterator();public boolean hasNext(){return it.hasNext();}public String next(){return it.next();}public int len(){return list.size();}};}
 @Override public NetworkInterfaceIterator getInterfaces()throws Exception {List<io.nekohasekai.libbox.NetworkInterface> list=new ArrayList<>();for(Network n:cm.getAllNetworks()){NetworkCapabilities c=cm.getNetworkCapabilities(n);LinkProperties lp=cm.getLinkProperties(n);if(c==null||lp==null||c.hasTransport(NetworkCapabilities.TRANSPORT_VPN))continue;java.net.NetworkInterface ni=java.net.NetworkInterface.getByName(lp.getInterfaceName());if(ni==null)continue;io.nekohasekai.libbox.NetworkInterface x=new io.nekohasekai.libbox.NetworkInterface();x.setName(ni.getName());x.setIndex(ni.getIndex());x.setMTU(ni.getMTU());x.setFlags(1|64|(ni.isLoopback()?8:0)|(ni.isPointToPoint()?16:0)|(ni.supportsMulticast()?4096:0));x.setType(c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?Libbox.InterfaceTypeWIFI:c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?Libbox.InterfaceTypeCellular:Libbox.InterfaceTypeEthernet);x.setMetered(!c.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED));List<String> ips=new ArrayList<>(),dns=new ArrayList<>();for(LinkAddress a:lp.getLinkAddresses())ips.add(a.toString());for(InetAddress a:lp.getDnsServers())dns.add(a.getHostAddress());x.setAddresses(strings(ips));x.setDNSServer(strings(dns));list.add(x);}return new NetworkInterfaceIterator(){Iterator<io.nekohasekai.libbox.NetworkInterface> it=list.iterator();public boolean hasNext(){return it.hasNext();}public io.nekohasekai.libbox.NetworkInterface next(){return it.next();}};}
 @Override public LocalDNSTransport localDNSTransport(){return new PlatformDns(()->underlying);}@Override public void writeLog(String text){NetworkErrors.record("core",server,text);}@Override public boolean underNetworkExtension(){return false;}@Override public boolean includeAllNetworks(){return false;}@Override public WIFIState readWIFIState(){return Libbox.newWIFIState("","");}@Override public StringIterator systemCertificates(){return strings(Collections.emptyList());}@Override public void clearDNSCache(){}@Override public void sendNotification(io.nekohasekai.libbox.Notification n){}
}
