// get-schema.mjs - fetch a schema by its global id from the (Confluent-compatible) Schema Registry.
//
// Usage:
//   node get-schema.mjs <id>
//
// The registry returns { "schema": "<escaped string>" [, "schemaType": "JSON|PROTOBUF"] }; schemaType
// is omitted for AVRO. We pretty-print the schema string so it is easy to read.

import cfg from './config.mjs';

const [id] = process.argv.slice(2);

if (!id) {
  console.error('Usage: node get-schema.mjs <id>');
  process.exit(1);
}

const url = `${cfg.registryUrl}/schemas/ids/${encodeURIComponent(id)}`;
const res = await fetch(url, { headers: { accept: 'application/json' } });
const payload = await res.json();

if (res.ok) {
  console.log(`[${cfg.ts()}] schema id=${id} (${payload.schemaType || 'AVRO'}):`);
  console.log(prettyIfJson(payload.schema));
} else {
  // Confluent error body: { "error_code": <int>, "message": <string> }
  console.error(`[${cfg.ts()}] get failed (HTTP ${res.status}):`, JSON.stringify(payload));
  process.exit(1);
}

function prettyIfJson(text) {
  try {
    return JSON.stringify(JSON.parse(text), null, 2);
  } catch {
    return text;
  }
}
