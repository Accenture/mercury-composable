- [ ] **Is `minimalist-kafka`'s ManagedCache in front of Confluent's registry client needed? Awaiting Eric's ruling** (asked
  2026-10-30 after the session log's follow-up). Measured against Confluent 8.3.2 (a counting registry, the real `SchemaCodec`):
  - **The id cache is redundant.** Confluent caches every id lookup with no expiry (`idToSchemaCache` per subject plus
    `parsedSchemaCache`, bounded by the 1000 capacity, positive-only since `SchemaCodec` pins the missing caches to 0). After
    the ManagedCache entry expired, a decode and a serialize made no registry request. Confluent's serdes never call the
    `getSchemaById` override (`getSchemaBySubjectAndId` / `getSchemaEntityBySubjectAndId`); only Mercury's own code does.
  - **The `latest` entry works against freshness.** Confluent caches `getLatestSchemaMetadata` for `latest.cache.ttl.sec`
    (60 s); the ManagedCache entry in front holds it for `schema.registry.cache.ttl` (30 min), so a new version is picked up
    up to 30 times later.
  - **The pinned-version cache is needed.** Confluent does not cache `getSchemaMetadata(subject, version)` unless deleted
    versions are looked up: three calls made three requests.
  - Recommendation: drop the id override and the `latest` entries, keep the pinned-version cache, retire or remap
    `schema.registry.cache.ttl`. The Rust port has no Confluent layer, so its id cache stays, but its expire-after-write TTL
    could become a size bound only, like Confluent's, which would close the outage exposure its guide now documents.
  - origin: 2026-10-30-191414
  <!-- id: minimalist-kafka-schema-cache-ruling | created: 2026-10-30 | last_used: 2026-10-30 | uses: 1 | tier: working | origin: 2026-10-30-191414 -->
