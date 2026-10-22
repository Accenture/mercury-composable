- [ ] **Dynatrace OneAgent compatibility - the field experiment on a snapshot of `feature/dynatrace-oneagent-compatibility`**
  (→ proposal: RFC-0009; serves [[vision-mercury-composable]]). The branch adds the live invocation-observer seam to
  platform-core (`org.platformlambda.core.spi`, hooked in `WorkerHandler`, `EventEmitter`'s Event Script dispatch and its five send
  entry points) plus two optional adapters, `extensions/dynatrace-oneagent` (OneAgent SDK, hops as linked messages) and
  `extensions/opentelemetry-tracer` (live OpenTelemetry API spans). **Eric's rule (2026-10-21): no PR until the feature has been
  tested in the field as a snapshot release**; the field takes the branch as a snapshot, the results decide the adjustments, and
  the next release carries the finished feature. Next: the field runs both adapters in turn (procedure:
  `docs/guides/observability.md#live-tracing`) and reports, per adapter, the PurePath tree, the `invocation.observer.unlinked`
  fragment (the negative control), the log ids per line, a Mono hop's duration, any `OneAgent SDK:` warnings, and the overhead.
  The branch keeps version 4.12.25: a snapshot published to an artifact repository would collide with the release.
  <!-- id: dynatrace-oneagent-field-experiment | created: 2026-10-21 | last_used: 2026-10-21 | uses: 1 | tier: working | origin: 2026-10-22-003118 -->
