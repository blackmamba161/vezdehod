package local.blackmamba.vpn;
import android.app.*;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;

/** A dedicated editing screen; the store changes only after Save. */
final class RuleEditor {
 final MainActivity a; final JSONObject original; String type,value,name,network,target; boolean subdomains;
 EditText valueField,nameField;
 private LinearLayout appSuggestions;
 private List<AppPicker.Entry> installedApps;
 private boolean loadingApps,appsLoadFailed;
 private Map<String,Boolean> appChanges=new LinkedHashMap<>();
 RuleEditor(MainActivity activity,JSONObject rule){a=activity;original=rule;String t=rule==null?"app":a.str(rule,"type","domain_suffix");type=t.equals("app")?"app":t.equals("cidr")?"cidr":"domain";subdomains=!t.equals("domain");value=a.str(rule,"value","");name=a.str(rule,"label","");network=a.str(rule,"network","all");target=a.str(rule,"target","auto");}
 JSONObject state()throws Exception{stash();return Store.obj("original",original==null?JSONObject.NULL:original,"type",type,"value",value,"name",name,"network",network,"target",target,"subdomains",subdomains,"appChanges",new JSONObject(appChanges));}
 static RuleEditor restore(MainActivity a,JSONObject state)throws Exception{RuleEditor editor=new RuleEditor(a,state.optJSONObject("original"));editor.type=state.getString("type");editor.value=state.getString("value");editor.name=state.getString("name");editor.network=state.getString("network");editor.target=state.getString("target");editor.subdomains=state.getBoolean("subdomains");JSONObject changes=state.optJSONObject("appChanges");if(changes!=null)for(Iterator<String> it=changes.keys();it.hasNext();){String pkg=it.next();editor.appChanges.put(pkg,changes.getBoolean(pkg));}return editor;}
 void stash(){if(valueField!=null)value=valueField.getText().toString().trim();if(nameField!=null)name=nameField.getText().toString().trim();}
 boolean appBatch(){return original==null&&type.equals("app")&&!appChanges.isEmpty();}
 void draw(){valueField=nameField=null;appSuggestions=null;boolean batch=appBatch();LinearLayout host=a.content;TextView back=a.text(L10n.s(R.string.msg_789841320c),14,MainActivity.ACCENT);back.setMinHeight(a.dp(44));back.setOnClickListener(v->close());host.addView(back);a.title(original==null?L10n.s(R.string.msg_b559f9f1ca):L10n.s(R.string.msg_97a3265a4a),null);
  LinearLayout types=a.row();String[] ids={"app","domain","cidr"},labels={L10n.s(R.string.msg_f9886ca2bc),L10n.s(R.string.msg_045bef7d26),"IP"};for(int i=0;i<ids.length;i++){final String id=ids[i];Button button=type.equals(id)?a.primary(labels[i],()->selectType(id)):a.button(labels[i],()->selectType(id));types.addView(button,new LinearLayout.LayoutParams(0,a.dp(52),1));}host.addView(types);a.gap(host,8);
  if(batch)a.note(host,L10n.format(R.string.app_picker_pending,appChanges.size()));
  else{a.label(host,type.equals("app")?L10n.s(R.string.msg_f9886ca2bc):type.equals("cidr")?L10n.s(R.string.msg_56970d69fd):L10n.s(R.string.msg_67787241c9));valueField=a.edit(type.equals("app")?L10n.s(R.string.msg_875be13421):type.equals("cidr")?"192.168.0.0/16":"example.com",value);valueField.setSingleLine(true);valueField.setTextDirection(type.equals("app")?View.TEXT_DIRECTION_FIRST_STRONG:View.TEXT_DIRECTION_LTR);host.addView(valueField);}
  if(type.equals("app")&&!batch){
   appSuggestions=a.vertical();appSuggestions.setVisibility(View.GONE);host.addView(appSuggestions,a.lp(-1,-2));
   LinearLayout suggestions=appSuggestions;EditText input=valueField;
   suggestions.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
    if(b-t!=ob-ot&&input.hasFocus()&&b>t)suggestions.post(()->{
     if(suggestions==appSuggestions&&suggestions.isShown()&&input.hasFocus())
      suggestions.requestRectangleOnScreen(new android.graphics.Rect(0,0,suggestions.getWidth(),Math.min(suggestions.getHeight(),a.dp(104))),true);
    });
   });
   valueField.addTextChangedListener(a.watcher(this::updateAppSuggestions));updateAppSuggestions();
  }
  if(type.equals("app")){Button apps=a.button(L10n.s(original==null?R.string.app_picker_multiple:R.string.msg_7819ea82b0),this::pickApps);host.addView(apps,a.lp(-1,52));}
  else if(type.equals("domain")){Switch suffix=a.switchControl();suffix.setText(L10n.s(R.string.msg_72f953a4f1));suffix.setTextColor(MainActivity.TEXT);suffix.setTextSize(14*a.uiScale);suffix.setMinHeight(a.dp(48));suffix.setChecked(subdomains);suffix.setOnCheckedChangeListener((b,on)->subdomains=on);host.addView(suffix);}
  a.gap(host,6);a.item(host,L10n.s(R.string.msg_b765f32584),routeLabel(),()->selectTarget());a.item(host,L10n.s(R.string.msg_190ba6d582),network.equals("all")?L10n.s(R.string.msg_b03361f6c0):a.routeName(network),()->selectNetwork());a.gap(host,8);
  if(!batch){a.label(host,L10n.s(R.string.msg_7a1fc2cbc5));nameField=a.edit(L10n.s(R.string.msg_09d340f276),name);host.addView(nameField);}
  if(original!=null){a.gap(host,6);Button delete=a.button(L10n.s(R.string.msg_e4d9463d19),()->a.confirmRemoveRule(original,()->close()));delete.setTextColor(MainActivity.RED);host.addView(delete,a.lp(-1,52));}
 }
 private void updateAppSuggestions(){
  if(a.isDestroyed()||a.ruleEditor!=this||!"rule-edit".equals(a.tab)||appSuggestions==null)return;
  appSuggestions.removeAllViews();appSuggestions.setVisibility(View.GONE);
  String input=valueField.getText().toString().trim();
  if(input.isEmpty()||original!=null&&"app".equals(original.optString("type"))&&input.equals(original.optString("value")))return;
  if(installedApps==null){
   if(!loadingApps&&!appsLoadFailed){
    loadingApps=true;
    a.io.execute(()->{
     List<AppPicker.Entry> loaded;
     try{loaded=AppPicker.installedApps(a);}catch(RuntimeException ignored){loaded=null;}
     List<AppPicker.Entry> result=loaded;
     a.main.post(()->{installedApps=result;loadingApps=false;appsLoadFailed=result==null;updateAppSuggestions();});
    });
   }
   a.note(appSuggestions,L10n.s(appsLoadFailed?R.string.msg_8c5bfb9222:R.string.msg_e5834c4ca1));appSuggestions.setVisibility(View.VISIBLE);return;
  }
  // An exact package already identifies the app, including after picking it in the full list.
  for(AppPicker.Entry app:installedApps)if(app.pkg.equals(input))return;
  String query=input.toLowerCase(Locale.ROOT);int count=0;
  for(AppPicker.Entry app:installedApps){
   if(app.system||!app.matches(query))continue;
   addAppSuggestion(app);if(++count==4)break;
  }
  if(count==0)a.note(appSuggestions,L10n.s(R.string.msg_1e1b70b17c));
  appSuggestions.setVisibility(View.VISIBLE);
 }
 private void addAppSuggestion(AppPicker.Entry app){
  LinearLayout row=a.row(),labels=a.vertical();row.setMinimumHeight(a.dp(48));row.setBackground(a.selectable());
  ImageView icon=new ImageView(a);icon.setImageDrawable(a.appIcon(app.pkg));icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  LinearLayout.LayoutParams image=a.lp(28,28);image.setMarginEnd(a.dp(8));row.addView(icon,image);
  TextView title=a.bold(app.name,14),pkg=a.text(app.pkg,11,MainActivity.MUTED);pkg.setTextDirection(View.TEXT_DIRECTION_LTR);pkg.setSingleLine(true);pkg.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
  labels.addView(title);labels.addView(pkg);row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
  row.setOnClickListener(v->{valueField.setText(app.pkg);valueField.setSelection(app.pkg.length());a.getSystemService(android.view.inputmethod.InputMethodManager.class).hideSoftInputFromWindow(valueField.getWindowToken(),0);});
  row.setFocusable(true);appSuggestions.addView(row,a.lp(-1,-2));
 }
 private void pickApps(){
  if(original!=null){a.pickApp(valueField);return;}
  try{
   Map<String,Boolean> checked;
   synchronized(a.store){checked=AppRuleSelection.checked(a.store.data.getJSONArray("rules"));}
   new AppPicker(a,checked,appChanges,changes->{stash();appChanges=changes;a.render();}).show();
  }catch(Exception e){a.message(TunnelService.safeError(e));}
 }
 private void saveAppBatch(){
  try{
   synchronized(a.store){
    JSONArray rules=a.store.data.getJSONArray("rules"),next=AppRuleSelection.apply(rules,appChanges,target,network,a::appName);
    a.store.change("rules",next);
   }
   TunnelService.reload();close();
  }catch(Exception e){a.message(TunnelService.safeError(e));}
 }
 void selectType(String next){if(type.equals(next))return;stash();type=next;value="";a.render();}
 String routeLabel(){return target.equals("auto")?L10n.s(R.string.msg_3c0423bc77):a.targetName(target);}
 void selectTarget(){stash();new RoutePicker(a,L10n.s(R.string.msg_b765f32584),target,value->{target=value;a.render();}).show();}
 void selectNetwork(){stash();String[] ids={"all","wifi","cellular"};AlertDialog d=a.new DialogBuilder(a).setTitle(L10n.s(R.string.msg_190ba6d582)).setSingleChoiceItems(new String[]{L10n.s(R.string.msg_b03361f6c0),L10n.s(R.string.msg_58e849be29),L10n.s(R.string.msg_0288127e28)},a.index(ids,network),(dialog,index)->{network=ids[index];dialog.dismiss();a.render();}).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).create();a.sheet(d);}
 void footer(){LinearLayout buttons=a.row();buttons.setPaddingRelative(a.dp(16),a.dp(8),a.dp(16),a.dp(12));buttons.setBackgroundColor(MainActivity.BG);buttons.addView(a.button(L10n.s(R.string.msg_0ec753be8d),()->close()),new LinearLayout.LayoutParams(0,a.dp(56),1));buttons.addView(a.primary(L10n.s(R.string.msg_4864057d62),()->save()),new LinearLayout.LayoutParams(0,a.dp(56),1));a.root.addView(buttons);}
 void save(){stash();if(appBatch()){saveAppBatch();return;}String actual=type.equals("domain")?(subdomains?"domain_suffix":"domain"):type;try{MainActivity.validateRule(actual,value);}catch(Exception e){valueField.setError(e.getMessage());valueField.requestFocus();return;}try{JSONObject rule=Store.obj("id",original==null?UUID.randomUUID().toString():original.optString("id"),"type",actual,"value",value,"label",name.isEmpty()?(actual.equals("app")?a.appName(value):value):name,"network",network,"target",target,"enabled",original==null||original.optBoolean("enabled",true));synchronized(a.store){JSONArray rules=new JSONArray(a.store.data.getJSONArray("rules").toString());if(original==null)rules.put(rule);else{boolean found=false;for(int i=0;i<rules.length();i++)if(rules.getJSONObject(i).optString("id").equals(original.optString("id"))){rules.put(i,rule);found=true;break;}if(!found)throw new Exception(L10n.s(R.string.msg_2bc1c34c0c));}a.store.change("rules",RuleGroups.consolidate(rules));}TunnelService.reload();close();}catch(Exception e){a.message(TunnelService.safeError(e));}}
 void close(){a.tab="rules";a.ruleEditor=null;a.render();}
}
