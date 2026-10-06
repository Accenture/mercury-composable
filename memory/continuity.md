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
- **latest_release:** v4.12.20 (2026-10-01 03:11:27Z — **exact decimal arithmetic for money and a deterministic package format, lock-step with
  the Rust port**: release PR #485 squash `9e515825`, tag `v4.12.20` → `fc940bea` (two memory-only commits past the squash; the non-memory
  diff is empty), pom verified at the tag; the GitHub release is published (not a draft). **Content:** the `DECIMAL` statement #471
  ([[decimal-statement-exact-arithmetic]], ADR-0025), the `f:decimal*` plugins #475, `round` half-up #472, the `CanonicalPackager` #481
  ([[canonical-packager-wire-contract]], ADR-0026), the dialect docs #467, the money-loop and packager guides, and a late `Float` widening
  in the packager (Eric). **READ:** a numeric-looking string now compares as a number in `== != < <= > >=`, and `round(-2.5)` is `-3`;
  both reach graphs that never say `DECIMAL`. Sweep BUILD FILES ONLY 43 / 98 (the 4.12.19 shape). Readiness: full reactor
  `mvn -o clean install` BUILD SUCCESS, 231 suites, 1587 tests, 0 failures, 3 skipped (4.12.19 had 1520), then platform-core 525/0 after the
  Float change. **Lockstep:** Rust v4.12.20 the same minute (mercury #340 → merge `b4783c5b`, tag → `d63e102a`, release 03:12:43Z;
  Increments 143–147; 123 suites / 655 tests / 0 failed); the Java–Rust byte-for-byte interop on the 14 tutorials and 50 fixtures
  (`docs/test-reports/canonical-package-java-rust-interop.md`, composable #486 / mercury #341, docs only) found 0 differences with a
  14/14 negative control; the python/node packs need no change. Main CI green on both tag commits. **The twelve Rust crates are on crates.io at 4.12.20 (verified 12/12, published 03:21:45Z-03:21:58Z by Eric's `cargo publish --workspace` from the tag; the published
  platform-core tarball matches the tag).** Next: the field-acceptance wait (CI, Snyk, Sonar),
  and the AI SDLC/MCP backlog ([[bp-agent-orchestration]]). Origin 2026-09-30-221603.md.
  Prior: v4.12.19 (2026-09-25 23:58:19Z — the rapid-prototyping deploy lane on both engines; #466 squash `a261ff18`, tag → `35000ef2`;
  `graph.model.automation` takes a comma-separated list of manifests and the later one wins ([[graph-manifest-list-later-wins]]); 1520
  tests; Rust #333 → `ff6e269c`, crates 12/12; FIELD-ACCEPTED 2026-09-25 — the field's CI, Snyk and Sonar clean. Origin 2026-09-25-232807.md.)
  Prior: v4.12.18 (2026-09-25 20:56:19Z — two field reports answered on both engines; #464 squash `1e419a29`, tag → `70e00474`;
  #462 graph.math typed and finite + CONDITION ([[graph-math-typed-arithmetic]] — READ: `true` never computes as 1/0 and `Infinity`
  never propagates) and #463 the field's Snyk bumps (Jackson 2 BOM 2.22.3, Jackson 3 BOM 3.2.3, Netty 4.2.18.Final, MsgPack 0.9.12
  best effort — CVE-2026-90472 medium, no upstream fix); 1517 tests; Rust #331 → `568d71b2`, tag → `7d07e9bd`, Increments 140–141;
  crates 12/12. Origin 2026-09-25-194902.md.)
  Prior: v4.12.17 (2026-09-25 00:19:27Z — the field's Kafka gap closed on both engines; #461 squash `e9cde291`, tag → `8a13a02e`; the
  consumer-side Schema Registry identity #458/#460 + the sample #459 ([[kafka-consumer-registry-identity]] — READ before opting in);
  1514 tests; Rust #328 → `ad957930`, tag → `af9d6f30`, Increments 137–139; the flaky Rust tests hardened after it (mercury #329,
  Increment 140). Origin 2026-09-24-234713.md.)
  Prior: v4.12.16 (2026-09-24 00:08Z — the correctness round from two field reports; #457 squash `df605533`, tag → `dc0ee6fa`;
  the shared null-source rule, graph.math naming the variable, every abort carrying its reason (#456), the embedded-Redis
  OpenSSL docs (#455), JaCoCo 0.8.15 (#452); Rust #324 → `743d4ea2`, tag → `cc138af3`, crates 12/12. Origin 2026-09-23-234558.md.)
  Prior: v4.12.15 (2026-09-23 01:35Z — the connected-trace release, a lock-step round on all four runtimes; #451 squash `aafeff04`,
  tag → `b705e9ff`; [[connected-edge-spans]], [[platform-onshutdown-lifecycle]], [[minigraph-dev-mode-app-shape]]; READ notes in
  the CHANGELOG; Rust #322 → `87ee371f`, packs 4.12.1 → 4.12.15; registries the same night. Origin 2026-09-23-014650.md.)
  Prior: v4.12.14 (2026-09-22 — the first lock-step release with the Rust port; #437 squash `dbc26f31`, tag → `e8a8d8e5`;
  `group.protocol=${KAFKA_GROUP_PROTOCOL:auto}` in the bundled consumer template — [[kafka-group-protocol-auto-default]], READ:
  an app that never set it joins a KIP-848 cluster with the consumer protocol. Origin 2026-09-21-233928.md.) · v4.12.13
  (2026-09-21, 11 accumulated improvements, #435 `1feb3d73`, FIELD-ACCEPTED) · v4.12.12 (2026-09-17, the produce-only unblock,
  #410, [[kafka-config-class-static-init-loader]] — archived) · v4.12.11 (2026-09-16, the field unblock, #405, [[otel-optional-service-and-negative-control]]) · v4.12.10 (2026-09-16,
  Berkeley DB store retired, ADR-0024, #400) · v4.12.9 (2026-09-16, the distributed-Redis release, #364–#397; ACTION: the
  `soa.redis.health` route rename, `minimalist-kafka` no longer transitive). The live version source stays the root pom.xml.
- **last_enabled:** 2026-06-20
- **last_review:** 2026-10-06 | through 2026-10-06-012541.md (SIZE — the new `graph-set-pack-and-deploy` fact made 36 decay-eligible facts
  against a cap of 35; 6 sessions since the 2026-10-05 review: `refresh-metadata` refreshed 19 footers, tier changes 14 (11 active →
  archive-candidate, 3 working → active); archived 2 faded Key Decisions, `connected-edge-spans` (sslu 24) and
  `playground-example-flows-are-sample-config` (sslu 23), after step 6 found no window commit on their code and no reliance in the window's
  logs; swept 4 closed threads (`canonical-msgpack-packager`, `decimal-mode`, `flow-11-doc-label`, `playground-bundle-stale`); reactivated 0,
  superseded 0, archive-verify pass; invariants not due (38 of 40 since 2026-09-25-022841); stalled threads none; contradiction scan: one
  stale statement corrected (`canonical-packager-wire-contract` listed the folder tooling and the loader as deferred); Doc Gaps: none in the
  window. Live facts 59 → 53, lint 0 warnings. Smoke test not run.)
  Prior: 2026-10-05 | through 2026-10-05-230033.md (ON COMMAND — the first step-6 declaration-gap run at v4.42.3: refreshed 30 footers,
  archived 0 with two facts KEPT as declaration gaps, the stalled-thread gate raised; facts 56 → 57) · 2026-10-02 | through
  2026-10-02-185029.md (CADENCE + SIZE — refreshed 35 footers, tier changes 25, archived 8 faded Key Decisions,
  swept 4 threads; facts 62 → 50, lines 867 → 683) · 2026-09-28 | through 2026-09-28-230310.md (CADENCE — refreshed 6 footers, tier changes 3, archived 0, swept 0; two stale statements
  corrected; facts 46; smoke test 12/12, read set gained `vision.md`) · 2026-09-25 | through 2026-09-25-014100.md (ON COMMAND after the
  v4.12.17 cycle — archived 0, swept 0; the invariant re-verify thread raised and completed 2026-09-25-022841, `instant-serialization` demoted
  from core and archived; lines 851 → 802, facts 52) · earlier: 2026-09-24, 2026-09-23 (×2), 2026-09-22 (size sweeps; their summaries live in
  those session logs).
- **vision_evolved:** 2026-09-17 (Eric approved) — `memory/vision.md` now states **two tracks**: Track 1 *knowledge graph as
  application* (deterministic — rules, business logic, outcome; L3 leverages L2 + L1) and Track 2 *knowledge graph as AI SDLC*
  (governed AI processing for ambiguity a deterministic program cannot handle; L3 is the foundation and **AI is also the
  runtime for certain nodes**). The tracks echo each other — shared co-design, certification and promotion — and differ only
  in the nature of the work and where execution goes; AI gains a **run-time participant** role. **The north star MOVED: the
  next invariant re-verify must check the 2026-09-17 text, not the 2026-06-20 one.** [[bp-agent-orchestration]] is Track 2's
  Blueprint gap; Track 1 and the collaboration foundation are delivered.
- **last_invariant_check:** 2026-09-25 | 2026-09-25-022841.md (COMPLETE — Eric walked the 19 `core` facts and the Vision — **the 2026-09-17
  two-track text** — against live-tree evidence; 18 facts + the Vision CONFIRMED as written, `instant-serialization` DEMOTED
  from core (Eric: an implementation detail that no longer influences key decisions; unreferenced since 2026-06-27, so it
  archives as faded in the same review); core 19 → 18; [[reverify-invariants-20260925]] closed. Cadence 40; the next re-verify
  is due 40 sessions after 2026-09-25-022841. Prior: 2026-09-16 | 2026-09-16-041500.md (COMPLETE — 3 invariants + 5 stack + 5 key decisions +
  3 conventions + eric-release-rhythm + the Vision; `virtual-threads-rpc` ENRICHED from ADR-0024; `conv-telemetry-presentation-parity`
  RETIRED; core 18 → 17.) Prior: 2026-09-11 | 2026-09-11-005808.md.)

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
  <!-- id: otel-optional-service-and-negative-control | created: 2026-09-16 | last_used: 2026-10-02 | uses: 14 | tier: archive-candidate | origin: 2026-09-16-193203 -->

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
  <!-- id: clean-knowledge-design-over-engine-coverage | created: 2026-09-18 | last_used: 2026-10-04 | uses: 15 | tier: archive-candidate | origin: 2026-09-18-174943 -->

- **A Layer 3 application is one graph endpoint plus dev mode, and its home page is dev-mode wiring too (2026-09-15, Eric's
  polish round on the starter template + the cache example; P10 ruling 2026-09-22, SHIPPED in v4.12.15).** Three shape
  rules. **(1) One endpoint, every graph:** `POST /api/graph/{graph_id}` takes the id from the URL path, so a Layer 3 app
  needs exactly one `rest.yaml` entry and the stock `graph-executor` flow no matter how many models it deploys — adding a
  graph means adding its id to `graphs.yaml`, never a bespoke route. **(2) Dev mode is settings that travel together:**
  `app.env=dev` AND the dev-mode rest.yaml entries — every Playground/companion service is `@OptionalService("app.env=dev")`
  and `RoutingEntry.resolveServices` SKIPS a rest entry whose service is unregistered, so routes without the switch are dead
  and the switch without routes leaves only a WebSocket; both ship pre-wired in `templates/starter-graph` and
  `examples/distributed-cache-example`, and removing the one line closes the surface for production. **(3) A graph app
  declares ONE Mercury dependency — `minigraph-playground-engine`** (event-script-engine and platform-core come transitively,
  Gradle too); the classpath-order collision that once made a wrongly ordered dependency list serve platform-core's
  placeholder page is RETIRED by P10, and the one-dependency advice stands for tidiness. **P10 (Eric; Java #449, Rust
  #319):** the React Playground's `index.html` WAS the engine jar's static `public/index.html`, so a Layer 3 app served the
  Playground at `/` in EVERY environment ("the user would panic"). The entry page is now `template/playground.html`,
  reachable only through the `get.index.html` route (at `/index.html`, reached by `/`) and only when `app.env=dev`; an
  ABSENT `app.env` is production (the function used to default to dev). The engine's static `public/index.html` is the plain
  "MiniGraph Service" page; both starters gained the route; `deploy.js`/`clean.js` split the bundle (assets →
  `public/assets/`, page → `template/playground.html`). **READ at 4.12.15:** an app without the route gets the plain page at
  `/` in dev too — add the route; an app with no `app.env` gets the plain page. Documented in
  `playground-and-companion.md` (#enabling) and `ai-agent-guide.md` (#scaffolding). Relates [[playground-session-broker]];
  applies to [[ot-distributed-cache]]'s worked example.
  <!-- id: minigraph-dev-mode-app-shape | created: 2026-09-15 | last_used: 2026-10-04 | uses: 19 | tier: archive-candidate | origin: 2026-09-15-221451 -->

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
  <!-- id: playground-session-broker | created: 2026-09-03 | last_used: 2026-10-04 | uses: 15 | tier: archive-candidate | origin: 2026-09-03-172753 -->

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
- **A static decision table is GRAPH DATA — a skill-less node's properties, handed whole to a generic function by ONE
  `graph.task` input entry; never hard-coded in a function bundled with the graph (Eric, 2026-09-20; a doc gap, no engine
  change — PR #430; the `lookup` plugin PR #431).** Found when an AI agent compiled a rule-by-state table into a composable
  function shipped with its graph. Both engines copy every node's properties into the state machine at instantiation
  (`initializeWithNodeProperties`: skill node → non-reserved keys at `{node}.{key}`; skill-less node → the whole map at
  `{node}`) and the shared LHS resolver reads any selector, so `state-rules -> table` maps the table in one entry.
  **Presentation (Eric):** each value is a JSON array written as text — `keys=[ "a", "b" ]`, `a=[ "CA", "TX" ]` — which
  reads as a table on the node and arrives as a string the function reconstructs with `SimpleMapper`; `key[]=` lines build a
  real list; a nested table is one triple-quoted JSON text parsed by `f:json(state-rules.table)` at mapping time. **Why:**
  the product owner certifies the rules on the graph in the business vocabulary, and one table replaces a ladder of
  IF-THEN-ELSE. **The common case needs no function (Eric's `f:lookup(table, value, default)` simple plugin):** one
  `graph.data.mapper` entry — `f:lookup(state-rules, input.body.state, text(unknown)) -> output.body.rule` — the optional
  third argument the default on a miss; a composable function stays for a ruling that needs more than a lookup. The plugin
  takes the table as a map or JSON text, lists as lists or JSON arrays written as text, compares as text case-insensitively.
  **Two traps:** (1) a plugin class may not reference `SimpleMapper` — the loader's bytecode gate (`ALLOWED_PACKAGES`) SKIPS
  it silently and every `f:lookup` mapping fails `SimplePlugin 'lookup' not found`; the serializer is reached only through
  the allowlisted `SimplePluginUtils` (`SimplePluginGateTest` pins discovery); (2) **a null mapping source — CHANGED
  2026-09-23 (field issue #453, Eric's ruling; #456, Rust Increment 136):** the graph engine applies Event Script's rule — a
  null or unresolved source CLEARS a `model.*` target (removed; set to null when the source key exists or the target is
  indexed) and is IGNORED for any other target — via `GraphLambdaFunction.applyNullSource` in mapper/math/js MAPPING,
  `for_each`, fetcher/extension parameters (a null parameter is not supplied) and the graph.task/extension/fetcher output
  mapping; until 4.12.15 it removed ANY target (claim `null-source-removes-target` now states the shared rule). A default for
  a model variable comes from the source side (the plugin's third argument or `f:defaultValue`), never from
  default-then-overlay — unsupported in BOTH layers; the same round made graph.math name every unresolved `{selector}`.
  **Rule:** the product owner reads and certifies the table ON the graph, a new table is a new graph version and never a
  code change, and the function stays generic by reading rule names from `table.keys`. In `skills-reference.md`
  (graph.task), the in-Playground help and the AI agent guide's checklist; pinned by `unit-test-task-9` on both engines.
  Applies [[event-script-over-code]] to DATA and [[clean-knowledge-design-over-engine-coverage]].
  <!-- id: static-decision-table-is-graph-data | created: 2026-09-20 | last_used: 2026-10-04 | uses: 5 | tier: archive-candidate | origin: 2026-09-20-152704 -->
- **EventApiService serves LOCAL routes only — an inbound `/api/event` call to a route
  the instance does not host answers 404 even when the instance's own
  `yaml.event.over.http` map points that route at a peer (Eric ratified 2026-08-30).**
  Forwarding would make every app an Event-over-HTTP relay and open routing loops; the
  `x-event-api` wire marker is the existing loop guard (an event that crossed the wire
  once is never re-forwarded), the map is caller-side knowledge (not a promise to third
  parties), and deliberate hop-through is an explicit relay function (demo:
  `hello.remote.relay`). Recorded in the progressive-rendering interop report.
  <!-- id: event-api-local-routes-only | created: 2026-08-30 | last_used: 2026-08-30 | uses: 1 | tier: core | origin: 2026-08-30-050040 -->

- **graph.math is typed and finite: a boolean is never a number, an unknown function and an overflow fail by name, and
  exact-decimal money belongs in a `graph.task` function — the math package stays minimal (Eric's rulings on a field
  installation's nine "wrong answer" behaviours, 2026-09-25; PR #462 squash `0c017d5e` MERGED 2026-09-25; Rust twin mercury #330 squash `d97eab9b`, Increment 141).**
  The evaluator coerced a boolean to 1/0 in arithmetic, `<`/`>` and function arguments while equality type-checked — in the
  field one JSON `true` in a threshold slot became a large overcharge with no error. Now one uniform rejection mapped back to
  the selector (`Boolean operand: model.flag (true) in '…' - a boolean is not a number; store a boolean with CONDITION or
  assert the type with f:validate`), a bare boolean COMPUTE result included. **`CONDITION: var -> expr`** is the declared
  boolean statement (evaluated as a boolean whatever operators it carries; `IF` tests it directly); `COMPUTE` keeps yielding
  a boolean for a comparison — by design, now documented. `Unknown function: mn` replaces the generic message; every
  arithmetic result is checked finite (`Arithmetic overflow in '*' (result Infinity)`, `Division by zero or arithmetic
  overflow in '/'`, NaN by name), so `Infinity` never travels on to fail a later node as an unknown identifier. **Ruled
  documentation, not engine:** a `run` on the same Playground instance keeps `model.*` (a `model.x[]` append appends again)
  and `instantiate graph`/`start` is the reset; a taken IF inside a `for_each` body ends the walk (per-row rules are
  arithmetic gates); the end node is the terminus (last writer wins); `BigDecimal` is never added to the dialect. **READ:** a
  graph that relied on `true` computing as 1 or on a propagating `Infinity` now fails at that statement. Two of the nine
  were already closed in 4.12.16 (#456). Applies [[clean-knowledge-design-over-engine-coverage]]; extends
  [[static-decision-table-is-graph-data]] and [[minigraph-guarded-async-completion]]. **Partly superseded 2026-09-30 by
  [[decimal-statement-exact-arithmetic]]:** the "exact-decimal money belongs in a `graph.task` function, `BigDecimal` is never
  added to the dialect" ruling no longer holds — `DECIMAL:` is that statement; the typed/finite `COMPUTE` rules stand.
  <!-- id: graph-math-typed-arithmetic | created: 2026-09-25 | last_used: 2026-10-02 | uses: 8 | tier: archive-candidate | origin: 2026-09-25-183540 -->

- **`graph.model.automation` accepts a comma-separated list of manifests, and the later manifest wins (Eric, 2026-09-25;
  PR #465 squash `40ce30a7` MERGED 2026-09-25, Rust twin mercury #332 merge `d3d82a3f`, Increment 142; SHIPPED in
  v4.12.19 on both engines).** Born at the leadership demo's deploy step: an
  exported graph went live in the running example with `-Dgraph.model.automation=file:/tmp/graph/deploy/graphs.yaml` and no
  rebuild — but the one manifest REPLACED the bundled set, so a prototype delegating through `graph.extension` to a bundled
  graph could not run. Now the `yaml.flow.automation` convention (CompileFlows precedent): each manifest carries its own
  `location`, they compile in order, one that fails to load is skipped with a warning, `CompiledGraphs` records each graph's
  source location, and `list graphs` / the `import graph from` fallback span every location. **Rule:** the later manifest OWNS
  a duplicate id — its copy replaces the earlier one (`WARN Graph X from B replaces the copy from A`) and a rejected later copy
  leaves the id not executable (404), never a silent fallback to the copy the operator meant to replace. **Why (Eric):** the
  prototyping loop is `import graph from` a deployed graph → correct → dry-run → export → stage in the deploy folder with its
  manifest → restart with BOTH manifests → curl the deployed behaviour → bundle. Entries are manifests, never bare folders (a
  manifest is the gate's allowlist). Proven live: the deploy copy of `tutorial-1` answered over curl while `tutorial-2` kept
  serving from the jar. Docs: `ai-agent-guide.md#deploy-without-rebuild`, the config reference, the walkthrough; claim
  `graph-manifest-list-later-wins` on both engines; the Rust override is a `-D` program argument
  (`cargo run -p minigraph-playground -- -D…`). Extends ADR-0011 without changing its rule. Relates [[eric-code-changes-via-pr]].
  <!-- id: graph-manifest-list-later-wins | created: 2026-09-25 | last_used: 2026-10-06 | uses: 10 | tier: active | origin: 2026-09-25-223633 -->

- **Exact decimal arithmetic is a `DECIMAL:` statement in `graph.math` — the high-precision `COMPUTE:`, strings at rest, no graph-level
  mode (Eric's rulings, 2026-09-30; RFC-0001 promoted to ADR-0025 in PR #473; implemented in PR #471, squash `3b4cad82`; SHIPPED in
  v4.12.20).** `DECIMAL: var -> expr` evaluates in `BigDecimal` and stores a canonical string at
  `{node}.result.{var}` (plain notation, computed scale kept, a zero of any scale `"0"`), because the state machine does not keep
  Java types across `graph.suspend`/`graph.resume` or any event hop. `/` is exact when it terminates, else 34 digits HALF_EVEN;
  `round(x, scale, mode)` always names its mode; `sqrt`, `log`, trigonometry, `random()`, `PI`, `E` and a boolean result fail by
  name. `COMPUTE:` keeps its meaning and no `CompileGraph` check exists. **Two changes reach existing graphs (READ at release):**
  numeric strings compare as numbers in `== != < <= > >=` everywhere (PR #471, Eric: it removes a recurring developer error), and
  the dialect `round` is half up away from zero, like `f:round` (PR #472: `round(-2.5)` is `-3`). A `Double` operand converts
  through its shortest decimal text, lenient and DECLARED in the guide (send money as strings). Parity rides 151 shared vectors
  run in every engine. **Supersedes the decimal half of [[graph-math-typed-arithmetic]]** ("exact-decimal money belongs in a
  `graph.task` function; `BigDecimal` never enters the dialect"); the rest of that ruling stands. **Method note:** RFC first,
  implement on one branch, promote to ADR only after review and green CI — the ledger recorded a decision, never a proposal
  ([[conv-proposals-not-in-adr-ledger]]). Delivered on both engines (Rust twin: mercury #335 + #336, one shared core) and released in v4.12.20; the field-acceptance wait is [[decimal-ports-and-release]] (the python/node packs need no change — they are Event-over-HTTP function hosts; closes [[decimal-mode]]). (ADR-0025)
  <!-- id: decimal-statement-exact-arithmetic | created: 2026-09-30 | last_used: 2026-10-05 | uses: 2 | tier: active | origin: 2026-09-30-221603 -->

- **A deterministic MsgPack packager: keys sorted by UTF-8 bytes at every depth, `{manifest, maps}`, one canonical profile, integrity left to the
  application (Eric's rulings, 2026-10-01; RFC-0002 promoted to ADR-0026 in PR #484; Java `CanonicalPackager` #481/#482/#483 and the Rust
  twin `platform_core::canonical_packager`, mercury #339, Increment 147; SHIPPED in v4.12.20).** The ordering is the packager's own step — Gson has no
  ordered-keys option and a `String`-order sort is UTF-16, which differs above U+FFFF — and the bytes are written through msgpack-core
  directly, because `MsgPack.pack` drops nulls unless configured, packs a `Float` as float32 and writes a `BigDecimal` zero as `"0.00"`. The
  profile: nulls kept, smallest integers (signed 64-bit), finite float64 only (a `Float` is WIDENED through its shortest decimal text — Eric, 2026-10-01, before 4.12.20; it was rejected at first), shortest str/bin headers, exact numbers as strings (zero `"0"`),
  nothing else; the read is strict by default (re-encode and compare). **Digital signature is OUT — the user application decides whether to
  protect the exact bytes and with which algorithm; no hash mode, verifier seam or key in the framework.** **The interop proof is one vector file,
  byte-identical in both repos, whose expected bytes come from an independent encoder written from the spec (not from an engine)** — 73 values, 6
  packages, 25 rejections and a seeded 60-document corpus; each engine matching it means the engines match each other, and both passed first time.
  Method worth keeping: an independent oracle plus one shared file beats a cross-engine drive; it also found that `rmpv` counts two depth units
  per nesting level (its 64 accepted 31), so a library-limit corner needs its own vector. Language packs do not carry it (Event-over-HTTP function
  hosts). Deferred: trusted timestamps and per-entry manifest records. The folder `pack`/`unpack` tooling and the graph-set loader, deferred
  here at first, became RFC-0005 and then ADR-0027 ([[graph-set-pack-and-deploy]]; the command line shipped in #508). Relates
  [[graph-manifest-list-later-wins]] (the loader's precedence). (ADR-0026)
  <!-- id: canonical-packager-wire-contract | created: 2026-10-01 | last_used: 2026-10-06 | uses: 7 | tier: active | origin: 2026-09-30-221603 -->

- **`graph.js` is deprecated and its module will be removed once field installations finish migrating (Eric, 2026-09-30; the
  docs have carried the DEPRECATED notice since 2026-09).** New capability targets `graph.math`, with `graph.task` for what the
  dialect cannot express; `graph.js` gets no new feature and no branch in shared code: the `DECIMAL:` statement of RFC-0001 is
  `graph.math` only, and `graph.js` rejects it as it rejects any tag it does not know. The reasons are in
  `docs/guides/knowledge-graph/skills-reference.md#js` (runtime JavaScript is an injection surface, and an equality comparison
  with a quoted string literal was measured silently evaluating `false`). Relates [[clean-knowledge-design-over-engine-coverage]].
  <!-- id: graph-js-deprecated-removal | created: 2026-09-30 | last_used: 2026-10-02 | uses: 3 | tier: archive-candidate | origin: 2026-09-30-170759 -->

- **The LLM helper is not engine code: it is a Claude-only function host in the Python and Node packs, and the Java engine only points at it (PR #488, squash `7c938ef1`; Eric's rulings,
  2026-10-01).** `examples/llm-helper` in mercury-python (#38) and mercury-nodejs (#106) serves `llm.chat`, `llm.stream` and `llm.health` on the Anthropic SDK; the engine stays LLM-free and holds no
  credential, and the playground's `support-triage` graph and the `/api/llm/stream` relay (`LlmStreamRelay`) reach the helper through `event-over-http.yaml` by route name. **The evidence is
  `docs/test-reports/llm-helper-certification.md`** (byte-identical with the Rust engine's, and registered in the three places the AI contract snapshot needs: the pom include set, `skill/files.list`
  and `SkillSnapshotTest`'s fixed extras): Java and Rust in front of both helpers through a Layer 1 streaming service, a Layer 2 flow and two Layer 3 graphs, with real Claude calls (40 results per
  pair, 124 model calls, no failed check). Every batch the helper forwarded reached the edge as its own frame (50 to 101 per stream, 4 to 10 ms of drift) — Eric's rule is that progressive tokens are
  never buffered into one message — the cadence is the API's and depends on the model (Haiku 4.5 continuous, Opus 5.5 bursts about every 600 ms), the error contract holds on the real SDKs, and all 32
  traces that touched a helper rebuild as one tree ([[connected-edge-spans]]). The deploy lane did the deploying ([[graph-manifest-list-later-wins]]). **Opus 5.5 stays the helper's default**
  (Eric): it thinks before it answers and its thinking tokens count against `max_tokens`, so the triage graph now asks for 2000 tokens (512 before) and Haiku 4.5 is the documented choice for smooth
  rendering; a 48-call probe found no empty result at the old budgets either, so the change removes the question. AWS Bedrock through IAM is the helper's planned second backend (a thread in the
  packs). Rust twin: mercury #342.
  <!-- id: llm-helper-certification-java | created: 2026-10-01 | last_used: 2026-10-05 | uses: 3 | tier: active | origin: 2026-10-02-001806 -->

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
  <!-- id: playground-webapp-single-source | created: 2026-10-02 | last_used: 2026-10-04 | uses: 5 | tier: archive-candidate | origin: 2026-10-02-175751 -->

- **The Playground's dry-run lifecycle is three explicit steps - Instantiate, Upload (optional), Run - and a mock-data upload loads every member's instance (Eric's design, 2026-10-02;
  PR #498 squash `ce0e7155` and the Rust twin mercury #349 merge `278bb023`, Increment 154; both MERGED 2026-10-03 06:00Z).** In a shared session one member's Instantiate used to open the "Upload Mock Data" form on every member's screen: the UI sent
  `upload mock data` as soon as the instance existed, the engine replays every command to every member, and each member's own invitation line opened its panel. Now Instantiate only
  creates the instance (and a fresh one while one exists), the toolbar's Upload opens the form for the clicking session's own `/api/mock/{sessionId}` with no console command (an
  `upload.invitation` line never opens a panel by itself; `useAutoMockUpload` is gone), and Run runs with whatever input the instance holds - tutorial 1 needs none. **Engine rule:**
  `GraphCommandService.uploadContent` routes the payload through the command service as an `upload` event; the primary loads it and replays it into every subscriber's instance, a
  subscriber's payload goes through the primary, every member's console prints `Mock data loaded into 'input.body' namespace`, and a session without an instance is refused (HTTP 400);
  the Rust engine mirrors it (`commands::handle_upload`). **This reverses the same-day ruling that uploads stay per member** (2026-10-02-050449): a replayed `run` on another member's
  instance had aborted without the data. The multi-select hint left the canvas in the same PR. Pinned by `SessionManagementTest.mockUploadLoadsEveryMemberInstanceTest` and the Rust
  `mock_upload_loads_every_member_instance`. Extends [[playground-session-broker]] (equal partners); applies [[help-edit-needs-bundle-release]] and
  [[playground-webapp-single-source]] (one bundle, deployed to both engines).
  <!-- id: playground-three-step-run-controls | created: 2026-10-02 | last_used: 2026-10-03 | uses: 2 | tier: archive-candidate | origin: 2026-10-02-232248 -->

- **A graph file becomes the session draft through `POST /api/graph/import/{id}`, which travels like a command, and the Playground downloads the model as `<graph-id>.json` with the root
  node named after the id (Eric's usability sprint, 2026-10-03; PR #500 squash `856e084b`, the Rust twin mercury #350 merge `dae6377d`, Increment 155; both MERGED 2026-10-03 16:03Z).** The Graph view's Import Graph button (toolbar and empty canvas) and a
  `.json` file dropped on the canvas validate the file in the browser (`utils/graphFile.ts`: a JSON object whose only top-level sections are `nodes`, mandatory, and `connections`, optional;
  anything else refused by name) and post it; the engine validates again with its own importer on a scratch `MiniGraph` (`GraphCommandService.validateGraphModel`) and routes an `import`
  event through the command service exactly like the mock upload (`handleImport`: the primary imports and replays, a subscriber forwards), so every member's draft is replaced and every
  console prints `Graph model imported as draft` - the line the UI already refreshes on, which is why `useGraphFileImport` toasts errors only. **Rulings:** simple validation, CompileGraph
  stays the quality gate (Eric); the UI asks before replacing a loaded graph (no undo for an import); a file drag is claimed even while disconnected so the browser never opens the file in
  the tab; Download mirrors `export graph as` (the id follows the engine's file-name rule and becomes the root node's `name` in the file, the live graph untouched; Chromium's save picker,
  else the download folder); the "Graph Data (Raw)" tab is "Raw"; a rejected model reports `Graph model not imported - <reason>` on the console (both engines, also for `import graph
  from`). A dev-route addition touches five Java `rest.yaml` copies, three Rust ones and the guide excerpt ([[minigraph-dev-mode-app-shape]]). Extends
  [[playground-three-step-run-controls]] (the broadcast shape); applies [[playground-webapp-single-source]] and [[help-edit-needs-bundle-release]] (bundle `index-Cv2pdvxg`) and
  [[clean-knowledge-design-over-engine-coverage]]. Pinned by `SessionManagementTest.graphImportLoadsEveryMemberDraftTest` and the Rust `graph_import_loads_every_member_draft`.
  <!-- id: playground-file-import-download | created: 2026-10-03 | last_used: 2026-10-03 | uses: 2 | tier: archive-candidate | origin: 2026-10-03-153654 -->

- **A mapping source inserts its `{namespace.key}` values verbatim, never quoted; a JSONPath filter is the one place a text value is quoted (Eric's ruling, 2026-10-03;
  PR #503 squash `8db2a46f` and the Rust twin mercury #351 merge `5c408037`, Increment 156; both MERGED 2026-10-04 06:01Z).** A graph mapping source may embed a reference that resolves before the source is read: a
  key segment (`census-2020.{model.state}`, the keyed-table read beside `f:lookup`), a list index (`items[{model.i}]`, never Event Script's bare `[model.i]`), or text in a
  constant or a plugin argument, in `mapping[]`, `MAPPING:`, `for_each[]`, the task/extension/fetcher `input[]`/`output[]` and a Dictionary's `output[]`
  (`GraphLambdaFunction.substituteMappingSource`, `common::substitute_mapping_source`). Those six call sites had used the expression renderer, whose boolean-operator
  heuristic quoted every text value when the source held `!`, `<`, `>`, `==`, `&&` or `||` (`Hello 'Peter'!`). **By design (Eric, 2026-10-03):** an unresolved reference
  renders `null` (Event Script keeps a non-model brace pair verbatim) and a mapping target is never resolved (literal); also documented: any namespace may be read (Event
  Script: `model.*` only), a composed key is case-sensitive, and a fixed prefix should precede a request value. Found documenting the keyed census
  table of a live team demo; pinned by the byte-identical fixture `unit-test-mapping-1` and the claim `mapping-source-dynamic-variables`; the plugin table now states the
  `-1` of `int`/`long`/`float`/`double` for a null or non-numeric value (Eric agreed). Follow-up: the JSONPath result shape (Jayway's rule: a definite path yields the
  value or null, an indefinite one always a list, null when a member name before its first indefinite step is missing) is documented and pinned by the shared fixture
  `unit-test-jsonpath-1` and the claim `json-path-result-shape` in #504 (squash `74720116`; no change here), the Rust fix being mercury #352 (merge `bdc7b5be`,
  Increment 157); both MERGED 2026-10-04 06:13Z. Extends
  [[static-decision-table-is-graph-data]]; applies [[help-edit-needs-bundle-release]] (bundle `index-Bz9k-ffR`).
  <!-- id: mapping-source-verbatim-substitution | created: 2026-10-03 | last_used: 2026-10-04 | uses: 2 | tier: archive-candidate | origin: 2026-10-04-054327 -->

- **A graph holds no null property: `"key": null` is filtered out wherever a graph enters an engine, `"key": ""` is a value, and
  `serializer.null.transport` does not apply (Eric's rulings, 2026-10-05; PR #508 squash `5e9a0fab` and mercury #356 merge `779cffe1`, both
  MERGED 2026-10-06).** Found building
  the graph packager: Java's `ConfigReader` drops a null-valued key when it normalizes a deployed graph (`Utility.getFlatMap` skips nulls),
  Rust's reader kept it and both engines' graph import refuses one (`addProperty`: "value cannot be null"), so a graph deployed on Java only,
  and the Java packager packed the nil. Now one rule: a map entry whose value is null is filtered at every depth - on deploy (Java's
  `ConfigReader`; Rust `compiler::load_raw_graph`, also `instantiate graph`), when a set is packed or read (`GraphModelGate.withoutNullProperties`;
  Rust `model_gate::without_null_properties` = `serializer::strip_nulls_always`), and in the Playground's draft import, the one place the REST
  import, its replay and `import graph from` pass (`importGraphAsDraft` + `validateGraphModel`; Rust `import_graph_model` + `validate_graph_model`;
  `import graph from` reads a file as text, so a deployed graph holding a null could not be imported before) - while a list keeps its elements.
  `serializer.null.transport` (default false; platform-core's `SimpleMapper` and `MsgPack`) governs what the event transport keeps, never what a
  graph holds: the packagers write through the canonical packager, and both engines' tests pack the same bytes with the switch on (Java in a
  second JVM, after a probe shows `SimpleMapper` keeping a null). **The deploy read beyond nulls (Eric approved options A and E, 2026-10-05):**
  Java's `ConfigReader` flatten-and-rebuild also drops an empty map or list (and one left empty), turns such an element inside a list into
  null and drops it at the end; Rust's `model_gate::normalize_graph` reproduces that on deploy (a mapping list ending in null passed only on
  Java; an empty `{}` property deployed only on Rust), pinned by the byte-identical `graph-read-normalization-vectors.json` in both engines'
  test resources. A package keeps empty collections, as the files hold them. Relates [[graph-set-packaging]], [[canonical-packager-wire-contract]].
  <!-- id: graph-null-property-filtered | created: 2026-10-06 | last_used: 2026-10-06 | uses: 4 | tier: active | origin: 2026-10-06-000717 -->

- **A graph set is one or more graphs in a canonical `<set>.pack`, checked by the gate when packed and deployed all or none through a
  generated manifest; a set of one graph is how one graph is signed (Eric: decided 2026-10-03, accepted as ADR-0027 on 2026-10-06; PR #510
  squash `2ebcaea5` and the Rust twin mercury #357 merge `60e59aeb`, Increment 159, both MERGED 2026-10-06).** RFC-0005 was promoted after
  WP1 and WP2 shipped (#508 squash `5e9a0fab`,
  mercury #356 merge `779cffe1`). The rules: the gate is one shared method (`GraphModelGate` / `model_gate`) that also runs at pack time (D2);
  the command lines are `helpers/graph-packager` and `tools/graph-packager` (D8); an entry is `<graph-id>.json`, the root name equals the id
  (D6), and `graph_id` is an optional entry point (D5); the bytes depend on the graphs and the manifest fields only (no clock, `${...}`
  packed unresolved, nulls filtered - [[graph-null-property-filtered]]). A deployment manifest lists `sets` and a read-write `file:/`
  `unpack` folder; each set is unpacked before the gate with a generated manifest that compiles next, all or none (D3), and a duplicate
  involving a set logs an ERROR (D4). The Playground packs on the engine (D7, D9), and signing stays outside (a detached `<set>.pack.sig`).
  **The one-graph rule (Eric, 2026-10-06):** only an empty set is refused; a graph packed alone gets bytes of its own to sign, and both
  engines' tests pin one such set (327 bytes, SHA-256 `c500281a…`), the guard for every signature - a change that alters the bytes of the
  same graphs and fields breaks the signatures made over the old ones. WP3, the loader, shipped in #515 (squash `eb6021a9`) and
  mercury #359 (merge `42f6cf81`, Increment 161); open work, WP4 to WP6: [[graph-set-packaging]]. Extends
  [[canonical-packager-wire-contract]] and [[graph-manifest-list-later-wins]]. (ADR-0027)
  <!-- id: graph-set-pack-and-deploy | created: 2026-10-06 | last_used: 2026-10-06 | uses: 1 | tier: working | origin: 2026-10-06-012206 -->

- **A MsgPack payload may nest at most 64 maps and lists in both engines, the outermost container being level 1 (Eric, 2026-10-06;
  Java #514 squash `15f26aac` and the Rust twin mercury #358 merge `49d599a0`, Increment 160, both MERGED 2026-10-06).** Found answering the field scanners' finding on
  `msgpack-core` 0.9.12 (CVE-2026-90472, deep nesting in the library's `unpackValue()`; CVE-2026-90473, a MAP32 count overflow in
  `skipValue()`). The engine reaches neither method: platform-core's `MsgPack` walks maps and lists itself and calls `skipValue()` only on
  an extension value, and `CanonicalPackager` calls neither. But its own reader recursed with no bound, so about 100 KB of nested arrays
  ended in `StackOverflowError`, which the Event API's `catch (Exception e)` misses. Eric approved 1,024 first, then asked for "a smaller
  and realistic nesting level for both engines": 64, the canonical packager's bound. **The rule:** `MsgPack.MAX_DEPTH` and Rust
  `serializer::MAX_DEPTH` are 64; a 65th level is refused with `Nesting deeper than 64 levels` (Java `IOException`, which `EventEnvelope`
  reports as `IllegalArgumentException`; Rust `serializer::from_msgpack`), and the Event API answers HTTP 400 in both. The envelope map
  is level 1, so a body nests at most 63. **Rust lesson, measured with a throwaway test:** rmp-serde's default of 1,024 (it counts the
  outermost container, so 1,023 decode) is no guard in a debug build: a 2 MiB thread stack overflowed between 500 and 600 nested arrays
  and aborted the process. Measure a library's depth limit against the stack, not against its documentation. Pinned by the claim
  `msgpack-nesting-limit` (both registries; the wire format's rule 8 and both envelope references) and twin tests; a negative control
  showed the Java tests failing on the old reader. Relates [[canonical-packager-wire-contract]], [[msgpack-core-cve-upgrade]].
  <!-- id: msgpack-nesting-limit-64 | created: 2026-10-06 | last_used: 2026-10-06 | uses: 1 | tier: working | origin: 2026-10-06-025608 -->

- **A zero-dependency MessagePack codec of this repository, `system/minimalist-msgpack`, replaces `org.msgpack:msgpack-core` under
  platform-core (built 2026-10-06 at Eric's investigation ask; RFC-0006 Open; PR #517 squash `527ac5eb` MERGED 2026-10-06, head
  `488c826e`, the full reactor green with 1,767 tests; RFC-0006 awaits promotion).**
  Eric's criteria - at par or faster, virtual-thread friendly, no deprecated libraries, no unsafe operations - and his
  "minimalist" rule, no custom (extension) types. The module (`org.platformlambda.mini.msgpack`, `java.base` only) is a streaming
  `MsgPackWriter`/`MsgPackReader` written from the published specification for nil, bool, int, float, str, bin, array and map;
  big-endian fields through `VarHandle` byte-array views, no `Unsafe`, no `synchronized`/`ThreadLocal`/lock, one reader or
  writer per call. The value layer stays in platform-core (`MsgPack`, `CanonicalPackager`; `_T`/`_D`, null transport and
  [[msgpack-nesting-limit-64]] unchanged). **Reader rules:** every length and count is 32-bit unsigned and checked against the
  remaining bytes before anything is allocated (N elements need N bytes, N entries 2N - the CVE-2026-90473 header is refused at
  the header); `0xc1` refused; `skipValue()` iterative; a failed read leaves the position unchanged; one checked
  `MsgPackException extends IOException`. **Evidence:** the canonical vectors ([[canonical-packager-wire-contract]]) pass
  unchanged; a 20,000-document seeded corpus packs BYTE-IDENTICAL on both backends and cross-decodes with 0 mismatches
  (`benchmark/msgpack-codec-harness/`, outside the reactor so msgpack-core never enters a manifest); benchmark best of 3
  interleaved runs: the 395-byte envelope packs in 0.54 and unpacks in 0.55 of msgpack-core's time, the 8 KB payload 0.39/0.72,
  the 7 KB record list at par (`docs/test-reports/minimalist-msgpack-benchmark.md`). **Three behaviour changes, each pinned:**
  `org.msgpack.*` no longer transitive; every decoding failure is an IOException (EventEnvelope -> IllegalArgumentException,
  where msgpack-core also threw unchecked exceptions); a uint64 above Long.MAX_VALUE reads as BigInteger (the Rust engine's
  `rmpv` keeps a u64). Home ruling (Eric): `system/`, because platform-core must depend on it. **Method:** the differential's
  first pass reported 9,124 mismatches from the harness's own expectation model - a comparison must be shown able to fail
  ([[otel-optional-service-and-negative-control]]). Supersedes nothing; [[msgpack-core-cve-upgrade]] closed as moot at the
  merge. Thread: [[minimalist-msgpack]].
  <!-- id: minimalist-msgpack-codec | created: 2026-10-06 | last_used: 2026-10-06 | uses: 1 | tier: working | origin: 2026-10-06-164045 -->

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
  <!-- id: conv-squash-title-prefill-check | created: 2026-08-19 | last_used: 2026-10-04 | uses: 69 | tier: archive-candidate | origin: 2026-08-19-195244 -->
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
  <!-- id: conv-changelog-from-tag-range | created: 2026-09-17 | last_used: 2026-10-02 | uses: 21 | tier: archive-candidate | origin: 2026-09-17-183008 -->
- **Retired Maven modules need placeholder manifests for Snyk (2026-09-01, Snyk team +
  Eric).** Snyk keys a project on repository+branch+manifest path and never retires it —
  deleting a module freezes its findings on the last resolved dependency tree, failing
  the security gate forever. A parentless dependency-free `packaging=pom` placeholder
  re-tests to an empty graph (zero findings). Live at `system/rest-spring-3/` +
  `examples/rest-spring-3-example/` (PR #305) with relocation metadata to the Boot-4 twins;
  **release version sweeps must include these non-reactor poms deliberately.** Relates
  [[stack-integration-spring-boot4]].
  <!-- id: snyk-retired-manifest-placeholders | created: 2026-09-01 | last_used: 2026-10-05 | uses: 25 | tier: active | origin: 2026-09-01-022524 -->
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
  <!-- id: conv-ports-adopt-java-release-number | created: 2026-09-16 | last_used: 2026-10-06 | uses: 28 | tier: active | origin: 2026-09-16-003354 -->
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
  <!-- id: conv-template-version-sweep | created: 2026-09-11 | last_used: 2026-10-05 | uses: 21 | tier: active | origin: 2026-09-11-005808 -->
- Watch serialization gotchas (Long↔Integer downcast; use `util.str2int/str2long`).
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
  RFC-0002 in the RFC register, PR #470); all five sibling checkouts scanned clean, tree and commit messages. Relates
  [[eric-code-changes-via-pr]] (how a change reaches main).
  <!-- id: conv-compartment-downstream-secrets | created: 2026-09-30 | last_used: 2026-09-30 | uses: 1 | tier: core | origin: 2026-09-30-164034 -->

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
  carry the result. Relates [[minigraph-dev-mode-app-shape]] (the deploy layout: only the entry
  page moved out of `public/`).
  <!-- id: help-edit-needs-bundle-release | created: 2026-10-01 | last_used: 2026-10-04 | uses: 9 | tier: archive-candidate | origin: 2026-10-02-015208 -->

- **Under the Spring Boot parent, a bump of a Boot-managed library goes through Boot's version property, and a version sweep is
  verified by what resolves, not by the pom text (2026-10-06, #513).** The poms declare `junit-bom` as a plain test dependency
  (`<type>pom</type>`), which manages nothing, so bumping it from 6.0.3 to 6.1.3 left every test on Boot 4.1.1's JUnit 6.0.3, as
  `mvn dependency:list` showed. `<junit-jupiter.version>` (37 poms, beside `<tomcat.version>`) moved it. Likewise a module that never pins a
  Maven plugin builds with Boot's pluginManagement version (8 modules: compiler 3.14.1, surefire 3.5.4) or Maven's default (the Kotlin
  example); the build log's `--- plugin:version` lines show which ran. Replace versions by artifact, never by bare string: 3.3.1 is both
  maven-source-plugin and maven-resources-plugin. Relates [[conv-template-version-sweep]] (the templates' Gradle builds pin `junit-bom` too).
  <!-- id: conv-boot-managed-version-overrides | created: 2026-10-06 | last_used: 2026-10-06 | uses: 1 | tier: working | origin: 2026-10-06-025608 -->

- **Unit tests remove the temporary files they write when they complete, and that is verified by measuring, not by reading the code
  (Eric, 2026-10-06; applied in #516 squash `fd02ca6e` and mercury #360 merge `acf767d7`).** Prefer a folder the framework removes (JUnit's `@TempDir`, a Rust drop guard), and remove in `finally` whatever a
  test writes elsewhere: a test that opens a Playground session closes it (the engine then removes its draft from `location.graph.temp`),
  a test whose graph ends suspended removes its record, and an embedded server removes its data folder once it has stopped. A Rust test
  binary has no after-all hook: it uses `mercury-test-support` (`temp_path` for per-process files, `run_at_exit` for a cleanup the code
  under test owns, such as `elastic_queue::shutdown_cleanup`), and a starter template, which cannot depend on it, a drop guard. Verify with a
  marker file: run the suite, then list what in the temp folders (`$TMPDIR`, `/tmp/*`) is newer than the marker, module by module. A file
  created and deleted during the run does not show, so whatever shows was left behind; an empty folder does not show in a files-only
  listing, so look for folders too. Thread: [[test-temp-file-housekeeping]].
  <!-- id: conv-tests-remove-temp-files | created: 2026-10-06 | last_used: 2026-10-06 | uses: 1 | tier: working | origin: 2026-10-06-045656 -->

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
  graph.task→wrapper drive, real Gemini verdicts, one distributed trace. **Re-driven 2026-10-01 with real Claude through both engines and both
  packs ([[llm-helper-certification-java]]); the AI nodes are now a Claude-only helper app in the packs.** Enterprise LLM
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
  <!-- id: bp-agent-orchestration | created: 2026-08-25 | last_used: 2026-10-02 | uses: 20 | tier: working | origin: 2026-08-25-213703 -->
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
