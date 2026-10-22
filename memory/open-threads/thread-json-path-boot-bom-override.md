- [ ] **`json-path` resolves 2.10.0 in every consumer of platform-core, which declares 3.0.0** (found 2026-10-22 while fixing
  the ASM/Vert.x/Netty advisories). Every module's parent is `spring-boot-starter-parent` 4.1.1, and its BOM manages
  `json-path` at 2.10.0. So 34 modules, the starter templates and presumably field applications built from them run 2.10.0,
  while platform-core is built and tested against 3.0.0 (its version since 4.4.0, 2026-03-29). No scanner flags 2.10.0, so
  this is consistency, not security. The likely fix is the `netty.version` shape: a `json-path.version` property in each pom.
  The decision is Eric's: align, or accept and document. Relates [[conv-library-pins-are-direct-dependencies]].
  <!-- id: json-path-boot-bom-override | created: 2026-10-22 | last_used: 2026-10-22 | uses: 1 | tier: working | origin: 2026-10-22-031653 -->
