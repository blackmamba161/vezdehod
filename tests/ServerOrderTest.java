package local.blackmamba.vpn;

import java.text.Collator;
import java.util.*;

public class ServerOrderTest {
 static void check(List<String> actual,String... expected){if(!actual.equals(Arrays.asList(expected)))throw new AssertionError(actual);}
 public static void main(String[] args)throws Exception{
  Map<String,String> names=new LinkedHashMap<>();names.put("a","Zulu");names.put("b","Alpha");names.put("c","Bravo");names.put("d","Charlie");
  Map<String,Integer> ms=new HashMap<>();Collator collator=Collator.getInstance(Locale.ENGLISH);
  check(ServerOrder.sorted(ServerOrder.PING,names,ms,collator),"a","b","c","d");
  ms.put("a",-1);ms.put("c",30);check(ServerOrder.sorted(ServerOrder.PING,names,ms,collator),"c","b","d","a");
  ms.put("b",30);ms.put("d",0);check(ServerOrder.sorted(ServerOrder.PING,names,ms,collator),"d","b","c","a");
  ms.put("a",4);check(ServerOrder.sorted(ServerOrder.PING,names,ms,collator),"d","a","b","c");
  check(ServerOrder.sorted(ServerOrder.NAME,names,ms,collator),"b","c","d","a");
  check(ServerOrder.sorted(ServerOrder.SOURCE,names,ms,collator),"a","b","c","d");
  if(!ServerOrder.PING.equals(Preferences.defaults().getString(ServerOrder.KEY)))throw new AssertionError("default order");
  System.out.println("PASS live ping order, stable ties, pending/failed nodes, name and source order");
 }
}
