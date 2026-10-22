- [ ] **An in-house JSONPath engine to RFC 9535 replaces Jayway JsonPath** (→ proposal: RFC-0010; serves [[vision-mercury-composable]]).
  - **Why:** `json-smart` 2.6.0 is reported vulnerable (CVE-2026-113602), has no fixed release, and json-path 3.0.0's filter
    engine calls its parser directly, so the dependency cannot be bumped or excluded away.
  - **Shape:** a module `system/minimalist-jsonpath` evaluating RFC 9535 over the engine's own trees.
  - **Rules kept:** Mercury's result shape and hyphens in dotted names.
  - **Refusal by name** of Jayway-only syntax, also at deployment.
  - **Substitution first, then parsing,** with a check where each value lands: in a path, a plain key path or an index; in a
    filter, an escaped literal. This keeps existing queries and closes a JSONPath injection.
  - **Proof:** the RFC 9535 compliance suite (715 cases) as the shared oracle for both engines.
  - **Eric (2026-10-22): RFC 9535 is preferred.** Jayway predates the standard, and moving Java to it is a transition.
  - **Eric's answers (2026-10-22):**
    - module `minimalist-jsonpath`, package `org.platformlambda.mini.jsonpath`;
    - **ships in v4.12.26**, swapping and refusing in one release (no warning release);
    - strict RFC 9535, no alternative to `in`;
    - substitution first, then JSONPath parsing, so that no existing graph or flow breaks: `$.input.body.{model.key}` with
      `item` is `$.input.body.item`. This was Eric's adjustment, replacing the flow-compiler refusal first proposed. The check where each value lands
      was **accepted** the same day.
    - [[json-path-boot-bom-override]] closes with the swap.
  - **RFC-0010 merged** (#577, squash `89cb2ea1`).
  - **WP1 done** on branch `feature/minimalist-jsonpath`, 2026-10-22:
    - the module `system/minimalist-jsonpath`, registered in the root pom after minimalist-msgpack;
    - **the compliance suite 715 of 715 with no deviation,** values and normalized paths both;
    - the Rust engine's shape and hyphen cases ported;
    - 779 tests in all, 100% of lines and 95.7% of branches covered.
  - **WP2 done,** same branch, 2026-10-22:
    - `MultiLevelMap` evaluates `$` paths through `compileLenient` + `read`, with a 2,000-entry Caffeine cache;
    - json-path, json-smart and accessors-smart are out of every module;
    - the Jayway differential found one intended difference (a present non-array member yields `[]`, where Jayway
      answered not found or threw), and evaluation is about 3x faster;
    - report `docs/test-reports/minimalist-jsonpath-differential.md`, harness `benchmark/jsonpath-differential/`;
    - the whole reactor passes, 2,622 tests.
  - **Next: WP3, refusal at deployment, on both engines. Designed 2026-10-22, not started (Eric resumes it; three
    decisions pending).**
    - Today neither engine parses a `$` source before it runs: both flow compilers accept one on a prefix test, neither
      graph gate looks at one, and Rust's `json_path_query` (`mlm.rs:166`) drops the parse error, so a typo reads as null.
    - One check per engine, the evaluator's own compile (`compileLenient`; Rust `parse` + the bracketize retry), over the
      WHOLE surface ([[clean-knowledge-design-over-engine-coverage]]): Event Script input and output sources; the graph's
      `mapping`/`input`/`output`/`for_each`, `graph.math` statements (`MAPPING:` and `{$...}` selectors), plugin
      arguments, fetcher dictionary defaults. On Java the graph half goes into `GraphModelValidator`, which serves
      CompileGraph, the packager and the Playground's `run`.
    - A source with a reference is parsed with a stand-in chosen by where the reference lands (a name, `0`, letters, a
      string literal); the scanner that finds the landing site is the one WP4 needs.
    - Rust also: an invalid path becomes an error at evaluation (`get_lhs_element` already returns a `Result`), and the
      same `cts.json` runs against `serde_json_path`. Evidence: a shared vector file, negative fixtures on both engines.
    - **Decisions pending:** (1) a flow with a bad `$` source: drop the task like every other invalid entry (today's rule,
      recommended; the RFC's "the flow is refused" then needs correcting), reject the flow for `$` only, or reject it for
      any invalid entry; (2) build the landing-site scanner in WP3 and reuse it in WP4 (recommended); (3) teach the
      plugin-argument splitter brackets and quotes on both engines, since it cuts `f:x($.a['b','c'])` at the comma
      (recommended).
  - If accepted, it makes [[json-path-boot-bom-override]] moot. Relates [[conv-library-pins-are-direct-dependencies]].
  <!-- id: minimalist-jsonpath | created: 2026-10-22 | last_used: 2026-10-22 | uses: 1 | tier: working | origin: 2026-10-22-040817 -->
