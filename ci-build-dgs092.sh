#!/usr/bin/env bash
set -euo pipefail
rm -rf dgs092
mkdir -p dgs092
base64 --decode dgs092-source.zip.b64 > dgs092.zip
unzip -q dgs092.zip -d dgs092
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/usr/local/lib/android/sdk}}"
SDKMANAGER="$(find "$SDK/cmdline-tools" -type f -name sdkmanager 2>/dev/null | sort | tail -n 1)"
test -n "$SDKMANAGER"
yes | "$SDKMANAGER" --sdk_root="$SDK" --licenses >/dev/null 2>&1 || true
"$SDKMANAGER" --sdk_root="$SDK" "platforms;android-34" "build-tools;34.0.0"
cd dgs092
chmod +x BUILD_APK.sh
ANDROID_SDK_ROOT="$SDK" ANDROID_BUILD_TOOLS="$SDK/build-tools/34.0.0" ANDROID_PLATFORM_JAR="$SDK/platforms/android-34/android.jar" ./BUILD_APK.sh
test -s DragonGameStudio-0.9.2-debug.apk
"$SDK/build-tools/34.0.0/apksigner" verify --verbose --print-certs DragonGameStudio-0.9.2-debug.apk | tee certs.txt
grep -q 'd2467e6ed5b4b815c605c8670660e287494c8e8556f93f24932cb75717f30e93' certs.txt
