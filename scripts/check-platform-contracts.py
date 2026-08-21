#!/usr/bin/env python3
"""Deterministic drift check for Mercury's packaged operational contract."""

from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

CONTRACT_ROOT = Path("contracts/src/main/resources/mercury/agent-skill")
CONTRACT_BUILD = Path("contracts/src/main/java/org/platformlambda/contracts/ContractBuild.java")
VERSIONED_POMS = (
    Path("pom.xml"),
    Path("contracts/pom.xml"),
    Path("system/platform-core/pom.xml"),
    Path("system/event-script-engine/pom.xml"),
    Path("system/minigraph-playground-engine/pom.xml"),
)
EXPECTED_PROVIDERS = {
    Path("system/platform-core/src/main/java/org/platformlambda/contracts/providers/PlatformCoreContractProvider.java"):
        "org.platformlambda.contracts.providers.PlatformCoreContractProvider",
    Path("system/event-script-engine/src/main/java/com/accenture/contracts/EventScriptContractProvider.java"):
        "com.accenture.contracts.EventScriptContractProvider",
    Path("system/minigraph-playground-engine/src/main/java/com/accenture/minigraph/contracts/MiniGraphContractProvider.java"):
        "com.accenture.minigraph.contracts.MiniGraphContractProvider",
}
EXPECTED_CONTRACT_COUNT = 4
PROVIDER_SERVICE = Path("META-INF/services/org.platformlambda.contracts.MercuryContractProvider")
PROVIDER_MODULES = (
    Path("system/platform-core/src/main/resources"),
    Path("system/event-script-engine/src/main/resources"),
    Path("system/minigraph-playground-engine/src/main/resources"),
)


def pom_version(path: Path) -> str:
    root = ET.parse(path).getroot()
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    value = root.findtext("m:version", namespaces=namespace)
    if not value:
        raise ValueError(f"missing project version in {path}")
    return value.strip()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=str(Path(__file__).resolve().parent.parent))
    args = parser.parse_args()
    root = Path(args.root)
    errors: list[str] = []

    try:
        versions = {str(path): pom_version(root / path) for path in VERSIONED_POMS}
    except (OSError, ET.ParseError, ValueError) as exc:
        print(f"platform-contracts: unable to read versions — {exc}")
        return 1
    if len(set(versions.values())) != 1:
        errors.append(f"Mercury POM versions differ: {versions}")
    mercury_version = next(iter(versions.values()))
    build_text = (root / CONTRACT_BUILD).read_text(encoding="utf-8")
    match = re.search(r'MERCURY_VERSION\s*=\s*"([^"]+)"', build_text)
    if not match or match.group(1) != mercury_version:
        errors.append("ContractBuild.MERCURY_VERSION does not match the Maven reactor")

    inventory_path = root / CONTRACT_ROOT / "files.list"
    inventory = [line.strip() for line in inventory_path.read_text(encoding="utf-8").splitlines()
                 if line.strip()]
    if inventory != sorted(set(inventory)):
        errors.append("files.list must contain unique paths in lexical order")
    if any("llms.txt" in path for path in inventory):
        errors.append("the offline contract must not package docs/llms.txt")
    expected_inventory = {
        "SKILL.md",
        "references/contract-index.md",
        "references/fixtures/rest-bindings.yaml",
        "security.json",
    }
    docs = root / "docs"
    expected_inventory.update(
        f"references/{path.relative_to(docs).as_posix()}"
        for path in (docs / "guides").rglob("*") if path.is_file()
    )
    expected_inventory.update({
        "references/index.md",
        "references/arch-decisions/ADR.md",
        "references/test-reports/event-over-http-interop.md",
    })
    if set(inventory) != expected_inventory:
        missing = sorted(expected_inventory - set(inventory))
        extra = sorted(set(inventory) - expected_inventory)
        errors.append(f"files.list does not match the offline guide closure; missing={missing}, extra={extra}")
    for path in inventory:
        local = root / CONTRACT_ROOT / path
        doc = root / "docs" / path.removeprefix("references/")
        if not local.is_file() and not (path.startswith("references/") and doc.is_file()):
            errors.append(f"missing packaged contract resource: {path}")

    expected_descriptors = []
    for provider_path, descriptor_name in EXPECTED_PROVIDERS.items():
        provider = root / provider_path
        if not provider.is_file():
            errors.append(f"missing contract provider: {provider_path}")
            continue
        expected_descriptors.append(descriptor_name)
    actual_descriptors = []
    for resources in PROVIDER_MODULES:
        descriptor = root / resources / PROVIDER_SERVICE
        if not descriptor.is_file() or not descriptor.read_text(encoding="utf-8").strip():
            errors.append(f"missing ServiceLoader provider descriptor: {descriptor.relative_to(root)}")
        else:
            actual_descriptors.append(descriptor.read_text(encoding="utf-8").strip())
    if sorted(actual_descriptors) != sorted(expected_descriptors):
        errors.append("ServiceLoader descriptors do not match the exact provider inventory")

    fixture = (root / CONTRACT_ROOT / "references/fixtures/rest-bindings.yaml").read_text(
        encoding="utf-8")
    if "service: 'http.flow.adapter'" not in fixture or "flow: 'agent-skill-rest-flow'" not in fixture:
        errors.append("canonical REST fixture must contain both the flow adapter and flow id")

    skill = (root / CONTRACT_ROOT / "SKILL.md").read_text(encoding="utf-8")
    if not skill.startswith("---\nname: mercury-platform\n"):
        errors.append("canonical SKILL.md frontmatter is missing or invalid")

    if errors:
        print("platform-contracts: DRIFT DETECTED")
        for error in errors:
            print(f"  - {error}")
        return 1
    print(f"platform-contracts: {mercury_version}, {len(inventory)} packaged files, "
          f"{EXPECTED_CONTRACT_COUNT} anchored contracts — in sync")
    return 0


if __name__ == "__main__":
    sys.exit(main())
