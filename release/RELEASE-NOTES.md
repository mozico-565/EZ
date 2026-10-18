# EZ 1.0.0

Native Android local file sharing, Arabic RTL, approved EZ visual identity.

- Device media/browser using scoped MediaStore and authorized document folders; search, filtering, ordering, thumbnails and multi-selection.
- Protocol2 encrypted multi-file sessions with explicit SAS approval on both phones, SHA-256 verification and receipt for each file, safe partial-file cancellation.
- Offline versioned/expiring QR pairing with CameraX/on-device ZXing scanner; QR does not replace SAS comparison.
- Android Sharesheet for complete verified standalone APKs; split base packages are not falsely exported as installable.
- Themed header branding and corrected Receive device illustration.
- Measured streaming speed and timing breakdown; no fixed throughput promise.

Android10+, app IDcom.ezshare.app. Both peers must use compatible protocol2. Prior Alpha/debug builds may need uninstall/reinstall because production signing differs; preserve important app data first. Completed files survive a later file interruption; interrupted files are not marked successful. Pause/resume and Wi-Fi Direct are not available.

Publish only after the exact signed APK passes the documented two-phone matrix. Repository privacy still controls download access. The app shares an official link only when it verifies a published non-prerelease URL available without authentication; otherwise APK sharing remains available.
