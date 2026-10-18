# EZ 1.0.0 — APK regression and automatic saving update

Continued from PR2 and signed candidate 2ba7ee7991c1662296918219c7b42873534378a4, not rebuilt. VersionCode11 preserves the production certificate E8B79642B969337B9550DFB691994E1D90F52B438D3C8A82C4FCBB8048B3A908. Stable release v1.0.0 is published: https://github.com/mozico-565/EZ/releases/tag/v1.0.0 . Approved signed source: 8b183d07b9c660eedbb767399655f9397493207c. APK SHA-256: 54ed700f3835bbacfdd4ee0082e3216fc0c9345708f19b89639c4c9d9c5babbb.

New changes: byte-verified standalone installed APK export without false missing-metadata rejection; automatic MediaStore Download/EZ receive, pending publication, duplicate-safe naming and history open/share. Existing SAS/AES-GCM/SHA-256 transport, visual identity, capsule navigation and scrolling remain unchanged. Split app export remains honestly limited; no base-only installability claim.

The owner requested: «انشى الريليس بعد اكتماله وبدون اختبار النقل بين جهازين». PHYSICAL-ACCEPTANCE.json records this explicit waiver; completed remains false and physical results remain not_tested. Automated emulator tests do not replace physical transfer/camera/OEM verification. Publisher still verifies exact APK hash, source fingerprint, pinned permanent certificate, successful candidate provenance and explicitly approved source commit.

Real MediaProvider/PackageManager/Sharesheet tests passed: 8 on Android10/API29 and 8 on Android15/API35. Signed candidate run 39158022188 and promoted-main run 39159309663 both succeeded. Final publisher run 39160735324 succeeded and downloaded/compared all five published assets byte-for-byte against the approved candidate and release evidence. The existing tag was verified, not overwritten. Unit/UI/signing/gate and native checks total 77 executions; lint has zero errors, 48 warnings per variant.

Wi-Fi Direct, pause/resume and process-death transfer recovery remain unavailable. Maintain private encrypted key backups and the original owner password; do not generate a replacement identity. No private key/password belongs in source or public release assets.
