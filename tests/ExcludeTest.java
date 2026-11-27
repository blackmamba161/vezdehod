import org.json.*;import java.util.*;import local.blackmamba.vpn.Configuration;import static local.blackmamba.vpn.Store.*;
/** Pure-JVM check of the system split-tunnelling plan: which apps Android keeps out of / lets into the TUN. */
public class ExcludeTest {
 static JSONObject rule(String type,String value,String target,String network,boolean enabled)throws Exception{return obj("id",type+value,"type",type,"value",value,"label",value,"network",network,"target",target,"enabled",enabled);}
 static Configuration.Plan plan(String def,String network,JSONObject... rules)throws Exception{JSONArray rr=new JSONArray();for(JSONObject r:rules)rr.put(r);return Configuration.plan(obj("servers",arr(obj("id","one","outbound",obj("type","direct"))),"defaultTarget",def,"rules",rr),network);}
 static void only(Configuration.Plan p,boolean allow,String... names){check(p!=null,"plan is null, expected "+Arrays.toString(names));check(p.allow==allow,"allow flag for "+Arrays.toString(names));check(p.packages.equals(new LinkedHashSet<>(Arrays.asList(names))),"packages "+p.packages+" != "+Arrays.toString(names));}
 static void check(boolean b,String m){if(!b)throw new RuntimeException(m);}
 public static void main(String[] a)throws Exception{
  check(local.blackmamba.vpn.Preferences.defaults().getBoolean("appScope"),"System split tunneling should be enabled by default");
  /* Rest of the phone uses the VPN: only apps routed direct bypass the TUN. VPN and blocked apps stay inside. */
  only(plan("auto","wifi",rule("app","com.direct.one","direct","all",true),rule("app","com.vpn.app","auto","all",true),rule("app","com.blocked","block","all",true)),false,"com.direct.one");
  /* Rest of the phone goes direct: only VPN apps enter the TUN. */
  only(plan("direct","wifi",rule("app","com.vpn.app","auto","all",true),rule("app","com.vpn.two","server:one","all",true),rule("app","com.direct.one","direct","all",true)),true,"com.vpn.app","com.vpn.two");
  /* Blocked apps must still enter the TUN so the core can reject their traffic. */
  only(plan("direct","wifi",rule("app","com.vpn.app","auto","all",true),rule("app","com.blocked","block","all",true)),true,"com.vpn.app","com.blocked");
  /* Disabled rules are ignored. Network-specific rules must still work after a network switch. */
  check(plan("direct","wifi",rule("app","com.vpn.app","auto","all",false))==null,"disabled rule produced a plan");
  only(plan("direct","wifi",rule("app","com.vpn.app","auto","all",true),rule("app","com.cellular","auto","cellular",true)),true,"com.vpn.app","com.cellular");
  /* Mixed per-network rules cannot be a static list: such an app stays in the TUN. */
  only(plan("direct","wifi",rule("app","com.vpn.app","auto","all",true),rule("app","com.mixed","auto","cellular",true),rule("app","com.mixed","direct","all",true)),true,"com.vpn.app","com.mixed");
  only(plan("direct","wifi",rule("app","com.blocked","block","cellular",true)),true,"com.blocked");
  /* Final "block": everything must stay in the TUN, otherwise blocked apps would escape. */
  check(plan("block","wifi",rule("app","com.vpn.app","auto","all",true))==null,"block-all must not exclude anything");
  check(plan("direct","wifi")==null,"no rules -> no exclusion");
  /* Domain / CIDR rules never affect the app scope. */
  check(plan("direct","wifi",rule("domain_suffix","youtube.com","auto","all",true))==null,"domain rule affected app scope");
  /* Equality is by value: the service compares the live plan with the connected one. */
  check(plan("direct","wifi",rule("app","com.vpn.app","auto","all",true)).equals(plan("direct","wifi",rule("app","com.vpn.app","auto","all",true))),"plans with same content must be equal");
  check(!plan("direct","wifi",rule("app","com.vpn.app","auto","all",true)).equals(plan("direct","wifi",rule("app","com.vpn.two","auto","all",true))),"different packages must not be equal");
  check(!plan("direct","wifi",rule("app","com.x","auto","all",true)).equals(plan("auto","wifi",rule("app","com.x","direct","all",true))),"allow vs disallow must not be equal");
  /* The same package with two rules (direct + block): excluding it would let it escape the block, so it must stay in the TUN. */
  only(plan("auto","wifi",rule("app","com.direct.one","direct","all",true),rule("app","com.direct.one","block","all",true),rule("app","com.other","direct","all",true)),false,"com.other");
  only(plan("direct","wifi",rule("app","com.vpn.app","direct","all",true),rule("app","com.vpn.app","auto","all",true)),true,"com.vpn.app");
  System.out.println("PASS split plan: disallow/allow, blocked apps stay in TUN, disabled/other-network/mixed rules, block-all, same-package direct+block, equality");
 }
}
