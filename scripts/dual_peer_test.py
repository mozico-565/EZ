#!/usr/bin/env python3
"""Drive two real Android processes. ADB bridges independent emulator NATs; not a LAN/mDNS or optical-camera claim."""
import argparse, hashlib, json, pathlib, socket, subprocess, time, traceback
p=argparse.ArgumentParser();p.add_argument('--baseline',action='store_true');p.add_argument('--output',required=True);args=p.parse_args()
out=pathlib.Path(args.output);out.mkdir(parents=True,exist_ok=True)
serials={'A':'emulator-5554','B':'emulator-5556'};controls={'A':40001,'B':40002};data={'A':45001,'B':45002};events=[];processes=[]
def adb(peer,*command,check=True):
 return subprocess.run(['adb','-s',serials[peer],*command],capture_output=True,check=check)
def event(name,**fields):
 row={'time':time.time(),'event':name,**fields};events.append(row);print(json.dumps(row),flush=True)
def rpc(peer,op='state',**values):
 with socket.create_connection(('127.0.0.1',controls[peer]),5) as s:
  s.settimeout(20);s.sendall((json.dumps({'op':op,**values})+'\n').encode());reply=json.loads(s.makefile().readline())
  if not reply.get('ok'):raise AssertionError((peer,op,reply))
  return reply
def wait(peer,predicate,timeout=25):
 end=time.monotonic()+timeout;state={}
 while time.monotonic()<end:
  try:
   state=rpc(peer)
   if predicate(state):return state
  except (OSError,ValueError):pass
  time.sleep(.12)
 raise AssertionError(f'{peer} state timeout: {state}')
def receive(peer):
 rpc(peer,'receive');s=wait(peer,lambda s:s['listening'] and s['port']>0)
 adb(peer,'forward',f'tcp:{data[peer]}',f'tcp:{s["port"]}');return s
payload=bytes(i%251 for i in range(65536))*2+bytes([0]);digest=hashlib.sha256(payload).hexdigest()
def transfer(sender,receiver,qr=False,reject=False):
 state=receive(receiver)
 old=rpc(sender).get('operation');address=f'10.0.2.2:{data[receiver]}'
 if qr:
  encoded=rpc(receiver,'qr',host='10.0.2.2',port=data[receiver])['qr'];rpc(sender,'send',qr=encoded)
 else:rpc(sender,'send',address=address)
 if args.baseline and sender=='B':
  s=rpc(sender)
  if s['listening'] and not s['busy'] and 'أوقف' in s['status']:
   event('REPRODUCED_ROLE_REVERSAL_BLOCKED',sender=sender,receiver=receiver,status=s['status']);return False
 a=wait(sender,lambda s:s['request']);b=wait(receiver,lambda s:s['request'])
 assert a['sas']==b['sas'], 'SAS mismatch'
 event('explicit_pairing',sender=sender,receiver=receiver,sas_equal=True,qr_bitmap=qr)
 rpc(sender,'decide',accept=True);rpc(receiver,'decide',accept=not reject)
 if reject:
  wait(sender,lambda s:not s['busy'] and bool(s.get('error')));wait(receiver,lambda s:not s['busy']);event('rejection_closed_without_success');return True
 for peer in (sender,receiver):
  s=wait(peer,lambda s:not s['busy'] and s['stage']=='result' and s.get('operation')!=old if peer==sender else not s['busy'] and s['stage']=='result')
  assert s['results']==1 and not s.get('error'),s
 r=rpc(receiver);received=[h for h in r['history'] if h['incoming'] and h['status']=='completed']
 assert received and all(h['sha']==h['storedSha']==digest and h['bytes']==len(payload) for h in received)
 event('transfer_verified',sender=sender,receiver=receiver,bytes=len(payload),sha256=digest,qr_bitmap=qr)
 return True
try:
 for peer in serials:
  adb(peer,'forward',f'tcp:{controls[peer]}','tcp:39441')
  f=(out/f'{peer}-instrumentation.txt').open('wb')
  processes.append(subprocess.Popen(['adb','-s',serials[peer],'shell','am','instrument','-w','-r','-e','class','com.ezshare.app.NetworkPeerHarnessTest','-e','ez_peer_harness','true','com.ezshare.app.test/androidx.test.runner.AndroidJUnitRunner'],stdout=f,stderr=subprocess.STDOUT))
  wait(peer,lambda s:True,120)
 assert transfer('A','B')
 if args.baseline:
  assert transfer('B','A') is False,'Expected baseline role reversal bug was not reproduced'
  rpc('A','expiredScan');s=rpc('A');event('baseline_expired_scan',has_visible_error=bool(s.get('error')),status=s['status'])
 else:
  assert transfer('B','A',qr=True)
  for i in range(6):assert transfer('A' if i%2==0 else 'B','B' if i%2==0 else 'A',qr=i%2==0)
  # Receiver is still listening; sender can retry without cancelling that listener.
  for i in range(3):assert transfer('A','B')
  previous=rpc('B');rpc('B','stop');rpc('B','receive');current=wait('B',lambda s:s['listening'] and s['port']>0)
  assert previous['nonce']!=current['nonce'] and current['expiry']>=previous['expiry']
  time.sleep(.3);assert rpc('B')['port']==current['port']
  event('restart_keeps_new_listener',old_port=previous['port'],new_port=current['port'],nonce_rotated=True)
  adb('B','forward',f'tcp:{data["B"]}',f'tcp:{current["port"]}')
  rpc('A','send',address=f'10.0.2.2:{data["B"]}',nonce=previous['nonce'])
  wait('A',lambda s:not s['busy'] and bool(s.get('error')));wait('B',lambda s:not s['busy'] and bool(s.get('error')));event('stale_qr_rejected')
  assert transfer('A','B',qr=True)
  rpc('A','expiredScan');assert rpc('A').get('error');event('expired_qr_visible_error')
  rpc('A','send',address='10.0.2.2:45999');wait('A',lambda s:not s['busy'] and bool(s.get('error')));event('connection_refused_visible_error')
  assert transfer('A','B',reject=True)
  before=len([h for h in rpc('B')['history'] if h['incoming'] and h['status']=='completed'])
  receive('B');rpc('A','send',address=f'10.0.2.2:{data["B"]}',size=64*1024*1024)
  a=wait('A',lambda s:s['request']);b=wait('B',lambda s:s['request']);assert a['sas']==b['sas']
  rpc('A','decide',accept=True);rpc('B','decide',accept=True)
  wait('B',lambda s:s['busy'] and s['status']=='جارٍ النقل الفعلي…');rpc('B','cancel')
  wait('A',lambda s:not s['busy'] and bool(s.get('error')));b=wait('B',lambda s:not s['busy'])
  after=len([h for h in b['history'] if h['incoming'] and h['status']=='completed'])
  assert before==after and b['results']==0
  event('interrupted_transfer_not_completed',completed_files_before=before,completed_files_after=after)
  assert transfer('A','B',qr=True)
  for i in range(30):rpc('B','stop');rpc('B','receive')
  s=wait('B',lambda s:s['listening'] and s['port']>0);time.sleep(.3);assert rpc('B')['port']==s['port'];event('rapid_restart_30_survives')
  assert transfer('A','B')
  receive('B');rpc('A','discover');time.sleep(3)
  event('nsd_observed_under_isolated_emulator_nat',A=rpc('A')['peers'],receiver_listening=rpc('B')['listening'],cross_device_multicast_verified=False)
 event('suite_passed',baseline=args.baseline,physical_phones=False,optical_camera_verified=False)
except Exception:
 event('suite_failed',traceback=traceback.format_exc());raise
finally:
 for peer in serials:
  (out/f'{peer}-before-shutdown.png').write_bytes(adb(peer,'exec-out','screencap','-p',check=False).stdout)
  try:rpc(peer,'shutdown')
  except Exception:pass
  (out/f'{peer}-logcat.txt').write_bytes(adb(peer,'logcat','-d','-v','threadtime',check=False).stdout)
  (out/f'{peer}-tcp-state.txt').write_bytes(adb(peer,'shell','ss','-tan',check=False).stdout)
  (out/f'{peer}-final.png').write_bytes(adb(peer,'exec-out','screencap','-p',check=False).stdout)
 for proc in processes:
  try:proc.wait(15)
  except subprocess.TimeoutExpired:proc.terminate()
 (out/'results.json').write_text(json.dumps(events,ensure_ascii=False,indent=2))
