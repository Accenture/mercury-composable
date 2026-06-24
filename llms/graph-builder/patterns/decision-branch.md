# Pattern — Decision Branch (verified, runnable)

> **Verified end-to-end on the live engine** (session `ws-177533-2`, engine source `f9d77584`, 2026-06-16). Backed by ledger claims [C-55](../evidence/dogfood-customer-360.md) (routing) + the footgun re-verifications C-56/C-57. Copy this skeleton and re-domain it — it is the smallest graph that routes one of two outcomes on a runtime value.

**Intent:** input `input.body.score` (number); if `score >= 70` → `output.body.result = "pass"`, else `"fail"`.

```text
root → mapper-input → decide ─(THEN: pass)→ dec-pass → end
                         └────(ELSE: fail)→ dec-fail → end
```

## The exact build (each command auto-verified via companion.mjs)

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
*(`End` takes no `with properties` block — verified, C-58.)*
```
create node mapper-input
with type Mapper
with properties
skill=graph.data.mapper
mapping[]=input.body.score -> model.score
```
```
create node decide
with type Evaluator
with properties
skill=graph.math
statement[]='''
IF: {model.score} >= 70
THEN: dec-pass
ELSE: dec-fail
'''
```
```
create node dec-pass
with type Decision
with properties
skill=graph.data.mapper
mapping[]=text(pass) -> output.body.result
```
```
create node dec-fail
with type Decision
with properties
skill=graph.data.mapper
mapping[]=text(fail) -> output.body.result
```
```
connect root to mapper-input with execute
connect mapper-input to decide with evaluate
connect decide to dec-pass with pass
connect decide to dec-fail with fail
connect dec-pass to end with complete
connect dec-fail to end with complete
```
```
instantiate graph
int(85) -> input.body.score
```
```
run
```

## Verified results

| seed | branch | `output.body` |
|---|---|---|
| `int(85)` | THEN → dec-pass | `{ "result": "pass" }` |
| `int(50)` | ELSE → dec-fail | `{ "result": "fail" }` |

## Why this skeleton is correct (the load-bearing point)

`decide` has **two natural outgoing edges** (to both `dec-pass` and `dec-fail`) *and* an IF/THEN/ELSE that **names** both targets — and only the matched branch ran each time. **A named-target IF does not fan out.** The "a decision must have exactly one natural outgoing edge" footgun applies to a node returning **bare `next`** (or a statement-less node), which traverses *all* natural edges — **not** to an IF/THEN/ELSE that returns a named target. So the customer-360 `validate-person` shape (two natural edges + named branches) is correct; you do **not** need to delete natural edges or use jump-only targets for a decision. (Resolves the contradiction surfaced by the eval baseline; verified live, C-55.)

**Reminder:** `result` is a scalar, so `inspect output.body.result` returns `404` by design — read the container `output.body` (C-57).
