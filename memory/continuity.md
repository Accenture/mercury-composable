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
- **last_session:** 2026-06-26T03:29:08Z | agent: Claude Code
- **last_review:** 2026-06-24 | through 2026-06-24-222752.md
- **last_invariant_check:** 2026-06-24 | 2026-06-24-222752.md (confirmed by Eric — all 11 never-decay facts hold)

> This agent-memory layer was seeded on 2026-06-20 from a prior prototyping
> environment, carrying forward only the confirmed Vision + Blueprint and the
> durable project facts — a clean start for the official repo (see the
> 2026-06-20 bootstrap session log).

## Stack & Tools

> Canonical live home for the current stack — language version, dependencies, tool
> versions. `instructions.md` keeps only a high-level descriptor and points here.

- Language: Java 21 (virtual threads). (Kotlin appears only as an example module, not a framework language.)
  <!-- id: stack-language-java21 | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->
- Build: Maven 3.9.7+ is the current build tool (multi-module reactor, `com.accenture.mercury:parent-mercury`).
  **Gradle support is planned to be added alongside it** (Eric, 2026-06-24 — see Open Thread `thread-add-gradle-build`).
  <!-- id: stack-build-maven | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->
- Integration: Spring Boot (rest-spring-3 / -4 modules)
  <!-- id: stack-integration-spring | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->
- Messaging: Kafka connectors; MsgPack wire serialization; customized Gson
  <!-- id: stack-messaging-kafka | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->
- Persistence: **not part of the framework core** — the framework is persistence-agnostic. The
  `extensions/reactive-postgres` module (reactive PostgreSQL via R2DBC) is an **example/optional add-on**
  demonstrating one persistence approach, not a built-in persistence layer. (Corrected — Eric, 2026-06-24.)
  <!-- id: stack-persistence-r2dbc | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: active -->
- Docs: mkdocs (`docs_dir: docs`) → accenture.github.io/mercury-composable
  <!-- id: stack-docs-mkdocs | created: 2026-06-20 | last_used: 2026-06-22 | uses: 7 | tier: active -->
- CI: GitHub Actions (`.github/workflows/`)
  <!-- id: stack-ci-gha | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->

## Architectural Invariants

> Hard constraints that must never change. These never decay (`core`).

- Functions are fully decoupled — coupled only by route-name strings and
  `EventEnvelope`; orchestration lives in YAML event flows, not code. (ADR-0001)
  <!-- id: functions-decoupled-routes | created: 2026-06-20 | last_used: 2026-06-24 | uses: 11 | tier: core -->
- `TypedLambdaFunction` **key-by-key data mapping** (Event Script Layer 2, Knowledge Graph Layer 3)
  requires Map or PoJo — a List cannot be mapped field-by-field. The **`*` whole-body passthrough**
  (`model.list -> *`) bypasses key-by-key mapping and, with `@PreLoad(inputPojoClass=…)`, enables
  `List<PoJo>` at the function boundary in an Event Script flow. Layer 1 (Platform Core) uses the
  same `inputPojoClass` for external JSON-list ingestion. (ADR-0003)
  <!-- id: typed-io-map-or-pojo | created: 2026-06-20 | last_used: 2026-06-24 | uses: 8 | tier: core -->

## Key Decisions

- Java 21 virtual threads throughout; synchronous PostOffice RPC ≈ reactive perf. (ADR-0002)
  <!-- id: virtual-threads-rpc | created: 2026-06-20 | last_used: 2026-06-22 | uses: 3 | tier: active -->
- `pom.xml` is the source of truth for the version (drift observed across docs).
  <!-- id: pom-version-source-of-truth | created: 2026-06-20 | last_used: 2026-06-22 | uses: 4 | tier: archive-candidate -->
- Docs live under `docs/` (mkdocs `docs_dir: docs`): `guides/` → `docs/guides/` and
  `arch-decisions/` → `docs/arch-decisions/`, **keeping the `guides/` subfolder so
  published URLs stay `/guides/...`** (the MiniGraph webapp, help tutorials, and README
  link to those absolute URLs). Root GitHub files (README/CHANGELOG/CONTRIBUTING/
  CODE_OF_CONDUCT/INCLUSIVITY) stay at repo root as external nav links; `docs/index.md`
  is the site Home. Verified by `mkdocs build --strict` (exit 0).
  <!-- id: docs-dir-layout | created: 2026-06-20 | last_used: 2026-06-22 | uses: 7 | tier: archive-candidate -->
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
  <!-- id: docs-rewrite-architecture | created: 2026-06-20 | last_used: 2026-06-22 | uses: 13 | tier: archive-candidate -->
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
  <!-- id: docs-dsl-spec | created: 2026-06-20 | last_used: 2026-06-22 | uses: 5 | tier: archive-candidate -->
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
  <!-- id: docs-style-conventions | created: 2026-06-22 | last_used: 2026-06-23 | uses: 5 | tier: active -->
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
  <!-- id: docs-content-canon | created: 2026-06-22 | last_used: 2026-06-23 | uses: 10 | tier: active -->
- **No backward-compat redirects (clean rewrite).** All `mkdocs-redirects` entries removed (2026-06-22, Eric):
  old URLs (`/guides/CHAPTER-N/`, `/APPENDIX-*/`, `/composable-design/`, `/TABLE-OF-CONTENTS/`, and the
  case-only ones) now 404 by design — the docs are a brand-new user experience and the **navigation is the
  source of truth**. The `redirects` plugin is dropped from `mkdocs.yml` and `mkdocs-redirects` from the CI
  install. This reverses the "redirects as the safety net" aspect of `docs-style-conventions` /
  `docs-content-canon` (their redirect language is now historical). The `check-doc-canon.py`
  case-only-redirect guard stays (dormant) to reject a bad redirect if one is ever re-added.
  <!-- id: docs-no-redirects | created: 2026-06-22 | last_used: 2026-06-23 | uses: 2 | tier: active -->
- **ADR pattern adopted** (the agent-memory optional Architecture Decision Record log; opted in 2026-06-22, Eric). A
  human-facing governance ledger lives at `docs/arch-decisions/ADR.md`. `DESIGN-NOTES.md` — the author's design notepad — was **removed** (2026-06-23) as a drift source; the ADR
  ledger now holds the durable design rationale, and the `arch-decisions/` folder is repurposed for the ledger. Seeded
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
  <!-- id: adr-pattern-adopted | created: 2026-06-22 | last_used: 2026-06-23 | uses: 5 | tier: active -->
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
  <!-- id: request-pipeline-model | created: 2026-06-22 | last_used: 2026-06-22 | uses: 2 | tier: active -->
- **Service mesh is opt-in, not the default.** `cloud.connector=none` is the framework default. The Kafka
  service mesh (`cloud.connector=kafka` + presence-monitor) solves exactly two problems: (1) synchronous
  request-response across application instances over Kafka (sync over async), and (2) service discovery
  between pods. Applications that do not need either must be designed cloud-native (self-contained,
  horizontally scaled, no cross-instance coupling). Superimposing sync over async is a recipe for a
  "distributed monolith" — full operational cost of distribution with monolith-level coupling. The mesh is an
  advanced opt-in for specific use cases (cross-application RPC, leader selection, pod-aware broadcast).
  This preference must be front-and-center in documentation and AI guides. (ADR-0006)
  <!-- id: kafka-mesh-opt-in | created: 2026-06-23 | last_used: 2026-06-24 | uses: 2 | tier: core -->
- **The built-in HTTP flow adapter (`HttpToFlow` / `http.flow.adapter`) is the only one in `event-script-engine`;
  a minimalist Kafka pair now ships as the `system/minimalist-kafka` library** (added 2026-06-26, commit `c8824519`):
  `KafkaFlowAdapter` (inbound, routes a topic into an Event Script flow) + `SimpleKafkaNotification` (outbound,
  `simple.kafka.notification`). It is **not** auto-wired into the engine — an app opts in by depending on the library
  and configuring `yaml.kafka.flow.adapter`. The `connectors/adapters/kafka/*` modules remain the **cloud connector**
  (event-stream mesh, `cloud.connector=kafka`) — a *different* concern, not a flow adapter. So: HTTP flow-triggering is
  built-in; Kafka flow-triggering is an opt-in library, not core. Production installs may still run their own.
  <!-- id: kafka-adapter-not-in-repo | created: 2026-06-22 | last_used: 2026-06-26 | uses: 4 | tier: active -->
- **W3C OpenTelemetry distributed tracing** (`feature/open-telemetry` branch). Each function gets a 16-hex
  `span_id` + `parent_span_id` propagated end-to-end: PostOffice/WorkerHandler stamp+emit them; Event Script
  `TaskExecutor` threads the parent span via a `TaskReference` anchor (**virtual-thread-safe, no ThreadLocal**);
  MiniGraph `GraphExecutor` threads it through graph traversal; the HTTP boundary uses `W3cTrace` to build/parse
  `traceparent` (`AsyncHttpClient` injects, `HttpRouter` extracts). The trace-metrics dataset now carries
  `span_id`/`parent_span_id`/trace id. `trace.http.legacy.header.enabled` toggles the legacy `X-Trace-Id`
  outbound alongside `traceparent`. **Export = the open item, now done:** `distributed.trace.forwarder` is the
  framework's extension point (`Telemetry` forwards completed trace metrics to it if registered); the new
  **`extensions/opentelemetry-forwarder`** module is a drop-in reusable forwarder that builds OTel `SpanData`
  **directly** (preserving Mercury's exact W3C ids — the `Tracer` API would regenerate them and break lineage)
  and exports via **OTLP/HTTP**. Auto-registers just by adding the jar (it's under `org.platformlambda`, an
  always-scanned base package). **Config-driven for production:** `OpenTelemetryForwarder`'s no-arg constructor
  reads `application.properties` via `AppConfigReader` (values support `${ENV_VAR:default}` substitution) —
  `otel.exporter.otlp.endpoint`, `otel.service.name`, `otel.trace.forwarder.enabled`, `otel.exporter.otlp.timeout`,
  and `otel.exporter.otlp.headers`. **Credentials**: set `otel.exporter.otlp.headers=${OTEL_EXPORTER_OTLP_HEADERS}`
  with **no default** so no secret is hard-coded (static-analysis-safe) — an unset var resolves to null → `"null"` →
  zero headers, which the no-auth mock accepts. A package-private 2nd constructor injects a context (in-memory
  exporter) for unit tests; the old static install()/getInstance() singleton seam was removed. Verified green:
  21 module tests + the existing `W3cTraceTest`/`PostOfficeTest`/`SpanPropagationTest`/`GraphSpanPropagationTest`.
  **SonarQube/security-hardened:** `opentelemetry-proto` bumped 1.3.2-alpha → **1.10.0-alpha** so the transitive
  `protobuf-java` is 4.34.0 (clears **CVE-2024-7254**, fixed in 4.28.2); `TraceMetricsSpanData` constructor collapsed
  to 4 args (S107), its `StatusData` field renamed `statusData`; `OpenTelemetryForwarder` dropped redundant
  `instances=1`, removed a plain-text-link Javadoc URL, and its no-arg (reflective `@PreLoad`) constructor now has a
  direct test.
  **JaCoCo coverage** (the project's 85% minimum): line 95.4% / instruction 95.4% / branch 84.6%, **enforced** by a
  `jacoco:check` gate on LINE + INSTRUCTION ≥ 0.85 (branch not gated — its last gap is the boot-time disabled
  constructor branch). Report at `target/site/jacoco/`.
  Validated **end-to-end through the real forwarder → mock** at **Level-1** (`OtlpTracePipelineTest`: a traced
  `unit.test → fun.1 → fun.2 → fun.3` PostOffice RPC chain → 3 linked spans, asserting shared trace id +
  root/child/grandchild lineage decoded off the wire) **and Level-2** (`OtlpFlowTraceTest`: a `task.1 → task.2`
  Event Script flow via `FlowExecutor` → the same Level-1-style task chain **plus the one synthetic
  `task.executor` flow-summary span**, annotated with the flow id; RPC round-trip records carry no `span_id` and
  are gracefully skipped by the mapper). Level-3 (MiniGraph `GraphExecutor`) rides the same WorkerHandler path.
  The mock OTLP backend is built **the composable way** (Eric's preference) — a `@PreLoad`
  `TypedLambdaFunction` `mock.otlp.collector` behind test `rest.yaml` (`POST /api/v2/otlp/v1/traces`) +
  `application.properties` + `@MainApplication` (`MockOtlpAppMain`), booted via `AutoStart`, so a human can run
  it from an IDE and point a real exporter/`curl` at it. `rest.yaml` maps **both** backend ingest paths to the
  one function — `/api/v2/otlp/v1/traces` (Dynatrace) and `/v2/trace/otlp` (Splunk). The test drives the real
  OTLP exporter against both and asserts the credential header arrives. The mock **decodes the OTLP protobuf**
  (`io.opentelemetry.proto:opentelemetry-proto`, test scope) and logs the span key-values + asserts the
  round-tripped trace/span/parent ids and `service.name` survived the wire — a deliberately self-explanatory
  reviewer example (unit-test-as-documentation). **Documented** in the new **Observability** guide
  (`docs/guides/observability.md`, nav: Operate & Integrate) — built-in tracing design across the 3 layers +
  OpenTelemetry/OTLP export; wired into `mkdocs.yml`, `llms.txt`, and `configuration-reference.md#observability`
  (doc-canon checker passes). Possible future: batch export. **Shipped & merged to `main`** via
  [PR #122](https://github.com/Accenture/mercury-composable/pull/122) (approved in peer review) — the OpenTelemetry
  feature is now part of the framework.
  <!-- id: otel-w3c-tracing | created: 2026-06-24 | last_used: 2026-06-24 | uses: 1 | tier: working -->

## Conventions

- Add capability: function (`@PreLoad` + `TypedLambdaFunction`) → flow YAML →
  register in `flows.yaml` → `rest.yaml` mapping if HTTP-facing.
  <!-- id: conv-add-capability | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->
- Watch serialization gotchas (Long↔Integer downcast; use `util.str2int/str2long`).
  <!-- id: conv-serialization-gotchas | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->
- See `examples/composable-example` (`FlowTest`) as the canonical reference.
  <!-- id: conv-canonical-example | created: 2026-06-20 | last_used: 2026-06-22 | uses: 2 | tier: archive-candidate -->

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
  <!-- id: bp-docs-ai-human-rewrite | created: 2026-06-20 | last_used: 2026-06-22 | uses: 11 | tier: working -->
- [ ] (blueprint) Integrate a **pluggable AI companion LLM backend**; mature `POST /api/companion/{id}`
  from a dev-only command pipe into a governed collaboration layer. → serves: vision-mercury-composable
  <!-- id: bp-ai-companion-llm-backend | created: 2026-06-20 | last_used: 2026-06-21 | uses: 1 | tier: working -->
- [ ] (blueprint) **Enterprise governance lifecycle** for graph models (dry-run → certify → stage →
  approve → production), so models promote to production as standard endpoints. → serves: vision-mercury-composable
  <!-- id: bp-graph-governance-lifecycle | created: 2026-06-20 | last_used: 2026-06-21 | uses: 1 | tier: working -->

## Open Threads

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
- [x] (in progress) **Layer-standardization reorg** — "Shared Foundations + lean parallel layers"
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
  **Done 2026-06-23:** folded into `guides/event-driven/` (index.md + function-execution.md + write-your-first-function.md tutorial);
  Layer 1 now fully parallel to Layers 2 & 3; all cross-references updated; mkdocs build --strict 0 warnings; deployed to gh-pages.
  `site/` gitignored and untracked.
  <!-- id: thread-layer-reorg | created: 2026-06-22 | last_used: 2026-06-23 | uses: 5 | tier: working -->
- [x] (next agenda — Eric, 2026-06-22) **Content polishing round 2 + AI context discovery.** Next working
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
  completed + corrected. (2) AI-context-discovery on-ramp **done (2026-06-23):** created
  `docs/guides/ai-developer-guide.md` — cross-layer guide for AI agents joining a mercury-composable
  project (brownfield orientation, layer-choice decision tree, add-a-feature at each layer, testing,
  invariants, DSL guide table); added to nav (Foundations) and `llms.txt` (Start here). The whole
  rewrite is **ready for peer review** (2026-06-23); `gh pr create` is blocked for the Enterprise-Managed-User,
  so the PR is opened **manually via the GitHub web UI** (branch is fully pushed).
  <!-- id: thread-next-ai-context | created: 2026-06-22 | last_used: 2026-06-23 | uses: 2 | tier: working -->
- [x] (future — after the docs-rewrite phase; Eric, 2026-06-22) **Add a minimalist Kafka flow adapter (inbound) +
  Kafka notification function (outbound) to this repo.** Today only the HTTP flow adapter ships here (see
  `kafka-adapter-not-in-repo`); production installations have their own. Deferred until the documentation rewrite
  (`bp-docs-ai-human-rewrite`) completes, then build a reference-grade minimalist pair so the Kafka path is demonstrable
  in-repo. → serves `vision-mercury-composable`.
  **Done 2026-06-26 (commit `c8824519`):** built as the `system/minimalist-kafka` library (`org.platformlambda.mini.kafka`,
  depends on `event-script-engine`) — `SimpleKafkaNotification` (`simple.kafka.notification`, outbound) + `KafkaFlowAdapter`/
  `KafkaFlowConsumer` (inbound, one poll-loop thread per `topic→flow`, low-level `PostOffice` routing with W3C span
  continuity, commit-after-process = at-least-once) + `KafkaFlowAutoStart` (`@MainApplication`). Emerged from the
  sync-over-async sprint (`thread-redis-kafka-rpc`) rather than waiting on the docs rewrite. 87% coverage; standalone
  embedded-KRaft e2e. (Folded into `thread-redis-kafka-rpc`.)
  <!-- id: thread-minimalist-kafka-adapter | created: 2026-06-22 | last_used: 2026-06-26 | uses: 2 | tier: working -->
- [x] **Re-verify invariants (first invariant check; 24 session files ≥ `verify_invariants_every` 20).**
  Confirmed the never-decay set still holds, or supersede any that don't (`DECAY.md` §9): the Architectural
  Invariants (`functions-decoupled-routes`, `typed-io-map-or-pojo`), the `core` Key Decision
  `kafka-mesh-opt-in`, the **7 facts promoted to `core` this review** (`stack-language-java21`,
  `stack-build-maven`, `stack-integration-spring`, `stack-messaging-kafka`,
  `stack-ci-gha`, `conv-add-capability`, `conv-serialization-gotchas`), and the Vision
  (`vision-mercury-composable`). **Done 2026-06-24 (Eric):** all 11 confirmed to hold. One content
  correction — `stack-language-java21` Kotlin clause removed (Kotlin is only an example module, not a
  framework language). `stack-persistence-r2dbc` had already been reclassified out of the set (demoted
  `core`→`active` — it's an example extension).
  <!-- id: thread-verify-invariants-2026q2 | created: 2026-06-24 | last_used: 2026-06-24 | uses: 2 | tier: working -->
- [ ] (planned — Eric, 2026-06-24) **Add Gradle build support** alongside the existing Maven reactor
  (Maven stays the current build tool; see `stack-build-maven`). Scope TBD — likely a parallel Gradle
  build for the multi-module project.
  <!-- id: thread-add-gradle-build | created: 2026-06-24 | last_used: 2026-06-24 | uses: 1 | tier: working -->
- [ ] (docs backlog — Eric, 2026-06-24) **Documentation improvement — serve both audiences, every sprint.**
  The standing purpose of the documentation sprints (sharpens the dual-design principle in
  `docs-rewrite-architecture` / `docs-content-canon`; extends `bp-docs-ai-human-rewrite`,
  `thread-next-ai-context`): **for humans — storytelling: engaging, why-before-how, a narrative arc;**
  **for AI agents — token-efficient: the shortest path to the point, machine-greppable, "generate from
  this page alone."** These are the acceptance criteria for every doc change. Backlog of concrete items
  (grows as findings surface; first batch from a fresh-agent discovery pass on 2026-06-24 — building the
  OTel forwarder via `llms.txt` → REST-automation guide):
  - **Biggest gap: an AI-agent "boot & test an app" recipe.** Authoring a `rest.yaml` was well-documented;
    *standing up and testing* an app was not — had to read platform-core source + test fixtures for
    `AutoStart.main`, the minimal `application.properties` (`rest.automation`/`web.component.scan`/`server.port`),
    `@PreLoad` base-package auto-scan, and the `AsyncHttpRequest` service contract (incl. returning an empty 200).
  - **Surface the working test-fixture pattern** (`TestBase` + a service function + test `rest.yaml`) as a
    documented example — it was the single highest-signal context, yet lives only in `src/test`.
  - **Machine-readable runtime-API signatures** — like the DSL `*.json` catalogs (`docs-dsl-spec`) but for
    `AsyncHttpRequest`/`AutoStart`/`AppConfigReader`; agents grep source for exact signatures today.
  - **Repo-relative links in `llms.txt`** (alongside the published URLs) so an in-repo agent maps map→file in one hop.
  - Re-validate each pass with the **fresh-agent test** (`docs-dsl-spec` methodology): can a clean agent build
    *and test* from the docs alone?
  Second batch — from building the **whole** OpenTelemetry forwarder feature end-to-end (2026-06-24), where the
  recurring friction was *grepping platform-core source* for things prose didn't cover (AI-context-discovery focus):
  - **Reserved-route extension contract + a machine-readable dataset schema.** Writing a `distributed.trace.forwarder`
    meant reverse-engineering the trace-metrics map shape (`id`/`span_id`/`parent_span_id`/`service`/`path`/`from`/
    `origin`/`start`/`exec_time`/`round_trip`/`success`/`status`/`exception` + `annotations`) from `Telemetry`/
    `WorkerHandler`. Document the reserved routes (`distributed.trace.forwarder`, `transaction.journal.recorder`) with
    a JSON dataset schema like the DSL `*.json` catalogs. *(The new Observability guide now documents the dataset in
    prose — the goal is the machine-readable schema.)*
  - **"Author a reusable extension" recipe + the auto-registration fact.** The cornerstone — `@PreLoad` classes under
    the base packages (`org.platformlambda.*`/`com.accenture.*`) are *always* scanned, so dropping the jar on the
    classpath auto-registers the route — lives only in `SimpleClassScanner` source.
  - **Document `${ENV_VAR:default}` config substitution** (`AppConfigReader`), incl. an unset `${VAR}` with no default
    resolving to null — central to production config + keeping secrets out of files; learned from Eric, not the docs.
  - **Test server-readiness via the `async.http.response` provider** (`Platform.waitForProvider`), not `Thread.sleep`
    — the signal the HTTP server registers after `listen()`; found in `AppStarter` source. Folds into the boot-&-test recipe.
  - **Drive an Event Script flow programmatically** (`FlowExecutor.request(originator, traceId, tracePath, flowId,
    dataset, cid, timeout)`) and document the synthetic `task.executor` flow-summary span — needed for testing flows.
  - **Surface the machine-readable catalogs in `llms.txt`** (the DSL `*.json` files) as first-class entries, and add
    "build & test an app" + "author an extension" entries so an agent doesn't discover them only by reading prose.
  → serves `vision-mercury-composable`.
  <!-- id: thread-docs-improvement-backlog | created: 2026-06-24 | last_used: 2026-06-24 | uses: 1 | tier: working -->
- [ ] (next iteration — Eric, 2026-06-24; **design + implement**) **Cross-pod request-response via Redis
  Pub/Sub RPC + Kafka.** A distributed sync-over-async pattern (an advanced opt-in use case, cf.
  `kafka-mesh-opt-in`): `REST sync request-response → Composable service (POD-1) → Redis Pub/Sub RPC + Kafka
  **outbound** topic; Kafka **inbound** topic (response) → Composable service (POD-2) → Redis`. A
  **correlation-id** is the return-path reference so Redis routes the response back to POD-1. Build:
  (1) a composable function interfacing **Redis** + Kafka send/receive topics; (2) a **minimalist Kafka flow
  adapter (inbound)**; (3) a **Kafka notification function (outbound)**. Items (2)+(3) are the scope of
  `thread-minimalist-kafka-adapter` — now folded into this larger concept. → serves `vision-mercury-composable`.
  **Prototyping started 2026-06-25 on branch `feature/sync-over-async`** (design reviewed from Eric's spec).
  **Locked decisions:** return path = **Redis** (cloud-native REST facade for UI apps; deliberately *not* the full
  mesh/presence discovery); Redis client = **Lettuce** (Reactor-native, matches `reactive-postgres`, battle-tested,
  robust pub/sub + auto-reconnect); module = new self-initializing extension **`extensions/sync-over-async`**;
  tests = **embedded Redis** (codemonstur `embedded-redis`, arm64; Testcontainers/Docker fallback) + an **embedded
  Kafka** extracted from `connectors/adapters/kafka/kafka-standalone` (`EmbeddedKafka.java`) for unit tests, with
  `kafka-standalone` for integration. Pod identity = `Platform.getOrigin()`. **Reliability cornerstones** (from
  review): payload in Redis `SETEX` is the source of truth, pub/sub is wake-up only, and a **final Redis read before
  timeout** is MVP-required (correctness independent of pub/sub); race-safe idempotent future completion.
  **Phase plan → MVP:** P1 = return-route engine (TDD, embedded Redis, no Kafka — cross-pod return, timeout→408,
  duplicate, orphan, missed-pubsub→final-read); P2 = Kafka legs (outbound notifier + inbound adapter, trace headers,
  mock SoR loopback); P3 = REST facade + e2e (+ trace via the OTel forwarder); P4 (post-MVP) = guardrails/503/metrics,
  two-JVM test, docs. **Note:** trace-across-Kafka is *not* free — needs cid + `traceparent` in Kafka headers + the
  inbound adapter rebuilding trace context.
  **Status (2026-06-25): scaffold + P1 ✅ done.** Module `extensions/sync-over-async` (pkg `org.platformlambda.sync`):
  `PendingRequests` (race-safe idempotent registry + max-pending), `ReturnRouteStore` (Lettuce `SETEX`/`GET` for
  `request:`/`response:` keys), `ReturnRouteCoordinator` (per-pod: `begin`/`awaitResponse`-with-final-read/`deliver`;
  pub/sub callback dispatches the blocking read to a virtual thread to avoid stalling the Lettuce event loop).
  16 tests vs embedded Redis (incl. cross-pod return, timeout, missed-notification→final-read, orphan, duplicate);
  JaCoCo 93.6% line, **85% gate enforced**.
  **Status (2026-06-26): MVP complete + building blocks extracted into a library** (commit `c8824519`).
  P2 (Kafka legs) → P3 (REST facade `test.endpoint`, the composable way: `event-script-engine` + `rest.yaml` →
  `sync-to-async` flow) → P4 (refactor the raw legs into composable **building blocks**: a drop-n-forget Kafka
  **notification function** + a **Kafka Flow Adapter** that routes each topic into an Event Script flow,
  one poll-loop thread per topic, synchronous request + commit-after-process = at-least-once). Full round-trip
  proven: `REST → http.flow.adapter → sync-to-async → test.endpoint (begin+notify) → Kafka topic-1 → adapter →
  system-of-record (echo+notify topic-2) → Kafka topic-2 → adapter → soa-reply → coordinator.deliver → Redis
  return route → HTTP 200 / 408`. **OTel span propagation across Kafka fixed without touching `event-script-engine`**
  (use the low-level `PostOffice` API: notification stamps its own span into the Kafka `traceparent`; the consumer
  parses it and `forward.setSpanId(parentSpanId)` so the flow chains onto it — `WorkerHandler:103` adopts the
  event span-id as the function's parent; validated against the telemetry log = one continuous trace, the two
  notification hops are the bridge spans). **Then promoted the pair to a reusable library** (Eric's call) —
  see `thread-minimalist-kafka-adapter` (now fulfilled): `system/minimalist-kafka` (`org.platformlambda.mini.kafka`,
  depends on `event-script-engine`, 87% cov, standalone embedded-Kafka e2e); `sync-over-async` now depends on it
  and is purely the Redis return-route engine (96% cov, 20 tests). Both green in the reactor on JDK 21.
  **Remaining (post-MVP, not yet built):** productionization — Redis coordinator config-driven init, consumer
  partition-pinning, 503 guardrails/metrics, two-JVM test, module docs/README; and Gradle build (`thread-add-gradle-build`).
  <!-- id: thread-redis-kafka-rpc | created: 2026-06-24 | last_used: 2026-06-26 | uses: 3 | tier: working -->

## User Preferences

- From the documentation-rewrite effort onward, the **official Accenture GitHub repo is the
  source of truth**; work directly here (not a separate prototyping repo) to keep a clean
  AI–Human commit log on the official repo.
  <!-- id: pref-github-source-of-truth | created: 2026-06-20 | last_used: 2026-06-22 | uses: 2 | tier: archive-candidate -->

## Team / Members

(none recorded yet)
