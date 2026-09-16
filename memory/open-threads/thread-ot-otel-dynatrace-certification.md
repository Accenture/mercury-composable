- [x] (feature) **OpenTelemetry forwarder — Dynatrace certification. DONE 2026-09-16.** Opt-in
  `otel.forwarding` switch (`@OptionalService`, default off), per-export credentials, self-diagnosing
  export failures; legacy `otel.trace.forwarder.enabled` retired and the hand-rolled shutdown hook
  migrated to `Platform.onShutdown`. Certified live against a Dynatrace SaaS OTLP endpoint and
  **confirmed queryable in the Dynatrace UI** — 6 spans in one trace under `mercury-otel-cert`, span
  tree reconstructed from `parent_span_id`, `server`/`internal` kinds mapped, scope version 4.12.10
  resolved at runtime. Report: `docs/test-reports/otel-dynatrace-certification.md`. PR #404, shipped
  in v4.12.11.
  **Durable lesson:** when a verification is blocked on someone else's access, ask what your own
  evidence would look like if the thing were broken — "zero export failures" proves nothing until a
  negative control shows a failure was possible. The A-B-A credential experiment (real/bogus/real
  token → 0/6/0 failures) turned an inconclusive report into a conclusive one, and incidentally
  demonstrated that a telemetry outage leaves the transaction at HTTP 201.
  origin: `memory/sessions/2026-09-16-193203.md`
  <!-- id: ot-otel-dynatrace-certification | created: 2026-09-16 | last_used: 2026-09-16 | uses: 4 | tier: active | origin: 2026-09-16-175425 -->
