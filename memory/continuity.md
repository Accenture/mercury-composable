# Continuity — mercury-composable

> Shared ground truth for project state across all agents and sessions.
> Update at the end of every session. Never delete — only archive (see `REVIEW.md`).
>
> Each fact carries a metadata footer in an HTML comment, maintained by the review
> ritual — invisible when rendered, read/written by agents:
> `<!-- id: kebab-id | created: YYYY-MM-DD | last_used: YYYY-MM-DD | uses: N | tier: active -->`
> See `.agent/schema.md` for the fields and `memory/decay-policy.md` for the windows.

---

## Project State

- **project:** mercury-composable
- **status:** active, mature framework (Maven reactor)
- **repo:** github.com/Accenture/mercury-composable (official — source of truth)
- **last_enabled:** 2026-06-20
- **last_session:** 2026-06-20 | agent: Claude Code
- **last_review:** (none yet)
- **last_invariant_check:** (none yet)

> This agent-memory layer was seeded on 2026-06-20 from a prior prototyping
> environment, carrying forward only the confirmed Vision + Blueprint and the
> durable project facts — a clean start for the official repo (see the
> 2026-06-20 bootstrap session log).

## Stack & Tools

> Canonical live home for the current stack — language version, dependencies, tool
> versions. `instructions.md` keeps only a high-level descriptor and points here.

- Language: Java 21 (virtual threads); one Kotlin example module
  <!-- id: stack-language-java21 | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->
- Build: Maven 3.9.7+ (multi-module reactor, `com.accenture.mercury:parent-mercury`)
  <!-- id: stack-build-maven | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->
- Integration: Spring Boot (rest-spring-3 / -4 modules)
  <!-- id: stack-integration-spring | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->
- Messaging: Kafka connectors; MsgPack wire serialization; customized Gson
  <!-- id: stack-messaging-kafka | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->
- Persistence (extension): reactive PostgreSQL (R2DBC)
  <!-- id: stack-persistence-r2dbc | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->
- Docs: mkdocs (`docs_dir: docs`) → accenture.github.io/mercury-composable
  <!-- id: stack-docs-mkdocs | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->
- CI: GitHub Actions (`.github/workflows/`)
  <!-- id: stack-ci-gha | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->

## Architectural Invariants

> Hard constraints that must never change. These never decay (`core`).

- Functions are fully decoupled — coupled only by route-name strings and
  `EventEnvelope`; orchestration lives in YAML event flows, not code.
  <!-- id: functions-decoupled-routes | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: core -->
- `TypedLambdaFunction` I/O is Map or PoJo only — a List of PoJo is unsupported.
  <!-- id: typed-io-map-or-pojo | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: core -->

## Key Decisions

- Java 21 virtual threads throughout; synchronous PostOffice RPC ≈ reactive perf.
  <!-- id: virtual-threads-rpc | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->
- `pom.xml` is the source of truth for the version (drift observed across docs).
  <!-- id: pom-version-source-of-truth | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->
- Docs live under `docs/` (mkdocs `docs_dir: docs`): `guides/` → `docs/guides/` and
  `arch-decisions/` → `docs/arch-decisions/`, **keeping the `guides/` subfolder so
  published URLs stay `/guides/...`** (the MiniGraph webapp, help tutorials, and README
  link to those absolute URLs). Root GitHub files (README/CHANGELOG/CONTRIBUTING/
  CODE_OF_CONDUCT/INCLUSIVITY) stay at repo root as external nav links; `docs/index.md`
  is the site Home. Verified by `mkdocs build --strict` (exit 0).
  <!-- id: docs-dir-layout | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: working -->

## Conventions

- Add capability: function (`@PreLoad` + `TypedLambdaFunction`) → flow YAML →
  register in `flows.yaml` → `rest.yaml` mapping if HTTP-facing.
  <!-- id: conv-add-capability | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->
- Watch serialization gotchas (Long↔Integer downcast; use `util.str2int/str2long`).
  <!-- id: conv-serialization-gotchas | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->
- See `examples/composable-example` (`FlowTest`) as the canonical reference.
  <!-- id: conv-canonical-example | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->

## Blueprint  *(gap from Current State → Vision; `(blueprint)` threads serve `vision-mercury-composable`)*

- [ ] (blueprint) **Rewrite the documentation to be AI- and human-friendly** — the
  user-facing surface is the Active Knowledge Graph. **This is the first iteration.** → serves: vision-mercury-composable
  *Progress (2026-06-20): structural prerequisite landed — docs consolidated into `docs/` and the
  mkdocs build fixed (see `docs-dir-layout`). Pilot scope = restructure CHAPTER-10/11 (+ companion)
  for retrieval: self-contained sections, consistent headings, stable anchors, frontmatter +
  sidecar `llms.txt`. Content rewrite still pending.*
  <!-- id: bp-docs-ai-human-rewrite | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: working -->
- [ ] (blueprint) Integrate a **pluggable AI companion LLM backend**; mature `POST /api/companion/{id}`
  from a dev-only command pipe into a governed collaboration layer. → serves: vision-mercury-composable
  <!-- id: bp-ai-companion-llm-backend | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: working -->
- [ ] (blueprint) **Enterprise governance lifecycle** for graph models (dry-run → certify → stage →
  approve → production), so models promote to production as standard endpoints. → serves: vision-mercury-composable
  <!-- id: bp-graph-governance-lifecycle | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: working -->

## Open Threads

- [ ] No CI builds the docs site — the mkdocs config was broken-as-committed (no `docs_dir`,
  defaulted to absent `./docs`) and no workflow caught it (`.github/workflows/` builds Maven only).
  Now fixed on this branch; add a CI step running `mkdocs build --strict` so future doc breakages
  fail the build.
  <!-- id: thread-ci-docs-build | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: working -->

## User Preferences

- From the documentation-rewrite effort onward, the **official Accenture GitHub repo is the
  source of truth**; work directly here (not a separate prototyping repo) to keep a clean
  AI–Human commit log on the official repo.
  <!-- id: pref-github-source-of-truth | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->

## Team / Members

(none recorded yet)
