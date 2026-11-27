package local.blackmamba.vpn;

import org.json.*;
import java.util.*;
import java.util.function.Function;

/** One checkbox per package: checked when at least one of its app rules is enabled. */
public final class AppRuleSelection {
 public static Map<String,Boolean> checked(JSONArray rules){
  Map<String,Boolean> out=new LinkedHashMap<>();
  for(int i=0;i<rules.length();i++){
   JSONObject rule=rules.optJSONObject(i);String pkg=appPackage(rule);
   if(!pkg.isEmpty())out.merge(pkg,rule.optBoolean("enabled",true),(x,y)->x||y);
  }
  return out;
 }
 public static JSONArray apply(JSONArray original,Map<String,Boolean> changes,String target,String network,Function<String,String> label)throws JSONException{
  JSONArray rules=RuleGroups.copy(original);Set<String> existing=new HashSet<>();
  for(int i=0;i<rules.length();i++){
   JSONObject rule=rules.getJSONObject(i);String pkg=appPackage(rule);
   if(pkg.isEmpty())continue;
   existing.add(pkg);Boolean enabled=changes.get(pkg);
   if(enabled!=null&&enabled!=rule.optBoolean("enabled",true))rule.put("enabled",enabled);
  }
  for(Map.Entry<String,Boolean> change:changes.entrySet()){
   String pkg=change.getKey();if(!change.getValue()||pkg.isEmpty()||existing.contains(pkg))continue;
   rules.put(Store.obj("id",UUID.randomUUID().toString(),"type","app","value",pkg,"label",label.apply(pkg),"network",network,"target",target,"enabled",true));
  }
  return rules;
 }
 private static String appPackage(JSONObject rule){return rule!=null&&"app".equals(rule.optString("type"))?rule.optString("value"):"";}
 private AppRuleSelection(){}
}
