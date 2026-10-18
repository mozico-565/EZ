# Production signing — owner action required

The repository had no repository/environment Actions secrets when inspected. No production key has yet been generated. The first-time setup below is prepared but requires the owner password before execution. Do not substitute the development cache keystore. Keep an offline encrypted backup of the persistent release key: losing it can prevent future app updates.

In GitHub (mobile browser works): repository → Settings → Secrets and variables → Actions → New repository secret. Configure these from your existing private production keystore, outside chat:

| Secret | Value |
|---|---|
| EZ_RELEASE_KEYSTORE_BASE64 | Base64 encoding of the complete persistent JKS/PKCS12 file, as one line |
| EZ_RELEASE_STORE_PASSWORD | Keystore password |
| EZ_RELEASE_KEY_ALIAS | Optional for first-time setup: defaults to `ez-release`; set explicitly for another existing alias |
| EZ_RELEASE_KEY_PASSWORD | Optional for first-time PKCS12: defaults to the saved store password; set explicitly if different |

Under Variables configure `EZ_RELEASE_CERT_SHA256` with the public SHA-256 certificate fingerprint (64hex characters). Never send private keystore bytes/passwords in chat. No broad PAT is needed: the workflow uses GitHub's scoped job token.

If a production key does not exist, create and retain it under your control using Android Studio Generate Signed Bundle/APK or the JDK's official keytool, then configure the above. This is an essential owner setup, not a reason to label a debug build stable.

## First-time setup (owner confirmed no existing production key)

1. In repository Actions Secrets, create `EZ_RELEASE_STORE_PASSWORD` yourself. Use a password manager to generate and retain a random ASCII password of at least 32 characters with no whitespace. Do not type it in chat. It will encrypt the permanent keystore; losing this password and backup can prevent future updates.
2. Run `android.yml` manually on `fix/native-design-parity`, operation `prepare-first-key`, confirmation `CREATE-FIRST-KEY`. This mode never builds/publishes an APK, refuses reruns and refuses an already configured keystore. Pushes and PRs cannot create a production key. The minimal default-branch launcher exposes these inputs; explicitly select the reviewed application branch. The launcher itself loads no secrets and merges no application code. When the PR chain is later merged, retain the full reviewed workflow instead of the launcher stub; do not remove the confirmation guard.
3. Download `EZ-FIRST-KEY-ENCRYPTED-BACKUP-<run-id>` immediately. It expires after **7 days**. Keep **two private offline copies**, with the password stored separately. This is a password-encrypted PKCS12 (RSA4096, SHA256withRSA, 10000 days, PBES2 protection with 600000 iterations), not a plaintext private key. Base64 is only encoding, not additional encryption. Because this repository is public, other authenticated GitHub users may download the encrypted artifact during retention: a long random password is essential. Never publish the backup in Releases or commit it.
4. Owner-only: copy `EZ_RELEASE_KEYSTORE_BASE64.txt` from the backup into secret `EZ_RELEASE_KEYSTORE_BASE64`; the prepared workflow defaults to alias `ez-release` and the saved store password, so no second password entry is needed for this new PKCS12. Optional alias/key-password secrets override these defaults for imported identities. Keep `EZ_RELEASE_STORE_PASSWORD` unchanged. Set variable `EZ_RELEASE_CERT_SHA256` from `PUBLIC-IDENTITY.json`. Do not send these secret values in chat.
5. Run operation `sign`. Verify the APK certificate matches the backed-up identity. Install/test the **exact signed candidate** on both phones before the release gate can pass. Once signing is configured, never run first-key preparation again.

Only the encrypted keystore/base64, public certificate and public identity metadata enter the short-lived artifact. Passwords never enter artifacts, logs, command arguments or source. The workflow does not obtain Secrets-write permissions or a PAT. The owner must enter new credentials; the agent does not fill password fields. An automatic setup test generates and deletes a separate disposable identity; that test identity must never be used for production.

## Signed candidate without stable publication

The existing `android.yml` workflow supports a signed-candidate job only on `workflow_dispatch`. It can be run from Actions → EZ Android validation → Run workflow → branch `fix/native-design-parity`. Ordinary pushes/PRs never access production secrets and retain contents:read. Choose operation `sign` (the default `validate` never signs). On signing execution, signing fails closed if credentials or the expected certificate are missing. Result artifact: `EZ-1.0.0-signed-candidate`, containing the non-debug APK, checksum and metadata. This is for acceptance and is not a stable GitHub Release.

## Publish only the tested binary

After testing the exact signed candidate on two phones, complete `release/PHYSICAL-ACCEPTANCE.json` with actual device details/results and candidate SHA/certificate/source fingerprint. Set repository variable `EZ_RELEASE_APPROVED_SHA` to that candidate's exact source commit. Do not set pass for unperformed tests.

The reviewed `.github/workflows/release.yml` must be available on the repository's default branch for its manual dispatch UI. Reconcile the PR1→main / PR2→alpha chain through normal reviewed merges; never reset/force-push existing work. Its `publish` mode takes the successful candidate run ID and downloads that exact artifact, rather than signing a different binary after phone testing. It checks source fingerprint, binary hash, certificate, run provenance and every physical gate. Only that publishing job has contents:write/actions:read. It creates tagv1.0.0 without overwriting an existing tag, then a real Release with APK/checksum/notes. Before that, no stable tag is created.

Repository privacy controls who can download a release. The current GitHub repository displays Public (visibility was not changed by this task). The app's official-link action does not invent public access or embed a token: it verifies an unauthenticated published non-prerelease URL before sharing. Once a real public v1.0.0 Release is published, its verified link can be shared. If privacy changes later, do not expose source automatically. Offline complete APK sharing remains independent of public links.

Old Alpha0.1/0.1.1 debug installs cannot be promised an in-place production update due to different signing certificates. Preserve important app data before uninstall/reinstall. All future production updates must retain the same signing identity.
