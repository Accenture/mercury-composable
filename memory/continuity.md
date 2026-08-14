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
- **last_session:** 2026-08-14 | agent: Claude Code (2026-08-14-015642)
- **last_review:** 2026-08-14 | through 2026-08-14-005928.md
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
  <!-- id: release-4-8-1-shipped | created: 2026-07-11 | last_used: 2026-08-07 | uses: 13 | tier: archive-candidate | origin: 2026-07-12-002326 -->

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
  <!-- id: graph-suspend-resume-design | created: 2026-07-29 | last_used: 2026-08-11 | uses: 14 | tier: active | origin: 2026-07-29-010343 -->

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
  <!-- id: compilegraph-mandatory-gate | created: 2026-07-29 | last_used: 2026-08-11 | uses: 10 | tier: active | origin: 2026-07-29-190328 -->

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

- **Telemetry/log presentation parity across language engines is a field requirement (Eric,
  2026-07-23).** Installations will be POLYGLOT for a long time — DevSecOps teams see both engines'
  telemetry and logs in one aggregation, and any presentation difference is a support burden.
  Operating rule: the Java engine is the REFERENCE implementation; a same-language interop run must
  be an exact structural replica after normalizing volatile fields — then cross-language runs are
  symmetric by construction (reference-signature procedure: session 2026-07-23-145132).
  **Scope extension: the Event Script surface is part of the cross-engine contract** — flows are
  engine-portable YAML, so any new built-in simple plugin ships in lock-step on both engines (with
  closely matching error messages), or flows stop being portable.
  <!-- id: conv-telemetry-presentation-parity | created: 2026-07-23 | last_used: 2026-08-11 | uses: 22 | tier: active | origin: 2026-07-23-145132 -->

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
  <!-- id: graphjs-phase-out-direction | created: 2026-08-01 | last_used: 2026-08-10 | uses: 5 | tier: active | origin: 2026-08-01-035647 -->

- **The `helpers/` standalone servers exist for Docker-less developer machines and are
  the standard local test servers for Rust ports (Eric, 2026-07-29).** They embed REAL
  redis/kafka servers as plain `java -jar` apps because many field developers work on
  Windows — especially VDI environments with no virtualization system, where Docker/
  Testcontainers are unavailable. Usage convention: redis-standalone serves the Rust
  minigraph-playground (suspend/resume live drives); kafka-standalone + the
  schema-registry mock will serve the future minimalist-kafka Rust port.
  **Field-confirmed 2026-08-03: the embedded redis server works on Windows under VDI**
  (the design-target environment — no Docker, community Redis binary 5.0.14, plain
  `java -jar`), reported by the field alongside the v4.11.2 rollout; this also validates
  the version-aware GETDEL/MULTI-EXEC consume strategy in its motivating environment
  (see the closed thread-redis-getdel-compat).
  <!-- id: conv-helpers-docker-less | created: 2026-07-29 | last_used: 2026-08-11 | uses: 9 | tier: active | origin: 2026-07-29-190328 -->
- Add capability: function (`@PreLoad` + `TypedLambdaFunction`) → flow YAML →
  register in `flows.yaml` → `rest.yaml` mapping if HTTP-facing.
  <!-- id: conv-add-capability | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->
- Watch serialization gotchas (Long↔Integer downcast; use `util.str2int/str2long`).
  <!-- id: conv-serialization-gotchas | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->
## Blueprint  *(gap from Current State → Vision; `(blueprint)` threads serve `vision-mercury-composable`)*

- [ ] (blueprint) Integrate a **pluggable AI companion LLM backend**; mature `POST /api/companion/{id}`
  from a dev-only command pipe into a governed collaboration layer. → serves: vision-mercury-composable
  <!-- id: bp-ai-companion-llm-backend | created: 2026-06-20 | last_used: 2026-08-14 | uses: 2 | tier: working -->
- [ ] (blueprint) **Enterprise governance lifecycle** for graph models (dry-run → certify → stage →
  approve → production), so models promote to production as standard endpoints. → serves: vision-mercury-composable
  <!-- id: bp-graph-governance-lifecycle | created: 2026-06-20 | last_used: 2026-08-14 | uses: 2 | tier: working -->
## Open Threads

- [ ] **(memory-health) Post-review continuity holds 34 decay-eligible facts above the
  configured cap of 30.** The 2026-08-14 review archived every item beyond the 20-session
  window; the remainder is recent or still live. Decide whether to condense recent completed
  threads as they age or raise `continuity_max_facts` for this mature multi-module reactor.
  <!-- id: memory-health-fact-cap-2026-08-14 | created: 2026-08-14 | last_used: 2026-08-14 | uses: 1 | tier: working | origin: 2026-08-14-005928 -->

- [x] (release — SHIPPED AND PUBLISHED 2026-08-10 local / 2026-08-11 UTC, **both repos in
  lock-step at v4.11.8**) **v4.11.8 — the dry-run suspend/resume regression-fix release, same evening as
  the regression report.** Java: release
  [PR #279](https://github.com/Accenture/mercury-composable/pull/279) squash `92dd64a8`
  (tree verified identical to the gated `7929b309`), CI green (7m29s), 33-pom sweep, full
  reactor as the gate, tag dereference-verified on the squash. Rust: release PR #205 merge
  `d16d68f0` carrying `5b659e50` (tree verified), CI green (2m34s), Cargo 4.11.6→4.11.8
  (CHANGELOG notes v4.11.7 was Java-only), 58/305 + clippy + fmt, tag on the merge,
  dereference-verified. Sole content: [[thread-dry-run-graph-scope-fix]] (Java PR #278 /
  Rust PR #204). Both GitHub releases PUBLISHED by Eric 2026-08-10.
  <!-- id: thread-release-4-11-8 | created: 2026-08-11 | last_used: 2026-08-11 | uses: 2 | tier: active | origin: 2026-08-11-051612 -->

- [x] (release — SHIPPED AND PUBLISHED 2026-08-11, **both repos in lock-step at v4.11.9**; cut
  explicitly FOR FIELD DEPLOYMENT) **v4.11.9 — the dry-run graph identity simplification.** Java: release
  [PR #281](https://github.com/Accenture/mercury-composable/pull/281) squash `eff46c5f` (tree
  verified identical to the gated `b134e1ca`), CI green (7m52s), 33-pom sweep, full reactor as
  the gate, tag dereference-verified on the squash. Rust: release PR #207 merge `27fa527e`
  carrying `40f99dc8` (tree verified), CI green (2m20s), 58/305 + clippy + fmt, tag on the merge,
  dereference-verified; both GitHub releases PUBLISHED by Eric 2026-08-11. Sole content:
  [[thread-untitled-dry-run-identity]] (Java PR #280 / Rust PR #206). **Extra pre-release gate for the field cut: a LIVE drive against the built v4.11.9
  artifacts** (redis-standalone + playground, real WS sessions) covering BOTH paths — tutorial-14's
  four-run named workflow (fresh → resume ×3 → "shipped") and a nameless draft suspending to
  `graph:untitled:{cid}`, resuming on a second instantiation, record consumed.
  <!-- id: thread-release-4-11-9 | created: 2026-08-11 | last_used: 2026-08-11 | uses: 1 | tier: working | origin: 2026-08-11-220600 -->

- [x] (fix — **MERGED ON BOTH ENGINES 2026-08-11, all CI green; rides the next release.**
  Java [PR #280](https://github.com/Accenture/mercury-composable/pull/280) squash `68cd9d28`
  (tree verified identical to the gated `f660ec32`), Build & Unit Tests 7m4s. Rust mercury
  PR #206 merge `3bdcd3b3` carrying `a901b1b7` (tree verified), test 2m17s.)
  **Dry-run graph identity simplified: an unnamed draft is scoped `untitled` instead of rejected**
  (Eric's patch; I reviewed, agreed, and applied the follow-ups). **The durable insight: the store
  contract needs the dry-run identity to be STABLE ACROSS INSTANTIATIONS, not derived from the
  model name** — so v4.11.8's rejection guard was only ever defending against its own ephemeral
  `playground-{uuid}` fallback, and a stable constant makes it unnecessary. Not a reversal of the
  morning ruling: that objection was correct *given a uuid*; this removes the premise rather than
  the capability. A GLOBAL constant also beats anything session-derived — the session handle comes
  from the WebSocket route (GraphLambdaFunction.java:273-276) and changes on reconnect, which would
  reintroduce the silent fresh-restart across a leave-and-return flow. Refuted during review (worth
  not re-litigating): the shared `untitled` bucket is not a NEW risk class (two developers dry-running
  the same NAMED graph with the same literal cid already collide), and GraphResume.java:111-113
  rejects a restored record whose node alias is absent, so structurally different drafts fail loudly.
  Follow-ups applied: Rust lock-step (guard + helpers + orphaned mirror test deleted); the missing
  pin for the unnamed branch on BOTH engines (nameless draft suspends then resumes across a second
  instantiation, **mutation-proven** against a per-instantiation handle); `## Unreleased` CHANGELOG
  entries on both repos (the published v4.11.8 section deliberately NOT rewritten); one stale comment.
  **Sonar S5961 lesson: it counts assertions reached through private test helpers, and counts static
  call SITES** — 12 graph-building `syncCommand` calls collapsed into ONE loop over a command list
  took the test from 28 to 22. Gates: engine 118/118; Rust 58/305 + clippy 0 + fmt clean.
  Relates [[thread-dry-run-graph-scope-fix]], [[graph-suspend-resume-design]].
  <!-- id: thread-untitled-dry-run-identity | created: 2026-08-11 | last_used: 2026-08-11 | uses: 1 | tier: working | origin: 2026-08-11-220600 -->

- [x] (fix — **MERGED ON BOTH ENGINES 2026-08-11, all CI green; rides the next release.**
  Java [PR #278](https://github.com/Accenture/mercury-composable/pull/278) squash
  `b697de5f` (tree verified identical to the gated `573c62aa`), Build & Unit Tests 7m49s.
  Rust mercury PR #204 merge `f5256ecc` carrying `8fc45b94` (tree verified), test 2m8s.)
  **v4.11.6 regression: dry-run suspend/resume never resumed — the playground lane's ephemeral
  graphId broke the graph-scoped store key.** Eric's tutorial-14 regression drive on v4.11.7
  (redis-standalone) hit "Transaction not found" at manager approval; root cause proven live
  BEFORE fixing (two orphaned `graph:playground-<uuid>:order-1001` records in his Redis): the
  dry-run lane minted `playground-{uuid}` per instantiation, so v4.11.6's `graph:{graph_id}:{cid}`
  key never matched across instantiations (pre-v4.11.6 `graph:state:{cid}` made the handle
  harmless — the same hidden-blocker shape as R2's extension cid). Executor lane never affected
  (stable manifest id — why all executor-lane e2e stayed green). **Fix + Eric's ruling:** dry-run
  identity = the root node's `name` property (export keeps it in sync with the file/deployment
  id); **an unnamed root + suspend/resume model is REJECTED at instantiation with a teaching
  message** (a silent ephemeral fallback would break resume invisibly — Eric); nameless
  non-suspending drafts keep the playground handle; guard-first = rejection has no side effects.
  Eric's S3776 catch fixed by extracting instantiateGraph (helper-extraction shape); Sonar smells
  folded (S6213 record→storedRecord, text block). Regression test PROVEN against unfixed code
  (fails on the key-scope pin) on Java; both engines pin resume-across-instantiations + the
  rejection. **Verified live end-to-end:** fixed build + redis-standalone + real WS session —
  tutorial-14's four runs (submit/approve/release/ship) all resumed, record under
  `graph:tutorial-14:{cid}`; both apps left running for Eric. Also folded (Eric's asks):
  minigraph-state-redis pom carries the engine's ENTIRE build section (sources JAR at build time —
  closes the IDE decompiled-class gap — jacoco, pinned plugins). Gates: engine 118/118, redis
  module 12/12, Rust 58/305 + clippy 0 + fmt clean.
  Relates [[thread-field-graph-scoped-state-and-error-context]], [[graph-suspend-resume-design]],
  [[thread-release-4-11-7]].
  <!-- id: thread-dry-run-graph-scope-fix | created: 2026-08-11 | last_used: 2026-08-11 | uses: 2 | tier: active | origin: 2026-08-11-051612 -->

- [x] (release — SHIPPED AND PUBLISHED 2026-08-10 local / 2026-08-11 UTC, **Java only — minimalist-kafka
  has no Rust counterpart by design; Rust stays at v4.11.6**) **v4.11.7 — the KIP-848
  auto-adoption release, same-day from field report to ship.** Release
  [PR #277](https://github.com/Accenture/mercury-composable/pull/277) squash `0c26cab4`
  (tree verified identical to the gated `f2856085`), CI green (7m27s), 33-pom sweep, full
  reactor as the gate, tag `v4.11.7` dereference-verified on the squash. Sole content:
  [[thread-kafka-kip848-auto]] (PR #276). GitHub release PUBLISHED by Eric 2026-08-10.
  <!-- id: thread-release-4-11-7 | created: 2026-08-11 | last_used: 2026-08-11 | uses: 2 | tier: active | origin: 2026-08-11-024542 -->

- [x] (feature — **MERGED 2026-08-11 as
  [PR #276](https://github.com/Accenture/mercury-composable/pull/276), squash `f709e168`
  (tree verified identical to the gated `03898b50`), CI green (Build & Unit Tests 7m10s);
  rides the next release via CHANGELOG Unreleased. Java-only by design, no Rust
  lock-step — minimalist-kafka's grammar is the future port's contract.**)
  **`group.protocol=auto`: the Kafka flow adapter adopts KIP-848 when the cluster supports
  it** — driven by a field report of high CPU during unscheduled consumer rebalances
  (cloud infra interruptions; the classic protocol's group-wide sync barrier makes every
  member rejoin when one pod flaps). **Durable facts:** (1) KIP-848 enablement is the
  cluster-wide finalized feature flag `group.version` (≥1), read via
  `Admin.describeFeatures()` which rides the pre-auth `ApiVersions` handshake —
  **never ACL-gated** (verified in the 4.3.1 broker source; honors the KafkaHealthCheck
  convention of avoiding Cluster-Describe-gated admin APIs — no grant needed beyond the
  template's connection credentials). (2) The adapter runs the consumer protocol with
  ZERO code change (17/17 e2e passed with the template value, incl. topic-pattern;
  java-regex subscribe implemented for the new consumer, KAFKA-15538); F4/F5 carry over
  (max.poll.interval.ms valid under both protocols). (3) **Fail-fast hazard:**
  session.timeout.ms / heartbeat.interval.ms / partition.assignment.strategy with
  group.protocol=consumer → ConfigException at construction — so auto's conflict guard
  resolves to classic + WARN naming the keys (never silently strip operator tuning).
  (4) Resolution at the single choke point KafkaClientConfig.consumerProperties (covers
  adapter, health check, twin-kafka secondary — per-cluster templates → independent
  per-cluster resolution); one probe per bootstrap per JVM, decision logged; probe
  failure/absent flag → classic (Confluent Cloud/Kora feature-field reporting unverified
  — auto conservatively stays classic there; CP 8.x = Apache 4.x core, reports it).
  (5) EmbeddedKafka gained a feature-pinned variant constructor
  (Formatter.setFeatureLevel) — tests pin BOTH live outcomes (group.version=1 → consumer;
  group.version=0 broker → classic) + e2e consume under the resolved protocol.
  Gates: minimalist-kafka 180/180, twin-kafka 9/9, both exit 0. Docs:
  guide #rebalance-protocol section, twin-kafka per-cluster note, template comment block,
  CHANGELOG Unreleased. Relates [[thread-kafka-consumer-resilience]],
  [[thread-redis-getdel-compat]] (the detect-once pattern), [[thread-release-4-11-2]].
  <!-- id: thread-kafka-kip848-auto | created: 2026-08-11 | last_used: 2026-08-11 | uses: 1 | tier: working | origin: 2026-08-11-024542 -->

- [x] (release — SHIPPED AND PUBLISHED 2026-08-10, **both repos in lock-step at
  v4.11.6**) **v4.11.6 — the field-review follow-ups release, out the same day as the
  review itself.** Java:
  release [PR #275](https://github.com/Accenture/mercury-composable/pull/275) squash
  `c29915ee` (tree verified identical to the gated branch commit), CI green (8m3s),
  33-pom sweep, full reactor as the gate, tag dereference-verified on the squash. Rust:
  release PR #203 merge `c008d11b` carrying `a3ae466f`, CI green (2m18s), 58/305 +
  clippy + fmt (exit codes verified unpiped), tag on the merge. Contents:
  [[thread-field-graph-scoped-state-and-error-context]] (BREAKING store key
  `graph:{graph_id}:{cid}` — the CHANGELOG's `### Changed` section LEADS with the
  upgrade note per R1) + [[thread-dynamic-statement-targets]] (incl. the recovery
  semantics). CHANGELOG shape: BREAKING Changed first, Added ordered exception-context →
  orchestrator → dynamic-vars (item 3 references the handler item 1 introduces).
  Both GitHub releases PUBLISHED by Eric 2026-08-10 (notes lead with the BREAKING
  upgrade note).
  <!-- id: thread-release-4-11-6 | created: 2026-08-10 | last_used: 2026-08-10 | uses: 1 | tier: active | origin: 2026-08-10-223319 -->

- [x] (feature+fix — **COMPLETE ON BOTH ENGINES 2026-08-08, all CI green; rides the
  next release.** Java: [PR #267](https://github.com/Accenture/mercury-composable/pull/267)
  squash `e16f4b40` + accept-header follow-up
  [PR #268](https://github.com/Accenture/mercury-composable/pull/268) squash `7ab9c771`
  (incl. Eric's javadoc cosmetics). Rust: mercury PR #197 merge `79212bc0` carrying
  `0530bd13` — the lock-step mirror PLUS **Eric's ruling: the Rust async HTTP client
  sends a default `Accept: */*` when the caller gives none** (Java reactor-netty parity;
  both REST servers omit response content-type absent Accept, so a model omitting
  `headers.accept` previously decoded JSON on Java but got raw bytes on Rust; explicit
  accept never overridden, wire-echo pinned both ways) + the INCREMENTS ledger repair
  (78/79 reconstructed, tail ordered 76→83, Overview extended).)
  **graph.task input mapping gains `model.*` staging (Event Script parity) + tutorial-13
  remodeled onto async.http.request.** Eric's debug report: `text(100) -> model.id` then
  `{model.id}` in a later entry resolved to "null" — the RHS silently landed in the
  request body (graph.task was the fetcher-family outlier; fetcher/extension already
  staged model.* under the shared guard). Fix: model.* RHS → guard + state-machine
  write, visible to later entries. **Durable facts:** (1) the CompileGraph gate already
  rejects reserved-metadata targets in input[] ("compiled or 404"); (2) **env-var
  substitution is load-time in BOTH lanes by design** — CompileGraph loads deployed
  models via ConfigReader (eager reference resolution), and the dry-run's `instantiate
  graph` round-trips the session graph through a temp file + ConfigReader, so the run
  instance gets resolved values while the authored/exported model keeps `${...}`
  placeholders; (3) the sync companion returns a traversal's JSON payload in `result`,
  console narration in `output`; (4) AsyncHttpRequest.fromMap renders the input-mapping
  map into the HTTP request (keys host/url/method/headers.{name}/body); (5) **graph/flow
  ttl bounds only the EVENT call to a composable function** — the AsyncHttpClient's own
  HTTP timeout is `headers.x-ttl` in MILLISECONDS (absent → 30s default, decoupled from
  the caller's deadline), which also rides the wire for end-to-end deadline propagation
  (Eric's X-TTL round: taught in tutorial-13, graph-task help, the guide's
  by-configuration section; wire-echo pinned via the mock's observed_ttl); (6) **never
  rely on an HTTP library's implicit default Accept** — reactor-netty sends `*/*`, the
  Rust client sends none, and both REST servers omit response content-type absent
  Accept, so JSON decoding silently differs across engines unless `headers.accept` is
  declared (the reference http-client-by-config flow always did). tutorial-13 =
  HTTP client by configuration vs mock.mdm.profile; HelloTask (v1.hello.task) retired.
  Module 105/105, webapp 212/212. Relates [[conv-telemetry-presentation-parity]],
  [[compilegraph-mandatory-gate]].
  <!-- id: thread-graph-task-model-staging | created: 2026-08-08 | last_used: 2026-08-10 | uses: 2 | tier: active | origin: 2026-08-09-025009 -->

- [x] (release — SHIPPED 2026-08-09, **both repos in lock-step at v4.11.5**) **v4.11.5 —
  the graph.task parity + teaching-surfaces release, out a day ahead of the field
  review.** Java: release [PR #270](https://github.com/Accenture/mercury-composable/pull/270)
  squash `9a6a9569` + the post-merge diagram patch `f8dd9cd7` (straight to main per
  Eric — no PR for a one-line docs patch), tag on `f8dd9cd7`, full reactor green (6:29).
  Rust: release PR #199 merge `4380e29d` + diagram patch `82b020e6`, tag MOVED
  pre-publication onto `82b020e6` (the standing docs-inclusive-tag ruling), 305 tests
  green. Contents: [[thread-graph-task-model-staging]] (incl. the Rust default-Accept
  ruling), tutorial-13 as HTTP-client-by-configuration, and the checkpoint/decision docs
  reframe (Eric's proposal; live on both gh-pages sites — the decision-pattern diagram's
  overlapping loop label fixed to 'waiting...', layout verified via the site's own
  mermaid bundle and confirmed clean on the live page by Eric).
  <!-- id: thread-release-4-11-5 | created: 2026-08-09 | last_used: 2026-08-09 | uses: 1 | tier: working | origin: 2026-08-09-164000 -->

- [x] (feature+fix — **COMPLETE ON BOTH ENGINES 2026-08-10, both merged same day; rides
  v4.11.6.** Java: [PR #273](https://github.com/Accenture/mercury-composable/pull/273)
  squash `96d9c35f`, CI green (Build & Unit Tests 6m49s). Rust: mercury PR #201 merge
  `354c1134` carrying `7d2da900`, CI green incl. the Format check (test job 2m9s).)
  **Dynamic variables in every statement command —
  completing the generic error handler — PLUS the recovery semantics follow-up
  (Java [PR #274](https://github.com/Accenture/mercury-composable/pull/274) squash
  `5a01c0c6`, CI green 7m17s; Rust PR #202 merge `213b739a` carrying `6c7cf134`, CI
  green — shipped in v4.11.6 same day, see [[thread-release-4-11-6]]): a successful retry of `error.source` RESOLVES the virtual 'error' node
  (code=200, source kept, message/stack removed; the source match keeps parallel
  branches safe — Eric's rationale) → three states: empty / recovered / outstanding.
  Pinned by unit-test-error-recovery (executor) + a tutorial-12 companion dry-run
  (traveler); engine 116/116, Rust 58/305 + clippy + fmt.** Eric's regression pass (tutorial 12, live
  session via the sync companion) found RESET:/NEXT: took targets literally — only
  MAPPING/COMPUTE expressions and IF conditions substituted — so a generic RETRY handler
  still needed per-node clones despite [[thread-field-graph-scoped-state-and-error-context]]'s
  error context. Ratified fix: NEXT:/THEN:/ELSE: targets, RESET: entries and DELAY:
  values all resolve {namespace.key} at execution time (unresolved → "null": RESET no-op,
  DELAY skipped, jump fails loudly). Java = shared getNext(tag, command, stateMachine)
  overload + per-tag substitution in GraphMath AND GraphJs; Rust = math executor only
  (no graph.js, per the retirement). tutorial-12 genericized on both engines
  (IF {error.code} == 200, RESET/NEXT {error.source}, clear-exception RESET
  {error.source}) — its e2e pins RESET/NEXT; new unit-test-dynamic-jump +
  DynamicStatementTargetTest pin THEN:/DELAY:. First finding of the same pass was NO bug:
  {error.status} → the contract key is error.code (R3, Event Script parity), validated
  live on Eric's session. Gates: Java engine 114/114 + webapp 212/212; Rust 58 suites /
  305 tests + clippy + fmt (fmt verified by real exit code — a piped `head` masked the
  first check; fmt belongs in the local gate line). Eric's cosmetics folded (S125-safe
  comment quoting ×3 files + 'safe point').
  <!-- id: thread-dynamic-statement-targets | created: 2026-08-10 | last_used: 2026-08-10 | uses: 1 | tier: active | origin: 2026-08-10-223319 -->

- [x] (feature — **COMPLETE ON BOTH ENGINES 2026-08-10, same day as the field review;
  both ride the next release.** Java: feature
  [PR #271](https://github.com/Accenture/mercury-composable/pull/271) squash `adfb2a0d`
  + post-merge polish [PR #272](https://github.com/Accenture/mercury-composable/pull/272)
  squash `0612ec6d` (Eric's IDE cosmetics + the S3776 fix — GraphExecutor adopts the
  traveler's handleSkillResponse→handleSkillSuccess split, making the walker twins MORE
  symmetric; the walker staging had pushed complexity to 18), both CI green;
  ADR-0013/ADR-0014 accepted via the merge. Rust: mercury PR #200 merge `283d41e2`
  carrying `24eeef89` + the `cargo fmt` follow-up `7dadd1ff` (first CI run failed the
  Format check — scripted test edits weren't rustfmt-clean; the mercury local gate is
  tests + clippy + FMT), 58 suites / 305 tests green; Rust ADR-0012/ADR-0013 accepted;
  port divergence documented: no native stack-trace transport → error.stack only on
  cross-engine records.) **Field review follow-ups: graph-scoped workflow state +
  generic exception context.** Eric's five rulings (R1 flag-day store-key change, release-note the break;
  R2 business-cid propagation for flow→subflow/flow→graph/graph→subgraph consistency;
  R3 Event Script parity naming error.source/code/message/stack; R4 reserve alias
  'error'; R5 orchestrator = unit test + docs mention, no tutorial-14 change).
  **Feature A (ADR-0013 proposed):** store contract scoped by graph + cid — envelope
  {cid, graph, node, ttl, model, seen, run}, get body {cid, graph}, Redis key
  `graph:{graph_id}:{cid}` (BREAKING: old-key records invisible → resume = fresh);
  GraphExtension.inheritBusinessCid stamps the parent's model.cid like an Event Script
  sub-flow (the second, hidden subgraph blocker: the child's cid was a per-call random
  UUID) → the orchestrator pattern composes (parent delegates independently resumable
  subgraph paths; reference pair unit-test-orchestrator/unit-test-sub-suspend).
  **Feature B (ADR-0014 proposed):** walkers stage error.source/code/message/stack at
  their exception choke points (one site per walker covers every skill incl. graph.task
  + async.http.request); skills' consolidated stageNodeError adds {node}.stack; 'error'
  was ALWAYS reserved in MiniGraph.RESERVED_NAMES (input/output/model/response/result/
  parameter/none/next/api/error) so NO breaking edge and no new gate rule; `inspect
  error` works by construction (raw state-machine viewer — Eric's virtual-node question).
  Durable gotchas: IF/THEN/ELSE = ONE multi-line statement string; probe possibly-absent
  keys with the `=` guard or the expression engine throws "Unknown identifier: null";
  mock MDM 'x-exception' header = AppException(401). Verified: engine 112/112, Redis
  store green (both consume strategies), webapp 212/212, full reactor exit 0.
  Relates [[graph-suspend-resume-design]], [[thread-suspend-resume-rationalization]],
  [[thread-graph-suspend-resume]].
  <!-- id: thread-field-graph-scoped-state-and-error-context | created: 2026-08-10 | last_used: 2026-08-11 | uses: 3 | tier: active | origin: 2026-08-10-180744 -->

- [x] (field validation — **CLOSED 2026-08-10: the review WENT WELL.** The team demoed a
  complex multi-suspension use case built in a short time on checkpoint-only v4.11.x —
  impressive adoption; the decision-node feature arrived after they started. The demo
  surfaced two structural asks — correlation-ID-only store references collide across
  domains/subgraphs, and per-fetcher error-handler clones make graphs busy — both
  ratified and taken up same-day as [[thread-field-graph-scoped-state-and-error-context]].)
  **The field team reviews the v4.11.4 suspend/resume rationalization on Monday
  2026-08-10** — the second team whose pain report drove the re-design evaluates whether
  edge/jump modes address their concerns; Eric reports back with their inputs.
  Relates [[thread-suspend-resume-rationalization]], [[thread-release-4-11-4]].
  <!-- id: thread-field-review-rationalization | created: 2026-08-08 | last_used: 2026-08-10 | uses: 2 | tier: active | origin: 2026-08-08-022929 -->

- [x] (release — SHIPPED AND PUBLISHED 2026-08-08, **both repos in lock-step at
  v4.11.4**) **v4.11.4 — the suspend/resume rationalization release.** Java: release
  [PR #266](https://github.com/Accenture/mercury-composable/pull/266), squash
  `ad60f7e4`, CI green, 33-pom sweep, tag on the verified squash, full reactor green
  (6:18) as the sole gate (Eric's ruling: no live kafka-demo regression — the
  minimalist-kafka delta is test-only + one comment). Rust: release PR #196, merge
  `27c2cc8e`, workspace 4.11.1→4.11.4 (CHANGELOG notes 4.11.2/4.11.3 were Java-only),
  58 suites green, tag on the verified merge. Contents:
  [[thread-suspend-resume-rationalization]] (ADR-0012/ADR-0011) + the S5778/S125 field
  Sonar fixes riding toward the field rescan
  ([[thread-sonar-4-11-x-field-round-3]]).
  <!-- id: thread-release-4-11-4 | created: 2026-08-08 | last_used: 2026-08-08 | uses: 1 | tier: archive-candidate | origin: 2026-08-08-022929 -->

- [x] (design+feature — **RATIFIED by Eric 2026-08-07 (R1-R7; R2 refined by Eric);
  Java half MERGED same day as
  [PR #265](https://github.com/Accenture/mercury-composable/pull/265), squash
  `392f7128`, CI green — the PR also folded Eric's cosmetic polish + two field Sonar
  fixes (S5778/S125 in minimalist-kafka; the field coverage condition now passes at
  82.7%, so those were the last gate failures) + the frame() helper cleanup; note the
  5-commit squash compounded six identical co-author trailers — keep the canonical
  trailer to the PR footer on many-commit PRs. Rust lock-step half IMPLEMENTED
  2026-08-08 on the mercury repo's feature/suspend-resume-rationalization, commit
  `995cfeb7` (Rust ADR-0011 amends ADR-0009; webapp REPLACED from this repo's latest
  UI per Eric's directive — PR #262's UI work now on both engines; knowledge-graph
  9 suites, e2e, webapp 212/212, clippy 0) — **MERGED 2026-08-08 as mercury PR #195,
  merge `4e6bdf43`, CI green. COMPLETE ON BOTH ENGINES**; both ride the next release**)
  **Suspend/resume rationalization: retire `suspend=true`; suspension becomes a
  destination** (drawn edge or graph.math jump into the reserved suspend node;
  Suspensible type = visual-only). Driver: a second field team hit the same
  decide-before-you-suspend wall — a design signal, not a docs gap. Review + phased
  plan: `draft-design-specs/suspend-resume-rationalization.md` (gitignored).
  **Ratified model — the discriminator is graph SHAPE, not skill class (Eric's
  refinement):** edge mode (drawn edge into suspend + mandatory continuation edge,
  shape-only gate rule, no statement inspection) = back-compat shape, resume skips
  suspend and continues, NO re-execution, continuation fan-out fine; jump mode
  (IF-THEN-ELSE jump, no drawn edge; suspend island-anchored — REQUIRED, the export
  path rejects orphan nodes) = best practice, resume RE-EXECUTES the decision against
  the new input (wait loop without RESET — obsoletes tutorial-14's await-decision
  idiom). By construction only routing skills can jump, so jump mode ⇒ routing skill
  with zero classification. Gate successor rules: routing-skill drawn edge to suspend
  rejected (the new teaching error); exception=suspend rejected (R7, pending);
  suspend=true = deprecation WARN no-op. Back-compat structurally cheap (v4.11.x
  models replay identically; record contract {cid,node,ttl,model,seen,run} untouched).
  Includes ADR-0012 proposal partially superseding ADR-0010's suspensible vocabulary.
  Rust lock-step required; field-core regression-critical surface.
  Relates [[graph-suspend-resume-design]], [[thread-tutorial-14-decision]].
  <!-- id: thread-suspend-resume-rationalization | created: 2026-08-07 | last_used: 2026-08-10 | uses: 4 | tier: active | origin: 2026-08-07-225034 -->

- [x] (feature — **COMPLETE ON BOTH ENGINES 2026-08-07**: Java MERGED as
  [PR #263](https://github.com/Accenture/mercury-composable/pull/263), squash `0c3e7618`,
  CI green; Rust MERGED same day as mercury PR #193, merge `bea95c80` carrying commit
  `8162b733` with its single co-author trailer, CI green — both ride the next release
  via CHANGELOG Unreleased / INCREMENTS 81)
  **tutorial-14: the manager approval became a real three-outcome decision** (team-member
  suggestion via Eric): approved → the approval checkpoint; explicit rejected → terminal
  manager-reject with the reason (then 404 on the consumed cid); anything else →
  re-suspend through await-decision, whose continuation loops back to check-approval.
  **Durable engine facts:** (1) the traveler/executor never re-execute a node marked
  seen, and seen marks are part of the PERSISTED suspension state — a wait loop across
  suspensions must RESET its own nodes (RESET before the IFs; an IF that jumps returns
  immediately; the seen mark is set at walk-entry so self-RESET sticks; resetNodes clears
  nodeSeen + skillRun + node scratch); (2) **the Playground Tutorials tab bakes
  resources/help/*.md into the webapp bundle at build time (vite ?raw glob)** — help
  edits are invisible in the tab until `npm run release` regenerates the bundle (both
  repos regenerated). Grammar surface: decide-before-you-suspend + the suspensible
  capability envelope + the wait-loop RESET pattern stated in the guide design rules,
  tutorial help, graph.suspend skill help, and the AI catalog (suspend entries
  byte-identical across engines); the suspend-on-routing-skill error TEACHES at all four
  enforcement sites. E2E: three outcomes + loop stability pinned on both engines.
  Relates [[thread-graph-suspend-resume]], [[graph-suspend-resume-design]].
  <!-- id: thread-tutorial-14-decision | created: 2026-08-07 | last_used: 2026-08-10 | uses: 4 | tier: active | origin: 2026-08-07-142823 -->

- [x] (release — SHIPPED 2026-08-07, **Java only — no Rust-ported surface touched; Rust
  stays at 4.11.1**) **v4.11.3 — the field support roll-up.**
  [PR #259](https://github.com/Accenture/mercury-composable/pull/259), squash `6a45867c`,
  CI green, tag on the verified squash. Consolidates: consumer poll-loop resilience +
  derived max.poll.interval.ms + schema negative tests (#255, polished by #258 —
  pollOnce extraction, Utility.sleep helpers with isInterrupted shutdown checks); the
  third field Sonar round (#256); KafkaRequestPublisher.partitions(topic) (#257); the
  kafka-demo piped-mode helper fix (#254, CHANGELOG entry added at the cut). Pre-release
  gates: full reactor green + live kafka-demo README regression, which also live-proved
  the release's own changes (piped publishers, retries→DLQ through pollOnce, interval
  derivation silent under the floor). Remaining external: the field rescan
  ([[thread-sonar-4-11-x-field-round-3]]).
  <!-- id: thread-release-4-11-3 | created: 2026-08-07 | last_used: 2026-08-07 | uses: 1 | tier: working | origin: 2026-08-07-021646 -->

- [ ] (field support — Sonar rescan of the 4.11.x line FAILED the field gate 2026-08-06:
  15 new issues + new-code coverage 79.4% vs 80; **all 13 code findings FIXED and MERGED
  2026-08-07 as [PR #256](https://github.com/Accenture/mercury-composable/pull/256),
  squash `26dfae7c`, CI green — remaining: the field rescan**; the partition-metadata
  API merged the same day as
  [PR #257](https://github.com/Accenture/mercury-composable/pull/257), squash `d029d144`,
  after the keep-both CHANGELOG Unreleased fold) **Third field Sonar remediation round.**
  3× S3776 helper extractions (GraphTraveler.handleSkillResponse → handleSkillSuccess +
  early-return late-reply guard; PlaygroundLoader → loadFeature; WorkerHandler →
  registerLogContext, same-thread synchronous so trace/log-context thread-keying is
  unchanged); 2× S1192 constants (kafka-demo catch-all); 5× S125 trailing-semicolon prose
  rewords; S5778 lambda single-invocation; S6213 `record` rename; S2925 replaced by a
  deterministic expiry rewrite on the stored record (Eric first hinted Utility.sleep,
  then preferred the deterministic version); S5961 27-assertion test split. Verified:
  kafka-demo 8/8, minigraph 99/99, platform-core 425 green. Coverage condition: ~6 lines
  short on 971 new — the resilience round's 8 tests + this round's split ride the next
  drop; if the rescan still fails, target the field's uncovered-lines view directly.
  Relates [[thread-sonar-4-10-4-field-rejection]] (same arc shape).
  <!-- id: thread-sonar-4-11-x-field-round-3 | created: 2026-08-07 | last_used: 2026-08-08 | uses: 3 | tier: working | origin: 2026-08-07-003746 -->

- [x] (field support — reported 2026-08-06 by a field member's code review; all three
  findings CONFIRMED and FIXED 2026-08-07; **MERGED as
  [PR #255](https://github.com/Accenture/mercury-composable/pull/255), squash `25d4f19c`,
  CI green (Build & Unit Tests 7m42s)**) **KafkaFlowConsumer poll-loop resilience.**
  F4 (fixed): the poll loop had no per-iteration guard — a routine post-rebalance
  CommitFailedException killed the binding until pod restart; now known transients WARN +
  continue (redelivery preserves at-least-once), unexpected exceptions pause with
  escalating backoff (1s→30s) — loud but alive. F5 (fixed; worse than reported — the
  retry envelope multiplies occupancy, so a 75s flow ttl breaches the 300s default on a
  failing message): the adapter now derives `max.poll.interval.ms` per binding from
  (maxRetries+1)×slowest-target-ttl + retries×backoff, ×max.poll.records + headroom
  (floor = Kafka default; explicit template value respected + WARN). Schema observation
  (fixed): json.fail.invalid.schema now pinned by negative tests on both serde sides via
  the `schema.registry.serde.*` pass-through. 171/171 module tests, JaCoCo gate met; new
  "Consumer liveness" guide section; CHANGELOG Unreleased. Repo wording generic.
  Remaining: Eric's PR gate; no Rust lock-step (module is Java-only by design).
  <!-- id: thread-kafka-consumer-resilience | created: 2026-08-07 | last_used: 2026-08-11 | uses: 3 | tier: active | origin: 2026-08-07-003746 -->

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
  <!-- id: thread-task-ttl-override | created: 2026-08-01 | last_used: 2026-08-02 | uses: 4 | tier: archive-candidate | origin: 2026-08-01-022358 -->

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
  <!-- id: thread-redis-getdel-compat | created: 2026-07-31 | last_used: 2026-08-11 | uses: 6 | tier: active | origin: 2026-07-31-180131 -->

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
  <!-- id: thread-kafka-2nd-level-routing | created: 2026-07-30 | last_used: 2026-08-11 | uses: 9 | tier: active | origin: 2026-07-30-233623 -->

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
  procedure). Residual observation RESOLVED 2026-08-03 (session 2026-08-03-163641,
  branch fix/kafka-demo-node-pipe-mode): publish-inbound.js had a real piped-EOF
  send/disconnect race (fixed with the inflight-chain pattern); the suspected
  publish-orders.js "off-by-one" was a capture artifact (prompt glue + an output
  filter), NOT a script bug — both publishers now suppress the prompt on non-TTY
  stdin. Full detail: origin log.
  <!-- id: thread-release-4-11-2 | created: 2026-08-03 | last_used: 2026-08-11 | uses: 4 | tier: active | origin: 2026-08-03-155225 -->

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
  <!-- id: thread-release-4-11-1 | created: 2026-08-01 | last_used: 2026-08-03 | uses: 2 | tier: archive-candidate | origin: 2026-08-01-230946 -->

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
  <!-- id: thread-graph-suspend-resume | created: 2026-07-28 | last_used: 2026-08-07 | uses: 10 | tier: archive-candidate | origin: 2026-07-29-003528 -->

- [x] (field support — CLOSED 2026-07-26: **field rescan of v4.10.6 PASSED the Sonar gate with a
  perfect Overall-Code score** — 0/0/0/0, coverage 80.5%) **v4.10.4 failed the field Sonar quality
  gate — 5 findings, fixed and released as v4.10.6** (fix PR #231 merge `c7d05d83`, release PR
  #232 merge `2a940250`; Copilot authored, Claude Code reviewed line-by-line + prepared the
  release, Eric gated every step). Findings: 2× S125 (prose comments ending in a stray semicolon
  pattern-match as commented-out code) + 3× S3776 (cognitive complexity — helper extraction,
  behavior-preserving). Verified by full reactor + a live Java-to-Java Event-over-HTTP interop
  drive targeting the three refactored trace/cid classes. Arc shape: rejection → fix → release →
  clean rescan (the [[thread-sonar-4-9-1-field-rejection]] shape). Full detail: origin log.
  <!-- id: thread-sonar-4-10-4-field-rejection | created: 2026-07-25 | last_used: 2026-08-07 | uses: 3 | tier: archive-candidate | origin: 2026-07-25-005125 -->

- [x] (release — CLOSED 2026-07-23) **v4.10.2 SHIPPED in lock-step** — metadata contract (#221),
  temporary.inbox alignment (Rust #171), team-contributed collection plugins
  isEmpty/getFirst/getLast (#220, reviewed in-session; formal GitHub review API blocked for EMU
  accounts). Java: PR #222, tag `v4.10.2` on `61ddb772` — **lesson: the first tag landed on the
  wrong commit (pull raced the merge); verify what a tag landed on before pushing it.** Rust:
  PR #172, tag on `6a39bccc`, after a deterministic fix for a parallel-test config-freeze CI race.
  Full detail: origin log.
  <!-- id: thread-release-4-10-2 | created: 2026-07-23 | last_used: 2026-07-23 | uses: 1 | tier: working | origin: 2026-07-23-211728 -->

- [x] (release — CLOSED 2026-07-23) **v4.10.1 SHIPPED in lock-step** — telemetry presentation
  parity patch (PR #217 content: /api/event visible span, declarative rename, event.api.auth
  demo, interop report). Java: PR #218, tag on `9ae666df`. Rust: PR #170, tag on `2c4e4066`.
  Full detail: origin log.
  <!-- id: thread-release-4-10-1 | created: 2026-07-23 | last_used: 2026-07-23 | uses: 1 | tier: working | origin: 2026-07-23-145132 -->

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
  <!-- id: thread-field-trace-propagation-4-6-3 | created: 2026-07-13 | last_used: 2026-08-01 | uses: 8 | tier: working | origin: 2026-07-13-142021 -->

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
  <!-- id: thread-minimalist-kafka-protobuf-revival | created: 2026-07-01 | last_used: 2026-08-03 | uses: 2 | tier: working | origin: 2026-07-01-224313 -->

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
  <!-- id: thread-kafka-client-version-upgrade | created: 2026-07-01 | last_used: 2026-08-03 | uses: 2 | tier: archive-candidate | origin: 2026-07-01-230246 -->

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

## User Preferences

## Team / Members

(none recorded yet)
