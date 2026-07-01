# Standalone Schema Registry server

A convenient application to run a **Confluent-compatible Schema Registry for local development and testing** —
with **no Docker image required**. It implements the subset of the Confluent Schema Registry REST API that the
Confluent serializers/deserializers actually call, for **Avro, JSON Schema, and Protobuf**, on the built-in
platform-core reactive HTTP server (no `rest-spring`).

> For development and testing only — **not** for production use.

This is a sibling of [`kafka-standalone`](../kafka-standalone) and [`redis-standalone`](../redis-standalone)
under `helpers/`: together they give you a local Kafka broker, a local Redis server, and a local Schema
Registry without external infrastructure. It backs the schema features of
[`minimalist-kafka`](../../system/minimalist-kafka) and the
[`sync-over-async-demo`](../../examples/sync-over-async-demo). See also the guide:
[`docs/guides/schema-registry-mock.md`](../../docs/guides/schema-registry-mock.md).

## Build and run

```shell
cd helpers/schema-registry-standalone
mvn clean package
java -jar target/schema-registry-standalone-4.5.0.jar
```

The server starts on `http://127.0.0.1:8081`. Press `Ctrl-C` to stop.

Point a Confluent client (or `minimalist-kafka`) at it with `schema.registry.url=http://localhost:8081`.

## REST API

All error responses use the Confluent shape `{"error_code": <int>, "message": <string>}`, where `error_code`
is a registry-specific sub-code (not the HTTP status).

### `GET /` — health check

Returns `200` with an empty object `{}`.

```shell
curl http://localhost:8081/
# {}
```

### `POST /subjects/{subject}/versions` — register a schema

Registers a schema under a subject and returns its **global id**. The id is content-addressed (identical
content returns the same id, even across subjects); a new **version** is assigned under the subject unless the
same content is already a version there (idempotent).

Request body:

```json
{ "schema": "<escaped schema string>", "schemaType": "AVRO | JSON | PROTOBUF" }
```

`schema` is required (an opaque string — the registry is schema-language-agnostic). `schemaType` is optional
and defaults to `AVRO`.

```shell
curl -X POST http://localhost:8081/subjects/orders-value/versions \
  -H 'content-type: application/vnd.schemaregistry.v1+json' \
  -d '{"schemaType":"JSON","schema":"{\"type\":\"object\",\"properties\":{\"hello\":{\"type\":\"string\"}}}"}'
# {"id":1}
```

| Result | HTTP | error_code |
|---|---|---|
| Registered (or already present) | 200 | — (`{"id": N}`) |
| Missing subject in path | 404 | 40401 |
| Missing `schema` string, or not well-formed JSON (Avro/JSON only) | 422 | 42201 |

### `GET /schemas/ids/{id}` — fetch a schema by global id

```shell
curl http://localhost:8081/schemas/ids/1
# {"schema":"{...}","schemaType":"JSON"}
```

Returns `{"schema": ...}` plus `schemaType` for non-Avro schemas (Confluent omits `schemaType` for Avro, the
default). Unknown id → `404` / `40403`.

### `GET /subjects/{subject}/versions/{version}` — resolve a subject/version

`{version}` is a positive integer (to pin a specific version) or the alias `latest`. This is what
`getSchemaMetadata(subject, version)` / `getLatestSchemaMetadata(subject)` call to resolve a subject/version to
a global schema id.

```shell
curl http://localhost:8081/subjects/orders-value/versions/latest
# {"subject":"orders-value","id":1,"version":1,"schema":"{...}","schemaType":"JSON"}

curl http://localhost:8081/subjects/orders-value/versions/1
# {"subject":"orders-value","id":1,"version":1,"schema":"{...}","schemaType":"JSON"}
```

Returns `{subject, id, version, schema}` plus `schemaType` for non-Avro schemas.

| Result | HTTP | error_code |
|---|---|---|
| Found | 200 | — |
| Unknown subject | 404 | 40401 |
| Unknown version (subject exists) | 404 | 40402 |

## Storage

Schemas are persisted as **one file per global id** — `<id>.json` (e.g. `1.json`) — under the directory set by
`schema.registry.data.store` (default `/tmp/schema-registry`). Each file holds
`{"schema", "schemaType", "subject", "version"}`.

- **Stable ids across restarts.** Files are loaded on boot, so ids stay stable — the store is a convenient
  place to back up or **seed** schemas with known ids (see the demo's `registry/{1,2,3}.json`).
- **On-demand pickup.** A `<id>.json` dropped into the directory while the server is running is served on the
  next `GET /schemas/ids/{id}` — no restart needed.
- **Subjects & versions.** A global id is content-addressed; versions are per-subject, tracked in an in-memory
  `subject → (version → id)` index rebuilt on boot from the per-id files. A subject-based lookup therefore
  resolves subjects present at boot (or registered while running); a file dropped in after boot is
  id-resolvable immediately but subject-resolvable only after a restart. A single `<id>.json` records one
  subject/version, so the uncommon case of identical content under multiple subjects only round-trips the
  first across a restart.

Override the store for a durable location:

```shell
java -Dschema.registry.data.store="$HOME/schema-registry" -jar target/schema-registry-standalone-4.5.0.jar
```

## Choosing a port

The port defaults to `8081`. Override it with the `rest.server.port` property:

```shell
java -Drest.server.port=8082 -jar target/schema-registry-standalone-4.5.0.jar
```

## Prefer Docker / the real registry?

This helper simply removes the external-infrastructure requirement for local work. You can of course run the
real Confluent Schema Registry (via Docker or Confluent Cloud) and point `schema.registry.url` at it instead —
the API surface above is a faithful subset, so clients need no changes.
