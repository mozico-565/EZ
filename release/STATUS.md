# EZ 1.0.0 — APK regression and automatic saving update

Continued from PR2 and signed candidate 2ba7ee7991c1662296918219c7b42873534378a4, not rebuilt. VersionCode11 preserves the production certificate E8B79642B969337B9550DFB691994E1D90F52B438D3C8A82C4FCBB8048B3A908. Release publication is pending green updated validation and exact binary approval.

New changes: byte-verified standalone installed APK export without false missing-metadata rejection; automatic MediaStore Download/EZ receive, pending publication, duplicate-safe naming and history open/share. Existing SAS/AES-GCM/SHA-256 transport, visual identity, capsule navigation and scrolling remain unchanged. Split app export remains honestly limited; no base-only installability claim.

The owner requested: «انشى الريليس بعد اكتماله وبدون اختبار النقل بين جهازين». PHYSICAL-ACCEPTANCE.json records this explicit waiver; completed remains false and physical results remain not_tested. Automated emulator tests do not replace physical transfer/camera/OEM verification. Publisher still verifies exact APK hash, source fingerprint, pinned permanent certificate, successful candidate provenance and explicitly approved source commit.

Current CI adds real MediaProvider/PackageManager/Sharesheet tests on Android10/API29 and Android15/API35. Outcomes must be read from the successful final workflow; test definitions alone are not passing evidence. Prior candidate evidence and signing identity are historical records, never reused as verification for changed bytes.

Wi-Fi Direct, pause/resume and process-death transfer recovery remain unavailable. Maintain private encrypted key backups and the original owner password; do not generate a replacement identity. No private key/password belongs in source or public release assets.
