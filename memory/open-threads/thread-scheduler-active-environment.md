- [ ] **The mini-scheduler runs scheduled jobs in ONE environment: an active-environment switch for active-active Production/DR
  deployments (Eric's problem statement, 2026-10-09; high priority, ahead of v4.12.22).** A field installation deploys the scheduler
  in Production and DR active-active; the DR scheduler fires the same jobs, whose flows publish to the DR Kafka cluster, and the DR
  applications - which must stay idle while Production is up - consume them: unintended side effects. Eric's solution sketch: an
  application property naming this instance's environment (sketched as `app.env=${ENV_NAME}`), an active environment that defaults
  to `prod`, the scheduler running its jobs only when its own environment is the active one and standing by otherwise, and a REST
  call from the operations dashboard that sets the active environment (`prod` ↔ `DR`) around a Production maintenance window.
  Open design points (Claude Code, same day): the property name - `app.env` already selects the Playground's dev mode
  (`@OptionalService("app.env=dev")`, 18 services) so the scheduler needs a name of its own; where the active value lives (per
  instance in memory, or the application's state resolver); whether standby means jobs are not fired or fired and skipped at the
  resolver; the REST shape in the example and the field's app; and the cron.yaml/application.properties split. Governing facts:
  [[event-script-over-code]] (the REST edge wires through rest.yaml), [[kafka-mesh-opt-in]] (the existing leader election is
  in-cluster, not cross-site). Gate: unit tests on `mini-scheduler` and `scheduler-example`; the configuration reference and the
  module README. Then v4.12.22, then [[bp-agent-orchestration]] resumes.
  **Decided (Eric, 2026-10-09):** the two properties as proposed, `scheduler.environment=${ENV_NAME:prod}` and
  `scheduler.active.environment=prod`; the active environment PERSISTED through a function the field implements against its own
  database (a template like the example's `StateResolver`), because a pod restarts without notice. **Delivered 2026-10-09 on
  `feat/scheduler-active-environment` (`1d116170`; PR #539 opened by Eric, CI running):** `ActiveEnvironment` in the module (status, refresh from the
  store before every scheduled job, activate = persist then memory), the executor's standby skip with one log line and the honoured
  operator run, the startup announcement (a WARN when no store is registered); the example's file-backed `EnvironmentStore`
  (`v1.environment.store`, the `get`/`set` contract) and `EnvironmentAdmin` behind `GET`/`POST /api/scheduler/environment`; the
  configuration reference's three properties, both READMEs, CHANGELOG Added 7, the claim `scheduler-active-environment-standby`
  (pinned by the probe job the test fires through the executor in both modes) and RFC-0008 (Open; promotion after the merge).
  **Eric's review on PR #539 (2026-10-09):** with the data store as the source of truth, `scheduler.active.environment` is not
  required - dropped in `51731690`; the active environment is `prod` until an operator sets one, the store is read at every scheduled
  point (confirmed: `isActiveNow()` → `refresh()` before each scheduled job), the last value read is the fallback for a store
  that fails to answer.
  **PR #539 MERGED 2026-10-09 as squash `74212b1e`** (identical to `51731690` outside `memory/`; main fast-forwarded, the branch
  deleted). **Promotion (Eric: "1 and 3 first"):** ADR-0030 written and RFC-0008 marked promoted on `docs/adr-0030-scheduler-active-environment` (`22f7e844`; CHANGELOG Added 8; SkillSnapshotTest and the canon green), the fact [[scheduler-active-environment-store]] created; PR for Eric to open, the thread closes at its merge. Then v4.12.22.
  → serves: vision-mercury-composable
  <!-- id: scheduler-active-environment | created: 2026-10-09 | last_used: 2026-10-09 | uses: 1 | tier: working | origin: 2026-10-09-045453 -->
