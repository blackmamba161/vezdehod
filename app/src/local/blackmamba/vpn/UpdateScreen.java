package local.blackmamba.vpn;
import android.app.*;import android.content.*;import android.view.View;import android.widget.*;
final class UpdateScreen {
 final MainActivity a;TextView state,releaseInfo;Button check,action,cancel,notes;long shownCode=-1;
 UpdateScreen(MainActivity a){this.a=a;}
 void draw(){
  a.title(L10n.s(R.string.update_title),null);LinearLayout card=a.card(a.content);
  a.note(card,"Vezdehod "+AppUpdates.version(a));state=a.text("",13,MainActivity.MUTED);card.addView(state);releaseInfo=a.bold("",15);card.addView(releaseInfo);
  check=a.button(L10n.s(R.string.update_check),()->AppUpdates.check(a,true,error->{if(!a.isDestroyed()){if(error!=null)a.message(error);refresh();}}));card.addView(check,a.lp(-1,48));
  action=a.primary("",()->{try{if(AppUpdates.available(a)!=null&&AppUpdates.prefs(a).getLong("downloadCode",0)==AppUpdates.available(a).code&&AppUpdates.downloadState(a).status==DownloadManager.STATUS_SUCCESSFUL)a.new DialogBuilder(a).setTitle(L10n.s(R.string.update_install)).setMessage(L10n.s(R.string.update_install_hint)).setNegativeButton(L10n.s(R.string.msg_0ec753be8d),null).setPositiveButton(L10n.s(R.string.update_install),(d,w)->AppUpdates.install(a)).show();else{UpdateRelease r=AppUpdates.available(a);if(r!=null)AppUpdates.download(a,r);}}catch(Exception e){AppUpdates.failure=L10n.s(R.string.update_download_failed);}refresh();});card.addView(action,a.lp(-1,48));
  cancel=a.button(L10n.s(R.string.msg_0ec753be8d),()->{AppUpdates.cancelDownload(a);refresh();});card.addView(cancel,a.lp(-1,48));
  notes=a.button(L10n.s(R.string.update_notes),this::notes);card.addView(notes,a.lp(-1,48));
  LinearLayout settings=a.card(a.content);LinearLayout row=a.row();row.addView(a.bold(L10n.s(R.string.update_auto),14),new LinearLayout.LayoutParams(0,-2,1));Switch auto=a.switchControl();auto.setContentDescription(L10n.s(R.string.update_auto));auto.setChecked(AppUpdates.prefs(a).getBoolean("auto",true));auto.setOnCheckedChangeListener((v,on)->{AppUpdates.configure(a,"auto",on);a.render();});row.addView(auto);settings.addView(row);
  refresh();
 }
 void refresh(){
  if(state==null)return;UpdateRelease r=AppUpdates.available(a);AppUpdates.Progress p=AppUpdates.downloadState(a);boolean matching=r!=null&&AppUpdates.prefs(a).getLong("downloadCode",0)==r.code;
  boolean running=matching&&(p.status==DownloadManager.STATUS_RUNNING||p.status==DownloadManager.STATUS_PENDING||p.status==DownloadManager.STATUS_PAUSED);boolean ready=matching&&p.status==DownloadManager.STATUS_SUCCESSFUL;
  String text=AppUpdates.checking?L10n.s(R.string.update_checking):!AppUpdates.failure.isEmpty()?AppUpdates.failure:r!=null?L10n.s(R.string.update_available):AppUpdates.prefs(a).contains("lastCheck")?L10n.s(R.string.update_latest):"";
  if(running)text=p.status==DownloadManager.STATUS_RUNNING?(p.total>0?Math.min(100,p.bytes*100/p.total)+"%":"…"):L10n.s(R.string.update_waiting);
  if(matching&&p.status==DownloadManager.STATUS_FAILED)text=L10n.s(R.string.update_download_failed);
  if(AppUpdates.verifying)text=L10n.s(R.string.update_verifying);
  state.setText(text);state.setVisibility(text.isEmpty()?View.GONE:View.VISIBLE);check.setEnabled(!AppUpdates.checking);
  releaseInfo.setVisibility(r==null?View.GONE:View.VISIBLE);if(r!=null)releaseInfo.setText("Vezdehod "+r.version+" · "+android.text.format.Formatter.formatShortFileSize(a,r.size));
  action.setVisibility(r==null||running?View.GONE:View.VISIBLE);action.setEnabled(!AppUpdates.verifying);action.setText(L10n.s(ready?R.string.update_install:R.string.update_download));
  cancel.setVisibility(running?View.VISIBLE:View.GONE);notes.setVisibility(r==null?View.GONE:View.VISIBLE);
 }
 void notes(){UpdateRelease r=AppUpdates.available(a);if(r==null)return;String body=Compat.blank(r.notes)?"Vezdehod "+r.version:r.notes;a.new DialogBuilder(a).setTitle(L10n.s(R.string.update_notes)).setMessage(body).setPositiveButton(L10n.s(R.string.msg_4ae50d3073),null).show();}
}
