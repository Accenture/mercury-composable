- [ ] **Subgraph suspend/resume under `for_each` — JAVA SHIPPED; Rust lockstep + a doc sweep remain.**
  A parent invoking a suspending subgraph via `graph.extension` + `for_each` wrote ONE store record for
  all iterations (business-cid + graphId), so concurrent iterations raced and only one survived. Spec:
  `draft-design-specs/subgraph-suspend-resume-for-each.md` (PR #415, squash `6f324943`); shape and
  Q1–Q5 ruled by Eric 2026-09-18.
  **Java MERGED 2026-09-18 (PR #418, squash `fb171c18`):** array INDEX rides the invocation dataset's
  `HEADER` map — *not* the `graph-executor` flow, which is application-owned and so cannot carry a new
  mapping to already-deployed apps — and is lifted into a reserved `model.iteration_index` that joins
  `RESERVED_MODEL_METADATA`. Store key is now `graph:{graph_id}:{cid}:{index}`; an absent index leaves
  the key unchanged, so pre-upgrade records stay reachable. §5.1 guide correction and §5.2 declared
  rules landed with it.
  **REMAINING:** (1) **Rust lockstep** — the store key is an explicit cross-engine contract
  ([[conv-ports-adopt-java-release-number]]); recorded reading is Java leads, twin immediately after.
  (2) **Doc sweep — four surfaces still state the two-segment key** and were outside #418's diff:
  `workflow-suspension.md` §The Redis store module (the same file's contract section is correct),
  `skills-reference.md` §graph.suspend, `minigraph-commands.json` (the machine-readable AI contract),
  and `help graph-suspend.md` (mirrored into the built Playground bundle).
  **Deliberately NOT built:** parent→for_each→flow→suspending graph, nested `for_each` with
  suspension, CompileGraph detection — see [[clean-knowledge-design-over-engine-coverage]].
  → serves: vision-mercury-composable
  origin: `memory/sessions/2026-09-18-174943.md`
  <!-- id: ot-subgraph-for-each-suspend | created: 2026-09-18 | last_used: 2026-09-18 | uses: 2 | tier: working | origin: 2026-09-18-174943 -->
