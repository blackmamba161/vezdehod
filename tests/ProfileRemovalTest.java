import org.json.*;import local.blackmamba.vpn.ProfileRemoval;
public class ProfileRemovalTest {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 static JSONObject data()throws Exception{return new JSONObject("{profiles:[{id:p},{id:q}],servers:[{id:a,profile:p},{id:b,profile:p},{id:c,profile:q}],wifi:a,cellular:b,fallbackWifi:b,fallbackCellular:c,defaultTarget:'server:b',rules:[{id:r,target:'server:a',enabled:true},{id:s,target:auto,enabled:true}]}");}
 public static void main(String[] args)throws Exception{
  JSONObject original=data();String before=original.toString();check(ProfileRemoval.usages(original,"p").size()==5,"Missing references");
  JSONObject next=ProfileRemoval.apply(original,"p","c",false);check(before.equals(original.toString()),"Input mutated");check(next.getJSONArray("profiles").length()==1&&next.getJSONArray("servers").length()==1,"Removal incomplete");
  for(String k:new String[]{"wifi","cellular","fallbackWifi","fallbackCellular"})check(next.getString(k).equals("c"),"Assignment not retained/replaced: "+k);
  check(next.getString("defaultTarget").equals("server:c"),"Default route not replaced");check(next.getJSONArray("rules").getJSONObject(0).getString("target").equals("server:c"),"Rule not replaced");check(next.getJSONArray("rules").getJSONObject(1).getString("target").equals("auto"),"Unrelated rule modified");
  for(String invalid:new String[]{"","a","missing"}){boolean rejected=false;try{ProfileRemoval.apply(original,"p",invalid,false);}catch(Exception expected){rejected=true;}check(rejected&&before.equals(original.toString()),"Unsafe replacement accepted");}
  JSONObject last=data();last.getJSONArray("servers").remove(2);last.getJSONArray("profiles").remove(1);last.put("fallbackCellular","");boolean rejected=false;try{ProfileRemoval.apply(last,"p","",false);}catch(Exception expected){rejected=true;}check(rejected,"Last subscription cleared without consent");
  next=ProfileRemoval.apply(last,"p","",true);check(next.getString("wifi").isEmpty()&&next.getString("cellular").isEmpty(),"Last assignments retained");check(next.getString("defaultTarget").equals("direct"),"Last default route unsafe");check(!next.getJSONArray("rules").getJSONObject(0).getBoolean("enabled"),"Dangling rule enabled");
  JSONObject unused=data();unused.getJSONArray("profiles").put(new JSONObject().put("id","empty"));check(ProfileRemoval.apply(unused,"empty","",false).getJSONArray("servers").length()==3,"Unused removal affected servers");
  System.out.println("PASS replacement of every reference, unrelated data preserved, no mutation on failure, explicit last-profile consent");
 }
}
