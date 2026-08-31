# Annotation → Macro → Decorator: the Registration-Metadata Interop Design

Date: 2026-07-25 (draft for maintainer review)
Scope: Java (reference) → Rust (refine now) → Python, Node.js (future ports)
Inputs: two deliberately context-free external reviews
(`annotation-macro-fidelity-report-2026-07-25.md`, `preload-interop-blueprint.md`),
verified line-by-line against both codebases by an 8-agent survey (2026-07-25).

Goals (maintainer):
1. Developers see consistent, decoupled annotations/macros — Java scans the classpath at
   runtime, Rust collects at link time, but the code style reads the same.
2. The Rust port becomes the best-practice template for the Python and Node.js ports.

Out of short-term scope (maintainer): minimalist-kafka and sync-over-async ports — the
Kafka-facing annotations ride that future iteration.

---

## 1. Ground truth (verified, with citations)

### 1.1 The Java annotation surface (reference)

Core package `org.platformlambda.core.annotations` — 10 annotations, all
`@Target(TYPE) @Retention(RUNTIME)`:

| Annotation | Payload | Consumed by |
|---|---|---|
| `@PreLoad` | `route` (comma-separated aliases), `customSerializer=Void.class`, `inputPojoClass=Void.class`, `instances=1`, `envInstances=""`, `isPrivate=true`, `inputStrategy/outputStrategy=DEFAULT` (enum SNAKE/CAMEL/DEFAULT) | AppStarter.preload → Platform.register (AppStarter.java:363-447) |
| `@BeforeApplication` | `sequence=10` (0 reserved, 2 Event Script) | AppStarter.prepareApp (ascending) |
| `@MainApplication` | `sequence=10` | AppStarter (after preload + web) |
| `@WebSocketService` | `value` (name), `namespace="ws"` | AppStarter.prepareWebsocketServices |
| `@OptionalService` | `value` — OR-list of `key`, `!key`, `key=value` | `Feature.isRequired` (Feature.java:35-76), honored by BeforeApp/MainApp/PreLoad/WebSocket scan + rest-spring WebLoader + minigraph PlaygroundLoader |
| `@ZeroTracing` | marker | WorkerDispatcher:40 — suppresses trace start, log-context, telemetry |
| `@EventInterceptor` | marker | ServiceDef:62 — raw envelope in, response swallowed, hygiene-scrub exempt |
| `@KernelThreadRunner` | marker | ServiceDef:63-67 — kernel executor instead of virtual threads |
| `@CloudConnector` | `name`, `original=""` | Platform.connectToCloud — only the Kafka mesh stack uses it |
| `@CloudService` | `name`, `original=""` | Platform.startCloudServices — Kafka mesh only |

Module-local extension annotations:

| Annotation | Module | Shape | Loader |
|---|---|---|---|
| `@SimplePlugin` (marker) | event-script-engine | class implements `PluginFunction` (`getName()` default = class-name→camelCase; `Object calculate(Object...)`) | SimplePluginLoader, `@BeforeApplication(seq=3)` — before CompileFlows (seq 5) so `f:` names validate at compile time |
| `@FetchFeature("name")` | minigraph-playground-engine | class implements `FeatureRunner` | PlaygroundLoader, `@MainApplication(seq=8)`; gates every class through `Feature.isRequired` |

Operational companion: **`yaml.preload.override`** (AppStarter.java:289-434) — config files
that rename / fan out / re-tune `instances` of any preloaded route without recompiling
(entries: `original`, `routes`, `instances`, `keep-original`; multiple files merge:
route-sets union, first-set instances wins).

Conflict semantics in Java today (inconsistent — see §4.3):
- Platform.register: **warn + reload** (Platform.java:555) — but the javadoc *claims* it
  throws on duplicates (Platform.java:382, 396) — a documentation bug.
- PlaygroundLoader features: **silent replace** (ConcurrentHashMap.put, PlaygroundLoader.java:72).
- SimplePluginLoader: warns on duplicates.
- Cloud services: duplicate names in `cloud.services` rejected with an error (Platform.java:258).

### 1.2 The Rust macro surface (current)

Three proc-macro crates; all entries collected via link-time `inventory`:

| Macro | Args | Target | Entry struct |
|---|---|---|---|
| `#[preload]` | `route` (comma aliases, compile-validated), `instances=1` (clamped 1..=1000 like ServiceDef), `env_instances` (config KEY, resolved at boot), `is_private=true`, `typed`, `zero_tracing`, `interceptor` | struct | `PreloadEntry{route, instances, env_instances, optional_service, zero_tracing, interceptor, is_private, factory}` |
| `#[before_application]` / `#[main_application]` | `sequence=10` | struct (EntryPoint) | `BeforeAppEntry` / `MainAppEntry` (+ optional_service) |
| `#[websocket_service]` | name, `namespace="ws"` | struct | `WsServiceEntry` (+ optional_service) |
| `#[optional_service("cond")]` | condition | **first-class stackable attribute**, consumed by the four platform macros; compile error if used alone | folded into the primary's entry |
| `#[simple_plugin]` | `name` (default = camelCase of fn name — deliberately mirrors PluginFunction.getName) | **free fn** `fn(&[rmpv::Value]) -> Result<rmpv::Value, String>` | `SimplePluginEntry{name, body}` |
| `#[fetch_feature("name")]` | name only — **any other arg is a compile error** | struct (FeatureRunner) | `FetchFeatureEntry{name, factory}` — **no optional_service field** |

Marker stacking already mirrors Java: `#[zero_tracing]`, `#[event_interceptor]`,
`#[optional_service]` may be written as separate stacked attributes; `#[preload]` strips
and consumes them (platform-macros/src/lib.rs:144-147). Documented in
macros-reference.md "Stacked markers".

Collectors/lifecycle: AutoStart::main iterates BeforeApp → PreLoad → MainApp inventories;
`env_instances` resolved at **boot** (app_starter.rs:426-430) with the same
digits-else-fallback semantics as Java's getInstancesFromEnv; `optional_service`
evaluated by `util::feature::is_required` (feature.rs:32-53 — same OR-list/!/= grammar as
Java's Feature) for BeforeApp/PreLoad/MainApp/Ws entries. Event-script plugins load at
`before_application(seq=3)`; knowledge-graph features at `before_application(seq=1)`.

Conflict semantics in Rust today: preload = **warn + reload** (platform.rs:238-244, the
F10 Java-parity fix); websocket = silent replace; simple plugins = **silent replace —
a user plugin can silently shadow a built-in** (plugins.rs:78-83); fetch features =
**skip/first-wins** on the declarative path (features.rs:74) but replace on explicit
`register()`.

### 1.3 Adoption reality

- `#[preload]` ×43, `#[before_application]` ×6, `#[main_application]` ×3,
  `#[websocket_service]` ×2, `#[optional_service]` ×17 in production Rust code.
- `#[simple_plugin]` and `#[fetch_feature]`: **zero production usages** — all 46 built-in
  mapping plugins are hard-coded in `builtin_registrations()` (plugins.rs:350-410,
  incl. the PR #220 trio isEmpty/getFirst/getLast at :402-404), both built-in fetch
  features hard-coded in `register_builtins()` (features.rs:157-164).
- Java, by contrast: **all 47 built-in plugins are `@SimplePlugin` classes** under
  `com.accenture.services.plugins/` and **both built-in features are `@FetchFeature`
  classes** — Java's engine dogfoods its own extension points; Rust's does not.

---

## 2. Evaluation of the two external reports

### 2.1 Fidelity report — verdict-by-verdict

| Report claim | Verified outcome |
|---|---|
| FetchFeature is the least faithful port | **CONFIRMED** — hard-wired builtins, no OptionalService path (macro rejects extra args), first-wins vs Java's last-wins |
| SimplePlugin mostly faithful; builtins hard-wired; test-only adoption | **CONFIRMED** — incl. the Java half (46/47 annotation-driven) the report could not have known to double-check |
| Rust plugin = free fn vs Java class | **CONFIRMED** — assessed here as a *correct* per-language idiom, not a defect (§4.4) |
| zero_tracing/event_interceptor "do not exist as standalone attributes" | **OVERSTATED** — they already stack as separate marker attributes below `#[preload]`, mirroring Java's annotation stacking; they are consumed by preload rather than independent proc-macros, which is the right Rust mechanic. Goal 1 is already met here. |
| KernelThreadRunner/CloudConnector/CloudService unported | **CONFIRMED — and already documented as deliberate** (macros-reference.md:324-330, function-execution.md, port-scope.md). The report asked us to "decide explicitly"; the decision exists in writing. |
| P2 "deterministic conflict policy" | **VALID — and bigger than the report knew**: Java itself is internally inconsistent and its Platform javadoc contradicts the implementation. This is a cross-engine contract item, not a Rust patch. |

What the context-free reviewers missed (the intentional blind spots):
1. **`yaml.preload.override`** — a whole operational surface of the Java annotation model
   (config-driven rename/fan-out/instance-tuning), unported in Rust (documented), unknown
   to the blueprint.
2. **`#[optional_service]` is already first-class and stackable** in Rust — the platform
   pattern the report recommends inventing for fetch_feature already exists to reuse.
3. **Java's SimplePluginLoader does NOT honor @OptionalService** (only FetchFeature's
   loader does) — so "gate everything" would *exceed* Java, a lock-step decision, not a
   parity fix.
4. Lifecycle nuance: Java loads fetch features at `@MainApplication(seq 8)`; Rust at
   `before_application(seq 1)`. Both satisfy "features ready before graph execution";
   noted as an accepted difference.
5. Two **stale Rust docs** contradicting shipped behavior: event-script/syntax.md:354-360
   claims `#[preload]` takes a single route (comma aliases ARE supported and used in
   production); api-overview.md:28-36 claims no public/private distinction (is_private +
   the /api/event 403 exist).

### 2.2 Blueprint report — what to keep, what to replace

Keep (the reusable skeleton): one-schema-many-mechanisms; **resolve late, register
early**; string enums on the wire; explicit per-language discovery; the 6-step boot
sequence; conformance-by-golden-JSON. All of this matches how we already proved the
envelope wire format (golden vectors + normalize-and-diff), so it fits the project's
established parity method.

Replace: the entire placeholder schema — §3 below carries the real model. Its §3.2
"known gap: customSerializer/inputPojoClass may be silently broken in Rust macros" is
**refuted**: the four serialization-related @PreLoad fields are *intentionally absent*
(serde is the single compile-time serializer; `TypedFunction<I,O>` + `body_as::<Vec<T>>()`
subsumes inputPojoClass, which exists only to defeat JVM type erasure). One genuine
residual: Java can flip snake/camel per route or via `snake.case.serialization` at
runtime; Rust cannot without recompiling — carried in the capability matrix, not a bug.

---

## 3. The registration-metadata contract (real model, v1 proposal)

One canonical, language-agnostic metadata model; per-language carriers (annotation /
proc-macro / decorator) are idioms; **semantics are fixed**:

```json
{
  "kind": "function | websocket | entrypoint-before | entrypoint-main | plugin | feature",
  "route": "string (functions; comma-separated aliases allowed)",
  "name": "string (websocket/plugin/feature)",
  "namespace": "string, websocket only, default \"ws\"",
  "sequence": "integer, entrypoints only, default 10 (0 framework-reserved)",
  "instances": "integer, default 1, clamped 1..1000 at registration",
  "envInstances": "string config KEY, default \"\" — resolved at BOOT: numeric value wins, else instances",
  "isPrivate": "boolean, default true",
  "zeroTracing": "boolean marker, default false",
  "eventInterceptor": "boolean marker, default false",
  "optionalService": "string | null — OR-list of key, !key, key=value against app config, evaluated at BOOT",
  "capabilities": {
    "customSerializer": "type name | null — JVM-family capability; N/A where serialization is compile-time (Rust)",
    "inputPojoClass": "type name | null — JVM type-erasure workaround; N/A in monomorphized/duck-typed runtimes",
    "serializationStrategy": "\"SNAKE\"|\"CAMEL\"|\"DEFAULT\" in/out — runtime-flippable in Java/Python/Node; compile-time (serde rename_all) in Rust",
    "executionHint": "\"default\" | \"blocking\" — Java=@KernelThreadRunner, Rust=reserved (tokio spawn_blocking, unimplemented), Python/Node=TBD (GIL/worker-threads)"
  }
}
```

Fixed semantics (every port MUST):
1. **Attach at definition, resolve at boot** — env keys and optional-service conditions
   are never evaluated at decoration/expansion/import time.
2. **Optional-service grammar** is exactly Java's Feature grammar: comma = OR, `!` = NOT,
   `key=value` case-insensitive, bare `key` means `key=true`.
3. **Conflict policy** (see D2): identical across all registries and all languages,
   logged with both sources.
4. **Boot sequence**: discover → register → *override (config)* → resolve (env) →
   validate (unique routes, non-empty registry) → route table. The `override` step is
   Java's `yaml.preload.override`; ports carry it (D4).
5. **Lifecycle anchors**: plugins load before flow compilation; features before graph
   execution; entrypoint sequences ascend; sequence 0 is framework-reserved.
6. **Wire form for tooling**: JSON, camelCase keys, enums as strings — never ordinals.
7. **Discovery is explicit per language** and must fail loudly when a registry that
   should be populated is empty: Java = classpath scan (free); Rust = link-time
   inventory (+ startup count assertion); Python = package walk + import before resolve;
   Node = glob + dynamic import before resolve.

Extension-point shape guidance for Python/Node (from the Rust experience):
- **Plugins are named pure functions** — Rust's free-fn form is the better template than
  Java's class+interface (a decorator on a function is the natural Python/Node idiom);
  the *name derivation* rule (camelCase of the declared name, overridable) is the
  portable part. Java's class shape stays as-is (reference does not churn).
- **Features are small stateful runners** — struct/class with before/after hooks; the
  annotation carries the feature name explicitly.

Conformance: each engine exports its resolved demo-app registry (hello-world fixture set)
to canonical JSON; a shared golden file must match after key-sorting — the
golden-vector method that already guards the envelope wire format.

---

## 4. The Rust refinement plan (P1 — this iteration)

### 4.1 Dogfood the extension points (goal 1's biggest visible gap)
Convert all 46 built-in mapping plugins from `builtin_registrations()` to
`#[simple_plugin]` fns (same bodies, same names — isEmpty/getFirst/getLast keep exact
Java error-message parity), and both built-in fetch features to `#[fetch_feature]`
structs. Delete `builtin_registrations()` / `register_builtins()`; replace the
`include!("plugins_e8.rs")` textual include with a proper module. Add the startup
assertion (registry count) so a linker-elision regression fails loudly in CI.

### 4.2 fetch_feature reaches parity with the platform macros
Accept stacked `#[optional_service("...")]` (reuse the existing platform strip/fold
pattern), add `optional_service: Option<&'static str>` to `FetchFeatureEntry`, evaluate
via the existing `util::feature::is_required` in `load_declared_features()` with the
same "Skip optional …" log line as Java's PlaygroundLoader.

### 4.3 One conflict policy, both engines (D2)
Proposed: **explicit register() wins over declarative; within a registry, duplicate
names = WARN with both sources + last-wins reload** — Java Platform's actual behavior,
extended everywhere. Rust: features skip→warn+replace; plugins/websocket add the WARN.
Java (small lock-step PR): fix the Platform.register javadoc (says "throws", actually
reloads); add the WARN to PlaygroundLoader's silent put. Regression tests both engines,
including "user plugin shadows built-in emits a warning naming both".

### 4.4 Documented divergences (no code change; contract text)
Plugin carrier shape (Java class / Rust fn), fetch-feature load anchor
(MainApplication-8 vs before_application-1), serialization capability matrix,
KernelThreadRunner→tokio posture. Fix the two stale Rust doc claims (syntax.md
single-route; api-overview public/private).

## 5. P2 (same arc, gated separately)
- **Port `yaml.preload.override` to Rust** (D4): a boot-time transform over the collected
  PreloadEntry set between inventory iteration and registration — rename/fan-out/
  instances with Java's merge rules; removes the one operational annotation-surface gap.
- **Publish the contract** (D5): spec page in the Java repo (reference), adapted in the
  Rust docs like the macros-reference; golden conformance fixture; propose
  ADR "Registration metadata is a cross-language contract; carriers are per-language
  idioms" (Java ADR-0009 / Rust ADR-0008).

## 6. Deferred (explicitly)
- `@KernelThreadRunner` analog (`executionHint: blocking` → spawn_blocking): reserved in
  the contract, unimplemented — revisit when a field workload needs it.
- `@CloudConnector`/`@CloudService` + Kafka adapter annotations: ride the future
  minimalist-kafka / sync-over-async port iteration (maintainer direction).
- Whether `@SimplePlugin` should honor `@OptionalService` (would exceed Java — a
  both-engines enhancement, not parity): proposed DEFER unless wanted now (D3b).

## 7. Decision points for the maintainer
- **D1** Convert Rust built-ins to declarative macros (46 plugins + 2 features). [rec: yes]
- **D2** Conflict policy = warn + last-wins everywhere, explicit register() > declarative;
  incl. the small Java javadoc/log lock-step fixes. [rec: yes; alternative: startup error]
- **D3a** fetch_feature gains optional_service (Java parity). [rec: yes]
  **D3b** simple_plugin too (exceeds Java, needs Java-side change in lock-step). [rec: defer]
- **D4** Port yaml.preload.override to Rust. [rec: yes, P2]
- **D5** Contract spec page + golden conformance fixture + ADR pair. [rec: yes, P2]
- **D6** executionHint reserved-only for now. [rec: defer implementation]
