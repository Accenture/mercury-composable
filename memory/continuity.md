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
- **last_session:** 2026-06-28T18:21:25Z | agent: Claude Code
- **last_review:** 2026-06-28 | through 2026-06-28-173142.md
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
  <!-- id: adr-pattern-adopted | created: 2026-06-22 | last_used: 2026-06-27 | uses: 6 | tier: archive-candidate -->
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
- **Code-style conventions have a documentation home.** Soft, evolving code-organization/naming
  recommendations live in `docs/guides/code-conventions.md` — a new page, sibling to
  `documentation-conventions.md` in the nav meta area, linked from `llms.txt` (Reference). Altitude tier:
  **below** ADRs (durable decisions) and `methodology.md` (the 4 principles) — if breaking a guideline
  breaks the system, promote it to an ADR instead. Seeded with: group Event Script flow-task functions
  under a `tasks` package (e.g. `org.platformlambda.tasks`) while runtime/coordinator classes stay in the
  feature package and config in `support`/`config`; plus route-naming discipline and function granularity.
  Add future code-style recs here. Established 2026-06-27 (Eric + Claude Code).
  <!-- id: code-conventions-home | created: 2026-06-27 | last_used: 2026-06-28 | uses: 3 | tier: archive-candidate -->
- **Standalone dev servers live in `helpers/`; worked examples teach the patterns.** `helpers/` (new
  top-level folder, 2026-06-27) holds no-Docker standalone dev servers: `kafka-standalone` (moved here from
  `connectors/adapters/kafka/`) and `redis-standalone` (embedded Redis via `embedded-redis`). Both pin
  transient working files to **`/tmp/soa-redis`** / `/tmp` (cloud-native pattern); the sync-over-async
  `RedisTestBase` uses the same `/tmp/soa-redis` dir. `examples/kafka-demo` is the minimalist-kafka
  producer+consumer **worked example** (Java flow + kafkajs Node programs: create-topics/listen/publish),
  validated live end-to-end. `examples/sync-over-async-demo` is the **sync-over-async worked example** (done
  2026-06-28): one jar, two pods via Spring profile (`-Dspring.profiles.active=facade|backend`), cross-pod
  REST-over-Kafka with a Redis return route (ADR-0006); promoted `soa.reply` into the extension. Worked
  examples are how this project teaches pattern adoption (Eric). On `feature/sync-over-async` (PR #124).
  <!-- id: helpers-and-worked-examples | created: 2026-06-27 | last_used: 2026-06-28 | uses: 3 | tier: archive-candidate -->
- **Examples are kept deliberately minimal (avoid drift).** Bare-minimum examples on principle (Eric:
  "minimalist is our design principle; too many examples drift thinking"). Retired `csv-flow-adapter` +
  `csv-flow-demo` (2026-06-27, were not in the reactor + drifting): the built-in **HTTP flow adapter**
  (sync) + minimalist-kafka's **`KafkaFlowAdapter`** (async) sufficiently demonstrate the flow-adapter
  pattern, and `KafkaFlowAdapter` is the reference for writing a custom adapter (the "Writing your own Flow
  Adapters" section of `actuators-and-http-client.md` now points there). Don't add a new example without a
  clear, non-redundant teaching purpose. Relates to [[helpers-and-worked-examples]].
  <!-- id: minimalist-examples | created: 2026-06-27 | last_used: 2026-06-28 | uses: 1 | tier: archive-candidate -->
- **Kafka flow-adapter consumer error semantics = bounded retry → per-topic dead-letter, commit-gated on a
  confirmed write (Eric, 2026-06-26; design grounded in web research).** When an Event Script flow fails for a
  consumed message, `KafkaFlowConsumer` retries up to `kafka.flow.max.retries` (default 3, with
  `kafka.flow.retry.backoff.ms` default 500ms), then writes the original message (headers preserved +
  `dlq.origin.topic` / `dlq.error`) to its **per-topic** DLQ `<topic><suffix>`. **The DLQ is strictly
  per-topic** — a global/shared DLQ is an **anti-pattern** (mixing source schemas makes reprocessing, the whole
  point of a DLQ, hard; confirmed by Confluent/Spring/Redpanda research). Only the **suffix** is configurable
  via `kafka.flow.dlq.suffix` (default `.dlq`; orgs vary — `-DLQ`, `.DLT`, …; a blank suffix falls back to
  `.dlq` so the DLQ can never equal the source topic). The DLQ write is **confirmed** (`publishSync` blocks on
  broker ack); the offset is committed **only if that write succeeds** — otherwise the message is **not**
  committed and **redelivers** (no silent loss). So **DLQ topics must be pre-provisioned** (Kafka
  auto-creation is off in prod; producing to a missing topic blocks `max.block.ms`≈60s then fails → a missing
  DLQ stalls the partition loudly rather than dropping data — the correct trade-off for a reprocessing holding
  area). This evolved through three steps the same day: original = log+commit (silent loss); then a brief
  `kafka.flow.dlq.topic` global override (**reverted** — anti-pattern); now per-topic suffix + commit-gating.
  Wired via the `RetryPolicy` record (`maxRetries`, `backoffMs`, `dlqSuffix`, `deadLetterPublisher`) through
  `KafkaFlowAutoStart` → `KafkaFlowAdapter` → consumer, reusing the shared `KafkaRequestPublisher`. From the
  Copilot review (mk#2) + Eric's reprocessing-correctness pushback.
  **Enforcement boundary (Eric, 2026-06-26 — why this lives in the library):** baking retry → DLQ → commit-gating
  *into* `minimalist-kafka` makes "no silent loss" **structural/enforced** for every consumer — a doc-level
  recommendation could not guarantee it. The library's responsibility ends at **durable capture** (the message
  lands in its per-topic DLQ holding area); **reprocessing** (read `<topic>.dlq` → fix → replay) is
  **per-business-domain logic, deliberately out of scope** — schemas and remediation differ per topic, so it is
  not enforceable by the library. Responsibility line: **library = durable capture (enforced); domain =
  reprocessing (not enforced).**
  <!-- id: kafka-flow-failure-dlq | created: 2026-06-26 | last_used: 2026-06-27 | uses: 9 | tier: archive-candidate | origin: 2026-06-26-230722.md -->
- **sync-over-async production bootstrap = config-driven init (Eric, 2026-06-26).** The module's `main` was
  previously only the engine; `RedisClient` + coordinator were wired in test code (hardcoded
  `redis://127.0.0.1`, `pod-mvp`). Built the production path mirroring minimalist-kafka's
  `KafkaFlowAutoStart`/`KafkaRuntime`: **`SyncOverAsyncAutoStart`** (`@MainApplication`, **opt-in** via
  `sync.over.async.enabled=true`, default **false** because the coordinator **eagerly connects** to Redis) →
  reads config → builds `RedisClient` + `ReturnRouteCoordinator` keyed by `Platform.getInstance().getOrigin()`
  → `start()` → stored in **`SyncRuntime`** holder (coordinator + client + `shutdown()`). **Redis startup params
  = discrete properties** (Eric's choice over a single URI): `redis.host`/`redis.port`/`redis.password`/
  `redis.ssl`/`redis.database`/`redis.timeout.ms` → `RedisConfig.from(ConfigBase)` → Lettuce `RedisURI`. Engine
  tunables via `SyncOverAsyncConfig.from(ConfigBase)`: `sync.return.channel.prefix`/`sync.route.ttl.seconds`/
  `sync.response.ttl.seconds`/`sync.max.pending.requests`. The coordinator must **not** own the shared client
  (engine tests share one client across two coordinators), so `SyncRuntime.shutdown()` owns client teardown.
  MVP test rewired to boot through the production autostart (embedded Redis injected via the `redis.port`
  system property). **Config gotcha:** `ConfigBase` lives in `org.platformlambda.core.util.common`, and
  `ConfigReader.get()` does dotted-path traversal (flat dotted map keys do NOT resolve), so unit tests feed the
  `from()` loaders via a small in-memory `ConfigBase` double (`MapConfig`), not `ConfigReader.load(Map)`.
  Closes the "Redis coordinator config-driven init" post-MVP item in [[thread-redis-kafka-rpc]].
  <!-- id: soa-config-driven-init | created: 2026-06-27 | last_used: 2026-06-27 | uses: 4 | tier: archive-candidate | origin: 2026-06-27-000648.md -->
- **Kafka client config is externalized to template files, not hard-coded (Eric, 2026-06-27).** The
  minimalist-kafka producer/consumer settings now come from **`kafka-producer.properties` +
  `kafka-consumer.properties`** templates in `system/minimalist-kafka/src/main/resources/`, loaded by
  `KafkaClientConfig` via `ConfigReader` from a **file→classpath fallback** location
  (`file:/tmp/config/<name>,classpath:/<name>`, overridable via `kafka.producer.properties` /
  `kafka.consumer.properties`) with `${ENV_VAR:default}` substitution. **Why:** enterprise Kafka installs
  vary enormously (on-prem/cloud/SaaS/Confluent; SASL/PLAIN, SASL/SCRAM, OAuth2 client-id+secret, mTLS) —
  encoding every variation in code is untenable; ops fill in a template (or drop one at `/tmp/config` via
  CI/CD), code overrides only critical params. **Pinned in code (override the template):** producer
  key/value serializers; consumer deserializers + `enable.auto.commit=false` + `max.poll.records=1` —
  the byte[] wire contract + at-least-once design.
  Everything else (bootstrap, security.protocol, sasl.*, ssl.*, acks, auto.offset.reset) = template.
  **Consumer `group.id` is configured per binding** in `kafka-flow-adapter.yaml` via an optional `group:`
  field (under the `consumer` section — the `consumer_` prefix would be redundant), **used exactly as given**
  (enterprise DevSecOps assigns topics/ACLs/groups administratively → no suffix/decoration; Eric, 2026-06-27);
  when omitted it defaults to `kafka-flow-adapter.<topic>` for dev. The YAML is loaded by `ConfigReader`, so
  `group` (like any value) supports `${ENV_VAR:default}` substitution — `resolveReferences()` bakes resolved
  values into the nested list at load time, so `entry.get("group")` returns the resolved value. Resolved by
  `KafkaFlowAdapter.resolveGroupId`; the consumer template intentionally omits `group.id`.
  **bootstrap.servers is template-only** via `${KAFKA_BOOTSTRAP_SERVERS:127.0.0.1:9092}` — the
  `kafka.bootstrap.servers` code property was **removed**; tests inject the embedded broker via the
  `KAFKA_BOOTSTRAP_SERVERS` **system property** (resolved by the placeholder: env var → system prop/app
  config → default). Mirrors the cloud connector's `kafka.properties` convention (`KafkaConnector`/
  `getCompositeKeyValues()`), split into producer/consumer. `KafkaFlowAdapter` now takes the base consumer
  `Properties` (adds the per-topic group.id) instead of a bootstrap string. Test-config location keys listed
  per [[pref-explicit-test-config]]; shares the producer with [[kafka-flow-failure-dlq]] (the DLQ writer).
  <!-- id: kafka-client-config-templates | created: 2026-06-27 | last_used: 2026-06-27 | uses: 5 | tier: archive-candidate | origin: 2026-06-27-005108.md -->
- **Kafka consumer partition pinning = opt-in per binding (Eric, 2026-06-27).** A `kafka-flow-adapter.yaml`
  consumer entry may include an optional `partition:` field. When present, `KafkaFlowConsumer` manually
  **`assign`s** that single `TopicPartition` instead of group-managed **`subscribe`** — bypassing consumer-group
  rebalancing so the pinned consumer reads exactly that partition; offsets still commit under the configured
  group. When absent, behaviour is unchanged (subscribe). The operator owns the deployment model (one consumer
  per partition, or each pod pinning a distinct partition via `partition: ${POD_PARTITION}` — the value is
  `${ENV_VAR:default}`-substitutable like the rest of the YAML). Parsed by `KafkaFlowAdapter.parsePartition`
  (non-negative int, else `IllegalArgumentException`); applied via `KafkaFlowConsumer.subscribeOrAssign`
  (both seams unit-tested with `MockConsumer`/maps, no broker). Closes the "consumer partition-pinning"
  post-MVP item in [[thread-redis-kafka-rpc]]. Extends [[kafka-client-config-templates]].
  <!-- id: kafka-partition-pinning | created: 2026-06-27 | last_used: 2026-06-27 | uses: 3 | tier: archive-candidate | origin: 2026-06-27-020039.md -->
- **minimalist-kafka + sync-over-async are documented in mkdocs (2026-06-27).** Two new published guides under
  **Operate & integrate**: `docs/guides/kafka-flow-adapter.md` (the library — adapter YAML schema
  topic/flow/group/partition, externalized producer/consumer client templates, consumer group, partition
  pinning, retry→DLQ, `simple.kafka.notification`, trace continuity) and `docs/guides/sync-over-async.md` (the
  cross-pod Redis return-route pattern, reliability cornerstones, when-to-use vs the service mesh). All new
  config keys added to `configuration-reference.md` under `#kafka-flow-adapter` + `#sync-over-async`. Nav
  (`mkdocs.yml`) + `docs/llms.txt` updated; conforms to doc canon (frontmatter/At-a-glance/See-also, banned
  terms). **Local mkdocs validation:** mkdocs is **not** a repo dependency (CI does `pip install mkdocs`); run
  `uv run --with mkdocs mkdocs build --strict` (theme=readthedocs, plugins=search — both built-in; no extras).
  Validated: doc-canon OK + `--strict` clean. Closes the "module docs" post-MVP item in [[thread-redis-kafka-rpc]].
  Documents [[kafka-client-config-templates]], [[kafka-flow-failure-dlq]], [[kafka-partition-pinning]],
  [[soa-config-driven-init]].
  <!-- id: kafka-soa-docs | created: 2026-06-27 | last_used: 2026-06-27 | uses: 3 | tier: archive-candidate | origin: 2026-06-27-022232.md -->
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
  <!-- id: agent-memory-upgrade-v4261 | created: 2026-06-28 | last_used: 2026-06-28 | uses: 1 | tier: working | origin: 2026-06-28-182125 -->

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
  <!-- id: agent-memory-upgrade-v4250 | created: 2026-06-28 | last_used: 2026-06-28 | uses: 1 | tier: working | origin: 2026-06-28-173142 -->

- [ ] (planned — Eric, 2026-06-28; **next challenge**) **Schema Registry feature.** The richer Kafka
  payload-encoding layer that sits on top of minimalist-kafka's `byte[]` building block — `simple.kafka.
  notification`'s javadoc already names "a Confluent Schema Registry" as the intended layer-on-top. Scope
  to be designed next session. Builds on [[helpers-and-worked-examples]] / the minimalist-kafka work.
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

## User Preferences

## Team / Members

(none recorded yet)
