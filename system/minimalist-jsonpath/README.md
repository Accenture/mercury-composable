# Minimalist JSONPath

A JSONPath engine written from [RFC 9535](https://www.rfc-editor.org/rfc/rfc9535), the IETF JSONPath standard,
with no dependency beyond `java.base`. It is to replace Jayway JsonPath in `platform-core`, where `MultiLevelMap`
evaluates every `$` path of an Event Script flow or a knowledge graph (RFC-0010 in `docs/arch-decisions/RFC.md`).

## Why it exists

Jayway JsonPath needs `json-smart`, whose 2.6.0 release is reported vulnerable (CVE-2026-113602), and no later
release exists. Jayway's filter engine calls json-smart's parser directly, so no exclusion removes it. Jayway also
predates the standard and follows the 2007 proposal, while the Rust engine evaluates `$` paths with an RFC 9535
implementation, so a Jayway-only query answered on Java and silently answered null on Rust. An engine written from
the standard removes the dependency chain and gives both engines one grammar.

## What it supports

RFC 9535 in full:

| Part | What |
|---|---|
| Segments | child (`.name`, `.*`, `[...]`) and descendant (`..name`, `..*`, `..[...]`) |
| Selectors | name (`['a']`, `["a"]`), wildcard, index (negative counts from the end), slice (`[start:end:step]`), filter (`[?...]`) |
| Filters | `\|\|`, `&&`, `!`, parentheses, `==` `!=` `<` `<=` `>` `>=`, existence tests, relative (`@`) and absolute (`$`) queries |
| Literals | strings (single or double quoted, with the standard's escapes), numbers, `true`, `false`, `null` |
| Functions | `length`, `count`, `match`, `search`, `value`, type-checked when the query compiles |
| Regular expressions | I-Regexp ([RFC 9485](https://www.rfc-editor.org/rfc/rfc9485)), translated into `java.util.regex` |

Comparisons are type-strict, as the standard specifies: numbers compare by value (`1 == 1.0`; a `Double` through
its shortest decimal text), strings by code point, and `"5" == 5` is false. A query outside the grammar or the type
rules is refused with a `JsonPathException` (an `IllegalArgumentException`) that names the reason and the position.

## Mercury's two rules

Both match the Rust engine (crate `event-script`, `mlm.rs`) case for case:

1. **`JsonPath.compileLenient`** accepts a hyphen in a dot-notation member name (`$.fetcher-ext.result`), because
   graph node names carry hyphens. A query RFC 9535 refuses is retried once with such names rewritten to bracket
   notation (`$['fetcher-ext'].result`); a query RFC 9535 accepts is never rewritten. `JsonPath.compile` is the
   standard alone.
2. **`JsonPath.read(document, notFound)`** shapes the result by the kind of query, not by the number of matches. A
   singular query (one name or one index per segment) yields the value, or `notFound`. Any other query yields a
   list, one match being a one-element list and none an empty list, unless a member name before its first other
   segment is missing, which yields `notFound`. `JsonPath.select` returns the standard's nodelist.

```java
JsonPath path = JsonPath.compileLenient("$.input.body.people[?@.team == 'red'].name");
Object names = path.read(state, null);     // a List, compiled once and safe to share between threads
```

## Limits

- Parsing nests at most 64 levels (parentheses, filters and function arguments), and an I-Regexp at most 64 groups.
- The descendant segment walks the document with its own stack, so a deep document cannot exhaust a thread's.
- A compiled query is immutable and holds no shared state, so it can be cached and used from any thread.

## Verification

- **The JSONPath Compliance Test Suite**: `cts.json` from
  [jsonpath-standard/jsonpath-compliance-test-suite](https://github.com/jsonpath-standard/jsonpath-compliance-test-suite)
  (BSD-2, its licence and pinned commit in `src/test/resources/cts/`). All 715 cases run against `JsonPath.compile`.
  The 249 invalid queries are refused; each valid query selects the expected values and normalized paths, or one of
  the listed alternatives where the standard leaves an order open. There are no deviations.
- **Mercury's rules**: the Rust engine's own cases for the rewrite and the result shape, and the shared fixture
  `unit-test-jsonpath-1`.
- **The rest**: refusals with their messages and positions, the nesting limits, every Java number type, non-JSON
  leaves, normalized path escapes, I-Regexp's translation, and a document 100,000 levels deep.
- A JaCoCo gate of 90% of lines and branches.
