- [ ] **Dynatrace OneAgent compatibility - integrating `feature/dynatrace-oneagent-compatibility` into the framework**
  (→ proposal: RFC-0009; serves [[vision-mercury-composable]]). The branch adds the live invocation-observer seam to
  platform-core (`org.platformlambda.core.spi`, hooked in `WorkerHandler`, `EventEmitter`'s Event Script dispatch and its five send
  entry points) plus two optional adapters, `extensions/dynatrace-oneagent` (OneAgent SDK, hops as linked messages) and
  `extensions/opentelemetry-tracer` (live OpenTelemetry API spans). **The field-snapshot gate is LIFTED, and the feature is REGULAR
  feature development, no longer experimental (Eric, 2026-10-23).** The plan of 2026-10-21 was to certify it in the field on
  a locally built snapshot, with no PR before that. With the support of Dynatrace's engineering team, the experiment runs
  instead from the dev machine against a Dynatrace sandbox environment, so a dev cycle is a rebuild, not a field release. The
  run below proved enough, so the branch goes to main through PR #583, with the lab committed to it, and the next release
  carries the feature. Next: Eric's
  decisions on the run's findings below, then the OpenTelemetry adapter's run in the lab. A run's checklist:
  `docs/guides/observability.md#live-tracing` - the PurePath tree, the `invocation.observer.unlinked` fragment (the negative
  control), the log ids per line, a Mono hop's duration, any `OneAgent SDK:` warnings, and the overhead. The branch keeps
  version 4.12.25 until the release round, so the lab builds with `-am` and nothing is installed into `~/.m2`.
  **Sandbox run 2026-10-22 (a Dynatrace sandbox tenant, the lab `lab/dynatrace-oneagent/`, OneAgent 1.347.64 full stack in
  Colima's VM):** with the OneAgent adapter on, REST and the composable hops are ONE connected, nested PurePath (the RPC and
  the `Mono` included) where the baseline showed a single span, and every log line carries `dt.trace_id`/`dt.span_id`.
  Findings for Eric's decision (detail in 2026-10-23-005210): the switch collides with its `dynatrace.oneagent.*` settings in
  `ConfigReader` (rename to `dynatrace.oneagent.enabled`); `ManifestReader` reports OneAgent's version under OneAgent (a main
  fix); the negative control splits the rest of the flow; no link across Kafka (the tag is not a Kafka header); SDK
  in-process-link warnings where OneAgent's Reactor support already continues the path.
  **Eric's rulings 2026-10-23 (detail in 2026-10-23-215957):** (1) the switch is `dynatrace.oneagent.enabled` - DONE on
  #583; (4) the tag crosses Kafka as `x-mercury-tag-*` headers - DONE on #583, proven A-B-A in the lab (the worker joins
  the edge's trace; three Dynatrace traces per order become two); (2) `ManifestReader` - a separate PR from main; (3) the
  negative control - explained, awaiting his choice (document the split, or keep the caller's tag on the unlinked route's
  reply, recommended). The key audit found the same collision in the released scheduler - its own PR from main
  ([[conv-config-key-not-value-and-parent]]). Still to run: the OpenTelemetry adapter in the lab.
  <!-- id: dynatrace-oneagent-field-experiment | created: 2026-10-21 | last_used: 2026-10-21 | uses: 1 | tier: working | origin: 2026-10-22-003118 -->
