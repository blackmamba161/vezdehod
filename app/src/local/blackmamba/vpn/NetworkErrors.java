package local.blackmamba.vpn;

import android.util.Log;
import java.util.*;

/** Bounded diagnostic categories only: never log addresses, URLs or credentials. */
final class NetworkErrors {
 private static final Map<String,Long> recent=new LinkedHashMap<>();
 static String category(String message){
  String text=message==null?"":message.toLowerCase(Locale.ROOT);List<String> result=new ArrayList<>();
  String[][] markers={{"reality","reality"},{"vision","vision"},{"tls","tls"},{"certificate","certificate"},{"handshake","handshake"},{"dns","dns"},{"aaaa","dns-aaaa"},{"exchange4","dns-a"},{"rcode","dns-rcode"},{"rejected","rejected"},{"sniff","sniff"},{"route","route"},{"inbound","inbound"},{"outbound","outbound"},{"connection owner","uid-lookup"},{"quic","quic"},{"buffer","buffer"},{"fragment","fragment"},{"packet","packet"},{"resolve","resolver"},{"no such host","host-not-found"},{"timeout","timeout"},{"deadline","deadline"},{"reset","reset"},{"refused","refused"},{"connection refused","connection-refused"},{"127.0.0.1","loopback"},{"[::1]","loopback6"},{"172.19.0.2","vpn-dns"},{":53","dns-port"},{"udp","udp"},{"tcp","tcp"},{"unreachable","unreachable"},{"eof","eof"},{"closed","closed"},{"canceled","cancelled"},{"protect","socket-protection"},{"bind","socket-binding"},{"permission","permission"},{"network changed","network-changed"},{"protocol version","protocol-version"},{"authentication","authentication"},{"invalid","invalid"},{"unsupported","unsupported"},{"probe http","http-status"}};
  for(String[] marker:markers)if(text.contains(marker[0]))result.add(marker[1]);
  return result.isEmpty()?"other":String.join("/",result);
 }
 static synchronized void record(String stage,String node,String message){
  String code=category(message),key=stage+":"+Integer.toHexString(node.hashCode())+":"+code;long now=android.os.SystemClock.elapsedRealtime();
  if(now-recent.getOrDefault(key,-10000L)<10000)return;
  if(recent.size()>=64)recent.remove(recent.keySet().iterator().next());recent.put(key,now);
  Log.w("VezdehodNet",key);
 }
}
