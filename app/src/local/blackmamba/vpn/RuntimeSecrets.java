package local.blackmamba.vpn;
import android.content.Context;import android.system.Os;import java.io.*;import java.nio.file.Files;

/** Only generated engine definitions; never deletes subscriptions or encrypted backups. */
final class RuntimeSecrets {
 static void write(File file,String json)throws Exception{if(!file.exists()&&!file.createNewFile())throw new IOException("Cannot create private configuration");Os.chmod(file.getPath(),0600);Files.writeString(file.toPath(),json);}
 static void cleanup(Context context){new File(context.getFilesDir(),"xray.json").delete();for(File dir:new File[]{context.getFilesDir(),context.getCacheDir()}){File[] files=dir.listFiles();if(files==null)continue;for(File f:files)if(f.isFile()&&(f.getName().startsWith("vpn-group-")||f.getName().startsWith("subscription-check-"))&&f.getName().endsWith(".json"))f.delete();}}
}
