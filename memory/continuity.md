# Continuity — mercury-composable

> Shared ground truth for project state across all agents and sessions.
> Update at the end of every session. Never delete — only archive (see `REVIEW.md`).
>
> Each fact carries a metadata footer in an HTML comment, maintained by the review
> ritual — invisible when rendered, read/written by agents:
> `<!-- id: kebab-id | created: YYYY-MM-DD | last_used: YYYY-MM-DD | uses: N | tier: active -->`
> See `.agent/schema.md` for the fields and `memory/decay-policy.md` for the windows.
> Condensed 2026-07-31 (line-bloat review advisory): shipped-item narrative compressed
> to essentials; full detail lives in each fact's `origin` session log.

---

## Project State

- **project:** mercury-composable
- **status:** active, mature framework (Maven reactor)
- **repo:** github.com/Accenture/mercury-composable (official — source of truth)
- **last_enabled:** 2026-06-20
- **last_session:** 2026-08-03 | agent: Claude Code (2026-08-03-155225)
- **last_review:** 2026-07-31 | through 2026-07-31-001057.md
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

- **Release 4.8.3 — SHIPPED 2026-07-13 (tag `v4.8.3` on `6696a76f`; PRs #168-#175).** Security
  patch + hardening, field-pipeline-validated (Snyk + Sonar) BEFORE tagging — the deferred-tag
  flow works. Durable facts: **kafka.health** probes via `KafkaConsumer.listTopics` Metadata,
  which needs NO ACL (brokers filter by Topic Describe → empty-but-successful under locked-down
  principals); startup grace returns placeholder-healthy (`kafka.health.timeout` 5s /
  `kafka.health.startup.grace` 30s). Lessons: field screenshots' naming conventions are
  client-identifiable — paraphrase generically in fixtures/commits/PRs; a flow needs ≥1 `end`
  task (a `sink` only terminates a side branch). Full detail: origin log.
  See [[release-4-8-2-shipped]] for the prior cycle.
  <!-- id: release-4-8-3-shipped | created: 2026-07-13 | last_used: 2026-07-24 | uses: 10 | tier: archive-candidate | origin: 2026-07-13-170933 -->

- **Field trace-propagation report on 4.6.3 diagnosed (2026-07-13): not a framework bug** —
  v4.5.0's documented tracing cleanup removed the `X-Correlation-Id`-doubles-as-trace-id
  conflation that made 4.4.x *appear* to propagate; the trace id travels only as `X-Trace-Id` /
  W3C `traceparent`, never echoed back. **Support checklist for this symptom:** (1) `tracing: true`
  on every rest.yaml entry involved (per-endpoint, default FALSE); (2) app-to-app calls must use
  `async.http.request` (only the framework client auto-stamps trace headers; custom/Spring clients
  must forward `traceparent` manually); (3) trace context is thread-bound
  ([[trace-thread-keyed-mono-gotcha]]). Validated live + regression
  `traceContinuesAcrossApplicationToApplicationHttpCall`. Full detail: origin log.
  <!-- id: field-trace-propagation-4-6-3-diagnosis | created: 2026-07-13 | last_used: 2026-07-24 | uses: 5 | tier: archive-candidate | origin: 2026-07-13-142021 -->

- **Release 4.8.2 — SHIPPED 2026-07-12 (tag `v4.8.2` on `6c024311`; PRs #164-#166).** Durable
  facts: (1) **Impedance matching is the twin-kafka demo pattern** — each cluster keeps its own
  business correlation-id header; adapters read the cluster header into model.cid, flows map
  model.cid back out under the NEXT cluster's name; never leak a cluster's header name across the
  bridge (wire-level tests assert it). (2) **Template externalization is OPT-IN**
  (minimalist/twin-kafka family only): location defaults are classpath-only; devops sets the
  location key explicitly (4.8.2 CHANGELOG migration note). (3) **Release-sweep gotcha:** when the
  outgoing version is a substring of a dependency version (classgraph 4.8.184 ⊃ "4.8.1"), the perl
  sweep needs a digit lookahead `(?!\d)`. Cross-vendor loop precedent (Copilot authored, Claude
  completed the crash gap, dual attribution): origin log. See [[release-4-8-1-shipped]].
  <!-- id: release-4-8-2-shipped | created: 2026-07-12 | last_used: 2026-07-24 | uses: 5 | tier: archive-candidate | origin: 2026-07-13-014037 -->

- **Release 4.8.1 — SHIPPED 2026-07-11 (tag `v4.8.1` on `3d226c5b`; PRs #159-#161).** Durable
  facts: (1) **SimpleRandomPartitioner is minimalist-kafka's producer DEFAULT** (`putIfAbsent` in
  KafkaClientConfig — a template's own `partitioner.class` wins; explicit `partition` header
  bypasses; keyed records keep murmur2); Kafka's sticky default starves multi-instance consumer
  groups at low volume. (2) **CryptoApi DSA methods REMOVED.** (3) Release-bump sweep = 32 poms;
  coverage aggregate 85.8% line / 80.0% Sonar-combined — field Sonar should exclude `benchmark/**`.
  (4) **Flow-authoring conventions:** the engine does NOT auto-convert a Map into a byte[]-typed
  function — use `f:binary(model.x) -> *` (`:binary` colon shorthand DEPRECATED); set
  `text(application/json) -> output.header.content-type` on flow HTTP responses. (5) **No version
  strings in pom comments** — history belongs in the CHANGELOG. Full detail: origin log.
  See [[release-4-8-0-shipped]].
  <!-- id: release-4-8-1-shipped | created: 2026-07-11 | last_used: 2026-07-30 | uses: 9 | tier: active | origin: 2026-07-12-002326 -->

- **Release 4.8.0 — SHIPPED 2026-07-10 (tag `v4.8.0` on `5d9fda45`; PRs #153-#157): twin-kafka.**
  Durable architecture facts: (1) **twin-kafka is a separate `system/` module depending on
  minimalist-kafka** — dual-cluster is a special case; single-cluster apps never carry its weight;
  artifacts named `secondary.*`; a bridge is flow YAML (consume via one adapter, publish via the
  other cluster's notification; trace + model.cid continuous). (2) **Reuse seams in
  minimalist-kafka** (behavior-preserving): KafkaClientConfig location-key overloads;
  SimpleKafkaNotification protected accessors; SchemaCodec.fromConfig(prefix) — **distinct caches
  per registry are a correctness requirement** (Confluent global schema ids are per-registry;
  bridging framed payloads = decode-and-re-encode, NEVER relay raw framed bytes). (3) Registry is
  optional PER CLUSTER; Azure Event Hubs works via the Kafka endpoint. (4) **DLQ correctness:
  RetryPolicy carries the publisher → secondary dead letters land on the secondary cluster.**
  (5) kafka-standalone `dual.servers=true` = brokers 9092 + 8092. (6) **Header-name precedence:**
  per-entry > application.properties global > built-in default; W3C traceparent always wins for
  the trace-id. (7) Map the business cid from `model.cid` (engine-seeded), never the raw record
  header; CompileFlows rejects mappings overwriting reserved model keys. Full detail: origin log.
  <!-- id: release-4-8-0-shipped | created: 2026-07-10 | last_used: 2026-07-31 | uses: 9 | tier: active | origin: 2026-07-11-031930 -->

- **Graph workflow suspension: short runs + external state store, encapsulated in skills
  (design ratified by Eric 2026-07-28). (ADR-0010)** A human checkpoint = persist
  {cid, node, ttl, model minus reserved keys, seen, run} via `skill=graph.suspend` and complete
  the run; resume = same business cid restores state and jumps past the checkpoint without
  re-execution (`graph.resume`, `resume:<alias>` directive). Both skills are supersets of
  graph.task invoking a pluggable store function (`task=`) with a fixed put/get contract — zero
  node data mapping. `suspend` = reserved node ALIAS (one per graph, alias⇔skill enforced, drawn
  checkpoint edge required); `suspend=true` = reserved property; `ttl` = mandatory, no default.
  **Refinement (2026-07-29): `missing=<node>` ELIMINATED** — absent and expired records look the
  same by design; handling is application logic on the resume node's forward path. Instead
  `graph.resume` sets engine-managed **`model.run` = `resume` | `fresh`** (set after the model
  merge; excluded from persistence). Consume-on-retrieve (Redis GETDEL) = at-most-once resume.
  Constraints: sole active branch; model is the workflow's durable memory ({node}.result does not
  survive); cid = resume capability (auth resume endpoints); no graph.extension crossing. Store:
  Redis = extensions/minigraph-state-redis imported by apps, NEVER the engine. Delivered by
  [[thread-graph-suspend-resume]]; serves [[bp-graph-workflow-suspension]].
  <!-- id: graph-suspend-resume-design | created: 2026-07-29 | last_used: 2026-07-31 | uses: 6 | tier: active | origin: 2026-07-29-010343 -->

- **CompileGraph is the MANDATORY deployment gate for graph models — CompileFlows parity
  (Eric's rulings 2026-07-29; ADR-0011 ACCEPTED via the PR #240 merge, squash `4348b0da`).**
  A deployed graph is executable at `POST /api/graph/{graph-id}` only when listed in the manifest
  (`graph.model.automation`) AND passing the gate; failed or unlisted = HTTP-404 as if nonexistent
  ("compiled or 404" is the whole rule; lazy per-request loading DELETED). The manifest carries its
  own `location` (default `classpath:/graph`); `location.graph.deployed` retired. **Two-lane
  validation:** production = models → CompileGraph → GraphExecutor (trusts the gate, keeps only
  data-driven guards); dry-run = /tmp/graph drafts → UI CLI validation → GraphTraveler with FULL
  runtime validation. Whole-graph rules modularized in `GraphModelValidator`, reused by the
  playground `run` pre-run check — also the landing pad for
  [[thread-compilegraph-syntax-validation]]. Hot-dropping JSON into the deploy folder no longer
  executes (deployment = explicit act). Full detail: origin log.
  <!-- id: compilegraph-mandatory-gate | created: 2026-07-29 | last_used: 2026-07-30 | uses: 3 | tier: active | origin: 2026-07-29-190328 -->

- **ManagedCache eviction: Java accepts + documents non-determinism; Rust is strict LRU — a
  deliberate cross-engine asymmetry (Eric, 2026-07-27).** Java keeps Caffeine W-TinyLFU (eviction
  under `maxItems` pressure is approximate; no policy knob exists); javadoc + CHANGELOG state it —
  callers must never rely on which entry survives. The Rust port is moka `EvictionPolicy::lru()`
  (deterministic) per Eric's ruling there. Eviction is internal state, NOT a presentation surface —
  [[conv-telemetry-presentation-parity]] does not require closing this gap. "Frequency aging" is
  NOT a determinism remediation. Revisit trigger: the first consumer that truly runs at capacity
  (schema-registry caches are the candidate). Options record: the Rust repo's
  docs/design/managed-cache-port.md. Full detail: origin log.
  <!-- id: managed-cache-eviction-determinism | created: 2026-07-28 | last_used: 2026-07-28 | uses: 1 | tier: archive-candidate | origin: 2026-07-28-005814 -->

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
  conflict policy — explicit > declarative, duplicates WARN + last-wins; plugins = flow vocabulary
  never gated, features honor gating; loud-failure discovery; misuse is a tested error surface).
  Spec: docs/guides/registration-metadata-contract.md. Conformance: golden vectors shared verbatim
  (registration-vectors/{core,plugin,feature}.json). New ports pass the three vector suites before
  their declaration surface is done.
  <!-- id: registration-metadata-contract | created: 2026-07-26 | last_used: 2026-07-29 | uses: 3 | tier: active | origin: 2026-07-25-235904 -->

- **Telemetry/log presentation parity across language engines is a field requirement (Eric,
  2026-07-23).** Installations will be POLYGLOT for a long time — DevSecOps teams see both engines'
  telemetry and logs in one aggregation, and any presentation difference is a support burden.
  Operating rule: the Java engine is the REFERENCE implementation; a same-language interop run must
  be an exact structural replica after normalizing volatile fields — then cross-language runs are
  symmetric by construction (reference-signature procedure: session 2026-07-23-145132).
  **Scope extension: the Event Script surface is part of the cross-engine contract** — flows are
  engine-portable YAML, so any new built-in simple plugin ships in lock-step on both engines (with
  closely matching error messages), or flows stop being portable.
  <!-- id: conv-telemetry-presentation-parity | created: 2026-07-23 | last_used: 2026-07-30 | uses: 12 | tier: active | origin: 2026-07-23-145132 -->

- **graph.js is slated for eventual phase-out in favor of graph.task (Eric, 2026-07-31),
  and the Rust port does not carry it at all.** The skill is troublesome by nature — it is
  code injection; developers have been warned to use it with caution — and the newer
  graph.task can express very complex logic, so at some point graph.js will be retired.
  Operating consequences: don't invest in hardening graph.js beyond containment (its 5s
  default execution deadline exists to bound damage, not to bless long scripts); prefer
  graph.task in examples and guidance; graph.math stays (safe expression engine, no
  loops); **graph.js work is never a Rust lock-step item** — the Rust validator's
  deadline-skill set legitimately names three skills where Java names four.
  Relates [[thread-task-ttl-override]].
  <!-- id: graphjs-phase-out-direction | created: 2026-08-01 | last_used: 2026-08-01 | uses: 1 | tier: working | origin: 2026-08-01-035647 -->

- **The `helpers/` standalone servers exist for Docker-less developer machines and are
  the standard local test servers for Rust ports (Eric, 2026-07-29).** They embed REAL
  redis/kafka servers as plain `java -jar` apps because many field developers work on
  Windows — especially VDI environments with no virtualization system, where Docker/
  Testcontainers are unavailable. Usage convention: redis-standalone serves the Rust
  minigraph-playground (suspend/resume live drives); kafka-standalone + the
  schema-registry mock will serve the future minimalist-kafka Rust port.
  <!-- id: conv-helpers-docker-less | created: 2026-07-29 | last_used: 2026-07-30 | uses: 2 | tier: active | origin: 2026-07-29-190328 -->
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
- [x] (blueprint — RATIFIED by Eric 2026-07-28; **CLOSED by Eric's gate 2026-07-30** — realized on
  BOTH engines and published in v4.11.0: first-class vocabulary (graph.suspend/graph.resume,
  reserved alias, suspend=true, model.run), pluggable stores with the shipped Redis module, the
  mandatory CompileGraph gate it prompted (ADR-0011), validated across a mixed Java/Rust fleet;
  per Eric CORE functionality for a few field installations) **Workflow suspension for the Active
  Knowledge Graph** — human-in-the-loop checkpoints as first-class graph vocabulary: suspend/resume
  via pluggable external state stores, so a graph model expresses a long-running business process
  as a sequence of short runs. Realized by [[thread-graph-suspend-resume]].
  → serves: vision-mercury-composable
  <!-- id: bp-graph-workflow-suspension | created: 2026-07-28 | last_used: 2026-07-30 | uses: 5 | tier: active | origin: 2026-07-29-003528 -->

## Open Threads

- [x] (feature — **COMPLETE ON BOTH ENGINES: Rust half shipped 2026-08-01 in
  mercury PR #191 + release PR #192, tag v4.11.1 — see session 2026-08-02-013842;
  the x-ttl budget derivation (32-bit parse, ceil-to-seconds) is a pinned wire
  contract on both engines.** Java half MERGED 2026-07-31 as
  [PR #250](https://github.com/Accenture/mercury-composable/pull/250), squash
  `8191ab1c`, CI green (Build & Unit Tests 7m27s)** — four branch commits squashed:
  feature + review-1 hardening + deadline-cleanup round + the x-ttl ruling; event-script
  185/185, minigraph 98/98, platform-core 425/425, full reactor green; **remaining: the
  Rust lock-step half** — handoff at /tmp/task-ttl-override-rust-handoff.md, sections
  1-7, zero open questions; the suspend/resume-adjacent deadline surface is
  regression-critical field-core on both engines)
  **Task-level ttl override: catchable child timeouts (field report) + the deadline
  cleanup round.** TTL propagation copies the parent's FULL ttl with a restarted timer →
  the parent always expires first and its own timeout is uncatchable. Ratified:
  propagation stays the default; per-task `ttl` (duration, < flow.ttl, flow:// tasks
  only, runtime WARN) + per-node `ttl` on graph.extension/api.fetcher/task (suspend
  grammar; suspend's own ttl = store expiry, distinct) → a shorter child deadline makes
  the child's 408 CATCHABLE → budgeted retries (proven live). **Model metadata
  (model.{cid,instance,flow,ttl,trace,parent,root,none,run}) is IMMUTABLE in the graph
  engine at both layers** — gate + pre-run check (incl. MAPPING: statement lines) +
  the shared runtime guard on all four model-writing paths. **Cleanup round (Eric's four
  rulings 2026-07-31): delay now defers sub-flow launches** (cancelled at flow teardown —
  orphaned-launch fix); **dry-run run-level watcher at model.ttl** (exactly-one-terminal
  CAS arbitration, owner-tagged watcher slot, companion drain sized past the deadline +
  drain-timeout = ok=false); **fetcher stamps x-ttl** (wire read-timeout = deadline+1s);
  **graph.js deadline via sticky GraalVM close(true) on a virtual thread, default 5s**
  (NOT model.ttl — scripts are simple computation; node ttl overrides; run-level error,
  not exception=-routable). Adversarial round 2: 16 confirmed findings fixed — durable
  GraalVM lessons: interrupt is non-destructive + gap-consumable (sticky close(true) is
  the correct watchdog) and interrupt(Duration.ZERO) BLOCKS indefinitely (never on an
  event loop). **Eric's ruling (2026-07-31): x-ttl deadline propagation is a FEATURE —
  keep and document.** A Mercury caller's deadline propagates end-to-end (ingress honors
  inbound x-ttl over rest.yaml timeout); documented in the rest.yaml timeout grammar row,
  fetcher docs and CHANGELOG; both stamp branches pinned by wire-echo tests ("7000" node
  ttl / "30000" propagated model.ttl). Residual
  (pre-existing, recorded): overlapping runs share session instance state (late-callback
  bleed). Spec: draft-design-specs/task-ttl-override.md (gitignored). Full detail:
  origin log + session 2026-08-01-035647.
  <!-- id: thread-task-ttl-override | created: 2026-08-01 | last_used: 2026-08-01 | uses: 1 | tier: working | origin: 2026-08-01-022358 -->

- [x] (field support — **COMPLETE ON BOTH ENGINES: Rust half shipped 2026-08-01 in
  mercury PR #191 (fallback proven on the wire: contiguous MULTI/GET/DEL/EXEC in the
  RESP-double journal), released in Rust v4.11.1.** 2026-07-31; **Java half MERGED as
  [PR #248](https://github.com/Accenture/mercury-composable/pull/248), squash `5b73b140`,
  CI green; field member validates on Windows VDI 2026-07-31** — remaining: the Rust
  lock-step half, handoff ready) **Graph state store fails with `ERR unknown command
  GETDEL` on Redis < 6.2 — fixed with a version-aware consume strategy.** Field report: Windows VDI +
  redis-standalone + tutorial-14 (the embedded-redis library bundles Redis 6.2.x for
  macOS/Linux but only **5.0.14 for Windows** — the community port stopped there).
  **Eric's ruling: version-aware (deployed envs are Linux, but enterprise managed Redis —
  AWS for one field installation, also Azure/GCP — is outside our control).** Fix on
  branch `fix/redis-getdel-compat` (commit `d2b49beb`): detect `redis_version` from
  `INFO server` once per connection (stated in the startup log); ≥ 6.2 → native GETDEL,
  older → **atomic MULTI/EXEC GET+DEL** (at-most-once resume per ADR-0010 holds on both
  paths; plain sequential GET→DEL would open a double-resume race); undetectable version →
  the transactional fallback (works everywhere). Tests 10/10 incl. the fallback exercised
  for real via forced strategy; docs updated (workflow-suspension guide, reserved-names,
  EmbeddedRedis javadoc, CHANGELOG Fixed); the permanent interop report untouched.
  **Remaining: Eric's PR gate; then the Rust lock-step half (identical exposure at
  lib.rs:131; handoff ready at /tmp/redis-getdel-compat-rust-handoff.md) — the
  suspend/resume surface is regression-critical field-core on both engines.**
  Relates [[graph-suspend-resume-design]], [[conv-helpers-docker-less]].
  <!-- id: thread-redis-getdel-compat | created: 2026-07-31 | last_used: 2026-07-31 | uses: 1 | tier: working | origin: 2026-07-31-180131 -->

- [x] (feature — design RATIFIED 2026-07-30; **MERGED same day as
  [PR #246](https://github.com/Accenture/mercury-composable/pull/246), squash `268e5ff6`, CI
  green; rides the next release via CHANGELOG Unreleased.**) **Second-level routing for the
  kafka-flow-adapter + JSON serialization symmetry.** Per-binding `flows:` rule list (XOR `flow:`):
  `<selector>(<matcher>) -> <target>` — selectors `input.header.<name>` (case-INSENSITIVE name
  lookup) and `input.body` dot-bracket composite paths (Map AND List bodies, incl. top-level
  arrays `input.body[0].type`; body lookups run under a synthetic root, making MultiLevelMap's
  throwing `$`-JsonPath dispatch structurally unreachable); three matcher modes — exact, wildcard
  (presence of `*`), explicit `regex:` ("regex is the exception" — Eric); **first match wins in
  declaration order**; non-match never errors; mandatory `default`. Targets: `flow://<flow-id>`
  and `task://<route>` (direct function invocation — verbatim header copy + whole payload,
  business cid on the my_cid TAG, per-binding `ttl` default 30s; `task://event.script.manager`
  rejected). **`serializer: 'json'`** (non-schema topics only, XOR schema.enabled): best-effort
  SimpleMapper decode — object→Map, array→List, anything else keeps raw byte[] passed to the
  target as-is (Eric's ruling: no adapter poison handling; a target that can't digest bytes fails
  into the normal retry/DLQ path). **Outbound symmetry:** simple.kafka.notification +
  secondary.kafka.notification (inherits) auto-serialize Map/List bodies to JSON bytes on
  non-schema publishes; the `subject` path keeps its byte[] JSON-document contract. Adversarial
  review round: 13 confirmed findings → 7 fixes, all pinned. Shared surface: twin-kafka's
  secondary adapter constructs the SAME KafkaFlowAdapter → routing + topic-pattern identical on
  both adapters; secondary dead letters ride the secondary publisher. No Rust lock-step constraint
  — the grammar becomes the future minimalist-kafka port's contract. Spec:
  draft-design-specs/second-level-routing-kafka-flow-adapter.md (gitignored). Full detail:
  sessions 2026-07-30-233623 + 2026-07-31-001057. **Demo/migration template (Eric's
  direction — the feature replaces a proprietary field implementation): MERGED
  2026-07-31 as [PR #247](https://github.com/Accenture/mercury-composable/pull/247),
  squash `929a87b9`, CI green — examples/kafka-demo gained a demo.orders binding
  exercising every grammar element beside the direct-routing binding, driven by the
  new publish-orders.js (works piped for scripted regression); smoke-driven live
  end-to-end; README doubles as the manual-regression procedure** (session
  2026-07-31-162554). **Feedback round from Eric's manual regression (session
  2026-08-01-001528, branch fix/kafka-demo-feedback awaiting PR gate):** DLQ topics
  pre-created; `refund <json>` = details + auto-envelope; `order <plain text>` = the
  canonical failure-path trigger (byte[] into a Map-typed function throws back to the
  adapter → retries → DLQ — Eric: exactly proves the parse-failure contract);
  numbered command/example instructions. **Field adoption: the composable
  content-based partitioning pattern (selector function → explicit partition header,
  docs `8ed23f34`) APPROVED for field use 2026-07-31.**
  Relates [[thread-redis-kafka-rpc]].
  <!-- id: thread-kafka-2nd-level-routing | created: 2026-07-30 | last_used: 2026-07-31 | uses: 2 | tier: active | origin: 2026-07-30-233623 -->

- [ ] (observation — surfaced 2026-07-30 by the second-level-routing code study;
  pre-existing, separate from that feature) **Header-casing mismatch: mixed-case Kafka
  headers are unreachable in Event Script data mapping.** Event Script lowercases
  `input.header.*` references (TaskExecutor — matches the HTTP adapter, which ingests
  headers lowercased), but the Kafka flow adapter delivers record headers in original
  wire casing. A producer-sent `Content-Type` Kafka header can never be addressed by
  any `input.header.*` mapping today. Needs a ruling: lowercase at the Kafka adapter
  (HTTP parity — likely a breaking change for flows matching exact casing via `*`
  passthrough) vs case-insensitive header lookup in the engine. The routing feature
  itself dodges the trap (its header-name lookup is case-insensitive by design).
  Relates [[thread-kafka-2nd-level-routing]].
  <!-- id: thread-kafka-header-casing-mismatch | created: 2026-07-30 | last_used: 2026-07-30 | uses: 1 | tier: working | origin: 2026-07-30-233623 -->

- [x] (release — SHIPPED 2026-08-03, **Java only by nature — the Maven dependency surface
  has no Rust counterpart; Rust stays at 4.11.1**) **v4.11.2 — the field lz4-CVE security
  patch + Kafka 4.3.1.** Field Snyk rejected a deployment over CVE-2026-59949 in
  `at.yawk.lz4:lz4-java` (transitive via kafka-clients, no upstream remediation path).
  [PR #253](https://github.com/Accenture/mercury-composable/pull/253), squash `08a31cfa`,
  CI green, tag on the verified squash commit. **Durable lessons:** (1) the repo had
  always excluded the unused lz4 codec, but the exclusions pinned the library's FORMER
  coordinate `org.lz4:lz4-java` and became silent no-ops when Kafka switched to the
  maintained `at.yawk.lz4` fork — **a groupId-pinned exclusion silently expires when
  upstream renames a coordinate**; fixed at all six declaration sites. (2) **lz4 contract
  (Eric):** LZ4 compression is an exception rather than a norm — a field installation
  that needs it adds the dependency itself; the framework ships codec-free. Also executed
  and closed the deferred kafka.version 4.2.0→4.3.1 upgrade (Confluent 8.3.x's tested
  pairing; kafka-standalone's kafka_2.13 now on `${kafka.version}`, no broker/metadata
  skew). Pre-release gates Eric asked for: per-module dependency-tree sweep (zero lz4,
  all 14 Kafka artifacts uniform 4.3.1) + live kafka-demo regression against the 4.3.1
  standalone broker (all routing rules, trace continuity, retries→DLQ — the README
  procedure). Residual observation: the demo's publish helpers have two piped-mode-only
  display/EOF quirks (interactive use unaffected; candidate two-line fix). Full detail:
  origin log.
  <!-- id: thread-release-4-11-2 | created: 2026-08-03 | last_used: 2026-08-03 | uses: 1 | tier: working | origin: 2026-08-03-155225 -->

- [x] (release — SHIPPED 2026-08-01, **Java only — the first Java-ahead-of-Rust release
  since the 4.8.x line, deliberate**) **v4.11.1 — the second-level routing + deadline
  release.** [PR #252](https://github.com/Accenture/mercury-composable/pull/252), squash
  `410e03bb`, CI green, tag on the verified squash commit. Consolidates: second-level
  routing + JSON serde symmetry (#246) with the kafka-demo template (#247/#249, Eric
  field-tested); Redis GETDEL compat (#248); task-ttl override + model-metadata
  immutability + deadline cleanup (#250); Sonar polish (#251). 33-pom sweep clean, no
  substring hazards; prep catch: a broad `git add` staged the local `.interop-mercury`
  dev symlink — amended out and gitignored. **Rust stays at 4.11.0 until the two
  lock-step halves land** (GETDEL + task-ttl/deadline, handoffs final in /tmp);
  second-level routing is Java-only by design (no minimalist-kafka Rust port — the
  grammar is the future port's contract). Rust 4.11.1 ships when the lock-step session
  runs. Full detail: origin log.
  <!-- id: thread-release-4-11-1 | created: 2026-08-01 | last_used: 2026-08-01 | uses: 1 | tier: working | origin: 2026-08-01-230946 -->

- [x] (release — SHIPPED AND PUBLISHED 2026-07-30, both repos in lock-step)
  **v4.11.0 — the suspend/resume feature release.** Java: PR #245, squash `3a870951`, tag on the
  verified merge commit, published. Rust: PR #189 merge `cc529071`, docs-parity fix PR #190 →
  tag MOVED pre-publication onto the docs-inclusive merge `167484bd` (**Eric's rulings: release
  tags must include the updated docs; a tag never moves after publication**), published. Also
  shipped: the skipTests fix (hardcoded `<skipTests>false</skipTests>` silently overrode
  `-DskipTests` in all 26 poms — removed; quick reactor build ~34s), 33-pom sweep, docs-nav
  consolidation on both sites. **Field note (Eric): suspend/resume is CORE functionality for a
  few installations — this surface is regression-critical on both engines.** Full detail:
  origin log + 2026-07-30-173341.
  <!-- id: thread-release-4-11-0 | created: 2026-07-30 | last_used: 2026-07-30 | uses: 1 | tier: working | origin: 2026-07-30-030533 -->

- [x] (feature — COMPLETE across P1-P5, both engines) **Graph suspend/resume: workflow suspension
  for the Active Knowledge Graph.** Design: [[graph-suspend-resume-design]] (ADR-0010). Delivery:
  **P1-P4 MERGED 2026-07-28 as [PR #238](https://github.com/Accenture/mercury-composable/pull/238)**
  (squash `168527ff`; ADR-0010 accepted via the merge; Eric drove three manual-test refinement
  rounds — business-cid fidelity via the my_cid tag, tutorial-14 as a THREE-checkpoint purchase
  workflow, span lineage via worker-thread eRequest in the four Mono-wrapped skills).
  **Production-polish round MERGED 2026-07-29 as
  [PR #240](https://github.com/Accenture/mercury-composable/pull/240)** (squash `4348b0da`;
  ADR-0011 accepted — [[compilegraph-mandatory-gate]]; four-lens adversarial sweep, 15 findings
  fixed incl. the reserved-key strip on restore so a forged store record cannot overwrite
  model.cid; `model.run` joined the reserved flow-metadata family). **P5 Rust lock-step arc
  MERGED 2026-07-30 as mercury PR #186** (five commits; 296 tests/clippy 0; Java-side 5-lens
  consistency review: 22 findings incl. 4 blockers — composite-path forged-record bypass, missing
  instantiate auto-cid, walker seen-marking race, RESERVED_PARAMETERS missing 'suspend' — all
  fixed; live drive vs redis-standalone matched the Java reply contract byte-for-byte; reciprocal
  Java putAll-immunity pin merged via #242). Both engines carry the IDENTICAL surface; released
  in v4.11.0 ([[thread-release-4-11-0]]). Full detail: sessions 2026-07-29-003528,
  2026-07-29-190328, 2026-07-30-030533.
  <!-- id: thread-graph-suspend-resume | created: 2026-07-28 | last_used: 2026-07-30 | uses: 7 | tier: active | origin: 2026-07-29-003528 -->

- [x] (feature — CLOSED 2026-07-26; both PRs merged: Java
  [PR #236](https://github.com/Accenture/mercury-composable/pull/236) squash `6ed481e1` + Rust
  #183; Rust v4.10.6 released same day on the arc) **Ops-tunable worker instances, both engines.**
  Durable facts: ActuatorServices 30 → 5 with NEW `worker.instances.actuator.services` (one knob,
  7 aliases; the Rust port's five per-endpoint actuator services share the SAME key — one runbook
  line tunes both engines); lambda-example event.api.auth 10 → 30. **Ops-tunability principle
  (Eric): declared counts are rules of thumb; operations teams tune in QA/Perf via config before
  promoting to Production.** Doc bug fixed: `worker.instances.<route>` only overrides routes whose
  `@PreLoad` declares `envInstances` (any-route override = `yaml.preload.override`). Full detail:
  origin log.
  <!-- id: thread-ops-tunable-instances | created: 2026-07-26 | last_used: 2026-07-27 | uses: 1 | tier: archive-candidate | origin: 2026-07-27-005415 -->

- [x] (feature — ARC COMPLETE 2026-07-26; P1 merged 2026-07-25 as Java PR #234 squash `265f295d` +
  Rust #181; P2 merged 2026-07-26 as Java PR #235 squash `84c4957f` + Rust #182) **Annotation →
  macro consistency arc.** Delivered: D1 Rust built-ins converted to declarative macros (46
  flipped; positional `#[simple_plugin("name")]` grammar; order-insensitive marker stacking;
  trybuild compile-fail guards); D2 ONE conflict policy both engines (explicit > declarative;
  duplicate = WARN + last-wins); D3a fetch_feature + stacked optional_service; D4
  yaml.preload.override ported to Rust with Java's exact semantics; D5 the Registration Metadata
  Contract ([[registration-metadata-contract]], ADR-0009/ADR-0008 pair, golden vectors shared
  VERBATIM). Deferred by design: D3b plugin gating (**Eric's principle: plugins are Event Script
  capabilities — flow vocabulary, never conditionally on/off**), D6 executionHint. Also: Eric's
  string-semantics ruling (Unicode scalar values in all ports; Java's UTF-16 = documented JVM
  legacy — the F20 UTF-16 retrofit REVERTED with anti-re-retrofit guard); tests/ui fixtures =
  test resources, no license headers. Future ports (Python/Node) start from the contract page +
  vectors. Full detail: origin log + spec
  draft-design-specs/annotation-macro-interop-design.md.
  <!-- id: thread-annotation-macro-consistency | created: 2026-07-25 | last_used: 2026-07-27 | uses: 2 | tier: archive-candidate | origin: 2026-07-25-235904 -->

- [x] (field support — CLOSED 2026-07-26: **field rescan of v4.10.6 PASSED the Sonar gate with a
  perfect Overall-Code score** — 0/0/0/0, coverage 80.5%) **v4.10.4 failed the field Sonar quality
  gate — 5 findings, fixed and released as v4.10.6** (fix PR #231 merge `c7d05d83`, release PR
  #232 merge `2a940250`; Copilot authored, Claude Code reviewed line-by-line + prepared the
  release, Eric gated every step). Findings: 2× S125 (prose comments ending in a stray semicolon
  pattern-match as commented-out code) + 3× S3776 (cognitive complexity — helper extraction,
  behavior-preserving). Verified by full reactor + a live Java-to-Java Event-over-HTTP interop
  drive targeting the three refactored trace/cid classes. Arc shape: rejection → fix → release →
  clean rescan (the [[thread-sonar-4-9-1-field-rejection]] shape). Full detail: origin log.
  <!-- id: thread-sonar-4-10-4-field-rejection | created: 2026-07-25 | last_used: 2026-07-27 | uses: 2 | tier: archive-candidate | origin: 2026-07-25-005125 -->

- [x] (release — CLOSED 2026-07-24 same day) **v4.10.5 security patch SHIPPED in lock-step —
  react-router CVE remediation** (RSC Mode CSRF Bypass; react-router-dom RETIRED upstream at
  7.18.1 → both webapps moved to `react-router ^8.3.0` directly; npm audit 0, 124 tests,
  resources/public rebuilt). Java: PR #230, tag on squash `4c82eae0`. Rust: PR #180, tag on
  `5ae307c2`. **Operational lesson: EMU accounts CANNOT create PRs via API (GraphQL + REST both
  403) — the web UI is the only PR path.** Full detail: origin log.
  <!-- id: thread-release-4-10-5 | created: 2026-07-24 | last_used: 2026-07-24 | uses: 1 | tier: archive-candidate | origin: 2026-07-24-154543 -->

- [x] (release — CLOSED 2026-07-24 same day) **v4.10.4 SHIPPED in lock-step — standards-first
  traceparent carrier + interop header hygiene.** Java: PR #229, tag on squash `0125c17b`
  (verified before tagging). Rust: PR #179, tag on `03424582`. Validated by the ce_traceparent
  four-way drive — all eight echoes identical. **PR-branch lesson: VERIFY which branch a PR points
  at before assuming pushed commits appear in it** (fix commits sat on a second branch; resolved by
  fast-forwarding). Fifth lock-step release of the 4.10 arc. Full detail: origin log.
  <!-- id: thread-release-4-10-4 | created: 2026-07-24 | last_used: 2026-07-24 | uses: 1 | tier: archive-candidate | origin: 2026-07-24-154543 -->

- [x] (feature — CLOSED 2026-07-24; RELEASED in v4.10.4) **Configurable traceparent header name
  (field request).** `http/kafka/secondary.kafka.traceparent.header` + per-entry overrides
  (rest.yaml / kafka-flow-adapter.yaml `traceparent.header`) — renames the CARRIER, not the
  semantics: full W3C context crosses a header-stripping gateway, so cross-app span parenting
  survives (fixes the known limitation in [[thread-field-trace-propagation-4-6-3]]). Outbound =
  stamp BOTH names; **inbound final ruling: the STANDARD traceparent always wins, custom name read
  only when the standard is absent** (presence of the standard means the upstream already
  upgraded). **Standards position in all docs (Eric): W3C/OTel traceparent is the position;
  traceparent.header = backward compat with legacy systems ONLY; departure discouraged.** Both
  engines in lock-step (Java #227, Rust #177) + a hygiene round (both engines scrub the 5 engine
  keys from the delivered envelope view; interceptors keep raw fidelity; x-ttl ingress alignment)
  → ALL EIGHT interop echoes identical. Full detail: origin log.
  <!-- id: thread-traceparent-header-config | created: 2026-07-24 | last_used: 2026-07-24 | uses: 1 | tier: archive-candidate | origin: 2026-07-24-154543 -->

- [x] (release — CLOSED 2026-07-23 same day) **v4.10.3 SHIPPED in lock-step — field-deployment
  roll-up** (releases are immutable per Eric, so post-4.10.2 fixes shipped as a new patch;
  consolidates the whole 4.10 line for field quality gates). Java: PR #226, tag on squash
  `bd7e909d` (verified before tagging — the 4.10.2 tag-race lesson). Rust: PR #176, tag on
  `b3804a67`. Full detail: origin log.
  <!-- id: thread-release-4-10-3 | created: 2026-07-23 | last_used: 2026-07-24 | uses: 1 | tier: archive-candidate | origin: 2026-07-24-023859 -->

- [x] (release — CLOSED 2026-07-23) **v4.10.2 SHIPPED in lock-step** — metadata contract (#221),
  temporary.inbox alignment (Rust #171), team-contributed collection plugins
  isEmpty/getFirst/getLast (#220, reviewed in-session; formal GitHub review API blocked for EMU
  accounts). Java: PR #222, tag `v4.10.2` on `61ddb772` — **lesson: the first tag landed on the
  wrong commit (pull raced the merge); verify what a tag landed on before pushing it.** Rust:
  PR #172, tag on `6a39bccc`, after a deterministic fix for a parallel-test config-freeze CI race.
  Full detail: origin log.
  <!-- id: thread-release-4-10-2 | created: 2026-07-23 | last_used: 2026-07-23 | uses: 1 | tier: working | origin: 2026-07-23-211728 -->

- [x] (feature — COMPLETE 2026-07-23; released in v4.10.2) **Metadata injection/sanitization
  hardening (Eric's 3rd interop round).** Design ruling: function inputs = headers/body/instance;
  headers = envelope-header COPY + metadata INJECTED at entry, SANITIZED at exit; metadata NEVER
  transported in the event. **Durable mechanics: the business cid rides the engine tag `my_cid`
  (EventEmitter.BUSINESS_CID_TAG — a tags wire field, no spec change); the worker injects 4 my_*
  keys at entry (+ legacy-header compat, x-event-api strip) and filters symmetrically at exit;
  HTTP responses echo X-Correlation-Id (function-set header wins).** Rust aligned to Java's single
  reserved private route `temporary.inbox` (retiring its inbox.* namespace reservation; the
  one-record-per-span gate re-keyed to the rpc TAG). Four-way interop re-test passed in full —
  the inbox refactor observably invisible. Both PRs merged (Java #221 `a25d95d5`, Rust #171
  `f86fbec2`). Relates [[conv-telemetry-presentation-parity]]. Full detail: origin log.
  <!-- id: thread-metadata-injection-hardening | created: 2026-07-23 | last_used: 2026-07-24 | uses: 1 | tier: archive-candidate | origin: 2026-07-23-211728 -->

- [x] (release — CLOSED 2026-07-23) **v4.10.1 SHIPPED in lock-step** — telemetry presentation
  parity patch (PR #217 content: /api/event visible span, declarative rename, event.api.auth
  demo, interop report). Java: PR #218, tag on `9ae666df`. Rust: PR #170, tag on `2c4e4066`.
  Full detail: origin log.
  <!-- id: thread-release-4-10-1 | created: 2026-07-23 | last_used: 2026-07-23 | uses: 1 | tier: working | origin: 2026-07-23-145132 -->

- [x] (feature — COMPLETE 2026-07-23; released in v4.10.1) **Post-4.10.0 telemetry presentation
  parity + auth demo (Eric's manual-test findings).** /api/event edge became a visible span
  (EventApiService no longer @ZeroTracing; worker-thread span capture for async callbacks);
  demo→declarative rename; event.api.auth demo (env-secret bearer check, session-info proof).
  Rust mirrored (callback-dispatch refactor, request-scoped context gating, my_* strip).
  **Four-way verification: java-to-java, rust-to-rust, java-to-rust, rust-to-java all EMPTY DIFF
  against the normalized reference signature.** Java PR #217 + Rust #169 merged; both repos' docs
  carry the extended interop report + future-ports playbook. Relates
  [[conv-telemetry-presentation-parity]]. Full detail: origin log.
  <!-- id: thread-telemetry-parity-auth | created: 2026-07-23 | last_used: 2026-07-23 | uses: 1 | tier: archive-candidate | origin: 2026-07-23-145132 -->

- [x] (design — COMPLETED 2026-07-22 with the v4.10.0 release) **Common event envelope wire format
  for cross-language interop (Event over HTTP with the Rust port).** The durable contract:
  standard envelope = MsgPack map with descriptive string keys (the `toMap()` form promoted to a
  wire contract); compact 1-char and standard ≥2-char keys are disjoint → decoders sniff both, no
  negotiation; **default = `standard`** (Event over HTTP is transport, not storage; `compact` kept
  as explicit fallback) via `event.over.http.format` + per-request `x-event-format`; Java
  `exceptionBytes` excluded — portable error = status + message + stack text. API:
  `Format {COMPACT, STANDARD}`, `toMap(Format)`/`toBytes(Format)` (no-arg forms preserve legacy
  behavior), sniffing `load()`. **Golden vectors** at
  system/platform-core/src/test/resources/envelope-vectors/vectors.json, shared byte-identical
  with the Rust port — the golden-vector conformance method's first application. Phase 1 Java
  PR #212; Rust increments 59-62 reviewed for consistency (high fidelity, no blockers); live
  bidirectional interop 7/7 both directions (the drive found + fixed real bug D1:
  getTimeoutSeconds floor-division → 1s HTTP read timeout, PR #214; Rust D2/D3 fixed, declarative
  `yaml.event.over.http` added). Released in v4.10.0; permanent record at
  docs/test-reports/event-over-http-interop.md. → serves `vision-mercury-composable` (polyglot
  deployment). Full detail: origin log + sessions 2026-07-22-*.
  <!-- id: thread-event-envelope-interop | created: 2026-07-21 | last_used: 2026-07-25 | uses: 7 | tier: archive-candidate | origin: 2026-07-21-215951 -->

- [x] (field support — CLOSED 2026-07-26 by the review; close condition subsumed by the v4.10.6
  perfect-score rescan, see [[thread-sonar-4-10-4-field-rejection]]) **v4.9.1 REJECTED by the
  field Sonar quality gate; remediated in v4.9.2** (19 issues from the 4.9.0/4.9.1 minigraph
  companion/discovery code — S3776/S1192/S5778/S125/S1168/S5961/S6126 — all fixed
  behavior-preserving via helper extraction, constants, test splits, prose rewording; PR #210,
  v4.9.2 tag on `b574f41e`). **Durable lesson: extract-as-you-go when touching methods near the
  S3776/S5961 thresholds.** Per-issue map: session 2026-07-21-173614.
  <!-- id: thread-sonar-4-9-1-field-rejection | created: 2026-07-21 | last_used: 2026-07-27 | uses: 4 | tier: archive-candidate | origin: 2026-07-21-173614 -->

- [ ] (field support — 2026-07-13; ROOT CAUSE FOUND) **Trace-propagation report: the internal API
  gateway strips `traceparent` AND `X-Trace-Id` (neither on its allow-list); only
  `X-Correlation-Id` passes.** 4.4.11 "worked" because the legacy conflation rode the allow-listed
  header. A proposed `legacy.trace.id` flag was **REJECTED by Eric — re-mixing the business cid
  with the trace id makes things worse**; THE fix = gateway allow-list change (traceparent +
  X-Trace-Id), which Eric took to the infra team. Interim: the field runs legacy conflation
  (`http.trace.id.header` = `http.correlation.id.header` = X-Correlation-Id) — safe when the edge
  supplies the header; the absent-header divergence was FIXED (PR #179: colliding names + absent
  header → ONE id, trace authoritative, both ingress paths) and validated live by Eric. Support
  nuance: with conflation the outbound trace id rides the configured header name; traceparent is
  stamped only for W3C-shaped (32-hex) ids — cross-app SPAN parenting still needs traceparent, so
  tooling stitches by trace id until the gateway passes it. **Pending: gateway team's allow-list
  change (asked 2026-07-14; Eric updates after the devops cloud-dev test).** Diagnosis +
  checklist: [[field-trace-propagation-4-6-3-diagnosis]]. Full detail: origin log.
  <!-- id: thread-field-trace-propagation-4-6-3 | created: 2026-07-13 | last_used: 2026-07-24 | uses: 7 | tier: working | origin: 2026-07-13-142021 -->

- [ ] (P0–P5 code-complete — 2026-07-05, branch `feature/elastic-queue-file-fifo`, submitted as
  [PR #137](https://github.com/Accenture/mercury-composable/pull/137); remaining = field canary →
  P4 retire-BDB) **Replace ElasticQueue's Berkeley DB spill tier with a portable file-backed
  segmented FIFO.** State: `ElasticQueue` = facade over an `ElasticStore` strategy; **ONE switch**
  `elastic.queue.store` (`file` ⇒ per-route virtual-thread dispatch, `bdb` ⇒ event loop — derived,
  vthread+bdb unreachable since BDB pins VT carriers); **default flipped to `file`** (Eric chose:
  fold into #137 + re-frame). Results: throughput +56%, write p99.9 ~47× better, stalls>20ms
  90→3; the one `file` blemish is a rare OS dirty-page-flush outlier (document tmpfs). The
  `elastic.queue.cleanup` BDB work is `@KernelThreadRunner`-isolated (closes a live in-field
  VT-pinning vector). Copilot review hardening applied (bounded dispatch mailbox with no-drop
  back-pressure, O(1) segment channels, stale-dir cleanup). `benchmark/benchmark-reporter` module
  added — self-contained field A/B harness → HTML report. **Next = field steps: run
  benchmark-reporter on real envs, then P4 retire BDB.** Design spec + field notes: gitignored
  draft-design-specs. Docs/ADR sync: [[thread-elastic-queue-docs-adr]]. Full detail: origin log.
  <!-- id: thread-elastic-queue-bdb-to-file | created: 2026-07-05 | last_used: 2026-07-06 | uses: 12 | tier: working | origin: 2026-07-05-033922 -->

- [ ] (backlog — do at ElasticQueue merge / P4) **Docs sync + ADR for the ElasticQueue file store /
  off-loop dispatch.** Deferred deliberately: nothing in the current guides is wrong today, and the
  config surface is still moving (P4 retires BDB → removes `deferred.commit.log`, the
  `elastic.queue.cleanup` reserved route, and collapses `elastic.queue.store`). At merge/P4:
  configuration-reference (segment size key, final store surface, tmpfs tip), reserved-names
  (drop elastic.queue.cleanup), architecture.md (overflow-buffer line), propose an ADR
  (human-gated), graduate the field notes into the PR/runbook.
  Relates [[thread-elastic-queue-bdb-to-file]], [[elastic-queue-file-fifo-plan]].
  <!-- id: thread-elastic-queue-docs-adr | created: 2026-07-05 | last_used: 2026-07-05 | uses: 1 | tier: working | origin: 2026-07-05-033922 -->

- [ ] (planned — backlog, no ETA) **Reintroduce Protobuf support in minimalist-kafka's Schema
  Registry integration.** Blocked on Confluent adopting the renamed
  `com.squareup.wire:wire-runtime` coordinate in `kafka-protobuf-provider` (unchanged as of 8.3.0,
  checked 2026-07-01 — re-check on any future Confluent release). Alternative unblocks: (a) vendor
  a patched `wire-runtime-jvm` fork (the upstream fix is a one-liner); (b) a field installation
  explicitly needs Protobuf and accepts the residual CVE-2026-45799 risk. What to restore:
  [[minimalist-kafka-protobuf-removed]] (all still in git history).
  <!-- id: thread-minimalist-kafka-protobuf-revival | created: 2026-07-01 | last_used: 2026-07-01 | uses: 1 | tier: working | origin: 2026-07-01-224313 -->

- [ ] (backlog — Eric, 2026-07-02; needs its own design pass) **`CompileGraph` does not carry out
  comprehensive syntax validation** for the mapping-string mini-DSL (the one
  `DataMappingHelper`/`SimpleTypeMatchingConverter` handle). Open questions: what "comprehensive"
  means (malformed plugin calls? unknown plugin names? arg-count/type checks?), and whether to
  reuse or diverge from event-script-engine's own `validInput`/`validOutput` (already diverges —
  minigraph's per-skill namespace rules don't match event-script's). Landing pad exists:
  `GraphModelValidator` ([[compilegraph-mandatory-gate]]).
  <!-- id: thread-compilegraph-syntax-validation | created: 2026-07-02 | last_used: 2026-07-29 | uses: 4 | tier: working | origin: 2026-07-02-004606 -->

- [x] (planned — **CLOSED 2026-08-03: executed by the v4.11.2 release**, which gained a CVE
  driver after all — see [[thread-release-4-11-2]]; all poms now pin 4.3.1, validated by the
  full reactor + embedded KRaft broker + a live kafka-demo drive) **Upgrade `kafka.version`
  (4.2.0 → 4.3.x) across the 24 pom.xml files that pin it.** Deferred alongside the
  `confluent.version` 8.2.0→8.3.0 bump — see [[minimalist-kafka-confluent-8-3-0]]. Scope when
  picked up: verify kafka-clients 4.3.x + the embedded KRaft broker behavioral compatibility
  across all 24 files — a materially larger test surface than a serializer-library bump.
  <!-- id: thread-kafka-client-version-upgrade | created: 2026-07-01 | last_used: 2026-07-01 | uses: 1 | tier: working | origin: 2026-07-01-230246 -->

- [ ] (planned — Eric, 2026-06-24) **Add Gradle build support** alongside the existing Maven reactor
  (Maven stays the current build tool; see `stack-build-maven`). Scope TBD — likely a parallel Gradle
  build for the multi-module project.
  <!-- id: thread-add-gradle-build | created: 2026-06-24 | last_used: 2026-06-24 | uses: 1 | tier: working -->
- [ ] (docs backlog — Eric, 2026-06-24) **Documentation improvement — serve both audiences, every
  sprint.** Acceptance criteria for every doc change: **for humans — storytelling** (engaging,
  why-before-how, a narrative arc); **for AI agents — token-efficient** (shortest path to the
  point, machine-greppable, "generate from this page alone"). Backlog (from two fresh-agent
  discovery passes, 2026-06-24; re-validate each pass with the fresh-agent test):
  - **Biggest gap: an AI-agent "boot & test an app" recipe** (AutoStart.main, minimal
    application.properties, `@PreLoad` base-package auto-scan, the AsyncHttpRequest contract,
    server-readiness via `Platform.waitForProvider("async.http.response")` not Thread.sleep).
  - **Surface the working test-fixture pattern** (TestBase + service function + test rest.yaml) —
    highest-signal context, lives only in src/test.
  - **Machine-readable runtime-API signatures** (like the DSL *.json catalogs) for
    AsyncHttpRequest/AutoStart/AppConfigReader; and surface the catalogs in llms.txt as
    first-class entries (+ repo-relative links so an in-repo agent maps page→file in one hop).
  - **Reserved-route extension contract + machine-readable dataset schema** for
    `distributed.trace.forwarder` / `transaction.journal.recorder` (trace-metrics map shape).
  - **"Author a reusable extension" recipe + the auto-registration fact** (`@PreLoad` under
    `org.platformlambda.*`/`com.accenture.*` is always scanned — jar on classpath = route).
  - **Document `${ENV_VAR:default}` config substitution** (unset `${VAR}` with no default → null).
  - **Drive an Event Script flow programmatically** (`FlowExecutor.request(...)`) + the synthetic
    `task.executor` flow-summary span.
  → serves `vision-mercury-composable`. Full detail: sessions of 2026-06-24.
  <!-- id: thread-docs-improvement-backlog | created: 2026-06-24 | last_used: 2026-07-20 | uses: 4 | tier: working -->
- [ ] (next iteration — Eric, 2026-06-24; **design + implement**) **Cross-pod request-response via
  Redis Pub/Sub RPC + Kafka** — a distributed sync-over-async pattern (advanced opt-in, cf.
  `kafka-mesh-opt-in`): REST sync request → POD-1 → Kafka outbound; Kafka inbound (response) →
  POD-2 → Redis return route keyed by correlation-id back to POD-1. **Locked decisions:** return
  path = Redis (deliberately NOT the full mesh/presence discovery); client = Lettuce; module =
  `extensions/sync-over-async`; reliability cornerstones — Redis SETEX payload is the source of
  truth, pub/sub is wake-up only, a final Redis read before timeout is required; race-safe
  idempotent completion. **State: MVP COMPLETE (2026-06-26) and the Kafka legs promoted to the
  reusable `system/minimalist-kafka` library** (Eric's call) — sync-over-async is now purely the
  Redis return-route engine (96% cov). Full round-trip proven incl. OTel span propagation across
  Kafka (notification stamps its own span into traceparent; consumer chains the flow onto it).
  Subsequent minimalist-kafka growth (topic-pattern, dlq-topic, auto-commit, metadata.*,
  cid-header fix, terminology refactor businessCorrelationId/internalCorrelationId — Java
  identifiers only, wire strings unchanged) delivered via PR #133 + review rounds. **Still open
  (post-MVP): 503 guardrails/metrics, two-JVM test, per-module README; Gradle build
  ([[thread-add-gradle-build]]).** → serves `vision-mercury-composable`. Full detail: sessions
  2026-06-25 → 2026-07-04.
  <!-- id: thread-redis-kafka-rpc | created: 2026-06-24 | last_used: 2026-07-31 | uses: 8 | tier: working -->

- [x] (CLOSED 2026-07-27 — **ALL 15 CONFIRMED by Eric** in a one-by-one walkthrough with fresh
  live-tree evidence per item: 5 stack facts, 3 architectural invariants, 6 core
  conventions/gotchas, + the Vision; several now carry stronger guarantees than when written;
  Vision current-state refreshed; cadence reset) **Re-verify invariants (was due — 50 sessions ≥
  verify_invariants_every 40).** The never-decay set: the 5 `stack-*` facts;
  `functions-decoupled-routes`, `typed-io-map-or-pojo`, `virtual-threads-rpc`;
  `trace-thread-keyed-mono-gotcha`, `instant-serialization`, `kafka-mesh-opt-in`,
  `event-script-over-code`, `conv-add-capability`, `conv-serialization-gotchas`; and the Vision
  (`memory/vision.md`). Detail: session 2026-07-27-215011.
  <!-- id: thread-reverify-invariants-2026q2 | created: 2026-06-29 | last_used: 2026-07-27 | uses: 2 | tier: archive-candidate -->

## User Preferences

## Team / Members

(none recorded yet)
