#!/usr/bin/env python3
"""Drift check for the Event Script flow-grammar spec.

Asserts that the execution types documented in
docs/guides/event-script/event-script-flow.json exactly match the authoritative set in the
engine — CompileFlows.EXECUTION_TYPES — so the spec cannot drift from the compiler.

Exit 0 = in sync; exit 1 = drift (with details). Run from anywhere:
    python3 scripts/check-event-script-grammar.py [--root PATH]
"""
import argparse
import json
import re
import sys
from pathlib import Path

COMPILE_REL = "system/event-script-engine/src/main/java/com/accenture/automation/CompileFlows.java"
CATALOG_REL = "docs/guides/event-script/event-script-flow.json"


def code_execution_types(java_src: str) -> set[str]:
    # 1. map constant NAME -> "value"  (e.g. private static final String DECISION = "decision";)
    consts = dict(re.findall(r'static\s+final\s+String\s+([A-Z_]+)\s*=\s*"([^"]+)"', java_src))
    # 2. extract the EXECUTION_TYPES = { ... } array body
    m = re.search(r'EXECUTION_TYPES\s*=\s*\{([^}]*)\}', java_src, re.DOTALL)
    if not m:
        return set()
    members = [x.strip() for x in m.group(1).split(",") if x.strip()]
    # 3. resolve each member: a constant NAME -> its value, or a bare "literal"
    out: set[str] = set()
    for mem in members:
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

    java = root / COMPILE_REL
    catalog = root / CATALOG_REL
    if not java.is_file():
        print(f"ERROR: engine source not found: {java}")
        return 1
    if not catalog.is_file():
        print(f"ERROR: catalog not found: {catalog}")
        return 1

    code = code_execution_types(java.read_text(errors="ignore"))
    if not code:
        print(f"ERROR: could not extract EXECUTION_TYPES from {COMPILE_REL}")
        return 1
    spec = {e["type"] for e in json.loads(catalog.read_text()).get("execution_types", [])}

    missing_in_spec = code - spec          # engine has it, docs don't  -> a NEW execution type
    extra_in_spec = spec - code            # docs claim one the engine doesn't have
    if missing_in_spec or extra_in_spec:
        print(f"event-script-grammar: DRIFT — code={sorted(code)} spec={sorted(spec)}")
        for t in sorted(missing_in_spec):
            print(f"  - execution type '{t}' is in the engine but MISSING from the catalog")
        for t in sorted(extra_in_spec):
            print(f"  - execution type '{t}' is in the catalog but NOT in the engine")
        return 1
    print(f"event-script-grammar: {len(spec)} execution types {sorted(spec)} — in sync with engine")
    return 0


if __name__ == "__main__":
    sys.exit(main())
