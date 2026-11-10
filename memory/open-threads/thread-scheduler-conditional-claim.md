- [x] **The mini-scheduler's conditional claim - `_origin` in place of `_firing`, worked examples for PostgreSQL and
  MongoDB, and a runnable MongoDB example (Eric, 2026-11-04) — SHIPPED in v4.12.29 (2026-11-05).** #612 squash `95fd7587`
  (`_origin`, the claim, the window, both worked examples), #613 the user guide, #615 squash `fa03205d` (the example on
  MongoDB, `helpers/mongodb-standalone`, the driver at 5.14.0); release #616. Lesson: the database must pick the winner
  in one conditional write - a read, a lock or a timed read-back each fails in a way the lab measured
  ([[scheduler-resolver-conditional-claim]], [[scheduler-example-mongodb]]). The field's resolver drops its record lock
  for the claim. origin: 2026-11-04-234824; close 2026-11-05-065411.
  → serves: vision-mercury-composable
  <!-- id: scheduler-conditional-claim | created: 2026-11-04 | last_used: 2026-11-05 | uses: 3 | tier: archive-candidate | origin: 2026-11-04-234824 -->
