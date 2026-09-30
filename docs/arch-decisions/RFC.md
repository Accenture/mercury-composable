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

## RFC-0002 — Graph-set packages: a graph and its subgraphs as one signed MsgPack file
**Status:** Open · **Raised:** 2026-09-30 · **Serves:** vision-mercury-composable · **Thread:** none yet
<!-- id: rfc-0002 | status: open | thread: none -->

**Motivation.** In field installations each knowledge graph represents one business use case, and use cases are small; the 750-node readability guideline has not been a constraint, and a complex system is expressed as many discrete use-case graphs. Some installations, though, must certify a use case together with the subgraphs it calls, and deploy exactly what was certified. Today each graph is deployed and validated on its own:

- **Graphs load and are rejected one at a time.** `CompileGraph` compiles each id from a manifest separately, and a rejected graph answers 404. A parent can load while one of its subgraphs is rejected.
- **Subgraph targets are resolved only at call time.** `graph.extension` looks up its target when the node runs, so a missing subgraph shows up mid-run rather than at startup.
- **There is no integrity check at load.**

Both engines already use MsgPack for event envelopes (Java through `msgpack-core`, `platform-core/.../serializers/MsgPack.java`; Rust through `rmpv`), so a single canonical MsgPack byte array is the smallest possible package: one file, one hash, one signature, no archive format.

**Proposal: a deliberately minimal first version.**

1. **The package.** One file containing an outer MsgPack **array of two elements**, `[payload, signature]`.
   - `payload` is a MsgPack **binary** holding the canonical encoding (item 2) of an ordered map: `format` (e.g. `"mercury-graph-set"`) and `format_version`; `set_id`, `version`, and `entry` (the id of the graph that invokes the others); `graphs`, a map from graph id to graph, each in its `exportGraph()` structure (membership is declared, never inferred from graph ids); and `metadata`, an optional map of string fields the adopter defines, recorded but never interpreted by the engine.
   - `signature` is a map `{alg, key_id, value}`, covering **exactly the `payload` bytes as stored**, so it never covers itself.
   - The set's identity is the **SHA-256 of the `payload` bytes**. A verifier hashes those bytes directly and never re-encodes them. Per-graph hashes are not needed: changing any graph changes the payload hash.
2. **Canonical MsgPack profile, identical in every engine.** MsgPack allows several byte encodings of the same value and the hash depends on the exact bytes, so:
   - **maps:** every map at every depth, including node and relation properties, is written with keys in ascending order of their **UTF-8 bytes** (not Java's default UTF-16 `String` order, which differs for supplementary characters). Arrays keep their order; nodes and connections follow `exportGraph()` order;
   - **integers:** always the smallest encoding, positive and negative fixint first;
   - **numbers that need precision are strings**, following RFC-0001, so the payload holds no floats. A double-mode value that must be carried is float64 only, never float32 (today a Java `Float` packs as float32 and a `Double` as float64 in `MsgPack.java`, so the same number could otherwise encode two ways);
   - **text:** always MsgPack str, never bin. Binary appears only as the payload wrapper;
   - **nothing else:** no extension types, no timestamps (dates are ISO-8601 strings); booleans and nil use their single forms.
   - The decoder used for the load-time canonical check must **preserve key order**; a `HashMap`-based decode would defeat it.
3. **Loading.** Packages are listed in a new config key, `graph.set.automation` (`file:/` or `classpath:/` paths). For each package the engine: (1) decodes the outer array; (2) verifies the signature over the payload bytes against a configured public key (`graph.set.public.key`), rejecting unsigned or badly signed packages when a key is configured (RSA sign/verify already exists in `CryptoApi`); (3) decodes the payload and **re-encodes it canonically**, rejecting the package if the bytes differ, so every accepted package has exactly one byte form; (4) compiles each graph through the existing `CompileGraph` gate; (5) checks that every static `graph.extension` target that is not `flow://` is in `graphs`; (6) registers **all graphs or none**, logging which step failed.
4. **Precedence with manifests.** A package is its own allowlist, like a manifest. When a graph id appears in both a package and a `graph.model.automation` manifest, the **later-loaded source wins** (the manifest-list rule), with the same warning, and a rejected later copy leaves the id not executable. Packages load after manifests, in `graph.set.automation` order.
5. **Behaviour change to declare.** All-or-none registration differs from today's per-graph 404 behaviour, so it applies to packages only; manifests keep their current semantics. The distinction belongs in the release READ notes.
6. **Tooling.** `pack` canonical-encodes a set of graphs into a payload (signing is a separate, explicit step). `unpack` decodes a package back to readable graph JSON, numbers as strings, for review and audit. The JSON graphs remain the human-readable source people certify; the MsgPack file is only the deployment artifact.
7. **Parity.** A handful of shared test vectors, run by every engine's test suite: one graph set with its expected payload bytes (hex) and SHA-256; a tampered payload; a non-canonical encoding of the same set (float32, unsorted keys); a missing subgraph.

A decision would commit every engine to the canonical MsgPack profile, the `[payload, signature]` envelope, atomic set registration, and the load-time subgraph check. It reuses the MsgPack libraries and `CryptoApi` the engines already have.

**Declared limit (not enforced).** The load-time subgraph check sees only static targets. It cannot see through `flow://` or a target computed at run time, so a green check does not prove every call resolves. Per the project's posture that constraints are declared rather than partially enforced, the guide states this in its Design-rules voice.

**Deferred until field use asks for it:** trusted timestamps (RFC 3161, an optional field on the signature); a pluggable verifier or KMS/HSM signer; hot reload without restart; two versions of a set side by side; graphs shared across sets; `list` and offline `verify` commands; per-graph hashes.

**Options.**
- *(a) One canonical MsgPack file, `[payload, signature]` (above).* A single artifact and a single hash over stored bytes, using the serializer every engine already has. **Recommended.**
- *(b) A zip of sorted graph JSON plus a manifest of per-graph hashes and a set hash.* Readable without a decoder, but it needs a two-level hash scheme because zip bytes vary with entry order and timestamps.
- *(c) A signed manifest of hashes over loose JSON files.* Smallest change, but no single artifact to promote, and partial copies are easy to make.
- *(d) Leave it to adopters.* No engine change, but each adopter re-solves canonical encoding and atomic loading.

*Open questions:*
- Do `msgpack-core` (`packInt` / `packLong`) and `rmpv` both choose the smallest integer encoding? *(verify; the parity vectors will show it)*
- Must the canonical-encoding check (step 3.3) always run, or may it be skipped when the signature is valid? Mandatory is proposed: it is cheap and guarantees one byte form.
- Which engines carry it? The Java and Rust engines are the reference pair; should `mercury-go`, `mercury-python` and `mercury-nodejs` (which consume graphs rather than execute them) be required to pass the vectors?

**Resolution.**

---

## RFC-0001 — Decimal mode: exact decimal arithmetic for `graph.math` and the arithmetic plugins
**Status:** Open · **Raised:** 2026-09-30 · **Serves:** vision-mercury-composable · **Thread:** none yet
<!-- id: rfc-0001 | status: open | thread: none -->

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

1. **Enabling it.** A graph-level switch on the root node, `numeric: decimal`, plus an application default `graph.math.numeric = double | decimal`. The graph setting wins. `CompileGraph` records the mode, and it is part of the exported JSON, so any content hash covers it.
2. **Value model.** Add `DecimalValue(BigDecimal)` in Java and an arbitrary-precision decimal in Rust. In decimal mode, numeric literals parse **from text**, straight to decimal, never through a double; integers and numeric strings convert exactly. A `Double` input is lossy by definition: **strict** (recommended default for decimal mode) rejects it with an error naming the node and key; **lenient** converts through its shortest representation (`BigDecimal.valueOf`).
3. **Operators.** `+ - *` are exact, with a **specified result scale** (`+`/`-`: the larger operand scale; `*`: the sum of scales) so both engines agree. `/` never truncates (`7 / 2` gives `3.5`); a non-terminating quotient uses a fixed division context, proposed as 34 significant digits (decimal128) with HALF_EVEN, until the author applies an explicit scale. `%` is defined on decimals. Comparisons use numeric value (`2.0 == 2.00` is true).
4. **Rounding is always explicit.** `round(x, scale, mode)` takes `HALF_UP | HALF_EVEN | HALF_DOWN | UP | DOWN | CEILING | FLOOR` and returns a decimal at exactly that scale. The two-argument form uses a graph-level `rounding` default (proposed HALF_EVEN). `floor`, `ceil`, `abs`, `min`, `max`, and `pow` with a whole-number exponent are exact.
5. **Operations that cannot be exact are refused, at run time.** In decimal mode `sqrt`, `log`, `log10`, `exp`, trigonometry, `pow` with a fractional exponent, and `random()` raise an error naming the node. They are refused at run time rather than gated at compile time: a compile-time check would be a partial gate that cannot see values arriving through mapped inputs. An author who needs them keeps that node in double mode or uses a `graph.task` function.
6. **Strings in, strings out.** Every high-precision number is exported and imported **as a string**, wherever a decimal-mode number is stored or moves: graph JSON, the run's state machine, `output.*`, the exported graph, and a MsgPack graph-set package (RFC-0002). A JSON or MsgPack **number** is never used for a decimal, because standard parsers and MsgPack's float types would round-trip it through a double. Import parses the string straight into a decimal; export writes the canonical form below, so a value survives any number of export/import cycles unchanged. A decimal-mode graph holding a numeric (non-string) value where a decimal is expected is rejected by `CompileGraph` (strict) or converted with a warning (lenient).
   **Canonical string form:** plain notation, never scientific; keeps its scale (`round(10.5, 2, HALF_UP)` gives `"10.50"`); negative zero normalizes to zero at the same scale.
7. **Mixed modes.** A decimal string read by a **double-mode** node is a string, not a number, and follows the existing typed-arithmetic rules: the node fails by name rather than coercing silently. Crossing modes is explicit, through `f:decimal.*` or a conversion at a mapper node. A decimal-mode node may read a double-mode graph's numbers only under lenient mode.
8. **Plugins.** Add an explicit decimal family for flows and mapper nodes: `f:decimal.add`, `.sub`, `.mul`, `.div`, `.mod`, `.round(x, scale, mode)`, `.compare`. The existing `f:add` family keeps its behaviour, so nothing changes silently. Explicit names beat a hidden mode for plugins, which run outside any one graph's context.
9. **Parity, enforced by shared test vectors.** A committed JSON file of `{expression or plugin call, inputs, mode, expected canonical string or expected error}` runs in **every** executing engine's test suite, alongside the drift-tested claims registry. It must cover: the classic traps (`0.1 + 0.2`, `1.005` at scale 2, `±2.5` and `±0.5` in every rounding mode, `1/3` and `2/3` in the division context); **scale propagation through `+ - * /`**; large magnitudes; negative zero; strict-mode rejection of doubles; the refused-operation errors; and the mixed-mode failures.
10. **A side fix independent of decimal mode:** document, or align, the dialect `round(-2.5) = -2` versus plugin `f:round(-2.5) = -3` discrepancy.

A decision would commit every executing engine to one written decimal specification (result scales, division context, rounding modes, canonical string form, strings-only import and export), a shared conformance-vector file maintained in lock-step, and one new `Value` variant plus a plugin family per engine.

**Options.**
- *(a) Opt-in decimal mode (above).* Backward compatible; graphs stay readable and exact. Needs one spec shared across engines. **Recommended.**
- *(b) Replace double with decimal everywhere.* Simplest to reason about, but breaks graphs that rely on double functions and current rendering.
- *(c) Leave the core unchanged and ship a decimal function library for `graph.task`* (the existing ruling). No engine change, but formulas become opaque code again, and every adopter reinvents rounding and serialization.
- *(d) Fixed-point integers (minor units, e.g. cents, as `long`).* Exact and fast for sums, but awkward for rates, percentages, per-period amounts and currencies of different scale, and division still needs a rounding policy.

*Open questions:*
- **Division context.** Is 34 digits with HALF_EVEN right, or should it be configurable per graph? Configurable adds parity surface.
- **Rust type.** An arbitrary-precision crate (e.g. `bigdecimal`) matches Java `BigDecimal` semantics best; a fixed 96-bit type (e.g. `rust_decimal`, about 28 digits) cannot reproduce a 34-digit context. *(verify both crates' rounding-mode and result-scale behaviour)*
- **Trailing zeros.** Keep the computed scale (proposed) or strip? Either works if engines agree; keeping the scale matches financial reports.
- **Strict by default?** Reject double inputs (proposed) or only warn?
- **Scope.** Should `graph.data.mapper` arithmetic and `f:lookup` numeric keys also follow the graph's mode?
- **Reach.** Which engines carry it, and in which release? `mercury-go`, `mercury-python` and `mercury-nodejs` need at least the canonical string form.

**Resolution.**

