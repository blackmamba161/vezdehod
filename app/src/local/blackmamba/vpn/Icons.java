package local.blackmamba.vpn;
import android.content.Context;
import android.graphics.*;
import android.view.View;

/** A single 24 dp grid, rounded 1.8 dp strokes and consistent optical weight. */
public final class Icons extends View {
 private String name; private final int color; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 public Icons(Context c,String name,int color){super(c);this.name=name;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 @Override public void setContentDescription(CharSequence text){super.setContentDescription(text);if(text!=null){setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setFocusable(true);setTooltipText(text);}}
 public void setName(String value){name=value;invalidate();}
 private void line(Canvas c,float... xy){Path path=new Path();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);c.drawPath(path,p);}
 private void dot(Canvas c,float x,float y){p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,1,p);p.setStyle(Paint.Style.STROKE);}
 @Override protected void onDraw(Canvas c){super.onDraw(c);float scale=Math.min(getWidth()-getPaddingLeft()-getPaddingRight(),getHeight()-getPaddingTop()-getPaddingBottom())/24f;c.save();c.translate((getWidth()-24*scale)/2,(getHeight()-24*scale)/2);c.scale(scale,scale);p.setColor(color);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);
  if(getLayoutDirection()==LAYOUT_DIRECTION_RTL&&(name.equals("chevron")||name.equals("back"))){c.translate(24,0);c.scale(-1,1);}
  switch(name){
   case "download":line(c,12,3,12,15);line(c,7,10,12,15,17,10);line(c,4,16,4,21,20,21,20,16);break;
   case "check":line(c,4,12,9,17,20,6);break;
   case "copy":c.drawRoundRect(8,8,21,21,2,2,p);line(c,16,5,16,3,3,3,3,16,5,16);break;
   case "ping":c.drawArc(3,4,21,22,180,180,false,p);line(c,3,13,3,17);line(c,21,13,21,17);line(c,12,14,17,8);dot(c,12,14);break;
   case "refresh":c.drawArc(4,4,20,20,45,285,false,p);line(c,20,3,20,9,14,9);break;
   case "back":line(c,14,5,7,12,14,19);break;
   case "language":c.drawCircle(12,12,9,p);c.drawOval(8,3,16,21,p);line(c,3,12,21,12);break;
   case "direct":line(c,4,12,20,12);line(c,14,6,20,12,14,18);break;
   case "block":c.drawCircle(12,12,9,p);line(c,6,6,18,18);break;
   case "home":line(c,3,10,12,3,21,10);line(c,5,9,5,21,10,21,10,15,14,15,14,21,19,21,19,9);break;
   case "servers":c.drawRoundRect(3,3,21,10,2,2,p);c.drawRoundRect(3,14,21,21,2,2,p);dot(c,7,6.5f);dot(c,7,17.5f);line(c,12,6.5f,17,6.5f);line(c,12,17.5f,17,17.5f);break;
   case "networks":line(c,3,7,21,7,17,3);line(c,21,7,17,11);line(c,21,17,3,17,7,13);line(c,3,17,7,21);break;
   case "rules":line(c,10,5,21,5);line(c,10,12,21,12);line(c,10,19,21,19);line(c,3,5,4.5f,6.5f,7,3.5f);line(c,3,12,4.5f,13.5f,7,10.5f);line(c,3,19,4.5f,20.5f,7,17.5f);break;
   case "profiles":c.drawRoundRect(3,4,21,20,3,3,p);line(c,3,9,21,9);line(c,7,14,12,14);line(c,7,17,10,17);break;
   case "settings":for(int i=0;i<6;i++){c.save();c.rotate(i*60,12,12);line(c,10,3,10,1.8f,14,1.8f,14,3);c.restore();}c.drawCircle(12,12,8.2f,p);c.drawCircle(12,12,3,p);break;
   case "shield":line(c,12,2.5f,20,5.5f,20,12,18,17,12,21.5f,6,17,4,12,4,5.5f,12,2.5f);line(c,8,11.5f,11,14.5f,16,9);break;
   case "appearance":c.drawCircle(12,12,9,p);p.setStyle(Paint.Style.FILL);c.drawArc(3,3,21,21,90,180,true,p);break;
   case "bell":c.drawArc(6,3,18,15,180,180,false,p);line(c,6,9,6,14,3.5f,18,20.5f,18,18,14,18,9);c.drawArc(9,18,15,23,20,140,false,p);break;
   case "diagnostics":line(c,2,12,6,12,9,5,13,19,16,12,22,12);break;
   case "wifi":c.drawArc(1,4,23,26,225,90,false,p);c.drawArc(5,9,19,23,225,90,false,p);c.drawArc(9,14,15,20,225,90,false,p);dot(c,12,20);break;
   case "cellular":line(c,4,20,4,17);line(c,9,20,9,13);line(c,14,20,14,9);line(c,19,20,19,4);break;
   case "qr":c.drawRect(3,3,9,9,p);c.drawRect(15,3,21,9,p);c.drawRect(3,15,9,21,p);line(c,15,15,18,15,18,18,21,18,21,21);line(c,15,18,15,21,18,21);break;
   case "clipboard":c.drawRoundRect(5,4,19,22,2,2,p);c.drawRoundRect(8,2,16,6,1,1,p);line(c,9,11,15,11);line(c,9,16,15,16);break;
   case "link":c.save();c.rotate(-45,12,12);c.drawRoundRect(4,8,14,16,4,4,p);c.drawRoundRect(10,8,20,16,4,4,p);c.restore();break;
   case "plus":line(c,12,5,12,19);line(c,5,12,19,12);break;
   case "move-up":line(c,12,20,12,4);line(c,6,10,12,4,18,10);break;
   case "move-down":line(c,12,4,12,20);line(c,6,14,12,20,18,14);break;
   case "up":line(c,6,15,12,9,18,15);break;
   case "down":line(c,6,9,12,15,18,9);break;
   case "chevron":line(c,9,5,16,12,9,19);break;
   case "close":line(c,6,6,18,18);line(c,18,6,6,18);break;
   case "edit":line(c,4,20,5,15,16,4,20,8,9,19,4,20);line(c,14,6,18,10);break;
   case "trash":line(c,4,6,20,6);line(c,9,6,9,3,15,3,15,6);line(c,6,6,7,21,17,21,18,6);line(c,10,10,10,17);line(c,14,10,14,17);break;
   default:c.drawCircle(12,12,9,p);line(c,12,10,12,16);dot(c,12,7);
  }c.restore();
 }
}
