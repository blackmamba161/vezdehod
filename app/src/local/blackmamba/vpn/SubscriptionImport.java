package local.blackmamba.vpn;
import android.app.*;import android.content.*;import android.widget.*;import org.json.*;import java.util.UUID;

/** User-triggered import. No clipboard polling and no configuration writes until confirmation. */
final class SubscriptionImport {
 final MainActivity a;
 SubscriptionImport(MainActivity a){this.a=a;}
 void choose(){LinearLayout box=a.vertical();box.setPaddingRelative(a.dp(16),a.dp(4),a.dp(16),0);AlertDialog d=a.new DialogBuilder(a).setTitle(L10n.s(R.string.msg_46f4fe51cc)).setView(box).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).create();
  a.serverAction(box,"qr",L10n.s(R.string.msg_f91f5592a9),null,()->{d.dismiss();a.startActivityForResult(new Intent(a,QrScannerActivity.class),41);});
  a.serverAction(box,"clipboard",L10n.s(R.string.msg_19a5f8e5b8),null,()->{ClipboardManager clipboard=a.getSystemService(ClipboardManager.class);ClipData clip=clipboard.getPrimaryClip();if(clip==null||clip.getItemCount()==0){a.message(L10n.s(R.string.msg_a30b87ef8c));return;}CharSequence text=clip.getItemAt(0).getText();if(text==null){a.message(L10n.s(R.string.msg_2a1dd82994));return;}d.dismiss();enter(text.toString());});
  a.serverAction(box,"link",L10n.s(R.string.msg_bb84bedbe2),null,()->{d.dismiss();enter("");});a.sheet(d);
 }
 void enter(String value){
  LinearLayout box=a.vertical();box.setPaddingRelative(a.dp(12),0,a.dp(12),0);EditText input=a.edit("https://…",value);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);input.setTextDirection(android.view.View.TEXT_DIRECTION_LTR);input.setSingleLine(false);input.setMinLines(2);input.setMaxLines(3);input.setTextSize(14*a.uiScale);box.addView(input);
  TextView error=a.text("",13,MainActivity.RED);error.setPaddingRelative(a.dp(6),a.dp(6),a.dp(6),a.dp(4));error.setVisibility(android.view.View.GONE);box.addView(error);
  ScrollView scroll=new ScrollView(a);scroll.setVerticalScrollBarEnabled(false);scroll.addView(box);
  AlertDialog d=a.new DialogBuilder(a).setTitle(L10n.s(R.string.msg_ccb6cb6834)).setView(scroll).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).setPositiveButton(L10n.s(R.string.msg_3f75368ab9),null).create();
  d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{try{
   String source=ImportSource.normalize(input.getText().toString());checkDuplicate(source);error.setVisibility(android.view.View.GONE);input.setEnabled(false);d.getButton(-1).setEnabled(false);d.getButton(-1).setText(L10n.s(R.string.msg_1114ae9044));a.getSystemService(android.view.inputmethod.InputMethodManager.class).hideSoftInputFromWindow(input.getWindowToken(),0);
   String type=ImportSource.subscription(source)?"subscription":"key";String name=type.equals("subscription")?new java.net.URI(source).getHost():"VPN";JSONObject profile=Store.obj("id",UUID.randomUUID().toString(),"name",name,"type",type,"source",source,"auto",true);
   a.io.execute(()->{try{JSONObject result=Subscriptions.fetch(profile,a);a.main.post(()->{if(!d.isShowing()||a.isDestroyed())return;d.dismiss();confirm(result);});}catch(Exception e){a.main.post(()->{if(!d.isShowing()||a.isDestroyed())return;input.setEnabled(true);d.getButton(-1).setEnabled(true);d.getButton(-1).setText(L10n.s(R.string.msg_9e506acb19));error.setText(TunnelService.safeError(e));error.setVisibility(android.view.View.VISIBLE);});}});
  }catch(Exception e){error.setText(e.getMessage());error.setVisibility(android.view.View.VISIBLE);}}));a.sheet(d);
 }
 void checkDuplicate(String source)throws Exception{JSONArray profiles=a.store.snapshot().getJSONArray("profiles");for(int i=0;i<profiles.length();i++)if(source.equals(profiles.getJSONObject(i).optString("source")))throw new Exception(L10n.s(R.string.msg_1a69e59ba3));}
 void confirm(JSONObject result){SecurityReview.confirm(a,result,()->confirmReviewed(result));}
 void confirmReviewed(JSONObject result){try{
  JSONObject profile=result.getJSONObject("profile");JSONArray servers=result.getJSONArray("servers");if(profile.optString("type").equals("key")&&servers.length()==1)profile.put("name",servers.getJSONObject(0).optString("name","VPN"));
  LinearLayout box=a.vertical();box.setPaddingRelative(a.dp(20),a.dp(4),a.dp(20),a.dp(4));a.label(box,L10n.s(R.string.msg_3de49828e8));EditText name=a.edit(L10n.s(R.string.msg_11b385895c),profile.optString("name"));box.addView(name);a.note(box,L10n.s(R.string.msg_92ce2819af)+servers.length()+" · "+result.optString("format",L10n.s(R.string.msg_e8d9cd7b4e)));
  if(result.optInt("skipped")>0){LinearLayout warning=a.row();TextView summary=a.text(L10n.s(R.string.msg_06bb7e72a9)+result.optInt("skipped"),12,MainActivity.RED);warning.addView(summary,new LinearLayout.LayoutParams(0,-2,1));JSONArray details=result.optJSONArray("warnings");StringBuilder reason=new StringBuilder();if(details!=null)for(int i=0;i<details.length();i++)reason.append(details.optString(i)).append("\n");warning.addView(a.helpButton(L10n.s(R.string.msg_44548e3dc4),reason.toString().trim()),a.lp(44,44));box.addView(warning);}
  if(profile.optLong("expire")>0)a.note(box,L10n.s(R.string.msg_2c285a5142)+a.date(profile.optLong("expire")));
  AlertDialog d=a.new DialogBuilder(a).setTitle(L10n.s(R.string.msg_46f4fe51cc)).setView(box).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).setPositiveButton(L10n.s(R.string.msg_559a87f7cc),null).create();d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{String label=name.getText().toString().trim();if(label.isEmpty()){name.setError(L10n.s(R.string.msg_0af444c6af));return;}try{checkDuplicate(profile.optString("source"));profile.put("name",label);a.store.putProfile(profile,servers);d.dismiss();TunnelService.reload();a.tab="profiles";a.render();}catch(Exception e){a.message(TunnelService.safeError(e));}}));a.sheet(d);
 }catch(Exception e){a.message(L10n.s(R.string.msg_fb701daed8));}}
}
