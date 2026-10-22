- [x] **Release v4.12.25 — SHIPPED 2026-10-13 (Java only): the field's Sonar findings on 4.12.24 closed, pre-accepted by the field's
  pipeline on the merged content.** #562 squash `f496291e` = tag `v4.12.25` (release 02:14:53Z); #560 `a4d94916` and #561 `d776685d` the
  content; 249 suites / 1830 tests. Rust stays at 4.12.24 (Eric: it catches up with its next feature or fix). Lesson: the field's gate
  judges new code with heuristics that catch prose and framework idioms - and a rename made to satisfy one rule must be checked against
  the same rule's other half ([[conv-sonar-gate-heuristics]]). Next: record the field's deployment of 4.12.25 on `latest_release`.
  origin: 2026-10-13-020033; close 2026-10-13-021606.
  → serves: vision-mercury-composable
  <!-- id: release-4-12-25 | created: 2026-10-13 | last_used: 2026-10-13 | uses: 2 | tier: archive-candidate | origin: 2026-10-13-020033 -->
