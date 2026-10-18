# EZ 1.0.0 — signed candidate ready; official publication blocked on acceptance

The existing Kotlin/Jetpack Compose application and PR2 are preserved on `fix/native-design-parity`. No force-push, blind application merge or capsule/scroll redesign was performed. Main contains only the original README and a minimal read-only manual workflow launcher; reconcile that launcher normally when merging the reviewed PR chain.

## Verified signed binary

GitHub Actions run **39138474099** succeeded for candidate source **2ba7ee7991c1662296918219c7b42873534378a4**. The APK is `com.ezshare.app`, versionName **1.0.0**, versionCode **10**, non-debuggable, signed with the permanent EZ production identity. CI apksigner verification passed against the pinned certificate. Downloaded binary SHA-256 was checked again against candidate metadata.

- APK SHA-256: `a1e1546a21a571c58fb615245b360b28859b253fe5d8c8c51fde490c5bd7bf11`
- Certificate SHA-256: `e8b79642b969337b9550dfb691994e1d90f52b438d3c8a82c4fcbb8048b3a908`
- Validation: **25 core + 28 Android/Compose + 4 signing-setup + 2 release-gate tests**, zero failures. Debug/release lint: zero errors, 44 warnings each. Signed assembleRelease succeeded. Eighteen native captures were produced; fixture screens are not physical transfer/camera evidence.
- The owner-approved first-key run39137952284 succeeded. The encrypted backup was checked for certificate/hash/base64 consistency and saved separately. No private keystore or password is committed. Public identity: PRODUCTION-IDENTITY.json. Exact APK evidence: SIGNED-CANDIDATE.json.

## Implemented scope

Themed transparent header mark, corrected Receive vector, verified complete APK export/native Sharesheet and honest published-link lookup; native MediaStore/authorized SAF file browser with seven categories, search/sort/thumbnails/persistent multi-selection; CameraX/ZXing local QR pairing with strict expiry/address validation and both-phone SAS approval; encrypted protocol2 batches with per-file integrity/readback/receipt, safe partial/error/history handling; actual per-file/overall progress and stage timings. Existing visual tokens, launcher geometry, navigation, RTL, themes and motion remain preserved.

Synthetic localhost 4MB/100MB/1GB and 64-small-file timings are reproducible benchmarks only, not radio/device measurements or a two-second promise. Wi-Fi Direct, pause/resume and process-death recovery are not implemented or claimed.

## Outstanding official-release gates

1. **Real two-phone acceptance is unperformed.** Install this exact signed APK on both phones, follow PHYSICAL-TESTING.md, and record actual devices/results in PHYSICAL-ACCEPTANCE.json. Its hashes are prefilled, but completed remains false and all tests remain not_tested. An owner form provided with the review files exports the same schema without preselecting success.
2. **Durable CI keystore Secret is not yet configured.** The first signed candidate imported the approved encrypted backup from run39137952284 using the saved password and pinned certificate. That bootstrap expires with its seven-day artifact. Keep two private backup copies and have the owner enter the backed-up base64 into EZ_RELEASE_KEYSTORE_BASE64 before future release builds. Do not change the password or create another identity.
3. After actual acceptance, set EZ_RELEASE_APPROVED_SHA to this candidate's exact source commit and reconcile PR2→alpha / PR1→main through normal reviewed merges, retaining the full production workflow. The publisher downloads and verifies this exact tested signed artifact; it must not rebuild a different APK after acceptance.

No v1.0.0 stable tag or GitHub Release has been published. The official-link option remains unavailable until a real release exists. The signed APK is ready for final acceptance, not proof that physical tests have passed. The repository currently displays Public; this task did not change its visibility.

Old Alpha debug installs have a different signing identity and may require uninstall/reinstall. Preserve important app data first. Future production updates must retain the new permanent EZ certificate.
