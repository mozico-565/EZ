# Two-phone validation — required, not performed remotely

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
