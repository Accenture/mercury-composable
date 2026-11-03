- [x] **Is `minimalist-kafka`'s ManagedCache in front of Confluent's registry client needed? Ruled and done 2026-10-30** (Eric
  accepted the recommendation). The id and `latest` entries are gone (Confluent's client caches both); the pinned-version
  cache stays, since Confluent does not cache `getSchemaMetadata(subject, version)`; `schema.registry.cache.ttl` is retired
  and passed on as `latest.cache.ttl.sec`. Java #598, Rust mercury #399 (Increment 193). Lesson: measure what a library
  client already caches before fronting it with a cache of your own.
  - origin: 2026-10-30-191414
  <!-- id: minimalist-kafka-schema-cache-ruling | created: 2026-10-30 | last_used: 2026-10-30 | uses: 1 | tier: archive-candidate | origin: 2026-10-30-191414 -->
