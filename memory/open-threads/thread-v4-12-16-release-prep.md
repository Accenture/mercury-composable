- [x] **v4.12.16 release prep — SHIPPED 2026-09-24 (Java #457 `df605533` → tag `dc0ee6fa`; Rust #324 `743d4ea2` → tag
  `cc138af3`; GitHub releases = the CHANGELOG entries; crates.io 12/12 by 00:12:52Z).** Sweep 43 / 98 (Java) and 13 / 24 +
  Cargo.lock (Rust) unchanged; CHANGELOGs from the tag ranges; readiness Java 1506 tests / 0 failures, Rust 603 / 0 / 9.
  Lessons: refresh memory footers BEFORE the PR handoff (a push seconds after the PR opened cancelled the Rust workflow's first
  run under its cancel-in-progress group); a Rust test run started right after the Java reactor build failed three timing-sensitive
  tests — environmental, confirmed by a clean re-run, but start it on an idle machine. origin: 2026-09-23-234558.md
  <!-- id: v4-12-16-release-prep | created: 2026-09-23 | last_used: 2026-09-23 | uses: 1 | tier: working | origin: 2026-09-23-230527 -->
