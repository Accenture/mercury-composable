# Requests for Comments (RFC) — the proposal register

> **For humans.** Work under consideration at the Design altitude: proposals that may become
> an Architecture Decision Record, be reshaped, merge with another, or be withdrawn. It is the
> sibling of `ADR.md` and exists so that **the ledger records decisions only** — an ADR is
> written when a proposal is accepted, never before (`memory/PROTOCOL.md` *Work from intent*,
> `DECAY.md` §12). Read **on demand**, not part of the per-session agent read path (zero
> default token cost, the same footing as `ADR.md`).

## Rules

- **Separate sequences.** `RFC-NNNN` and `ADR-NNNN` never share numbers: a proposal does not
  reserve an ADR number, because proposals and decisions do not map one-to-one — some merge,
  some split, some die.
- **Two exits, both recorded here.** *Promoted:* the human accepted it; the ADR is written in
  `ADR.md` and this entry keeps a pointer (`Promoted → ADR-NNNN`, date). *Withdrawn:* the entry
  stays with the reason. An entry is **never deleted**; a proposal may be revised freely while
  open, and only its final form reaches the ledger.
- **Status:** `Open` (under consideration) · `Parked` (deliberately deferred — reactivate on
  demand) · `Promoted → ADR-NNNN` · `Withdrawn`.
- **Map, don't duplicate.** The live work item stays in memory — an Open Thread in
  `memory/open-threads/` carrying a `→ proposal: RFC-NNNN` pointer — exactly as an accepted ADR
  is pointed to by a `(ADR-NNNN)` tag on its continuity fact. The register holds the proposal's
  reasoning (options, trade-offs, what a decision would commit to); the thread holds its state.
- **The human decides.** The agent raises and revises proposals here; promotion is the
  Design-altitude human gate (`DECAY.md` §12). Newest first.

## Format

```
## RFC-NNNN — <Title>
**Status:** Open · **Raised:** YYYY-MM-DD · **Serves:** <vision-id> · **Thread:** `<thread-id>`
<!-- id: rfc-NNNN | status: open | thread: <thread-id> -->

**Proposal.** What would change, and what a decision would commit the project to.
**Options.** The alternatives on the table, with their trade-offs.
**Resolution.** Empty while open; `Promoted → ADR-NNNN (date)` or `Withdrawn (date): <reason>`.
```

---

## RFC-0004 — A bracket-lookup plugin for tariff tables (`f:decimalBracket`)
**Status:** Withdrawn · **Raised:** 2026-10-01 · **Serves:** vision-mercury-composable · **Thread:** none
<!-- id: rfc-0004 | status: withdrawn | thread: none -->

**Proposal.** A plugin in the `f:lookup` family, `f:decimalBracket(amount, bands, scale, mode)`, taking an ordered table of `{ upTo, rate }` bands (and an optional open final band) and returning the banded amount as a canonical decimal string, so a tariff schedule that arrives as data stays on the graph. Raised by an external review of the DECIMAL statement.
**Options.** (a) the plugin; (b) leave a fixed schedule unrolled in `DECIMAL` with `min` and `max`, one line per band, and a schedule that arrives with each request in a `graph.task` function.
**Resolution.** Withdrawn (2026-10-01, Eric): option (b). A static schedule is graph data that the product owner certifies on the graph, and it unrolls into lines that can be read; bands supplied at run time are not graph data and belong in a function. The plugin would also force rulings the engine has no need to own: marginal bands or the whole amount at the band's rate, an inclusive or an exclusive upper bound, and the open final band — the edge-case chase `clean-knowledge-design-over-engine-coverage` warns against. The unrolled pattern is documented in the DECIMAL guide.

---

## RFC-0003 — Expose the `for_each` iteration index to the statements of a `graph.math` loop
**Status:** Parked · **Raised:** 2026-10-01 · **Serves:** vision-mercury-composable · **Thread:** none
<!-- id: rfc-0003 | status: parked | thread: none -->

**Proposal.** `GraphMath.executeForEach` counts the iteration and does not store it. Store it for the duration of the iteration, for example `model.<node>.index` (0-based) and `model.<node>.count`, overwritten on each pass and absent after the loop, so a statement can address the last line or the Nth line (`{model.index} == {model.count} - 1 ? residual : share`, inside a `DECIMAL` ternary; a taken `IF` would end the walk). Raised by an external review of the DECIMAL statement; both engines would carry it.
**Options.** (a) store `index` and `count`; (b) store nothing and compute an allocation residual outside the loop (`amount - share * n`, which already goes to a named party without an index); (c) a `graph.task` function for the rare per-line output list.
**Resolution.** Parked (2026-10-01, Eric): defer until a field case needs a residual on a specific line of a per-line output list. The loop-free residual covers allocation to a named party today. Reactivate with that case in hand; the change is small and additive, but it is one more engine surface that both engines must carry for ever.

---

## RFC-0002 — Canonical MsgPack packager: sorted-key maps and a manifest as one deterministic byte array
**Status:** Open · **Raised:** 2026-09-30 · **Serves:** vision-mercury-composable · **Thread:** `canonical-msgpack-packager`
<!-- id: rfc-0002 | status: open | thread: canonical-msgpack-packager -->

**Motivation.** Some installations must promote a *set* of related documents (knowledge graphs today, but equally flows, rules or tables) as one artifact whose identity can be recorded, compared and, where the field wants it, protected. Loose files cannot do that. Each one is deployed and validated alone, JSON text is not a stable artifact (key order and whitespace vary), and there is no single thing to hash. MsgPack does not fix this by itself: a map has no guaranteed order, and one value has several valid byte encodings, so two packagings of the same content can differ, and their hashes with them. Both engines already use MsgPack (Java through `msgpack-core`, `platform-core/.../serializers/MsgPack.java`; Rust through `rmpv`, `crates/platform-core`), so the smallest useful piece is a **deterministic packager**. Whether a package is then hashed, signed or neither is a deployment decision and stays outside it.

**Proposal.**

1. **The packager.** A small utility beside `MsgPack` in platform-core (and its Rust twin) that turns maps into one byte array. It first converts every map to an **ordered map with keys sorted recursively** (at every depth, including maps inside lists; list order is kept), then writes the result as MsgPack under the canonical profile (item 3). **The ordering is done by the packager itself** (Eric, 2026-10-01): the Gson serializer has no ordered-keys option, and an ordered-keys option such as Jackson's sorts by `String` order (UTF-16), which differs from another engine's byte order above U+FFFF. The input map may be of any kind; its own order never reaches the bytes. The bytes are written through the MsgPack library directly, not through the platform's general `MsgPack.pack`, which drops nulls unless configured, packs a `Float` as float32 and writes a `BigDecimal` zero as `"0.00"` - none of which the profile allows. The same content gives the same bytes, in either engine.
2. **Package structure: a manifest metadata map plus the maps.** The package is one MsgPack map with two parts:
   - `manifest`, a metadata map. The packager writes `format` and `format_version`; everything else is caller-defined string fields, recorded but never interpreted by the engine. For graphs, the convention is that **`graph_id` holds the graph ID** of the graph the package delivers (the id the deployment manifest lists, `POST /api/graph/{graph_id}` serves, and the file `<graph_id>.json` holds), with the other maps as its subgraphs. A set name, a version or a certification state can sit beside it;
   - `maps`, the packed maps, keyed by entry name.

   ```text
   { "manifest": { "format": "mercury-package", "format_version": "1", "graph_id": "quote", ... },
     "maps":     { "quote-fees.json": { ...keys sorted recursively... }, "quote.json": { ... } } }
   ```

   **Multiple maps are saved in sorted filename order.** Entry names are file names, ordered by their UTF-8 bytes (never OS listing order or locale collation). Because `maps` is keyed by name and every map is key-sorted, that order needs no separate rule. `manifest` sorts before `maps`, so a reader can read the metadata before decoding any map. A duplicate entry name, or a caller field named `format` or `format_version`, is an error.
3. **Canonical MsgPack profile, identical in every engine.**
   - **Keys** are text, in ascending order of their UTF-8 bytes (not Java's UTF-16 `String` order, which differs for supplementary characters). A non-text key is converted to text, as the existing serializer does; a collision after conversion, or a null key, is an error.
   - **Null values** are written as nil and never dropped (the general serializer omits null-valued entries unless `supportNulls` is set).
   - **Integers** use the smallest encoding, fixints first.
   - **Floats** are float64 and finite only: a `Float` (which `MsgPack.java` packs as float32), NaN and Infinity are rejected.
   - **Text and bytes** are MsgPack str and bin, each with the shortest header.
   - **Exact numbers** travel as strings, following RFC-0001 including its zero rule, a zero of any scale being `"0"` (the existing serializer already packs `BigInteger` and `BigDecimal` as strings, but writes a `BigDecimal` zero as `"0.00"`, so the packager applies the rule itself), and a date is an ISO-8601 string. No extension types, no timestamps; booleans and nil use their single forms.
   - **Any other type** is rejected, naming its path, rather than stringified.
4. **Integrity is outside the packager, entirely the user application's decision (Eric, 2026-10-01).** Nothing inside the package refers to how it is protected: no signature, hash, key id or timestamp, and the framework carries no hash mode, no verifier seam and no key. The packager produces deterministic bytes; the application chooses whether to protect those **exact stored bytes**, with which algorithm (SHA-256 recorded in a release record or a sidecar, a detached signature from its own signer, a KMS, `openssl`, or nothing), and where the proof is kept. An earlier draft had the engine implement `none` and `hash` and call a route-addressed verifier for `signature`; that is withdrawn so that the framework owns no algorithm, key rotation or fail-closed policy, which belong to the field.
5. **Reading.** `unpack` returns ordered maps (insertion order preserved, never a hash-ordered map) and the manifest. A **strict** read, the default, also re-encodes the decoded content canonically and rejects the package if the bytes differ, so an accepted package has exactly one byte form; a non-strict read decodes anything that is valid in the profile's types. The decoder rejects a non-text key, a duplicate key, an extension type, bytes after the value and nesting beyond a fixed bound (64), so untrusted bytes cannot exhaust the stack.
6. **Tooling.** *Deferred.* A `pack` that builds a package from a folder or a list of files (JSON to start with) and an `unpack` that writes the maps back as readable JSON, exact numbers kept as text, are the natural next step; the source files remain what people read and certify, and the package is only the deployment artifact.
7. **Parity, and the Rust interop.** One vector file, `canonical-package-vectors.json`, is **byte-identical in the Java and Rust repositories** and run by both engines' suites. Its expected bytes come from an **independent encoder written from this specification**, not from either engine, so an engine that matches the file matches every other engine that does. It holds:
   - values: integers at every encoding boundary, float64 including `-0.0`, str and bin header boundaries, Unicode (and an unnormalized `e` + combining accent), map and list header boundaries (15 and 16), nulls kept, keys sorted at depth, keys whose UTF-8 and UTF-16 orders differ, exact numbers as strings (the RFC-0001 zero rule included);
   - packages: the graph-set example in both entry orders (same bytes), entry names ordered by UTF-8 bytes, manifest fields sorted, an empty package, each with its expected hex and SHA-256;
   - a **generated corpus** of 60 packages of random nested documents from a seeded generator, a differential test that shows a divergence in a rarely used corner of the profile as a byte difference;
   - rejection cases: `Float`, NaN, Infinity, an unsupported type, a duplicate entry name, a caller field named `format` or `format_version`; and on read, non-canonical integer width, out-of-order keys, a float32, trailing bytes, a non-map, a missing or extra top-level key, a wrong format or version, a non-text manifest field, an entry that is not a map, a duplicate key, a non-text key, an extension type and a truncated package. The cases only a strict read rejects are marked.

   The Rust twin runs the same file; an engine-to-engine drive (the same documents packed by each engine, the bytes compared, and each engine's strict read accepting the other's bytes) follows with it.

A decision would commit every engine to the canonical profile as a wire contract, the `manifest` + `maps` structure, and the rule that integrity never lives inside the package. It adds one packager per engine and no dependency.

**First consumer, deliberately out of scope.** A graph-set loader is the intended first consumer and would be raised as its own RFC. It would register all graphs or none, define precedence against `graph.model.automation` manifests (the later-loaded source wins, as for manifest lists), verify that `graph_id` names one of the packed graphs, and could also check static `graph.extension` targets at load, declaring that it cannot see through `flow://` or targets computed at run time.

**Deferred until field use asks for it:** the folder `pack` and `unpack` tooling; trusted timestamps; per-entry records or hashes in the manifest, to show which map changed between versions; hot reload; two versions loaded side by side; compression.

**Options.**
- *(a) A generic canonical packager, integrity external and optional (above).* One artifact, and the framework owns no signature scheme. **Recommended.**
- *(b) An embedded signature envelope, `[payload, signature]`.* One file carries its own signature, but the format is bound to one scheme and the framework owns algorithms, key rotation and timestamps; the `none` and `hash` modes need a degenerate envelope.
- *(c) Canonical JSON (RFC 8785) plus an external hash.* Human-readable and diffable, but it needs a canonicalizer in each engine, its number rules conflict with strings for exact numbers (RFC-0001), and MsgPack is already the bus format in Java and Rust.
- *(d) A zip of files plus a manifest of per-entry hashes.* Readable without a decoder, but it needs a two-level hash scheme, because zip bytes vary with entry order and timestamps.
- *(e) Leave it to adopters.* No engine change, but each adopter re-solves canonical encoding.

**Settled (Eric, 2026-10-01).** Digital signature, the hash mode and the verifier seam are out of the packager and left to the user application (item 4); the work is the key ordering and the packaging into MsgPack; byte-for-byte compatibility with the Rust engine is a requirement and is enforced by the shared vector file and a later engine-to-engine drive (item 7).

*Implemented as proposed in the first Java PR, to confirm at review:*
- `graph_id` stays a documented convention that the packager never interprets, because a graph-set loader can check it against the packed entry names.
- A strict read is the default.
- `Float` and non-finite doubles are rejected. The packager is faithful to a value's type, so the integer 1 and the float 1.0 are different content, and strings are written as given (no Unicode normalization); both are declared in the class documentation.
- `msgpack-core` and `rmpv` both choose the smallest integer encoding: checked on 21 integers at every encoding boundary and the str headers, all identical bytes (the vectors run the same cases in both engines).
- The Java packager is `org.platformlambda.core.serializers.CanonicalPackager` beside `MsgPack`: a builder (`manifest`, `add`, `build`), `encode`, `decode` and `unpack`; it needs no new dependency.
- The `mercury-python` and `mercury-nodejs` language packs do not carry it: they are minimalist LLM extensions that serve functions to a Java or Rust application over Event-over-HTTP, as if local, and carry no event script or minigraph, so they never read a graph package. (`mercury-go` is the agent-memory tool, not a language pack.)

**Resolution.**

---

## RFC-0001 — Exact decimal arithmetic: a `DECIMAL:` statement for `graph.math` and an explicit plugin family
**Status:** Promoted → ADR-0025 · **Raised:** 2026-09-30 · **Serves:** vision-mercury-composable · **Thread:** `decimal-mode`
<!-- id: rfc-0001 | status: promoted | thread: decimal-mode -->

**Motivation.** Knowledge graphs are a natural home for financial rules (settling contracts, rates, fees). Money and rate math must be **exact**, and **reproducible bit-for-bit across runs and across engines**. Today every numeric path is IEEE double or `long`:

| Where | Current behaviour | Consequence |
|---|---|---|
| Math dialect values | `Value.NumberValue(double)`; literals go through `Double.parseDouble` (`minigraph/math/Value.java`, `Parser.java:333`) | `0.1 + 0.2` gives `0.30000000000000004` |
| Dialect functions | `MathFunction.apply(double...)`; `round` is `Math.round` (`EvalContext.java:33`) | `round(-2.5)` gives **-2** |
| Arithmetic plugins | `SimplePluginUtils.promoteNumber`: integers become `Long`, strings try `Long` then `Double`; `reduceNumbers` uses `long` math when every input is whole, `double` otherwise | `f:div(7, 2)` gives **3** (integer truncation); the Rust twin asserts the same |
| `f:round` | `BigDecimal.valueOf(double).setScale(dp, HALF_UP).doubleValue()` (`RoundNumbers.java`) | Returns a double again. `f:round(-2.5)` gives **-3**, disagreeing with the dialect's `round(-2.5)` = -2 |
| Rendering | `BigDecimal.valueOf(d).stripTrailingZeros().toPlainString()` (`Evaluator.java:239`) | Deterministic, but renders the double's value, not the intended decimal |

**This reopens an earlier ruling, deliberately.** The current guidance (ruled 2026-09-25, recorded with the typed-arithmetic decision) is that exact-decimal money belongs in a `graph.task` function and that `BigDecimal` is not added to the dialect, keeping the math package minimal. That works, but it hides the formulas in code, while the point of a knowledge graph is that owners read and certify rules on the graph itself. This RFC asks whether that trade still holds; option (c) below is the existing ruling.

**Proposal.** Add a **`DECIMAL:` statement** to `graph.math`, the high-precision `COMPUTE:`, with identical semantics in every engine that executes graphs. `COMPUTE:` keeps its meaning, so existing graphs are untouched by construction: there is no graph-level mode, no application default and no `CompileGraph` check. Exactness is declared where it is needed, statement by statement, the way `CONDITION:` declares a boolean instead of letting `COMPUTE:` infer one from its operators. One rule does reach existing statements: numeric strings compare as numbers (item 3), a READ item at release.

1. **The statement.** `DECIMAL: var -> expression` evaluates the expression with exact decimal arithmetic and stores the result at `{node}.result.{var}` as a canonical decimal string (item 6). The tag joins `COMPUTE:` and `CONDITION:` in the statement recognizer, and it is valid in `graph.math` nodes only (`graph.js`, which is deprecated, already rejects any tag it does not know). A `DECIMAL:` statement computes a number: a comparison may appear inside a ternary test, but a boolean result is an error naming the statement (`CONDITION:` is the boolean statement).
2. **Value model and reading.** In Java, add `DecimalValue(BigDecimal)` and a decimal literal node beside the double ones (`Value` and `Expr` are sealed with exhaustive switches, so the compiler finds every site) and evaluate a `DECIMAL:` statement with a **parallel decimal evaluator**, leaving the double path untouched. `graph.math` renders each `{selector}` into the expression text and then parses it, so the parser never sees a Java type: numeric literals, substituted numbers included, parse **from text** straight to a decimal (the lexer already reads fraction and exponent forms), never through a double. A `Long` or `Integer` value is exact. A `Double` or `Float` value is inexact by nature, and its Java type is gone once a value is text, so `GraphMath` converts such a value before it renders it: through the shortest decimal text it prints as, at its minimal scale and in plain notation (`5.0E-4` is `0.0005`, `100.0` is `100`), so a number and the same decimal written as a string give the same answer. This is the lenient choice, and it is **declared, not enforced**: the guide says that a double already computed in floating point (a `COMPUTE:` result, a client's own arithmetic, a JSON number longer than a double holds) is only as exact as that computation, so sending money as strings is a conscious decision, and `f:validate` asserts it for callers who insist. Every value, number or string, is rendered into the statement text, so the evaluator sees literals and not sources: a substituted string is indistinguishable from one the author typed. The read rule therefore lives in the evaluator, and substitution does not change. A decimal read from the state machine works in either form: a `BigDecimal` renders as a number literal, and its canonical string renders as a number literal in an arithmetic position and as a string literal where text is quoted (IF, CONDITION, a comparison). A `DECIMAL:` statement computes numbers, so its selectors always render in the arithmetic (unquoted) form, even inside a ternary test, and `+` always adds; a string literal the author types in the expression converts if it is a **canonical decimal** (plain notation: an optional minus sign, digits without leading zeros, an optional fraction), and any other non-numeric operand is an error naming it. Outside a `DECIMAL:` statement a string that is not a canonical number is never converted: in an arithmetic position it still renders unquoted and parses as a number, exactly as today, and in a boolean position it stays text.
3. **Operators.** `+ - *` are exact, with a **specified result scale** (`+`/`-`: the larger operand scale; `*`: the sum of scales) so both engines agree; a scale is never negative (a literal such as `1e3` parses to `1000`, scale 0). `/` never truncates (`7 / 2` gives `3.5`): a quotient that terminates is exact, at the smallest scale that represents it and is not below the dividend's scale minus the divisor's (`BigDecimal.divide`), and a non-terminating one uses a fixed division context of 34 significant digits (decimal128) with HALF_EVEN, until the author applies an explicit scale; division by zero is an error naming the operator. `%` is defined on decimals. `**` and `pow` with a whole-number exponent are exact, the exponent bounded (proposed 999) so an expression cannot exhaust memory. Comparisons use numeric value (`2.0 == 2.00` is true).
   **Numeric strings compare as numbers, everywhere (Eric, 2026-09-30).** In `==`, `!=`, `<`, `<=`, `>` and `>=`, in IF, CONDITION, `COMPUTE:` and `DECIMAL:` alike, a string operand that is a canonical number is converted to a number before the comparison, so `'200' == '200'`, `200 == 200` and `200 == '200'` are the same comparison, and `'9.5' > '10.25'` compares 9.5 with 10.25. A string that is not a canonical number keeps today's behaviour: two strings compare as text, and a number against such a string is an error naming the operands. Today `200 == '200'` and `'9.5' > 10.25` are errors, `'9.5' > '10.25'` is true (text order) and `'1.0' == '1'` is false. The comparison is exact in both evaluators: a number operand is taken as the decimal text it prints as (`BigDecimal.valueOf(double)`) and a string as the decimal it spells, so two distinct 20-digit ids never collapse into equal numbers, and no precision guard is needed. READ at release: a graph that compared two numeric strings as text now compares them as numbers.
4. **Rounding is always explicit.** `round(x, scale, mode)` takes `HALF_UP | HALF_EVEN | HALF_DOWN | UP | DOWN | CEILING | FLOOR` and returns a decimal at exactly that scale. The one- and two-argument forms are refused in a `DECIMAL:` statement, so there is no hidden default to configure; `COMPUTE:` keeps `Math.round`. `floor`, `ceil`, `abs`, `min` and `max` are exact.
5. **Operations that cannot be exact are refused, at run time.** In a `DECIMAL:` statement `sqrt`, `log`, `log10`, `exp`, trigonometry, `pow` with a fractional exponent, `random()`, and the constants `PI` and `E` raise an error naming the node and the statement. They are refused at run time rather than gated at compile time: a compile-time check would be a partial gate that cannot see values arriving through mapped inputs. An author who needs them keeps that step in a `COMPUTE:` statement or uses a `graph.task` function.
6. **Strings at rest, `BigDecimal` only in flight.** A `DECIMAL:` result is stored in the state machine as its canonical string, never as a `BigDecimal`. The state machine does not keep Java types across a serialization boundary: `graph.suspend` persists the `model` namespace to a pluggable store and `graph.resume` merges it back without conversion, and every `graph.task` and `graph.extension` hop is an event envelope. The platform's MsgPack serializer writes a `BigDecimal` as a string (a zero at scale 2 as `"0.00"`) and reads it back as a `String`, and a small `Long` comes back as an `Integer`, so a `BigDecimal` kept in memory would change type at a suspension point, and IF, which quotes text, would compare it differently after the resume than before. A string is the same before and after, and the read rule in item 2 makes it a number wherever `graph.math` meets it. The JSON serializer also writes a `BigDecimal` as a plain-notation string, so a decimal never travels as a JSON or MsgPack **number**, which standard parsers and MsgPack's float types would round-trip through a double. That covers graph JSON, the exported graph, the run's `output.*`, a suspension record, and a canonical MsgPack package (RFC-0002); literals in node properties are text already and parse exactly.
   **Canonical string form:** plain notation, never scientific; keeps its computed scale (`round(10.5, 2, HALF_UP)` gives `"10.50"`); **a zero of any scale is `"0"`** (Eric, 2026-09-30). The JSON serializer already writes that; the MsgPack serializer writes a `BigDecimal` zero as `"0.00"`, which is one more reason to store the canonical string rather than a `BigDecimal`, and the canonical packager in RFC-0002 applies the zero rule itself. This is also the grammar item 2 reads, so what a `DECIMAL:` statement writes is always what it reads back. A `BigDecimal` has no negative zero, so nothing further needs normalizing.
7. **No mode to mix.** There is no graph-level mode, so there is nothing to mix, nothing for `CompileGraph` to check and nothing for a subgraph to inherit: exactness travels with the data (canonical strings at rest), and each statement declares its arithmetic. The hazards are an author using `COMPUTE:` on decimal strings, which computes in binary floating point exactly as it does today, and a double that was computed in floating point reaching a `DECIMAL:` statement (item 2). The guide says to use `DECIMAL:` for money, to send money as strings, and to keep a `COMPUTE:` result out of a `DECIMAL:` statement, and it names `COMPUTE:` as the floating-point statement; the limits are **declared in the guide, not enforced**.
8. **Plugins.** Add an explicit decimal family for flows and mapper nodes: `f:decimalAdd`, `f:decimalSubtract`, `f:decimalMultiply`, `f:decimalDiv`, `f:decimalMod`, `f:decimalRound(x, scale, mode)` and `f:decimalCompare` (camelCase, like every simple plugin; Eric, 2026-09-30). The existing `f:add` family keeps its behaviour, so nothing changes silently. Explicit names beat a hidden mode for plugins, which run outside any one graph's context. Today `promoteNumber` rejects a `BigDecimal` by name, so the `f:add` family fails loudly on a decimal instead of degrading it to a double. The plugins are the second delivery step (see Delivery).
9. **Parity, enforced by shared test vectors.** A committed JSON file of `{statement or plugin call, inputs, expected canonical string or expected error}` (an error is an engine-neutral code that each engine matches against its own message) runs in **every** executing engine's test suite, alongside the drift-tested claims registry. It must cover: the classic traps (`0.1 + 0.2`, `1.005` at scale 2, `±2.5` and `±0.5` in every rounding mode, `1/3` and `2/3` in the division context); **scale propagation through `+ - * /`**; zero at several scales; large magnitudes and the exponent bound; how a `Long`, a `BigDecimal`, a canonical string, a non-canonical string and a converted `Double` behave in a `DECIMAL:` statement; the comparison matrix in both evaluators (a number, a canonical string, a non-canonical string and a non-numeric string on each side of the six operators, long digit ids included); a MsgPack round trip of a `model` holding decimal strings, as `graph.suspend` and `graph.resume` do it; the refused-operation errors; and unchanged `COMPUTE:` results.
10. **A side fix independent of decimal arithmetic:** document, or align, the dialect `round(-2.5) = -2` versus plugin `f:round(-2.5) = -3` discrepancy.

**Delivery (Eric, 2026-09-30).** The first implementation is Java `graph.math` only: items 1 to 7 and 9. It is the `DECIMAL:` statement in `GraphMath` and the statement recognizer, the decimal evaluator in the `minigraph-playground-engine` math package, the docs (`skills-reference.md`, the in-Playground help and `minigraph-commands.json`), a claim for the statement (the `math-expression-dialect` claim keeps describing `COMPUTE:`), and the shared vectors; item 3's comparison rule is part of it and reaches the double evaluator. The `f:decimal*` plugins (item 8) and the Rust twin follow as separate PRs against the same vectors. `graph.data.mapper` arithmetic and `f:lookup` numeric keys are unchanged.

A decision would commit every executing engine to one written decimal specification (the `DECIMAL:` statement, result scales, division context, rounding modes, canonical string form, strings at rest, the numeric-string comparison rule), a shared conformance-vector file maintained in lock-step, and one new `Value` variant plus a plugin family per engine.

**Options.**
- *(a) A `DECIMAL:` statement (above).* Explicit and declared where it is needed, like `CONDITION:`; no graph-level state, and existing graphs untouched. **Recommended.**
- *(b) Replace double with decimal everywhere.* Simplest to reason about, but it changes every statement's result from a number to a string and the rendering of every existing result.
- *(c) Leave the core unchanged and ship a decimal function library for `graph.task`* (the existing ruling). No engine change, but formulas become opaque code again, and every adopter reinvents rounding and serialization.
- *(d) Fixed-point integers (minor units, e.g. cents, as `long`).* Exact and fast for sums, but awkward for rates, percentages, per-period amounts and currencies of different scale, and division still needs a rounding policy.
- *(e) A graph-level `numeric: decimal` mode.* Zero compatibility risk, but one more thing to configure, and a mixed-mode limit between graphs that `CompileGraph` cannot see; the statement declares exactness where it is needed.
- *(f) Type-directed arithmetic with nothing declared.* No new syntax, but a graph that passes numeric strings into arithmetic would flip from a number to a string result.

**Settled (Eric, 2026-09-30).** A fixed decimal128 division context (34 digits, HALF_EVEN) with no configuration; the computed scale kept in canonical output, and a zero of any scale written `"0"`; a `Double` accepted in a `DECIMAL:` statement through its shortest decimal text, declared in the guide as a conscious decision (lenient); scope stays `graph.math`, so `graph.data.mapper` arithmetic and `f:lookup` numeric keys are unchanged; delivery in three separate PRs against the same vectors, Java `graph.math` first, then the `f:decimal*` plugins, then the Rust twin; decimals stored as canonical strings at rest, because `graph.suspend` and every hop serialize the state (item 6); numeric strings comparing as numbers (item 3); and the `DECIMAL:` statement, the high-precision `COMPUTE:`, in place of a graph-level mode (item 1), with rounding always explicit and no graph-level default.

*Still open:*
- **The comparison rule and existing statements.** Implemented as proposed in the first PR: IF, CONDITION and `COMPUTE:` get the rule too, so `'9.5' > '10.25'` and `'1.0' == '1'` change for existing graphs (a READ item at release). The alternative leaves them as they are today, and `{model.status} == '200'` keeps failing there; confirm at review.
- **Rust type.** The Rust twin would use an arbitrary-precision crate. `bigdecimal` matches Java `BigDecimal` semantics best and is already in that workspace's lockfile (0.4.10, through `apache-avro`); a fixed 96-bit type (e.g. `rust_decimal`, about 28 digits) cannot reproduce a 34-digit context. *(verify `bigdecimal`'s rounding-mode coverage and result-scale behaviour against the shared vectors when the Rust PR starts)*
- **Exponent bound.** Proposed 999 for `**` and `pow`; confirm when the evaluator lands.
- **Release.** Which version carries each delivery step? The language packs (`mercury-python`, `mercury-nodejs`) need no engine change for the decimal work: they host functions over Event-over-HTTP and never evaluate a statement or a plugin; a function of theirs that returns money returns a plain-notation string, which a `DECIMAL` statement or a decimal plugin reads (`mercury-go` is the agent-memory tool, not a language pack).

**Resolution.** Promoted → ADR-0025 (2026-09-30): option (a), the `DECIMAL:` statement. Delivered in PR #471 (Java `graph.math`, with the numeric-string comparison rule) and PR #472 (item 10, the `round` alignment); items 8 (the `f:decimal*` plugins) and the Rust twin follow as separate PRs against the shared vectors.
