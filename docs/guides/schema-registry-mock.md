---
title: Schema Registry Mock
description: A lightweight, standalone Schema Registry mock for local development
---

# Standalone Schema Registry Mock

## At a glance

The `schema-registry-standalone` module is a lightweight, zero-dependency mock of the Confluent Schema Registry. It runs as a `MainApplication` on the `platform-core` runtime and perfectly mimics the HTTP API required by `KafkaAvroSerializer` and `KafkaJsonSchemaSerializer`.

* **Location:** `helpers/schema-registry-standalone`
* **Default Port:** `8081` (Confluent's default)
* **Storage:** In-memory, backed by a JSON file at `/tmp/mini-schema-registry/schemas.json`
* **Supported Types:** Avro (default), JSON Schema, Protobuf.

## Why use this?

Confluent's serializers require a real HTTP endpoint to register and retrieve schemas (they cannot easily use a Java-level mock object across pods). Running this standalone application provides that HTTP endpoint locally without the heavy footprint of running Zookeeper, Kafka, and the full Confluent Schema Registry in Docker.

## Running the Standalone Server

First, compile the project:
```bash
mvn clean install -f helpers/schema-registry-standalone/pom.xml
```

Then, run the server:
```bash
java -jar helpers/schema-registry-standalone/target/schema-registry-standalone-4.5.0.jar
```

If you need to change the port, provide it as a system property:
```bash
java -Dserver.port=8082 -jar helpers/schema-registry-standalone/target/schema-registry-standalone-4.5.0.jar
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

1. **`GET /`** (Health Check) - returns `{}`.
2. **`POST /subjects/{subject}/versions`** - accepts `{"schema": "...", "schemaType": "JSON"}` and returns `{"id": 101}`.
3. **`GET /schemas/ids/{id}`** - retrieves the schema and its type by ID.

## State Management

Schemas are persisted to `/tmp/mini-schema-registry/schemas.json`. This ensures that if you restart the mock server, the schema IDs assigned to your applications remain valid, preventing deserialization errors during iterative local development.

## See also
* [Minimalist Kafka Flow Adapter](kafka-flow-adapter.md)
