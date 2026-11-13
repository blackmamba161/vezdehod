import local.blackmamba.vpn.*;import org.json.*;import java.nio.file.*;
public class ServerProtocolTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static JSONObject parse(String s)throws Exception{return new JSONObject(s);}
 public static void main(String[] args)throws Exception{
  for(String type:new String[]{"vless","vmess","trojan","shadowsocks","hysteria2","tuic","socks","http","anytls"})check(ServerProtocol.value(parse("{\"type\":\""+type+"\"}")).equals(type),type);
  check(ServerProtocol.label(parse("{\"protocol\":\"vless\",\"outbound\":{\"type\":\"trojan\"}}")).equals("TROJAN"),"Stale badge overrides actual outbound");
  check(ServerProtocol.label(parse("{\"name\":\"VLESS\",\"outbound\":{\"type\":\"xhttp\",\"xray\":{\"protocol\":\"vmess\"}}}")).equals("VMESS"),"Transport mistaken for protocol");
  JSONObject group=parse("{\"type\":\"xray-group\",\"config\":{\"outbounds\":[{\"protocol\":\"freedom\"},{\"protocol\":\"vless\"},{\"protocol\":\"trojan\"},{\"protocol\":\"vless\"},{\"protocol\":\"blackhole\"},{\"protocol\":\"loopback\"}]}}");
  check(ServerProtocol.value(group).equals("vless + trojan"),"Mixed group loses protocols or includes auxiliary routes");
  check(ServerProtocol.label(parse("{\"protocol\":\"vless\"}")).equals("—"),"Unknown definition gets invented VLESS label");
  for(String path:args){JSONArray configs=new JSONArray(Files.readString(Path.of(path)));SubscriptionFormats.Result result=SubscriptionFormats.parse(configs.toString(),"test",(s,p)->null);check(result.skipped==0&&result.nodes.length()==configs.length(),"Provider records dropped");for(int i=0;i<configs.length();i++){JSONObject node=result.nodes.getJSONObject(i);check(node.getString("protocol").equals(ServerProtocol.value(node.getJSONObject("outbound"))),"Import label mismatch");check(ServerProtocol.label(node).equals("VLESS"),"Fixture actual VLESS graph mislabeled");}}
  JSONObject mixed=parse("{\"remarks\":\"Mixed fixture\",\"observatory\":{\"subjectSelector\":[\"vpn\"]},\"outbounds\":[{\"tag\":\"vpn-one\",\"protocol\":\"trojan\",\"settings\":{\"servers\":[{\"address\":\"example.invalid\",\"port\":443,\"password\":\"fixture\"}]}},{\"tag\":\"vpn-two\",\"protocol\":\"vless\",\"settings\":{\"vnext\":[{\"address\":\"example.invalid\",\"port\":443,\"users\":[]}]}}]}");
  JSONObject imported=SubscriptionFormats.parse(mixed.toString(),"mixed",(s,p)->null).nodes.getJSONObject(0);check(imported.getString("protocol").equals("trojan + vless"),"Importer hardcodes mixed group as VLESS");
  check(ServerProtocol.details(parse("{\"outbound\":{\"type\":\"vless\",\"transport\":{\"type\":\"ws\"}}}")).equals("VLESS · WebSocket"),"Native transport");
  check(ServerProtocol.details(parse("{\"outbound\":{\"type\":\"hysteria2\"}}")).equals("HYSTERIA2"),"Invented TCP transport");
  check(ServerProtocol.details(parse("{\"outbound\":{\"type\":\"xray-group\",\"config\":{\"outbounds\":[{\"protocol\":\"vless\",\"streamSettings\":{\"network\":\"grpc\"}},{\"protocol\":\"vless\"},{\"protocol\":\"vless\",\"streamSettings\":{\"network\":\"raw\"}},{\"protocol\":\"trojan\",\"streamSettings\":{\"network\":\"ws\"}},{\"protocol\":\"freedom\"}]}}}")).equals("VLESS · gRPC / TCP + TROJAN · WebSocket"),"Mixed transport association/deduplication");
  if(args.length>1){SubscriptionFormats.Result actual=SubscriptionFormats.parse(Files.readString(Path.of(args[1])),"test",(s,p)->null);for(int i=0;i<actual.nodes.length();i++){JSONObject node=actual.nodes.getJSONObject(i);String expected=i<6?"VLESS · XHTTP":i==6?"VLESS · TCP / gRPC":"VLESS · TCP";check(ServerProtocol.details(node).equals(expected),"Actual fixture transport at "+i+": "+ServerProtocol.details(node));System.out.println(node.optString("name")+": "+ServerProtocol.details(node));}}
  System.out.println("PASS real protocols, mixed graphs, XHTTP transport, stale metadata, unknown configs, provider fixtures");
 }
}
