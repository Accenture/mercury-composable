# P0 Dogfood Evidence — Customer 360

Status: P0 complete. All targeted primitives probed; `graph.extension` successful-invocation is the one partial (needs a deployed sub-graph the scratch playground can't provide).
Engine: `localhost:8085`, session `ws-844990-2`. Stub: `node evidence/stub-server.mjs` on `localhost:8099`. Date: 2026-06-08.
Method: built one mutation at a time through `companion.mjs` (each auto-verified), then instantiate + run with `--expect`, then inspect state. Traversal path (`seen`) is WebSocket-only, so all conclusions are inferred from inspected state.

This is the P0 output artifact referenced by improvement-plan.md Gate 1.

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

## Immediate doc implications (for P1 — ✅ applied 2026-06-08, W11/W12)

- **graph.math / graph.js halt silently on a bad/unresolved expression.** Remove the `!= null` "direct null check" bullet; document null detection via `:boolean(null=true)` in a **mapper**, then branch. Document the working `graph.js` state-access idiom (`$.model.x.length` does not work).
- **`exception={alias}`** is real and works; failed fetcher records `{node}.status`/`.error`; a fetcher failure **without** a handler propagates the upstream error body to `output.body` (not a silent abort).
- **`graph.join`** waits for all backward-linked predecessors; a never-completing predecessor deadlocks it — the convergence-node pattern is required, not stylistic.
- **`for_each`** output via `output[]=result -> model.x[]` produces nested arrays; document the `f:listOfMap` flatten idiom. `instantiate` supports array-append seeding.
- **`model.zero` → `int(0)`** (the fetcher example bug).
- **`graph.extension`** targets resolve from the deployed-graph registry, distinct from `export`/`import` names — document this so authors don't expect an exported playground graph to be callable.
- Note the `types[]` vs `with type` asymmetry and the null-source-skips mapping behavior.

Applied to `minigraph-syntax.md` on 2026-06-08 (W11), then corrected per the independent review (W12): a verification legend now separates **(verified)** from asserted; the function table beyond `f:defaultValue`, `model.none`, and the `EXECUTE/RESET/NEXT/DELAY/BEGIN/END` verbs are labeled **asserted/untested** and carried as verification debt.
