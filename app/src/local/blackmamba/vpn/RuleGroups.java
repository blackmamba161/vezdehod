package local.blackmamba.vpn;

import org.json.*;
import java.util.*;

/** Display order is exactly engine order. Never move a rule across a different action implicitly. */
public final class RuleGroups {
 public static final class Group {
  public final String service;public final int start,end;
  Group(String service,int start,int end){this.service=service;this.start=start;this.end=end;}
  public int size(){return end-start;}
 }
 public static List<Group> groups(JSONArray rules){
  List<Group> out=new ArrayList<>();int start=0;
  while(start<rules.length()){String service=RuleCatalog.service(rules.optJSONObject(start));int end=start+1;while(end<rules.length()&&service.equals(RuleCatalog.service(rules.optJSONObject(end))))end++;out.add(new Group(service,start,end));start=end;}return out;
 }
 public static JSONArray copy(JSONArray rules)throws JSONException{return new JSONArray(rules.toString());}
 /** Equal actions commute even where predicates overlap; conflicting exceptions keep their exact relative order. */
 public static JSONArray consolidate(JSONArray original)throws JSONException {
  List<JSONObject> rules=new ArrayList<>();for(int i=0;i<original.length();i++)rules.add(new JSONObject(original.getJSONObject(i).toString()));
  for(int first=0;first<rules.size();first++){
   String service=RuleCatalog.service(rules.get(first));int insert=first+1;
   while(insert<rules.size()&&service.equals(RuleCatalog.service(rules.get(insert))))insert++;
   for(int next=insert;next<rules.size();next++){
    JSONObject candidate=rules.get(next);if(!service.equals(RuleCatalog.service(candidate)))continue;boolean safe=true;
    for(int j=insert;j<next;j++){JSONObject other=rules.get(j);if(candidate.optBoolean("enabled",true)&&other.optBoolean("enabled",true)&&!candidate.optString("target","auto").equals(other.optString("target","auto"))){safe=false;break;}}
    if(safe){rules.remove(next);rules.add(insert++,candidate);}else break;
   }first=insert-1;
  }JSONArray out=new JSONArray();for(JSONObject r:rules)out.put(r);return out;
 }
 public static JSONArray moveGroup(JSONArray original,int index,int direction)throws JSONException {
  List<Group> groups=groups(original);int other=index+direction;if(Math.abs(direction)!=1||index<0||index>=groups.size()||other<0||other>=groups.size())return copy(original);
  Collections.swap(groups,index,other);JSONArray out=new JSONArray();for(Group group:groups)for(int i=group.start;i<group.end;i++)out.put(new JSONObject(original.getJSONObject(i).toString()));return out;
 }
 public static JSONArray moveRule(JSONArray original,int index,int direction)throws JSONException {
  JSONArray out=copy(original);if(Math.abs(direction)!=1)return out;int other=index+direction;
  for(Group g:groups(original))if(index>=g.start&&index<g.end&&other>=g.start&&other<g.end){JSONObject r=out.getJSONObject(index);out.put(index,out.getJSONObject(other));out.put(other,r);break;}return out;
 }
 public static boolean covered(JSONArray rules,JSONObject candidate){
  for(int i=0;i<rules.length();i++){
   JSONObject r=rules.optJSONObject(i);String net=r.optString("network","all");if(!net.equals("all"))continue;
   String type=r.optString("type"),v=r.optString("value").toLowerCase(Locale.ROOT),ct=candidate.optString("type"),cv=candidate.optString("value").toLowerCase(Locale.ROOT);
   if(type.equals(ct)&&v.equals(cv))return true;
   if(type.equals("domain_suffix")&&(ct.equals("domain")||ct.equals("domain_suffix"))&&(cv.equals(v)||cv.endsWith("."+v)))return true;
  }return false;
 }
 public static int missing(JSONArray rules,RuleCatalog.Service service)throws JSONException {int count=0;JSONArray candidates=service.rules();for(int i=0;i<candidates.length();i++)if(!covered(rules,candidates.getJSONObject(i)))count++;return count;}
 /** Append below existing exceptions; do not overwrite routes or re-enable disabled user rules. */
 public static JSONArray addPresets(JSONArray original,Set<String> selected)throws JSONException {
  JSONArray out=copy(original);for(RuleCatalog.Service service:RuleCatalog.SERVICES)if(selected.contains(service.id)){JSONArray candidates=service.rules();for(int i=0;i<candidates.length();i++){JSONObject candidate=candidates.getJSONObject(i);if(!covered(out,candidate))out.put(candidate);}}return consolidate(out);
 }
 private RuleGroups(){}
}
