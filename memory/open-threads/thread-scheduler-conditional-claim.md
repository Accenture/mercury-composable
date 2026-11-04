- [ ] **The mini-scheduler's conditional claim - `_origin` in place of `_firing`, and the claim's worked examples for
  PostgreSQL and MongoDB (Eric, 2026-11-04).** Branch `feature/scheduler-conditional-claim`, PR pending: the scheduler
  sends the instance's origin id as `_origin`, logs it at startup and words the skipped-retry warning for a peer's
  claim; the mini-scheduler README's *Running more than one instance* gives the claim, the unique key, the window and
  both worked examples ([[scheduler-resolver-conditional-claim]]). Next: Eric's review and merge, then the next release
  (the field's resolver drops its record lock for the claim; the Rust port has no scheduler).
  origin: 2026-11-04-234824.
  → serves: vision-mercury-composable
  <!-- id: scheduler-conditional-claim | created: 2026-11-04 | last_used: 2026-11-04 | uses: 1 | tier: working | origin: 2026-11-04-234824 -->
