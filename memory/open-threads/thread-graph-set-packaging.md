- [x] **Graph set packaging and deployment (ADR-0027) — CLOSED 2026-10-07.** Outcome: the sprint's six work packages shipped on both
  engines between 2026-10-05 and 2026-10-07 — the gate as a method and the command line (#508/#356, the one-graph rule #510/#357), the
  loader (#515/#359), the endpoints (#524/#364, moved to `/api/graph-set` by #525/#365), the Playground's "Graph set packaging" panel
  (#526/#366) and the docs with five loader claims (#527 squash `db7069ee`, #367 merge `daa493ba`, Increment 169). Lesson: one shared
  file and an engine-side packer keep two engines byte-identical (the panel's pack equals the command line's), and a docs work package
  is a lock-step change because the Rust repository carries its own guide pages. Origin: 2026-10-03-171402.md; the narrative is in the
  session logs of 2026-10-05 to 2026-10-07 (last: 2026-10-07-004323.md).
  → proposal: RFC-0005
  → serves: vision-mercury-composable
  <!-- id: graph-set-packaging | created: 2026-10-03 | last_used: 2026-10-07 | uses: 10 | tier: active | origin: 2026-10-03-171402 -->
