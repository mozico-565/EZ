# EZ Alpha 0.1

Native Kotlin / Jetpack Compose Android application. Arabic RTL, Android 10+ (API29), compile/target35. **Alpha: awaiting two-physical-phone testing and independent security review.** No WebView, cloud transfer, account, or paid backend.

## Implemented
Single real SAF-selected file → local TCP connection (NSD discovery + manual private IPv4:port fallback) → ephemeral P-256 ECDH and AES-256-GCM records → six-digit SAS comparison and explicit approval on both phones → 64KiB bounded streaming → receiver writes `.ezpart-*` → verifies SHA-256 and length → closes and renames document → sends verified receipt → sender reports success.

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

Initial browser bootstrap may place source in `EZ-Alpha-Source.zip`; CI validates/builds it and a narrowly scoped job expands the tested source into normal files on the `alpha/*` branch after success. No force push or settings changes. Subsequent builds use normal source files; the archive is only an initial checkpoint. Default workflow permissions are read-only; only the one-time source materialization job writes to this development branch.

## Limitations / do not overclaim
- No two-phone validation yet; loopback tests are not a device-network benchmark.
- New custom pairing protocol, not independently audited or release-ready. SAS requires careful human comparison. Metadata (filename/length) is encrypted but shared with the selected peer before final approval; file contents wait for both approvals.
- NSD resolution can fail/busy on some Android versions; manual private IPv4 fallback is required and supported. IPv6 and public IP connections not implemented.
- SAF providers must support creation, write, delete and rename. Use local Documents/EZ, not a cloud provider; offline save capability depends on provider. Available capacity is not claimed for opaque SAF providers; I/O errors fail the transfer. Partial cleanup is best effort; manually delete `.ezpart-*` after an unclean process death or provider deletion failure.
- Final receipt loss can cause sender failure although receiver has a verified saved file. Inspect receiver before retrying; duplicate names are not overwritten.
- Cancellation/timeouts close sockets, but a hostile/broken document provider can block its own I/O. Android may kill the process; no resume/recovery. Background/foreground behavior needs testing across OEMs.
- Active receive mode needs foreground-service lifetime and can reach system time limits; stop when not needed. Notification denial does not imply broad file permissions.
- App state/history does not persist across process death. Destination access can be persisted by SAF, but this Alpha chooses it in the current session.
- Wi-Fi Direct, QR, multi-file, pause/resume, detailed history, avatars/dark mode, app sharing are roadmap only. The earlier HTML design prototype is not the transfer engine.

## Troubleshooting
No peers: manual IP/port and recheck same subnet, AP isolation, VPN, firewall. Timeout: compare/approve before120s and ensure receiver foreground. Unknown file size: percentage/ETA can be unknown until final count. No save: choose local subfolder, verify capacity, try another provider. Hash mismatch/incomplete connection: no successful result; retry as new transfer. No broad storage permission or MANAGE_EXTERNAL_STORAGE is used.

See `TESTING.md` for required measurements and `SECURITY.md` for protocol details.
