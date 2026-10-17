# EZ Alpha 0.1

Native Kotlin / Jetpack Compose Android application. Arabic RTL, Android 10+ (API29), compile/target35. **Alpha: awaiting two-physical-phone testing and independent security review.** No WebView, cloud transfer, account, or paid backend.

## Implemented
Single real SAF-selected file → local TCP connection (NSD discovery + manual private IPv4:port fallback) → ephemeral P-256 ECDH and AES-256-GCM records → six-digit SAS comparison and explicit approval on both phones → 64KiB bounded streaming → receiver writes `.ezpart-*` → verifies stream SHA-256 and length → closes and re-reads saved bytes for SHA-256 verification → renames document → sends verified receipt → sender reports success.

Progress and MB/s use real stream bytes and monotonic nanoseconds; speeds are not benchmarks. Known/unknown lengths, duplicate names, bounded metadata, 16GiB safety cap, timeouts, cancellation, errors and partial cleanup are implemented. Notification foreground service covers active transfers/receive mode. Long file streams are never loaded into RAM.

## Quick test on two phones
1. Download `EZ-Alpha-0.1-debug` artifact from a successful Actions run, unzip, install `app-debug.apk` on both devices. Allow that download source to install this APK when Android asks. Do not disable Play Protect globally.
2. Join the same Wi-Fi (internet unnecessary), or connect one phone to the other's hotspot. Some routers/hotspots isolate clients; try another private Wi-Fi if connection fails.
3. Receiver: **استقبال** → choose a local subfolder such as Documents/EZ with Android's directory picker → **تفعيل الاستقبال**. Keep receive mode running. The app shows its private IP and dynamic TCP port.
4. Sender: **إرسال** → choose real file → **البحث عن مستقبلين** and select device, or enter receiver `192.168.x.x:port` → connect.
5. Compare the identical six digits on both physical screens. Accept on both only when they match. Reject if they differ or the peer is unexpected.
6. Observe actual progress. Receiver success requires written length + SHA-256 and document rename; sender success requires receiver's matching receipt. Check received file opens and its contents match.
7. Stop receive mode when finished. Cancel notification or in-app controls close sockets. No pause/resume in Alpha.

## Builds and tests
JDK17, Android SDK35/build-tools35.0.0. Stable pinned dependency versions: Gradle8.11.1, AGP8.9.2, Kotlin/Compose compiler2.1.20, Compose BOM2025.04.01.

```sh
./gradlew :core:test :app:lintDebug :app:assembleDebug
```
Debug output: `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions runs tests/lint/build on push, PR and manual dispatch; artifacts appear at the bottom of each successful run for signed-in repo members, expire after14days. Debug signing is automatic and unsuitable for production release.

## Layout
- `core/`: pure Kotlin JVM crypto records, metadata, transport, integrity and speed.
- `app/`: Compose, ViewModel, app-owned transfer controller, NSD, SAF sink, foreground service.
- `core/src/test/`: real TCP loopback integration and unit/error tests.
- `.github/workflows/android.yml`: build, reports and debug artifact.

`EZ-Alpha-Source.zip` in the repository is a historical bootstrap checkpoint, not the current source. CI expanded the tested source into the normal directories. Current canonical source is `app/`, `core/` and the Gradle files. The one-time write-permission materialization job has been removed; ordinary CI now has read-only repository permissions. Fresh branch builds cancel superseded runs and cache Gradle dependencies.

## Limitations / do not overclaim
- No two-phone validation yet; loopback tests are not a device-network benchmark.
- New custom pairing protocol, not independently audited or release-ready. SAS requires careful human comparison. Metadata (filename/length) is encrypted but shared with the selected peer before final approval; file contents wait for both approvals.
- NSD resolution can fail/busy on some Android versions; manual private IPv4 fallback is required and supported. IPv6 and public IP connections not implemented.
- SAF providers must support creation, write, delete and rename. Use local Documents/EZ, not a cloud provider; offline save capability depends on provider. Available capacity is not claimed for opaque SAF providers; I/O errors fail the transfer. Partial cleanup is best effort; manually delete `.ezpart-*` after an unclean process death or provider deletion failure.
- Final receipt loss can cause sender failure although receiver has a verified saved file. Inspect receiver before retrying; duplicate names are not overwritten.
- Cancellation/timeouts close sockets, but a hostile/broken document provider can block its own I/O. Android may kill the process; no resume/recovery. Background/foreground behavior needs testing across OEMs.
- Active receive mode needs foreground-service lifetime and can reach system time limits; stop when not needed. Notification denial does not imply broad file permissions.
- Active transfer state does not persist across process death. Completed/failed session history and appearance preferences persist locally. Destination access can be persisted by SAF, but this Alpha chooses it in the current session.
- Wi-Fi Direct, QR, multi-file, pause/resume and app sharing remain roadmap only. History, avatars and appearance preferences are implemented in the design correction. The earlier HTML design prototype is not the transfer engine.

## Troubleshooting
No peers: manual IP/port and recheck same subnet, AP isolation, VPN, firewall. Timeout: compare/approve before120s and ensure receiver foreground. Unknown file size: percentage/ETA can be unknown until final count. No save: choose local subfolder, verify capacity, try another provider. Hash mismatch/incomplete connection: no successful result; retry as new transfer. No broad storage permission or MANAGE_EXTERNAL_STORAGE is used.

See `TESTING.md` for required measurements and `SECURITY.md` for protocol details.

## Native design correction 0.1.1
VersionCode2 keeps package com.ezshare.app and the existing transfer engine. Source-faithful home, files/send, receive, discovery, confirmation, progress, history/detail, settings, optional dark theme and shared components replace the Alpha UI shortcuts. Adaptive launcher and monochrome icons preserve the approved mark. See design/REFERENCE.md for the screen comparison, tokens, motion, honest Alpha deviations and visual verification limits.

Search/category filters apply to the single SAF-selected file. History stores up to100 actual session records locally and clearing it never deletes documents. Name/avatar/theme/reduced-motion preferences persist. QR, pause/resume and app sharing remain clearly labelled coming soon.

UI screenshot tests use native Robolectric/Roborazzi; CI artifacts include EZ-native-design-captures. They are generated Android renders, not physical-device screenshots or approved HTML pixel goldens. Run `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

Upgrade note: package and versionCode preserve the Android upgrade path, but the initial CI debug keystore was ephemeral. If Android reports signature mismatch, Android cannot upgrade it in-place without the prior signing key. This build does not silently uninstall data or change package ID. Export received files first and make an explicit user decision before uninstalling. CI now uses an explicit development debug key path and attempts cache reuse. Cache scope/eviction can still change that identity; production and reliable cross-branch updates require a stable user-managed signing key. No private signing key is committed.
