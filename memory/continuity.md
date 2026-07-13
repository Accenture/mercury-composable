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
- **last_session:** 2026-07-13 | agent: Claude Code (2026-07-13-142021)
- **last_review:** 2026-07-13 | through 2026-07-13-001009.md
- **last_invariant_check:** 2026-06-29 | 2026-06-29-223651.md (re-verify prompted — cadence reset; pending Eric via Open Thread thread-reverify-invariants-2026q2)

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
- Functions execute on **Java 21 virtual threads** over the Vert.x in-memory event bus; a synchronous
  PostOffice RPC (`po.request`) suspends the virtual thread and releases its carrier, so sequential
  blocking-style code performs on par with reactive — and a function may still return `Mono`/`Flux`.
  This is why e.g. 250 instances of a blocking `sync.await` are cheap. (ADR-0002)
  <!-- id: virtual-threads-rpc | created: 2026-06-20 | last_used: 2026-06-27 | uses: 4 | tier: core -->

## Key Decisions

- **Field trace-propagation report on 4.6.3 diagnosed (2026-07-13): not a framework bug.** A field team
  saw the traceId stop propagating between application endpoints after upgrading 4.4.11 → 4.6.3. Root
  cause: **v4.5.0's documented breaking tracing cleanup** — `trace.http.header` /
  `trace.http.legacy.header.enabled` removed, so `X-Correlation-Id` no longer doubles as the trace id
  (the pre-4.5.0 conflation is what made 4.4.x setups appear to propagate); the trace id now travels
  only as `X-Trace-Id` / W3C `traceparent`, never echoed back. **Support checklist for this symptom:**
  (1) `tracing: true` on every rest.yaml entry involved (per-endpoint, default FALSE — without it the
  endpoint ignores inbound trace headers); (2) app-to-app calls must go through `async.http.request`
  (only the framework HTTP client auto-stamps X-Trace-Id + traceparent; custom/Spring clients must
  forward `traceparent` manually); (3) trace context is thread-bound (Mono path fixed in 4.6.x —
  [[trace-thread-keyed-mono-gotcha]]). **Validated**: live sync-over-async-demo (2 JVMs, curl with a
  caller traceparent → one continuous trace, spans chained onto the caller's span across both Kafka
  hops) + new regression test `traceContinuesAcrossApplicationToApplicationHttpCall`
  (branch `test/trace-continuity-http-hop`, `DownstreamCaller` fixture + `/api/chain/probe`) proving
  traced app-to-app HTTP continuity over the real HTTP stack — a previously untested contract.
  <!-- id: field-trace-propagation-4-6-3-diagnosis | created: 2026-07-13 | last_used: 2026-07-13 | uses: 1 | tier: working | origin: 2026-07-13-142021 -->

- **Snyk gate rejected v4.8.2 in the field (2026-07-13) — remediated and MERGED (PR #168, merge
  commit `e21be449`, 30 poms; ships in v4.8.3; the team trials v4.7.1 for the trace fix meanwhile —
  4.7.1 carries the same three vulnerable transitives, so it is trial-only, not deployable).** Three transitive findings, all
  reported through org.platformlambda artifacts: httpcore5/-h2 5.4.2→5.4.3 (HIGH CWE-770), log4j-api
  2.25.4→2.26.1 (MEDIUM CWE-116), reactor-netty-http 1.3.5→1.3.6 via reactor-bom 2025.0.5→2025.0.6
  (MEDIUM CWE-319). **Durable lessons:** (1) **Spring Boot's `dependencyManagement` beats
  nearest-wins** — a direct dependency pin does NOT propagate to consumers when SB's parent manages
  the artifact (why Snyk said "no supported fix"); the correct idiom is overriding the SB *property*
  (`httpcore5.version`, like `log4j2.version`/`tomcat.version`/`netty.version`) in **every** module
  that resolves the chain (here: the six Confluent-chain modules). The scram-client direct-pin
  precedent only worked because no management was in play. (2) **A dependency-version sweep must also
  find poms *missing* the property** — the 4.8.1 log4j sweep updated only poms that already had
  `log4j2.version`, leaving four on SB's vulnerable default. (3) Reactor-wide verification =
  `mvn dependency:tree` grep for the vulnerable versions across all modules **plus** the non-reactor
  subprojects (api-playground, pg-example, benchmark-reporter — field scans cover them too).
  Relates [[release-4-8-2-shipped]], [[field-sonar-covers-all-sources]].
  <!-- id: snyk-4-8-2-remediation | created: 2026-07-13 | last_used: 2026-07-13 | uses: 1 | tier: working | origin: 2026-07-13-135841 -->

- **Release 4.8.2 — SHIPPED 2026-07-12 (tag `v4.8.2` on merge commit `6c024311`; PRs #164-#166).**
  Patch release: twin-kafka-demo correlation-id impedance matching + opt-in template
  externalization. Cross-vendor loop precedent: GitHub Copilot authored the patch (its session log
  2026-07-13-001009), crashed mid-task; Claude Code reviewed all edits, completed the crash gap
  (twin-kafka.md table, configuration-reference rows, sync-over-async properties) and committed with
  dual attribution (one Co-Authored-By per collaborator per AGENTS.md). **Durable facts:**
  (1) **Impedance matching is the demo pattern:** each cluster keeps its own business
  correlation-id header (on-prem X-Correlation-Id, cloud X-Cloud-Correlation-Id); adapters read the
  cluster header into model.cid, flows map model.cid back out under the NEXT cluster's name — never
  leak a cluster's header name across the bridge (twin-kafka tests assert this at the wire level).
  (2) **Template externalization is OPT-IN** (scope: minimalist-kafka/twin-kafka family only):
  KafkaClientConfig + SecondaryKafkaAutoStart location defaults are classpath-only; devops
  pipelines set the location key to a rendered file, optionally with a classpath fallback chain —
  field migration note in the 4.8.2 CHANGELOG (deployments relying on the implicit /tmp/config
  fallback must set keys explicitly). Legacy connector stack + mini-scheduler keep their own
  file-first conventions. (3) **SOR response leg publishes via secondary.kafka.notification** —
  the demo response stays on the cloud cluster until the bridge consumes it; Spring profiles are
  logical personalities, not security boundaries (README documents that real isolation needs
  separate deployment/credentials/network policy). (4) **Release-sweep gotcha:** when the outgoing
  version is a substring of a dependency version (classgraph 4.8.184 contains "4.8.1"), the perl
  sweep needs a digit lookahead `(?!\d)` — not every bump is naturally safe.
  See [[release-4-8-1-shipped]] for the prior cycle's facts.
  <!-- id: release-4-8-2-shipped | created: 2026-07-12 | last_used: 2026-07-12 | uses: 1 | tier: active | origin: 2026-07-13-014037 -->

- **Release 4.8.1 — SHIPPED 2026-07-11 (tag `v4.8.1` on merge commit `3d226c5b`; PRs #159-#161).**
  Maintenance release: dependency security updates (Jackson 2.22.1 closed dependabot/28; log4j2/
  netty/tomcat/gson/vertx refreshed), twin-kafka-demo, SimpleRandomPartitioner, DSA retirement and
  the repo-wide coverage program. **Durable facts:** (1) **SimpleRandomPartitioner is
  minimalist-kafka's producer DEFAULT** (`putIfAbsent` in KafkaClientConfig — a template's own
  `partitioner.class` wins; explicit `partition` header bypasses partitioners; keyed records keep
  murmur2; SecureRandom shared instance — ThreadLocal avoided per repo convention). Kafka's sticky
  default starves multi-instance consumer groups at low volume — proven in the demo (all-on-one
  partition before, spread after). (2) **CryptoApi DSA methods REMOVED** (4.8.1) — the SHA256withDSA
  field-Sonar hotspot is resolved at the source; no UI disposition needed. (3) **Ten example modules
  build+test inside the reactor** (pg-example standalone: embedded-postgres binary download;
  benchmark-reporter off) — release-bump sweep is now 32 poms; CI runs ~918 tests. (4) **Coverage
  aggregate 85.8% line / 80.0% Sonar-combined** — zero margin: field Sonar config should exclude
  `benchmark/**` (938 untested lines), and pg-example's coverage needs its own `mvn test` in the
  pipeline. kafka-connector's remaining gap (topic substitution + boot branches) needs a second
  test-app config — not reachable in one JVM. (5) **Flow-authoring conventions** (learned via
  twin-kafka-demo, examples/twin-kafka-demo): the engine does NOT auto-convert a Map into a
  byte[]-typed function — use `f:binary(model.x) -> *` (the `:binary` colon shorthand is
  DEPRECATED); set `text(application/json) -> output.header.content-type` on flow HTTP responses
  (platform-core otherwise content-negotiates from the request's accept header). (6) **No version
  strings in pom comments** — the release sweep mangled a historical "retired 4.8.0" note; history
  belongs in the CHANGELOG. See [[release-4-8-0-shipped]] for the twin-kafka architecture facts.
  <!-- id: release-4-8-1-shipped | created: 2026-07-11 | last_used: 2026-07-13 | uses: 1 | tier: active | origin: 2026-07-12-002326 -->

- **Release 4.8.0 — SHIPPED 2026-07-10 (tag `v4.8.0` on merge commit `5d9fda45`; PRs #153-#157).**
  Feature release: **twin-kafka** (dual Kafka cluster bridging), configurable trace-id headers with
  per-entry overrides, and the hardened model.cid path. **Durable architecture facts:**
  (1) **twin-kafka is a separate `system/` module depending on minimalist-kafka** — dual-cluster is a
  special case; single-cluster apps must never carry its weight. Naming: module twin-kafka, artifacts
  `secondary.*` (plain English; "gemini" rejected — GEMINI.md/Google clash). A bridge is flow YAML:
  consume via one adapter, publish via the other cluster's notification function; trace + model.cid
  continuous across both hops. (2) **Reuse seams in minimalist-kafka** (behavior-preserving):
  KafkaClientConfig location-key overloads; SimpleKafkaNotification protected accessors (publisher/
  codec/header names/registryUrlKey); SchemaCodec.fromConfig(config, url, keyPrefix) deriving keys,
  serde prefix, template location AND ManagedCache names from the prefix — **distinct caches per
  registry are a correctness requirement** (Confluent global schema ids are per-registry; bridging
  framed payloads = decode-and-re-encode via schema.enabled + subject, NEVER relay raw framed bytes).
  (3) Registry is optional PER CLUSTER (real-world: on-prem Apache + cloud Confluent); Azure Event
  Hubs works via the Kafka endpoint (no Confluent registry, pre-provisioned topics). (4) DLQ
  correctness: RetryPolicy carries the publisher → secondary dead letters land on the secondary
  cluster. (5) kafka-standalone `dual.servers=true` = broker 9092 + broker 8092; twin templates
  default to 8092. (6) **Header-name precedence** (both surfaces): per-entry (rest.yaml /
  kafka-flow-adapter.yaml `trace.id.header`/`correlation.id.header`) > application.properties global
  (`http/kafka.trace.id.header`, `http/kafka.correlation.id.header`) > built-in default; W3C
  traceparent always wins for the trace-id. (7) **Flow convention:** map the business correlation-id
  from `model.cid` (engine-seeded), never from the raw record header; CompileFlows rejects data
  mappings that overwrite reserved model keys (cid/instance/flow/ttl). See [[release-4-7-0-shipped]]
  for the release-bump surface and prior caveats.
  <!-- id: release-4-8-0-shipped | created: 2026-07-10 | last_used: 2026-07-13 | uses: 1 | tier: active | origin: 2026-07-11-031930 -->

- **Release 4.7.0 — SHIPPED 2026-07-08 (tag `v4.7.0` on merge commit `e41a20b7`; PRs #146 feature +
  #147 bump).** Feature release: **MiniGraph `graph.task` skill** — a Task node invokes any composable
  function (`@PreLoad` route) with Event Script style `input[]`/`output[]` mapping (`*` whole-body
  sequential merge, `header.{name}`, PoJo auto-conversion, `for_each[]`+`concurrency` fork-join,
  `exception=` routing); plus tutorial-13 (deployed graph + help topics) and playground UI bundle
  refresh. **STRATEGIC (Eric): graph.task is intended to be the LAST built-in skill** — custom logic
  now enters a graph as a composable function; flows/subgraphs (graph.extension) remain for
  orchestration. **Durable caveats:** (1) **new-skill/tutorial checklist** — a shipped `help graph-*.md`
  page MUST have a matching entry in `docs/guides/knowledge-graph/minigraph-commands.json` and the docs
  mirror (skills-reference, command-reference matrix, index tables — "eight skills" counts); CI's
  `scripts/check-minigraph-grammar.py` anti-drift gate fails the PR otherwise (caught live on PR #146);
  (2) `help.md` topic index is hand-maintained — new skills/tutorials must be added there too;
  (3) test-only tutorial graphs are numbered 1xx (tutorial-113/114) so deployed tutorials own the
  canonical names. Release-bump surface and prior caveats unchanged — see [[release-4-6-2-shipped]].
  **UPDATE 2026-07-09: v4.7.1 shipped the next day** (tag on merge commit `60716bf9`; PRs #150 feature +
  #151 bump) — **minimalist-kafka Schema Registry OAuth 2.0**, closing the 4.6.3 field loose ends.
  Durable facts: (1) **`schema-registry.properties` is the third client template** (verbatim pass-through
  to the Confluent SR client; `schema.registry.url` stays in application.properties as the feature
  switch — Eric: "layered approach that is a best practice"); (2) **OAuthUrlAllowList auto-registers
  token endpoint URLs** on the JVM system property `org.apache.kafka.sasl.oauthbearer.allowed.urls`
  at template-load time (merge/dedupe, never clobber) — BOTH the Kafka transport and the Confluent SR
  OAuth provider validate against it (Confluent reuses Kafka's ConfigurationUtils.validateUrl);
  (3) **CODE CONVENTION: prefer `ReentrantLock` over `synchronized`** in framework code
  (virtual-thread friendly — Eric's review feedback); (4) EmbeddedSchemaRegistry has an opt-in OAuth
  mode (HS256 token endpoint + Bearer enforcement) for authenticated registry tests.
  <!-- id: release-4-7-0-shipped | created: 2026-07-08 | last_used: 2026-07-10 | uses: 2 | tier: archive-candidate | origin: 2026-07-08-224933 -->

- **Release 4.6.1 — security + maintenance patch on top of 4.6.0 (2026-07-06, branch `chore/release-4.6.1`,
  Claude Code).** 4.6.0 was already GitHub-released (tag `v4.6.0`, immutable); rather than recall/re-tag it,
  Eric chose to supersede with a clean 4.6.1 (treat a published release as immutable). **Scope:** (1) **Snyk
  OSS fixes** — `org.postgresql:r2dbc-postgresql` 1.1.1→1.1.2 in `examples/pg-example` +
  `extensions/reactive-postgres`, plus direct `com.ongres.scram:scram-client`/`scram-common` `3.3` deps that
  override the transitive **vulnerable 3.2** r2dbc-postgresql 1.1.2 pulls in (same coords + major → nearest-wins,
  no leftover 3.2; Eric confirmed Snyk wants 3.3); (2) **`opentelemetry-forwarder` — protobuf runtime removed
  from the module entirely** — OTel BOM 1.45.0→1.63.0 and dropped the test-scoped
  `io.opentelemetry.proto:opentelemetry-proto` (the only thing pulling in `com.google.protobuf`, retired for a
  no-fix CVE). Verified `com.google.protobuf` resolves on **no scope**. Production export path was already
  protobuf-java-free (OTLP/HTTP exporter uses OTel's internal marshaler in `opentelemetry-exporter-otlp-common`,
  not protobuf-java). **`MockOtlpCollector` (test) rewritten** to decode OTLP protobuf with a small hand-rolled
  wire-format `ProtoReader` (no generated classes); full round-trip coverage retained (22 tests green). Key bug
  found+fixed during the rewrite: a `pos += (int) readVarint()` compound-assignment discarded the read-advance
  (Java captures the LHS before the RHS mutates `pos`). (3) **Added two OTLP exporter tunables** —
  `otel.exporter.otlp.compression` (`gzip`/`none`, default `none`) + `otel.exporter.otlp.connect.timeout`
  (ms, default 10000), wired via a new 5-arg `OtelForwarderContext.buildExporter` (old 3-arg kept, defaults
  unchanged). Harness limitation noted: Mercury REST automation delivers a `null` body for a gzip-encoded
  request, so the gzip test asserts `Content-Encoding: gzip` on the wire rather than an inflate-decode round-trip.
  **Version bump 4.6.0→4.6.1:** 31 module poms + CHANGELOG (Security/Added/Changed) + docs/guides + example/helper
  READMEs + CLAUDE.md/GEMINI.md + memory current-version refs. **Pre-existing, left as-is:** `OtelForwarderContext.VERSION`
  instrumentation-scope constant still reads `4.5.0` (was not bumped for 4.6.0 either — out of release scope; flagged).
  See [[thread-release-4.6.1-field-scan]] and the earlier [[snyk-oss-dependency-update-2026-07]].
  <!-- id: release-4.6.1-security-patch | created: 2026-07-06 | last_used: 2026-07-06 | uses: 1 | tier: working | origin: 2026-07-06-164315 -->

- **platform-core gotcha: the per-function trace context is thread-id-keyed and torn down when the worker
  returns.** `EventEmitter.traces` is keyed by `Thread.currentThread().threadId()+instance+route`, and
  `WorkerHandler` calls `stopTracing` (removing it) as soon as `processEvent` returns. So any work that
  finishes on a **different thread or after the worker returns** (notably a `Mono`/`Flux` completion on the
  reactor executor) **cannot** call `getTrace(...)` to read its own span/annotations — it must **capture the
  `TraceInfo` on the worker thread first**. This caused Mono-returning flow tasks to drop their `span_id`
  from the response, orphaning the next task's `parent_span_id` (fixed 2026-06-28 in
  `WorkerHandler.handleMonoResponse` via `applyTraceContext`; see `WorkerHandlerTest.monoResponseForwardsSpanId`).
  Watch for this in any future async/reactive code that needs trace context. The **Flux** path was checked
  and is **safe** — it returns its response (the `x-stream-id` handle) synchronously on the worker thread, and
  `FluxPublisher` streaming never reads the trace (guarded by `WorkerHandlerTest.fluxResponseForwardsSpanId`).
  <!-- id: trace-thread-keyed-mono-gotcha | created: 2026-06-28 | last_used: 2026-06-28 | uses: 1 | tier: core -->

- **platform-core serializes `java.time.Instant` as first-class (2026-06-27).** Instant had no adapter and
  round-tripped wrongly (Gson reflected it to `{seconds,nanos}`; MsgPack fell through to String/PoJo).
  Fixed at the root in all three serialization paths — `SimpleMapper` (Gson adapter), `MsgPack` (nested
  `case Instant`), `PayloadMapper` (top-level encode) — each mirroring `Date` via
  `date2str(Date.from(instant))` → UTC, **millisecond-precision** ISO-8601/RFC-3339 string (same wire format
  as Date; sub-ms precision is intentionally dropped for consistency). Prefer `Instant` over `java.util.Date`
  in new code (also clears SonarQube `java:S2143`). Relates to `typed-io-map-or-pojo` (ADR-0003).
  <!-- id: instant-serialization | created: 2026-06-27 | last_used: 2026-06-27 | uses: 1 | tier: core -->

- **Service mesh is opt-in, not the default.** `cloud.connector=none` is the framework default. The Kafka
  service mesh (`cloud.connector=kafka` + presence-monitor) solves exactly two problems: (1) synchronous
  request-response across application instances over Kafka (sync over async), and (2) service discovery
  between pods. Applications that do not need either must be designed cloud-native (self-contained,
  horizontally scaled, no cross-instance coupling). Superimposing sync over async is a recipe for a
  "distributed monolith" — full operational cost of distribution with monolith-level coupling. The mesh is an
  advanced opt-in for specific use cases (cross-application RPC, leader selection, pod-aware broadcast).
  This preference must be front-and-center in documentation and AI guides. (ADR-0006)
  <!-- id: kafka-mesh-opt-in | created: 2026-06-23 | last_used: 2026-06-24 | uses: 2 | tier: core -->
- **Event Script config is preferred over code for orchestration.** When a step is orchestration —
  sequencing functions, branching, failure handling, moving data — express it as Event Script YAML
  (tasks, `execution` types, I/O data mapping, exception handler), not imperative code; code is reserved
  for the unit of work (the function body). Two reasons: it **communicates intent** (the flow file is a
  legible statement of the event flow — sequence, topics, fail-fast path, branches — without reading
  Java) and it **manages dependencies** (the engine enforces control- and data-flow wiring, functions
  stay decoupled per `functions-decoupled-routes`, reusable blocks like `simple.kafka.notification` are
  composed by reference not duplicated). Bounded by `one-atom-four-roles`: not all code becomes YAML — an
  intrinsically in-function concern (e.g. a blocking rendezvous that must wrap a publish) stays in code.
  Routing vocabulary to learn: `decision` selects a `next` entry by value (`true`=`1`=first, `false`=`2`=
  second; integer is 1-based → multi-way switch — engine `TaskExecutor.handleDecisionTask`, intentional;
  several *derived* docs had it inverted and were corrected 2026-06-27), and `byte[]` rides through
  `model` via the `*` passthrough. Distilled from the sync-over-async composable refactoring (2026-06-27,
  Claude Code). (ADR-0007)
  <!-- id: event-script-over-code | created: 2026-06-27 | last_used: 2026-06-27 | uses: 1 | tier: core -->
## Conventions

- Add capability: function (`@PreLoad` + `TypedLambdaFunction`) → flow YAML →
  register in `flows.yaml` → `rest.yaml` mapping if HTTP-facing.
  <!-- id: conv-add-capability | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->
- Watch serialization gotchas (Long↔Integer downcast; use `util.str2int/str2long`).
  <!-- id: conv-serialization-gotchas | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->

## Blueprint  *(gap from Current State → Vision; `(blueprint)` threads serve `vision-mercury-composable`)*

- [ ] (blueprint) Integrate a **pluggable AI companion LLM backend**; mature `POST /api/companion/{id}`
  from a dev-only command pipe into a governed collaboration layer. → serves: vision-mercury-composable
  <!-- id: bp-ai-companion-llm-backend | created: 2026-06-20 | last_used: 2026-06-21 | uses: 1 | tier: working -->
- [ ] (blueprint) **Enterprise governance lifecycle** for graph models (dry-run → certify → stage →
  approve → production), so models promote to production as standard endpoints. → serves: vision-mercury-composable
  <!-- id: bp-graph-governance-lifecycle | created: 2026-06-20 | last_used: 2026-06-21 | uses: 1 | tier: working -->

## Open Threads

- [ ] (release in flight — 2026-07-13) **v4.8.3 bumped for field pipeline test; TAG DEFERRED.**
  Branch `chore/release-4.8.3` (commit `00c3b285`) carries the Snyk remediation (#168), trace
  regression guard + observability docs (#169), ScheduleAdminTest race fix (#170). Eric syncs main
  to the field after merge. **Do NOT create the v4.8.3 tag until the field pipeline test passes and
  publishes to the artifact repository** (Eric's instruction — leaves room for further fixes). When
  the field passes: tag the merge commit, provide release text, then the memory-closure PR with a
  [[release-4-8-3-shipped]] fact superseding the in-flight state.
  <!-- id: thread-release-4-8-3-tag-deferred | created: 2026-07-13 | last_used: 2026-07-13 | uses: 1 | tier: working | origin: 2026-07-13-142021 -->

- [ ] (field support — 2026-07-13, awaiting field team's response via Eric) **Trace-propagation report on
  4.6.3: confirm the field config fix.** Diagnosis + support checklist in
  [[field-trace-propagation-4-6-3-diagnosis]]; the two questions back to the team: is `tracing: true` set
  on every rest.yaml endpoint involved, and is the app-to-app call made through `async.http.request` (vs
  a custom/Spring HTTP client that must forward `traceparent` itself)? Regression test + observability
  impedance-matching docs MERGED (PR #169); ScheduleAdminTest race fix MERGED (PR #170 — file existence
  ≠ readiness when writes are truncate-then-write; poll the consuming API for settled state). The team
  trials v4.7.1 for the trace behavior while v4.8.3 is prepared. Also answered (2026-07-13): setting
  `http.trace.id.header` = `http.correlation.id.header` = `X-Correlation-Id` (legacy conflation) is safe
  when the edge always supplies the header — one value feeds both ids end-to-end; when absent, the two
  ids diverge by design (trace via traceparent stays continuous; shared-header slot resolves cid-last on
  HTTP, trace-last on Kafka).
  <!-- id: thread-field-trace-propagation-4-6-3 | created: 2026-07-13 | last_used: 2026-07-13 | uses: 1 | tier: working | origin: 2026-07-13-142021 -->

- [ ] (P0–P5 code-complete — Claude Code, 2026-07-05, branch `feature/elastic-queue-file-fifo`; remaining = field canary → P4 retire-BDB) **Replace
  ElasticQueue's Berkeley DB spill tier with a portable file-backed segmented FIFO.** Full detail + rationale
  (perf/complexity/Rust portability) in the Key Decision [[elastic-queue-file-fifo-plan]] and the design spec
  `draft-design-specs/elastic_queue_file_fifo_design.md` (gitignored). **P0 (`fc225d34`):** quantified the BDB
  event-loop tail. **P1 (`07221351`):** `ElasticQueue` → facade over an `ElasticStore` strategy
  (`elastic.queue.store = bdb | file`, default `bdb`); `BdbElasticStore` = old logic verbatim, `FileElasticStore`
  = new per-route segmented append FIFO; parity test passes both. Worse-case file vs bdb: write p99.9
  1.63ms→0.035ms (~46×), read max 98ms→3.9ms (~25×), stalls>20ms 53→2 — **tail flattened.** **P2 (`2885d9e3`):**
  hardening tests (multi-segment FIFO, bounded-disk reclamation, no-leak across 50 reuse cycles, clean reuse,
  degenerate inputs); `segmentBytes` → per-instance config. **Root-caused the 582ms `file` outlier: NOT GC**
  (max GC pause 2.0ms) → OS dirty-page-flush throttle. Decided (Eric): no in-store flusher; **P5 elevated to
  RECOMMENDED** (per-route VT off-loading makes on-loop spill stalls harmless) + **document tmpfs**. Full
  platform-core 381 green. Remaining P2: external-IO fault injection (needs a mockable seam; deferred).
  **P3 A/B (`73d1959a`, reports saved via `-Dbench.report`):** file vs bdb — throughput +56%, write p99.9
  1.66ms→0.035ms (~47×), read max 54ms→3.5ms (~15×), stalls>20ms 90→3; file's only blemish a single 552ms
  OS-flush outlier (bdb's badness is pervasive: 90 stalls). **`file` decisively flattens the tail — goal met.**
  **P5 (`e8e5cdee`):** off-loop dispatch — `elastic.queue.dispatch = loop | vthread` (default `loop`); in
  `vthread` a per-route virtual thread runs the `ServiceQueue` state machine + blocking spill I/O, so the
  552ms OS-flush stall parks the VT carrier, not the shared loop. Full platform-core 381 green in BOTH modes;
  `DispatchBenchmark` loop 83k vs vthread 98k events/s (+18% under load, no hot-path regression); measured
  mailbox fast-path overhead ~5–9µs/dispatch. **Reactor validation (`dd77f067`) found BDB pins VT carriers**
  (vthread+bdb starved minigraph; vthread+file green everywhere) → **simplified to ONE switch:**
  `elastic.queue.store` alone (`bdb`⇒loop, `file`⇒vthread, derived from `supportsVirtualThreadDispatch()`);
  removed the standalone `elastic.queue.dispatch` config so vthread+bdb is unreachable. Verified: platform-core
  381 green both modes, minigraph 55 green with file.
  **Cleanup isolation (`70c9feff`):** annotated the `elastic.queue.cleanup` `Cleanup` class `@KernelThreadRunner`
  so its heavy BDB `cleanLog()`/cursor work runs on a kernel thread — closes a live in-field VT-pinning vector
  (independent of dispatch mode). **Canary field notes** drafted at
  `draft-design-specs/elastic-queue-file-mode-field-notes.md` (gitignored; graduate into the PR/runbook at merge).
  **All phases P0–P5 + the mode simplification + cleanup isolation implemented + tested; submitted as
  PR [#137](https://github.com/Accenture/mercury-composable/pull/137)** (`feature/elastic-queue-file-fifo` →
  `main`; opt-in, BDB stays default). **Field-test prep (2026-07-05, on the same branch, NOT pushed yet):**
  (1) flipped the default to `file` (`6e21fcd1`; bdb = explicit fallback; platform-core 381 green on the new
  default); (2) added the `benchmark/benchmark-reporter` module (`e4c39aee`) — self-contained single-JVM
  callback+RPC end-to-end harness → self-contained HTML report (inline SVG histogram + percentile plot),
  runnable in the field/pipeline, records store+dispatch for file-vs-bdb A/B. **Open decision for Eric:** the
  flip inverts PR #137's "default unchanged" premise — fold into #137 (re-frame) or split into a follow-up PR;
  **Eric chose (2026-07-05): fold into PR #137 + re-frame** (file = default + benchmark tooling); pushed
  `44202a57`, so #137 is now the whole field-test-ready change (re-framed title/description handed to Eric).
  **Review hardening (GitHub Copilot, 2026-07-05):** bounded the off-loop dispatch mailbox
  (`elastic.queue.dispatch.mailbox.size`, default 1024, clamped ≥ memory buffer) with no-drop back-pressure;
  changed `FileElasticStore` to keep only O(1) segment channels open, add RUNNING/keepalive stale-dir cleanup,
  purge current leftovers at startup, and clear `peeked` on reset for both stores; added regressions + small
  benchmark-reporter smoke. **Next = field steps: run benchmark-reporter on real envs, then P4 retire BDB.** Docs/ADR sync tracked
  separately in [[thread-elastic-queue-docs-adr]].
  <!-- id: thread-elastic-queue-bdb-to-file | created: 2026-07-05 | last_used: 2026-07-06 | uses: 12 | tier: working | origin: 2026-07-05-033922 -->

- [ ] (backlog — do at ElasticQueue merge / P4, Claude Code 2026-07-05) **Docs sync + ADR for the ElasticQueue
  file store / off-loop dispatch.** Deferred deliberately: nothing in the current guides is *wrong* today
  (default `bdb` is unchanged; Berkeley DB is never named in the guides), and the config surface is still moving
  (P4 retires BDB → removes `deferred.commit.log`, the `elastic.queue.cleanup` reserved route, and collapses
  `elastic.queue.store`). When the branch merges (post-canary) / at P4: (1) `docs/guides/configuration-reference.md`
  — add `elastic.queue.segment.size.bytes`, adjust `elastic.queue.store` to the final surface, add the tmpfs tip
  on `transient.data.store`, remove/soften `deferred.commit.log`; (2) `docs/guides/reserved-names-and-headers.md`
  — remove `elastic.queue.cleanup` when BDB is retired; (3) `docs/guides/architecture.md` — refresh the
  overflow-buffer line to the file segmented FIFO + off-loop VT dispatch (VT-first/portability angle);
  (4) propose an **ADR** in `docs/arch-decisions/ADR.md` for the durable decision (replace BDB, off-loop VT
  dispatch, one switch) — human-gated; (5) graduate the field notes
  (`draft-design-specs/elastic-queue-file-mode-field-notes.md`) into the PR description / canary runbook.
  Relates [[thread-elastic-queue-bdb-to-file]], [[elastic-queue-file-fifo-plan]].
  <!-- id: thread-elastic-queue-docs-adr | created: 2026-07-05 | last_used: 2026-07-05 | uses: 1 | tier: working | origin: 2026-07-05-033922 -->

- [ ] (planned — backlog, no ETA) **Reintroduce Protobuf support in minimalist-kafka's Schema Registry
  integration.** Blocked on Confluent adopting the renamed `com.squareup.wire:wire-runtime` coordinate in
  `kafka-protobuf-provider` (still unchanged at `wire-runtime-jvm:5.5.0` as of `8.3.0`, checked 2026-07-01 —
  re-check on any future Confluent release before assuming it's fixed). Alternative unblocking paths, either
  of which could move this sooner than waiting on Confluent: (a) vendor a patched fork of `wire-runtime-jvm`
  — the upstream fix (reject a negative varint length in `skipGroup()`) is a tiny, well-understood one-line
  change; (b) a specific field installation explicitly needs Protobuf and accepts the residual
  CVE-2026-45799 risk, reintroducing it for that installation only. See [[minimalist-kafka-protobuf-removed]]
  for the removal rationale and exactly what to restore (`ProtobufSchemaSerde`/`ProtobufConversions`,
  the `kafka-protobuf-serializer` dependency, the demo's protobuf-topic-1/2 path — all still in git history).
  <!-- id: thread-minimalist-kafka-protobuf-revival | created: 2026-07-01 | last_used: 2026-07-01 | uses: 1 | tier: working | origin: 2026-07-01-224313 -->

- [ ] (backlog — Eric, 2026-07-02; next iteration, not scoped) **`CompileGraph` does not carry out
  comprehensive syntax validation.** Raised during PR #130 code review closeout — not a blocker for that
  sprint. `CompileGraph` today does structural validation (`MiniGraph.importGraph()`) and the deprecated-
  syntax conversion pass ([[event-script-minigraph-code-review-2026-07]]), but "comprehensive syntax
  validation" for the mapping-string mini-DSL itself (the same one `DataMappingHelper`/`SimpleTypeMatching
  Converter` handle) isn't yet defined or scoped — needs its own design pass before implementation:
  what would "comprehensive" mean here (malformed plugin calls? unknown plugin names? arg-count/type
  checks?), and should it reuse or diverge from `event-script-engine`'s own `validInput`/`validOutput`
  validation (already confirmed to diverge in places — minigraph's per-skill namespace rules, e.g. fetcher
  input/output/dictionary, don't match event-script's).
  <!-- id: thread-compilegraph-syntax-validation | created: 2026-07-02 | last_used: 2026-07-02 | uses: 1 | tier: working | origin: 2026-07-02-004606 -->

- [ ] (planned — backlog, no ETA, no CVE driver) **Upgrade `kafka.version` (4.2.0 → 4.3.x) across the 24
  pom.xml files that pin it.** Deliberately deferred alongside the `confluent.version` 8.2.0→8.3.0 bump — see
  [[minimalist-kafka-confluent-8-3-0]] for the full reasoning. Confluent Platform 8.3.x's own tested pairing
  is Kafka 4.3.x, but nothing here requires following that pairing (this repo pins its own Kafka client
  independent of Confluent's suggestion, and the Confluent serializers are client-version-tolerant by
  design). Scope when picked up: verify kafka-clients 4.3.x + the embedded KRaft broker (`kafka_2.13:4.3.x`)
  behavioral compatibility (config defaults, controller behavior) across all 24 files, not just
  `minimalist-kafka` — a materially larger test surface than a serializer-library bump.
  <!-- id: thread-kafka-client-version-upgrade | created: 2026-07-01 | last_used: 2026-07-01 | uses: 1 | tier: working | origin: 2026-07-01-230246 -->

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
  **Remaining (post-MVP):** ~~Redis coordinator config-driven init~~ **done** ([[soa-config-driven-init]]),
  ~~consumer partition-pinning~~ **done 2026-06-27** ([[kafka-partition-pinning]]), ~~module docs~~ **done
  2026-06-27** (mkdocs guides — [[kafka-soa-docs]]); still open — 503 guardrails/metrics, two-JVM test,
  per-module README (code-level); and Gradle build (`thread-add-gradle-build`).
  Also done this sprint: externalized Kafka client config ([[kafka-client-config-templates]]), configurable
  per-binding consumer group, and the Copilot-review hardening (incl. [[kafka-flow-failure-dlq]]).
  **2026-07-02 extension (PR #133, `feature/kafka-regex-dlq-commit-mode`):** regex topic subscription
  (`topic-pattern`), per-binding `dlq-topic` (replaces `kafka.flow.dlq.suffix`), `auto-commit` delivery
  mode (`max-poll-records` configurable), and `metadata.*` (topic/partition/offset/timestamp/key) injected
  into the flow input dataset. `KafkaConsumerBinding` (builder pattern) carries all per-binding config.
  PR open: https://github.com/Accenture/mercury-composable/pull/133
  **Review round 1 (2026-07-03, Copilot) applied:** merged `main` (PR #132) clean; fixed the inbound
  business-cid loss ([[kafka-flow-consumer-cid-header]]) + the stale `kafka-demo` DLQ-suffix config.
  **E2E-validated** against `sync-over-async-demo` (byte[] round-trip, 3-pod trace continuity, cid
  propagation, cross-pod Redis return route, 408 timeout).
  **Review round 2 (2026-07-03):** corrected the stale "model.cid is RPC noise" comments in the two
  test-resource sink flows (the round-1 fix made model.cid carry the Kafka cid); added embedded-Kafka
  `KafkaFlowAdapterTest` assertions that a task's `getMyCorrelationId()` == the sent cid, regression-guarding
  the fix end-to-end. See [[kafka-flow-consumer-cid-header]].
  **Terminology refactor (2026-07-04):** renamed the ambiguous `correlationId` naming to
  `businessCorrelationId`/`internalCorrelationId` across event-script-engine, minimalist-kafka,
  platform-core's HTTP path, and `extensions/sync-over-async` — Java identifiers only (fields/locals/internal
  constants); `EventEnvelope.cid`, `PostOffice.getMyCorrelationId()`, and all wire header strings/config keys
  are unchanged. Full reactor + all affected test suites green. See
  [[business-vs-internal-correlation-id-terminology]].
  **Test-infra improvements (2026-07-04, reviewer-driven):** (1) added `topic-pattern` embedded-Kafka e2e
  test proving `subscribe(Pattern)` + `input.metadata.*` surfaces the concrete matched topic/partition
  (was mock/config-only); (2) renamed the JSON schema-registry test path `schema-*`→`json-*` to sit
  parallel to the Avro case; (3) split the shared `schema-sink-flow`/`SchemaSinkTask` (an agile artifact -
  JSON first, Avro bolted on) into per-format `json-sink-flow`/`JsonSinkTask` + `avro-sink-flow`/
  `AvroSinkTask`, removing the latent shared-queue coupling (codec id-dispatch still proven by
  `SchemaCodecTest`). minimalist-kafka 78 tests green.
  **Review-driven hardening pass (2026-06-26, Claude Code):** applied the Copilot review
  (`draft-design-specs/kafka-sync-over-async-review.md`) via `apply-critique` — 6 fixes across both modules:
  mk#1 producer failure-logging callback (still drop-n-forget), mk#2 consumer retry→DLQ (`kafka-flow-failure-dlq`),
  mk#3 fail-fast flow-adapter config validation; soa#1 atomic-reservation cap (TOCTTOU), soa#2 explicit Redis
  DEL cleanup on success, soa#3 `start()` double-invocation guard + graceful `close()`. Deferred design nits
  mk#4 (`KafkaRuntime` singleton), mk#5 (poll loop on platform thread — non-issue), soa#4 (coordinator
  decomposition). Green: minimalist-kafka 12 tests, sync-over-async 24 tests, both coverage gates met. (The
  older `evaluation_feedback_report.md` is a stale Gemini Phases-1&2 report describing pre-extraction code —
  superseded, not the Copilot review.)
  <!-- id: thread-redis-kafka-rpc | created: 2026-06-24 | last_used: 2026-06-27 | uses: 6 | tier: working -->

- [ ] **Re-verify invariants (due — 50 sessions since the last check ≥ verify_invariants_every 40).** Raised by
  the 2026-06-29 review (cadence). Confirm each never-decay fact still holds, or supersede any that don't
  (`DECAY.md` §9 — the review never auto-invalidates):
  core stack — `stack-language-java21`, `stack-build-maven`, `stack-integration-spring`,
  `stack-messaging-kafka`, `stack-ci-gha`; architectural invariants — `functions-decoupled-routes`,
  `typed-io-map-or-pojo`, `virtual-threads-rpc`; core gotchas/decisions — `trace-thread-keyed-mono-gotcha`,
  `instant-serialization`, `kafka-mesh-opt-in`, `event-script-over-code`, `conv-add-capability`,
  `conv-serialization-gotchas`; and the **Vision** (`memory/vision.md`). Check off when re-confirmed.
  <!-- id: thread-reverify-invariants-2026q2 | created: 2026-06-29 | last_used: 2026-06-29 | uses: 1 | tier: working -->

## User Preferences

## Team / Members

(none recorded yet)
