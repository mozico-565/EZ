#!/usr/bin/env bash
# Preserve real-device-provider test status while collecting diagnostics on failure.
set -uo pipefail
chmod +x gradlew
./gradlew --no-daemon :app:connectedDebugAndroidTest
check_status=$?
mkdir -p native-evidence
adb pull /sdcard/Download/EZ/native-apk-sharesheet.png native-evidence/ || true
exit "$check_status"
