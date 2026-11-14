package local.blackmamba.vpn;

import android.app.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Service rows first; individual predicates are visible only after expansion. */
final class RulesScreen {
 final MainActivity a;
 final Set<String> expanded=new HashSet<>();
 RulesScreen(MainActivity a){this.a=a;}
 void draw()throws Exception {
  LinearLayout heading=a.row();heading.addView(a.bold(L10n.s(R.string.msg_667d8b44a6),24),new LinearLayout.LayoutParams(0,-2,1));heading.setMinimumHeight(a.dp(48));
  heading.addView(a.helpButton(L10n.s(R.string.msg_9e572ab9c6),L10n.s(R.string.msg_02580e2fda)),a.lp(44,44));a.content.addView(heading);
  LinearLayout actions=a.row();actions.addView(a.primary(L10n.s(R.string.msg_9276259877),()->presets(false)),new LinearLayout.LayoutParams(0,a.dp(48),1));actions.addView(a.button(L10n.s(R.string.msg_fc7987118c),()->a.editRule(null)),new LinearLayout.LayoutParams(0,a.dp(48),1));a.content.addView(actions);a.gap(a.content,6);
  JSONArray rules=a.store.data.getJSONArray("rules");List<RuleGroups.Group> groups=RuleGroups.groups(rules);
  if(groups.isEmpty()){TextView empty=a.text(L10n.s(R.string.msg_dc9a84c4a4),14,MainActivity.MUTED);empty.setPaddingRelative(0,a.dp(16),0,a.dp(16));a.content.addView(empty);}
  Map<String,Integer> parts=new HashMap<>();
  for(int gi=0;gi<groups.size();gi++){
   RuleGroups.Group group=groups.get(gi);int part=parts.getOrDefault(group.service,0)+1;parts.put(group.service,part);
   String key=group.service+":"+part;String title=RuleCatalog.title(group.service)+(part>1?" · "+part:"");boolean open=expanded.contains(key);final int groupIndex=gi;
   LinearLayout card=a.card(a.content),header=a.row(),labels=a.vertical();card.setPaddingRelative(a.dp(10),a.dp(3),a.dp(6),a.dp(3));
   LinearLayout nameRow=a.row();Icons disclosure=new Icons(a,open?"down":"chevron",MainActivity.MUTED);nameRow.addView(disclosure,a.lp(16,20));TextView name=a.bold(title,14);name.setMaxLines(2);nameRow.addView(name,new LinearLayout.LayoutParams(0,-2,1));labels.addView(nameRow);
   int enabled=0;Set<String> routes=new HashSet<>();for(int i=group.start;i<group.end;i++){JSONObject r=rules.getJSONObject(i);if(r.optBoolean("enabled",true))enabled++;routes.add(route(r));}
   String route=routes.size()==1?routes.iterator().next():L10n.s(R.string.msg_8840608925);
   TextView summary=a.text((enabled==0?L10n.s(R.string.msg_d934aed196):enabled<group.size()?enabled+L10n.s(R.string.msg_6ce4fa393d)+group.size()+L10n.s(R.string.msg_4cc948fff2):count(group.size()))+" · "+route,11,MainActivity.MUTED);summary.setPaddingRelative(a.dp(16),0,0,a.dp(4));summary.setMaxLines(2);labels.addView(summary);labels.setMinimumHeight(a.dp(60));labels.setGravity(Gravity.CENTER_VERTICAL);labels.setBackground(a.selectable());labels.setContentDescription((open?L10n.s(R.string.msg_26e42bb274):L10n.s(R.string.msg_dcfb1b173b))+title);labels.setOnClickListener(v->{if(open)expanded.remove(key);else expanded.add(key);refresh();});header.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
   header.addView(arrow("move-up",L10n.s(R.string.msg_8f769303f6)+title,gi>0,()->moveGroup(groupIndex,-1)),a.lp(44,48));header.addView(arrow("move-down",L10n.s(R.string.msg_c800355918)+title,gi<groups.size()-1,()->moveGroup(groupIndex,1)),a.lp(44,48));
   Switch toggle=a.switchControl();toggle.setChecked(enabled>0);toggle.setContentDescription(L10n.s(R.string.msg_9189fc3f32)+title);toggle.setOnCheckedChangeListener((b,on)->{try{JSONArray next=RuleGroups.copy(a.store.data.getJSONArray("rules"));for(int i=group.start;i<group.end;i++)next.getJSONObject(i).put("enabled",on);save(next);}catch(Exception e){a.message(TunnelService.safeError(e));refresh();}});header.addView(toggle,a.lp(-2,48));card.addView(header);
   if(open){
    LinearLayout children=a.vertical();children.setPaddingRelative(a.dp(8),a.dp(3),a.dp(6),a.dp(3));children.setBackground(a.bg(a.light?0xffe8f0f9:0xff252b33,12));
    LinearLayout.LayoutParams nested=a.lp(-1,-2);nested.setMarginStart(a.dp(10));nested.bottomMargin=a.dp(6);card.addView(children,nested);
    for(int i=group.start;i<group.end;i++)rule(children,rules.getJSONObject(i),i,group);
   }
  }
  a.item(a.content,L10n.s(R.string.msg_50e1cb1272),a.targetName(a.store.data.optString("defaultTarget","direct")),()->new RoutePicker(a,L10n.s(R.string.msg_50e1cb1272),a.store.data.optString("defaultTarget","direct"),target->a.change("defaultTarget",target)).show());
 }
 String count(int n){if(!L10n.locale().getLanguage().equals("ru"))return L10n.s(R.string.msg_667d8b44a6)+": "+n;int last=n%10,ten=n%100;return n+" "+(ten>=11&&ten<=14?L10n.s(R.string.msg_3ca212f4fc):last==1?L10n.s(R.string.msg_5d1611f2fb):last>=2&&last<=4?L10n.s(R.string.msg_69b46d600c):L10n.s(R.string.msg_3ca212f4fc));}
 String route(JSONObject rule){String target=rule.optString("target","auto"),network=rule.optString("network","all");return (target.equals("auto")?"VPN":a.targetName(target))+(network.equals("all")?"":" · "+a.routeName(network));}
 void rule(LinearLayout parent,JSONObject r,int index,RuleGroups.Group group){
  if(index>group.start)a.divider(parent);LinearLayout row=a.row(),labels=a.vertical();String value=r.optString("value"),title=r.optString("label",value);if(title.isEmpty())title=value;
  TextView name=a.text(title,13,MainActivity.TEXT);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);labels.addView(name);
  TextView detail=a.text((r.optString("type").equals("app")?L10n.s(R.string.msg_f9886ca2bc):r.optString("type").equals("cidr")?"IP":!title.equals(value)?value:L10n.s(R.string.msg_045bef7d26))+" · "+route(r),11,MainActivity.MUTED);detail.setMaxLines(2);detail.setEllipsize(android.text.TextUtils.TruncateAt.END);labels.addView(detail);labels.setMinimumHeight(a.dp(52));labels.setGravity(Gravity.CENTER_VERTICAL);labels.setBackground(a.selectable());labels.setContentDescription(L10n.s(R.string.msg_2681a24f90)+title);labels.setOnClickListener(v->a.editRule(r));row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
  row.addView(arrow("move-up",L10n.s(R.string.msg_27c9c75d4a)+title,index>group.start,()->moveRule(index,-1)),a.lp(44,48));row.addView(arrow("move-down",L10n.s(R.string.msg_08525c9005)+title,index<group.end-1,()->moveRule(index,1)),a.lp(44,48));
  Switch toggle=a.switchControl();toggle.setChecked(r.optBoolean("enabled",true));toggle.setContentDescription(L10n.s(R.string.msg_573d59bada)+title);toggle.setOnCheckedChangeListener((b,on)->{try{JSONArray next=RuleGroups.copy(a.store.data.getJSONArray("rules"));next.getJSONObject(index).put("enabled",on);save(next);}catch(Exception e){a.message(TunnelService.safeError(e));refresh();}});row.addView(toggle,a.lp(-2,48));parent.addView(row);
 }
 View arrow(String icon,String description,boolean enabled,Runnable click){Icons v=new Icons(a,icon,MainActivity.ACCENT);v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);v.setPaddingRelative(a.dp(12),a.dp(14),a.dp(12),a.dp(14));v.setBackground(a.selectable());v.setContentDescription(description);v.setEnabled(enabled);v.setAlpha(enabled?1:.22f);v.setFocusable(enabled);v.setOnClickListener(w->click.run());return v;}
 void moveGroup(int index,int direction){try{save(RuleGroups.moveGroup(a.store.data.getJSONArray("rules"),index,direction));}catch(Exception e){a.message(TunnelService.safeError(e));}}
 void moveRule(int index,int direction){try{save(RuleGroups.moveRule(a.store.data.getJSONArray("rules"),index,direction));}catch(Exception e){a.message(TunnelService.safeError(e));}}
 void save(JSONArray rules)throws Exception{a.store.change("rules",rules);TunnelService.reload();refresh();}
 void refresh(){int y=a.content.getParent() instanceof ScrollView?((ScrollView)a.content.getParent()).getScrollY():0;a.render();if(a.content.getParent() instanceof ScrollView){ScrollView scroll=(ScrollView)a.content.getParent();scroll.post(()->scroll.scrollTo(0,y));}}
 void presets(boolean first){
  try{
   JSONArray current=a.store.data.getJSONArray("rules");Set<String> selected=new LinkedHashSet<>();Map<String,CheckBox> boxes=new LinkedHashMap<>();
   LinearLayout body=a.vertical();body.setPaddingRelative(a.dp(12),0,a.dp(12),0);ScrollView scroll=new ScrollView(a);scroll.setVerticalScrollBarEnabled(false);scroll.addView(body);
   AlertDialog dialog=a.new DialogBuilder(a).setTitle(L10n.s(R.string.msg_a11443ec76)).setView(scroll).setNegativeButton(first?L10n.s(R.string.msg_fd042c9a3a):L10n.s(R.string.msg_0ec753be8d),null).setPositiveButton(L10n.s(R.string.msg_768af67762),null).create();
   String section="";for(RuleCatalog.Service service:RuleCatalog.SERVICES){
    if(!section.equals(service.section)){section=service.section;LinearLayout title=a.row();TextView name=a.bold(RuleCatalog.sectionName(section),13);name.setTextColor(MainActivity.MUTED);title.addView(name,new LinearLayout.LayoutParams(0,a.dp(38),1));if(section.equals("base")){TextView all=a.text(L10n.s(R.string.msg_bdda7682fb),12,MainActivity.ACCENT);all.setGravity(Gravity.CENTER);all.setMinHeight(a.dp(44));all.setPaddingRelative(a.dp(8),0,a.dp(8),0);all.setBackground(a.selectable());all.setOnClickListener(v->{boolean complete=true;for(RuleCatalog.Service s:RuleCatalog.SERVICES)if(s.section.equals("base")&&boxes.get(s.id).isEnabled()&&!boxes.get(s.id).isChecked())complete=false;for(RuleCatalog.Service s:RuleCatalog.SERVICES)if(s.section.equals("base")&&boxes.get(s.id).isEnabled())boxes.get(s.id).setChecked(!complete);});title.addView(all);}body.addView(title);}
    int missing=RuleGroups.missing(current,service);CheckBox box=new CheckBox(a);box.setTag("app-text");box.setText(service.name+(missing==0?L10n.s(R.string.msg_accc8ab2b1):""));box.setTextSize(14*a.uiScale);box.setTextColor(missing==0?MainActivity.MUTED:MainActivity.TEXT);box.setMinHeight(a.dp(48));box.setPaddingRelative(0,0,a.dp(4),0);box.setButtonTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{MainActivity.ACCENT,MainActivity.MUTED}));box.setEnabled(missing>0);box.setChecked(missing==0);boxes.put(service.id,box);body.addView(box,a.lp(-1,48));box.setOnCheckedChangeListener((v,checked)->{if(checked)selected.add(service.id);else selected.remove(service.id);Button apply=dialog.getButton(-1);if(apply!=null){apply.setEnabled(!selected.isEmpty());apply.setText(selected.isEmpty()?L10n.s(R.string.msg_768af67762):L10n.s(R.string.msg_97013008b5)+selected.size());apply.setAlpha(selected.isEmpty()?.45f:1);}});
   }
   if(first)dialog.setOnDismissListener(d->{try{a.store.change("rulePresetsSeen",true);}catch(Exception e){a.message(TunnelService.safeError(e));}});
   a.sheet(dialog);Button apply=dialog.getButton(-1);apply.setEnabled(false);apply.setAlpha(.45f);apply.setOnClickListener(v->{try{JSONArray before=a.store.data.getJSONArray("rules"),after=RuleGroups.addPresets(before,selected);int added=after.length()-before.length();a.store.change("rules",after);dialog.dismiss();TunnelService.reload();a.tab="rules";a.render();a.message(L10n.s(R.string.msg_dcd66875e7)+added);}catch(Exception e){a.message(TunnelService.safeError(e));}});
  }catch(Exception e){a.message(TunnelService.safeError(e));}
 }
}
