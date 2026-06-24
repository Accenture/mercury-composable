# Pattern — Iterative / List Transform (verified, runnable)

> **Verified end-to-end on the live engine** (session `ws-177533-2`, engine source `f9d77584`, 2026-06-16). Backed by ledger claims [C-61/C-62](../evidence/dogfood-customer-360.md). For transforming **every item of a list** into an output list.

**Intent:** input `input.body.ids` (list of numbers) → `output.body.results` = each ×10.

```text
root → mapper-input → iterate (graph.js) → shape → end
```

## The exact build (verified)

```
create node mapper-input
with type Mapper
with properties
skill=graph.data.mapper
mapping[]=input.body.ids -> model.ids
```
```
create node iterate
with type Evaluator
with properties
skill=graph.js
statement[]=COMPUTE: results -> {model.ids}.map(x => x * 10)
statement[]=MAPPING: iterate.result.results -> model.results
```
```
create node shape
with type Mapper
with properties
skill=graph.data.mapper
mapping[]=model.results -> output.body.results
```
```
create node root
with type Root
with properties
purpose=entry
```
```
create node end
with type End
```
```
connect root to mapper-input with execute
connect mapper-input to iterate with execute
connect iterate to shape with execute
connect shape to end with complete
```
```
instantiate graph
int(1) -> input.body.ids[]
int(2) -> input.body.ids[]
int(3) -> input.body.ids[]
```
*(Array-append seeding — repeat the `[]` line per item, C-29.)*
```
run
```

## Verified result

`output.body` = `{ "results": [10, 20, 30] }`.

## The load-bearing points

- **For a pure list transform, use one `graph.js` COMPUTE with `.map()` (C-61)** — `{model.ids}` substitutes to the JSON array `[1,2,3]`, so `[1,2,3].map(x => x * 10)` → `[10,20,30]` lands in `iterate.result.results`. Clean and aggregated in one shot.
- **Write the callback as an arrow with NO block braces** — `x => x * 10`, *not* `x => { return x * 10; }`. A `{ … }` body with no colon hits the [`{path}` substitution guard](../minigraph-syntax.md#appendix-provenance-and-sources) (C-54): the engine reads it as a state path, fails to resolve it, and the node halts silently.
- **`for_each` + `output[]` does NOT aggregate COMPUTE results (C-62).** A `for_each` on `graph.js` *does* iterate and return `next`, but `output[]=iterate.result.tenx -> model.results[]` collected **nothing** — only the last iteration's value survived in `iterate.result`. The `output[]` per-iteration aggregation idiom (C-30) is for **`graph.api.fetcher` / `graph.extension`** (each iteration yields a fresh `{node}.result`), **not** for COMPUTE. Reach for `for_each` when each item drives a **fetch/extension call**; reach for `.map()` when each item drives a **computation**.
