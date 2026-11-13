package local.blackmamba.vpn;
/** Hysteresis prevents changing servers on a single transient probe failure. */
public final class FailoverPolicy {
 public enum Decision { KEEP, RESERVE, PRIMARY }
 private int failures,recoveries;
 public void reset(){failures=0;recoveries=0;}
 public Decision observe(boolean usingReserve,boolean currentOk,boolean candidateOk){
  if(usingReserve){failures=0;recoveries=candidateOk?recoveries+1:0;if(recoveries>=2){reset();return Decision.PRIMARY;}}
  else{recoveries=0;failures=currentOk?0:failures+1;if(failures>=2&&candidateOk){reset();return Decision.RESERVE;}}
  return Decision.KEEP;
 }
}
