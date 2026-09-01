- [x] (field support — MERGED 2026-08-25/26:
  [PR #296](https://github.com/Accenture/mercury-composable/pull/296) +
  [PR #297](https://github.com/Accenture/mercury-composable/pull/297); shipped in
  v4.11.12; field re-scan cleared, Snyk PASSED) **Field scan remediation** (netty pin,
  memory-lint rename upstreamed, Sonar round 4 + catch-path coverage). Lesson: pair an
  S2139 rewrite of an uncovered catch block with a catch-path test in the same PR.
  origin: 2026-08-25-231243 + 2026-08-26-020650. Relates [[thread-sonar-4-11-x-field-round-3]].
  <!-- id: thread-field-scan-remediation-20260825 | created: 2026-08-25 | last_used: 2026-08-27 | uses: 5 | tier: archive-candidate | origin: 2026-08-25-231243 -->
