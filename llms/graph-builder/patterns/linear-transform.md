# Pattern — Linear Transform (verified, runnable)

> **Verified end-to-end on the live engine** (session `ws-177533-2`, engine source `f9d77584`, 2026-06-16). Backed by ledger claim [C-60](../evidence/dogfood-customer-360.md). The smallest graph that normalizes/computes one value and shapes the output — no fan-out, no branching.

**Intent:** input `input.body.name` (string) → `output.body.greeting = ("hello, " + name)` uppercased.

```text
root → greet (graph.js) → end
```

## The exact build (each command auto-verified via companion.mjs)

```
create node root
with type Root
with properties
purpose=entry
```
```
create node greet
with type Evaluator
with properties
skill=graph.js
statement[]=COMPUTE: greeting -> ('hello, ' + '{input.body.name}').toUpperCase()
statement[]=MAPPING: greet.result.greeting -> output.body.greeting
```
```
create node end
with type End
```
```
connect root to greet with execute
connect greet to end with complete
```
```
instantiate graph
text(ada) -> input.body.name
```
```
run
```

## Verified result

`output.body` = `{ "greeting": "HELLO, ADA" }`  (and `greet.result` = `{ "greeting": "HELLO, ADA" }`).

## The load-bearing point — quote string `{path}` refs in a COMPUTE (C-60)

A `graph.js` COMPUTE substitutes a `{path}` to its **raw value**. For a **number** that's fine (`{model.x} + 1` → `5 + 1`). For a **string** the raw value is *unquoted*, so `'hello, ' + {input.body.name}` becomes `'hello, ' + ada` → invalid JS → the node halts silently. **Wrap a string ref in quotes yourself:** `'{input.body.name}'` → `'hello, ' + 'ada'`. (Substitution still fires inside the quotes — the [`{path}` rule](../minigraph-syntax.md#appendix-provenance-and-sources) only skips a brace group containing a colon/newline/tab/CR.)

A COMPUTE result lands at `{node}.result.{var}`; route it with a `MAPPING:` statement in the same node (here both run in `greet`).
