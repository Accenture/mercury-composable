- [ ] **v4.12.20 post-release: the crates.io publication and field acceptance.** v4.12.20 is tagged and released on both repos (Java
  `9e515825`/tag `fc940bea`, Rust `b4783c5b`/tag `d63e102a`; GitHub releases published 03:11Z and 03:12Z; main CI green on both tag commits).
  **Open:** (1) the twelve Rust crates are NOT on crates.io yet — at 03:14Z the sparse index and the API still ended at 4.12.19 for every
  crate; `cargo publish --workspace` from the tag is Eric's step, then verify 12/12 at 4.12.20 (index paths `me/rc/mercury-*`, API
  `crates.io/api/v1/crates/<name>/versions`, with a User-Agent); (2) the field-acceptance wait (CI, Snyk, Sonar on v4.12.20), as for 4.12.19.
  The python/node language packs need no change (they host functions over Event-over-HTTP and carry no graph, plugin or packager).
  Not part of this thread: the folder `pack`/`unpack` tooling and the graph-set loader RFC (deferred in ADR-0026), RFC-0003 (Parked) and
  RFC-0004 (Withdrawn). Relates [[decimal-statement-exact-arithmetic]], [[canonical-packager-wire-contract]], [[eric-release-rhythm]].
  → serves: vision-mercury-composable
  <!-- id: decimal-ports-and-release | created: 2026-10-01 | last_used: 2026-10-01 | uses: 4 | tier: working -->
