# EZ 1.0.0 release work

Existing native Kotlin/Jetpack Compose project continued from PR2, not rebuilt. App ID`com.ezshare.app`, Android10+, Arabic RTL, approved pastel identity and fixed floating capsule/scroll behavior. Source version1.0.0/versionCode10 is **not a published stable release**. Production signing credentials and the physical two-phone acceptance matrix are still required. No Alpha APK is being offered as an official release.

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

Stable publishing uses `.github/workflows/release.yml` only after owner-recorded [physical acceptance](release/PHYSICAL-TESTING.md), matching exact APK hash/source fingerprint/certificate/approved commit. It publishes that same tested artifact, without a new rebuild or tag overwrite. No v1.0.0 tag/release is created while gates are unmet.

## Transfer flow

Both phones use compatible protocol2 and the same local router/hotspot (internet is unnecessary for file transfer). Receiver chooses a local subfolder such as Documents/EZ and starts Receive. Sender selects actual files, discovers/chooses receiver or scans its QR/manual IP:PORT. Compare the same six digits and approve both sides. Receiver verifies stored data before publication/receipt; sender requires the verified receipt. Camera and OEM background behavior still need actual device testing.

## Limits and honest failures

- No physical two-phone acceptance or audited-security claim yet. Six-digit SAS requires careful human comparison; names/IP/QR alone are not authentication. See SECURITY.md.
- Old Alpha protocol1 fails closed with a compatibility message; update both peers. Production signing differs from old ephemeral debug keys, so keep important app data before uninstall/reinstall. Future production updates must retain the persistent key.
- NSD can fail or be blocked by AP isolation/VPN/OEM behavior; private IPv4 manual fallback remains. IPv6, Wi-Fi Direct, auto-trust, pause/resume and recovery after process death are not implemented.
- Scoped storage cannot enumerate every document without authorization. Non-launcher apps may not be visible; split packages need a complete distributable from a trusted source. SAF provider must support local create/read/write/delete/rename. Capacity for an opaque provider is not fabricated.
- Earlier verified files survive a later interruption. A failed current partial is not marked successful; failed deletion warns about possible `.ezpart`. Process death can leave partial documents. Receipt loss after verified publication can cause sender failure despite a valid receiver file; inspect receiver before retry.
- Selection and active transfer are in memory; completed/failed file history (up to100) and appearance/device preferences persist locally. Clearing history never deletes files. Foreground service and notification behavior require OEM testing.
- The repo currently displays Public in GitHub. A published official download link still does not exist merely because source is public; the app verifies a real non-prerelease Release before sharing. This task did not change repository visibility.

## Evidence

[Release status](release/STATUS.md), [performance](release/PERFORMANCE.md), [phone matrix](release/PHYSICAL-TESTING.md), [signing setup](release/SIGNING-SETUP.md), original design tokens/motion in`design/REFERENCE.md`. Historical0.1.1 validation is retained as history, not proof of this release.
