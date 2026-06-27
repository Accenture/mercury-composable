---
title: Kafka Flow Adapter
summary: The opt-in minimalist-kafka library — route Kafka topics into Event Script flows (inbound) and
  publish events to Kafka (outbound), with externalized client config, per-binding consumer groups,
  partition pinning, and bounded retry plus per-topic dead-letter handling.
layer: operate
audience: [developer, ai-agent]
keywords: [kafka, flow adapter, minimalist-kafka, consumer, producer, dead letter queue, dlq, partition,
  consumer group, kafka-flow-adapter.yaml, simple.kafka.notification, at-least-once, traceparent]
---

# Kafka Flow Adapter

*Guide: route Kafka topics into Event Script flows, and publish events to Kafka, with the opt-in
`minimalist-kafka` library.*

> **At a glance**
>
> - **What** — `minimalist-kafka` is an opt-in library with two composable building blocks: an **inbound**
>   Kafka Flow Adapter that routes each topic into an Event Script flow (the Kafka counterpart of
>   `rest.yaml`), and an **outbound** notification function that publishes an event to a topic.
> - **Config, not code** — Kafka client connection/security comes from external `kafka-producer.properties`
>   / `kafka-consumer.properties` templates; the YAML binds topics to flows with optional consumer group and
>   partition pinning. Enterprise SASL/OAuth2/mTLS is configured, never coded.
> - **Reliable** — at-least-once consume (commit-after-process), bounded retry then a per-topic dead-letter
>   topic, and continuous W3C trace context across the Kafka hop.
> - **For** developers and operators triggering flows from Kafka, or emitting Kafka events from a flow.

The built-in HTTP flow adapter routes HTTP requests into flows. `minimalist-kafka` does the same for Kafka:
a topic listener mints an `EventEnvelope` and hands it to the Event Script engine, so the flow's tasks — not
the I/O layer — do the work. It is **not** the [service mesh](service-mesh.md) (`cloud.connector=kafka`),
which is a different concern; this library is an application-level building block you opt into.

> **This is an opt-in library.** Add the `minimalist-kafka` dependency and set `yaml.kafka.flow.adapter` to
> activate the inbound adapter. The outbound `simple.kafka.notification` function registers automatically.

## Enabling the library {#enable}

1. Depend on `system/minimalist-kafka` (it depends on `event-script-engine`).
2. Point `yaml.kafka.flow.adapter` at your adapter config (inbound). Without it, no consumer starts.
3. Provide the Kafka client templates (see [client config](#client-config)) — the classpath defaults work
   for local dev.

```properties
yaml.kafka.flow.adapter=classpath:/kafka-flow-adapter.yaml
```

The library autoloads at startup (`@MainApplication`): it builds the shared producer and, if
`yaml.kafka.flow.adapter` is set, starts one consumer thread per topic binding.

## Inbound: the adapter YAML {#adapter-yaml}

`kafka-flow-adapter.yaml` lists `topic -> flow` bindings:

```yaml
consumer:
  - topic: 'incoming-orders'
    flow: 'process-order'
    group: 'sales-order-group'        # optional
  - topic: 'incoming-payments'
    flow: 'process-payment'
    partition: 0                      # optional
```

| Field | Required | Description |
|-------|----------|-------------|
| `topic` | yes | Source Kafka topic. |
| `flow` | yes | Event Script flow id each message is routed into. |
| `group` | no | Consumer group id (see [consumer group](#group)). Defaults to `kafka-flow-adapter.<topic>`. |
| `partition` | no | Pins a single partition (see [partition pinning](#pinning)). Omit for group-managed assignment. |

The file is read by `ConfigReader`, so **every value supports `${ENV_VAR:default}` substitution** — e.g.
`group: '${KAFKA_CONSUMER_GROUP:sales-order-group}'`. A malformed entry (missing `topic`/`flow`, non-map)
fails startup fast and loud rather than being silently skipped.

### Consumer group {#group}

`group` is the Kafka consumer group id, used **exactly as given**. Enterprise DevSecOps teams typically
provision topics, ACLs, and consumer groups administratively, so the library never decorates the value. When
omitted it defaults to `kafka-flow-adapter.<topic>` for convenience in dev/test. All instances that share a
group load-balance that topic's partitions; set it explicitly to your assigned group in production.

### Partition pinning {#pinning}

When `partition` is present, the consumer **manually assigns** that single topic-partition instead of joining
the consumer group for dynamic assignment. This bypasses group rebalancing — the pinned consumer reads
exactly that partition — so you own the deployment model (one consumer per partition, or each pod pinning a
distinct partition via `partition: ${POD_PARTITION}`). Offsets still commit under the configured group.
Omit `partition` for normal group-managed consumption.

## Kafka client configuration {#client-config}

Connection and security settings live in **template files**, not code, because enterprise Kafka varies
widely (on-prem, cloud, SaaS, Confluent; SASL/PLAIN, SASL/SCRAM, OAuth2, mTLS):

- `kafka-producer.properties` — used by the publisher and the dead-letter writer.
- `kafka-consumer.properties` — base config for every adapter consumer.

Each is loaded by `ConfigReader` from a **file-then-classpath fallback** location (so CI/CD can drop a
rendered file at `/tmp/config`), with `${ENV_VAR:default}` substitution. The library **pins** only the
parameters its contract depends on and lets the template own everything else:

| Concern | Pinned by the library | From the template |
|---------|-----------------------|-------------------|
| Serialization | key=`String`, value=`byte[]` (de)serializers | — |
| Delivery semantics (consumer) | `enable.auto.commit=false`, `max.poll.records=1` | `auto.offset.reset` |
| Connection / security | — | `bootstrap.servers`, `security.protocol`, `sasl.*`, `ssl.*`, `acks` |

`bootstrap.servers` is template-only via `${KAFKA_BOOTSTRAP_SERVERS:127.0.0.1:9092}`. The byte[] wire
contract keeps the building blocks serializer-free; richer encodings (e.g. a Schema Registry) layer on top.

## Reliability: at-least-once, retry, and dead-letter {#reliability}

The consumer commits offsets **only after** the flow finishes a message, one message at a time
(`max.poll.records=1`). If the instance crashes before the commit, Kafka redelivers — the deliberate
resilience-over-throughput trade-off.

On a flow-processing **failure**, the message is retried up to `kafka.flow.max.retries` times (with
`kafka.flow.retry.backoff.ms` between attempts), then written to a **per-topic dead-letter topic**
`<topic><suffix>` (suffix `kafka.flow.dlq.suffix`, default `.dlq`):

- The DLQ is **strictly per topic** — a global/shared DLQ is an anti-pattern because mixing source schemas
  makes reprocessing (the whole point of a DLQ) hard. Only the suffix is configurable.
- The DLQ write is **confirmed** (it blocks on broker acknowledgement); the offset commits **only if that
  write succeeds**. If it fails — e.g. the DLQ topic does not exist — the offset is **not** committed and the
  message redelivers, so nothing is silently lost.
- Therefore **DLQ topics must be pre-provisioned** (Kafka auto-creation is off in production). A missing DLQ
  stalls the partition loudly instead of dropping data — the correct trade-off for a reprocessing holding area.
- The original record's headers are preserved, plus `dlq.origin.topic` and `dlq.error`.

**Reprocessing** (read `<topic>.dlq` → fix → replay) is business-domain logic and is intentionally out of
scope: the library guarantees durable capture, not replay.

## Outbound: publishing to Kafka {#outbound}

`simple.kafka.notification` is a composable function that publishes an event to a topic. Send it an
`EventEnvelope` with a `topic` header (required), an optional `partition` header, a `byte[]` body, and any
other headers (forwarded as Kafka headers):

```java
po.send(new EventEnvelope().setTo("simple.kafka.notification")
        .setHeader("topic", "outgoing-events")
        .setHeader("cid", correlationId)
        .setBody(payloadBytes));
```

Publishing is **drop-n-forget** (Kafka's commit log is the durable buffer), but async delivery failures are
logged rather than silently masked.

### Trace continuity across Kafka {#tracing}

Rather than forwarding the caller's stale `traceparent`, the notification function stamps a **fresh** W3C
`traceparent` from its own current span; the adapter parses it on the way in and chains the flow onto that
span. The result is one continuous distributed trace across the asynchronous Kafka boundary — the two
notification hops are the bridge spans. See [Observability](observability.md).

## Configuration keys {#config}

All keys are documented in the [Configuration Reference](configuration-reference.md#kafka-flow-adapter).
The essentials:

| Key | Default | Description |
|-----|---------|-------------|
| `yaml.kafka.flow.adapter` | — | Adapter config location; unset = inbound adapter off. |
| `kafka.producer.properties` | `file:/tmp/config/kafka-producer.properties,classpath:/kafka-producer.properties` | Producer template location(s). |
| `kafka.consumer.properties` | `file:/tmp/config/kafka-consumer.properties,classpath:/kafka-consumer.properties` | Consumer template location(s). |
| `kafka.flow.timeout.ms` | `30000` | Per-message flow timeout (also bounds the confirmed DLQ write). |
| `kafka.flow.max.retries` | `3` | Retry attempts before dead-lettering. |
| `kafka.flow.retry.backoff.ms` | `500` | Pause between retry attempts. |
| `kafka.flow.dlq.suffix` | `.dlq` | Suffix appended to the source topic to form its DLQ. |

## See also

- [Sync-over-Async](sync-over-async.md) — cross-pod synchronous request/response built on this library plus a Redis return route.
- [Configuration Reference](configuration-reference.md#kafka-flow-adapter) — every Kafka flow-adapter key.
- [Observability](observability.md) — how trace context stays continuous across the Kafka hop.
- [Minimalist Service Mesh](service-mesh.md) — the *different* `cloud.connector=kafka` event mesh.
