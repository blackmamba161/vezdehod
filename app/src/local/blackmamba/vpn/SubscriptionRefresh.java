package local.blackmamba.vpn;

import org.json.*;
import java.util.*;

/** Metadata and display order updates must not interrupt a running tunnel. */
public final class SubscriptionRefresh {
 public static boolean sameConnections(JSONArray before,JSONArray after)throws JSONException {
  return canonical(connections(before)).equals(canonical(connections(after)));
 }
 private static JSONObject connections(JSONArray nodes)throws JSONException {
  JSONObject result=new JSONObject();
  for(int i=0;i<nodes.length();i++){
   JSONObject n=nodes.getJSONObject(i);String id=n.getString("id");
   if(result.has(id))throw new JSONException("Duplicate server ID");
   JSONObject connection=new JSONObject().put("outbound",n.getJSONObject("outbound"));if(n.has("providerDns"))connection.put("providerDns",n.getJSONArray("providerDns"));result.put(id,connection);
  }
  return result;
 }
 private static String canonical(Object value)throws JSONException {
  if(value instanceof JSONObject){
   JSONObject object=(JSONObject)value;List<String> keys=new ArrayList<>();object.keys().forEachRemaining(keys::add);Collections.sort(keys);
   StringBuilder text=new StringBuilder("{");for(String key:keys)text.append(JSONObject.quote(key)).append(':').append(canonical(object.get(key))).append(',');return text.append('}').toString();
  }
  if(value instanceof JSONArray){JSONArray array=(JSONArray)value;StringBuilder text=new StringBuilder("[");for(int i=0;i<array.length();i++)text.append(canonical(array.get(i))).append(',');return text.append(']').toString();}
  return value instanceof String?JSONObject.quote((String)value):String.valueOf(value);
 }
}
