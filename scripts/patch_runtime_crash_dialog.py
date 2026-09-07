#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(sys.argv[1]).resolve()

# Keep MIDlet init/start failures on-screen instead of letting the HandlerThread
# throw an uncaught RuntimeException that kills the whole Android process.
midlet_thread = ROOT / "app" / "src" / "main" / "java" / "javax" / "microedition" / "shell" / "MidletThread.java"
s = midlet_thread.read_text(encoding="utf-8")

marker = "\t@Override\n\tpublic boolean handleMessage(@NonNull Message msg) {\n"
helper = '''\tprivate void showStartupError(String phase, Throwable t) {\n\t\tLog.e(TAG, phase, t);\n\t\tstate = DESTROYED;\n\t\tfinal MicroActivity activity = ContextHolder.getActivity();\n\t\tif (activity != null) {\n\t\t\tfinal String message = phase + "\\n\\n" + Log.getStackTraceString(t);\n\t\t\tactivity.runOnUiThread(() -> activity.showErrorDialog(message));\n\t\t}\n\t}\n\n'''
if helper not in s:
    if marker not in s:
        raise SystemExit("MidletThread handleMessage marker not found")
    s = s.replace(marker, helper + marker, 1)

old = '\t\t\t\t} catch (Throwable t) {\n\t\t\t\t\tthrow new RuntimeException("Init midlet failed", t);\n\t\t\t\t}\n'
new = '\t\t\t\t} catch (Throwable t) {\n\t\t\t\t\tshowStartupError("KPAH init MIDlet failed", t);\n\t\t\t\t}\n'
if old in s:
    s = s.replace(old, new, 1)
elif 'showStartupError("KPAH init MIDlet failed", t);' not in s:
    raise SystemExit("MidletThread init catch pattern not found")

old = '\t\t\t\t} catch (Throwable t) {\n\t\t\t\t\tstate = DESTROYED;\n\t\t\t\t\tthrow new RuntimeException("Failed startApp", t);\n\t\t\t\t}\n'
new = '\t\t\t\t} catch (Throwable t) {\n\t\t\t\t\tshowStartupError("KPAH startApp failed", t);\n\t\t\t\t}\n'
if old in s:
    s = s.replace(old, new, 1)
elif 'showStartupError("KPAH startApp failed", t);' not in s:
    raise SystemExit("MidletThread start catch pattern not found")

midlet_thread.write_text(s, encoding="utf-8")

# Catch failures that happen even earlier in the standalone runtime/profile setup.
micro_activity = ROOT / "app" / "src" / "main" / "java" / "javax" / "microedition" / "shell" / "MicroActivity.java"
s = micro_activity.read_text(encoding="utf-8")
old = '''\t\tmicroLoader = new MicroLoader(this, appPath);\n\t\tif (!microLoader.init()) {\n\t\t\tConfig.startApp(this, appName, appPath, true, arguments);\n\t\t\tfinish();\n\t\t\treturn;\n\t\t}\n\t\tmicroLoader.applyConfiguration();\n'''
new = '''\t\ttry {\n\t\t\tmicroLoader = new MicroLoader(this, appPath);\n\t\t\tif (!microLoader.init()) {\n\t\t\t\tConfig.startApp(this, appName, appPath, true, arguments);\n\t\t\t\tfinish();\n\t\t\t\treturn;\n\t\t\t}\n\t\t\tmicroLoader.applyConfiguration();\n\t\t} catch (Throwable t) {\n\t\t\tLog.e("KPAH_STARTUP", "Runtime/profile initialization failed", t);\n\t\t\tshowErrorDialog("KPAH runtime init failed\\n\\n" + Log.getStackTraceString(t));\n\t\t\treturn;\n\t\t}\n'''
if old in s:
    s = s.replace(old, new, 1)
elif 'KPAH runtime init failed' not in s:
    raise SystemExit("MicroActivity runtime init block not found")

# MicroActivity already imports many Android classes; add Log import if needed.
if 'import android.util.Log;\n' not in s:
    anchor = 'import android.text.method.DigitsKeyListener;\n'
    if anchor not in s:
        raise SystemExit("MicroActivity import anchor not found")
    s = s.replace(anchor, anchor + 'import android.util.Log;\n', 1)

micro_activity.write_text(s, encoding="utf-8")
print("Runtime crash dialog diagnostics patched")
