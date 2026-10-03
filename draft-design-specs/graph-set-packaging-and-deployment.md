# Graph sets — a pack/unpack CLI, a Playground packaging panel, and packaged sets in the deployment manifest

**Status:** DRAFT design review + implementation plan for the next sprint — 2026-10-03, Claude Code.
**Concept:** Eric's design notes of 2026-10-03 (four concepts: a command-line packager for pipelines, a Playground panel that
builds a canonically packaged set of graphs from dropped or imported JSON files with manifest key-values, download of the
packed file, and direct deployment of packed sets through `graphs.yaml` — a `sets` list and an `unpack` location, with
automatic unpacking before the CompileGraph quality gate and the later-wins rule for duplicate graph ids).
**Proposal register:** RFC-0005 in `docs/arch-decisions/RFC.md`; the work item is the open thread `graph-set-packaging`.
**Grounding:** the live tree on 2026-10-03 (file:line anchors below). The packager is ADR-0026 (RFC-0002), shipped in
v4.12.20 on both engines; this plan is the "first consumer" that RFC deliberately left out.

---

## 1. Verdict

**The design is correct in its intent and fits the engine with one structural refinement.** Everything it needs already
exists as a primitive: the deterministic package (`CanonicalPackager`, ADR-0026), the manifest list with the later-wins rule
(`graph.model.automation`, ADR-0011 extended on 2026-09-25), the gate (`CompileGraph`), and, since PR #500, a Playground that
validates a graph file and posts it to the engine. The refinement is about *how* the unpack location joins the deployment:

| Concept | Verdict | Refinement |
|---|---|---|
| 1. A command-line packager (`pack`, `unpack`) for pipelines; signing and verification in a separate utility | Correct, and exactly the shape ADR-0026 asked for (integrity outside the package; the exact bytes are what an application protects) | The CLI must *be* the engine's packager, not a third implementation: a thin `main` over `CanonicalPackager` in Java and a `bin` over `platform_core::canonical_packager` in Rust, both proven byte-identical by the shared vectors and one cross-engine pack of the tutorials. It must never inject a timestamp or environment data into the manifest, or the bytes stop being reproducible and a signature stops meaning "this content". |
| 2. A Playground panel that assembles a set from dropped or imported graph JSON files, with manifest key-values | Correct | Pack on the engine through a dev-mode endpoint, not in the browser: a TypeScript packager would be a third implementation of the wire contract to keep byte-identical forever. The panel reuses PR #500's file validation per entry. |
| 3. Download of the packed file | Correct | A binary sibling of the `saveTextFile` helper shipped in #500; the file is `<set>.mpk`. |
| 4. `sets` + `unpack` in `graphs.yaml`, automatic unpacking before the gate, later wins with an error log | Correct in intent; one rule needs care | The automation list is a list of **manifests**, never bare folders — a manifest is the allowlist behind "compiled or 404" (ADR-0011). So the unpacker **writes a generated manifest into the unpack folder** and compiles it as the next manifest in sequence. Everything downstream (`CompiledGraphs` locations, `list graphs`, the `import graph from` fallback, the later-wins rule) then works unchanged. The unpack location must be a `file:/` folder on the machine's file system with read and write access — the application writes the unpacked JSON there and CompileGraph reads it back — so `classpath:` is rejected (Eric's ruling); never the Playground's `location.graph.temp`, whose housekeeping deletes expired files. |

Two properties fall out of the refinement that the concept did not state and that are worth having:

- **A set registers all of its graphs or none** (the RFC-0002 sketch of the loader). A set is a certified unit, often with
  `graph.extension` cross-references; half a set deployed is worse than none. A loose `graphs` entry keeps today's per-graph
  behavior.
- **The gate can run at pack time, too.** Every check in `compileOneGraph` is static on the graph JSON (structure, root
  `purpose`, an `end` node, `GraphModelValidator`); none consults the target application's function registry. The CLI and the
  panel can therefore refuse to pack a graph that would be rejected at deployment — the gate at startup stays the authority,
  so this is defense in depth, not a replacement.

---

## 2. What exists today

### 2.1 The package (ADR-0026)

`org.platformlambda.core.serializers.CanonicalPackager` (CanonicalPackager.java:84-253): `builder()` →
`manifest(key, value)` → `add(name, map)` → `build()` gives the bytes; `unpack(bytes)` (strict by default, re-encodes and
compares) returns `Package(manifest, maps)` with ordered maps; `encode`/`decode` are the value-level pair. The Rust twin is
`platform_core::canonical_packager` (canonical_packager.rs:103-233, `Builder`, `unpack`, `unpack_with`). The package is

```text
{ "manifest": { "format": "mercury-package", "format_version": "1", <caller fields as text> },
  "maps":     { "<entry-name>": { ...keys sorted at every depth... }, ... } }
```

The packager writes `format` and `format_version`; every other manifest field is caller text it never interprets. Entry names
are ordered by UTF-8 bytes; a duplicate entry name is an error. The guide (`docs/guides/canonical-package-format.md`) names
one convention, `graph_id` = "the graph the package delivers, with the other maps as its subgraphs". **A set delivers several
graphs, so that convention needs relaxing** (decision D5 below). Integrity is outside the package: nothing inside refers to a
hash or signature; the vectors file (`system/platform-core/src/test/resources/canonical-package-vectors.json`, byte-identical
in both repos) is the interop proof.

### 2.2 The deployment gate (ADR-0011, extended 2026-09-25)

`CompileGraph` runs at `@BeforeApplication(sequence = 6)` (CompileGraph.java:76). `start` (:95-116) splits
`graph.model.automation` on commas and calls `compileManifest` per entry in order; `compileManifest` (:118-141) reads the
manifest's `location` (`file:` or `classpath:`, default `classpath:/graph`), registers it with
`CompiledGraphs.addDeployedLocation`, then calls `compileOneGraph` per `graphs[i]`; a manifest that cannot be read is skipped
with a WARN. `compileOneGraph` (:143-177): a graph id already registered from a *different* location is replaced with
`WARN Graph {} from {} replaces the copy from {}` (the later manifest owns the id — if its copy is then rejected, the id is
not executable rather than served from the replaced copy); the file `<location>/<id>.json` is read, deprecated mapping syntax
converted, `MiniGraph.importGraph` (structure), root `purpose`, an `end` node and `GraphModelValidator.validate` are
checked; a failure logs `ERROR Rejected graph {} - {}` and the id stays unregistered (404). The Rust twin is
`knowledge_graph::compiler` (compiler.rs:75-155) with the same messages; `graphs.rs` is the registry
(`add_graph`, `graph_location`, `deployed_locations`).

The manifest today (the example's and the template's):

```yaml
graphs:
  - 'tutorial-1'
  - 'tutorial-2'
location: 'classpath:/graph'
```

### 2.3 The Playground's file path (PR #500)

`POST /api/graph/import/{id}` takes one graph model, validated by `GraphCommandService.validateGraphModel` (a JSON object
whose only top-level sections are `nodes` and `connections`, then the importer's own checks) and routed like a command to
every member of a shared session. The webapp's `utils/graphFile.ts` validates a file the same way, `useGraphFileImport`
posts it, `saveTextFile` writes `<graph-id>.json` through the browser's save dialog or the download folder. The left slot of
the Playground hosts in-place panels (`LeftPanelMode = 'console' | 'node-edit' | 'upload'`).

---

## 3. The refined design

### 3.1 The artifact: a graph set

- **File:** `<set>.mpk` ("mercury package"; D1). One package = one set. The file name without the extension is the **set
  name**, the identifier `graphs.yaml` lists.
- **Entries:** one per graph, named `<graph-id>.json` (the RFC-0002 convention: entry names are file names). The graph id is
  the file name without `.json`; it follows the engine's file-name rule (`[A-Za-z0-9_-]+`, `validGraphFileName`) and, when the
  root node carries a `name`, must equal it (the `export graph as` rule; D6).
- **Manifest fields** (all text; the packager never interprets them, the loader reads a few):
  - written by the packager: `format`, `format_version`;
  - recommended for a set: `set` (equals the file name), `version`, `description`, `author`;
  - optional: `graph_id` — the set's entry-point graph; when present the loader checks that it names a packed entry (RFC-0002's
    intended check) and prints it in `list graphs`; absent for a set of independent graphs (D5);
  - anything else the operator wants (ticket, commit, certification reference) — recorded, printed, never interpreted.
  - **No automatic timestamp.** The same inputs must give the same bytes; a build time is a field the operator passes
    explicitly if the pipeline wants one.
- **Determinism across tools:** the CLI parses JSON with the same reader the gate uses (`ConfigReader(path).getMap()` in Java,
  `conversions::from_json` in Rust), so what is packed is what the gate would have read, and both CLIs produce identical bytes
  for the same files (checked on the tutorials, see §5).
- **Signature:** a detached file beside the package (`<set>.mpk.sig`) is the convention the separate signing utility and a
  later verifying hook can share; nothing about it is inside the package or interpreted by the engine in this sprint.

### 3.2 The command-line packager

Java: a new module **`helpers/graph-packager`** producing an executable jar (`java -jar graph-packager.jar …`); Rust: a bin
crate **`tools/graph-packager`** (`graph-packager …`) — D8. Both are thin fronts over the engine's packager and the gate's
validation; neither starts the platform.

```text
graph-packager pack   --set <name> [--manifest key=value]... [--out <dir>] <graph.json>... | <folder>
graph-packager unpack <file.mpk> --out <dir>            # writes <graph-id>.json, keys in canonical order, 2-space indent
graph-packager inspect <file.mpk> [--json]              # manifest, entries (id, nodes, connections), size, SHA-256
```

- `pack` reads each file (a folder means every `*.json` in it), derives the id from the file name, checks the file-name rule
  and the root-name agreement, runs the gate's static validation (D2) and refuses the whole set when any graph fails, listing
  every reason; writes `<set>.mpk` and prints the SHA-256 (a convenience for the signer, not integrity inside the package).
- `unpack` writes readable JSON with sorted keys so two unpacks of two versions diff cleanly; exact numbers stay text.
- `inspect --json` is for pipelines; exit codes: 0 success, 1 a refused input (validation, name rules), 2 an I/O or format
  error (a non-canonical package fails the strict read).
- The gate's checks move into a reusable method the CLI, the pack endpoint and `CompileGraph` share (`GraphModelGate` or a
  static `CompileGraph.validateModel(graphId, model)`), so "passes the gate" means the same thing in all three places.

### 3.3 The Playground panel and its endpoints

- **Endpoints (dev-mode, `@OptionalService("app.env=dev")`, listed in every `rest.yaml` copy like `import.graph.content`):**
  - `POST /api/graph/pack` — body `{ "manifest": {k: v}, "graphs": { "<id>": <model>, ... } }`; validates each model
    (the import validation, then the gate's static checks when D2 says so), packs, answers `application/octet-stream` with
    `Content-Disposition: attachment; filename="<set>.mpk"`; 400 names the first refused graph and reason.
  - `POST /api/graph/unpack` — body the `.mpk` bytes; answers `{ "manifest": {...}, "graphs": { "<id>": <model> } }` (D9).
    The panel uses it to inspect a dropped package and to import one entry as the draft through the existing import path.
- **Panel:** a new left-slot mode `'package'` ("Package graphs"), opened from the Tools menu, in the console's slot like the
  mock-upload form. It has a drop zone and a Browse button that accept several `.json` files at once (the current importer
  takes one — the panel's own handler takes many), an **Add current graph** button for the live draft, a list of entries
  (graph id, node and connection counts, a remove button; a duplicate id or a root-name mismatch is flagged in place), a
  manifest editor (the `set` name required and validated by the file-name rule, `version` and `description` suggested, plus
  free key-value rows), and **Pack and download**. A dropped `.mpk` switches the panel to inspect mode (manifest and entries,
  "Import as draft" per entry).
- **Download:** `saveBinaryFile(bytes, "<set>.mpk")`, the binary twin of `saveTextFile` (the native save dialog in Chromium,
  the download folder elsewhere).

### 3.4 The deployment manifest

```yaml
graphs:                      # loose graphs, as today
  - 'tutorial-1'
  - 'tutorial-2'
location: 'classpath:/graph'  # where <graph-id>.json AND <set>.mpk are read from (default classpath:/graph)

sets:                        # packaged sets, by set name; <location>/<set>.mpk
  - 'settlement-1'
  - 'settlement-2'
unpack: 'file:/tmp/deployed'  # REQUIRED when sets are present; file:/ only (classpath: is rejected); never location.graph.temp
```

Rules: `sets` entries and `unpack` are optional; `sets` without a usable `unpack` logs an ERROR and skips the sets (the loose
graphs still compile). **`unpack` is a `file:/` location on the machine's file system with read and write access** (`/tmp`,
`/opt`, a mounted volume): the application writes the unpacked graph JSON files there and CompileGraph reads them back from
there, so a `classpath:` value is rejected outright (Eric's ruling, 2026-10-03), as is any value that is not `file:/`. At
startup the loader creates the folder when it is missing and verifies that it can read and write there; a folder it cannot
write logs an ERROR and the sets are skipped. `unpack` must not equal `location.graph.temp` (the Playground's workspace, whose
housekeeping deletes expired files) and must be unique across the manifests of one application (a second manifest naming the
same folder has its sets skipped with an ERROR).

### 3.5 The loader

Inside `compileManifest`, after the loose graphs: first the `unpack` check — `file:/`, not `classpath:`, not the Playground
temp folder, not used by another manifest, created when missing, readable and writable (a probe file) — and on any failure an
ERROR and no set of this manifest is touched. Then, for each set in listed order:

1. **Read and verify the package.** `<location>/<set>.mpk` (classpath or file); a missing file or a strict-read failure logs
   `ERROR Set {} not deployed - {}` and the set is skipped.
2. **Check names before any path is built.** Every entry name must be `<id>.json` with a valid id; when `graph_id` is present
   it must name an entry; a root `name` present in a model must equal the id (D6). A failure rejects the whole set (ERROR).
3. **Unpack to files.** Write `<unpack>/<id>.json` for every entry (readable JSON, canonical key order), after deleting the files
   the previous run generated for this set (tracked by the generated manifest, step 4) so a graph removed from a new version
   of the set does not linger. The unpacker never deletes a file it did not generate.
4. **Write the generated manifest** `<unpack>/graphs.yaml`: the ids of every unpacked graph (all sets of this manifest),
   `location: <unpack>`, and provenance as comments — the source manifest, each set's file, SHA-256 and manifest fields, the
   unpack time (a comment, so the manifest itself stays reproducible).
5. **Compile the set through the gate, all or none.** Validate every graph of the set first (the shared gate method); if any
   fails, log `ERROR Set {} rejected - {} of {} graphs failed: <id>: <reason>; ...` and register none of them. If all pass,
   register them; a graph id already registered from another location is replaced with
   `ERROR Graph {} from set {} ({}) replaces the copy from {}` (D4). The generated manifest's location is added to
   `CompiledGraphs` right after its parent manifest's location, so `list graphs`, `import graph from <id>` (the deployed
   fallback) and the Playground's deployed-model discovery see the unpacked set like any deployed location.
6. **Log the outcome:** `INFO Deployed set {} (version {}) from {} - {} graphs into {}`.

**Precedence**, stated once: compile order is `[M1.graphs, M1.sets in listed order, M2.graphs, M2.sets, …]`, and a later
graph in that order owns a duplicate id (the rule of 2026-09-25, unchanged); within a set duplicates are impossible. So a set
wins over the loose graphs of its own manifest and over everything in earlier manifests, and a later manifest wins over it.
`graph.model.automation` itself is not rewritten; the generated manifest is an implicit next entry, logged.

**Failure modes:**

| Situation | Behavior |
|---|---|
| `sets` present and `unpack` missing, `classpath:` (rejected: the loader must write there), not `file:/`, equal to `location.graph.temp`, or not writable | ERROR, the sets skipped, loose graphs compile |
| Two manifests share one `unpack` | ERROR for the second, its sets skipped |
| Package missing or not canonical (strict read) | ERROR, that set skipped |
| Bad entry name, invalid id, `graph_id` not an entry, root name mismatch | ERROR, that set rejected before any file is written |
| One graph of a set fails the gate | ERROR naming every failure, the set registers none (files and generated manifest still written for inspection) |
| Duplicate id across sets or manifests | later wins; ERROR when a set is involved, WARN between loose manifests as today (D4) |
| Startup cost | one MsgPack decode and one JSON write per graph; negligible |
| Hot reload | not in scope (ADR-0026 deferred it); a restart re-unpacks |

**Path safety:** set names (from `graphs.yaml`) and graph ids (from entry names) are validated against the file-name rule
before any path is composed, so a crafted package cannot write outside the unpack folder.

### 3.6 Out of scope here, with the seams left for it

Signing, verification and signature queries are the separate utility (concept 1); the engine does not verify at startup in
this sprint. The seams: the detached `<set>.mpk.sig` convention, `inspect`'s SHA-256, and step 1 of the loader, where a
verifying hook would sit. Also out: hot reload, two versions of a set side by side, compression, per-entry hashes (all
deferred by ADR-0026).

---

## 4. Decisions needed (recommendation first)

| # | Decision | Recommendation | Alternative |
|---|---|---|---|
| D1 | File extension | `.mpk` | `.pack`, `.mgs` |
| D2 | Run the gate's static checks at pack time (CLI and panel), refusing the set on a failure | Yes — the same method as the startup gate | Pack anything; the gate catches it at deployment |
| D3 | Registration per set | All or none | Per graph, like loose files |
| D4 | Log level for a duplicate id | ERROR when a set is involved; WARN kept between loose manifests (the rapid-prototyping override is intended there) | ERROR everywhere |
| D5 | `graph_id` manifest field | Optional entry point, verified against the entries when present; the guide's "subgraphs" wording relaxed | Required for every set |
| D6 | Root `name` must equal the entry's id | Enforce at pack and at unpack | Warn only |
| D7 | Where packing happens for the panel | The engine, `POST /api/graph/pack` | A TypeScript packager (a third implementation) |
| D8 | Module homes | Java `helpers/graph-packager` (executable jar); Rust `tools/graph-packager` (bin) | A `main` inside the engine module; a `--features cli` bin in the Rust engine crate |
| D9 | Inspect and import of a `.mpk` in the Playground | In scope (small: one endpoint, one panel mode) | Later |

---

## 5. Implementation plan for the next sprint

Lock-step on both engines, each step a PR Eric gates; the webapp lands once (one source, deployed to both repos).

| WP | Scope | Tests and proof |
|---|---|---|
| **WP1 — the gate as a method** (Java + Rust) | Extract `compileOneGraph`'s checks into a reusable validation (`validateModel(graphId, model)`); `CompileGraph` and `compiler.rs` call it; no behavior change | existing CompileGraph tests; the 15 example graphs still compile |
| **WP2 — the CLI, pack first** (Java `helpers/graph-packager`, Rust `tools/graph-packager`) | `pack`, `unpack`, `inspect`; the name rules; D2; exit codes; the `.mpk` extension | round trip on the example's graphs; a refused graph (no `end`); both CLIs pack the 15 tutorials to identical bytes (the interop report gains a section) |
| **WP3 — the loader** (Java + Rust) | `sets` + `unpack` in `graphs.yaml`; the unpacker, generated manifest, all-or-none, precedence, logs, path safety; `CompiledGraphs` location order | CompileGraph tests with packages built in the test from JSON fixtures: a set deploys and `POST /api/graph/{id}` serves it; a set with one bad graph registers none; a duplicate across set and loose graph (ERROR, later wins); missing `unpack`; `unpack` on classpath refused; a removed graph is cleaned on the next start; `import graph from` finds an unpacked graph; a crafted entry name is refused |
| **WP4 — the endpoints** (Java + Rust) | `POST /api/graph/pack` and `/unpack`, dev-mode; the five Java and three Rust `rest.yaml` copies and the guide excerpt | REST tests through the real router (200 bytes with the file name header, 400 naming the graph, strict-read 400 on unpack) |
| **WP5 — the Playground panel** (webapp) | `'package'` left-slot mode, multi-file drop, Add current graph, entries, manifest editor, Pack and download, inspect mode; `saveBinaryFile` | vitest for the entry model and the manifest validation; a live drive on both engines (pack in the panel, unpack with the CLI, deploy the file through `sets`, curl the graph) |
| **WP6 — docs and ledger** | `configuration-reference.md` (`sets`, `unpack`), `ai-agent-guide.md` (deploy-without-rebuild gains the packaged path), `canonical-package-format.md` (the extension, the CLI, the set convention; the "Not part of the packager" paragraph shrinks), `build-your-first-graph.md`, the templates' `graphs.yaml` comments, a `help package` topic for the panel, CHANGELOG both repos, INCREMENTS; RFC-0005 → ADR-0027 on Eric's acceptance | `check-doc-canon`, the grammar specs, `check-doc-claims` (the new ERROR/INFO messages become pinned claims) |

Suggested order: WP1 → WP2 (a `pack` that exists makes every later fixture real) → WP3 → WP4 + WP5 → WP6, with the ADR
written when Eric accepts the whole. Roughly two to three engine PRs per repo and one webapp PR.

---

## 6. Open questions

- Should `pack` accept a `graphs.yaml` as input (pack exactly what a manifest lists) in addition to files and folders? Cheap and
  pipeline-friendly; proposed as a WP2 flag (`--from-manifest`).
- Should the generated manifest be hidden from `graph.model.automation`'s documentation entirely, or described as "an implicit
  entry the loader adds after its parent manifest"? The plan describes it; the property's value is never rewritten.
- Field use may want a set's `version` to appear in `list graphs` and in the `INFO` line; the plan prints it when present.

## 7. References

- ADR-0026 and RFC-0002 (`docs/arch-decisions/`), the guide `docs/guides/canonical-package-format.md`, the interop report
  `docs/test-reports/canonical-package-java-rust-interop.md`.
- ADR-0011 (compiled or 404) and the manifest-list rule of 2026-09-25 (`graph-manifest-list-later-wins`).
- PR #500 (the Playground's file import and download) and the sprint thread `playground-usability-sprint`.
