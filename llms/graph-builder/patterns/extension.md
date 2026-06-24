# Pattern — Composed Graph / Extension (verified, runnable)

> **Verified end-to-end on the live engine** (session `ws-177533-2`, engine source `f9d77584`, 2026-06-16). Backed by ledger claim [C-64](../evidence/dogfood-customer-360.md) (re-confirms C-44). For calling another **already-deployed** graph (or an Event Script `flow://`) as a sub-graph.

**Intent:** call the deployed `tutorial-1` sub-graph; return its result on `output.body.sub_result`.

```text
root → call-tut (graph.extension → tutorial-1) → end
```

## The exact build (verified)

```
create node call-tut
with type Extension
with properties
skill=graph.extension
extension=tutorial-1
input[]=input.body.name -> name
output[]=result -> output.body.sub_result
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
connect root to call-tut with execute
connect call-tut to end with complete
```
```
instantiate graph
text(world) -> input.body.name
```
```
run
```

## Verified result

`output.body` = `{ "sub_result": "hello world" }`; `call-tut` = `{ result:"hello world", target:"tutorial-1", status:200 }`.

## The load-bearing points (the build/deploy constraint that bites)

- **The target resolves only from the read-only DEPLOYED-graph location** (`location.graph.deployed`, default `classpath:/graph/{id}.json`) — a **different store** from `export`/`import` (`location.graph.temp`, `/tmp/graph`). So `export graph as X` does **not** make `X` callable as `extension=X` (→ `"not found"`, `400`). Only **already-deployed** graphs work: the shipped tutorials (`tutorial-1`…), or a graph you've deployed via a rebuild/restart. **The Companion API cannot deploy a target at runtime** (C-39/C-44). `app.env=dev` allows calling `tutorial-*`.
- **Single-call mode requires ≥1 `input[]` mapping** (even if the target ignores it).
- **`output[]=result -> …`** routes the sub-graph's whole `output.body` (surfaced as `{node}.result`); `{node}.status` / `{node}.error` carry the call outcome. On failure, error + status are copied to `output.body` (add `exception={alias}` to handle it).
- **Design implication:** if your sub-graph is *not* deployed (a custom graph you just built), you cannot call it via `extension=`. Compose with island-backed fetchers instead, or deploy the target first. Surface this as a `blocks: build` constraint in `/design`.
