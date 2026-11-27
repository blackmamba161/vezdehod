package local.blackmamba.vpn;

public final class VpnRecoveryTest {
 static void expect(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args){
  VpnRecovery timer=VpnRecovery.waitFrom(300);
  expect(timer.remainingSeconds(300)==15,"Initial countdown");
  expect(timer.remainingSeconds(301)==15,"Round remaining seconds up");
  expect(timer.remainingMillis(5300)==10_000,"Progress follows the same clock");
  expect(timer.remainingSeconds(15_299)==1&&!timer.isDue(15_299),"Do not start early");
  expect(timer.isDue(15_300)&&timer.remainingSeconds(15_301)==0,"Start at the deadline and don't show negative time");
  expect(!VpnRecovery.CHECKING.isDue(30_000),"Do not schedule a second probe while the first is running");
  expect(VpnRecovery.waitFrom(30_000).remainingSeconds(30_000)==15,"A failed check gets a fresh countdown");
  System.out.println("PASS VPN recovery countdown and probe timing");
 }
}
