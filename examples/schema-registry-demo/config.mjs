// Shared configuration for the schema-registry-demo Node helpers.
// Override the registry base URL with SCHEMA_REGISTRY_URL (defaults to the standalone dev server).

export default {
  registryUrl: process.env.SCHEMA_REGISTRY_URL || 'http://127.0.0.1:8081',
  // ISO-8601 timestamp for simple, sortable console logging
  ts: () => new Date().toISOString(),
};
