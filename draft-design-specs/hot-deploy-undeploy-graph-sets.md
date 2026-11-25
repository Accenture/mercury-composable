# Hot deploy and undeploy of knowledge graphs - a certified graph set uploaded to a running engine, registered through the gate, and removed by name

**Status:** DRAFT, round 2 - 2026-11-24 (Claude Code). Round 1 was the review of the maintainer's draft; in round 2,
the same day, the maintainer accepted every recommendation of sections 5 and 6 as the default and replaced the polling
option for several instances with on-demand registration, refined here into a revision marker (3.8); in round 3, still
the same day, he keyed that reference on the set's main graph: a hot-deployable set is named after its main graph,
exposes it alone at the endpoint, and its own record is the marker (3.3, 3.7, 3.8). Round 4, after the merge of
the register entry: a task node names its function's provider with `server.url` and `server.header[]`, so a
hot-deployed graph reaches a function the static Event-over-HTTP map has never heard of (3.10, D16). Round 5: the
developer's two surfaces - a deployment dialog in the Playground for dev mode, and two Node template scripts for an
engine without the UI (3.11, D17), which reverses Q8. Raised as RFC-0017 in `docs/arch-decisions/RFC.md`; next: the
build in both engines, the live drive, then promotion to an ADR. It amends ADR-0011 and extends ADR-0027 (section 4).
**Concept:** the maintainer's future-state draft of 2026-11-24 - bundled graphs and the prototyping workspace unchanged;
a third folder, `/tmp/deploy`, where a graph set uploaded to a deployment endpoint is registered without a restart and
removed by an undeployment endpoint; two feature switches, off for field installations that have not adopted the AI
SDLC; the architecture recommendation that user-defined functions move to the language packs so the engine itself never
restarts for a graph; the operational dashboard owned by the field.
**Proposal register:** RFC-0017 in `docs/arch-decisions/RFC.md`; the work item is the open thread `hot-deploy-graph-sets`.
**Grounding:** the live tree at `ea03269a5` (2026-11-24). File and method anchors below are to that tree.

---

## 1. Verdict

**The concept is right, and the engine already holds every primitive it needs.** The unit it deploys is the graph set
of ADR-0027, read strictly, gated and registered all or none by `GraphSetLoader`; the registry that execution reads is
`CompiledGraphs`; the dev-mode gate it keeps for the workspace is `app.env=dev`, which already fences every Playground
service; the upload path is the one `UnpackGraphSet` uses today. The design work is in four places the draft leaves
open, each decided below with the recommendation first: who owns a graph id when a set arrives at run time (section
3.5), how a set replaces its previous version without a run seeing half of it (3.6), how a deployment survives a
restart and reaches a second instance without a poll (3.7 and 3.8), and what the two switches do to the folder when
they are off (3.2).

| Draft item | Verdict | Refinement |
|---|---|---|
| Bundled graphs unchanged: `resources/graph`, listed in `graphs.yaml` | Correct | Nothing changes. A bundled id is owned by its manifest and cannot be shadowed or undeployed at run time (D4). |
| Prototyping workspace unchanged: `/tmp/graph`, dev mode when `app.env=dev` | Correct, and already so | Every Playground service carries `@OptionalService("app.env=dev")`, and only those services read `location.graph.temp`. Section 2.2 records it; no change. |
| Hot deploy and undeploy use `/tmp/deploy` from a default in `application.properties` | Correct | A new key, `location.graph.deploy`, `file:/` only, writable, outside the temp folder and outside every manifest's `unpack` folder (D3). |
| Switches `graph.hot.deploy` and `graph.hot.undeploy` | Correct | Default **off**; either switch on makes the engine read the folder at startup, so an installation that turns deploy off after a cut-over keeps serving what it deployed and can still remove it (D2). |
| Deploy: prototype, certify the set, export, upload to a REST endpoint, a function compiles and registers it, the id is live at once | Correct | The set is the unit (one graph is packed alone, as ADR-0027 already says). The endpoint is a production function behind the switch, not a dev-mode one. An optional `X-Graph-Set-SHA256` header lets the ledger check happen in the engine without the engine owning the ledger (D5). |
| Undeploy by set through a POST endpoint; bundled graphs cannot be undeployed | Correct | Undeploy acts on the hot folder's record only; a set name the record does not hold is 404 (3.4). |
| Persistence: mount the folder as a persistent volume | Correct, with one addition | The folder holds the packages, the unpacked graphs and one engine-written record per set, named after the set's main graph; at startup the engine re-registers what the records list, so the volume is enough to survive a restart (3.7). The record is also the marker every other instance reads, on demand, at its next request (3.8). |
| Architecture: user-defined functions in language-pack sidecars, the engine free of them | Correct, with one enabling property | The engine's part is a deploy-time advisory, never a refusal, listing the routes a set names that this instance does not know (D9), a guide section, and since round 4 `server.url` on a task node, so a hot-deployed graph names the provider of a function the static Event-over-HTTP map has never heard of (3.10, D16). |
| Operational dashboard through an API gateway | Correct, out of scope | The endpoints carry no authentication of their own; the gateway does (Q4). A read endpoint for the dashboard is in scope (3.4). Since round 5 the developer's own surfaces are in scope too: a deployment dialog in the Playground for dev mode, and two Node template scripts for an engine without the UI (3.11, D17); the field's dashboard stays theirs. |

Two properties fall out of the design that the draft did not state and that are worth having:

- **A set is replaced in one step.** Today the registry is three concurrent maps written one graph at a time, which
  is fine at startup, before any request. At run time a set with `graph.extension` cross-references would be half new
  and half old for a few microseconds. The registry becomes one immutable snapshot swapped once per deployment, so a
  run sees the whole old set or the whole new one (D6).
- **The record, not the folder, is the allowlist.** ADR-0011 made the manifest the only door and ruled hot-dropping a
  file out. The hot folder keeps that: the engine reads only the sets its own record names, and a `.pack` someone
  copied into the folder is ignored with a warning. The endpoint is the door, as the manifest is at startup (D7).
- **Every instance converges at its next request, and still all or none per set.** The maintainer's direction in
  round 2 was on-demand registration in place of a poll; its refined form reads the set's own record - one small file
  named after the set's main graph - per request for a hot-deployed or unknown id and, when it changed or vanished,
  reconciles that set under the lock: a set appears, is replaced or disappears as a unit, in one swap, on every
  instance, and a change to one set costs the others nothing (3.8, D13).
- **A set is named after its main graph and exposes it alone (round 3, D14, D15).** Exactly one main graph, zero or
  more subgraphs; the set name is the main graph's id, so the pack file, the record, the deploy answer, the undeploy
  path and the REST path are one string, and a subgraph is reached only through `graph.extension` from its own set.

---

## 2. What exists today

### 2.1 Three lanes, one registry

- **Bundled.** `graph.model.automation` names one or more manifests; `CompileGraph.start` (`@BeforeApplication(sequence
  = 6)`) compiles each in order through `compileManifest`: the manifest's loose `graphs` from its `location` (default
  `classpath:/graph`), then its packaged `sets` through `GraphSetLoader.deploy`. A graph that passes
  `GraphModelGate.validate` is registered in `CompiledGraphs`; a later manifest owns a duplicate id (4.12.19, the
  rapid-prototyping lane). `GraphExecutor` reads the registry alone and answers 404 for anything else
  (`GraphExecutor.java:575-579`, ADR-0011); `graph.extension` resolves its targets and their root schemas from the same
  registry.
- **The prototyping workspace.** `location.graph.temp` (default `file:/tmp/graph`, `GraphCommandService.java:99-124`)
  is where `export graph as` writes and `import graph from` reads first, with the deployed locations as the fallback.
  Nothing in it is executable over REST.
- **Deploy without a rebuild.** A second manifest in a `file:/` folder, appended to the property with a JVM flag. Every
  guide says it plainly: a deployment is still a restart, since `CompileGraph` runs once and `compileManifest` has no
  runtime caller outside tests.

`CompiledGraphs` (`CompiledGraphs.java:42-45`) holds four static concurrent structures: the models, each graph's
location, the deployed locations in manifest order, and each graph's `DeployedSet(name, version)`. `removeGraph`
exists for the later-wins replacement only. No code path removes a graph after startup.

### 2.2 The graph-set loader (ADR-0027)

`GraphSetLoader.deploySet` is the whole runtime path in miniature: the set name is checked before it becomes a path
(`GraphModelGate.isValidGraphId`); the package is read strictly (`GraphSet.read`, every name rule checked, the SHA-256
of the bytes kept for provenance); every graph is written as `<unpack>/<graph-id>.json`; the gate reads each file back
through `ConfigReader` so `${...}` references resolve for this environment; the set registers all of its graphs or none;
a generated manifest `<unpack>/graphs.yaml` records what was deployed and which files were written, and the next start
removes those files before it unpacks again. `checkUnpack` refuses an `unpack` folder that is the temp folder, is inside
it, or is used by another manifest. The loader never deletes a file it did not write.

### 2.3 Dev mode and the Playground endpoints

Every Playground service is `@OptionalService("app.env=dev")`: the command service, the traveler, the WebSocket user
interface, the mock functions and all of the REST functions under `com.accenture.minigraph.rest`, including
`PackGraphSet` (`POST /api/graph-set/pack`, builds a set from models and answers the `.pack` as a download) and
`UnpackGraphSet` (`POST /api/graph-set/unpack`, takes the `.pack` as an `application/octet-stream` body or a stream,
answers the manifest and the graphs). `GetIndexHtml` serves the Playground web app in dev mode only
(`GetIndexHtml.java:46-51`). The example's `application.properties` sets `app.env=dev` and says to bind it to an
environment variable in a deployed environment. A REST entry whose service route is not registered answers 503,
`Service <route> not reachable` (`HttpRouter.java:641`). **Corrected by the live drive (2026-11-25):** that 503 is for a
route present when `rest.yaml` loaded and unreachable at request time; a service absent at load makes `RoutingEntry`
skip the entry with a WARN, and the URL answers 404 (`Resource not found`).

### 2.4 The Rust twin

`crates/knowledge-graph/src/compiler.rs` (`compile_graphs`, `compile_manifest`), `graph_set_loader.rs` (`deploy`,
`deploy_set`) and `graphs.rs` (the registry behind `RwLock`s: `add_graph`, `add_set_graph`, `remove_graph`,
`graph_location`, `deployed_locations`) mirror the Java engine method for method; `rest.rs` carries `pack_graph_set`
and its unpack twin.

---

## 3. The refined design

### 3.1 Three folders

| Folder | Key | Default | Who writes | Who reads | Executable |
|---|---|---|---|---|---|
| Bundled models | the manifest's `location` | `classpath:/graph` | the build | `CompileGraph` at startup | yes, when listed and gated |
| Prototyping workspace | `location.graph.temp` | `file:/tmp/graph` | `export graph as` (dev mode) | `import graph from` (dev mode) | no |
| Hot deployment | `location.graph.deploy` (new) | `file:/tmp/deploy` | the deploy and undeploy functions | the reconcile at startup and after each change | yes, when recorded and gated |

The hot folder must be a `file:/` folder the application can write, as `unpack` must be. It may not be the temp
folder or inside it, and may not be any manifest's `unpack` folder (the example's bundled `tutorials` set unpacks into
`file:/tmp/unpack`); `checkUnpack` gains the converse rule. On a developer's machine `/tmp` is cleared at reboot, so the
guide says to point the key at a folder under the home directory when a deployment should outlive a reboot; in a
container the folder is a mounted volume.

### 3.2 Two switches

```properties
# hot deploy and undeploy of graph sets (section 3 of the design spec) - both OFF by default
graph.hot.deploy=false
graph.hot.undeploy=false
location.graph.deploy=file:/tmp/deploy
# optional - the least time between two reads of the revision marker, in ms; 0 reads it at every
# request to a hot-deployed or unknown graph id (section 3.8)
# graph.hot.deploy.throttle=0
```

- `graph.hot.deploy=true` registers the deploy function and the list function.
- `graph.hot.undeploy=true` registers the undeploy function and the list function.
- **Either switch on** makes `CompileGraph` reconcile the hot folder at startup (3.7) and adds the folder to the
  deployed locations, so `list graphs`, `describe graph`, `import graph from`, the OpenAPI endpoint and
  `graph.extension` see hot-deployed graphs as they see bundled ones.
- **Both off** is today's engine exactly: the folder is never read, the functions never register, and the REST entries
  answer 404 as any unknown path does, since REST automation skips an entry whose function is absent at startup (the
  live drive, 2026-11-25; the draft said 503). A field installation that has not adopted the AI SDLC
  changes nothing.

The functions carry `@OptionalService` on their switch, the shape the project settled for an operational choice (the
OpenTelemetry forwarder, 2026-09-16): one artifact ships and the installation decides per environment. The switches are
not dev mode: a production engine with `app.env` unset hot deploys when its operator turns the switch on.

Two switches are two questions, may add and may remove, so both stay. The posture they allow and that one switch
could not: after a release is frozen, deploy off and undeploy on - nothing new enters, but an emergency removal is one
request away.

### 3.3 The unit: a graph set

A deployment is a graph set, the `<set>.pack` of ADR-0027, and nothing else: one graph is packed alone when it is the
whole deployment (that is also how one graph is signed, as the ADR says). A loose JSON file is not hot-deployable. The
set name is the file name without the extension and the manifest's `set` field; the engine takes it from the manifest
inside the package, never from the request, so the client sends bytes and nothing it says about them is trusted.

**A hot-deployable set has exactly one main graph and is named after it (the maintainer, round 3).** The main graph
is the one a caller reaches at `POST /api/graph/{graph-id}`; every other graph in the set is a subgraph, reached from
the main graph through `graph.extension` and from nowhere else (D15). The set name is the main graph's id - the
set-name rule already uses the graph-id character set, so every set name is a valid graph id - and a set with no
graph of that id is refused at upload. ADR-0027's optional `graph_id` field, when the manifest has it, must equal the
set name; it stays a consistency check, not a second identity. One string is therefore the handle everywhere: the
pack file `<id>.pack`, the record `<id>.deployed.yaml` (3.7), the deploy answer, the undeploy path and the REST path.
A developer with two independent entry points packs two sets. A startup-lane set (a bundled `sets` entry of the
manifest) keeps ADR-0027 as it is: any name, every graph public.

The field's lifecycle maps onto the engine without a new concept:

| Vision step | Where | What the engine sees |
|---|---|---|
| dry-run | the Playground in dev mode | nothing |
| certify | the field: a ledger of SHA-256 per set, or a signature with the field's CA | nothing, by design (ADR-0027: signing and verification stay outside the engine) |
| stage | `pack` in the Playground, the download on the administrator's machine or in an artifact store | nothing |
| approve | the field's dashboard | nothing |
| production | `POST /api/graph-set/deploy` | the package, read strictly, gated, registered |

### 3.4 The endpoints

Three REST entries in `rest.yaml`, mapped to functions as the Playground's pack entries are (`service:`, not a flow:
the work is one function with no orchestration). They are production entries: no `app.env` condition, no CORS beyond
what the installation configures, `tracing: true`.

| Entry | Route | Switch | Body | Answer |
|---|---|---|---|---|
| `POST /api/graph-set/deploy` | `deploy.graph.set` | `graph.hot.deploy` | the `.pack`, `application/octet-stream` or a stream, as `UnpackGraphSet` reads it | the set as deployed (below) |
| `POST /api/graph-set/undeploy/{graph-id}` | `undeploy.graph.set` | `graph.hot.undeploy` | none | the set, named by its main graph, as removed |
| `GET /api/graph-set/deployed` | `list.deployed.sets` | either | none | the record (below) |

**Deploy** checks, in this order, and stops at the first failure with every reason it has:

1. the body is present (400 otherwise, the unpack endpoint's message);
2. when the request carries `X-Graph-Set-SHA256`, the hex digest of the body equals it (400, `SHA-256 does not match`
   - the administrator read the digest from the field's ledger, and the engine confirms the bytes are those bytes);
3. the package reads strictly (`GraphSet.read`; 400 with the set rules' reasons);
4. the set is named after its main graph (3.3): a graph whose id is the set name is in the set, and `graph_id`, when
   the manifest has it, equals the set name (400 otherwise, naming the rule);
5. ownership (3.5; 409 naming each graph id and its owner);
6. the files are written to the hot folder under temporary names and renamed into place, `<id>.pack` and one
   `<graph-id>.json` per graph, so a crash leaves no half-written set;
7. the gate reads every graph back and validates it; a refusal (400 with every graph's reasons) removes the files just
   written - the reasons are in the answer, so unlike the startup loader there is nothing to leave for inspection;
8. the registry swaps (3.6), the set's record `<id>.deployed.yaml` is written last (3.7), one INFO line is logged with
   the set, its version, its SHA-256, the graph count and the remote address, and the answer is 200.

```json
{ "set": "settlement", "version": "1.4.0", "sha256": "9f2c...", "graphs": ["settlement", "settlement-review"],
  "deployed_at": "2026-11-24T18:02:11.412Z",
  "replaced": { "version": "1.3.2", "sha256": "41b0..." },
  "routes": { "known": ["v1.ledger.lookup"], "unknown": ["v1.settlement.score"] } }
```

`replaced` is present when the set name was already deployed (3.5). `routes` is the advisory of D9, and beside it
`extensions` lists the `graph.extension` targets the set names outside itself, each marked known or unknown to this
instance, so a dependency on another set or on a bundled graph is visible at deploy time. A second upload of bytes
already deployed is 200 with the same answer and no change.

**Undeploy** validates `{graph-id}` with the graph-id rule before it is a path, finds the set's record
`<id>.deployed.yaml` (404 when there is none - a bundled graph or set name is therefore 404 too, since the hot folder
never holds a record for one), removes the set's graphs from the registry in one swap, deletes the record first and
then the files it named, logs one INFO line, and answers 200 with the set, its version, its SHA-256 and the graph ids
removed. The order matters for the other instances (3.8): an instance that looks for the record next finds none and
never needs the files.

**List** answers the record as it stands, for the dashboard and for an operator with `curl`:

```json
{ "location": "file:/tmp/deploy",
  "sets": [ { "set": "settlement", "version": "1.4.0", "sha256": "9f2c...", "deployed_at": "...",
              "graphs": ["settlement", "settlement-review"], "status": "deployed" },
            { "set": "pricing", "version": "0.9.0", "sha256": "c77e...", "deployed_at": "...",
              "graphs": ["pricing", "pricing-quote"], "status": "not deployed - graph id pricing-quote is owned by classpath:/graph" } ] }
```

Status codes, in one place: 200 done; 400 the package or a graph is refused, the set is not named after its main graph,
or the digest does not match; 404 undeploy of a set the folder does not hold, and a subgraph asked for at
`/api/graph/{graph-id}` or `/api/openapi/{graph-id}` (D15, the compiled-or-404 shape); 409 a graph id is owned by a
manifest or another set; 404 the switch is off (the function is unregistered and the entry not served - found in the
live drive, 2026-11-25; the draft said 503); 503 the switch is on but the folder was refused at startup, so hot
deployment is off for this run. The
deploy and undeploy functions serialize their work on one `ReentrantLock` over the folder, the record and the registry
(the project's lock rule while the toolchain is Java 21), so two administrators cannot interleave.

### 3.5 Who owns a graph id

The rule at startup is later wins, across manifests. At run time it would be wrong: an upload that lists a bundled
id would replace a graph the build pipeline certified, and undeploying that set later would have to bring the bundled
copy back. The draft forbids undeploying a bundled graph; shadowing one is undeploying it by another name. So:

- **A configured manifest owns its ids.** A set that holds an id a manifest's graph or set registered is refused at
  upload (409) and skipped at reconcile (ERROR in the log, `not deployed` in the record's status). A rebuild that adds
  a bundled graph whose id a hot set holds therefore takes that set out at the next start, loudly; the operator
  undeploys or renames.
- **One hot set owns an id.** A set that holds an id another hot set holds is refused (409), because undeploying either
  set would otherwise break the other.
- **A set replaces its previous version.** An upload whose set name the record already holds replaces that set all or
  none: the new graphs register, the ids the new version no longer holds leave the registry, the old files are removed,
  and the answer says what was replaced. The version field is the manifest's text, recorded and never compared; the
  bytes decide whether anything changed.

**Ids, and a self-contained set (the maintainer's question of 2026-11-24).** A graph id is unique across the whole
engine - the registry is one flat namespace and the REST path is `/api/graph/{graph-id}` - so the rules above are
enforced, not recommended: two sets cannot both carry an id, and the second is 409. Since round 3 a set name is itself
a graph id (3.3), so set names and graph ids share that one namespace, and "the main graph id must be unique" is the
same rule. Inside one set the ids are unique
by the package format itself (the packager refuses a duplicate entry name, and a root's `name` must equal its entry
id). What stays a design rule, declared in the guide: a set is **self-contained** - its main graph and its subgraphs
carry ids no other set uses, a set-name prefix being the easy convention - so that all or none means the set holds
everything it needs. A subgraph two sets need is copied into each under its own id, or deployed as a set of its own
that both then depend on; the second shape is allowed, as delegating to a bundled graph through `graph.extension` is,
and the deploy answer makes the dependency visible (D9).

**Why one owner is necessary, not a safeguard to relax (the maintainer's follow-up question).** Reuse across sets is
by reference, never by containment. Were a second set allowed to carry an id another set holds, even with identical
bytes, a graph would have no single owner, and three things would lose their one answer: undeploying one set would
have to leave the shared graph in place for the other, so the registry would count references and the record would
show two owners; a new version of one set that changes the shared graph would either change the other set's behaviour
silently - its certified bytes no longer describing what runs - or be refused, and then the shared graph could never be
updated without undeploying the other set first; and the question which certified artifact a graph runs from, the one
the dashboard and the audit ask, would have two answers. The engine's one set and version per graph is that answer.
A private copy is renamed before packing - the root `name`, the file name and the `extension=` references in the main
graph - which the Playground's import, rename and export do in a few commands; a reference left on the old id appears
in the deploy answer as an unknown target outside the set, at deploy time rather than at the first run.

**Where reuse does happen: at the function boundary (the maintainer, the same exchange).** A graph has one owner; a
composable function has as many callers as name its route. A `graph.task` node names a route, every graph in every set
may name the same route, and the function knows none of them - the engine's founding invariant (ADR-0001), and the
point of the architecture recommendation in 3.9: the shared utilities of graphs are functions, bundled in the engine or
served by a language-pack sidecar through the Event-over-HTTP map, never shared graph files. The deploy answer's route
list is that reuse surface made visible at deploy time. One consequence, declared: a function's contract change
reaches every graph that names it through the function's own release lifecycle, not through any set's deployment.

### 3.6 One swap per deployment

`CompiledGraphs` becomes a holder of one immutable snapshot - the models, the locations, the sets and the deployed
locations in one record - behind an `AtomicReference`. A read is a map lookup on the current snapshot, as fast as
today's `ConcurrentHashMap.get`, preceded, when either switch is on and the id is not a manifest's, by the record
check of 3.8. A write builds the next snapshot and publishes it once. Startup keeps its semantics
(each manifest's graphs and sets register in order; the later-wins rule stands); a hot deploy or undeploy is one
publish, so a run sees the whole previous set or the whole new one and never a mixture. The Rust registry does the
same under its write lock, one struct instead of several maps.

In-flight runs are unaffected by construction: the executor deep-copies the model when a run starts
(`GraphExecutor.java:579`), and `graph.extension` resolves its target when it reaches that node. Two consequences are
declared in the guide, not engineered around:

- A **suspended** run resumes on the model that is current at resume time. After its set was replaced, the resume
  continues on the new model, which must still hold the checkpoint's node aliases; after its set was undeployed, the
  resume answers 404 like any request for an unknown graph. A set that may hold suspended runs is replaced with its
  checkpoint aliases kept, or after the runs are drained; no drain is built.
- An **asynchronous extension** (`graph.extension async=true`) that started before an undeploy finishes on the copy it
  took; a `for_each` fan-out resolves the target once per iteration, so a late iteration can meet 404 and route to its
  `exception=` handler, as a missing target does today.

### 3.7 The record, the restart and the folder

The hot folder holds, per set, the package `<id>.pack`, one unpacked `<graph-id>.json` per graph (the gate reads them
back through `ConfigReader` so `${...}` references resolve for this environment, and discovery lists them as it lists
a manifest's folder), and one engine-written record named after the set's main graph, `<id>.deployed.yaml`. There is
no global record: the record is both the set's deployment record and its revision marker, which is what the maintainer
asked for in round 3 when he keyed the reference on the main graph id.

```yaml
# Written by the hot-deploy functions - rewritten at every change of this set; do not edit
set: 'settlement'
version: '1.4.0'
sha256: '9f2c...'
deployed_at: '2026-11-24T18:02:11.412Z'
graphs:
  - 'settlement'
  - 'settlement-review'
files:
  - 'settlement.pack'
  - 'settlement.json'
  - 'settlement-review.json'
```

Every change writes in one order - the set's files, then the record, by rename - so an instance that finds a record
finds complete files behind it; an undeploy deletes the record first, so an instance that finds no record never needs
the files. **Reconcile** is one idempotent procedure over one set, run at startup for every record the folder holds,
after a deploy or undeploy of that set on this instance, and on any instance whose record check (3.8) finds the set's
record changed or gone:

1. read the record; read `<id>.pack` and compare its SHA-256 with the recorded one: a match that is not registered on
   this instance, or is registered from different bytes, is loaded, unpacked, gated and registered as the loader does
   it, all or none; on a mismatch the record is read once more, since a deploy may have moved the package before the
   record, and a mismatch that persists unregisters the set if it was registered and logs an ERROR, because the bytes
   are not the bytes the record vouches for;
2. a set registered from the hot folder whose record is gone leaves the registry (another instance undeployed it);
3. a `.pack` or `.json` in the folder that no record names is ignored with a WARN. The records are the allowlist; the
   endpoint wrote them; a hand-copied file is not a deployment.

So a developer cannot drop a graph into the hot folder: the files and the record are written by the endpoint, and only
a set a record names, with the SHA-256 the record vouches for, is ever read (the maintainer confirmed this reading on
2026-11-24). A record written by hand is write access to the volume, which is the installation's perimeter, as it is
for the jar itself. The developer's path to a running engine is the Playground and a dry run, then one `curl` to the
deploy endpoint of the developer's own engine with the switch on, or the 4.12.19 lane with a restart.

A restart therefore restores exactly the sets whose records the folder holds, from the bytes they vouch for, through
the same gate, and a persistent volume is all the installation needs. The reconcile acts on sets, never on single
graphs: a set is registered, replaced or dropped as a unit, in one swap, so ADR-0027's all-or-none rule holds on every
instance. The loader's pieces - `load`, `gate`, `register` - become package-private helpers that the startup loader
and the hot-deploy functions share, so the strict read, the SHA-256, the `${...}` resolution and the all-or-none rule
are one code path; the startup loader keeps its own generated manifest for the `unpack` folders of ADR-0027, which
this design does not touch.

### 3.8 Several instances: on-demand registration through the set's own record

The draft describes one engine. A field installation runs several replicas behind a service that round-robins, so an
upload lands on one instance and the others must learn of it. Round 1 recommended a periodic check of a record. The
maintainer's direction in round 2 was on-demand registration instead: no poll, no thread, no interval; when a request
names a graph id the registry does not hold, look in the hot folder and register it if it is there, and when a
registered graph's file is gone, unregister it - one read of file metadata per request. Round 2 refined the trigger
from the graph's file to one global revision marker, because the per-graph check has two gaps:

- **Replacement goes unnoticed.** A re-upload keeps every `<graph-id>.json` in place, so the file exists before and
  after; the other instances keep the old version until they restart. A per-file timestamp would catch it, but then
  each graph of a set refreshes only when it is next requested, so a set with `graph.extension` cross-references runs
  half old and half new on an instance - the all-or-none rule of ADR-0027 and the one swap of D6 are lost.
- **The folder becomes the allowlist.** A graph file copied into the folder by hand would be executable at the next
  request. The gate would still run, so it is not the unvalidated lazy load ADR-0011 removed, but it is the hot-drop it
  ruled out, and D7 made the record the allowlist for that reason.

In round 3 the maintainer keyed the reference on the set's main graph, and that is the form taken: **the set's own
record, `<id>.deployed.yaml`, is the marker** (3.7). It removes round 2's global file, and with it the one race round
2 had to declare - two administrators deploying different sets no longer write the same file - and a change to one set
never costs another set a read.

**The mechanism, in the registry's own lookup.** When either switch is on, a lookup for an id that no configured
manifest owns goes one of two ways. A **registered hot-deployed id** belongs to a set the registry knows, so the
instance reads that set's record: the same SHA-256 as the registered copy means serve; a different one, or no record,
means reconcile that set under the lock (3.7) and look up again - the requesting graph gets the current state within
its own request, a replaced set is swapped before it runs, an undeployed set answers 404. An **unknown id** is either
a set's main graph that another instance deployed, or nothing: the instance checks whether `<id>.deployed.yaml`
exists - the maintainer's `file.exists()`, now on the record - and loads the set if it does, 404 if it does not. A
subgraph never needs the second path, because a subgraph is reached only from its own set's main graph (D15), which
is registered whenever the subgraph is. A manifest's id never reads anything, since a bundled graph cannot change at
run time (D4), so bundled traffic pays nothing. The record's content decides, not a timestamp, so the check does not
depend on a volume's clock; it is written last, so a reader never sees a record ahead of its files.

What this gives over the per-graph check, at the maintainer's cost of one small read per request:

- a replaced set is caught, and replaced as a unit in one swap;
- the records stay the allowlist: a hand-copied file is still ignored with a WARN;
- one check site covers every reader - the executor, `graph.extension`, the resume path, the OpenAPI endpoint,
  `list graphs` - because they all read the registry;
- a refused set is gated once per record, not once per request, since a reconcile runs only when the record moved;
- an unknown id that no record names costs one existence check and nothing more;
- convergence is per set: nothing happening to set B is ever read on a request for set A.

Undeploy keeps the draft's semantics: the instance that undeploys deletes the set's record and then its files; every
other instance drops the set at its next request for one of its graphs.

**Two things declared, not engineered.** First, real time is the volume's: on a local disk a change is visible at once;
on a network volume with attribute caching (an NFS-backed persistent volume, for one) an instance may read the old
record for the cache window, typically seconds - better than a poll's interval, and the volume's property, not the
engine's. Second, two administrators deploying the same set at the same moment can leave a record and a package from
different uploads; the reconcile's second read (3.7) catches the ordinary case, and a mismatch that persists unregisters
the set with an ERROR that the list endpoint shows, until either administrator's next upload. For an installation
where a metadata read is a network round trip on a busy graph, `graph.hot.deploy.throttle` (ms, default `0`) sets the
least time between two record reads for the same set; at the default the record is read at every such request, as
the maintainer proposed.

Alternatives on the table and not taken: a periodic check of a record (round 1's option B - a thread and an interval
to tune, and a latency of its own); one global marker for the whole folder (round 2's form - every instance re-read
the whole record on any change, and two administrators wrote one file); a broadcast over the service mesh (works
where `cloud.connector=kafka` is on; the mesh is opt-in and most installations do not have it, ADR-0006). The gateway
or the dashboard calling every instance stays possible for a headless service and is nothing the engine has to build.

### 3.9 The architecture recommendation, and what the engine does about it

The recommendation stands on its own: an engine that holds no user-defined functions never restarts for a graph, and
the graphs' functions live in language-pack sidecars (`localhost`) or containers (cluster DNS) reached through the
Event-over-HTTP map, which exists today. For a field with hundreds of tools and MCP bindings this is the only shape that
scales, because the engine's deployable artifact stops changing. The engine's part is small:

- **A deploy-time advisory, never a refusal (D9).** The answer lists the routes the set's `task` properties name,
  split into those this instance knows (its registry or its Event-over-HTTP map) and those it does not, and the
  `graph.extension` targets the set names outside itself, known or unknown (3.5); a target that is another set's
  subgraph, when that set is known here, is reported as private with the main graph to reference instead (D15). A refusal
  would create an ordering dependency between a graph's deployment and its sidecar's, and it would be a partial gate - a
  route known now can be gone at run time; the project's posture is that constraints are declared, not enforced
  halfway. The agent loop's pre-flight check stays where it is, at run time.
- **A guide section** (the knowledge-graph AI agent guide, beside *Deploy without a rebuild*): the three folders, the
  lifecycle table of 3.3, the ownership rules, the two declared consequences of 3.6, and the recommendation as design
  guidance for an installation that adopts hot deploy.
- **A task's provider on the node** (round 4, 3.10): `server.url` and `server.header[]`, because the Event-over-HTTP
  map is loaded once at startup and a new function in a sidecar would otherwise need the restart this sprint removes.

### 3.10 A task's provider on the node: `server.url`

The recommendation of 3.9 has a consequence the draft of round 4 names (the maintainer, 2026-11-24): with no
user-defined function in the engine, a hot-deployed graph that needs more than `graph.math` and `graph.data.mapper`
calls a custom composable function in a language-pack sidecar or container, and the engine's way to reach a remote
route - the Event-over-HTTP map, `yaml.event.over.http` - is loaded once at startup. A function the map has never
heard of cannot resolve a provider address without the restart this sprint removes. So the node names its provider:

```text
skill=graph.task
task=my.custom.function
server.url=${TARGET_UTIL_SERVER}/api/event
server.header[]=authorization: Bearer ${UTIL_TOKEN}
```

- **`server.url`** is the provider's Event-over-HTTP endpoint, with the usual `${...}` substitution. A URL without a
  path gets `/api/event`; a URL with a path is used as written, which is how an API gateway's own path reaches the
  target (the gateway reformats it before the provider sees it). Node properties are a multi-level map, so `server` is
  the parent label and the endpoint is its `url` leaf - the maintainer's adjustment, since a label cannot be both a
  value and a parent.
- **`server.header[]`** (optional) adds transport headers to the HTTP call, `name: value` with substitution, the
  shape the static map's `headers` have: a gateway's bearer token travels here, and the graph holds the variable's
  name, never the secret. The event's own headers from `header.*` input mapping are unaffected: they travel inside the
  envelope to the function, as they do today.
- **Resolved per instance at registration.** The gate reads the graph back through `ConfigReader`, so the references
  resolve from each pod's own environment when the set is registered, once, not per call. One variable per provider
  serves every function it hosts.
- **Precedence.** `server.url` wins over a local function and over the static map, as the map wins over a local
  function today: explicit beats implicit. A developer who wants a local mock leaves `server.url` unset. The static
  map is unchanged and remains the installation-wide binding; the node's binding is the graph's own, and the two
  coexist.
- **The call.** The skill already issues a single task through `PostOffice.eRequest` on the worker thread so the
  outbound event carries the node's span; the same method takes the headers and the endpoint, so the change is one
  argument, and the trace flows into the pack as every Event-over-HTTP call's does. The skill's reachability check -
  a route must be local or in the map - is skipped when `server.url` is set: the provider decides, and a wrong route
  comes back as its 404, the node's error, which `exception=` handles like any failure. The deadline travels as
  `X-TTL`, as today. `for_each` has no endpoint form of the batch request, so it becomes one call per element under
  the existing concurrency window: the same semantics, different plumbing.
- **The gate.** `server.url`, when present, is an absolute `http` or `https` URL after resolution, else the graph is
  refused: an unset variable leaves `/api/event` with no host, which would fail at the first run. The
  `${TARGET_UTIL_SERVER:http://127.0.0.1:8086}` default form covers a developer's machine at pack time. Each
  `server.header[]` entry is `name: value`. Both properties are accepted wherever `task` is consumed - `graph.task`,
  and by the same rule `graph.suspend` and `graph.resume`, so a pack can host a state store - and refused on any
  other skill, as `ttl` placement is.
- **The advisory.** The deploy answer reports a task with `server.url` as remote, with its resolved endpoint, in place
  of known or unknown: the provider decides, and the advisory says where.
- **Declared.** A certified graph is trusted content, as the fetcher's `url` is: no allowlist of providers. A
  provider that is down fails the node as any task failure does.

Recorded as D16; the Rust twin's `graph.task` takes the same two properties over its Event API client, whose endpoint
split already supplies `/api/event` for a URL without a path.

### 3.11 The developer's two surfaces: a deployment panel in dev mode, two scripts without it

The field's dashboard stays the field's (3.12), but a developer prototyping on a single engine, and an operator of an
engine that has no Playground, each need a way to use the three endpoints without hand-writing an HTTP call. Round 5
(the maintainer, 2026-11-24) adds one surface for each case; both are clients of the same endpoints, and the engine
changes nothing for them.

**In dev mode: a deployment dialog in the Playground.** The packaging panel (`GraphSetPanel`, its hook
`useGraphSetPanel`) already assembles a set from dropped or imported graph files with manifest key-values, has the
engine pack it (`POST /api/graph-set/pack`, the gate running there) and downloads `<set>.pack`, and inspects a
dropped package through the unpack endpoint. It gains a **Deploy** dialog with three parts:

- **Deploy a set.** Either the assembled set in one step - pack on the engine, then post the bytes to
  `POST /api/graph-set/deploy`, the download optional - or a dropped `.pack`. The panel computes the package's
  SHA-256 in the browser (Web Crypto) and shows it, so the digest can be written to the field's ledger from the same
  screen; an optional *expected digest* field, pasted from the ledger, is sent as `X-Graph-Set-SHA256`. The answer is
  shown as the endpoint gives it: the set, its version, its graphs, what was replaced, the route and extension
  advisory; a refusal shows every reason (400), the owner of a colliding id (409), or the hint that hot deploy is off
  on this engine with the two property names (503).
- **Deployed sets.** `GET /api/graph-set/deployed` as a table - set, version, digest, deployed at, graphs, status -
  with a refresh and an **Undeploy** action per row behind a confirmation that names the set and its graphs, calling
  `POST /api/graph-set/undeploy/{graph-id}`.
- **The hint when the switches are off.** The panel's first read of the list tells it whether this engine hot
  deploys; off, the dialog says so and names `graph.hot.deploy` and `graph.hot.undeploy`, and nothing else is
  offered.

Dev mode only by construction: the Playground's web app is served only under `app.env=dev` (2.3), while the three
endpoints are production entries the panel calls like any client, same origin, no CORS. The help browser gains
`help deploy.md`, and `help package.md` points at it from its *Deploying a set* section. The webapp is one source for
both engines, so the Java bundle is released first (`npm run release`) and both bundles once the Rust twin's
endpoints exist (`npm run release:all`), as the day plugins did. Tests follow the panel's own: the hook against a
mocked `fetch` (deploy, replace, refuse, 503, list, undeploy), the component's rendering of each answer.

**Without dev mode: two Node template scripts.** In `templates/starter-graph/scripts`, beside the AI companion
broker and under the same rules - zero dependencies, Node.js 26 LTS (minimum 22, the folder's floor), this folder the
only copy, which the Rust repository carries byte for byte:

```bash
node scripts/deploy-graph-set.mjs --engine https://gateway.example.com/engine --file settlement.pack \
     --sha256 9f2c... --header "authorization: Bearer $DEPLOY_TOKEN"
node scripts/undeploy-graph-set.mjs --engine https://gateway.example.com/engine --graph settlement
node scripts/undeploy-graph-set.mjs --engine https://gateway.example.com/engine --list
node scripts/deploy-graph-set.mjs --file settlement.pack --digest
```

- `--engine` is the engine's base URL, or the gateway's path in front of it, to which the script appends the entry's
  path; `--file` the package; `--sha256` the ledger's digest, sent as the header; `--header` a transport header,
  repeatable, the token taken from the shell's environment so it never sits in a command history as a literal;
  `--digest` prints the package's SHA-256 and exits, the ledger step without an upload; `--list` prints the deployed
  sets; `--graph` the main graph id to undeploy.
- The answer is printed as the engine gives it, one JSON document; the exit code is 0 on 200, 1 on a refusal with the
  engine's reasons on stderr, 2 on a usage error. No prompt: a script in a pipeline runs unattended.
- The scripts' README section states what the folder's does for the broker: the command, the flags, the exit codes,
  and that the gateway's authentication is the installation's.

Recorded as D17; the live drive (WP5) uses both surfaces - the panel against the example engine in dev mode, the
scripts against an engine started with `app.env` unset.

### 3.12 Out of scope, with the seams left for it

- **The certification means.** The ledger or the signature is the field's; the engine records and compares a SHA-256
  when asked (3.4 step 2). A **verifier route** - `graph.hot.deploy.verifier=<route>`, a composable function the field
  provides that receives the bytes and the digest and answers 200 or a refusal - is the seam for a signature check
  inside the engine's request path. It is named here and not built (Q7).
- **Authentication.** The gateway's. `rest.yaml`'s `authentication` option is available to an installation that wants
  an in-engine check on these entries, unchanged.
- **The operational dashboard.** The field's; the list endpoint is its read, the two POSTs its writes.
- **A Playground button** was out of scope in rounds 1 to 4 (Q8); round 5 brought it in as the deployment dialog of
  3.11, with the two scripts for an engine that has no Playground.
- **Identity in the log.** The log line carries the remote address; who approved is the dashboard's record.

---

## 4. Impact on existing decisions

| Decision | Impact | How it is recorded |
|---|---|---|
| **ADR-0011** - CompileGraph is the mandatory gate; "hot-dropping a JSON file into the deploy folder no longer works (deployment is an explicit, restart-scoped act)"; the lazy per-request loading of models removed | **Amended.** Deployment stays explicit and gated - the only doors are the manifest at startup and the deploy endpoint at run time, both through `GraphModelGate` - but it is no longer restart-scoped for a graph set uploaded under the switch, and a set may register at first use on an instance that did not receive the upload (3.8), still only through the record and the gate. Hot-dropping a file still does nothing: the record is the allowlist. Compiled-or-404 stands unchanged. | the new ADR's status line names ADR-0011 as amended; ADR-0011's status line gains `amended by ADR-NNNN`, as ADR-0010's did for ADR-0012 |
| **ADR-0027** - graph sets, gated when packed and deployed all or none | **Extended.** The set becomes the unit of run-time deployment too; a hot-deployable set is named after its main graph and exposes it alone (D14, D15), two rules a startup-lane set is not asked to follow - its name stays free and its graphs public, so nothing bundled changes; the loader's pieces are shared. No decision of ADR-0027's D1 to D9 changes. | the new ADR's status line names ADR-0027 as extended |
| **ADR-0006** - the service mesh is opt-in | **Respected.** Option C of 3.8 cannot be the default path. | none |
| **`event-api-local-routes-only`** (invariant) - an instance's `/api/event` serves the routes it reaches itself and never relays | **Respected.** `server.url` is caller-side knowledge on the node, as the static map is; the provider still serves its local routes only. | none |
| **The Event-over-HTTP map** (`yaml.event.over.http`, loaded once at startup) | **Unchanged, with a sibling.** The map stays the installation-wide binding; a task node's `server.url` is the graph's own binding and wins where both exist (3.10). | the new ADR, and the skills reference |
| **ADR-0033, ADR-0035, ADR-0036** - the agent loop, the audit store, the tools | **None.** An agent graph hot-deploys like any set; its catalog rows travel in the graph; the packs' routes are the routes it names. | none |
| **ADR-0023** - the claims fixture | **A claim per rule.** `graph-manifest-list-later-wins` has a claim on both engines; the ownership rule and the one-swap rule get theirs. | `claims-registry.json` in WP3 |
| **The Vision** - graph models promote through dry-run, certify, stage, approve, production | **Served.** The production step gains its mechanism without a rebuild; the Blueprint's queued item, the promotion of an AI graph through the governance lifecycle, is what this sprint builds the engine side of. | the Blueprint thread, at the sprint's close |

---

## 5. Decisions - accepted by the maintainer on 2026-11-24 as recommended, D13 to D17 from his direction

The maintainer's words in round 2: "The defaults are fine except: several instances - instead of polling, we should
allow on-demand graph registration." In round 3: "a graph set must have at least one main graph and zero to more
subgraphs. Only the main graph ID is used at the /api/graph/{graphId} service endpoint. Therefore, the revision marker
can use this main graphId as the reference. The main graphId must be unique", then "or we can enforce the name of the
graph set to be the same as the main graph ID?" In round 4, after PR #671 merged: "please add a feature update in the
graph.task node to support a new property that holds the provider URL", then, on the review, "Yes, fold it in" with
the name `server.url`, since node properties are a multi-level map. In round 5: "for easy prototyping, please update
the react UI to support hot deploy into the minigraph-playground and hot undeploy from a dialog panel. when dev mode
is disabled, please provide a template command line script (e.g. node mjs) to upload a graph set into a target engine
and another one to undeploy from the target engine." D1 to D12 stand as written; D13 to D17 record the directions and
their refinement.

- **D1. The unit is a graph set.** `.pack` only; a single graph is packed alone. A loose JSON upload is not offered.
- **D2. Both switches default to `false`; either switch on reads the folder at startup.** Default on would turn a
  write path on in every field installation at the next upgrade; the project's precedent for an operational choice is
  off, with the installation deciding per environment. The alternative is default `true` with the field setting `false`,
  as the draft's last section can be read.
- **D3. The folder key is `location.graph.deploy`, default `file:/tmp/deploy`**, beside `location.graph.temp`. The
  retired `location.graph.deployed` still warns at startup; the two names are close, and the alternative is
  `graph.hot.deploy.location`, which groups with its switch (Q3).
- **D4. Ownership as 3.5:** manifests own their ids; one hot set owns an id; a re-upload of the same set name replaces
  the set all or none; collisions are 409.
- **D5. Integrity: the optional `X-Graph-Set-SHA256` header, mismatch 400;** the record and the log carry the SHA-256;
  the verifier route is a named seam, not built.
- **D6. One snapshot, one swap** per deployment, both engines.
- **D7. The record is the allowlist** at reconcile; a hand-copied package is ignored with a WARN.
- **D8. A refused upload leaves no files;** the startup loader keeps leaving a refused set's files for inspection, as
  ADR-0027 built it.
- **D9. The route advisory** in the deploy answer: report, never refuse - the `task` routes, and since the maintainer's
  question on ids (3.5) the `graph.extension` targets outside the set, each known or unknown to this instance.
- **D10. The list endpoint** ships with the sprint, under either switch.
- **D11. Suspended runs and asynchronous extensions across a replace or an undeploy are declared** (3.6), not drained.
- **D12. The Rust twin lands in the same sprint,** since the engines release in lock-step and both share the loader's
  shape; the Playground packaging panel is already on both.
- **D13. Several instances converge on demand through the set's own record, named after its main graph (3.8):** no
  poll, no broadcast, no global file; one small read per request to a hot-deployed or unknown id, that set reconciled
  when its record moved or vanished, always as a unit. The maintainer confirmed in round 2 that this keeps all or
  nothing per graph set, and in round 3 keyed the reference on the main graph id, which removed round 2's global marker
  and the last-writer race it carried. Replaces round 1's option B and its `graph.hot.deploy.sync` interval;
  `graph.hot.deploy.throttle` (default `0`) is the one knob, for a volume where a metadata read is a round trip.
- **D14. A hot-deployable set has exactly one main graph and is named after it (3.3):** the set name is the main
  graph's id, a set with no graph of that id is refused (400), and `graph_id`, when present, must equal it. One string
  is the handle at every surface. A startup-lane set keeps ADR-0027's freedom of name.
- **D15. A hot-deployed set's subgraphs are private:** `/api/graph/{graph-id}` and `/api/openapi/{graph-id}` answer
  404 for one (the compiled-or-404 shape), a `graph.extension` from another set is refused with the main graph named,
  and only the set's own graphs reach them. Enforced at both edges, since both are in the engine (the executor already
  tells an HTTP entry from an extension's dataset), so it is a whole gate, not a partial one. A startup-lane set keeps
  every graph public, as today.
- **D16. A task node names its function's provider with `server.url`, and its transport headers with
  `server.header[]` (3.10):** an absolute `http` or `https` URL after `${...}` resolution, `/api/event` supplied when
  the URL has no path, resolved per instance at registration; it wins over a local function and the static map; the
  reachability check is skipped and the provider's 404 is the node's error; `for_each` calls per element; accepted
  wherever `task` is consumed and refused elsewhere; a certified graph is trusted content, so no allowlist. The
  maintainer named the property: `server` is the parent label of the node's multi-level map, so the endpoint is
  `server.url`. The alternative, a configured server alias with URL and headers, keeps the URL out of the graph but
  needs an engine configuration change per server; a reloadable map would be a second artifact to coordinate with
  each set. Neither is taken.
- **D17. Two developer surfaces for the same three endpoints (3.11):** in dev mode a **Deploy** dialog in the
  Playground's packaging panel - pack and deploy the assembled set or a dropped package, the digest shown and an
  expected digest sent as the header, the deployed sets listed with an undeploy behind a confirmation, the switches'
  state told when they are off - and, for an engine without the UI, `deploy-graph-set.mjs` and
  `undeploy-graph-set.mjs` in `templates/starter-graph/scripts`, zero dependencies, the folder's Node floor, the
  only copy, with `--engine`, `--file`, `--sha256`, `--header`, `--digest`, `--list` and `--graph`, the answer as the
  engine gives it and exit codes a pipeline can read. The engine changes nothing for either. Reverses Q8.

## 6. Open questions - answered by the maintainer on 2026-11-24

Every recommendation below was accepted as the default ("The defaults are fine except..."); Q1 was answered with the
on-demand direction that became D13. The questions stay as asked, with the answer after each.

- **Q1. Several instances (3.8):** build option B now, after the single-instance drive, or not in this sprint? The
  record is designed for it either way. Recommendation: after the drive, in the same sprint if the drive is clean.
  **Answer:** neither - on-demand registration, no poll; refined into the revision marker of 3.8 and accepted (D13).
  It is part of the sprint, not an increment after it.
- **Q2. The switch defaults:** `false` (D2) or `true`? **Answer:** `false`.
- **Q3. The folder key's name:** `location.graph.deploy` (D3) or `graph.hot.deploy.location`? **Answer:** `location.graph.deploy`.
- **Q4. Who may call the endpoints:** the gateway alone, with the engine adding nothing (recommended), or an in-engine
  check through `rest.yaml`'s `authentication` on these three entries in the example? **Answer:** the gateway alone.
- **Q5. Replacing a deployed set:** silently, with `replaced` in the answer (D4, recommended), or only with an explicit
  query parameter such as `?replace=true`, a 409 otherwise? **Answer:** silently, with `replaced` in the answer.
- **Q6. The undeploy verb:** `POST /api/graph-set/undeploy/{set}` as the draft says (recommended, since dashboards and
  gateways treat POST uniformly), or `DELETE /api/graph-set/deploy/{set}`? **Answer:** POST; since round 3 the path
  segment is the main graph id, which is the set name.
- **Q7. The verifier route:** a seam only (D5), or built in this sprint for the signature case? **Answer:** a seam only.
- **Q8. A Playground action** "deploy to this engine" in the packaging panel, for a developer's single engine in dev
  mode with the switch on: this sprint, later, or never? **Answer:** not this sprint (the recommendation was to leave it out; nothing was ruled for later). **Reversed in
  round 5 (the maintainer):** in this sprint, as the deployment dialog of 3.11 for dev mode, with the two Node template
  scripts for an engine without the UI (D17).
- **Q9. The live drive's set.** The example bundles every tutorial (`tutorial-1` loose, `tutorial-2` to `-14` as the
  `tutorials` set), so a hot-deployed copy of any of them is a 409 by D4 - which is itself a good negative control. The
  positive case needs a set the example does not bundle: a `hot-demo` set built in the Playground from a renamed
  tutorial, packed, uploaded, called, re-uploaded as a new version, undeployed, and shown to survive a restart.
  **Answer:** as proposed; since round 3 the set is named after its main graph, so the drive's set is `hot-demo` with
  a main graph `hot-demo` and one subgraph, which also exercises D15.
- **Q10. Should a startup-lane set that happens to be named after one of its graphs follow D14 and D15 later,** so that
  the two lanes have one rule? Not in this sprint: bundled graphs are unchanged by the maintainer's draft, and the
  change would need an upgrade note for any field pack whose subgraphs are called at REST. Left open for a later
  release.
- **Q11. The transport headers' form (round 4):** a list property on the node, or a configured server alias with URL
  and headers? Recommendation: the list property, so the graph holds the variable's name and the engine's configuration
  does not change per server. **Answer:** the list property, as `server.header[]` beside `server.url`.

## 7. Implementation plan

| WP | Scope | Validation |
|---|---|---|
| WP0 | The RFC entry in `docs/arch-decisions/RFC.md` from this spec after the rulings; the open thread. **Done 2026-11-24:** RFC-0017, thread `hot-deploy-graph-sets`. | the AI contract provider's tests (`RFC.md` is packaged) |
| WP1 | Java engine, no endpoint yet: the three keys; `CompiledGraphs` as one snapshot behind an `AtomicReference` (startup semantics unchanged); the loader's `load`, `gate`, `register` and manifest writer shared; a `HotDeployment` class with the folder checks, the per-set records, the reconcile, the lock and the record check the registry's lookup calls; `CompileGraph` calls the reconcile when either switch is on; `checkUnpack` refuses the hot folder. | `CompileGraphTest` and `GraphSetLoaderTest` green unchanged; new tests: a record restores its sets at startup, a changed package is unregistered, a stray package is ignored, a bundled id refuses a set, one swap is observed by a reader, a second registry over the same folder (two instances in one test) sees a deploy, a replace and an undeploy at its next lookup and never half a set |
| WP2 | Java endpoints: `deploy.graph.set`, `undeploy.graph.set`, `list.deployed.sets`, `@OptionalService` on their switches; `rest.yaml` entries in the engine, the example and the starter-graph template; the main-graph naming check (D14); subgraphs private at the REST edge, the OpenAPI endpoint and a cross-set `graph.extension` (D15); the digest header; the route and extension advisory; `server.url` and `server.header[]` on the task-consuming skills (D16) - the endpoint call, the per-element fan-out for `for_each`, the validator's placement and URL rules, the advisory's remote entry. | tests through the REST lane as `GraphSetLoaderTest` drives it: deploy then call the main graph, 404 for a subgraph at REST and at OpenAPI, a cross-set extension refused and an in-set one served, 400 for a set not named after its main graph, 400 with reasons and no files left, 409 on each ownership case, replace, undeploy then 404, list; 503 with the switches off; a task with `server.url` served by a test peer over Event-over-HTTP, with a header, with `for_each`, a wrong route's 404 as the node's error, an unresolved URL refused by the gate, the property refused on another skill |
| WP2c | The Playground's **Deploy** dialog (3.11): the hook and the component beside `GraphSetPanel`, the digest in the browser, the deployed-sets table with undeploy, the switches-off hint; `help deploy.md` and the pointer in `help package.md`; the Java bundle released (`npm run release`), both bundles once WP4 lands. | the hook against a mocked `fetch` (deploy, one-step pack-and-deploy, replace, refusal with reasons, 409, 503, list, undeploy), the component's rendering of each answer; `npm test` |
| WP2d | The two scripts in `templates/starter-graph/scripts` with their README section (3.11): `deploy-graph-set.mjs` and `undeploy-graph-set.mjs`, zero dependencies, the folder's Node floor. | run against the example engine with the switches on: a deploy, a refused upload, `--digest`, `--list`, an undeploy, and each exit code |
| WP3 | Docs: the configuration reference (three keys), the AI agent guide's new section and the deploy-without-rebuild section's cross-reference, `build-your-first-graph.md` step 4, the Playground help `package.md`, `graph-task.md` and the new `deploy.md` (a help edit releases the bundle), the scripts README, the skills reference's `graph.task` section and the command reference for `server.url` and `server.header[]`, the reserved names page (the record file), the claims for the two rules, CHANGELOG Unreleased with an upgrade note (off by default, no action). | `python3 scripts/check-doc-canon.py`; the AI contract provider's tests; the pull request's `docs` run |
| WP4 | The Rust twin: `graphs.rs` as one struct under one lock, `graph_set_loader.rs` shared pieces, the reconcile, the three handlers in `rest.rs`, `server.url` and `server.header[]` in `skills.rs` over the Event API client, the same keys and messages. | the twin tests of WP1 and WP2 |
| WP5 | The live drive on the example engine (Q9): negative controls first (switch off is 503, a bundled id is 409, a tampered body is 400), then the positive path through the Playground's **Deploy** dialog in dev mode, the two scripts against an engine started with `app.env` unset, `curl`, REST calls, a replace, an undeploy and a restart. Record: `docs/test-reports/hot-deploy-live-drive.md`. **Done 2026-11-25** on both engines: a switch that is off is 404 (finding 1), a tampered body is 400 when malformed or when the digest header is sent (finding 2), a provider's function must be public (finding 3). | the record |
| WP6 | The two-instance live drive of 3.8: two example engines on one folder, a deploy on one called on the other at once, a replace, an undeploy, a hand-copied package ignored. Part of WP5's record. **Done 2026-11-25**, the Java and the Rust engine on one folder. | the record |
| WP7 | Promotion: the ADR from this spec and the drive, ADR-0011 marked amended, ADR-0027 marked extended, the RFC entry marked promoted. | the maintainer's acceptance |

## 8. References

- `system/minigraph-playground-engine/src/main/java/com/accenture/minigraph/start/CompileGraph.java` - the gate at startup
- `.../start/GraphSetLoader.java` - the set loader, the generated manifest, `checkUnpack`
- `.../models/CompiledGraphs.java` - the registry
- `.../rest/PackGraphSet.java`, `.../rest/UnpackGraphSet.java` - the dev-mode pack endpoints and the raw-body read
- `.../services/GraphExecutor.java` - compiled or 404; the deep copy at run start
- `.../skills/GraphTask.java` - the task call (`eRequest` on the worker thread), the reachability check, the `for_each` batch
- `system/platform-core/src/main/java/org/platformlambda/core/system/PostOffice.java` - `asyncRequest`, `request` and `eRequest` with an `eventEndpoint`
- `system/platform-core/src/main/java/org/platformlambda/core/system/EventEmitter.java` - the Event-over-HTTP map, consulted before local discovery
- `system/minigraph-playground-engine/webapp/src/components/GraphSetPanel/`, `.../hooks/useGraphSetPanel.ts`, `.../utils/graphSet.ts` - the packaging panel the deployment dialog extends
- `templates/starter-graph/scripts/` - the AI companion broker and its README, the home of the two scripts
- `.../services/GraphCommandService.java` - the temp folder; discovery over the deployed locations
- `system/platform-core/src/main/java/org/platformlambda/automation/services/HttpRouter.java` - 503 for an unregistered route
- `docs/arch-decisions/ADR.md` - ADR-0011, ADR-0027, ADR-0006
- `draft-design-specs/graph-set-packaging-and-deployment.md` - the sibling spec this one extends
- `docs/guides/knowledge-graph/ai-agent-guide.md` - *Rapid prototyping - deploy without a rebuild*
- `docs/guides/configuration-reference.md` - `graph.model.automation`, `location.graph.temp`
- The Rust twin: `crates/knowledge-graph/src/{compiler.rs, graph_set_loader.rs, graphs.rs, rest.rs}` in the `mercury` repository
