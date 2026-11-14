package local.blackmamba.vpn;
import android.app.ActivityManager;import android.content.Context;import io.nekohasekai.libbox.Libbox;
/** Configure the shared Go runtime once, using Android's low-memory classification. */
final class CoreRuntime {
 private static boolean configured;
 static synchronized void configure(Context c){if(configured)return;ActivityManager am=c.getSystemService(ActivityManager.class);Libbox.mobileMemoryBudget((am!=null&&am.isLowRamDevice()?64L:128L)*1024*1024);configured=true;}
}
