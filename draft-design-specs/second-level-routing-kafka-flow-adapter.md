# Second-Level Routing for the Kafka Flow Adapter

**Status:** design ratified by Eric 2026-07-30 (refined from the ideation draft in a
review round grounded against the v4.11.0 adapter code); implementation not yet started.

## Concept

The kafka-flow-adapter today routes every record of a binding to ONE flow
(`flow: <flow-id>`). Second-level routing is an **optional** alternative: inspect a
key-value in the inbound record's header or payload and pick the target per record —
the common Kafka pattern of one topic carrying mixed event types (e.g. a `type` header).

Grounding: flow selection is already per-message (the adapter sends `flow_id` as a
per-request header to `event.script.manager` on every record), so this feature is
**purely adapter-local** — no engine changes.

## Configuration

`flow` and `flows` are **mutually exclusive** — configure direct or second-level
routing, never both (startup validation; the `topic` XOR `topic-pattern` precedent).

### Direct routing (unchanged)

```yaml
consumer:
  - topic: 'mini-test-topic'
    flow: 'kafka-sink-flow'
```

### Second-level routing

```yaml
consumer:
  - topic: 'mini-test-topic'
    flows:
      - 'input.header.type(order) -> flow://order-flow'
      - 'input.header.type(order-*) -> flow://order-variant-flow'
      - 'input.header.type(regex: ^shipment-(eu|us)$) -> flow://shipment-flow'
      - 'input.body.event.kind(refund) -> task://v1.refund.processor'
      - 'default -> flow://catch-all-flow'
```

### Second-level routing on a registry-less JSON topic (`serializer`)

```yaml
consumer:
  - topic: 'json-events'
    serializer: 'json'  # default SimpleMapper tries deserialization; extensible later
    flows:
      - 'input.body.event.kind(refund) -> task://v1.refund.processor'
      - 'default -> flow://catch-all-flow'
```

## Rule grammar

Each entry: `<selector>(<matcher>) -> <target>` — plus the mandatory `default` entry.

### Selector

- `input.header.<name>` — a Kafka record header. **Header-NAME lookup is
  case-insensitive** (Kafka preserves wire casing; rules must not depend on it).
  The matched value is the UTF-8-decoded header value.
- `input.body` followed by a **dot-bracket composite path** (MultiLevelMap semantics) —
  a Map body via `input.body.order.type`, a **top-level List body** via
  `input.body[0].type` (Eric's ruling 2026-07-30: `serializer: 'json'` applies to both
  Map and List, since the dot-bracket convention retrieves list items naturally), and
  any nesting of the two (`input.body.items[1].kind`). Usable only when the payload is
  a Map or List (see Payload prerequisites). Implementation note: body lookups run
  under a synthetic `body` root — this is what makes a top-level List addressable AND
  keeps MultiLevelMap's `$`-JsonPath escape hatch (whose parser can throw at record
  time) structurally unreachable; a `$`-prefixed key is an ordinary literal segment.
  Rule-lookup is additionally hardened at runtime so any surprise degrades to a
  non-match — the never-throws contract holds at both layers (review round,
  2026-07-30).

### Matcher — three modes, explicit over sniffing

| Form | Mode | Notes |
|------|------|-------|
| `key(value)` | exact match | case-sensitive value comparison |
| `key(val*)` | wildcard | the presence of `*` makes it one; `*` matches any run of characters |
| `key(regex: <expr>)` | regex | always explicit — the exception, not the norm; compiled at startup (the `topic-pattern` precedent) |

Footnote: an *exact* value that legitimately contains `*` cannot be expressed in exact
mode — use `regex:` with the character escaped. Expected never to matter for routing
keys.

### Evaluation semantics

- **Order matters: first match wins**, in YAML declaration order. Overlapping rules
  (natural with wildcards) shadow deliberately — put the most specific first.
- A missing header/key, a non-Map body for an `input.body` rule, or a non-string value
  is a **non-match — never an error**. Rule evaluation runs on the binding's single
  poll thread inside the existing failure envelope; it must not throw.
- **When none matches, the `default` target is used.** `default` is mandatory
  (startup-validated; the rest.yaml default-authentication precedent) and may target
  `flow://` or `task://` alike.

## Targets

### `flow://<flow-id>`

The existing five-site repo convention (Event Script process tag, modules.autostart,
mini-scheduler, graph.extension, external.state.machine): strip the prefix, dispatch to
the flow engine exactly as direct routing does today — same dataset
(`input.header`/`input.body`/`metadata.*`), same model.cid seeding, same trace
continuity, same ttl (the flow's own).

### `task://<route>` — direct invocation of a composable function

For processing simple enough that a flow is overweight. No input/output data mapping;
the dispatch contract (which already exists verbatim in TaskExecutor's sub-flow
dispatch and mini-scheduler's bare-route dispatch):

1. copy all inbound record headers to the EventEnvelope headers;
2. copy the whole payload (Map or byte[]) as the EventEnvelope body;
3. stamp traceId / tracePath / spanId and the business-correlation-id tag exactly as
   the flow path does (platform-core then gives the function trace adoption and the
   `my_*` metadata view for free — log-context stays business-or-nothing).

**Timeout:** a bare function has no flow ttl — an optional per-binding `ttl` (duration
syntax: `20s`, `5m`…) applies to task invocations; default 30s. Flows keep their own.

**Naming note (deliberate divergence):** existing dual-target sites use a *bare* route
name for direct function dispatch. In a field-facing routing table, the explicit
`task://` prefix is chosen instead — self-documenting and typo-proof; the divergence is
intentional and documented.

## Payload prerequisites (input.body rules)

Today the body is a Map **only** on the schema-registry path (`schema.enabled: true` +
registry URL; JSON-schema and Avro-record decodes yield Maps) — and not every
installation uses a schema registry. Therefore v1 ships an opt-in:

### `serializer: 'json'` — registry-less deserialization (ratified into v1, 2026-07-30)

An optional per-binding parameter for non-schema topics. `json` tells the adapter to
use the default **SimpleMapper** to TRY deserializing the record value (UTF-8) before
routing. The parameter is deliberately open-ended so the serializer feature can be
extended later (other formats, custom implementations).

- A JSON **object** becomes a Map — `input.body.<key>` rules match, and the flow/task
  receives the same Map-shaped dataset a schema binding produces.
- A JSON **array** becomes a List — **addressable by bracket rules**
  (`input.body[0].type(order) -> flow://order-processing`) and delivered as decoded.
- A top-level **scalar** keeps the raw byte[] like a parse failure does — Kafka's
  native body is bytes, so "not a JSON structure" means "unchanged" (implementation
  refinement 2026-07-30, aligned with the parse-failure ruling and the platform-core
  JSON-sniffing idiom; routing outcome is identical either way).
- **"Try" semantics — a parse failure needs NO special handling in the adapter's
  processing loop (ratified):** the record keeps its raw byte[] body, every
  `input.body` rule is a non-match, routing falls through to header rules / `default`,
  and the raw byte[] is passed as the input body to whichever flow or task is
  selected. If that target expects something else, the **target** throws — and the
  exception rides the existing retry → DLQ path naturally. The schema-registry path
  stays the strict decode-before-routing contract; `serializer` is best-effort by
  design.
- Numeric values follow SimpleMapper's customized-Gson semantics (the documented
  Integer/Long gotcha — flows should use `util.str2int` / `util.str2long` when a
  specific width matters).
- `serializer` combined with `schema.enabled: true` is a startup validation error —
  the registry already owns the decode.
- The parameter is independent of `flows`: a plain `flow:` binding benefits too (Map
  body without a registry).

Bindings with **neither** schema nor `serializer` keep the byte[] body: every
`input.body` rule is a non-match → `default`. That is the norm there, not an edge
case — route on headers, or opt in.

On schema-enabled bindings, `input.body.*` rules work as before; a decode failure
dead-letters before routing (unchanged poison handling).

## Outbound symmetry (ratified by Eric 2026-07-30)

`simple.kafka.notification` and `secondary.kafka.notification` (which inherits — the
twin-kafka subclass overrides accessors only) accept a **Map or List body** from the
calling application and automatically serialize it to JSON bytes with the default
SimpleMapper — the outbound twin of the inbound `serializer: 'json'`. byte[] passes
through verbatim (the minimalist default); `null` stays `null` (a Kafka tombstone);
anything else is rejected loudly. **Non-schema-registry topics only** (Eric's
scoping): with a `subject` header the body contract stays a byte[] JSON document,
exactly as before — the schema registry owns that encoding.

## Offset commit, retry, DLQ — unchanged envelope, both targets

Unless `auto-commit: true`, the adapter commits only after the target — flow **or
task** — finishes successfully (synchronous request, reply status < 400; the task path
reuses the identical await). Failure follows the existing path: retries
(`kafka.flow.max.retries`, backoff) then the binding's `dlq-topic`. A routing
*non-match* is not a failure — it selects `default`. A **synchronous dispatch error**
(e.g. a task route released after startup validation → "Route not found") joins the
same retry/DLQ envelope instead of escaping to the poll loop, which would kill the
consumer thread (review round, 2026-07-30).

## Startup validation (fail-fast, before any Kafka connection — the existing contract)

- `flow` XOR `flows`; `flows` non-empty; exactly one `default` entry.
- Every rule parses; regex matchers compile.
- Every `flow://` target exists in the compiled flows (the existing `Flows.getFlow`
  check); every `task://` route exists in the platform registry (checkable — the
  adapter starts via `@MainApplication`, after function preload).
- `task://event.script.manager` is rejected — the flow engine is a registered route,
  but addressing it as a bare task bypasses the flow-launch contract (no `flow_id`);
  flows are dispatched with `flow://` only (review round, 2026-07-30).
- Body keys use the dot-bracket composite-path convention (evaluated under a synthetic
  root, so JsonPath dispatch is unreachable by construction — no key syntax needs
  rejecting).
- Per-binding `ttl` (if present) parses as a valid duration.
- `serializer` (when present) is `json` — the only supported value in v1 — and is not
  combined with `schema.enabled: true`.

## Observability

Identical presentation for both targets: tracePath `KAFKA /<actual-topic>`, spans
chained onto the upstream context (traceparent > configured trace header > fresh id),
business correlation-id from the configured header reaching `model.cid` (flow) / the
business-cid tag (task). The selected rule may be recorded as a trace annotation
(e.g. `routing=input.header.type(order)`); the default path likewise.

## Out of scope / future

- `metadata.*` selectors (route by actual topic on pattern bindings) — natural later
  extension of the same grammar.
- Additional `serializer` values beyond `json` (plain text, custom implementations) —
  the parameter is shaped for this.
- Rust port: minimalist-kafka has no Rust port yet — no lock-step constraint now; this
  configuration grammar becomes part of the future port's contract (portable YAML).

## Implementation surface (sketch)

- New rule parser + matcher (small, adapter-local — the `key(matcher)` shape is a new
  grammar; the Event Script mapping DSL does not apply).
- `KafkaFlowAdapter.buildConsumer`: `flows` + `serializer` parsing + the validation
  list above; `KafkaConsumerBinding` carries the compiled rule list (immutable) and
  the serializer choice.
- `KafkaFlowConsumer.toDataset`: the `serializer: json` SimpleMapper decode branch
  beside the schema decode (best-effort — raw byte[] body kept on parse failure; no
  poison logic here, target failure rides the existing retry → DLQ path).
- `KafkaFlowConsumer.routeToFlow`: rule evaluation before envelope construction; the
  task-dispatch variant (headers+body copy + trace/cid stamping + per-binding ttl).
- Tests: matcher unit suite (exact/wildcard/regex/ordering/non-match/casing);
  embedded-Kafka e2e — header routing, body routing on a schema binding, `serializer:
  json` body routing without a registry (incl. top-level array → default and malformed
  JSON keeping byte[] → default), byte[] → default, task:// target incl.
  commit-after-success and DLQ-on-failure, mutual exclusivity and validation
  rejections.
- Docs: minimalist-kafka guide section, configuration reference, CHANGELOG.

## Related pre-existing observation (separate from this feature)

Event Script lowercases `input.header.*` references (matching the HTTP adapter's
lowercased ingestion), but the Kafka adapter preserves wire casing — a mixed-case
Kafka header is unreachable in flow data mapping today. Tracked separately; this
feature's case-insensitive header-name lookup avoids the trap for routing.
