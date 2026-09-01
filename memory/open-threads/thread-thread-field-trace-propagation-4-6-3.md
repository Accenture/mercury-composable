- [ ] (field support — 2026-07-13; ROOT CAUSE FOUND) **Trace-propagation report: the internal API
  gateway strips `traceparent` AND `X-Trace-Id` (neither on its allow-list); only
  `X-Correlation-Id` passes.** 4.4.11 "worked" because the legacy conflation rode the allow-listed
  header. A proposed `legacy.trace.id` flag was **REJECTED by Eric — re-mixing the business cid
  with the trace id makes things worse**; THE fix = gateway allow-list change (traceparent +
  X-Trace-Id), which Eric took to the infra team. Interim: the field runs legacy conflation
  (`http.trace.id.header` = `http.correlation.id.header` = X-Correlation-Id) — safe when the edge
  supplies the header; the absent-header divergence was FIXED (PR #179: colliding names + absent
  header → ONE id, trace authoritative, both ingress paths) and validated live by Eric. Support
  nuance: with conflation the outbound trace id rides the configured header name; traceparent is
  stamped only for W3C-shaped (32-hex) ids — cross-app SPAN parenting still needs traceparent, so
  tooling stitches by trace id until the gateway passes it. **Pending: gateway team's allow-list
  change (asked 2026-07-14; Eric updates after the devops cloud-dev test).** Diagnosis +
  checklist: [[field-trace-propagation-4-6-3-diagnosis]]. Full detail: origin log.
  <!-- id: thread-field-trace-propagation-4-6-3 | created: 2026-07-13 | last_used: 2026-08-01 | uses: 8 | tier: working | origin: 2026-07-13-142021 -->
