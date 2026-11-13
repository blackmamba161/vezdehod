import local.blackmamba.vpn.*;
import org.json.*;
import java.util.*;

public class RuleGroupsTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static JSONObject rule(String id,String type,String value,String target)throws Exception{return new JSONObject().put("id",id).put("type",type).put("value",value).put("target",target).put("network","all").put("enabled",true);}
 static String route(JSONArray rules,String app,String domain,String network)throws Exception{
  for(int i=0;i<rules.length();i++){JSONObject r=rules.getJSONObject(i);if(!r.optBoolean("enabled",true)||!r.optString("network","all").equals("all")&&!network.equals(r.optString("network")))continue;String t=r.getString("type"),v=r.getString("value");if(t.equals("app")&&v.equals(app)||t.equals("domain")&&v.equals(domain)||t.equals("domain_suffix")&&(v.equals(domain)||domain.endsWith("."+v)))return r.optString("target","auto");}return "direct";
 }
 public static void main(String[] args)throws Exception {
  JSONObject youtube=rule("yt","domain_suffix","youtube.com","auto"),telegram=rule("tg","app","org.telegram.messenger","auto"),video=rule("vid","domain_suffix","googlevideo.com","auto");
  JSONArray original=new JSONArray().put(youtube).put(telegram).put(video);String untouched=original.toString();JSONArray grouped=RuleGroups.consolidate(original);
  check(grouped.getJSONObject(1).getString("id").equals("vid"),"Service not grouped");check(original.toString().equals(untouched),"Input mutated");check(RuleGroups.groups(grouped).size()==2,"Wrong group count");check(RuleGroups.consolidate(grouped).toString().equals(grouped.toString()),"Grouping not idempotent");
  JSONArray moved=RuleGroups.moveGroup(grouped,1,-1);check(moved.getJSONObject(0).getString("id").equals("tg"),"Group move");check(moved.getJSONObject(1).getString("id").equals("yt")&&moved.getJSONObject(2).getString("id").equals("vid"),"Internal order lost");check(RuleGroups.moveGroup(moved,0,1).toString().equals(grouped.toString()),"Group reverse");
  JSONArray swapped=RuleGroups.moveRule(grouped,0,1);check(swapped.getJSONObject(0).getString("id").equals("vid"),"Rule move");check(RuleGroups.moveRule(grouped,1,1).toString().equals(grouped.toString()),"Rule escaped group");check(RuleGroups.moveGroup(grouped,0,-1).toString().equals(grouped.toString()),"Edge move");
  JSONArray exception=new JSONArray().put(youtube).put(rule("deny","domain","ads.googlevideo.com","block")).put(video);
  JSONArray safe=RuleGroups.consolidate(exception);check(safe.toString().equals(exception.toString()),"Exception priority changed");
  Random random=new Random(7);String[] targets={"auto","direct","block","server:test"};
  for(int trial=0;trial<200;trial++){
   JSONArray input=new JSONArray();for(int i=0;i<18;i++){JSONObject r=new JSONObject(original.getJSONObject(random.nextInt(3)).toString());r.put("id","r"+i).put("target",targets[random.nextInt(targets.length)]).put("enabled",random.nextBoolean()).put("network",random.nextBoolean()?"all":"cellular");input.put(r);}
   JSONArray output=RuleGroups.consolidate(input);for(String net:new String[]{"wifi","cellular"})for(String app:new String[]{"org.telegram.messenger","other"})for(String domain:new String[]{"youtube.com","ads.youtube.com","googlevideo.com","video.googlevideo.com","example.com"})check(route(input,app,domain,net).equals(route(output,app,domain,net)),"Migration changed routing");
  }
  Set<String> all=new HashSet<>();for(RuleCatalog.Service s:RuleCatalog.SERVICES)all.add(s.id);
  JSONArray presets=RuleGroups.addPresets(new JSONArray(),all);check(presets.length()>60,"Presets empty");check(RuleGroups.addPresets(presets,all).toString().equals(presets.toString()),"Repeated apply duplicates rules");check(route(presets,"org.telegram.messenger","1.2.3.4","wifi").equals("auto"),"App IP traffic omitted");check(route(presets,"other","example.com","wifi").equals("direct"),"Unmatched traffic changed");check(route(presets,"other","mail.google.com","cellular").equals("direct"),"Google routed too broadly");
  JSONArray existing=new JSONArray().put(rule("custom","domain_suffix","youtube.com","direct").put("enabled",false));JSONArray added=RuleGroups.addPresets(existing,Set.of("youtube"));check(added.getJSONObject(0).toString().equals(existing.getJSONObject(0).toString()),"Custom route/disabled state overwritten");int count=0;for(int i=0;i<added.length();i++)if(added.getJSONObject(i).getString("value").equals("youtube.com"))count++;check(count==1,"Duplicate existing rule");
  JSONArray networkRule=new JSONArray().put(rule("wifi","domain_suffix","youtube.com","direct").put("network","wifi"));JSONArray both=RuleGroups.addPresets(networkRule,Set.of("youtube"));check(route(both,"other","youtube.com","wifi").equals("direct"),"Wi-Fi exception overridden");check(route(both,"other","youtube.com","cellular").equals("auto"),"Mobile rule missing");
  for(int i=0;i<presets.length();i++){JSONObject r=presets.getJSONObject(i);check(!r.getString("value").contains("*"),"Unsupported wildcard");check(r.getString("type").equals("app")||r.getString("value").contains("."),"Invalid domain");check(!RuleCatalog.service(r).equals("custom"),"Preset loses group");}
  System.out.println("PASS groups, arrows, boundaries, 200 routing-preservation cases, dedupe, disabled/custom routes, app IP routing, network exceptions, direct fallback");
 }
}
