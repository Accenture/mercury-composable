# AI Grammar Coverage Study — does Mercury live up to its claims?

**Status:** STUDY REPORT — empirical, fresh-agent evaluation of the Java engine's AI grammar
(discovery, sufficiency, token economics), with a ranked improvement backlog for the feedback
circuit.
**Method owner:** Claude Code, 2026-09-06 · commissioned by Eric Law
**Engine under test:** mercury-composable @ main `63621f1a` (post-v4.12.3)
**Study cost:** 46 subagents across two orchestrated phases (~4.9M tokens of agent work),
verdicts backed by file:line evidence throughout.

---

## 1 · What was tested, and how

The white paper ("Intent-Driven Development…", `docs/mercury-story.md`) makes four testable
claims about the AI grammar: it is **sufficient** (an AI partner can author correct artifacts
without reading engine source), **verified** (CI binds its claims to the code),
**discoverable** (the map routes to the right page in one hop), and **economical** (context
cost scales with the task, not the dependency tree). The study tested all four the way the
paper says methodology should be tested: with fresh agents under load.

**Phase 1 — inventory + discovery (12 agents).** Five persona seeds (app developer, graph
author, platform/ops engineer, field-history reconstruction from the CHANGELOG, AI-agent
authoring) proposed 45 use cases, canonicalized to **24** (10 core / 8 intermediate / 6 edge)
spanning all three layers, config/ops, building blocks, and polyglot. Each use case then went
to a **map-only** router: an agent allowed to read exactly one file — `docs/llms.txt` — and
required to pick its first-hop page(s) and judge the map (obvious / ambiguous / missing).

**Phase 2 — sufficiency trials (34 agents).** 17 of the 24 (8 of 10 core, 4 of 8
intermediate, 5 of 6 edge — including every case Phase 1 rated ambiguous) went through an
author → adversarial verifier pipeline:

- **Authors** were fresh agents locked to the documentation tier (`docs/**` only — no source,
  no examples, no poms), given only the developer's question (never the acceptance rubric),
  required to log every file read and to declare gaps instead of inventing.
- **Verifiers** had full repository access and an adversarial mandate: refute each artifact
  against engine source, tests, and reference examples; classify every defect and every
  author-declared gap as *doc gap confirmed* vs *author error*, citing file:line evidence.

The three highest-impact findings were independently re-verified by the study author before
publication.

## 2 · Headline results

| Claim | Result | Evidence |
|---|---|---|
| **Sufficient** | **Substantially holds.** 17/17 docs-only artifacts were working or near-working; **0/17 wrong**. 8/17 correct as-is; 9/17 minor-issues — and the majority of those defects were *inherited from incorrect doc sentences*, not authoring failures. Every edge-case gotcha trial (serialization downcast, Mono trace teardown, sub-flow ttl, suspend/resume, CSFLE) came out **correct**. | §5 matrix |
| **Discoverable** | **Holds with named exceptions.** 19/24 one-hop routes obvious, 5/24 ambiguous, **0 missing**. All five ambiguities are annotation gaps in the map text, not structural holes — and in Phase 2, four of those five still produced correct artifacts. | §4C |
| **Economical** | **Strongly holds.** Map ≈ 4.0k tokens. Per-task docs cost: **min 13.7k / median 45.8k / max 86.4k tokens**, vs ≈ 542k tokens of engine source — **2.5%–16% of the source-corpus ceiling, 8.4% at the median** (ceiling, not a measured source-hunt control; see §6). The full guide tier is ≈ 230k tokens; no trial needed even half of it. | §3, §6 |
| **Verified** | **Holds for the gated surfaces; measurable drift in ungated prose.** The three DSL kits (catalogs + drift tests) held up — no trial found a catalog contradicting the engine, with one omission (`stream:`). But the study found **10 places where doc *prose* contradicts engine behavior**, none covered by any gate. | §4A |
| **Prototyping speed** | Assessed by proxy only (no wall-clock or source-arm control): authors produced each complete deliverable in a single pass over 4–12 doc files, with high self-reported confidence in 17/17, and no trial stalled. The declared-gap mechanism worked as designed: 81 declared gaps, most verifier-confirmed as genuine, about one in eight reclassified as author error or non-gap. | §5, §6 |

**Bottom line:** Mercury lives up to its claims for the objective Eric set — *meaningful
coverage for most use cases, source as the edge-case fallback*. No trial produced an unusable
deliverable, even at the edges (one printed *element* — UC-09's guessed Maven groupId — would
fail resolution as written; its author flagged it as unverified). The study's real yield is
the backlog below:
10 drift fixes and a short list of recurrent gaps that would move several "minor-issues"
verdicts to "correct".

## 3 · Token economics (Java engine, measured)

| Tier | Size | ≈ Tokens |
|---|---|---|
| `docs/llms.txt` (exhaustive map) | 16.0 KB | ~4,000 |
| Full guide tier (46 md + 3 JSON catalogs) | 921 KB | ~230,000 |
| Engine source (`system/*` main Java) | 2,169 KB | ~542,000 |

Per-trial cost = map + the pages the author actually read (4–12 files):
13.7k (UC-17, SSE) … 45.8k median … 86.4k max (UC-04, error handling). The paper's benchmark
(measured on the Rust engine's curated map: ~2.5k → ~46k vs ~515k) is **consistent with the
Java measurement**: the Java map is larger (exhaustive by design, 4k vs 2.5k) but the routed
per-task cost lands in the same band, and the median task costs ~8% of the source alternative.

## 4 · Findings

### 4A · Doc drift — prose that contradicts the engine (10, ranked by blast radius)

These are the "verified" claim's blind spot: all live outside the drift-tested surfaces.
Each was confirmed against source; items 1–3 re-verified independently.

1. **`error.status` should be `error.code`** — the exception-context status key is named
   `error.status` in `flow-grammar.md:90`, `syntax.md:554`, and
   `event-envelope-reference.md:713`, but the engine defines only `code`
   (`TaskExecutor.java:84`); `error.status` maps null silently. The worked examples and
   `flow-schema-reference.md` are correct — the three prose/table mentions are the drift.
   *(Hit in 2 independent trials; both authors navigated it by trusting the examples.)*
2. **Config precedence inverted** — `configuration-reference.md:27-28` says `.properties`
   beats `.yml`; the engine merges `application.yml` last with putAll semantics, so **.yml
   wins** (`app-config-reader.yml:24`, `AppConfigReader.java:121` — proven empirically by
   running platform-core-4.12.3.jar with both files present). Same lines also overstate
   env-var handling for plain-Java apps (env vars enter via `${VAR}` substitution, not direct
   key override).
3. **Kafka success threshold** — `minimalist-kafka.md:370` says a flow succeeds only with
   status 200; the engine acknowledges any status **< 400** (`KafkaFlowConsumer.java:459`).
   A 201/204/3xx reply is NOT retried or dead-lettered. *(Hit in 2 trials.)*
4. **Startup failure semantics overstated** — `event-driven/ai-agent-guide.md:40-41` says
   duplicate routes / bad interfaces fail the application at startup; the engine reloads or
   skips with a log and keeps starting (`Platform.java:565-568`, `AppStarter.java:393-424`).
5. **`envInstances` example invalid** — `annotations-reference.md:163` shows
   `envInstances="${HEAVY_TASK_INSTANCES:10}"`, but `envInstances` must be a property KEY;
   the `${...}` literal silently no-ops (`AppStarter.java:495-496`). Lines 176-177 also
   present `worker.instances.<route>` as a generic override without the opt-in condition that
   `configuration-reference.md:357-369` states correctly.
6. **`inspect` semantics wrong** — `command-reference.md:214, 225-226` say inspect resolves
   only leaf keys / prints `{}` for subtrees; the engine returns populated nested subtrees
   (`MultiLevelMap.getElement`, pinned by `PlaygroundTest.java:308`).
7. **Orphan-export rule unbacked** — `command-reference.md:284` claims export fails on orphan
   nodes; no such check exists in the export path (and a single-unconnected-root export
   succeeds in `CompanionSyncTest`). CompileGraph re-validates at the gate, so the claim is
   also unnecessary.
8. **Non-compilable snippet** — `composing-the-layers.md#exposure`'s `graph-executor.yml`
   omits mandatory `flow.description`/`flow.ttl`/task `description` (CompileFlows rejects it)
   and the exception-handler task the real flow carries. *(Partly author-avoidable in the
   trial — `flow-grammar.md`, which the author read, states the mandatory fields — but the
   published snippet is genuinely broken and worth fixing.)*
9. **Flows location default wrong** — `flow-schema-reference.md:52` says the default is the
   resources folder root; the engine default is `classpath:/flows/` (`CompileFlows.java:135`).
10. **Stale Playground transcript** — `build-your-first-graph.md:129-130` shows an export
    acknowledgment line the current engine no longer emits.

*(Minor accuracy nits, same class: `workflow-suspension.md:367` lists 6 of 9 reserved model
keys; suspend-node validation requires ≥1 outgoing connection, not literally "to end".)*

### 4B · Coverage gaps — genuinely missing from the docs tier (ranked by recurrence)

1. **Maven coordinates — the #1 systemic gap (6 of 17 trials).** No page in `docs/` prints a
   dependency block for `platform-core`, `event-script-engine`, `minimalist-kafka`, or
   `minigraph-state-redis`. Framing note: this is partly by design — the docs delegate to
   `examples/*/pom.xml` and module READMEs, a path the study's docs-only tier deliberately
   severed. But the trap inside the gap is real regardless of tier: module artifacts use the
   legacy `org.platformlambda` groupId while the reactor parent is `com.accenture.mercury`,
   and one author guessed wrong exactly there (the only outright-broken element produced in
   17 trials). One dependency-coordinates block in `getting-started.md` (plus per-guide
   snippets for the opt-in modules) kills the entire class — for both repo readers and
   packaged-skill consumers, who have no `examples/` either.
2. **Import/package lines (5 trials).** `AppException`, `Utility`, `EventStreamWriter`,
   `TraceInfo`/`po.getTrace()` appear in docs unqualified; `org.platformlambda.core.exception`
   and `.core.util` appear nowhere in `docs/`. Authors guessed by convention (correctly), but
   an import line each, once, ends the guessing.
3. **`stream: true` missing from the REST catalog.** Documented only in `http-streaming.md`;
   absent from `rest-grammar.md` and `rest-automation.json`'s `rest_entry_fields` — an agent
   validating against the authoritative catalog would flag a correct entry as unknown. This is
   the one place a *gated* surface has a hole.
4. **`configuration-reference.md` completeness**: no entries for `graph.model.automation`,
   `location.graph.temp`, or the `schema.registry.serde.*` pass-through prefix.
5. **Decision-task runtime semantics** (undocumented and surprising): a null decision aborts
   the flow, but a garbage string silently coerces to branch 1 (`TaskExecutor.java:720-737`,
   `Math.max(1, str2int(...))`). Belongs in `syntax.md`'s decision section.
6. **`graph.extension` edge behaviors**: a missing `flow://` target throws *before* the call
   and is NOT `exception=`-routable; `flow://` ids are not validated by the CompileGraph gate
   (call-time resolution). Belongs in `skills-reference.md`'s delegation contract.
7. **Event-over-HTTP operational note** (→ `event-over-http.md`): the map loads once at
   startup — editing requires a restart (`EventEmitter.java:135-147`); nowhere stated. Also
   unstated there: nested `event:`/`http:` and flat `event.http:` spellings are equivalent.
8. **Tracing API reference** (→ `api-overview.md` Distributed-tracing section +
   `observability.md` ~line 73): `po.getTrace()`/`TraceInfo` in no reference page; the
   annotate-before-worker-return deadline for Mono functions; late `annotateTrace` is a
   silent no-op; events sent off-worker get trace id but no span parenting.
9. **Smaller confirmed absences, each verifier-named to a page**:
   `graph.exception.handler` is an engine built-in (→ `composing-the-layers.md`); root
   `purpose` is enforced only at the CompileGraph gate, not by dry-run validation
   (→ `command-reference.md`); missing/empty `rest.yaml`/`flows.yaml` degrades gracefully,
   never fails startup (→ `configuration-reference.md`); `simple.kafka.notification`
   auto-stamps the correlation-id header as fallback and rejects non-byte[]/Map/List/null
   bodies with IllegalArgumentException (→ `minimalist-kafka.md`); a child flow's own
   `flow.exception` cannot swallow its TTL abort, the timeout message text, and the
   resilience-handler missing-attempt→0 default (→ `flow-schema-reference.md` ttl section);
   no page shows a direct-bound (non-flow) function reading a path parameter end-to-end
   (→ `rest-automation/index.md`); absent `graph.model.automation` = warn + nothing
   executable, and the `/api/graph` wiring ships pre-wired in the playground apps
   (→ `composing-the-layers.md`); Playground browser URL (→ `build-your-first-graph.md`);
   shipped `auto.offset.reset` default `earliest` and `input.metadata.timestamp` type,
   epoch millis (→ `minimalist-kafka.md`); default no-handler error body shape
   (→ `flow-grammar.md`); suspended-reply HTTP status 200 (→ `workflow-suspension.md`);
   user-PoJo shape requirements (→ `event-driven/ai-agent-guide.md`); the CSFLE serde
   credential-inheritance guarantee — the v4.12.3 fix, code-proven but unstated
   (→ `minimalist-kafka.md` CSFLE §3).

### 4C · Map annotation gaps (the 5 ambiguous routes from Phase 1)

No use case was unroutable, but five routed on inference rather than map text. One-line
annotation touch-ups close them: name **exception handling / `AppException`** somewhere in
the Event Script cluster; name **`decision`** in a Layer-2 blurb; say which envelope page owns
**serialization gotchas**; name the **trace-context lifecycle** in the Observability blurb;
name **`ttl` / sub-flow deadlines** in the flow-grammar blurb. (Also worth a word each:
`CompileGraph`/manifest in a Layer-3 blurb, env-var/`${VAR:default}` overrides in the
config-reference blurb, and the graph.js deprecation.)

### 4D · The exoneration file — author errors prove the docs often had it

Verifiers reclassified about one in eight of the 81 author-declared gaps as **author error**
or non-gap: the information
existed in docs the author skipped or misread (`int(201) -> output.status` in
flow-schema-reference; `application.name`, `no.op`'s registration in
reserved-names-and-headers; the rest.yaml auth contract; `output.status` in the KG agent
guide; the `:type` suffix's model-only scope). This matters for calibration: the grammar's
content coverage is *better* than the raw gap count suggests — a real developer with an IDE
and search closes most of these instantly.

## 5 · Trial matrix

| UC | Use case | Tier | Route | Verdict | Docs tokens | Doc-drift inherited? |
|---|---|---|---|---|---|---|
| 01 | REST endpoint → flow | core | obvious | minor-issues | 62k | no (author overstatements) |
| 02 | Typed PoJo function | core | obvious | minor-issues | 47k | yes (§4A-4, -5) |
| 04 | AppException + exception handler | core | **ambiguous** | **correct** | 86k | navigated §4A-1 correctly |
| 05 | Minimum app config | core | obvious | minor-issues | 52k | yes (§4A-2) |
| 06 | First graph dry-run | core | obvious | minor-issues | 25k | yes (§4A-6, -7) |
| 07 | CompileGraph deploy gate | core | obvious | minor-issues | 78k | partly (§4A-7, -8, -10; the §4A-8 snippet defect was also author-avoidable) |
| 08 | Kafka topic → flow | core | obvious | minor-issues | 74k | yes (§4A-3) |
| 09 | simple.kafka.notification | core | obvious | minor-issues | 37k | yes (§4A-3) + wrong groupId guess (§4B-1) |
| 11 | Decision branching | int. | **ambiguous** | minor-issues | 31k | partly (§4B-5 unknown to docs) |
| 15 | graph.extension flow delegation | int. | obvious | minor-issues | 27k | partly (§4B-6) |
| 17 | SSE progressive streaming | int. | obvious | **correct** | 14k | — |
| 18 | Declarative Event-over-HTTP | int. | obvious | **correct** | 31k | — |
| 19 | Long→Integer downcast | edge | **ambiguous** | **correct** | 46k | — |
| 20 | Mono trace teardown | edge | **ambiguous** | **correct** | 53k | — |
| 21 | Sub-flow ttl override | edge | **ambiguous** | **correct** | 40k | navigated §4A-1 correctly |
| 22 | Suspend/resume on Redis | edge | obvious | **correct** | 83k | — |
| 24 | Schema Registry + CSFLE | edge | obvious | **correct** | 35k | — |

Notable: **the harder the tier, the better the result** — edge 5/5 correct, intermediate 2/4,
core 1/8 (seven of the eight core trials were minor-issues, most inheriting a §4A drift item);
the five ambiguous-route trials still went 4/5 correct. The gotcha content — serialization,
tracing, ttl, suspension, CSFLE — is where the guides are strongest (it was written from field
pain); the drift lives in the older walkthrough/reference prose that predates the gates.

## 6 · Limitations, honestly

- **The docs-only rule was stricter than the real consumer surface, in two ways.** (1) The
  packaged Agent Skill (`ai-contract-provider`) ships `references/fixtures/rest-bindings.yaml`,
  so the "snippet-include outside docs/" gap the UC-01/02/17 authors hit does **not** affect
  skill consumers — only raw-repo readers. (2) The docs deliberately delegate some content
  (dependency poms, reference handler code, module READMEs) to `examples/` — a path the study
  severed; §4B-1's ranking should be read with that in mind, though the fix recommendation
  stands because packaged-skill consumers have no `examples/` either.
- **The economics ratio is against a corpus ceiling, not a measured control.** The 542k-token
  denominator is the whole engine source; no control arm measured what a source-allowed agent
  actually reads in a grep-guided hunt. The absolute per-task numbers (13.7k/45.8k/86.4k) are
  the solid claim; the percentage framing is indicative only.
- **Prototyping speed was assessed by proxy** (files-read counts, single-pass completion,
  self-reported confidence) — no wall-clock, turn-count, or examples-available comparison arm.
- Single model family authored and verified (independent contexts, adversarial prompts, but
  shared blind spots are possible). The paper's fresh-agent exercises used live multi-vendor
  agents; this study trades that for controlled tiering and source-verified verdicts.
- 17 of 24 use cases were trialed for sufficiency (all 24 for discovery). Untrialed: UC-03,
  UC-10 (core), UC-12/13/14/16 (intermediate), UC-23 (edge) — each adjacent in kind to a
  trialed case.
- Token counts approximate at 4 bytes/token, same method as the paper's benchmark.
- Verifier verdicts, while evidence-cited, are themselves agent judgments; the three most
  load-bearing drift claims were independently re-verified by the study author, and this
  report itself passed an adversarial claim-check and completeness critique (whose findings
  were applied).

## 7 · Recommendations (balancing discovery · tokens · prototyping speed)

**Now (cheap, high trust-ROI):**
1. Fix the 10 drift items in §4A — they are one-line-to-one-paragraph edits, and drift is the
   direct counterexample to the grammar's "verified" promise.
2. Add ONE dependency-coordinates block (`getting-started.md`) + module snippets in the
   Kafka/Redis guides; add the missing import lines where each class is first shown. Kills
   the two most recurrent gap classes for a few hundred tokens.
3. Add `stream` to `rest-automation.json` + `rest-grammar.md` — restores the catalog's
   authority for the one field it misses.

**Next (small PRs, feedback-circuit fed):**
4. `configuration-reference.md` completeness sweep (§4B-4) and the §4B-5..8 behavioral notes,
   each on the page the verifier named.
5. The eight map-annotation touch-ups (§4C) — a few words each; the map stays ~4k tokens.

**Directional (the interesting one):**
6. The gates cover catalogs and grammar, not prose behavior claims — and that is exactly
   where all 10 drift items live. Consider a small **claims-fixture test**: a curated list of
   high-value doc assertions (defaults, precedence, thresholds, failure semantics) each pinned
   by an existing or tiny new engine test. Ten entries would have caught every §4A item. This
   extends "compiled documentation" from *shape* to *behavior* — the natural next increment of
   the methodology, and portable to the Rust engine.
7. Do **not** inflate the map or split the big pages: 94 KB `syntax.md` still landed inside
   economical per-task budgets, and Phase 1 shows the exhaustive map's discovery quality is
   annotation-limited, not size-limited.

---

*Method artifacts: Phase 1 (inventory + discovery) run `wf_d3e82229-9cc`; Phase 2 (authoring
trials) run `wf_f201705c-02f` — full per-agent transcripts and structured results retained in
the session's workflow journals. All file:line citations refer to main `63621f1a`.*
