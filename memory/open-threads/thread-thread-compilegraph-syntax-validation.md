- [ ] (backlog — Eric, 2026-07-02; needs its own design pass) **`CompileGraph` does not carry out
  comprehensive syntax validation** for the mapping-string mini-DSL (the one
  `DataMappingHelper`/`SimpleTypeMatchingConverter` handle). Open questions: what "comprehensive"
  means (malformed plugin calls? unknown plugin names? arg-count/type checks?), and whether to
  reuse or diverge from event-script-engine's own `validInput`/`validOutput` (already diverges —
  minigraph's per-skill namespace rules don't match event-script's). Landing pad exists:
  `GraphModelValidator` ([[compilegraph-mandatory-gate]]). **New case (2026-09-01, E0
  round): a node with task/input/output but NO `skill` property compiles and silently
  passes through as a structural node** — a gate warning would have saved a debugging
  round (origin: 2026-09-01-022524).
  <!-- id: thread-compilegraph-syntax-validation | created: 2026-07-02 | last_used: 2026-09-01 | uses: 5 | tier: working | origin: 2026-07-02-004606 -->
