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
  System.out.println("PASS transient failure, failed reserve, confirmed failover, recovery hysteresis, network reset");
 }
}
