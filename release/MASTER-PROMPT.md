# EZ 1.0.0 — Unified Codex Execution Prompt
## Official Android Release | Continue Existing GitHub Work, Fix CI, Complete Features

> **This is the ONE consolidated instruction file.** It supersedes earlier *task instructions* for Alpha 0.1 / 0.1.1 / 0.1.2, but does **not** supersede the approved visual design in `EZ-Prototype.html` or erase any working implementation. Do not create another Alpha milestone. Do not start from scratch.

## 0. Authoritative project context

- Repository: https://github.com/mozico-565/EZ
- Existing PR #1: https://github.com/mozico-565/EZ/pull/1 (Alpha transfer foundation)
- Existing PR #2: https://github.com/mozico-565/EZ/pull/2 (native visual parity, launcher)
- PR #2 head branch at last inspection: `fix/native-design-parity`; base `alpha/0.1`.
- Last inspected PR #2 head: `bf421aca7b55bc2f68be1494a85d29d1692c03a2`. **Check latest remote state before acting; these are historical identifiers, not a command to reset.**
- Last inspected failing CI run: https://github.com/mozico-565/EZ/actions/runs/39074712529 (run #21). `:app:testDebugUnitTest` failed with `DesignCaptureTest.home` at `DesignCaptureTest.kt:43`, while 18 other app tests passed. The test asserts the Arabic hero title renders on two lines and does not overflow horizontally. Fix the actual layout, not the test's safety assertions.
- Application ID: `com.ezshare.app` (preserve it); Android 10+; Arabic-first RTL; native Kotlin + Jetpack Compose.
- Existing local Wi-Fi/TCP transfer, NSD/manual address, explicit pairing/SAS confirmation, AES-GCM streaming, SAF, SHA-256 readback verification, cancellation, real progress and history must be preserved.
- Current UI has a fixed black capsule bottom navigation and **scrollable content**. **There is no confirmed overlap bug between the navigation and IP:PORT field. Do not alter the navigation or scrolling based on that mistaken observation.**

## 1. Mission and execution rules

Act as senior Android/Compose engineer, local-network performance engineer, security reviewer, UI QA, and release engineer. Inspect current code, open PRs, commits, workflows and documentation; then implement, test, and deliver **EZ 1.0.0 official signed Release**, not a renamed debug APK.

- Preserve every existing working feature. No WebView, no fake files/peers/benchmarks, no backend/cloud dependency for local transfers.
- Do not merge or overwrite active branches blindly; reconcile PR #1/#2 and work from latest valid source. Use incremental commits and a clear PR strategy; never force-push.
- Do not disable tests, verification, encryption, authentication, or user approval merely to pass CI or inflate speed.
- No broad storage permission unless genuinely necessary and policy-appropriate. Prefer scoped MediaStore, Android photo picker/SAF, and narrow permissions.
- Do not claim physical-device validation, release signing, or publishing unless confirmed with evidence.
- Prioritize completing the task rather than writing a plan. Ask for truly missing inputs, secrets, or phone tests only when required.

## 2. Design is fixed by the approved reference

The authoritative UI/interaction reference is **`EZ-Prototype.html`** (approved Arabic RTL interactive prototype). Inspect it and compare against real Compose screenshots, not against a guessed description. Keep the approved blue E / black Z brand, white/pastel design, orange Send card, blue Receive card, pink privacy card, Arabic type, floating black capsule bottom navigation, and screen transitions. Support dark theme without changing the intended visual hierarchy.

Existing `EZ-Codex-Design-Fix.md` and `EZ-Codex-Master-Prompt.md` are historical background if attached; this consolidated file is the current execution brief. Their earlier instructions to build an Alpha debug APK are replaced by the **Release** gate below.

### 2A. Fix current failing home screenshot test

- Reproduce `DesignCaptureTest.home` failure from run #21; inspect `app/src/test/java/com/ezshare/app/DesignCaptureTest.kt` and the hero layout in `EzComponents.kt`.
- Ensure Arabic hero `ملفاتك توصل.\nبلمح البصر.` fits two lines without width overflow on the tested viewport, including 360dp, 412dp, dark theme and larger font scale.
- Adjust actual Compose constraints, responsive font sizing/spacing, artwork overlap, or layout as appropriate. Do not simply weaken/remove the test or force clipping.
- Run all previously passing tests and capture new screenshots.

### 2B. Dark-mode EZ logo

- User screenshot shows the blue/black EZ mark trapped inside a conspicuous **white rounded rectangle** on the dark background.
- Use a correctly proportioned transparent/themed asset or appropriate background treatment so the header logo integrates into dark UI. Maintain the approved mark and contrast. Distinguish **header logo** from **adaptive launcher icon**; do not damage the latter.
- Verify day/night screenshots and icon masks/safe zones.

### 2C. Receive illustration

- In the Receive screen, the smartphone outline overlaps the desktop monitor incorrectly, with stray/intersecting strokes. Correct the vector geometry and relative layering of monitor and phone; check light/dark and densities.
- Use the actual screenshot as a visual reference. Avoid replacing it with unrelated art.

### 2D. Keep navigation and scrolling unchanged unless a reproducible defect is found

- The user explicitly clarified that the bottom nav does **not** cover the manual IP:PORT field. Content scrolls behind/above the fixed bar by design. **Do not create a navigation 'fix' for this non-bug.**

## 3. Fully functional Share EZ App bottom sheet

Current source explicitly labels both options as coming soon (`EzApp.kt` share sheet). Implement them:

**Share official link:** Only when a real, verified GitHub Release/download URL exists. Use Android `ACTION_SEND` / `Intent.createChooser` with `text/plain`. Do not invent a Play Store link. Before publishing, show an accurate unavailable state; after publishing, link the actual release.

**Share APK offline:** User taps the option and gets the **native Android Sharesheet** with Bluetooth, Quick Share, and other installed compatible targets. Use a valid, distributable, signed standalone APK made available to the app, a controlled trusted cached asset, or an explicitly selected distributable APK. Expose via `FileProvider` `content://` and temporary read grants. Verify signature/package/version and availability. Android-installed apps may use split APKs; never share a lone base split and claim it is installable. If the app cannot provide a complete installable package, offer an honest actionable alternative rather than a fake share. Do not build a custom Bluetooth stack for APK sharing.

Test chooser launch, receiving access to URI, valid APK installability, and no unintended exposure of other private files.

## 4. Native in-app browser for REAL files, media, and installed apps

The current files screen largely displays only the single previously selected SAF file; category chips are not a real content browser. Replace with a polished **in-app** Compose browser, not a workflow that always jumps to ZArchiver or Android's external file picker.

- Categories: All, Photos, Videos, Apps, Documents, Audio, Other; actual local data, not placeholders.
- Grid/list layouts as appropriate; thumbnails, app icons, file name, size, modified date, video duration, loading/empty/error states.
- Internal search, filters, sorting, browsing, preview where feasible, clear selection checkmarks and bottom action summary.
- Support multi-selection **end-to-end**, including sending multiple files in one session, correct per-file metadata/progress and overall progress, receipt, cancellation, duplicate-name handling, safe partial-file cleanup and SHA-256 verification.
- MediaStore for media indexed on device with version-specific permissions, SAF for documents/unindexed files and user-authorized folders. Do not claim Android allows unbounded file access under scoped storage.
- Installed applications: show real app label/icon and only legitimately visible apps under Android package visibility rules; determine whether distributable APK is single or split, and avoid falsely promising universal export/installability. If supporting split-package bundles, implement a safe, explicit compatible package format and receiving/install instructions; otherwise show a clear limitation.
- Handle Android 10 through current Android versions, permission denial, empty galleries, long RTL filenames, large files and accessibility.
- Preserve security guarantees of existing single-file engine while extending protocol; version any incompatible protocol change and give useful compatibility errors.

## 5. Real QR pairing, BOTH sides

Current sender QR tile navigates to a 'coming soon' scanner; receiver also displays 'QR soon'. Implement a complete functioning offline workflow:

**Receiver:** Generate and display a scannable QR for the current live receive session. Encode a versioned payload with local address/port and only necessary pairing information, optionally a short-lived random session nonce. Never embed long-lived secrets or assume QR alone authenticates a peer. Refresh when address/port/session changes; handle offline/local-hotspot cases and expiration.

**Sender:** CameraX + ML Kit or another maintained suitable on-device scanner. Request camera permission contextually; decode and strictly validate version, size, address/port, nonce/expiry, and scheme. Avoid arbitrary external URL launching or remote endpoints from QR. Connect to receiver and continue existing SAS identity comparison and **explicit approval on both devices**. Handle invalid QR, denial, timeout, wrong network, and retry.

Test scanner with real camera on physical phone, QR generation with independent decoder, security failures and compatibility. No 'working' label if camera flow is untested.

## 6. Transfer speed investigation and optimization

User observed ~10 seconds for a ~4 MB video in EZ versus ~2 seconds in SHAREit. This is an uncontrolled end-to-end observation, **not** proof that EZ TCP throughput is 0.4 MB/s. Do not promise a fixed 2-second outcome.

- Instrument durations for discovery, connection, TLS/cryptographic handshake if present, SAS approval/waiting, first byte, wire transfer, SAF write, readback SHA-256 verification, receipt and total time. Report actual MB/s **during streaming** separately from total time.
- Audit `TransferController.kt`, core `TransferEngine`/`Connection`, stream chunk sizes, framing, per-chunk AES-GCM overhead, allocation, `flush()` frequency, socket send/receive buffers, `TCP_NODELAY` relevance, coroutine scheduling, file resolver I/O and double-read verification.
- Optimize based on measured bottlenecks, using controlled tests for 4 MB, 100 MB, 1 GB and small-file batches. Compare cold and warm session runs, multiple Android devices, 2.4/5 GHz conditions, signal strength, and repeat trials.
- Preserve AES-GCM, SAS approval, SHA-256 verification, bounded memory, cancellation, partial-file safety, and real progress; never optimize by silently dropping safety checks.
- Evaluate faster discovery/reconnection, trusted device/session reuse with appropriate identity verification, efficient batch manifests, bounded concurrency, Wi-Fi Direct, local-only hotspot and optionally Nearby Connections as separate experimental options. Do not assert proprietary SHAREit internals or claim Wi-Fi Direct guarantees better throughput.
- Document before/after benchmark methodology and measured results; if no physical test devices available, clearly mark device benchmarks **not performed** and leave reproducible steps.

## 7. Official EZ 1.0.0 Release, not Alpha

- Set `versionName=1.0.0`, increment `versionCode` appropriately; keep `com.ezshare.app`.
- Remove obsolete Alpha labels from release UI/docs without falsely implying unimplemented features exist.
- Implement all requested features above before declaring stable; no fake QR, file browser, or APK-sharing controls.
- Run core tests, app unit tests, Compose/Robolectric captures, lint, assembleRelease, protocol/security regression tests, and a real two-phone acceptance matrix: discovery, QR, manual connection, approve/reject, small/large/multiple files, interruption, integrity, permissions, and sharing installable APK.
- Use a **persistent secure production release keystore** managed as GitHub Actions secrets or an approved signing service. Never commit private keys or print secrets. An ephemeral debug key is NOT a production release signature. Verify signing, package ID, version and upgrade path; if prior debug installation cannot be upgraded due to signing mismatch, document uninstall/reinstall/data consequences.
- Release workflow should produce a **signed, non-debug APK**, SHA-256 checksum, release notes, version tag `v1.0.0` and a real GitHub Release with working download link. Consider AAB separately only if needed.
- **Do not publish v1.0.0 as stable** if release gates are unverified. Keep work in PR and clearly identify blockers. If production signing credentials or two-phone tests require user input, request only those essentials rather than claiming completion.
- Review existing GitHub Actions permissions; use least privilege and explicitly scoped release publishing token, no unnecessary write permissions in ordinary CI.

## 8. Delivery process and proof

1. Inspect latest GitHub state; report what is already done and what's missing. Do not redo complete fixes.
2. Fix failing CI first; push commits and get green validation.
3. Implement small independent fixes: logo, illustration, share flow.
4. Implement QR end-to-end and native file browser + real multi-file transfer with regression tests.
5. Profile and improve transfer performance, publish reproducible measurements.
6. Compare actual Compose captures with `EZ-Prototype.html` and user screenshots/video. Do not invent a 'pixel-perfect' claim.
7. Prepare signed Release, verify on two physical Android phones, then publish `v1.0.0` only when ready.
8. Return links to commits/PR, green Actions run, release tag, APK, SHA-256 and concise test evidence/blockers.

## 9. Visual reference attachments to inspect

Please use the attachments included with the user's message when available:

- `EZ-Prototype.html` (approved source of truth for design).
- `Screenshot_20261018_063412_com.ezshare.app.jpg` (Share EZ sheet with inactive options).
- `Screenshot_20261018_063539.jpg` (white-backed header logo in dark mode).
- `Screenshot_20261018_065344_com.ezshare.app.jpg` (Receive monitor/phone vector error).
- `Screenshot_20261018_065407_com.ezshare.app.jpg` (sender QR tile and nearby-device UI; **not** a navigation-overlap bug).
- `SVID_20261018_064709_1.mp4` (user's in-app file selection experience).
- Optional historical context: `EZ-Codex-Design-Fix.md`, `EZ-Codex-Master-Prompt.md`, previous installed debug APK.

If an attachment is absent in Codex's environment, say so and request it only if necessary. GitHub source is authoritative for code; screenshots/video are user-supplied visual evidence. Do not assert you inspected absent attachments.

**FINAL INSTRUCTION: Execute, commit and verify, not merely plan. Preserve the existing project. The next public version is EZ 1.0.0 Release only after all release gates are genuinely satisfied.**
