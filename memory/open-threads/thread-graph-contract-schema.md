- [x] **The graph contract (RFC-0007) — CLOSED 2026-10-09 at the promotion to ADR-0029 (PR #537 squash `647ad3d2`).** Outcome: a
  `schema` on the root and end nodes (`body`/`header`), a closed OpenAPI 3.0 keyword subset the gate enforces, input validation at the
  root as a step the engine assumes (`graph.schema.validator`), the OpenAPI document on demand and the Playground's Schema panel,
  delivered in four work packages on both engines (Java #533–#536, Rust #370–#373, Increments 171–174) and documented
  (`docs/guides/knowledge-graph/graph-contract.md`). Lesson: a gate that fires on one engine names a file to check on the twin before
  the push (the skill inventory, mercury #373). Fact: [[graph-contract-by-declaration]] (ADR-0029). origin: 2026-10-08-190305.
  <!-- id: graph-contract-schema | created: 2026-10-08 | last_used: 2026-10-09 | uses: 6 | tier: working | origin: 2026-10-08-190305 -->
