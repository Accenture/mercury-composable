- [ ] **v4.12.16 release prep (Java + Rust lock-step; Eric asked 2026-09-23 after PRs #456 / mercury #323 merged).**
  Content since v4.12.15: the shared null-source mapping rule, graph.math naming the unresolved variable, every
  dry-run abort carrying its reason (#456; Rust #323, Increment 136), the embedded-Redis OpenSSL prerequisite docs
  (#455), JaCoCo 0.8.15 (#452). Java: branch `release/4.12.16`, sweep BUILD FILES ONLY 43 files / 98 occurrences
  (re-derived, unchanged), CHANGELOG from `git log v4.12.15..HEAD`, full `mvn clean install` as readiness. Rust:
  branch `release/4.12.16`, 13 manifests / 24 occurrences + Cargo.lock (`cargo update --workspace`), CHANGELOG twin
  entry; gates fmt, clippy, claims, links, mkdocs green — `cargo test --workspace` runs AFTER the Java build (shared
  test ports). Then: PRs opened by Eric, merge gates, tags one memory commit past each merge, GitHub releases = the
  CHANGELOG entries, crates.io publish by Eric from the tag (clean tree), `latest_release` updates at the seam close.
  <!-- id: v4-12-16-release-prep | created: 2026-09-23 | last_used: 2026-09-23 | uses: 1 | tier: working | origin: 2026-09-23-230527 -->
