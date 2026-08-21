#!/usr/bin/env python3
"""Drift check for the MiniGraph command-grammar spec.

Ties docs/guides/knowledge-graph/minigraph-commands.json to the engine's shipped help
resources and source, so the documented grammar cannot silently fall behind the code.

Checks (deterministic, stdlib only):
  1. every command's `help` file referenced by the catalog exists in the help dir;
  2. every skill's `help` file exists;
  3. every shipped `help graph-*.md` skill page maps to a skill route present in the catalog
     (catches a NEW skill added to the engine but missing from the spec);
  4. every catalog skill route is registered in the engine source (route="graph.*")
     (catches a renamed/removed route).
  5. the three read-only Mercury contract commands stay synchronized across the Java
     dispatcher, command catalog, shipped help, and web autocomplete source.

Exit 0 = in sync; exit 1 = drift (with details). Run from anywhere:
    python3 scripts/check-minigraph-grammar.py [--root PATH]
"""
import argparse
import json
import re
import sys
from pathlib import Path

ENGINE = "system/minigraph-playground-engine"
HELP_REL = f"{ENGINE}/src/main/resources/help"
CATALOG_REL = "docs/guides/knowledge-graph/minigraph-commands.json"
SUGGESTIONS_REL = f"{ENGINE}/webapp/src/utils/commandSuggestions.ts"
COMMAND_SERVICE_REL = f"{ENGINE}/src/main/java/com/accenture/minigraph/services/GraphCommandService.java"
CONTRACT_COMMANDS = {
    "help mercury": "help.md",
    "list contracts": "help list.md",
    "describe contract {contract-id}": "help describe.md",
}


def help_file_to_route(name: str) -> str:
    # "help graph-data-mapper.md" -> "graph.data.mapper"
    stem = name[len("help "):-len(".md")] if name.startswith("help ") and name.endswith(".md") else name
    return stem.replace("-", ".")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=str(Path(__file__).resolve().parent.parent),
                    help="repo root (default: parent of scripts/)")
    args = ap.parse_args()
    root = Path(args.root)

    help_dir = root / HELP_REL
    catalog_path = root / CATALOG_REL
    errors: list[str] = []

    if not catalog_path.is_file():
        print(f"ERROR: catalog not found: {catalog_path}")
        return 1
    if not help_dir.is_dir():
        print(f"ERROR: help dir not found: {help_dir}")
        return 1

    catalog = json.loads(catalog_path.read_text())
    help_files = {p.name for p in help_dir.glob("*.md")}

    # 1 + 2: referenced help files exist
    for item in catalog.get("commands", []) + catalog.get("skills", []):
        h = item.get("help")
        label = item.get("name") or item.get("route")
        if h and h not in help_files:
            errors.append(f"missing help file for '{label}': expected '{h}' in {HELP_REL}/")

    catalog_routes = {s["route"] for s in catalog.get("skills", [])}

    # 3: every shipped skill help page is in the catalog
    for hf in sorted(f for f in help_files if f.startswith("help graph-")):
        route = help_file_to_route(hf)
        if route not in catalog_routes:
            errors.append(f"skill '{route}' has a shipped help page ('{hf}') but is NOT in the catalog")

    # 4: every catalog skill route is registered in the engine source
    # match the route as a quoted literal anywhere in source — it appears either inline in
    # @PreLoad(route = "graph.x") or as a `static final String ROUTE = "graph.x"` constant.
    src_routes: set[str] = set()
    route_re = re.compile(r'"(graph\.[A-Za-z0-9_.]+)"')
    for java in (root / ENGINE / "src/main/java").rglob("*.java"):
        for m in route_re.finditer(java.read_text(errors="ignore")):
            src_routes.add(m.group(1))
    for route in sorted(catalog_routes):
        if route not in src_routes:
            errors.append(f"catalog skill route '{route}' not found in engine source (route=\"...\")")

    # 5: read-only Mercury contract command parity
    catalog_commands = {item.get("syntax"): item.get("help") for item in catalog.get("commands", [])}
    suggestions = (root / SUGGESTIONS_REL).read_text(errors="ignore")
    command_service = (root / COMMAND_SERVICE_REL).read_text(errors="ignore")
    for syntax, help_file in CONTRACT_COMMANDS.items():
        if syntax == "help mercury":
            help_syntax = catalog_commands.get(
                "help | help {command} | help {topic} | help {skill-topic} | help mercury", "")
            if help_syntax != "help.md":
                errors.append("catalog generic help command does not include 'help mercury'")
        elif catalog_commands.get(syntax) != help_file:
            errors.append(f"catalog command '{syntax}' must reference '{help_file}'")
        template = syntax.replace("{contract-id}", "{contract-id}")
        if f"template: '{template}'" not in suggestions:
            errors.append(f"web autocomplete is missing '{template}'")
    for token in ('equalsIgnoreCase("mercury")', 'equalsIgnoreCase("contracts")',
                  'equalsIgnoreCase("contract")', "MercuryContractCommands.ROUTE"):
        if token not in command_service:
            errors.append(f"GraphCommandService is missing contract dispatch token: {token}")

    n_cmd = len(catalog.get("commands", []))
    n_skill = len(catalog_routes)
    if errors:
        print(f"minigraph-grammar: {n_cmd} commands, {n_skill} skills — DRIFT DETECTED")
        for e in errors:
            print(f"  - {e}")
        return 1
    print(f"minigraph-grammar: {n_cmd} commands, {n_skill} skills, "
          f"{len([f for f in help_files if f.startswith('help graph-')])} skill help pages — in sync")
    return 0


if __name__ == "__main__":
    sys.exit(main())
