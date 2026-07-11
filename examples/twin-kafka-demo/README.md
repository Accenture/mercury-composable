# twin-kafka-demo — profile management bridged across two Kafka clusters

A worked example for the **twin-kafka** module: a hypothetical profile management service whose
requests enter through HTTP on the **on-prem** side, cross a **dual-cluster bridge**, and are served by a
system-of-record on the **cloud** side — with the trace id and business correlation id staying continuous
across two Kafka clusters and three applications.

```
curl ──> [rest app] ──OP_PROFILE_REQUEST──> [bridge app] ──C_PROFILE_REQUEST──> [sor app]
          (202 ack)     on-prem, JSON Schema    twin-kafka      cloud, plain JSON      /tmp store
                                                                                          │
listener <──OP_PROFILE_RESPONSE── [bridge app] <──C_PROFILE_RESPONSE──────────────────────┘
 (node)      on-prem, JSON Schema
```

The two clusters are deliberately **asymmetric** — the on-prem cluster governs messages with a Confluent
Schema Registry (JSON Schema wire format); the cloud cluster carries plain JSON bytes. This mirrors the
real-world topologies the twin-kafka module was designed for (on-prem Confluent + cloud open-source Kafka
or Azure Event Hubs, and every combination in between): the Schema Registry is **per-cluster and
optional**, and the bridge **decodes and re-encodes** rather than relaying framed bytes, because a schema
id is only meaningful to the registry that issued it.

## The three roles (one jar)

| Profile | Side | What it does | Kafka footprint |
|---------|------|--------------|-----------------|
| `rest`  | on-prem | REST edge: `GET`/`POST`/`DELETE /api/profile` → immediate **202** ack, then publishes the Request to `OP_PROFILE_REQUEST` (subject `op-profile-request`) | minimalist-kafka (producer only, with registry) |
| `bridge` | both | `OP_PROFILE_REQUEST` → decode → `C_PROFILE_REQUEST` (plain JSON); `C_PROFILE_RESPONSE` → re-encode (subject `op-profile-response`) → `OP_PROFILE_RESPONSE`. **Both flows are pure YAML — no Java.** | twin-kafka (primary + secondary) |
| `sor`  | cloud | System-of-record: applies READ / UPSERT / DELETE to the temp store `/tmp/twin-kafka-demo` (one JSON file per id; 5 workers) and publishes the outcome to `C_PROFILE_RESPONSE`. Errors travel as data — a READ of a missing id replies `not found`. | minimalist-kafka (its "primary" IS the cloud broker — no registry) |

> **Zero-weight principle.** In a real deployment only the bridge carries the twin-kafka dependency; the
> rest and sor roles are plain minimalist-kafka apps (the sor's bootstrap simply points at the cloud
> broker). This demo packages all three roles in ONE jar for convenience — the secondary flow adapter
> starts only in the bridge profile because only `application-bridge.properties` sets
> `yaml.secondary.kafka.flow.adapter`.

## Trace and correlation continuity

All roles configure explicit Kafka header names (the 4.8.0 configurable headers):

```properties
kafka.trace.id.header=X-Trace-Id
kafka.correlation.id.header=X-Correlation-Id
```

`simple.kafka.notification` / `secondary.kafka.notification` stamp both automatically on every publish
(no flow mapping needed), and each flow adapter continues the trace and carries the business
correlation id (`model.cid`) on consume. The node listener at the end of the chain prints the very ids
minted at the HTTP edge — send `-H 'X-Correlation-Id: <your id>'` with curl and watch it come back after
two clusters and three apps.

## Build

```shell
# from the repository root: install the framework libraries first (skip if already installed)
mvn -q clean install -DskipTests

cd examples/twin-kafka-demo
mvn clean package
cd node && npm install && cd ..
```

## Run (the multi-terminal method)

All commands from the repository root. `x.y.z` is the current version.

### Terminal A — both Kafka brokers (one JVM)

```shell
cd helpers/kafka-standalone && java -Ddual.servers=true -jar target/kafka-standalone-x.y.z-exec.jar
```

The on-prem broker listens on **9092**, the cloud broker on **8092**.

### Terminal B — the on-prem Schema Registry (seeded)

The producer never registers schemas at runtime (schemas are governed artifacts), so seed the registry's
store with the demo's two subjects before starting it:

```shell
mkdir -p /tmp/schema-registry
cp examples/twin-kafka-demo/registry/*.json /tmp/schema-registry/
cd helpers/schema-registry-standalone && java -jar target/schema-registry-standalone-x.y.z.jar
```

Subjects `op-profile-request` (seed `11.json`) and `op-profile-response` (seed `12.json`), port **8081**.

### Terminal C — create the topics (both clusters, 10 partitions)

```shell
cd examples/twin-kafka-demo/node && node create_topic.js
```

> Run this **before** the apps — otherwise the flow adapter auto-creates the topics at 1 partition.

### Terminals D, E, F — the three apps

```shell
java -Dspring.profiles.active=sor    -jar examples/twin-kafka-demo/target/twin-kafka-demo-x.y.z.jar
java -Dspring.profiles.active=bridge -jar examples/twin-kafka-demo/target/twin-kafka-demo-x.y.z.jar
java -Dspring.profiles.active=rest   -jar examples/twin-kafka-demo/target/twin-kafka-demo-x.y.z.jar
```

The rest app serves HTTP on **8500**.

### Terminal G — the response listener

```shell
cd examples/twin-kafka-demo/node && node listen_response.js
```

### Terminal H — drive it with curl

```shell
# CREATE/UPDATE (UPSERT)
curl -sS -X POST http://127.0.0.1:8500/api/profile \
     -H 'content-type: application/json' -H 'X-Correlation-Id: order-001' \
     -d '{"id":100,"name":"Peter Parker","address":"20 Ingram Street, Queens","telephone":"212-555-1212"}'

# READ
curl -sS -H 'X-Correlation-Id: order-002' http://127.0.0.1:8500/api/profile/100

# DELETE
curl -sS -X DELETE -H 'X-Correlation-Id: order-003' http://127.0.0.1:8500/api/profile/100

# READ again -> the system of record replies "not found" (errors travel as data)
curl -sS -H 'X-Correlation-Id: order-004' http://127.0.0.1:8500/api/profile/100
```

Each curl returns the immediate ack (**202**):

```json
{"command":"UPSERT","id":100,"originator":"HTTP_REQUEST","message":"Request accepted for processing"}
```

…and a moment later the listener prints the system-of-record's outcome, e.g.:

```
[2026-07-11T...Z] topic=OP_PROFILE_RESPONSE partition=6 schemaId=12
  X-Trace-Id=4f0e2a...
  X-Correlation-Id=order-001
  {"command":"UPSERT","id":100,"profile":{...},"originator":"SYSTEM_OF_RECORDS","message":"Profile 100 saved"}
```

Note the `X-Correlation-Id` you sent from curl and the `X-Trace-Id` minted at the HTTP edge — both
continuous across the on-prem cluster, the bridge, the cloud cluster, the system of record, and back.

## Message shapes

**Request** (subject `op-profile-request`, on-prem; plain JSON on the cloud):

```json
{ "command": "READ | UPSERT | DELETE", "id": 100, "profile": { "id", "name", "address", "telephone" } }
```

`profile` is present for UPSERT only.

**Response** (subject `op-profile-response`, on-prem; plain JSON on the cloud):

```json
{ "command": "...", "id": 100, "profile": { ... }, "originator": "HTTP_REQUEST | SYSTEM_OF_RECORDS",
  "message": "some relevant status message" }
```

The immediate HTTP ack uses `originator: HTTP_REQUEST`; the system-of-record's outcome uses
`originator: SYSTEM_OF_RECORDS`.

## Design notes

- **Fully asynchronous.** The HTTP request is acknowledged immediately (`execution: response`); the
  outcome is observed on the response topic. No sync-over-async (see the sync-over-async-demo for that
  pattern) and no field encryption (see composable-example).
- **No ordering assumptions.** The system-of-record runs 5 workers and messages spread across the 10
  partitions by the producer's default (sticky) partitioner. At demo pace consecutive messages often
  share a batch and land on the same partition; the spread shows under real load.
- **The bridge is pure YAML.** Consuming with `schema.enabled: true` hands the flow a decoded Map;
  publishing with a `subject` header re-encodes. The two bridge flows are just topic-to-topic mappings —
  `bridge-request.yml` and `bridge-response.yml`.
- **Maps become JSON bytes with `:binary`.** The notification functions take `byte[]`, so wherever a flow
  holds a Map (a schema-decoded consume, or a task result), the type-qualifier mapping
  `model.x:binary -> *` serializes it to its JSON bytes — still pure YAML, no conversion task.
- **The Confluent frame is JSON-friendly.** After the 5-byte frame (magic byte + schema id), a JSON
  Schema value is the plain UTF-8 JSON string — that is why `listen_response.js` can print it directly
  (an Avro value would be Avro binary).
- **DLQ stays default-on** but is not demonstrated here; see the twin-kafka guide for the per-cluster
  dead-letter behavior.
