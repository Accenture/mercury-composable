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
- **last_session:** 2026-06-22 | agent: Claude Code
- **last_review:** 2026-06-21 | through 2026-06-22-003844.md
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
  <!-- id: docs-dir-layout | created: 2026-06-20 | last_used: 2026-06-21 | uses: 3 | tier: active -->
- Documentation rewrite (the Design for `bp-docs-ai-human-rewrite`) = a **structural, layered
  re-architecture** into Parts I–VI ascending the layers, centering **"Knowledge Graph as
  application"** (Part IV, mostly new — the current CHAPTER-11 is a glossy stub). **Dual design:**
  human narrative spine (why-before-how, story arc) + AI direct-discovery (`docs/llms.txt` map,
  per-doc YAML frontmatter `summary/layer/audience/keywords/related`, consistent heading taxonomy,
  stable anchors, "At a glance" blocks). **Semantic slug URLs** + mkdocs `redirects` for the old
  `/guides/CHAPTER-N/` links (webapp + help tutorials reference those absolutely). **Process:** build
  new chapters alongside old → per-layer sign-off → retire superseded docs (APPENDIX-I,
  old CHAPTER-11) + fix nav/cross-links/baked URLs. First iteration = spine opener + Part IV.
  Approved by Eric Law 2026-06-20 (3 forks confirmed). Lower-layer chapters (CH 1–9) are sound —
  migrate + refresh, don't rewrite from scratch.
  <!-- id: docs-rewrite-architecture | created: 2026-06-20 | last_used: 2026-06-21 | uses: 4 | tier: active -->
- Each DSL gets a deterministic **spec layer for AI agents**: a rule-based grammar reference +
  a machine-readable catalog (JSON) + an AI-agent guide (endpoint contract + pre-send checklist) +
  a CI **drift test** keeping the spec in sync with the shipped help + engine routes. **Validation
  method:** a clean-context fresh AI agent must build from the spec docs ALONE (no source); the gaps
  it flags drive doc fixes. Done for MiniGraph (`docs/guides/knowledge-graph/command-reference.md`,
  `minigraph-commands.json`, `ai-agent-guide.md`, `scripts/check-minigraph-grammar.py`,
  `.github/workflows/docs.yml`); 2 fresh-agent passes closed config-node-wiring, `response.*`, and
  type-casing gaps. **Event Script** spec added too (`docs/guides/event-script/`,
  `scripts/check-event-script-grammar.py`; drift-checks against `CompileFlows.EXECUTION_TYPES`),
  fresh-agent-validated over 2 passes (closed whole-result capture, bare input-body target,
  name/next resolution). **REST automation** spec added too (`docs/guides/rest-automation/`,
  `scripts/check-rest-automation-grammar.py`; drift-checks against `RoutingEntry.VALID_METHODS`),
  fresh-agent-validated. **All 3 DSLs (MiniGraph, Event Script, REST) now have the deterministic
  spec kit + CI drift test.**
  <!-- id: docs-dsl-spec | created: 2026-06-20 | last_used: 2026-06-21 | uses: 3 | tier: active -->
- **Finalized doc-style conventions** (the consistency pass after the migration was declared "done";
  3 forks decided by Eric Law 2026-06-22): (1) **ALL docs use lowercase-kebab semantic slugs** — every
  remaining ALL-CAPS file was renamed (`ARCHITECTURE`→`architecture`, `METHODOLOGY`→`methodology`,
  `COMPOSABLE-DESIGN`→`composable-design`, `QUICKSTART`→`quickstart`, the `*-REFERENCE` set→lowercase,
  `APPENDIX-II`→`reserved-names-and-headers`, `APPENDIX-III`→`actuators-and-http-client`, and
  `CHAPTER-10`→`knowledge-graph/property-graph.md` (co-located into Part IV)); each old path keeps an
  `mkdocs-redirects` entry. (2) **Every content doc carries the full pattern** — frontmatter +
  "At a glance" + "See also"; **reference docs get At-a-glance too** (not exempt — so it is not later
  flagged as drift). (3) **`TABLE-OF-CONTENTS` is retired** (redirect → Home; the Part I–VI sidebar
  nav is the table of contents). The published-URL safety net is the redirect map; live sources (docs,
  README, llms.txt) are repointed to the new slugs, CHANGELOG (historical) is left to the redirect.
  <!-- id: docs-style-conventions | created: 2026-06-22 | last_used: 2026-06-22 | uses: 1 | tier: working -->

## Conventions

- Add capability: function (`@PreLoad` + `TypedLambdaFunction`) → flow YAML →
  register in `flows.yaml` → `rest.yaml` mapping if HTTP-facing.
  <!-- id: conv-add-capability | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->
- Watch serialization gotchas (Long↔Integer downcast; use `util.str2int/str2long`).
  <!-- id: conv-serialization-gotchas | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->
- See `examples/composable-example` (`FlowTest`) as the canonical reference.
  <!-- id: conv-canonical-example | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->

## Blueprint  *(gap from Current State → Vision; `(blueprint)` threads serve `vision-mercury-composable`)*

- [x] (blueprint) **Rewrite the documentation to be AI- and human-friendly** — the
  user-facing surface is the Active Knowledge Graph. **This is the first iteration.** → serves: vision-mercury-composable
  *Progress (2026-06-20): (1) structural prerequisite landed — docs consolidated into `docs/`,
  mkdocs build fixed (see `docs-dir-layout`). (2) Design approved — the layered re-architecture
  in `docs-rewrite-architecture`. (3) Part IV opener `guides/knowledge-graph/index.md` written
  (code-true, grounded in the live MiniGraph engine + shipped help tutorials) + `docs/llms.txt`
  AI map; the doc PATTERN (frontmatter, At-a-glance, narrative, stable anchors, honest
  built-vs-roadmap) is **accepted** by Eric Law 2026-06-20. Next: deeper Part IV chapters
  (build-a-graph walkthrough, 7-skills reference, layer-integration how-to, Playground & companion),
  then spine refinement, then retire CH-11 behind a redirect. Lower-layer chapters migrate+refresh later.
  (4) Part IV chapters written + committed (overview, build-your-first-graph, skills-reference,
  composing-the-layers, playground-and-companion — all code-true, mkdocs --strict green); sent to
  Eric for batch review. Remaining: refine spine opener (`docs/index.md`), retire CH-11 behind a
  redirect (needs `mkdocs-redirects`), migrate+refresh lower-layer chapters (Parts I–III, V–VI).
  (5) Added the MiniGraph **DSL spec layer** for deterministic AI generation (see `docs-dsl-spec`)
  + the docs CI gate; fresh-agent-validated. (6) Event Script DSL spec added + fresh-agent-validated
  (2 passes); REST automation spec added + 2-pass-validated — **all 3 DSLs done**. (7) Spine opener
  (`docs/index.md`) refined into the paradigm story + CHAPTER-11 retired behind an mkdocs redirect
  (`mkdocs-redirects` added to plugins + CI; `/guides/CHAPTER-11/` → `/guides/knowledge-graph/`; in-docs
  refs repointed). (8) Lower-layer migration **started**: the whole nav is restructured into the
  **Part I–VI skeleton** (DSL spec docs integrated into their layers), and Part I (Getting Started /
  CHAPTER-1) refreshed (frontmatter, H1, jar version). **Pending user decision** (pinged): the
  per-chapter pattern depth (At-a-glance + See-also footer replacing the legacy prev/next nav tables)
  and the **slug question** — keep stable `/guides/CHAPTER-N/` URLs vs. rename to semantic slugs
  (high cross-link churn; ~10 inbound links per chapter via the interlinked bottom-nav tables). Then
  roll through Parts II/III/V/VI; retire APPENDIX-I (superseded by CONFIGURATION-REFERENCE).
  (9) **DECIDED: Option 2** — semantic slugs + `mkdocs-redirects`, and remove the prev/next nav tables,
  replacing them with meaningful "See also" footers (Eric, 2026-06-21). **Part I migrated** as the full
  exemplar: CHAPTER-1 → `getting-started.md` (redirect; frontmatter; At-a-glance; See-also; all inbound
  links repointed via `perl`). **Remaining (per-chapter, same pattern):** CH-2→function-execution,
  CH-3 & CH-4 co-located into their DSL folders as `index.md` (rest-automation/, event-script/),
  CH-5→build-test-deploy, CH-6→spring-boot, CH-7→event-over-http, CH-8→service-mesh, CH-9→api-overview;
  refresh + retire APPENDIX-I. Mechanics: `git mv` + `perl -i` for inbound links + redirect + `--strict`.
  **Done so far:** Part I (`getting-started`), Part II core (`function-execution`; ARCHITECTURE +
  METHODOLOGY templated), **Part V** (`spring-boot`, `event-over-http`, `service-mesh`), and the
  **DSL co-locations** — CH-3→`rest-automation/index.md`, CH-4→`event-script/index.md` (each folder
  now = tutorial `index` + grammar + agent guide). **Next (the home stretch):**
  CH-5→`build-test-deploy`, CH-9→`api-overview` (simple flat renames), template COMPOSABLE-DESIGN +
  the reference docs (ANNOTATIONS / CONFIGURATION / EVENT-ENVELOPE / FLOW-SCHEMA / APPENDIX-II/III),
  retire APPENDIX-I (redirect → CONFIGURATION-REFERENCE). That completes the rewrite.
  (10) **Consistency pass (2026-06-22):** the migration above was declared "done" but left a tail of
  old-style remnants (12 ALL-CAPS files incl. an un-migrated `CHAPTER-10`, a BOM-corrupted
  `event-script/index.md` frontmatter, reference docs missing At-a-glance, a legacy `TABLE-OF-CONTENTS`).
  Closed per `docs-style-conventions`: full slug-normalization + redirects, At-a-glance on every doc,
  TOC retired, all inbound links (docs + README + llms.txt) repointed, stale prose "Chapter-N" refs and
  a stale jar version in quickstart fixed. `mkdocs build --strict` exit 0 / 0 warnings; 3 grammar drift
  checks pass; all redirect stubs resolve. The rewrite is now stylistically uniform old→new.*
  <!-- id: bp-docs-ai-human-rewrite | created: 2026-06-20 | last_used: 2026-06-22 | uses: 1 | tier: working -->
- [ ] (blueprint) Integrate a **pluggable AI companion LLM backend**; mature `POST /api/companion/{id}`
  from a dev-only command pipe into a governed collaboration layer. → serves: vision-mercury-composable
  <!-- id: bp-ai-companion-llm-backend | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: working -->
- [ ] (blueprint) **Enterprise governance lifecycle** for graph models (dry-run → certify → stage →
  approve → production), so models promote to production as standard endpoints. → serves: vision-mercury-composable
  <!-- id: bp-graph-governance-lifecycle | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: working -->

## Open Threads

- [x] No CI builds the docs site — the mkdocs config was broken-as-committed (no `docs_dir`,
  defaulted to absent `./docs`) and no workflow caught it (`.github/workflows/` builds Maven only).
  Now fixed on this branch; add a CI step running `mkdocs build --strict` so future doc breakages
  fail the build. **Done 2026-06-20:** `.github/workflows/docs.yml` runs `mkdocs build --strict`
  + the grammar drift check on docs/spec/help changes.
  <!-- id: thread-ci-docs-build | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: working -->
- [x] **Old/new doc-style inconsistency** — the rewrite was declared complete but mixed legacy
  ALL-CAPS docs (un-migrated `CHAPTER-10`, BOM-broken `event-script/index.md` frontmatter, reference
  docs without At-a-glance, legacy `TABLE-OF-CONTENTS`) with the new slug/frontmatter/At-a-glance/
  See-also pattern. **Done 2026-06-22:** resolved per `docs-style-conventions` — see bp-docs progress (10).
  <!-- id: thread-docs-style-consistency | created: 2026-06-22 | last_used: 2026-06-22 | uses: 1 | tier: working -->

## User Preferences

- From the documentation-rewrite effort onward, the **official Accenture GitHub repo is the
  source of truth**; work directly here (not a separate prototyping repo) to keep a clean
  AI–Human commit log on the official repo.
  <!-- id: pref-github-source-of-truth | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->

## Team / Members

(none recorded yet)
