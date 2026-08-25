# Mercury AI contract provider

A standalone composable app that serves Mercury's **version-matched operational contract**
for AI discovery, and exports the offline `mercury-platform` Agent Skill. It is itself a
small reference implementation of the composable pattern: every endpoint is wired
`rest.yaml` → `http.flow.adapter` → Event Script flow → function.

The served contract is a **product of the runtime, not a description of it**:

- `mercury_version` is read from the platform-core dependency's own `pom.properties` —
  there is no version constant to bump at release time, and startup refuses a mixed
  assembly where platform-core and event-script-engine disagree.
- Each contract in [`contracts.yaml`](src/main/resources/contracts.yaml) names **behavior
  anchor classes**. The module's tests resolve every anchor with `Class.forName`
  (MiniGraph anchors come from a test-scope dependency), so a renamed or removed behavior
  class fails the reactor build — the catalog cannot drift from the code.
- The packaged guide closure is inventoried in `skill/files.list`, which a test asserts
  equals the real `docs/guides` tree — adding a guide page without updating the inventory
  fails this module's tests with a one-line fix.

## Run the discovery server

```bash
java -jar target/ai-contract-provider-<version>-exec.jar
```

Read-only endpoints on port 8999 (`rest.server.port`):

| Endpoint | Purpose |
| --- | --- |
| `GET /api/discovery` | the one URL an agent needs first: version, contract ids, endpoint map |
| `GET /api/contracts` | list the installed operational contracts |
| `GET /api/contracts/{id}` | one contract with behavior anchors and references |
| `GET /api/skill` | the `mercury-platform` skill entrypoint (SKILL.md) |
| `GET /api/references?path={reference-path}` | one packaged reference file (exact inventory members only) |
| `GET /api/manifest` | per-file SHA-256 and whole-snapshot hash |

## Export the offline Agent Skill

Filesystem export is deliberately **not** a REST endpoint — it is a local operator action:

```bash
java -jar target/ai-contract-provider-<version>-exec.jar --export /existing/output/directory
```

This writes `<directory>/mercury-platform/` — the exact snapshot the REST endpoints serve
(SKILL.md, security.json, the linked guide closure, the generated
`references/installed-contracts.md`) with `manifest.json` written **last** as the completion
marker. The exporter refuses an existing `mercury-platform/` target and never overwrites or
deletes a published snapshot; re-export is: verify the manifest, remove that exact directory,
export again. Two exports of the same build are byte-identical.

To verify a snapshot: recompute each file's SHA-256 against `manifest.json`, then hash the
UTF-8 sequence `path` + LF + file-hash + LF over all entries in sorted path order and compare
with `snapshot_sha256`.

Note: `--export` boots the app briefly; if a discovery server is already running on 8999,
pass `-Drest.server.port=<free-port>` for the export run.
