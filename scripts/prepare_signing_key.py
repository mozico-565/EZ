"""First-time owner-controlled signing setup. Never called by pushes or PRs."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile


def prepare(output: Path):
    password = os.environ.get('EZ_RELEASE_STORE_PASSWORD', '')
    problems = []
    if not password:
        problems.append('secret_missing')
    else:
        if len(password) < 32:
            problems.append('length_below_32')
        if not password.isascii():
            problems.append('non_ASCII_characters')
        if any(c.isspace() for c in password):
            problems.append('whitespace_or_newline')
    if problems:
        # Report only failed rules, never contents, positions or actual length.
        raise ValueError('Signing password rejected: ' + ', '.join(problems) + '. Update EZ_RELEASE_STORE_PASSWORD directly in GitHub Secrets; do not post its value.')
    if os.environ.get('EXISTING_RELEASE_KEY', ''):
        raise ValueError('A production keystore secret already exists. Refusing to generate a replacement identity.')
    # Exclusive directory creation also prevents accidental overwriting of a local backup.
    output.mkdir(mode=0o700, parents=False, exist_ok=False)
    try:
        with tempfile.TemporaryDirectory(prefix='ez-first-key-') as temporary:
            store = Path(temporary) / 'EZ-production.p12'
            # Password is passed only by env, never argv. Explicit high work factors
            # protect the encrypted PKCS12 backup even outside GitHub Secrets.
            subprocess.run([
                'keytool', '-J-Dkeystore.pkcs12.keyPbeIterationCount=600000',
                '-J-Dkeystore.pkcs12.certPbeIterationCount=600000',
                '-J-Dkeystore.pkcs12.macIterationCount=600000',
                '-genkeypair', '-noprompt', '-storetype', 'PKCS12',
                '-keystore', str(store), '-alias', 'ez-release',
                '-storepass:env', 'EZ_RELEASE_STORE_PASSWORD',
                '-keypass:env', 'EZ_RELEASE_STORE_PASSWORD',
                '-keyalg', 'RSA', '-keysize', '4096', '-sigalg', 'SHA256withRSA',
                '-validity', '10000', '-dname', 'CN=EZ Release,O=EZ'
            ], check=True, capture_output=True)
            certificate = subprocess.run([
                'keytool', '-exportcert', '-keystore', str(store), '-alias', 'ez-release',
                '-storepass:env', 'EZ_RELEASE_STORE_PASSWORD'
            ], check=True, capture_output=True).stdout
            fingerprint = hashlib.sha256(certificate).hexdigest().upper()
            encrypted = store.read_bytes()
            (output / 'EZ-production.p12').write_bytes(encrypted)
            (output / 'EZ_RELEASE_KEYSTORE_BASE64.txt').write_text(base64.b64encode(encrypted).decode('ascii'))
            (output / 'certificate.der').write_bytes(certificate)
            (output / 'PUBLIC-IDENTITY.json').write_text(json.dumps({
                'alias': 'ez-release', 'certificate_sha256': fingerprint,
                'keystore_sha256': hashlib.sha256(encrypted).hexdigest(),
                'source_commit': os.environ.get('GITHUB_SHA', 'local'),
                'password_included': False, 'encrypted_pkcs12': True
            }, indent=2) + '\n')
            for file in output.iterdir():
                file.chmod(0o600)
            print('Encrypted signing backup prepared. Public certificate SHA-256: ' + fingerprint)
            print('Keep two private offline copies and the password. This backup does not sign or publish an APK.')
    except Exception:
        # Never expose keytool output or secrets through exception logs.
        for file in output.iterdir():
            file.unlink()
        output.rmdir()
        raise RuntimeError('Signing-key preparation failed; no usable backup was produced.') from None


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    try:
        prepare(args.output)
    except (ValueError, FileExistsError, RuntimeError) as error:
        parser.exit(1, str(error) + '\n')
