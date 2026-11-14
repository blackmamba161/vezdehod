package local.blackmamba.vpn;
import android.content.Context;import java.io.*;import java.nio.file.*;import java.text.SimpleDateFormat;import java.util.*;
/** Only controlled event messages, never core logs, hosts, subscription URLs or credentials. */
public final class EventLog {
 public static synchronized void add(Context c,String event){try{File f=new File(c.getFilesDir(),"events.txt");String old=f.exists()?Compat.readString(f.toPath()):"";String[] lines=old.split("\n");StringBuilder b=new StringBuilder();for(int i=Math.max(0,lines.length-99);i<lines.length;i++)if(!lines[i].isEmpty())b.append(lines[i]).append('\n');b.append(new SimpleDateFormat("dd.MM HH:mm:ss",Locale.ROOT).format(new Date())).append(" · ").append(event).append('\n');Compat.writeString(f.toPath(),b.toString());}catch(IOException ignored){}}
 public static synchronized String read(Context c){try{File f=new File(c.getFilesDir(),"events.txt");return f.exists()?Compat.readString(f.toPath()):L10n.s(R.string.msg_33f32d875b);}catch(IOException e){return L10n.s(R.string.msg_dd624657c4);}}
 public static synchronized void clear(Context c){new File(c.getFilesDir(),"events.txt").delete();}
}
