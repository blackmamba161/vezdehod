package local.blackmamba.vpn;
import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicLong;
/** Deadline runs independently of the serialized engine worker. */
final class ConnectionDeadline implements AutoCloseable {
 private final ScheduledExecutorService clock=Executors.newSingleThreadScheduledExecutor();
 private final AtomicLong generation=new AtomicLong();private ScheduledFuture<?> pending;
 synchronized void arm(long milliseconds,Runnable timeout){clear();long token=generation.incrementAndGet();pending=clock.schedule(()->{if(generation.compareAndSet(token,token+1))timeout.run();},milliseconds,TimeUnit.MILLISECONDS);}
 synchronized void clear(){generation.incrementAndGet();if(pending!=null){pending.cancel(false);pending=null;}}
 public synchronized void close(){clear();clock.shutdownNow();}
}
