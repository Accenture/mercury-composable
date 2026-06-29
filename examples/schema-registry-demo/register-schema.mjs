// register-schema.mjs - register a schema with the (Confluent-compatible) Schema Registry.
//
// Usage:
//   node register-schema.mjs <subject> <schema-json-string> [schemaType]
//
// The schema is passed as a JSON string, so you can submit an (edited copy of a) test-data file:
//   node register-schema.mjs user-value   "$(cat schemas/user-avro.json)"
//   node register-schema.mjs person-value "$(cat schemas/person-jsonschema.json)" JSON
//
// On success it prints the global schema id (Confluent assigns one id per unique schema content).

import cfg from './config.mjs';

const [subject, schema, schemaType = 'AVRO'] = process.argv.slice(2);

if (!subject || !schema) {
  console.error('Usage: node register-schema.mjs <subject> <schema-json-string> [schemaType]');
  process.exit(1);
}

// Confluent wire format: the schema document travels as an escaped STRING inside the request body.
// Avro and JSON Schema are themselves JSON, so we compact + validate them (matching what a real client
// sends); a non-JSON schema (e.g. Protobuf IDL) is forwarded verbatim.
const schemaString = compactIfJson(schema);
const url = `${cfg.registryUrl}/subjects/${encodeURIComponent(subject)}/versions`;

const res = await fetch(url, {
  method: 'POST',
  headers: { 'content-type': 'application/json', accept: 'application/json' },
  body: JSON.stringify({ schema: schemaString, schemaType }),
});
const payload = await res.json();

if (res.ok) {
  console.log(`[${cfg.ts()}] registered '${subject}' (${schemaType}) -> id=${payload.id}`);
} else {
  // Confluent error body: { "error_code": <int>, "message": <string> }
  console.error(`[${cfg.ts()}] register failed (HTTP ${res.status}):`, JSON.stringify(payload));
  process.exit(1);
}

function compactIfJson(text) {
  try {
    return JSON.stringify(JSON.parse(text));
  } catch {
    return text;
  }
}
