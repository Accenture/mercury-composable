# Graph Patterns — Intent → Runnable Exemplar

Pick by **intent**, not by resemblance to a past use case. Each row points at the smallest *runnable* graph that exhibits the mechanism, plus the syntax reference for its rules.

> **Prefer the in-repo exemplars (✓live).** The six exemplars below marked **✓live** are **complete, copy-paste-runnable** command sequences **verified end-to-end on the live engine** (session `ws-177533-2`, engine source `f9d77584`, 2026-06-16) and anchored to ledger claims. *Complete* means every node — including `root` and `end` — and every edge is shown; copy top to bottom (create all nodes, then connect, then instantiate + run). Read these first — they are in-scope and reproduce a green run. **You need an open Playground session id first** — see [Getting a session id](../minigraph-syntax.md#getting-a-session-id--you-cannot-mint-one-over-rest). The shipped *tutorials* (`system/minigraph-playground-engine/target/classes/graph/{id}.json`) are secondary references and **out of the docs' scope** for a fresh build.

> **De-domain mentally.** Read `person_id` as "the item key," `profile-api` as "a source," etc. Copy the *shape*, not the domain.

| Intent | Idiomatic mechanism | Runnable exemplar | Rules |
|---|---|---|---|
| Linear transform (normalize → compute → shape) | mapper / `graph.js` `COMPUTE` | **[linear-transform.md](./linear-transform.md)** ✓live | [COMPUTE](../minigraph-syntax.md#compute) |
| Fetch from one provider cluster | `graph.api.fetcher` + Dictionary + Provider | **[fetch-and-shape.md](./fetch-and-shape.md)** ✓live · tutorial-3 | [Data Dictionary Pattern](../minigraph-syntax.md#data-dictionary-pattern) |
| Branch on data (decision routing) | `graph.math`/`graph.js` `IF/THEN/ELSE`, named targets | **[decision-branch.md](./decision-branch.md)** ✓live · tutorial-12 | [Statement Syntax](../minigraph-syntax.md#statement-syntax-graphmath--graphjs) |
| Fan-out then join | no-skill fan-out + `graph.join` | **[fan-out-join.md](./fan-out-join.md)** ✓live · tutorial-5 | [`graph.join`](../minigraph-syntax.md) |
| Iterate / transform a list | `.map()` COMPUTE for *compute*; `for_each` for *per-item fetch* (see C-62) | **[iterative.md](./iterative.md)** ✓live · tutorial-6 | [`for_each`](../minigraph-syntax.md#for_each--concurrency-verified) |
| Compose / call a deployed graph or flow | `graph.extension` → **deployed** target | **[extension.md](./extension.md)** ✓live | [`extension`](../minigraph-syntax.md) |
| Group dictionaries/providers as a catalog | `graph.island` + `contains`/`data`/`provider` edges | tutorial-3, tutorial-6 | Data Dictionary Pattern |
| Retry a failing call (bounded) | `exception=` handler + `RESET` loop + attempts counter | tutorial-12 | `exception`, `RESET` |
| Bounded loop / pagination | `RESET` the loop body **and its head** + an `IF` exit | tutorial-12 | `RESET` (loop-head self-RESET) |
| **Per-item failure isolation** (one bad item recorded, batch continues) | sequential `RESET` loop, one item/pass, each call's `exception=` records a `status:"failed"` record | **[per-item-isolation.md](./per-item-isolation.md)** ← no tutorial covers this | the card |

The six ✓live exemplars cover the core graph shapes (the [`graph-builder-eval.md`](../graph-builder-eval.md) intents EV-1…EV-6), each verified green and ledger-backed — they exist because a fresh-agent eval showed the only prior exemplars were heavy (customer-360) or out-of-scope (tutorials under `target/classes`). The bottom rows still point at tutorials; [per-item-isolation.md](./per-item-isolation.md) fills the one gap the tutorials don't cover (`for_each` aborts on a failing iteration).
