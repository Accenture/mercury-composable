# Spec: Runtime-served Mercury operational contract

- **Status:** Shipped
- **Owner:** Mercury maintainers
- **Plan:** [`plan.md`](plan.md)
- **Constrained by:** ADR-0015
- **Brief:** none
- **Discovery:** none
- **Contract:** `contracts/`
- **Shape:** integration

> **Spec contract:** this document defines what "done" means. The implementing
> pull request must match this spec, or update it with maintainer approval.

## Objective

Mercury publishes one authoritative, version-matched operational contract from its
runtime. A first-class `contracts/` module packages the complete offline Agent Skill,
and one product command service exposes that contract using Mercury's existing command
vocabulary. Installed runtime modules register explicit providers that name the behavior
they implement. A deterministic Java generator exports one `mercury-platform` skill with
all required references; it does not use an LLM or the network.

## Boundaries

### Always do

- Keep the canonical skill entrypoint, reference inventory, and generation code under
  `contracts/`; package them as the versioned `platform-contracts` Maven artifact.
- Use one product command service. Installed modules contribute contract providers rather
  than exposing independent command services.
- Make each provider identify its Mercury module, contract ids, and behavior anchors with
  compile-time Java class references. Do not infer ownership by scanning source text.
- Preserve Mercury vocabulary and the MiniGraph command grammar: `help mercury`,
  `list contracts`, and `describe contract <id>`. Keep export out of the remotely
  reachable MiniGraph companion path; operators invoke the local Java exporter.
- Serve the same packaged contract that the Java generator exports. Generated output is a
  complete offline skill snapshot with a short `SKILL.md`, progressive references, the
  full linked Mercury guide set, the Mercury release, installed provider inventory, and
  behavior anchors. MkDocs includes are expanded deterministically during rendering.
- Keep generation deterministic, local, and pure Java. The exporter accepts a separately
  trusted, existing allowed root, atomically reserves only its fixed `mercury-platform/`
  child, creates content with no-follow/create-new semantics, and writes `manifest.json`
  last as the publication commit marker. It never replaces an existing path.
- Register the command service as a private, development-only Platform Core service. The
  pure Java registry and exporter remain directly callable without enabling the service.
- Keep the corrected REST flow binding contract: `service: http.flow.adapter` and `flow:`
  are both required.

### Ask first

- Add a second product command service, split the one platform skill, publish a release
  asset, or change the provider/command compatibility contract.
- Make the runtime command surface available outside the existing development-only
  MiniGraph environment.
- Add model-generated content, network retrieval, or an independently versioned module
  to the required export path.

### Never do

- Generate instructions with an LLM or treat `docs/llms.txt` as a normative contract.
- Rediscover behavior ownership from class names, source strings, documentation links,
  or repository layout.
- Introduce a live AI operator, command confirmation protocol, transcript store, six-skill
  projection, or repository-wide Markdown fence classifier.
- Let a generated skill claim a module or behavior anchor that was not registered by the
  running Mercury classpath.
- Follow symlinks, accept path traversal, reveal internal paths in public errors, or
  overwrite an existing export destination.
- Let MiniGraph's companion endpoint perform a filesystem export.

## Testing Strategy

- **Provider and command registry: TDD.** Verify deterministic ordering, unique contract
  ids, duplicate rejection, explicit class anchors, and ServiceLoader discovery.
- **Generator: TDD plus manual invocation.** Render twice from the same provider set and
  compare files and bytes; reject an existing or concurrently reserved target and
  path/symlink escapes; validate the generated skill, expanded includes, security metadata,
  and internal references.
- **Runtime integration: focused Maven tests.** Load the private platform service in a
  development environment and prove it is absent in production; drive the three read-only
  commands through MiniGraph's real command service. Invoke export through the local Java
  entrypoint and assert the snapshot.
- **Documentation/build wiring: goal-based checks.** Strict MkDocs and the Maven reactor
  prove the corrected REST example and packaged contract remain buildable.

## Acceptance Criteria

- [x] **AC1.** `contracts/` is a Maven module named `platform-contracts`, versioned with
  Mercury, containing the canonical `SKILL.md`, a fixed reference inventory, the provider
  API, product command registry, and deterministic Java skill generator. Its packaged JAR
  contains the full linked guide set, every indexed reference, and no network-derived
  content; `docs/llms.txt` remains excluded.
- [x] **AC2.** Exactly one product command service is registered in development mode.
  Providers for
  `platform-core`/REST automation, Event Script, and MiniGraph are discovered with
  ServiceLoader, use unique stable contract ids, and expose compile-time class anchors for
  the behavior they implement. `platform-contracts` has no Mercury runtime-module
  dependencies; Platform Core and every downstream provider depend on it directly, so the
  dependency graph cannot cycle.
- [x] **AC3.** MiniGraph's existing command service accepts `help mercury`,
  `list contracts`, and `describe contract <id>` and delegates them to the private,
  development-only product service. Production mode does not register that service.
  MiniGraph does not expose skill export through its command or companion surface, and
  existing MiniGraph commands keep their behavior.
- [x] **AC4.** The Java generator exports exactly one `mercury-platform/` skill to a new
  destination beneath a caller-supplied trusted root. The root must already exist and pass
  `toRealPath`; the exporter rejects a symbolic-link root, a pre-existing destination, and
  every symbolic-link or indeterminate component. It atomically reserves the final child,
  creates files with no-follow/create-new semantics, verifies all non-manifest content, and
  atomically creates `manifest.json` last from a verified same-filesystem staging file using
  a no-replace hard link as the publication commit marker. If the filesystem does not support
  that atomic no-replace publication, export fails and cleans the reserved incomplete directory.
  The snapshot contains a
  valid `SKILL.md`, all indexed `references/`, and a deterministic runtime inventory naming the Mercury version,
  installed providers, contract ids, behavior anchors, canonical file inventory, per-file
  SHA-256 hashes, and a snapshot hash. A repeated render is byte-identical.
- [x] **AC5.** Runtime `help mercury` and generated `SKILL.md` come from the same packaged
  resource. Every indexed reference exists in the JAR, every MkDocs include is expanded,
  and every relative Markdown link in the exported snapshot resolves within it. External
  links remain informational and are never required for offline operation.
- [x] **AC6.** The old six-skill manifest/scanner, CODEOWNERS bootstrap, live MiniGraph
  operator design, transaction machinery, and their partial implementation are absent.
  The original REST flow-binding inconsistency remains fixed.
- [x] **AC7.** Focused module tests, generator tests, an end-to-end command/export drive,
  strict MkDocs, and available repository linters pass. No production network, secret,
  GitHub mutation, or model call is required.
- [x] **AC8.** Every provider embeds the exact contract API version/build identity and the
  registry fails closed before serving or exporting if any provider differs. Public command
  and export failures use bounded stable codes and messages without exception text or
  absolute paths; a failed export cleans up only the final directory it successfully
  reserved, and an incomplete directory is identifiable by the absence of `manifest.json`.
- [x] **AC9.** The generated skill is advisory-only: it declares no required shell,
  network, or write authority; treats packaged references as immutable vendor material;
  preserves user authority and task scope; and ships a deterministic `security.json` with
  those constraints. The manifest documents verification, removal, and clean re-export by
  snapshot hash.

## Canonical contract inventory

The packaged snapshot has an explicit source map; adding or removing an entry is a
reviewed contract change.

| Contract id | Provider | Behavior anchors | Packaged references |
|---|---|---|---|
| `platform-core` | Platform Core | `AppStarter`, `PostOffice`, `EventEnvelope` | `guides/ai-developer-guide.md`; `guides/event-driven/ai-agent-guide.md` |
| `rest-automation` | Platform Core | `RoutingEntry` | `guides/rest-automation/ai-agent-guide.md`; `guides/rest-automation/rest-grammar.md`; `guides/rest-automation/rest-automation.json`; `fixtures/rest-bindings.yaml` |
| `event-script` | Event Script | `CompileFlows` | `guides/event-script/ai-agent-guide.md`; `guides/event-script/flow-grammar.md`; `guides/event-script/event-script-flow.json` |
| `minigraph` | MiniGraph | `GraphCommandService`, `CompileGraph`, `GraphModelValidator` | `guides/knowledge-graph/ai-agent-guide.md`; `guides/knowledge-graph/command-reference.md`; `guides/knowledge-graph/minigraph-commands.json`; `guides/knowledge-graph/skills-reference.md` |

`docs/llms.txt` remains a public-site discovery map and is not packaged in the offline
skill. Its external URLs and retrieval directions are outside the immutable snapshot.
Concrete resource paths come only from the contract artifact's checked local inventory.

## Compatibility and failure contract

- The contract API publishes one build identity from the `platform-contracts` artifact.
  Every ServiceLoader provider declares that same identity. An absent, blank, duplicate,
  or mismatched identity is `CONTRACT_VERSION_MISMATCH`; no partial registry is served.
- Public failures are limited to `INVALID_COMMAND`, `UNKNOWN_CONTRACT`,
  `CONTRACT_VERSION_MISMATCH`, `INVALID_EXPORT_ROOT`, `EXPORT_EXISTS`,
  `EXPORT_INTEGRITY_FAILED`, `EXPORT_IO_FAILED`, and `EXPORT_CLEANUP_FAILED`. Messages are
  fixed and never include raw exception text, session identifiers, or filesystem paths. A
  cleanup failure is attached without replacing the primary export failure.
- Export takes one existing allowed root, not a caller-composed destination. It has no
  variable path segment: output is `<allowed-root>/mercury-platform/`. The CLI accepts one
  root argument, performs one export, and exits; there is no remote, repeated, or
  agent-reachable export service.
- A failed export removes only the fixed final directory that invocation successfully
  reserved, and only before `manifest.json` has been written. Successful removal or
  revocation is an operator action: verify the snapshot hash, remove that exact
  `mercury-platform/` directory outside the exporter, then run a clean export. The exporter
  itself never deletes a published snapshot.
- `manifest.json` lists every other exported regular file and no directory entries. For
  each file it hashes the raw bytes with SHA-256 and records a lowercase hexadecimal
  digest. Entries are ordered by their slash-separated UTF-8 relative path. The snapshot
  hash is SHA-256 over the UTF-8 bytes of each `path`, one LF byte, its 64-character file
  digest, and one LF byte, concatenated in that order. `manifest.json` is excluded from its
  own hash; verification rejects an undeclared file, missing file, non-regular file,
  symbolic link, digest mismatch, or snapshot mismatch.

## Assumptions

- The caller-supplied root is trusted in the filesystem sense: untrusted processes cannot
  rename or replace its namespace while export runs. The exporter still checks root/target
  file identity around path operations, refuses pre-existing or symbolic-link components,
  and safely arbitrates concurrent Mercury exporters; it does not claim to sandbox a hostile
  process that already has write authority over the trusted root.
- Mercury's primary modules currently release together under one version. The registry
  nevertheless enforces exact provider/contract build identity so a mixed classpath fails
  closed rather than silently reporting the wrong release.
- MiniGraph is the first user-facing frontend because it already has the `help`, `list`,
  and `describe` vocabulary. The product service remains reusable by another read-only
  frontend without creating another authority; filesystem export stays local-only.
- Human documentation remains source material for focused references, while the packaged
  contract inventory and runtime providers define what the offline skill actually claims.
