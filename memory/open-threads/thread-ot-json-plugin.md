- [x] **`json` simple plugin — land the PR pair (Java + Rust lock-step).** DONE 2026-09-02:
  PR #311 squash `1519dc0f` (this repo) + mercury PR #225 squash `b049dc67`, both CI-green
  before merge (checks watched to completion — no late-failure repeat). Feature: `f:json(...)`
  parses JSON text into a live dataset in one mapping statement; 2 review fixes rode along
  (null → clean error; malformed JSON → 400 not 500). Lesson: a new simple plugin's real
  review surface is the argument pipeline (tokenizer, substitution, null path, top-level
  exception mapping), not the plugin body. Demo-repo response letter updated (item 7.3).
  origin: 2026-09-02-163132
  <!-- id: ot-json-plugin | created: 2026-09-02 | last_used: 2026-09-02 | uses: 2 | tier: archive-candidate | origin: 2026-09-02-163132 -->
