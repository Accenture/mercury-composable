- [ ] **Dynatrace OneAgent compatibility - integrating `feature/dynatrace-oneagent-compatibility` into the framework**
  (→ proposal: RFC-0009; serves [[vision-mercury-composable]]). The branch adds the live invocation-observer seam to
  platform-core (`org.platformlambda.core.spi`, hooked in `WorkerHandler`, `EventEmitter`'s Event Script dispatch and its five send
  entry points) plus two optional adapters, `extensions/dynatrace-oneagent` (OneAgent SDK, hops as linked messages) and
  `extensions/opentelemetry-tracer` (live OpenTelemetry API spans; REMOVED 2026-10-23, see below). **The field-snapshot gate is LIFTED, and the feature is REGULAR
  feature development, no longer experimental (Eric, 2026-10-23).** The plan of 2026-10-21 was to certify it in the field on
  a locally built snapshot, with no PR before that. With the support of Dynatrace's engineering team, the experiment runs
  instead from the dev machine against a Dynatrace sandbox environment, so a dev cycle is a rebuild, not a field release. The
  run below proved enough, so the branch goes to main through PR #583, with the lab committed to it, and the next release
  carries the feature. Next: Eric's
  decisions on the run's findings below, then the OpenTelemetry adapter's run in the lab. A run's checklist:
  `docs/guides/observability.md#live-tracing` - the PurePath tree, the `invocation.observer.unlinked` fragment (the negative
  control), the log ids per line, a Mono hop's duration, any `OneAgent SDK:` warnings, and the overhead. The branch keeps
  version 4.12.25 until the release round. The lab is NOT in the root reactor (Eric, 2026-10-23: the field's static
  analysis registers every root-reactor module for good); its own `lab/dynatrace-oneagent/pom.xml` aggregator builds the
  Mercury modules it uses from source, installs nothing into `~/.m2`, and joins the release version sweep.
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
  negative control - option B, DONE on #583: an unlinked route's scope closes before its reply leaves and the reply keeps
  the caller's tag, so the route alone is a fragment (lab: one Dynatrace trace per order plus the audit). Gotcha: OneAgent
  carries a thread's active PurePath into an executor task (`runTaskExecutor`), and an SDK tag started there is ignored.
  The key audit found the same collision in the released scheduler, fixed by #584 (MERGED)
  ([[conv-config-key-not-value-and-parent]]). The OpenTelemetry adapter under OneAgent (lab, 2026-10-23): spans recorded but never linked across the event bus -
  OneAgent links API spans only through its own thread context and ignores a W3C remote parent (SERVER the same, INTERNAL
  no span); its PurePath is not the OTel current context at the REST entry. Eric chose option B: the adapter is REMOVED
  from #583 (the seam stays vendor-neutral); option C, asking Dynatrace about W3C parents on API spans, was DROPPED (Eric).
  Production shape in the lab (no unlinked route): ONE Mercury trace = ONE Dynatrace trace across both applications; with a
  client `traceparent` the ids are even identical. Without one, Mercury now CONTINUES the agent's trace context, at the
  REST edge and at the Kafka consumer (`InvocationObserver.traceparent`, DONE on #583; lab 3 of 3): a Mercury trace id IS
  the Dynatrace trace id, and Mercury's first span hangs under OneAgent's node, so the lineage is continuous across apps.
  **FIELD-READY (Eric, 2026-10-23): "your current implementation is good to deploy to the field"** - after the sandbox
  runs showed one connected trace per request across both applications (the `Mono` warnings gone, every hop named
  `Topic: <application> - <route>` under one service, the default). #585 merged into the branch. Next: Eric's call on
  #583's review and merge, then the release round.
  <!-- id: dynatrace-oneagent-field-experiment | created: 2026-10-21 | last_used: 2026-10-21 | uses: 1 | tier: working | origin: 2026-10-22-003118 -->
