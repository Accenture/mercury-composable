- [x] **Streaming return route — cross-pod progressive rendering for sync-over-async. DONE
  2026-09-12, one day from idea to E-series-complete.** Outcome: one Redis List mechanism for
  one-shot and streaming (D1–D8; no sequence number; a one-shot response is the degenerate
  stream), `StreamResponder`/`StreamBridge` shipped, both driving use cases proven live
  cross-pod incl. real Gemini tokens; final-drain nuance CONFIRMED by Eric; ships with the
  next release. Refs: spec PRs #365–#368; E1–E4 + findings PRs #369–#374 (squashes
  `adc62997`, `4bd5bd11`, `b1e57537`, `b9d20d52`, `04224cb9`, `e08f28ca`); permanent record
  docs/test-reports/streaming-return-route-cross-pod.md; serves [[bp-agent-orchestration]]
  Q8 (transport delivered). Durable lesson: the producer contract (post-in-order) binds
  FORWARDERS too — an ordered reply consumer must be a single-instance route, the same
  reason the HTTP edge's reply lanes are. Full narrative: origin + logs 2026-09-12-032830 /
  -044817 / -192109 / -213254 / -220504 / -230025.
  <!-- id: ot-streaming-return-route | created: 2026-09-12 | last_used: 2026-09-13 | uses: 9 | tier: archive-candidate | origin: 2026-09-12-021649 -->
