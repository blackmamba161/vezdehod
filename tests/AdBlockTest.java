import org.json.*;import java.util.*;import java.nio.file.*;import local.blackmamba.vpn.*;import static local.blackmamba.vpn.Store.*;
public class AdBlockTest {
 public static void main(String[] args)throws Exception{
  JSONObject d=obj("servers",arr(obj("id","one","outbound",obj("type","socks","server","127.0.0.1","server_port",9))),"wifi","one","cellular","one","auto",true,"defaultTarget","direct","rules",arr(obj("type","app","value","com.example.test","target","auto")),"adBlock",true,"adBlockPath",args[0]);
  String original=d.getJSONArray("rules").toString();
  for(String net:new String[]{"wifi","cellular"}){
   JSONObject c=new JSONObject(Configuration.build(d,net,Map.of(),"").singbox);
   if(!c.getJSONObject("route").getJSONArray("rules").getJSONObject(2).optString("action").equals("reject"))throw new AssertionError("ad blocking must precede app routing");
   if(!c.getJSONObject("dns").getJSONArray("rules").getJSONObject(0).optString("action").equals("reject"))throw new AssertionError("DNS filtering missing");
   Files.writeString(Path.of(args[1]+"-"+net+".json"),c.toString());
  }
  d.put("adBlockAllow","ADS.Example.COM\nads.example.com");
  if(AdBlock.exceptions(d.getString("adBlockAllow")).length()!=1)throw new AssertionError("duplicate exception");
  JSONObject c=new JSONObject(Configuration.build(d,"wifi",Map.of(),"").singbox);
  JSONObject reject=c.getJSONObject("route").getJSONArray("rules").getJSONObject(2);
  if(!reject.getString("mode").equals("and")||!reject.getJSONArray("rules").getJSONObject(1).getBoolean("invert"))throw new AssertionError("exception logic");
  Files.writeString(Path.of(args[1]+"-exceptions.json"),c.toString());
  for(String bad:new String[]{"https://example.com","example.com/path","*.com","example.com:443",".com"}){try{AdBlock.exceptions(bad);throw new AssertionError("accepted invalid exception");}catch(Exception expected){}}
  d.put("adBlock",false);c=new JSONObject(Configuration.build(d,"wifi",Map.of(),"").singbox);
  if(c.getJSONObject("route").has("rule_set")||c.getJSONObject("dns").getJSONArray("rules").length()!=0)throw new AssertionError("off did not restore routing");
  if(!d.getJSONArray("rules").toString().equals(original))throw new AssertionError("user rules mutated");
  System.out.println("PASS both networks; block precedes app routes; DNS block; exception validation; off restores routing; user rules preserved");
 }
}
