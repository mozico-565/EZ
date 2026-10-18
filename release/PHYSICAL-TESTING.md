# Two-phone acceptance — not performed in this environment

Install the **same signed candidate APK** on both phones and record its SHA-256 and certificate. Record model/Android version; test both sender/receiver directions. Do not substitute debug, unsigned, or a later rebuild.

1. Idle/Home light/dark, header mark without white frame, corrected monitor/phone silhouette, launcher/splash, 360dp screen, 150% fonts, RTL and back. Retain existing capsule/scroll behavior.
2. Deny media permissions; permit selected photos only where supported; later grant/revoke while app is backgrounded and refresh. Documents/other files appear only inside folders authorized via SAF. No MANAGE_EXTERNAL_STORAGE permission.
3. Search existing files and no-result query, clear, switch7categories, sort name/size/date and direction, list/grid, thumbnail/preview, long Arabic names. Select files in multiple categories and confirm persistent count/total. Check256file/16GiB-per-file limits are honest.
4. Installed-app label/icon: verify a standalone APK exports; verify split app cannot be mislabeled as installable base-only APK.
5. Choose a local receive folder, start Receive; decode full QR using the other phone's camera. Test valid QR both directions, invalid scheme/public IP/expired/wrong-session/wrong network, denied camera and retry. Compare six-digit SAS on both phones; explicitly approve both. Reject once on each side and verify no file bytes saved.
6. Discover by NSD and manual IP:PORT. Same router/hotspot; no internet. Verify old Alpha peer gets an explicit compatibility error. Test two concurrent requests and rapid taps without starting duplicate sessions.
7. Transfer4MB/100MB/1GB and mixed multi-file batches (photos/videos/docs/audio/standalone apps), unknown-size provider if available, zero-byte file and duplicate names. Compare sender/receiver SHA-256 and sizes. Counts/progress remain per-file plus total, speed is actual streaming. No partial file is marked complete.
8. Cancel, lose network mid-file and in a later batch file, fill storage, revoke folder access, stop/background and screen-off under OEM policy. Earlier verified files remain. Current partial is deleted where supported; cleanup failure explicitly warns about `.ezpart`. If receipt is lost after verified publication, sender may report failure although a valid receiver file exists; never automatically claim remote success.
9. Share EZ APK with Bluetooth/Quick Share or another installed target: chooser launches, only read access is granted to one content URI, recipient can read and **install complete signed APK**; no private files are exposed. Test app installed as split: actionable limitation, no false standalone claim.
10. Official link is unavailable until a real accessible Release exists. On published public release, verify the real download URL and sharing intent; with no network, offer APK sharing instead. Private releases require an owner-approved distribution decision, not silently published source.
11. Run speed methodology in PERFORMANCE.md (bands, repeated trials, discovery/approval/stream/storage/receipt separately). Do not label localhost results as device benchmarks.

Record actual results in PHYSICAL-ACCEPTANCE.json. The workflow blocks stable publication on not_tested/fail, mismatched binary/source/certificate or missing production identity.
