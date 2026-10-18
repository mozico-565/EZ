#!/usr/bin/env python3
import hashlib,json,os,re,subprocess,sys
from pathlib import Path
from source_fingerprint import fingerprint
apk=Path(sys.argv[1]);sdk=Path(os.environ['ANDROID_HOME'])/'build-tools/35.0.0'
verify=subprocess.check_output([str(sdk/'apksigner'),'verify','--verbose','--print-certs',str(apk)],text=True)
cert=re.search(r'Signer #1 certificate SHA-256 digest: ([a-fA-F0-9]+)',verify)
assert cert,'Missing APK certificate';cert=cert.group(1).lower()
expected=os.environ.get('EZ_RELEASE_CERT_SHA256','').lower().replace(':','').replace(' ','')
assert len(expected)==64 and cert==expected,'Unexpected production signing certificate'
assert 'CN=Android Debug' not in verify and 'CN=EZ Android Debug' not in verify,'Development key is not a production identity'
badging=subprocess.check_output([str(sdk/'aapt'),'dump','badging',str(apk)],text=True)
assert "package: name='com.ezshare.app'" in badging and "versionName='1.0.0'" in badging
assert 'application-debuggable' not in badging,'Debuggable APK rejected'
metadata={'package_id':'com.ezshare.app','version_name':'1.0.0','debuggable':False,'apk_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'certificate_sha256':cert,'source_commit':os.environ['GITHUB_SHA'],'source_fingerprint':fingerprint()}
Path('candidate/build-metadata.json').write_text(json.dumps(metadata,indent=2)+'\n')
Path('candidate/SHA256SUMS.txt').write_text(metadata['apk_sha256']+'  '+apk.name+'\n')
print('Verified non-debug APK, package, version and persistent certificate')
