package local.blackmamba.vpn;

import android.app.AlertDialog;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.function.Consumer;

/** One selection per route. Filtering and probing never commit a selection. */
final class ServerPicker {
 final MainActivity a;
 final String title,initial;
 String selected;
 final boolean optional;
 final Consumer<String> chosen;
 final Runnable back;
 final LinearLayout list;
 final ServerListSort sorting;
 final Map<String,TextView> delays=new LinkedHashMap<>();
 final Map<String,PingRadar> radars=new HashMap<>();
 final Map<String,Consumer<Boolean>> markers=new HashMap<>();
 Button disable;TextView summary,source;LinearLayout selection;
 AlertDialog dialog;Spinner filter;Button check;String scope;JSONArray visible;

 ServerPicker(MainActivity a,String title,String selected,Consumer<String> chosen,Runnable back,boolean optional){
  this.a=a;this.title=title;this.initial=this.selected=selected;this.chosen=chosen;this.back=back;this.optional=optional;
  list=a.vertical();sorting=new ServerListSort(a,list,ServerOrder.PING);
 }
 void show(){show(a::pingAll);}
 void show(Consumer<JSONArray> probe){
  LinearLayout box=a.vertical();
  TextView heading=a.bold(title,20);heading.setAccessibilityHeading(true);box.addView(heading);a.gap(box,8);
  selection=a.row();LinearLayout info=a.vertical();Icons mark=new Icons(a,"check",MainActivity.ACCENT);mark.setBackground(a.flatSurface(MainActivity.SELECTED,16));int pad=a.dp(6);mark.setPadding(pad,pad,pad,pad);selection.addView(mark,a.lp(28,28));
  summary=a.bold("",14);source=a.text("",12,MainActivity.MUTED);summary.setPadding(0,0,0,0);source.setPaddingRelative(0,a.dp(2),0,0);info.addView(summary);info.addView(source);LinearLayout.LayoutParams textLayout=new LinearLayout.LayoutParams(0,-2,1);textLayout.setMarginStart(a.dp(10));selection.addView(info,textLayout);selection.setPaddingRelative(0,0,0,a.dp(12));box.addView(selection);
  JSONArray profiles=a.store.data.optJSONArray("profiles");String[] ids=new String[profiles.length()+1];ids[0]="all";
  for(int i=1;i<ids.length;i++)ids[i]=profiles.optJSONObject(i-1).optString("id");
  filter=a.spinner(a.profileNames(),0);if(profiles.length()>0){LinearLayout.LayoutParams fp=(LinearLayout.LayoutParams)filter.getLayoutParams();fp.height=-2;fp.bottomMargin=a.dp(8);box.addView(filter,fp);}
  check=a.button(L10n.s(R.string.msg_4ecfcffd55),()->probe.accept(visible));
  if(optional){
   LinearLayout actions=new LinearLayout(a){@Override protected void onMeasure(int w,int h){int space=Math.max(0,MeasureSpec.getSize(w)-a.dp(8));int left=wordWidth(disable),right=wordWidth(check);int first=left+right<=space?Math.max(left,Math.min(space/2,space-right)):space/2;disable.getLayoutParams().width=first;check.getLayoutParams().width=space-first;super.onMeasure(w,h);}};actions.setBaselineAligned(false);disable=a.button("",()->choose(""));
   for(Button b:new Button[]{disable,check}){b.setStateListAnimator(null);b.setBackground(actionSurface());b.setPaddingRelative(a.dp(6),a.dp(8),a.dp(6),a.dp(8));b.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);b.setSingleLine(false);b.setMaxLines(Integer.MAX_VALUE);b.setEllipsize(null);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1);if(b==check)lp.setMarginStart(a.dp(8));actions.addView(b,lp);}
   box.addView(actions,a.lp(-1,-2));
  }else box.addView(check,a.lp(-1,-2));
  a.gap(box,8);box.addView(list);updateSummary();
  dialog=a.new DialogBuilder(a).setView(box).setPositiveButton(L10n.s(R.string.msg_4864057d62),(d,w)->{if(!selected.equals(initial))chosen.accept(selected);}).create();
  if(back!=null)dialog.setOnCancelListener(d->back.run());
  dialog.setOnDismissListener(d->{if(a.serverPicker==this)a.serverPicker=null;});
  a.serverPicker=this;a.sheet(dialog);draw("all");
  filter.setOnItemSelectedListener(a.listener(()->{int pos=filter.getSelectedItemPosition();if(a.serverPicker==this&&pos>=0&&pos<ids.length)draw(ids[pos]);}));
 }
 int wordWidth(Button b){float width=0;for(String word:b.getText().toString().split("\\s+"))width=Math.max(width,b.getPaint().measureText(word));return (int)Math.ceil(width)+b.getCompoundPaddingLeft()+b.getCompoundPaddingRight()+a.dp(1);}
 android.graphics.drawable.Drawable actionSurface(){android.graphics.drawable.GradientDrawable shape=a.bg(MainActivity.FIELD,12);shape.setStroke(a.dp(1),MainActivity.CONTROL_BORDER);return new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(a.light?0x18000000:0x30ffffff),shape,null);}
 void updateSummary(){
  JSONObject current=Store.find(a.store.data.optJSONArray("servers"),selected);
  selection.setVisibility(current==null?View.GONE:View.VISIBLE);if(current!=null){summary.setText(current.optString("name"));source.setText(a.profileName(current.optString("profile")));}
  if(disable!=null){boolean off=selected.isEmpty();disable.setText((off?"✓  ":"")+L10n.s(R.string.msg_2d8f7570e1));disable.setSelected(off);disable.setTextColor(off?MainActivity.ACCENT:MainActivity.TEXT);}
 }
 void mark(String id,boolean on){Consumer<Boolean> marker=markers.get(id);if(marker!=null)marker.accept(on);}
 void choose(String id){if(id.equals(selected))return;mark(selected,false);selected=id;mark(id,true);updateSummary();}
 void draw(String next){
  if(next.equals(scope))return;scope=next;list.removeAllViews();sorting.reset();delays.clear();radars.clear();markers.clear();
  JSONArray nodes=a.store.data.optJSONArray("servers");visible=new JSONArray();
  for(int i=0;i<nodes.length();i++){JSONObject n=nodes.optJSONObject(i);if(!scope.equals("all")&&!scope.equals(n.optString("profile")))continue;
   visible.put(n);String id=n.optString("id"),name=n.optString("name");boolean marked=selected.equals(id);
   LinearLayout group=a.vertical(),row=a.row(),labels=a.vertical();row.setPaddingRelative(a.dp(8),a.dp(4),a.dp(4),a.dp(4));row.setMinimumHeight(a.dp(64));
   TextView label=a.bold(name,15);labels.addView(label);String detail=n.optString("description");
   if(scope.equals("all"))detail=a.profileName(n.optString("profile"))+(Compat.blank(detail)?"":" · "+detail);
   labels.addView(a.text((Compat.blank(detail)?"":detail+" · ")+ServerProtocol.details(n),12,MainActivity.MUTED));row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
   // Put ping text below the name, not in a fixed-width column competing with long translations.
   TextView delay=a.text("",12,MainActivity.MUTED);labels.addView(delay);delays.put(id,delay);
   PingRadar radar=a.makeRadar(id,name);radar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);row.addView(radar,a.lp(48,48));radars.put(id,radar);
   markers.put(id,on->{label.setText((on?"✓  ":"")+name);row.setSelected(on);row.setBackground(a.selectionSurface(on));});mark(id,marked);row.setOnClickListener(v->choose(id));group.addView(row);a.divider(group);
   list.addView(group);sorting.add(id,name,group);
  }
  refresh();
 }
 void refresh(){
  ProbeManager probes=ProbeManager.INSTANCE;boolean running=probes.running();check.setEnabled(running||!delays.isEmpty());
  String caption=running?L10n.s(R.string.msg_0ec753be8d)+" · "+probes.progress():L10n.s(R.string.msg_4ecfcffd55);if(!caption.contentEquals(check.getText()))check.setText(caption);
  for(Map.Entry<String,TextView> entry:delays.entrySet()){
   String id=entry.getKey();TextView v=entry.getValue();String value=a.pingLabelText(id);if(!value.contentEquals(v.getText()))v.setText(value);
   Integer ms=a.visiblePing(id);v.setTextColor(ms==null?MainActivity.MUTED:ms<0?MainActivity.RED:a.light?0xff14784f:0xff86d9ae);radars.get(id).active(probes.pending(id));
  }
  sorting.refresh();
 }
}
