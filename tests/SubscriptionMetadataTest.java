import local.blackmamba.vpn.SubscriptionMetadata;
import org.json.*;
public class SubscriptionMetadataTest {
 static void check(boolean value,String name){if(!value)throw new AssertionError(name);}
 public static void main(String[] args)throws Exception{
  JSONObject p=new JSONObject();
  SubscriptionMetadata.apply(p,"upload=143195331599; download=63877834184; total=0; expire=1795866450");
  check(SubscriptionMetadata.hasUsage(p),"No-limit plan hides usage");
  check(SubscriptionMetadata.used(p)==207073165783L,"Traffic sum");
  check(p.getLong("expire")==1795866450L,"Expiry lost");
  SubscriptionMetadata.apply(p," upload = 0 ; download = 0 ; total=1000 ; expire=0");
  check(SubscriptionMetadata.hasUsage(p)&&SubscriptionMetadata.used(p)==0,"Zero usage is known usage");
  check(p.getLong("total")==1000,"Limit missing");
  SubscriptionMetadata.apply(p,null);check(!SubscriptionMetadata.hasUsage(p)&&!p.has("expire"),"Stale metadata survived missing header");
  SubscriptionMetadata.apply(p,"download=-2;upload=garbage;expire=9999999999999999999999");check(!SubscriptionMetadata.hasUsage(p),"Invalid metadata accepted");
  SubscriptionMetadata.apply(p,"upload=9223372036854775807;download=5");check(SubscriptionMetadata.used(p)==Long.MAX_VALUE,"Counter overflow");
  System.out.println("PASS subscription metadata: zero limit, known zero traffic, expiry, missing/invalid headers, overflow");
 }
}
