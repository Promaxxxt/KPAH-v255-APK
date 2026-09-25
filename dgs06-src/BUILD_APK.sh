#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")" && pwd)"
sdk_dir="${ANDROID_SDK_ROOT:-$project_dir/../sdk}"
tools_dir="${ANDROID_BUILD_TOOLS:-$sdk_dir/android-14}"
platform_jar="${ANDROID_PLATFORM_JAR:-$sdk_dir/android-34/android.jar}"
build_dir="$project_dir/build"
mkdir -p "$build_dir/classes"
find "$project_dir/src" -name '*.java' > "$build_dir/sources.txt"
"$tools_dir/aapt" package -f -M "$project_dir/AndroidManifest.xml" -I "$platform_jar" -A "$project_dir/src" -F "$build_dir/resources.apk"
java com.sun.tools.javac.Main -source 8 -target 8 -encoding UTF-8 -cp "$platform_jar" -d "$build_dir/classes" @"$build_dir/sources.txt"
python3 - "$build_dir" <<'PY'
import os,sys,zipfile
b=sys.argv[1]
with zipfile.ZipFile(os.path.join(b,'classes.jar'),'w',zipfile.ZIP_DEFLATED) as z:
 for parent,folders,files in os.walk(os.path.join(b,'classes')):
  for name in files:
   if name.endswith('.class'):
    path=os.path.join(parent,name)
    z.write(path,os.path.relpath(path,os.path.join(b,'classes')))
PY
"$tools_dir/d8" --min-api 26 --output "$build_dir" "$build_dir/classes.jar"
python3 - "$build_dir" <<'PY'
import os,sys,zipfile
b=sys.argv[1]
with zipfile.ZipFile(os.path.join(b,'resources.apk')) as inp,zipfile.ZipFile(os.path.join(b,'unsigned.apk'),'w') as out:
 for item in inp.infolist():out.writestr(item,inp.read(item.filename))
 out.write(os.path.join(b,'classes.dex'),'classes.dex')
PY
"$tools_dir/zipalign" -f 4 "$build_dir/unsigned.apk" "$build_dir/aligned.apk"
"$tools_dir/apksigner" sign --ks "$project_dir/debug.keystore" --ks-key-alias dragon-debug --ks-pass pass:android --key-pass pass:android --out "$project_dir/DragonGameStudio-0.6-debug.apk" "$build_dir/aligned.apk"
"$tools_dir/apksigner" verify "$project_dir/DragonGameStudio-0.6-debug.apk"
echo "$project_dir/DragonGameStudio-0.6-debug.apk"