- [x] **An in-house JSONPath engine to RFC 9535 replaces Jayway JsonPath** (RFC-0010 → ADR-0031; serves [[vision-mercury-composable]]).
  - **Outcome:** five work packages on both engines, 2026-10-22: the module (compliance suite 715 of 715), the swap, refusal at
    deployment, substitution checks and the docs. PR #579 squash `68637dd6`; mercury #391, #392 and #393; ships in v4.12.26.
  - **Lesson:** a promise in a guide is a contract. The plugin-argument form had no use anywhere but was documented, so it was kept
    and its splitter fixed rather than refused; and a check that preserves each form's existing rendering is what let the
    safeguard land with no working query broken.
  - Decision fact [[minimalist-jsonpath-rfc-9535]]; origin: 2026-10-22-040817 (narrative in the 2026-10-22 logs through 2026-10-22-175132).
  <!-- id: minimalist-jsonpath | created: 2026-10-22 | last_used: 2026-10-22 | uses: 10 | tier: active | origin: 2026-10-22-040817 -->
