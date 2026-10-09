- [x] **The mini-scheduler's active environment (RFC-0008) — CLOSED 2026-10-09 at the promotion to ADR-0030 (PR #540 squash `b8d6cbd2`).**
  Outcome: `scheduler.environment` names the instance's environment; the active environment lives in a data store the application
  provides (`scheduler.environment.store`, default `v1.environment.store`), read at every scheduled point; a standby instance skips a
  scheduled job at execution and honours an operator's run; the example's `EnvironmentStore` template and `GET`/`POST
  /api/scheduler/environment` switch; delivered in PR #539 (squash `74212b1e`), documented with the claim `scheduler-active-environment-standby`.
  Lesson: when a value has a source of truth, do not also give it a property (the first draft's `scheduler.active.environment` was
  dropped at Eric's review). Fact: [[scheduler-active-environment-store]] (ADR-0030). Next: v4.12.22, then [[bp-agent-orchestration]]
  resumes. origin: 2026-10-09-045453.
  <!-- id: scheduler-active-environment | created: 2026-10-09 | last_used: 2026-10-09 | uses: 3 | tier: working | origin: 2026-10-09-045453 -->
