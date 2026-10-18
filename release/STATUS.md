# EZ 1.0.0 release work — gates still closed

Continued PR2 at7445dae (historical home failure already fixed). Existing main/alpha branches are preserved; work remains on fix/native-design-parity, no force-push or blind merge. Capsule navigation/scrolling unchanged.

Implemented in source: transparent/themed header mark, corrected Receive vector, complete verified APK export/sharesheet, official-release link verification, MediaStore/authorized-folder browser with7categories/search/sort/thumbnails/multi-select, CameraX/ZXing local QR and strict payload validation, protocol2 approved batch transfer, per-file receipts/partial safety/history, stage timings and release gates.

Evidence so far: original12 core tests plus13 batch/QR tests pass locally. First integrated Android release build8d9b82f passed run39096801377, including Android tests, debug/release lint and assembleRelease. Later selection/scanner/gate tests and final refinements are being validated in the next run. Native captures are review images, not physical-camera proof.

Synthetic JVM localhost4MB/100MB/1GB and64small-file batch timings are recorded with source and limitations. No device/radio benchmarks or fixed2second promise.

Production signing: repository/environment Actions secrets were empty when inspected. Pipeline is prepared but no production key has been supplied or used. No signed candidate/stable v1.0.0 tag/Release exists from this task. Physical matrix remains not_tested, with explicit fail-closed checks binding exact APK/certificate/source/approval. Official link remains honestly unavailable until a real published release is verified. GitHub currently displays Public; this task did not change visibility.

Next essential owner input after code validation: persistent signing credentials in Actions secrets, then installation/testing of the exact signed candidate on two phones. Do not mark complete or publish stable before those gates.


## First-time production signing preparation

Owner confirmed no existing production signing key. Added intentional manual first-key setup to the existing Android workflow, an encrypted PKCS12 backup generator and three tests (missing password, existing-key refusal, encrypted round trip / wrong-password / overwrite protection). The owner must enter a new long random password directly in Actions Secrets before generation. No production key has been generated yet; no signed APK or official release is claimed. The latest application binary/source remains the previously validated 73fd182 candidate; these changes affect signing infrastructure/docs only.

The publisher now accepts the exact successful manual signed-candidate artifact from android.yml as well as release.yml, preserving all binary/hash/certificate/source/two-device acceptance checks. This lets the first candidate be tested from the existing branch without rebuilding it after acceptance.


## Production signing identity created (18 October 2026)

Owner explicitly approved creation and seven-day encrypted backup upload on the public repository. Run39137952284 succeeded; encrypted backup integrity, public certificate and encoded-keystore consistency were verified. Public identity is in PRODUCTION-IDENTITY.json; private bytes remain outside source. Persistent keystore Secret is not yet configured. Added an explicit first-candidate import from the verified backup run to reuse the owner-entered password without copying private material through chat. Pinning, provenance and tamper rejection are covered by the signing-setup tests. Production signing/build and physical acceptance still must succeed before an official release can be claimed.
