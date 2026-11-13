package local.blackmamba.vpn;
import android.content.*;import android.net.VpnService;
public final class BootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent intent){if(!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))return;try{if(Store.get(c).data.optBoolean("boot",false)&&VpnService.prepare(c)==null){EventLog.add(c,L10n.s(R.string.msg_04926ac15c));TunnelService.start(c);}}catch(Exception e){EventLog.add(c,L10n.s(R.string.msg_04d374e07a));}}
}
