"""Import the already-created encrypted owner backup, never generate a key."""
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import sys


def verify_backup(root: Path, expected_certificate: str, expected_commit: str) -> bytes:
    expected = expected_certificate.lower().replace(':', '').replace(' ', '')
    if not re.fullmatch('[0-9a-f]{64}', expected):
        raise ValueError('A pinned production certificate is required.')
    names = ['EZ-production.p12', 'EZ_RELEASE_KEYSTORE_BASE64.txt', 'certificate.der', 'PUBLIC-IDENTITY.json']
    blobs = {}
    for name in names:
        path = root / name
        if path.is_symlink() or not path.is_file() or path.stat().st_size > 65536:
            raise ValueError('Invalid or oversized backup entry.')
        blobs[name] = path.read_bytes()
    identity = json.loads(blobs['PUBLIC-IDENTITY.json'])
    if identity.get('alias') != 'ez-release' or identity.get('source_commit') != expected_commit:
        raise ValueError('Backup identity or source provenance mismatch.')
    if identity.get('password_included') is not False or identity.get('encrypted_pkcs12') is not True:
        raise ValueError('Unexpected backup protection metadata.')
    certificate = hashlib.sha256(blobs['certificate.der']).hexdigest()
    if certificate != expected or identity.get('certificate_sha256', '').lower() != expected:
        raise ValueError('Backup certificate does not match the pinned production identity.')
    encrypted = blobs['EZ-production.p12']
    if hashlib.sha256(encrypted).hexdigest() != identity.get('keystore_sha256'):
        raise ValueError('Encrypted keystore integrity mismatch.')
    if base64.b64decode(blobs['EZ_RELEASE_KEYSTORE_BASE64.txt'], validate=True) != encrypted:
        raise ValueError('Encrypted keystore representations do not match.')
    return encrypted


if __name__ == '__main__':
    try:
        run = json.loads(Path(sys.argv[2]).read_text())
        encrypted = verify_backup(Path(sys.argv[1]), os.environ.get('EZ_RELEASE_CERT_SHA256', ''), run['head_sha'])
        target = Path(sys.argv[3])
        # Refuse overwriting an unexpected pre-existing runner-local signing file.
        with target.open('xb') as file:
            os.chmod(target, 0o600)
            file.write(encrypted)
        with open(os.environ['GITHUB_ENV'], 'a') as environment:
            environment.write('EZ_RELEASE_STORE_FILE=' + str(target) + '\n')
        print('Imported encrypted owner backup; certificate and source provenance verified. No key was regenerated.')
    except Exception:
        sys.exit('Encrypted signing-backup import failed. Refusing another key or debug fallback.')
