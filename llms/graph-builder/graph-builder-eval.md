# Graph Builder Eval — Fresh-Agent Build Success

Status: standing fitness signal for the context system (Layer 1b of the layered plan). The no-code analogue of [`memory/smoke-test.md`](../../memory/smoke-test.md), applied to *graph authoring* instead of *project orientation*.

> **What it measures:** can a **fresh-context agent, given the `llms/graph-builder/` docs alone**, author a graph for a stated intent that builds and runs **green** on the engine — first try? A ⚠️/❌ is a *documentation* gap to fix (add a verified exemplar, resolve a contradiction), **never** a question to soften. App-level eval is unsolved industry-wide; this is the markdown version. (Eval-driven optimization: Khattab et al. 2023, *DSPy*, arXiv:2310.03714.)

## How to run

1. Spawn a **fresh-context agent**. Allow it to read ONLY `llms/graph-builder/{minigraph-syntax,graph-design,README}.md` + `patterns/` + `examples/`. **Forbid** the engine source and the evidence/worklog files — the point is to test the *docs*, not the agent's ingenuity.
2. Give it one **build intent** below. It produces the ordered Companion command sequence and a **friction log** (every doc gap / guess / contradiction — be specific, quote the doc).
3. **Score** and append a Result-log row. Two modes:
   - **Live** (engine up, default `localhost:8085`): run the commands via `companion.mjs`; ✅ = builds + runs green, ⚠️ = builds but wrong/uncertain output, ❌ = fails to build or run.
   - **Doc-authoring** (engine offline): score the *authored* commands against the syntax/design docs; ✅ = correct + footgun-free + confident, ⚠️ = plausible but rests on an unresolved doc gap, ❌ = violates a documented rule. (Weaker signal — a live re-run supersedes it.)
4. For every ⚠️/❌, file the friction as a fix (a missing exemplar, a contradiction thread). Then the next run should improve. **Do not edit the intents to make them pass.**

## Build intents (one per documented graph shape)

The shapes are the [Design Patterns](./graph-design.md#design-patterns) in `graph-design.md`.

| id | shape | intent | exercises |
|---|---|---|---|
| EV-1 | linear transform | input `input.body.name` (string) → return `output.body.greeting = "hello, " + name`, uppercased | mapper / `graph.js`, scalar output |
| EV-2 | fetch and shape | fetch a profile by `input.body.id` from a provider; return `output.body.profile_name` *(needs `evidence/stub-server.mjs`)* | provider, dictionary, fetcher, `response.*` extraction |
| EV-3 | fan-out and join | from root, two parallel mappers (`input.body.x`→`output.body.a`, `input.body.y`→`output.body.b`), converge at a join before end | no-skill fan-out, `graph.join`, parallel edges |
| EV-4 | decision branch | input `input.body.score` (number); if `>= 70` → `output.body.result="pass"` else `"fail"` | `graph.math` IF/THEN/ELSE, named jump targets, bare literals |
| EV-5 | iterative | input `input.body.ids` (list); fetch/transform per item; return aggregated `output.body.results` | `for_each`, `concurrency`, output aggregation |
| EV-6 | composed / extension | call deployed `tutorial-1`; return its result on `output.body.sub_result` *(deployed-target only)* | `graph.extension`, deployed-location resolution |

## Result log

| Date | Through | Mode | id | Score | Headline gap → action |
|---|---|---|---|---|---|
| 2026-06-16 | (baseline) | doc-authoring | EV-4 | ⚠️ | Footgun "decision = exactly one natural edge" appears to **contradict** `customer-360` `validate-person` (two natural edges + IF/THEN/ELSE); no verified *simple* decision exemplar. → contradiction thread + Layer 3 exemplar |
| 2026-06-16 | Layer 3 | **live** (`ws-177533-2`) | EV-4 | ✅ | Built + ran green; THEN→pass, ELSE→fail with two natural edges. Contradiction **resolved** (named-target IF ≠ bare `next`). Exemplar: [patterns/decision-branch.md](./patterns/decision-branch.md). |
| 2026-06-16 | (baseline) | doc-authoring | EV-3 | ⚠️ | No `create` example for a no-skill fan-out node or `graph.join`; unclear whether pre-join `output.body` writes survive (verified idiom shapes output *after* the join). → Layer 3 exemplar |
| 2026-06-16 | Layer 3 | **live** (`ws-177533-2`) | EV-3 | ✅ | Built + ran green; `output.body={a,b}` both present. `graph.join` needs only `skill`; concurrent pre-join writes to distinct keys survive (C-59). Exemplar: [patterns/fan-out-join.md](./patterns/fan-out-join.md). |

| 2026-06-16 | Layer 3 | **live** (`ws-177533-2`) | EV-1 | ✅ | Built + ran green; `output.body.greeting="HELLO, ADA"`. Surfaced C-60 (quote string `{path}` in COMPUTE). Exemplar: [patterns/linear-transform.md](./patterns/linear-transform.md). |
| 2026-06-16 | Layer 3 | **live** (`ws-177533-2`) | EV-5 | ✅ | `output.body.results=[10,20,30]` via `.map()` COMPUTE. Surfaced C-61 (`.map` idiom + braceless arrow) and C-62 (`for_each`+`output[]` doesn't aggregate COMPUTE). Exemplar: [patterns/iterative.md](./patterns/iterative.md). |
| 2026-06-16 | Layer 3 | **live** (`ws-177533-2` + stub `:8099`) | EV-2 | ✅ | Real `GET /profile/P-10001` → `output.body.profile_name="Ada Example"`. Minimal wiring; fetcher writes direct to output.body (C-63). Exemplar: [patterns/fetch-and-shape.md](./patterns/fetch-and-shape.md). |
| 2026-06-16 | Layer 3 | **live** (`ws-177533-2`) | EV-6 | ✅ | `extension=tutorial-1` → `output.body.sub_result="hello world"`, status 200 (C-64). Exemplar: [patterns/extension.md](./patterns/extension.md). **All 6 shapes verified-green.** |

### Regression gate (2026-06-16) — exemplars vs baseline

Re-ran fresh cold agents (same strict doc-only rules + doc scope as baseline; doc-authoring mode) **after** the example-first exemplar library landed. The format restructure was **not** yet done — so this isolates the exemplars' impact.

| shape | baseline | regression | what changed |
|---|---|---|---|
| EV-4 decision | ⚠️ ~70% (couldn't resolve the natural-edge contradiction) | **✅ ~90%** | copied `patterns/decision-branch.md` verbatim; contradiction gone |
| EV-3 fan-out-join | ⚠️ ~65% (join props? pre-join output writes?) | **✅ ~96%** | `patterns/fan-out-join.md` resolved every structural Q |
| EV-5 iterative (trap test) | — | **✅ ~90%** | `patterns/iterative.md` steered it to `.map`, avoided the `for_each` aggregation trap |

**Result: 0 clean → 3 clean.** All three found and copied the matching exemplar; both baseline blockers eliminated. Example-first validated empirically.

**New gaps the gate surfaced (now the top blockers — *not* formatting):**
1. **Session-id onboarding gap** — the docs never describe how a cold agent *starts/obtains* a session over REST (only that the id prints on the WebSocket console). EV-4 called this its single most likely failure point. (`ot-gb-session-onboarding-gap`)
2. **Exemplar completeness** — exemplars collapse `root`/`end`/`connect` to "*(plus root/end; edges…)*", so a strict copy-paste isn't runnable; + `Evaluator` vs undocumented `Transform` type inconsistency. (`ot-gb-exemplar-completeness`)

### Baseline finding (2026-06-16)

Two fresh-context agents, current docs, engine offline. Both **⚠️ partial** (self-rated 70% / 65% green), **zero clean ✅**. They failed in different shapes but for the **same reason**: there is **no verified, runnable, *simple* exemplar per shape**. The only in-scope worked example (`customer-360`) is heavy and partly unverified; the runnable tutorials sit under `system/.../target/classes/graph/` — out of doc-scope. This is the empirical case for **example-first re-authoring** (Layer 3 / `ot-gb-example-first-format`) and confirms the **breadth gap** (`ot-gb-coverage-breadth`). Re-run all intents in **live** mode after Layer 3 as the regression gate.
