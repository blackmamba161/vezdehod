package local.blackmamba.vpn;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

/** Window owns the perimeter; title, body and actions only own gaps between sections. */
final class SheetSpacing {
 static final int EDGE=20;
 private static View part(View decor,String name){
  int id=decor.getResources().getIdentifier(name,"id","android");
  return id==0?null:decor.findViewById(id);
 }
 static void apply(MainActivity a,View decor){
  int edge=a.dp(EDGE);decor.setPaddingRelative(edge,edge,edge,edge);
  View title=part(decor,"title_template");if(title!=null)title.setPaddingRelative(0,0,0,a.dp(10));
  View message=decor.findViewById(android.R.id.message);if(message!=null)message.setPaddingRelative(0,0,0,0);
  // These platform spacers normally supply the outer margin when a section is absent.
  for(String name:new String[]{"titleDividerNoCustom","textSpacerNoTitle","textSpacerNoButtons"}){
   View spacer=part(decor,name);if(spacer!=null)spacer.setVisibility(View.GONE);
  }
  View footer=part(decor,"buttonPanel");if(footer!=null)footer.setPaddingRelative(0,0,0,0);
 }
 static void footer(MainActivity a,LinearLayout panel){
  panel.setPaddingRelative(0,a.dp(12),0,0);int visible=0;
  for(int i=0;i<panel.getChildCount();i++){
   View child=panel.getChildAt(i);if(!(child instanceof Button)){child.setVisibility(View.GONE);continue;}
   if(child.getVisibility()!=View.VISIBLE)continue;
   LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)child.getLayoutParams();
   lp.setMargins(0,0,0,0);lp.setMarginStart(visible++==0?0:a.dp(8));lp.setMarginEnd(0);child.setLayoutParams(lp);
  }
 }
}
