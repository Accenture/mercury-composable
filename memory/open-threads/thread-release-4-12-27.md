- [ ] **Release v4.12.27 — PREPARED 2026-10-30 on both engines for the field to test: the Snyk fix for 4.12.26 (Tomcat 11.0.27 with
  Spring Boot 4.1.2 and the library round, #594), the mini-scheduler job's time zone (#599), and the `minimalist-kafka`
  dead-letter key and schema-cache round (#597, #598).** Branch `release/4.12.27` in each repo: the CHANGELOG cut under a "Nothing yet"
  Unreleased, the sweep 50 build files / 114 plus the lab's three literals. Next, Eric's gates ([[eric-release-rhythm]]): open
  and merge the PR, tag `v4.12.27`, publish the GitHub release with the CHANGELOG section as its notes; then verify the tag
  commit (the pom at the tag, CI), rewrite `latest_release` (4.12.26 to Prior), close this thread, and record the field's CI,
  Snyk and Sonar result. In lock-step with Rust 4.12.27 (Eric, 2026-10-30), prepared on the Rust repo's `release/4.12.27`.
  origin: 2026-10-30-225722.
  → serves: vision-mercury-composable
  <!-- id: release-4-12-27 | created: 2026-10-30 | last_used: 2026-10-30 | uses: 1 | tier: working | origin: 2026-10-30-225722 -->
