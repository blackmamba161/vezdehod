package local.blackmamba.vpn;

import android.view.*;
import android.widget.*;
import java.text.Collator;
import java.util.*;

/** View state for one filtered list; only move existing rows when the order changes. */
final class ServerListSort {
 private final MainActivity a;
 private final LinearLayout list;
 private final Map<String,String> names=new LinkedHashMap<>();
 private final Map<String,View> rows=new HashMap<>();
 private final Map<String,Integer> snapshot=new HashMap<>();
 private final Collator collator=Collator.getInstance(L10n.locale());
 private final String[] modes={ServerOrder.PING,ServerOrder.NAME,ServerOrder.SOURCE};
 private final int[] labels={R.string.sort_ping,R.string.sort_name,R.string.sort_source};
 private boolean dirty=true;
 private String mode;
 ServerListSort(MainActivity activity,LinearLayout list){this(activity,list,activity.store.data.optString(ServerOrder.KEY,ServerOrder.PING));}
 ServerListSort(MainActivity activity,LinearLayout list,String initialMode){a=activity;this.list=list;mode=initialMode;if(!Arrays.asList(modes).contains(mode))mode=ServerOrder.PING;collator.setStrength(Collator.SECONDARY);}
 void reset(){rows.clear();names.clear();snapshot.clear();dirty=true;}
 void add(String id,String name,View row){names.put(id,name);rows.put(id,row);}
 void refresh(){
  if(ServerOrder.PING.equals(mode))for(String id:names.keySet()){Integer value=a.visiblePing(id);if(!Objects.equals(snapshot.put(id,value),value))dirty=true;}
  if(!dirty)return;dirty=false;
  int i=0;for(String id:ServerOrder.sorted(mode,names,snapshot,collator)){View row=rows.get(id);if(list.getChildAt(i)!=row){list.removeView(row);list.addView(row,i);}i++;}
 }
 private String description(){return L10n.s(R.string.server_sort)+": "+L10n.s(labels[Arrays.asList(modes).indexOf(mode)]);}
 View button(){
  Icons button=new Icons(a,"sort",MainActivity.ACCENT);button.setBackground(a.selectable());button.setPaddingRelative(a.dp(11),a.dp(11),a.dp(11),a.dp(11));button.setContentDescription(description());
  button.setOnClickListener(v->{PopupMenu menu=new PopupMenu(a,button,Gravity.END);for(int i=0;i<modes.length;i++)menu.getMenu().add(1,i+1,i,L10n.s(labels[i]));menu.getMenu().setGroupCheckable(1,true,true);menu.getMenu().findItem(Arrays.asList(modes).indexOf(mode)+1).setChecked(true);
   menu.setOnMenuItemClickListener(item->{String next=modes[item.getItemId()-1];if(!next.equals(mode))try{a.store.change(ServerOrder.KEY,next);mode=next;dirty=true;refresh();button.setContentDescription(description());}catch(Exception e){a.message(TunnelService.safeError(e));}return true;});menu.show();});
  return button;
 }
}
