- [ ] **Graph set packaging and deployment (Eric's design, 2026-10-03) — the next sprint.** The first consumer of the canonical packager (ADR-0026): a `pack`/`unpack`/`inspect`
  CLI over the engine's packager in both repos, a Playground panel that assembles a `.pack` set from graph JSON files with manifest key-values and downloads it (packed on the
  engine through `POST /api/graph/pack`), and `sets` + `unpack` in `graphs.yaml` — each set unpacked before the gate into a `file:/` folder (read/write; `classpath:` rejected)
  with a generated manifest that compiles next in sequence, all-or-none per set, later wins with an ERROR when a set is involved. Review and plan:
  `draft-design-specs/graph-set-packaging-and-deployment.md` (six work packages: the gate as a method, the CLI, the loader, the endpoints, the panel, the docs and the ADR).
  Decided 2026-10-03 (Eric): `.pack`, the gate at pack time, all-or-none, ERROR for a set duplicate, optional `graph_id`, root name = id, engine-side packing,
  the module homes, inspect in scope; `pack --from-manifest`, the generated manifest documented, `version` in `list graphs`. Next: the sprint (WP1-WP6); the ADR
  (RFC-0005 → ADR-0027) is written when the implementation lands. Relates [[canonical-packager-wire-contract]],
  [[graph-manifest-list-later-wins]], [[playground-file-import-download]].
  → proposal: RFC-0005
  → serves: vision-mercury-composable
  <!-- id: graph-set-packaging | created: 2026-10-03 | last_used: 2026-10-03 | uses: 1 | tier: working | origin: 2026-10-03-171402 -->
