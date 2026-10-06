- [ ] **Graph set packaging and deployment (Eric's design, 2026-10-03) — the next sprint.** The first consumer of the canonical packager (ADR-0026): a `pack`/`unpack`/`inspect`
  CLI over the engine's packager in both repos, a Playground panel that assembles a `.pack` set from graph JSON files with manifest key-values and downloads it (packed on the
  engine through `POST /api/graph/pack`), and `sets` + `unpack` in `graphs.yaml` — each set unpacked before the gate into a `file:/` folder (read/write; `classpath:` rejected)
  with a generated manifest that compiles next in sequence, all-or-none per set, later wins with an ERROR when a set is involved. Review and plan:
  `draft-design-specs/graph-set-packaging-and-deployment.md` (six work packages: the gate as a method, the CLI, the loader, the endpoints, the panel, the docs and the ADR).
  Decided 2026-10-03 (Eric): `.pack`, the gate at pack time, all-or-none, ERROR for a set duplicate, optional `graph_id`, root name = id, engine-side packing,
  the module homes, inspect in scope; `pack --from-manifest`, the generated manifest documented, `version` in `list graphs`. Next: the sprint (WP1-WP6);
  **RFC-0005 was promoted to ADR-0027 on 2026-10-06 (Eric), after WP2** ([[graph-set-pack-and-deploy]]). Relates [[canonical-packager-wire-contract]],
  [[graph-manifest-list-later-wins]], [[playground-file-import-download]].
  **WP1 + WP2 implemented 2026-10-05 (Eric: "start with the command-line packager"); MERGED 2026-10-06 - Java #508 squash `5e9a0fab`,
  mercury #356 merge `779cffe1`:** Java #508 - `GraphModelGate`
  (CompileGraph's checks as one shared method, no change in behavior), `GraphSet` (the set rules, shared with WP3-WP5 later) and
  `helpers/graph-packager` (an executable jar, GraalVM left out) - and the Rust twin mercury #356 (`model_gate`, `graph_set`,
  `tools/graph-packager` with `publish = false`, Increment 158). Both pack the 14 tutorials to identical bytes (`4715598820261ca6…`),
  print identical `inspect` reports and read each other's files; on the 52 shared fixtures both gates refuse the same 18 graphs (the
  interop report's addendum). Settled in the code: `${...}` references are packed unresolved while the gate checks a resolved copy, and
  `inspect --json` sorts its keys. **Settled 2026-10-05 (Eric), in the same PRs:** a graph holds no null property - filtered out on deploy,
  pack and read in both engines, `"key": ""` kept, `serializer.null.transport` not consulted ([[graph-null-property-filtered]]).
  **A set of one graph is valid on purpose (Eric, 2026-10-06): packing a graph alone is how ONE graph gets signed** (the detached
  `<set>.pack.sig` of RFC-0005 §3.1). Confirmed live on main: `pack --set tutorial-1 tutorial-1.json` writes the same 410 bytes in Java and
  Rust (SHA-256 `74c213b7…`), again on a second pack and on an unpack-then-repack; an Ed25519 signature over the Java pack verified the Rust
  pack and failed once a manifest field changed; only an empty set is refused (`a set needs at least one graph`, exit 1). **Then pinned and
  recorded (Eric: "proceed with the additional unit test ... promote RFC-0005 as ADR-0027"):** `aSingleGraphIsPackedAloneSoItCanBeSigned` and the
  Rust twin pin one 327-byte set (SHA-256 `c500281a…`) in both engines, and ADR-0027 states the reason - PR #510 squash `2ebcaea5` and
  mercury #357 merge `60e59aeb` (Increment 159), both MERGED 2026-10-06. **Open:** publishing the Rust tool to crates.io is a release
  decision. **WP3, the loader, MERGED 2026-10-06 - Java #515 squash `eb6021a9`, mercury #359 merge `42f6cf81` (Increment 161):**
  `GraphSetLoader` / `graph_set_loader` deploy a manifest's `sets` through a writable `unpack` folder, all or none, right after its
  loose graphs; the generated manifest's `graphs` lists what deployed and `generated` every file written, which the next start removes
  (a deliberate refinement of the design spec's §3.5 step 4, flagged in both PRs); `list graphs` shows a set graph's set and version.
  **WP4, the endpoints, MERGED 2026-10-06 - Java #524 squash `ca0e07a3`, mercury #364 merge `f89c0997` (Increment 166):** `POST /api/graph-set/pack`
  (`pack.graph.set`) and `POST /api/graph-set/unpack` (`unpack.graph.set`) - first `/api/graph/pack` and `/unpack`, moved by #525 and
  mercury #365 (Increment 167) because those collided with the executor's `/api/graph/{graph_id}` - dev-mode, in every `rest.yaml`
  copy and the guide excerpt;
  the set name travels as `manifest.set` (decided there, flagged in both PRs for Eric's review); `GraphSetEndpointTest` and
  `tests/graph_set_endpoints.rs` drive both through the real routers. Next: WP5 (the panel), then WP6 (the docs), each on Eric's word.
  → proposal: RFC-0005
  → serves: vision-mercury-composable
  <!-- id: graph-set-packaging | created: 2026-10-03 | last_used: 2026-10-06 | uses: 5 | tier: working | origin: 2026-10-03-171402 -->
