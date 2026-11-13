package local.blackmamba.vpn;
public final class VezdehodApp extends android.app.Application {
 @Override public void onCreate(){super.onCreate();L10n.init(this);RuntimeSecrets.cleanup(this);AppUpdates.cleanInstalled(this);AppUpdates.schedule(this);}
 @Override public void onConfigurationChanged(android.content.res.Configuration c){super.onConfigurationChanged(c);L10n.invalidate();}
}
