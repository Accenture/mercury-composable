- [x] **`[undeclared-reference]` shipped in agent-memory v4.41.0 and VERIFIED 2026-09-17.** Our report
  landed as `a45d7572` (#413), going past the proposal: `--range` for CI as well as `--staged`,
  vision + open-threads as surfaces, advisory as recommended.
  Verified by reproduction, not changelog: `--range` over the real commits flags the exact two misses
  (`903694f0`, `41bac544`) and stays silent on the two that were correctly handled.
  **Durable lesson:** my first harness reported four meaningless passes — every case edited a fact the
  log already declared, so none tested the positive path. A verification harness needs its own
  positive control; prove it can report failure before trusting a success.
  origin: `memory/sessions/2026-09-17-183008.md`
  <!-- id: ot-undeclared-reference-check | created: 2026-09-17 | last_used: 2026-09-17 | uses: 2 | tier: active | origin: 2026-09-17-183008 -->
