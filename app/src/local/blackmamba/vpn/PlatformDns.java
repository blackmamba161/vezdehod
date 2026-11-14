package local.blackmamba.vpn;
import android.net.*;import android.os.CancellationSignal;import io.nekohasekai.libbox.*;import java.io.IOException;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.util.function.Supplier;
/** System DNS is always resolved by Android on an explicit non-VPN network. */
final class PlatformDns implements LocalDNSTransport {
 private static final Semaphore queries=new Semaphore(32);
 private final Supplier<Network> network;
 PlatformDns(Supplier<Network> network){this.network=network;}
 public boolean raw(){return true;}
 public void lookup(ExchangeContext ctx,String family,String domain)throws Exception{throw new IOException("Raw DNS required");}
 public void exchange(ExchangeContext ctx,byte[] query)throws Exception{
  Network captured=network.get();if(captured==null)throw new IOException("Physical network unavailable");
  if(!queries.tryAcquire())throw new IOException("System DNS query limit reached");
  CancellationSignal cancellation=new CancellationSignal();CountDownLatch done=new CountDownLatch(1);AtomicReference<byte[]> answer=new AtomicReference<>();AtomicReference<Exception> error=new AtomicReference<>();AtomicBoolean finished=new AtomicBoolean();
  try{
   ctx.onCancel(()->{finished.set(true);cancellation.cancel();done.countDown();});
   if(finished.get())throw new IOException("DNS cancelled");
   DnsResolver.getInstance().rawQuery(captured,query,DnsResolver.FLAG_NO_RETRY,Runnable::run,cancellation,new DnsResolver.Callback<byte[]>(){
    public void onAnswer(byte[] data,int rcode){if(finished.compareAndSet(false,true)){answer.set(data);done.countDown();}}
    public void onError(DnsResolver.DnsException e){if(finished.compareAndSet(false,true)){error.set(e);done.countDown();}}
   });
   if(!done.await(5,TimeUnit.SECONDS))throw new IOException("System DNS deadline exceeded");
   if(!captured.equals(network.get()))throw new IOException("DNS network changed");
   if(error.get()!=null)throw new IOException("System DNS failed",error.get());
   if(answer.get()==null)throw new IOException("DNS cancelled");
   ctx.rawSuccess(answer.get());
  }finally{finished.set(true);cancellation.cancel();queries.release();}
 }
}
