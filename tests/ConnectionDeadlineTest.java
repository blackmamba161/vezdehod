package local.blackmamba.vpn;
import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicInteger;
/** Timeout/stop must not wait for a blocked engine's serialized executor. */
public final class ConnectionDeadlineTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  ExecutorService blocked=Executors.newSingleThreadExecutor();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger fired=new AtomicInteger();
  try(ConnectionDeadline deadline=new ConnectionDeadline()){
   blocked.submit(()->{entered.countDown();try{release.await();}catch(InterruptedException ignored){}});check(entered.await(1,TimeUnit.SECONDS),"Worker did not start");
   CountDownLatch expired=new CountDownLatch(1);deadline.arm(40,()->{fired.incrementAndGet();expired.countDown();});check(expired.await(1,TimeUnit.SECONDS),"Blocked startup prevented independent timeout");
   CountDownLatch premature=new CountDownLatch(1);deadline.arm(60,premature::countDown);deadline.clear();check(!premature.await(150,TimeUnit.MILLISECONDS),"Cancelled timeout still ran");
   deadline.arm(60,()->fired.addAndGet(100));CountDownLatch newest=new CountDownLatch(1);deadline.arm(100,()->{fired.incrementAndGet();newest.countDown();});check(newest.await(1,TimeUnit.SECONDS),"Replacement deadline did not run");check(fired.get()==2,"Stale deadline affected a new connection");
  }finally{release.countDown();blocked.shutdownNow();check(blocked.awaitTermination(1,TimeUnit.SECONDS),"Worker leaked");}
  System.out.println("PASS deadline fires while engine worker is blocked; cancel and replacement discard stale deadlines; threads terminate");
 }
}
