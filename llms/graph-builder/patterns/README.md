# Graph Patterns — Intent → Runnable Exemplar

Pick by **intent**, not by resemblance to a past use case. Each row points at the smallest *runnable* graph that exhibits the mechanism, plus the syntax reference for its rules. The deployed tutorials live at `system/minigraph-playground-engine/target/classes/graph/{id}.json`.

> **De-domain mentally.** The tutorials use a person/account/profile domain; the *mechanism* is domain-neutral. Read `person_id` as "the item key," `mdm-profile` as "a source," etc. Don't copy the domain — copy the shape.

| Intent | Idiomatic mechanism | Runnable exemplar | Rules |
|---|---|---|---|
| Group dictionaries/providers as a catalog | `graph.island` + `contains`/`data`/`provider` edges | **tutorial-3**, tutorial-6 | [minigraph-syntax.md → Data Dictionary Pattern](../minigraph-syntax.md#data-dictionary-pattern) |
| Fetch from one provider cluster | `graph.api.fetcher` + Dictionary + Provider | **tutorial-3** | same |
| Iterate over a list (per item) | `for_each` (+ optional `concurrency`); output aggregates as nested arrays → `f:listOfMap` | **tutorial-6** | minigraph-syntax.md → `for_each + concurrency` |
| Branch on data (decision routing) | `graph.math` / `graph.js` `IF/THEN/ELSE` with named targets | **tutorial-12** (also tutorial-4) | minigraph-syntax.md → Statement Syntax |
| Compute over data (JS objects/arrays) | `graph.js` `COMPUTE` → `{node}.result.var` → route via a mapper | **tutorial-4**, tutorial-9 | minigraph-syntax.md → COMPUTE |
| Retry a failing call (bounded) | `exception=` handler + `RESET` loop + an attempts counter | **tutorial-12** | minigraph-syntax.md → `exception`, `RESET` |
| Fan-out then join | parallel fetchers from a no-skill node + `graph.join` | **tutorial-5** | minigraph-syntax.md → `graph.join`; examples/customer-360-design-spec.md |
| Bounded loop / pagination | `RESET` the loop body **and its head** + an `IF` exit | tutorial-12 (retry form) | minigraph-syntax.md → `RESET` (loop-head self-RESET note) |
| **Process a list with per-item failure isolation** (one bad item recorded, batch continues) | sequential `RESET` loop, one item per pass, each call's `exception=` records a `status:"failed"` **data record** | **[per-item-isolation.md](./per-item-isolation.md)** ← no tutorial covers this | the card |

The last row is the one pattern the shipped tutorials don't demonstrate — `for_each` iterates but aborts on a failing iteration, so isolation needs the loop-and-exception shape captured in [per-item-isolation.md](./per-item-isolation.md). Everything above it has a verified tutorial exemplar; this directory exists to fill the gap, not to re-author what the tutorials already prove.
