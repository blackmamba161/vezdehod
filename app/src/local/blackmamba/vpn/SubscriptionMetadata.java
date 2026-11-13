package local.blackmamba.vpn;
import org.json.*;
import java.util.*;

/** Missing limits do not imply missing traffic statistics. Values come only from the provider. */
public final class SubscriptionMetadata {
 public static void apply(JSONObject profile,String header)throws JSONException {
  for(String key:new String[]{"upload","download","total","expire"})profile.remove(key);
  if(header==null)return;
  for(String part:header.split(";")){
   String[] pair=part.trim().split("=",2);if(pair.length!=2)continue;
   String key=pair[0].trim().toLowerCase(Locale.ROOT);
   if(!Arrays.asList("upload","download","total","expire").contains(key))continue;
   try{long value=Long.parseLong(pair[1].trim());if(value>=0)profile.put(key,value);}catch(NumberFormatException ignored){}
  }
 }
 public static boolean hasUsage(JSONObject profile){return profile.has("upload")||profile.has("download");}
 public static long used(JSONObject profile){long up=Math.max(0,profile.optLong("upload")),down=Math.max(0,profile.optLong("download"));return up>Long.MAX_VALUE-down?Long.MAX_VALUE:up+down;}
}
