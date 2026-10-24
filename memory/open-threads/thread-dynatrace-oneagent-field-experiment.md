- [x] **Dynatrace OneAgent compatibility - integrating `feature/dynatrace-oneagent-compatibility` into the framework**
  (→ RFC-0009, promoted to ADR-0032; serves [[vision-mercury-composable]]; closed by Eric 2026-10-24).
  - **Outcome:** MERGED as #583 (squash `a09a1535`), with #584 (the scheduler key collision) and #585 (the version under OneAgent)
    found on the way. Verified against a Dynatrace sandbox environment: one Mercury request is one connected Dynatrace trace across REST,
    every function, Kafka and a second application, with the same trace id; Mercury's own span tree unbroken (`./lab.sh lineage`).
    Judged field-ready by Eric; report `docs/test-reports/dynatrace-one-agent-integration.md`. The field's own run follows the release.
  - **Lesson:** with OneAgent, a hop handed to an executor on a thread with an active PurePath is linked by OneAgent itself, and an API
    span links only through that thread context - so a link is broken by ending the path, and W3C parents do not link OpenTelemetry API
    spans under OneAgent (the adapter was removed). See [[live-invocation-observers]].
  - origin: 2026-10-22-003118 (the narrative is in 2026-10-23-005210 and 2026-10-23-215957; closed in 2026-10-24-013405)
  <!-- id: dynatrace-oneagent-field-experiment | created: 2026-10-21 | last_used: 2026-10-21 | uses: 1 | tier: working | origin: 2026-10-22-003118 -->
