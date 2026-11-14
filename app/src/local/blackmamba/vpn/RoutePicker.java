package local.blackmamba.vpn;

import android.app.*;
import android.view.*;
import android.widget.*;
import java.util.function.Consumer;

/** One routing vocabulary for the default route and individual rules. */
final class RoutePicker {
 final MainActivity a;final String title,current;final Consumer<String> selected;
 AlertDialog dialog;
 RoutePicker(MainActivity a,String title,String current,Consumer<String> selected){this.a=a;this.title=title;this.current=current;this.selected=selected;}
 void show(){
  LinearLayout box=a.vertical();box.setPaddingRelative(a.dp(12),0,a.dp(12),0);LinearLayout heading=a.row();heading.addView(a.bold(title,18),new LinearLayout.LayoutParams(0,-2,1));heading.setMinimumHeight(a.dp(44));
  if(title.equals(L10n.s(R.string.msg_50e1cb1272)))heading.addView(a.helpButton(title,L10n.s(R.string.msg_938ad66f65)),a.lp(44,44));box.addView(heading);
  option(box,"direct","direct",L10n.s(R.string.msg_1667336ad3),null,()->choose("direct"));
  String description=a.store.data.optBoolean("auto",true)?"Wi-Fi: "+a.serverName(a.store.data.optString("wifi"))+L10n.s(R.string.msg_e26ed24f9e)+a.serverName(a.store.data.optString("cellular")):L10n.s(R.string.msg_7be14a7ba6)+a.serverName(a.store.selected("wifi"));
  option(box,"auto","shield",L10n.s(R.string.msg_3c0423bc77),description,()->choose("auto"));
  option(box,"block","block",L10n.s(R.string.msg_e481dedaf2),null,()->choose("block"));
  a.divider(box);option(box,"server","servers",L10n.s(R.string.msg_0e4d55ab7c),current.startsWith("server:")?a.targetName(current):L10n.s(R.string.msg_8f67e4d13d),()->{dialog.dismiss();a.pickServer(L10n.s(R.string.msg_0e4d55ab7c),current.startsWith("server:")?current.substring(7):"",id->selected.accept("server:"+id),()->show());});
  ScrollView scroll=new ScrollView(a);scroll.setVerticalScrollBarEnabled(false);scroll.addView(box);dialog=a.new DialogBuilder(a).setView(scroll).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).create();a.sheet(dialog);
 }
 void choose(String target){dialog.dismiss();selected.accept(target);}
 void option(LinearLayout host,String id,String icon,String name,String description,Runnable action){
  boolean checked=id.equals(current)||id.equals("server")&&current.startsWith("server:");LinearLayout row=a.row();row.setPaddingRelative(a.dp(12),a.dp(8),a.dp(10),a.dp(8));row.setMinimumHeight(a.dp(54));
  android.graphics.drawable.GradientDrawable bg=a.bg(checked?MainActivity.SELECTED:MainActivity.FIELD,12);bg.setStroke(a.dp(1),checked?MainActivity.ACCENT:MainActivity.BORDER);row.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(a.light?0x18000000:0x30ffffff),bg,null));
  Icons symbol=new Icons(a,icon,checked?MainActivity.ACCENT:MainActivity.MUTED);LinearLayout.LayoutParams iconLayout=a.lp(22,22);iconLayout.setMarginEnd(a.dp(12));row.addView(symbol,iconLayout);LinearLayout labels=a.vertical();labels.addView(a.bold(name,14));if(description!=null){TextView detail=a.text(description,12,MainActivity.MUTED);detail.setMaxLines(2);detail.setEllipsize(android.text.TextUtils.TruncateAt.END);labels.addView(detail);}row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
  RadioButton mark=new RadioButton(a);mark.setChecked(checked);mark.setClickable(false);mark.setFocusable(false);mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);mark.setButtonTintList(android.content.res.ColorStateList.valueOf(checked?MainActivity.ACCENT:MainActivity.MUTED));row.addView(mark,a.lp(36,36));row.setOnClickListener(v->action.run());row.setContentDescription(name+(checked?L10n.s(R.string.msg_dd6b0e1e55):""));row.setFocusable(true);row.setTag("route:"+id);LinearLayout.LayoutParams p=a.lp(-1,-2);p.bottomMargin=a.dp(6);host.addView(row,p);
 }
}
