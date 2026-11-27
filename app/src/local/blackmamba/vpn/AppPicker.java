package local.blackmamba.vpn;
import android.app.*;import android.content.*;import android.content.pm.*;import android.content.res.ColorStateList;import android.graphics.drawable.Drawable;import android.view.*;import android.widget.*;import java.util.*;import java.util.function.Consumer;

/** Searchable, recycled app rows; package names remain available without dominating the list. */
final class AppPicker {
 final MainActivity a;final EditText destination;final List<Entry> all=new ArrayList<>(),shown=new ArrayList<>();final android.util.LruCache<String,Drawable> icons=new android.util.LruCache<>(48);
 final Map<String,Boolean> checked,initialChanges,changes;
 final Consumer<Map<String,Boolean>> onApply;
 AlertDialog dialog;EditText search;ListView list;TextView empty;boolean systems=false;String selected;long generation;
 static final class Entry {
  final String pkg,name;final boolean system;final ApplicationInfo info;private final String searchName,searchPackage;
  Entry(ApplicationInfo i,String n,boolean s){info=i;pkg=i.packageName;name=n;system=s;searchName=n.toLowerCase(Locale.ROOT);searchPackage=pkg.toLowerCase(Locale.ROOT);}
  boolean matches(String query){return searchName.contains(query)||searchPackage.contains(query);}
 }
 static List<Entry> installedApps(MainActivity a){
  PackageManager pm=a.getPackageManager();Set<String> launchable=new HashSet<>();
  for(ResolveInfo r:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0))launchable.add(r.activityInfo.packageName);
  List<Entry> loaded=new ArrayList<>();
  for(ApplicationInfo info:pm.getInstalledApplications(0)){
   if(info.packageName.equals(a.getPackageName()))continue;
   boolean service=(info.flags&ApplicationInfo.FLAG_SYSTEM)!=0&&!launchable.contains(info.packageName);
   loaded.add(new Entry(info,pm.getApplicationLabel(info).toString(),service));
  }
  java.text.Collator order=java.text.Collator.getInstance(L10n.locale());loaded.sort((x,y)->order.compare(x.name,y.name));return loaded;
 }
 AppPicker(MainActivity a,EditText destination){this(a,destination,Collections.emptyMap(),Collections.emptyMap(),null);}
 AppPicker(MainActivity a,Map<String,Boolean> checked,Map<String,Boolean> changes,Consumer<Map<String,Boolean>> onApply){this(a,null,checked,changes,onApply);}
 private AppPicker(MainActivity a,EditText destination,Map<String,Boolean> checked,Map<String,Boolean> changes,Consumer<Map<String,Boolean>> onApply){
  this.a=a;this.destination=destination;this.checked=checked;initialChanges=new LinkedHashMap<>(changes);this.changes=new LinkedHashMap<>(changes);this.onApply=onApply;selected=destination==null?"":destination.getText().toString();
 }
 boolean multiple(){return onApply!=null;}
 boolean selected(String pkg){return multiple()?changes.getOrDefault(pkg,checked.getOrDefault(pkg,false)):pkg.equals(selected);}
 void toggle(String pkg){
  boolean next=!selected(pkg);
  if(next==checked.getOrDefault(pkg,false))changes.remove(pkg);else changes.put(pkg,next);
  updateApply();
 }
 void updateApply(){if(multiple())dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!changes.equals(initialChanges));}
 void show(){LinearLayout body=a.vertical();body.setPaddingRelative(a.dp(12),0,a.dp(12),0);a.gap(body,8);search=a.edit(L10n.s(R.string.msg_875be13421),"");search.setSingleLine(true);search.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);body.addView(search);
  LinearLayout options=a.row();options.addView(a.text(L10n.s(R.string.msg_f51f551f4b),13,MainActivity.MUTED),new LinearLayout.LayoutParams(0,-2,1));Switch include=a.switchControl();include.setContentDescription(L10n.s(R.string.msg_cab718e280));options.addView(include);body.addView(options);
   final int maxListHeight=a.dp(Math.min(420,(int)(a.getResources().getDisplayMetrics().heightPixels/a.getResources().getDisplayMetrics().density*.5f)));FrameLayout frame=new FrameLayout(a);list=new ListView(a);list.setNestedScrollingEnabled(true);list.setDivider(null);list.setCacheColorHint(android.graphics.Color.TRANSPARENT);list.setVerticalScrollBarEnabled(false);frame.addView(list,new FrameLayout.LayoutParams(-1,-1));empty=a.text(L10n.s(R.string.msg_e5834c4ca1),14,MainActivity.MUTED);empty.setGravity(Gravity.CENTER);frame.addView(empty,new FrameLayout.LayoutParams(-1,-1));list.setEmptyView(empty);body.addView(frame,a.lp(-1,96));
   Adapter adapter=new Adapter();list.setAdapter(adapter);Runnable filter=()->{String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);shown.clear();for(Entry e:all)if((systems||!e.system||selected(e.pkg)||checked.containsKey(e.pkg))&&e.matches(query))shown.add(e);empty.setText(L10n.s(R.string.msg_1e1b70b17c));adapter.notifyDataSetChanged();ViewGroup.LayoutParams size=frame.getLayoutParams();size.height=Math.min(maxListHeight,a.dp(Math.max(72,shown.size()*62)));frame.setLayoutParams(size);};
   include.setOnCheckedChangeListener((b,on)->{systems=on;filter.run();});search.addTextChangedListener(a.watcher(filter));
   list.setOnItemClickListener((p,v,pos,id)->{Entry app=shown.get(pos);if(multiple()){toggle(app.pkg);adapter.bindSelection(v,(Holder)v.getTag(),app);}else{destination.setText(app.pkg);dialog.dismiss();}});
   AlertDialog.Builder builder=a.new DialogBuilder(a).setTitle(L10n.s(multiple()?R.string.app_picker_multiple:R.string.msg_7819ea82b0)).setView(body).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null);
   if(multiple())builder.setPositiveButton(L10n.s(R.string.msg_768af67762),(d,w)->onApply.accept(new LinkedHashMap<>(changes)));
   dialog=builder.create();dialog.setOnDismissListener(d->{generation++;icons.evictAll();});a.sheet(dialog);updateApply();long token=generation;
   a.io.execute(()->{try{List<Entry> loaded=installedApps(a);a.main.post(()->{if(token!=generation||a.isDestroyed()||!dialog.isShowing())return;all.addAll(loaded);filter.run();});}catch(Exception e){a.main.post(()->{if(dialog.isShowing())empty.setText(L10n.s(R.string.msg_8c5bfb9222));});}});
 }
 final class Holder {ImageView icon;TextView name,pkg;Icons mark;CheckBox checkbox;}
 final class Adapter extends BaseAdapter {
  public int getCount(){return shown.size();}public Object getItem(int position){return shown.get(position);}public long getItemId(int position){return position;}
  public View getView(int position,View reuse,ViewGroup parent){Holder h;LinearLayout row;
   if(reuse==null){row=a.row();row.setMinimumHeight(a.dp(62));row.setPaddingRelative(a.dp(4),a.dp(5),a.dp(4),a.dp(5));h=new Holder();h.icon=new ImageView(a);LinearLayout.LayoutParams ip=a.lp(30,30);ip.setMarginEnd(a.dp(10));row.addView(h.icon,ip);LinearLayout labels=a.vertical();h.name=a.bold("",14);h.name.setSingleLine(true);h.name.setEllipsize(android.text.TextUtils.TruncateAt.END);h.pkg=a.text("",11,MainActivity.MUTED);h.pkg.setSingleLine(true);h.pkg.setTextDirection(View.TEXT_DIRECTION_LTR);h.pkg.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);labels.addView(h.name);labels.addView(h.pkg);row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
    if(multiple()){
     h.checkbox=new CheckBox(a);h.checkbox.setFocusable(false);h.checkbox.setClickable(false);h.checkbox.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
     h.checkbox.setButtonTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{MainActivity.ACCENT,MainActivity.MUTED}));row.addView(h.checkbox,a.lp(40,40));
    }else{h.mark=new Icons(a,"check",MainActivity.ACCENT);row.addView(h.mark,a.lp(24,24));}
    row.setTag(h);
   }else{row=(LinearLayout)reuse;h=(Holder)row.getTag();}
   Entry e=shown.get(position);bindSelection(row,h,e);h.name.setText(e.name);h.pkg.setText(e.pkg);Drawable icon=icons.get(e.pkg);if(icon==null){icon=e.info.loadIcon(a.getPackageManager());icons.put(e.pkg,icon);}h.icon.setImageDrawable(icon);return row;
  }
  void bindSelection(View row,Holder h,Entry e){
   boolean on=selected(e.pkg);row.setBackground(a.selectionSurface(on));row.setContentDescription(e.name+", "+e.pkg+(on?L10n.s(R.string.msg_dd6b0e1e55):""));
   if(h.checkbox!=null)h.checkbox.setChecked(on);else h.mark.setVisibility(on?View.VISIBLE:View.INVISIBLE);
  }
 }
}
