- [ ] **Close stalled threads (due):** `bp-ai-companion-llm-backend` (135 sessions),
  `bp-graph-governance-lifecycle` (135 sessions) — for each, close it, or re-affirm it (DECAY.md §6).
  Raised by the 2026-09-17 review, the first run of the v4.40.0 closure gate — a feature that grew
  out of this repo's own staleness report, so this is the mechanism working on the repo that asked
  for it.
  **Both are `(blueprint)` gaps, so closing either is an altitude decision (DECAY.md §12) and
  therefore Eric's alone.** The review does not close a thread; it only surfaces the signal.
  Context for that conversation, so it is not re-derived:
  - `bp-ai-companion-llm-backend` — mature `POST /api/companion/{id}` from a dev-only command pipe
    into a governed collaboration layer with a pluggable LLM backend. Unreferenced since 2026-08-25.
    Related work has moved: the companion endpoint is now the AI-agent drive path for the Playground
    ([[playground-session-broker]]), and [[bp-agent-orchestration]] carries the LLM integration
    thinking. The question is whether this gap is still distinct from that one, or absorbed by it.
  - `bp-graph-governance-lifecycle` — dry-run → certify → stage → approve → production for graph
    models. Unreferenced since 2026-08-25. Partially realized in substance: CompileGraph is the
    deployment quality gate today, and this review's own work hardened it
    ([[thread-compilegraph-syntax-validation]]). The question is whether the *promotion lifecycle*
    half remains a Blueprint gap or has quietly become a documentation task.
  Neither is stale in the sense the other seven were — no false claims, nothing shipped-but-unstruck.
  They are genuinely open long-horizon gaps that simply have not been touched in ~135 sessions, which
  is exactly the case the gate exists to surface rather than to resolve.
  → serves: vision-mercury-composable
  <!-- id: ot-close-stalled-threads-20260917 | created: 2026-09-17 | last_used: 2026-09-17 | uses: 1 | tier: working | origin: 2026-09-17-020650 -->
