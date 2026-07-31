# kafka-demo — minimalist-kafka worked example

A hands-on, end-to-end demonstration of the **minimalist-kafka** consumer + producer pattern — including
both **routing styles** of the Kafka flow adapter, side by side. You publish a message from a terminal, it
travels through Kafka into a composable Java app, gets processed, is published to another topic, and shows
up in a second terminal — with the whole path visible in the Java app's telemetry log.

```
  publish-inbound.js  --(demo.inbound)-->  kafka-demo (Java)  --(demo.outbound)-->  listen-outbound.js
   (program-2, you type)                    |  flow adapter            ^                (program-1, logs it)
                                            |  -> demo.processor       |
                                            |  -> simple.kafka.notification
                                            +--------------------------+

  publish-orders.js   --(demo.orders)--->  kafka-demo (Java)  --(demo.outbound)-->  listen-outbound.js
   (program-3, you type)                    |  flow adapter: SECOND-LEVEL ROUTING (per-record rules)
                                            |  -> flow://demo-order-flow       (type=order / order-*)
                                            |  -> task://demo.refund.processor (body event.kind=refund)
                                            |  -> flow://demo-catch-all-flow   (default)
```

The Java app is pure minimalist-kafka: the **Kafka flow adapter** binds `demo.inbound` to the
`kafka-demo-flow` (**direct routing** — every message goes to one flow), whose `demo.processor` task wraps
the message with processing metadata, and `simple.kafka.notification` publishes the result to
`demo.outbound`. The `demo.orders` binding uses **[second-level
routing](../../docs/guides/minimalist-kafka.md#routing)** instead — a rule list inspects each record and
picks the target per message. No code wires any of those steps together — the flow YAML does (see
ADR-0007).

## Prerequisites

- Java 21 + Maven (to build this repo: `mvn clean install` at the root, or build the modules below).
- Node.js 18+ (for the three helper programs).
- The local Kafka broker from [`helpers/kafka-standalone`](../../helpers/kafka-standalone) — no Docker needed.

Install the Node dependencies once:

```shell
cd examples/kafka-demo/node
npm install
```

## Run it — five terminals

Run each step in its own terminal, from the repo root unless noted.

### Terminal A — start the local Kafka broker

> **Note**: `x.y.z` denotes the current Mercury version shown in the root `pom.xml`.

```shell
cd helpers/kafka-standalone
mvn clean package
java -jar target/kafka-standalone-x.y.z-exec.jar
```
Wait for it to report the broker is up on `127.0.0.1:9092`.

### Terminal B — create the demo topics (10 partitions each)
```shell
cd examples/kafka-demo/node
node create-topics.js
# -> created (10 partitions each): demo.inbound, demo.orders, demo.outbound
```

### Terminal C — start the kafka-demo Java app
```shell
cd examples/kafka-demo
mvn clean package
java -jar target/kafka-demo-x.y.z.jar
```
It starts the flow adapter (consuming `demo.inbound`) and the producer.

### Terminal D — listen on the outbound topic (program-1)
```shell
cd examples/kafka-demo/node
node listen-outbound.js
# -> listening on 'demo.outbound' ...
```

### Terminal E — publish from the console (program-2)
```shell
cd examples/kafka-demo/node
node publish-inbound.js
> hello composable kafka
```

## What you should see

**Terminal E (publisher)** sends a `traceparent`, so it prints the `traceId` it started:
```
[2026-06-27T16:30:00.123Z] -> demo.inbound cid=2f1c... traceId=0af7651916cd43dd8448eb211c80319c hello composable kafka
```
**Terminal C (Java app)** logs the receipt (with the trace-id and the incoming span it chained onto) and
the telemetry **end-to-end path** (same trace id):
```
... DemoProcessor - Received from demo.inbound (cid=2f1c..., traceId=0af765...319c, incoming span=7bd5f5...): hello composable kafka
... Telemetry - {trace={... id=0af765...319c, span_id=bd18e9..., service=demo.processor ...}}
... Telemetry - {trace={... parent_span_id=bd18e9..., span_id=ac1bc0..., service=simple.kafka.notification ...}}
... Telemetry - {trace={... service=task.executor ...}, annotations={execution=Run 2 tasks ...,
      tasks=[{name=demo.processor}, {name=simple.kafka.notification}], flow=kafka-demo-flow}}
```
**Terminal D (listener)** receives the processed message, carrying the **same trace id** but a **new span**:
```
[2026-06-27T16:30:00.456Z] <- demo.outbound[p3] cid=2f1c... traceId=0af765...319c span=ac1bc0... {"received":"hello composable kafka","processedBy":"kafka-demo","processedAt":"2026-06-27T16:30:00.4Z","traceId":"0af765...319c"}
```

The `cid` is preserved end-to-end, and the **`traceId` is identical** at the publisher, in the Java
telemetry, and at the listener — proof the trace stays continuous across both Kafka hops. (Each hop gets a
new `span_id`; `simple.kafka.notification`'s span becomes the parent of the next hop, while the trace-id is
carried unchanged.) If the publisher sends no `traceparent`, the flow simply starts a fresh trace instead.

## Second-level routing — one topic, many targets

The `demo.orders` binding shows the adapter's **second-level routing**: instead of one `flow`, a `flows`
rule list inspects a key-value of each record and picks the target per message — the common Kafka pattern
of one topic carrying mixed event types. See the rule grammar in
[`kafka-flow-adapter.yaml`](src/main/resources/kafka-flow-adapter.yaml); the first matching rule wins, in
declaration order, and the mandatory `default` catches the rest. `serializer: 'json'` decodes each JSON
record to a `Map` before routing, so the `input.body` rule can match — a non-JSON record keeps its raw
`byte[]` and falls through to the default.

### Terminal F — publish mixed events (program-3)
```shell
cd examples/kafka-demo/node
node publish-orders.js
```
One command per routing rule (an optional trailing JSON overrides the canned payload):

| You type | Record shape | Rule that fires | Target |
|----------|--------------|-----------------|--------|
| `order` | `type: order` header + JSON body | `input.header.type(order)` — exact | `flow://demo-order-flow` |
| `order-42` | `type: order-42` header + JSON body | `input.header.type(order-*)` — wildcard | `flow://demo-order-flow` |
| `refund` | no `type` header, `{"event":{"kind":"refund"},...}` body | `input.body.event.kind(refund)` — body path | `task://demo.refund.processor` |
| `hello world` | raw text (not JSON) | none — falls through | `default` → `flow://demo-catch-all-flow` |

**What you should see per command:**

- `order` / `order-42` — the Java log shows `OrderProcessor - Order event routed by rule type(...)`, and
  **Terminal D** receives the processed order on `demo.outbound` with `"routedBy"` naming the matched key.
  The flow publishes the processor's `Map` straight through `simple.kafka.notification`, which
  **auto-serializes it to JSON bytes** — the outbound symmetry of `serializer: 'json'` (`Map` in the
  function, JSON on the wire).
- `refund` — the **Java log** shows `RefundProcessor - Refund routed by rule input.body.event.kind(refund)`
  with the same `cid`/`traceId` the publisher printed. Nothing arrives on `demo.outbound`: a `task://`
  target invokes the function **directly** — all record headers copied verbatim, the whole payload as the
  body, no flow and no data mapping. Use it for processing simple enough that a flow is overweight;
  anything needing orchestration (like publishing onward) belongs in a `flow://` target.
- anything else — **Terminal D** receives the annotated record from `demo-catch-all-flow` with
  `"routedBy": "default"` and a `"shape"` field showing whether the body arrived as a decoded `Map`/`List`
  or as raw bytes (`serializer: 'json'` is best-effort: an unparseable record passes through unchanged,
  and the default handler deals with it — the pattern a production catch-all should follow).

Each published record carries its own `traceparent`, so every routed message — flow or task — shows full
trace continuity in the telemetry log, exactly like the direct-routing path.

## How it maps to minimalist-kafka

| Piece | What it shows |
|-------|---------------|
| [`kafka-flow-adapter.yaml`](src/main/resources/kafka-flow-adapter.yaml) | the **consumer** side, both styles: direct routing (`flow`) and second-level routing (`flows` + `serializer` + `ttl`) |
| [`kafka-demo-flow.yml`](src/main/resources/flows/kafka-demo-flow.yml) | orchestration as config: `demo.processor` → `simple.kafka.notification` |
| [`demo-order-flow.yml`](src/main/resources/flows/demo-order-flow.yml) | a rule-selected **specific flow**; publishes a `Map` that `simple.kafka.notification` auto-serializes |
| [`demo-catch-all-flow.yml`](src/main/resources/flows/demo-catch-all-flow.yml) | the mandatory **default** flow; its task handles both body shapes (Map or raw bytes) |
| [`DemoProcessor.java`](src/main/java/com/accenture/kafka/demo/tasks/DemoProcessor.java) | a self-contained function (the unit of work), in a `tasks` package per the [Code Conventions](../../docs/guides/code-conventions.md) |
| [`RefundProcessor.java`](src/main/java/com/accenture/kafka/demo/tasks/RefundProcessor.java) | a **`task://` routing target**: invoked directly by the adapter — headers copied verbatim, payload as body, no flow |
| `simple.kafka.notification` | the **producer** side: publish to a topic via data mapping (`text(demo.outbound) -> header.topic`) |

## Notes

- Point at a different broker with `export KAFKA_BOOTSTRAP_SERVERS=host:port` (both the Java app and the
  Node programs honor it).
- On repeated processing failure, a message is dead-lettered to the binding's configured `dlq-topic`
  (`demo.inbound.dlq` / `demo.orders.dlq` in `kafka-flow-adapter.yaml`); pre-create those topics if you
  want to exercise the failure path — it applies identically to `flow://` and `task://` targets. The happy
  path never touches them.
- The second-level routing rule grammar (selectors, the three matcher modes, targets, `serializer`,
  `ttl`) is documented in the [Minimalist Kafka guide](../../docs/guides/minimalist-kafka.md#routing).
