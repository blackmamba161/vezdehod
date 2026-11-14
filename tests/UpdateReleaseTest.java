import local.blackmamba.vpn.UpdateRelease;import org.json.*;
public class UpdateReleaseTest {
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 static JSONObject release(long code,boolean beta)throws Exception{return new JSONObject().put("draft",false).put("prerelease",beta).put("body","Release notes").put("assets",new JSONArray().put(new JSONObject().put("name","Vezdehod-0.2.15-"+code+"-arm64-v8a.apk").put("size",1234).put("digest","sha256:"+"a".repeat(64)).put("browser_download_url","https://github.com/example/vezdehod/releases/download/v0.2.15/Vezdehod.apk")));}
 static void reject(JSONObject r,String why)throws Exception{try{UpdateRelease.select(new JSONArray().put(r),"example/vezdehod",true,1);throw new AssertionError(why);}catch(IllegalArgumentException expected){}}
 public static void main(String[] args)throws Exception{
  JSONArray releases=new JSONArray().put(release(19,true)).put(release(17,false)).put(release(18,false));
  check(UpdateRelease.select(releases,"example/vezdehod",false,16).code==18,"Stable selected beta or wrong order");
  check(UpdateRelease.select(releases,"example/vezdehod",true,16).code==19,"Beta missing");
  check(UpdateRelease.select(releases,"example/vezdehod",true,19)==null,"Downgrade offered");
  check(UpdateRelease.select(new JSONArray().put(release(30,false).put("draft",true)),"example/vezdehod",true,1)==null,"Draft offered");
  JSONObject r=release(18,false);r.getJSONArray("assets").getJSONObject(0).put("name","Vezdehod-0.2.15-18-x86_64.apk");check(UpdateRelease.select(new JSONArray().put(r),"example/vezdehod",true,1)==null,"Wrong ABI");
  for(String url:new String[]{"http://github.com/example/vezdehod/releases/download/v1/a.apk","https://github.com.attacker.invalid/example/vezdehod/releases/download/v1/a.apk","https://github.com/other/vezdehod/releases/download/v1/a.apk","https://github.com@example.invalid/example/vezdehod/releases/download/v1/a.apk","https://github.com/example/vezdehod/releases/download/../a.apk"}){r=release(18,false);r.getJSONArray("assets").getJSONObject(0).put("browser_download_url",url);reject(r,"Untrusted URL");}
  r=release(18,false);r.getJSONArray("assets").getJSONObject(0).put("digest",JSONObject.NULL);reject(r,"Missing checksum");
  r=release(18,false);r.getJSONArray("assets").getJSONObject(0).put("size",UpdateRelease.MAX_APK+1);reject(r,"Oversize");
  check(!UpdateRelease.repositoryValid("example/../secret")&&!UpdateRelease.repositoryValid(""),"Unsafe repository");
  JSONObject mixed=release(24,false);JSONObject nativeAsset=mixed.getJSONArray("assets").getJSONObject(0),universal=new JSONObject(nativeAsset.toString()).put("name","Vezdehod-0.2.15-24-universal.apk");mixed.getJSONArray("assets").put(universal);
  check(UpdateRelease.select(new JSONArray().put(mixed),"example/vezdehod",false,22,new String[]{"armeabi-v7a"}).name.endsWith("-universal.apk"),"ARMv7 offered ARM64-only APK");
  check(UpdateRelease.select(new JSONArray().put(mixed),"example/vezdehod",false,22,new String[]{"arm64-v8a","armeabi-v7a"}).name.endsWith("-arm64-v8a.apk"),"ARM64 did not prefer smaller native asset");
  check(UpdateRelease.select(new JSONArray().put(release(24,false)),"example/vezdehod",false,22,new String[]{"armeabi-v7a"})==null,"Incompatible legacy APK offered to ARMv7");
  mixed.put("assets",new JSONArray().put(universal).put(nativeAsset));check(UpdateRelease.select(new JSONArray().put(mixed),"example/vezdehod",false,22,new String[]{"arm64-v8a"}).name.endsWith("-arm64-v8a.apk"),"Asset ordering changed architecture preference");
  System.out.println("PASS stable/beta, unordered releases, drafts, downgrade, ABI, origin, checksum, size and repository validation");
 }
}
