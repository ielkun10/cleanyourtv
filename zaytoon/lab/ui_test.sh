#!/usr/bin/env bash
# Runs on the GitHub Android TV emulator (1920x1080, API 34) after the APK is built: installs it, opens Home and
# every tab of the panel, walks the remote's arrows, and saves screenshots and the log to zaytoon/lab/ui/.
set -u
APK=$1
OUT=zaytoon/lab/ui
PKG=sa.zaytoon.home
mkdir -p "$OUT"
shot() { sleep "${2:-2}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1"; }
key() { for k in "$@"; do adb shell input keyevent "$k"; sleep 0.7; done; }

adb shell wm size
adb shell getprop ro.build.version.release
adb logcat -c
adb install -r -g "$APK" || { echo "INSTALL FAILED"; exit 1; }
adb shell dumpsys package $PKG | grep -E "versionName|versionCode|minSdk|targetSdk" | head -4

adb shell am start -n $PKG/com.gathin.tvhome.MainActivity
shot 01-home-first-seconds 8
shot 02-home-after-data 40
# Wallpapers download in the background; give the first ones time to arrive and Home to rebuild with them.
shot 03-home-with-wallpaper 90
key DPAD_RIGHT DPAD_RIGHT
shot 04-dock-focus-right 1
key DPAD_LEFT DPAD_LEFT DPAD_LEFT DPAD_LEFT
shot 05-dock-focus-left 1
key DPAD_UP
shot 06-top-bar 1
for i in 1 2 3 4 5 6; do key DPAD_LEFT; done
shot 07-top-bar-left-end 1
for i in 1 2 3 4 5 6 7 8; do key DPAD_RIGHT; done
shot 08-top-bar-right-end 1
key DPAD_DOWN
shot 09-back-to-dock 1
key DPAD_DOWN
shot 10-down-from-dock 1

for tab in 0 1 2 3 4 5; do
  adb shell am start -n $PKG/sa.zaytoon.ZaytoonActivity --ei tab $tab
  shot "20-panel-tab-$tab" 6
  adb shell input keyevent BACK
  sleep 1
done
adb shell am start -n $PKG/sa.zaytoon.ZaytoonActivity --ei tab 2
sleep 6
key DPAD_LEFT
shot 30-sports-content-focus 1
for i in 1 2 3 4 5 6; do key DPAD_DOWN; done
shot 31-sports-scrolled 1
key DPAD_RIGHT
shot 32-back-to-tabs 1
key DPAD_UP DPAD_UP DPAD_UP DPAD_UP
key DPAD_LEFT
for i in 1 2 3 4 5 6; do key DPAD_DOWN; done
shot 33-weather-rows 1
key DPAD_UP DPAD_UP DPAD_UP DPAD_UP DPAD_UP DPAD_UP DPAD_UP DPAD_UP
key DPAD_DOWN DPAD_CENTER
shot 34-city-chooser 2
key BACK
shot 35-after-back 1
key BACK
sleep 2
shot 36-home-after-panel 3

adb logcat -d > "$OUT/logcat.txt"
grep -n -E "FATAL EXCEPTION|AndroidRuntime: |Zaytoon" "$OUT/logcat.txt" | head -80 > "$OUT/errors.txt" || true
if grep -q "FATAL EXCEPTION" "$OUT/logcat.txt"; then echo "CRASH found, see errors.txt"; touch "$OUT/CRASHED"; fi
adb shell ls -la /sdcard/Android/data/$PKG/files/ /sdcard/Android/data/$PKG/files/wallpapers/ > "$OUT/files.txt" 2>&1 || true
adb shell cat /sdcard/Android/data/$PKG/files/config.json > "$OUT/config.json" 2>&1 || true
exit 0
