package local.blackmamba.vpn;
import org.json.*;
import java.util.*;
import static local.blackmamba.vpn.Store.*;

/** Local filtering: exceptions only skip this filter, never override routing. */
public final class AdBlock {
 public static JSONArray exceptions(String input)throws Exception {
  Set<String> names=new LinkedHashSet<>();
  for(String raw:input.trim().split("[\\s,;]+")){
   if(raw.isEmpty())continue;
   String name=java.net.IDN.toASCII(raw.toLowerCase(Locale.ROOT));
   if(name.length()>253||!name.matches("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+"))throw new Exception(L10n.s(R.string.msg_e07f86b4c4));
   names.add(name);
  }
  if(names.size()>200)throw new Exception(L10n.s(R.string.msg_3baad0b2cf));
  return new JSONArray(names);
 }
 public static JSONObject rejection(JSONObject data)throws Exception {
  JSONArray allow=exceptions(data.optString("adBlockAllow",""));
  JSONObject match=obj("rule_set",arr("ads-light"));
  if(allow.length()>0)match=obj("type","logical","mode","and","rules",arr(match,obj("domain_suffix",allow,"invert",true)));
  match.put("action","reject");return match;
 }
 public static void attach(JSONObject config,JSONObject data)throws Exception {
  if(!data.optBoolean("adBlock",false))return;
  String path=data.getString("adBlockPath");
  config.getJSONObject("route").put("rule_set",arr(obj("type","local","tag","ads-light","format","binary","path",path)));
 }
}
