---
title: Kafka Flow Adapter
summary: The opt-in minimalist-kafka library — route Kafka topics into Event Script flows (inbound) and
  publish events to Kafka (outbound), with externalized client config, per-binding consumer groups,
  partition pinning, and bounded retry plus per-topic dead-letter handling.
layer: operate
audience: [developer, ai-agent]
keywords: [kafka, flow adapter, minimalist-kafka, consumer, producer, dead letter queue, dlq, partition,
  consumer group, kafka-flow-adapter.yaml, simple.kafka.notification, at-least-once, traceparent,
  schema registry, confluent, avro, protobuf, json schema, subject, version, schema.enabled]
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
| `schema.enabled` | no | When `true`, decode the Confluent-framed value into a `Map` before routing it into the flow (see [Schema Registry](#schema)). Default `false` (raw `byte[]`). |

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
contract keeps the building blocks serializer-free; richer encodings layer on top via the
[Schema Registry integration](#schema) (JSON Schema / Avro / Protobuf), opt-in per binding.

## Reliability: at-least-once, retry, and dead-letter {#reliability}

The consumer commits offsets **only after** the flow finishes a message, one message at a time
(`max.poll.records=1`). If the instance crashes before the commit, Kafka redelivers — the deliberate
resilience-over-throughput trade-off.

A flow **succeeds** when it replies with status `200`. Any other status — or a thrown exception, including a
**timeout** when the flow does not reply within its own `ttl` — is a **failure**. (Kafka is asynchronous, so
unlike an HTTP entry the adapter has no inherent request timeout: the flow's `ttl` *is* the processing
deadline. There is no separate flow-timeout knob.)

On a **failure**, the message is retried up to `kafka.flow.max.retries` times (with
`kafka.flow.retry.backoff.ms` between attempts), then written to a **per-topic dead-letter topic**
`<topic><suffix>` (suffix `kafka.flow.dlq.suffix`, default `.dlq`):

- The DLQ is **strictly per topic** — a global/shared DLQ is an anti-pattern because mixing source schemas
  makes reprocessing (the whole point of a DLQ) hard. Only the suffix is configurable.
- The DLQ write is **confirmed** (it blocks on broker acknowledgement, bounded by `kafka.dlq.timeout.ms`);
  on success the offset commits.
- **DLQ topics must be pre-provisioned** (Kafka auto-creation is off in production). The original record's
  headers are preserved, plus `dlq.origin.topic` and `dlq.error`.

> **When the DLQ write itself fails (data loss).** The DLQ is the last line of defense; a failed write to it
> is an *exception of an exception* with no further fallback. Blocking the partition to retry forever would
> re-run the failing flow and re-attempt the failing DLQ write indefinitely — a self-sustaining **recovery
> storm** (a known cause of prolonged outages). So the adapter instead logs a loud `ERROR` and **commits**,
> deliberately **dropping that one message** to keep the partition live. This is a conscious data-loss
> trade-off; a planned improvement is a classic resilience **alternative path** — persisting the record to a
> durable store for later replay instead of dropping it.

**Reprocessing** (read `<topic>.dlq` → fix → replay) is business-domain logic and is intentionally out of
scope: the library guarantees durable capture (when the DLQ is reachable), not replay.

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

One header opts a publish into the Confluent wire format instead of raw `byte[]`: `subject` (with an optional
`version`; see [Schema Registry](#schema)). It is an encoding directive — consumed by the function, not
forwarded as a Kafka header.

### Trace continuity across Kafka {#tracing}

Rather than forwarding the caller's stale `traceparent`, the notification function stamps a **fresh** W3C
`traceparent` from its own current span; the adapter parses it on the way in and chains the flow onto that
span. The result is one continuous distributed trace across the asynchronous Kafka boundary — the two
notification hops are the bridge spans. See [Observability](observability.md).

## Schema Registry: typed payloads (opt-in) {#schema}

The default wire contract is raw `byte[]`, which keeps the building blocks serializer-free. To interoperate
with existing Confluent client projects, the library can also speak the **Confluent Schema Registry wire
format** — `[magic 0x00][4-byte global schema id][payload]` — using Confluent's *own* serializers as a
library (not a reinvented codec). **JSON Schema, Avro, and Protobuf** are supported.

Set `schema.registry.url` to turn the feature on (point it at a real Confluent registry or the local
[`schema-registry-standalone`](schema-registry-mock.md) mock). When it is unset, schema features stay off and
the library keeps its raw `byte[]` behavior.

```properties
schema.registry.url=${SCHEMA_REGISTRY_URL:http://127.0.0.1:8081}
schema.registry.cache.ttl=30m                          # TTL for the in-memory schema cache (by id)
```

### Produce: subject-driven {#schema-produce}

`simple.kafka.notification` serializes the body into the wire format when you supply a `subject` header:

| Header | Description |
|--------|-------------|
| `subject` | The registry **subject** to serialize against. The schema must be **pre-registered**; the producer resolves the subject to a global schema id (and its type — `JSON`, `AVRO`, or `PROTOBUF`) from the registry and never registers. |
| `version` | Optional. The subject version to resolve: a positive integer to **pin** a specific version, or `latest` to track the current version. Defaults to `latest`. |

```yaml
# in a flow task that publishes via simple.kafka.notification
input:
  - 'text(orders) -> header.topic'
  - 'text(orders-value) -> header.subject'    # version omitted → latest
  - 'model.payload -> *'        # the body: a Map / JSON document
process: 'simple.kafka.notification'
```

The producer resolves the subject (+ version) to a **global schema id** and its **schema type** from the
registry, then serializes with Confluent's own serializer. The wire format itself is unchanged — it still
carries only the global id (`[magic 0x00][4-byte global schema id][payload]`) — and the consumer
(id-from-wire) is unchanged; only the producer's *input* moved from an explicit id+type to a subject. Whoever
registers the schema — CI, a client project, an admin tool — owns the subject naming strategy (TopicName /
RecordName / TopicRecordName are all supported, and a topic can carry many record types). This assumes schemas
are **governed artifacts registered out-of-band**, as they are in practice; the producer never auto-registers.

> **CSFLE not yet wired.** [Confluent Client-Side Field Level Encryption (CSFLE)](https://docs.confluent.io/cloud/current/security/encrypt/csfle/overview.html)
> is **not yet supported** by `minimalist-kafka` — the serializer is configured **without** encryption rule
> executors or a KMS. A schema that carries CSFLE encryption rules would therefore be serialized in
> **PLAINTEXT** (the rules are silently ignored, not enforced). **Do not use `simple.kafka.notification` to
> produce to CSFLE-protected topics** until CSFLE support lands (planned).

### Consume: decode by embedded id {#schema-consume}

Set `schema.enabled: true` on a consumer binding. The adapter reads the magic byte + embedded id, looks up
the registered schema's type, dispatches to the matching deserializer, and hands the flow a **`Map`** as
`input.body` (instead of `byte[]`). No flow-YAML change is needed (`input.body -> *` is type-neutral); a
schema-fed flow task simply takes `Map<String,Object>` instead of `byte[]`.

```yaml
consumer:
  - topic: 'orders'
    flow: 'process-order'
    group: 'order-group'
    schema.enabled: true
```

A **decode failure is a poison message** (retrying won't help), so the raw record is dead-lettered
immediately via the [DLQ path](#reliability) rather than retried.

### Notes {#schema-notes}

- **One subject-driven path, three formats.** The producer and consumer are type-generic; only the `subject`
  (and the registered schema behind it) differ — the producer reads the schema type from the registry, so the
  flow never names it. JSON Schema is *open* (`additionalProperties`), while Avro and
  Protobuf records are *closed-shape* — a message must match the declared fields, and a non-schema field is
  dropped on the wire. Avro applies declared field defaults for absent fields; Protobuf relies on proto3
  implicit defaults. Avro/Protobuf decode to generic records (no generated classes), rendered to a `Map`.
- **Schema cache.** Lookups by id are cached **in memory** (platform `ManagedCache`, TTL
  `schema.registry.cache.ttl`, default `30m`) to cut registry round-trips. A global schema id is immutable, so
  a cache hit is always the right schema. **Positive results only** — a not-found id is never cached, so a
  schema registered while the app is running becomes visible on the next lookup. The TTL lets schema changes
  be picked up without restarting pods (handy in dev / lower environments); lengthen it in production where
  schemas change rarely. The cache is rebuildable and **cleared at startup**.
- **Subject→id resolution cache.** The producer also caches the *subject (+ version) → schema id* resolution,
  and how long depends on the version. A **pinned numeric version** (`subject` + `version: N`) maps to one
  immutable schema id, so it is cached **long** (effectively forever). `latest` (the default) can change when a
  new version is registered, so it is cached on a **short TTL** and re-resolved frequently, picking up a new
  current version without a restart. Pin a version in production paths where the schema must not shift
  underneath you; use `latest` in dev / lower environments where tracking the newest schema is convenient.
- **Worked example.** The [sync-over-async demo](sync-over-async.md) runs the same end-to-end flow over all
  three formats (`json-topic-1/2`, `avro-topic-1/2`, `protobuf-topic-1/2`) alongside the raw `byte[]` path.

## Configuration keys {#config}

All keys are documented in the [Configuration Reference](configuration-reference.md#kafka-flow-adapter).
The essentials:

| Key | Default | Description |
|-----|---------|-------------|
| `yaml.kafka.flow.adapter` | — | Adapter config location; unset = inbound adapter off. |
| `kafka.producer.properties` | `file:/tmp/config/kafka-producer.properties,classpath:/kafka-producer.properties` | Producer template location(s). |
| `kafka.consumer.properties` | `file:/tmp/config/kafka-consumer.properties,classpath:/kafka-consumer.properties` | Consumer template location(s). |
| `kafka.dlq.timeout.ms` | `10000` | Confirm-write timeout for the dead-letter publish. (Flow processing has no timeout knob — the flow's own `ttl` is the deadline.) |
| `kafka.flow.max.retries` | `3` | Retry attempts before dead-lettering. |
| `kafka.flow.retry.backoff.ms` | `500` | Pause between retry attempts. |
| `kafka.flow.dlq.suffix` | `.dlq` | Suffix appended to the source topic to form its DLQ. |
| `schema.registry.url` | — | Confluent Schema Registry URL; unset = [schema features](#schema) off (raw `byte[]`). |
| `schema.registry.cache.ttl` | `30m` | TTL for the in-memory (`ManagedCache`) schema cache (by id); positive results only; cleared at startup. |

## See also

- [Sync-over-Async](sync-over-async.md) — cross-pod synchronous request/response built on this library plus a Redis return route.
- [Schema Registry mock](schema-registry-mock.md) — the local Confluent-compatible registry the [schema integration](#schema) talks to.
- [Configuration Reference](configuration-reference.md#kafka-flow-adapter) — every Kafka flow-adapter key.
- [Observability](observability.md) — how trace context stays continuous across the Kafka hop.
- [Minimalist Service Mesh](service-mesh.md) — the *different* `cloud.connector=kafka` event mesh.
