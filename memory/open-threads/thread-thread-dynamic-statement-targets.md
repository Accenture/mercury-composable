- [x] (feature — COMPLETE BOTH ENGINES 2026-08-10, shipped in v4.11.6: Java
  [PR #273](https://github.com/Accenture/mercury-composable/pull/273) squash `96d9c35f`
  + recovery semantics
  [PR #274](https://github.com/Accenture/mercury-composable/pull/274) squash `5a01c0c6`;
  Rust PRs #201/#202) **Dynamic `{namespace.key}` variables in every statement command
  (NEXT/THEN/ELSE/RESET/DELAY) — completes the generic error handler; a successful
  retry of `error.source` RESOLVES the virtual error node.** Lesson: unresolved targets
  fail loudly (jump to "null"), RESET no-ops, DELAY skips.
  origin: 2026-08-10-223319.
  <!-- id: thread-dynamic-statement-targets | created: 2026-08-10 | last_used: 2026-08-25 | uses: 2 | tier: archive-candidate | origin: 2026-08-10-223319 -->
