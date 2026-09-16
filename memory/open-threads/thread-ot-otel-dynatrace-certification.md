- [ ] (feature) **OpenTelemetry forwarder — Dynatrace certification run.** Code COMPLETE and green on
  branch `feature/otel-certification` (commit `2036b5b4`, unpushed): `otel.forwarding` opt-in switch
  (default false, `@OptionalService`), per-export credential resolution, and self-diagnosing export
  failures. **BLOCKED** on Dynatrace support granting the API token the `openpipeline:traces:ingest`
  scope (Eric raised the ticket 2026-09-16). Everything else is proven working end to end.
  **Resume runbook** (all three secrets stay in Eric's `.zshenv`; never echo or commit them):
  1. `mvn clean install -DskipTests -f extensions/opentelemetry-forwarder/pom.xml` then
     `mvn clean package -DskipTests -f examples/composable-example/pom.xml` — **verify the nested jar
     actually carries the change** (`unzip -p .../BOOT-INF/lib/opentelemetry-forwarder-*.jar ... | strings`);
     a stale fat jar cost two dead ends.
  2. Launch: `OTLP_AUTH_HEADER=<dynatrace-form> OTLP_SERVICE_NAME="mercury-otel-cert"
     java -Dotel.forwarding=true -jar examples/composable-example/target/composable-example-<ver>.jar`
     — take the literal `<dynatrace-form>` (header name + auth scheme) from the commented Dynatrace
     example in `examples/composable-example/src/main/resources/application.properties`; it is not
     repeated here because the secret-scan guard cannot be waived for `memory/`, by design. Both
     variables are process-scoped only; committed config keeps `otel.forwarding=false`.
  3. Transaction (id must be NUMERIC — `Profile.id` is an `Integer`; a string id 500s):
     `curl -X POST http://127.0.0.1:8100/api/profile -H 'content-type: application/json'
     -d '{"id":20260916,"name":"OTel Certification","address":"1 Trace Street","telephone":"123-456-7890"}'`
     → HTTP 201, and 6 spans in one trace (`http.flow.adapter` → `v1.create.profile` →
     {`async.http.response`, `v1.encrypt.fields` → `v1.save.profile`}, plus `task.executor`).
  4. Success = zero `OTLP export failed` lines. Redact the endpoint from any pasted output.
  **Then:** write `docs/test-reports/otel-dynatrace-certification.md` in the house format (frontmatter,
  scenario, evidence, observations) — Eric asked for it explicitly to "close the complete loop".
  **Already solved, do not re-diagnose:** the endpoint needs the full `/v1/traces` signal path, not
  Dynatrace's `/api/v2/otlp` base (was a 404); and the 403 names the missing scope in its response body.
  Also pending on this branch: docs still show `OTEL_SERVICE_NAME` in two generic examples while the
  example app uses `OTLP_SERVICE_NAME` — a caution note was added rather than renaming, deliberately.
  → serves: vision-mercury-composable
  <!-- id: ot-otel-dynatrace-certification | created: 2026-09-16 | last_used: 2026-09-16 | uses: 1 | tier: working | origin: 2026-09-16-175425 -->
