---
title: Schema Registry Mock
description: A lightweight, standalone Schema Registry mock for local development
---

# Standalone Schema Registry Mock

## At a glance

The `schema-registry-standalone` module is a lightweight, zero-dependency mock of the Confluent Schema Registry. It runs as a `MainApplication` on the `platform-core` runtime and perfectly mimics the HTTP API required by `KafkaAvroSerializer` and `KafkaJsonSchemaSerializer`.

* **Location:** `helpers/schema-registry-standalone`
* **Default Port:** `8081` (Confluent's default)
* **Storage:** In-memory, persisted as one file per schema id (`<id>.json`, e.g. `1.json`) under a configurable directory (`schema.registry.data.store`, default `/tmp/schema-registry`)
* **Supported Types:** Avro (default), JSON Schema, Protobuf — the mock is schema-language-agnostic (it stores/serves an opaque schema string regardless of type), so it can serve any Confluent client, including a Protobuf one. **`minimalist-kafka`'s own client integration only consumes JSON Schema and Avro** — see the [Kafka Flow Adapter guide](kafka-flow-adapter.md#schema) for why Protobuf support was removed.

## Why use this?

Confluent's serializers require a real HTTP endpoint to register and retrieve schemas (they cannot easily use a Java-level mock object across pods). Running this standalone application provides that HTTP endpoint locally without the heavy footprint of running Kafka and the full Confluent Schema Registry in Docker.

It is built on `platform-core` alone — the built-in reactive HTTP server and REST automation provide the
endpoints, so there is no dependency on `rest-spring`. Each endpoint is wired in `rest.yaml` directly to a
composable function that receives the raw `AsyncHttpRequest` (no flow).

## Running the Standalone Server

First, package the project:
```bash
mvn clean package -f helpers/schema-registry-standalone/pom.xml
```

Then, run the server:

> **Note**: `x.y.z` denotes the current Mercury version shown in the root `pom.xml`.

```bash
java -jar helpers/schema-registry-standalone/target/schema-registry-standalone-x.y.z.jar
```

If you need to change the port, provide it as a system property **before** `-jar` (the built-in HTTP
server reads `rest.server.port`):
```bash
java -Drest.server.port=8082 -jar helpers/schema-registry-standalone/target/schema-registry-standalone-x.y.z.jar
```

## How to use it in your application

Configure your Kafka producer and consumer properties to point to the local server.

In your application's `application.properties` (or `kafka-producer.properties`/`kafka-consumer.properties`):
```properties
schema.registry.url=http://127.0.0.1:8081
```

If you are using Avro:
```properties
key.serializer=org.apache.kafka.common.serialization.StringSerializer
value.serializer=io.confluent.kafka.serializers.KafkaAvroSerializer
```

If you are using JSON Schema:
```properties
key.serializer=org.apache.kafka.common.serialization.StringSerializer
value.serializer=io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer
```

## Endpoints Implemented

The standalone server implements exactly what the Confluent clients require:

1. **`GET /`** (Health Check) — returns `{}`.
2. **`POST /subjects/{subject}/versions`** — accepts `{"schema": "...", "schemaType": "AVRO|JSON|PROTOBUF"}` and returns `{"id": 101}`.
3. **`GET /schemas/ids/{id}`** — retrieves the schema (and its type, for non-Avro) by ID.

### The `schema` field is a string

Per the Confluent spec, the schema travels as an **escaped string** inside the request/response body —
`{"schema": "{\"type\":\"record\",...}"}`, not a nested JSON object. The registry treats a schema as an
opaque string (it is schema-language-agnostic), so the same envelope carries Avro, JSON Schema, and
Protobuf. `schemaType` defaults to `AVRO` and is omitted from the `GET` response for Avro.

The schema id is **global and content-based**: registering identical content (even under a different
subject) returns the same id.

### Error responses

Errors use Confluent's shape — `{"error_code": <int>, "message": "..."}` — with the matching HTTP status:

| Condition | HTTP | `error_code` |
|-----------|------|--------------|
| Schema id not found (`GET /schemas/ids/{id}`) | 404 | 40403 |
| Subject missing | 404 | 40401 |
| Missing or malformed schema (Avro/JSON must be well-formed JSON) | 422 | 42201 |

## State Management

Schemas are persisted as **one file per id** (`<id>.json`, holding `{"schema": ..., "schemaType": ...}`)
under a configurable directory, so that if you restart the mock the schema IDs assigned to your applications
remain valid — preventing deserialization errors during iterative local development.

Because storage is one file per id, you can **drop a new schema file (e.g. `10.json`) into the directory
while the server is running** and it is picked up on demand by the next `GET /schemas/ids/10` — no restart
needed. (A file present at boot is loaded then; a file added later is loaded lazily on first lookup.)

The directory is set with the `schema.registry.data.store` config key (in `application.properties`),
defaulting to a transient `/tmp/schema-registry`:

```properties
schema.registry.data.store=/tmp/schema-registry
```

Point it at a **durable** location to keep ids stable across reboots — for example your home directory —
and override it at runtime with a JVM flag:

```bash
java -Dschema.registry.data.store=$HOME/schema-registry \
     -jar helpers/schema-registry-standalone/target/schema-registry-standalone-x.y.z.jar
```

Making the path configurable also sidesteps filesystem-permission issues: you choose a directory your
process can write to.

## Try it: the worked example

[`examples/schema-registry-demo`](https://github.com/Accenture/mercury-composable/tree/main/examples/schema-registry-demo)
drives the server with `curl` and two tiny zero-dependency Node scripts (`register-schema.mjs`,
`get-schema.mjs`) plus copy-and-edit test-data schema files — no application to build.

## See also
* [Minimalist Kafka Flow Adapter](kafka-flow-adapter.md)
