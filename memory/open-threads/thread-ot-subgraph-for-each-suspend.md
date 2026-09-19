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
  **Doc sweep DONE (PR #420, squash `c74ee6be`):** six surfaces outside #418's diff still stated the
  two-segment key — `workflow-suspension.md` §The Redis store module, `skills-reference.md`
  §graph.suspend, `minigraph-commands.json` (the machine-readable AI contract), `help graph-suspend.md`
  (plus the Playground bundle, rebuilt — the help markdown reaches the UI only through it), and the
  `PersistModel`/`RetrieveModel` javadocs. Verified after merge: 14 of 15 live statements now carry the
  index; the one that does not is `ADR.md` §ADR-0013, deliberately, because an ADR records what was
  decided then.
  **NEXT SESSION STARTS HERE (Eric, at sprint close 2026-09-19):** the Rust lockstep in the `mercury`
  repo — port the three-segment store key `graph:{graph_id}:{cid}:{index}`, the `x-iteration-index`
  header from `graph.extension`'s `for_each` branch, the reserved `model.iteration_index`, and the
  FileStateStore/Redis store changes, then re-run the Java negative-control tests' shape against the twin.
  **REMAINING:** (1) **Rust lockstep** — the store key is an explicit cross-engine contract
  ([[conv-ports-adopt-java-release-number]]); recorded reading is Java leads, twin immediately after.
  (2) ~~amending ADR~~ **DONE 2026-09-18** — Eric accepted ADR-0013 with the `for_each` index folded
  in **directly**, rather than superseding it, because it had never left `Status: Proposed`. Its
  rationale now records why the amendment belongs to that decision and not a separate one: the cid
  inheritance ADR-0013 mandated is exactly what makes the iterations collide.
  **Deliberately NOT built:** parent→for_each→flow→suspending graph, nested `for_each` with
  suspension, CompileGraph detection — see [[clean-knowledge-design-over-engine-coverage]].
  → serves: vision-mercury-composable
  origin: `memory/sessions/2026-09-18-174943.md`
  <!-- id: ot-subgraph-for-each-suspend | created: 2026-09-18 | last_used: 2026-09-18 | uses: 3 | tier: working | origin: 2026-09-18-174943 -->
