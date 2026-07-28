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
- **last_session:** 2026-07-28 | agent: Claude Code (2026-07-28-005814)
- **last_review:** 2026-07-27 | through 2026-07-27-214357.md
- **last_invariant_check:** 2026-07-27 | 2026-07-27-215011.md (all 15 confirmed by Eric — one-by-one walkthrough with live-tree evidence; thread-reverify-invariants-2026q2 closed)

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
- Docs site: **Material for MkDocs** (switched from readthedocs 2026-07-20, mirroring the Rust
  port's mkdocs.yml). Mermaid renders natively via the `pymdownx.superfences` custom fence — all
  diagrams are mermaid blocks in the markdown (the scanned PNGs in `docs/guides/diagrams/` were
  removed). Reference pages (configuration, flow-schema, annotations, event-envelope) present
  keys/fields/methods as per-entry sections (heading + Type/Default mini-table + prose), not wide
  tables — same pattern as the Rust port, so the two sites stay structurally aligned. CI installs
  `mkdocs-material` in `.github/workflows/docs.yml`.
  <!-- id: docs-site-material-theme | created: 2026-07-20 | last_used: 2026-07-21 | uses: 2 | tier: archive-candidate | origin: 2026-07-20-222709 -->

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

- **Release 4.8.3 — SHIPPED 2026-07-13 (tag `v4.8.3` on merge commit `6696a76f`; PRs #168-#175).**
  Security patch + hardening release, validated by the field pipeline (Snyk + Sonar PASSED) BEFORE
  tagging — the deliberate deferred-tag flow worked as designed. Contents: (1) **Snyk remediation**
  (#168 — httpcore5 5.4.3 via SB property override, log4j-api 2.26.1, reactor-netty 1.3.6);
  (2) **trace-continuity regression guards** (#169 HTTP app-to-app, #175 declarative flow task incl.
  the sink-after-response shape — BOTH verified passing on v4.7.1 too, so the field's declarative
  report was environmental, not a framework defect); (3) **observability + REST per-entry header docs**
  (#169/#172); (4) **ScheduleAdminTest + SonarQube touch-up** (#170/#173 — file-existence ≠ readiness;
  HelloException → HelloExceptionHandler; S5443 suppressions per ProfileStore precedent);
  (5) **kafka.health** (#174 — `KafkaConsumer.listTopics` Metadata probe needs NO ACL: brokers filter
  by Topic Describe rather than reject, so it degrades to an empty-but-successful response under
  locked-down principals; startup grace returns placeholder-healthy while the client warms up on a
  background virtual thread; `kafka.health.timeout` 5s / `kafka.health.startup.grace` 30s).
  **Durable lessons:** field screenshots' naming conventions are client-identifiable — paraphrase
  generically in fixtures/commits/PRs; a flow needs ≥1 `end` task (a `sink` only terminates a side
  branch). See [[release-4-8-2-shipped]] for the prior cycle.
  <!-- id: release-4-8-3-shipped | created: 2026-07-13 | last_used: 2026-07-24 | uses: 10 | tier: active | origin: 2026-07-13-170933 -->

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
  <!-- id: field-trace-propagation-4-6-3-diagnosis | created: 2026-07-13 | last_used: 2026-07-24 | uses: 5 | tier: active | origin: 2026-07-13-142021 -->

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
  <!-- id: release-4-8-2-shipped | created: 2026-07-12 | last_used: 2026-07-24 | uses: 5 | tier: active | origin: 2026-07-13-014037 -->

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
  <!-- id: release-4-8-1-shipped | created: 2026-07-11 | last_used: 2026-07-27 | uses: 8 | tier: active | origin: 2026-07-12-002326 -->

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
  <!-- id: release-4-8-0-shipped | created: 2026-07-10 | last_used: 2026-07-24 | uses: 8 | tier: active | origin: 2026-07-11-031930 -->

- **ManagedCache eviction: Java accepts + documents non-determinism; Rust is strict LRU —
  a deliberate cross-engine asymmetry (Eric, 2026-07-27).** Java's `ManagedCache` keeps
  Caffeine (3.2.4) W-TinyLFU: under `maxItems` pressure eviction is approximate and
  non-deterministic (frequency-based admission + anti-HashDoS jitter admitting 1/128
  losing candidates at random + lossy read buffers; no policy knob exists in the builder).
  Javadoc on both `createCache` overloads + CHANGELOG state it; callers must never rely on
  which entry survives nor assert eviction victims. The Rust port's ManagedCache is moka
  `EvictionPolicy::lru()` (deterministic; increment 71, Rust PR #185) per Eric's
  "deterministic eviction" ruling there. Eviction is internal state, NOT a presentation
  surface — [[conv-telemetry-presentation-parity]] does not require closing this gap.
  **"Frequency aging" is NOT a determinism remediation** (investigated vs the pinned jar:
  `FrequencySketch.reset()` already ages counters; aging fixes stale popularity, not
  reproducibility). Revisit trigger: the first consumer that truly runs at capacity
  (schema-registry caches are the candidate); today every caller uses the 2-arg form
  (default maxItems 2000, nothing close). Full handoff + options record:
  the Rust repo's docs/design/managed-cache-port.md.
  <!-- id: managed-cache-eviction-determinism | created: 2026-07-28 | last_used: 2026-07-28 | uses: 1 | tier: working | origin: 2026-07-28-005814 -->

- **Application log context is ON by default (2026-07-22, Eric via leadership request; branch
  `feature/lambda-example-interop-echo`, ships in the next release).** platform-core carries a built-in
  `default-log-context.yaml` (cid/traceId/tracePath/spanId/parentSpanId/service/timestamp); when json/compact
  logging is active, every traced function's log line gets the `context` block with zero setup. Override =
  the app's own `app-log-context.yaml` (replaces the template entirely); opt-out = `app.log.context=false`
  (default true). **Design note:** the built-in uses a DISTINCT filename + explicit code fallback in
  `LogContextConfig.loadConfigFile()` (the `default-rest.yaml` precedent) — never ship a same-named resource
  in a library jar, because classpath shadowing across jars is classloader-order-dependent. Companion fixes
  in the same branch: eager `LogContextConfig` init in `AppStarter.reConfigLogger` (kills the "Recursive call
  to appender" warning) and RPC `round_trip` telemetry now carrying span_id/parent_span_id (inbox family).
  <!-- id: log-context-on-by-default | created: 2026-07-22 | last_used: 2026-07-23 | uses: 3 | tier: archive-candidate | origin: 2026-07-22-234845 -->

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

- **Registration metadata is a cross-language contract; carriers are per-language idioms.
  (ADR-0009)** One canonical model + fixed semantics for @PreLoad and family (boot-time
  envInstances resolution; OptionalService OR/!/= grammar; order-free marker stacking; one
  conflict policy — explicit > declarative, duplicates WARN + last-wins; extension-point
  naming: explicit positional name or same-name derivation from idiomatic declarations;
  plugins = flow vocabulary never gated, features honor gating; discover → register →
  override → resolve → validate → route table; loud-failure discovery; misuse is a tested
  error surface). Spec: docs/guides/registration-metadata-contract.md. Conformance:
  golden vectors shared verbatim (registration-vectors/{core,plugin,feature}.json) — the
  wire-format golden-vector method applied to the declaration surface. New ports pass the
  three vector suites before their declaration surface is done.
  <!-- id: registration-metadata-contract | created: 2026-07-26 | last_used: 2026-07-27 | uses: 2 | tier: active | origin: 2026-07-25-235904 -->

- **Telemetry/log presentation parity across language engines is a field requirement (Eric,
  2026-07-23).** Rationale: even after the Rust engine is accepted into the field, installations
  will be POLYGLOT for a long time — DevSecOps teams see both engines' telemetry and logs in one
  aggregation, and any presentation difference (record shapes, span topology, context-block
  gating, header hygiene) is a support burden they will flag. Operating rule: the Java engine is
  the REFERENCE implementation; a same-language interop run (java-to-java vs rust-to-rust) must
  be an exact structural replica after normalizing volatile fields — then cross-language runs are
  symmetric by construction. Reference signature procedure established 2026-07-23
  (normalized record set: service names, symbolic parent edges, round_trip vs exec-only kind,
  paths, one-record-per-span, no dangling parents, my_*-free response headers, context-gating).
  **Scope extension (Eric, 2026-07-23): the Event Script surface is part of the cross-engine
  contract** — flows are engine-portable YAML, so any new built-in simple plugin ships in
  lock-step on both engines (with closely matching error messages — presentation parity extends
  to error text), or flows stop being portable. Precedent: the #220 collection plugins mirrored
  into the Rust v4.10.2.
  <!-- id: conv-telemetry-presentation-parity | created: 2026-07-23 | last_used: 2026-07-28 | uses: 7 | tier: active | origin: 2026-07-23-145132 -->

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

- [x] (feature in flight — 2026-07-26; CLOSED 2026-07-26 — BOTH PRs MERGED same day:
  Java [PR #236](https://github.com/Accenture/mercury-composable/pull/236) squash
  `6ed481e1`, CI 6m51s; Rust PR #183 — the shared family key
  `worker.instances.actuator.services` is now live on both engines. Rust v4.10.6 RELEASED
  same day on the back of the merged arc — tag on merge `9732799e`, published; Eric
  confirmed the pretty-print parity also improved the MiniGraph /api/graph output, live
  proof of the single-render-path design.) **Ops-tunable worker instances, both engines
  (Eric's /info/routes review round).** Context: the Rust typed-AsyncHttpRequest arc shipped
  `/info/routes` (Rust branch `feature/typed-async-http-request`, unpushed), Eric reviewed
  the live output and ruled: actuators → 5 instances, Rust demo `http.request.filter` → 20,
  `event.api.auth` → 30 (real-world = OAuth2 bearer-token verification, I/O-bound), and the
  ops-tunability principle: **declared counts are rules of thumb; operations teams tune in
  QA/Perf via config before promoting to Production.** Java commit `6f0f03df` on
  `feature/ops-tunable-instances` (NOT pushed): ActuatorServices 30 → 5 + NEW
  `worker.instances.actuator.services` (one knob, 7 aliases); lambda-example event.api.auth
  10 → 30 + `worker.instances.event.api.auth`; **doc bug fixed** — configuration-reference
  claimed `worker.instances.<route>` overrides ANY route, but code (AppStarter.
  getInstancesFromEnv + Platform.register) only honors it where `@PreLoad` declares
  `envInstances` (any-route override = `yaml.preload.override`); added missing
  `worker.instances.http.flow.adapter` section. Proof: platform-core 424 green WITH the
  actuator key active (=8) + `actuatorFamilySharesOneEnvInstanceKey`; lambda-example 14.
  **Cross-engine key parity decision: the Rust port's five per-endpoint actuator services
  share the SAME key name `worker.instances.actuator.services`** (its actuator.services
  route is unported; one runbook line tunes both engines). Rust half in flight in the agent
  session (same branch/commit as the typed-request arc). Close when both PRs merge.
  <!-- id: thread-ops-tunable-instances | created: 2026-07-26 | last_used: 2026-07-27 | uses: 1 | tier: active | origin: 2026-07-27-005415 -->

- [x] (feature in flight — 2026-07-25; ARC COMPLETE 2026-07-26 — P1 AND P2 merged both
  repos) **Annotation → macro consistency arc (Eric's initiative; design RATIFIED — see
  session 2026-07-25-235904 + the full spec at
  draft-design-specs/annotation-macro-interop-design.md).** **P2 MERGED: Java PR #235
  (squash `84c4957f`) + Rust PR #182 — delivering D4 (yaml.preload.override ported with
  Java's exact merge/application semantics, 7-scenario suite) and D5 (the Registration
  Metadata Contract: docs/guides/registration-metadata-contract.md + ADR-0009/ADR-0008
  pair + golden vectors registration-vectors/{core,plugin,feature}.json shared VERBATIM +
  one conformance suite per kind in both engines — see [[registration-metadata-contract]]).
  The P2 round also carried: the Rust invariant re-verification (all five confirmed, code
  drifts fixed, cadence reset), the AI-companion-test.md move to docs/test-reports
  (published in nav), and Eric's string-semantics ruling (Unicode scalar values in all
  ports; Java's UTF-16 = documented JVM legacy, bounded to supplementary-plane chars —
  the F20 UTF-16 retrofit REVERTED in Rust with emoji+CJK evidence; anti-re-retrofit
  guard recorded). Ratified dispositions: D3b (plugin gating) + D6 (executionHint)
  deferred by design; tests/ui fixtures = test resources, no license headers (Eric,
  2026-07-26). The full ratified scope is DELIVERED; future ports (Python/Node) start
  from the contract page + vectors. No release scheduled — rides the next patch.** Goals: (1) Rust macro surface
  reads like the Java annotation surface (decoupled; runtime classpath scan vs link-time
  inventory is mechanics, not style); (2) the Rust port becomes the best-practice template
  for future Python/Node ports. Verified ground truth: Java dogfoods its extension points
  (47 @SimplePlugin built-ins, 2 @FetchFeature built-ins) while Rust hard-codes all of them
  with zero production macro usage; #[fetch_feature] can't accept optional_service; conflict
  semantics diverge AND Java itself is inconsistent (Platform.register javadoc claims
  throws-on-duplicate but warn+reloads; PlaygroundLoader replaces silently). Ratified:
  D1 convert Rust built-ins to declarative macros (explicit names where camelCase
  derivation mismatches/keyword-collides; exact Java error-message parity); D2 ONE conflict
  policy both engines (explicit register() > declarative; duplicate = WARN both sources +
  last-wins; Java lock-step javadoc/log fixes); D3a fetch_feature + stacked
  #[optional_service] (Java parity); D3b DEFERRED with Eric's principle: **plugins are
  Event Script capabilities (flow vocabulary) — never conditionally on/off**; D4 (P2) port
  yaml.preload.override to Rust; D5 (P2) registration-metadata contract spec page (real
  schema; golden-JSON conformance per the wire-format precedent) + ADR pair; D6 DEFERRED
  executionHint:blocking. Kafka annotations (@CloudConnector/@CloudService) ride the future
  minimalist-kafka/sync-over-async port. Bonus fixes: two stale Rust docs (syntax.md
  single-route claim; api-overview public/private claim). Branches:
  feature/annotation-macro-consistency both repos (Java = javadoc+WARN lock-step; Rust =
  the refinement round, delegated). **P1 MERGED 2026-07-25 (Java PR #234 squash
  `265f295d`, CI 6m37s; Rust PR #181 merge, CI 2m26s incl. the first trybuild run —
  .stderr files matched CI's stable toolchain first try).** Landed beyond the original
  ratification (three mid-round refinements by Eric): positional #[simple_plugin("name")]
  grammar (46 built-ins flipped; name= alias kept), order-insensitive marker stacking
  (#[zero_tracing]/#[event_interceptor] as real proc-macros via the optional_service
  self-reattachment pattern — "Java does not require stack order"), and the trybuild
  compile-fail guards upgraded from P2 into the round (11 fixtures, 3 tests/ui suites in
  the runtime crates; Rust workspace 265 tests). **Remaining = P2: D4 port
  yaml.preload.override to Rust; D5 registration-metadata contract spec page (real
  schema; golden-JSON conformance per the wire-format precedent) + ADR pair
  (Java ADR-0009 / Rust ADR-0008).** No release scheduled yet — rides a future patch.
  <!-- id: thread-annotation-macro-consistency | created: 2026-07-25 | last_used: 2026-07-27 | uses: 2 | tier: active | origin: 2026-07-25-235904 -->

- [x] (field support — 2026-07-25; CLOSED 2026-07-26 — **field rescan of v4.10.6 PASSED
  the Sonar quality gate with a perfect Overall-Code score**: 0 vulnerabilities / 0 bugs /
  0 code smells / 0 hotspots, coverage 80.5% ≥ the 60% requirement — Eric shared the field
  dashboard 2026-07-26; arc complete: rejection → fix #231 → release v4.10.6 #232 → clean
  rescan, the [[thread-sonar-4-9-1-field-rejection]] shape) **v4.10.4 failed
  the field Sonar quality gate — 5 findings, fix reviewed + verified, MERGED as
  [PR #231](https://github.com/Accenture/mercury-composable/pull/231) (merge commit
  `c7d05d83`), then released as
  [v4.10.6](https://github.com/Accenture/mercury-composable/releases/tag/v4.10.6) via
  [PR #232](https://github.com/Accenture/mercury-composable/pull/232) (chore/release,
  merge commit `2a940250`, tag pushed on that commit, CI green both PRs).** GitHub
  Copilot authored the fix (commit `ac36ec4f`); Claude Code independently reviewed all 5
  diffs line-by-line, verified, prepared the release (32-pom + CLAUDE.md/GEMINI.md/
  memory/instructions.md version sweep, CHANGELOG entry following the v4.9.2
  pure-Sonar-remediation precedent), and drafted the GitHub release notes; Eric gated
  every PR-open/merge/tag/publish step. Findings: 2× S125 (commented-out code — both were
  prose comments ending in a stray semicolon, which Sonar's heuristic mistakes for
  commented-out code, in `HttpRouter.java` and `KafkaFlowConsumer.java`) + 3× S3776
  (Cognitive Complexity >15 in `InboxBase.recordTrace`, `AsyncHttpResponse.handleEvent`,
  `AsyncHttpClient.updateHttpHeaders` — fixed via helper-method extraction / guard-clause
  early returns, confirmed behavior-preserving). **Verification:** full reactor
  `mvn clean install` (29 modules) BUILD SUCCESS on both the fix and the bumped version;
  live Java-to-Java Event-over-HTTP interop drive (composable-example ⇄ lambda-example,
  both declarative + programmatic patterns) as a targeted regression test of the three
  refactored trace/cid-propagation classes — 17 span records, zero duplicates, zero
  dangling `parent_span_id`s, correct cross-process span parenting in both patterns
  (programmatic pattern's non-adoption of a foreign span confirmed consistent with the I2
  fix behavior in [[thread-event-envelope-interop]]). **Remaining:** close this thread
  when the field team confirms the rescan of v4.10.6 passes the gate (precedent:
  [[thread-sonar-4-9-1-field-rejection]], which followed the identical shape).
  <!-- id: thread-sonar-4-10-4-field-rejection | created: 2026-07-25 | last_used: 2026-07-27 | uses: 2 | tier: active | origin: 2026-07-25-005125 -->

- [x] (release in flight — 2026-07-24; CLOSED same day) **v4.10.5 security patch SHIPPED
  AND PUBLISHED in lock-step (both repos) — react-router CVE remediation.** Dependabot #16
  on the Rust repo (react-router 7.18.1 RSC Mode CSRF Bypass, follow-up to CVE-2026-22030,
  patched 8.3.0; the Java twin webapp carried the identical exposure) — Eric CONFIRMED the
  alert closed on release. Remediation: react-router-dom is RETIRED upstream at 7.18.1
  (pins the vulnerable react-router exactly — why dependabot could not auto-fix); v8
  consolidated into the single react-router package, so both webapps now depend on
  `react-router ^8.3.0` directly with import specifiers updated in 4 files (six stable
  declarative exports; React 19.2.8 already satisfies the >= 19.2.7 peer). Validation both
  webapps: npm audit 0, lockfile registry+integrity clean, 124 tests, resources/public
  rebuilt via npm run release (Eric's instruction). Java: PR #230, tag `v4.10.5` on squash
  commit `4c82eae0` (verified before tagging), CI 7m14s + reactor 5:33. Rust: PR #180,
  tag on merge `5ae307c2`, CI green (260 tests). Operational notes: a transient
  GitHub web-UI 500 delayed PR creation (API was healthy; status page lagged); EMU
  accounts CANNOT create PRs via API either (GraphQL + REST both 403) — web UI is the only
  PR path. Sixth lock-step release of the 4.10 arc.
  <!-- id: thread-release-4-10-5 | created: 2026-07-24 | last_used: 2026-07-24 | uses: 1 | tier: archive-candidate | origin: 2026-07-24-154543 -->

- [x] (release in flight — 2026-07-24; CLOSED same day) **v4.10.4 SHIPPED AND PUBLISHED in
  lock-step (both repos) — standards-first traceparent carrier + interop header hygiene.**
  Java: PR #229, tag `v4.10.4` on squash commit `0125c17b` (verified before tagging), CI
  7m29s green + local reactor 5:06 on the release version. Rust: PR #179, tag on merge
  `03424582`, CI green (260 tests). Both PRs' content merged earlier as Java #228 (squash
  `fcd4fbc1`: report + envelope scrub + resolution + standards position + precedence flip)
  and Rust #178. Both releases validated by the ce_traceparent four-way drive — all eight
  echoes identical. PR-branch lesson retained: Eric created the fix PRs from the stacked
  report branches while the fix commits sat on a second branch — resolved by
  fast-forwarding the PR branches (strict descendants); VERIFY which branch a PR points at
  before assuming pushed commits appear in it. Fifth lock-step release of the 4.10 arc:
  4.10.0 interop → 4.10.1 presentation parity → 4.10.2 boundary demarcation → 4.10.3 field
  roll-up → 4.10.4 standards-first traceparent + hygiene.
  <!-- id: thread-release-4-10-4 | created: 2026-07-24 | last_used: 2026-07-24 | uses: 1 | tier: archive-candidate | origin: 2026-07-24-154543 -->

- [x] (feature in flight — 2026-07-24; CLOSED same day — RELEASED in v4.10.4) **Configurable traceparent header name (field request).**
  Field wants `http.traceparent.header` / `kafka.traceparent.header` /
  `secondary.kafka.traceparent.header` (secondary → primary fallback) alongside the existing
  trace-id/correlation-id families. Assessment AGREED (design-clean, unlike the rejected
  `legacy.trace.id` conflation flag: renames the CARRIER, not the semantics — full W3C context
  crosses a header-stripping gateway, so cross-app span parenting survives; fixes the known
  limitation in [[thread-field-trace-propagation-4-6-3]]). Eric ratified the 3 design points
  2026-07-24: (2) inbound = configured name wins when well-formed (sidecar-injected standard
  traceparent cannot override the peer's context), standard header as fallback; (3) outbound =
  stamp BOTH names (the "alongside" precedent); (4) per-entry overrides included
  (rest.yaml + kafka-flow-adapter.yaml `traceparent.header`) for a symmetric surface. Default
  stays `traceparent`; docs carry the standards-deviation warning (renamed carrier invisible to
  OTel/Istio/APM). Java reference IMPLEMENTED on `feature/configurable-traceparent-header`:
  platform-core (HttpRouter class-load static + per-entry, dual stamp in AsyncHttpClient +
  EventEmitter setTraceHeaders helper), minimalist-kafka (notification dual stamp +
  isPropagatableHeader exclusion, consumer custom-first parse, per-binding key), twin-kafka
  (secondaryOrGlobal). Tests: platform-core 420 green (5 new; suite runs WITH the global custom
  name active = additive proof), minimalist-kafka 101 (5 new incl. wire-level dual-stamp e2e),
  twin-kafka 9 (wire-level secondary→primary fallback assert). Docs: config-reference 3 new
  keys, observability table + "renamed traceparent beats conflation" note, rest-grammar +
  rest-automation.json + ai-agent-guide, minimalist/twin-kafka guides, reserved-names,
  CHANGELOG Unreleased. **BOTH PRs MERGED 2026-07-24 (Java #227 merge `47e948ef`, CI 7m40s;
  Rust #177 merge `e99013cb`, 257 tests) and the pre-release ce_traceparent interop drive
  PASSED in full** (gateway simulation: W3C value supplied ONLY under the custom name; edge
  adoption + cross-language span parenting both directions; wire-level dual stamp both
  engines; report round appended to docs/test-reports/event-over-http-interop.md in both
  repos). **Findings queued for a follow-up hygiene round (pre-existing, not the feature):**
  both programmatic demo tasks transport their injected my_* view (accidental-copy
  anti-pattern, request side); the engines sanitize different subsets at the /api/event door
  (Java delivers my_correlation_id / strips route+trace keys, Rust the inverse + strips
  x-event-api); Rust wire nits (duplicate trace headers, x-correlation-id on /api/event,
  missing traceparent-name startup log). **Hygiene round COMPLETE 2026-07-24 (Eric directed;
  ships in v4.10.4):** both engines scrub the 5 engine keys from the delivered ENVELOPE view
  (non-interceptor; legacy my_correlation_id honored-then-scrubbed; interceptors keep raw
  fidelity — the Rust fix also RESTORED interceptor fidelity its partial scrub violated);
  both demos forward business headers only; Rust wire aligned to the Java reference (single
  stamps, no x-correlation-id on the event-over-HTTP leg, accept + x-small-payload-as-bytes,
  startup header-name log lines); x-ttl ingress alignment (Java represents the route timeout
  as the request's x-ttl header, caller-sent wins — AsyncHttpRequest.setTimeoutSeconds; Rust
  ingress now mirrors). **Final matrix: ALL EIGHT ECHOES IDENTICAL after normalization** —
  report Resolution subsection in both repos. Java branch fix/interop-header-hygiene
  (stacked on the report branch); Rust same-name branch (33fba853 + 7e22af9a, 260 tests).
  **Standards position stated in all docs (Eric's ruling): W3C/OTel traceparent is the
  position; traceparent.header = backward compat with legacy systems ONLY; departure
  discouraged. Final ruling (SUPERSEDES design point 2): inbound precedence = STANDARD
  traceparent always wins, custom name read only when standard absent (presence of the
  standard means the legacy system already upgraded; proprietary value is residual). Both
  engines flipped in lock-step, regressions inverted, report carries the refinement note.**
  **RELEASED 2026-07-24 in v4.10.4 both repos ([[thread-release-4-10-4]]) — the full arc
  closed: field request → design ruling → lock-step implementation → ce_traceparent live
  interop → hygiene round → standards position → release.**
  <!-- id: thread-traceparent-header-config | created: 2026-07-24 | last_used: 2026-07-24 | uses: 1 | tier: archive-candidate | origin: 2026-07-24-154543 -->

- [x] (release in flight — 2026-07-23; CLOSED same day) **v4.10.3 SHIPPED AND PUBLISHED in
  lock-step (both repos) — field-deployment roll-up.** Releases are immutable (Eric), so the
  post-4.10.2 fixes shipped as a new patch for the field pipeline: demo clean-echo
  (#225/Rust #175) + npm webapp refresh (#224/Rust #174); no engine behavior changes — the
  release consolidates the whole 4.10 line (wire format, presentation parity, metadata
  contract, reserved inbox, collection plugins) for field quality gates. Java: PR #226, tag
  `v4.10.3` on squash commit `bd7e909d` (verified before tagging — the 4.10.2 tag-race
  lesson), CI 7m24s green + clean local reactor 4:49 (first gate run failed only from Eric's
  concurrent build on the same tree). Rust: PR #176, tag on merge `b3804a67`, CI green
  (252 tests). Fourth lock-step release of the arc: 4.10.0 interop → 4.10.1 presentation
  parity → 4.10.2 boundary demarcation → 4.10.3 field roll-up.
  <!-- id: thread-release-4-10-3 | created: 2026-07-23 | last_used: 2026-07-24 | uses: 1 | tier: archive-candidate | origin: 2026-07-24-023859 -->

- [x] (release in flight — 2026-07-23; CLOSED same day) **v4.10.2 SHIPPED AND PUBLISHED in
  lock-step (both repos).** Java: PR #222, tag `v4.10.2` on `61ddb772`, published. Rust:
  PR #172, tag on merge `6a39bccc`, published — after the CI race fix `0d09154d` (the latent
  parallel-test config-freeze flake from #171's essential sequencing; deterministic Once fix,
  0/80 under load). Third lock-step release of the arc: 4.10.0 interop → 4.10.1 presentation
  parity → 4.10.2 boundary demarcation + community plugins.
  Patch release in lock-step with Rust: metadata contract (#221), temporary.inbox alignment
  (Rust #171), + PR #220 (team-contributed collection plugins isEmpty/getFirst/getLast —
  REVIEWED by Claude Code at Eric's request: correct + convention-consistent; polish commit
  `a38bb181` added license headers ×6 + syntax-guide Collection docs; CHANGELOG entry deferred
  to release prep to avoid branch conflict; formal GitHub review API blocked for EMU accounts —
  review delivered in-session). Sweep 4.10.1→4.10.2 done; CHANGELOG dated 7/23/2026 with the
  boundary-demarcation summary. **Java HALF DONE: PR #222 merged, tag `v4.10.2` on `61ddb772`
  (first tag landed on the wrong commit — pull raced the merge; deleted + re-tagged within a
  minute, lesson: verify what the tag landed on), RELEASE PUBLISHED.** Rust half BLOCKED on a
  CI failure in PR #172: flows_run_end_to_end_like_java panics "Flow dynamic-reserved-key not
  found" (green locally, fails on slower CI — suspected timing/ordering race; agent diagnosing
  with deterministic-fix-over-retry guidance). Close when Rust tagged + published.
  <!-- id: thread-release-4-10-2 | created: 2026-07-23 | last_used: 2026-07-23 | uses: 1 | tier: working | origin: 2026-07-23-211728 -->

- [x] (in flight — 2026-07-23; COMPLETE same day, released in v4.10.2) **Metadata
  injection/sanitization hardening (Eric's 3rd interop round).** Design ruling: function inputs = headers/body/instance; headers = envelope-header
  COPY + metadata INJECTED at entry, SANITIZED at exit; metadata NEVER transported in the event.
  Java reference DONE on `feature/metadata-injection-hardening` (1 commit, NOT pushed): business
  cid → engine tag `my_cid` (EventEmitter.BUSINESS_CID_TAG; tags wire field, no spec change;
  three stamping sites converted), worker entry injection (4 my_* keys + legacy-header compat +
  x-event-api strip), symmetric exit filter (copyResponseHeaders + x-event-api), HTTP response
  X-Correlation-Id echo (AsyncContextHolder + AsyncHttpResponse; function-set header wins).
  Full reactor green; platform-core 415 (4 new regressions); live verification passed (cid echo
  inbound+generated; function view: 4 injected my_*, business cid intact, no x-event-api).
  Tracing signature UNCHANGED (all four directions re-verified against the reference before the
  fix). **Rust mirror COMPLETE (`794fb287`, 249 tests green) and four-way re-verification
  PASSED with the extended invariants** — identical injected my_* key set on both callees,
  end-to-end cid identity across the language boundary (response header == callee-injected
  my_correlation_id), x-event-api absent everywhere, span signature empty-diff in all four
  combinations. **+ Second item (Eric): Rust RPC-reply design gap — the "inbox." prefix
  pseudo-routes reserve the whole inbox.* namespace (collides with workflow-app route names
  like inbox.approval); Rust session aligning to Java's single reserved private route
  `temporary.inbox` (@ZeroTracing @EventInterceptor, 500 instances, cid-keyed registry,
  composite cid-seq split) as a 2nd commit on the same branch; CRITICAL sub-item: the Rust
  one-record-per-span suppression gate keyed off the "inbox." prefix must re-key (prefer the
  rpc tag, Java's real mechanism).** **Rust 2nd commit DONE (`698de3c4`, 250 tests; gate
  re-keyed to the rpc TAG; essential sequencing + @origin never-emit per Eric's hints; found a
  genuine AsyncHttpClientService global-platform bug) and the INTEROP RE-TEST PASSED in full**
  (cross-language both directions: functionality, auth, cid echo + generated-identity, my_*
  parity, x-event-api-free, signature empty-diff ×2×2 — the inbox refactor is observably
  invisible, validating Eric's robustness hypothesis). **BOTH PRs MERGED 2026-07-23: Java #221
  (merge `a25d95d5`) + Rust #171 (merge `f86fbec2`), CI green both.** Remaining: the v4.10.2
  lock-step releases ([[thread-release-4-10-2]]). Relates [[conv-telemetry-presentation-parity]].
  <!-- id: thread-metadata-injection-hardening | created: 2026-07-23 | last_used: 2026-07-24 | uses: 1 | tier: archive-candidate | origin: 2026-07-23-211728 -->

- [x] (release in flight — 2026-07-23; CLOSED same day) **v4.10.1 SHIPPED via the normal
  flow (Java repo)** — tag `v4.10.1` on merge commit `9ae666df` (PR #218, CI green + local
  full reactor), release notes delivered, release published. Rust shipped in lock-step the
  same day (tag on `2c4e4066`, PR #170). Patch release in lock-step with the Rust engine's v4.10.1 (its branch
  `chore/release-4.10.1`, commit `44658df5`, pushed): telemetry presentation parity. Content:
  PR #217 (/api/event visible span, declarative rename, event.api.auth demo, interop report
  parity outcome + future-ports playbook). Sweep done (32 poms + CLAUDE/GEMINI +
  instructions.md), CHANGELOG dated 7/23/2026. Close when tagged + release published.
  <!-- id: thread-release-4-10-1 | created: 2026-07-23 | last_used: 2026-07-23 | uses: 1 | tier: working | origin: 2026-07-23-145132 -->

- [x] (in flight — 2026-07-23; COMPLETE same day) **Post-4.10.0 telemetry presentation parity +
  auth demo (Eric's manual-test findings).** Java reference branch `feature/event-api-span-and-auth` (2 commits,
  NOT pushed): /api/event edge is now a visible span (EventApiService no longer @ZeroTracing;
  worker-thread span capture for async callbacks; regression eventApiServiceIsAVisibleSpanInThe
  Trace), demo→declarative rename, event.api.auth demo (${DEMO_PEER_TOKEN:demo} env secret,
  session-info proof, 3 tests). Rust branch of the same name (2 commits, NOT pushed): callback-
  dispatch refactor + async.http.response service, request-scoped context gating, my_* strip,
  rename+auth mirror (session-info forwarding implemented). **Four-way verification COMPLETE:
  java-to-java, rust-to-rust, java-to-rust, rust-to-java all EMPTY DIFF against the normalized
  reference signature** (/tmp/java-to-java-reference-signature.md; per-pattern 8+9 records).
  **UPDATE: both PRs MERGED 2026-07-23** — Java #217 (merge `d3c2f853`, CI green 6m29s) and
  Rust #169 (merge `ecec21c5`, CI green); both repos' docs carry the extended interop test
  report (parity drive + four-way empty-diff + future-ports playbook). **COMPLETE: both
  v4.10.1 releases published 2026-07-23 ([[thread-release-4-10-1]]) — the parity arc closed
  end to end: reference implementation → refactor → four-way empty diff → lock-step release.**
  Relates [[conv-telemetry-presentation-parity]].
  <!-- id: thread-telemetry-parity-auth | created: 2026-07-23 | last_used: 2026-07-23 | uses: 1 | tier: archive-candidate | origin: 2026-07-23-145132 -->

- [x] (release in flight — 2026-07-22; CLOSED same day) **v4.10.0 SHIPPED via the normal flow** —
  tag `v4.10.0` on merge commit `af21e6f6` (PR #216, CI green + local full reactor), release
  text delivered, release published.
  Feature release: wire format (#212/#213), D1 timeout fix (#214), demo pair + default-on log
  context + RPC span lineage (#215). Both gates passed before the branch was cut: Java PR #215
  merged (`9d8ae12a`, CI green ×3 rounds) and Rust mercury PR #167 merged (`c64d0683` — parity
  batch + I1/I2/I3 telemetry fixes); live bidirectional interop drives PASSED in full (both
  patterns, both directions, span-accurate — permanent record:
  `docs/test-reports/event-over-http-interop.md`). Sweep done (32 poms + CLAUDE/GEMINI +
  instructions.md coordinates), CHANGELOG dated 7/22/2026. **PR #216 merged (`af21e6f6`, CI
  green); tag `v4.10.0` pushed on the merge commit; release notes delivered.** Close when the
  GitHub release is published.
  <!-- id: thread-release-4-10-0 | created: 2026-07-22 | last_used: 2026-07-23 | uses: 1 | tier: archive-candidate | origin: 2026-07-23-015717 -->

- [x] (design — 2026-07-21; COMPLETED 2026-07-22 with the v4.10.0 release) **Common event
  envelope wire format for cross-language interop (Event over HTTP with the Rust port).** Design DRAFTED at
  `draft-design-specs/event-envelope-interop-design.md` — standard envelope = MsgPack map
  with descriptive string keys (the existing `toMap()` form promoted to a wire contract);
  compact 1-char keys and standard ≥2-char keys are disjoint → decoders sniff both, no
  negotiation; encode: requester chooses (config + per-call header), responder mirrors;
  Java `exceptionBytes` (ObjectOutputStream) excluded — portable error = status + message
  + stack text. Rust port is the interop testbed but is READ-ONLY from this repo's
  sessions (another session owns Rust edits). Includes draft ADR text. **Design REVIEWED
  by Eric 2026-07-21: default = `standard` (Event over HTTP is transport, not storage —
  no serialized data outlives the exchange; upgrade both sides together), `compact` kept
  as explicit fallback for slow-to-upgrade installations (FIFO-vs-BDB precedent);
  config `event.over.http.format` + per-request `x-event-format` header CONFIRMED.**
  **API shape AGREED 2026-07-21:** `enum Format {COMPACT, STANDARD}`; `toMap(Format)` /
  `toBytes(Format)`; no-arg `toMap()`=STANDARD, no-arg `toBytes()`=COMPACT (both preserve
  today's behavior — EventEmitter's `of(event.toMap())` clone path depends on the
  standard no-arg); `load()` sniffs. Outbound format = transport policy (groundwork for
  Redis/S3 event-to-bytes transports). Mesh investigated: compact on every hop today,
  Java-only fleet, stays out of v1 scope (MultipartPayload segmentation is a second
  proprietary layer). **Phase 1 IMPLEMENTED 2026-07-21** on branch
  `feature/event-envelope-standard-format`: Format enum API, sniffing load +
  getWireFormat(), EventEmitter format resolution at both encode points, EventApiService
  response mirroring, 8 new tests incl. golden vectors
  (`system/platform-core/src/test/resources/envelope-vectors/vectors.json` — share with
  the Rust session), spec page `docs/guides/event-envelope-wire-format.md`. Spec
  adjustment from implementation: body is when-set on encode (MsgPack.packMap skips
  nulls); Rust decoder needs a serde default on `body`. **Phase 1 MERGED 2026-07-21
  (PR #212, merge commit `2cf2ebdf`; CI green: 951 reactor tests + docs verify; spec page
  live on the docs site).** Hand-off note
  for the Rust session: `/tmp/event-envelope-rust-handoff.md` (contract summary, compact
  decision + flag table, /api/event semantics, vector procedure, interop test plan) —
  Eric will ask this repo's session to REVIEW the Rust implementation for consistency
  afterward. **Phase 2 IMPLEMENTED in the Rust session (mercury increments 59-61, PRs
  #163-#165) and REVIEWED for consistency 2026-07-22: high fidelity, no blockers** —
  vectors byte-identical, envelope/service/client semantics match (see session
  2026-07-22-004243 for the asymmetry list). Review follow-up: new additive golden vector
  `standard-trace-context` (span_id coverage, their finding) on branch
  `test/fetcher-cache-key-guard`; Rust re-syncs vectors.json + bumps its count assertion
  (note: `/tmp/event-envelope-vectors-update.md`). **LIVE BIDIRECTIONAL INTEROP TEST
  PASSED 2026-07-22** (report `/tmp/event-over-http-interop-test-report.md`; session
  2026-07-22-015924): Java→Rust 7/7, Rust→Java 6/7 (last case blocked only by the Rust
  client's mirror of defect D1 — flagged to the Rust repo), trace continuity both ways.
  Drive found+fixed a REAL pre-existing Java bug (D1: getTimeoutSeconds floor-division →
  1s HTTP read timeout; fixed on branch `fix/http-client-response-timeout` with
  regression test). **UPDATE same day: Rust→Java also 7/7 — D2 fixed (div_ceil + grace,
  regression test) and the blocked case re-verified live; D3 (example echo binary drop)
  fixed; declarative `yaml.event.over.http` routing implemented for parity (increment 62,
  live zero-code cross-language proof verified in Java telemetry). ALL MERGED 2026-07-22:
  Java D1 fix = PR #214 (merge `2b5504a0`); Rust D2+D3+test-service+declarative = mercury
  PR #166 (merge `e36e5dc5`, commits f62a69bf/230ee55b/258f3578). The "redundant D2 chip
  session" turned out not to exist (session list + transcript search clean); repo memory
  protocol protects against late arrivals anyway. Interop test processes stopped.**
  **UPDATE 2026-07-22 (evening): the full release-gate cycle completed** — Java PR #215
  (demo pair, default-on log context, RPC span lineage + the spanIdFromResponder
  refinement) and Rust mercury PR #167 (parity batch + I1/I2/I3 telemetry fixes) BOTH
  merged; live bidirectional pattern drives (programmatic + declarative) PASSED in full
  with span-accurate telemetry; permanent record at
  `docs/test-reports/event-over-http-interop.md`. **v4.10.0 SHIPPED AND PUBLISHED 2026-07-22
  ([[thread-release-4-10-0]]) — this thread is COMPLETE: design → implementation → parity →
  live interop → release, both engines in lock-step.** → serves `vision-mercury-composable`
  (polyglot deployment)
  <!-- id: thread-event-envelope-interop | created: 2026-07-21 | last_used: 2026-07-25 | uses: 7 | tier: active | origin: 2026-07-21-215951 -->

- [x] (field support — 2026-07-21; CLOSED 2026-07-26 by the review — close condition
  subsumed: the field's Sonar gate passed with a perfect Overall-Code score on v4.10.6,
  which contains the entire 4.9.2 remediation, so the pending "field rescan passes" is
  satisfied — see [[thread-sonar-4-10-4-field-rejection]])
  **v4.9.1 REJECTED by the field Sonar quality gate; remediation
  MERGED (PR #210, `7110561c`), v4.9.2 release in flight for the field rescan.** 19 issues
  (8 HIGH: S3776 complexity ×4 + S1192 literals ×4; 11 MEDIUM: S5778 ×5, S125 ×2, S1168 ×2,
  S5961, S6126), all introduced by the 4.9.0/4.9.1 minigraph companion/discovery code (field's
  prior scan was pre-4.9). All 19 fixed via helper extraction, constants, test splits,
  prose-comment rewording, and the `deployedModel` empty-map contract — see session
  2026-07-21-173614 for the per-issue map + Eric's IDE review round. Behavior-preserving; full
  reactor green; Eric's manual HTTP-404 regression passed (dry-run + deployed execution).
  **v4.9.2 SHIPPED 2026-07-21** (tag on merge commit `b574f41e`, PR #211; 943 reactor tests
  green + Eric's manual HTTP-404 regression). Close when the field rescan passes. Durable
  lesson recorded: extract-as-you-go when touching methods near the S3776/S5961 thresholds.
  <!-- id: thread-sonar-4-9-1-field-rejection | created: 2026-07-21 | last_used: 2026-07-27 | uses: 4 | tier: active | origin: 2026-07-21-173614 -->

- [x] (release in flight — 2026-07-21; CLOSED same day) **v4.9.1 SHIPPED via the normal flow** —
  tag `v4.9.1` on merge commit `26a132f9` (PR #208, CI green + local full reactor 942 tests),
  release text delivered, release published. Patch release: #206 describe-graph trailing-bracket
  fix + #207 docs overhaul (Material theme, mermaid, per-entry reference pages). Follow-on same
  day: dependabot HIGH alert #29 (js-yaml 4.2.0, dev-only transitive in the playground webapp
  lockfile) remediated via PR #209 (`902da23f`, lockfile-only bump to 4.3.0) — landed one commit
  after the release tag; no artifact impact.
  <!-- id: thread-release-4-9-1 | created: 2026-07-21 | last_used: 2026-07-21 | uses: 2 | tier: archive-candidate | origin: 2026-07-21-012842 -->


- [x] (release in flight — 2026-07-13; CLOSED same day) **v4.8.4 SHIPPED via the normal flow** —
  tag `v4.8.4` on merge commit `b1265f18` (PR #177), release text delivered. Carries PR #176
  (secondary.kafka.health, 5 health-check workers, Minimalist Kafka guide rename). Eric's ruling
  retained for the record: the 4.8.3 deferred-tag flow was SPECIFIC to the Snyk rejection; normal
  releases tag immediately on merge.
  <!-- id: thread-release-4-8-4-tag-deferred | created: 2026-07-13 | last_used: 2026-07-21 | uses: 3 | tier: archive-candidate | origin: 2026-07-13-230916 -->


- [ ] (field support — 2026-07-13; ROOT CAUSE FOUND via Eric's devops screen share) **Trace-propagation
  report: the internal API gateway strips `traceparent` AND `X-Trace-Id` (neither on its allow-list);
  only `X-Correlation-Id` passes.** 4.4.11 "worked" because `trace.http.header=X-Correlation-Id, X-Trace-Id`
  made the trace ride the allow-listed first entry. Correction to the field's initial write-up: 4.6.3+
  DOES emit X-Trace-Id alongside traceparent on every traced outbound call (verified in code + wire-level
  tests on v4.7.1) — the gateway eats both. Fix options: (a) config-only on 4.8.3:
  `http.trace.id.header=X-Correlation-Id` both apps (caveat: cid wins the shared slot when ids diverge);
  (b) a proposed `legacy.trace.id` flag — **REJECTED by Eric (2026-07-13): no code change; re-mixing the
  business correlation-id with the trace id makes things worse — the goal is proper traceId/spanId/
  parentSpanId propagation**; (c) gateway allow-list change (add `traceparent` + `X-Trace-Id`
  pass-through) = **THE fix — Eric is taking it to the infra team**.**Original checklist below retained.**
  Trace-propagation report on 4.6.3: confirm the field config fix. Diagnosis + support checklist in
  [[field-trace-propagation-4-6-3-diagnosis]]; the two questions back to the team: is `tracing: true` set
  on every rest.yaml endpoint involved, and is the app-to-app call made through `async.http.request` (vs
  a custom/Spring HTTP client that must forward `traceparent` itself)? Regression test + observability
  impedance-matching docs MERGED (PR #169); ScheduleAdminTest race fix MERGED (PR #170 — file existence
  ≠ readiness when writes are truncate-then-write; poll the consuming API for settled state). The team
  trials v4.7.1 for the trace behavior while v4.8.3 is prepared. Also answered (2026-07-13): setting
  `http.trace.id.header` = `http.correlation.id.header` = `X-Correlation-Id` (legacy conflation) is safe
  when the edge always supplies the header — one value feeds both ids end-to-end. **UPDATE 2026-07-14:
  the absent-header divergence was FIXED** (`fix/conflated-header-id-unification`) — when the resolved
  trace/cid header names collide and the shared header is absent, both ingress paths (HTTP + Kafka
  adapter) now yield ONE id (trace authoritative, honors traceparent; cid adopts it). Divergence remains
  by design only for DISTINCT names. **VALIDATED by Eric 2026-07-14 (merged as PR #179, in the post-#181
  main)**: complete local build + live two-app hop with no header supplied → downstream saw traceparent
  trace-id == x-correlation-id (one generated id). Supplied-header case also validated (abc123 fed both
  ids across the hop). Demo pair for reproducing: composable-example (8100) -> lambda-example (8085)
  via /api/cross/app/tracing; traceId confirmed in BOTH apps' trace logs for both cases. Support nuance: with conflation, the outbound trace id rides the CONFIGURED header
  name (X-Correlation-Id), and traceparent is stamped only when the id is W3C-shaped (32-hex) — a short
  business id travels on the shared header alone, which is exactly why this works behind the
  traceparent-stripping gateway. Telemetry confirmed spanId/parentSpanId chain correctly within each
  app under the shared trace id; CROSS-app span parenting still needs traceparent (it carries the
  caller's span id), so tooling stitches by trace id until the gateway passes traceparent — positive
  case VERIFIED live too: with traceparent on the wire (generated 32-hex id), lambda-example's
  hello.world span parented directly onto the traceparent's span id across the HTTP hop. Field runs this conflation short-term; gateway team has been ASKED (2026-07-14) to
  allow-list traceparent + X-Trace-Id at the gateway/Istio — Eric updates after the devops team
  tests in the cloud dev environment.
  <!-- id: thread-field-trace-propagation-4-6-3 | created: 2026-07-13 | last_used: 2026-07-24 | uses: 7 | tier: working | origin: 2026-07-13-142021 -->

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
  <!-- id: thread-compilegraph-syntax-validation | created: 2026-07-02 | last_used: 2026-07-22 | uses: 2 | tier: working | origin: 2026-07-02-004606 -->

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
  <!-- id: thread-docs-improvement-backlog | created: 2026-06-24 | last_used: 2026-07-20 | uses: 4 | tier: working -->
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

- [x] (CLOSED 2026-07-27 — **ALL 15 CONFIRMED by Eric** in a one-by-one walkthrough with
  fresh live-tree evidence per item: 5 stack facts, 3 architectural invariants, 6 core
  conventions/gotchas, + the Vision. Several now carry stronger guarantees than when
  written: monoResponseForwardsSpanId regression, golden registration vectors pinning
  inputPojoClass, `"none"` default read live at ActuatorServices:109, and the Gson
  Integer→Long gotcha proven by a live hit in the conformance round. Vision current-state
  refreshed to v4.10.6 + Rust lock-step; target statement unchanged. Cadence reset.)
  **Re-verify invariants (due — 50 sessions since the last check ≥ verify_invariants_every 40).** Raised by
  the 2026-06-29 review (cadence). Confirm each never-decay fact still holds, or supersede any that don't
  (`DECAY.md` §9 — the review never auto-invalidates):
  core stack — `stack-language-java21`, `stack-build-maven`, `stack-integration-spring`,
  `stack-messaging-kafka`, `stack-ci-gha`; architectural invariants — `functions-decoupled-routes`,
  `typed-io-map-or-pojo`, `virtual-threads-rpc`; core gotchas/decisions — `trace-thread-keyed-mono-gotcha`,
  `instant-serialization`, `kafka-mesh-opt-in`, `event-script-over-code`, `conv-add-capability`,
  `conv-serialization-gotchas`; and the **Vision** (`memory/vision.md`). Check off when re-confirmed.
  <!-- id: thread-reverify-invariants-2026q2 | created: 2026-06-29 | last_used: 2026-07-27 | uses: 2 | tier: active -->

## User Preferences

## Team / Members

(none recorded yet)
