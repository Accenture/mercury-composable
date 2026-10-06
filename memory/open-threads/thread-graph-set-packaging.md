- [ ] **Graph set packaging and deployment (Eric's design, 2026-10-03) — the next sprint.** The first consumer of the canonical packager (ADR-0026): a `pack`/`unpack`/`inspect`
  CLI over the engine's packager in both repos, a Playground panel that assembles a `.pack` set from graph JSON files with manifest key-values and downloads it (packed on the
  engine through `POST /api/graph/pack`), and `sets` + `unpack` in `graphs.yaml` — each set unpacked before the gate into a `file:/` folder (read/write; `classpath:` rejected)
  with a generated manifest that compiles next in sequence, all-or-none per set, later wins with an ERROR when a set is involved. Review and plan:
  `draft-design-specs/graph-set-packaging-and-deployment.md` (six work packages: the gate as a method, the CLI, the loader, the endpoints, the panel, the docs and the ADR).
  Decided 2026-10-03 (Eric): `.pack`, the gate at pack time, all-or-none, ERROR for a set duplicate, optional `graph_id`, root name = id, engine-side packing,
  the module homes, inspect in scope; `pack --from-manifest`, the generated manifest documented, `version` in `list graphs`. Next: the sprint (WP1-WP6); the ADR
  (RFC-0005 → ADR-0027) is written when the implementation lands. Relates [[canonical-packager-wire-contract]],
  [[graph-manifest-list-later-wins]], [[playground-file-import-download]].
  **WP1 + WP2 implemented 2026-10-05 (Eric: "start with the command-line packager"; PRs open, merges gated):** Java #508 - `GraphModelGate`
  (CompileGraph's checks as one shared method, no change in behavior), `GraphSet` (the set rules, shared with WP3-WP5 later) and
  `helpers/graph-packager` (an executable jar, GraalVM left out) - and the Rust twin mercury #356 (`model_gate`, `graph_set`,
  `tools/graph-packager` with `publish = false`, Increment 158). Both pack the 14 tutorials to identical bytes (`4715598820261ca6…`),
  print identical `inspect` reports and read each other's files; on the 52 shared fixtures both gates refuse the same 18 graphs (the
  interop report's addendum). Settled in the code: `${...}` references are packed unresolved while the gate checks a resolved copy, and
  `inspect --json` sorts its keys. **Settled 2026-10-05 (Eric), in the same PRs:** a graph holds no null property - filtered out on deploy,
  pack and read in both engines, `"key": ""` kept, `serializer.null.transport` not consulted ([[graph-null-property-filtered]]). **Open:**
  publishing the Rust tool to crates.io is a release decision. Next: WP3, the loader.
  → proposal: RFC-0005
  → serves: vision-mercury-composable
  <!-- id: graph-set-packaging | created: 2026-10-03 | last_used: 2026-10-03 | uses: 1 | tier: working | origin: 2026-10-03-171402 -->
