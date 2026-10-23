- [ ] **Dynatrace OneAgent compatibility - the field experiment on a snapshot of `feature/dynatrace-oneagent-compatibility`**
  (→ proposal: RFC-0009; serves [[vision-mercury-composable]]). The branch adds the live invocation-observer seam to
  platform-core (`org.platformlambda.core.spi`, hooked in `WorkerHandler`, `EventEmitter`'s Event Script dispatch and its five send
  entry points) plus two optional adapters, `extensions/dynatrace-oneagent` (OneAgent SDK, hops as linked messages) and
  `extensions/opentelemetry-tracer` (live OpenTelemetry API spans). **Eric's rule (2026-10-21): no PR until the feature has been
  tested in the field as a snapshot release**; the field takes the branch as a snapshot, the results decide the adjustments, and
  the next release carries the finished feature. **Eric, 2026-10-23: a PR is opened to track the branch, with the lab committed to it.** Next: the field runs both adapters in turn (procedure:
  `docs/guides/observability.md#live-tracing`) and reports, per adapter, the PurePath tree, the `invocation.observer.unlinked`
  fragment (the negative control), the log ids per line, a Mono hop's duration, any `OneAgent SDK:` warnings, and the overhead.
  The branch keeps version 4.12.25: a snapshot published to an artifact repository would collide with the release.
  **Sandbox run 2026-10-22 (a Dynatrace sandbox tenant, the lab `lab/dynatrace-oneagent/`, OneAgent 1.347.64 full stack in
  Colima's VM):** with the OneAgent adapter on, REST and the composable hops are ONE connected, nested PurePath (the RPC and
  the `Mono` included) where the baseline showed a single span, and every log line carries `dt.trace_id`/`dt.span_id`.
  Findings for Eric's decision (detail in 2026-10-23-005210): the switch collides with its `dynatrace.oneagent.*` settings in
  `ConfigReader` (rename to `dynatrace.oneagent.enabled`); `ManifestReader` reports OneAgent's version under OneAgent (a main
  fix); the negative control splits the rest of the flow; no link across Kafka (the tag is not a Kafka header); SDK
  in-process-link warnings where OneAgent's Reactor support already continues the path.
  <!-- id: dynatrace-oneagent-field-experiment | created: 2026-10-21 | last_used: 2026-10-21 | uses: 1 | tier: working | origin: 2026-10-22-003118 -->
