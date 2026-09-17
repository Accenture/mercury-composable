- [x] (field support → **RESOLVED 2026-09-16**) **The internal API gateway stripped `traceparent` and
  `X-Trace-Id`; only `X-Correlation-Id` passed.** Closed because the field accepted our
  header-standardization proposal, so the gateway allow-list now carries the standard trace headers.
  Engine half shipped earlier (PR #179: colliding names + absent header → ONE id, trace authoritative).
  **Lesson:** the right fix lived in someone else's system, and refusing the `legacy.trace.id`
  compatibility flag — which would have entrenched the cid/trace-id conflation — is what left
  standardization as the path. Diagnosis retained in [[field-trace-propagation-4-6-3-diagnosis]].
  origin: `memory/sessions/2026-07-13-142021.md`
  <!-- id: thread-field-trace-propagation-4-6-3 | created: 2026-07-13 | last_used: 2026-08-01 | uses: 8 | tier: archive-candidate | origin: 2026-07-13-142021 -->
