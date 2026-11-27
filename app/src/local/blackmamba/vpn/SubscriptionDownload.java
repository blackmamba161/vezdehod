package local.blackmamba.vpn;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

final class SubscriptionDownload {
 interface Open {HttpURLConnection connection(URL url) throws IOException;}
 final String body,usage,title;
 private SubscriptionDownload(String body,String usage,String title){this.body=body;this.usage=usage;this.title=title;}

 static SubscriptionDownload fetch(URL url,String userAgent,File noBackupDir,Open open) throws Exception {
  String initialOrigin=SubscriptionDeviceId.origin(url);
  String hwid=noBackupDir==null?null:SubscriptionDeviceId.forOrigin(noBackupDir,initialOrigin);
  for(int redirects=0;;redirects++){
   String origin=SubscriptionDeviceId.origin(url);
   HttpURLConnection c=open.connection(url);
   try{
    c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);
    c.setRequestProperty("User-Agent",userAgent);
    if(hwid!=null&&initialOrigin.equals(origin))c.setRequestProperty("X-HWID",hwid);
    int status=c.getResponseCode();
    if(status==301||status==302||status==303||status==307||status==308){
     String location=c.getHeaderField("Location");
     if(redirects>=5||location==null||location.trim().isEmpty())throw new IOException(L10n.s(R.string.msg_81f0596a4a)+status);
     url=new URL(url,location);continue;
    }
    if(status!=200)throw new IOException(L10n.s(R.string.msg_81f0596a4a)+status);
    String usage=c.getHeaderField("Subscription-Userinfo"),title=c.getHeaderField("Profile-Title");
    try(InputStream in=c.getInputStream();ByteArrayOutputStream b=new ByteArrayOutputStream()){
     byte[] buf=new byte[16384];int n;
     while((n=in.read(buf))!=-1){if(b.size()+n>4*1024*1024)throw new IOException(L10n.s(R.string.msg_7344ead1f2));b.write(buf,0,n);}
     return new SubscriptionDownload(b.toString("UTF-8"),usage,title);
    }
   }finally{c.disconnect();}
  }
 }
}
