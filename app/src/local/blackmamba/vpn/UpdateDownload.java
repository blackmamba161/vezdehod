package local.blackmamba.vpn;
import java.io.*;import java.net.*;import java.security.MessageDigest;import java.util.Locale;import java.util.concurrent.*;
/** Bounded streaming transfer. It never delegates app-owned traffic to another UID. */
final class UpdateDownload {
 interface Open {HttpURLConnection open(URL url)throws IOException;}
 private static final ScheduledExecutorService CLOCK=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"update-deadline");t.setDaemon(true);return t;});
 private static final ExecutorService ABORT=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"update-abort");t.setDaemon(true);return t;});
 final long expected;final String sha256;volatile long bytes;volatile boolean canceled,timedOut;private volatile HttpURLConnection connection;
 UpdateDownload(long expected,String sha256){this.expected=expected;this.sha256=sha256;}
 void cancel(){canceled=true;HttpURLConnection c=connection;if(c!=null)ABORT.execute(c::disconnect);}
 static boolean allowed(URL url){String h=url.getHost();return "https".equals(url.getProtocol())&&(url.getPort()==-1||url.getPort()==443)&&url.getUserInfo()==null&&url.getRef()==null&&(h.equals("github.com")||h.equals("release-assets.githubusercontent.com")||h.equals("objects.githubusercontent.com")||h.equals("github-releases.githubusercontent.com"));}
 void run(URL initial,File part,Open opener,long deadlineMillis)throws Exception{
  if(expected<=0||expected>UpdateRelease.MAX_APK||!sha256.matches("[a-f0-9]{64}"))throw new IOException("Invalid download metadata");
  ScheduledFuture<?> deadline=CLOCK.schedule(()->{timedOut=true;cancel();},deadlineMillis,TimeUnit.MILLISECONDS);
  boolean complete=false;
  try{
   URL url=initial;
   for(int redirect=0;;redirect++){
    if(canceled)throw new InterruptedIOException("Download cancelled");if(!allowed(url))throw new IOException("Untrusted download redirect");
    HttpURLConnection c=opener.open(url);connection=c;c.setConnectTimeout(12000);c.setReadTimeout(12000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept-Encoding","identity");c.setRequestProperty("User-Agent","Vezdehod-Updater");
    if(canceled)throw new InterruptedIOException("Download cancelled");int status=c.getResponseCode();
    if(status==301||status==302||status==303||status==307||status==308){String location=c.getHeaderField("Location");if(location==null||redirect>=5)throw new IOException("Invalid redirect chain");url=new URL(url,location);c.disconnect();connection=null;continue;}
    if(status!=200)throw new IOException("Download HTTP "+status);long length=c.getContentLengthLong();if(length>=0&&length!=expected)throw new IOException("Download size mismatch");
    MessageDigest digest=MessageDigest.getInstance("SHA-256");
    try(InputStream input=c.getInputStream();FileOutputStream output=new FileOutputStream(part)){
     byte[] buffer=new byte[32768];int n;while((n=input.read(buffer))!=-1){if(canceled)throw new InterruptedIOException("Download cancelled");if(bytes+n>expected)throw new IOException("Download exceeds expected size");output.write(buffer,0,n);digest.update(buffer,0,n);bytes+=n;}output.getFD().sync();
    }
    if(canceled)throw new InterruptedIOException("Download cancelled");if(bytes!=expected)throw new IOException("Incomplete download");StringBuilder hash=new StringBuilder();for(byte value:digest.digest())hash.append(String.format(Locale.ROOT,"%02x",value&255));if(!hash.toString().equals(sha256))throw new IOException("Download checksum mismatch");complete=true;return;
   }
  }finally{deadline.cancel(false);HttpURLConnection c=connection;connection=null;if(c!=null)c.disconnect();if(!complete)part.delete();}
 }
}
