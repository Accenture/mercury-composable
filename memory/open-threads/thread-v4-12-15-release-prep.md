- [ ] **v4.12.15 release preparation — the survey's fold-ins (Eric agreed 2026-09-22).** Before the four-repo
  release round (Java/Rust `v4.12.14..HEAD`, packs `v4.12.1..HEAD`; the packs adopt 4.12.15 per
  [[conv-ports-adopt-java-release-number]]; CHANGELOGs from the tag ranges per [[conv-changelog-from-tag-range]];
  sweeps per [[conv-template-version-sweep]] incl. [[snyk-retired-manifest-placeholders]]): (a) the packs' CHANGELOG
  `Unreleased (4.12.15 in preparation)` carries two `### Changed` headings each — merge and retitle; (b) the Rust
  `docs/guides/http-streaming.md` lacks the *Tracing a stream* section the Java guide gained in the connected-spans
  round; (c) three Java draft-spec status headers are stale — `second-level-routing-kafka-flow-adapter.md`
  ("implementation not yet started"), `http-response-streaming.md` ("Rust twin pending"),
  `subgraph-suspend-resume-for-each.md` ("Not implemented") — one docs PR; (d) stale memory to close at the seam:
  the Rust `ot-minimalist-kafka-port` thread is still `[ ]` though K1–K5 shipped in 4.12.14 (its tail says crates.io
  waits for K5; its "auto = classic" question was answered by the optimistic rebuild), the Rust continuity's "Eric's
  Dynatrace lookup is the remaining gate" (confirmed since), the Python/Node `otel-forwarder-lockstep` threads close
  at 4.12.15; (e) memory health — this repo's review is overdue by size (39 > 35 facts, 1015 > 1000 lines), the Rust
  lint reports skipped metadata refresh; (f) cite the 2026-09-22 four-runtime Dynatrace drives as the packs'
  per-release interop evidence (`bp-publish-interop-gate` — its "no build workflow" claim is outdated, both packs
  ship `ci.yml`). **Release gate CLEARED 2026-09-23:** P10 merged on both engines (Java #449 `b4051b33`, Rust #319 `49403c23`) and
  the Rust note-3 retry merged (#320 `34ff82d6`). Fold-ins (b) and (c) are branches awaiting PRs: Rust
  `docs/http-streaming-tracing` (`11a27336`), Java `docs/draft-spec-status-headers` (`13c12034`). **Carry-overs for AFTER 4.12.15:** the Rust `async.http.response` graph-run record has no `from` (Java
  `from=task.executor`); an optional CLIENT span kind for `async.http.request`; `minigraph-state-redis` still drives
  its own Redis manager without the lifecycle retry; the parked engine items (CompileGraph static output-mapping
  check, Rust async-callback parity check, typed cache helper, sync SSE client form; the Playground UI path is DONE);
  the Splunk header form never run live, the Rust gzip delta, a real-provider token stream when Gemini quota returns;
  the AI SDLC/MCP backlog ([[bp-agent-orchestration]]) resumes after 4.12.15 (Eric).
  <!-- id: v4-12-15-release-prep | created: 2026-09-22 | last_used: 2026-09-22 | uses: 1 | tier: working | origin: 2026-09-22-235615 -->
