- [ ] (docs backlog — Eric, 2026-06-24) **Documentation improvement — serve both audiences, every
  sprint.** Acceptance criteria for every doc change: **for humans — storytelling** (engaging,
  why-before-how, a narrative arc); **for AI agents — token-efficient** (shortest path to the
  point, machine-greppable, "generate from this page alone"). Backlog (from two fresh-agent
  discovery passes, 2026-06-24; re-validate each pass with the fresh-agent test):
  - **Biggest gap: an AI-agent "boot & test an app" recipe** (AutoStart.main, minimal
    application.properties, `@PreLoad` base-package auto-scan, the AsyncHttpRequest contract,
    server-readiness via `Platform.waitForProvider("async.http.response")` not Thread.sleep).
  - **Surface the working test-fixture pattern** (TestBase + service function + test rest.yaml) —
    highest-signal context, lives only in src/test.
  - **Machine-readable runtime-API signatures** (like the DSL *.json catalogs) for
    AsyncHttpRequest/AutoStart/AppConfigReader; and surface the catalogs in llms.txt as
    first-class entries (+ repo-relative links so an in-repo agent maps page→file in one hop).
  - **Reserved-route extension contract + machine-readable dataset schema** for
    `distributed.trace.forwarder` / `transaction.journal.recorder` (trace-metrics map shape).
  - **"Author a reusable extension" recipe + the auto-registration fact** (`@PreLoad` under
    `org.platformlambda.*`/`com.accenture.*` is always scanned — jar on classpath = route).
  - **Document `${ENV_VAR:default}` config substitution** (unset `${VAR}` with no default → null).
  - **Drive an Event Script flow programmatically** (`FlowExecutor.request(...)`) + the synthetic
    `task.executor` flow-summary span.
  → serves `vision-mercury-composable`. Full detail: sessions of 2026-06-24.
  <!-- id: thread-docs-improvement-backlog | created: 2026-06-24 | last_used: 2026-08-24 | uses: 10 | tier: working -->
