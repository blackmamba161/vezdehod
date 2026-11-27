import local.blackmamba.vpn.FailoverPolicy;
import static local.blackmamba.vpn.FailoverPolicy.Decision.*;
public final class FailoverTest {
 static void expect(Object actual,Object wanted){if(actual!=wanted)throw new AssertionError(actual+" != "+wanted);}
 public static void main(String[] args){FailoverPolicy p=new FailoverPolicy();
  expect(p.observe(false,false,true),KEEP);expect(p.observe(false,true,true),KEEP);
  expect(p.observe(false,false,true),KEEP);expect(p.observe(false,false,false),KEEP);
  expect(p.observe(false,false,true),RESERVE);
  expect(p.observe(true,true,true),KEEP);expect(p.observe(true,true,false),KEEP);
  expect(p.observe(true,true,true),KEEP);expect(p.observe(true,true,true),PRIMARY);
  expect(p.observe(false,false,true),KEEP);p.reset();expect(p.observe(false,false,true),KEEP);
  p.reset();expect(p.observe(false,false,true,true),RESERVE);
  expect(p.observe(false,true,true,true),KEEP);expect(p.observe(false,false,false,true),KEEP);
  expect(p.observe(true,false,true),PRIMARY);
  expect(FailoverPolicy.recoverFromDirect(false,true),RESERVE);expect(FailoverPolicy.recoverFromDirect(true,true),PRIMARY);expect(FailoverPolicy.recoverFromDirect(false,false),KEEP);
  System.out.println("PASS startup reserve, transient failures, confirmed failover, return hysteresis and recovery from direct");
 }
}
