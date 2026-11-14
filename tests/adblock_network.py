"""Exercise real core DNS/route filtering on loopback; no internet requests."""
import socket,threading,struct,json,subprocess,tempfile,time,pathlib
root=pathlib.Path(__file__).resolve().parents[1]
base=json.loads((root/'.build/adblock-tests/config-wifi.json').read_text())
dns=socket.socket(socket.AF_INET,socket.SOCK_DGRAM);dns.bind(('127.0.0.1',0))
http=socket.socket();http.bind(('127.0.0.1',0));http.listen()
stop=threading.Event()
def dnsloop():
 while not stop.is_set():
  try:
   msg,addr=dns.recvfrom(4096);end=12
   while msg[end]:end+=msg[end]+1
   end+=5;typ=struct.unpack('!H',msg[end-4:end-2])[0]
   answer=b'\xc0\x0c'+struct.pack('!HHIH',1,1,0,4)+socket.inet_aton('127.0.0.1') if typ==1 else b''
   dns.sendto(msg[:2]+struct.pack('!HHHHH',0x8180,1,int(bool(answer)),0,0)+msg[12:end]+answer,addr)
  except OSError:return
def httploop():
 while not stop.is_set():
  try:
   c,_=http.accept();c.settimeout(2);c.recv(4096);c.sendall(b'HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nOK');c.close()
  except OSError:return
threading.Thread(target=dnsloop,daemon=True).start();threading.Thread(target=httploop,daemon=True).start()
def port(kind=socket.SOCK_STREAM):
 with socket.socket(socket.AF_INET,kind) as s:s.bind(('127.0.0.1',0));return s.getsockname()[1]
def query(p,domain):
 msg=b'\xab\xcd'+struct.pack('!HHHHH',0x0100,1,0,0,0)+b''.join(bytes([len(v)])+v.encode() for v in domain.split('.'))+b'\x00\x00\x01\x00\x01'
 with socket.socket(socket.AF_INET,socket.SOCK_DGRAM) as s:
  s.settimeout(3);s.sendto(msg,('127.0.0.1',p));r=s.recv(4096);return r[3]&15,struct.unpack('!H',r[6:8])[0]
try:
 for mode in ['on','exception','off']:
  c=json.loads(json.dumps(base));socks=port();dnsin=port(socket.SOCK_DGRAM)
  c['inbounds']=[{'type':'mixed','listen':'127.0.0.1','listen_port':socks},{'type':'direct','listen':'127.0.0.1','listen_port':dnsin,'network':'udp'}]
  c['dns']['servers']=[{'tag':'local','address':'udp://127.0.0.1:'+str(dns.getsockname()[1]),'detour':'direct'},{'tag':'remote','address':'udp://127.0.0.1:'+str(dns.getsockname()[1]),'detour':'direct'}]
  c['dns']['strategy']='ipv4_only';c['route'].pop('auto_detect_interface',None)
  if mode=='exception':
   for rules in [c['dns']['rules'],c['route']['rules']]:
    for i,r in enumerate(rules):
     if r.get('rule_set')==['ads-light']:rules[i]={'type':'logical','mode':'and','rules':[{'rule_set':['ads-light']},{'domain_suffix':['doubleclick.net'],'invert':True}],'action':'reject'}
  if mode=='off':
   c['route'].pop('rule_set',None)
   for section in ['dns','route']:c[section]['rules']=[r for r in c[section]['rules'] if 'rule_set' not in r]
  with tempfile.TemporaryDirectory() as td:
   pth=pathlib.Path(td)/'config.json';pth.write_text(json.dumps(c))
   with open(pathlib.Path(td)/'core.log','w+') as log:
    core=subprocess.Popen([str(root/'.build/sing-box-linux'),'run','-c',str(pth)],stdout=log,stderr=log)
    try:
     for _ in range(40):
      try:
       with socket.create_connection(('127.0.0.1',socks),.1):break
      except OSError:time.sleep(.05)
     assert core.poll() is None,'core failed'
     for domain,blocked in [('example.com',False),('g.doubleclick.net',mode=='on')]:
      res=subprocess.run(['curl','--noproxy','','--socks5-hostname','127.0.0.1:'+str(socks),'--max-time','3','-sS','http://'+domain+':'+str(http.getsockname()[1])],capture_output=True,text=True)
      assert (res.returncode!=0)==blocked,(mode,domain,res.returncode,res.stderr)
      code,count=query(dnsin,domain)
      assert (code!=0 or count==0)==blocked,(mode,domain,code,count)
     print('PASS real core DNS + connection:',mode,flush=True)
    finally:core.terminate();core.wait(timeout=5)
finally:stop.set();dns.close();http.close()
