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
- **last_session:** 2026-08-25 | agent: Codex (2026-08-25-135145)
- **last_review:** 2026-08-25 | through 2026-08-25-135145.md
- **last_invariant_check:** 2026-08-21 | 2026-08-21-005515.md (all 15 confirmed by Eric — one-by-one walkthrough with live-tree evidence; stack-messaging-kafka wording refreshed to name the grown Kafka family; ot-reverify-invariants-20260821 closed)

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
  <!-- id: virtual-threads-rpc | created: 2026-06-20 | last_used: 2026-06-27 | uses: 4 | tier: core -->

## Key Decisions

- **Polyglot functions = Event-over-HTTP wrappers, NOT subprocesses (Eric's design; D0–D8
  ratified 2026-08-22).** python/node functions run as long-lived Event API peers speaking the
  standard envelope wire format, addressed through the declarative `yaml.event.over.http` map —
  flows and graph.task call them as if local; non-blocking on the JVM (interceptor relay, zero
  threads per in-flight call); no subprocess stability surface. **Scope fence:** wrappers =
  envelope codec + `/api/event` host + preload registry + thin PostOffice client + dev CLI +
  the minimalist utilities (config with the engines' `resources/` convention and `-Dkey=value`
  override syntax, engine-format logging, trace context) — NO orchestration. **Fence amended 2026-08-23 (Eric):** + the primitive in-process event bus (per-route
  FIFO mailboxes, faithful `instances`, deliver/publish only, NO spill tier/queue cap —
  back-pressure stays with the engines' flows/graphs) + the engines' actuator endpoints
  (/info, /info/routes, /env, /health, /livenessprobe; type=info/type=health
  health-function contract) + log.format text|json|compact.
  Engine-parity behaviors pinned: handler errors ride HTTP 200 with envelope status; transport
  errors 400/403/404/408; engine-identical messages; my_cid tag → my_correlation_id; compact
  format rejected (standard only). **One engine gap approved for fix (D5): graph.task's
  po.exists guard doesn't consult the event-over-http map** (GraphTask.java:74, Rust
  skills.rs:362) — surgical relaxation, Java+Rust lock-step. stdio-subprocess alternative
  investigated and SHELVED (niche: single-artifact embedded scripting; revisit only on field
  demand). The node wrapper is the sanctioned "fresh node.js re-port" answer. Golden envelope
  vectors are the conformance gate; interop report per wrapper release (D6). Spec:
  draft-design-specs/polyglot-script-runner.md. Delivered by [[thread-polyglot-initiative]];
  serves [[bp-polyglot-functions]].
  <!-- id: polyglot-event-over-http-design | created: 2026-08-22 | last_used: 2026-08-24 | uses: 5 | tier: active | origin: 2026-08-22-164936 -->

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
  <!-- id: compilegraph-mandatory-gate | created: 2026-07-29 | last_used: 2026-08-22 | uses: 11 | tier: active | origin: 2026-07-29-190328 -->

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
  <!-- id: conv-telemetry-presentation-parity | created: 2026-07-23 | last_used: 2026-08-24 | uses: 29 | tier: active | origin: 2026-07-23-145132 -->

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
  <!-- id: graphjs-phase-out-direction | created: 2026-08-01 | last_used: 2026-08-22 | uses: 6 | tier: active | origin: 2026-08-01-035647 -->

- **Glance at GitHub's pre-filled squash-dialog title before confirming a squash-merge
  (Eric's feedback, 2026-08-19).** GitHub pre-fills the dialog with title-plus-body text,
  and stray words can survive into the immutable commit title — PR #283's squash
  `1685842c` landed as "…cannot drop a span  Body (#283)" (a leaked "Body" + double
  space). Trim the pre-filled title to the intended one-liner on every squash; same
  review moment as the co-author-trailer dedup rule in AGENTS.md.
  Relates [[thread-otlp-export-retry]].
  <!-- id: conv-squash-title-prefill-check | created: 2026-08-19 | last_used: 2026-08-24 | uses: 5 | tier: active | origin: 2026-08-19-195244 -->
- Add capability: function (`@PreLoad` + `TypedLambdaFunction`) → flow YAML →
  register in `flows.yaml` → `rest.yaml` mapping if HTTP-facing.
  <!-- id: conv-add-capability | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->
- Watch serialization gotchas (Long↔Integer downcast; use `util.str2int/str2long`).
  <!-- id: conv-serialization-gotchas | created: 2026-06-20 | last_used: 2026-06-24 | uses: 2 | tier: core -->
## Blueprint  *(gap from Current State → Vision; `(blueprint)` threads serve `vision-mercury-composable`)*

- [ ] (blueprint) **Polyglot function execution** — python/node.js functions join Event Script
  flows and MiniGraph graphs as Event-over-HTTP peers (ratified design D0–D8, 2026-08-22);
  wrappers live in the repurposed Accenture/mercury-python + mercury-nodejs repos.
  → serves: vision-mercury-composable
  <!-- id: bp-polyglot-functions | created: 2026-08-22 | last_used: 2026-08-24 | uses: 4 | tier: working -->
- [ ] (blueprint) Integrate a **pluggable AI companion LLM backend**; mature `POST /api/companion/{id}`
  from a dev-only command pipe into a governed collaboration layer. → serves: vision-mercury-composable
  <!-- id: bp-ai-companion-llm-backend | created: 2026-06-20 | last_used: 2026-08-14 | uses: 2 | tier: working -->
- [ ] (blueprint) **Enterprise governance lifecycle** for graph models (dry-run → certify → stage →
  approve → production), so models promote to production as standard endpoints. → serves: vision-mercury-composable
  <!-- id: bp-graph-governance-lifecycle | created: 2026-06-20 | last_used: 2026-08-14 | uses: 2 | tier: working -->
## Open Threads

- [x] (release — SHIPPED/PUBLISHED 2026-08-23, both repos at v4.11.11)
  **graph.task Event-over-HTTP field release.** Java PR #293 squash `50d890bf`; Rust
  PR #213 merge `3040e8e3`; trees/tags and full gates verified. Sole content was the
  P2 graph.task guard (#292/#212), enabling deployed graphs to call polyglot hosts.
  Relates [[thread-polyglot-initiative]]; full detail: origin log.
  <!-- id: thread-release-4-11-11 | created: 2026-08-24 | last_used: 2026-08-24 | uses: 2 | tier: active | origin: 2026-08-24-001110 -->

- [ ] (feature — design RATIFIED D0–D8 2026-08-22 + two in-flight refinements (minimalist
  utilities; resources/ + -D config conventions); **both wrapper repos REBOOTED and scaffolded
  same day on `feature/polyglot-event-over-http`, and **both MERGED same day** — mercury-python
  [PR #15](https://github.com/Accenture/mercury-python/pull/15) true merge `99ae249` carrying
  `ad7e104`+`2c92a29` (30/30 tests), mercury-nodejs
  [PR #86](https://github.com/Accenture/mercury-nodejs/pull/86) true merge `05e66a5` carrying
  `0ae7a17`+`5f56d1d` (31/31; legacy npm mercury-composable v4.3.28 in history); trees verified
  identical to the gated commits, branches deleted both ends; both codecs verified against the
  shared golden envelope vectors;
  **interop proven live:** python⇄node both directions, and composable-example 4.11.10's
  shipped `event-over-http-declarative` FLOW executed the python `hello.declarative` function
  unchanged — my_correlation_id injected, engine trace_id in the python log, zero engine
  change.) **Polyglot initiative — python/node.js Event-over-HTTP wrappers.**
  **P2 DONE 2026-08-22 (both engines, same day the wrapper repos came back agent-memory-
  enabled): Java commit `10c53ca3` + Rust `83b12c36` on `feature/graph-task-event-over-http`
  branches — the graph.task guard consults the event-over-http map (Java adds
  `PostOffice.getEventHttpTarget`), unit-test-task-7 pin byte-identical on both engines and
  proven vs unfixed code, compiled-set pin 49→50; gates: minigraph 119/119 + platform-core
  426 green; Rust 63 suites + clippy 0 + fmt clean. **MERGED 2026-08-22: Java
  [PR #292](https://github.com/Accenture/mercury-composable/pull/292) squash `fad24f11` ==
  gated `a31c5316` (incl. Eric's IDE cosmetics; title clean), Rust
  [PR #212](https://github.com/Accenture/mercury/pull/212) merge `c49d6cd7` carrying
  `83b12c36`; trees verified, CI green both, branches deleted both ends — **SHIPPED to the field in v4.11.11, 2026-08-23**
  ([[thread-release-4-11-11]]).**
  **Wrapper feature round MERGED 2026-08-23** — both wrappers gained the primitive
  event bus (instances/private faithful, local PostOffice delivery, no-spill ruling),
  the actuator endpoints, log.format text|json|compact and a sample
  resources/application.yml: python
  [PR #17](https://github.com/Accenture/mercury-python/pull/17) true merge `f38ac17`,
  node [PR #87](https://github.com/Accenture/mercury-nodejs/pull/87) true merge
  `fa7b2bf`; trees verified, branches deleted both ends; detail in the wrapper repos'
  memory (logs 2026-08-23-031558 / 2026-08-23-031920).
  **P4 docs SHIPPED 2026-08-24 in two halves.** Wrapper docs sites LIVE same day
  (python [PR #20](https://github.com/Accenture/mercury-python/pull/20) merge `0bc97f7`,
  node [PR #89](https://github.com/Accenture/mercury-nodejs/pull/89) merge `5ccf355`;
  sites at accenture.github.io/mercury-python + /mercury-nodejs on the engine Material
  theme; the fresh wrapper ci.yml workflows shipped in the same PRs — maiden runs green,
  wrapper-CI item CLOSED; node repo's Pages source had to be flipped off the legacy
  doc/2025-11-15 branch). Engine half MERGED AND LIVE same day: Java
  [PR #294](https://github.com/Accenture/mercury-composable/pull/294) squash
  `807a063c` == gated `8c89b1ad` (chapter guides/polyglot-functions.md + ADR-0016
  Proposed + interop-report wrapper round; tree verified, title clean, reactor green) /
  Rust PR #214 merge `c5221258` (twin chapter + ADR-0014 + report round + **home page
  unified on the Java reference and site renamed "Composable for Rust"** per Eric's
  directive) + Rust PR #215 merge `4573bd21` (nav parity: Operate & integrate +
  Orientation tabs — tab row now identical across the two engine sites, Eric confirmed
  live). The
  "examples demo" item resolved by documentation: the wrapper demo apps register
  hello.declarative and default to port 8085 (lambda-example/hello-world's slot), so
  the shipped zero-code demo swaps in a wrapper callee with one -D override — taught in
  the chapter, no new example code. CI fix round: both engines' ai-contract-provider
  link validators failed closed on the new cross-link until
  `references/guides/polyglot-functions.md` joined both files.list inventories (Java
  `8c89b1ad`, Rust `d278d8ae`) — new guide pages linked from packaged references must
  join the snapshot inventory on BOTH engines.
  **P4 COMPLETE across all four repos.** Remaining: P5 publishing gates only
  (npm version strategy vs legacy 4.3.28; PyPI name availability — Eric's calls);
  optional extras offered: a live Rust-engine→wrapper drive; Rust layer-tab label
  parity ("Event Script" vs "Composable"). Design record: [[polyglot-event-over-http-design]]; serves
  [[bp-polyglot-functions]]. Full detail: origin log + 2026-08-24-170545.
  <!-- id: thread-polyglot-initiative | created: 2026-08-22 | last_used: 2026-08-24 | uses: 5 | tier: working | origin: 2026-08-22-164936 -->

- [x] (release — SHIPPED/PUBLISHED 2026-08-21, both repos at v4.11.10)
  **AI discovery release.** Java PR #291 squash `5cb65f04`; Rust PR #211 merge
  `b77f17e8`; trees/tags and full gates verified. Contents: ai-contract-provider
  (ADR-0015), f:setConfig, two OTLP span-loss fixes, and flow-binding docs fix.
  Full detail: origin log.
  <!-- id: thread-release-4-11-10 | created: 2026-08-22 | last_used: 2026-08-22 | uses: 1 | tier: active | origin: 2026-08-22-032106 -->

- [ ] (onboarding — assessment round 2026-08-21, Eric ratified; **fork MERGED 2026-08-22 as
  [PR #290](https://github.com/Accenture/mercury-composable/pull/290) squash `f85aa02a`, tree
  == gated `14ae11f5`, CI green 7m50s, branches deleted**; **handoff DELIVERED and shipped
  upstream as agent-memory v4.38.0, 2026-08-22** — the fork is now a sanctioned converged
  form (`fork-ok`, this repo's root pinned verbatim in the tool's test suites), the target
  protocol carries the consumers-exit-here step-0 + the 3–6-line close-record spec + the
  optional checkpoint, and `[closed-thread-bloat]`/`closed_narrative_max_lines` measure the
  over-retention class; this repo upgraded same day)
  **Onboarding efficiency: root AGENTS.md now forks contributors (memory protocol) from
  consumers (system/AGENTS.md) with the ratified role-resolution ladder — a fresh
  interactive agent ASKS one question whenever the first instruction doesn't itself
  resolve the path ("the answer creates your path", Eric); memory-carrying sessions
  never re-ask; headless defaults contributor.
  instructions.md gained terse-close-record + ready-to-work conventions.** Verdict: consumer side NOT over-engineered; contributor side over-retained
  (continuity was 64% closed-thread narrative — hence the new conventions). Upstream proposals
  (shim role-routing, close-record economy, readiness checkpoint, P3 declined as artifact):
  `~/Desktop/agent-memory-onboarding-handoff.md`. Remaining — next review: condense the
  `[x]` backlog to stubs (the new advisory measures it); re-run the fresh-agent
  context-efficiency exercise after two reviews vs the 64% baseline; then consider lowering
  continuity_max_lines 1000→~600. Full detail: origin log.
  <!-- id: thread-onboarding-efficiency | created: 2026-08-22 | last_used: 2026-08-22 | uses: 2 | tier: working | origin: 2026-08-22-003007 -->

- [x] (feature — MERGED on both engines 2026-08-21) **Event Script `f:setConfig` runtime
  config override plugin.** Java PR #289 squash `b5aeaf56`; Rust PR #208 merge
  `9a7b3a47`; trees and CI verified. Rulings: stringify any value, blank key returns
  false, and portable name is `setConfig`; byte-identical flow fixtures pin parity.
  First built-in plugin with a global side effect; full detail: origin log.
  <!-- id: thread-config-plugin | created: 2026-08-21 | last_used: 2026-08-22 | uses: 2 | tier: active | origin: 2026-08-21-231215 -->

- [x] (fix — MERGED 2026-08-21, PR #287 squash `fcba13ce`) **ObjectStreamTest expiry
  tests made deterministic.** Replaced the sleep-margin race with immediately attached
  consumers and never-delivering publishers, so the per-publisher timer is the sole outcome.
  Mutation-proven; platform-core 426/426 and CI green. Follows the S2925 precedent;
  full mechanism and evidence: origin log.
  <!-- id: thread-objectstream-expiry-test-determinism | created: 2026-08-21 | last_used: 2026-08-21 | uses: 2 | tier: archive-candidate | origin: 2026-08-21-195149 -->

- [x] (feature — MERGED 2026-08-21) **AI discovery standalone app and docs correction.**
  Java PRs #285/#286 (ADR-0015) plus Sonar follow-up #288; scoped `system/AGENTS.md`
  routes consumers to `GET :8999/api/discovery` / `--export`. Nested-jar resource access,
  canonical RoutingEntry fixture, catalog anchors, and export/served snapshot parity were
  live-verified; Rust port followed in PR #209. Full design and evidence: origin log.
  <!-- id: thread-ai-contract-provider | created: 2026-08-21 | last_used: 2026-08-22 | uses: 5 | tier: active | origin: 2026-08-21-173902 -->

- [x] **Invariant re-verification CLOSED 2026-08-21:** Eric confirmed all 15 core facts
  and the Vision one by one against the live tree. `stack-messaging-kafka` wording was
  refreshed to name the grown Kafka family; no invariant was superseded. The full
  evidence walkthrough lives in the origin log.
  <!-- id: ot-reverify-invariants-20260821 | created: 2026-08-21 | last_used: 2026-08-22 | uses: 2 | tier: archive-candidate | origin: 2026-08-21-005515 -->

- [x] (fix — MERGED 2026-08-19, PR #283 squash `1685842c`) **OTLP forwarding now
  retries every IOException and uses its own queued two-thread executor.** This fixes both
  stale pooled-connection span drops and shared-dispatcher rejection; failure causes are logged.
  Killed-connection, saturation, and executor-lifecycle pins were mutation-proven; module
  gates and CI passed. Full diagnosis: origin log.
  <!-- id: thread-otlp-export-retry | created: 2026-08-19 | last_used: 2026-08-22 | uses: 3 | tier: active | origin: 2026-08-19-184142 -->

- [x] (release — SHIPPED/PUBLISHED 2026-08-11, both repos at v4.11.9) **Dry-run graph
  identity simplification.** Java PR #281 squash `eff46c5f`; Rust PR #207 merge
  `27fa527e`; trees/tags and gates verified. A live field-cut drive covered both the
  named tutorial workflow and nameless `graph:untitled:{cid}` suspend/resume path.
  Full detail: origin log.
  <!-- id: thread-release-4-11-9 | created: 2026-08-11 | last_used: 2026-08-19 | uses: 2 | tier: archive-candidate | origin: 2026-08-11-220600 -->

- [x] (release — SHIPPED 2026-08-09, both repos at v4.11.5) **graph.task parity and
  teaching surfaces.** Java PR #270 squash `9a6a9569`; Rust PR #199 merge `4380e29d`;
  docs-inclusive tags and gates verified. Included graph.task staging, tutorial-13's
  HTTP-client-by-configuration lesson, and checkpoint/decision docs reframing.
  Full detail: origin log.
  <!-- id: thread-release-4-11-5 | created: 2026-08-09 | last_used: 2026-08-09 | uses: 1 | tier: working | origin: 2026-08-09-164000 -->

- [x] (release — SHIPPED 2026-08-07, Java-only v4.11.3) **Field support roll-up.**
  PR #259 squash `6a45867c`; tag and CI verified. Consolidated Kafka consumer resilience,
  the third Sonar round, partition metadata, and kafka-demo piped-mode fix; full reactor
  plus live README regression passed. Field rescan remains tracked separately.
  Full detail: origin log.
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
  <!-- id: thread-sonar-4-11-x-field-round-3 | created: 2026-08-07 | last_used: 2026-08-21 | uses: 5 | tier: working | origin: 2026-08-07-003746 -->

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

- [x] (release — CLOSED 2026-07-23) **v4.10.2 shipped in lock-step.** Java PR #222
  tag `61ddb772`; Rust PR #172 tag `6a39bccc`. Included metadata contract,
  temporary.inbox alignment, and collection plugins. Durable lesson: dereference-verify
  the target commit before pushing a release tag. Full detail: origin log.
  <!-- id: thread-release-4-10-2 | created: 2026-07-23 | last_used: 2026-07-23 | uses: 1 | tier: working | origin: 2026-07-23-211728 -->

- [x] (release — CLOSED 2026-07-23) **v4.10.1 shipped in lock-step.** Telemetry
  presentation parity patch; Java PR #218 tag `9ae666df`, Rust PR #170 tag `2c4e4066`.
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
  <!-- id: thread-docs-improvement-backlog | created: 2026-06-24 | last_used: 2026-08-24 | uses: 10 | tier: working -->
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

- **Release rhythm (Eric; established by 2026-07-25):** Claude Code prepares every release
  artifact — branch, version sweep, build verification, CHANGELOG, release notes — but
  never merges, tags, or publishes without Eric's explicit go-ahead for that specific
  step; PR-open and tag/publish are each individually gated. (Materialized into shared
  memory 2026-08-22: the smoke test found six session logs referencing this id while the
  fact lived only in an agent's personal store — a project-relevant working rhythm belongs
  in the shared layer.)
  <!-- id: eric-release-rhythm | created: 2026-08-22 | last_used: 2026-08-24 | uses: 11 | tier: active | origin: 2026-08-22-180334 -->

## Team / Members

(none recorded yet)
