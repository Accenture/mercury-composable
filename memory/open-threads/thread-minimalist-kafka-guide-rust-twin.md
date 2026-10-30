- [ ] **The Rust port catches up with the minimalist-kafka guide gaps and the dead-letter key** (Eric, 2026-10-30: "Rust doc
  catches up after this PR"; the Java change is branch `docs/minimalist-kafka-guide-gaps`).
  - **The dead-letter key.** The Rust publisher builds every record with no key (`crates/minimalist-kafka/src/publisher.rs`,
    `FutureRecord::to(topic)`), so its dead-letter copy drops the original key as Java's did; keep it, as Java's Fixed 3 does.
  - **The guide** (`docs/guides/minimalist-kafka.md`): the partitioning text says keyed records keep murmur2 hashing while
    the outbound function publishes without a key; state the keyless publish, the explicit `partition` header for per-entity
    ordering, and the keyed dead-letter copy. State what a registry outage does on decode and on publish, after confirming
    the Rust registry client's own retry behaviour (Java: Confluent retries a network error or an HTTP 408/429/5xx three
    times with jittered exponential backoff). Rust already documents `json.fail.invalid.schema`.
  - origin: 2026-10-30-182330
  <!-- id: minimalist-kafka-guide-rust-twin | created: 2026-10-30 | last_used: 2026-10-30 | uses: 1 | tier: working | origin: 2026-10-30-182330 -->
