package local.blackmamba.vpn;
import org.json.*;import java.util.*;

/** Calculates removal on a copy; no caller-visible mutation until the store commits it. */
public final class ProfileRemoval {
 public static Set<String> serverIds(JSONObject data,String profile)throws JSONException{Set<String> ids=new HashSet<>();JSONArray nodes=data.getJSONArray("servers");for(int i=0;i<nodes.length();i++){JSONObject n=nodes.getJSONObject(i);if(profile.equals(n.optString("profile")))ids.add(n.getString("id"));}return ids;}
 public static List<String> usages(JSONObject data,String profile)throws JSONException{Set<String> ids=serverIds(data,profile);List<String> use=new ArrayList<>();for(String key:new String[]{"wifi","cellular","fallbackWifi","fallbackCellular"})if(ids.contains(data.optString(key)))use.add(key);if(ids.contains(data.optString("defaultTarget").replace("server:","")))use.add("defaultTarget");JSONArray rules=data.getJSONArray("rules");for(int i=0;i<rules.length();i++){JSONObject rule=rules.getJSONObject(i);if(ids.contains(rule.optString("target").replace("server:","")))use.add("rule:"+rule.optString("label",rule.optString("value")));}return use;}
 public static JSONObject apply(JSONObject original,String profile,String replacement,boolean clear)throws Exception{
  JSONObject data=new JSONObject(original.toString());boolean exists=false;JSONArray profiles=data.getJSONArray("profiles");for(int i=0;i<profiles.length();i++)if(profile.equals(profiles.getJSONObject(i).optString("id")))exists=true;if(!exists)throw new IllegalArgumentException("Missing subscription");
  Set<String> removed=serverIds(data,profile);JSONArray remaining=new JSONArray(),kept=new JSONArray();for(int i=0;i<data.getJSONArray("servers").length();i++){JSONObject n=data.getJSONArray("servers").getJSONObject(i);if(!removed.contains(n.getString("id")))remaining.put(n);}for(int i=0;i<profiles.length();i++)if(!profile.equals(profiles.getJSONObject(i).optString("id")))kept.put(profiles.get(i));
  if(!usages(data,profile).isEmpty()){
   boolean valid=false;for(int i=0;i<remaining.length();i++)if(replacement.equals(remaining.getJSONObject(i).getString("id")))valid=true;
   if(!valid&&!(clear&&remaining.length()==0))throw new IllegalArgumentException("Choose a replacement server");
   for(String key:new String[]{"wifi","cellular","fallbackWifi","fallbackCellular"})if(removed.contains(data.optString(key)))data.put(key,valid?replacement:"");
   if(removed.contains(data.optString("defaultTarget").replace("server:","")))data.put("defaultTarget",valid?"server:"+replacement:"direct");
   JSONArray rules=data.getJSONArray("rules");for(int i=0;i<rules.length();i++){JSONObject r=rules.getJSONObject(i);if(removed.contains(r.optString("target").replace("server:",""))){r.put("target",valid?"server:"+replacement:"auto");if(!valid)r.put("enabled",false);}}
  }
  data.put("profiles",kept).put("servers",remaining);return data;
 }
}
