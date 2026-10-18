import hashlib,os,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
from source_fingerprint import fingerprint
from verify_release_gate import verify,required

class ReleaseGateTest(unittest.TestCase):
    def test_absent_phone_evidence_blocks_publication(self):
        with self.assertRaises(AssertionError):verify({'completed':False},{},'absent.apk')
    def test_gate_binds_exact_binary_and_source_and_certificate(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk=Path(tmp)/'fixture.apk';apk.write_bytes(b'synthetic gate fixture, not an APK')
            h=hashlib.sha256(apk.read_bytes()).hexdigest();cert='a'*64;commit='b'*40
            metadata={'version_name':'1.0.0','package_id':'com.ezshare.app','debuggable':False,'apk_sha256':h,'source_fingerprint':fingerprint(),'certificate_sha256':cert,'source_commit':commit}
            evidence={'completed':True,'devices':[{'model':'fixture1','android':'10'},{'model':'fixture2','android':'15'}],'results':dict.fromkeys(required,'pass'),'tested_at':'fixture','tested_by':'unit test only','apk_sha256':h,'source_fingerprint':fingerprint(),'certificate_sha256':cert}
            with patch.dict(os.environ,{'EZ_RELEASE_CERT_SHA256':cert,'EZ_RELEASE_APPROVED_SHA':commit}):
                self.assertTrue(verify(evidence,metadata,apk))
                for change in ({'apk_sha256':'0'*64},{'source_fingerprint':'0'*64},{'certificate_sha256':'0'*64},{'completed':False},{'results':{}}):
                    with self.assertRaises(AssertionError):verify(evidence|change,metadata,apk)
                with self.assertRaises(AssertionError):verify(evidence,metadata|{'debuggable':True},apk)
                apk.write_bytes(b'changed binary')
                with self.assertRaises(AssertionError):verify(evidence,metadata,apk)
if __name__=='__main__':unittest.main()
