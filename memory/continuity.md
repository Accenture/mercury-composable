# Continuity — mercury-composable

> Shared ground truth for project state across all agents and sessions.
> Update at the end of every session. Never delete — only archive (see `REVIEW.md`).
>
> Each fact carries a metadata footer in an HTML comment, maintained by the review
> ritual — invisible when rendered, read/written by agents:
> `<!-- id: <kebab-id> | created: YYYY-MM-DD | last_used: YYYY-MM-DD | uses: N | tier: active -->`
> (the id is a placeholder on purpose — before agent-memory v4.41.2 a literal id here was parsed as
> a live fact and inflated the review's fact count; see `conv-schema-example-not-a-fact`)
> See `.agent/schema.md` for the fields and `memory/decay-policy.md` for the windows.
> Condensed 2026-07-31 (line-bloat review advisory): shipped-item narrative compressed
> to essentials; full detail lives in each fact's `origin` session log.

---

## Project State

- **project:** mercury-composable
- **status:** active, mature framework (Maven reactor)
- **repo:** github.com/Accenture/mercury-composable (official — source of truth)
- **latest_release:** v4.12.30 (2026-11-05 23:41:17Z — lock-step with Rust 4.12.30: the Playground panel name from
  `playground.panel.name` (#617, [[playground-panel-name-meta]]), the AI companion broker - renamed from the Playground session
  broker, kept once in `templates/starter-graph`, with `POST /subscribe` - and the AI agent guide's recommended collaboration
  start (#618, [[conv-one-copy-of-shared-tooling]]), `websocket.idle.timeout` documented in seconds (#619); release PR #620
  squash `ea06d6bc` = tag; the GitHub release published, its notes the CHANGELOG section. **READ:** a note naming
  `scripts/playground-session-broker.mjs` names `scripts/ai-companion-broker.mjs`, run from `templates/starter-graph`; a
  `websocket.idle.timeout` written as minutes is multiplied by 60. Sweep 52 build files / 118 plus the lab's three literals.
  Readiness: `mvn -o clean install` 277 suites, 3007 tests, 0 failures. Rust 4.12.30 the same day (mercury #409 merge
  `852c2977` = tag, thirteen crates). The packs stay at 4.12.21. **Field result 2026-11-06: Sonar reported five new-code smells,
  one a Blocker, and the field's pipeline rejected the build; closed by #622 (squash `4b92dd2b`, [[conv-sonar-gate-heuristics]]),
  which Eric patches into the field's pipeline - no new release.** Origin 2026-11-05-234314.md.
  **Prior releases, one line each (the full record is each origin log and the CHANGELOG; the version source stays the root pom.xml):**
  v4.12.29 (2026-11-05, Java only: the scheduler's conditional claim keyed on `_origin` #612/#613/#615, the agent loop and
  RFC-0011 step 3 #605/#606/#609/#611, `TargetRoute` a record #614; #616 squash `39c90da8`; 3002 tests; origin 2026-11-05-065411) ·
  v4.12.28 (2026-11-03, Java only: the scheduler's `RetryRequest` and `_firing` #603, the LLM relay's folder #601; #604 squash
  `d28077b8`; FIELD-DEPLOYED; origin 2026-11-03-212127) · v4.12.27 (2026-10-30, the field's Snyk fix for 4.12.26 - Tomcat 11.0.27
  with Boot 4.1.2 #594 - a job's time zone #599, the `minimalist-kafka` round #597/#598; lock-step with Rust; #600 squash `2929ea31`;
  FIELD-DEPLOYED, CI, Snyk and Sonar passed; origin 2026-10-30-231959) · v4.12.26 (2026-10-26, JSONPath to RFC 9535 ADR-0031, live
  invocation observers ADR-0032, the date plugins, `reactive-postgres` batches; lock-step with Rust; #593 squash `50964467`; the
  field's Snyk FAILED on Tomcat 11.0.26, fixed in 4.12.27; origin 2026-10-26-170700) · v4.12.25 (2026-10-13, Java only: the field's
  Sonar findings on 4.12.24 closed; #562 squash `f496291e`; FIELD-DEPLOYED 2026-10-14; origin 2026-10-13-021606) · v4.12.24
  (2026-10-12, the Guava resolution the field's Snyk gate rejected in 4.12.23 - [[conv-library-pins-are-direct-dependencies]] -
  and the range plugins' bounds; lock-step with Rust; #559 squash `e1940a4d`; Snyk PASSED, Sonar FAILED, fixed in 4.12.25; origin
  2026-10-12-223627) · v4.12.23 (2026-10-11, the API playground and `f:rangeLookup`; #551 squash `79995691`; REJECTED by the
  field's Snyk gate, Guava 32.0.1 through Confluent; origin 2026-10-11-034330) · v4.12.22 (2026-10-10, the graph contract on both
  engines ADR-0029 [[graph-contract-by-declaration]] and the scheduler's active environment ADR-0030; #542 squash `67e573e8`; READ:
  a root `schema` is validated from this release on, `graph.schema.validator` is reserved; origin 2026-10-10-021202) · v4.12.21
  (2026-10-07, graph sets ADR-0027 and the engine's own MessagePack codec ADR-0028 [[minimalist-msgpack-codec]] on both engines,
  the packs caught up from 4.12.15; #528 squash `b76db298`; READ: `org.msgpack.*` no longer transitive, every decoding failure
  an `IOException`, HTTP 400 past 64 nesting levels or with extra bytes; FIELD-ACCEPTED 2026-10-08; origin 2026-10-07-013257) ·
  v4.12.20 (2026-10-01, exact decimal arithmetic ADR-0025 [[decimal-statement-exact-arithmetic]] and the deterministic package
  format ADR-0026 [[canonical-packager-wire-contract]]; #485 squash `9e515825`; READ: a numeric-looking string compares as a
  number and `round(-2.5)` is `-3`; origin 2026-09-30-221603) · v4.12.19 (2026-09-25, the rapid-prototyping deploy lane on both
  engines [[graph-manifest-list-later-wins]]; #466 squash `a261ff18`; FIELD-ACCEPTED; origin 2026-09-25-232807) · v4.12.18
  (2026-09-25, graph.math typed and finite plus CONDITION [[graph-math-typed-arithmetic]] - READ: `true` never computes as 1/0,
  `Infinity` never propagates - and the field's Snyk bumps #463; #464 squash `1e419a29`; origin 2026-09-25-194902) · v4.12.17
  (2026-09-25, the consumer-side Schema Registry identity [[kafka-consumer-registry-identity]] - READ before opting in; #461
  squash `e9cde291`; origin 2026-09-24-234713) · v4.12.16 (2026-09-24, the correctness round from two field reports #456, the
  embedded-Redis OpenSSL docs #455; #457 squash `df605533`; origin 2026-09-23-234558) · v4.12.15 (2026-09-23, the connected-trace
  release on all four runtimes [[connected-edge-spans]], [[platform-onshutdown-lifecycle]], [[minigraph-dev-mode-app-shape]];
  #451 squash `aafeff04`; origin 2026-09-23-014650) · v4.12.14 (2026-09-22, the first lock-step release with the Rust port;
  `group.protocol=${KAFKA_GROUP_PROTOCOL:auto}` [[kafka-group-protocol-auto-default]] - READ: an app that never set it joins a
  KIP-848 cluster with the consumer protocol; #437 squash `dbc26f31`; origin 2026-09-21-233928) · v4.12.13 (2026-09-21, 11
  improvements, #435, FIELD-ACCEPTED) · v4.12.12 (2026-09-17, the produce-only unblock, #410) · v4.12.11 (2026-09-16, the field
  unblock, #405, [[otel-optional-service-and-negative-control]]) · v4.12.10 (2026-09-16, Berkeley DB store retired, ADR-0024,
  #400) · v4.12.9 (2026-09-16, the distributed-Redis release, #364–#397; ACTION: the `soa.redis.health` route rename,
  `minimalist-kafka` no longer transitive).
- **last_enabled:** 2026-06-20
- **last_review:** 2026-11-14 | through 2026-11-14-074819.md (SIZE — continuity passed 1000 lines after the ADR work; 4
  sessions since 2026-11-13-030403: `refresh-metadata` refreshed 11 footers, tier changes 4 (`agent-loop-tool-names-map-to-routes`,
  `graph-failed-resume-readvises` and `conv-test-fixed-port-outside-ephemeral` → active; `release-4-12-30` → archive-candidate);
  archived 0: `otel-optional-service-and-negative-control` (22 sessions) KEPT as a declaration gap — the 2026-11-14-065413 live drive
  applied its negative-control-first method without naming it, the window's three commits on its paths were release sweeps and
  an IDE round; swept 0 (the three threads closed 2026-11-05 are 10 sessions old); invariants not due (3 sessions since
  2026-11-13-031614); stalled threads: `ai-sdlc-document-ingestion` (41) → [[close-stalled-threads-20261114]] raised for Eric;
  contradictions none; smoke test not run. Live facts 48, decaying 28 of a 35 limit; continuity stays above 1000 lines because the
  facts are long, not many - the next lean-up is the sweep of the three 2026-11-05 threads once past the window.)
  Prior: 2026-11-13 | through 2026-11-13-030403.md (ON COMMAND — refreshed 11 footers, tier changes 6; archived 1 superseded
  fact and 1 faded; declaration gaps none; live facts 47 → 45; then Eric promoted `clean-knowledge-design-over-engine-coverage`
  to `core`, so it moved back from the archive: 46.)
  Earlier reviews, each summarized in its own session log: 2026-11-10-015436 (on command + cadence; 23 footers, 10 tier
  changes, 1 superseded archived, 2 threads swept), 2026-11-04-050444 (cadence; the invariant gate raised), 2026-11-03-010235,
  2026-10-29-012834, 2026-10-24-030134, 2026-10-22-180311 (10 faded facts archived; lines 879 → 655), 2026-10-22-012004,
  2026-10-14-000312, 2026-10-11-034330, 2026-10-09-032243, 2026-10-06-012541, 2026-10-05-230033 (the first step-6
  declaration-gap run), 2026-10-02-185029, 2026-09-28-230310 (smoke test 12/12), 2026-09-25-014100 (`instant-serialization`
  demoted from core and archived), 2026-09-24, 2026-09-23 (×2), 2026-09-22.
- **vision_evolved:** 2026-09-17 (Eric approved) — `memory/vision.md` now states **two tracks**: Track 1 *knowledge graph as
  application* (deterministic — rules, business logic, outcome; L3 leverages L2 + L1) and Track 2 *knowledge graph as AI SDLC*
  (governed AI processing for ambiguity a deterministic program cannot handle; L3 is the foundation and **AI is also the
  runtime for certain nodes**). The tracks echo each other — shared co-design, certification and promotion — and differ only
  in the nature of the work and where execution goes; AI gains a **run-time participant** role. **The north star MOVED: the
  next invariant re-verify must check the 2026-09-17 text, not the 2026-06-20 one.** [[bp-agent-orchestration]] is Track 2's
  Blueprint gap; Track 1 and the collaboration foundation are delivered.
- **last_invariant_check:** 2026-11-13 | 2026-11-13-031614.md (COMPLETE — Eric walked the 20 `core` facts and the Vision (the
  2026-09-17 two-track text) against live-tree evidence at `aaac2370`, the gate [[reverify-invariants-20261104]]; ALL
  CONFIRMED. Java 21 stays the toolchain until most field installations run Java 25; `trace-thread-keyed-mono-gotcha`
  gains its second instance (#609, async fan-out); the Vision's Track 2 status refreshed (the agent loop shipped in both
  engines); core 19 → 20 with `clean-knowledge-design-over-engine-coverage`, promoted the same day. Cadence 40; the
  next re-verify is due 40 sessions after this log.)
  Prior walks: 2026-11-04-050444 (the gate raised; walked 2026-11-13) · 2026-10-22-234147 (ALL CONFIRMED against the 2026-09-17
  two-track Vision; `event-api-local-routes-only` and the Vision's Type line reworded; core 19) · 2026-10-09-045453 (ALL
  CONFIRMED; `stack-messaging-kafka` and `conv-serialization-gotchas` reworded for ADR-0028) · 2026-09-25-022841
  (`instant-serialization` demoted from core; 19 → 18) · 2026-09-16-041500 (`virtual-threads-rpc` enriched from ADR-0024;
  18 → 17) · 2026-09-11-005808.
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
  (+ demos); MsgPack wire serialization through the engine's own `minimalist-msgpack` codec since ADR-0028 (2026-10-06); customized Gson.
  (Wording refreshed 2026-08-21 and 2026-10-09 at invariant re-verify — substance unchanged.)
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

- **A jar under a base scan package needs an `@OptionalService` master switch, and a vendor integration is not done until a
  negative control proves the happy path (2026-09-16, Eric's design → PR #404, v4.12.11).** `opentelemetry-forwarder` lives
  under `org.platformlambda`, so the jar alone auto-registered `distributed.trace.forwarder` — carrying the dependency
  silently turned trace export on. It is now `@OptionalService("otel.forwarding")`, default **off**: one artifact ships and
  DevOps decides per environment (properties or `-Dotel.forwarding=true`); that is the reusable shape for any scanned
  extension whose behaviour is an operational choice, and `composable-example` pins "dependency present, feature off". The
  legacy `otel.trace.forwarder.enabled` was RETIRED (a second switch with no reachable use; `Telemetry` already no-ops on an
  unregistered route). Credentials resolve **per export** through a `Supplier` ([[preload-before-mainapp-lazy-config]]) and
  the exporter closes via [[platform-onshutdown-lifecycle]]. **The method is the durable half.** Certified live against
  Dynatrace SaaS and confirmed queryable in its UI; getting there needed an **A-B-A credential experiment** — real token 0/6
  export failures, bogus token 6/6, real token 0/6 — because *zero failures proves nothing until a failure is shown to be
  possible*. Generalize it: **when a verification is blocked on access you do not have, ask what your evidence would look like
  if the thing were broken; if broken and working look the same, a negative control is the experiment, not a garnish.** Two
  by-products worth keeping: the app returned HTTP 201 in all three legs (a telemetry outage degrades observability and
  nothing else), and the backend-visible instrumentation scope version is a free check that the artifact under test is the
  one that shipped — Dynatrace support confirmed the v4.12.11 field-acceptance traces reading scope version **4.12.11**
  ([[ot-otel-acceptance-traces-pending]]). Report: `docs/test-reports/otel-dynatrace-certification.md`; closes
  [[ot-otel-dynatrace-certification]]. Splunk's header form is parsed and documented but NOT run live. **Extended
  2026-09-22 (v4.12.15):** the forwarder exists on all four runtimes (the Rust port's, and the zero-dependency ports of its
  OTLP encoder in mercury-python #33 and mercury-nodejs #101), certified together in Scenario 8 (four token-bearing traces,
  cross-application lineage, 0 export failures). Lesson: the LLM provider, not the pipeline, decided which calls succeeded —
  probe and pin the model per drive. Eric's Dynatrace review then found the trees broken at the root; the fix is
  [[connected-edge-spans]].
  <!-- id: otel-optional-service-and-negative-control | created: 2026-09-16 | last_used: 2026-11-14 | uses: 38 | tier: active | origin: 2026-09-16-193203 -->

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
  **Applied 2026-09-23 to the toolchain (Eric):** embedded-redis's bundled macOS-arm64 `redis-server` is linked against Homebrew
  OpenSSL 3 (Linux x86-64 needs `libssl.so.3` + glibc 2.34, both on `ubuntu-latest`; the rest are self-contained), so a Mac
  without it fails `mvn clean install` in `extensions/redis-connection`. Eric REJECTED the
  fallback-chain module of the experiment branch `fix/embedded-redis-apple-silicon` (never merge it): a dev-machine prerequisite is
  DOCUMENTED (PR #455, squash `11bba2fd`), not engineered around, while CI and the field pass; the library's default provider
  ignores `EMBEDDED_REDIS_EXECUTABLE`.
  <!-- id: clean-knowledge-design-over-engine-coverage | created: 2026-09-18 | last_used: 2026-11-13 | uses: 37 | tier: core | origin: 2026-09-18-174943 | note: promoted to core 2026-11-13 (Eric): a standing design posture that does not decay in relevance -->

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
  **Second instance (2026-11-03, #609, the asynchronous `graph.extension`):** with `for_each`, the thread that starts each
  later run of the sliding window has no trace context, so the run events carry the node's span explicitly.
  <!-- id: trace-thread-keyed-mono-gotcha | created: 2026-06-28 | last_used: 2026-06-28 | uses: 1 | tier: core -->

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
- **EventApiService serves only the routes it reaches itself — local, or over the Kafka mesh when
  `cloud.connector` is on — and never relays through its own Event-over-HTTP map: an inbound
  `/api/event` call to any other route answers 404, even when the instance's own
  `yaml.event.over.http` map points that route at a peer (Eric ratified 2026-08-30; wording
  refreshed 2026-10-22 at invariant re-verify, substance unchanged).**
  Forwarding would make every app an Event-over-HTTP relay and open routing loops; the
  `x-event-api` wire marker is the existing loop guard (an event that crossed the wire
  once is never re-forwarded), the map is caller-side knowledge (not a promise to third
  parties), and deliberate hop-through is an explicit relay function (demo:
  `hello.remote.relay`). Recorded in the progressive-rendering interop report.
  <!-- id: event-api-local-routes-only | created: 2026-08-30 | last_used: 2026-08-30 | uses: 1 | tier: core | origin: 2026-08-30-050040 -->

- **The Playground webapp has one source and two deploy targets, and the help pages are one consolidated set (Eric, 2026-10-02; PR #496 squash `a6dc9ce5`, the Rust twin mercury #347 merge `781fae43`, both MERGED 2026-10-02).** The webapp "graduated": its scoped
  memory bank, steering files and pre-graduation specs under `system/minigraph-playground-engine/webapp/` are retired, its technical documentation is regenerated from the code in `webapp/docs/` (README,
  architecture, protocol, components, graph-view, hooks-and-state, build-test-deploy, extending; 1,336 lines), and its session memory and open threads live at this root. The Rust repo's verbatim copy
  (`crates/knowledge-graph/webapp/`, K7 of the port spec) is retired: `npm run release:rust` in the webapp (or `release:all`, both engines from one build) deploys the bundle to
  `../../../../mercury/crates/knowledge-graph/resources/` (`MERCURY_RUST_REPO` overrides; `scripts/targets.js` resolves the target and fails clearly when the repo is absent) and MIRRORS
  `src/main/resources/help/` there, because the help is compiled into the bundle AND read by both engines at run time. `release` alone stays Java-only, so a contributor without the Rust checkout is
  unaffected. **Why one help set (Eric's direction, my recommendation agreed):** the two sets had drifted on 38 of 42 pages (~3,700 lines) while only ONE fact was a real engine difference; the Rust
  2026-07-19 rewrite (Syntax/Example/Notes) is the base, Java-only content kept, wording engine-neutral, differences stated in place (`graph.js` deprecated here, not registered in Rust). Three stale Java
  claims fell out (`round()` "follows Math.round" — half up away from zero since #472; "an instance runs once" — `run` repeats and `model.*` persists; tutorial 12 titled "Tutorial 10") and one Rust claim
  (`session reset` keeps the draft — both engines clear it). The bundles are byte-identical on both engines (`index-lxX8FQ68`). **Found there and fixed the same day (PR #497 squash `029e5a9f`, deployed to Rust by mercury #348 merge `67a31296`, both MERGED 2026-10-02):** the workspace clipboard paste wrote scalars as
  `key[]=value`, which the engine stores as one-element lists (`skill` included); the paste now reuses the node editor's conversion and the authoring builder, the one serialization boundary, and a node
  the grammar cannot carry is refused with "Paste failed" (bundle `index-CN-KsNrA`). Extends [[help-edit-needs-bundle-release]]; closed the three Rust-twin threads (a port twin of a UI change is a deploy
  once the UI has one source). Applies [[conv-ports-adopt-java-release-number]] (the Java repo is the reference).
  <!-- id: playground-webapp-single-source | created: 2026-10-02 | last_used: 2026-11-05 | uses: 27 | tier: archive-candidate | origin: 2026-10-02-175751 -->

- **The mini-scheduler's state resolver picks one instance with a conditional claim in `expires`, and its contract
  changes only inside the existing `parameters`, where a key that starts with `_` is the system's (Eric, 2026-11-03 and
  2026-11-04).** The field stores the resolver's requests in a database whose schema follows the contract, so a new
  header would be a schema change in every field installation. The scheduler sends the asking instance's origin id
  (`Platform.getInstance().getOrigin()`) as `_origin` in the parameters of `expires` and `start`, logged at startup;
  it replaced 4.12.28's per-firing `_firing` (Eric's proposal). A slow resolver is retried (`RetryRequest`: 3 s, 3
  retries, timeouts only, each logged) rather than given a longer timeout. **The claim:** `expires` claims the job in
  ONE conditional write on a unique key on the job's name, succeeding only when the window has passed or `_origin`
  already holds the claim, so the database picks one winner and the loser is answered at once. The README's worked
  examples (PostgreSQL `INSERT ... ON CONFLICT ... DO UPDATE ... WHERE`, MongoDB `updateOne` upsert with the duplicate
  key as the refusal) each won exactly once per round with 8 instances at the same instant. **What fails, measured
  2026-11-04:** read-then-write ran a job twice in 10 of 11 simultaneous firings and in 12 of 240 with
  `deferred.start` (the random delay picks the same second 1 time in 10); a write, a wait and a read-back (Fischer's
  timed mutual exclusion, Eric's proposal) holds only while every write lands within the wait, and a 1.5 s pause ran
  both in 20 of 20; an upsert without its condition, or a separate name field with a plain index, lets every instance
  win, so ACID alone picks no winner; a record lock held while answering makes the loser wait into the scheduler's
  timeout, the field's incident. **The window:** more than 18 s plus the database's latency (9 s of deferral, 9 s of
  retries), less than the job's period minus 9 s. `deferred.start` only spreads the questions. **How to apply:**
  before changing what the scheduler sends the state resolver or the environment store, ask whether a field
  installation would have to change a schema to accept it; an additive key in `parameters` is the shape. A resolver
  for more than one instance needs one conditional write, so a second read in it is the smell.
  <!-- id: scheduler-resolver-conditional-claim | created: 2026-11-04 | last_used: 2026-11-05 | uses: 4 | tier: archive-candidate | supersedes: scheduler-resolver-contract-in-parameters | origin: 2026-11-04-234824 -->

- **The scheduler example is the claim's runnable worked example on MongoDB, and its embedded MongoDB is a pure-Java
  emulator (Eric, 2026-11-05).** `StateResolver`, `EnvironmentStore` and `ScheduleAdmin` keep their records in MongoDB
  (`job_state`, one document per job with the name as `_id`; `environment`), the resolver's claim the README's MongoDB
  worked example word for word ([[scheduler-resolver-conditional-claim]]). The tests start `mongo-java-server` (test
  scope) and `helpers/mongodb-standalone`, in the root reactor by Eric's choice, runs the same server for a test drive
  without Docker. **Why pure Java:** the real `mongod` is a first-run download from fastdl.mongodb.org (80 MB on macOS
  arm64, 104 MB on Linux, 791 MB on Windows) that a VDI without Docker, CI and the field's pipeline would each have to
  reach; the emulator is a Maven dependency. Measured: with driver 5.8.1 and Netty 4.2.19 the claim race had one
  winner in 600 of 600 rounds; it has no sessions, so no transactions, and no `hello` command. Eric: the example uses
  a very small set of features, so the emulator is an excellent fit, and the helper's README says it is for dev and
  testing without container support only, emulating a subset of MongoDB. The same tests run against a real MongoDB
  with `-Dtest.mongodb.uri` (15 of 15 on 8.0.34). The 30-second window needs a period above 39 seconds, so the demo
  jobs run once a minute. **The driver is pinned at 5.14.0 by `mongodb.version`** in both poms (Eric; the IDE's
  dependency scan flagged Boot 4.1.2's 5.8.1, CVE-2026-88033 in sync and CVE-2026-18710 in core): a `<version>` on
  `mongodb-driver-sync` alone left `bson`, `mongodb-driver-core` and `bson-record-codec` at Boot's 5.8.1, measured,
  so the property is the pin, and a Boot bump compares it ([[conv-library-pins-are-direct-dependencies]]). **How to
  apply:** an example that needs a database gets an embedded server that comes from Maven Central, run once against
  the real database before it ships; a helper that emulates a product says so.
  <!-- id: scheduler-example-mongodb | created: 2026-11-05 | last_used: 2026-11-05 | uses: 1 | tier: archive-candidate | origin: 2026-11-05-045407 -->

- **The Playground entry page carries one server-filled tag: `playground.panel.name` renames the Minigraph panel
  through `<meta name="playground-panel-name" content="">` (a field installation's request, 2026-11-05).** The bundle
  ships the tag empty; the Java `get.index.html` fills it, escaped for HTML, in dev mode; `webapp/src/utils/panelName.ts`
  shows it in place of the config title, for the Minigraph playground only (`titleFromServer`). The default stays
  `Minigraph Playground` (Eric), and the browser tab and the JSON-Path playground keep their titles. A server-filled
  tag rather than a REST read: the page is already served by a function, so every application's `rest.yaml` stays as
  it is and the default title never flashes first. A page served with the tag empty keeps the default, so the bundle
  could be shared before the twin landed; the Rust engine fills the tag too since mercury #407 (Increment 200,
  `with_panel_name`, 2026-11-05), with the bundle and help deployed from here. **How to
  apply:** a per-deployment setting the web app shows goes into the entry page as a server-filled tag, found by its
  exact text and guarded by a test of the built page (`GetIndexHtmlTest`); any webapp source edit still needs the
  release ([[help-edit-needs-bundle-release]]).
  <!-- id: playground-panel-name-meta | created: 2026-11-05 | last_used: 2026-11-12 | uses: 3 | tier: active | origin: 2026-11-05-175904 -->

- **The agent loop: `llm.run` in the engines maps tool names to routes; the model turn and the MCP bindings live in the
  language packs (ADR-0033, accepted 2026-11-14 after the live drive through both packs).** A node whose model reasons with
  tools is `llm.run`, engine-side function code in the opt-in `extensions/agent-loop` (Rust: `mercury-agent-loop`), not core,
  linking no provider SDK, MCP client or store client. The tool catalog is graph data certified with the graph: each row maps
  an API-safe name to a composable route with a description written on the graph, a closed-subset input schema, `strict`,
  `write: true` or `ask: true`; the loop checks it and every route before the first turn and validates each input before
  dispatch. The model turn stays `llm.chat` in the packs with the loop contract (the append-only transcript in the pack's
  store, the model pinned at the first turn, a stale turn 409). The node's mode governs the use of tools (`write` default,
  `auto`, `manual`, `unattended`); a waiting turn runs none of its calls, the graph suspends, and the resume runs the calls
  from the restored state. Agent graphs are asynchronous (async `graph.extension`; progress events to the caller's return
  route). MCP bindings are pack routes, one per allowlisted remote tool, failing at boot on a missing tool or a changed
  schema digest. Design spec `draft-design-specs/llm-helper-agent-loop.md`; the runnable example is the Playground's
  `order-assistant` ([[graph-failed-resume-readvises]]). **How to apply:** a new tool for an agent is a route and a catalog
  row, never engine code; a new provider or MCP server is pack configuration; the hold on a write comes from the row's mark,
  so design safety on the catalog and the mode, not on the prompt; a tool whose reply carries a payload the model does not
  need (a rendered image beside its verdict) gets `result_limit` on its node - uncapped it cost 36,000 to 43,000 tokens a turn
  in the 2026-11-14 drive, capped at 8 KiB 4,300; better, a function of the application in front of the bound route stores the
  payload and returns a reference (the example's `diagram.render`, the SVG under `/tmp/rendered_diagrams/<reference>.svg`),
  which is the out-of-band shape the research rulings asked for. (ADR-0033)
  <!-- id: agent-loop-tool-names-map-to-routes | created: 2026-11-14 | last_used: 2026-11-14 | uses: 2 | tier: active | origin: 2026-11-14-071025 -->

- **A graph that resumes an agent loop re-advises on a failed resume, because `graph.resume` consumes the suspension record
  (2026-11-14, found live in sprint 3 WP1).** A resume request whose decisions the loop refuses - an unknown call id, a call
  decided twice, a decision for a call that did not wait - raises 400 before any token is spent, which is right; but the
  record was consumed on retrieval, so when the handler simply reports the error the run ends and the transaction is gone,
  while the pack still holds the conversation until its TTL. The `order-assistant` example's shape: the loop node's
  `exception=` handler notes whether an approval was pending (`text(={model.agent.status})` probe), and a decision after it
  sends such a failure back to the advice checkpoint (`RESET: advise` then `NEXT: advise`), which stages the waiting calls
  with the error and pauses again; any other failure ends the run with the error. Also in that graph: a resume without
  decisions, or naming another turn than the one that waits (`input.body.turn` against `model.agent.turn`), re-advises
  without a model call, and the turn is the idempotency key a supervisor's client echoes. **Declared, not engineered:** a
  loop that fails after the pack appended the results but before the model answered leaves the record's turn count
  moved, so a re-sent decision meets 409 and re-advises again; the operator then restarts the transaction. **How to
  apply:** every graph that suspends around `llm.run` needs a path from the loop's failure back to its advice while an
  approval is pending; design it on the graph, never by making the loop forgive a bad decision.
  <!-- id: graph-failed-resume-readvises | created: 2026-11-14 | last_used: 2026-11-14 | uses: 2 | tier: active | origin: 2026-11-14-065413 -->

- **The packs' trace records mirror the engines' callee and caller, so a span lineage is continuous from an engine into a
  pack (2026-11-14; Eric's criterion: for OpenTelemetry to be certified, the span lineage ought to be continuous end to
  end).** The first end-to-end lineage drive - the `diagram-assistant` graph on Haiku 4.5 through the Python and the Node
  pack, negative control first - found every engine record chained and the pack legs absent: an RPC over Event-over-HTTP is
  recorded by neither side by default, since `async.http.request` is in `skip.rpc.tracing` here and the packs suppressed the
  worker-side record of an RPC-served execution without the callee-side records this engine leaves (`http.request` for
  `POST /api/event`, `event.api.service`, the function's inbox record with the responder's span), and their replies carried no
  span for an inbox to chain onto. mercury-python #53 and mercury-nodejs #120 made the packs leave the same three records
  and carry the responder's span on the reply; the re-drive gave 20 records per run, all connected, the tree the same shape
  as with a Java callee (`docs/test-reports/agent-loop-trace-lineage.md`; the earlier Dynatrace report carries an addendum).
  Also settled there: an alias route (the MCP binding's `mcp.tool` shape) never appears in a trace - every trace field comes
  from the service entry the event was addressed to. **How to apply:** a certification drive's lineage check parses every
  record of the trace from every process and verifies each parent is a span some record owns - a tree without orphans can
  still be missing hops; a hop into any runtime must leave the callee's edge record, the ingress record and the function's
  record, or the backend shows the hop as time inside the caller; every traced execution leaves exactly one record, and the
  question for a new path is which side emits it.
  <!-- id: pack-trace-records-engine-parity | created: 2026-11-14 | last_used: 2026-11-14 | uses: 1 | tier: working | origin: 2026-11-14-115548 -->

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
  Third instance, a new cause (2026-10-21): PR #571's squash `e269a3cb` landed with the PR's STALE
  title, "…export the graph as a PNG or an SVG image". The review had dropped the SVG export, but
  `gh pr edit` is refused for the EMU account, so the PR still carried the old title and the dialog
  pre-filled it. Agent-side guard: when the agent cannot update a PR's title itself, the handoff
  says that the title on GitHub is now wrong and must be replaced in the squash dialog.
  Fourth instance, the trailer (2026-10-24): PR #586's squash `356b5190` carries the same `Co-Authored-By` line three
  times, from the contributor's commit and the agent's two commits pushed onto the PR branch. The first guard recorded for
  it, leaving the trailer off a commit on a PR branch, was WRONG: #588's squash `5b5cab91` then carried none, because
  GitHub builds the squash message from the commits' messages and does not copy the PR body's footer. Guard: every commit
  keeps the trailer, and the squash dialog trims the copies to one, the same review moment as the title.
  Fifth instance, where the extra copy comes from (2026-10-24): PR #591's squash `ca2344ae` carries three - one from each of
  its two commits and GitHub's own `Co-authored-by` below a `---------` line, which GitHub adds for a multi-commit squash. A
  clean squash keeps that last line alone (`138ae1be`), so the dialog holds one copy per commit plus one; the handoff says
  to delete everything above the `---------` line's trailer.
  Sixth instance, a handoff that went stale (2026-10-29): PR #595's squash `f950a4bd` carries three. The handoff, given when
  the PR held one commit, said to keep one `Co-Authored-By` line. A second commit was then pushed for round 5 without
  re-sending the handoff, so it no longer named the `---------` rule. Guard: a commit pushed onto an open PR re-sends the
  squash handoff, with the title on its own line and the trailer rule for the current commit count.
  Seventh instance, a PR whose kind changed after it opened (2026-10-30): PR #598 was opened as docs-only, then gained a
  `refactor` commit (the schema cache retired, a config key retired). The handoff gave a replacement title and the
  `---------` rule, but squash `d6319a3a` landed with the earlier `docs(minimalist-kafka)` title and four trailers, so
  `git log` shows a behaviour change as a docs change (the CHANGELOG states it correctly). Guard: once a PR is open, a
  change of another kind - a behaviour change on a docs PR - goes in a PR of its own, so its title is right by
  construction and the dialog has nothing to fix.
  Eighth instance, the rule was in the handoff (2026-10-30): PR #599's squash `a5bd2bf6` carries three trailers. The
  handoff, re-sent for the second commit, gave the title on its own line and said to delete everything above the
  `---------` line; the title held, the trailer step did not. So a handoff alone does not reliably fix the body. What would
  fix it by construction (not yet chosen by Eric): squash the PR branch to one commit before the merge handoff, so the dialog
  pre-fills one trailer, or set the repository's squash-message default to the PR title and description, with the trailer
  once in the PR body's footer (the fourth instance shows a commit-based default never copies it).
  Ninth instance (2026-11-04): PR #605's squash `b4e0f35d` carries five trailers - one per commit of four, plus GitHub's -
  with the `---------` rule in the handoff, after Eric chose to keep the commits rather than have the branch squashed.
  The title held; the body's trim did not. The repository default (the PR title and description) remains the open fix.
  Tenth instance (2026-11-04): PR #610's squash `89ffe5d9`, a two-commit PR that a side session opened, carries two
  `Co-Authored-By` lines, one per commit; the title held. A side session's PR needs the same handoff rule.
  Relates [[thread-otlp-export-retry]].
  <!-- id: conv-squash-title-prefill-check | created: 2026-08-19 | last_used: 2026-11-14 | uses: 132 | tier: active | origin: 2026-08-19-195244 -->
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
  repository. Corrected in both places via PR #411. **Third instance, a placement error (2026-10-21):** #572 and #574 each
  appended their entry to the first `### Fixed` below `## Unreleased`, which belonged to the SHIPPED 4.12.25 section, because
  Unreleased had no Fixed heading yet; #575 moved both. Before adding an entry, confirm its heading lies between `## Unreleased`
  and the first `## Version`, and add the heading when it is missing; after the edit, every released section still diffs empty
  against `git show <its tag>:CHANGELOG.md`. **Fourth instance, a separator (2026-10-22, the Rust repo):** RFC-0010 WP3's
  Changed 5, appended at the end of the Unreleased section, landed below the `---` that closes it (merged in mercury #391; WP4
  moved it back). The released sections still diffed empty, so that check does not catch it. After an append at the end of
  Unreleased, also confirm that the `---` directly precedes the first `## Version`, as it does at the tag. Relates [[conv-template-version-sweep]] (the
  sibling rule for the version sweep: re-derive, never carry the prior count forward).
  <!-- id: conv-changelog-from-tag-range | created: 2026-09-17 | last_used: 2026-11-14 | uses: 72 | tier: active | origin: 2026-09-17-183008 -->
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
  <!-- id: conv-template-version-sweep | created: 2026-09-11 | last_used: 2026-11-05 | uses: 40 | tier: archive-candidate | origin: 2026-09-11-005808 -->
- Watch serialization gotchas (Long↔Integer downcast — the in-house codec writes the smallest integer width, so it holds since
  ADR-0028; use `util.str2int/str2long`). (Wording refreshed 2026-10-09 at invariant re-verify — substance unchanged.)
  <!-- id: conv-serialization-gotchas | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->
- **Declare a Memory Reference when a fact is CONSULTED to make a decision, not only when it is edited (Eric agreed,
  2026-09-04) — since agent-memory v4.42.1 the protocol states the rule, and this fact keeps the local history.** The rule:
  `memory/PROTOCOL.md` (*Maintain memory while working*: a fact is relied on when it shaped a decision) and `DECAY.md` §2; its
  review-time half is `REVIEW.md` step 6, *declaration gaps* (the window's commits first, v4.42.2). Raised upstream from this
  repo and the Rust port on 2026-10-04 and adopted the same day (upgraded here by #505 and #506), the path the RFC rule took
  ([[conv-proposals-not-in-adr-ledger]]). Local origin: `2026-09-04-011530.md` declared `(none)` while reasoning from
  `conv-telemetry-presentation-parity`, and the next refresh demoted that 42-use fact to `archive-candidate` (found by the
  2026-09-04 smoke test; both endangered facts are now `core`). Follow-up: [[step6-release-sweep-gaps]].
  <!-- id: conv-declare-consulted-references | created: 2026-09-04 | last_used: 2026-09-04 | uses: 1 | tier: core -->

- **A proposal is not a decision: raise it in `docs/arch-decisions/RFC.md` as `RFC-NNNN`, never
  as a `Proposed` ADR (Eric, 2026-09-18).** The ADR ledger is an **immutable journey of decisions**,
  so an entry is written when a decision is *accepted* — never before. A proposal may be reshaped,
  merged or withdrawn, and a withdrawn proposal is a non-decision that would otherwise sit in the
  ledger forever looking like one. `RFC-NNNN` and `ADR-NNNN` are separate sequences: a proposal does
  not reserve an ADR number, because proposals and decisions do not map one-to-one.
  **For a few hours this was a local override of the agent-memory protocol's wording** ("propose a
  newer ADR … and wait for human approval"); raised upstream the same day, it was adopted as protocol
  **v4.41.2** (`memory/PROTOCOL.md` now says the ledger records decisions only and work under
  consideration lives in `docs/arch-decisions/RFC.md` with `RFC-NNNN` ids) and then as the optional
  **governance pair** seed in **v4.42.0** (`ADR.md` + `RFC.md` skeletons; `.agent/schema.md` §RFC.md).
  So the rule is no longer ours alone — the tool states it; this fact records the local instance and
  its history. The register was renamed `proposed.md` → `RFC.md` and `P-NNNN` → `RFC-NNNN` on
  2026-09-18 to match (Eric: industry convention), and `RFC.md` is the upstream skeleton byte-for-byte
  — status vocabulary `Open · Parked · Promoted → ADR-NNNN · Withdrawn`, entries never deleted, newest
  first — which replaces the first register's remove-on-promotion table. The rule is also stated in
  the ADR ledger's own header and in `RFC.md` — and both files are packaged into the AI contract snapshot (PR #424, squash `5337ee30`), which **link-checks every relative link inside the snapshot**: that check is what turned #421's unpackaged `ADR.md → register` link into a red main. Anything under `docs/` but outside `docs/guides/**` that the snapshot must carry is enumerated in THREE places — the `ai-contract-provider` pom include set, `skill/files.list`, and `SkillSnapshotTest`'s fixed extras — so a new doc page that the ledger links to is a Java change, not a docs-only one. Superseded/Deprecated handling is unchanged — that is the
  *post*-decision stage and stays in `ADR.md`, marked in place, never deleted.
  Adopted in the same sitting that accepted the five ADRs left at `Proposed` (0013–0017), which is
  what exposed the gap: a status that means "not yet decided" had accumulated for a month across
  decisions that had in fact all shipped. Shipped as PR #421 (squash `92ced8cc`).
  **Raised upstream 2026-09-18 and adopted as agent-memory v4.41.2 the same day** (Eric's direction — keep the
  engine and the tool consistent) as a guidance-only change, with replacement wording for both
  clauses and the note that the sharper case is the one we did NOT hit: a *withdrawn* proposal has no
  correct status in an ADR ledger — `Superseded` implies a successor, `Deprecated` implies it was
  once in force, and both are false. Relates [[eric-code-changes-via-pr]] (the sibling rule for
  how a change reaches main).
  <!-- id: conv-proposals-not-in-adr-ledger | created: 2026-09-18 | last_used: 2026-09-18 | uses: 1 | tier: core | origin: 2026-09-18-221732 -->

- **Downstream secrets are compartmentalized: nothing that identifies a downstream adopter enters the open-source Mercury repos
  or their memory (Eric, standing instruction, 2026-09-30).** The family (`mercury`, `mercury-composable`, `mercury-python`,
  `mercury-nodejs`, `mercury-go`) is public, so customer or company names and brands, project and repository names, people's
  names, and other project-identifiable data (pilot lengths and dates, internal RFC or ticket ids, business-domain specifics,
  hostnames, cloud accounts) never appear in code, tests, docs, commit or PR text, or `memory/` (session logs, continuity, open
  threads, archives, ADR and RFC entries). A downstream brand name is never recorded in session memory or in any open thread.
  **Why:** an adopter's identity is confidential and `main` is append-only, so a leak cannot be quietly undone. **How to apply:**
  refer to "a downstream adopter" or "the field" (defined in `memory/instructions.md`); describe the technical need, not whose it
  was; generalize a domain-specific example (financial contracts, not the adopter's niche); read an input document from a
  downstream project as source material to scrub, citing it only as "a downstream design draft", never by path or repo name; scan
  the diff (and staged memory) for source-material identifiers before a commit or PR, and if one leaks, stop and tell Eric before
  pushing. Rule text: `memory/instructions.md` Core Rule 6. First applied the same day to two downstream drafts (RFC-0001 and
  RFC-0002 in the RFC register, PR #470); all five sibling checkouts scanned clean, tree and commit messages. **Extended
  2026-10-16 (Eric):** the field's INFRASTRUCTURE COST AND SETUP ISSUES - an API credit balance, cloud account setup, expired
  credentials, model access gating - are outside this open-source project's control, are escalated to the field admin, and are
  never logged here either, in memory or in docs; a record says what the project did and learned. The 2026-10-16 research
  record was redacted accordingly, the sprint-1 records in all four repositories generalized, and the full rule text with
  the clause copied into the Rust port's, both packs' and mercury-go's `memory/instructions.md` as their Core Rule 6 (Eric,
  2026-10-16) - the whole family carries it.
  Relates [[eric-code-changes-via-pr]] (how a change reaches main).
  <!-- id: conv-compartment-downstream-secrets | created: 2026-09-30 | last_used: 2026-09-30 | uses: 1 | tier: core | origin: 2026-09-30-164034 -->

- **A library's `dependencyManagement` never reaches a consumer - a transitive version the library must control is a DIRECT
  dependency of that library, with the artifacts that declare the old one excluding it (Eric's fix, 2026-10-12; v4.12.24).** The
  4.12.23 Guava pin in `minimalist-kafka`'s `dependencyManagement` converged that module's own build and nothing else: Maven's
  nearest-wins in `twin-kafka`, the demos and every field application saw Confluent's Guava 32.0.1-jre two levels down and
  `platform-core`'s 33.7.2 three levels down, picked the old one, and the field's Snyk gate - one project per pom
  ([[snyk-retired-manifest-placeholders]]) - rejected the release on `twin-kafka`'s manifest. A direct dependency travels with the
  library and sits one level below it in every consumer, ahead of any transitive declarer. The shape: declare the version in the
  library that drags the old one in (`guava`, `failureaccess`, `tink` in `minimalist-kafka`), exclude it from the artifacts that
  declare it so ONE version appears in the tree, and keep `failureaccess` explicit at the version Guava itself declares - this is the
  SECOND Guava field incident; the first (4.6.2, archived `thread-guava-failureaccess-field-fix`) was a curated field resolver dropping
  the deep transitive node (`NoClassDefFoundError` on `InternalFutureFailureAccess`), so the pin stays even though plain Maven brings
  the artifact. `platform-core` declares no Guava (Caffeine is its cache; no source in the repo imports `com.google.common`). **The
  verification that holds:** `mvn install` the edited modules first (a consumer's tree reads the INSTALLED pom), then
  `mvn dependency:tree -Dincludes=<groups>` over the WHOLE reactor, counting versions - one of each - and the consuming modules' tests
  (the CSFLE round trip exercises Tink). A module's own clean build proves nothing about its consumers. **Two more cases
  (2026-10-21, the ASM/Vert.x/Netty fix):** (1) a direct pin that WINS still leaves the losing version in the verbose graph -
  `accessors-smart`'s ASM 9.7.1 stayed "omitted for conflict" under platform-core's 9.10.1 and IntelliJ's dependency scanner
  reported it - so exclude it from the artifact that declares it and check with `dependency:tree -Dverbose`; (2) every module's
  parent is `spring-boot-starter-parent`, whose BOM overrides a transitive version for each consumer, which is why
  `netty.version` is set in all 38 poms - and why `json-path` resolves 2.10.0 (Boot's) in 34 modules while platform-core
  declares 3.0.0. **Both ways round (2026-10-26, the Tomcat fix for 4.12.26):** Caffeine 3.3.1, declared in platform-core,
  reached no consumer (40 nodes stayed on Boot 4.1.2's 3.2.4) until `caffeine.version` joined `netty.version` in the 41 poms,
  and Gson had shipped that way in 4.12.26 (the nine poms without `gson.version` resolved Boot's 2.13.2, 20 nodes, under the
  2.14.0 the others set; now 2.15.0 in all 41); and an override written when it was ahead of Boot falls behind the next Boot - the Reactor BOM a pom imports itself
  (2025.0.7, which takes precedence over the parent's), `httpcore5.version` 5.4.3 and the literal `r2dbc-postgresql`
  1.1.2.RELEASE would each have held its library below Boot 4.1.2. So a Boot bump compares every property and pin the poms
  set with the new `spring-boot-dependencies` properties, and a library bump in platform-core asks whether Boot manages it.
  **A consumer's own pin hides an exclusion gap (2026-10-30):** the Guava pin five examples and lab modules kept after
  4.12.24 rewrote the Guava 32.0.1 that two Confluent encryption artifacts (`encryption-tink`, `dek-registry-client`)
  still declared to `version managed from 32.0.1`, so their verbose graphs looked clean while every application on
  `minimalist-kafka` saw it `omitted for conflict`. Removing the pins exposed it, and the exclusion went into
  `minimalist-kafka` itself. Before dropping a pin, compare `dependency:tree -Dverbose` with and without it. The same day
  `httpcore5.version`, equal to Boot 4.1.2's managed 5.4.4, was retired from its eight poms.
  Relates
  [[conv-template-version-sweep]] (the other reactor-wide sweep).
  <!-- id: conv-library-pins-are-direct-dependencies | created: 2026-10-12 | last_used: 2026-11-05 | uses: 11 | tier: archive-candidate | origin: 2026-10-12-221919 -->

- **The field's Sonar gate judges NEW code, and a round fixes the WHOLE report, because a pre-existing finding counts as new
  the next time its file is touched (the fifth to thirteenth rounds, 2026-10-12 to 2026-11-12; the earlier rounds are the
  archived `thread-sonar-*` threads). The rules, by finding:**
  - S1845 (Blocker): a field and a constant that differ only by case. Name a property-key constant `*_KEY` and a collection
    name `*_COLLECTION`; before renaming a field compare every field against every constant mechanically (lower-case both):
    `activeEnvironment` → `active` met `ACTIVE`, `types` met `TYPES`, `environment()` met `ENVIRONMENT`.
  - S125: a prose comment ending in `;`, or a `${...}` placeholder in javadoc, reads as code - end a comment with a period.
  - S6213: `record` is a restricted identifier even as a lambda parameter, a method or a test helper - grep new code for ` record(`.
  - S1450 / S3077: a field set in one method and read in a callback, or a `volatile` reference, is a final `AtomicReference`.
  - S6548 (Info): an intentional singleton carries `@SuppressWarnings("java:S6548")` and "Intentional singleton" in its javadoc.
  - Limits: S3776 cognitive complexity 15 per method; S5961 25 assertions per test method (a helper that asserts, such as
    `refusal()`, counts, and `assert x != null` → `assertNotNull(x)` adds to it); S103 120 columns.
  - S1168: a helper returns an empty array or an `Optional`, never null. S5778: one invocation per `assertThrows` lambda. S1192: a
    literal inside `List.of` counts against a constant. S6204: a loop that only maps is a stream, but `.toList()` is
    unmodifiable - where the list is later sorted, extended, handed to an application or changed by the data mapping use
    `new ArrayList<>(...)`, `Collectors.toCollection(ArrayList::new)`, `.sorted()` or `Stream.concat`, and a loop that reads the
    size of the list it fills stays a loop. S6035 `\\w`, S6353 `\\d`, S5998 a possessive quantifier on a repeated group.
  - S3358: a lambda in a ternary's branch that holds a ternary is nested - move the body into a method. S108: an empty
    `default -> { }` carries a comment. S5785 `assertNotNull` over `assertTrue(x != null)`. S1130: no `throws` the body cannot
    throw. S1221: a method named `equal` reads as a misspelt `equals`. A loop counting down with `i += by` for a negative `by` is
    flagged: pass the magnitude and write `i -= by`. S6885 `Math.clamp` (confirm min <= max). S3398: a private static method only
    a nested class calls moves into it. S6916 is NOT a false positive: a case body that is a lone `if` moves into a method the
    case calls, since Java 21 allows a `when` guard only on a pattern label. S6126: a `\n`-joined literal over several lines is
    a text block. S2095: a resource that lives as long as the application is a FIELD created by a method that returns it, closed
    by `onShutdown`. S2629: a method call passed as a log argument goes into a local first.
  - Kotlin: `require(...)` over if-throw, `map[key]` over `map.get(key)`, `?: throw` over `!!`, `Unit?` over `Void` for no output.
  - Java facts behind the IDE's findings: a `Boolean` is not a `Number`, so `instanceof Number && !(instanceof Boolean)` is dead
    code; an `instanceof` chain is a pattern `switch` with `when` guards; `keySet` plus `get` is `entrySet`; `setScale` takes a
    `RoundingMode`; a route name in javadoc is `{@code llm.chat}` and a package name in prose `{@code}`. A record nested in an
    interface is public, so a package-private type in its signatures (`Node` in `Expr` and `Selector`) is made public with a
    javadoc saying why - the fix that looked wrong and was right.
  - **Review the IDE's quick fixes against the sentence they replace:** its grammar fixes changed what 16 comments said ("may
    be null" to "maybe null", an instruction turned into a false statement, articles dropped, braces stripped, list items merged,
    a package name split); "boolean method is always inverted" renamed a method and left its javadoc; a class turned into a
    record renames its accessors, so ask who calls the type before writing an upgrade note (`TargetRoute` needed none). A
    refactor of an equality helper that a vector harness also compares with needs a fresh negative control.
  - False positives: "class never used" on a `@PreLoad` function; "envelop"; "resume" read as "résumé"; "never used" on a
    constant another module reads (the IDE scans one module). IntelliJ reports a `try` holding only a comment as empty (a helper
    that uses the resource keeps S2095 and S5778 satisfied) and cannot see a non-null proved in another method.
  Relates [[conv-library-pins-are-direct-dependencies]] (the Snyk half of the same gate).
  <!-- id: conv-sonar-gate-heuristics | created: 2026-10-12 | last_used: 2026-11-14 | uses: 32 | tier: active | origin: 2026-10-13-001452 -->

- **A help-page edit is not done until the Playground bundle is rebuilt and committed (2026-10-02; PR #491 squash `f9ddf3c7`; the Rust twin is mercury #346).** The Playground's help pages
  (`system/minigraph-playground-engine/src/main/resources/help/*.md`) are compiled INTO the webapp bundle (`webapp/src/data/helpContent.ts`, `import.meta.glob`), so an edit shows in the Playground only after
  `npm ci` (once) and `npm run release` in `system/minigraph-playground-engine/webapp` have written the bundle to `src/main/resources/public/` (under `assets/`) and the result is committed; only `index.html` goes
  elsewhere, becoming `template/playground.html`. This repo tracks the source maps too, so the new `index-*.js.map` goes in the same commit (the Rust repo ignores its maps). **CI never builds the webapp**, so nothing
  else catches drift: the bundle was last regenerated on 2026-09-23 (#456) while four `help graph-math.md` edits (#462 `CONDITION`, #467 the closed dialect, #471 `DECIMAL`, #480 the money loop) landed after it,
  and the Playground listed five statement types until #491. **The build is reproducible** (Node 22.12 with one harmless `react-router` engines warning): a rebuild of the unchanged sources reproduced every other
  committed file byte for byte, so a regenerated diff reads as the help text plus the `sourceMappingURL` comment (110 lines added, 8 removed in #491). The same PR added `DECIMAL` and `CONDITION` to the page's
  property list (the Rust help lists them) and corrected the webapp's scoped instructions (that scoped memory bank was retired on 2026-10-02; the rule's home is this fact and `webapp/docs/build-test-deploy.md`). **Any webapp source edit needs the release, a comment included:** the map embeds every source file, so it changes while the
  chunk and its hash stay put if only comments changed (#492: two comments in `helpContent.ts`, one map line). The regenerated bundle ships in the engine jar of the next release. NOT built: a CI step that
  rebuilds the bundle and compares it with the committed one, feasible because the build is reproducible; Eric has not asked for it. **Since 2026-10-02 the release has two targets**
  ([[playground-webapp-single-source]]): `npm run release:rust` or `release:all` also deploys the bundle and a mirror of the help pages into the sibling Rust repo, so a help edit is done when BOTH repos
  carry the result. **A bundler bump changes the bundle too (2026-10-21, #572):** Vite 8.1.5 → 8.3.5 moved Rolldown 1.1 → 1.2
  and changed every JavaScript chunk, its CommonJS interop helper included. A Dependabot PR cannot rebuild the bundle, so a
  maintainer pushes the rebuilt bundle onto that PR, A/B-tests the two bundles in a browser (the unit tests run against the
  source, never the bundle), and deploys the same files to Rust. **Run `npm ci` before a release when `node_modules` may be stale
  (2026-10-22):** it held Vite 8.1.5 against the lockfile's 8.3.5 after #572 was rebuilt in a worktree, and a build from it would not
  have reproduced the committed bundle; `npm ls vite` shows it. **A test-only dependency leaves the bundle alone, but its
  tests are then run by hand (2026-10-24, #589):** happy-dom 20.11.1 → 21.0.4 is the tests' environment, and no source the
  build reaches imports it, so no release was needed. No workflow runs the webapp tests, so a Dependabot merge of a test
  dependency has run none of them; after `npm ci`, `npm test` passed 490 of 490 on 21.0.4. `npm run build` alone writes only
  the ignored `dist/`, which compares with the committed bundle (`public/assets`, `template/playground.html`) and with the Rust
  copy, maps excluded, without touching a tracked file; both were byte-identical. Relates [[minigraph-dev-mode-app-shape]] (the deploy layout:
  only the entry page moved out of `public/`).
  <!-- id: help-edit-needs-bundle-release | created: 2026-10-01 | last_used: 2026-11-05 | uses: 37 | tier: archive-candidate | origin: 2026-10-02-015208 -->

- **A reactor build rewrites every example's `target/*.jar` in place, so an app running from one fails at the next class it loads
  for the first time (2026-10-22).** `mvn verify` repackages `examples/*/target/<name>-<version>.jar`. A JVM running from that jar
  keeps working on the classes it has already loaded, then throws `NoClassDefFoundError` (a `ClassNotFoundException` from Spring
  Boot's `JarUrlClassLoader`) for a class the jar does contain. Met when the Playground example ran from `target/` during an agent's
  reactor build: closing the browser tab sent the run's first WebSocket close frame (`WebSocketCloseStatus`), log4j then failed to
  print the error, and the Vert.x event loop thread died. **How to apply:** run an example from a copy of its jar outside
  `target/`. Before a reactor build, an agent checks for a running example (`pgrep -fl 'target/.*\.jar'`) and says so rather than
  rebuild under it. When a running app reports a missing class that its jar contains, compare the jar's change time
  (`stat -f %Sc`) with the error; the modification time is the reproducible-build timestamp, 1980. Normal close detection is
  sound (`Session ... closed (1000, ok)`, `SessionManagementTest`), and the idle check expires an unclosed session after 60
  seconds. Since 4.12.26 the WebSocket warning names the exception type.
  <!-- id: example-jar-rewritten-by-reactor-build | created: 2026-10-22 | last_used: 2026-11-14 | uses: 24 | tier: active | origin: 2026-10-22-232342 -->

- **A configuration key is never both a value and the parent of other keys (2026-10-23).** `ConfigReader` loads a
  properties file into a `MultiLevelMap`, where a node is either a value or a map. When `a.b=x` and `a.b.c=y` are both set,
  in either order, the map wins: `getProperty("a.b")` answers the map's text (`{c=y}`), not null and not `x`. A switch then
  silently stays off, and a validated value fails with a misleading message. Met twice:
  - `dynatrace.oneagent` beside its settings (PR #583: the switch is now `dynatrace.oneagent.enabled`);
  - `scheduler.environment` beside `scheduler.environment.store` (since 4.12.22: startup fails with "must be one word").

  Both test configurations carried the collision, and no test read the leaf next to its children.
  **How to apply:** a switch or a value whose feature has settings takes its own child name, `<prefix>.enabled` like
  `redis.cache.enabled`, so the prefix stays a parent only. Before adding a key, check that no existing key is its parent
  or its child. To audit the tree, collect the keys of every `.properties` file, every `getProperty` and `@OptionalService`
  literal and every configuration-reference heading, then list each key that prefixes another. The 2026-10-23 log has the
  scan; `cloud.connector.health` is a route, not a key. YAML cannot express the collision.
  <!-- id: conv-config-key-not-value-and-parent | created: 2026-10-23 | last_used: 2026-11-05 | uses: 6 | tier: archive-candidate | origin: 2026-10-23-215957 -->

- **A test server's fixed port stays below 32768, and a wait on its listen covers failure too (2026-11-04, PR #607).**
  `EventOverHttpStreamTest`'s mock peer listened on 58586 and waited on `onSuccess` alone. CI failed three times with
  "misbehaving-peer mock must start" (2026-10-06 twice, 2026-11-04), and the failure was first read as a slow runner.
  The cause was a refused bind. 58586 lies in the ephemeral range of Linux (32768-60999), macOS and Windows
  (49152-65535), so the client side of one of the suite's many loopback connections can hold it. A refused bind
  completes the listen's `Future` with a failure, which nothing waited on, so the wait expired and logged nothing.
  **How to apply:** bind port 0 when the test can learn the port. When a static config must address the server (the
  mock's `${sse.mock.port}` in `event-over-http.yaml`), use a fixed port below 32768 that no other test uses. Wait on
  the listen's own outcome, for example `listen(p).toCompletionStage().toCompletableFuture().get(...)`, so a refused
  bind fails at once with its cause. **Reading the evidence:** a timed wait that expires at its exact bound, on a
  runner whose neighbouring classes ran at normal speed, more likely saw a callback that never fired than a slow one.
  A macOS negative control must hold the port on `::` with `IPV6_V6ONLY=0`: a listener on `127.0.0.1` or on IPv4
  `0.0.0.0` alone lets Vert.x bind anyway.
  <!-- id: conv-test-fixed-port-outside-ephemeral | created: 2026-11-04 | last_used: 2026-11-14 | uses: 3 | tier: active | origin: 2026-11-04-031520 -->

- **A lab or dev-machine tool stays out of the root reactor (Eric, 2026-10-23).** The field's static code analysis scans
  the modules the root `pom.xml` declares, registers each one, and keeps scanning it for good. A module listed there
  becomes a long-term field scan target, and removing it later needs a placeholder pom, the reason the Spring Boot 3
  placeholders stay ([[snyk-retired-manifest-placeholders]], archived). A module outside the root reactor is never
  scanned and needs no placeholder.
  **How to apply:** a lab gets its own aggregator in its folder, as `lab/dynatrace-oneagent/pom.xml` does. It lists by
  relative path the Mercury modules the lab uses, so Maven builds them from source instead of resolving the released
  version of the same number from `~/.m2`, and installs nothing. Its literal version joins the release version sweep.
  Before adding any module to the root reactor, ask whether it ships to the field's artifact repository.
  <!-- id: conv-lab-outside-root-reactor | created: 2026-10-23 | last_used: 2026-11-05 | uses: 13 | tier: archive-candidate | origin: 2026-10-23-215957 -->

- **A tool that runs from the checkout lives once, in the starter template, and the examples point to it (Eric,
  2026-11-05: do not repeat ourselves).** The Playground session broker had three identical copies (the
  `starter-graph` template, the `minigraph-playground` and `distributed-cache-example` examples); now only
  `templates/starter-graph/scripts/ai-companion-broker.mjs` exists, and each example's `scripts/README.md` runs it as
  `node ../../templates/starter-graph/scripts/ai-companion-broker.mjs --target <its port>`. The template keeps it
  because a project copied from the template must carry its own. The same day Eric renamed it the **AI companion
  broker** (`playground-session-broker.mjs` before), since "broker" alone also reads as a Kafka or message broker. **How
  to apply:** before adding a script or tool to an example, look for it in the template and point to that copy; a
  resource an application packages and loads (a flow such as `graph-executor.yml`) is copied, because the app's
  build needs it on its own classpath.
  <!-- id: conv-one-copy-of-shared-tooling | created: 2026-11-05 | last_used: 2026-11-05 | uses: 1 | tier: archive-candidate | origin: 2026-11-05-201925 -->

- **Releases v4.0.0 to v4.5.0 live as tags and GitHub releases; their `release/v*` branches are gone (Eric,
  2026-11-12).** Tags began at `v4.6.0`; before it, each release was cut as a `release/v*` branch (149). Each now has
  an annotated tag `vX.Y.Z` at its branch tip (the tagger date the release commit's) and a GitHub release titled with
  the tag, its notes the version's CHANGELOG section after a line saying it was published retroactively, not marked
  latest. GitHub dates a release by its tagged commit, so these sort into history and `v4.12.30` kept the Latest
  badge; 216 releases and 216 tags. Then Eric had the 149 branches deleted, every tip held by a tag
  (`release/v4.2.24` pointed at the 4.2.43 release; `v4.2.24` marks `679ce398`, the last commit on `main` at 4.2.24,
  its message saying so); GitHub keeps only `main` and `gh-pages`. `main`'s CHANGELOG heads 4.2.41's section
  `4.2.42, 5/17/2025`, so its notes came from the CHANGELOG at its own tag. **Applied to the siblings 2026-11-13
  (Eric):** the Rust port's repository (the Java Mercury v1.12.8–v3.0.19 with v1.13.0; 2.9.0 was never released, so no
  tag), mercury-python (v1.11.39–v2.5.0) and mercury-nodejs (v4.2.40–v4.3.28), each recorded in its own memory
  (`historical-releases-tags-and-pages-rust`, `-python`, `-nodejs`); all four repositories keep only `main` and
  `gh-pages`, and their READMEs point to the tagged releases. **How to apply:** find an old release by
  its tag or release page, never a branch. A release's notes come from its CHANGELOG section, read at its tag when
  `main`'s heading disagrees, else from the next release's CHANGELOG when a heading is wrong or missing at the tag, the
  note saying so; a release published after the fact passes `--latest=false`; a branch for a version never released
  (its tip another release's commit, and no CHANGELOG section anywhere, like 2.9.0) gets no tag.
  <!-- id: historical-releases-tags-and-pages | created: 2026-11-12 | last_used: 2026-11-13 | uses: 2 | tier: active | supersedes: release-branches-historical-record | origin: 2026-11-12-134344 -->

- **Retired Maven modules need placeholder manifests for Snyk (2026-09-01, Snyk team +
  Eric).** Snyk keys a project on repository+branch+manifest path and never retires it —
  deleting a module freezes its findings on the last resolved dependency tree, failing
  the security gate forever. A parentless dependency-free `packaging=pom` placeholder
  re-tests to an empty graph (zero findings). Live at `system/rest-spring-3/` +
  `examples/rest-spring-3-example/` (PR #305) with relocation metadata to the Boot-4 twins;
  **release version sweeps must include these non-reactor poms deliberately.** Relates
  [[stack-integration-spring-boot4]]. **Reactivated 2026-10-23:** a lab stays out of the root reactor, so it never
  needs a placeholder ([[conv-lab-outside-root-reactor]]).
  <!-- id: snyk-retired-manifest-placeholders | created: 2026-09-01 | last_used: 2026-11-05 | uses: 35 | tier: archive-candidate | origin: 2026-09-01-022524 -->

- **Every port adopts the JAVA release number on catch-up — no downstream repo runs its own version
  sequence (Eric, 2026-09-16).** The Java repo is the reference implementation, so a version number
  identifies **content**, not "this engine's Nth release". This covers the Rust port AND the python
  and node language packs alike: whenever one is next updated, it is tagged at the Java number it
  caught up to — never at an intermediate number invented to represent partial catch-up. Consequence
  to read correctly: a port sitting below Java (Rust at v4.12.7, the python/node packs at 4.12.1,
  while Java shipped v4.12.9 — resolved 2026-09-21 when Rust caught up at v4.12.12 in one step) is **lag awaiting catch-up, not divergence**, and the gap is not a
  compatibility signal. Corollary for release notes and continuity entries: state a port's number as
  the content it currently carries, never as a separate cadence. The Java-is-reference principle this
  rests on outlived `conv-telemetry-presentation-parity` (retired 2026-09-16): Eric restated it
  directly when giving this convention, so it stands on its own. Governs the Rust half of
  [[ot-distributed-cache]].
  **When each port catches up (Eric, 2026-11-03):** the Rust port catches up closely once Java has a new release, because
  Java is the reference implementation. The language packs catch up when functionality crosses from the engine to the
  packs (telemetry, AI SDLC and the like), so their number may trail Java further than Rust's does. Both packs' Visions
  were amended to say so the same day, settling the versioning threads the 2026-11-03 reviews raised there.
  **No number without content (Eric, 2026-11-05):** the Rust port does catch up closely, and it takes a Java number only
  when it carries that release's content. 4.12.28 and 4.12.29 are Java only because their work is the mini-scheduler,
  which Rust does not have: there is nothing to catch up, so Rust stays at 4.12.27 rather than being raised
  artificially. The AI SDLC epic's features (RFC-0011) were first to wait for the epic ([[bp-agent-orchestration]]);
  the same day Eric brought every common feature since 4.12.27 across instead (the agent loop `llm.run`, the
  asynchronous `graph.extension`, progress events, the abort record, `playground.panel.name`: mercury #403-#407,
  Increments 196-200), so the epic does not gate a port's catch-up; only work the port has no counterpart for does.
  <!-- id: conv-ports-adopt-java-release-number | created: 2026-09-16 | last_used: 2026-11-14 | uses: 54 | tier: active | origin: 2026-09-16-003354 -->

## Blueprint  *(gap from Current State → Vision; `(blueprint)` threads serve `vision-mercury-composable`)*

- [ ] (blueprint) **AI agent orchestration ("graph engineering")** — the Active Knowledge Graph as the governed run-time for
  AI agents: LLM reasoning, MCP tools and inner-loop agents join graphs as wrapper-side functions (bounded-agency decision
  graphs; agent-as-node); suspend/resume is the human in the loop; CompileGraph and the promotion lifecycle are the governance
  answer ("governed nondeterminism", never a determinism claim). The engine core stays LLM- and vendor-free; adapters are
  functions ON the wrappers. Run-time complement to the design-time [[bp-ai-companion-llm-backend]]; builds on
  [[bp-polyglot-functions]]; compounds [[bp-graph-governance-lifecycle]]. Concept doc `draft-design-specs/ai-agent-orchestration.md`
  (Q1-Q8, E0-E4); direction ratified by Eric 2026-08-25. **Status framing (Eric, 2026-09-17): the graph-based AI SDLC is in its
  DESIGN phase** - the sole remaining Blueprint gap, deliberately long-horizon: an epic, not a task.
  **Delivered so far.** E0 (2026-09-01, [[ot-agent-orchestration-e0]]): the support-triage graph on `llm.chat`, the first live
  graph.task-to-wrapper drive; re-driven 2026-10-01 with real Claude through both engines and both packs
  ([[llm-helper-certification-java]]). Q8's transport (2026-09-12, [[ot-streaming-return-route]]). **Sprint 1, AWS Bedrock
  (2026-10-13/14):** the helper's `bedrock` backend over the AWS credential chain (mercury-python #43/#44, mercury-nodejs
  #111/#112), driven live through IAM with negative controls first (the certification report, Scenario 8 and Finding 7); Bedrock
  has two surfaces (`llm.aws.api`: `mantle` or `runtime`) and per-model agreements; the concept's Q2 Bedrock half is done. The
  2026-10-14 hold for newer models was lifted 2026-10-29: the Claude API backend serves every current model. **The agent loop
  (RFC-0011, 2026-10-28 to 2026-11-05):** `llm.run` in the engines maps tool names to routes, the model turn and the MCP
  bindings in the packs, agent graphs asynchronous - shipped in Java 4.12.29 and Rust 4.12.30 ([[agent-loop-llm-run]]).
  **Sprint 3 "the live loop" (Eric, 2026-11-14):** WP1 the first live drive of `llm.run` through the new `order-assistant` graph on
  Haiku 4.5 and Opus 5.5 through both packs, negative control first (#624 merged; `docs/test-reports/agent-loop-live-drive.md`);
  RFC-0011 PROMOTED to ADR-0033 (#625, [[agent-loop-tool-names-map-to-routes]]); WP2 the MCP bindings in both packs
  (mercury-python #51, mercury-nodejs #118, merged); the engine-side drive with an MCP tool in a graph's catalog RAN the same
  day on the public Mermaid Chart MCP server (Eric's choice) through the new `diagram-assistant` graph (#626 MERGED, squash
  `f4c1c30f6`) - **sprint 3 COMPLETE**.
  **The trace lineage (2026-11-14):** the loop's trace is continuous from the engine's REST edge into both packs and back
  after the packs mirrored the engines' callee records (mercury-python #53, mercury-nodejs #120;
  `docs/test-reports/agent-loop-trace-lineage.md`, [[pack-trace-records-engine-parity]]).
  Next: the packs' catch-up release, then the epic's next sprint (Eric's call).
  **Research 2026-10-15/16 (Eric's direction), the rulings that hold:** a large payload travels OUT OF BAND (a reference to a
  blob in a shared store that the helper dereferences); a conversion's verdict is a LIST of per-block outcomes with their own
  confidence; not every block enters the graph and every later round reads the previous round's output; other formats need
  tools made available to the graph, and common tools and MCP are components of this epic. The first lab experiment showed
  the model's segmentation is the unstable part, so a DETERMINISTIC SEGMENTER owns block identity, and agreement across runs
  on stable ids is the determinism measure. **The document-ingestion lab itself is field research outside the repositories
  (Eric, 2026-11-14): its thread closed; its results return here only as engine or pack work.**
  **What it shares with the closed companion gap, and what it does not (Eric's clarification).** *Shared:* the authoring
  lifecycle - an AI SDLC graph is co-invented by human and AI in the Playground in dev mode, the same prototyping surface,
  reused rather than rebuilt. *Different:* what survives promotion. For the companion the AI is a **design-time** participant
  only; for AI SDLC **some nodes carry AI skills**, so after promotion those nodes reach an AI agent through gateways, MCP
  servers or other tools: the AI is in the **production execution path**. That is why this is a separate epic - the authoring
  pattern is solved and inherited; the runtime posture is the open problem - and why the engine core stays vendor-free with
  adapters as functions ON the wrappers: the gateway/MCP edge is where vendor specifics live.
  → serves: vision-mercury-composable
  <!-- id: bp-agent-orchestration | created: 2026-08-25 | last_used: 2026-11-14 | uses: 34 | tier: working | origin: 2026-08-25-213703 -->
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
