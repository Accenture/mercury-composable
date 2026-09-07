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
                       task/function "interchangeable", a stale "rewrite in progress" note);
  7. llms.txt cover  - every guide page and DSL catalog is listed in docs/llms.txt (the canon
                       calls llms.txt the machine-readable site map "kept current"; nothing
                       enforced it, so pages shipped invisible to the guide-first lookup rule);
  8. claims fixture  - every entry in docs/guides/claims-registry.json keeps its normative
                       sentence present on one of its pages AND its engine-test pin resolvable
                       (module::Class#method exists). The AI grammar coverage study found all
                       doc drift in ungated prose - this gate extends drift testing from shape
                       to behavior (ADR-0023).

Exit 0 = clean; exit 1 = drift (with details). Run from anywhere:
    python3 scripts/check-doc-canon.py [--root PATH]
"""
import argparse
import json
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

    # 6. no case-only redirects in mkdocs.yml — their stubs clobber the real page on a
    #    case-insensitive filesystem (macOS / 'mkdocs serve'); the old URL differs only by case.
    mkdocs = root / "mkdocs.yml"
    if mkdocs.exists():
        redirect_rx = re.compile(r"^\s*'([^']+)'\s*:\s*'([^']+)'\s*$")
        for i, line in enumerate(mkdocs.read_text(encoding="utf-8").splitlines(), 1):
            m = redirect_rx.match(line)
            if m and m.group(1).lower() == m.group(2).lower():
                errors.append(f"[redirect] case-only redirect (clobbers page on macOS): "
                              f"mkdocs.yml:{i}  {m.group(1)} -> {m.group(2)}")

    # 7. llms.txt coverage - the canon (documentation-conventions.md) calls llms.txt the
    #    machine-readable site map, kept current. An unlisted page is unreachable by the
    #    guide-first lookup rule, so an agent falls back to reading engine source.
    if llms.exists():
        listed = llms.read_text(encoding="utf-8")
        for p in sorted(list(guides.rglob("*.md")) + list(guides.rglob("*.json"))):
            rel = p.relative_to(docs).as_posix()
            slug = rel.removesuffix(".md").removesuffix("/index")
            if f"/{slug}/" not in listed and f"/{slug}" not in listed:
                errors.append(f"[llms] not listed in docs/llms.txt: {p.relative_to(root)}")

    # 8. claims-fixture gate - registered prose behavior claims stay pinned on both sides:
    #    the normative sentence must appear (whitespace-normalized) on one of the claim's
    #    pages, and the named engine test must still exist in that module's test tree.
    registry = guides / "claims-registry.json"
    if registry.exists():
        def norm(s: str) -> str:
            return re.sub(r"\s+", " ", s).strip().lower()
        try:
            claims = json.loads(registry.read_text(encoding="utf-8")).get("claims", [])
        except json.JSONDecodeError as e:
            claims = []
            errors.append(f"[claims] registry is not valid JSON: {e}")
        for c in claims:
            if not isinstance(c, dict):
                errors.append(f"[claims] non-object entry in claims list: {str(c)[:60]}")
                continue
            cid = c.get("id", "?")
            pages = [root / p for p in c.get("pages", []) if isinstance(p, str)]
            for p in pages:
                if not p.exists():
                    errors.append(f"[claims] {cid}: page not found: {p}")
            quote = norm(c.get("quote", "") or "")
            if not quote:
                errors.append(f"[claims] {cid}: empty quote")
            elif not any(quote in norm(p.read_text(encoding="utf-8"))
                         for p in pages if p.exists()):
                errors.append(f"[claims] {cid}: pinned sentence not found on any of its "
                              f"pages: \"{c.get('quote', '')[:80]}\"")
            test = c.get("test", "") or ""
            if test:
                ref = re.fullmatch(r"([^:#]+)::([^:#]+)#([^:#(]+)", test)
                if not ref:
                    errors.append(f"[claims] {cid}: malformed test ref "
                                  f"(want module::Class#method): {test}")
                else:
                    module, clazz, method = ref.groups()
                    tdir = root / module / "src" / "test" / "java"
                    hits = list(tdir.rglob(f"{clazz}.java")) if tdir.exists() else []
                    # match a method DEFINITION, not a call site or comment
                    def_rx = re.compile(
                        rf"\b(?:void|public|protected|private|static)\s+"
                        rf"(?:[\w<>\[\]]+\s+)?{re.escape(method)}\s*\(")
                    if not hits:
                        errors.append(f"[claims] {cid}: test class not found: {test}")
                    elif not any(def_rx.search(h.read_text(encoding="utf-8"))
                                 for h in hits):
                        errors.append(f"[claims] {cid}: test method '{method}' not defined "
                                      f"in {clazz}.java")

    if errors:
        print("Documentation canon drift detected:\n")
        for e in errors:
            print("  - " + e)
        print(f"\n{len(errors)} issue(s). See docs/guides/documentation-conventions.md.")
        return 1
    print("Documentation canon: OK (slugs, frontmatter, at-a-glance, no BOM, no retired terms, "
          "llms.txt coverage, claims fixtures).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
