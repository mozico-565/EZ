"""Uses an ephemeral test identity, never a production key or password."""
import base64
import hashlib
import json
import os
from pathlib import Path
import secrets
import subprocess
import tempfile
import unittest
from unittest.mock import patch
from prepare_signing_key import prepare
from load_signing_backup import verify_backup


class SigningSetupTest(unittest.TestCase):
    def test_reject_missing_password_before_creating_files(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ, {'EZ_RELEASE_STORE_PASSWORD': '', 'EXISTING_RELEASE_KEY': ''}):
            path = Path(directory) / 'backup'
            with self.assertRaises(ValueError):
                prepare(path)
            self.assertFalse(path.exists())

    def test_safe_diagnostics_distinguish_password_rules(self):
        samples = [('', 'secret_missing'), ('a' * 31, 'length_below_32'),
                   ('a' * 32 + '\n', 'whitespace_or_newline'),
                   ('a' * 32 + 'ع', 'non_ASCII_characters')]
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'backup'
            for password, expected in samples:
                with self.subTest(rule=expected), patch.dict(os.environ, {'EZ_RELEASE_STORE_PASSWORD': password, 'EXISTING_RELEASE_KEY': ''}):
                    with self.assertRaisesRegex(ValueError, expected) as raised:
                        prepare(path)
                    if password:
                        self.assertNotIn(password, str(raised.exception))
                    self.assertFalse(path.exists())

    def test_refuse_replacement_of_existing_production_key(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ, {'EZ_RELEASE_STORE_PASSWORD': secrets.token_urlsafe(48), 'EXISTING_RELEASE_KEY': 'already-present'}):
            with self.assertRaises(ValueError):
                prepare(Path(directory) / 'backup')

    def test_encrypted_backup_round_trip_and_overwrite_guard(self):
        password = secrets.token_urlsafe(48)
        with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ, {'EZ_RELEASE_STORE_PASSWORD': password, 'EXISTING_RELEASE_KEY': ''}):
            path = Path(directory) / 'backup'
            prepare(path)
            identity = json.loads((path / 'PUBLIC-IDENTITY.json').read_text())
            self.assertEqual(base64.b64decode((path / 'EZ_RELEASE_KEYSTORE_BASE64.txt').read_text(), validate=True), (path / 'EZ-production.p12').read_bytes())
            self.assertEqual(identity['certificate_sha256'], hashlib.sha256((path / 'certificate.der').read_bytes()).hexdigest().upper())
            self.assertEqual(verify_backup(path, identity['certificate_sha256'], identity['source_commit']), (path / 'EZ-production.p12').read_bytes())
            with self.assertRaises(ValueError):
                verify_backup(path, '0' * 64, identity['source_commit'])
            with self.assertRaises(ValueError):
                verify_backup(path, identity['certificate_sha256'], 'another-source')
            original = (path / 'EZ-production.p12').read_bytes()
            (path / 'EZ-production.p12').write_bytes(original + b'tamper')
            with self.assertRaises(ValueError):
                verify_backup(path, identity['certificate_sha256'], identity['source_commit'])
            (path / 'EZ-production.p12').write_bytes(original)
            encoded = (path / 'EZ_RELEASE_KEYSTORE_BASE64.txt').read_bytes()
            (path / 'EZ_RELEASE_KEYSTORE_BASE64.txt').write_text(base64.b64encode(b'wrong-backup').decode())
            with self.assertRaises(ValueError):
                verify_backup(path, identity['certificate_sha256'], identity['source_commit'])
            (path / 'EZ_RELEASE_KEYSTORE_BASE64.txt').write_bytes(encoded)
            with self.assertRaises(FileExistsError):
                prepare(path)
            # Verify the private entry is encrypted and cannot be opened with another password.
            result = subprocess.run(['keytool', '-list', '-keystore', str(path / 'EZ-production.p12'), '-storepass:env', 'WRONG_PASSWORD'], env={**os.environ, 'WRONG_PASSWORD': secrets.token_urlsafe(48)}, capture_output=True)
            self.assertNotEqual(result.returncode, 0)
            inspected = subprocess.run(['openssl', 'pkcs12', '-info', '-noout', '-in', str(path / 'EZ-production.p12'), '-passin', 'env:EZ_RELEASE_STORE_PASSWORD'], capture_output=True, check=True)
            protection = inspected.stderr.decode()
            self.assertIn('AES-256-CBC, Iteration 600000', protection)
            self.assertIn('MAC: sha256, Iteration 600000', protection)
            for file in path.iterdir():
                self.assertNotIn(password.encode(), file.read_bytes())


if __name__ == '__main__':
    unittest.main()
