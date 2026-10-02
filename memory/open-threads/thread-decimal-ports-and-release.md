- [ ] **v4.12.20 post-release: field acceptance.** v4.12.20 is tagged, released and published on both repos (Java `9e515825`/tag `fc940bea`, Rust
  `b4783c5b`/tag `d63e102a`; GitHub releases 03:11Z and 03:12Z; main CI green on both tag commits; the twelve Rust crates verified 12/12 on crates.io at
  4.12.20, published 03:21:45Z-03:21:58Z, the published platform-core tarball matching the tag). **Open:** the field-acceptance wait (CI, Snyk, Sonar on
  v4.12.20), as for 4.12.19; the two docs-only interop-report PRs (composable #486, mercury #341) are separate. The python/node language packs need no
  change. Not part of this thread: the folder `pack`/`unpack` tooling and the graph-set loader RFC (deferred in ADR-0026), RFC-0003 (Parked) and RFC-0004
  (Withdrawn). Relates [[decimal-statement-exact-arithmetic]], [[canonical-packager-wire-contract]], [[eric-release-rhythm]].
  → serves: vision-mercury-composable
  <!-- id: decimal-ports-and-release | created: 2026-10-01 | last_used: 2026-09-30 | uses: 1 | tier: working -->
