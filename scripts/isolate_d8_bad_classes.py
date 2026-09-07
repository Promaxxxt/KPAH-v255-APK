#!/usr/bin/env python3
import argparse
import os
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path

TARGET = "Undefined value encountered during compilation"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("jar")
    ap.add_argument("d8")
    ap.add_argument("--output", default="D8_BAD_CLASSES.txt")
    args = ap.parse_args()

    jar = Path(args.jar).resolve()
    d8 = Path(args.d8).resolve()
    report = Path(args.output).resolve()
    if not jar.is_file():
        raise SystemExit(f"JAR not found: {jar}")
    if not d8.is_file():
        raise SystemExit(f"D8 not found: {d8}")

    with zipfile.ZipFile(jar, "r") as zf:
        class_names = sorted(n for n in zf.namelist() if n.endswith(".class"))
        class_bytes = {n: zf.read(n) for n in class_names}

    work = Path(tempfile.mkdtemp(prefix="kpah-d8-isolate-"))
    tests = 0
    other_errors = []
    bad = []

    def test_subset(names):
        nonlocal tests
        tests += 1
        tag = f"t{tests:04d}"
        inp = work / f"{tag}.jar"
        out = work / f"{tag}.zip"
        with zipfile.ZipFile(inp, "w", compression=zipfile.ZIP_STORED) as z:
            for name in names:
                z.writestr(name, class_bytes[name])
        cmd = [str(d8), "--intermediate", "--min-api", "21", "--output", str(out), str(inp)]
        p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, errors="replace")
        log = p.stdout or ""
        target_fail = TARGET in log
        if p.returncode != 0 and not target_fail:
            other_errors.append((list(names), log[-4000:]))
        try:
            inp.unlink()
        except OSError:
            pass
        try:
            out.unlink()
        except OSError:
            pass
        return p.returncode == 0, target_fail, log

    # Verify the standalone D8 invocation reproduces the same target failure.
    ok, target_fail, whole_log = test_subset(class_names)
    if ok:
        report.write_text(
            "Standalone D8 accepted the full class set.\n"
            "The Gradle failure depends on its dependency/desugaring context rather than a single malformed class.\n",
            encoding="utf-8",
        )
        print(report.read_text(encoding="utf-8"))
        shutil.rmtree(work, ignore_errors=True)
        return 0
    if not target_fail:
        report.write_text(
            "Standalone D8 did not reproduce the target Undefined-value failure.\n\n"
            + whole_log[-12000:],
            encoding="utf-8",
        )
        print(report.read_text(encoding="utf-8"))
        shutil.rmtree(work, ignore_errors=True)
        return 0

    print(f"Reproduced target D8 failure across {len(class_names)} classes; bisecting...")

    def bisect(names, depth=0):
        if not names:
            return
        if len(names) == 1:
            ok1, bad1, log1 = test_subset(names)
            if bad1:
                bad.append((names[0], log1[-3000:]))
                print("BAD", names[0])
            return
        mid = len(names) // 2
        for half in (names[:mid], names[mid:]):
            okh, badh, logh = test_subset(half)
            if badh:
                bisect(half, depth + 1)

    bisect(class_names)

    lines = [
        "KPAH D8 malformed-class isolation",
        f"Total classes: {len(class_names)}",
        f"D8 subset tests: {tests}",
        f"Bad classes: {len(bad)}",
        "",
    ]
    for name, log in bad:
        lines.append(f"BAD_CLASS: {name}")
        lines.append(log)
        lines.append("=" * 72)
    if not bad:
        lines += [
            "No individual class reproduced the error.",
            "The failure likely requires an interaction/desugaring context between multiple classes.",
        ]
    if other_errors:
        lines += ["", f"Non-target D8 subset errors observed: {len(other_errors)}"]
        for names, log in other_errors[:5]:
            lines.append(f"Subset size {len(names)}: {names[0]} ... {names[-1]}")
            lines.append(log)

    report.write_text("\n".join(lines), encoding="utf-8")
    print(report.read_text(encoding="utf-8"))
    shutil.rmtree(work, ignore_errors=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
