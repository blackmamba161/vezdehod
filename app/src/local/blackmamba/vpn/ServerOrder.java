package local.blackmamba.vpn;

import java.text.Collator;
import java.util.*;

/** Sort a copy of the visible IDs. A stable tie keeps the subscription order. */
final class ServerOrder {
 static final String KEY="serverSort", PING="ping", NAME="name", SOURCE="source";
 static List<String> sorted(String mode, Map<String,String> names, Map<String,Integer> pings, Collator collator){
  List<String> ids=new ArrayList<>(names.keySet());
  if(NAME.equals(mode))ids.sort((a,b)->collator.compare(names.get(a),names.get(b)));
  else if(!SOURCE.equals(mode))ids.sort(Comparator.comparingLong(id->rank(pings.get(id))));
  return ids;
 }
 private static long rank(Integer ms){return ms==null?Long.MAX_VALUE-1:ms<0?Long.MAX_VALUE:ms.longValue();}
}
