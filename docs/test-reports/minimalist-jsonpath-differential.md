---
title: Test Report — JSONPath, minimalist-jsonpath (RFC 9535) vs Jayway JsonPath
summary: Permanent record of the checks behind RFC-0010 WP2 - the JSONPath Compliance Test Suite on the new engine, a differential of every '$' path the repository uses between Jayway JsonPath 3.0.0 and the in-house RFC 9535 engine, the suite's queries through Jayway, the Jayway-only forms, and a benchmark - kept as the evidence for replacing the dependency.
layer: reference
audience: [developer, architect]
keywords: [jsonpath, rfc 9535, minimalist-jsonpath, jayway, json-smart, differential, compliance suite, benchmark, test report]
---

# Test Report — JSONPath, minimalist-jsonpath (RFC 9535) vs Jayway JsonPath

*Validation of `system/minimalist-jsonpath`, the zero-dependency JSONPath engine written from RFC 9535 that replaces Jayway
JsonPath in platform-core's `MultiLevelMap` (RFC-0010 in `docs/arch-decisions/RFC.md`). Conducted 2026-10-22 on the
branch `feature/minimalist-jsonpath`, before the change was reviewed.*

This report is a permanent record. It answers three questions:
- **Does the new engine implement the standard?**
- **Where does it answer differently from the Jayway engine the installations run today, and is each difference
  intended?**
- **Is it at par or faster?**

## Why this report exists

Jayway JsonPath requires json-smart. json-smart 2.6.0, the latest release, is reported vulnerable (CVE-2026-113602).
Jayway's filter engine calls json-smart's parser directly, so neither a version bump nor an exclusion removes it.

Jayway also predates RFC 9535 and follows the 2007 proposal. The Rust engine evaluates `$` paths with an RFC 9535
implementation, so the two engines answered some queries differently.

RFC-0010 decided to replace Jayway with an engine written from the standard, shipping in v4.12.26 with RFC 9535 adhered
to. A replacement at that altitude must be shown to conform to the standard, and every way it differs from what runs
today must be listed and judged.

## Method

The harness is `benchmark/jsonpath-differential/` (see its README). It is not a Maven module, because Jayway needs
json-smart on its classpath. It calls each engine the way `MultiLevelMap` does:
- **Jayway, before the swap:** `JsonPath.read`, with `PathNotFoundException` read as not found.
- **The new engine, after:** `compileLenient(path).read(document, notFound)`, which adds Mercury's two rules: a hyphen in a
  dotted name, and the result shaped by the kind of query.

Machine: Apple M3 Pro, OpenJDK 27. The engine compiles for Java 21.

1. **The compliance suite.** `cts.json` from jsonpath-standard/jsonpath-compliance-test-suite, at commit `20ebdcce`, with its
   BSD-2 licence: 715 cases, 249 of them invalid queries. It runs in the module's tests against the strict `compile`.
2. **The repository's paths.** Every `$` path found in the flows, graph models, fixtures, tutorials, guides and tests: 40
   texts, 2 of which are prose fragments that both engines refuse.
   - Each path runs over 1,000 documents grown along its own names (half of them mutated by dropping a member, setting it to
     null or retyping it) and over 1,000 shared random documents.
   - As in `MultiLevelMap`, a document is a non-empty map.
   - 74,000 evaluations in all; 19,587 returned a non-empty result.
3. **The suite through Jayway.** Each of the suite's queries is run on its own document by both engines.
4. **The Jayway-only forms.** Jayway's extra operators and trailing functions, with their RFC 9535 replacements.
5. **The benchmark.** The repository's 38 real paths, each over a document grown from it. Compile once and evaluate many;
   best of 5 rounds of 2 s after 3 s of warm-up; three runs.

## Results

### 1. The standard

All 715 cases pass, with no deviation:
- the 249 invalid queries are refused;
- every valid query selects the expected values and the expected normalized paths, or one of the listed alternatives for the
  9 cases where the standard leaves an order open.

Mercury's hyphen rule is applied only by `compileLenient`, so the suite's `$.a-b` refusal holds against the strict engine.

### 2. The repository's paths

The two engines differ on 7 of the 38 paths, in 170 of 74,000 evaluations, and in one way only: where the path indexes,
slices or filters a member that is present but is not an array (null, or text after a mutation).

| Jayway | RFC 9535 engine | Evaluations |
|---|---|---|
| not found | an empty list | 100 |
| threw `InvalidPathException` at run time | an empty list | 70 |

For example, `$.input.body.people[?(@.team == 'red')].name` over a body whose `people` is null: Jayway threw at run time,
which failed the mapping, while the new engine answers `[]`.

This is the intended answer for three reasons:
- **The standard:** a selector selects nothing from a value that is not an array.
- **Mercury's documented shape:** an indefinite path yields a list unless a member name before its first indefinite step is
  *missing*, and `people` is present.
- **The Rust engine** already answers `[]`.

The engines never returned two different non-empty answers.

### 3. The suite through Jayway

| Kind | Queries |
|---|---|
| valid, the same answer | 91 |
| valid, a different answer | 21 |
| valid, refused by Jayway | 354 |
| invalid, refused by both | 116 |
| invalid, accepted by Jayway | 133 |

Jayway parses fewer than a quarter of the standard's valid queries: filters without parentheses, the five functions, and
blank space in places the standard allows. Every one of the 21 different answers is Jayway departing from the standard:

- **A union of names is a list.** Jayway returns `$['a','b']` as an object projection, `{a=..., b=...}`, and
  `$..['a','d']` as projections too. The guides already promised a list for a union (the claim `json-path-result-shape`),
  and the Rust engine returns one, so Java now matches its own documentation.
- **Slices follow their step and their sign.** Jayway ignores a step (`$[1:6:2]` gave 1 to 5) and answers negative steps
  wrongly (`$[9:0:-1]` gave `[]`, `$[-1:1:-1]` gave `[9, 0]`). It also misreads some negative bounds: `$[-5:7]` gave 12
  elements of a 10-element array, and `$[1:-1]` gave `[]`. A step of 0 selects nothing (Jayway gave `[1]`).

### 4. The Jayway-only forms

All 21 are refused:
- the filter operators `in`, `nin`, `subsetof`, `anyof`, `noneof`, `size`, `empty` and `=~`;
- the trailing functions `.length()`, `.min()`, `.max()`, `.avg()`, `.stddev()`, `.sum()`, `.keys()`, `.first()`, `.last()`,
  `.index()`, `.concat()` and `.append()`;
- an array literal in a filter.

Their RFC 9535 replacements work:
- `@.code == 'a' || @.code == 'b'` for a small `in`;
- `match()` and `search()` for `=~`;
- `length()` and `count()` inside a filter.

Jayway's own trailing functions were unreliable before the swap. On a list of three items, `.min()`, `.max()`, `.avg()`,
`.stddev()`, `.sum()`, `.first()`, `.last()` and `.index(1)` all threw "Aggregation function attempted to calculate value
using empty array", and `.concat('!')` returned an empty string. No path in the repository uses any Jayway-only form.

### 5. The benchmark

Per path, best of each run:

| | Run 1 | Run 2 | Run 3 |
|---|---|---|---|
| evaluate, Jayway | 412 ns | 435 ns | 438 ns |
| evaluate, RFC 9535 engine | 139 ns | 144 ns | 144 ns |
| compile, Jayway | 181 ns | 179 ns | 183 ns |
| compile, RFC 9535 engine | 198 ns | 196 ns | 207 ns |

The new engine evaluates the repository's paths in about a third of Jayway's time. It compiles about 10% slower, which
`MultiLevelMap` pays once per distinct path, because it keeps compiled queries in a bounded cache (2,000 entries), as
Jayway's `JsonPath.read` did internally.

## Conclusion

- **The standard:** the engine conforms to RFC 9535 as its compliance suite defines it.
- **The repository's paths:** it differs from Jayway in one intended way. A path through a present member that is not an
  array now yields an empty list, as the standard, Mercury's documented shape and the Rust engine all say.
- **The suite through Jayway:** every other difference is a Jayway departure from the standard: object projections for
  unions, and slices that ignore their step or sign.
- **The Jayway-only forms** are refused, as RFC-0010 accepts. No path in the repository uses one.
- **Performance:** evaluation is about three times faster than Jayway.
- **Dependencies:** `com.jayway.jsonpath`, `net.minidev` and the ASM 9.7.1 node behind `accessors-smart` no longer appear in
  any module's dependency tree.
