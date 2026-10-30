- [x] **minimalist-kafka's dead-letter path: two rulings** (raised and closed 2026-10-30, Eric's rulings the same day).
  - **Outcome:** the dead-letter copy keeps the original record key (`KafkaRequestPublisher.publishSync` overload with the key;
    CHANGELOG Fixed 3). No library retry for the registry: Confluent 8.3.2's client already retries a network error or an
    HTTP 408/429/5xx three times with jittered exponential backoff (measured: 4 attempts, about 6 s, against a registry
    answering 503), which Eric's "unless the Confluent solution is suboptimal" left in place; the guide documents it.
    Branch `docs/minimalist-kafka-guide-gaps`.
  - **Lesson:** measure a third-party retry with the construction the library really uses - the two-argument
    `CachedSchemaRegistryClient` never retries, the four-argument one with a config map does, and reading bytecode had
    suggested network errors were not retried at all.
  - origin: 2026-10-30-182330
  <!-- id: minimalist-kafka-dlq-rulings | created: 2026-10-30 | last_used: 2026-10-30 | uses: 1 | tier: working | origin: 2026-10-30-182330 -->
