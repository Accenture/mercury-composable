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
  → serves: vision-mercury-composable
  <!-- id: scheduler-active-environment | created: 2026-10-09 | last_used: 2026-10-09 | uses: 1 | tier: working | origin: 2026-10-09-045453 -->
