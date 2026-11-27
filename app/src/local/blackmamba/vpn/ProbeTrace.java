package local.blackmamba.vpn;

import android.util.Log;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** ADB opt-in. Report only fixed categories and counts, never child output. */
final class ProbeTrace {
 private static final String TAG="VezdehodProbe";
 static boolean enabled(){return Log.isLoggable(TAG,Log.DEBUG);}
 static void counts(String stage,int ok,int total,long elapsed){if(enabled())Log.d(TAG,stage+"="+ok+"/"+total+" ms="+elapsed);}
 static void drain(Process child){Thread t=new Thread(()->{
  Map<String,Integer> counts=new TreeMap<>();
  try(BufferedReader in=new BufferedReader(new InputStreamReader(child.getInputStream(),StandardCharsets.UTF_8))){
   for(String line;(line=in.readLine())!=null;){String l=line.toLowerCase(Locale.ROOT);for(String category:new String[]{"no such host","failed to resolve ip","network is unreachable","connection refused","i/o timeout","context deadline exceeded","received real certificate","processed invalid connection","certificate signed by unknown authority","unexpected status","connection reset by peer","no application protocol","no route to host","outbound socket protection denied","failed to dial to dest"})if(l.contains(category))counts.merge(category,1,Integer::sum);}
  }catch(IOException ignored){}finally{if(enabled())Log.d(TAG,"xray="+counts);}
 },"probe-engine-categories");t.setDaemon(true);t.start();}
 private ProbeTrace(){}
}
