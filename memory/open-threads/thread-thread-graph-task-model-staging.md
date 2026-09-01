- [x] (feature+fix — COMPLETE BOTH ENGINES 2026-08-08, shipped in v4.11.5: Java
  [PR #267](https://github.com/Accenture/mercury-composable/pull/267)/[PR #268](https://github.com/Accenture/mercury-composable/pull/268);
  Rust PR #197) **graph.task input mapping gains `model.*` staging; tutorial-13 onto
  async.http.request with x-ttl propagation.** Lesson: never rely on an HTTP library's
  implicit Accept — declare `headers.accept` or JSON decoding silently differs across
  engines. origin: 2026-08-09-025009.
  <!-- id: thread-graph-task-model-staging | created: 2026-08-08 | last_used: 2026-08-25 | uses: 3 | tier: archive-candidate | origin: 2026-08-09-025009 -->
