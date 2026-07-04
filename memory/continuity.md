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
- **last_session:** 2026-07-04 | agent: Claude Code (2026-07-04-224922)
- **last_review:** 2026-07-02 | through 2026-07-02-161433.md
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

- **Tracing & correlation-id cleanup (2026-07-03, branch `fix/traceid-correlationid-propagation`, uncommitted).**
  Implemented Eric's `~/Desktop/tracing-and-correlation.md` brief. **TraceId:** retired `trace.http.header` +
  `trace.http.legacy.header.enabled` entirely — trace ID travels only as hardcoded `X-Trace-Id` + W3C
  `traceparent`; removed the legacy `X-Correlation-Id, X-Trace-Id` config from 7 application.properties;
  `EventEmitter` event-over-HTTP relay now also emits traceparent. **No echo-back:** deleted the single
  `HttpRouter` line that echoed the trace header onto the HTTP response (+ flipped the 2 RestEndpointTest
  assertions). **Correlation-id end-to-end:** new configurable `http.correlation.id.header`
  (default `X-Correlation-Id`) + `kafka.correlation.id.header` (default `cid`); captured at the edge (fresh
  dash-less `util.getUuid()` if absent), preserved as `model.cid`, exposed to every function task as read-only
  reserved header `my_correlation_id` (`PostOffice.getMyCorrelationId()`), and stamped outbound by
  `simple.kafka.notification`; `KafkaFlowConsumer` reads the configurable inbound header with fresh-UUID
  fallback (was traceId). Reserved `my_*` headers no longer leak to Kafka. **Feature-complete propagation to
  any touch point:** `PostOffice.touch()` stamps `my_correlation_id` on every outgoing event (single choke
  point for send/RPC/broadcast/event-over-HTTP, mirroring trace-context propagation), so the business cid
  follows the whole call graph incl. the cross-instance event-over-HTTP hop (peer reads via
  `getMyCorrelationId()`); `AsyncHttpClient` emits `http.correlation.id.header` downstream. Diagnostic finding:
  `EventEnvelope.cid` is an RPC reply-routing field, not surfaced to fire-and-forget targets — business cid
  rides the `my_correlation_id` header, not the routing cid. **Plain REST-to-function endpoints** (no flow)
  are covered too: `HttpRequestEvent.correlationId` (set at edge; in toMap/fromMap) + `HttpRouter` stamps
  `my_correlation_id` on the primary/secondary/post-auth events, so `getMyCorrelationId()` works for any HTTP
  entry (self-originated chains — scheduler/startup — seed their own if needed). Full reactor (28) + pg-example (6) green; tests:
  `PostOfficeTest#reservedHeadersExposedToFunction`, `FlowTests#correlationIdPropagationTest`,
  `EventHttpTest#eventOverHttpPropagatesTraceAndCorrelationId`,
  `RestEndpointTest#asyncHttpClientForwardsCorrelationIdDownstream`,
  `RestEndpointTest#plainRestFunctionReceivesCorrelationId`.
  Plan: `~/.claude/plans/glowing-tinkering-glacier.md`. See [[flow-cid-vs-business-correlation-decoupled]] and
  [[thread-traceid-correlation-propagation]]. **Status: implemented + feature-complete, all tests green, uncommitted — pending Eric.**
  <!-- id: tracing-correlation-cleanup | created: 2026-07-03 | last_used: 2026-07-03 | uses: 1 | tier: working | origin: 2026-07-03-014759 -->

- **External assessment of the shipped trace/cid feature (PR #132): one real fix kept, one "finding" rejected
  by design (2026-07-04, uncommitted).** GitHub Copilot's report
  (`/tmp/traceid-business-correlationid-propagation-assessment.md`) reviewed `main` and raised two
  "high-confidence" gaps; verified both against source. **(1) KEPT — `my_correlation_id` was overridable despite
  being documented read-only:** `TaskExecutor` stamped the reserved header *before* applying `md.optionalHeaders`,
  so a flow relaying inbound HTTP headers (`input.header -> header`) let a caller spoof the value a task reads via
  `getMyCorrelationId()` → fix: stamp `my_correlation_id` **last**, after optional headers, so the framework value
  wins. Regression test proven to fail pre-fix (`FlowTests#correlationIdPropagationTest` extended + `Greetings`
  echoes `getMyCorrelationId()`). **(2) REJECTED — "AsyncHttpClient leaks caller-supplied `traceparent`":** first
  fixed (added `traceparent` to `HEADERS_TO_IGNORE`) then **reverted on Eric's guidance** — see the design
  principle [[developer-set-trace-headers-are-intentional]]. In production the "hijack" cannot occur (a traced
  request always stamps a fresh W3C traceparent that overwrites; Mercury ids are always W3C-compatible), and when
  untraced, forwarding a developer-set `traceparent`/`X-Trace-Id` is the *intended* behavior (propagating a trace
  to a 3rd party / forwarding upstream). The report's leak only reproduces with non-W3C ids (a test artifact).
  AsyncHttpClient net change = a documenting comment only; replaced the negative test with a positive guard
  `RestEndpointTest#asyncHttpClientForwardsDeveloperSuppliedTraceparent`. Finding-1 defense-in-depth
  (strip `my_route`/`my_trace_id`/`my_trace_path`) assessed + deferred: `WorkerHandler` L207-214 already makes
  `my_route` authoritative and overwrites `my_trace_id`/`my_trace_path` whenever traced (inert-only when spoofable).
  Full suites green: platform-core 372, event-script-engine 132. Refines [[tracing-correlation-cleanup]]; see
  [[thread-traceid-correlation-propagation]]. **Status: shipped as PR [#134](https://github.com/Accenture/mercury-composable/pull/134)**
  (branch `fix/correlation-id-readonly-and-trace-header-docs`; 2 commits — `3065f804` source+tests+docs,
  `707056e5` memory) — pending review/merge.
  <!-- id: traceid-cid-propagation-edgecase-fixes | created: 2026-07-04 | last_used: 2026-07-04 | uses: 1 | tier: working | origin: 2026-07-04-223237 -->

- **Design principle — a developer-set outbound trace header (`X-Trace-Id` / W3C `traceparent`) is an
  intentional act the framework must honor, not strip (Eric, 2026-07-04).** `AsyncHttpClient` deliberately does
  **not** filter caller-supplied trace headers. Two legitimate uses: (a) when the call is **untraced**
  (`tracing: false`, no current context), an app may explicitly set `traceparent`/`X-Trace-Id` to hand a trace
  context to a downstream/3rd-party system — this passes through unchanged; (b) when the call **is traced**, the
  framework's own current trace context takes precedence (stamps `X-Trace-Id` = current traceId and `traceparent`
  = current traceId+spanId at `AsyncHttpClient.updateHttpHeaders`), so the downstream span chains to *this*
  caller's span — and because a traced request's context is itself adopted from the upstream `X-Trace-Id`/
  `traceparent` at ingress (`HttpRouter.getTraceId` + `W3cTrace.parse`, L485-495), the upstream trace still
  propagates automatically. Net: framework overwrites only when it has an active trace of its own; otherwise the
  developer's explicit header wins. This is *why* the assessment's "traceparent leak" finding was rejected (the
  only leak case needs non-W3C ids, a test artifact). Contrast: `my_correlation_id` is a reserved *internal*
  header (surfaced via `getMyCorrelationId()`, not a wire header) and **is** protected as read-only
  ([[traceid-cid-propagation-edgecase-fixes]] finding 1); the business cid goes downstream as the configurable
  `http.correlation.id.header` (e.g. `X-Correlation-Id`), which AsyncHttpClient also honors-if-caller-set.
  **Testability corollary (Eric):** because an *untraced* invocation (the default in a plain unit test — no
  trace context set) passes every non-hop-by-hop header through verbatim, a unit test can exercise an external
  /downstream REST endpoint with full control over the request headers (incl. trace headers), no framework
  interference — guarded by `RestEndpointTest#asyncHttpClientForwardsDeveloperSuppliedTraceparent`.
  **Documented (2026-07-04):** "let the framework manage trace headers; don't set `X-Trace-Id`/`traceparent`
  yourself (with the untraced escape hatch)" callouts added to `docs/guides/observability.md` (§W3C),
  `reserved-names-and-headers.md` (§Trace ID), and `actuators-and-http-client.md` (§AsyncHttpClient).
  <!-- id: developer-set-trace-headers-are-intentional | created: 2026-07-04 | last_used: 2026-07-04 | uses: 1 | tier: working | origin: 2026-07-04-224922 -->

- **A flow's reply cid and its business correlation-id are distinct and must stay decoupled (2026-07-03).**
  `FlowInstance.cid` is the **reply-routing** key: REST automation awaits an HTTP response whose correlationId
  equals the internal per-request UUID (`requestId`), matched via HttpRouter's `contexts` map — so the flow's
  reply cid must equal that requestId. The **business** correlation-id (upstream `X-Correlation-Id`, or fresh)
  is a separate concern surfaced as `model.cid`. Conflating them (setting the flow cid = business cid) breaks
  HTTP reply matching → every HTTP flow times out (discovered when 65/70 FlowTests failed). Design: added
  `FlowInstance.correlationId` (business → `model.cid` + the `my_correlation_id` reserved header) alongside
  `FlowInstance.cid` (routing, unchanged); the business cid rides the launch-event header
  `EventScriptManager.CORRELATION_ID` ("correlation_id"), set by `FlowExecutor` (new 6-arg `launch`,
  businessCid defaults to routing cid for non-HTTP callers) and by `HttpToFlow` (routing = requestId,
  business = X-Correlation-Id); subflows inherit the parent's business cid via the same header. Kafka: the two
  coincide (no requestId indirection). Relates [[trace-thread-keyed-mono-gotcha]].
  <!-- id: flow-cid-vs-business-correlation-decoupled | created: 2026-07-03 | last_used: 2026-07-03 | uses: 1 | tier: working | origin: 2026-07-03-014759 -->

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
  <!-- id: application-log-context | created: 2026-06-30 | last_used: 2026-06-30 | uses: 1 | tier: archive-candidate | origin: 2026-06-30-212955 -->

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
  = register API); mock got a README. **CSFLE is now wired** (see [[kafka-csfle-delegation]]; uncommitted as
  of 2026-07-02, [[thread-csfle-field-encryption]]).
  Validated e2e (JSON/Avro/Protobuf) incl. continuous OTel span propagation across both Kafka hops. Design spec:
  `draft-design-specs/kafka_schemaid_from_subject_version_design.md` (gitignored). Supersedes the id-driven
  detail in [[minimalist-kafka-schema-registry]].
  <!-- id: kafka-schemaid-from-subject-version | created: 2026-07-01 | last_used: 2026-07-01 | uses: 1 | tier: archive-candidate | origin: 2026-07-01-004724 -->

- **CSFLE (Client-Side Field Level Encryption) for minimalist-kafka — pure delegation, no framework rule/schema
  code (2026-07-02, branch `feature/kafka-csfle-field-encryption`, uncommitted).** Eric's field observation
  ("real apps just delegate CSFLE to the Confluent serdes via config, no framework code") replaced an earlier
  in-session plan (attach the ruleSet ourselves via `resolve()`/`copy(ruleSet)`). Decompiling Confluent
  8.2.0/8.3.0 confirmed delegation is sufficient: `executeRules(WRITE)` fires in the `use.schema.id` branch
  minimalist-kafka already uses, and `getSchemaById` (our existing lookup) already returns a `ParsedSchema`
  with its `ruleSet` populated (from `SchemaString.getRuleSet()`) — so **zero changes to `resolve()`,
  `ResolvedSchema`, or `SchemaStore`**. CSFLE reduces to: add the encryption executor + **one** KMS driver jar
  (AWS uncommented/default in `pom.xml`; Azure/GCP dependency blocks present but commented out — a field
  installation configures exactly one vendor), and thread operator config into the serde. New
  `SchemaCodec.extractSerdeConfig(ConfigBase)` extracts the generic `schema.registry.serde.*` prefix
  (stripped) and merges it into **both** `JsonSchemaSerde`/`AvroSchemaSerde`'s serializer and deserializer
  config maps; absent config ⇒ unchanged plaintext behavior (regression-tested).
  **Per-subject binding, verified not assumed:** Eric flagged that CSFLE config must bind per-subject in field
  installations; decompiling `FieldEncryptionExecutorTransform.getKekName` confirmed `encrypt.kek.name`/
  `encrypt.kms.type`/`encrypt.kms.key.id` resolve strictly from the **registered rule's own params** (or
  schema `Metadata`) — never from the executor-level config map — so this was **already correct** in the
  design; the mistake was the guide's example and both tests redundantly duplicating those 3 keys into the
  serde-level config, implying a false global KEK. Corrected in the guide (rule params = per-subject/
  schema-owned; `schema.registry.serde.*` = the KMS **driver's own** credentials only, e.g. AWS's
  `access.key.id`/`secret.access.key`) and in `SchemaCodecCsfleConfigTest`.
  **Local verification without HTTP mocking:** `FieldEncryptionExecutor` needs Confluent's DEK-registry
  (`DekRegistryClient.getOrCreateKek`), which neither `EmbeddedSchemaRegistry` nor the standalone mock
  implement — but `EncryptionExecutor.configure()` builds that client from the *same* `schema.registry.url`
  value, and `DekRegistryClientFactory` honors the identical `mock://<scope>` test convention as
  `SchemaRegistryClientFactory` (both Confluent test utilities). So a single `mock://<scope>` URL backs both
  clients — new tests `CsfleLocalRoundTripTest` (raw Confluent API spike) and `SchemaCodecCsfleConfigTest` (4
  tests through the real `SchemaCodec.Encoder`/`Decoder`, via the package-private constructor since
  `fromConfig`'s `ManagedCacheSchemaRegistryClient` opens a real `RestService` and does **not** honor
  `mock://`) both run with zero HTTP, zero cloud KMS, zero changes to `EmbeddedSchemaRegistry`/the mock.
  **Wire/consumer unchanged** — only the tagged field *values* become ciphertext. Guide docs:
  `docs/guides/kafka-flow-adapter.md` §"CSFLE". Design spec (v3.0, gitignored):
  `draft-design-specs/kafka_csfle_field_encryption_design.md` — §10/§11 record exactly what shipped.
  **Status: implemented, all tests green, uncommitted — pending Eric's review.** See [[thread-csfle-field-encryption]].
  <!-- id: kafka-csfle-delegation | created: 2026-07-02 | last_used: 2026-07-02 | uses: 1 | tier: working | origin: 2026-07-02-020429 -->

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
  <!-- id: snyk-oss-dependency-update-2026-07 | created: 2026-07-01 | last_used: 2026-07-02 | uses: 2 | tier: active | origin: 2026-07-01-215533 -->

- **Dependabot alert #28 (jackson-databind `@JsonIgnoreProperties` case-insensitive bypass, CWE-915) assessed
  NOT APPLICABLE (2026-07-02).** The same jackson-databind CVE tracked as "no fix yet in the 2.x line" in
  [[snyk-oss-dependency-update-2026-07]], now surfaced as GitHub alert #28 on `helpers/kafka-standalone/pom.xml`
  (pins 2.22.0; affected >= 2.22.0, < 2.22.1). **Verified, not assumed:** the exploit requires BOTH
  case-insensitive property matching (`ACCEPT_CASE_INSENSITIVE_PROPERTIES` via `MapperFeature` or per-property
  `@JsonFormat`) AND reliance on per-property `@JsonIgnoreProperties` to keep a field unwritable — the repo's
  Java sources contain **zero occurrences of either**, and `helpers/kafka-standalone` never constructs an
  `ObjectMapper` at all (its databind pin only satisfies Kafka's transitive dependency). **No bump possible yet:**
  Maven Central's latest is still 2.22.0 — the fixing 2.22.1 is unreleased ("Patched version: None"; only 3.x
  shipped its fix, 3.1.4 on 2026-06-04). Recommended to Eric: dismiss alert #28 as "vulnerable code is not in
  use"; bump when 2.22.1 lands — Dependabot re-flags on release (that's the reminder), and the version is
  centralized (`jackson-2-bom.version` in ~30 poms + 3 explicit `jackson-databind` pins: platform-core,
  rest-spring-3, kafka-standalone). Do not re-investigate this alert while the pinned version is 2.22.0 and
  those two grep checks stay empty.
  <!-- id: jackson-databind-alert28-not-applicable | created: 2026-07-02 | last_used: 2026-07-02 | uses: 1 | tier: working | origin: 2026-07-02-055423 -->

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
  their defaults). (A `ProtobufSchemaSerde` existed briefly but was **removed 2026-07-01, pre-release** — see
  [[minimalist-kafka-protobuf-removed]] — `SchemaType.PROTOBUF` is still recognized so dispatch fails clearly.)
  The codec dispatches by the `schema-type` header on produce / the registered
  type on consume; producer + consumer are type-generic. **Thread-safety (2026-06-30):** the Confluent serdes
  are NOT thread-safe, so `SchemaCodec` is a **factory** — `newEncoder()`/`newDecoder()` mint owner-confined
  serde sets (serializers per producer worker-`instance`, deserializers per consumer), and
  `simple.kafka.notification` is `@KernelThreadRunner` (the serdes use `synchronized` → would pin a VT carrier).
  **Gotcha (fat jar only):** Confluent serdes resolve config classes (the default `context.name.strategy` →
  `NullContextNameStrategy`) via the **thread context classloader**; building/using them off the app-classloader
  thread (e.g. the `@KernelThreadRunner` ForkJoinPool worker) throws `ConfigException: ...could not be found`.
  Fix: `SchemaCodec.withSerdeClassLoader(...)` pins `AbstractKafkaSchemaSerDeConfig`'s classloader as the TCCL
  around build + serialize/decode. Flat-classpath unit tests can't reproduce it.
  **JSON + Avro serdes done, tested, demoed end-to-end, and documented** (`EmbeddedSchemaRegistry` test
  helper + sync-over-async-demo `json/avro-topic-1/2` paths, all manually validated via continuous-trace
  telemetry; kafka-flow-adapter guide has the schema section) — **Protobuf was also completed at this point
  but later removed pre-release, see [[minimalist-kafka-protobuf-removed]]**. Shipped alongside Kafka-flow-adapter hardening: a flow's own `ttl` is the deadline (no
  `kafka.flow.timeout.ms`); success = HTTP < 400 (2xx/3xx), else retry→DLQ; a failed DLQ write
  drops-with-ERROR + commits (no recovery storm) bounded by `kafka.dlq.timeout.ms`. Builds on
  [[standalone-schema-registry-mock]].
  **Optimization iteration (2026-06-30, PR #127):** swapped the file cache → in-memory `ManagedCache` (above),
  shrank the `simple.kafka.notification` pool 10→5 (kernel-thread frugality, `@KernelThreadRunner`), and per-id'd
  the mock store ([[standalone-schema-registry-mock]]). **Forensic finding: no client-side negative caching
  exists** (decompiled Confluent 8.2.0 — the id→schema path `getSchemaById`→`getSchemaBySubjectAndId` caches
  positives only; `missingIdCache` is producer-side getId-by-content, default TTL 0=off); an earlier "not found
  persists" symptom was the mock loading `schemas.json` only at boot, now fixed by its on-demand per-id store.
  <!-- id: minimalist-kafka-schema-registry | created: 2026-06-29 | last_used: 2026-07-01 | uses: 11 | tier: active | origin: 2026-06-29-010147 -->

- **Protobuf support removed from `minimalist-kafka`'s Schema Registry integration, pre-release (2026-07-01).**
  Snyk's pipeline gate (a hard must-pass — no exemption path available) flagged `com.squareup.wire:wire-runtime-jvm:5.5.0`
  (pulled transitively via `io.confluent:kafka-protobuf-serializer` → `kafka-protobuf-provider`) for
  CVE-2026-45799 / GHSA-7xpr-hc2w-34m9: `skipGroup()` doesn't reject a negative varint length, so a crafted
  **10-byte payload crashes any Wire-decoding consumer** (uncaught `ArrayIndexOutOfBoundsException` escapes
  the documented `IOException` boundary — a DoS, not RCE). **No fix exists or ever will for this coordinate** —
  confirmed via the live GitHub advisory: `wire-runtime-jvm` is discontinued; the fix ships only under the
  renamed `com.squareup.wire:wire-runtime` (6.3.0+), a different Maven coordinate Confluent has not adopted
  (checked directly against `kafka-protobuf-provider:8.3.0`'s own POM — `wire.version` is still pinned to
  `5.5.0`, unchanged from 8.2.0). Excluding `wire-runtime-jvm` and forcing the renamed `wire-runtime` ourselves
  was considered and rejected — an unverified major-version (5→6) substitution into a dependency graph we
  don't control is too risky to gamble on a hard release gate. **Decision:** remove Protobuf entirely
  (`ProtobufSchemaSerde`/`ProtobufConversions` deleted, `kafka-protobuf-serializer` dependency dropped,
  `SchemaType.PROTOBUF` kept as a recognized-but-unwired enum value so `SchemaCodec`'s dispatcher still fails
  clearly via `UnsupportedOperationException`, never silently/NPE). Backed by an industry check (WebSearch,
  no hard % found but strong qualitative consensus across 2025–2026 Kafka-ecosystem sources): Avro dominates
  Confluent-Schema-Registry-backed Kafka specifically, Protobuf's stronghold is gRPC/polyglot microservices —
  a different ecosystem — so JSON+Avro covers the dominant case here. **Zero backward-compat cost**: per
  CHANGELOG.md, `minimalist-kafka` (and its Schema Registry integration) was introduced *in* 4.5.0 — the
  exact version blocked at this Snyk gate — so no client has ever received a published artifact with Protobuf
  support. Removed end-to-end: `system/minimalist-kafka` code + tests + pom;
  `examples/sync-over-async-demo`'s parallel protobuf-topic-1/2 demo path (Java tasks, flows, rest.yaml entry,
  kafka-adapter bindings, registry seed, Node topic script); user-facing docs
  (`docs/guides/kafka-flow-adapter.md`, `configuration-reference.md`, `schema-registry-mock.md`, the demo's own
  README, `helpers/schema-registry-standalone/README.md`) all state the rationale + limitation explicitly
  rather than silently dropping it — an explicit honesty/objectivity call from Eric. **Not a dead end:**
  tracked as a backlog item — [[thread-minimalist-kafka-protobuf-revival]] — to re-wire once Confluent adopts
  `wire-runtime`, or sooner for a specific field installation that explicitly needs Protobuf and accepts the
  residual risk. Full reactor `mvn test` green after removal (see [[thread-minimalist-kafka-protobuf-removal]]).
  Supersedes the Protobuf-inclusive claims in [[minimalist-kafka-schema-registry]] (its JSON/Avro
  architecture detail is otherwise unchanged and still current).
  <!-- id: minimalist-kafka-protobuf-removed | created: 2026-07-01 | last_used: 2026-07-01 | uses: 2 | tier: active | origin: 2026-07-01-224313 -->

- **`minimalist-kafka`: `confluent.version` bumped 8.2.0 → 8.3.0; `kafka.version` (4.2.0) upgrade deliberately
  deferred (2026-07-01).** After the Protobuf removal above, `confluent.version` only governs
  `kafka-json-schema-serializer`/`kafka-avro-serializer` (no `kafka-protobuf-serializer` anymore). Verified
  before bumping, not assumed: resolved dependency-tree diff is clean (Confluent's own artifacts + a
  `swagger-annotations`→`swagger-annotations-jakarta` swap + two split-out artifacts `kafka-avro-types`/
  `kafka-schema-types` + minor patch bumps), zero OSV vulnerabilities in anything new/changed, full test
  suite green (49/49 in `minimalist-kafka` incl. the stock-Confluent-serializer interop tests). **Deferred:**
  Confluent's own compatibility table pairs Platform 8.3.x with Apache Kafka **4.3.x**, but this repo
  deliberately does not follow that pairing — `kafka.version` is pinned independently (excluding Confluent's
  transitively-suggested `kafka-clients`) specifically so the Confluent serializers (which implement the
  stable `Serializer`/`Deserializer` contract, tolerant of client-version drift) never force a broker/client
  version choice here. Bumping `kafka.version` to 4.3.x is a **separate, deliberately deferred** decision:
  no CVE drives it (checked: 0 vulns at both 4.2.0 and 4.3.1), but the blast radius is far wider — it's
  pinned in **24 pom.xml files**, including the embedded KRaft broker used across many modules' integration
  tests, a materially different risk category (broker behavioral change) from a pure serializer-library bump.
  Tracked as its own backlog item, not blocked on anything — see [[thread-kafka-client-version-upgrade]].
  <!-- id: minimalist-kafka-confluent-8-3-0 | created: 2026-07-01 | last_used: 2026-07-01 | uses: 1 | tier: active | origin: 2026-07-01-230246 -->

- **Code review closeout for PR #130 added deprecation-conversion visibility + real lifecycle docs
  (2026-07-02).** Eric's IDE-driven review of the simple-type-matching deprecation work
  ([[thread-deprecate-simple-type-matching]]) surfaced two durable additions beyond small cleanups
  (unused import, constant-reuse, a missing loop `break`, inlining a trimmed-to-one-line private method —
  `MultiLevelMap.removeElement(String)` is the real API, not `.remove()`): (1) **both `CompileFlows` and
  `CompileGraph` now log `WARN` whenever they silently auto-convert a deprecated `model.key:type` entry** —
  `"Deprecated {input|output} syntax in task {} of {} - '{}' converted to '{}'"` in `CompileFlows` (two call
  sites, qualified input/output since they'd otherwise produce identical, ambiguous lines) and
  `"Deprecated syntax in graph {} node[{}].{} - '{}' converted to '{}'"` in `CompileGraph` (one call site,
  covers `mapping`/`input`/`output`/`for_each` generically) — so silent conversion became visible
  conversion, closing a real gap Eric noticed (previously "CompileFlows silently converts... It would be
  better to advise the user"). (2) **New "Application Lifecycle" section in `docs/guides/architecture.md`**
  documenting the real `@BeforeApplication` → `@PreLoad` → HTTP-server-startup → `@MainApplication`
  sequence, traced from source (`AutoStart.java` → `AppStarter.java` → rest-spring's `AppLoader.java`) —
  prompted by Eric noticing the sequence-number rationale was "hidden in code and comments, not in user
  facing documentation." Along the way, found and fixed a real doc bug: `annotations-reference.md` claimed
  `sequence=2` was reserved for the Event Script engine; the actual source uses `sequence=5`
  (`CompileFlows`) with `CompileGraph` at `6` right after it (confirmed deliberate — reuses the same
  `SimpleTypeMatchingConverter`, and keeps flow-then-graph startup ordering deterministic). Also corrected
  `@MainApplication`'s Spring Boot note from vague ("after the HTTP server completes startup") to precise
  (deferred until Spring's `ApplicationReadyEvent`, both `rest-spring-3`/`rest-spring-4`). All fixes verified
  with a full module test run immediately after each edit — event-script-engine 115 tests, minigraph-
  playground-engine 55 tests, green throughout; docs changes needed no test run.
  <!-- id: event-script-minigraph-code-review-2026-07 | created: 2026-07-02 | last_used: 2026-07-02 | uses: 1 | tier: working | origin: 2026-07-02-004606 -->

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

- [ ] (implemented, **uncommitted** — Claude Code, 2026-07-03) **TraceId & correlation-id propagation cleanup.**
  Branch `fix/traceid-correlationid-propagation` (was empty; now ~36 files changed, nothing committed). Full
  detail in the Key Decision [[tracing-correlation-cleanup]] + the decoupling insight
  [[flow-cid-vs-business-correlation-decoupled]] and the 2026-07-03-014759 session log. Retired
  `trace.http.header`; no trace echo-back; configurable `http.correlation.id.header`/`kafka.correlation.id.header`;
  business cid → `model.cid` → `my_correlation_id` → `simple.kafka.notification`. Full reactor + pg-example +
  4 new tests green. **Feature-complete:** business cid propagates to any touch point via `PostOffice.touch()`
  (incl. event-over-HTTP) + `AsyncHttpClient` downstream + `simple.kafka.notification`. Full reactor (28) +
  pg-example (6) green. **Next: Eric's code review, then commit + PR** (same flow as #128/#129).
  **Update (2026-07-04):** the feature shipped as PR #132 (`1ab78077` on `main`); an external assessment then
  reviewed it — one real fix (`my_correlation_id` read-only) + trace-header docs shipped as
  PR [#134](https://github.com/Accenture/mercury-composable/pull/134) (the "traceparent leak" finding was
  rejected by design) — see [[traceid-cid-propagation-edgecase-fixes]] and
  [[developer-set-trace-headers-are-intentional]].
  <!-- id: thread-traceid-correlation-propagation | created: 2026-07-03 | last_used: 2026-07-03 | uses: 1 | tier: working | origin: 2026-07-03-014759 -->

- [x] (completed — Eric, 2026-07-01) **Remove Protobuf support from minimalist-kafka's Schema Registry
  integration (Snyk-driven, pre-release).** Full detail in the Key Decision
  [[minimalist-kafka-protobuf-removed]] and the 2026-07-01-224313 session log. Code + tests + pom +
  sync-over-async-demo's parallel demo path + user-facing docs (kafka-flow-adapter.md,
  configuration-reference.md, schema-registry-mock.md, demo README, schema-registry-standalone README) all
  updated with the rationale stated explicitly, not silently dropped. Full reactor `mvn test`: green.
  <!-- id: thread-minimalist-kafka-protobuf-removal | created: 2026-07-01 | last_used: 2026-07-01 | uses: 1 | tier: active | origin: 2026-07-01-224313 -->

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
  <!-- id: thread-compilegraph-syntax-validation | created: 2026-07-02 | last_used: 2026-07-02 | uses: 1 | tier: working | origin: 2026-07-02-004606 -->

- [ ] (planned — backlog, no ETA, no CVE driver) **Upgrade `kafka.version` (4.2.0 → 4.3.x) across the 24
  pom.xml files that pin it.** Deliberately deferred alongside the `confluent.version` 8.2.0→8.3.0 bump — see
  [[minimalist-kafka-confluent-8-3-0]] for the full reasoning. Confluent Platform 8.3.x's own tested pairing
  is Kafka 4.3.x, but nothing here requires following that pairing (this repo pins its own Kafka client
  independent of Confluent's suggestion, and the Confluent serializers are client-version-tolerant by
  design). Scope when picked up: verify kafka-clients 4.3.x + the embedded KRaft broker (`kafka_2.13:4.3.x`)
  behavioral compatibility (config defaults, controller behavior) across all 24 files, not just
  `minimalist-kafka` — a materially larger test surface than a serializer-library bump.
  <!-- id: thread-kafka-client-version-upgrade | created: 2026-07-01 | last_used: 2026-07-01 | uses: 1 | tier: working | origin: 2026-07-01-230246 -->

- [x] (sprint complete — Eric, 2026-07-02) **Deprecate 'simple type matching' in TaskExecutor → 'simple
  plugin' syntax.** Shipped as **PR [#130](https://github.com/Accenture/mercury-composable/pull/130)**
  (`feature/deprecate-simple-type-matching`, worktree `~/accenture/mercury-composable-2`). Full detail
  across the 2026-07-01-172822 session log (original implementation) and 2026-07-02-004606 (code review
  closeout). Eric's closing words: *"What you have done is not trivial. The framework is streamlined."*
  Code review pass fixed a handful of small IDE-flagged items in both modules and added a genuinely new
  capability — see [[event-script-minigraph-code-review-2026-07]] for detail (deprecation-conversion WARN
  logging in both `CompileFlows` and `CompileGraph`, plus a new "Application Lifecycle" doc section born
  from this review). One backlog item raised, not blocking: [[thread-compilegraph-syntax-validation]].
  Commits from the original implementation: `d5761c38` — event-script-engine (SimpleTypeMatchingConverter,
  CompileFlows, TaskExecutor cleanup, `ne` plugin, TypeConversionUtils + DataMappingHelper bug fixes, 115
  tests green); `495d2252` — minigraph CompileGraph startup gate + CompiledGraphs cache (55 tests green);
  `56ad6fb8` — GraphCommandService interactive validation + deprecation notice + help doc updates;
  `d125d4a3` — memory. (Two more commits — jackson bump `994b1954`, Protobuf removal `63d46041` — landed
  mid-review as a separate Snyk-driven thread; see [[minimalist-kafka-protobuf-removed]].) The code-review
  round's own changes (event-script-engine + minigraph-playground-engine fixes, docs) are queued for commit
  as of this writing.
  <!-- id: thread-deprecate-simple-type-matching | created: 2026-07-01 | last_used: 2026-07-02 | uses: 4 | tier: active | origin: 2026-07-01-172822 -->

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
  <!-- id: agent-memory-upgrade-v4280 | created: 2026-06-30 | last_used: 2026-06-30 | uses: 2 | tier: archive-candidate | origin: 2026-06-30-055333 -->

- [x] **Upgraded agent-memory v4.26.1 → v4.27.0** (Mode B, by Claude Code from the tool checkout). Single,
  additive rung — **standardized PR descriptions (What / Why)**: every enabled repo now ships a
  `.github/pull_request_template.md` whose body leads with **What** + **Why** (intent, not a restatement)
  and closes with a self-identifying `Co-Authored-By:` footer (extends the commit/session-log authorship
  convention to the PR altitude; advisory, never a gate). Applied here: installed the template (mercury had
  none), **additively merged** the convention + a checklist line into `AGENTS.md` (customizations preserved),
  stamped `.agent/version.md` → 4.27.0. No memory-shape/skill/adapter change. Post-upgrade `memory-lint`:
  **0 errors, 0 warnings**. Working tree **uncommitted** — review + commit at the mercury team's discretion.
  <!-- id: agent-memory-upgrade-v4270 | created: 2026-06-29 | last_used: 2026-06-30 | uses: 2 | tier: archive-candidate | origin: 2026-06-29-231747 -->

- [x] (completed — Eric, 2026-06-29) **minimalist-kafka Schema Registry serdes — feature COMPLETE & pushed.**
  All three serde phases done, tested (49 tests, 85% gate), demoed end-to-end, and **pushed** to
  `feature/sync-over-async` (PR #124, `7e2fe746..5ffb7dc7`): JSON Schema, Avro (`1bc2731e`/`6fc8c56c`),
  Protobuf (`bdab28f3`/`bc9be976`); Avro + Protobuf each validated via Eric's multi-terminal run
  (continuous-trace telemetry). The kafka-flow-adapter guide now documents the Schema Registry integration
  (`5ffb7dc7`). Closed — the feature is ready for PR review/merge. See [[minimalist-kafka-schema-registry]].
  <!-- id: thread-schema-registry-avro-protobuf | created: 2026-06-29 | last_used: 2026-06-29 | uses: 4 | tier: archive-candidate | origin: 2026-06-29-010147 -->
- [x] (completed — Eric, 2026-07-01, PR #129) **minimalist-kafka: resolve `schemaId` from subject/version
  for `simple.kafka.notification`.** Shipped as the subject/version producer contract — full detail in the
  Key Decision [[kafka-schemaid-from-subject-version]] and the 2026-07-01-004724 session log. Resolved to
  **subject/version only** (no id-driven fallback; naming strategies dropped), two-tier cache, new mock
  `GET /subjects/{subject}/versions/{version}` endpoint. Validated e2e.
  <!-- id: thread-kafka-schemaid-from-name | created: 2026-06-30 | last_used: 2026-07-01 | uses: 1 | tier: archive-candidate | origin: 2026-06-30-212955 -->

- [ ] (implemented, **uncommitted** — Claude Code, 2026-07-02) **minimalist-kafka: Confluent CSFLE wired.**
  Full detail in the Key Decision [[kafka-csfle-delegation]] and the 2026-07-02-020429 session log. Branch
  `feature/kafka-csfle-field-encryption`, working tree dirty (5 modified + 2 new test files), **nothing
  committed**. All tests green (5 new + full `minimalist-kafka` suite 54, coverage gate met). Design spec
  `draft-design-specs/kafka_csfle_field_encryption_design.md` (v3.0, gitignored) fully in sync — §10/§11
  record exactly what shipped. **Next action: Eric's code review, then commit + PR** (same flow as #128/#129).
  → relates [[minimalist-kafka-schema-registry]], [[kafka-schemaid-from-subject-version]].
  <!-- id: thread-csfle-field-encryption | created: 2026-07-01 | last_used: 2026-07-02 | uses: 2 | tier: working | origin: 2026-07-02-020429 -->
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
  <!-- id: standalone-schema-registry-mock | created: 2026-06-28 | last_used: 2026-07-01 | uses: 6 | tier: archive-candidate -->
