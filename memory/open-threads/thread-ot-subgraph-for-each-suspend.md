- [x] **Subgraph suspend/resume under `for_each` — CLOSED 2026-09-19, shipped in both engines.** A parent's
  `for_each` fan-out over ONE suspending subgraph collided on `graph:{id}:{cid}` (every iteration inherits the
  cid by design); the array index now joins the key — `graph:{graph_id}:{cid}:{index}`, appended only when
  present. Java: spec #415 `6f324943`, implementation #418 `fb171c18`, doc sweep #420 `c74ee6be`, the rule-
  statement fix #425 `8c81a295`; ADR-0013 accepted in place with the index. Rust: mercury #284 (merge
  `c239532b`, feature `3c98043d`, Increment 118), whose end-to-end fan-out also exposed and fixed a Rust-only
  fork-join trace loss. Lesson: grep for the THING you touched, not the topic — the key was swept across 15
  surfaces while the rule stated without it survived two sweeps; and re-run a parity assertion on every shape
  a feature has. → serves: vision-mercury-composable
  origin: `memory/sessions/2026-09-18-174943.md` (spec + rulings) → close record: `2026-09-19-020551.md`
  <!-- id: ot-subgraph-for-each-suspend | created: 2026-09-18 | last_used: 2026-09-19 | uses: 4 | tier: archive-candidate | origin: 2026-09-18-174943 -->
