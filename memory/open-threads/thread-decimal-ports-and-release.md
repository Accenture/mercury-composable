- [x] **v4.12.20 post-release: field acceptance — CLOSED 2026-10-08, accepted with v4.12.21.** Outcome: the field never took
  4.12.20 on its own; v4.12.21 (Java tag → `87dd9e78`, carrying 4.12.20's `DECIMAL:` statement and canonical packager) passed the
  field's CI pipeline with the Snyk and Sonar scans clean and is deployed to the field (Eric, 2026-10-08) — recorded on the Java
  and Rust `latest_release` lines as FIELD-ACCEPTED. Lesson: a field-acceptance wait closes on the first release the field
  actually takes, which need not be the release that opened it. Origin: 2026-10-07-013257.md (the field-acceptance paragraph).
  → serves: vision-mercury-composable
  <!-- id: decimal-ports-and-release | created: 2026-10-01 | last_used: 2026-10-07 | uses: 2 | tier: active -->
