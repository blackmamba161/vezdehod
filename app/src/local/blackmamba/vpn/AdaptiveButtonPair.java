package local.blackmamba.vpn;

import android.widget.*;

/** Keep a pair of actions level, or stack them when both full labels cannot fit. */
final class AdaptiveButtonPair extends LinearLayout {
  final int gap;
  AdaptiveButtonPair(MainActivity a,Button first,Button second){super(a);gap=a.dp(8);setBaselineAligned(false);
   for(Button b:new Button[]{first,second}){if(b.getBackground() instanceof android.graphics.drawable.InsetDrawable)b.setBackground(((android.graphics.drawable.InsetDrawable)b.getBackground()).getDrawable());b.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);b.setTextSize(14*a.uiScale);b.setSingleLine(false);b.setMaxLines(Integer.MAX_VALUE);b.setEllipsize(null);addView(b,new LayoutParams(-1,-2));}
  }
  @Override protected void onMeasure(int widthSpec,int heightSpec){
   int available=MeasureSpec.getSize(widthSpec)-gap;
   int back=labelWidth((Button)getChildAt(0)),proceed=labelWidth((Button)getChildAt(1));
   boolean stack=back+proceed>available;int first=Math.max(back,Math.min(available/2,available-proceed));
   setOrientation(stack?VERTICAL:HORIZONTAL);
   for(int i=0;i<getChildCount();i++){LayoutParams p=(LayoutParams)getChildAt(i).getLayoutParams();p.width=stack?-1:(i==0?first:available-first);p.height=stack?-2:-1;p.setMarginStart(!stack&&i>0?gap:0);p.topMargin=stack&&i>0?gap:0;p.resolveLayoutDirection(getLayoutDirection());}
   super.onMeasure(widthSpec,heightSpec);
  }
  int labelWidth(Button b){return Math.max(b.getMinimumWidth(),(int)Math.ceil(android.text.Layout.getDesiredWidth(b.getText(),b.getPaint()))+b.getCompoundPaddingLeft()+b.getCompoundPaddingRight());}
 }
