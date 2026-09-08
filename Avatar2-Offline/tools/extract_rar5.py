#!/usr/bin/env python3
"""Extract RAR5 archives using system libarchive via ctypes.

Useful on minimal Linux runners where libarchive.so exists but bsdtar/unrar is not installed.
"""
from __future__ import annotations

import ctypes
import sys
from pathlib import Path


def extract(src: Path, out: Path) -> int:
    lib = ctypes.CDLL("libarchive.so")
    archive_p = ctypes.c_void_p
    entry_p = ctypes.c_void_p
    lib.archive_read_new.restype = archive_p
    lib.archive_read_support_filter_all.argtypes = [archive_p]
    lib.archive_read_support_format_all.argtypes = [archive_p]
    lib.archive_read_open_filename.argtypes = [archive_p, ctypes.c_char_p, ctypes.c_size_t]
    lib.archive_read_open_filename.restype = ctypes.c_int
    lib.archive_read_next_header.argtypes = [archive_p, ctypes.POINTER(entry_p)]
    lib.archive_read_next_header.restype = ctypes.c_int
    lib.archive_entry_pathname.argtypes = [entry_p]
    lib.archive_entry_pathname.restype = ctypes.c_char_p
    lib.archive_entry_filetype.argtypes = [entry_p]
    lib.archive_entry_filetype.restype = ctypes.c_uint
    lib.archive_read_data.argtypes = [archive_p, ctypes.c_void_p, ctypes.c_size_t]
    lib.archive_read_data.restype = ctypes.c_ssize_t
    lib.archive_error_string.argtypes = [archive_p]
    lib.archive_error_string.restype = ctypes.c_char_p
    lib.archive_read_free.argtypes = [archive_p]

    out.mkdir(parents=True, exist_ok=True)
    out_root = out.resolve()
    archive = lib.archive_read_new()
    lib.archive_read_support_filter_all(archive)
    lib.archive_read_support_format_all(archive)
    result = lib.archive_read_open_filename(archive, str(src).encode(), 1024 * 1024)
    if result != 0:
        raise RuntimeError(lib.archive_error_string(archive).decode(errors="replace"))

    count = 0
    try:
        while True:
            entry = entry_p()
            result = lib.archive_read_next_header(archive, ctypes.byref(entry))
            if result == 1:  # ARCHIVE_EOF
                break
            if result < 0:
                raise RuntimeError(lib.archive_error_string(archive).decode(errors="replace"))
            raw = lib.archive_entry_pathname(entry)
            if not raw:
                continue
            relative = Path(raw.decode("utf-8", "replace").replace("\\", "/"))
            target = (out / relative).resolve()
            if target != out_root and out_root not in target.parents:
                continue
            if lib.archive_entry_filetype(entry) & 0o040000:
                target.mkdir(parents=True, exist_ok=True)
                continue
            target.parent.mkdir(parents=True, exist_ok=True)
            with target.open("wb") as fh:
                buf = ctypes.create_string_buffer(1024 * 1024)
                while True:
                    n = lib.archive_read_data(archive, buf, len(buf))
                    if n == 0:
                        break
                    if n < 0:
                        raise RuntimeError(lib.archive_error_string(archive).decode(errors="replace"))
                    fh.write(buf.raw[:n])
            count += 1
    finally:
        lib.archive_read_free(archive)
    return count


def main() -> int:
    if len(sys.argv) != 3:
        print("Usage: extract_rar5.py INPUT.rar OUTPUT_DIR", file=sys.stderr)
        return 2
    try:
        count = extract(Path(sys.argv[1]), Path(sys.argv[2]))
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    print(f"Extracted {count} files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
