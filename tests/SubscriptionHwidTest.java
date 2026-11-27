package local.blackmamba.vpn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SubscriptionHwidTest {
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}

 static final class Response extends HttpURLConnection {
  final int status;
  final String location;
  String hwid,agent;
  boolean closed;
  Response(URL url,int status,String location){super(url);this.status=status;this.location=location;}
  @Override public void connect(){}
  @Override public boolean usingProxy(){return false;}
  @Override public void disconnect(){closed=true;}
  @Override public void setRequestProperty(String key,String value){
   if(key.equals("X-HWID"))hwid=value;
   if(key.equals("User-Agent"))agent=value;
  }
  @Override public int getResponseCode(){check(!getInstanceFollowRedirects(),"redirects must be handled explicitly");return status;}
  @Override public String getHeaderField(String name){
   switch(name){case "Location":return location;case "Subscription-Userinfo":return "total=10";case "Profile-Title":return "Fixture";default:return null;}
  }
  @Override public InputStream getInputStream(){return new ByteArrayInputStream("subscription".getBytes(StandardCharsets.UTF_8));}
 }

 public static void main(String[] args)throws Exception{
  check(Preferences.defaults().getBoolean(Preferences.SEND_SUBSCRIPTION_HWID),"HWID should be enabled by default");
  Path dir=Files.createTempDirectory("subscription-hwid-test");
  try{
   URL first=new URL("https://Provider.Example/sub?token=one");
   String origin=SubscriptionDeviceId.origin(first);
   check(origin.equals(SubscriptionDeviceId.origin(new URL("https://provider.example:443/other?token=two"))),"same origin normalized");
   List<Response> requests=new ArrayList<>();
   SubscriptionDownload.fetch(first,"FixtureUA",null,url->{Response r=new Response(url,200,null);requests.add(r);return r;});
   check(requests.get(0).hwid==null&&requests.get(0).closed,"opt-out must not send HWID");
   try(var files=Files.list(dir)){check(files.findAny().isEmpty(),"opt-out must not generate a secret");}

   requests.clear();
   SubscriptionDownload result=SubscriptionDownload.fetch(first,"FixtureUA",dir.toFile(),url->{
    int step=requests.size();
    Response r=step==0?new Response(url,302,"https://provider.example:443/next"):
     step==1?new Response(url,307,"https://elsewhere.example/list"):new Response(url,200,null);
    requests.add(r);return r;
   });
   String id=requests.get(0).hwid;
   check(id!=null&&id.matches("[0-9a-f]{32}")&&id.equals(requests.get(1).hwid),"stable HWID on same-origin redirect");
   check(requests.get(2).hwid==null,"HWID sent to redirect target on another origin");
   check(requests.stream().allMatch(r->r.closed&&"FixtureUA".equals(r.agent)),"connections closed and User-Agent preserved");
   check(result.body.equals("subscription")&&result.usage.equals("total=10")&&result.title.equals("Fixture"),"body and metadata preserved");
   check(id.equals(SubscriptionDeviceId.forOrigin(dir.toFile(),origin)),"persisted ID changed");
   for(String other:List.of("https://elsewhere.example/","https://provider.example:8443/")){
    check(!id.equals(SubscriptionDeviceId.forOrigin(dir.toFile(),SubscriptionDeviceId.origin(new URL(other)))),"distinct origins share an ID");
   }
   check(!id.equals(SubscriptionDeviceId.forOrigin(Files.createDirectory(dir.resolve("fresh")).toFile(),origin)),"fresh install reused ID");

   requests.clear();
   try{
    SubscriptionDownload.fetch(first,"FixtureUA",dir.toFile(),url->{Response r=new Response(url,301,"http://provider.example/list");requests.add(r);return r;});
    throw new AssertionError("HTTPS downgrade accepted");
   }catch(IOException expected){check(requests.size()==1&&requests.get(0).closed,"HTTP connection opened or redirect left open");}
   requests.clear();
   try{
    SubscriptionDownload.fetch(first,"FixtureUA",dir.toFile(),url->{Response r=new Response(url,302,"/loop");requests.add(r);return r;});
    throw new AssertionError("redirect loop accepted");
   }catch(IOException expected){check(requests.size()==6&&requests.stream().allMatch(r->r.closed),"redirect limit or cleanup missing");}
   System.out.println("PASS HWID opt-in, stable private seed, origin isolation, redirect policy and subscription response");
  }finally{
   try(var files=Files.walk(dir)){for(Path path:files.sorted(Comparator.reverseOrder()).toArray(Path[]::new))Files.delete(path);}
  }
 }
}
