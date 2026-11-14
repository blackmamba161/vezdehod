package local.blackmamba.vpn;
import com.sun.net.httpserver.*;import java.net.*;import java.io.*;import java.nio.file.*;import java.security.*;import java.util.*;import java.util.concurrent.*;
public class UpdateDownloadTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static String hash(byte[] b)throws Exception{StringBuilder s=new StringBuilder();for(byte x:MessageDigest.getInstance("SHA-256").digest(b))s.append(String.format("%02x",x&255));return s.toString();}
 static void reject(UpdateDownload d,URL url,File file,UpdateDownload.Open open,String why)throws Exception{try{d.run(url,file,open,3000);throw new AssertionError(why);}catch(IOException expected){check(!file.exists(),"Rejected download left a partial APK");}}
 public static void main(String[] args)throws Exception{
  byte[] body=new byte[100003];new Random(8).nextBytes(body);HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.setExecutor(Executors.newCachedThreadPool(r->{Thread t=new Thread(r);t.setDaemon(true);return t;}));
  server.createContext("/ok",x->{x.sendResponseHeaders(200,body.length);x.getResponseBody().write(body);x.close();});
  server.createContext("/redirect",x->{x.getResponseHeaders().set("Location","https://release-assets.githubusercontent.com/ok");x.sendResponseHeaders(302,-1);x.close();});
  server.createContext("/unsafe",x->{x.getResponseHeaders().set("Location","http://github.com/ok");x.sendResponseHeaders(302,-1);x.close();});
  server.createContext("/loop",x->{x.getResponseHeaders().set("Location","/loop");x.sendResponseHeaders(302,-1);x.close();});
  server.createContext("/short",x->{x.sendResponseHeaders(200,body.length-1);x.close();});
  server.createContext("/stall",x->{try{x.sendResponseHeaders(200,body.length);x.getResponseBody().write(body,0,1);x.getResponseBody().flush();Thread.sleep(4000);}catch(Exception ignored){}finally{x.close();}});
  server.start();Path dir=Files.createTempDirectory("vezdehod-update-");File file=dir.resolve("download.part").toFile();UpdateDownload.Open open=u->(HttpURLConnection)new URL("http://127.0.0.1:"+server.getAddress().getPort()+u.getPath()).openConnection();
  try{
   UpdateDownload d=new UpdateDownload(body.length,hash(body));d.run(new URL("https://github.com/redirect"),file,open,3000);check(Arrays.equals(Files.readAllBytes(file.toPath()),body)&&d.bytes==body.length,"Redirected download corrupted");file.delete();
   reject(new UpdateDownload(body.length,hash(body)),new URL("https://github.com/unsafe"),file,open,"HTTP redirect accepted");
   reject(new UpdateDownload(body.length,hash(body)),new URL("https://github.com/loop"),file,open,"Infinite redirect accepted");
   reject(new UpdateDownload(body.length,hash(body)),new URL("https://github.com/short"),file,open,"Wrong length accepted");
   reject(new UpdateDownload(body.length,"0".repeat(64)),new URL("https://github.com/ok"),file,open,"Wrong checksum accepted");
   for(String bad:new String[]{"https://github.com.attacker.test/ok","https://attacker.test/ok","https://user@github.com/ok","https://github.com:444/ok","http://github.com/ok"})check(!UpdateDownload.allowed(new URL(bad)),"Unsafe origin accepted");
   UpdateDownload slow=new UpdateDownload(body.length,hash(body));long start=System.nanoTime();try{slow.run(new URL("https://github.com/stall"),file,open,150);throw new AssertionError("Deadline ignored");}catch(IOException expected){check(slow.timedOut&&slow.canceled,"Deadline did not cancel transfer");check(!file.exists(),"Cancelled transfer left APK");check(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()-start)<6,"Cancelled transfer did not terminate");}
   UpdateDownload cancelled=new UpdateDownload(body.length,hash(body));cancelled.cancel();reject(cancelled,new URL("https://github.com/ok"),file,open,"Cancelled transfer restarted");
   System.out.println("PASS redirected streaming, checksum, size, HTTPS origins, redirect bound, deadline, cancellation and partial-file cleanup");
  }finally{server.stop(0);file.delete();Files.delete(dir);}
 }
}
