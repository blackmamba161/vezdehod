package local.blackmamba.vpn;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import io.nekohasekai.libbox.Libbox;
import static local.blackmamba.vpn.Store.*;

/** Checks imported definitions with the shipped engines before saving them. Does not start a tunnel. */
final class SubscriptionValidator {
 static void validate(Context context,JSONObject result)throws Exception{
  JSONArray nodes=result.getJSONArray("servers");String first=nodes.getJSONObject(0).getString("id");
  JSONObject data=obj("servers",nodes,"rules",new JSONArray(),"wifi",first,"cellular",first,"auto",true,"defaultTarget","direct");
  Map<String,Integer> ports=new HashMap<>();for(int i=0;i<nodes.length();i++)ports.put(nodes.getJSONObject(i).getString("id"),23000+i);
  Configuration.Result config=Configuration.build(data,"wifi",ports,UUID.randomUUID().toString());
  try{Libbox.checkConfig(config.singbox);}catch(Exception e){throw new Exception(L10n.s(R.string.msg_cde2c9f8b9));}
  java.util.List<String> definitions=new ArrayList<>(config.groups.values());if(config.xray!=null)definitions.add(config.xray);for(String definition:definitions){
  File file=File.createTempFile("subscription-check-",".json",context.getCacheDir());Process process=null;
  try{
   RuntimeSecrets.write(file,definition);
   process=new ProcessBuilder(context.getApplicationInfo().nativeLibraryDir+"/libxray.so","run","-test","-config",file.getPath()).redirectErrorStream(true).redirectOutput(new File("/dev/null")).start();
   if(!process.waitFor(10,TimeUnit.SECONDS))throw new Exception(L10n.s(R.string.msg_a734112137));
   if(process.exitValue()!=0)throw new Exception(L10n.s(R.string.msg_3d53cd3ab3));
  }finally{if(process!=null&&process.isAlive())process.destroyForcibly();file.delete();}
  }
 }
}
