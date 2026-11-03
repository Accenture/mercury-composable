- [x] **minimalist-kafka's dead-letter path: two rulings** (raised and closed 2026-10-30, Eric's rulings the same day).
  - **Outcome:** the dead-letter copy keeps the original record key (#597, CHANGELOG Fixed 3); no library retry for the
    registry, since Confluent 8.3.2's client already retries a network error or an HTTP 408/429/5xx three times with backoff.
  - **Lesson:** measure a third-party retry with the construction the library really uses - the two-argument
    `CachedSchemaRegistryClient` never retries, the four-argument one with a config map does.
  - origin: 2026-10-30-182330
  <!-- id: minimalist-kafka-dlq-rulings | created: 2026-10-30 | last_used: 2026-10-30 | uses: 1 | tier: archive-candidate | origin: 2026-10-30-182330 -->
