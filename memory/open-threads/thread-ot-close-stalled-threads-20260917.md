- [x] **Close stalled threads — COMPLETE 2026-09-17.** Both dispositioned by Eric at the first run of
  the v4.40.0 closure gate, a feature that grew out of this repo's own staleness report.
  - `bp-ai-companion-llm-backend` (135 sessions) → **CLOSED, delivered.** The AI companion is
    production quality with measured success in field installations; the live-Gemini
    progressive-rendering drive settled the pluggable-backend half.
  - `bp-graph-governance-lifecycle` (136 sessions) → **CLOSED, delivered.** It *is* the human–AI
    collaboration and product-owner certification process, delivered as part of companion maturity.
  **Durable lesson:** neither was rot — both were *finished work whose record had not caught up*.
  The staleness signal cannot tell "abandoned" from "done but unrecorded", and this first run was
  entirely the second kind. Read a stale flag as "ask the owner", never as "this decayed".
  Also corrected here: dev-gating (`@OptionalService("app.env=dev")`) is the right posture for a
  design-time tool, not evidence of immaturity — what gets promoted is the graph, not the authoring
  surface.
  origin: `memory/sessions/2026-09-17-020650.md`
  <!-- id: ot-close-stalled-threads-20260917 | created: 2026-09-17 | last_used: 2026-09-17 | uses: 1 | tier: working | origin: 2026-09-17-020650 -->
