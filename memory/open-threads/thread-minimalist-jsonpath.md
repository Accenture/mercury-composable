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
  - **WP3 done on both engines, 2026-10-22** (Java on this branch; Rust on `feat/jsonpath-refusal-rfc-0010`, Increment 185).
    Eric's rulings (recorded in RFC-0010):
    - a flow drops the task, with an ERROR naming RFC 9535;
    - a graph is rejected;
    - one scanner finds where each reference lands (`JsonPathSource`, Rust `jsonpath_source.rs`), and WP4 reuses it;
    - a simple plugin's `$` argument stays supported (the Event Script guide promises it), with the argument splitter fixed;
    - a `{reference}` never holds a JSONPath query;
    - a dictionary default is a source, the first use case of JSONPath.
    Results:
    - The checks cover mapping sources, plugin arguments, `MAPPING:` statements and dictionary defaults.
    - On Rust, a bad path is an error at evaluation, no longer a silent null.
    - The shared `jsonpath-source-vectors.json` holds 49 vectors.
    - Rust runs the same `cts.json` against `serde_json_path`: every answer is the standard's and the 249 invalid queries are
      refused; 16 normalized paths lack RFC 9535's escapes, unused by the engine and pinned.
    - Parity checked (Eric's question): Rust already substitutes before it parses, at every site. Nested braces differ; that
      is left for WP4.
  - **Rust WP3 merged:** mercury #391, 2026-10-22.
  - **WP4 done on both engines, 2026-10-22** (Java on this branch; Rust on `feat/jsonpath-substitution-rfc-0010`, Increment
    186). Each value substituted into a `$` source is checked where it lands. Eric's two rulings, recorded in RFC-0010:
    - Event Script keeps a text value bare in a filter as written when it is already one literal (`10`, `true`, `'abc'`), so
      numeric text still compares as a number; the knowledge graph quotes all text, as it always has.
    - A name takes Mercury's dot-bracket key path (`items[0].name`).
    Also done:
    - the graph's `graph.math` quoting escapes the backslash, a Fixed item on both engines;
    - nested references inside a `$` source are refused on both engines;
    - 39 shared substitution vectors give the same queries and messages on both engines;
    - one runtime flow and one runtime graph exercise it end to end on each engine.
    Found: my WP3 Rust CHANGELOG edit had put Changed 5 below the `---` that closes the Unreleased section; WP4 restores it
    ([[conv-changelog-from-tag-range]]).
  - **Rust WP4 opened** as mercury #392.
  - **WP5 done on both engines, 2026-10-22** (Java on this branch; Rust on `feat/jsonpath-docs-rfc-0010`, stacked on #392,
    Increment 187).
    - The Event Script syntax guide's JSON Path section: the grammar, the result shape, the refusal, a table of Jayway-only
      forms and their replacements, and the substitution checks.
    - The plugin-argument sentence is kept and explained.
    - Updated as well: the flow schema reference (its map-of-lists error fixed), the flow grammar, the command reference, the
      commands catalog, the AI agent checklist and two help pages.
    - Claims: 2 updated and 5 new, on both engines; the result-shape test is renamed from Jayway to Mercury.
    - The Playground bundle is rebuilt after `npm ci` (Eric's go-ahead) and deployed to both engines: only the main chunk
      changed (`index-D5Sx-GL2.js`).
  - **Next:** the one Java PR for WP1 to WP5 (`feature/minimalist-jsonpath`), and the Rust WP5 PR after #392. After the merge,
    RFC-0010 is promoted to an ADR (the RFC-0006 method) and `main` is merged into the OneAgent branch (Eric's instruction).
  - If accepted, it makes [[json-path-boot-bom-override]] moot. Relates [[conv-library-pins-are-direct-dependencies]].
  <!-- id: minimalist-jsonpath | created: 2026-10-22 | last_used: 2026-10-22 | uses: 1 | tier: working | origin: 2026-10-22-040817 -->
