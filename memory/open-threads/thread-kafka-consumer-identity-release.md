- [ ] **The release that carries the field's consumer-side Schema Registry identity (Eric, 2026-09-24: review the field's
  branch, reconstruct it here, then "propagate the fix to the field as a new release").** Java first — the field runs Java:
  PR #458 (from `feat/kafka-consumer-registry-identity` `13eddc76`) MERGED 2026-09-24 as squash `37e0f792`; the release prep at Eric's go — sweep BUILD FILES ONLY
  (re-derive the count), CHANGELOG from `git log v4.12.16..HEAD`, the readiness run, one memory commit past the merge as the
  tag target. Decisions pending with Eric: twin-kafka's secondary-consumer twin in the same release or later; Rust lockstep
  (same one-codec shape and seam, no CSFLE) or lag. Relates [[kafka-consumer-registry-identity]], [[eric-release-rhythm]],
  [[conv-changelog-from-tag-range]], [[conv-ports-adopt-java-release-number]].
  <!-- id: kafka-consumer-identity-release | created: 2026-09-24 | last_used: 2026-09-24 | uses: 1 | tier: working | origin: 2026-09-24-222349 -->
