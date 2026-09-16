- [x] (field support → **RESOLVED 2026-09-16, Eric**) **Trace-propagation: the internal API gateway
  stripped `traceparent` and `X-Trace-Id`; only `X-Correlation-Id` passed.** Closed because **the
  field accepted our header-standardization proposal** — the gateway allow-list now carries the
  standard trace headers, so the interim legacy conflation is no longer the only path and cross-app
  span parenting works on `traceparent` as designed.
  The engine-side half shipped long before: PR #179 fixed the absent-header divergence (colliding
  names + absent header → ONE id, trace authoritative, both ingress paths), validated live. Eric
  **rejected** the proposed `legacy.trace.id` flag on principle — re-mixing the business
  correlation-id with the trace id makes things worse — and the gateway allow-list was always THE
  fix; standardization is that fix, accepted.
  **Durable lesson:** the right fix was in someone else's system, and holding the line on that
  (rather than shipping a compatibility flag that would have entrenched the conflation) is what
  made standardization possible. Diagnosis + checklist retained in
  [[field-trace-propagation-4-6-3-diagnosis]].
  origin: `memory/sessions/2026-07-13-142021.md`
  <!-- id: thread-field-trace-propagation-4-6-3 | created: 2026-07-13 | last_used: 2026-08-01 | uses: 8 | tier: archive-candidate | origin: 2026-07-13-142021 -->
