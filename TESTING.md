# Current release acceptance

Signed candidate run39138474099 passed 25 core, 28 Android/Compose, 4 signing-setup and 2 gate tests, plus debug/release lint and signed assembleRelease. See release/SIGNED-CANDIDATE.json for the exact binary/certificate/source. The 17-case **physical** matrix has not been performed. Install the same production-signed APK on two phones; never use a historical debug artifact for current acceptance.

See release/PHYSICAL-TESTING.md and release/PHYSICAL-ACCEPTANCE.json for1.0.0. Historical Alpha checks below remain context, not current release evidence.

# Historical Alpha two-phone validation — not current release evidence

Record both models, Android versions, APK run/commit, Wi-Fi band/router or hotspot, approximate distance and free destination capacity. Android14/15 are useful targets; exact models are unknown.

- [ ] Install SAME debug APK on two phones. No crash on launch; Arabic RTL/logo/colors recognizable.
- [ ] Start receiver after choosing Documents/EZ. Manual IP/port shown. NSD discovers it if router permits.
- [ ] Pick a small real image. Compare six-digit SAS on both phones; accept. Verify bytes, SHA256, file opens on receiver and content/name are correct.
- [ ] Reject on receiver and separately reject on sender: no file content accepted/success, UI recoverable.
- [ ] Send100MB: record sender/receiver bytes, duration, averageMB/s and SHA. Do not compare averages without excluding approval time (engine starts transfer clock after approval).
- [ ] Send~1GB: record same, observe memory/background behavior and no full-memory buffering.
- [ ] Disconnect Wi-Fi mid-file: clear failure, no crash/success. Partial cleanup attempted; check for `.ezpart-*`. Retry starts anew, NOT resume.
- [ ] Cancel while waiting, connecting, receiving and sending; sockets/server close and no final success.
- [ ] Leave approval unanswered >120s, stall connection >30s: failure without partial success.
- [ ] Select a file whose URI size is unknown. Count streamed bytes and final integrity; unknown percentage is acceptable.
- [ ] Send empty file; very long Arabic/duplicate filenames; verify no path traversal or overwriting. Duplicate adds suffix.
- [ ] Choose a full/readonly/unsupported/cloud destination provider: error is truthful; no successful partial file. Test local-folder rename/delete.
- [ ] Deny notifications; file picker cancel; folder picker cancel: no crash, explanation and recovery.
- [ ] Screen off/background for long file; observe foreground notification and cancel action on both OEMs.
- [ ] AP isolation/VPN/no peers: manual fallback and errors are clear.

Return: model/version for each, Actions run + APK commit, sizes, elapsed seconds, averageMB/s, SHA matches, observed errors/screenshots, whether files open and any leftover `.ezpart-*`. A successful CI build does not prove any of these physical tests passed.

# Design correction 0.1.1 checks

- [ ] Launcher on circle/squircle masks: entire blue E and black Z visible, same aspect, no stretching. Check normal and themed monochrome icon, light/dark wallpaper, cold-start splash and label EZ.
- [ ] Compare native Home with supplied HTML at same device width: 24dp margins, purple hero/source art, orange right Send, blue left Receive/pink privacy stack, four category cards, black capsule with white active indicator.
- [ ] Small 360dp screen, display/font scale 1.0/1.3/2.0: scroll content; long Arabic filename wraps without horizontal clipping; controls remain reachable above keyboard/system bars.
- [ ] Tap Home/Files/History/Settings rapidly and Android Back; switch light/dark/system, reduce motion and restore settings; changes persist after restart.
- [ ] Real SAF file selection, checkbox, search no-results/clear, category filters, selected count/size, send CTA and manual IP are functional. Alpha deliberately selects one file only.
- [ ] Receive folder picker denial/cancel, active listening addresses, incoming SAS confirmation, rejection, accept and cancel remain truthful. No QR camera permission or simulated peer list.
- [ ] Real progress/result/error metrics and actual history/detail; clear history confirmation deletes metadata only.
- [ ] Settings identity updates both local preview and the name announced to the next peer; changes disabled during active listen/transfer.
- [ ] Share sheet and QR destination explicitly say coming soon and do not claim link/APK/scanning support.
- [ ] Repeat the full two-phone transfer checklist above after the UI correction. The CI core loopback test cannot establish OEM foreground-service or physical network behavior.

Generated native captures use Robolectric API35 and must not be labelled physical-device screenshots. Test transfer fixtures are restricted to test/ source and are never bundled as runtime file lists or measurements.
