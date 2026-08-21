# Mercury operational contracts

`platform-contracts` is Mercury's first-class home for the version-matched operational contract.
It has no dependency on a Mercury runtime module. Runtime modules depend on this API directly and
register `MercuryContractProvider` implementations with ServiceLoader.

The artifact contains the canonical `mercury-platform` Agent Skill entrypoint and a fixed offline
inventory containing the full linked Mercury guide set. The local exporter expands checked MkDocs
includes, adds the providers and behavior anchors present on the assembled Mercury classpath,
hashes the snapshot, and atomically reserves one new `<trusted-root>/mercury-platform/` directory.
It writes `manifest.json` last as the publication marker. It never uses a model, fetches the
network, overwrites an existing snapshot, or deletes a published snapshot.

## Local export

Run the exporter with the assembled Mercury runtime classpath and one existing operator-controlled
root:

```bash
java -cp "$MERCURY_CLASSPATH" \
  org.platformlambda.contracts.AgentSkillExportCli /approved/existing/export-root
```

The classpath determines the installed provider inventory. A provider built against a different
`platform-contracts` identity fails closed before export.

## Verification

```bash
mvn -pl contracts,system/platform-core,system/event-script-engine,system/minigraph-playground-engine \
  -am verify
python3 scripts/check-minigraph-grammar.py
mkdocs build --strict
```

`manifest.json` is written only after every other file has been created and verified. It lists
every other regular file. Recompute each raw-file SHA-256 and then hash the
UTF-8 sequence `path`, LF, 64-character lowercase file hash, LF in sorted slash-separated path
order. The manifest excludes itself and verification rejects undeclared files, missing files,
links, and digest mismatches.

To revoke or replace a snapshot, first verify its snapshot hash, remove that exact
`mercury-platform/` directory as a separate operator action, and perform a clean export. The
exporter intentionally has no removal or overwrite mode.
