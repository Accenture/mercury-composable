- [x] **Release v4.12.27 — SHIPPED 2026-10-30 on both engines for the field to test: the Snyk fix for 4.12.26 (Tomcat 11.0.27,
  #594), a scheduled job's time zone (#599) and the `minimalist-kafka` round (#597, #598).** #600 squash `2929ea31` = tag
  `v4.12.27` (release 23:18:11Z, notes = the CHANGELOG section); 265 suites / 2802 tests. Rust #400 merge `03952aee` = tag (release
  23:15:12Z), crates 12/12 at 23:17Z. Lesson: a field-facing record format change (the resolver's `schedule`) ships with its
  storage check in the upgrade note - the field checks its column and dashboard before opting in. Next: record the field's CI,
  Snyk and Sonar result on `latest_release`.
  origin: 2026-10-30-225722; close 2026-10-30-231959.
  → serves: vision-mercury-composable
  <!-- id: release-4-12-27 | created: 2026-10-30 | last_used: 2026-10-30 | uses: 1 | tier: working | origin: 2026-10-30-225722 -->
