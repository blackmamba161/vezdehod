package local.blackmamba.vpn;

/** Immutable recovery status shared by the worker, main screen and notification. */
final class VpnRecovery {
 static final int INTERVAL_MS=15_000;
 static final VpnRecovery CHECKING=new VpnRecovery(0);
 private final long nextCheckAt;
 private VpnRecovery(long nextCheckAt){this.nextCheckAt=nextCheckAt;}
 static VpnRecovery waitFrom(long now){return new VpnRecovery(now+INTERVAL_MS);}
 boolean isDue(long now){return this!=CHECKING&&now>=nextCheckAt;}
 int remainingMillis(long now){return (int)Math.min(INTERVAL_MS,Math.max(0,nextCheckAt-now));}
 int remainingSeconds(long now){return (remainingMillis(now)+999)/1000;}
}
