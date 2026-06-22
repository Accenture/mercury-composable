#!/usr/bin/env python3
"""Drift check for the documentation canon (terminology + structure conventions).

Guards the old->new documentation rewrite from silently regressing. The canon itself
is documented at docs/guides/documentation-conventions.md.

Checks (deterministic, stdlib only), over docs/:
  1. slug convention - every docs/guides/**/*.md filename is lowercase-kebab
                       (no ALL-CAPS legacy names: CHAPTER-*, APPENDIX-*, *-REFERENCE);
  2. no UTF-8 BOM    - no docs/**/*.md starts with a byte-order mark (it hides frontmatter);
  3. frontmatter     - every docs/guides/**/*.md begins with a YAML '---' block;
  4. at-a-glance     - every docs/guides/**/*.md has an 'At a glance' block;
  5. terminology     - no retired canon-violating phrases ("five distinct layers",
                       task/function "interchangeable", a stale "rewrite in progress" note).

Exit 0 = clean; exit 1 = drift (with details). Run from anywhere:
    python3 scripts/check-doc-canon.py [--root PATH]
"""
import argparse
import re
import sys
from pathlib import Path

GUIDES_REL = "docs/guides"
DOCS_REL = "docs"
CANON_DOC = "documentation-conventions.md"  # the rules page may quote anti-patterns; skip phrase scan

# Specific phrases the rewrite retired — a targeted safety net, not a general style linter.
BANNED = [
    ("runtime 'layers' (use 'request pipeline' / 'stages')",
        re.compile(r"distinct layers", re.I)),
    ("task/function called 'interchangeable' (one atom, named by role)",
        re.compile(r"(task|function)[^.\n]{0,40}interchangeab", re.I)),
    ("stale 'rewrite in progress' note",
        re.compile(r"rewrite in progress", re.I)),
]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=str(Path(__file__).resolve().parent.parent),
                    help="repo root (default: parent of scripts/)")
    args = ap.parse_args()
    root = Path(args.root)
    guides = root / GUIDES_REL
    docs = root / DOCS_REL
    errors: list[str] = []

    guide_md = sorted(guides.rglob("*.md"))

    # 1. slug convention (lowercase-kebab filenames in guides)
    for p in guide_md:
        if p.name != p.name.lower():
            errors.append(f"[slug] non-lowercase filename: {p.relative_to(root)}")

    # 2. no UTF-8 BOM in any docs markdown
    for p in sorted(docs.rglob("*.md")):
        if p.read_bytes()[:3] == b"\xef\xbb\xbf":
            errors.append(f"[bom] UTF-8 BOM at byte 0: {p.relative_to(root)}")

    # 3 + 4. frontmatter + At-a-glance for every guide doc
    for p in guide_md:
        text = p.read_text(encoding="utf-8").lstrip("﻿")
        if not text.startswith("---"):
            errors.append(f"[frontmatter] missing YAML frontmatter: {p.relative_to(root)}")
        if "at a glance" not in text.lower():
            errors.append(f"[at-a-glance] missing 'At a glance' block: {p.relative_to(root)}")

    # 5. retired terminology across docs markdown + llms.txt (skip the canon page itself)
    targets = sorted(docs.rglob("*.md"))
    llms = docs / "llms.txt"
    if llms.exists():
        targets.append(llms)
    for p in targets:
        if p.name == CANON_DOC:
            continue
        for i, line in enumerate(p.read_text(encoding="utf-8").splitlines(), 1):
            for label, rx in BANNED:
                if rx.search(line):
                    errors.append(f"[term] {label}: {p.relative_to(root)}:{i}")

    if errors:
        print("Documentation canon drift detected:\n")
        for e in errors:
            print("  - " + e)
        print(f"\n{len(errors)} issue(s). See docs/guides/documentation-conventions.md.")
        return 1
    print("Documentation canon: OK (slugs, frontmatter, at-a-glance, no BOM, no retired terms).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
