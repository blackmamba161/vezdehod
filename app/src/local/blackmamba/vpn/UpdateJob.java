package local.blackmamba.vpn;
import android.app.job.*;
public final class UpdateJob extends JobService {
 private volatile boolean stopped;
 @Override public boolean onStartJob(JobParameters p){stopped=false;AppUpdates.check(this,false,error->{if(!stopped)jobFinished(p,false);});return true;}
 @Override public boolean onStopJob(JobParameters p){stopped=true;return false;}
}
