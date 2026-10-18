# EZ 1.0.0

Native Android local file sharing, Arabic RTL, approved EZ visual identity.

- Device media/browser using scoped MediaStore and authorized document folders; search, filtering, ordering, thumbnails and multi-selection.
- Protocol2 encrypted multi-file sessions with explicit SAS approval on both phones, SHA-256 verification and receipt for each file, safe partial-file cancellation.
- Offline versioned/expiring QR pairing with CameraX/on-device ZXing scanner; QR does not replace SAS comparison.
- Android Sharesheet for byte-verified standalone APKs, including third-party apps and EZ itself. Missing OEM archive SigningInfo no longer causes a false mismatch; real comparable certificate mismatches are rejected. Split packages are explicitly limited, never exported as an installable base alone.
- Receive starts immediately, then saves approved transfers automatically in Download/EZ with Android MediaStore, duplicate-safe names, IS_PENDING and SHA-256 readback before publication. Failed/cancelled pending entries are removed; cleanup failure never marks a partial successful. No receive folder picker or broad write permission. Open/share completed received files from history.
- Themed header branding and corrected Receive device illustration.
- Measured streaming speed and timing breakdown; no fixed throughput promise.

Android10+, app IDcom.ezshare.app. Both peers must use compatible protocol2. Prior Alpha/debug builds may need uninstall/reinstall because production signing differs; preserve important app data first. Completed files survive a later file interruption; interrupted files are not marked successful. Pause/resume and Wi-Fi Direct are not available.

The owner explicitly requested publication without two-phone transfer acceptance on 2026-10-18. Those physical tests remain not performed; the waiver is recorded separately from automated CI/emulator results. No physical throughput, camera interoperability or OEM background-transfer assurance is claimed. Repository privacy still controls download access. The app shares an official link only when it verifies a published non-prerelease URL available without authentication; otherwise APK sharing remains available.

VersionName 1.0.0, versionCode 11. This build updates the earlier production-signed candidate with the same persistent certificate.
