- [ ] **Hot deploy and undeploy of knowledge graphs: a certified graph set uploaded to a running engine, registered on
  every instance on demand, removed by name (RFC-0017; the maintainer's sprint of 2026-11-24).** Deployment is a
  restart today (ADR-0011, the 4.12.19 lane included) and nothing undeploys. The draft: bundled graphs and the dev-mode
  workspace unchanged; a third folder `location.graph.deploy` (default `file:/tmp/deploy`) under two switches,
  `graph.hot.deploy` and `graph.hot.undeploy`, both off; a set uploaded to `POST /api/graph-set/deploy` is gated and
  registered at once, `POST /api/graph-set/undeploy/{set}` removes it, `GET /api/graph-set/deployed` lists the
  folder's record; user-defined functions belong in language-pack sidecars so the engine's artifact stops changing.
  Spec `draft-design-specs/hot-deploy-undeploy-graph-sets.md`, five rounds the same day.
  - **Rulings (the maintainer, 2026-11-24):** every recommendation accepted as the default - the set as the unit,
    switches off, manifests own their ids (a bundled id is 409, never shadowed), a re-upload replaces its set all or
    none, the optional `X-Graph-Set-SHA256` header, one registry snapshot swapped per deployment, the record as the
    allowlist, a refused upload leaves no files, the route advisory never refuses, suspended runs declared not
    drained, the Rust twin in the sprint. **Several instances: on-demand registration, no poll** (his direction),
    refined from a per-graph `file.exists()` to one revision marker read per lookup of a hot-deployed or unknown id,
    with a whole-record reconcile when it moved - he confirmed it keeps all or none per set, and that a developer
    cannot drop a graph into the folder: the endpoint is the only door. His question on ids: uniqueness across the
    engine is enforced (409), a self-contained set is the declared design rule, reuse across sets is by reference (a shared set) and never
    by containment - one owner per graph is what keeps undeploy, replace and the certified artifact unambiguous, while a
    composable function is reused by every graph that names its route - and the deploy advisory also lists
    `graph.extension` targets outside the set.
  - **Round 3 (Eric, the same day):** a set has exactly one main graph and zero or more subgraphs; only the main
    graph is used at `/api/graph/{graphId}`; the reference is the main graph id, and the set is named after it. So the
    set's own record `<id>.deployed.yaml` is the marker (no global file, no last-writer race between sets), the
    undeploy path is the main graph id, `graph_id` when present must equal the set name, and a hot-deployed set's
    subgraphs are private at REST, at OpenAPI and to another set's `graph.extension` - enforceable at both edges, so a
    whole gate. A startup-lane set keeps its free name and public graphs.
  - **Round 4 (Eric, after #671 merged):** a task node names its function's provider - `server.url`, the provider's
    Event-over-HTTP endpoint with `${...}` substitution and `/api/event` supplied when the URL has no path, and
    `server.header[]` for a gateway's token - because the static map is loaded once at startup and a new function in
    a sidecar would otherwise need a restart. Reviewed and accepted with the recommendations (D16): resolved per
    instance at registration, wins over local and the map, the reachability check skipped, `for_each` per element,
    refused by the gate unless an absolute URL, accepted wherever `task` is consumed. Eric named the property: node
    properties are a multi-level map, so `server` is the parent and the endpoint is `server.url`.
  - **Round 5 (Eric):** "for easy prototyping", a deployment dialog in the Playground's packaging panel for dev
    mode - pack and deploy the assembled set or a dropped package, the digest shown, the deployed sets listed, an
    undeploy behind a confirmation, the switches' state when off - and, when dev mode is disabled, two Node template
    scripts in `templates/starter-graph/scripts` (`deploy-graph-set.mjs`, `undeploy-graph-set.mjs`; zero
    dependencies, the folder's only copy) to upload a set and to undeploy. D17; reverses Q8. The engine changes
    nothing for either; the webapp is one source for both engines, so the Java bundle releases first and both once
    the Rust twin's endpoints exist.
  - **Impact:** ADR-0011 amended (explicit and gated, no longer restart-scoped; registration may happen at first
    use, still through the record and the gate), ADR-0027 extended (the set as the run-time unit; the loader's
    pieces shared).
  - **Next:** the build - WP1 the Java engine's snapshot registry, shared loader pieces, `HotDeployment` with the
    per-set records, the reconcile and the lock; WP2 the three endpoints and the `rest.yaml` entries (engine,
    example, starter-graph template); WP3 the docs and claims; WP4 the Rust twin; WP5 the live drive with negative
    controls first and a two-instance drive on one folder; WP7 promotion to an ADR.
  - **Raised as PR #671, MERGED 2026-11-24** (squash `a37c4fc76`: the spec, the register entry, this thread, the
    continuity line, the log). Next: the build, WP1 first.
  - **Rounds 4 and 5 raised as PR #672, MERGED 2026-11-24** (squash `2985ee161`). The design is complete on `main`;
    next: the build, WP1 first (the snapshot registry, the shared loader pieces, `HotDeployment`).
  - **WP1 BUILT 2026-11-24 (Java engine, no endpoint yet; branch `feat/hot-deploy-wp1`, for a pull request):**
    `CompiledGraphs` is one immutable snapshot behind an `AtomicReference` with a `Refresh` hook on every lookup and
    `replaceGraphs` for one swap; `GraphSetLoader` shares `gate`, `sha256`, `normalizedPath` and `folderOf`, and
    `checkUnpack` refuses the hot folder (inside or above); the new `HotDeployment` holds the three keys and the
    throttle, the per-set record `<id>.deployed.yaml` (set, version, sha256, deployed_at, graphs, files), the
    reconcile (no record → unregister; record and package disagree → one re-read, then unregister with an ERROR;
    else load, stage, gate, move into place, one swap), the hook (a hot id reads its set's record bytes, an unknown
    id checks for a record of its name), `deploy` (the digest header, the strict read, the main-graph rule, `graph_id`
    agreement, ownership 409, staging in `.staging-*`, the gate, the move, the record last; the same bytes again
    change nothing), `undeploy` (404 for a set the folder lacks; the record deleted first, then the files it named),
    `list` (every record reconciled, a status per set) and `isPrivateSubgraph` for WP2's edge; `CompileGraph`
    configures it before the manifests and reconciles after them, so the bundled folder stays the primary location.
    Properties in the engine's and the example's `application.properties`, both switches off. `HotDeploymentTest`,
    12 tests: deploy and serve, the same bytes then a replacement as a unit, the naming rule, a bundled id and
    another set's id refused with the owner named, a refused upload leaving no file, undeploy, the records restoring
    sets at startup with a stray package ignored, another instance's deploy, replace and undeploy seen at the next
    lookup, a tampered package unregistered, the switches governing each side, the hot and unpack folders refusing
    each other, one swap. `GraphSetLoaderTest` and `CompileGraphTest` unchanged and green.
  → proposal: RFC-0017
  → serves: vision-mercury-composable
  <!-- id: hot-deploy-graph-sets | created: 2026-11-24 | last_used: 2026-11-24 | uses: 1 | tier: working | origin: 2026-11-24-202705 -->
