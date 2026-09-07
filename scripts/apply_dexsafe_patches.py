#!/usr/bin/env python3
import argparse
import base64
import zlib
import zipfile
from pathlib import Path

PATCHES = {
    "offline/fix/ArcherPoisonMonsterFix.class": ("offline__fix__ArcherPoisonMonsterFix.class.b64", False, 1705),
    "offline/fix/ArcherPoisonTickSync.class": ("offline__fix__ArcherPoisonTickSync.class.b64", False, 888),
    "offline/fix/FashionCharListFix.class": ("offline__fix__FashionCharListFix.class.b64", False, 2905),
    "offline/persistence/player/MemoryPlayerStore.class": ("offline__persistence__player__MemoryPlayerStore.class.b64", True, 9394),
    "offline/persistence/rms/RmsPlayerStore.class": ("offline__persistence__rms__RmsPlayerStore.class.b64", True, 11736),
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("source_jar")
    ap.add_argument("output_jar")
    ap.add_argument("--patch-dir", default="patches")
    args = ap.parse_args()

    src = Path(args.source_jar)
    dst = Path(args.output_jar)
    patch_dir = Path(args.patch_dir)

    replacements = {}
    for class_name, (filename, compressed, expected_size) in PATCHES.items():
        raw = base64.b64decode((patch_dir / filename).read_text(encoding="ascii").strip(), validate=True)
        if compressed:
            raw = zlib.decompress(raw)
        if not raw.startswith(b"\xca\xfe\xba\xbe"):
            raise SystemExit(f"Invalid class payload: {class_name}")
        if len(raw) != expected_size:
            raise SystemExit(f"Unexpected class size: {class_name}: {len(raw)} != {expected_size}")
        replacements[class_name] = raw

    replaced = set()
    with zipfile.ZipFile(src, "r") as zin, zipfile.ZipFile(dst, "w") as zout:
        for info in zin.infolist():
            data = replacements.get(info.filename)
            if data is not None:
                replaced.add(info.filename)
                zout.writestr(info, data)
            else:
                zout.writestr(info, zin.read(info.filename))

    missing = sorted(set(replacements) - replaced)
    if missing:
        raise SystemExit("Missing patch targets: " + ", ".join(missing))

    for name in sorted(replaced):
        print(f"patched {name} ({len(replacements[name])} bytes)")


if __name__ == "__main__":
    main()
