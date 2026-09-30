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

## RFC-0002 — Canonical MsgPack packager: sorted-key maps and a manifest as one deterministic byte array
**Status:** Open · **Raised:** 2026-09-30 · **Serves:** vision-mercury-composable · **Thread:** `canonical-msgpack-packager`
<!-- id: rfc-0002 | status: open | thread: canonical-msgpack-packager -->

**Motivation.** Some installations must promote a *set* of related documents (knowledge graphs today, but equally flows, rules or tables) as one artifact whose identity can be recorded, compared and, where the field wants it, protected. Loose files cannot do that. Each one is deployed and validated alone, JSON text is not a stable artifact (key order and whitespace vary), and there is no single thing to hash. MsgPack does not fix this by itself: a map has no guaranteed order, and one value has several valid byte encodings, so two packagings of the same content can differ, and their hashes with them. Both engines already use MsgPack (Java through `msgpack-core`, `platform-core/.../serializers/MsgPack.java`; Rust through `rmpv`, `crates/platform-core`), so the smallest useful piece is a **deterministic packager**. Whether a package is then hashed, signed or neither is a deployment decision and stays outside it.

**Proposal.**

1. **The packager.** A small utility beside `MsgPack` in platform-core (and its Rust twin) that turns maps into one byte array. It first converts every map to an **ordered map with keys sorted recursively** (at every depth, including maps inside lists; list order is kept), then writes the result as MsgPack under the canonical profile (item 3). The same content gives the same bytes, in either engine.
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
4. **Integrity is external and optional.** Nothing inside the package refers to how it is protected: no signature, hash, key id or timestamp. The field chooses one mode per deployment, applied to the **exact stored bytes**:
   - `none`: the channel or artifact repository is trusted;
   - `hash`: the SHA-256 of the bytes, recorded outside the package (a release record, a sidecar `<name>.sha256`, a config value or an environment variable) and compared on load. It detects corruption and substitution, and authenticates only as far as the recorded value is trusted;
   - `signature`: a detached signature over the bytes, made by the field's own signer (a CI job, a KMS or HSM, `openssl`; any algorithm) and stored beside the package (`<name>.sig`).

   The engine implements `none` and `hash` itself (JDK `MessageDigest`, no new dependency) and reaches `signature` through a **verifier seam**: a route-addressed function that receives the bytes and the detached signature and answers accept or reject. The engine never holds a signing key and never signs, so algorithms, key rotation and KMS integration stay in the field's hands and out of the framework's release cycle. A configured check that fails, or `signature` mode with no verifier, fails closed: nothing is decoded.
5. **Reading.** `unpack` returns ordered maps (insertion order preserved, never a hash-ordered map) after the configured integrity check. A **strict** read also re-encodes the decoded content canonically and rejects the package if the bytes differ, so an accepted package has exactly one byte form.
6. **Tooling.** `pack` builds a package from a folder or a list of files (JSON to start with); `unpack` writes the maps back as readable JSON, exact numbers kept as text, for review and audit. The source files remain what people read and certify; the package is only the deployment artifact. Hashing and signing use the field's own tools.
7. **Parity.** Shared test vectors run in every engine's suite:
   - a fixed set of maps with expected bytes (hex) and SHA-256, covering maps nested in lists, keys whose UTF-8 and UTF-16 orders differ, and integer and str/bin header boundaries;
   - rejection cases: `Float`, NaN, an unsupported type, a duplicate entry name, a caller field named `format`, and non-canonical bytes under a strict read;
   - integrity cases: a hash mismatch, a verifier reject, and `signature` mode without a verifier, each decoding nothing.

A decision would commit every engine to the canonical profile as a wire contract, the `manifest` + `maps` structure, and the rule that integrity never lives inside the package. It adds one packager per engine and no dependency.

**First consumer, deliberately out of scope.** A graph-set loader is the intended first consumer and would be raised as its own RFC. It would register all graphs or none, define precedence against `graph.model.automation` manifests (the later-loaded source wins, as for manifest lists), verify that `graph_id` names one of the packed graphs, and could also check static `graph.extension` targets at load, declaring that it cannot see through `flow://` or targets computed at run time.

**Deferred until field use asks for it:** trusted timestamps; a reference verifier function (for example RSA over `CryptoApi`); per-entry records or hashes in the manifest, to show which map changed between versions; hot reload; two versions loaded side by side; compression.

**Options.**
- *(a) A generic canonical packager, integrity external and optional (above).* One artifact, and the framework owns no signature scheme. **Recommended.**
- *(b) An embedded signature envelope, `[payload, signature]`.* One file carries its own signature, but the format is bound to one scheme and the framework owns algorithms, key rotation and timestamps; the `none` and `hash` modes need a degenerate envelope.
- *(c) Canonical JSON (RFC 8785) plus an external hash.* Human-readable and diffable, but it needs a canonicalizer in each engine, its number rules conflict with strings for exact numbers (RFC-0001), and MsgPack is already the bus format in Java and Rust.
- *(d) A zip of files plus a manifest of per-entry hashes.* Readable without a decoder, but it needs a two-level hash scheme, because zip bytes vary with entry order and timestamps.
- *(e) Leave it to adopters.* No engine change, but each adopter re-solves canonical encoding.

*Open questions:*
- Should `graph_id` stay a documented convention that the packager never interprets (proposed), or should the packager offer a typed field for it? A graph-set loader can already check it, because a graph's id is its file name without `.json` today.
- Should a strict read be the default (proposed, since it is cheap) or opt-in?
- Is rejecting `Float` and non-finite doubles right, or should a `Float` be widened through its shortest decimal text?
- Should the verifier seam be a route (proposed), or a built-in RSA verify with a configured public key through `CryptoApi`?
- Do `msgpack-core` (`packInt` / `packLong`) and `rmpv` both choose the smallest integer encoding? *(verify; the vectors will show it)*
- Which engines carry it? Java and Rust are the reference pair (the Node.js pack uses `@msgpack/msgpack`). Which of `mercury-go`, `mercury-python` and `mercury-nodejs` must at least read a package and pass the vectors?

**Resolution.**

---

## RFC-0001 — Decimal mode: exact decimal arithmetic for `graph.math` and the arithmetic plugins
**Status:** Open · **Raised:** 2026-09-30 · **Serves:** vision-mercury-composable · **Thread:** `decimal-mode`
<!-- id: rfc-0001 | status: open | thread: decimal-mode -->

**Motivation.** Knowledge graphs are a natural home for financial rules (settling contracts, rates, fees). Money and rate math must be **exact**, and **reproducible bit-for-bit across runs and across engines**. Today every numeric path is IEEE double or `long`:

| Where | Current behaviour | Consequence |
|---|---|---|
| Math dialect values | `Value.NumberValue(double)`; literals go through `Double.parseDouble` (`minigraph/math/Value.java`, `Parser.java:333`) | `0.1 + 0.2` gives `0.30000000000000004` |
| Dialect functions | `MathFunction.apply(double...)`; `round` is `Math.round` (`EvalContext.java:33`) | `round(-2.5)` gives **-2** |
| Arithmetic plugins | `SimplePluginUtils.promoteNumber`: integers become `Long`, strings try `Long` then `Double`; `reduceNumbers` uses `long` math when every input is whole, `double` otherwise | `f:div(7, 2)` gives **3** (integer truncation); the Rust twin asserts the same |
| `f:round` | `BigDecimal.valueOf(double).setScale(dp, HALF_UP).doubleValue()` (`RoundNumbers.java`) | Returns a double again. `f:round(-2.5)` gives **-3**, disagreeing with the dialect's `round(-2.5)` = -2 |
| Rendering | `BigDecimal.valueOf(d).stripTrailingZeros().toPlainString()` (`Evaluator.java:239`) | Deterministic, but renders the double's value, not the intended decimal |

**This reopens an earlier ruling, deliberately.** The current guidance (ruled 2026-09-25, recorded with the typed-arithmetic decision) is that exact-decimal money belongs in a `graph.task` function and that `BigDecimal` is not added to the dialect, keeping the math package minimal. That works, but it hides the formulas in code, while the point of a knowledge graph is that owners read and certify rules on the graph itself. This RFC asks whether that trade still holds; option (c) below is the existing ruling.

**Proposal.** Add an **opt-in decimal mode** with identical semantics in every engine that executes graphs. The default stays `double`, so existing graphs and flows are unaffected.

1. **Enabling it.** A graph-level switch on the root node, `numeric: decimal`, plus an application default `graph.math.numeric = double | decimal`. The graph setting wins. `CompileGraph` records the mode, and it is part of the exported JSON, so any content hash covers it. `CompileGraph` rejects any other value of `numeric`.
2. **Value model.** In Java, add `DecimalValue(BigDecimal)` and a decimal literal node beside the double ones (`Value` and `Expr` are sealed with exhaustive switches, so the compiler finds every site) and evaluate decimal-mode expressions with a **parallel decimal evaluator**, leaving the double path untouched. `graph.math` renders each `{selector}` into the expression text and then parses it, so the parser never sees a Java type: numeric literals, substituted numbers included, parse **from text** straight to a decimal (the lexer already reads fraction and exponent forms), never through a double. A `Long` or `Integer` value is exact. A `Double` or `Float` value is lossy by definition and is **rejected** at substitution, the only place the type is known, with an error naming the selector and the node. A lenient mode that converts a double through its shortest text is deferred until a field asks for it, so strict is the only mode in the first version. A numeric **string** is exact in an arithmetic position (it substitutes unquoted) and stays a string in a boolean position (IF, CONDITION, a comparison: text is quoted there), so it is never coerced; convert it first, for example `COMPUTE: amount -> {input.body.amount}`, which stores a decimal.
3. **Operators.** `+ - *` are exact, with a **specified result scale** (`+`/`-`: the larger operand scale; `*`: the sum of scales) so both engines agree; a scale is never negative (a literal such as `1e3` parses to `1000`, scale 0). `/` never truncates (`7 / 2` gives `3.5`); a non-terminating quotient uses a fixed division context of 34 significant digits (decimal128) with HALF_EVEN, until the author applies an explicit scale, and division by zero is an error naming the operator. `%` is defined on decimals. `**` and `pow` with a whole-number exponent are exact, the exponent bounded (proposed 999) so an expression cannot exhaust memory. Comparisons use numeric value (`2.0 == 2.00` is true).
4. **Rounding is always explicit.** `round(x, scale, mode)` takes `HALF_UP | HALF_EVEN | HALF_DOWN | UP | DOWN | CEILING | FLOOR` and returns a decimal at exactly that scale. The two-argument form uses a graph-level `rounding` default (proposed HALF_EVEN). `floor`, `ceil`, `abs`, `min` and `max` are exact.
5. **Operations that cannot be exact are refused, at run time.** In decimal mode `sqrt`, `log`, `log10`, `exp`, trigonometry, `pow` with a fractional exponent, `random()`, and the constants `PI` and `E` raise an error naming the node and the mode. They are refused at run time rather than gated at compile time: a compile-time check would be a partial gate that cannot see values arriving through mapped inputs. An author who needs them keeps that node in double mode or uses a `graph.task` function.
6. **Exact in memory, strings on every serialized surface.** A decimal result is held in the state machine as a `BigDecimal`, not as a string. A `Number` renders unquoted in every position, so `IF: {fee.result.total} > 100` compares numbers, where a plain string would be quoted and fail as a string-versus-number comparison. Strings appear at serialization, and the platform already does this: the JSON serializer writes a `BigDecimal` as its plain-notation string and the MsgPack serializer packs it as a string, so a decimal never travels as a JSON or MsgPack **number**, which standard parsers and MsgPack's float types would round-trip through a double. That covers graph JSON, the exported graph, the run's `output.*`, and a canonical MsgPack package (RFC-0002). Import parses the string straight into a `BigDecimal`, so a value survives any number of export/import cycles unchanged; literals in node properties are text already and parse exactly.
   **Canonical string form:** plain notation, never scientific; keeps its computed scale (`round(10.5, 2, HALF_UP)` gives `"10.50"`); **a zero of any scale is `"0"`** (Eric, 2026-09-30). That is what the platform's JSON serializer already writes; the MsgPack serializer writes a `BigDecimal` zero as `"0.00"`, so the canonical packager in RFC-0002 applies the zero rule itself. A `BigDecimal` has no negative zero, so nothing further needs normalizing.
7. **Mixed modes.** The mode belongs to the graph. A decimal-mode graph never produces a double: what leaves it is a `BigDecimal` or its string. A double-mode graph that reads a `BigDecimal` keeps today's behaviour (the value renders as text and computes as a double), so no existing graph changes; crossing from decimal to double is the author's explicit act, and the guide says so. A decimal-mode graph that delegates to a subgraph through `graph.extension` should delegate to a decimal-mode subgraph. `CompileGraph` cannot see that, because targets resolve at call time and `flow://` is opaque, so the limit is **declared in the guide, not enforced**.
8. **Plugins.** Add an explicit decimal family for flows and mapper nodes: `f:decimal.add`, `.sub`, `.mul`, `.div`, `.mod`, `.round(x, scale, mode)`, `.compare`. The existing `f:add` family keeps its behaviour, so nothing changes silently. Explicit names beat a hidden mode for plugins, which run outside any one graph's context. Today `promoteNumber` rejects a `BigDecimal` by name, so the `f:add` family fails loudly on a decimal instead of degrading it to a double. The plugins are the second delivery step (see Delivery).
9. **Parity, enforced by shared test vectors.** A committed JSON file of `{expression or plugin call, inputs, mode, expected canonical string or expected error}` runs in **every** executing engine's test suite, alongside the drift-tested claims registry. It must cover: the classic traps (`0.1 + 0.2`, `1.005` at scale 2, `±2.5` and `±0.5` in every rounding mode, `1/3` and `2/3` in the division context); **scale propagation through `+ - * /`**; zero at several scales; large magnitudes and the exponent bound; how a `Long`, a rejected `Double` and a numeric string behave in an arithmetic and in a boolean position; the refused-operation errors; and the mixed-mode cases.
10. **A side fix independent of decimal mode:** document, or align, the dialect `round(-2.5) = -2` versus plugin `f:round(-2.5) = -3` discrepancy.

**Delivery (Eric, 2026-09-30).** The first implementation is Java `graph.math` only: items 1 to 7 and 9, in the `minigraph-playground-engine` math package, `GraphMath` and `CompileGraph`, with the docs, a claim for decimal mode (the `math-expression-dialect` claim keeps describing double mode) and the shared vectors. The `f:decimal.*` plugins (item 8) and the Rust twin follow as separate PRs against the same vectors. `graph.data.mapper` arithmetic and `f:lookup` numeric keys are unchanged.

A decision would commit every executing engine to one written decimal specification (result scales, division context, rounding modes, canonical string form, strings on every serialized surface), a shared conformance-vector file maintained in lock-step, and one new `Value` variant plus a plugin family per engine.

**Options.**
- *(a) Opt-in decimal mode (above).* Backward compatible; graphs stay readable and exact. Needs one spec shared across engines. **Recommended.**
- *(b) Replace double with decimal everywhere.* Simplest to reason about, but breaks graphs that rely on double functions and current rendering.
- *(c) Leave the core unchanged and ship a decimal function library for `graph.task`* (the existing ruling). No engine change, but formulas become opaque code again, and every adopter reinvents rounding and serialization.
- *(d) Fixed-point integers (minor units, e.g. cents, as `long`).* Exact and fast for sums, but awkward for rates, percentages, per-period amounts and currencies of different scale, and division still needs a rounding policy.

**Settled (Eric, 2026-09-30).** The proposed defaults stand: a fixed decimal128 division context (34 digits, HALF_EVEN) with no per-graph configuration; the computed scale kept in canonical output, and a zero of any scale written `"0"`; strict by default, with lenient deferred; scope stays `graph.math`, so `graph.data.mapper` arithmetic and `f:lookup` numeric keys are unchanged; and delivery in three separate PRs against the same vectors, Java `graph.math` first, then the `f:decimal.*` plugins, then the Rust twin.

*Still open:*
- **Rust type.** The Rust twin would use an arbitrary-precision crate. `bigdecimal` matches Java `BigDecimal` semantics best and is already in that workspace's lockfile (0.4.10, through `apache-avro`); a fixed 96-bit type (e.g. `rust_decimal`, about 28 digits) cannot reproduce a 34-digit context. *(verify `bigdecimal`'s rounding-mode coverage and result-scale behaviour against the shared vectors when the Rust PR starts)*
- **Exponent bound.** Proposed 999 for `**` and `pow`; confirm when the evaluator lands.
- **Release.** Which version carries each delivery step? `mercury-go`, `mercury-python` and `mercury-nodejs` need at least the canonical string form, zero rule included.

**Resolution.**

