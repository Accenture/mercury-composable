- [x] **Upstream `[undeclared-reference]` check — SHIPPED in agent-memory v4.41.0 and VERIFIED
  2026-09-17.** Our report (2026-09-17) proposed intersecting a change's own diff with its own session
  logs; v4.41.0 (`a45d7572`, #413) implements it and goes further — a `--range` mode for the CI floor
  as well as `--staged` for pre-commit, `memory/vision.md` and open-threads as surfaces alongside
  continuity, and a message that distinguishes "closes the fact" from "edits the fact". Advisory and
  non-blocking, as recommended.
  **Verified by reproduction, not by changelog** (the habit from the v4.40.1 round): fires on a
  continuity/vision/open-thread body edit that no staged log declares; silent on a footer-only
  refresh, on a commit with no log staged, on a declared edit, on condensing an already-closed record,
  and on a deletion. Decisively, `--range` over the **actual historical commits** flags
  `bp-ai-companion-llm-backend` at `903694f0` and `bp-graph-governance-lifecycle` at `41bac544` — the
  exact two misses — while staying silent on `9e5b5577` and `90c8d9c4`, which were a closed-record
  edit and a properly-declared one. No false positives on the incident that motivated it.
  **Durable lesson, and it is about me:** my first verification harness reported four clean passes
  that were all meaningless — every case edited a fact the log already declared, so nothing tested the
  positive path; two later runs then mislabelled results through a loose `grep` and a `||` bound to
  the wrong pipeline stage. Three harness faults, zero tool faults. **A verification harness needs its
  own positive control: prove it can report failure before trusting it to report success**
  ([[otel-optional-service-and-negative-control]] says the same about evidence; it applies to the
  instrument too).
  origin: `memory/sessions/2026-09-17-183008.md`
  <!-- id: ot-undeclared-reference-check | created: 2026-09-17 | last_used: 2026-09-17 | uses: 1 | tier: working | origin: 2026-09-17-183008 -->
