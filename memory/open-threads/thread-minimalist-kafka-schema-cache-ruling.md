- [x] **Is `minimalist-kafka`'s ManagedCache in front of Confluent's registry client needed? Ruled and done 2026-10-30** (Eric
  accepted the recommendation). The id override and the `latest` entries are gone - Confluent's client keeps every schema
  fetched by id for its life and caches `latest` for `latest.cache.ttl.sec`; the pinned-version cache stays, since
  Confluent does not cache `getSchemaMetadata(subject, version)`; `schema.registry.cache.ttl` is retired and a value still
  set is passed on as `latest.cache.ttl.sec`. Java on PR #598 (third commit), the Rust mechanism on mercury #399
  (Increment 193). Lesson: measure what a library client already caches before fronting it with a cache of your own.
  - origin: 2026-10-30-191414
  <!-- id: minimalist-kafka-schema-cache-ruling | created: 2026-10-30 | last_used: 2026-10-30 | uses: 1 | tier: working | origin: 2026-10-30-191414 -->
