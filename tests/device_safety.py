#!/usr/bin/env python3
from pathlib import Path
import subprocess,queue,threading,time,re,json,sys,argparse,shutil,os
parser=argparse.ArgumentParser(description='Bounded VPN tests on an explicitly selected Android device. Stop any working VPN before instrumentation.')
parser.add_argument('--adb',default=os.environ.get('ADB') or shutil.which('adb'))
parser.add_argument('--serial',required=True)
parser.add_argument('--timeout',type=int,default=100)
parser.add_argument('--output',type=Path,default=Path(__file__).resolve().parents[1]/'private/device-tests')
parser.add_argument('mode',choices=['safety-018','connection-safety-018','native-016','probe-016','dns-quality','audit-vpn','ui'])
args=parser.parse_args()
if not args.adb:parser.error('Install adb or supply --adb /path/to/adb')
ADB=args.adb;serial=args.serial;mode=args.mode;timeout=args.timeout
outdir=args.output;outdir.mkdir(parents=True,exist_ok=True,mode=0o700)
a=[ADB,'-s',serial];start=time.monotonic();lines=[];samples=[];q=queue.Queue()
p=subprocess.Popen(a+['shell','am','instrument','-w','-e','mode',mode,'local.blackmamba.vpn/.SelfTest'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1)
def reader():
 for l in p.stdout:q.put(l)
 q.put(None)
threading.Thread(target=reader,daemon=True).start();sample_at=0;failure=None
try:
 while time.monotonic()-start<timeout:
  try:l=q.get(timeout=.5)
  except queue.Empty:l=''
  if l is None:break
  if l:lines.append(l);print(l.rstrip(),flush=True)
  if time.monotonic()>sample_at:
   sample_at=time.monotonic()+4
   r=subprocess.run(a+['shell','dumpsys','meminfo','local.blackmamba.vpn'],capture_output=True,text=True,timeout=8)
   m=re.search(r'TOTAL PSS:\s*(\d+)',r.stdout) or re.search(r'^\s*TOTAL\s+(\d+)',r.stdout,re.M)
   if m:
    kb=int(m[1]);samples.append({'elapsed':round(time.monotonic()-start,2),'pss_kb':kb})
    if kb>320*1024:raise RuntimeError('Test app exceeded 320 MiB PSS; stopping to protect the low-memory phone')
 else:raise TimeoutError('Device test deadline exceeded')
except Exception as e:failure=str(e)
finally:
 if p.poll() is None:
  subprocess.run(a+['shell','am','force-stop','local.blackmamba.vpn'],capture_output=True,timeout=15)
  try:p.wait(timeout=5)
  except subprocess.TimeoutExpired:p.kill()
 raw=''.join(lines);passed='INSTRUMENTATION_CODE: -1' in raw and 'INSTRUMENTATION_RESULT: result=PASS' in raw
 report={'mode':mode,'passed':passed,'elapsed':round(time.monotonic()-start,2),'peak_pss_kb':max((x['pss_kb'] for x in samples),default=0),'failure':failure,'samples':samples}
 (outdir/(mode+'.log')).write_text(raw);(outdir/(mode+'.json')).write_text(json.dumps(report,indent=2));print(json.dumps(report),flush=True)
if failure or not passed:sys.exit(1)
