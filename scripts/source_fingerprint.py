#!/usr/bin/env python3
"""Stable source fingerprint excluding acceptance records and generated outputs."""
from pathlib import Path
import hashlib

def fingerprint(root=Path('.')):
    selected=[]
    for folder in ('app/src/main','core/src/main'):
        selected += [p for p in (root/folder).rglob('*') if p.is_file()]
    selected += [root/p for p in ('app/build.gradle.kts','core/build.gradle.kts','build.gradle.kts','settings.gradle.kts','gradle.properties','gradle/libs.versions.toml') if (root/p).is_file()]
    digest=hashlib.sha256()
    for p in sorted(selected):
        digest.update(str(p.relative_to(root)).encode()+b'\0'+hashlib.sha256(p.read_bytes()).digest())
    return digest.hexdigest()
if __name__=='__main__': print(fingerprint())
