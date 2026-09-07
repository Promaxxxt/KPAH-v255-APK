#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(sys.argv[1]).resolve()

# ---------------------------------------------------------------------------
# 1) Standalone Application startup
#    The original J2ME Loader debug build initializes MultiDex + ACRA before
#    MicroActivity exists.  The standalone APK does not need that launcher /
#    crash-reporting path.  Keep startup minimal and install our own persistent
#    uncaught-exception recorder so failures from KPAH worker threads (not only
#    MidletThread) are visible on the next launch.
# ---------------------------------------------------------------------------
app_java = ROOT / "app" / "src" / "main" / "java" / "ru" / "playsoftware" / "j2meloader" / "EmulatorApplication.java"
s = app_java.read_text(encoding="utf-8")
needle = "\t\tsuper.attachBaseContext(base);\n"
insert = r'''		super.attachBaseContext(base);
		if (!BuildConfig.FULL_EMULATOR) {
			ContextHolder.setApplication(this);
			Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
				String details = "KPAH fatal thread: " + thread.getName() + "\n\n"
						+ android.util.Log.getStackTraceString(error);
				android.util.Log.e("KPAH_FATAL", details, error);
				try {
					java.io.File crash = new java.io.File(getFilesDir(), "kpah_last_crash.txt");
					try (java.io.FileWriter writer = new java.io.FileWriter(crash, false)) {
						writer.write(details);
					}
				} catch (Throwable ignored) {
				}
				try {
					javax.microedition.shell.MicroActivity activity = ContextHolder.getActivity();
					if (activity != null) {
						activity.showFatalScreen(details);
					}
				} catch (Throwable ignored) {
				}
			});
			return;
		}
'''
if "kpah_last_crash.txt" not in s:
    if needle not in s:
        raise SystemExit("EmulatorApplication attachBaseContext marker not found")
    s = s.replace(needle, insert, 1)
app_java.write_text(s, encoding="utf-8")

# ---------------------------------------------------------------------------
# 2) MicroActivity persistent fatal screen
#    If Android kills the process after an uncaught Java exception, the
#    recorder above leaves a crash file.  Next launch displays it before the
#    game starts.  It also gives the global handler a very low-dependency UI.
# ---------------------------------------------------------------------------
micro = ROOT / "app" / "src" / "main" / "java" / "javax" / "microedition" / "shell" / "MicroActivity.java"
s = micro.read_text(encoding="utf-8")

activity_marker = "\t\tContextHolder.setCurrentActivity(this);\n"
activity_insert = r'''		ContextHolder.setCurrentActivity(this);
		File previousCrash = new File(getFilesDir(), "kpah_last_crash.txt");
		if (previousCrash.isFile()) {
			StringBuilder crashText = new StringBuilder();
			try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(previousCrash))) {
				String line;
				while ((line = reader.readLine()) != null) {
					crashText.append(line).append('\n');
				}
			} catch (Throwable t) {
				crashText.append(android.util.Log.getStackTraceString(t));
			}
			previousCrash.delete();
			showFatalScreen("KPAH previous crash\n\n" + crashText);
			return;
		}
'''
if "KPAH previous crash" not in s:
    if activity_marker not in s:
        raise SystemExit("MicroActivity current activity marker not found")
    s = s.replace(activity_marker, activity_insert, 1)

method_marker = "\tvoid showErrorDialog(String message) {\n"
fatal_method = r'''	public void showFatalScreen(String message) {
		runOnUiThread(() -> {
			android.widget.TextView text = new android.widget.TextView(this);
			text.setText(message);
			text.setTextSize(13f);
			text.setTextIsSelectable(true);
			text.setPadding(32, 32, 32, 32);
			android.widget.ScrollView scroll = new android.widget.ScrollView(this);
			scroll.addView(text);
			setContentView(scroll);
		});
	}

'''
if "public void showFatalScreen" not in s:
    if method_marker not in s:
        raise SystemExit("MicroActivity showErrorDialog marker not found")
    s = s.replace(method_marker, fatal_method + method_marker, 1)

micro.write_text(s, encoding="utf-8")

# ---------------------------------------------------------------------------
# 3) 16 KB ELF alignment for source-built native libraries.
#    NDK r22 uses lld and accepts these linker flags.  This covers javam3g and
#    micro3d, the two J2ME Loader native modules that can be loaded by game APIs.
# ---------------------------------------------------------------------------
android_mk = ROOT / "app" / "src" / "main" / "cpp" / "Android.mk"
s = android_mk.read_text(encoding="utf-8")
flags = "LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384\n"
if s.count(flags) < 2:
    first = "LOCAL_MODULE    := javam3g\n"
    second = "LOCAL_MODULE    := micro3d\n"
    if first not in s or second not in s:
        raise SystemExit("Android.mk module markers not found")
    if flags not in s.split(first, 1)[0]:
        s = s.replace(first, first + flags, 1)
    # Add separately for micro3d because CLEAR_VARS resets LOCAL_LDFLAGS.
    after_second = s.split(second, 1)[1]
    if not after_second.startswith(flags):
        s = s.replace(second, second + flags, 1)
android_mk.write_text(s, encoding="utf-8")

# Keep native libraries compressed/extracted on install.  This avoids relying
# on direct-from-APK mmap alignment for old third-party .so dependencies.
build_gradle = ROOT / "app" / "build.gradle"
s = build_gradle.read_text(encoding="utf-8")
legacy = """    packagingOptions {\n        jniLibs {\n            useLegacyPackaging true\n        }\n    }\n\n"""
if "useLegacyPackaging true" not in s:
    anchor = "    buildFeatures {\n        viewBinding true\n    }\n"
    if anchor not in s:
        raise SystemExit("build.gradle buildFeatures marker not found")
    s = s.replace(anchor, legacy + anchor, 1)
build_gradle.write_text(s, encoding="utf-8")

print("K70 safe standalone runtime patched")
