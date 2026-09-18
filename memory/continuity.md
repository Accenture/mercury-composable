# Continuity — mercury-composable

> Shared ground truth for project state across all agents and sessions.
> Update at the end of every session. Never delete — only archive (see `REVIEW.md`).
>
> Each fact carries a metadata footer in an HTML comment, maintained by the review
> ritual — invisible when rendered, read/written by agents:
> `<!-- id: <kebab-id> | created: YYYY-MM-DD | last_used: YYYY-MM-DD | uses: N | tier: active -->`
> (the id is a placeholder on purpose — a literal id here is parsed as a live fact and
> inflates the review's fact count; see `conv-schema-example-not-a-fact`)
> See `.agent/schema.md` for the fields and `memory/decay-policy.md` for the windows.
> Condensed 2026-07-31 (line-bloat review advisory): shipped-item narrative compressed
> to essentials; full detail lives in each fact's `origin` session log.

---

## Project State

- **project:** mercury-composable
- **status:** active, mature framework (Maven reactor)
- **repo:** github.com/Accenture/mercury-composable (official — source of truth)
- **latest_release:** v4.12.12 (2026-09-17 — the produce-only unblock, **1 fix PR #409** via release
  PR #410, squash `ebdd2e37`, tag `v4.12.12`, pom verified at the tag): **`kafka.health` works on a
  produce-only leg** (`kafka.consumer.enabled=false`), where v4.12.11's classloader fix covered only
  half the path — Kafka resolves class-valued config DEFAULTS in its config classes' STATIC
  INITIALIZERS, reached while resolving the template, before any client is built
  ([[kafka-config-class-static-init-loader]]). **ACTION TO READ, not to take:** no action to
  configure, no wire/API/config-key change. An app on a produce-only leg whose `/health` answered a
  raw **500** now answers correctly; consumer-enabled apps were unaffected by both the defect and the
  fix. **Java only — no lockstep** (JVM classloading has no Rust analogue); the outstanding lockstep
  is still v4.12.9's distributed cache ([[ot-distributed-cache]]). Sweep surface: BUILD FILES ONLY,
  **43 files / 98 occurrences** — unchanged from v4.12.11 because the fix touched no poms, and
  re-derived rather than carried forward (never trust the prior count). Prior: v4.12.11 (2026-09-16 21:03Z — the field-unblock release, **3 PRs #402–#404**
  via release PR #405, squash `06750214`, tag `v4.12.11`, pom verified at the tag): **`kafka.health`
  builds its probe client regardless of the thread context classloader** — the field bug it exists
  for, where a pooled kernel thread's loader could not see `kafka-clients` while the same JVM's real
  clients were fine ([[kafka-class-objects-over-names]]); **OpenTelemetry forwarding is now opt-in
  and certified against Dynatrace** (`otel.forwarding`, default off, `@OptionalService`; legacy
  `otel.trace.forwarder.enabled` RETIRED; exporter moved onto [[platform-onshutdown-lifecycle]] —
  [[otel-optional-service-and-negative-control]]). **ACTION TO READ, not to take:** unlike v4.12.10
  this is NOT a no-action release — an app whose Kafka template genuinely cannot build a client was
  reporting `/health` green and now answers 503 `Kafka client configuration is unusable`. That is the
  correction; nothing else changed in wire, API or config keys, and the retired OTel key cannot
  surprise anyone into exporting (its users land on the new switch's default of off). **Java only —
  no lockstep needed** (a JVM classloading concern has no Rust analogue); the outstanding lockstep is
  still v4.12.9's distributed cache ([[ot-distributed-cache]]). Ports adopt this number on catch-up
  ([[conv-ports-adopt-java-release-number]]). Sweep surface: BUILD FILES ONLY, **43 files / 98
  occurrences** (97 → 98 when #404 added a forwarder dependency — re-derive the sweep after a rebase,
  never trust the prior count). **CLOSED 2026-09-17:** Dynatrace support confirmed both
  field-acceptance traces in the UI, `OTel scope version` **4.12.11** on each — so the released
  artifacts, not a leftover build, submitted them. Scenario 6 updated (PR #412); the OTel
  certification is closed end to end ([[ot-otel-acceptance-traces-pending]]).
  **FIELD REGRESSION, now fixed:** the `kafka.health` half of this release was **incomplete** — a
  produce-only leg (`kafka.consumer.enabled=false`) still answered `/health` with a raw 500, because
  the loader override did not cover template resolution. **Corrected in v4.12.12** (above,
  [[kafka-config-class-static-init-loader]]); read this release's Kafka claim as consumer-path only.
  Prior: v4.12.10 (2026-09-16 04:01Z — the cleanup release, 2 PRs #399–#400, squash `92e94f2b`:
  Berkeley DB elastic-queue store RETIRED (ADR-0024), `ServiceQueue` collapsed to ONE dispatch mode
  ([[elastic-queue-file-store]]), separate-Redis-client guidance, and `BENCHMARK-LOG.md`; no upgrade
  action). Prior: v4.12.9 (2026-09-16 01:29Z — the distributed-Redis release, 34 PRs #364–#397,
  squash `9274cf92`: streaming return route, distributed cache, clustered Redis,
  `Platform.onShutdown`, MsgPack `packMapOrList`/`unpackMapOrList`, three-layer cache example +
  dev-mode template, the MiniGraph async-callback guard, three field Snyk fixes; action required
  there: the `soa.redis.health` ROUTE rename and `minimalist-kafka` no longer transitive). The live
  version source stays the root pom.xml.)
- **last_enabled:** 2026-06-20
- **last_review:** 2026-09-18 | through 2026-09-18-174943.md (ON COMMAND — 3 sessions since,
  review_every 10. **Nothing archived, nothing swept — and that is the correct outcome**: the oldest
  non-core fact and the oldest closed thread both sit at sslu 18, inside `archive_window` 20 (three
  threads at 17–18 will sweep next time). **The trigger itself was FALSE** — `memory-lint` parses the
  schema EXAMPLE in continuity.md's own header as a live fact, so `[continuity-bloat] 36 > 35` was
  really 35, exactly at the cap; proven by linting a copy with that one line neutralized (58 live /
  36 eligible / 1 warning → 57 / 35 / 0). Fixed here by making the example id a placeholder and
  reported upstream — see [[conv-schema-example-not-a-fact]]. Tier changes 0 (refresh-metadata already
  matched the reference log); reactivated 0; superseded 0; archive-verify pass. Invariant re-verify
  NOT due (15 sessions since, cadence 40). Stalled threads: none (4 unchecked, oldest at 6, window 40).
  Doc-gap sweep run for [[thread-doc-improvement-feedback-loop]]: one gap still open (graph.task
  OUTPUT-mapping LHS) plus four stale store-key surfaces left by `fb171c18`. **Left deliberately at
  36/35:** removing the phantom put the true count at 35, and this review's own new fact made it 36 —
  genuinely over, with nothing yet archivable. **Resolved later the same day:** three more session logs
  pushed four entries past the window, and the sweep took exactly ONE — `ot-redis-client-separation`, a
  CLOSED thread (step 5, textbook). The other three (`soa-redis-cluster-support`,
  `cache-separate-from-soa`, `redis-connection-foundation`) are **held deliberately** and will keep
  reporting `[overdue]`: each is cited by the still-open [[ot-distributed-cache]], whose Rust lockstep
  is pending, so they are the design record of live work rather than faded context. Sweep them when
  that thread closes, not before — the metric measures *reference recency*, which is not the same as
  *still needed*.
  Prior: 2026-09-17 | 2026-09-17-020650.md)
- **vision_evolved:** 2026-09-17 (Eric approved) — `memory/vision.md` now states **two tracks**:
  Track 1 *knowledge graph as application* (deterministic — rules, business logic, outcome; L3
  leverages L2 + L1) and Track 2 *knowledge graph as AI SDLC* (governed AI processing for ambiguity
  a deterministic program cannot handle; L3 is the foundation and **AI is also the runtime for
  certain nodes**). The tracks echo each other — shared co-design, certification and promotion —
  and differ only in the nature of the work and where execution goes. AI gains a **run-time
  participant** role. **The north star MOVED: the next invariant re-verify must check the 2026-09-17
  text, not the 2026-06-20 one.** [[bp-agent-orchestration]] is Track 2's Blueprint gap; Track 1 and
  the collaboration foundation are delivered.
- **last_invariant_check:** 2026-09-16 | 2026-09-16-041500.md (COMPLETE — Eric walked the set against
  live-tree evidence: 3 invariants + 5 stack + 5 key decisions + 3 conventions + eric-release-rhythm +
  the Vision all confirmed. `stack-language-java21` re-confirmed (Java 21 baseline, Java 25 now LTS =
  recommended runtime, toolchain stays until Java 25 is mainstream); `virtual-threads-rpc` ENRICHED
  from ADR-0024 (dispatch is a per-route virtual thread, one mode); **`conv-telemetry-presentation-parity`
  RETIRED** — pure invalidation, archived, no successor (the Java-is-reference principle survives in
  [[conv-ports-adopt-java-release-number]]). Core count 18 → 17.
  Prior: 2026-09-11 | 2026-09-11-005808.md)

> This agent-memory layer was seeded on 2026-06-20 from a prior prototyping
> environment, carrying forward only the confirmed Vision + Blueprint and the
> durable project facts — a clean start for the official repo (see the
> 2026-06-20 bootstrap session log).

## Stack & Tools

> Canonical live home for the current stack — language version, dependencies, tool
> versions. `instructions.md` keeps only a high-level descriptor and points here.

- Language: Java 21 (virtual threads). (Kotlin appears only as an example module, not a framework language.)
  **The build targets Java 21 deliberately — wider compatibility** (Eric, 2026-09-02); the
  documented recommended JDK/JRE is Java 25 (current LTS, fully supports the Java 21
  virtual-thread technology). **The toolchain (`.java-version`, CI setup-java) intentionally
  STAYS on 21 until the majority of field installations run Java 25** — Java version
  migration is slow across enterprise customers; do not bump it ahead of the field.
  **Re-confirmed 2026-09-16 (Eric, invariant re-verification):** Java 21 remains the **baseline**;
  Java 25 is now the LTS and therefore the **recommended runtime**; keep the toolchain on 21 until
  Java 25 is mainstream. Unchanged in substance — the trigger is still field adoption, not a date.
  <!-- id: stack-language-java21 | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->
- Build: Maven 3.9.7+ — the engine's multi-module reactor (`com.accenture.mercury:parent-mercury`)
  stays Maven by design. **Consumer applications choose Maven or Gradle**: the starter templates
  ship both build files, CI-verified (PR #357, 2026-09-11; thread-add-gradle-build closed —
  Gradle applies to templates only, never the reactor). (Reworded 2026-09-11 at invariant
  re-verify, Eric-confirmed.)
  <!-- id: stack-build-maven | created: 2026-06-20 | last_used: 2026-09-11 | uses: 3 | tier: core -->
- Integration: Spring Boot 4 only — `system/rest-spring-4` (+ its example). The Boot 3
  lane (rest-spring-3 + rest-spring-3-example) was RETIRED 2026-08-27 (Eric's directive:
  the Spring community no longer issues Boot 3 security patches, and field Snyk now
  REJECTS Boot 3 dependencies and requires Spring Framework ≥ 7 — the deployment
  pipeline was BLOCKED until removal; v4.11.12 is the unblocking release). Same
  integration surface; migration = dependency swap + the app's own Boot 3→4 upgrade.
  Spring stays optional, never required by core. (ADR-0017)
  <!-- id: stack-integration-spring-boot4 | created: 2026-08-27 | last_used: 2026-08-27 | uses: 1 | tier: core | supersedes: stack-integration-spring | origin: 2026-08-27-213034 -->
- Messaging: Kafka — the connector/presence pair (`connectors/adapters/kafka/`) plus the
  grown family: `system/twin-kafka`, `system/minimalist-kafka`, `helpers/kafka-standalone`
  (+ demos); MsgPack wire serialization; customized Gson. (Wording refreshed 2026-08-21 at
  invariant re-verify — substance unchanged.)
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
  **Since v4.12.10 the DISPATCH path is virtual-threaded too (ADR-0024; enrichment confirmed by Eric
  2026-09-16):** the Vert.x event loop only enqueues to a bounded per-route mailbox, and a per-route
  virtual thread runs the ServiceQueue state machine *and* the elastic-queue spill I/O. There is ONE
  dispatch mode — there used to be two, because a carrier-pinning spill store had to run inline on
  the loop. So the loop is now reserved for handing off work, and a slow consumer's spill parks its
  own virtual thread instead of blocking every route that shares that loop. See
  [[elastic-queue-file-store]].
  <!-- id: virtual-threads-rpc | created: 2026-06-20 | last_used: 2026-06-27 | uses: 4 | tier: core -->

## Key Decisions

- **@PreLoad functions are constructed BEFORE @MainApplication runs — never freeze late-arriving
  config in a @PreLoad constructor (2026-09-11, upstreamed field MR + Eric's ruling).** AppStarter's
  order is BeforeApplication → preload() → MainApplication, so a constructor-resolved template that
  interpolates system properties published by a start-up credential bootstrap (vault pattern) freezes
  them as missing for the life of the instance. `kafka.health`/`secondary.kafka.health` now resolve
  probe config through a `Supplier` at client build time (re-resolved on every rebuild) and, while
  the client cannot even be BUILT, report a passing "Waiting for Kafka connection" status instead of
  failing /health — a pod restart cannot produce a credential (Eric's ruling); only a real round-trip
  failure (client built, cluster unreachable) fails /health with 503. (Shipped via PR #360.) Applied
  again by `soa.redis.health` (PR #361, shipped as `redis.health`, renamed 2026-09-13 — the plain
  name is reserved for the planned generic Redis distributed-cache module's check; sync-over-async —
  one probe also covers minigraph-state-redis,
  same `redis.*` keys), with the Redis wrinkle: a late credential surfaces as a server-side auth rejection
  (NOAUTH/WRONGPASS) at connect time, not at client construction, so those classify as waiting too.
  Eric's standing rule: every critical infrastructure component needs a health check service.
  Hardened via PR #362 (squash `88bdff3a`): kernel threads for the Kafka checks (see
  [[kafka-clients-kernel-threads]]), AtomicReference fields, supplier guards, and the failure
  message as a `{text, code}` map — `code` for the aggregation/Kubernetes, `text` for the DevOps
  reader (the healthy shape keeps `status` as its human string).
  **Bounded 2026-09-16:** the passing "waiting" status covers ONLY a value that has not landed yet —
  a config that can never work fails the check instead, because reporting it as passing is how a real
  defect hid in the field for hours. See [[kafka-class-objects-over-names]].
  <!-- id: preload-before-mainapp-lazy-config | created: 2026-09-11 | last_used: 2026-09-17 | uses: 15 | tier: active | origin: 2026-09-11-185752 -->

- **Kafka-driving functions run on kernel threads — `@KernelThreadRunner` (2026-09-11, Eric's
  question → PR #362).** The Kafka consumer performs network I/O on the CALLING thread inside
  `synchronized` sections, and Confluent serializers are synchronized too — on Java 21 a virtual
  thread blocking there PINS its carrier (JEP 491 lifts that only in JDK 24+; the build targets 21
  and the field runs it). Module rule: functions that drive Kafka clients carry `@KernelThreadRunner`
  (SimpleKafkaNotification, SchemaCodec, SecondaryKafkaNotification, and now both kafka health
  checks — live probes AND the warm-up spawn via getKernelThreadExecutor). Event-loop clients are
  the counter-case: Lettuce does I/O on its own netty threads and callers only await futures, so
  `soa.redis.health` deliberately stays on virtual threads. KafkaConsumer itself is NOT thread-safe;
  sequential multi-thread access under external sync (the checks' ReentrantLock) is its contract.
  <!-- id: kafka-clients-kernel-threads | created: 2026-09-11 | last_used: 2026-09-17 | uses: 7 | tier: active | origin: 2026-09-11-191200 -->

- **Kafka class-valued config is set as `Class` OBJECTS, and a config that can never work must FAIL a
  health check (2026-09-16, field bug → PR #403; Eric ruled both halves).** Kafka resolves a class
  *name* through `Utils.getContextOrKafkaClassLoader()`, which prefers the thread context classloader
  and falls back to Kafka's own loader **only when the TCCL is `null`** — so a non-null but *wrong*
  TCCL fails a lookup that a null one would have completed. A `Class` object short-circuits
  `ConfigDef.parseType` (`if (value instanceof Class) return value`), so no loader is consulted at
  all; `KafkaClientConfig` now puts objects (via `put`, not `setProperty` — so those keys leave
  `stringPropertyNames()`, which `healthProbeProperties` accounts for), converging with
  kafka-connector, which always did. A template naming its own `partitioner.class` still wins as a
  String, resolved by Kafka on the app's own startup thread. **The durable lesson is broader than
  Kafka: a pooled kernel thread changes what code can SEE, not just when it runs** — `kafka.health`
  is `@KernelThreadRunner` and built its client on a pooled thread whose loader could not see
  `kafka-clients`, while the same JVM's flow-adapter consumers, same config and same jar on ordinary
  threads, were fine; that A/B *was* the diagnosis. Fix the loader, not the setting: with the TCCL
  override disabled, the blind-loader test fails on the deserializers, then `metric.reporters` →
  `JmxReporter`, then `sasl.oauthbearer.jwt.retriever.class` — Kafka resolves many configs this way.
  **Second half — the leniency boundary:** the passing "waiting" status exists for ONE case, a value a
  later `@MainApplication` bootstrap will publish; a class absent from the classpath will not appear
  because we waited, so it answers 503 naming the *configuration*, not the network. Detection is by
  message text (Kafka's `ConfigException` carries no cause) and defaults to *waiting*, so a reworded
  message degrades to leniency rather than to spurious outages. Bounds
  [[preload-before-mainapp-lazy-config]]; extends [[kafka-clients-kernel-threads]].
  **INCOMPLETE as shipped in v4.12.11 (2026-09-17):** scoping the loader override to client
  construction left a produce-only leg broken in the field — the loader is consulted earlier still, in
  a Kafka config class's static initializer, reached while resolving the template. See
  [[kafka-config-class-static-init-loader]].
  <!-- id: kafka-class-objects-over-names | created: 2026-09-16 | last_used: 2026-09-17 | uses: 4 | tier: active | origin: 2026-09-16-185851 -->

- **Kafka resolves class-valued config DEFAULTS inside its config classes' STATIC INITIALIZERS, so a
  wrong thread context loader poisons the class for the life of the JVM (2026-09-17, field report on
  v4.12.11 → `bee2bc51`).** `ConfigDef.define` parses a `Type.CLASS` setting's default the moment the
  key is defined (`ConfigKey.<init>`: `defaultValue = parseType(...)`), and
  `SaslConfigs.addClientSaslSupport` defines `sasl.oauthbearer.jwt.retriever.class` with a class NAME
  default — so merely initializing `ConsumerConfig` is a classloading event through
  `Utils.getContextOrKafkaClassLoader()`, with **no broker, no SASL and no credentials involved**.
  v4.12.11 wrapped only `new KafkaConsumer<>()`, and `kafka.health` still failed on a **produce-only**
  leg, where `healthProbeProperties` reaches that initializer via `ConsumerConfig.configNames()` while
  resolving the template — before `buildClient`. The consumer path was spared *only because* its
  references to the same class are compile-time `String` constants that **javac inlines**, so its
  first touch is the wrapped construction; verified in bytecode. Same jar, same thread, same config:
  the difference was one inlined constant.
  **Two properties that change how this class of bug must be handled.** (1) It arrives as an
  `ExceptionInInitializerError` — an **Error** — so `catch (Exception)` misses it and `/health`
  answered a raw **500**, not a 503. (2) A class whose initializer threw is erroneous for the life of
  the JVM (JLS 12.4.2): every later access fails on *every* thread however correct its loader, so the
  waiting/leniency model of [[preload-before-mainapp-lazy-config]] does not apply — there is nothing
  to wait for and a restart is the only cure. Fix: `probe()` and `href()` run **entirely** under the
  module loader, and a `LinkageError` renders as a 503 naming the configuration.
  `GroupProtocolResolver` reaches `AdminClientConfig`/`Admin.create` the same way and is safe only
  under that pin (noted in its javadoc).
  **Durable lesson, broader than the fix:** v4.12.11 taught "a pooled kernel thread changes what code
  can SEE"; this bounds it — **what a wrong loader can damage is not limited to the operation you
  wrapped**, because class initialization is one-shot, JVM-wide and irreversible, and the *compiler*
  decides which constant references are even capable of triggering it. Wrap every path into the
  library, not the call you think does the work. Method note: the diagnosis came from a 30-line
  fresh-JVM reproduction printing the field message verbatim — the field report had the mechanism
  right and the location wrong (it proposed wrapping `listTopics`, which is never reached), and
  reasoning from Kafka internals would have shipped that. Bounds
  [[kafka-class-objects-over-names]]; extends [[kafka-clients-kernel-threads]]; tracked by
  [[ot-kafka-health-producer-only-fix]].
  <!-- id: kafka-config-class-static-init-loader | created: 2026-09-17 | last_used: 2026-09-17 | uses: 1 | tier: active | origin: 2026-09-17-183008 -->

- **A jar under a base scan package needs an `@OptionalService` master switch, and a vendor
  integration is not done until a negative control proves the happy path (2026-09-16, Eric's design
  → PR #404, v4.12.11).** `opentelemetry-forwarder` lives under `org.platformlambda`, so the jar
  alone auto-registered `distributed.trace.forwarder` — carrying the dependency silently turned trace
  export on. It is now `@OptionalService("otel.forwarding")`, default **off**: one artifact ships and
  DevOps decides per environment, in properties or `-Dotel.forwarding=true` at launch. That is the
  reusable shape for any scanned extension whose behaviour is an operational choice, and
  `composable-example` pins it ("dependency present, feature off") so it cannot regress. The legacy
  `otel.trace.forwarder.enabled` was RETIRED with it — a second switch whose only reachable use was
  the contradictory `otel.forwarding=true` + `…enabled=false`, and `Telemetry` already no-ops on an
  unregistered route (`hasRoute`). Credentials resolve **per export** through a `Supplier` (applies
  [[preload-before-mainapp-lazy-config]]) and the exporter closes via
  [[platform-onshutdown-lifecycle]], which this extension had been missed by.
  **The method is the durable half.** Certified live against Dynatrace SaaS and confirmed queryable
  in its UI (6 spans, one trace, parent/child reconstructed, `server`/`internal` kinds, scope version
  resolved at runtime). Getting there needed an **A-B-A credential experiment** — real token 0/6
  export failures, bogus token 6/6, real token 0/6 — because *zero failures proves nothing until a
  failure is shown to be possible*: a forwarder that skipped export, or never attached the
  credential, yields the identical zero. Generalize it: **when a verification is blocked on access
  you do not have, ask what your evidence would look like if the thing were broken; if broken and
  working look the same, a negative control is the experiment, not a garnish.** Two by-products worth
  keeping — the app returned HTTP 201 in all three legs (a telemetry outage degrades observability
  and nothing else, previously asserted and now shown), and the backend-visible instrumentation scope
  version is a free check that the artifact under test is the one that shipped — and on 2026-09-17
  that check paid out: Dynatrace support confirmed both v4.12.11 field-acceptance traces in the UI
  reading scope version **4.12.11**, closing the loop on the RELEASED artifacts rather than a branch
  build ([[ot-otel-acceptance-traces-pending]]). Report:
  `docs/test-reports/otel-dynatrace-certification.md`; closes [[ot-otel-dynatrace-certification]].
  Splunk's header form is parsed and documented but NOT run live.
  <!-- id: otel-optional-service-and-negative-control | created: 2026-09-16 | last_used: 2026-09-17 | uses: 4 | tier: active | origin: 2026-09-16-193203 -->

- **Application log forwarding is an INFRASTRUCTURE task — the engine does not grow that capability
  (Eric with the field architects, 2026-09-18; CLOSED, not parked).** The field found the gap after
  the OTel trace certification closed: Dynatrace stores traces and logs in two databases, v4.12.12
  exports spans to `/v1/traces` and they are queryable, but nothing sends application logs to
  `/v1/logs` — so "View logs for this trace" is empty and a DQL query on the trace id returns nothing.
  Investigated and specced; the answer is **not** to build an exporter.
  **The decisive framing was the field's own Splunk practice:** container stdout is shipped to Splunk
  by a standard forwarder, configured by the platform, with the application doing nothing. One
  principle, two backends — a collector reads stdout and ships to `/v1/logs` for Dynatrace exactly as
  a forwarder does for Splunk. Rejected in-app alternative: an OTLP log appender was *easy* (
  `OtlpHttpLogRecordExporter` is already in the artifact the span exporter uses, so zero new
  dependencies) and that convenience is bought in the worst place — an HTTP exporter reachable from
  every `log.info` in the process, owning credentials, retry and back-pressure on the app's critical
  path. Dynatrace's own "collector copies spans into the logs store" workaround was also rejected,
  structurally: span-shaped records cannot contain the application's log messages.
  **The engine's half is already done and is what makes the infra half work.** `log.format=json|compact`
  plus `app-log-context.yaml` already emits `trace_id`/`span_id` on every line inside a traced worker —
  correlation on a virtual-thread runtime where the MDC pattern is an anti-pattern. The v4.12.12
  snake_case rename aligned those keys with the distributed-trace block, so a collector maps one
  vocabulary, and the enforced `$utc` gives it an unambiguous timestamp to parse. The gap was never
  correlation; it was transport, which is the platform's job.
  Applies [[clean-knowledge-design-over-engine-coverage]] one level up — the same "should the engine
  absorb this at all" question asked of a *capability* rather than a graph composition.
  <!-- id: log-forwarding-is-infrastructure | created: 2026-09-18 | last_used: 2026-09-18 | uses: 1 | tier: working | origin: 2026-09-18-174943 -->

- **Every edge case has edge cases — clean knowledge design beats engine coverage, and avoiding
  over-engineering is a PRODUCT-OWNER responsibility (Eric, 2026-09-18).** When a graph composition
  produces a hard case, the first question is whether the engine should absorb it at all. Chasing
  edge cases into the engine has no natural stopping point, and every guard added becomes a shape the
  engine must keep working forever. The answer is a model kept simple enough that the question does
  not arise — exercised in the **design phase** and at the **certification gate**, not at runtime.
  This is a standing design posture, not a MiniGraph fact: it decided four rulings in one sitting
  (defer the parent→for_each→flow→suspending-graph case; reject an author-nominated iteration
  discriminator; reject an index+size store key; decline CompileGraph detection of the risky shape).
  **Corollary for documentation — constraints are DECLARED, not enforced.** A partial gate is worse
  than none: it teaches that *unflagged means safe*, which is learned once and applied everywhere,
  and no gate can see every shape (CompileGraph cannot see through `flow://`). So limits belong in
  the guide's Design-rules voice, covering the whole surface. The field "designs graphs with a lot of
  imagination", so an undeclared limit is discovered by losing a suspension.
  **Method note from the same sitting, worth more than the outcome:** the rejected index+size key
  *failed safe* — a changed array size missed rather than restoring wrong state — and was still
  wrong, because the operation it made fail (appending a product to a warranty list) is the ordinary
  thing a user does, while the hazard it never caught (reordering) is the one that corrupts. **A
  guard that fails safe is still wrong if the thing it makes fail is the common legitimate case**;
  reason from what the user does, not from what the engine can detect. Serves
  [[vision-mercury-composable]] (the certification half of the governance lifecycle); applied by
  [[ot-subgraph-for-each-suspend]].
  <!-- id: clean-knowledge-design-over-engine-coverage | created: 2026-09-18 | last_used: 2026-09-18 | uses: 2 | tier: active | origin: 2026-09-18-174943 -->

- **sync-over-async runs on standalone OR clustered Redis behind one seam, in its own
  `soa.redis.*` config namespace (2026-09-14, field request; Eric ruled the design).**
  `RedisBackend` (StandaloneRedisBackend / ClusterRedisBackend, built by `RedisBackendFactory`)
  hides the topology. The seam is clean because in Lettuce the standalone `RedisCommands` and the
  cluster `RedisAdvancedClusterCommands` BOTH extend `RedisClusterCommands` — the single command
  type the store programs against (all ops single-key + `publish`). Cluster-safe by construction:
  the sole two-key `DEL` (cleanup) is split into two single-key DELs, so the **wire key format is
  UNCHANGED** (`request:{cid}` / `queue:{cid}`, no hash tags) — existing standalone polyglot
  interop is unaffected and there is no Rust wire break. Classic Pub/Sub wake-ups cross the
  cluster bus. **Cluster selection = TWO keys** (Eric, to keep a cache-style boolean intact):
  `soa.redis.cluster.detect=auto` (default) probes `INFO`→`cluster_enabled:1`; otherwise the
  boolean `soa.redis.cluster.mode` (true/false) decides — and the boolean is also the
  inconclusive-probe fallback (robust when INFO is restricted). **Config namespace:** all keys are
  `soa.redis.*` so sync-over-async never collides with a co-resident `redis.*` consumer (a
  distributed cache, or minigraph-state-redis), and **each key falls back to the un-prefixed
  `redis.*`** via the config reader's nested-default (`get(soaKey, get(redisKey, default))`) — so
  NO breaking change (the earlier `soa.redis.health` ROUTE rename #377 has no fallback and stands).
  **Auth is identical for both topologies AND matches the field's cache client** (verified against
  the field design extract): token → `withPassword` (my username-blank + password path); RBAC
  username+password → `withAuthentication` (my username-set path) — same Lettuce calls, same wire
  AUTH, no AWS SDK. New optional `soa.redis.username` (RBAC). Credentials resolve `${ENV_VAR}` from
  a lower-`sequence` credential bootstrap ([[preload-before-mainapp-lazy-config]]); the field's
  loader is `@MainApplication(sequence=9)` < the autostart's default 10, so it runs first (must
  publish resolved creds to the `redis.*`/`soa.redis.*` property names). Lettuce's
  `withAuthentication(RedisCredentialsProvider)` is the future seam if rotating IAM tokens are ever
  needed. Rust parity for cluster (cluster-client option + the same DEL split) is a parked
  follow-up, NOT a break. Extends [[soa-transport-neutral-cid]].
  <!-- id: soa-redis-cluster-support | created: 2026-09-14 | last_used: 2026-09-16 | uses: 7 | tier: archive-candidate | origin: 2026-09-14-181948 -->

- **The distributed cache is a SEPARATE module — sync-over-async stays small (Eric, 2026-09-14).**
  sync-over-async is a *rendezvous transport* (correlation-id `request:`/`queue:` keys,
  `RPUSH`/`LPOP`/`EVAL` drains), NOT a key-value cache; do not add cache operations to it. The
  reusable, generic asset is its **Redis client layer** — `RedisBackend` (standalone/cluster) +
  `RedisConfig` + `RedisBackendFactory` + auth + health — which the planned generic Redis
  distributed-cache module should share (extract to a small foundation, prefix-parameterised:
  `redis.*` for the cache, `soa.redis.*` for sync-over-async; the `redis.*` fallback already lets
  them coexist or share one server). The cache module is a composable **action function**
  (PUT→`setex`, GET→`get`, MGET→`mget`, DELETE→`del`, PUT_IF_NOT_PRESENT→atomic `SET NX EX` (not
  `setnx`+`expire`), PING) taking action+key+value+ttl — usable as an Event Script task / L3
  `graph.task` with output data mapping AND directly `PostOffice`-callable (L1); the field's two
  consumption patterns are both just "a composable function." Its home was already anticipated:
  the `redis.health` route name is reserved for it (sync-over-async's is `soa.redis.health`).
  Cluster note: Lettuce's cluster client scatter-gathers a cross-slot `MGET` for free. Keeps the
  lean-module vision and is a chance to converge a field L2 cache library. **Ruled Q1–Q8 (Eric,
  2026-09-14):** op set adds `MPUT` (pipelined per-entry `SETEX`, NOT `MSET` — MSET has no TTL) +
  list push/pop/length; extract a shared `redis-connection` foundation; opaque `byte[]`; app
  key-prefix; typed helper deferred; **Rust lockstep**. **IMPLEMENTED (Java) for v4.12.9** on the
  extracted [[redis-connection-foundation]] (spec draft-design-specs/distributed-cache.md);
  [[ot-distributed-cache]] tracks the remaining Rust lockstep. Builds on [[soa-redis-cluster-support]];
  serves [[vision-mercury-composable]].
  <!-- id: cache-separate-from-soa | created: 2026-09-14 | last_used: 2026-09-16 | uses: 10 | tier: archive-candidate | origin: 2026-09-14-191748 -->

- **The Redis client layer is a shared `extensions/redis-connection` foundation (2026-09-14; Java
  shipped for v4.12.9).** Extracted from sync-over-async's `support/`: `RedisBackend<V>` (generic in the
  value type — `<String>` for sync-over-async's text payloads, `<byte[]>` for the cache's opaque values;
  the standalone `RedisCommands<K,V>` and cluster `RedisAdvancedClusterCommands<K,V>` both extend
  `RedisClusterCommands<K,V>` for ANY V, so generification preserves the one-command-type seam; `async()`
  added for pipelining), prefix-parameterised `RedisConfig.from(config, prefix)` (`soa.redis.*` or
  `redis.*`, both falling back to un-prefixed `redis.*`), `RedisBackendFactory`, and `RedisHealthProbe` —
  the probe logic **de-annotated** (NO `@PreLoad`, else it would auto-register in every consumer under
  `web.component.scan=org.platformlambda`) and bound per-module by a thin `@PreLoad` subclass
  (`SoaRedisHealthCheck`→`soa.redis.health`, `CacheRedisHealthCheck`→`redis.health`). Two consumers:
  sync-over-async (`RedisBackend<String>`, behaviour unchanged) and distributed-cache
  (`RedisBackend<byte[]>`, `v1.cache.redis`). Pure refactor — no wire/behaviour change; the sole external
  consumer touched was the demo's `StreamProducer` import. Commits `5b73f311` (refactor) + `bb88e65c`
  (cache). Realizes the "extract the foundation" half of [[cache-separate-from-soa]]; tracked by
  [[ot-distributed-cache]]; applied [[preload-before-mainapp-lazy-config]] and
  [[conv-reentrantlock-not-synchronized]].
  <!-- id: redis-connection-foundation | created: 2026-09-14 | last_used: 2026-09-16 | uses: 5 | tier: archive-candidate | origin: 2026-09-14-230259 -->

- **platform-core has a lightweight shutdown lifecycle — `Platform.getInstance().onShutdown(Runnable)`
  (2026-09-14, Eric's minimalist-principle ruling; for v4.12.9).** The platform owns ONE JVM shutdown hook
  (installed lazily on the first registration); registered callbacks run in reverse registration order
  (last opened, first released), each isolated (`catch RuntimeException` + log — `Runnable.run()` throws
  nothing checked, so an Error propagates rather than being swallowed). `runShutdownHooks(List)` is
  package-private + parameterized for unit tests (no real JVM shutdown needed) and uses Java 21
  `SequencedCollection.reversed()`. It fills the gap where components hand-rolled their own
  `Runtime.getRuntime().addShutdownHook(new Thread(...))`: the three in-tree cases (`PersistentWsClient`,
  `BdbElasticStore`, `FileElasticStore`) were migrated onto it, and the distributed cache's `CacheRuntime`
  registers its Redis-connection close from `build()` (so only when a connection actually opened — which
  also makes the cache's `shutdown()` a used method, resolving the field Sonar "never used" finding without
  deleting it). Rust parity is a lockstep follow-up (internal lifecycle API, not a wire contract). Applies
  [[conv-reentrantlock-not-synchronized]]; used by [[redis-connection-foundation]].
  <!-- id: platform-onshutdown-lifecycle | created: 2026-09-14 | last_used: 2026-09-16 | uses: 4 | tier: archive-candidate | origin: 2026-09-15-011235 -->

- **MiniGraph async skill callbacks are guarded — a failure surfaces as the node's error, never a
  silent hang (2026-09-15; found building the distributed-cache example, PR #392).** A
  `graph.task`/`graph.extension`/`graph.api.fetcher` OUTPUT-mapping LHS may only be a constant or a
  `result.`/`model.`/`<node>.` element (`setOutputMappingEntry`, renamed from `setFetcherOutputEntry`
  in PR #394; its sibling `performFetcherOutputMapping`→`performOutputMapping`) — `input.*` is INPUT-side only; to
  echo an input value, stage it at a mapper/decision node (`MAPPING: input.body.id -> model.id` —
  graph.math MAPPING uses the unrestricted `getLhsOrConstant`) and map `model.id` out. Before the
  fix, that IllegalArgumentException threw inside `Mono.create(sink -> pending.thenAccept(...))`,
  was swallowed by the unobserved CompletableFuture stage, the sink never completed, and the caller
  timed out with ZERO diagnostics (graph traversed, Redis PUT logged 200 — it looked like a
  transport bug). Both async skills now complete through `GraphLambdaFunction.guardedCompletion`
  (failed future unwrapped + throwing handler → sink.error), so the failure renders exactly like a
  synchronous skill throw, trace context intact (WorkerHandler's Mono error path). Regression:
  `unit-test-task-8` + `GraphLambdaFunctionGuardTest`; PR #393 (`1df13078`, merged squash `6b5b5cf1`).
  Follow-up PR #394 (merged squash `3f3ef3fe`, Eric's review of the surfaced error) reworded the two mapping errors — drop the
  stale "API fetcher"/"data dictionary" labels, name the node, quote the offending `lhs -> rhs`, add
  the clue `'input.*' is valid only on the input side` — and renamed the helpers (above). Verified
  end-to-end in a live Playground: the distributed-cache-example run in dev mode (`app.env=dev` +
  `com.accenture.minigraph` scan + a trimmed companion/UI `rest.yaml`) hosted by the
  [[playground-session-broker]], driven via `/api/companion/{id}/sync` — a broken `cache-get` output
  mapping aborted the dry-run with the reworded error in the UI console (screenshot proof, 2026-09-15).
  Parked: CompileGraph static LHS check (dynamic `{…}` limits it to static cases);
  Rust-twin parity check of the same callback pattern. Relates [[trace-thread-keyed-mono-gotcha]]
  (the same async-callback minefield).
  <!-- id: minigraph-guarded-async-completion | created: 2026-09-15 | last_used: 2026-09-18 | uses: 6 | tier: active | origin: 2026-09-15-040141 -->

- **The elastic queue spills to a dependency-free file FIFO, and that choice sets the dispatch model
  (2026-09-16, P4 of the BDB migration). (ADR-0024)** Every route's back-pressure overflow buffer
  holds 20 events in memory then spills to per-route append-only segment files under the temp dir
  (`FileElasticStore`), transient, segment deleted once fully read. **Berkeley DB is RETIRED** — with it
  went the `elastic.queue.store` switch, `deferred.commit.log`, the `elastic.queue.cleanup` reserved route,
  and platform-core's `com.sleepycat:je` dependency. `ElasticStore` stays as the seam, one implementation.
  **The architectural half:** because the file store's blocking I/O parks a virtual thread instead of
  pinning its carrier, `ServiceQueue` has exactly ONE dispatch mode — every route dispatches off the event
  loop on its own virtual thread via a bounded mailbox (`elastic.queue.dispatch.mailbox.size`, 1024,
  blocks-not-drops). Store and dispatch are not independently configurable; the dual-mode branch is gone.
  Remaining tunables: that key + `elastic.queue.segment.size.bytes` (16 MB). **Durable lesson worth more
  than the outcome:** the microbenchmark favoured the store we removed — BDB was competitive-to-faster on a
  single isolated route. Only the mixed-workload probe (a latency-sensitive route measured *while* another
  route's spill runs) exposed what mattered: keeping spill off the shared Vert.x loop. Benchmark the
  interference, not just the component. Evidence retained at `benchmark/benchmark-reporter/analysis/` (the
  A/B is no longer reproducible — kept as the historical record); the tool is now a single-store baseline
  for validating milestone releases. No consumer action: `file` was already the default, an app still
  setting `elastic.queue.store` is unaffected (unread), and nothing in the buffer was ever durable.
  Closes [[thread-elastic-queue-bdb-to-file]] + [[thread-elastic-queue-docs-adr]]; relates
  [[virtual-threads-rpc]] and [[conv-reentrantlock-not-synchronized]] (the same carrier-pinning concern).
  <!-- id: elastic-queue-file-store | created: 2026-09-16 | last_used: 2026-09-16 | uses: 4 | tier: archive-candidate | origin: 2026-09-16-020051 -->

- **A Layer 3 application is one graph endpoint plus dev mode — and the Playground UI hides behind a
  classpath-order trap (2026-09-15, Eric's polish round on the starter template + the cache example).**
  Two shape rules. **(1) One endpoint, every graph:** `POST /api/graph/{graph_id}` takes the id from the
  URL path, so a Layer 3 app needs exactly one `rest.yaml` entry and the one stock `graph-executor` flow
  no matter how many models it deploys — adding a graph means adding its id to `graphs.yaml`, never a
  bespoke route (the cache example's `/api/l3/profile` + `l3-profile.yml` were deleted for this).
  **(2) Dev mode is two settings that must travel together:** `app.env=dev` AND the dev-mode rest.yaml
  entries. Every Playground/companion service is `@OptionalService("app.env=dev")`, and
  `RoutingEntry.resolveServices` SKIPS a rest entry whose service is unregistered (warn "Service ... not
  available"), so routes without the switch are dead on arrival and the switch without routes leaves only
  a WebSocket. Both now ship pre-wired in `templates/starter-graph` (plus `scripts/`), and in
  `examples/distributed-cache-example` alongside its ordinary L1/L2 routes — removing the one line closes
  the surface for production. **(3) A graph app declares ONE Mercury dependency —
  `minigraph-playground-engine`** — which brings event-script-engine and platform-core transitively
  (compile scope, verified by `dependency:tree`; Gradle too). Listing all three is not just redundant, it
  creates a resource collision: the Playground UI is `classpath:/public/index.html` inside the engine and
  platform-core ships a PLACEHOLDER page at the same path, first jar on the classpath wins (for
  `HttpRouter`'s static route and `GetIndexHtml` alike). The template declared platform-core first and
  therefore served the placeholder; Eric collapsed it to the single dependency (2026-09-15) rather than
  relying on declaration order, which a future tidy-up would silently undo. Everything else stays green —
  tests, curl, the companion endpoint — and only a browser shows the wrong page, which is why it
  survived until a live run. Parked structural option if it recurs elsewhere: move the UI off the
  colliding resource path in the engine (needs Rust lockstep). Documented in `playground-and-companion.md` (#enabling) and
  `ai-agent-guide.md` (#scaffolding). Relates [[playground-session-broker]]; applies to
  [[ot-distributed-cache]]'s worked example.
  <!-- id: minigraph-dev-mode-app-shape | created: 2026-09-15 | last_used: 2026-09-18 | uses: 6 | tier: active | origin: 2026-09-15-221451 -->

- **Playground session broker: an AI agent can HOST a Playground session (2026-09-03, Eric's
  design, contributed from ai-enabled-repo-demo).**
  `examples/minigraph-playground/scripts/playground-session-broker.mjs` (zero-dependency,
  Node ≥ 22) holds a `/ws/graph/playground` session with the UI's own welcome/ping handshake,
  auto-reconnects across app restarts (new session id captured), and exposes a localhost control
  API (`GET /session`, `POST /start|/stop`). Humans join with `session subscribe <id>` as equal
  co-authors (session sync is symmetric — all commands except `session` topology propagate to
  primary and subscribers alike); the agent drives via companion `/sync`. Identical copy in the
  Rust repo — both engines share the WS handshake. Dev-only, like the Playground itself.
  Reactivated 2026-09-14: now ALSO shipped in `templates/starter-graph` (both repos), and the AI
  docs are broker-first with the keep-alive failure mode named (mercury-composable#383, mercury#276).
  <!-- id: playground-session-broker | created: 2026-09-03 | last_used: 2026-09-17 | uses: 10 | tier: active | origin: 2026-09-03-172753 -->

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
  composed by reference not duplicated). Bounded by `one-atom-four-roles` (the **function** is the single atom; *service* / *task* / *skill* merely name how it is wired — see `docs/guides/documentation-conventions.md`, formalized as ADR-0004/ADR-0005): not all code becomes YAML — an
  intrinsically in-function concern (e.g. a blocking rendezvous that must wrap a publish) stays in code.
  Routing vocabulary to learn: `decision` selects a `next` entry by value (`true`=`1`=first, `false`=`2`=
  second; integer is 1-based → multi-way switch — engine `TaskExecutor.handleDecisionTask`, intentional;
  several *derived* docs had it inverted and were corrected 2026-06-27), and `byte[]` rides through
  `model` via the `*` passthrough. Distilled from the sync-over-async composable refactoring (2026-06-27,
  Claude Code). (ADR-0007)
  <!-- id: event-script-over-code | created: 2026-06-27 | last_used: 2026-06-27 | uses: 1 | tier: core -->
- **EventApiService serves LOCAL routes only — an inbound `/api/event` call to a route
  the instance does not host answers 404 even when the instance's own
  `yaml.event.over.http` map points that route at a peer (Eric ratified 2026-08-30).**
  Forwarding would make every app an Event-over-HTTP relay and open routing loops; the
  `x-event-api` wire marker is the existing loop guard (an event that crossed the wire
  once is never re-forwarded), the map is caller-side knowledge (not a promise to third
  parties), and deliberate hop-through is an explicit relay function (demo:
  `hello.remote.relay`). Recorded in the progressive-rendering interop report.
  <!-- id: event-api-local-routes-only | created: 2026-08-30 | last_used: 2026-08-30 | uses: 1 | tier: core | origin: 2026-08-30-050040 -->

## Conventions

- **Glance at GitHub's pre-filled squash-dialog title before confirming a squash-merge
  (Eric's feedback, 2026-08-19).** GitHub pre-fills the dialog with title-plus-body text,
  and stray words can survive into the immutable commit title — PR #283's squash
  `1685842c` landed as "…cannot drop a span  Body (#283)" (a leaked "Body" + double
  space). Trim the pre-filled title to the intended one-liner on every squash; same
  review moment as the co-author-trailer dedup rule in AGENTS.md.
  Second instance: PR #328's squash `fd7f14f5` landed the agent's PR-handoff PREAMBLE in the
  title ("docs/deck-benchmark-study (e385eec2, 55ab58e1 + logs), title Deck slide 13 …").
  Agent-side guard adopted 2026-09-07: in PR handoff text, give the title its own line/code
  block — never inline after branch/commit metadata, so a dialog paste cannot drag it along.
  Relates [[thread-otlp-export-retry]].
  <!-- id: conv-squash-title-prefill-check | created: 2026-08-19 | last_used: 2026-09-18 | uses: 48 | tier: active | origin: 2026-08-19-195244 -->
- **Derive a release's CHANGELOG from `git log <previous-tag>..HEAD`, never from what this session
  did — and re-verify any COUNT before restating it (2026-09-17, Eric caught the gap).** The v4.12.12
  CHANGELOG shipped describing one fix, because that is what the release session had worked on. PR
  #406 (`e6447ceb`) had landed between the v4.12.11 and v4.12.12 tags in an EARLIER session — the
  CompileGraph task/skill gate and the case-insensitive `input.header.*` lookup — and went out in the
  artifact undocumented. A patch release usually *is* one session's work, which is exactly what makes
  this fail quietly: the habit is right often enough to feel safe. The tag range is the only
  authority. Scope filter that held up on review: `chore(agent-memory)` tooling upgrades and
  test-fixture changes are internal and stay out; anything touching shipped behaviour goes in.
  **Consequence worth more than the omission:** the entry's upgrade note said "no action to
  configure" for the whole release, and the missing CompileGraph gate *can reject a graph model that
  deployed yesterday* — so an incomplete CHANGELOG is not merely thin, it can be actively wrong about
  the one thing a reader checks. Each item now carries its own upgrade note.
  **Same root cause, second instance, same episode:** the javadoc written during #406 claimed "67
  nodes carry one of the three skills"; re-counting before repeating it in the CHANGELOG gave **64**,
  and `e6447ceb~1` shows it was never 67 (the 65th at HEAD is a deliberate negative fixture from that
  PR). Both errors came from writing out of recollection of my own work instead of out of the
  repository. Corrected in both places via PR #411. Relates [[conv-template-version-sweep]] (the
  sibling rule for the version sweep: re-derive, never carry the prior count forward).
  <!-- id: conv-changelog-from-tag-range | created: 2026-09-17 | last_used: 2026-09-17 | uses: 1 | tier: active | origin: 2026-09-17-183008 -->
- **A thread id names the THING, never its kind — and an existing thread is never renamed (Eric,
  2026-09-18).** The tool writes every open thread to `memory/open-threads/thread-<id>.md`, so an id
  that already begins `ot-` or `thread-` stutters: `thread-ot-distributed-cache.md`, and worst,
  `thread-thread-doc-improvement-feedback-loop.md`. The directory and the tool's prefix already say
  it is a thread. **New thread ids therefore carry no kind prefix** — `distributed-cache`, not
  `ot-distributed-cache`.
  **Existing threads stay as they are, deliberately.** A rename looks free and is not: session logs
  are immutable and their `## Memory References` are the only input to `refresh-metadata`, so
  renaming an id orphans every historical declaration — `ot-distributed-cache` alone is declared in
  **12** logs. Its usage would collapse to ~1, it would decay to `archive-candidate`, and the next
  review would propose sweeping a live workstream. That is the same failure as the 2026-09-17
  `[overdue]` advisories, except self-inflicted and unrepairable. The schema agrees for its own
  reason: "the filename is the identity and **never changes** for the thread's lifetime". The ugly
  names retire on their own as threads close and the review sweeps them.
  **The single `thread-` prefix is upstream, not ours** — `memory-lint` hard-codes
  `expect = f"thread-{fid}.md"` (one production line; `archive-fact` already keys on the footer id
  and is prefix-agnostic). Reported to the agent-memory team 2026-09-18 as a cosmetic item,
  explicitly below the bar of the two decay-signal reports that preceded it — **and it SHIPPED as
  agent-memory v4.41.1 the same day** (PR #419, squash `c83c5095`), crediting "this team's
  2026-09-18 note". Both halves landed: the rule is now `DECAY.md` §1 and `.agent/schema.md`, and so
  is the never-rename constraint, carrying our 12-declarations example; the upgrade's own step 2
  reported "nothing renamed — 14 of 14 thread files carry a kind-prefixed id and stay as they are".
  Two things worth reading from that. A report ranked LOWEST of three shipped FASTEST — because it
  was the one that arrived with a concrete fix attached, not because it mattered most; rank by
  severity, but attach the fix regardless. And the rule is now upstream, so a future agent does not
  need this fact to follow it — only to understand why our existing ids look the way they do.
  Relates [[conv-declare-consulted-references]] (the same immutable-log constraint is why both rules
  are forward-looking only).
  <!-- id: conv-thread-id-names-the-thing | created: 2026-09-18 | last_used: 2026-09-18 | uses: 2 | tier: active | origin: 2026-09-18-174943 -->

- **`continuity.md`'s header shows the metadata footer as an EXAMPLE, and that example's id must stay a
  PLACEHOLDER (2026-09-18, found running the review).** `memory-lint`'s `FOOTER_RE` scans the whole
  file, so a literal-looking `id: kebab-id` in the header's teaching line parses as a live fact — it
  carries `tier: active`, is not pinned, and therefore counts toward `continuity_max_facts`. That is
  what fired `[continuity-bloat] 36 > 35` when the true count was 35, exactly at the cap: a **false
  trigger for the review ritual itself**, which is worse than a noisy advisory, because the review
  then goes looking for something to archive that does not exist — `REVIEW.md`'s costliest error
  approached from the other side. The id now reads `<kebab-id>`, consistent with the `YYYY-MM-DD` and
  `N` placeholders already on that line and failing the `[a-z0-9-]+` capture; it still renders as a
  real HTML comment, so it teaches the same syntax. **Do not tidy it back to a bare id.** The durable
  fix is upstream — the parser should skip the header example; every agent-memory repo ships this
  header and so over-counts by one — reported 2026-09-18. Method note: the diagnosis was a two-copy
  experiment (lint an untouched copy, lint a copy with the one line neutralized), not a reading of the
  regex; the regex told me it was *possible*, the copies told me it was *the* cause. Relates
  [[conv-thread-id-names-the-thing]] — the sibling case, where the tool's behaviour rather than our
  content was the thing to change.
  <!-- id: conv-schema-example-not-a-fact | created: 2026-09-18 | last_used: 2026-09-18 | uses: 1 | tier: working | origin: 2026-09-18-215436 -->

- **Retired Maven modules need placeholder manifests for Snyk (2026-09-01, Snyk team +
  Eric).** Snyk keys a project on repository+branch+manifest path and never retires it —
  deleting a module freezes its findings on the last resolved dependency tree, failing
  the security gate forever. A parentless dependency-free `packaging=pom` placeholder
  re-tests to an empty graph (zero findings). Live at system/rest-spring-3 +
  examples/rest-spring-3-example (PR #305) with relocation metadata to the Boot-4 twins;
  **release version sweeps must include these non-reactor poms deliberately.** Relates
  [[stack-integration-spring-boot4]].
  <!-- id: snyk-retired-manifest-placeholders | created: 2026-09-01 | last_used: 2026-09-16 | uses: 16 | tier: archive-candidate | origin: 2026-09-01-022524 -->
- **Every port adopts the JAVA release number on catch-up — no downstream repo runs its own version
  sequence (Eric, 2026-09-16).** The Java repo is the reference implementation, so a version number
  identifies **content**, not "this engine's Nth release". This covers the Rust port AND the python
  and node language packs alike: whenever one is next updated, it is tagged at the Java number it
  caught up to — never at an intermediate number invented to represent partial catch-up. Consequence
  to read correctly: a port sitting below Java (Rust at v4.12.7, the python/node packs at 4.12.1,
  while Java shipped v4.12.9) is **lag awaiting catch-up, not divergence**, and the gap is not a
  compatibility signal. Corollary for release notes and continuity entries: state a port's number as
  the content it currently carries, never as a separate cadence. The Java-is-reference principle this
  rests on outlived `conv-telemetry-presentation-parity` (retired 2026-09-16): Eric restated it
  directly when giving this convention, so it stands on its own. Governs the Rust half of
  [[ot-distributed-cache]].
  <!-- id: conv-ports-adopt-java-release-number | created: 2026-09-16 | last_used: 2026-09-18 | uses: 7 | tier: active | origin: 2026-09-16-003354 -->
- Add capability: function (`@PreLoad` + `TypedLambdaFunction`) → flow YAML →
  register in `flows.yaml` → `rest.yaml` mapping if HTTP-facing.
  <!-- id: conv-add-capability | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->
- **Release version sweeps must include the starter templates (2026-09-11).** The Java
  sweep's grep must cover `--include=build.gradle` alongside `--include=pom.xml`: each
  `templates/*` module carries a standalone pom (literal engine versions, like the Snyk
  placeholders) AND a Gradle build with a single `def mercuryVersion = '<version>'`
  literal. Mercury artifacts are NOT on Maven Central — the templates' Gradle builds
  resolve engine artifacts from mavenLocal (the reactor `mvn install`), which is also why
  ci.yml's templates-gradle job installs the engine modules first. Since v4.12.8 the sweep
  is BUILD FILES ONLY (40 at that release): template READMEs and all guide prose use the
  `x.y.z` placeholder with an explainer line (Eric's direction — prose never needs a
  version bump again).
  <!-- id: conv-template-version-sweep | created: 2026-09-11 | last_used: 2026-09-16 | uses: 10 | tier: archive-candidate | origin: 2026-09-11-005808 -->
- Watch serialization gotchas (Long↔Integer downcast; use `util.str2int/str2long`).
  <!-- id: conv-serialization-gotchas | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->
- **Declare a Memory Reference when a fact is CONSULTED to make a decision — not only when it is
  edited (Eric agreed, 2026-09-04).** A session log's `## Memory References` is the sole input to
  `refresh-metadata`, so an undeclared consultation reads as non-use and decays the fact. This is
  not hypothetical: `2026-09-04-011530.md` declared `(none)` while reasoning explicitly from
  `conv-telemetry-presentation-parity` to conclude the Kafka opt-out was Java-only, and the very
  next refresh demoted a 42-use fact to `archive-candidate`. Past logs are immutable, so the guard
  is forward-looking. Rule of thumb: if you would have decided differently without the fact, it is
  a reference. Surfaced by the 2026-09-04 smoke test; the two facts it endangered are now `core`.
  <!-- id: conv-declare-consulted-references | created: 2026-09-04 | last_used: 2026-09-04 | uses: 1 | tier: core -->

- **A proposal is not a decision: raise it in `docs/arch-decisions/proposed.md` as `P-NNNN`, never
  as a `Proposed` ADR (Eric, 2026-09-18).** The ADR ledger is an **immutable journey of decisions**,
  so an entry is written when a decision is *accepted* — never before. A proposal may be reshaped,
  merged or withdrawn, and a withdrawn proposal is a non-decision that would otherwise sit in the
  ledger forever looking like one. `P-NNNN` and `ADR-NNNN` are separate sequences: a proposal does
  not reserve an ADR number, because proposals and decisions do not map one-to-one.
  **This OVERRIDES the agent-memory protocol's wording**, which says to "propose a newer ADR … and
  wait for human approval" (`memory/PROTOCOL.md`) — that file is tool-managed and ships from the
  template, so it is not edited; this fact is the local rule, and the rule is also stated in the ADR
  ledger's own header and in `proposed.md`. Superseded/Deprecated handling is unchanged — that is the
  *post*-decision stage and stays in `ADR.md`, marked in place, never deleted.
  Adopted in the same sitting that accepted the five ADRs left at `Proposed` (0013–0017), which is
  what exposed the gap: a status that means "not yet decided" had accumulated for a month across
  decisions that had in fact all shipped. Relates [[eric-code-changes-via-pr]] (the sibling rule for
  how a change reaches main).
  <!-- id: conv-proposals-not-in-adr-ledger | created: 2026-09-18 | last_used: 2026-09-18 | uses: 1 | tier: core | origin: 2026-09-18-221732 -->

- **Use `ReentrantLock`, not `synchronized`, for locks/critical sections while the build targets Java 21
  (Eric's directive, 2026-09-14).** On Java 21 a virtual thread that blocks inside a `synchronized` block
  PINS its carrier thread; JEP 491 lifts that only in JDK 24+, and the toolchain intentionally stays on 21
  until the field runs Java 25 ([[stack-language-java21]]). So lazy-init and shared-state guards use a
  `ReentrantLock` (try/finally) — it does not pin. Precedent: `RedisHealthProbe` (carries the explicit
  comment) and the return-route coordinator; applied to the distributed cache's `CacheRuntime`. This
  decays once the toolchain moves to Java 25 (JEP 491 makes `synchronized` non-pinning). Relates
  [[virtual-threads-rpc]], [[kafka-clients-kernel-threads]]; applied in [[redis-connection-foundation]].
  <!-- id: conv-reentrantlock-not-synchronized | created: 2026-09-14 | last_used: 2026-09-16 | uses: 4 | tier: archive-candidate | origin: 2026-09-14-230259 -->

## Blueprint  *(gap from Current State → Vision; `(blueprint)` threads serve `vision-mercury-composable`)*

- [ ] (blueprint) **AI agent orchestration ("graph engineering")** — the Active Knowledge
  Graph as the governed run-time for AI agents: LLM reasoning, MCP tools, and inner-loop
  agents join graphs as wrapper-side functions (bounded-agency decision graphs;
  agent-as-node); suspend/resume = human-in-the-loop; CompileGraph + the promotion
  lifecycle = the governance answer ("governed nondeterminism", never determinism claims).
  Engine core stays LLM/vendor-free; wrapper scope fence intact (adapters are functions ON
  the wrappers). Run-time complement to the design-time [[bp-ai-companion-llm-backend]];
  builds on [[bp-polyglot-functions]]; compounds [[bp-graph-governance-lifecycle]].
  Concept doc: draft-design-specs/ai-agent-orchestration.md (Q1–Q8 open questions,
  E0–E4 experiment plan). Direction ratified by Eric 2026-08-25. **E0 DONE 2026-09-01**
  ([[ot-agent-orchestration-e0]]): support-triage graph + llm.chat/llm.stream python AI
  nodes + progressive token rendering out the engine SSE edge — the first live
  graph.task→wrapper drive, real Gemini verdicts, one distributed trace. Enterprise LLM
  access is platform-mediated only (Bedrock/Vertex/Foundry — concept doc Q2); the
  Anthropic SDK covers all three, so the switch is client-construction only — re-drive
  waits on Eric's cloud account. **Q8 second half: the TRANSPORT delivered 2026-09-12** —
  the streaming return route ([[ot-streaming-return-route]], E-series complete incl.
  E4's real-Gemini cross-pod token stream) carries a wrapper/graph node's tokens to
  whichever pod holds the user's connection, zero engine change; what remains here is
  driving that bridge from an LLM node INSIDE a live graph run. Next: E1 (suspend
  checkpoint on an LLM verdict), the in-graph-run streaming drive.
  **Status framing (Eric, 2026-09-17, at the first closure gate): graph-based AI SDLC is in the
  DESIGN phase**, with experiments proving the prerequisites — progressive rendering of token
  batches being the one already demonstrated. Sole remaining Blueprint gap, deliberately
  long-horizon: an epic, not a task.
  **What it shares with the closed companion gap, and what it does not (Eric's clarification).**
  *Shared:* the authoring lifecycle. An AI SDLC graph is co-invented by human and AI in the
  Playground in dev mode — the same prototyping surface, the same maturity, reused rather than
  rebuilt. *Different:* what survives promotion. For the companion the AI is a **design-time**
  participant only — after promotion the UI is disabled and the graph runs without it. For AI SDLC
  **some nodes carry AI skills**, so after promotion those nodes interface with an AI agent through
  gateways, MCP servers or other tools: the AI is in the **production execution path**, not just the
  authoring one. That is the whole reason this is a separate epic — the authoring pattern is
  solved and inherited; the runtime posture is the open problem. It is also why the engine core
  stays LLM/vendor-free with adapters as functions ON the wrappers: the gateway/MCP edge is where
  vendor specifics live, never in the engine.
  → serves: vision-mercury-composable
  <!-- id: bp-agent-orchestration | created: 2026-08-25 | last_used: 2026-09-17 | uses: 16 | tier: working | origin: 2026-08-25-213703 -->
- [x] (blueprint) **CLOSED 2026-09-17 (Eric, at the first closure gate)** — integrate a pluggable AI
  companion LLM backend. **Answered:** progressive rendering was driven end-to-end against the live
  Gemini API (E0 — `llm.chat`/`llm.stream` AI nodes, real verdicts, tokens out the engine SSE edge,
  one distributed trace), which settles the pluggable-backend question.
  **This gap is DISTINCT from [[bp-agent-orchestration]] (Eric, correcting the first close record).**
  The AI companion is *design-time collaboration* — an AI co-authoring graphs with a human in the
  minigraph Playground. `bp-agent-orchestration` is the *run-time* epic: using graphs to create an AI
  SDLC. One is how the model gets written, the other is what the model then runs. An earlier draft of
  this record said "absorbed by", inferred from the two being ruled on together; that was wrong.
  **Both halves are delivered (Eric, 2026-09-17): the AI companion is production quality, with
  measured success in field installations.** An earlier draft of this record called the maturation
  half undelivered because `PostCompanionCommandSync` is still `@OptionalService("app.env=dev")` —
  a wrong inference. In Eric's words: **"the human–AI collaboration for graph prototyping and
  productization has matured. It runs in dev mode. After promotion to production, the UI is
  disabled."** Dev-gating is therefore the *designed lifecycle*, not a shortfall — the Playground is
  where humans and an AI prototype and productize a graph; what promotes is the *graph*, and the
  authoring surface is switched off behind it ([[minigraph-dev-mode-app-shape]] — removing that one
  line is how the surface is closed). "Mature into a governed collaboration layer" meant
  collaboration quality and certification, not un-gating an endpoint. So the Vision bullet "Human–AI
  and human–human collaboration is first-class" is **realized**, and the altitude drift briefly
  flagged against this close does not exist.
  → served: vision-mercury-composable
  <!-- id: bp-ai-companion-llm-backend | created: 2026-06-20 | last_used: 2026-09-17 | uses: 4 | tier: active -->
- [x] (blueprint) **CLOSED 2026-09-17 (Eric, at the first closure gate)** — enterprise governance
  lifecycle for graph models (dry-run → certify → stage → approve → production). **Closed as part of
  AI companion maturity:** this gap *is* the human–AI collaboration and product-owner certification
  process, and that is delivered alongside the companion — production quality, with measured success
  in field installations ([[bp-ai-companion-llm-backend]], closed the same day). CompileGraph is the
  deployment quality gate in the running engine; the certification half is the human process the
  companion now supports. Not a documentation task and not deferred — realized.
  → served: vision-mercury-composable
  <!-- id: bp-graph-governance-lifecycle | created: 2026-06-20 | last_used: 2026-09-17 | uses: 4 | tier: active -->
## Open Threads

> Open Threads live **one per file** in `memory/open-threads/` (`thread-<id>.md`;
> filename = the thread's fact id) so concurrent thread work never merge-conflicts
> (v4.39.0). List that directory to see them; unchecked `- [ ]` threads are the live
> workstreams and never decay. Mark a completed thread `- [x]` in its file and leave
> it — the review sweeps it to the archive once older than `archive_window` sessions.
> Don't archive by hand. See `.agent/schema.md`.


## User Preferences

- **Release rhythm (Eric; established by 2026-07-25):** Claude Code prepares every release
  artifact — branch, version sweep, build verification, CHANGELOG, release notes — but
  never merges, tags, or publishes without Eric's explicit go-ahead for that specific
  step; PR-open and tag/publish are each individually gated. (Materialized into shared
  memory 2026-08-22: the smoke test found six session logs referencing this id while the
  fact lived only in an agent's personal store — a project-relevant working rhythm belongs
  in the shared layer.)
  <!-- id: eric-release-rhythm | created: 2026-08-22 | last_used: 2026-09-02 | uses: 29 | tier: core | origin: 2026-08-22-180334 | note: promoted to core 2026-09-04 (Eric): an operating preference that does not decay in relevance — and the sole User Preferences fact, so archiving it would fail the orientation question outright -->

- **Code changes go through a PR; memory-only commits may go straight to main (Eric, 2026-09-16).**
  Every code change gets a branch and a pull request, so CI runs and the change is reviewable — even
  a small, fully-verified one. Memory commits (`memory/`: session logs, continuity, threads) continue
  to land directly on main, which is the long-standing practice the protocol's own
  `git add memory/ && git commit` guidance assumes and which the history shows throughout.
  The ruling followed the one exception: `d351b2ab` (test fixtures reading their secret from the
  environment) was pushed straight to main because the instruction was "commit and push" and the
  branch was already main. It was locally green, but it bypassed PR CI — so the rule is now explicit
  rather than inferred from what happened to be convenient. Distinct from
  [[eric-release-rhythm]], which gates merge/tag/publish; this governs how *any* change reaches main.
  <!-- id: eric-code-changes-via-pr | created: 2026-09-16 | last_used: 2026-09-16 | uses: 1 | tier: core | origin: 2026-09-17-011049 -->

## Team / Members

(none recorded yet)
