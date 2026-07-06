# Archive Index

> One line per archived fact, greppable. Before saying "I have no context on X",
> grep here. Format: `id — one-line summary — quarter file`.
>
> This repo was upgraded to the evolving-memory layer on 2026-06-12; facts fade here as reviews run.

- thread-ci-docs-build — CI step running `mkdocs build --strict` + grammar drift check (shipped) — swept (completed thread) — 2026-Q2.md
- stack-docs-mkdocs — docs site built with mkdocs (`docs_dir: docs`) — faded — 2026-Q2.md
- pom-version-source-of-truth — `pom.xml` is the version source of truth — faded — 2026-Q2.md
- docs-dir-layout — docs under `docs/` keeping `guides/` subfolder for stable URLs — faded — 2026-Q2.md
- docs-rewrite-architecture — layered Parts I–VI docs re-architecture (Design) — faded — 2026-Q2.md
- docs-dsl-spec — deterministic DSL spec kit + CI drift test for the 3 DSLs — faded — 2026-Q2.md
- request-pipeline-model — protocol boundary → flow adapter → engine → bus → functions — faded — 2026-Q2.md
- conv-canonical-example — `examples/composable-example` (`FlowTest`) as canonical reference — faded — 2026-Q2.md
- pref-github-source-of-truth — official Accenture GitHub repo is the source of truth — faded — 2026-Q2.md
- bp-docs-ai-human-rewrite — first-iteration AI+human docs rewrite (completed blueprint) — faded — 2026-Q2.md
- thread-docs-style-consistency — old/new doc-style structural inconsistency (resolved) — swept (completed thread) — 2026-Q2.md
- thread-docs-content-consistency — old/new doc-style content inconsistency (resolved) — swept (completed thread) — 2026-Q2.md
- stack-persistence-r2dbc — Persistence: not part of the framework core — the framework is persistence-agnostic. The — faded — 2026-Q2.md
- docs-style-conventions — Finalized doc-style conventions (the consistency pass after the migration was declared "do… — faded — 2026-Q2.md
- docs-content-canon — Documentation content canon (Design for the content-polish pass; locked with Eric Law 2026… — faded — 2026-Q2.md
- docs-no-redirects — No backward-compat redirects (clean rewrite). All `mkdocs-redirects` entries removed (2026… — faded — 2026-Q2.md
- kafka-adapter-not-in-repo — The built-in HTTP flow adapter (`HttpToFlow` / `http.flow.adapter`) is the only one in `ev… — faded — 2026-Q2.md
- otel-w3c-tracing — W3C OpenTelemetry distributed tracing (`feature/open-telemetry` branch). Each function get… — faded — 2026-Q2.md
- pref-explicit-test-config — List configurable parameters explicitly in the test `application.properties`, even when th… — faded — 2026-Q2.md
- thread-layer-reorg — (in progress) Layer-standardization reorg — "Shared Foundations + lean parallel layers" — swept: completed thread — 2026-Q2.md
- thread-next-ai-context — (next agenda — Eric, 2026-06-22) Content polishing round 2 + AI context discovery. Next wo… — swept: completed thread — 2026-Q2.md
- thread-minimalist-kafka-adapter — (future — after the docs-rewrite phase; Eric, 2026-06-22) Add a minimalist Kafka flow adap… — swept: completed thread — 2026-Q2.md
- thread-verify-invariants-2026q2 — Re-verify invariants (first invariant check; 24 session files ≥ `verify_invariants_every`… — swept: completed thread — 2026-Q2.md
- agent-memory-upgrade-v4224 — Upgraded agent-memory v4.21.0 → v4.23.1 (in-place Mode B, 2026-06-28, by Claude Code from… — swept: completed thread — 2026-Q2.md
- kafka-flow-failure-dlq — Kafka flow-adapter consumer error semantics = bounded retry → per-topic dead-letter, commi… — faded — 2026-Q2.md
- soa-config-driven-init — sync-over-async production bootstrap = config-driven init (Eric, 2026-06-26). The module's… — faded — 2026-Q2.md
- kafka-client-config-templates — Kafka client config is externalized to template files, not hard-coded (Eric, 2026-06-27).… — faded — 2026-Q2.md
- kafka-partition-pinning — Kafka consumer partition pinning = opt-in per binding (Eric, 2026-06-27). A `kafka-flow-ad… — faded — 2026-Q2.md
- adr-pattern-adopted — ADR pattern adopted (the agent-memory optional Architecture Decision Record log; opted in… — faded — 2026-Q2.md
- code-conventions-home — Code-style conventions have a documentation home. Soft, evolving code-organization/naming — faded — 2026-Q2.md
- helpers-and-worked-examples — Standalone dev servers live in `helpers/`; worked examples teach the patterns. `helpers/`… — faded — 2026-Q2.md
- minimalist-examples — Examples are kept deliberately minimal (avoid drift). Bare-minimum examples on principle (… — faded — 2026-Q2.md
- kafka-soa-docs — minimalist-kafka + sync-over-async are documented in mkdocs (2026-06-27). Two new publishe… — faded — 2026-Q2.md
- schema-registry-mock — Schema Registry mock server implementation. Created `helpers/schema-registry-standalone`,… — faded — 2026-Q3.md
- agent-memory-upgrade-v4261 — Upgraded agent-memory v4.25.0 → v4.26.1 (Mode B, by Claude Code from the tool checkout) —… — faded — 2026-Q3.md
- agent-memory-upgrade-v4250 — Upgraded agent-memory v4.23.1 → v4.25.0 (Mode B, by Claude Code from the tool checkout). — faded — 2026-Q3.md
- thread-schema-registry — (completed — Eric, 2026-06-28) Schema Registry feature. Implemented `helpers/schema-regist… — faded — 2026-Q3.md
- application-log-context — Application log context — virtual-thread-safe MDC alternative (2026-06-30, PR #128). A `co… — faded — 2026-Q3.md
- kafka-schemaid-from-subject-version — minimalist-kafka producer: subject/version schema resolution (2026-07-01, PR #129). `simpl… — faded — 2026-Q3.md
- standalone-schema-registry-mock — Standalone Schema Registry mock for local development (Eric, 2026-06-28; corrected 2026-06… — faded — 2026-Q3.md
- agent-memory-upgrade-v4280 — Upgraded agent-memory v4.27.0 → v4.28.0 (Mode B, by Claude Code from the tool checkout). S… — completed thread swept — 2026-Q3.md
- agent-memory-upgrade-v4270 — Upgraded agent-memory v4.26.1 → v4.27.0 (Mode B, by Claude Code from the tool checkout). S… — completed thread swept — 2026-Q3.md
- thread-schema-registry-avro-protobuf — (completed — Eric, 2026-06-29) minimalist-kafka Schema Registry serdes — feature COMPLETE… — completed thread swept — 2026-Q3.md
- thread-kafka-schemaid-from-name — (completed — Eric, 2026-07-01, PR #129) minimalist-kafka: resolve `schemaId` from subject/… — completed thread swept — 2026-Q3.md
- minimalist-kafka-protobuf-removed — Protobuf support removed from `minimalist-kafka`'s Schema Registry integration, pre-releas… — faded — 2026-Q3.md
- minimalist-kafka-confluent-8-3-0 — `minimalist-kafka`: `confluent.version` bumped 8.2.0 → 8.3.0; `kafka.version` (4.2.0) upgr… — faded — 2026-Q3.md
- event-script-minigraph-code-review-2026-07 — Code review closeout for PR #130 added deprecation-conversion visibility + real lifecycle… — faded — 2026-Q3.md
- thread-minimalist-kafka-protobuf-removal — (completed — Eric, 2026-07-01) Remove Protobuf support from minimalist-kafka's Schema Regi… — faded — 2026-Q3.md
- thread-deprecate-simple-type-matching — (sprint complete — Eric, 2026-07-02) Deprecate 'simple type matching' in TaskExecutor → 's… — faded — 2026-Q3.md
- thread-snyk-oss-dependency-update — (completed — Eric, 2026-07-01) Repo-wide OSS dependency security update (Snyk-driven). Com… — completed thread swept (older than archive_window=20) — 2026-Q3.md
- thread-application-log-context — (completed — Eric, 2026-06-30) Application log context feature. Designed + implemented + — completed thread swept (older than archive_window=20) — 2026-Q3.md
