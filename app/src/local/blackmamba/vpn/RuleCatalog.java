package local.blackmamba.vpn;

import org.json.*;
import java.util.*;

/** Offline, deliberately scoped service presets. Sources: docs/rule-presets.txt. */
public final class RuleCatalog {
 public static final class Service {
  public final String id,name,section; public final List<String> apps,domains,exact;
  Service(String id,String name,String section,String apps,String domains,String exact){this.id=id;this.name=name;this.section=section;this.apps=words(apps);this.domains=words(domains);this.exact=words(exact);}
  public JSONArray rules() throws JSONException {
   JSONArray out=new JSONArray();
   for(String type:new String[]{"app","domain_suffix","domain"})for(String value:type.equals("app")?apps:type.equals("domain")?exact:domains){
    JSONObject rule=new JSONObject();rule.put("id",UUID.randomUUID().toString()).put("type",type).put("value",value).put("label",type.equals("app")?name:value).put("network","all").put("target","auto").put("enabled",true);out.put(rule);
   }return out;
  }
 }
 private static List<String> words(String s){return s.isEmpty()?Collections.emptyList():Collections.unmodifiableList(Arrays.asList(s.split(" +")));}
 public static final List<Service> SERVICES=Collections.unmodifiableList(Arrays.asList(
  new Service("youtube","YouTube","base","com.google.android.youtube","youtube.com youtu.be youtube-nocookie.com googlevideo.com ytimg.com ggpht.com youtubei.googleapis.com youtube.googleapis.com","yt3.googleusercontent.com"),
  new Service("telegram","Telegram","base","org.telegram.messenger org.telegram.messenger.web","telegram.org t.me telegram.me telegram.dog tdesktop.com telegra.ph telegram-cdn.org cdn-telegram.org telesco.pe",""),
  new Service("instagram","Instagram","base","com.instagram.android","instagram.com cdninstagram.com ig.me igcdn.com",""),
  new Service("facebook","Facebook / Threads","base","com.facebook.katana com.facebook.orca com.instagram.barcelona","facebook.com facebook.net fbcdn.net fb.com fb.me fb.watch messenger.com threads.net threads.com",""),
  new Service("whatsapp","WhatsApp","base","com.whatsapp com.whatsapp.w4b","whatsapp.com whatsapp.net wa.me",""),
  new Service("openai","ChatGPT / Codex","base","com.openai.chatgpt","chatgpt.com openai.com oaistatic.com oaiusercontent.com oaistatsig.com chatgpt.livekit.cloud","challenges.cloudflare.com cdn.openaimerge.com cdn.workos.com forwarder.workos.com frontend-apps-multi-region.workos.com images.workoscdn.com setup.workos.com"),
  new Service("claude","Claude","ai","com.anthropic.claude","claude.ai claude.com anthropic.com claudeusercontent.com",""),
  new Service("gemini","Gemini","ai","com.google.android.apps.bard","gemini.google.com gemini.google gemini.gstatic.com generativelanguage.googleapis.com aistudio.google.com","geminiweb-pa.clients6.google.com geminiweb-pa.clients.google.com robinfrontend-pa.googleapis.com proactivebackend-pa.googleapis.com geller-pa.googleapis.com"),
  new Service("discord","Discord","social","com.discord","discord.com discord.gg discordapp.com discordapp.net discord.media discordcdn.com discord-activities.com","discord-attachments-uploads-prd.storage.googleapis.com"),
  new Service("signal","Signal","social","org.thoughtcrime.securesms","signal.org signal.me signal.link signal.group signal.art signalusers.org whispersystems.org",""),
  new Service("x","X (Twitter)","social","com.twitter.android","x.com twitter.com t.co twimg.com twttr.com","")));
 public static Service find(String id){for(Service s:SERVICES)if(s.id.equals(id))return s;return null;}
 public static String service(JSONObject r){
  String type=r.optString("type"),value=r.optString("value").toLowerCase(Locale.ROOT);
  for(Service s:SERVICES){if(type.equals("app")){if(s.apps.contains(value))return s.id;}else if(type.equals("domain")||type.equals("domain_suffix")){
   for(String domain:s.domains)if(value.equals(domain)||value.endsWith("."+domain))return s.id;
   if(s.exact.contains(value))return s.id;
  }}return "custom";
 }
 public static String sectionName(String id){return id.equals("base")?L10n.s(R.string.msg_eee2285ba1):id.equals("ai")?L10n.s(R.string.msg_16dacca18e):L10n.s(R.string.msg_65ca123b53);}
 public static String title(String id){Service s=find(id);return s==null?L10n.s(R.string.msg_f72ae77771):s.name;}
 private RuleCatalog(){}
}
