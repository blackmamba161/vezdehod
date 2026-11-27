package local.blackmamba.vpn;
import android.content.Context;import android.graphics.*;import android.os.SystemClock;import android.view.View;
/** Outgoing radar: rings leave the centre and fade while a ping is in flight. Idle shows a muted static mark. */
final class PingRadar extends View{
 private static final int RINGS=3;private static final long PERIOD=1800;
 private final Paint ring=new Paint(Paint.ANTI_ALIAS_FLAG),core=new Paint(Paint.ANTI_ALIAS_FLAG);
 private boolean active;private long started;
 private final Runnable frame=new Runnable(){public void run(){if(!active)return;invalidate();postOnAnimation(this);}};
 PingRadar(Context c){super(c);ring.setStyle(Paint.Style.STROKE);ring.setStrokeWidth(2f);core.setStyle(Paint.Style.FILL);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 /** Show the live radar only while a measurement is running; idle draws a muted static mark. */
 void active(boolean value){if(active==value)return;active=value;removeCallbacks(frame);if(value){started=SystemClock.uptimeMillis();postOnAnimation(frame);}else invalidate();}
 @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();removeCallbacks(frame);}
 @Override protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)-2f;
  if(!active){/* Idle: a quiet dot inside one thin ring so the slot never looks empty. */core.setColor(MainActivity.MUTED);core.setAlpha(160);c.drawCircle(x,y,Math.max(2.5f,r*.18f),core);ring.setColor(MainActivity.MUTED);ring.setAlpha(90);ring.setStrokeWidth(1.2f);c.drawCircle(x,y,r*.62f,ring);return;}
  long t=SystemClock.uptimeMillis()-started;core.setColor(MainActivity.ACCENT);core.setAlpha(255);c.drawCircle(x,y,Math.max(3f,r*.2f),core);
  ring.setColor(MainActivity.ACCENT);ring.setStrokeWidth(2f);
  for(int i=0;i<RINGS;i++){float phase=((float)(t%PERIOD)/PERIOD+i/(float)RINGS)%1f;float radius=r*(0.2f+phase*0.8f);ring.setAlpha((int)(255*(1f-phase)));c.drawCircle(x,y,radius,ring);}
 }
}
