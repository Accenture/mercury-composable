# Schema Registry Demo

A worked example showing how to talk to the minimalist, Confluent-compatible Schema Registry mock in
[`helpers/schema-registry-standalone`](../../helpers/schema-registry-standalone).

A schema registry is just an HTTP service, so this demo is deliberately minimalist — no application to
build. You drive it with `curl` or two tiny Node scripts (`.mjs`, using the built-in `fetch` — **no
`npm install`**).

## 1. Start the registry server

In a separate terminal, from the repo root:

```bash
mvn clean package -f helpers/schema-registry-standalone/pom.xml
java -jar helpers/schema-registry-standalone/target/schema-registry-standalone-4.5.0.jar
```

It listens on port **8081** (Confluent's default). Override with `-Drest.server.port=…`.

Schemas are persisted to `schemas.json` under `schema.registry.data.store` (default `/tmp/schema-registry`),
so ids survive a restart. Point it at a durable directory to keep ids stable across reboots:

```bash
java -Drest.server.port=8081 -Dschema.registry.data.store=$HOME/schema-registry \
     -jar helpers/schema-registry-standalone/target/schema-registry-standalone-4.5.0.jar
```

## 2. Register and fetch schemas

The schema is sent as a **string** inside the request body — `{ "schema": "<escaped schema>",
"schemaType": "AVRO|JSON|PROTOBUF" }`. This looks odd, but it is exactly Confluent's wire format: the
registry treats a schema as an opaque string, so the same envelope carries Avro, JSON Schema, and
Protobuf alike. `schemaType` defaults to `AVRO`.

### With the Node helpers

The schema is passed as a JSON-string argument, so you can submit an edited copy of a test-data file
(see [`schemas/`](./schemas)) with `"$(cat …)"`:

```bash
# Register an Avro schema (schemaType defaults to AVRO)
node register-schema.mjs user-value "$(cat schemas/user-avro.json)"
# -> registered 'user-value' (AVRO) -> id=1

# Register a JSON Schema
node register-schema.mjs person-value "$(cat schemas/person-jsonschema.json)" JSON
# -> registered 'person-value' (JSON) -> id=2

# Fetch a schema by its global id
node get-schema.mjs 1
```

The schema id is **global and content-based**: registering identical schema content (even under a
different subject) returns the same id.

### With curl

```bash
# 1. Register an Avro schema
curl -s -X POST -H "content-type: application/json" \
  -d "{\"schema\": $(jq -Rs . < schemas/user-avro.json)}" \
  http://127.0.0.1:8081/subjects/user-value/versions
# -> {"id":1}

# 2. Register a JSON Schema (note schemaType)
curl -s -X POST -H "content-type: application/json" \
  -d "{\"schema\": $(jq -Rs . < schemas/person-jsonschema.json), \"schemaType\": \"JSON\"}" \
  http://127.0.0.1:8081/subjects/person-value/versions
# -> {"id":2}

# 3. Fetch a schema by id
curl -s http://127.0.0.1:8081/schemas/ids/1
# -> {"schema":"{\"type\":\"record\",\"name\":\"User\",...}"}
```

`jq -Rs .` reads a file as raw text and emits it as a JSON string — the escaping the `schema` field
needs. If you don't have `jq`, the Node helper does the same escaping for you.

## 3. Test data — copy and edit

[`schemas/`](./schemas) holds ready-to-edit schema documents:

| File | Type | Notes |
|------|------|-------|
| [`user-avro.json`](./schemas/user-avro.json) | Avro record | a nullable `email` field shows union types |
| [`person-jsonschema.json`](./schemas/person-jsonschema.json) | JSON Schema (draft-07) | `required` + `minimum` constraints |

Copy one, edit it, and register it under a new subject:

```bash
cp schemas/user-avro.json schemas/order-avro.json
# ...edit schemas/order-avro.json...
node register-schema.mjs order-value "$(cat schemas/order-avro.json)"
```

## Error responses

The mock returns Confluent-style errors — `{ "error_code": <int>, "message": "…" }`:

```bash
node get-schema.mjs 999            # HTTP 404, error_code 40403 (schema not found)
node register-schema.mjs s '{bad'  # HTTP 422, error_code 42201 (invalid schema)
```

## Endpoints

| Method & path | Purpose |
|---------------|---------|
| `GET /` | liveness probe — returns `{}` |
| `POST /subjects/{subject}/versions` | register a schema, returns `{ "id": <int> }` |
| `GET /schemas/ids/{id}` | fetch a schema by global id |
