package local.blackmamba.vpn;
import android.app.*;import org.json.*;

final class SecurityReview {
 static void confirm(MainActivity a,JSONObject result,Runnable accepted){try{JSONObject profile=result.getJSONObject("profile");JSONArray nodes=result.getJSONArray("servers");String fingerprint=SubscriptionSecurity.fingerprint(nodes);JSONObject current=Store.find(a.store.data.optJSONArray("profiles"),profile.optString("id"));if(fingerprint.isEmpty()||current!=null&&current.optString("source").equals(profile.optString("source"))&&fingerprint.equals(current.optString("securityAck"))){profile.put("securityAck",fingerprint);accepted.run();return;}
  int flags=0;for(int i=0;i<nodes.length();i++)flags|=SubscriptionSecurity.flags(nodes.getJSONObject(i));StringBuilder message=new StringBuilder();if((flags&SubscriptionSecurity.CERTIFICATE)!=0)message.append(L10n.s(R.string.security_certificate));if((flags&SubscriptionSecurity.PLAINTEXT)!=0){if(message.length()>0)message.append("\n\n");message.append(L10n.s(R.string.security_plaintext));}
  AlertDialog d=a.new DialogBuilder(a).setTitle(L10n.s(R.string.security_import_title)).setMessage(message.toString()).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).setPositiveButton(L10n.s(R.string.msg_3f75368ab9),(dialog,w)->{try{profile.put("securityAck",fingerprint);accepted.run();}catch(Exception e){a.message(L10n.s(R.string.msg_3d734741a7));}}).create();a.sheet(d);
 }catch(Exception e){a.message(L10n.s(R.string.msg_3d734741a7));}}
}
