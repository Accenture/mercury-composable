---
title: Kafka Flow Adapter
summary: The opt-in minimalist-kafka library — route Kafka topics into Event Script flows (inbound) and
  publish events to Kafka (outbound), with externalized client config, per-binding consumer groups,
  regex topic subscription, partition pinning, bounded retry plus per-binding dead-letter topics, and a
  choice of at-least-once or auto-commit delivery.
layer: operate
audience: [developer, ai-agent]
keywords: [kafka, flow adapter, minimalist-kafka, consumer, producer, dead letter queue, dlq, dlq-topic,
  topic-pattern, regex subscription, partition, consumer group, kafka-flow-adapter.yaml,
  simple.kafka.notification, at-least-once, auto-commit, max-poll-records, traceparent,
  metadata, input.metadata, offset, message key,
  schema registry, confluent, avro, json schema, subject, version, schema.enabled, csfle,
  field level encryption, kms, aws kms, azure key vault, gcp kms]
---

# Kafka Flow Adapter

*Guide: route Kafka topics into Event Script flows, and publish events to Kafka, with the opt-in
`minimalist-kafka` library.*

> **At a glance**
>
> - **What** — `minimalist-kafka` is an opt-in library with two composable building blocks: an **inbound**
>   Kafka Flow Adapter that routes each topic (or regex-matched set of topics) into an Event Script flow (the
>   Kafka counterpart of `rest.yaml`), and an **outbound** notification function that publishes an event to
>   a topic.
> - **Config, not code** — Kafka client connection/security comes from external `kafka-producer.properties`
>   / `kafka-consumer.properties` templates; the YAML binds topics (literal or regex) to flows with optional
>   consumer group, partition pinning, a per-binding dead-letter topic, and a per-binding delivery mode.
>   Enterprise SASL/OAuth2/mTLS is configured, never coded.
> - **Reliable, with a throughput escape hatch** — at-least-once consume (commit-after-process) by default,
>   bounded retry then a per-binding dead-letter topic, and continuous W3C trace context across the Kafka
>   hop; a binding may opt into Kafka-native auto-commit for higher throughput instead.
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
    dlq-topic: 'incoming-orders-dlq'  # optional; no DLQ if omitted (failed messages dropped w/ ERROR)
  - topic: 'incoming-payments'
    flow: 'process-payment'
    partition: 0                      # optional
  - topic-pattern: 'events\.[a-z]{2}' # optional; regex subscribe instead of a literal 'topic'
    flow: 'process-region-event'
    group: 'region-events-group'      # required for topic-pattern bindings
  - topic: 'clickstream'
    flow: 'ingest-clickstream'
    auto-commit: true                 # optional; trades pod-death redelivery for throughput
    max-poll-records: 500             # optional; only meaningful with auto-commit
```

| Field | Required | Description |
|-------|----------|-------------|
| `topic` | one of `topic`/`topic-pattern` | Literal source Kafka topic. |
| `topic-pattern` | one of `topic`/`topic-pattern` | Regex subscription instead of a literal topic (see [pattern subscription](#pattern)). |
| `flow` | yes | Event Script flow id each message is routed into. |
| `group` | no (required for `topic-pattern`) | Consumer group id (see [consumer group](#group)). Defaults to `kafka-flow-adapter.<topic>` for a literal topic; no default exists for a pattern. |
| `partition` | no | Pins a single partition (see [partition pinning](#pinning)). Omit for group-managed assignment. Cannot be combined with `topic-pattern`. |
| `schema.enabled` | no | When `true`, decode the Confluent-framed value into a `Map` before routing it into the flow (see [Schema Registry](#schema)). Default `false` (raw `byte[]`). |
| `dlq-topic` | no | Pre-provisioned topic for exhausted messages (see [reliability](#reliability)). No DLQ if omitted. |
| `auto-commit` | no | When `true`, use Kafka-native auto-commit instead of the default manual commit-after-process (see [delivery mode](#delivery-mode)). Default `false`. |
| `max-poll-records` | no | Override the delivery mode's default poll batch size (1 for manual-commit, 500 for auto-commit). |

The file is read by `ConfigReader`, so **every value supports `${ENV_VAR:default}` substitution** — e.g.
`group: '${KAFKA_CONSUMER_GROUP:sales-order-group}'`. A malformed entry (missing `topic`/`topic-pattern`/
`flow`, both `topic` and `topic-pattern` set, an invalid regex, a `dlq-topic` that equals or matches its own
source, etc.) fails startup fast and loud rather than being silently skipped.

### Message dataset {#dataset}

Every message hands the flow a `Map` with three top-level objects — `input.body`, `input.header`, and
`input.metadata`:

| Field | Type | Description |
|-------|------|--------------|
| `body` | `byte[]` or `Map` | The message payload; a `Map` when [`schema.enabled`](#schema) decodes a Confluent-framed value, raw `byte[]` otherwise. |
| `header` | `Map<String,String>` | The record's Kafka headers, including `traceparent` (consumed for [trace continuity](#tracing)) and `cid` (correlation id) when the producer set them. |
| `metadata` | `Map<String,Object>` | The record's own envelope facts — `topic`, `partition`, `offset`, `timestamp`, and `key` (omitted when the record carries no key). |

`metadata.topic` and `metadata.partition` are the record's **actual** topic and partition — not the
binding's configured `topic`/`topic-pattern`. For a literal `topic` binding this is redundant (the flow
already knows the topic from its own YAML), but for a [`topic-pattern`](#pattern) binding it's the *only*
way a flow recovers which of the many matched topics a given message came from, since every matched topic
shares one `flow`. It's equally useful for a reprocessing flow bound to a `dlq-topic`: `metadata.topic` there
is the DLQ topic itself, while the `dlq.origin.topic` header (see [reliability](#reliability)) carries the
original source topic — together they let a reprocessor recover both "where this landed" and "where it came
from" without any framework-side rule/schema code.

Because `metadata` is just another field on `input`, a task's own `input:` mapping can pass it straight to
a composable function's parameter — no `model.*` relay needed. This is what makes
[`topic-pattern`](#pattern) practical for a "serving" function that must vary its behavior by the concrete
topic a message arrived on, even though every matched topic shares one `flow`:

```yaml
# in the first task of a topic-pattern flow, passed straight to the composable function
input:
  - 'input.metadata.topic -> topic'      # e.g. 'events.de' - the function decides per-topic behavior
  - 'input.metadata.partition -> partition'
  - 'input.body -> body'
process: 'topic.aware.dispatcher'
```

`model.*` is only needed when a *later* task (not the one receiving the message) needs the value — store it
once (`'input.metadata.topic -> model.source_topic'`) and reference `model.source_topic` from there on.

### Consumer group {#group}

`group` is the Kafka consumer group id, used **exactly as given**. Enterprise DevSecOps teams typically
provision topics, ACLs, and consumer groups administratively, so the library never decorates the value. For
a literal `topic` it defaults to `kafka-flow-adapter.<topic>` for convenience in dev/test; a `topic-pattern`
binding has no sensible default (a regex string is not a group id) and must set `group` explicitly. All
instances that share a group load-balance that binding's partitions; set it explicitly to your assigned
group in production.

### Partition pinning {#pinning}

When `partition` is present, the consumer **manually assigns** that single topic-partition instead of joining
the consumer group for dynamic assignment. This bypasses group rebalancing — the pinned consumer reads
exactly that partition — so you own the deployment model (one consumer per partition, or each pod pinning a
distinct partition via `partition: ${POD_PARTITION}`). Offsets still commit under the configured group.
Omit `partition` for normal group-managed consumption. Mutually exclusive with `topic-pattern` (below), since
manual assignment needs concrete topic-partitions up front.

### Pattern subscription {#pattern}

Set `topic-pattern` instead of `topic` to subscribe to every topic matching a regex, using Kafka's native
`subscribe(Pattern)`: the client tracks which topics currently match and adds/removes them from the
subscription automatically as matching topics are created — no adapter-side polling of topic metadata, no
restart needed when a new matching topic appears. All messages from every matched topic route into the same
`flow`.

```yaml
  - topic-pattern: 'events\.[a-z]{2}'   # matches events.de, events.fr, events.us, ...
    flow: 'process-region-event'
    group: 'region-events-group'        # required - no sensible default for a regex string
```

Two rules follow from this: `topic-pattern` cannot be combined with `partition` (manual assignment needs
concrete topic-partitions up front, which a pattern doesn't provide), and `group` must be set explicitly.
`dlq-topic`, if configured, must not itself match the pattern (see [reliability](#reliability)).

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
| Delivery semantics (consumer) | `enable.auto.commit` / `max.poll.records` — per-binding overlay (see [delivery mode](#delivery-mode)) | `auto.offset.reset` |
| Connection / security | — | `bootstrap.servers`, `security.protocol`, `sasl.*`, `ssl.*`, `acks` |

`bootstrap.servers` is template-only via `${KAFKA_BOOTSTRAP_SERVERS:127.0.0.1:9092}`. The byte[] wire
contract keeps the building blocks serializer-free; richer encodings layer on top via the
[Schema Registry integration](#schema) (JSON Schema / Avro), opt-in per binding.

## Reliability: delivery mode, retry, and dead-letter {#reliability}

### Delivery mode {#delivery-mode}

By default (`auto-commit: false`, or omitted) the consumer commits offsets **only after** the flow finishes
a message, one message at a time (`max.poll.records` defaults to `1`). If the instance crashes before the
commit, Kafka redelivers to a surviving instance in the group — the deliberate resilience-over-throughput
trade-off.

Set `auto-commit: true` on a binding to trade that guarantee for throughput: Kafka commits offsets on its
own periodic timer regardless of processing outcome, and `max.poll.records` defaults to `500` (still
overridable via `max-poll-records`). A message being processed when a pod dies may already be considered
committed and is **not** redelivered. Retry/dead-letter handling on flow failure is unaffected either way —
auto-commit only changes *when* Kafka considers the offset committed, not whether a failure is retried or
dead-lettered. Choose this per binding for high-volume topics (e.g. clickstream/telemetry) that can tolerate
occasional loss on crash in exchange for throughput; leave strict topics on the default.

A flow **succeeds** when it replies with status `200`. Any other status — or a thrown exception, including a
**timeout** when the flow does not reply within its own `ttl` — is a **failure**. (Kafka is asynchronous, so
unlike an HTTP entry the adapter has no inherent request timeout: the flow's `ttl` *is* the processing
deadline. There is no separate flow-timeout knob.)

### Retry and dead-letter

On a **failure**, the message is retried up to `kafka.flow.max.retries` times (with
`kafka.flow.retry.backoff.ms` between attempts), then written to the binding's configured `dlq-topic`:

- **One DLQ topic per binding**, not per concrete topic — a `topic-pattern` binding that matches many topics
  still has a single `dlq-topic` (or none). The same flow that consumes a matched topic can reprocess a
  dead-lettered message later regardless of which concrete topic it originated from; that provenance is
  preserved via the `dlq.origin.topic` header, so a shared DLQ isn't the "mixing source schemas" anti-pattern
  it would be for unrelated topics. `dlq-topic` must not equal the source `topic`, nor match `topic-pattern`,
  or a dead-lettered message would be re-consumed by the same binding and fail forever — the adapter rejects
  that configuration at startup.
- `dlq-topic` is **optional**. When omitted, a message that exhausts retries is dropped with a logged
  `ERROR` instead of being dead-lettered — the same fallback used when the DLQ write itself fails (below).
- The DLQ write is **confirmed** (it blocks on broker acknowledgement, bounded by `kafka.dlq.timeout.ms`);
  on success the offset commits (or, in auto-commit mode, is left to Kafka's own timer as usual).
- **DLQ topics must be pre-provisioned** (Kafka auto-creation is off in production). The original record's
  headers are preserved, plus `dlq.origin.topic` and `dlq.error`.

> **When there's no DLQ, or the DLQ write itself fails (data loss).** A failed write to the DLQ is an
> *exception of an exception* with no further fallback. Blocking the partition to retry forever would re-run
> the failing flow and re-attempt the failing DLQ write indefinitely — a self-sustaining **recovery storm**
> (a known cause of prolonged outages). So the adapter instead logs a loud `ERROR` and **commits** (in
> manual-commit mode), deliberately **dropping that one message** to keep the partition live. This is a
> conscious data-loss trade-off; a planned improvement is a classic resilience **alternative path** —
> persisting the record to a durable store for later replay instead of dropping it.

**Reprocessing** (read the DLQ topic → fix → replay) is business-domain logic and is intentionally out of
scope: the library guarantees durable capture (when a `dlq-topic` is configured and reachable), not replay.

## Outbound: publishing to Kafka {#outbound}

`simple.kafka.notification` is a composable function that publishes an event to a topic. Send it an
`EventEnvelope` with a `topic` header (required), an optional `partition` header, a `byte[]` body, and any
other headers (forwarded as Kafka headers):

```java
po.send(new EventEnvelope().setTo("simple.kafka.notification")
        .setHeader("topic", "outgoing-events")
        .setHeader("cid", businessCorrelationId)
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
library (not a reinvented codec). **JSON Schema and Avro** are supported.

> **Protobuf is not currently supported.** It was implemented and demoed in an earlier development phase but
> removed before its first release: Confluent's `kafka-protobuf-provider` depends on
> `com.squareup.wire:wire-runtime-jvm`, a **discontinued** artifact carrying an unpatched denial-of-service CVE
> ([CVE-2026-45799 / GHSA-7xpr-hc2w-34m9](https://github.com/square/wire/security/advisories/GHSA-7xpr-hc2w-34m9))
> with no fix available anywhere in that coordinate — Wire's maintainers will not patch it, and Confluent has
> not adopted the renamed `wire-runtime` replacement as of `kafka-protobuf-provider:8.3.0`. This is a tracked
> backlog item, not an abandoned one: it gets re-wired once Confluent moves, or sooner for a specific field
> installation that explicitly needs Protobuf and accepts the residual risk. `SchemaType.PROTOBUF` is still
> recognized internally so a misconfigured attempt fails clearly (`UnsupportedOperationException`), not silently.

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
| `subject` | The registry **subject** to serialize against. The schema must be **pre-registered**; the producer resolves the subject to a global schema id (and its type — `JSON` or `AVRO`) from the registry and never registers. |
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

### CSFLE (Client-Side Field Level Encryption) {#csfle}

`minimalist-kafka` supports [Confluent CSFLE](https://docs.confluent.io/cloud/current/security/encrypt/csfle/overview.html)
by **delegation** — the framework adds no encryption/rule logic of its own. A schema's `ruleSet` (its
`ENCRYPT` rules, tagging fields for encryption) travels with the schema itself through the *same*
`getSchemaById` lookup the producer/consumer already use, and Confluent's own serializer/deserializer run
those rules during the *same* `use.schema.id` + envelope serialize/deserialize call this library already
makes. So there is no separate "encrypted" code path to opt into — CSFLE activates the moment (a) the
encryption executor + a KMS driver are on the classpath and (b) their configuration is present; everything
else — resolving keys, encrypting tagged fields on write, decrypting on read — is Confluent's serializer/
deserializer doing exactly what it would do in any Confluent-based application.

**1. Dependencies.** `system/minimalist-kafka/pom.xml` declares:

- `io.confluent:kafka-schema-registry-client-encryption` — the field-encryption rule executor (auto-discovered
  via `ServiceLoader`; no explicit `rule.executors` config needed). This is a normal compile dependency, so an
  app that depends on `minimalist-kafka` **inherits the executor transitively** — nothing to add.
- **Exactly one** cloud KMS driver — **which your application must supply itself.** In this module's own POM the
  AWS driver (`io.confluent:kafka-schema-registry-client-encryption-aws`) is uncommented as the default/template
  (Azure and GCP are present alongside it, commented out). But that dependency is marked Maven `<optional>true</optional>`,
  so it is **not inherited transitively** by a downstream consumer of the published artifact. Your app must
  therefore declare exactly one KMS driver appropriate to its environment — AWS, Azure, GCP, or (for tests) the
  local Tink driver — on its own classpath. A field installation configures one KMS vendor, not several.

> Why optional rather than a default AWS dependency for everyone? Forcing the AWS SDK onto every consumer would
> be wrong for Azure/GCP and non-CSFLE users. So `minimalist-kafka` ships the vendor-neutral executor and lets
> the application pick its one KMS driver. A common symptom of skipping this: a subject configured with a CSFLE
> rule fails at runtime because no KMS driver is on the app's classpath.

**2. The `ENCRYPT` rule — and its KEK/KMS identity — is per-subject, set on the schema, not in this app's
config.** When a subject is registered with an `ENCRYPT` rule tagging a field (e.g. `confluent:tags: ["PII"]`
inline in the schema, or via a `Metadata` tags map), that rule's own parameters —
`encrypt.kek.name`/`encrypt.kms.type`/`encrypt.kms.key.id` — say *which* key encrypts *that* subject's tagged
fields. This is deliberate: Confluent's rule executor resolves these from the registered rule (or the
schema's `Metadata`) and **never** from this library's serde config, so different subjects can use different
KEKs/vendors without any code or config change here. Whoever registers/governs the schema — CI, an admin
tool — owns this binding. A schema with no `ENCRYPT` rule serializes exactly as before (plaintext); CSFLE is
per-subject, never a single global on/off switch.

**3. What *does* go in `application.properties` — the `schema.registry.serde.*` pass-through.** This is
reserved for genuinely **global, app-level** settings the KMS *driver* itself needs (not per-subject key
identity): typically nothing at all if you rely on your cloud's default credential chain (e.g. an IAM role for
AWS), or explicit driver credentials if you don't:

```properties
# Only needed if you are not relying on the AWS SDK default credential chain (an IAM role, etc.):
schema.registry.serde.access.key.id=${AWS_ACCESS_KEY_ID}
schema.registry.serde.secret.access.key=${AWS_SECRET_ACCESS_KEY}
```

Any property under this prefix is merged, prefix stripped, into **both** the serializer's and the
deserializer's Confluent config map (decrypt is symmetric, so both directions need the same driver
credentials). It is a generic pass-through — a KMS driver's own config keys (AWS's `access.key.id`/
`secret.access.key`/`profile`/`role.arn`, or Azure's/GCP's equivalents) flow through with no code change here.

**4. Wire format and consumer are unchanged.** The frame is still `[magic 0x00][4-byte global schema id]
[payload]` — CSFLE only changes the *value* of the tagged fields inside that payload to ciphertext (with
embedded DEK metadata Confluent's deserializer reads to decrypt). DLQ handling, tracing, and the
`schema.enabled` consumer binding are unaffected.

**5. Not covered by the standalone mock.** [`schema-registry-standalone`](schema-registry-mock.md) is a
plaintext dev tool with no `ruleSet`/KMS support (deliberately — see its own docs). Test/demo CSFLE against a
real Confluent Schema Registry and a real (or local) KMS.

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

- **One subject-driven path, two formats.** The producer and consumer are type-generic; only the `subject`
  (and the registered schema behind it) differ — the producer reads the schema type from the registry, so the
  flow never names it. JSON Schema is *open* (`additionalProperties`), while Avro records are *closed-shape* —
  a message must match the declared fields, and a non-schema field is dropped on the wire. Avro applies
  declared field defaults for absent fields, and decodes to a generic record (no generated classes), rendered
  to a `Map`.
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
- **Worked example.** The [sync-over-async demo](sync-over-async.md) runs the same end-to-end flow over both
  formats (`json-topic-1/2`, `avro-topic-1/2`) alongside the raw `byte[]` path.

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
| `schema.registry.url` | — | Confluent Schema Registry URL; unset = [schema features](#schema) off (raw `byte[]`). |
| `schema.registry.cache.ttl` | `30m` | TTL for the in-memory (`ManagedCache`) schema cache (by id); positive results only; cleared at startup. |

## See also

- [Sync-over-Async](sync-over-async.md) — cross-pod synchronous request/response built on this library plus a Redis return route.
- [Schema Registry mock](schema-registry-mock.md) — the local Confluent-compatible registry the [schema integration](#schema) talks to.
- [Configuration Reference](configuration-reference.md#kafka-flow-adapter) — every Kafka flow-adapter key.
- [Observability](observability.md) — how trace context stays continuous across the Kafka hop.
- [Minimalist Service Mesh](service-mesh.md) — the *different* `cloud.connector=kafka` event mesh.
