# Plan: AiContractProvider — composable AI-discovery app

- **Status:** DRAFT — awaiting Eric's ratification (this file is gitignored; an ADR is
  proposed only at implementation-PR time, per protocol)
- **Supersedes:** PR #284 (closed unmerged; essence extracted, implementation discarded)
- **Decision driver (Eric, 2026-08-21):** the contract capability must adopt the
  Composable design pattern — a standalone app under `system/`, functions + Event Script
  flows + `rest.yaml`, REST discovery on port 8999 (and/or CLI) — not a library module
  that core depends on, and not commands grafted into the playground.

---

## 1. Essence extracted from PR #284 (what we keep as *requirements*)

1. **Version-matched truth**: an AI agent asks a *running Mercury* for its operational
   contract and gets answers that provably match the runtime version — not stale web docs.
2. **Anchored claims**: each contract names the behavior classes that implement it, so a
   refactor surfaces drift deterministically (in CI, before release).
3. **Offline skill export**: a deterministic `mercury-platform` snapshot (SKILL.md +
   linked guide closure + machine catalogs + `security.json`) with per-file SHA-256 and a
   snapshot hash, for agents with no repo and no network. Two exports are byte-identical.
4. **Read-only remote surface**: discovery over HTTP never writes; export is a local
   operator action only.
5. **The real docs bug**: `rest.yaml` flow bindings require BOTH `service:
   'http.flow.adapter'` AND `flow:` — the old `flow:`-only example never registered
   (`RoutingEntry` skips entries without `service`). Fix the guides and pin the corrected
   example through the production parser.
6. **Anti-drift in CI**: the packaged closure and the catalog cannot silently diverge from
   `docs/` or the code.
7. **Advisory-only skill posture**: `security.json` + SKILL.md instruction boundaries
   (no network/shell/write authority claimed; packaged references are immutable vendor
   material). Content rewritten fresh — no text copied from the PR.

## 2. What we deliberately drop (and why)

| Dropped | Why |
| --- | --- |
| `ServiceLoader` provider framework + `platform-contracts` library | Inverted the dependency arrow: platform-core/event-script/minigraph all gained a new compile dependency. All modules release under one version anyway, so a static catalog verified at build time gives the same guarantee with zero framework changes. |
| Playground commands (`help mercury`, `list contracts`, `describe contract`) + webapp/catalog/help edits | The playground surface is mirrored on the Rust engine (shared webapp bundle, byte-identical catalog). Java-only commands there break cross-engine parity at the next sync. A dedicated discovery endpoint removes the entire problem. |
| `CodeSource`-based resource reading | Live-proven broken under Spring Boot nested-jar packaging (`help mercury` failed in the real packaged playground). All resource reads go through `getResourceAsStream`. |
| The 658-line TOCTOU exporter (fileKey reservation loops, hook seams, 6 telescoping constructors, hard-link publication) | Security posture beyond the threat model at the cost of readability. We keep the *properties* (§6) with a straightforward implementation. |
| Compile-time `MERCURY_VERSION` constant + hard-coded version in an IT | Two new release-sweep sites outside the 33-pom sed sweep. Replaced by manifest-derived versions (§7) — zero new sweep sites; the new pom rides the existing sweep (33 → 34 poms, note in the release runbook). |
| Committed `docs/specs/` convention, self-accepted ADR, "Orca" reference | House convention: plans live in gitignored `draft-design-specs/`; ADRs are Proposed until the maintainer's gate; no unexplained external references in public docs. |

## 3. The app

**Module:** `system/ai-contract-provider` · artifact `ai-contract-provider-<version>.jar`
**Main:** `AiContractProvider implements EntryPoint`, `@MainApplication`, launched by
`AutoStart.main(args)` — the standard app shape.

**Runtime dependencies:** `platform-core` (REST automation, event system) and
`event-script-engine` (flows). **`minigraph-playground-engine` at TEST scope only** —
needed to verify anchor classes, but a compile/runtime dependency would auto-preload the
playground's own `@PreLoad` functions into this app (jar on classpath = routes). At
runtime, minigraph anchors are strings; the module's tests resolve every anchor via
`Class.forName` so a rename still fails the reactor deterministically.

**Zero diffs to existing modules.** The dependency arrow points INTO the app.

**Modes (one binary, two entrypoints):**
- Default: REST discovery server on **port 8999** (`rest.server.port=8999`).
- `--export <dir>`: one-shot CLI export of the offline skill, then exit. The EntryPoint
  triggers the export *flow* via `FlowExecutor.request(...)` — orchestration stays in
  YAML even in CLI mode (`event-script-over-code`).

**Build section:** mirrored from an existing standalone app (kafka-standalone) — pinned
plugins, sources JAR at build time, jacoco. (The PR's module lacked all three.)

## 4. Composable wiring (functions → flows → rest.yaml)

Functions — `TypedLambdaFunction` + `@PreLoad`, app-local routes (no reserved-name changes):

| Route | Purpose |
| --- | --- |
| `v1.discovery.index` | JSON index: app name, `mercury_version`, endpoint list, contract ids — the one URL an agent needs first |
| `v1.contract.list` | catalog summary (id — summary, deterministic order) |
| `v1.contract.detail` | one contract: id, module, summary, anchors, references; unknown id → `AppException(404)` |
| `v1.skill.entrypoint` | `SKILL.md` text (`content-type: text/markdown`) |
| `v1.reference.reader` | serve one packaged reference file; the path must be an exact member of the packaged inventory (no traversal logic to get wrong — membership lookup only) |
| `v1.manifest.generator` | inventory + per-file SHA-256 + snapshot hash (computed once, cached) |
| `v1.skill.exporter` | offline snapshot writer — **not mapped in rest.yaml** |

Flows (one per endpoint + `export-skill.yml`), registered in `flows.yaml`. Each flow is a
legible statement of the endpoint: input mapping → task → output mapping (incl. the
content-type header). The app thereby doubles as a small reference implementation of the
pattern it documents.

`rest.yaml` (all GET, all flow-bound with `service: 'http.flow.adapter'` — eating the
corrected docs example's own cooking):

```
GET /api/discovery          -> flow: discovery-index
GET /api/contracts          -> flow: contract-list
GET /api/contracts/{id}     -> flow: contract-detail
GET /api/skill              -> flow: skill-entrypoint
GET /api/references/*       -> flow: reference-fetch      (wildcard suffix; confirm against rest grammar during build)
GET /api/manifest           -> flow: manifest
```

`/info` and `/health` come free from platform-core.

## 5. Contract catalog — configuration over code

`src/main/resources/contracts.yaml` — one file, four contracts (platform-core,
rest-automation, event-script, minigraph): `id`, `module`, `summary`,
`anchors` (fully-qualified class names), `references` (paths into the packaged closure).

Validation is test-time, deterministic, in this module:
- every anchor resolves (`Class.forName`; minigraph via test-scope dependency);
- every reference is a member of the packaged closure;
- ids/summaries match the same bounded character rules as the PR (no markdown injection).

No ServiceLoader, no build-id handshake — the reactor builds everything at one version and
the tests bind the catalog to the code.

## 6. Export — same properties, simple implementation

Kept properties: target is `<dir>/mercury-platform`; refuse if it already exists; reject a
symlinked destination; CREATE_NEW writes only; expand the one known mkdocs `--8<--`
fixture include and fail closed if any other include marker remains; validate that every
relative markdown link resolves inside the snapshot; write `manifest.json` LAST (its
absence marks an incomplete snapshot); per-file SHA-256 + snapshot hash over
`path\nhash\n` in sorted order; on failure, best-effort cleanup of the partial directory;
messages never contain exception text or absolute paths (but the underlying cause is
LOGGED — the OTLP lesson).

Dropped: fileKey reservation re-checks, hard-link publication, injectable failure seams.
Target size: a focused class ≤ ~200 lines plus direct tests.

Packaged closure (same as the PR — it is the guides' actual link closure):
`docs/index.md`, `docs/arch-decisions/ADR.md`, `docs/guides/**`,
`docs/test-reports/event-over-http-interop.md`, plus `SKILL.md`, `security.json`,
the REST fixture, and a checked-in `files.list`. The `llms.txt` link in `index.md` is
replaced by a local note (llms.txt stays excluded — it points off-snapshot).

`files.list` discipline (soft form of the PR's docs tax): a unit test in THIS module walks
`docs/guides` on the filesystem at test time and asserts equality with `files.list` — a
new guide page fails this module's tests with a message telling the author the one-line
fix. No separate python gate needed for the inventory; `docs.yml` optionally gains a thin
`check-ai-contract-provider.py` for PR-time feedback without a Maven run (decision D4).

## 7. Version matching — no new sweep sites

- The app's pom version rides the existing release sed sweep (pom count 33 → 34; runbook
  note only).
- Served payloads report `mercury_version` from the app jar's `Implementation-Version`
  (`Utility.getVersion()` pattern).
- Fail-closed startup check: the app compares its own version with platform-core's jar
  manifest version; mismatched assembly → log + refuse to start.
- A unit test pins pom version == served version, so CI catches drift without a
  hard-coded constant anywhere.

## 8. Cross-engine position

Java-only by design (minimalist-kafka precedent) — no Rust lock-step obligation because no
shared surface is touched (no webapp, no catalog, no help pages, no playground grammar).
The REST discovery API itself is engine-portable; if a Rust twin is ever wanted, the same
endpoints + flows are the contract.

## 9. Testing & gates (deterministic)

Module tests: catalog validation (anchors + references + bounds); files.list ↔ filesystem
equality; link-closure validation; endpoint e2e (TestBase boots the app; AsyncHttpClient
hits every endpoint incl. 404 and traversal-shaped reference paths); export determinism
(two exports byte-identical — the PR's best pin, kept), existing-target refusal
(mutation-proven), manifest re-verification (recompute all hashes); version-mismatch
fail-closed; startup smoke.

Pre-PR gates (house standard): full reactor `mvn clean install` exit 0; live boot of the
built jar (`java -jar`, port 8999) + curl of every endpoint — **explicitly under the real
packaging**, the lesson PR #284's tests missed; one real `--export` with independent hash
verification; `mkdocs build --strict`; Sonar hygiene pass (no unused imports, S3776 within
bounds).

## 10. Delivery

- **PR A (small, independent): the docs fix.** Correct the flow-binding example in
  `ai-developer-guide.md`, `event-script/ai-agent-guide.md`,
  `rest-automation/ai-agent-guide.md`; add the canonical fixture
  `rest-bindings.yaml` under **platform-core test resources** (the module that owns
  `RoutingEntry` proves its own docs example) + `RoutingEntryGuideFixtureTest`
  (singleton restored in `finally`); guides embed the fixture via the existing mkdocs
  snippet include (single source; `check_paths: true` + `--strict` already guard it);
  fixture gains `cors:`/`headers:` blocks so the worked CORS example survives — asserted
  by the test; CHANGELOG Unreleased.
- **PR B: the app** (module, catalog, flows, endpoints, export, tests, README,
  ADR-NNNN *Proposed*, CHANGELOG Unreleased, optional thin CI script). The app packages
  PR A's fixture into the closure by relative resource include.
- PR #284: closed unmerged by Eric; both PR bodies credit the original find and design
  motivation to its author.

## 11. Decision points (recommendation first)

- **D1 — Modes:** hybrid (REST server default, `--export` one-shot CLI) ✅ recommended;
  alternatives: REST-only or CLI-only.
- **D2 — PR A separate from PR B:** yes ✅ (small, independently correct, mergeable now).
- **D3 — Fixture include in docs:** keep the mkdocs snippet include (one source of
  truth) ✅; alternative: plain inline YAML (drifts again).
- **D4 — CI script:** rely on the module's own tests only ✅ (reactor already gates);
  optional extra: a thin python check in docs.yml for fast PR feedback on docs-only
  changes.
- **D5 — minigraph at test scope:** yes ✅ (no preload pollution; anchors still
  build-verified). Alternative: runtime dependency + config to silence the playground
  (fragile).
- **D6 — Who implements PR A:** good first-arc candidate for Eu Gene under this plan
  (their find; the fixture test is theirs in spirit) — Eric's call.
- **D7 — Closure scope:** same as the PR (guides' true link closure) ✅; trimming would
  break link validation or force link rewrites.
