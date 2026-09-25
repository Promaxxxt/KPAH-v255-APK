#!/usr/bin/env bash
set -euo pipefail
rm -rf dgs091
mkdir -p dgs091
base64 --decode dgs091-source.tar.gz.b64 | tar -xz -C dgs091
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/usr/local/lib/android/sdk}}"
SDKMANAGER="$(find "$SDK/cmdline-tools" -type f -name sdkmanager 2>/dev/null | sort | tail -n 1)"
test -n "$SDKMANAGER"
yes | "$SDKMANAGER" --sdk_root="$SDK" --licenses >/dev/null 2>&1 || true
"$SDKMANAGER" --sdk_root="$SDK" "platforms;android-34" "build-tools;34.0.0"
cd dgs091
chmod +x BUILD_APK.sh
ANDROID_SDK_ROOT="$SDK" ANDROID_BUILD_TOOLS="$SDK/build-tools/34.0.0" ANDROID_PLATFORM_JAR="$SDK/platforms/android-34/android.jar" ./BUILD_APK.sh
test -s DragonGameStudio-0.9.1-debug.apk
"$SDK/build-tools/34.0.0/apksigner" verify --verbose --print-certs DragonGameStudio-0.9.1-debug.apk | tee certs.txt
grep -q 'd2467e6ed5b4b815c605c8670660e287494c8e8556f93f24932cb75717f30e93' certs.txt
