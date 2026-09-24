- [ ] **The release that carries the field's consumer-side Schema Registry identity (Eric, 2026-09-24: review the field's
  branch, reconstruct it here, then "propagate the fix to the field as a new release").** Java first — the field runs Java:
  PR #458 (from `feat/kafka-consumer-registry-identity` `13eddc76`) MERGED 2026-09-24 as squash `37e0f792`; the sample-config follow-up PR #459 MERGED as squash `af37ab7d` (both halves on main); the release prep at Eric's go — sweep BUILD FILES ONLY
  (re-derive the count), CHANGELOG from `git log v4.12.16..HEAD`, the readiness run, one memory commit past the merge as the
  tag target. Eric ruled 2026-09-24: sync to twin-kafka AND Rust before releasing **v4.12.17** — twin-kafka PR #460 MERGED
  as squash `47d9acf8`, Rust `SchemaCodec::for_consumer` (Increment 139) as
  mercury PR #327 MERGED as `7d5fbc17`. **Release prep 2026-09-24:** Java `release/4.12.17` `fcf44c83` (sweep 43 / 98,
  CHANGELOG from the tag range, readiness 1514 tests / 0 failures / 3 skipped); Rust `release/4.12.17` (13 / 24 + lock; its
  CHANGELOG carries Increments 137–139 — two Rust-only increments were unreleased since v4.12.16); PRs and tags Eric's. Relates [[kafka-consumer-registry-identity]], [[eric-release-rhythm]],
  [[conv-changelog-from-tag-range]], [[conv-ports-adopt-java-release-number]].
  <!-- id: kafka-consumer-identity-release | created: 2026-09-24 | last_used: 2026-09-24 | uses: 3 | tier: working | origin: 2026-09-24-222349 -->
