# kafka-demo — minimalist-kafka worked example

A hands-on, end-to-end demonstration of the **minimalist-kafka** consumer + producer pattern. You publish a
message from a terminal, it travels through Kafka into a composable Java app, gets processed, is published
to another topic, and shows up in a second terminal — with the whole path visible in the Java app's
telemetry log.

```
  publish-inbound.js  --(demo.inbound)-->  kafka-demo (Java)  --(demo.outbound)-->  listen-outbound.js
   (program-2, you type)                    |  flow adapter            ^                (program-1, logs it)
                                            |  -> demo.processor       |
                                            |  -> simple.kafka.notification
                                            +--------------------------+
```

The Java app is pure minimalist-kafka: the **Kafka flow adapter** binds `demo.inbound` to the
`kafka-demo-flow`, whose `demo.processor` task wraps the message with processing metadata, and
`simple.kafka.notification` publishes the result to `demo.outbound`. No code wires those steps together —
the [flow YAML](src/main/resources/flows/kafka-demo-flow.yml) does (see ADR-0007).

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
```shell
cd helpers/kafka-standalone
mvn clean package
java -jar target/kafka-standalone-4.5.0.jar
```
Wait for it to report the broker is up on `127.0.0.1:9092`.

### Terminal B — create the demo topics (10 partitions each)
```shell
cd examples/kafka-demo/node
node create-topics.js
# -> created (10 partitions each): demo.inbound, demo.outbound
```

### Terminal C — start the kafka-demo Java app
```shell
cd examples/kafka-demo
mvn clean package
java -jar target/kafka-demo-4.5.0.jar
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

## How it maps to minimalist-kafka

| Piece | What it shows |
|-------|---------------|
| [`kafka-flow-adapter.yaml`](src/main/resources/kafka-flow-adapter.yaml) | the **consumer** side: bind a topic to a flow, with a consumer group |
| [`kafka-demo-flow.yml`](src/main/resources/flows/kafka-demo-flow.yml) | orchestration as config: `demo.processor` → `simple.kafka.notification` |
| [`DemoProcessor.java`](src/main/java/com/accenture/kafka/demo/tasks/DemoProcessor.java) | a self-contained function (the unit of work), in a `tasks` package per the [Code Conventions](../../docs/guides/code-conventions.md) |
| `simple.kafka.notification` | the **producer** side: publish to a topic via data mapping (`text(demo.outbound) -> header.topic`) |

## Notes

- Point at a different broker with `export KAFKA_BOOTSTRAP_SERVERS=host:port` (both the Java app and the
  Node programs honor it).
- On repeated processing failure, a message is dead-lettered to `demo.inbound.dlq` (per
  `kafka.flow.dlq.suffix`); pre-create that topic if you want to exercise the failure path. The happy path
  never touches it.
