# EZ 1.0.0 validation evidence

Signed candidate: GitHub Actions run39158022188, source8b183d07b9c660eedbb767399655f9397493207c, versionName1.0.0/versionCode11, minSDK29/target35, non-debuggable.

- 26 core and 29 Android/Compose tests: zero failures, zero ignored.
- 4 signing-setup and 2 release-gate tests: passed in the signed job.
- Real Android emulator API29: 8 instrumentation tests, zero failures/errors/skips.
- Real Android emulator API35: 8 instrumentation tests, zero failures/errors/skips.
- Debug/release lint: zero errors, 48 warnings each; warnings are not misreported as eliminated.
- assembleRelease and apksigner passed; independently inspected downloaded manifest confirms package, version and non-debug status. Downloaded APK hash matches CI metadata.
- Production SHA-256: 54ed700f3835bbacfdd4ee0082e3216fc0c9345708f19b89639c4c9d9c5babbb.
- Permanent certificate SHA-256: e8b79642b969337b9550dfb691994e1d90f52b438d3c8a82c4fcbb8048b3a908.

Native tests cover MediaStore Downloads for images/videos/PDF/APK/binary/empty files; IS_PENDING and verified stored bytes; safe duplicates; cancellation cleanup; storage-corruption rejection; history open/share; exact-byte installed own/third-party APK export; real installed split-package limitation; authenticated third-party signed and ordinary unsigned .apk payloads; actual Android Sharesheet foreground.

The test harness uses bounded streaming hashes for large APK comparisons. Native UI screenshots are test-only emulator evidence, not a physical transfer or installer acceptance claim. Bluetooth/Quick Share targets are only claimed where installed, never invented for emulator images.

Physical two-phone transfer/camera/OEM/background/throughput acceptance was expressly waived by the repository owner on2026-10-18. PHYSICAL-ACCEPTANCE.json remains completed=false, with all physical results not_tested and the exact owner instruction. Automated loopback/emulator tests are not substituted as physical pass results. Wi-Fi Direct, pause/resume and process-death recovery remain unavailable.
