- [ ] **Hot deploy and undeploy of knowledge graphs: a certified graph set uploaded to a running engine, registered on
  every instance on demand, removed by name (RFC-0017; the maintainer's sprint of 2026-11-24).** Deployment is a
  restart today (ADR-0011, the 4.12.19 lane included) and nothing undeploys. The draft: bundled graphs and the dev-mode
  workspace unchanged; a third folder `location.graph.deploy` (default `file:/tmp/deploy`) under two switches,
  `graph.hot.deploy` and `graph.hot.undeploy`, both off; a set uploaded to `POST /api/graph-set/deploy` is gated and
  registered at once, `POST /api/graph-set/undeploy/{set}` removes it, `GET /api/graph-set/deployed` lists the
  folder's record; user-defined functions belong in language-pack sidecars so the engine's artifact stops changing.
  Spec `draft-design-specs/hot-deploy-undeploy-graph-sets.md`, two rounds the same day.
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
  - **Impact:** ADR-0011 amended (explicit and gated, no longer restart-scoped; registration may happen at first
    use, still through the record and the gate), ADR-0027 extended (the set as the run-time unit; the loader's
    pieces shared).
  - **Next:** the build - WP1 the Java engine's snapshot registry, shared loader pieces, `HotDeployment` with the
    per-set records, the reconcile and the lock; WP2 the three endpoints and the `rest.yaml` entries (engine,
    example, starter-graph template); WP3 the docs and claims; WP4 the Rust twin; WP5 the live drive with negative
    controls first and a two-instance drive on one folder; WP7 promotion to an ADR.
  → proposal: RFC-0017
  → serves: vision-mercury-composable
  <!-- id: hot-deploy-graph-sets | created: 2026-11-24 | last_used: 2026-11-24 | uses: 1 | tier: working | origin: 2026-11-24-202705 -->
