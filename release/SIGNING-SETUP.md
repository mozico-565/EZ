# Production signing — owner action required

The repository had no repository/environment Actions secrets when inspected. No private production key was created, committed, printed or uploaded by this task. Do not substitute the development cache keystore. Keep an offline encrypted backup of the persistent release key: losing it can prevent future app updates.

In GitHub (mobile browser works): repository → Settings → Secrets and variables → Actions → New repository secret. Configure these from your existing private production keystore, outside chat:

| Secret | Value |
|---|---|
| EZ_RELEASE_KEYSTORE_BASE64 | Base64 encoding of the complete persistent JKS/PKCS12 file, as one line |
| EZ_RELEASE_STORE_PASSWORD | Keystore password |
| EZ_RELEASE_KEY_ALIAS | Signing entry alias |
| EZ_RELEASE_KEY_PASSWORD | Entry password |

Under Variables configure `EZ_RELEASE_CERT_SHA256` with the public SHA-256 certificate fingerprint (64hex characters). Never send private keystore bytes/passwords in chat. No broad PAT is needed: the workflow uses GitHub's scoped job token.

If a production key does not exist, create and retain it under your control using Android Studio Generate Signed Bundle/APK or the JDK's official keytool, then configure the above. This is an essential owner setup, not a reason to label a debug build stable.

## Signed candidate without stable publication

The existing `android.yml` workflow supports a signed-candidate job only on `workflow_dispatch`. It can be run from Actions → EZ Android validation → Run workflow → branch `fix/native-design-parity`. Ordinary pushes/PRs never access production secrets and retain contents:read. On manual execution, signing fails closed if credentials or the expected certificate are missing. Result artifact: `EZ-1.0.0-signed-candidate`, containing the non-debug APK, checksum and metadata. This is for acceptance and is not a stable GitHub Release.

## Publish only the tested binary

After testing the exact signed candidate on two phones, complete `release/PHYSICAL-ACCEPTANCE.json` with actual device details/results and candidate SHA/certificate/source fingerprint. Set repository variable `EZ_RELEASE_APPROVED_SHA` to that candidate's exact source commit. Do not set pass for unperformed tests.

The reviewed `.github/workflows/release.yml` must be available on the repository's default branch for its manual dispatch UI. Reconcile the PR1→main / PR2→alpha chain through normal reviewed merges; never reset/force-push existing work. Its `publish` mode takes the successful candidate run ID and downloads that exact artifact, rather than signing a different binary after phone testing. It checks source fingerprint, binary hash, certificate, run provenance and every physical gate. Only that publishing job has contents:write/actions:read. It creates tagv1.0.0 without overwriting an existing tag, then a real Release with APK/checksum/notes. Before that, no stable tag is created.

Repository privacy controls who can download a release. The current GitHub repository displays Public (visibility was not changed by this task). The app's official-link action does not invent public access or embed a token: it verifies an unauthenticated published non-prerelease URL before sharing. Once a real public v1.0.0 Release is published, its verified link can be shared. If privacy changes later, do not expose source automatically. Offline complete APK sharing remains independent of public links.

Old Alpha0.1/0.1.1 debug installs cannot be promised an in-place production update due to different signing certificates. Preserve important app data before uninstall/reinstall. All future production updates must retain the same signing identity.
