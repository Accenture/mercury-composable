- [x] **Dynatrace OneAgent compatibility** (→ RFC-0009 → ADR-0032; serves [[vision-mercury-composable]]; closed by Eric 2026-10-24).
  - **Outcome:** #583 (squash `a09a1535`), with #584 and #585 found on the way: one Mercury request is one connected Dynatrace
    trace with the same id across REST, Kafka and two applications; report `docs/test-reports/dynatrace-one-agent-integration.md`.
  - **Lesson:** OneAgent links an executor handoff from a thread with an active PurePath by itself, and an API span only through
    that thread context, so W3C parents do not link OpenTelemetry API spans under it. See [[live-invocation-observers]].
  - origin: 2026-10-22-003118 (narrative in 2026-10-23-005210 and 2026-10-23-215957; closed in 2026-10-24-013405)
  <!-- id: dynatrace-oneagent-field-experiment | created: 2026-10-21 | last_used: 2026-10-24 | uses: 8 | tier: active | origin: 2026-10-22-003118 -->
