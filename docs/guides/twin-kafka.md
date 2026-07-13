---
title: Twin Kafka (dual-cluster bridge)
summary: The opt-in twin-kafka library - connect one application to a SECOND Kafka cluster on top
  of minimalist-kafka, for bridging between two clusters (e.g. on-prem Apache Kafka and cloud
  Confluent Kafka), with a secondary notification function, an optional secondary flow adapter, and
  an optional per-cluster Schema Registry.
layer: operate
audience: [developer, ai-agent]
keywords: [twin-kafka, dual cluster, bridge, secondary kafka, secondary.kafka.notification,
  yaml.secondary.kafka.flow.adapter, secondary-kafka-producer.properties, on-prem, cloud,
  confluent, apache kafka, schema registry, kafka-standalone, dual.servers]
related:
  - guides/kafka-flow-adapter.md
  - guides/configuration-reference.md
  - guides/reserved-names-and-headers.md
---

# Twin Kafka (dual-cluster bridge)

*Guide: connect an application to two Kafka clusters and bridge messages between them with the
opt-in `twin-kafka` library.*

> **At a glance**
>
> - **What** - `twin-kafka` adds a SECOND Kafka cluster to an application built on
>   [minimalist-kafka](kafka-flow-adapter.md): a **secondary notification function**
>   (`secondary.kafka.notification`), an optional **secondary flow adapter**
>   (`yaml.secondary.kafka.flow.adapter`), and an optional **secondary Schema Registry** - each the
>   exact counterpart of its primary sibling.
> - **Why a separate module** - dual-cluster connectivity is a special case (typically a bridge
>   between an on-prem and a cloud cluster). Keeping it out of minimalist-kafka means every ordinary
>   single-cluster application stays on the lightweight library; only the bridge application adds
>   this jar.
> - **A bridge is just a flow** - consume from one cluster's adapter, publish through the other
>   cluster's notification function. Trace continuity and business correlation-id propagation across
>   both Kafka hops come from the existing machinery.
> - **For** developers building a bridge (or any app that must speak to two Kafka clusters).

## How it works {#how}

`twin-kafka` depends on minimalist-kafka and reuses its machinery through explicit extension seams -
the flow adapter, publisher, retry/DLQ handling, templates mechanics, and Confluent codec are the
same classes, instantiated a second time against the second cluster. The two runtimes are fully
separate: dead letters from a secondary binding are written to the secondary cluster, and each
cluster's schema-id cache is isolated (Confluent global ids are only unique within one registry).

| Concern | Primary cluster (minimalist-kafka) | Secondary cluster (twin-kafka) |
|---|---|---|
| Publish function | `simple.kafka.notification` | `secondary.kafka.notification` |
| Inbound bindings | `yaml.kafka.flow.adapter` | `yaml.secondary.kafka.flow.adapter` |
| Producer template | `kafka-producer.properties` | `secondary-kafka-producer.properties` |
| Consumer template | `kafka-consumer.properties` | `secondary-kafka-consumer.properties` |
| Schema Registry (optional) | `schema.registry.url` + `schema-registry.properties` | `secondary.schema.registry.url` + `secondary-schema-registry.properties` |
| Outbound header names | `kafka.correlation.id.header` / `kafka.trace.id.header` | `secondary.kafka.correlation.id.header` / `secondary.kafka.trace.id.header` (fall back to the globals) |

All secondary templates follow the same mechanics as the primary ones: loaded from the bundled
classpath template by default, `${ENV_VAR:default}` substitution, and OAuth token endpoint URLs
auto-registered on the JVM allow-list. Externalization of configuration is opt-in - point the
location key at a file rendered by the devops pipeline (e.g. `file:/tmp/config/...`), optionally
with a classpath fallback as a comma-separated list. The default bootstrap is
`${SECONDARY_KAFKA_BOOTSTRAP_SERVERS:127.0.0.1:8092}` - matching the
[local dual-broker helper](#local-dev) below.

## The bridge pattern {#bridge}

Bridge on-prem (primary) to cloud (secondary) with one flow per direction - no bridge code, just
flow YAML. Inbound bindings on the source cluster:

```yaml
# kafka-flow-adapter.yaml (primary = on-prem)
consumer:
  - topic: 'orders.onprem'
    flow: 'bridge-to-cloud'
    group: 'bridge-up-group'
```

The bridge flow republishes through the other cluster's notification function, carrying the business
correlation-id (`model.cid`, seeded by the adapter) and stamping a fresh W3C `traceparent`:

```yaml
# bridge-to-cloud.yml
first.task: 'secondary.kafka.notification'
tasks:
  - input:
      - 'input.body -> *'
      - 'text(orders.cloud) -> header.topic'
      - 'model.cid -> header.cid'
    process: 'secondary.kafka.notification'
    output: []
    description: 'Republish the on-prem message to the cloud cluster'
    execution: end
```

The reverse direction mirrors it: a binding in `yaml.secondary.kafka.flow.adapter` routes the cloud
topic into a flow that publishes through `simple.kafka.notification`. Because each hop is a normal
flow, the bridge is observable (continuous trace across both clusters), governable (per-binding
retry + dead-letter on the *originating* cluster), and extensible (enrich/filter/transform tasks can
sit between consume and republish).

## Asymmetric Schema Registry {#asymmetric-registry}

The Schema Registry is **optional and per-cluster**. A common real-world topology pairs an on-prem
**Apache Kafka** (no registry, raw byte[]) with a cloud **Confluent** cluster (with a registry):
leave `schema.registry.url` unset and set only `secondary.schema.registry.url`. Subject-driven
Confluent serialization (`subject` header on `secondary.kafka.notification`) and `schema.enabled`
decoding on secondary bindings then work against the secondary registry only, while the primary side
keeps the minimalist raw-byte[] behavior. Each side fails fast with its own config key named in the
error if a subject is used where no registry is configured.

## Deployment topologies {#topologies}

Because connection/security is entirely template-driven and the registry is independently optional
per cluster, common dual-cluster pairings are configuration-only:

| Topology | Primary | Secondary | Configuration notes |
|---|---|---|---|
| **Apache + Confluent** | on-prem Apache Kafka (no registry) | Confluent Kafka with registry | The [asymmetric topology](#asymmetric-registry) above - set only `secondary.schema.registry.url`. This exact pairing is proven end-to-end by the module's test suite. |
| **Confluent + Confluent** | Confluent with registry 1 | Confluent with registry 2 | Set both registry URLs. The two registries are fully independent - separate auth templates (even two different OAuth identity providers) and separate schema-id caches. See the [id-translation note](#id-translation) below. |
| **Apache + Azure Event Hubs** | on-prem Apache Kafka | Event Hubs Kafka endpoint | Kafka-protocol compatible via the secondary templates: `SASL_SSL` with `PLAIN` (`$ConnectionString`) or `OAUTHBEARER` (Entra ID - the token URL is auto-allow-listed). Leave `secondary.schema.registry.url` unset (Azure Schema Registry is not Confluent-protocol compatible) and **pre-provision the event hubs** - Event Hubs does not honor Kafka topic auto-creation. Consumer groups, manual commit-after-process, and record headers (which carry `traceparent` and the correlation-id) are all supported by the Event Hubs Kafka endpoint. |

The design is symmetric: each pairing also works with the roles swapped - "primary" is simply the
cluster configured through minimalist-kafka's base templates.

### Bridging framed payloads between two registries {#id-translation}

Confluent global schema ids are only unique **within one registry**, and the bridge does not
translate ids. To bridge a Confluent-framed message between two registries, let the flow
decode-and-re-encode: `schema.enabled: true` on the inbound binding (decode against the source
registry) and a `subject` header on the outbound notification (re-frame against the target
registry's own id for that subject). Do **not** relay the raw framed bytes without `schema.enabled` -
the embedded id would be meaningless on the other registry.

## Local development: one helper, two brokers {#local-dev}

The [`kafka-standalone`](kafka-flow-adapter.md#client-config) helper can emulate the dual-cluster
topology in one process - set `dual.servers=true` (in its `application.properties` or as
`-Ddual.servers=true`) and it starts broker 1 on `127.0.0.1:9092` and broker 2 on `127.0.0.1:8092`
with separate log directories:

```shell
cd helpers/kafka-standalone && java -Ddual.servers=true -jar target/kafka-standalone-x.y.z-exec.jar
```

The shipped templates' defaults (`127.0.0.1:9092` primary, `127.0.0.1:8092` secondary) line up with
this, so a bridge application runs locally with zero connection configuration.

For a complete runnable walkthrough — an HTTP edge, a pure-YAML bridge, and a system-of-record
crossing the two emulated clusters with trace/correlation continuity — see the
[`twin-kafka-demo`](https://github.com/Accenture/mercury-composable/tree/main/examples/twin-kafka-demo) worked example.

## Configuration keys {#config}

| Key | Default | Description |
|-----|---------|-------------|
| `yaml.secondary.kafka.flow.adapter` | - | Secondary adapter config location; unset = secondary inbound adapter off. |
| `secondary.kafka.producer.properties` | `classpath:/secondary-kafka-producer.properties` | Secondary producer template location. Set to an external file path (or explicit fallback list) to externalize. |
| `secondary.kafka.consumer.properties` | `classpath:/secondary-kafka-consumer.properties` | Secondary consumer template location. Set to an external file path (or explicit fallback list) to externalize. |
| `secondary.schema.registry.url` | - | Secondary cluster's Schema Registry URL; unset = schema features off on that cluster. |
| `secondary.schema.registry.properties` | `classpath:/secondary-schema-registry.properties` | Secondary registry client template - auth/SSL passed verbatim to the Confluent client. Set to an external file path (or explicit fallback list) to externalize. |
| `secondary.schema.registry.cache.ttl` | `30m` | Secondary schema cache TTL (the cache is separate from the primary's - schema ids are per-registry). |
| `secondary.kafka.correlation.id.header` | falls back to `kafka.correlation.id.header` (`cid`) | Outbound correlation-id header on the secondary cluster. |
| `secondary.kafka.trace.id.header` | falls back to `kafka.trace.id.header` (unset) | Optional outbound trace-id header on the secondary cluster. |

The retry/dead-letter tuning keys (`kafka.dlq.timeout.ms`, `kafka.flow.max.retries`,
`kafka.flow.retry.backoff.ms`) are application-level policy shared by both adapters. Per-binding
options in the secondary adapter YAML are identical to the [primary's](kafka-flow-adapter.md#adapter-yaml) -
including `schema.enabled`, `dlq-topic`, and the `trace.id.header` / `correlation.id.header`
impedance-matching overrides.

## See also {#see-also}

- [twin-kafka-demo](https://github.com/Accenture/mercury-composable/tree/main/examples/twin-kafka-demo) - runnable worked example of the bridge pattern.
- [Kafka Flow Adapter](kafka-flow-adapter.md) - the foundation library this module extends.
- [Configuration Reference](configuration-reference.md#kafka-flow-adapter) - every key in one place.
- [Reserved names and headers](reserved-names-and-headers.md) - trace/correlation header conventions.
