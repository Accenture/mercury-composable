- [x] **kafka.health produce-only classloader fix — SHIPPED in v4.12.12 (2026-09-17).** v4.12.11
  scoped the loader override to client construction; Kafka reaches the loader earlier, in a config
  class's static initializer, which the produce-only path hits while resolving its template. PR #409
  (`534f910c`) → release PR #410, squash `ebdd2e37`, tag `v4.12.12` verified to carry the swept pom.
  **Durable lesson:** a wrong classloader can damage more than the operation you wrapped — class
  initialization is one-shot, JVM-wide and irreversible, and the *compiler* decides which constant
  references can even trigger it ([[kafka-config-class-static-init-loader]]).
  origin: `memory/sessions/2026-09-17-183008.md`
  <!-- id: ot-kafka-health-producer-only-fix | created: 2026-09-17 | last_used: 2026-09-17 | uses: 2 | tier: working | origin: 2026-09-17-183008 -->
