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
- **last_session:** 2026-06-23 | agent: Claude Code
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
  `EventEnvelope`; orchestration lives in YAML event flows, not code. (ADR-0001)
  <!-- id: functions-decoupled-routes | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: core -->
- `TypedLambdaFunction` I/O is Map or PoJo only — a List of PoJo is unsupported. (ADR-0003)
  <!-- id: typed-io-map-or-pojo | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: core -->

## Key Decisions

- Java 21 virtual threads throughout; synchronous PostOffice RPC ≈ reactive perf. (ADR-0002)
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
- **Documentation content canon** (Design for the content-polish pass; locked with Eric Law 2026-06-22,
  verified against source — docs are outdated, **code is source of truth**). Its layer model and
  one-atom-four-roles framing are formalized as (ADR-0004, ADR-0005). Resolves old/new *content*
  drift (7+ yrs, many human + AI contributors). Five decisions: (1) **"layers" = the 3 paradigm layers
  only** — Event-driven (Platform Core) → Composable (Event Script) → Semantic (Active Knowledge Graph);
  the runtime request flow is the **"request pipeline"** with **stages** (protocol boundary [REST automation for HTTP,
  a Kafka listener, …] → flow adapter → Event Manager/flow engine → in-memory event bus → composable functions; for
  each protocol a corresponding flow adapter — for HTTP, REST automation is the boundary that invokes the built-in HTTP
  flow adapter), never "layers" (fixes
  architecture.md's "five distinct layers"). (2) **Layer-3 vocabulary:** *Active Knowledge Graph (AKG)* =
  the thing/model; *Knowledge Graph as Application* = the paradigm tagline; *MiniGraph* = the engine
  (`graph.executor` + in-memory property graph + Playground); *semantic* = adjective only. (3) **Origin
  story is told:** Scala/Akka actor model → Eclipse Vert.x event bus → Java 21 virtual threads (the *why*
  of decoupled-functions-as-actors) — a Home one-liner + a "Where it came from" Architecture section.
  (4) **Human–AI collaboration = cross-cutting capability** across all 3 layers (agent-ready DSL specs +
  companion endpoint), NOT a 4th layer. (5) **"One atom, four roles":** the sole building block is the
  route-addressed **function** (`@PreLoad` + `LambdaFunction`/`TypedLambdaFunction`, Map/PoJo I/O, private
  by default); it is *named by how it is wired* — **function** (the atom), **service** (mapped straight to
  HTTP via `service:` in `rest.yaml` — narrow REST role only; `RoutingEntry.java:44`), **task** (a step in
  an Event Script flow with an `execution` type; `CompileFlows.EXECUTION_TYPES`), **skill** (attached to an
  AKG node via the node's `skill:` property; `GraphLambdaFunction.java:116`). "Function" = the general atom,
  "service" = the narrow REST role (Eric confirmed). **AI-discovery contract:** every doc carries
  frontmatter + At-a-glance + See-also + stable anchors; `llms.txt` is the current by-layer map (drop the
  "rewrite in progress / legacy" note); "generate from this page alone" claims belong ONLY to the 3 DSL
  agent-guides, not concept pages. **Conformance order (approved):** (1) index.md + llms.txt → (2)
  architecture.md → (3) methodology.md re-voice → (4) terminology sweep of lower/reference docs →
  (5) persist canon as a published page + wire a light drift check. Extends `docs-style-conventions` /
  `docs-rewrite-architecture`; serves `vision-mercury-composable`.
  <!-- id: docs-content-canon | created: 2026-06-22 | last_used: 2026-06-22 | uses: 1 | tier: working -->
- **No backward-compat redirects (clean rewrite).** All `mkdocs-redirects` entries removed (2026-06-22, Eric):
  old URLs (`/guides/CHAPTER-N/`, `/APPENDIX-*/`, `/composable-design/`, `/TABLE-OF-CONTENTS/`, and the
  case-only ones) now 404 by design — the docs are a brand-new user experience and the **navigation is the
  source of truth**. The `redirects` plugin is dropped from `mkdocs.yml` and `mkdocs-redirects` from the CI
  install. This reverses the "redirects as the safety net" aspect of `docs-style-conventions` /
  `docs-content-canon` (their redirect language is now historical). The `check-doc-canon.py`
  case-only-redirect guard stays (dormant) to reject a bad redirect if one is ever re-added.
  <!-- id: docs-no-redirects | created: 2026-06-22 | last_used: 2026-06-22 | uses: 1 | tier: working -->
- **ADR pattern adopted** (the agent-memory optional Architecture Decision Record log; opted in 2026-06-22, Eric). A
  human-facing governance ledger lives at `docs/arch-decisions/ADR.md` — **the repo's own path; the agent-memory default
  `docs/ADR.md` is not used** (AGENTS.md pointer updated to match). `DESIGN-NOTES.md` was moved to
  `docs/notes/design-notes.md` (design notes ≠ ADRs) and the `arch-decisions/` folder repurposed for the ledger. Seeded
  **retrospectively** with 5 ADRs that **formalize** existing Design-altitude facts — ADR-0001→`functions-decoupled-routes`,
  ADR-0002→`virtual-threads-rpc`, ADR-0003→`typed-io-map-or-pojo`, ADR-0004 & ADR-0005→`docs-content-canon` (the
  three-paradigm-layer model + one-atom-four-roles) — each verified against `platform-core`/`event-script-engine`/
  `minigraph-playground-engine` and the published guides (code/guides = source of truth in ambiguity). Published in the
  mkdocs nav as the first entry under **Part VII · Reference**. ADR lifecycle: `Proposed → Accepted → Superseded/Deprecated`,
  never deleted, monotonic numbering, newest-first; read **on demand** only. The `(ADR-NNNN)` tags now on the formalized
  facts are human pointers, not a cue to open the ledger. **Upkeep (agent-memory upgraded 4.14.1 → 4.15.0
  on 2026-06-22):** the ADR log is now actively maintained — superseding/invalidating an `(ADR-NNNN)`-tagged
  fact, or making a new durable architecture decision, **prompts a human-gated update** to
  `docs/arch-decisions/ADR.md` (add a newer ADR; old → `Superseded`/`Deprecated`, never deleted; keep
  `formalizes:` ↔ `(ADR-NNNN)` in sync). Serves `vision-mercury-composable`.
  <!-- id: adr-pattern-adopted | created: 2026-06-22 | last_used: 2026-06-22 | uses: 1 | tier: working -->
- **Request pipeline model** (Eric, 2026-06-22; stage term **"protocol boundary"** — chosen over "event boundary" for
  precision (requests aren't events until the flow adapter mints the `EventEnvelope`) + code-groundability, and to avoid
  colliding with Mercury's `EntryPoint`/`@MainApplication`): outside-in, `user/calling app → protocol boundary (REST automation for
  HTTP, a Kafka listener, or other protocol) → flow adapter → event manager/flow engine → in-memory event bus →
  composable functions`. For each protocol there is a corresponding flow adapter. **HTTP:** REST automation is the
  boundary — it holds the request/response objects per HTTP session, does endpoint rendering/serving/routing, and
  **invokes the built-in HTTP flow adapter** (`HttpToFlow`, route `http.flow.adapter`, in `event-script-engine`);
  synchronous request/response, the flow's result routed back to the HTTP response object. **Kafka:** the Kafka flow
  adapter embeds a topic listener; fully asynchronous; a reply (if any) is published to another topic by an outbound
  **Kafka notification function**. (Earlier docs put "REST automation" as a stage *after* the flow adapter — wrong;
  it is the boundary *in front* that invokes the adapter. Corrected in architecture.md / documentation-conventions.md /
  ADR-0004.)
  <!-- id: request-pipeline-model | created: 2026-06-22 | last_used: 2026-06-22 | uses: 1 | tier: working -->
- **Kafka flow adapter + notification function are NOT packaged in this repo** — only the built-in HTTP flow adapter
  (`HttpToFlow` / `http.flow.adapter`) ships here. The `connectors/adapters/kafka/*` modules are the **cloud connector**
  (event-stream mesh, `cloud.connector=kafka`) — a *different* concern, not a flow adapter that triggers Event Script
  flows. Production installations run their own Kafka flow adapter (inbound) + Kafka notification function (outbound);
  an in-repo minimalist version is planned (see Open Thread). Don't claim Kafka flow-triggering is built-in.
  <!-- id: kafka-adapter-not-in-repo | created: 2026-06-22 | last_used: 2026-06-22 | uses: 1 | tier: working -->

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
- [x] **Old/new doc-style *content* inconsistency** — beyond structure, the docs mixed old and new
  *content* (inconsistent layer model, layer-3 naming, missing origin story, whitepaper vs product voice,
  loose task/function terminology). **Done 2026-06-22:** locked the **Documentation Canon**
  (`docs-content-canon`) with Eric and conformed index/llms.txt/architecture/methodology + a terminology
  sweep; published the canon as `docs/guides/documentation-conventions.md` and added a CI drift check
  (`scripts/check-doc-canon.py`).
  <!-- id: thread-docs-content-consistency | created: 2026-06-22 | last_used: 2026-06-22 | uses: 1 | tier: working -->
- [ ] (in progress) **Layer-standardization reorg** — "Shared Foundations + lean parallel layers"
  (`docs-content-canon`). Each of the 3 layers gets the same shape: Overview → Tutorial → Grammar →
  Reference → AI guide → Integration; framework-wide pages live once in a Foundations part.
  **Pass 1 done (2026-06-22):** Foundations part created (architecture + methodology); new Layer-1 Overview
  (`event-driven-foundation.md`); `composable-design` absorbed into methodology + retired; nav → 7 Parts
  with "Layer N —" labels; `build-test-deploy` → Operate. **Pass 2 done (2026-06-22):** Layer 2 Overview —
  fronted the large `event-script/index` with an `## Overview` (places the layer in the ascent + the flow
  mental model: flow→tasks→execution types→state machine→adapters), approach (a) (no split), per Eric.
  **Core reorg complete:** all 3 layers now have an Overview + a consistent shape; Foundations consolidated.
  **Pass 3 done (2026-06-22):** Layer-2 overview promoted to the section **index** (`event-script/index.md` =
  "Composable Orchestration" overview; deep syntax moved to `event-script/syntax.md`; ~30 inbound links +
  README + llms.txt refactored) so every layer's overview sits at the section root, matching Layer 3. Added
  cross-layer "ascent" See-also links (Layers 1 & 2). **ALL mkdocs redirects then removed** (clean rewrite —
  see `docs-no-redirects`). Eric verified navigation in a browser.
  **Open (Eric's call):** Layer 1's overview is a flat page (`event-driven-foundation.md`), not a section
  folder — fold into `guides/event-driven/` for full parallelism, or leave as the layer's lead page?
  <!-- id: thread-layer-reorg | created: 2026-06-22 | last_used: 2026-06-22 | uses: 1 | tier: working -->
- [ ] (next agenda — Eric, 2026-06-22) **Content polishing round 2 + AI context discovery.** Next working
  session with Eric: (1) **continue content polishing** (improving but "not there yet"); (2) strengthen
  **AI context discovery** so an AI agent can collaborate with a human on **greenfield *and* brownfield**
  mercury-composable projects across every artifact — knowledge graph, Event Script, `rest.yaml`,
  **composable functions**, unit tests, integration tests — and **make sense of the 3 layers** to choose
  the right one. Especially a clear on-ramp for **writing composable functions**. Key framing (Eric's hint):
  a composable function is *just regular Java* (with or without Spring), writable in **sequential, reactive,
  or object-oriented** style — the framework constrains *coupling* (route names + `EventEnvelope`), not
  coding style. → serves `vision-mercury-composable` (AI-assisted semantic app dev / Human-AI collaboration).
  **Progress (2026-06-23):** (1) content polishing largely **done** — Quickstart/Getting-Started merged,
  the 3-layer site polished, wide reference tables fixed site-wide via `docs/css/extra.css` (wrap, not
  per-cell `<br>`), and a code-vs-docs **drift validation** of annotations/configuration/reserved-names
  completed + corrected. (2) AI-context-discovery on-ramp **still pending** — the next focus. The whole
  rewrite is **ready for peer review** (2026-06-23); `gh pr create` is blocked for the Enterprise-Managed-User,
  so the PR is opened **manually via the GitHub web UI** (branch is fully pushed).
  <!-- id: thread-next-ai-context | created: 2026-06-22 | last_used: 2026-06-22 | uses: 1 | tier: working -->
- [ ] (future — after the docs-rewrite phase; Eric, 2026-06-22) **Add a minimalist Kafka flow adapter (inbound) +
  Kafka notification function (outbound) to this repo.** Today only the HTTP flow adapter ships here (see
  `kafka-adapter-not-in-repo`); production installations have their own. Deferred until the documentation rewrite
  (`bp-docs-ai-human-rewrite`) completes, then build a reference-grade minimalist pair so the Kafka path is demonstrable
  in-repo. → serves `vision-mercury-composable`.
  <!-- id: thread-minimalist-kafka-adapter | created: 2026-06-22 | last_used: 2026-06-22 | uses: 1 | tier: working -->

## User Preferences

- From the documentation-rewrite effort onward, the **official Accenture GitHub repo is the
  source of truth**; work directly here (not a separate prototyping repo) to keep a clean
  AI–Human commit log on the official repo.
  <!-- id: pref-github-source-of-truth | created: 2026-06-20 | last_used: 2026-06-20 | uses: 1 | tier: active -->

## Team / Members

(none recorded yet)
