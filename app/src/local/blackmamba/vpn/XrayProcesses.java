package local.blackmamba.vpn;
import android.content.Context;import java.io.*;import java.util.*;import java.util.concurrent.TimeUnit;
/** Starts only groups actually selected by a route; unused provider groups consume no process. */
final class XrayProcesses implements AutoCloseable {
 final Context context;final Map<String,String> configs;final Map<String,Process> running=new HashMap<>();final List<File> files=new ArrayList<>();boolean closed;
 XrayProcesses(Context c,Map<String,String> configs){context=c;this.configs=configs;}
 synchronized void ensure(String id)throws Exception{if(!configs.containsKey(id))return;if(closed)throw new IOException("Closed group runtime");Process old=running.get(id);if(old!=null){if(!old.isAlive())throw new IOException("Group runtime stopped");return;}File f=File.createTempFile("vpn-group-",".json",context.getCacheDir());files.add(f);RuntimeSecrets.write(f,configs.get(id));Process p=new ProcessBuilder(context.getApplicationInfo().nativeLibraryDir+"/libxray.so","run","-config",f.getPath()).redirectErrorStream(true).redirectOutput(new File("/dev/null")).start();running.put(id,p);if(p.waitFor(120,TimeUnit.MILLISECONDS))throw new IOException("Group runtime failed");}
 synchronized void release(String id){Process p=running.remove(id);if(p!=null){p.destroy();try{if(!p.waitFor(300,TimeUnit.MILLISECONDS))p.destroyForcibly();}catch(InterruptedException e){p.destroyForcibly();Thread.currentThread().interrupt();}}}
 synchronized void check()throws IOException{for(Process p:running.values())if(!p.isAlive())throw new IOException("Group runtime stopped");}
 public synchronized void close(){closed=true;for(Process p:running.values())p.destroy();for(Process p:running.values())try{if(!p.waitFor(500,TimeUnit.MILLISECONDS))p.destroyForcibly();}catch(InterruptedException e){p.destroyForcibly();Thread.currentThread().interrupt();}running.clear();for(File f:files)f.delete();files.clear();}
}
