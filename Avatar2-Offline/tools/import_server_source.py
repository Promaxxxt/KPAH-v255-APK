#!/usr/bin/env python3
"""Import a minimal, public-safe Avatar2 server source tree.

Usage:
  python3 tools/import_server_source.py /path/to/extracted/server
  python3 tools/import_server_source.py /path/to/server.rar

For RAR input an external extractor (unrar, 7z/7zz, or bsdtar) must be
installed. Raw databases, local configuration and build/IDE output are never
copied. The import is staged and secret-scanned before replacing server-src.
"""
from __future__ import annotations

import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
PROJECT = HERE.parent
DEFAULT_DEST = PROJECT / "server-src"
SECRET_SCAN = HERE / "secret_scan.py"

BLOCKED_NAMES = {
    "config.properties",
    "database.properties",
    "application-local.properties",
    "application-local.yml",
    "application-local.yaml",
    ".env",
}
BLOCKED_SUFFIXES = {".sql", ".rar", ".7z", ".zip", ".jar", ".class", ".log"}
SKIP_DIRS = {".git", ".idea", ".vscode", "target", "build", "dist", "logs", "log"}
SOURCE_SUFFIXES = {".java"}
ROOT_FILES = {"pom.xml"}


def is_allowed(relative: Path) -> bool:
    if any(part in SKIP_DIRS for part in relative.parts):
        return False
    if relative.name in BLOCKED_NAMES or relative.suffix.lower() in BLOCKED_SUFFIXES:
        return False
    if len(relative.parts) == 1:
        return relative.name in ROOT_FILES
    if relative.parts[0] == "src":
        return relative.suffix.lower() in SOURCE_SUFFIXES
    return False


def find_server_root(root: Path) -> Path:
    candidates = [root, root / "server"]
    candidates.extend(p.parent for p in root.rglob("pom.xml"))
    for candidate in candidates:
        if (candidate / "pom.xml").is_file() and (candidate / "src").is_dir():
            return candidate
    raise RuntimeError(f"Could not find server root containing pom.xml + src under {root}")


def extract_archive(archive: Path, out_dir: Path) -> None:
    extractors = [
        ("unrar", ["x", "-o+", str(archive), str(out_dir) + "/"]),
        ("7zz", ["x", "-y", f"-o{out_dir}", str(archive)]),
        ("7z", ["x", "-y", f"-o{out_dir}", str(archive)]),
        ("bsdtar", ["-xf", str(archive), "-C", str(out_dir)]),
    ]
    for executable, args in extractors:
        path = shutil.which(executable)
        if path:
            subprocess.run([path, *args], check=True)
            return
    raise RuntimeError("RAR input needs one of: unrar, 7zz, 7z, bsdtar")


def copy_allowed(server_root: Path, stage: Path) -> tuple[int, int]:
    copied = skipped = 0
    for source in server_root.rglob("*"):
        if not source.is_file():
            continue
        relative = source.relative_to(server_root)
        if not is_allowed(relative):
            skipped += 1
            continue
        target = stage / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)
        copied += 1
    return copied, skipped


def scan(stage: Path) -> None:
    subprocess.run([sys.executable, str(SECRET_SCAN), str(stage)], check=True)


def import_source(input_path: Path, destination: Path = DEFAULT_DEST) -> tuple[int, int]:
    input_path = input_path.resolve()
    if not input_path.exists():
        raise FileNotFoundError(input_path)

    with tempfile.TemporaryDirectory(prefix="avatar2-server-import-") as tmp:
        tmp_path = Path(tmp)
        if input_path.is_file():
            extracted = tmp_path / "extracted"
            extracted.mkdir()
            extract_archive(input_path, extracted)
            root = find_server_root(extracted)
        else:
            root = find_server_root(input_path)

        stage = tmp_path / "server-src"
        stage.mkdir()
        copied, skipped = copy_allowed(root, stage)
        if copied == 0 or not (stage / "pom.xml").exists():
            raise RuntimeError("Import produced no usable Maven source tree")
        scan(stage)

        if destination.exists():
            shutil.rmtree(destination)
        shutil.copytree(stage, destination)
        return copied, skipped


def main() -> int:
    if len(sys.argv) not in {2, 3}:
        print("Usage: import_server_source.py INPUT [DESTINATION]", file=sys.stderr)
        return 2
    destination = Path(sys.argv[2]) if len(sys.argv) == 3 else DEFAULT_DEST
    try:
        copied, skipped = import_source(Path(sys.argv[1]), destination)
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    print(f"Imported {copied} files; skipped {skipped}; destination={destination}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
