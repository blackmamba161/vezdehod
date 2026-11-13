package local.blackmamba.vpn;
import android.app.*;import android.content.*;import android.widget.*;import android.view.*;import org.json.*;import java.util.*;

final class ProfileActions {
 final MainActivity a;final JSONObject profile;
 ProfileActions(MainActivity activity,JSONObject p){a=activity;profile=p;}
 void show(){LinearLayout box=a.vertical();box.setPaddingRelative(a.dp(12),a.dp(4),a.dp(12),0);ScrollView scroll=new ScrollView(a);scroll.setVerticalScrollBarEnabled(false);scroll.addView(box);AlertDialog menu=a.new DialogBuilder(a).setTitle(profile.optString("name")).setView(scroll).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).create();
  action(box,"edit",L10n.s(R.string.profile_edit),()->{menu.dismiss();new ProfileEditor(a,profile).show();});
  action(box,"copy",L10n.s(R.string.msg_a44800a12f),()->{menu.dismiss();new ProfileEditor(a,profile).copySource();});
  action(box,"share",L10n.s(R.string.profile_share),()->{menu.dismiss();Intent send=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,profile.optString("source"));a.startActivity(Intent.createChooser(send,L10n.s(R.string.profile_share)));});
  action(box,"refresh",L10n.s(R.string.msg_ff652348e8),()->{menu.dismiss();a.refresh(profile);});
  action(box,"servers",L10n.s(R.string.msg_c9eee72114),()->{menu.dismiss();a.filter=profile.optString("id");a.tab="servers";a.render();});
  action(box,"trash",L10n.s(R.string.msg_aa4986268e),()->{menu.dismiss();remove();});a.sheet(menu);
 }
 void action(LinearLayout box,String icon,String title,Runnable click){LinearLayout row=a.row();row.setMinimumHeight(a.dp(48));row.setPaddingRelative(a.dp(8),a.dp(4),a.dp(8),a.dp(4));Icons symbol=new Icons(a,icon,icon.equals("trash")?0xffef8181:MainActivity.ACCENT);LinearLayout.LayoutParams spacing=a.lp(22,22);spacing.setMarginEnd(a.dp(16));row.addView(symbol,spacing);row.addView(a.text(title,15,MainActivity.TEXT),new LinearLayout.LayoutParams(0,-2,1));row.setBackground(a.selectable());row.setContentDescription(title);row.setOnClickListener(v->click.run());box.addView(row,a.lp(-1,-2));}
 String usageName(String key){switch(key){case "wifi":return "Wi-Fi";case "cellular":return L10n.s(R.string.msg_6679cdceb0);case "fallbackWifi":return L10n.s(R.string.msg_e83d7ce312);case "fallbackCellular":return L10n.s(R.string.msg_fbf3611c53);case "defaultTarget":return L10n.s(R.string.msg_50e1cb1272);default:return L10n.s(R.string.msg_667d8b44a6)+": "+key.substring(5);}}
 void remove(){try{JSONObject data=a.store.snapshot();String id=profile.getString("id");List<String> usages=ProfileRemoval.usages(data,id);JSONArray available=new JSONArray();for(int i=0;i<data.getJSONArray("servers").length();i++){JSONObject n=data.getJSONArray("servers").getJSONObject(i);if(!id.equals(n.optString("profile")))available.put(n);}
  LinearLayout box=a.vertical();box.setPaddingRelative(a.dp(20),a.dp(4),a.dp(20),a.dp(8));box.addView(a.bold(profile.optString("name"),15));
  if(!usages.isEmpty()){List<String> names=new ArrayList<>();for(String key:usages)names.add(usageName(key));TextView use=a.text(String.join("\n",names),13,MainActivity.MUTED);use.setPadding(0,a.dp(8),0,a.dp(8));box.addView(use);}
  final String[] replacement={""};final boolean clear=!usages.isEmpty()&&available.length()==0;Button choose=null;
  if(!usages.isEmpty()&&!clear){a.note(box,L10n.s(R.string.profile_replace_hint));choose=a.button(L10n.s(R.string.profile_replacement),()->{});box.addView(choose,a.lp(-1,48));}
  if(clear)a.note(box,L10n.s(R.string.profile_remove_last));
  ScrollView scroll=new ScrollView(a);scroll.setVerticalScrollBarEnabled(false);scroll.addView(box);AlertDialog confirm=a.new DialogBuilder(a).setTitle(L10n.s(R.string.msg_6d039299cd)).setView(scroll).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).setPositiveButton(L10n.s(R.string.msg_86ea33aef5),null).create();
  Button selector=choose;confirm.setOnShowListener(v->{enableRemoval(confirm,usages.isEmpty()||clear);confirm.getButton(-1).setOnClickListener(w->{try{a.store.removeProfile(id,replacement[0],clear);confirm.dismiss();if(a.store.data.getJSONArray("servers").length()==0)TunnelService.stop(a);else TunnelService.reload();a.render();}catch(Exception error){a.message(L10n.s(R.string.profile_remove_changed));confirm.dismiss();}});if(selector!=null)selector.setOnClickListener(w->chooseReplacement(available,replacement,selector,confirm));});a.sheet(confirm);
 }catch(Exception e){a.message(L10n.s(R.string.profile_remove_changed));}}
 void enableRemoval(AlertDialog dialog,boolean enabled){dialog.getButton(-1).setEnabled(enabled);dialog.getButton(-1).setAlpha(enabled?1f:.38f);}
 void chooseReplacement(JSONArray nodes,String[] selected,Button label,AlertDialog confirm){LinearLayout list=a.vertical();list.setPaddingRelative(a.dp(16),0,a.dp(16),0);ScrollView scroll=new ScrollView(a);scroll.setVerticalScrollBarEnabled(false);scroll.addView(list);AlertDialog picker=a.new DialogBuilder(a).setTitle(L10n.s(R.string.profile_replacement)).setView(scroll).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).create();for(int i=0;i<nodes.length();i++){JSONObject node=nodes.optJSONObject(i);a.serverChoice(list,node,node.optString("id").equals(selected[0]),()->{selected[0]=node.optString("id");label.setText(node.optString("name")+" · "+a.profileName(node.optString("profile")));enableRemoval(confirm,true);picker.dismiss();});}a.sheet(picker);}
}
