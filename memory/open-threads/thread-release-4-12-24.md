- [x] **Release v4.12.24 — SHIPPED 2026-10-12: the Guava resolution the field's Snyk gate rejected in 4.12.23, plus the range plugins'
  bounds and refinements, on both engines.** Java #559 squash `e1940a4d`, tag `v4.12.24` → `c571e9c3` (release 22:33:21Z); Rust #383 merge
  `de283537`, tag → `aa67a429` (release 22:29:09Z); the twelve crates at 22:31Z, the platform-core tarball identical to the tag; the packs
  stay at 4.12.21. Lesson: a library's `dependencyManagement` never reaches a consumer - a transitive version the library must control is
  a direct dependency, verified by a whole-reactor tree after installing the edited modules ([[conv-library-pins-are-direct-dependencies]]).
  Next: the field's Snyk result on 4.12.24 is the acceptance to record on `latest_release`; [[bp-agent-orchestration]] resumes on Bedrock.
  origin: 2026-10-12-221919; close 2026-10-12-223627.
  → serves: vision-mercury-composable
  <!-- id: release-4-12-24 | created: 2026-10-12 | last_used: 2026-10-12 | uses: 2 | tier: active | origin: 2026-10-12-221919 -->
