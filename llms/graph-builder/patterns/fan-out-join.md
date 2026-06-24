# Pattern — Fan-Out and Join (verified, runnable)

> **Verified end-to-end on the live engine** (session `ws-177533-2`, engine source `f9d77584`, 2026-06-16). Backed by ledger claim [C-59](../evidence/dogfood-customer-360.md). The smallest graph that runs two branches in parallel and waits for both before returning.

**Intent:** copy `input.body.x` → `output.body.a` and `input.body.y` → `output.body.b` in **parallel**, converging at a join so both finish before `end`.

```text
root → fan-out ─→ copy-a ─→ join-branches → end
            └──→ copy-b ─→ join-branches
```

## The exact build (each command auto-verified via companion.mjs)

```
create node root
with type Root
with properties
purpose=entry
```
```
create node fan-out
with type Stage
with properties
purpose=parallel fan-out to copy-a and copy-b
```
*(No-skill node: no `skill` line ⇒ traversal walks **all** its forward links in parallel.)*
```
create node copy-a
with type Mapper
with properties
skill=graph.data.mapper
mapping[]=input.body.x -> output.body.a
```
```
create node copy-b
with type Mapper
with properties
skill=graph.data.mapper
mapping[]=input.body.y -> output.body.b
```
```
create node join-branches
with type Join
with properties
skill=graph.join
```
*(`graph.join` needs **only** `skill=graph.join` — no other properties — to build and run.)*
```
create node end
with type End
```
```
connect root to fan-out with execute
connect fan-out to copy-a with execute
connect fan-out to copy-b with execute
connect copy-a to join-branches with wait
connect copy-b to join-branches with wait
connect join-branches to end with complete
```
```
instantiate graph
text(hello-x) -> input.body.x
text(hello-y) -> input.body.y
```
```
run
```

## Verified result

`output.body` = `{ "a": "hello-x", "b": "hello-y" }` — **both** branches' writes present.

## The load-bearing points (engine-verified, C-59)

- A **no-skill** `fan-out` node traverses *all* its forward edges → `copy-a` and `copy-b` run in parallel.
- `graph.join` waits for **all** backward-linked predecessors (`copy-a` **and** `copy-b`) before returning, then continues to `end`. It needs only `skill=graph.join`.
- **Concurrent writes to *distinct* `output.body` keys both survive** the join — you do **not** have to accumulate in `model.*` and shape `output.body` in a single post-join mapper. (Distinct keys; this says nothing about two branches writing the *same* key.)

> **Join hazard (design, C-25):** the join returns `.sink` until *every* backward-linked predecessor completes. Wire only branches that will actually run in the reaching scenario as join predecessors, or the join deadlocks and `output.body` is never produced.
