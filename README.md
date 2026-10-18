# EZ 1.0.0

Existing native Kotlin/Jetpack Compose project continued from PR2, not rebuilt. App ID`com.ezshare.app`, Android10+, Arabic RTL, approved pastel identity and fixed floating capsule/scroll behavior. **EZ 1.0.0/versionCode11 is published as a stable signed release:** [Download APK](https://github.com/mozico-565/EZ/releases/download/v1.0.0/EZ-1.0.0.apk) · [Release and checksums](https://github.com/mozico-565/EZ/releases/tag/v1.0.0). Production signing uses the existing persistent identity. The owner waived physical two-phone acceptance on 2026-10-18; automated tests, signed-build integrity and explicit publication approval remain required. No Alpha APK is being offered as an official release.

## Implemented in this branch

- Real in-app MediaStore media browser (photos, videos, audio), visible launcher apps, authorized SAF document folders and imported documents.7categories, search, sorting, dates/duration, thumbnails, list/grid, persistent selection across tabs, batch summary. Android permissions are contextual and scoped; partial photo access is respected. No MANAGE_EXTERNAL_STORAGE or QUERY_ALL_PACKAGES.
- Complete standalone APK export validated against installed package/version/certificate, narrow FileProvider URI, temporary read grant and Android Sharesheet for Bluetooth/Quick Share/other targets. Split apps are explicitly limited, never base-only 'installable' exports. Official links are verified against an actual published release; no Play Store URL/token is invented.
- CameraX + ZXing on-device QR scanning (no GMS/model download/ML SDK dependency). Receiver generates a live local address/port, version2, short-lived nonce and expiry. Sender strictly validates scheme/fields/local IPv4/port/expiry. Both users must still compare SAS and explicitly approve.
- Protocol2 multi-file encrypted sessions: one manifest and key exchange, per-file SHA-256/storage readback/receipt, overall and per-file progress, completed-file history, duplicate-safe names, cancellation and honest failed/partial states. Max256files/session and16GiB/file; memory is bounded, not whole-file buffering.
- Connection/discovery/approval/first-chunk/streaming/storage-verification/receipt timing. Actual MB/s from monotonic byte deltas; synthetic loopback results are not phone benchmarks.
- Transparent/themed header mark, unchanged adaptive launcher safe zones, corrected monitor/phone contour, light/dark/large-font support, local identity/preferences/history and existing motion/reduced-motion behavior.

## Workflows and release gates

Ordinary CI uses contents:read, tests core and Android UI/security fixtures, checks debug/release lint and assembles a **non-debug unsigned release artifact**, explicitly named NOT-INSTALLABLE. It is validation evidence, not an installable release.

Manual dispatch of the existing Android validation workflow additionally runs a **production-signed candidate** job. It refuses missing credentials, verifies certificate/package/version/non-debug status and uploads APK/checksum/metadata for phone acceptance. No private key is committed or printed. See [signing setup](release/SIGNING-SETUP.md).

Stable publishing uses `.github/workflows/release.yml` only after owner-recorded acceptance or an explicit physical-test waiver, matching exact APK hash/source fingerprint/certificate/approved commit. It publishes that same tested artifact, without a new rebuild or tag overwrite. No v1.0.0 tag/release is created while gates are unmet.

## Transfer flow

Both phones use compatible protocol2 and the same local router/hotspot (internet is unnecessary for file transfer). Receiver taps Receive and listens immediately; accepted verified files save automatically in Download/EZ via MediaStore. Sender selects actual files, discovers/chooses receiver or scans its QR/manual IP:PORT. Compare the same six digits and approve both sides. Receiver verifies stored data before publication/receipt; sender requires the verified receipt. Camera and OEM background behavior still need actual device testing.

## Limits and honest failures

- No physical two-phone acceptance or audited-security claim yet. Six-digit SAS requires careful human comparison; names/IP/QR alone are not authentication. See SECURITY.md.
- Old Alpha protocol1 fails closed with a compatibility message; update both peers. Production signing differs from old ephemeral debug keys, so keep important app data before uninstall/reinstall. Future production updates must retain the persistent key.
- NSD can fail or be blocked by AP isolation/VPN/OEM behavior; private IPv4 manual fallback remains. IPv6, Wi-Fi Direct, auto-trust, pause/resume and recovery after process death are not implemented.
- Scoped storage cannot enumerate every document without authorization. Non-launcher apps may not be visible; split packages need a complete distributable from a trusted source. Receiving uses app-owned MediaStore Downloads, with no folder chooser or broad write permission. Capacity for an opaque provider is not fabricated.
- Earlier verified files survive a later interruption. A failed current partial is not marked successful; failed deletion leaves an unpublished IS_PENDING row and reports an error. Process death can leave an expiring pending row. Receipt loss after verified publication can cause sender failure despite a valid receiver file; inspect receiver before retry.
- Selection and active transfer are in memory; completed/failed file history (up to100) and appearance/device preferences persist locally. Clearing history never deletes files. Foreground service and notification behavior require OEM testing.
- The repository is public. The official v1.0.0 release is published; the app verifies the real non-prerelease Release before sharing. Repository visibility was not changed.

## Evidence

[Release status](release/STATUS.md), [performance](release/PERFORMANCE.md), [phone matrix](release/PHYSICAL-TESTING.md), [signing setup](release/SIGNING-SETUP.md), original design tokens/motion in`design/REFERENCE.md`. Historical0.1.1 validation is retained as history, not proof of this release.

## Latest fixes

See [automatic saving](release/AUTOMATIC-SAVING.md) for MediaStore publication and history open/share. Standalone installed APK export compares exact installed-source SHA-256 bytes plus package/version and available signing metadata; absent OEM archive metadata is not mislabeled as a mismatch. Ordinary APK file transfer never compares package signers. Split apps retain an honest complete-package limitation.

## Verified release evidence

Signed candidate [39158022188](https://github.com/mozico-565/EZ/actions/runs/39158022188), promoted-main validation [39159309663](https://github.com/mozico-565/EZ/actions/runs/39159309663), and exact published-asset verification [39160735324](https://github.com/mozico-565/EZ/actions/runs/39160735324) all passed. The signed candidate ran 26 core tests, 29 Android/Compose tests, 6 signing/gate checks and 8 native tests on each of API29 and API35 (77 executions). Debug and release lint each reported zero errors and 48 warnings. Physical two-phone testing was waived by the owner and remains not_tested.
