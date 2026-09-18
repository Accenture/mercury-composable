- [ ] **Subgraph suspend/resume under `for_each` — spec RULED, implementation pending.** A parent
  invoking a suspending subgraph via `graph.extension` + `for_each` writes ONE store record for all
  iterations (business-cid + graphId), so concurrent iterations race and only one survives. Spec:
  `draft-design-specs/subgraph-suspend-resume-for-each.md` — on main via PR #415, squash `6f324943`;
  shape and Q1–Q5 all ruled by Eric 2026-09-18.
  **To build:** array INDEX as a key segment, carried as a header like the business cid, landing in a
  reserved `model.iteration_index` that joins `RESERVED_MODEL_METADATA`; store key becomes
  `graph:{graph_id}:{cid}:{index}`; keys without an index are unchanged, so pre-upgrade records stay
  reachable. Plus the §5.1 guide correction and the §5.2 declared rules, in this sprint.
  **Rust lockstep is REQUIRED** — the store key is an explicit cross-engine contract
  ([[conv-ports-adopt-java-release-number]]); recorded reading is Java leads, twin immediately after.
  **Deliberately NOT built:** parent→for_each→flow→suspending graph, nested `for_each` with
  suspension, CompileGraph detection — see [[clean-knowledge-design-over-engine-coverage]].
  → serves: vision-mercury-composable
  origin: `memory/sessions/2026-09-18-174943.md`
  <!-- id: ot-subgraph-for-each-suspend | created: 2026-09-18 | last_used: 2026-09-18 | uses: 1 | tier: working | origin: 2026-09-18-174943 -->
