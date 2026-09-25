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
- **latest_release:** v4.12.18 (2026-09-25 20:56:19Z — **two field reports answered on both engines, lock-step with the Rust
  port**: release PR #464 squash `1e419a29`, tag `v4.12.18` → `70e00474` (one memory-only commit past the squash), pom verified at
  the tag; the GitHub release body is the CHANGELOG entry). **Content:** #462 graph.math typed and finite — a boolean is never a
  number, unknown functions and overflow fail by name, `CONDITION` ([[graph-math-typed-arithmetic]]; READ: a graph that relied on
  `true` computing as 1/0 or on `Infinity` propagating now fails at that statement) and #463 the field's Snyk bumps (Jackson 2 BOM
  2.22.3, Jackson 3 BOM 3.2.3, Netty 4.2.18.Final, MsgPack 0.9.12 — the MsgPack one best effort: CVE-2026-90472, medium, no
  upstream fix yet). Sweep BUILD FILES ONLY 43 / 98 (unchanged). Readiness 220 suites, 1517 tests, 0 failures, 3 skipped (surefire
  XML; +3 = the #462 tests). **Lockstep:** Rust v4.12.18 the same minute (mercury #331 → merge `568d71b2`, tag → `7d07e9bd`, release
  20:55:02Z; Increment 141 plus the Rust-only 140; crates 12/12 20:58Z); the python/node packs stay at 4.12.15. Main CI was in
  progress on the tag commit at close. **Unreleased on main since 2026-09-25:** #465 squash `40ce30a7` — `graph.model.automation` accepts a list of
  manifests, later wins ([[graph-manifest-list-later-wins]]; Rust #332 `d3d82a3f`, Increment 142) — the 4.12.19 content so far.
  Next: the AI SDLC/MCP backlog ([[bp-agent-orchestration]]) resumes (Eric). Origin
  2026-09-25-194902.md.
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
- **last_review:** 2026-09-25 | through 2026-09-25-014100.md (ON COMMAND, Eric, after the v4.12.17 cycle — window 0 sessions since the
  234713 advisory sweep; archived 0 (oldest archive-candidates at 18 of `archive_window` 20), swept 0 (six closed threads at
  completion ages 0–18, 56 narrative lines ≤ 150), reactivated 0, tier changes 0; invariant re-verify DUE at 40 — thread
  raised (see `last_invariant_check`); no stalled thread (`thread_stale_window` 40; the two unchecked threads at 14 and < 5);
  condensed four long shipped decisions (`static-decision-table-is-graph-data`, `minigraph-dev-mode-app-shape`,
  `otel-optional-service-and-negative-control`, `connected-edge-spans` — every rule, READ note, footer and link kept) and the
  release priors; two stale statements corrected (the Rust hardening follow-up is done — mercury #329; the consumer-identity
  fact carries its merge refs and its release). Lines 851 → 802, facts 52 (+1 gate thread).)
  Prior: 2026-09-24 | through 2026-09-24-234713.md (advisory sweep at the v4.12.17 seam — archived 2 overdue conventions whose rules
  shipped upstream; facts 34 → 32) · 2026-09-23 | through 2026-09-23-230527.md (CADENCE + SIZE — archived 6, swept 5; facts 40 → 34) ·
  2026-09-23 | 2026-09-23-014650.md (size; lines 1032 → 1000) · 2026-09-22 | 2026-09-21-233928.md (size; swept 2).
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
  <!-- id: otel-optional-service-and-negative-control | created: 2026-09-16 | last_used: 2026-09-23 | uses: 9 | tier: archive-candidate | origin: 2026-09-16-193203 -->

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
  <!-- id: clean-knowledge-design-over-engine-coverage | created: 2026-09-18 | last_used: 2026-09-25 | uses: 10 | tier: active | origin: 2026-09-18-174943 -->

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
  <!-- id: soa-redis-cluster-support | created: 2026-09-14 | last_used: 2026-09-22 | uses: 9 | tier: archive-candidate | origin: 2026-09-14-181948 -->

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
  <!-- id: cache-separate-from-soa | created: 2026-09-14 | last_used: 2026-09-22 | uses: 13 | tier: archive-candidate | origin: 2026-09-14-191748 -->

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
  [[conv-reentrantlock-not-synchronized]]; used by [[redis-connection-foundation]]. **minimalist-kafka rides it
  since 2026-09-22:** `KafkaRuntime.shutdown()` (PR #440) closes the flow adapter's consumers (LeaveGroup) then the
  producer, and PR #441 (squash `7fdc59ab`) bounds BOTH halves by `KafkaRuntime.SHUTDOWN_GRACE` (10 s) — the producer
  close reports what the grace could not deliver instead of waiting without bound (Eric's ruling; the Rust port
  flushes within the same bound, mercury #313). The lifecycle now has its first module-level consumer with a
  graceful-shutdown contract pinned by `KafkaShutdownTest`.
  <!-- id: platform-onshutdown-lifecycle | created: 2026-09-14 | last_used: 2026-09-23 | uses: 8 | tier: archive-candidate | origin: 2026-09-15-011235 -->

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
  (the same async-callback minefield). **Extended 2026-09-23 (#454, Eric — "every abort should come with a reason"):** a
  dry-run's terminal is `Graph traversal aborted: <reason>` on both engines (node named; the companion and the web app
  match the PREFIX — a bare terminal is gone); arithmetic plugins' null argument now carries a message. Java `bf3250d7`.
  <!-- id: minigraph-guarded-async-completion | created: 2026-09-15 | last_used: 2026-09-25 | uses: 10 | tier: active | origin: 2026-09-15-040141 -->

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
  <!-- id: minigraph-dev-mode-app-shape | created: 2026-09-15 | last_used: 2026-09-24 | uses: 11 | tier: archive-candidate | origin: 2026-09-15-221451 -->

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
  <!-- id: playground-session-broker | created: 2026-09-03 | last_used: 2026-09-23 | uses: 11 | tier: archive-candidate | origin: 2026-09-03-172753 -->

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
- **`v1.cache.redis` classifies its own Redis failures — a command timeout is 408, an unreachable Redis is
  503, only a server answer stays 500 — in both engines (Eric, 2026-09-20; Java `RedisFailure.classify` in
  `redis-connection`, applied by `RedisCache`; Rust `classify_command_error` in the foundation's command
  path; MERGED 2026-09-20 — #429 squash `aa35d8c6`, mercury #289 merge `017bf8ed`).** Found by the interop's outage leg after [[exception-status-from-cause-chain]] landed: Layers 2
  and 3 still answered 500 because the flow and graph engines pass a task's status through *faithfully* —
  and the status they were given was the platform's default for Lettuce's `RedisCommandTimeoutException`,
  which carries none; Layer 1 read 408 only because its 5 s RPC timer beat Lettuce's 5 s command timeout by
  milliseconds (the L2 flow's 10 s `ttl` never let the engine's own 408 fire). **Rule:** a status is set
  where the failure is known — in the function that owns the client — never repaired downstream in a
  handler. Classification walks the cause chain (a wrapper never hides it; an `AppException` on the chain
  is returned as-is): `RedisCommandTimeoutException` → 408 with Lettuce's message; `RedisConnectionException`
  / `ConnectException` / `ClosedChannelException` / a closed-or-rejected `RedisException` → 503 `Redis
  unavailable - …` (the `redis.health` vocabulary); `RedisCommandExecutionException` (e.g. WRONGTYPE) and
  unrelated failures → unclassified, the default mapping. Rust mirrors it in `RedisBackend::query`, so every
  consumer of that path inherits it (sync-over-async does not use it). Proven by the fifth full drive: 122/122,
  no outage probe on any layer of either engine answers 500 (Java all 408 — Lettuce buffers to its command
  timeout; Rust 408 for its deadline, 503 for refused/broken-pipe). Behaviour change to READ: a caller that
  keyed on 500 for a Redis outage now sees 408/503. Relates [[redis-connection-foundation]],
  [[l1-caller-checks-reply-status]].
  <!-- id: redis-failure-classification | created: 2026-09-20 | last_used: 2026-09-22 | uses: 3 | tier: archive-candidate | origin: 2026-09-20-004702 -->
- **The shared Redis connection is RESET after a command timeout — recovery is bounded by `redis.timeout.ms`,
  not by Lettuce's reconnect backoff (Eric's ruling on interop Finding 4, 2026-09-21; `redis-connection`
  foundation, branch `fix/redis-reset-on-timeout` `48ab9b4f`; PR #433 MERGED 2026-09-21, squash `d29318fd`).** Lettuce reconnects a dropped
  connection on an exponential backoff capped at 30 s, so after a long outage a pod kept timing out for up to
  ~30 s after Redis was back while `redis.health` (a fresh probe connection) read green. `ResettableRedisBackend`,
  the new base of both backends: `commands()`/`async()` are stable Proxy facades resolving the live connection
  per call (every consumer captures the handle at construction, so it follows the reset); **rule:** a timeout
  on a connection that is NOT open resets at once, on an OPEN connection the SECOND consecutive timeout does (a
  reply clears the count), one reset per connection instance; the next command reconnects, a failed connect is
  a fast 503 held for 250 ms against a connect storm; the first connect stays eager. The async path cannot see
  an awaited timeout, so `RedisBackend.onCommandTimeout()` lets the cache's `MPUT` report it — and MPUT's
  timeout is now the same 408 as a blocking one (was `IllegalStateException` → 500). **Proven live:** the
  Finding 4 scenario re-driven (40 s and 10 s outages) — one in-flight 408, then fast 503s with `/health` 400,
  and a live reply 30 ms after Redis returned in both runs (report section *Recovery after the connection-reset
  fix*, both twins). Rust needed no change (redis-rs reconnects on the first command). **Lessons:** a Proxy
  facade needs a typed rethrow (`<T extends Throwable> RuntimeException rethrow(Throwable) throws T`) so the
  command's own exception reaches the caller unwrapped without `throws Throwable` (Sonar S112); and a live
  outage drive must kill the helper's embedded `redis-server` child, not just its JVM, and assert the port
  closed — the first drive measured nothing. Relates [[redis-failure-classification]],
  [[redis-connection-foundation]], [[l1-caller-checks-reply-status]]; applies [[conv-reentrantlock-not-synchronized]].
  <!-- id: redis-connection-reset-on-timeout | created: 2026-09-21 | last_used: 2026-09-22 | uses: 2 | tier: archive-candidate | origin: 2026-09-21-012124 -->
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
  <!-- id: static-decision-table-is-graph-data | created: 2026-09-20 | last_used: 2026-09-25 | uses: 4 | tier: active | origin: 2026-09-20-152704 -->
- **EventApiService serves LOCAL routes only — an inbound `/api/event` call to a route
  the instance does not host answers 404 even when the instance's own
  `yaml.event.over.http` map points that route at a peer (Eric ratified 2026-08-30).**
  Forwarding would make every app an Event-over-HTTP relay and open routing loops; the
  `x-event-api` wire marker is the existing loop guard (an event that crossed the wire
  once is never re-forwarded), the map is caller-side knowledge (not a promise to third
  parties), and deliberate hop-through is an explicit relay function (demo:
  `hello.remote.relay`). Recorded in the progressive-rendering interop report.
  <!-- id: event-api-local-routes-only | created: 2026-08-30 | last_used: 2026-08-30 | uses: 1 | tier: core | origin: 2026-08-30-050040 -->

- **Both engines ship `group.protocol=auto` in the bundled Kafka consumer template (Eric, 2026-09-21).**
  Java resolves `auto` by probing the cluster's finalized `group.version` feature (`GroupProtocolResolver`,
  one probe per cluster, rides the pre-auth `ApiVersions` handshake); the Rust port resolves it
  optimistically — each binding's consumer starts with the KIP-848 `consumer` protocol and rebuilds once as
  `classic` when the broker refuses it (librdkafka's fatal `ConsumerGroupHeartbeat` error:
  `UNSUPPORTED_VERSION` with the protocol disabled, `_UNSUPPORTED_FEATURE` before 4.0; a refused join never
  becomes a member, so no probe and no synthetic group). Same outcome, different mechanism; both keep the
  conflict guard (`session.timeout.ms` / `heartbeat.interval.ms` / `partition.assignment.strategy` with `auto`
  → `classic`, named). Override: `KAFKA_GROUP_PROTOCOL` or the application's own template. **Why:** the Rust
  K4 interop drive (mercury #300) ran both engines classic on a Kafka 4.3.1 broker that already finalized
  `group.version=1`, because this template shipped the line commented out — automatic switching was opt-in
  and nobody had opted in. **Release note to READ:** an application that never set `group.protocol` now joins a
  KIP-848 cluster with the consumer rebalance protocol. Java branch `feat/kafka-group-protocol-auto-default`
  (`ee37b907`, PR #436 MERGED 2026-09-21, squash `7def2bb4`); Rust twin on mercury `feat/kafka-group-protocol-auto`. Relates [[kafka-mesh-opt-in]] (the module is
  the opt-in building block, not the mesh) and [[conv-ports-adopt-java-release-number]] (the Rust port carries
  the same default at its next catch-up).
  <!-- id: kafka-group-protocol-auto-default | created: 2026-09-21 | last_used: 2026-09-23 | uses: 3 | tier: archive-candidate | origin: 2026-09-21-184342 -->

- **The Kafka flow adapter can carry its own Schema Registry identity — an opt-in second codec on the existing prefix seam,
  never a new mechanism (a field installation's fix, reviewed and reconstructed 2026-09-24; PR #458 squash
  `37e0f792`; SHIPPED in v4.12.17).** `KafkaFlowAutoStart` shares one `SchemaCodec` between
  `simple.kafka.notification` and the flow adapter unless `schema.registry.consumer.properties` names a registry client
  template; then `resolveConsumerSchemaCodec` builds the adapter's codec through `SchemaCodec.fromConfig(config, url, prefix)`
  under `schema.registry.consumer` — the seam twin-kafka runs on for a second cluster's registry, applied to ONE registry with
  two identities: the same `schema.registry.url` (a consumer decodes ids minted by the registry its producers use), that
  template, `schema.registry.consumer.serde.*` overrides on top, its own caches. Presence is the opt-in (blank = unset, the
  `${ENV_VAR:}` idiom); unset, the old behaviour byte-for-byte, pinned by `assertSame`; `schema.registry.url` stays the feature
  switch. **Why:** Confluent installations that grant CSFLE key (KEK) access per direction — separate produce and consume
  identity pools — so no single identity decrypts everything a service consumes. **Two seam facts to READ before advising a
  field:** a `<prefix>.serde.*` override reaches the serdes' configuration and the DEK-registry client CSFLE builds from it
  (where key access is decided), NOT the codec's own schema-by-id client, which keeps the template's identity — a second
  template covers both; and each codec reads only its own prefix, so a `schema.registry.serde.*` KMS credential is repeated
  under the consumer prefix. **Synced 2026-09-24 (Eric's ruling before v4.12.17):** the policy is now the public, prefix-parameterized
  `SchemaCodec.forConsumer(config, url, keyPrefix, producerCodec)`; the primary helper delegates and twin-kafka's
  secondary adapter uses it under `secondary.schema.registry` (`secondary.schema.registry.consumer.properties`, PR #460
  squash `47d9acf8`); the Rust twin `SchemaCodec::for_consumer` (Increment 139)
  carries the identity in the template — no serde layer there. **Sample:** the sync-over-async demo's `application.properties` (commented block, both
  variants) and `schema-registry-consumer.properties` next to it — in the application, never the library jar (a same-named
  resource would collide by classpath order). Applies [[clean-knowledge-design-over-engine-coverage]]; relates [[kafka-mesh-opt-in]].
  <!-- id: kafka-consumer-registry-identity | created: 2026-09-24 | last_used: 2026-09-24 | uses: 3 | tier: archive-candidate | origin: 2026-09-24-222349 -->

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
  [[static-decision-table-is-graph-data]] and [[minigraph-guarded-async-completion]].
  <!-- id: graph-math-typed-arithmetic | created: 2026-09-25 | last_used: 2026-09-25 | uses: 3 | tier: active | origin: 2026-09-25-183540 -->

- **`graph.model.automation` accepts a comma-separated list of manifests, and the later manifest wins (Eric, 2026-09-25;
  PR #465 squash `40ce30a7` MERGED 2026-09-25, Rust twin mercury #332 merge `d3d82a3f`, Increment 142; UNRELEASED — ships in
  4.12.19).** Born at the leadership demo's deploy step: an
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
  <!-- id: graph-manifest-list-later-wins | created: 2026-09-25 | last_used: 2026-09-25 | uses: 2 | tier: active | origin: 2026-09-25-223633 -->

- **A traced HTTP request is ONE connected span tree whose root is the edge's round-trip span; a streamed response is traced
  at its head and its tail, never per token (Eric's rulings on the Dynatrace review of the v4.12.15 certification traces,
  2026-09-22; PR #444, Rust #315, Python #36, Node #104; SHIPPED in v4.12.15).** REST automation mints a span at receipt
  (`TraceInfo.newSpanId()`), the first function (and the auth service) parent onto it, and `HttpRouter.closeContext` emits
  the record `service=http.request` on every completion path — single-shot writer, stream terminal, error writer, housekeeper
  timeout — with `start` = receipt, `exec_time` = the round trip, `parent_span_id` = the inbound traceparent span; an in-band
  stream failure after the head reports its own status. **The four OTel forwarders map SERVER iff `service == http.request`;
  every function execution is INTERNAL** — a backend's response time for a service is now the request, not the first
  function's 0.5 ms. The Event-over-HTTP stream relay's client leg (`async.http.request`) parents onto the sender
  (`sendWithEventHttp` stamps the span; Rust stopped zero-tracing the route — `skip.rpc.tracing` only suppresses the
  caller-side RPC `round_trip` record, `InboxBase` semantics). `EventStreamWriter` stamps the producer's trace+span on the
  first segment and the terminal via the thread-keyed `EventEmitter.getCurrentTrace()`, the client relays stamp the client
  leg's own span on synthesized head/eof/exception segments and forward raw token frames untraced, and the reply lane
  annotates the terminal's record with `frames` = the data-segment count. **Why it was invisible:** 0 export failures in
  every drive; only the backend's trace tree showed the orphans — and the drive's fabricated `traceparent` broke every root, a
  drive artifact that looked like an engine defect (send `X-Trace-Id`, or nothing, when there is no real upstream span).
  **READ at 4.12.15:** one more span per traced request; the first function is INTERNAL; an Event-over-HTTP callee edge
  records its own round trip between the caller's span and `event.api.service`; dashboards keyed on `kind=SERVER` move to the
  edge record. Confirmed in Dynatrace by Eric (Scenario 9 and the token-bearing drive 9 with `annotation.frames: 8`; reports
  in `docs/test-reports/`). Extends [[otel-optional-service-and-negative-control]]; applies
  [[trace-thread-keyed-mono-gotcha]] (capture the span on the worker thread — the relay outlives it); tested by
  `EventOverHttpStreamTest.edgeRelaySpansAreConnected` and the Rust `event_over_http_stream::edge_relay_spans_are_connected`.
  <!-- id: connected-edge-spans | created: 2026-09-22 | last_used: 2026-09-23 | uses: 3 | tier: archive-candidate | origin: 2026-09-22-200813 -->

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
  <!-- id: conv-squash-title-prefill-check | created: 2026-08-19 | last_used: 2026-09-24 | uses: 53 | tier: archive-candidate | origin: 2026-08-19-195244 -->
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
  <!-- id: conv-changelog-from-tag-range | created: 2026-09-17 | last_used: 2026-09-25 | uses: 13 | tier: active | origin: 2026-09-17-183008 -->
- **Retired Maven modules need placeholder manifests for Snyk (2026-09-01, Snyk team +
  Eric).** Snyk keys a project on repository+branch+manifest path and never retires it —
  deleting a module freezes its findings on the last resolved dependency tree, failing
  the security gate forever. A parentless dependency-free `packaging=pom` placeholder
  re-tests to an empty graph (zero findings). Live at system/rest-spring-3 +
  examples/rest-spring-3-example (PR #305) with relocation metadata to the Boot-4 twins;
  **release version sweeps must include these non-reactor poms deliberately.** Relates
  [[stack-integration-spring-boot4]].
  <!-- id: snyk-retired-manifest-placeholders | created: 2026-09-01 | last_used: 2026-09-25 | uses: 23 | tier: active | origin: 2026-09-01-022524 -->
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
  <!-- id: conv-ports-adopt-java-release-number | created: 2026-09-16 | last_used: 2026-09-25 | uses: 21 | tier: active | origin: 2026-09-16-003354 -->
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
  <!-- id: conv-template-version-sweep | created: 2026-09-11 | last_used: 2026-09-25 | uses: 19 | tier: active | origin: 2026-09-11-005808 -->
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

- **Use `ReentrantLock`, not `synchronized`, for locks/critical sections while the build targets Java 21
  (Eric's directive, 2026-09-14).** On Java 21 a virtual thread that blocks inside a `synchronized` block
  PINS its carrier thread; JEP 491 lifts that only in JDK 24+, and the toolchain intentionally stays on 21
  until the field runs Java 25 ([[stack-language-java21]]). So lazy-init and shared-state guards use a
  `ReentrantLock` (try/finally) — it does not pin. Precedent: `RedisHealthProbe` (carries the explicit
  comment) and the return-route coordinator; applied to the distributed cache's `CacheRuntime`. This
  decays once the toolchain moves to Java 25 (JEP 491 makes `synchronized` non-pinning). Relates
  [[virtual-threads-rpc]], [[kafka-clients-kernel-threads]]; applied in [[redis-connection-foundation]].
  <!-- id: conv-reentrantlock-not-synchronized | created: 2026-09-14 | last_used: 2026-09-21 | uses: 6 | tier: archive-candidate | origin: 2026-09-14-230259 -->

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
  <!-- id: bp-agent-orchestration | created: 2026-08-25 | last_used: 2026-09-23 | uses: 19 | tier: working | origin: 2026-08-25-213703 -->
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
