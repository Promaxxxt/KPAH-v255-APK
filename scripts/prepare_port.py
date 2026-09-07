#!/usr/bin/env python3
import os
import re
import shutil
import sys
import zipfile
from pathlib import Path

ROOT = Path(sys.argv[1]).resolve()
GAME_JAR = Path(sys.argv[2]).resolve()

if not GAME_JAR.exists():
    raise SystemExit(f"Game JAR not found: {GAME_JAR}")

app = ROOT / "app"
libs = app / "libs"
libs.mkdir(parents=True, exist_ok=True)
shutil.copy2(GAME_JAR, libs / "KPAH.jar")

# Copy MIDlet manifest for the standalone flavor.
manifest_dir = app / "src" / "midlet" / "resources" / "MIDLET-META-INF"
manifest_dir.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(GAME_JAR, "r") as zf:
    mf = zf.read("META-INF/MANIFEST.MF")
(manifest_dir / "MANIFEST.MF").write_bytes(mf)

# Add the KPAH JAR as a dependency of the midlet flavor. Android's D8 will
# compile the game classes directly into the APK, so the app does not need to
# dex the JAR on the phone at install/runtime.
build_gradle = app / "build.gradle"
s = build_gradle.read_text(encoding="utf-8")
needle = "dependencies {\n"
insert = "dependencies {\n    midletImplementation files('libs/KPAH.jar')\n"
if "midletImplementation files('libs/KPAH.jar')" not in s:
    s = s.replace(needle, insert, 1)

# Avoid signature/manifest collisions from the embedded MIDlet JAR.
if "packaging {" not in s:
    anchor = "    buildFeatures {\n        viewBinding true\n    }\n"
    block = "    packaging {\n        resources {\n            excludes += ['META-INF/MANIFEST.MF', 'META-INF/*.SF', 'META-INF/*.RSA', 'META-INF/*.DSA']\n        }\n    }\n\n"
    s = s.replace(anchor, block + anchor, 1)
build_gradle.write_text(s, encoding="utf-8")

# Standalone startup profile: create it automatically instead of opening the
# emulator configuration screen on first run.
microloader = app / "src" / "main" / "java" / "javax" / "microedition" / "shell" / "MicroLoader.java"
s = microloader.read_text(encoding="utf-8")
old = "\t\tthis.params = ProfilesManager.loadConfig(config);\n\t\tif (params == null) {\n\t\t\treturn false;\n\t\t}\n"
new = "\t\tthis.params = ProfilesManager.loadConfig(config);\n\t\tif (params == null && !BuildConfig.FULL_EMULATOR) {\n\t\t\tif (!config.exists()) {\n\t\t\t\tconfig.mkdirs();\n\t\t\t}\n\t\t\tparams = new ProfileModel(config);\n\t\t\t// KPAH standalone defaults: wide landscape, full screen, translucent touch controls.\n\t\t\tparams.screenWidth = 960;\n\t\t\tparams.screenHeight = 432;\n\t\t\tparams.screenScaleToFit = true;\n\t\t\tparams.screenKeepAspectRatio = false;\n\t\t\tparams.screenScaleType = 2;\n\t\t\tparams.forceFullscreen = true;\n\t\t\tparams.showKeyboard = true;\n\t\t\tparams.touchInput = true;\n\t\t\tparams.vkType = VirtualKeyboard.TYPE_KPAH;\n\t\t\tparams.vkAlpha = 82;\n\t\t\tparams.vkForceOpacity = false;\n\t\t\tparams.fpsLimit = 60;\n\t\t\tProfilesManager.saveConfig(params);\n\t\t}\n\t\tif (params == null) {\n\t\t\treturn false;\n\t\t}\n"
if old not in s:
    raise SystemExit("MicroLoader init pattern not found")
s = s.replace(old, new, 1)
microloader.write_text(s, encoding="utf-8")

# Force landscape for the standalone port. This prevents phone-layout fallback
# and keeps the game canvas full width.
microactivity = app / "src" / "main" / "java" / "javax" / "microedition" / "shell" / "MicroActivity.java"
s = microactivity.read_text(encoding="utf-8")
old = "\t\tint orientation = microLoader.getOrientation();\n"
new = "\t\tint orientation = microLoader.getOrientation();\n\t\tif (!BuildConfig.FULL_EMULATOR) {\n\t\t\torientation = ORIENTATION_LANDSCAPE;\n\t\t}\n"
if old not in s:
    raise SystemExit("MicroActivity orientation pattern not found")
s = s.replace(old, new, 1)
microactivity.write_text(s, encoding="utf-8")

# Custom KPAH touch layout.
vk = app / "src" / "main" / "java" / "javax" / "microedition" / "lcdui" / "keyboard" / "VirtualKeyboard.java"
s = vk.read_text(encoding="utf-8")

# Add a dedicated layout id.
old = "\tprivate static final int TYPE_ARROWS = 6;\n"
new = "\tprivate static final int TYPE_ARROWS = 6;\n\tpublic static final int TYPE_KPAH = 7;\n"
if old not in s:
    raise SystemExit("VirtualKeyboard layout constant pattern not found")
s = s.replace(old, new, 1)

# Insert KPAH layout before TYPE_NUM_ARR/default.
marker = "\t\t\tcase TYPE_NUM_ARR:\n\t\t\tdefault:\n"
kpah = r'''			case TYPE_KPAH:
				Arrays.fill(keyScales, 1.0f);
				// Large directional cluster at lower-left: behaves like an 8-way joystick.
				keyScales[0] = 1.38f;
				keyScales[1] = 1.38f;
				// Numeric shortcuts slightly smaller; FIRE is larger.
				keyScales[6] = 0.90f;
				keyScales[7] = 0.90f;
				keyScales[8] = 1.35f;
				keyScales[9] = 1.35f;

				setSnap(KEY_DOWN_LEFT, SCREEN, RectSnap.INT_SOUTHWEST, true);
				setSnap(KEY_DOWN, KEY_DOWN_LEFT, RectSnap.EXT_EAST, true);
				setSnap(KEY_DOWN_RIGHT, KEY_DOWN, RectSnap.EXT_EAST, true);
				setSnap(KEY_LEFT, KEY_DOWN_LEFT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_RIGHT, KEY_DOWN_RIGHT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_UP_LEFT, KEY_LEFT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_UP, KEY_UP_LEFT, RectSnap.EXT_EAST, true);
				setSnap(KEY_UP_RIGHT, KEY_UP, RectSnap.EXT_EAST, true);

				// Attack button and quick actions on the right.
				setSnap(KEY_FIRE, SCREEN, RectSnap.INT_SOUTHEAST, true);
				setSnap(KEY_NUM0, KEY_FIRE, RectSnap.EXT_WEST, true);
				setSnap(KEY_NUM1, KEY_FIRE, RectSnap.EXT_NORTHWEST, true);
				setSnap(KEY_NUM3, KEY_FIRE, RectSnap.EXT_NORTH, true);
				setSnap(KEY_NUM7, KEY_NUM1, RectSnap.EXT_NORTH, true);
				setSnap(KEY_NUM9, KEY_NUM3, RectSnap.EXT_NORTH, true);

				// Cheat/menu keys stay on the left edge.
				setSnap(KEY_STAR, SCREEN, RectSnap.INT_NORTHWEST, true);
				setSnap(KEY_POUND, KEY_STAR, RectSnap.EXT_SOUTH, true);

				// Hide unused keypad buttons in the standalone game overlay.
				setSnap(KEY_NUM2, SCREEN, RectSnap.INT_NORTH, false);
				setSnap(KEY_NUM4, KEY_NUM2, RectSnap.EXT_WEST, false);
				setSnap(KEY_NUM5, KEY_NUM2, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_NUM6, KEY_NUM2, RectSnap.EXT_EAST, false);
				setSnap(KEY_NUM8, KEY_NUM5, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_SOFT_LEFT, KEY_UP_LEFT, RectSnap.EXT_NORTH, false);
				setSnap(KEY_SOFT_RIGHT, KEY_UP_RIGHT, RectSnap.EXT_NORTH, false);
				setSnap(KEY_A, SCREEN, RectSnap.INT_NORTHWEST, false);
				setSnap(KEY_B, SCREEN, RectSnap.INT_NORTHEAST, false);
				setSnap(KEY_C, KEY_A, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_D, KEY_B, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_MENU, KEY_UP, RectSnap.EXT_NORTH, false);
				break;
'''
if marker not in s:
    raise SystemExit("VirtualKeyboard resetLayout marker not found")
s = s.replace(marker, kpah + marker, 1)

vk.write_text(s, encoding="utf-8")

print("KPAH standalone port prepared")
print("Embedded JAR:", GAME_JAR)
