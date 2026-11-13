package local.blackmamba.vpn;
import android.app.*;import android.content.*;import android.graphics.Typeface;import android.provider.Settings;import android.text.InputType;import android.view.*;import android.widget.*;import org.json.*;

final class SettingsScreen {
 final MainActivity a;final Store store;final LinearLayout host;
 SettingsScreen(MainActivity activity){a=activity;store=a.store;host=a.content;}
 void draw(String page){
  if(!page.equals("main")){TextView back=a.text(L10n.s(R.string.msg_06b9e865d0),13,MainActivity.ACCENT);back.setMinHeight(a.dp(42));back.setOnClickListener(v->open("main"));host.addView(back);}
  switch(page){
   case "updates":a.updateScreen=new UpdateScreen(a);a.updateScreen.draw();break;case "logs":logs();break;case "adblock":adblock();break;case "notifications":notifications();break;case "appearance":appearance();break;case "connection":connection();break;case "advanced":advanced();break;case "diagnostics":diagnostics();break;
   default:a.title(L10n.s(R.string.msg_7f17c7c62a),null);LinearLayout c=a.card(host);
    a.iconItem(c,"shield",L10n.s(R.string.msg_7f2be1ba3d),store.data.optBoolean("adBlock")?L10n.s(R.string.msg_85dd4d46ca):L10n.s(R.string.msg_2106134f75),()->open("adblock"));
    a.iconItem(c,"appearance",L10n.s(R.string.msg_d206f1bed0),null,()->open("appearance"));
    a.iconItem(c,"language",L10n.s(R.string.msg_ed3f0e507a),languageName(),()->language());
    a.iconItem(c,"networks",L10n.s(R.string.msg_79e350f743),null,()->open("connection"));
    a.iconItem(c,"rules",L10n.s(R.string.msg_667d8b44a6),null,()->{a.tab="rules";a.render();});
    a.iconItem(c,"bell",L10n.s(R.string.msg_c7e6bf5234),null,()->open("notifications"));
    a.iconItem(c,"settings",L10n.s(R.string.msg_d7907f4d43),"DNS, IP, MTU",()->open("advanced"));
    a.iconItem(c,"diagnostics",L10n.s(R.string.msg_9ba1e22ac3),null,()->open("diagnostics"));
    a.iconItem(c,"download",L10n.s(R.string.update_title),null,()->open("updates"));
    try{a.note(host,L10n.s(R.string.msg_a84ab2656e)+a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName);}catch(Exception ignored){}
  }
 }
 void open(String page){a.settingsPage=page;a.tab="settings";a.render();}
 void set(String key,Object value,boolean core){try{store.change(key,value);EventLog.add(a,core?L10n.s(R.string.msg_6521dc9632):L10n.s(R.string.msg_7f59da61ba));if(core)TunnelService.reload();a.render();}catch(Exception e){a.message(TunnelService.safeError(e));}}
 void toggle(LinearLayout parent,String name,String hint,String key,boolean core){LinearLayout row=a.row(),labels=a.vertical();labels.addView(a.bold(name,14));row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));if(!hint.isEmpty())row.addView(a.helpButton(name,hint),a.lp(44,44));Switch sw=a.switchControl();sw.setContentDescription(name);sw.setChecked(store.data.optBoolean(key));sw.setOnCheckedChangeListener((b,on)->set(key,on,core));row.addView(sw);row.setPaddingRelative(0,a.dp(4),0,a.dp(4));parent.addView(row);a.divider(parent);}
 void choice(LinearLayout p,String title,String key,String[] labels,Object[] values,boolean core){choice(p,title,key,labels,values,core,null);}
 void choice(LinearLayout p,String title,String key,String[] labels,Object[] values,boolean core,String help){int selected=0;Object current=store.data.opt(key);for(int i=0;i<values.length;i++)if(values[i].toString().equals(String.valueOf(current)))selected=i;final int selection=selected;a.item(p,title,labels[selected],help,()->{AlertDialog d=new AlertDialog.Builder(a).setTitle(title).setSingleChoiceItems(labels,selection,(dialog,w)->{dialog.dismiss();set(key,values[w],core);}).setNegativeButton(L10n.s(R.string.msg_4ae50d3073),null).create();a.sheet(d);});}
 void adblock(){a.title(L10n.s(R.string.msg_7f2be1ba3d),L10n.s(R.string.msg_41a17b3bad));LinearLayout c=a.card(host);
  toggle(c,L10n.s(R.string.msg_d6d24ec981),L10n.s(R.string.msg_85e3709119),"adBlock",true);
  a.note(c,"HaGeZi Light · 12.11.2026");
  String exceptions=store.data.optString("adBlockAllow");int count=exceptions.isEmpty()?0:exceptions.split("\n").length;
  a.item(c,L10n.s(R.string.msg_ea65534c79),count==0?L10n.s(R.string.msg_f82a821941):count+L10n.s(R.string.msg_449885f5fe),L10n.s(R.string.msg_0f97e2780c),()->editAdExceptions());
 }
 void editAdExceptions(){LinearLayout box=a.vertical();box.setPaddingRelative(a.dp(20),a.dp(12),a.dp(20),a.dp(8));a.note(box,L10n.s(R.string.msg_2923b8f670));EditText input=a.edit("example.com",store.data.optString("adBlockAllow"));input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);input.setMinLines(3);input.setMaxLines(6);box.addView(input);
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle(L10n.s(R.string.msg_494d7c7be7)).setView(box).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).setPositiveButton(L10n.s(R.string.msg_4864057d62),null).create();dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(w->{try{org.json.JSONArray names=AdBlock.exceptions(input.getText().toString());StringBuilder text=new StringBuilder();for(int i=0;i<names.length();i++){if(i>0)text.append('\n');text.append(names.getString(i));}store.change("adBlockAllow",text.toString());TunnelService.reload();dialog.dismiss();a.render();}catch(Exception e){input.setError(e.getMessage());}}));a.sheet(dialog);
 }
 String languageName(){String selected=L10n.selection(a);if(selected.isEmpty())return L10n.s(R.string.msg_35d57a900e);for(int i=1;i<L10n.TAGS.length;i++)if(L10n.TAGS[i].equals(selected))return L10n.NAMES[i];return java.util.Locale.forLanguageTag(selected).getDisplayName();}
 void language(){String[] labels=L10n.NAMES.clone();labels[0]=L10n.s(R.string.msg_35d57a900e);String selected=L10n.selection(a);int index=a.index(L10n.TAGS,selected);AlertDialog d=a.new DialogBuilder(a).setTitle(L10n.s(R.string.msg_ed3f0e507a)).setSingleChoiceItems(labels,index,(dialog,which)->{dialog.dismiss();L10n.select(a,L10n.TAGS[which]);if(android.os.Build.VERSION.SDK_INT<33)a.recreate();}).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).create();a.sheet(d);}
 void appearance(){a.title(L10n.s(R.string.msg_d206f1bed0),null);LinearLayout c=a.card(host);
  choice(c,L10n.s(R.string.msg_553c45f1b8),"theme",new String[]{L10n.s(R.string.msg_bd16b23470),L10n.s(R.string.msg_8080010c5e),L10n.s(R.string.msg_f0a22d9d1a)},new Object[]{"dark","light","system"},false);
  choice(c,L10n.s(R.string.msg_70f0229afd),"accent",new String[]{L10n.s(R.string.msg_9f1325f197),L10n.s(R.string.msg_891c5e9e76),L10n.s(R.string.msg_0deebc3a96)},new Object[]{"gold","mint","blue"},false);
  choice(c,L10n.s(R.string.msg_a4617199a8),"textScale",new String[]{L10n.s(R.string.msg_4886f1af13),L10n.s(R.string.msg_e7e285a436),L10n.s(R.string.msg_52bac55f34),L10n.s(R.string.msg_d8fcb99153)},new Object[]{90,100,110,120},false);
  a.gap(host,4);host.addView(a.bold(L10n.s(R.string.msg_11bb1b11c0),19));c=a.card(host);
  toggle(c,L10n.s(R.string.msg_220afdba73),"","showPing",false);toggle(c,L10n.s(R.string.msg_917e05143a),"","showServer",false);toggle(c,L10n.s(R.string.msg_aff5818940),"","showExpiry",false);
 }
 boolean notificationVisible(){NotificationManager nm=a.getSystemService(NotificationManager.class);NotificationChannel channel=nm.getNotificationChannel("vpn");return nm.areNotificationsEnabled()&&(channel==null||channel.getImportance()!=NotificationManager.IMPORTANCE_NONE);}
 Intent notificationSettings(){NotificationManager nm=a.getSystemService(NotificationManager.class);if(!nm.areNotificationsEnabled()||nm.getNotificationChannel("vpn")==null)return new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,a.getPackageName());return new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,a.getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,"vpn");}
 void notifications(){a.title(L10n.s(R.string.msg_f56353b7eb),null);LinearLayout c=a.card(host);a.section(c,L10n.s(R.string.msg_64776d5102),L10n.s(R.string.msg_6ba4db3033));a.note(c,notificationVisible()?L10n.s(R.string.msg_4e73aa66da):L10n.s(R.string.msg_f1d3c27f2c));c.addView(a.button(L10n.s(R.string.msg_8960ddc30e),()->{try{a.startActivity(notificationSettings());}catch(ActivityNotFoundException e){a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+a.getPackageName())));}}),a.lp(-1,48));
  c=a.card(host);a.section(c,L10n.s(R.string.msg_be99ac8f13),L10n.s(R.string.msg_e57f3249b4));a.note(c,L10n.s(R.string.msg_136e1c4c3c));
 }
 void connection(){a.title(L10n.s(R.string.msg_79e350f743),L10n.s(R.string.msg_9d22ff847e));LinearLayout c=a.card(host);
  toggle(c,L10n.s(R.string.msg_fcb4b47313),L10n.s(R.string.msg_2be12d2d73),"boot",false);
  toggle(c,L10n.s(R.string.msg_a8e2cfb793),"","reconnect",false);
  choice(c,L10n.s(R.string.msg_448e226084),"retrySeconds",new String[]{L10n.s(R.string.msg_8c21ba2d9d),L10n.s(R.string.msg_8a45d0d1ad),L10n.s(R.string.msg_d84f514695),L10n.s(R.string.msg_7db1478f63)},new Object[]{5,15,30,60},false);
  fallback(c,L10n.s(R.string.msg_e83d7ce312),"fallbackWifi");fallback(c,L10n.s(R.string.msg_fbf3611c53),"fallbackCellular");

  a.item(host,L10n.s(R.string.msg_ca20e76c0c),L10n.s(R.string.msg_d26aec38b1),()->a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+a.getPackageName()))));
  a.item(host,L10n.s(R.string.msg_a9d9fc2d46),L10n.s(R.string.msg_8a6ceef063),()->a.startActivity(new Intent(Settings.ACTION_VPN_SETTINGS)));
 }
 void fallback(LinearLayout c,String title,String key){String id=store.data.optString(key);a.item(c,title,id.isEmpty()?L10n.s(R.string.msg_2d8f7570e1):a.serverName(id),()->{JSONArray nodes=store.data.optJSONArray("servers");String[] labels=new String[nodes.length()+1],ids=new String[labels.length];labels[0]=L10n.s(R.string.msg_2d8f7570e1);ids[0]="";for(int i=0;i<nodes.length();i++){JSONObject n=nodes.optJSONObject(i);labels[i+1]=n.optString("name")+" · "+a.profileName(n.optString("profile"));ids[i+1]=n.optString("id");}AlertDialog d=new AlertDialog.Builder(a).setTitle(title).setItems(labels,(dialog,w)->set(key,ids[w],true)).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).create();a.sheet(d);});}
 void advanced(){
  a.title(L10n.s(R.string.msg_d7907f4d43),"");LinearLayout c=a.card(host);
  TunnelService tunnel=TunnelService.instance;String now=tunnel==null?L10n.s(R.string.msg_c8243322dc):tunnel.nativeIpv6()?"IPv4 + IPv6":"IPv4";
  choice(c,L10n.s(R.string.msg_e0cf01e43c),"ipv6",new String[]{L10n.s(R.string.msg_e6da72edd6)+now,L10n.s(R.string.msg_5b9e24bda9)},new Object[]{true,false},true,L10n.s(R.string.msg_9be84e1d9f));
  a.item(c,L10n.s(R.string.msg_8f9bf25ca6),store.data.optString("remoteDns"),L10n.s(R.string.msg_5f1326b514),()->dnsSettings());
  a.item(c,"MTU",store.data.optInt("mtu",1400)+L10n.s(R.string.msg_0857ba97ab),L10n.s(R.string.msg_466019b718),()->mtuSettings());
  choice(c,L10n.s(R.string.msg_7e47a7ce78),"stack",new String[]{"gVisor",L10n.s(R.string.msg_eca17171d2),L10n.s(R.string.msg_d155047e55)},new Object[]{"gvisor","system","mixed"},true,L10n.s(R.string.msg_d4dbd88958));
  a.item(c,L10n.s(R.string.msg_e04a016600),null,()->{AlertDialog d=new AlertDialog.Builder(a).setTitle(L10n.s(R.string.msg_f1b8fc3ba0)).setMessage(L10n.s(R.string.msg_478ced019a)).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).setPositiveButton(L10n.s(R.string.msg_058f162d29),(v,w)->{try{store.resetNetworkPreferences();TunnelService.reload();a.render();}catch(Exception e){a.message(TunnelService.safeError(e));}}).create();a.sheet(d);});
 }
 void dnsSettings(){
  LinearLayout c=a.vertical();c.setPaddingRelative(a.dp(20),a.dp(12),a.dp(20),a.dp(8));

  EditText field=a.edit("https://…/dns-query",store.data.optString("remoteDns"));field.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);field.setTextDirection(android.view.View.TEXT_DIRECTION_LTR);field.setSingleLine(false);field.setMaxLines(3);c.addView(field);
  c.addView(a.button(L10n.s(R.string.msg_6a38870f87),()->field.setText("https://1.1.1.1/dns-query")),a.lp(-1,48));
  AlertDialog d=new AlertDialog.Builder(a).setTitle(L10n.s(R.string.msg_8f9bf25ca6)).setView(c).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).setPositiveButton(L10n.s(R.string.msg_4864057d62),null).create();
  d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{try{String value=field.getText().toString().trim();Preferences.validate("remoteDns",value);store.change("remoteDns",value);TunnelService.reload();d.dismiss();a.render();}catch(Exception e){field.setError(e.getMessage());}}));a.sheet(d);
 }
 void mtuSettings(){
  LinearLayout c=a.vertical();c.setPaddingRelative(a.dp(20),a.dp(12),a.dp(20),a.dp(8));
  EditText field=a.edit("MTU",Integer.toString(store.data.optInt("mtu",1400)));field.setInputType(InputType.TYPE_CLASS_NUMBER);c.addView(field);
  LinearLayout presets=a.row();Button normal=a.button(L10n.s(R.string.msg_8fb98f9cdd),()->field.setText("1400")),small=a.button(L10n.s(R.string.msg_b826985870),()->field.setText("1280"));presets.addView(normal,new LinearLayout.LayoutParams(0,a.dp(52),1));LinearLayout.LayoutParams smallParams=new LinearLayout.LayoutParams(0,a.dp(52),1);smallParams.setMarginStart(a.dp(8));presets.addView(small,smallParams);c.addView(presets);
  AlertDialog d=new AlertDialog.Builder(a).setTitle("MTU · 1280–1500").setView(c).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).setPositiveButton(L10n.s(R.string.msg_4864057d62),null).create();d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{try{int value=Integer.parseInt(field.getText().toString().trim());Preferences.validate("mtu",value);store.change("mtu",value);TunnelService.reload();d.dismiss();a.render();}catch(Exception e){field.setError(L10n.s(R.string.msg_5f233b0c2d));}}));a.sheet(d);
 }

 void edit(String title,String key,boolean number){LinearLayout c=a.vertical();c.setPaddingRelative(a.dp(20),a.dp(12),a.dp(20),a.dp(8));EditText field=a.edit(title,String.valueOf(store.data.opt(key)));field.setInputType(number?InputType.TYPE_CLASS_NUMBER:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);c.addView(field);AlertDialog d=new AlertDialog.Builder(a).setTitle(title).setView(c).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).setPositiveButton(L10n.s(R.string.msg_4864057d62),null).create();d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{try{Object value=number?Integer.valueOf(field.getText().toString().trim()):field.getText().toString().trim();Preferences.validate(key,value);store.change(key,value);TunnelService.reload();d.dismiss();a.render();}catch(Exception e){field.setError(number?L10n.s(R.string.msg_5f233b0c2d):e.getMessage());}}));a.sheet(d);}
 void diagnostics(){a.title(L10n.s(R.string.msg_9ba1e22ac3),L10n.s(R.string.msg_989e4afb04));LinearLayout c=a.card(host);a.note(c,L10n.s(R.string.msg_2471b6e04f)+TunnelService.status+L10n.s(R.string.msg_2eabbcfdaa)+a.routeName(a.currentNetwork()));c.addView(a.bold(a.serverName(TunnelService.server.isEmpty()?store.selected(a.currentNetwork()):TunnelService.server),14));a.note(c,L10n.s(R.string.msg_2afa58988e)+TunnelService.health);
  TextView result=a.text(a.diagnosticResult.isEmpty()?L10n.s(R.string.msg_4a198cee74):a.diagnosticResult,14,MainActivity.TEXT);c.addView(result);Button probe=a.primary(L10n.s(R.string.msg_19e7fa6964),()->{});probe.setOnClickListener(v->{TunnelService s=TunnelService.instance;if(s==null){a.message(L10n.s(R.string.msg_bc15101b19));return;}probe.setEnabled(false);result.setText(L10n.s(R.string.msg_1fa4998331));s.probe((value,error)->a.main.post(()->{probe.setEnabled(true);a.diagnosticResult=error==null?L10n.s(R.string.msg_2ddce0385c)+value.optString("ip")+L10n.s(R.string.msg_5a2f918d96)+value.optInt("ping")+L10n.s(R.string.msg_5160aea6bb):L10n.s(R.string.msg_32d477ea64)+error;result.setText(a.diagnosticResult);}));});c.addView(probe,a.lp(-1,52));
  a.item(host,L10n.s(R.string.msg_d32a398cf9),L10n.s(R.string.msg_57013f6956),()->a.speed());
  a.item(host,L10n.s(R.string.msg_f2ee635aaa),null,()->open("logs"));
 }
 void logs(){  a.title(L10n.s(R.string.msg_67ade741ae),"");LinearLayout log=a.card(host);TextView text=a.text(EventLog.read(a),12,MainActivity.MUTED);text.setTypeface(Typeface.MONOSPACE);text.setTextIsSelectable(true);log.addView(text);log.addView(a.button(L10n.s(R.string.msg_049d00ab33),()->{EventLog.clear(a);text.setText(L10n.s(R.string.msg_46a01e5fdc));}),a.lp(-1,48));
 }
}
