- [ ] **The mini-scheduler's conditional claim - `_origin` in place of `_firing`, and the claim's worked examples for
  PostgreSQL and MongoDB (Eric, 2026-11-04).** PR #612 MERGED 2026-11-04 (squash `95fd7587`): the scheduler
  sends the instance's origin id as `_origin`, logs it at startup and words the skipped-retry warning for a peer's
  claim; the mini-scheduler README's *Running more than one instance* gives the claim, the unique key, the window and
  both worked examples ([[scheduler-resolver-conditional-claim]]). The scheduler example runs the MongoDB one as an
  application, with `helpers/mongodb-standalone` for a test drive without Docker ([[scheduler-example-mongodb]],
  PR #615 MERGED 2026-11-05, squash `fa03205d`, the driver at 5.14.0). Next: the next release, v4.12.29, which ships both (the
  field's resolver drops its record lock for the claim; the Rust port has no scheduler).
  origin: 2026-11-04-234824.
  → serves: vision-mercury-composable
  <!-- id: scheduler-conditional-claim | created: 2026-11-04 | last_used: 2026-11-04 | uses: 1 | tier: working | origin: 2026-11-04-234824 -->
