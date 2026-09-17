- [ ] **Upstream report: `[undeclared-reference]` check — agent-memory is reviewing (sent 2026-09-17).**
  Report at `/tmp/agent-memory-undeclared-reference-check.md` (not committed — upstream correspondence).
  Proposes a `memory-lint` finding over the STAGED index: a continuity fact whose **body text** changed
  in a commit must be declared in a session log staged in the same commit. Grounded on two verified
  facts from this repo — every closure-gate commit staged continuity.md and the log together, and
  `refresh-metadata`'s diff is footer-only (so excluding `<!-- id: ... -->` lines removes the whole
  false-positive class). Recommended advisory, not blocking.
  **On their reply — do NOT just read the changelog.** When v4.40.1 shipped our last note, re-running
  the original reproduction is what confirmed it ([[conv-declare-consulted-references]] is the
  convention this backs up). If a version lands carrying this check, verify it fires on the motivating
  case: edit a continuity fact's body, stage it with a session log that omits the id, and confirm the
  warning — then confirm a `refresh-metadata`-shaped footer-only commit stays silent.
  **Why it matters beyond tidiness:** the miss decays a fact in active use toward `[overdue]`, and the
  archival ritual acts on that signal. Today's near-miss was two Blueprint gaps closed by Eric hours
  earlier reading `sslu 137`; a sweep would have looked identical to the correct outcome.
  origin: `memory/sessions/2026-09-17-183008.md`
  <!-- id: ot-undeclared-reference-check | created: 2026-09-17 | last_used: 2026-09-17 | uses: 1 | tier: working | origin: 2026-09-17-183008 -->
