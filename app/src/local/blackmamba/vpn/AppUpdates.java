package local.blackmamba.vpn;
import android.app.*;import android.app.job.*;import android.content.*;import android.content.pm.*;import android.database.Cursor;import android.net.Uri;import android.os.*;import java.io.*;import java.net.*;import java.security.MessageDigest;import java.util.*;import java.util.concurrent.*;import org.json.*;

final class AppUpdates {
 static final int JOB=7415;static final long DAY=24*60*60*1000L;
 static final ExecutorService IO=Executors.newSingleThreadExecutor();static volatile boolean checking,verifying;static volatile String failure="";
 interface Done{void done(String error);}
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("app_updates",Context.MODE_PRIVATE);}
 static String repository(Context c){return c.getString(R.string.update_repository).trim();}
 static long installed(Context c)throws Exception{return c.getPackageManager().getPackageInfo(c.getPackageName(),0).getLongVersionCode();}
 static String version(Context c){try{return c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName;}catch(Exception e){return "—";}}
 static UpdateRelease available(Context c){try{UpdateRelease r=new UpdateRelease(new JSONObject(prefs(c).getString("release","{}")));return r.code>installed(c)&&r.supports(Build.SUPPORTED_ABIS)&&UpdateRelease.trustedUrl(r.url,repository(c))?r:null;}catch(Exception e){return null;}}
 static void cleanInstalled(Context c){if(prefs(c).getBoolean("downloadAuto",false))cancelDownload(c);if(prefs(c).contains("channel")){prefs(c).edit().remove("release").remove("lastCheck").remove("channel").remove("download").apply();}try{long code=prefs(c).getLong("downloadCode",0);if(code>0&&code<=installed(c))cancelDownload(c);}catch(Exception ignored){}}
 static void schedule(Context c){
  JobScheduler jobs=c.getSystemService(JobScheduler.class);if(!prefs(c).getBoolean("auto",true)||!UpdateRelease.repositoryValid(repository(c))){jobs.cancel(JOB);return;}
  if(jobs.getPendingJob(JOB)!=null)return;
  jobs.schedule(new JobInfo.Builder(JOB,new ComponentName(c,UpdateJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setPeriodic(DAY,4*60*60*1000L).build());
 }
 static void configure(Context c,String key,Object value){
  if(!key.equals("auto")||!(value instanceof Boolean))throw new IllegalArgumentException("Unknown update setting");
  prefs(c).edit().putBoolean("auto",(Boolean)value).apply();
  failure="";schedule(c);
 }
 static void maybeCheck(Context c){schedule(c);if(prefs(c).getBoolean("auto",true)&&UpdateRelease.repositoryValid(repository(c))&&System.currentTimeMillis()-prefs(c).getLong("lastAttempt",0)>=DAY)check(c,false,null);}
 static synchronized void check(Context context,boolean manual,Done callback){
  Context c=context.getApplicationContext();if(checking){if(callback!=null)callback.done(null);return;}
  if(!manual&&!prefs(c).getBoolean("auto",true)){if(callback!=null)callback.done(null);return;}
  if(!UpdateRelease.repositoryValid(repository(c))){failure=L10n.s(R.string.update_not_configured);if(callback!=null)callback.done(failure);return;}
  checking=true;failure="";prefs(c).edit().putLong("lastAttempt",System.currentTimeMillis()).apply();
  IO.execute(()->{String error=null;try{
   HttpURLConnection connection=(HttpURLConnection)new URL("https://api.github.com/repos/"+repository(c)+"/releases?per_page=100").openConnection();connection.setConnectTimeout(12000);connection.setReadTimeout(12000);connection.setInstanceFollowRedirects(false);connection.setRequestProperty("Accept","application/vnd.github+json");connection.setRequestProperty("User-Agent","Vezdehod/"+version(c));
   String body;try{int status=connection.getResponseCode();if(status!=200)throw new IOException("HTTP "+status);try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("Response too large");out.write(buffer,0,n);}body=out.toString("UTF-8");}}finally{connection.disconnect();}
   UpdateRelease release=UpdateRelease.select(new JSONArray(body),repository(c),false,installed(c),Build.SUPPORTED_ABIS);
   if(manual||prefs(c).getBoolean("auto",true)){SharedPreferences.Editor e=prefs(c).edit().putLong("lastCheck",System.currentTimeMillis());if(release==null)e.remove("release");else e.putString("release",release.json().toString());e.apply();}
  }catch(Exception e){error=L10n.s(R.string.update_check_failed);}finally{failure=error==null?"":error;checking=false;if(callback!=null){String result=error;new Handler(Looper.getMainLooper()).post(()->callback.done(result));}}});
 }
 static File target(Context c,UpdateRelease r)throws IOException{File dir=c.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);if(dir==null)throw new IOException("Download storage unavailable");return new File(dir,r.name);}
 static synchronized void download(Context c,UpdateRelease release)throws Exception{
  if(!UpdateRelease.trustedUrl(release.url,repository(c)))throw new IOException("Invalid asset origin");
  if(prefs(c).getLong("downloadCode",0)==release.code&&downloadState(c).status!=DownloadManager.STATUS_FAILED&&prefs(c).getLong("downloadId",-1)>=0)return;
  cancelDownload(c);File file=target(c,release);if(file.exists()&&!file.delete())throw new IOException("Cannot replace download");
  DownloadManager.Request request=new DownloadManager.Request(Uri.parse(release.url)).setTitle("Vezdehod "+release.version).setMimeType("application/vnd.android.package-archive").setDestinationUri(Uri.fromFile(file)).setAllowedOverRoaming(false).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
  long id=c.getSystemService(DownloadManager.class).enqueue(request);prefs(c).edit().putLong("downloadId",id).putLong("downloadCode",release.code).putString("downloadRelease",release.json().toString()).apply();failure="";
 }
 static void cancelDownload(Context c){long id=prefs(c).getLong("downloadId",-1);if(id>=0)c.getSystemService(DownloadManager.class).remove(id);prefs(c).edit().remove("downloadId").remove("downloadCode").remove("downloadRelease").remove("downloadAuto").apply();new File(c.getFilesDir(),"update-ready.apk").delete();}
 static final class Progress{int status;long bytes,total;}
 static Progress downloadState(Context c){Progress p=new Progress();long id=prefs(c).getLong("downloadId",-1);if(id<0)return p;try(Cursor row=c.getSystemService(DownloadManager.class).query(new DownloadManager.Query().setFilterById(id))){if(row!=null&&row.moveToFirst()){p.status=row.getInt(row.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));p.bytes=row.getLong(row.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));p.total=row.getLong(row.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));}else p.status=DownloadManager.STATUS_FAILED;}return p;}
 static File verify(Context c,File source,UpdateRelease r)throws Exception{
  if(source.length()!=r.size||source.length()>UpdateRelease.MAX_APK)throw new IOException("APK size mismatch");
  File ready=new File(c.getFilesDir(),"update-ready.apk"),part=new File(c.getFilesDir(),"update-ready.part");ready.delete();
  try{
   MessageDigest hash=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(source);FileOutputStream out=new FileOutputStream(part)){byte[] buffer=new byte[32768];long count=0;int n;while((n=in.read(buffer))!=-1){count+=n;if(count>r.size)throw new IOException("APK too large");out.write(buffer,0,n);hash.update(buffer,0,n);}out.getFD().sync();if(count!=r.size)throw new IOException("APK incomplete");}
   StringBuilder digest=new StringBuilder();for(byte b:hash.digest())digest.append(String.format(Locale.ROOT,"%02x",b&255));if(!digest.toString().equals(r.sha256))throw new IOException("APK digest mismatch");
   PackageManager pm=c.getPackageManager();PackageInfo apk=pm.getPackageArchiveInfo(part.getPath(),PackageManager.GET_SIGNING_CERTIFICATES),local=pm.getPackageInfo(c.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
   if(apk==null||!c.getPackageName().equals(apk.packageName)||apk.getLongVersionCode()!=r.code||!r.version.equals(apk.versionName)||r.code<=local.getLongVersionCode()||apk.applicationInfo.minSdkVersion>Build.VERSION.SDK_INT)throw new IOException("APK identity/version mismatch");
   if(apk.signingInfo==null||local.signingInfo==null||!new HashSet<>(Arrays.asList(apk.signingInfo.getApkContentsSigners())).equals(new HashSet<>(Arrays.asList(local.signingInfo.getApkContentsSigners()))))throw new IOException("APK signature mismatch");
   boolean supported=false;try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(part)){for(String abi:Build.SUPPORTED_ABIS)if(zip.getEntry("lib/"+abi+"/libbox.so")!=null&&zip.getEntry("lib/"+abi+"/libxray.so")!=null){supported=true;break;}}if(!supported)throw new IOException("APK architecture mismatch");
   if(!part.renameTo(ready))throw new IOException("Cannot stage APK");return ready;
  }finally{part.delete();}
 }
 static void install(MainActivity a){
  if(verifying)return;
  if(!a.getPackageManager().canRequestPackageInstalls()){a.new DialogBuilder(a).setMessage(L10n.s(R.string.update_permission)).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).setPositiveButton(L10n.s(R.string.msg_4ae50d3073),(d,w)->a.startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+a.getPackageName())))).show();return;}
  verifying=true;failure="";IO.execute(()->{String error=null;try{UpdateRelease release=new UpdateRelease(new JSONObject(prefs(a).getString("downloadRelease","{}")));if(!UpdateRelease.trustedUrl(release.url,repository(a)))throw new IOException("Invalid source");verify(a,target(a,release),release);}catch(Exception e){error=L10n.s(R.string.update_invalid);cancelDownload(a);}String result=error;new Handler(Looper.getMainLooper()).post(()->{verifying=false;if(a.isFinishing()||a.isDestroyed())return;if(result!=null){failure=result;a.message(result);return;}try{Intent install=new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://"+a.getPackageName()+".updates/apk"),"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);a.startActivity(install);}catch(Exception e){failure=L10n.s(R.string.update_install_failed);a.message(failure);}});});
 }
}
