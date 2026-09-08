#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(sys.argv[1] if len(sys.argv) > 1 else "Avatar2-Offline/server-src")

SKIP_DIRS = {".git", "target", "build", "dist", ".idea", "logs"}
TEXT_EXTS = {
    ".java", ".xml", ".properties", ".yml", ".yaml", ".json", ".txt",
    ".md", ".sql", ".conf", ".ini", ".gradle", ".kts", ".sh", ".bat"
}

PATTERNS = [
    ("password assignment", re.compile(r"(?i)\b(pass(word)?|pwd)\b\s*[:=]\s*['\"]?[^\s'\"]{4,}")),
    ("token/secret assignment", re.compile(r"(?i)\b(token|secret|api[_-]?key|access[_-]?key)\b\s*[:=]\s*['\"]?[^\s'\"]{6,}")),
    ("database URL with credentials", re.compile(r"(?i)jdbc:[^\s]+(?:user|password)=[^\s&]+")),
    ("private key", re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----")),
]

ALLOW = [
    re.compile(r"(?i)(password|token|secret)\s*[:=]\s*(changeme|example|your_|<|\$\{|null|none|false|true)"),
]

findings = []
if not ROOT.exists():
    print(f"ERROR: source root not found: {ROOT}")
    sys.exit(2)

for path in ROOT.rglob("*"):
    if not path.is_file():
        continue
    if any(part in SKIP_DIRS for part in path.parts):
        continue
    if path.suffix.lower() not in TEXT_EXTS and path.name not in {"pom.xml", "Dockerfile"}:
        continue
    try:
        text = path.read_text(encoding="utf-8", errors="ignore")
    except OSError:
        continue
    for lineno, line in enumerate(text.splitlines(), 1):
        if any(a.search(line) for a in ALLOW):
            continue
        for label, pattern in PATTERNS:
            if pattern.search(line):
                findings.append((path, lineno, label, line.strip()[:240]))

if findings:
    print("Potential secrets found; review before pushing to a public repository:\n")
    for path, lineno, label, line in findings:
        print(f"{path}:{lineno}: {label}: {line}")
    sys.exit(1)

print(f"Secret scan passed: {ROOT}")
