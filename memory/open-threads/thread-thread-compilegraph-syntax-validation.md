- [x] (backlog → **FIXED 2026-09-16**) **`CompileGraph` accepted a working node with no skill** — it
  passed silently as structural, so the graph traversed it and nothing ran.
  `GraphModelValidator.validateWorkingNodeHasSkill` now hard-errors, bidirectionally: a `task` must be
  claimed by `graph.task`/`graph.suspend`/`graph.resume`, and each of those requires a `task`.
  Fixtures `unit-test-task-skill-err1..3` assert the message, plus a guard that descriptor nodes stay valid.
  **Lesson:** a validator is a claim about every model that exists — two wrong drafts (keying on
  `input`/`output`; `graph.task` only) were caught by running the corpus, not by reading the rule.
  Parked: comprehensive mapping-DSL syntax validation, a separate design pass.
  origin: `memory/sessions/2026-09-16-234646.md`
  <!-- id: thread-compilegraph-syntax-validation | created: 2026-07-02 | last_used: 2026-09-17 | uses: 7 | tier: active | origin: 2026-07-02-004606 -->
