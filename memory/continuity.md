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
- **last_session:** 2026-07-01T21:55:33Z | agent: Claude Code (2026-07-01-215533)
- **last_review:** 2026-06-29 | through 2026-06-29-223651.md
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

- **Application log context — virtual-thread-safe MDC alternative (2026-06-30, PR #128).** A `context`
  block (cid, traceId, tracePath, spanId, parentSpanId, service, utc + developer key-values) is injected
  into structured JSON log output so logs and OTel spans join on traceId/spanId. **Deliberately NOT
  Log4j MDC/ThreadLocal** (anti-pattern for virtual threads). Mirrors the `TraceInfo` mechanism: a
  per-request `LogContext` in a registry keyed by `Thread.currentThread().threadId()`
  (`org.platformlambda.core.logging` — `LogContext`/`LogContextManager`/`LogContextConfig`), created
  after `startTracing` and removed after `stopTracing` in `WorkerHandler` (same keying the OTel feature
  proves correct; see [[trace-thread-keyed-mono-gotcha]]). The JSON appender runs on the worker thread so
  it looks the context up by thread id in `JsonLogger.getJson` — nothing passed through Log4j; covers
  JsonAppender + CompactAppender. **Opt-in** via optional `app-log-context.yaml` (absent ⇒ one boolean
  check/line, no writes); entries are a reserved `$token` / `${ENV:default}` / hardcoded literal;
  null-valued keys omitted (never `"null"`); token `$service` (not `$route`) for vocabulary consistency.
  `PostOffice.updateContext(key,value)` adds custom keys (logging-only sink, distinct from `annotateTrace`;
  rejects reserved keys; no-op when untraced/off). Same boundary as tracing — no context on boot logs or
  post-return Mono/Flux tails. `LogContextConfig` is an eager Utility-style singleton (no volatile/DCL).
  Shipped with two pre-existing-SonarQube refactors in `WorkerHandler` (extract `invokeFunction` +
  null-guard; split `updateResponse`). 16 tests; platform-core suite 367 green; validated end-to-end in
  `composable-example`. Design spec: `draft-design-specs/application_log_context_design.md` (gitignored).
  <!-- id: application-log-context | created: 2026-06-30 | last_used: 2026-06-30 | uses: 1 | tier: working | origin: 2026-06-30-212955 -->

- **minimalist-kafka producer: subject/version schema resolution (2026-07-01, PR #129).** `simple.kafka.notification`
  is now **subject-driven**, not id-driven: the caller supplies a registry `subject` (+ optional `version`,
  default `latest`) and the producer resolves it to a global schema id + type, then serializes via the
  unchanged `use.schema.id` path. The old `schema-id`/`schema-type` headers were **removed** (no backward
  compat — 4.5.0 unreleased). It's the **single** contract, and it keeps the producer ignorant of Confluent's
  3 naming strategies (the caller names the subject → nothing to derive). Type is derived authoritatively from
  `getSchemaById(id).schemaType()` (also warms the id cache). **Two caches by mutability:** pinned numeric
  versions (immutable) in a dedicated long-TTL (10d)/bounded (3000) `ManagedCache`; `latest` (mutable) shares
  the existing 30m id→schema cache under a `latest/`-namespaced key. `resolve()` on
  `ManagedCacheSchemaRegistryClient` (+ `ResolvedSchema`, `SchemaCodec.resolve` facade); unknown subject /
  registry-down fails the notification loudly (never silent plaintext). **Wire + consumer unchanged** (global
  id on the wire; id-from-wire decode) — only the producer's input moved. Standalone mock + `EmbeddedSchemaRegistry`
  gained `GET /subjects/{subject}/versions/{version}` (latest|int, Confluent shape, 40401/40402) + an in-memory
  `subject→version→id` index rebuilt on boot from the per-id files (seed = drop `<id>.json` + restart; runtime
  = register API); mock got a README. **CSFLE is ready-not-wired** (see [[thread-csfle-field-encryption]]).
  Validated e2e (JSON/Avro/Protobuf) incl. continuous OTel span propagation across both Kafka hops. Design spec:
  `draft-design-specs/kafka_schemaid_from_subject_version_design.md` (gitignored). Supersedes the id-driven
  detail in [[minimalist-kafka-schema-registry]].
  <!-- id: kafka-schemaid-from-subject-version | created: 2026-07-01 | last_used: 2026-07-01 | uses: 1 | tier: working | origin: 2026-07-01-004724 -->

- **Repo-wide OSS dependency security update (2026-07-01, committed on `feature/deprecate-simple-type-matching`).**
  Snyk flagged high-risk dependency CVEs on `system/minimalist-kafka`, `extensions/sync-over-async`,
  `extensions/reactive-postgres` + two SAST findings. Root cause for the dependency CVEs: each Spring-Boot-parented
  module resolves transitive `netty:*`/`jackson-databind` versions from **its own** `netty.version`/`jackson-2-bom.version`
  properties, not from what `platform-core` (or any other dependency) declares — `minimalist-kafka` and
  `sync-over-async` had **no** `netty.version` override at all; `reactive-postgres`'s was stale
  (`4.2.13.Final`, missing 3 more CVEs fixed only in `4.2.15.Final`, confirmed via OSV.dev). Fixed repo-wide,
  not just the 3 flagged modules: `spring-boot-starter-parent` → **`4.1.0`** everywhere except
  `system/rest-spring-3` + `examples/rest-spring-3-example` (the Spring Boot 3.x line) → **`3.5.16`**;
  `netty.version` → **`4.2.15.Final`** in all 26 files that had it, **added** to the 2 that didn't
  (closing the actual root cause); `tomcat.version` → **`11.0.23`** (rest-spring-3 line → `10.1.56`);
  `vertx-core` → **`5.1.3`** (single declaration point, `platform-core/pom.xml`); `xsi:schemaLocation` → `https://`
  (all 28 files, both URL forms in use). Spring Boot 4.1.0's own BOM default already ships
  `jackson-2-bom.version=2.21.4`, so 2 of 3 jackson CVEs were fixed by the parent bump alone with no extra
  override needed. Spring Boot 4.1.0 also silently jumped `lettuce-core` `6.8.2.RELEASE` → `7.5.2.RELEASE`
  (its own managed default, not something either of us set) — verified via a full `sync-over-async` test run
  (32/32 green) specifically because of that major-version surprise. **Not fixable by a pom edit** (tracked,
  not resolved): `wire-runtime-jvm` (Confluent's protobuf dependency) is a discontinued artifact with no
  patched release — only fix is Confluent adopting the renamed `wire-runtime` coordinate upstream; one
  jackson-databind CVE (`@JsonIgnoreProperties` bypass) has no fix yet in the 2.x line either. The two SAST
  findings (AppConfigReader.java deserialization, HttpRouter.java CRLF injection) were verified **empirically**
  against the actual libraries in use — SnakeYAML 2.5's default `Yaml()` rejected a classic RCE gadget
  (`ComposerException: Global tag is not allowed`), Netty 4.2.12's `DefaultHttpHeaders` (under Vert.x's
  `putHeader`) rejected an embedded `\r\n` — both already mitigated by library defaults, no code change.
  Two full-reactor `mvn test` runs (before/after the touch-ups): **BUILD SUCCESS, all 28 modules, zero
  failures**, plus the standalone `examples/pg-example` (excluded from the root aggregator) run separately,
  also green both times.
  <!-- id: snyk-oss-dependency-update-2026-07 | created: 2026-07-01 | last_used: 2026-07-01 | uses: 1 | tier: working | origin: 2026-07-01-215533 -->

- **Schema Registry mock server implementation.** Created `helpers/schema-registry-standalone`, providing a minimalist REST API that mimics the Confluent schema registry (`/subjects/{subject}/versions` and `/schemas/ids/{id}`). It supports both Avro and JSON Schema and serves as an end-to-end demo and testing layer. The worked example `examples/schema-registry-demo` is now curl + zero-dependency `.mjs` scripts (no longer a Maven module). See [[standalone-schema-registry-mock]] and [[minimalist-kafka-schema-registry]].
  <!-- id: schema-registry-mock | created: 2026-06-28 | last_used: 2026-06-29 | uses: 2 | tier: active | origin: 2026-06-28-191114 -->

- **minimalist-kafka Schema Registry support — Confluent serdes as a library (2026-06-29; producer became
  subject/version-driven 2026-07-01, PR #129 — see [[kafka-schemaid-from-subject-version]]).**
  To interoperate with existing client projects, `minimalist-kafka` speaks the Confluent wire format
  (`[magic 0x00][global schema id][payload]`) using **Confluent's own serializers** (`io.confluent:kafka-json-schema-serializer`
  8.2.0 ↔ Kafka 4.2), NOT a reinvented codec — called as a library around the unchanged String/byte[]
  transport so DLQ + trace keep working on raw bytes. **Producer is subject-driven as of PR #129
  (2026-07-01 — was id-driven; see [[kafka-schemaid-from-subject-version]]):** `simple.kafka.notification`
  takes a `subject` (+ optional `version`, default `latest`) header and resolves it to a global id + type;
  the old `schema-id`/`schema-type` headers were removed. Under the hood the serializer still pins the
  resolved id (`use.schema.id` + `auto.register.schemas=false`, only `GET /schemas/ids/{id}` at serialize),
  so it remains agnostic to all 3 Confluent naming strategies (the caller names the subject — nothing to
  derive) and a topic can still carry many record types. **Consumer** opt-in per binding (`schema.enabled` in kafka-flow-adapter.yaml): reads the magic
  id → registered `schemaType` → matching deserializer → hands the flow a Map; decode failure dead-letters the
  raw record. Codec in `org.platformlambda.mini.kafka.schema` (`SchemaCodec`, `ManagedCacheSchemaRegistryClient`
  = in-memory platform `ManagedCache` of schemas by id serving the serde hot path — **positive results only**
  (a not-found id is never cached; Confluent's own `missing.*` caches pinned to 0), default TTL 30m, cleared at
  startup; `SchemaType`).
  Serialize uses `JsonSchemaUtils.envelope(schema, value)` (the pre-registered schema, not a derived one) to
  avoid the kjetland derivation WARN. Built as a **per-type `SchemaSerde` strategy** — `JsonSchemaSerde`,
  `AvroSchemaSerde` (`AvroConversions`: Map⇄GenericRecord, serialize walks the schema so absent fields take
  their defaults), `ProtobufSchemaSerde` (`ProtobufConversions`: Map⇄DynamicMessage reflectively, no codegen;
  proto3 implicit defaults). The codec dispatches by the `schema-type` header on produce / the registered
  type on consume; producer + consumer are type-generic. **Thread-safety (2026-06-30):** the Confluent serdes
  are NOT thread-safe, so `SchemaCodec` is a **factory** — `newEncoder()`/`newDecoder()` mint owner-confined
  serde sets (serializers per producer worker-`instance`, deserializers per consumer), and
  `simple.kafka.notification` is `@KernelThreadRunner` (the serdes use `synchronized` → would pin a VT carrier).
  **Gotcha (fat jar only):** Confluent serdes resolve config classes (the default `context.name.strategy` →
  `NullContextNameStrategy`) via the **thread context classloader**; building/using them off the app-classloader
  thread (e.g. the `@KernelThreadRunner` ForkJoinPool worker) throws `ConfigException: ...could not be found`.
  Fix: `SchemaCodec.withSerdeClassLoader(...)` pins `AbstractKafkaSchemaSerDeConfig`'s classloader as the TCCL
  around build + serialize/decode. Flat-classpath unit tests can't reproduce it.
  **All three serdes (JSON + Avro + Protobuf) done, tested (50 tests, 85% gate), demoed end-to-end, and
  documented** (`EmbeddedSchemaRegistry` test helper + sync-over-async-demo `json/avro/protobuf-topic-1/2`
  paths, all manually validated via continuous-trace telemetry; kafka-flow-adapter guide has the schema
  section). Shipped alongside Kafka-flow-adapter hardening: a flow's own `ttl` is the deadline (no
  `kafka.flow.timeout.ms`); success = HTTP < 400 (2xx/3xx), else retry→DLQ; a failed DLQ write
  drops-with-ERROR + commits (no recovery storm) bounded by `kafka.dlq.timeout.ms`. Builds on
  [[standalone-schema-registry-mock]].
  **Optimization iteration (2026-06-30, PR #127):** swapped the file cache → in-memory `ManagedCache` (above),
  shrank the `simple.kafka.notification` pool 10→5 (kernel-thread frugality, `@KernelThreadRunner`), and per-id'd
  the mock store ([[standalone-schema-registry-mock]]). **Forensic finding: no client-side negative caching
  exists** (decompiled Confluent 8.2.0 — the id→schema path `getSchemaById`→`getSchemaBySubjectAndId` caches
  positives only; `missingIdCache` is producer-side getId-by-content, default TTL 0=off); an earlier "not found
  persists" symptom was the mock loading `schemas.json` only at boot, now fixed by its on-demand per-id store.
  <!-- id: minimalist-kafka-schema-registry | created: 2026-06-29 | last_used: 2026-06-30 | uses: 8 | tier: active | origin: 2026-06-29-010147 -->

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

- [x] (PR open — Eric, 2026-07-01) **Deprecate 'simple type matching' in TaskExecutor → 'simple plugin'
  syntax.** Shipped as **PR [#130](https://github.com/Accenture/mercury-composable/pull/130)**
  (`feature/deprecate-simple-type-matching`, worktree `~/accenture/mercury-composable-2`). Full detail in
  the 2026-07-01-172822 session log. 4 commits: `d5761c38` — event-script-engine
  (SimpleTypeMatchingConverter, CompileFlows, TaskExecutor cleanup, `ne` plugin, TypeConversionUtils +
  DataMappingHelper bug fixes, 115 tests green); `495d2252` — minigraph CompileGraph startup gate +
  CompiledGraphs cache (55 tests green); `56ad6fb8` — GraphCommandService interactive validation +
  deprecation notice + help doc updates; `d125d4a3` — memory. Open to follow-up if review surfaces changes.
  <!-- id: thread-deprecate-simple-type-matching | created: 2026-07-01 | last_used: 2026-07-01 | uses: 3 | tier: working | origin: 2026-07-01-172822 -->

- [x] (completed — Eric, 2026-07-01) **Repo-wide OSS dependency security update (Snyk-driven).** Committed
  to `feature/deprecate-simple-type-matching` alongside PR #130's changes (Eric: "regular security
  vulnerability OSS update... we are good"). Full detail in the Key Decision
  [[snyk-oss-dependency-update-2026-07]] and the 2026-07-01-215533 session log. `spring-boot-starter-parent`
  → 4.1.0 (rest-spring-3 line → 3.5.16), `netty.version` → 4.2.15.Final (added where missing — the actual
  root cause), `tomcat.version` → 11.0.23 (rest-spring-3 line → 10.1.56), `vertx-core` → 5.1.3,
  `xsi:schemaLocation` → https. Two full-reactor `mvn test` runs, both BUILD SUCCESS, 28 modules, zero
  failures. `wire-runtime-jvm` (no supported fix, Confluent's discontinued artifact) and one jackson-databind
  CVE remain open — tracked as upstream-blocked, not resolvable here.
  <!-- id: thread-snyk-oss-dependency-update | created: 2026-07-01 | last_used: 2026-07-01 | uses: 1 | tier: working | origin: 2026-07-01-215533 -->

- [x] (completed — Eric, 2026-06-30) **Application log context feature.** Designed + implemented +
  documented + shipped on **PR #128** (`feature/application-log-context`). Full detail in the Key
  Decision [[application-log-context]] and the 2026-06-30-212955 session log. Manual end-to-end
  validation by Eric in `composable-example` passed (context block + custom `user:demo` key, logs/spans
  correlated). Open to follow-up if review surfaces changes.
  <!-- id: thread-application-log-context | created: 2026-06-30 | last_used: 2026-06-30 | uses: 1 | tier: active | origin: 2026-06-30-212955 -->

- [x] **Upgraded agent-memory v4.27.0 → v4.28.0** (Mode B, by Claude Code from the tool checkout). Single,
  additive rung — **co-author convention cleanup** (refines v4.27.0, from this repo's PR #126 finding):
  the `Co-Authored-By` self-identification now uses the **stable agent name** (e.g. `Claude Code`) — the
  actual AI collaborator, not a model-version string that churns each release — and on a **squash-merge**
  collapse to a **single** trailer (GitHub appends a consolidated one after `---------`; trim the inline
  repeats). Applied here: additively refined the `AGENTS.md` commit-trailer note + re-copied
  `.github/pull_request_template.md` (footer comment updated), stamped `.agent/version.md` → 4.28.0.
  Fetched + switched to `main` + `pull --ff-only` first (local was on the merged `chore/sonar-ide-cleanup`
  with a stale main). No memory-shape/skill/adapter change. `memory-lint`: **0 errors**. Working tree
  **uncommitted** — review + commit at the mercury team's discretion.
  <!-- id: agent-memory-upgrade-v4280 | created: 2026-06-30 | last_used: 2026-06-30 | uses: 1 | tier: working | origin: 2026-06-30-055333 -->

- [x] **Upgraded agent-memory v4.26.1 → v4.27.0** (Mode B, by Claude Code from the tool checkout). Single,
  additive rung — **standardized PR descriptions (What / Why)**: every enabled repo now ships a
  `.github/pull_request_template.md` whose body leads with **What** + **Why** (intent, not a restatement)
  and closes with a self-identifying `Co-Authored-By:` footer (extends the commit/session-log authorship
  convention to the PR altitude; advisory, never a gate). Applied here: installed the template (mercury had
  none), **additively merged** the convention + a checklist line into `AGENTS.md` (customizations preserved),
  stamped `.agent/version.md` → 4.27.0. No memory-shape/skill/adapter change. Post-upgrade `memory-lint`:
  **0 errors, 0 warnings**. Working tree **uncommitted** — review + commit at the mercury team's discretion.
  <!-- id: agent-memory-upgrade-v4270 | created: 2026-06-29 | last_used: 2026-06-29 | uses: 1 | tier: working | origin: 2026-06-29-231747 -->

- [x] **Upgraded agent-memory v4.25.0 → v4.26.1** (Mode B, by Claude Code from the tool checkout) — a final
  validation round. **4.26.0** adds `refresh-metadata` (a 7th built-in: recompute `last_used`/`uses`/`tier`
  from the session log deterministically — REVIEW.md steps 2–3, the metadata pass agents skip) + a
  `memory-lint` `[stale-metadata]` advisory. **4.26.1** refines it: a **pinned `- [ ]` open thread's tier is
  left alone** (pinned-ness protects it, not the label). Re-synced `memory-lint` (check 9 + refinement),
  `REVIEW.md`, `DECAY.md`; copied the new `refresh-metadata` skill; adapters synced (7 skills → 42); stamped
  4.26.1. **Validation result:** post-upgrade `memory-lint` flagged just **1** real drift
  (`agent-memory-upgrade-v4250` tier active→working) — the **5 pinned threads from the prior sanity check no
  longer flag** (the v4.26.1 refinement working). Ran `refresh-metadata` → it re-tiered the one real fact AND
  refreshed pinned `thread-redis-kafka-rpc`'s `uses 3→6` **without touching its `tier: working`** (the
  refinement's exact intent). Final `memory-lint`: **0 errors, 0 warnings**; py↔node parity confirmed here.
  Working tree **uncommitted** — review + commit at the mercury team's discretion.
  <!-- id: agent-memory-upgrade-v4261 | created: 2026-06-28 | last_used: 2026-06-28 | uses: 1 | tier: archive-candidate | origin: 2026-06-28-182125 -->

- [x] **Upgraded agent-memory v4.23.1 → v4.25.0** (Mode B, by Claude Code from the tool checkout).
  Three rungs: **4.23.2** (AGENTS.md long-session context-hygiene block), **4.24.0** (decay-policy retune +
  a `memory-lint` review-cadence/size advisory), **4.25.0** (`archive-fact` — a 6th built-in: a deterministic,
  safe archive-move that reads continuity into memory and writes once, so the truncate-before-read trap can't
  recur). Re-synced AGENTS.md, REVIEW.md, `.agent/schema.md`; merged `decay-policy.md` additively
  (`continuity_max_facts: 30` added; `continuity_max_lines` 300→600; `verify_invariants_every` 20→40 — all
  stock here, no custom values clobbered); copied the `memory-lint` + new `archive-fact` skills; re-synced
  adapters (6 skills → 36); stamped `.agent/version.md` → 4.25.0.
  **⚠️ A review is now due (lint says so):** `memory-lint` reports `[review-overdue]` (21 sessions since the
  last review ≥ review_every 10) and `[continuity-bloat]` (41 facts > continuity_max_facts 30), plus 11
  per-fact `[overdue]` advisories — this repo's own decay backlog. **Run the `REVIEW.md` ritual** (it can now
  use `archive-fact` to perform the moves safely). Left for the mercury team to curate — the faded facts are
  mercury's domain content; agent-memory only flags, never picks. 0 lint **errors**.
  <!-- id: agent-memory-upgrade-v4250 | created: 2026-06-28 | last_used: 2026-06-28 | uses: 1 | tier: archive-candidate | origin: 2026-06-28-173142 -->

- [x] (completed — Eric, 2026-06-28) **Schema Registry feature.** Implemented `helpers/schema-registry-standalone`, a minimalist Confluent-compatible mock server (Avro and JSON Schema). Created `examples/schema-registry-demo` to showcase usage. Adds Apache 2.0 license preamble. (Corrected + reworked 2026-06-29 — see [[standalone-schema-registry-mock]].)
  <!-- id: thread-schema-registry | created: 2026-06-28 | last_used: 2026-06-28 | uses: 1 | tier: active | origin: 2026-06-28-191114 -->
- [x] (completed — Eric, 2026-06-29) **minimalist-kafka Schema Registry serdes — feature COMPLETE & pushed.**
  All three serde phases done, tested (49 tests, 85% gate), demoed end-to-end, and **pushed** to
  `feature/sync-over-async` (PR #124, `7e2fe746..5ffb7dc7`): JSON Schema, Avro (`1bc2731e`/`6fc8c56c`),
  Protobuf (`bdab28f3`/`bc9be976`); Avro + Protobuf each validated via Eric's multi-terminal run
  (continuous-trace telemetry). The kafka-flow-adapter guide now documents the Schema Registry integration
  (`5ffb7dc7`). Closed — the feature is ready for PR review/merge. See [[minimalist-kafka-schema-registry]].
  <!-- id: thread-schema-registry-avro-protobuf | created: 2026-06-29 | last_used: 2026-06-29 | uses: 4 | tier: active | origin: 2026-06-29-010147 -->
- [x] (completed — Eric, 2026-07-01, PR #129) **minimalist-kafka: resolve `schemaId` from subject/version
  for `simple.kafka.notification`.** Shipped as the subject/version producer contract — full detail in the
  Key Decision [[kafka-schemaid-from-subject-version]] and the 2026-07-01-004724 session log. Resolved to
  **subject/version only** (no id-driven fallback; naming strategies dropped), two-tier cache, new mock
  `GET /subjects/{subject}/versions/{version}` endpoint. Validated e2e.
  <!-- id: thread-kafka-schemaid-from-name | created: 2026-06-30 | last_used: 2026-07-01 | uses: 2 | tier: working | origin: 2026-06-30-212955 -->

- [ ] (planned — Eric, 2026-07-01; **next session**) **minimalist-kafka: wire Confluent CSFLE (Client-Side
  Field Level Encryption).** The current installation uses CSFLE, but minimalist-kafka's serdes are
  configured **without encryption rule executors or a KMS**, so a schema carrying CSFLE encryption rules
  would serialize in **plaintext** (silent — the rules aren't enforced). The subject/version work
  ([[kafka-schemaid-from-subject-version]]) is the right, subject-centric foundation; this thread wires the
  encryption. **Investigate/verify first** (Confluent serdes 8.2.0): whether the encryption ruleSet executes
  under the current `use.schema.id` + `JsonSchemaUtils.envelope` mechanism, or whether serialize must move to
  subject + `use.latest.with.metadata`; and whether `getSchemaById(id)` returns the ruleSet. **Scope:** pass
  `rule.executors` (`FieldEncryptionExecutor`) + KMS config through `SchemaCodec`/serde config on **both**
  serializer and deserializer (decrypt is symmetric); a CSFLE **test environment** (real Confluent registry +
  real/local KMS — the standalone mock has no ruleSet/KMS and cannot exercise CSFLE). Until it lands, the
  kafka-flow-adapter guide's caveat stands: do not produce to CSFLE-protected topics. → relates
  [[minimalist-kafka-schema-registry]].
  <!-- id: thread-csfle-field-encryption | created: 2026-07-01 | last_used: 2026-07-01 | uses: 1 | tier: working | origin: 2026-07-01-004724 -->
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

- **Standalone Schema Registry mock for local development (Eric, 2026-06-28; corrected 2026-06-29).**
  `helpers/schema-registry-standalone` emulates the Confluent Schema Registry HTTP API (Avro + JSON Schema),
  a `http://localhost:8081` endpoint for `KafkaAvroSerializer`/`KafkaJsonSchemaSerializer` to register
  (`POST /subjects/{subject}/versions`) and fetch (`GET /schemas/ids/{id}`). **platform-core only** (built-in
  reactive HTTP server + REST automation, no `rest-spring`); each endpoint is wired in `rest.yaml` directly to
  a function taking `AsyncHttpRequest`. Returns faithful Confluent error bodies `{error_code,message}`
  (40403/40401/42201). Store is **configurable** via `schema.registry.data.store` (default transient
  `/tmp/schema-registry`, `-D`-overridable for a durable dir). **Persists one `<id>.json` per schema** (not a
  single `schemas.json`; changed 2026-06-30, PR #127) and *loads* on boot AND **on demand** — a file dropped
  into the store dir while the server runs is served on the next GET, no restart. Never wipes, so ids stay
  stable — the deliberate inverse of the redis/kafka helpers. Tests hit the real HTTP
  endpoints via `async.http.request`. Documented in `docs/guides/schema-registry-mock.md`. See [[minimalist-kafka-schema-registry]].
  <!-- id: standalone-schema-registry-mock | created: 2026-06-28 | last_used: 2026-06-30 | uses: 5 | tier: active -->
