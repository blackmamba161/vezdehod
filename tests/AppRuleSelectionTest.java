import java.util.*;
import org.json.*;
import local.blackmamba.vpn.AppRuleSelection;
import static local.blackmamba.vpn.Store.*;

public class AppRuleSelectionTest {
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  JSONArray original=arr(
   obj("id","a1","type","app","value","com.example.a","label","Custom name","target","direct","network","wifi"),
   obj("id","domain","type","domain_suffix","value","example.com","target","block"),
   obj("id","b1","type","app","value","com.example.b","target","block","enabled",false),
   obj("id","a2","type","app","value","com.example.a","target","block","network","cellular","enabled",false),
   obj("id","c1","type","app","value","com.example.c","enabled",false));
  String before=original.toString();
  check(AppRuleSelection.checked(original).equals(Map.of("com.example.a",true,"com.example.b",false,"com.example.c",false)),"saved checkboxes");
  Map<String,Boolean> changes=new LinkedHashMap<>();
  changes.put("com.example.a",false);changes.put("com.example.b",true);changes.put("com.example.new",true);changes.put("com.example.ignore",false);
  JSONArray after=AppRuleSelection.apply(original,changes,"server:one","cellular",pkg->"New app");
  check(after.length()==6,"no duplicates, no creation of unchecked rules");
  check(after.getJSONObject(0).similar(new JSONObject(original.getJSONObject(0).toString()).put("enabled",false)),"only enable flag changed");
  check(after.getJSONObject(1).similar(original.getJSONObject(1)),"domain rule untouched");
  check(after.getJSONObject(2).similar(new JSONObject(original.getJSONObject(2).toString()).put("enabled",true)),"previous settings retained when re-enabled");
  check(after.getJSONObject(3).similar(original.getJSONObject(3)),"same package and different network stay disabled");
  check(after.getJSONObject(4).similar(original.getJSONObject(4)),"untouched package preserved");
  JSONObject added=after.getJSONObject(5);
  check(added.getString("value").equals("com.example.new")&&added.getString("label").equals("New app")&&added.getString("target").equals("server:one")&&added.getString("network").equals("cellular")&&added.getBoolean("enabled"),"new rule uses editor settings");
  check(original.toString().equals(before),"original unchanged until saved");
  System.out.println("PASS app batch: checked state, add, disable, re-enable, no duplicates, original order and routes retained");
 }
}
