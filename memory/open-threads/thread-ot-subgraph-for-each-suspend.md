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
  **Rust lockstep PREPARED 2026-09-19** (`mercury` repo, branch `feat/for-each-suspend-resume-lockstep`,
  commit `3c98043d`, pushed; PR-open + merge are Eric's gates): the three-segment key, the
  `x-iteration-index` header, the reserved `model.iteration_index`, the Redis store + file-store mock,
  the three store negative controls + reserved-key pin + index reader — plus an end-to-end fan-out
  (`rust-orchestrator-foreach`) Java lacks, which caught a Rust-only defect fixed in the same PR (its
  fork-join skills spawned tasks and lost the trace bracket; children ran untraced). The Rust ledger
  got the same treatment: ADR-0012 (our 0013's twin) accepted in place with the index, 0013/0014
  accepted, `RFC.md` adopted and packaged. Rust Increment 118 carries the record.
  **Found on the way, THIS repo:** the guide's orchestrator bullet "One record per graph per cid … a
  `for_each` fan-out … would overwrite its own record" survived #418/#420 — a statement of the RULE,
  not the key. Corrected on branch `docs/for-each-orchestrator-rule` (`0c9166ba`, pushed; PR-open is
  Eric's gate), together with the "in the next rule" pointer that should read "the `for_each` rule above".
  **REMAINING:** (1) Eric opens + merges the Rust PR (the store key is an explicit cross-engine
  contract — [[conv-ports-adopt-java-release-number]]; Java led, the twin followed the next session);
  (2) Eric opens + merges the Java docs PR; then close this thread.
  (2) ~~amending ADR~~ **DONE 2026-09-18** — Eric accepted ADR-0013 with the `for_each` index folded
  in **directly**, rather than superseding it, because it had never left `Status: Proposed`. Its
  rationale now records why the amendment belongs to that decision and not a separate one: the cid
  inheritance ADR-0013 mandated is exactly what makes the iterations collide.
  **Deliberately NOT built:** parent→for_each→flow→suspending graph, nested `for_each` with
  suspension, CompileGraph detection — see [[clean-knowledge-design-over-engine-coverage]].
  → serves: vision-mercury-composable
  origin: `memory/sessions/2026-09-18-174943.md`
  <!-- id: ot-subgraph-for-each-suspend | created: 2026-09-18 | last_used: 2026-09-18 | uses: 3 | tier: working | origin: 2026-09-18-174943 -->
