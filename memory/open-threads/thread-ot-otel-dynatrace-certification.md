- [ ] (feature) **OpenTelemetry forwarder — Dynatrace certification.** Code COMPLETE and green;
  the live run is DONE and recorded in `docs/test-reports/otel-dynatrace-certification.md`. Dynatrace
  support granted the token the `openpipeline:traces:ingest` scope on 2026-09-16, which cleared the
  403 that blocked the round. Evidence from that run: the `@OptionalService("otel.forwarding")` switch
  is the only thing that registers the route; one `POST /api/profile` produced 6 spans in 1 trace; and
  an **A-B-A credential experiment** establishes what the clean legs prove — real token 0/6 export
  failures, bogus token 6/6 failures with the 401 diagnostic, real token 0/6 again. So Dynatrace
  returned 2xx for every span rather than the forwarder silently skipping export. The app returned
  HTTP 201 in all three legs: a telemetry outage degrades observability and nothing else.
  **THE ONE RESIDUAL ITEM:** nobody has yet confirmed the traces are **queryable in the Dynatrace UI**
  under service `mercury-otel-cert` — "accepted for ingest" and "visible after pipeline processing"
  are different claims. Eric has no dashboard access yet and has asked Dynatrace support for it
  (2026-09-16). When it arrives: look for trace `e761110f7e3b4364a6210c1cba4728f4` (19:29:41Z, the
  restoration leg) and **update** the report's "What remains" section rather than rewriting it.
  Re-running the experiment needs nothing but the runbook in the report plus the two process-scoped
  variables; the literal Dynatrace auth-header form is in the commented example in
  `examples/composable-example/src/main/resources/application.properties` (not repeated here — the
  secret-scan guard cannot be waived for `memory/`, by design).
  **Already solved, do not re-diagnose:** the endpoint needs the full `/v1/traces` signal path, not
  the vendor's OTLP base (that was the 404); the 403 named its own missing scope in the response body;
  and a stale fat jar cost two dead ends, so verify the nested forwarder jar before every run.
  Also pending: docs still show `OTEL_SERVICE_NAME` in two generic examples while the example app uses
  `OTLP_SERVICE_NAME` — a caution note was added rather than renaming, deliberately.
  → serves: vision-mercury-composable
  <!-- id: ot-otel-dynatrace-certification | created: 2026-09-16 | last_used: 2026-09-16 | uses: 1 | tier: working | origin: 2026-09-16-175425 -->
