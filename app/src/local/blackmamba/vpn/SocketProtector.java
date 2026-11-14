package local.blackmamba.vpn;
import android.net.*;import android.os.*;import android.system.Os;import java.io.*;import java.util.*;import java.util.concurrent.*;import java.util.function.Supplier;
/** Same-UID Xray workers hand off only outbound socket descriptors, never configuration data. */
final class SocketProtector implements AutoCloseable {
 final String name="vezdehod-"+UUID.randomUUID();private final LocalServerSocket server;private final TunnelService vpn;private final Supplier<Network> network;
 private final Set<LocalSocket> active=ConcurrentHashMap.newKeySet();private final ThreadPoolExecutor workers=new ThreadPoolExecutor(2,2,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(32),r->{Thread t=new Thread(r,"vpn-protect");t.setDaemon(true);return t;});private volatile boolean closed;
 SocketProtector(TunnelService vpn,Supplier<Network> network)throws IOException{this.vpn=vpn;this.network=network;server=new LocalServerSocket(name);Thread accept=new Thread(()->{while(!closed)try{LocalSocket socket=server.accept();active.add(socket);try{workers.execute(()->handle(socket));}catch(RejectedExecutionException e){active.remove(socket);socket.close();}}catch(IOException e){if(!closed)NetworkErrors.record("protect-accept","",e.toString());}} ,"vpn-protect-accept");accept.setDaemon(true);accept.start();}
 void handle(LocalSocket socket){FileDescriptor[] descriptors=null;try{
  socket.setSoTimeout(2000);if(socket.getPeerCredentials().getUid()!=android.os.Process.myUid())return;
  if(socket.getInputStream().read()!=1)return;descriptors=socket.getAncillaryFileDescriptors();boolean ok=false;
  if(descriptors!=null&&descriptors.length==1){Network physical=network.get();try(ParcelFileDescriptor fd=ParcelFileDescriptor.dup(descriptors[0])){if(physical!=null&&vpn.protect(fd.getFd())){physical.bindSocket(fd.getFileDescriptor());ok=true;}}}
  socket.getOutputStream().write(ok?1:0);socket.getOutputStream().flush();
 }catch(Exception e){if(!closed)NetworkErrors.record("protect-worker","",e.toString());}finally{if(descriptors!=null)for(FileDescriptor fd:descriptors)try{Os.close(fd);}catch(Exception ignored){}active.remove(socket);try{socket.close();}catch(Exception ignored){}}}
 void apply(ProcessBuilder process){process.environment().put("VEZDEHOD_PROTECT_SOCKET","@"+name);}
 public void close(){closed=true;try{server.close();}catch(Exception ignored){}for(LocalSocket socket:active)try{socket.close();}catch(Exception ignored){}workers.shutdownNow();}
}
