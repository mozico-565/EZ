#!/usr/bin/env bash
set -euo pipefail
mkdir -p network-evidence
# Current command-line tools and emulator releases can choose different default
# Android user directories. Give both an explicit AVD registry and content path.
export ANDROID_USER_HOME="$RUNNER_TEMP/ez-android-home"
export ANDROID_AVD_HOME="$ANDROID_USER_HOME/avd"
mkdir -p "$ANDROID_AVD_HOME"
adb start-server
trap 'adb -s emulator-5554 emu kill || true; adb -s emulator-5556 emu kill || true' EXIT
sdkmanager 'system-images;android-29;google_apis;x86_64'
for slot in A B; do
  echo no | avdmanager create avd -n "EZ_$slot" -k 'system-images;android-29;google_apis;x86_64' --device pixel_2 --force --path "$ANDROID_AVD_HOME/EZ_$slot.avd"
  test -f "$ANDROID_AVD_HOME/EZ_$slot.avd/config.ini"
  printf 'avd.ini.encoding=UTF-8\npath=%s\ntarget=android-29\n' "$ANDROID_AVD_HOME/EZ_$slot.avd" > "$ANDROID_AVD_HOME/EZ_$slot.ini"
  port=5554; [ "$slot" = B ] && port=5556
  "$ANDROID_HOME/emulator/emulator" -avd "EZ_$slot" -port "$port" -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect -memory 1536 -cores 2 -camera-back none -camera-front none > "network-evidence/$slot-emulator.txt" 2>&1 &
done
"$ANDROID_HOME/emulator/emulator" -list-avds | tee network-evidence/avd-registry.txt
for serial in emulator-5554 emulator-5556; do
  timeout 180 adb -s "$serial" wait-for-device
  timeout 240 bash -c 'until [ "$(adb -s "$1" shell getprop sys.boot_completed | tr -d "\r")" = 1 ]; do sleep 2; done' _ "$serial"
  adb -s "$serial" shell input keyevent 82
  adb -s "$serial" install official/EZ-1.0.0.apk
  adb -s "$serial" shell am start -n com.ezshare.app/.MainActivity
  sleep 2
  adb -s "$serial" shell dumpsys package com.ezshare.app > "network-evidence/$serial-official-package.txt"
  adb -s "$serial" exec-out screencap -p > "network-evidence/$serial-official.png"
  adb -s "$serial" uninstall com.ezshare.app
  adb -s "$serial" install baseline/app/build/outputs/apk/debug/app-debug.apk
  adb -s "$serial" install baseline/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
done
sudo tcpdump -i any -s 160 -U -w network-evidence/encrypted-tcp.pcap 'tcp port 45001 or tcp port 45002' > network-evidence/tcpdump.txt 2>&1 &
TCP_PID=$!
trap 'sudo kill "$TCP_PID" 2>/dev/null || true; adb -s emulator-5554 emu kill || true; adb -s emulator-5556 emu kill || true' EXIT
python3 scripts/dual_peer_test.py --baseline --output network-evidence/baseline
for serial in emulator-5554 emulator-5556; do
  adb -s "$serial" uninstall com.ezshare.app.test
  adb -s "$serial" uninstall com.ezshare.app
  adb -s "$serial" install app/build/outputs/apk/debug/app-debug.apk
  adb -s "$serial" install app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
done
python3 scripts/dual_peer_test.py --output network-evidence/patched
