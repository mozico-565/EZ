#!/usr/bin/env bash
set -euo pipefail
# Ephemeral synthetic THIRD-PARTY TEST key. Never uses production credentials.
bt="$ANDROID_HOME/build-tools/35.0.0"
mkdir -p "$RUNNER_TEMP/apk-fixture"
"$bt/aapt" package -f -M scripts/apk-fixture/AndroidManifest.xml -I "$ANDROID_HOME/platforms/android-35/android.jar" -F "$RUNNER_TEMP/apk-fixture/unsigned.apk"
keytool -genkeypair -keystore "$RUNNER_TEMP/apk-fixture/test.jks" -alias fixture -storepass android -keypass android -keyalg RSA -validity 2 -dname 'CN=Disposable third-party fixture'
"$bt/zipalign" -p -f 4 "$RUNNER_TEMP/apk-fixture/unsigned.apk" "$RUNNER_TEMP/apk-fixture/aligned.apk"
"$bt/apksigner" sign --ks "$RUNNER_TEMP/apk-fixture/test.jks" --ks-pass pass:android --out "$RUNNER_TEMP/apk-fixture/fixture.apk" "$RUNNER_TEMP/apk-fixture/aligned.apk"
"$bt/apksigner" verify "$RUNNER_TEMP/apk-fixture/fixture.apk"
for kind in SplitBase SplitConfig; do
  "$bt/aapt" package -f -M "scripts/apk-fixture/$kind.xml" -I "$ANDROID_HOME/platforms/android-35/android.jar" -F "$RUNNER_TEMP/apk-fixture/$kind-unsigned.apk"
  "$bt/zipalign" -p -f 4 "$RUNNER_TEMP/apk-fixture/$kind-unsigned.apk" "$RUNNER_TEMP/apk-fixture/$kind-aligned.apk"
  "$bt/apksigner" sign --ks "$RUNNER_TEMP/apk-fixture/test.jks" --ks-pass pass:android --out "$RUNNER_TEMP/apk-fixture/$kind.apk" "$RUNNER_TEMP/apk-fixture/$kind-aligned.apk"
done
