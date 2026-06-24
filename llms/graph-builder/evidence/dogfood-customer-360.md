# MiniGraph Claim Ledger

> **Canonical single-source-of-truth for verified MiniGraph engine behavior.** Each numbered claim below is a ledger entry with a stable id (`C-NN`), a lifecycle `status`, and a `verified-against` engine commit. The `(verified)` / `(source-verified)` tags in [`minigraph-syntax.md`](../minigraph-syntax.md) are a **projection** of this ledger — see [Claim Record & Lifecycle](#claim-record--lifecycle). Entries originate from the **customer-360 P0 dogfood** (hence the filename); breadth beyond that one scenario is unproven — see *Coverage and Limitations* below. As more shapes are dogfooded, append entries/rounds here.

Status: P0 complete. All targeted primitives probed; `graph.extension` successful-invocation is the one partial (needs a deployed sub-graph the scratch playground can't provide).
Engine: `localhost:8085`, session `ws-844990-2`. Stub: `node evidence/stub-server.mjs` on `localhost:8099`. Date: 2026-06-08.
Method: built one mutation at a time through `companion.mjs` (each auto-verified), then instantiate + run with `--expect`, then inspect state. Traversal path (`seen`) is WebSocket-only, so all conclusions are inferred from inspected state.

This is the P0 output artifact referenced by improvement-plan.md Gate 1.

## Claim Record & Lifecycle

*Reference: the schema and status model for every claim in this ledger. The recompute that **projects** these into `minigraph-syntax.md` tags is Layer 2; this section (Layer 1a) defines the substrate.*

**Record.** Each claim is one row in the round tables below, carrying:

| field | meaning |
|---|---|
| `id` | the claim's number as `C-NN` (claim 12 = `C-12`). **Stable and immutable** — never renumber; it is the handle `minigraph-syntax.md` links to. |
| statement | the asserted engine behavior (the "Asserted claim" cell). |
| evidence | the "Observed behavior" cell — what the engine actually did. |
| `status` | lifecycle state (below), **derived** from the verdict by the mapping below — not hand-scored. |
| `verified-against` | the engine commit the observation holds against. Global default **`f9d77584`** (the dogfood engine state) for all execution-verified claims unless a row says otherwise. |
| origin | the round/section that recorded it — provenance, one hop to the method. |

**Status lifecycle** (deterministic states; no scoring):

```
asserted         documented from the help surface; not yet execution-tested.
verified         execution-tested green against verified-against.
source-verified  read from engine source, not executed (e.g. RESET, BEGIN/END).
stale            engine source changed past verified-against → must be re-checked.
superseded       proven false or replaced. TERMINAL — never silently revived; only a
                 re-verification at a new commit restores a claim.
```

**Transitions.**
- `asserted → verified` when execution-tested; `→ source-verified` when confirmed by reading source.
- `verified / source-verified → stale` on the Freshness trigger (a commit after `verified-against` touches the cited engine source — see [Verification Freshness](#verification-freshness)).
- `stale → verified` after a `/graph-verify` re-run **and** a `verified-against` bump.
- `* → superseded` the moment a claim is proven false — terminal (the `DECAY.md` §9 rule applied to claims). A superseded claim stays visibly marked; its replacement is a new claim.

**Verdict → status mapping.** The existing `keep / change / add / open` verdicts *are* the status, read through this map — so `status` need not be hand-typed per row:

| verdict | status |
|---|---|
| keep | **verified** (doc claim matched the engine) |
| add | **verified** (undocumented-but-real behavior, now a claim) |
| change | the prior assertion is **superseded**; the corrected statement is **verified** |
| open / untestable | **asserted** (not cleanly isolated; treat as unverified) |

This is the markdown realization of *generate, don't transcribe*: the ledger is canonical truth; the syntax-doc tags are a recomputed view (Layer 2). A tag with **no** ledger entry, or whose entry is `stale`/`superseded`, is a drift signal the recompute must catch.

## Projection & Recompute Ritual

*Ritual — the REVIEW-for-claims. Regenerates the `minigraph-syntax.md` `(verified)` / `(source-verified)` tags from this ledger and surfaces drift. The lifecycle states are defined above; this is how they propagate to the consumer doc.*

**When it runs:** on engine-source change (the freshness trigger), after any edit to a tag or a claim, or on demand ("recompute claims").

**Inputs:** this ledger (claims + status), the Projection Index below, and `git`.

**Steps (deterministic):**
1. **Ground (git — no live engine needed).** Run the drift check:
   `git log f9d77584..HEAD --oneline -- system/minigraph-playground-engine/src/main/java/com/accenture/minigraph/{skills,common,services,rest}`.
   Non-empty ⇒ mark every execution-`verified` claim **stale** until re-verified via `/graph-verify` (then bump `verified-against`). *Live re-verification of a stale claim needs the engine; drift detection does not.*
2. **Project.** For each Projection-Index row, confirm the doc tag matches the claim's status: `verified`/`source-verified` ⇒ doc bears the matching tag; `stale` ⇒ doc tag downgraded to "asserted (re-verify pending)"; `superseded` ⇒ doc rule removed/corrected.
3. **Flag orphans.** A `(verified)` tag with **no** Projection-Index row (no backing claim) is an orphan — record it and open a backfill item; never silently keep it.
4. **Flag contradictions** between claims, or between a claim and a doc rule — surface, never silently reconcile (the `DECAY.md` §10 rule).
5. **Stamp.** Record the recompute date + drift result in the log below.

### First recompute — 2026-06-16
- **Grounding:** drift check **empty** → no engine-source change since `f9d77584`; all execution-verified claims remain **fresh**. (Engine need not be up for this step.)
- **Orphans found — 3:** footgun #3 (`'''`-unwrapped → hollow node) and footgun #5 (`inspect` scalar-leaf → 404), plus the *decision-specific* early-completion case of footgun #1 (`next` fan-out), all trace to **probe session `ws-697490-4`** and have **no dedicated ledger claim**. The consumer doc presents them as verified; the ledger does not contain them → backfill tracked (`ot-gb-ledger-probe-backfill`). **Resolved 2026-06-16 (Round 9, live session `ws-177533-2`):** C-56 backs #3, C-57 backs #5, C-55 clarifies #1.
- **Contradictions — 1 carried:** `ot-gb-decision-edge-contradiction` (decision-node natural-edge footgun vs `customer-360` `validate-person`).

## Projection Index

Maps each high-stakes `minigraph-syntax.md` claim → backing ledger claim(s) → projected status. Seed = the Read-This-First footguns + the legend; extend as tags are touched (notably the L3 re-author).

| syntax-doc claim | ledger | projected status | note |
|---|---|---|---|
| Footgun #2 — IF halts on an unresolved value | C-09, C-10 | verified | solid backing |
| Footgun #4 — no `number()` constant | C-35 | verified | solid backing |
| Footgun #1 — `next` fans to ALL natural edges | C-23, C-32, **C-55** | verified | C-55 clarifies: a *named-target* IF does not fan out — only bare `next` does |
| Footgun #3 — un-`'''`-wrapped block → hollow node | **C-56** | verified | backed live 2026-06-16 (was orphan) |
| Footgun #5 — `inspect` scalar leaf → 404 | **C-57** | verified | backed live 2026-06-16 (was orphan) |
| Legend `(verified)`/`(source-verified)` semantics | (all rows) | n/a | the legend defines the projection — see Claim Record & Lifecycle |

## Coverage and Limitations — read before trusting a `(verified)` tag

This matrix is **deep, not broad.** It probes each load-bearing primitive once or twice, but almost entirely **within a single evolving scenario** (customer-360) across a handful of sessions (`ws-844990-2`, `ws-697490-4`, plus a few follow-ups). Concretely:

- A `(verified)` tag means *"observed true in this probe,"* **not** *"true in every composition."* A primitive that works here may behave differently inside an untested graph shape or interaction.
- **Breadth across the documented graph shapes is unproven.** The shapes in [`graph-design.md`](../graph-design.md#design-patterns) (linear, fetch-and-shape, fan-out-join, decision, iterative, extension) were **not** each independently dogfooded end-to-end; coverage clusters around the customer-360 fan-out-join build.
- Several claims rest on **one** session, and a few (`RESET`, `BEGIN/END`) are **source-verified only** (read from source, not executed).

So treat the matrix as strong evidence for *individual primitive behavior* and weaker evidence for *cross-shape generalization*. Widening breadth — a small runnable exemplar per documented shape — is a known gap. For keeping the claims that *are* here fresh as the engine changes, see **Verification Freshness** next.

## Verification Freshness

*When `(verified)` / `(source-verified)` claims must be re-checked.*

A tag is true only **as of the engine state it was checked against** — tags rot silently when the engine changes underneath them (classic doc/code drift). To keep them honest:

- **Verified against engine commit `f9d77584`** (2026-06-03 — the last change to the cited engine source). This dogfood ran 2026-06-08, *after* that commit, and the engine source has not changed since → the corpus is current.
- **Trigger (event-based, not calendar):** any commit *after the stamped one* that touches the cited engine source invalidates the dependent tags until re-checked — the files are `skills/` (`GraphJs`, `GraphMath`, `GraphApiFetcher`, `GraphJoin`, `GraphIsland`, `GraphExtension`, `GraphDataMapper`), `common/GraphLambdaFunction`, `services/GraphTraveler` + `GraphExecutor`, and `rest/InspectStateMachine`. Detect drift with:

  ```bash
  git log f9d77584..HEAD --oneline -- \
    system/minigraph-playground-engine/src/main/java/com/accenture/minigraph/{skills,common,services,rest}
  ```

  Non-empty output ⇒ re-verify the affected claims.
- **Procedure:** re-run [`/graph-verify`](../verify.md) against the affected claims (or the whole matrix), update the verdicts, then bump the **Verified against** commit above to the new HEAD.
- Until re-checked against a changed engine, a `(verified)` tag is effectively **asserted** — downgrade your trust accordingly.

> Optional enforcement upgrade: a CI step or pre-commit hook can run the drift `git log` above and fail/warn when engine source moved past the stamped commit without a freshness bump — turning this discipline into a gate. No-code by default; wire it only if you want teeth.

## What was built

A runnable core graph that needs no external HTTP, chosen to exercise the load-bearing syntax claims and yield a real happy path **and** a real negative path:

```
root → mapper-input → check-id ──(THEN: == false)──→ shape-output → end
                          └──────(ELSE)─────────────→ validation-error → end
```

Final working node definitions (the ones that ran green):

- `mapper-input` (graph.data.mapper): `input.body.person_id -> model.person_id`; `model.person_id:boolean(null=true) -> model.person_id_missing`; `boolean(false) -> model.degraded`
- `check-id` (graph.math): `IF: {model.person_id_missing} == false / THEN: shape-output / ELSE: validation-error`
- `shape-output` (graph.data.mapper): `model.person_id -> output.body.customer_id`; `text(ok) -> output.body.status`; `model.degraded -> output.body.degraded`
- `validation-error` (graph.data.mapper): `text(validation_error) -> output.body.error`; `text(...) -> output.body.message`

## Results

- **Happy path** (`text(P-10001) -> input.body.person_id`): `output.body = {customer_id: "P-10001", status: "ok", degraded: false}`. ✅
- **Negative path** (input.body present, no person_id): `model.person_id_missing = true` → `output.body = {error: "validation_error", message: "Missing or malformed person_id"}`. ✅

P0 "Done when" (instantiate + run once happy + at least one negative path) is **met for the runnable core**.

## Claim matrix

Verdict: keep = doc is correct; change = doc wording is wrong/misleading; add = undocumented behavior worth adding; open = not cleanly isolated yet.

| # | Asserted claim (minigraph-syntax.md) | Observed behavior | Verdict |
|---|---|---|---|
| 1 | `create node` / `with type` / `with properties` | 6 nodes created & verified | **keep**, but the graph model stores **`types` (array)** while input is `with type` (singular) — note it |
| 2 | `connect A to B with {relation}` | 5 connections; stored as `{source, relations:[{type}], target}` | **keep** |
| 3 | `input.body.* -> model.*` mapping | works; when source is null the target is **silently omitted** (model.person_id absent in negative case) | **keep** + **add** the null-source-skips note |
| 4 | `text(...)`, `boolean(...)` constants | both work; boolean preserved as a real JSON boolean | **keep** |
| 5 | `model.* -> output.body.*` mapping | all three output fields written | **keep** |
| 6 | `instantiate graph` + `text()` seed | input.body seeded, instance live | **keep** |
| 7 | end node → `output.body` is the response | `output.body` retrievable after run | **keep** |
| 8 | `graph.math` IF → THEN/ELSE jump to a node alias | works for **both** THEN and ELSE **when the condition resolves** | **keep** |
| 9 | `!= null` "direct null check (graph.math)" | **FAILS** — a bare `{x} != null` IF halts the node silently (no output, no error in state) | **change/remove** — delete the "direct null check" bullet; the working form is #11 |
| 10 | unresolved `{...}` reference in a graph.math IF | **silently halts** the node — no branch taken, no error surfaced in inspectable state | **add** — this is the footgun behind #9; document it |
| 11 | `:boolean(null=true)` null selector | **works in a `graph.data.mapper`** (true when source null, false when present) | **keep** but **clarify location** |
| 12 | same `:boolean(null=true)` as an inline `MAPPING:` statement in `graph.math` | did **not** create the target value (model key never appeared) | **change** — null-check belongs in a mapper, not a math `MAPPING:` statement (or needs further study) |
| 13 | node reachable only by a jump (no natural inbound edge) | `validation-error` ran via the ELSE jump with no inbound natural edge from check-id | **add** — jump targets need not have a natural inbound edge |
| 14 | `THEN: next` = "continue to next statement" (vs node-level `next`) | **open** — not cleanly isolated; my first failure used `THEN: next` *and* a failing condition (#9), so the halt is attributable to #10, not proven to be `next` | **open** |

## Fetcher / Dictionary / Provider + exception (round 2, against the stub)

Built a profile fetcher (success) and a risk fetcher (forced 500) against `evidence/stub-server.mjs`:

- **Happy fetch** → `output.body.profile_name = "Ada Example"`. The engine called `GET /profile/P-10001` (templated), extracted `response.name`, and chained `result → model → output`.
- **Failing fetch with `exception=risk-fallback`** → handler ran, `output.body = {profile_name, risk:"unavailable", customer_id, degraded:true, status:"ok"}` — a complete degraded response.
- **Failing fetch without `exception`** → forward traversal stopped (shape-output never ran) **but** the upstream error body was copied to `output.body = {error:"upstream_unavailable"}`.

| # | Asserted claim | Observed behavior | Verdict |
|---|---|---|---|
| 15 | Data-dictionary pattern (Provider+Dictionary+Fetcher, `dictionary[]`, `connect dict to provider with provider`) | works end-to-end against live HTTP | **keep** |
| 16 | Provider URL path templating `{person_id}` ← `input[]=person_id -> path_parameter.person_id` | filled correctly (`GET /profile/P-10001`) | **keep** |
| 17 | Dictionary `output[]=response.X -> result.Y` extraction | `response.name` → `result.profile_name` worked | **keep** |
| 18 | Fetcher `input[]=model.X -> param` / `output[]=result.Y -> model.Z` chaining | works | **keep** |
| 19 | Island/"dictionary container" node + `contains`/`data` edges | **not needed** — a single fetcher worked with only provider+dictionary+fetcher and one `provider` edge | **change** — present the container as optional/organizational, not required |
| 20 | `exception={handler-alias}` (build-critical, was only asserted) | **works** — on fetch 500, jumps to handler, which runs and rejoins; graph completes degraded | **keep/add** — now execution-backed |
| 21 | Failed fetcher records `{node}.status` / `{node}.error` | `fetch-risk` = `{status:500, error:{error:"upstream_unavailable"}, target:"dict-risk"}` | **keep** |
| 22 | "API fetcher failures abort traversal" (design claim) | **refine** — forward traversal stops, but the upstream error body is auto-copied to `output.body`; not a silent/empty abort | **change** |

## Join + for_each + concurrency (round 3)

Restructured the happy branch into a true fan-out-join: `check-id THEN: fan-out`; `fan-out` (no-skill) → `fetch-profile` + `fetch-risk` + `fetch-accounts` in parallel; risk success/fallback converge at `risk-complete`; `fetch-profile` + `risk-complete` + `fetch-accounts` → `join-sources` → `shape-output`.

| # | Asserted claim | Observed behavior | Verdict |
|---|---|---|---|
| 23 | No-skill node fans out across multiple natural edges (parallel) | `fan-out` hit profile + fail/risk + 3 accounts within ~6ms (stub timestamps) | **keep** |
| 24 | `graph.join` proceeds only when **all** backward-linked predecessors complete | join proceeded once profile + risk-complete + accounts all finished; output produced | **keep** |
| 25 | Join deadlock hazard — a non-completing predecessor stalls the join | wiring `validation-error` (never runs on happy path) as a join predecessor → join `.sink`, `output.body` never produced though all real branches ran | **keep/add** — confirms the design's D-02 warning; the convergence node is necessary, not stylistic |
| 26 | Exception-path convergence (success + fallback → single `risk-complete`) | works; join sees one always-completing risk predecessor | **keep** |
| 27 | `for_each` iterates a fetcher once per list item | 3 `account_ids` → 3 HTTP calls (`/accounts/A-1..A-3`); `model.accounts_collected` has 3 entries | **keep** |
| 28 | `concurrency` on an iterative fetcher | accepted; observable parallel/out-of-order iteration (A-2, A-1, A-3) | **keep** |
| 29 | `instantiate` array-append seeding `text(x) -> model.key[]` | builds `model.account_ids = [A-1, A-2, A-3]` | **add** — undocumented but useful for tests |
| 30 | `for_each` output aggregation via `output[]=result -> model.x[]` | appends per iteration → **nested arrays**; flattening likely needs `f:listOfMap` | **add/clarify** — document the nesting + the flatten idiom |

## graph.js + numeric + model.zero + extension (round 4)

| # | Asserted claim | Observed behavior | Verdict |
|---|---|---|---|
| 31 | `graph.js` COMPUTE executes JavaScript | `COMPUTE: two -> 1 + 1` → `enrich.result.two = 2` | **keep** |
| 32 | A skill node with no branch directive returns node-level `next` → natural traversal | `enrich` continued to `shape-output` after COMPUTE/MAPPING | **keep** — also partly answers the round-1 open `next` question |
| 33 | `graph.js` state access in an expression | `$.model.account_ids.length` (JSONPath + JS `.length`) **halted the node silently** | **add/change** — document the working state-access idiom; a malformed expression halts like graph.math |
| 34 | `int()` / `double()` constants | `int(42) -> 42`, `double(3.14) -> 3.14` | **keep** |
| 35 | "There is no `number()`" | `number(7)` rejected → **whole instance dropped** (input.body 404) | **keep** — confirmed, including the silent-drop |
| 36 | `model.zero` | **undefined** — `zero_probe` absent in output (null source skipped); `int(0)` works | **change** — the known bug; replace `model.zero` with `int(0)` |
| 37 | `export graph as {name}` / `import graph from {name}` | export persisted; import restored all 20 nodes/17 connections | **keep** |
| 38 | `session reset` | cleared the session to an empty graph | **keep/add** — confirms the (rolled-back) command is real |
| 39 | `graph.extension` (`extension={id}`, `input[]`, `output[]`) | node accepted; at runtime resolves the target from a **deployed-graph registry**, **not** the export/import name store — `extension=customer-360-sub` → `"not found"` (400), error copied to `{node}.status`/`.error` **and** `output.body`. Successful invocation needs a deployed sub-graph the scratch playground doesn't provide. | **partial/add** — wiring + error path confirmed; success path untestable here |

## f:defaultValue + with-type (round 5, independent-review follow-up)

An independent reviewer (fresh agent, no authoring context) re-tested two claims that had been documented without execution evidence:

| # | Asserted claim | Observed behavior | Verdict |
|---|---|---|---|
| 40 | `f:defaultValue(source, fallback)` | source present → source (`REAL-VALUE`); source null → fallback (`FB-absent`) | **keep** — the prescribed null-skip remedy is real (was previously documented but untested) |
| 41 | `with type` on skill nodes | **optional / cosmetic** — omitting it stores `types: ["untyped"]` and the node runs; `skill` drives behavior | **add** — document; type is load-bearing only for `Root`/`End`/no-skill nodes |

## Follow-along validation (round 6 — P5 spec dogfood)

A no-context agent built and tested a small `classify-score` graph by following only `build.md` / `test.md`. Two engine facts surfaced:

| # | Asserted claim | Observed behavior | Verdict |
|---|---|---|---|
| 42 | Seeding a *missing* required input | a bare `instantiate graph` with **no** seed lines yields a live empty `input.body: {}` (not a `404`) — companion's instance check passes | **add** — documents how to seed missing-input test scenarios (now in test.md) |
| 43 | `export` persistence over the REST driver | confirmed only via an in-place `session reset` + `import graph from {name}` (node count returns); the REST driver cannot mint a scratch session, so the round-trip clears and restores the live graph | **add** — corrects build.md's export-verification step (the scratch-session phrasing was not executable) |
| 44 | `graph.extension` **success** path (closes the claim-39 partial) | a parent calling deployed `tutorial-1` got `call-tut.result="hello world"`, `status=200`, `target=tutorial-1`; `output[]=result -> output.body.sub_result` applied. **Why claim 39 failed:** target resolves from `location.graph.deployed` (default `classpath:/graph/{id}.json`, read-only) — a different store from `export`/`import` (`location.graph.temp`, `/tmp/graph`). Only deployed graphs (shipped tutorials) are callable; `app.env` defaults to `dev`, so `tutorial-*` targets are allowed. | **verified** — success path works; export-name is not deployable at runtime via the Companion API |

## Verification-debt clearing (round 7)

Dogfooded the previously asserted-but-untested surface:

| # | Asserted claim | Observed behavior | Verdict |
|---|---|---|---|
| 45 | Function table beyond `f:defaultValue` | `f:now` (epoch ms `1780979431097` + local `2026-06-09 00:30:31.098`), `f:parseDate("2026-01-15", yyyy-MM-dd; ms)` → `1768453200000`, `f:uuid` → a UUID, `f:text(int 42)` → `"42"`, `f:includes("hello","ell")` → `true`, `f:concat("foo","bar")` → `"foobar"`, `f:listOfMap(model.cols)` (parallel `name[]`/`age[]`) → `[{name,age},…]`, `f:removeKey(list,"age")` → maps with `age` dropped | **verified** |
| 46 | `model.none` | resolves to null — used as a mapping source, the target is skipped (genuine null, unlike undefined `model.zero`) | **verified** |
| 47 | `graph.math` verbs `NEXT` / `DELAY` / `COMPUTE` | `NEXT: dest` jumped to the named node; `DELAY: 50` completed; `COMPUTE: marker -> 1 + 1` → `result.marker=2` | **verified** |
| 48 | `EXECUTE` statement | **corrects the doc**: it **merges another evaluator's statements into the current node** and runs them here — `EXECUTE: eval-a` (whose `COMPUTE` doubles) produced `driver.result.doubled=42`; `eval-a` itself was never traversed. Not "run a node"; does not run a mapper. | **verified + corrected** |
| 49 | `RESET` / `BEGIN`-`END` | source-verified (read `GraphMath.resetNodes` + `GraphLambdaFunction` iteration handling): `RESET` clears nodes for bounded loops; `BEGIN/END` bound which statements iterate under `for_each`. Not separately executed. | **source-verified** |

## graph.js COMPUTE `{var}` substitution (round 8 — retrospective claim verification, W39)

Tested the VoltaicMesh retrospective's two `graph.js`/`build` claims — "multi-line `'''` COMPUTE fails / multi-line IIFE is non-buildable" and "`{var}` refs are swallowed inside braces." Source first (`GraphJs.compute` → `substituteVarIfAny`/`replaceWithParameter` in `GraphLambdaFunction.java:208-261`; `Utility.extractSegments`/`findEndBracket` at `Utility.java:1316-1356`), then five live runs on a minimal `root → enrich(graph.js) → end` graph seeded `model.x = int(5)`.

| # | Asserted claim | Observed behavior | Verdict |
|---|---|---|---|
| 50 | `{var}` substitutes in a single-line COMPUTE | `COMPUTE: y -> {model.x} + 1` → `enrich.result.y = 6` | **verified** |
| 51 | `{var}` substitutes inside a single-line IIFE | `COMPUTE: z -> (function(){ return {model.x} * 2; })()` → `z = 10` | **verified** |
| 52 | Does a multi-line `'''` COMPUTE / IIFE work? | `'''`-wrapped `COMPUTE: mln -> (function(){\n return {model.x} * 3;\n})()` with `model.x=5` → `inspect enrich.result` = `{mln:15}` (statement stored with newlines intact; reproduced twice; distinct key, so not stale). Identical to the single-line form. The `404` logged in a now-corrected revision was an `inspect` **scalar-leaf** artifact — querying `enrich.result.mln` (a number) returns `404` by design (`InspectStateMachine.java:47-51` serves only `Map`/`List`), while the container `enrich.result` holds `{mln:15}`. | **multi-line WORKS — retrospective's "multi-line fails" claim REFUTED** (re-verified W41: two independent runs + a fresh-context reviewer + source). |
| 53 | Nested `{var}` inside a colon-brace (object literal) (refutes "swallowed inside braces") | `COMPUTE: obj -> JSON.stringify({tag: {model.x}})` → `obj = "{\"tag\":5}"` — `extractSegments` resolves innermost-first, so the inner `{model.x}` substitutes (→5) while the outer `{tag: …}` is left verbatim as a JS object | **verified — retrospective claim REFUTED** |
| 54 | The **one real constraint**: a `{…}` group whose inner text has `:`/newline/tab/CR is left verbatim and NOT substituted (`GraphLambdaFunction.java:238`) | `COMPUTE: bad -> {model.x : 0}` (a lone `{path}` sharing its braces with a `:`, no inner nesting) → `enrich.result` **absent (404)**: the token was left verbatim, the JS was invalid, and **the node halted silently** (matching claim 33's silent-halt-on-bad-expression) | **verified — the genuine, narrow footgun** |

**Net (corrected W41):** the retrospective made TWO claims; **both** are refuted.
- (1) "`{var}` refs are swallowed inside any brace" — **REFUTED.** Substitution is innermost-first, so a `{path}` resolves inline, inside a single-line IIFE body, and nested inside an object literal (rows 50/51/53). The *only* swallow is a lone `{path}` that shares its braces with a colon/newline/tab/CR (row 54), which then halts the node.
- (2) "multi-line `'''` COMPUTE fails / is non-buildable" — **REFUTED** (row 52): the multi-line form returns `{mln:15}`, identical to the single-line form, with newlines preserved in the stored statement.

So the one correct, narrow rule is: **keep each `{path}` in clean braces** — nesting inside object literals / IIFE bodies is fine, and a COMPUTE may be single- or multi-line. The *only* COMPUTE footgun is a lone `{path}` sharing its braces with a colon/newline (row 54).

> **Correction trail (W41, 2026-06-10) — three records, the third was the wrong one.** Row 52 was logged W39 as "multi-line works → 15"; revised W40 to "multi-line FAILS → 404"; re-verified W41 as **works**. W40's `404` came from inspecting the **scalar leaf** `enrich.result.mln` (a number → `404` by design, `InspectStateMachine.java:47-51`) and reading it as a silent halt — the exact inspect-scalar-404 trap the retrospective §IV(b) flagged. The container `enrich.result` always held `{mln:15}`. Re-confirmed two independent ways: a direct re-run (distinct key, reproduced twice, stored statement shown to retain its `\n`s) **and** a fresh-context `/graph-verify` reviewer given no hint of the prior verdict, which reproduced `{mln:15}` and independently traced the 404 to the same source. The recursive lesson the retrospective's §V predicts: *verify against the engine, and verify the verifications* — including a confident **correction**. This is precisely the failure the optional `/graph-verify` step exists to catch (see `verify.md`), and here it caught it.

## Round 9 — live re-verification (2026-06-16, session `ws-177533-2`, engine source @ `f9d77584`)

Re-verified against a live engine to resolve the Layer-2 orphan tags + the decision-edge contradiction, and to produce a verified simple **decision-branch exemplar** ([patterns/decision-branch.md](../patterns/decision-branch.md)).

| # | Asserted claim | Observed behavior | Verdict |
|---|---|---|---|
| 55 | A `graph.math` IF/THEN/ELSE that **names both targets** routes to exactly the matched branch — **even when the node also has two natural outgoing edges** | `decide` wired naturally to BOTH `dec-pass` and `dec-fail`; `score=85` → `output.body={result:"pass"}` only, `score=50` → `{result:"fail"}` only (the other branch never fired) | **keep/clarify** — resolves the apparent contradiction with footgun #1: the `next`-fan-out hazard is a **bare-`next`** return, NOT a named-target IF. `customer-360`'s `validate-person` (two natural edges + IF/THEN/ELSE) is correct. |
| 56 | An un-`'''`-wrapped IF/THEN/ELSE statement aborts the whole property block (hollow node) | `bad-decide` sent unwrapped → POST `accepted` but the stored node has **`skill` AND `statement` absent**; `companion.mjs` flagged `VERIFICATION FAILED` | **verified** — backs footgun #3 (was an orphan tag). |
| 57 | `inspect` serves containers only — scalar leaf → 404 | `GET /api/inspect/{s}/output.body.result` (string) → **404**; `GET …/output.body` (map) → **200** | **verified** — backs footgun #5 (was an orphan tag). |
| 58 | `create node end` with **no** `with properties` block is valid | `create node end` / `with type End` verified present | **add** — resolves a fresh-agent friction point. |
| 59 | Fan-out-and-join end to end: no-skill `fan-out` → 2 parallel mappers → `graph.join` (only `skill=graph.join`) → end; concurrent pre-join writes to **distinct** `output.body` keys both survive | `fan-out`→`copy-a`(`input.body.x→output.body.a`)+`copy-b`(`input.body.y→output.body.b`)→`join-branches`→end; run → `output.body={a:"hello-x", b:"hello-y"}` (both present) | **keep/add** — backs EV-3; refutes the fresh-agent worry that pre-join `output.body` writes are lost (they are not, for distinct keys). Exemplar `patterns/fan-out-join.md`. |
| 60 | `graph.js` COMPUTE interpolating a **string** `{path}` needs **manual quotes** — the engine substitutes the raw (unquoted) value (per C-54), so `'hi ' + {input.body.name}` is invalid JS; write `'hi ' + '{input.body.name}'` | `COMPUTE: greeting -> ('hello, ' + '{input.body.name}').toUpperCase()`, name=`ada` → `output.body.greeting="HELLO, ADA"` | **add** — extends C-50/C-51 (numbers) to strings; backs EV-1. Exemplar `patterns/linear-transform.md`. |
| 61 | List transform via a single `graph.js` COMPUTE `.map()` — `{model.ids}` substitutes to a JSON array; arrow callback with **no block braces** | `COMPUTE: results -> {model.ids}.map(x => x * 10)`, ids=`[1,2,3]` → `model.results=[10,20,30]` → `output.body.results=[10,20,30]` | **add** — backs EV-5. Arrow **must** be braceless (`x => x*10`); `x => {return…}` hits the C-54 brace guard and halts. Exemplar `patterns/iterative.md`. |
| 62 | `for_each` + `output[]` does **NOT** aggregate `graph.js` COMPUTE results | `for_each[]=model.ids->model.id` + `COMPUTE: tenx -> {model.id}*10` + `output[]=iterate.result.tenx -> model.results[]`: iterate ran (returned `next`, `iterate.result={tenx:30}` = last item only), but `model.results` never created | **change/clarify** — the `output[]` per-iteration aggregation idiom (C-30) is **fetcher/extension-specific**; for per-item *computation* use `.map()` (C-61), not `for_each`+`output[]`. |
| 63 | Fetch-and-shape end to end vs live HTTP, minimal wiring (C-19); fetcher `output[]` writes **directly to `output.body`** | Provider (templated `…/profile/{id}`) + Dictionary (`response.name -> result.profile_name`) + Fetcher (`dictionary[]`, `input[]=input.body.id -> id`, `output[]=result.profile_name -> output.body.profile_name`); 3 edges; `id=P-10001` → real `GET /profile/P-10001` → `output.body.profile_name="Ada Example"`, `fetch-profile.status=200` | **keep/add** — re-confirms C-15–C-19 live; adds: no shape mapper needed for a pass-through fetch. Exemplar `patterns/fetch-and-shape.md`. |
| 64 | `graph.extension` success calling a **deployed** target, live | `extension=tutorial-1`, `input[]=input.body.name -> name`, `output[]=result -> output.body.sub_result`; run → `output.body.sub_result="hello world"`, `call-tut.status=200`, target `tutorial-1` | **keep** — re-confirms C-44 live (deployed-location resolution; ≥1 `input[]` required). Exemplar `patterns/extension.md`. |

At engine source `f9d77584` (unchanged since the P0 dogfood — drift check empty), so these share the global `verified-against`. **Orphans #3/#5 are now backed (C-56/C-57); footgun #1's named-target case is clarified (C-55).**

## Immediate doc implications (for P1 — ✅ applied 2026-06-08, W11/W12)

- **graph.math / graph.js halt silently on a bad/unresolved expression.** Remove the `!= null` "direct null check" bullet; document null detection via `:boolean(null=true)` in a **mapper**, then branch. Document the working `graph.js` state-access idiom (`$.model.x.length` does not work).
- **`exception={alias}`** is real and works; failed fetcher records `{node}.status`/`.error`; a fetcher failure **without** a handler propagates the upstream error body to `output.body` (not a silent abort).
- **`graph.join`** waits for all backward-linked predecessors; a never-completing predecessor deadlocks it — the convergence-node pattern is required, not stylistic.
- **`for_each`** output via `output[]=result -> model.x[]` produces nested arrays; document the `f:listOfMap` flatten idiom. `instantiate` supports array-append seeding.
- **`model.zero` → `int(0)`** (the fetcher example bug).
- **`graph.extension`** targets resolve from the deployed-graph registry, distinct from `export`/`import` names — document this so authors don't expect an exported playground graph to be callable.
- Note the `types[]` vs `with type` asymmetry and the null-source-skips mapping behavior.

Applied to `minigraph-syntax.md` on 2026-06-08 (W11), then corrected per the independent review (W12): a verification legend now separates **(verified)** from asserted; the function table beyond `f:defaultValue`, `model.none`, and the `EXECUTE/RESET/NEXT/DELAY/BEGIN/END` verbs are labeled **asserted/untested** and carried as verification debt.
