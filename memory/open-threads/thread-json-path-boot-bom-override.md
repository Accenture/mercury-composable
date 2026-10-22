- [x] **`json-path` resolved 2.10.0 in every consumer of platform-core, which declared 3.0.0** (the Spring Boot parent's BOM
  managed it). CLOSED 2026-10-22: moot. RFC-0010 WP2 (branch `feature/minimalist-jsonpath`) removes json-path,
  json-smart and accessors-smart from every module, so there is no version left to align. The Boot BOM overriding a
  transitive version for every consumer is kept in `conv-library-pins-are-direct-dependencies`.
  (origin: 2026-10-22-031653; closed in 2026-10-22-053616)
  <!-- id: json-path-boot-bom-override | created: 2026-10-22 | last_used: 2026-10-22 | uses: 1 | tier: working | origin: 2026-10-22-031653 -->
