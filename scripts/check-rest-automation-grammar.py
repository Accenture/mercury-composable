#!/usr/bin/env python3
"""Drift check for the REST automation (rest.yaml) grammar spec.

Asserts that the HTTP methods documented in
docs/guides/rest-automation/rest-automation.json exactly match the authoritative set in the
engine — RoutingEntry.VALID_METHODS — so the spec cannot drift from the parser.

Exit 0 = in sync; exit 1 = drift. Run from anywhere:
    python3 scripts/check-rest-automation-grammar.py [--root PATH]
"""
import argparse
import json
import re
import sys
from pathlib import Path

ROUTING_REL = "system/platform-core/src/main/java/org/platformlambda/automation/config/RoutingEntry.java"
CATALOG_REL = "docs/guides/rest-automation/rest-automation.json"


def code_methods(java_src: str) -> set[str]:
    consts = dict(re.findall(r'static\s+final\s+String\s+([A-Z_]+)\s*=\s*"([^"]+)"', java_src))
    m = re.search(r'VALID_METHODS\s*=\s*\{([^}]*)\}', java_src, re.DOTALL)
    if not m:
        return set()
    out: set[str] = set()
    for mem in (x.strip() for x in m.group(1).split(",") if x.strip()):
        lit = re.fullmatch(r'"([^"]+)"', mem)
        if lit:
            out.add(lit.group(1))
        elif mem in consts:
            out.add(consts[mem])
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=str(Path(__file__).resolve().parent.parent))
    args = ap.parse_args()
    root = Path(args.root)

    java = root / ROUTING_REL
    catalog = root / CATALOG_REL
    if not java.is_file():
        print(f"ERROR: parser source not found: {java}")
        return 1
    if not catalog.is_file():
        print(f"ERROR: catalog not found: {catalog}")
        return 1

    code = code_methods(java.read_text(errors="ignore"))
    if not code:
        print(f"ERROR: could not extract VALID_METHODS from {ROUTING_REL}")
        return 1
    spec = set(json.loads(catalog.read_text()).get("http_methods", []))

    missing = code - spec
    extra = spec - code
    if missing or extra:
        print(f"rest-automation-grammar: DRIFT — code={sorted(code)} spec={sorted(spec)}")
        for t in sorted(missing):
            print(f"  - method '{t}' is accepted by the parser but MISSING from the catalog")
        for t in sorted(extra):
            print(f"  - method '{t}' is in the catalog but NOT accepted by the parser")
        return 1
    print(f"rest-automation-grammar: {len(spec)} HTTP methods {sorted(spec)} — in sync with parser")
    return 0


if __name__ == "__main__":
    sys.exit(main())
