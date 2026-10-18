#!/usr/bin/env python3
"""Fail closed: requires real signed-binary acceptance; never creates evidence."""
import hashlib,json,os,re,subprocess,sys
from pathlib import Path
from source_fingerprint import fingerprint

required=('discovery','qr_camera_both_directions','manual_connection','approve_reject','single_4mb','single_100mb','single_1gb','multiple_files','duplicate_names','interruption_cleanup','sha256_integrity','media_permissions_denied_partial','camera_permission_denied','apk_share_read_access','shared_apk_install','light_dark_small_screen','background_transfer')
def verify(evidence,metadata,apk):
    assert evidence.get('completed') is True,'Physical two-phone acceptance has not been completed'
    devices=evidence.get('devices',[]);assert len(devices)>=2 and all(d.get('model') and d.get('android') for d in devices),'Two named Android devices required'
    assert all(evidence.get('results',{}).get(k)=='pass' for k in required),'Physical acceptance matrix has missing/failing results'
    assert evidence.get('tested_at') and evidence.get('tested_by'),'Tester and date required'
    assert metadata['version_name']=='1.0.0' and metadata['package_id']=='com.ezshare.app' and metadata['debuggable'] is False
    actual=hashlib.sha256(Path(apk).read_bytes()).hexdigest()
    assert actual==metadata['apk_sha256']==evidence['apk_sha256'],'Physical tests must cover this exact signed APK'
    assert metadata['source_fingerprint']==evidence['source_fingerprint']==fingerprint(),'Source changed since physical validation'
    assert metadata['certificate_sha256']==evidence['certificate_sha256'],'Certificate changed since validation'
    expected=os.environ.get('EZ_RELEASE_CERT_SHA256','').lower().replace(':','').replace(' ','')
    assert len(expected)==64 and expected==metadata['certificate_sha256'],'Expected persistent release certificate must match'
    assert re.fullmatch('[a-f0-9]{40}',metadata['source_commit']),'Invalid candidate commit'
    approved=os.environ.get('EZ_RELEASE_APPROVED_SHA','')
    assert approved==metadata['source_commit'],'Repository release approval must match tested candidate commit'
    return True
if __name__=='__main__':
    verify(json.loads(Path(sys.argv[1]).read_text()),json.loads(Path(sys.argv[2]).read_text()),sys.argv[3]);print('Physical, binary, source and signing gates passed')
