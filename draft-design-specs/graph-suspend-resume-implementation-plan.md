# Graph suspend/resume — design review + implementation plan

**Status:** RATIFIED design, plan ready for P1 — 2026-07-28, Claude Code
**Concept:** Eric's `graph-suspend-resume-concept.md` (2026-07-28) + ratified rulings:
(R1) dedicated skills `graph.suspend` / `graph.resume` encapsulate persistence I/O — no
node-level data mapping, skills are a superset of `graph.task`; (R2) **skill defines
behavior, type is visual** — types `Suspend`/`Resume`/`Suspensible` are convention/best
practice only; suspensibility of a skilled node is the reserved node property
`suspend=true`; (R3) **`suspend` is a reserved node ALIAS** (the root/end pattern: a
special alias resolved by name because traversal jumps to it — the routing vocabulary is
`"next" | node-alias`, so the suspensible override is a plain jump to the well-known
name). Bidirectional: alias `suspend` ⇔ `skill=graph.suspend`; exactly one suspend node
per graph. The `resume` NAME stays convention (it is reached by normal traversal — no
mechanism rides its name). (R4) The Redis store is a self-contained module/crate imported
by the PLAYGROUND EXAMPLE APP, never by the engine (both repos); engine unit tests use a
temp-file mock store under `/tmp/suspend-resume` implementing the same contract.
Decisions D1-D6 ratified as recommended (D3: not-found = normal first-run case,
default `next`).
**Grounding:** five-agent code study of the live tree (engine core, state machine, entry
paths, Redis/TTL precedents, Rust parity), file:line anchors below are from that study.

---

## 1. Verdict

**The design is sound and viable.** It fits the engine's architecture unusually well:

- **Jump-to-node already exists** — a skill's reply body naming a node alias jumps there
  (`GraphExecutor.nextOrJump`, GraphExecutor.java:286-300). Resume rides an existing
  primitive, not a new traversal mode.
- **Suspension converts a long-lived workflow into a sequence of short-lived runs.** Each
  run stays inside the normal flow ttl (rest.yaml timeout); only the persisted record is
  long-lived (its own `ttl`, e.g. `2d`). No engine timer, no parked instance, no memory
  pinned between runs — the in-memory `GraphInstance` lifecycle (created per run, reaped by
  `graph.housekeeper` at flow end) is untouched.
- **The pluggable store has an exact in-repo precedent**: sync-over-async's
  `ReturnRouteStore` (SETEX/GET/DEL keyed by cid, TTL as crash safety net, explicit delete
  as fast path — ReturnRouteStore.java:36-69), including embedded-redis tests and the
  `RedisConfig` record with `${ENV_VAR:default}` support.
- The two rulings (R1/R2) already dissolved the concept's three worst gaps (resume
  jump-target channel, CompileGraph mapping special cases, persist-envelope
  under-specification).

The one hard constraint the code imposes: **traversal position is not a pointer.** The
executor's state is a set of pending event callbacks (`<flowInstanceId>@<node>` cids) plus
`nodeSeen`/`skillRun` maps; `walkNext` fans out to ALL forward links unconditionally
(GraphExecutor.java:302-310), and there is no registry of in-flight branches. Suspension
is therefore only well-defined when the suspending path is the graph's sole active branch
(§4-D5). Everything else in the concept survives contact with the code.

## 2. Corrections to the concept doc (verified against the tree)

1. **"CompileGraph will reject `result -> model`" is wrong.** CompileGraph does only
   deprecated-syntax conversion + structural import + root-purpose check
   (CompileGraph.java:97-161). The bare-`model` RHS rejection is a *runtime* check in the
   skills' output-mapping path (GraphLambdaFunction.setFetcherOutputEntry:478-481), and it
   only fires when the mapped value is non-null. Conversely, `model -> *` as a graph.task
   INPUT is *already allowed today* and ships the whole model (incl. copied reserved keys).
   Under R1 both points become moot for this feature (no mapping on the new nodes), but the
   doc's claims should be corrected.
2. **`root`/`end` are not "reserved names" in the code's sense** — they are special aliases
   outside `MiniGraph.RESERVED_NAMES` (which holds input/output/model/response/result/
   parameter/none/next/api/error, MiniGraph.java:42-43). Under R2, `suspend`/`resume` names
   stay unreserved (convention), so no change to either list is needed for *names*.
3. **Reserved-property scope (ruled):** `suspend` and `missing` join `RESERVED_PARAMETERS`
   (engine-routing configuration, the `for_each`/`concurrency` precedent). `ttl` is
   deliberately NOT reserved — Eric's ruling: it is a task parameter (the store's expiry
   timer) with no collision anywhere, so it keeps the standard node-parameter behavior
   (echoes into the state machine as `suspend.ttl`, harmless).
4. **Kafka-initiated graphs don't work directly today**: the kafka-flow-adapter dataset has
   no `path_parameter.graph_id`, so `graph-executor` rejects it ("Missing graph ID in
   header", GraphExecutor.java:103-105; KafkaFlowConsumer.toDataset:429-435). A wrapper
   flow (or programmatic `FlowExecutor.launch` with a hand-built dataset) is required.
   Resume-from-Kafka works the same way — with the business cid arriving via the
   configured Kafka correlation header. Worth one sentence in the feature docs.
5. Nits: "Suspensbile" (typo); the concept's `skill.math`/`skill.js` should read
   `graph.math`/`graph.js`; the resume-node example's `purpose` says "Persist…" where it
   means "Retrieve…"; the concept's dual mechanisms ("type contains Suspensible" vs the
   example's `suspend=true`) are resolved by R2 in the property's favor.

## 3. Feature specification (post-rulings)

### 3.1 New reserved surface

| Item | Kind | Notes |
|---|---|---|
| `graph.suspend` | skill route (built-in, minigraph engine) | superset of graph.task; instances=300 like peers |
| `graph.resume` | skill route (built-in, minigraph engine) | superset of graph.task |
| `suspend` | reserved node property (`true`) | on skilled nodes; routes to the suspend path after skill + output mapping |
| `ttl` | node parameter (NOT reserved — Eric's ruling: it is a task parameter, the store's expiry timer, colliding with nothing) | on `graph.suspend` nodes; **mandatory, no default** (a checkpoint may wait a minute or days — only the designer knows); `Utility.getDurationInSeconds` format (`20s/5m/2h/2d`, bare number = seconds — Utility.java:1185-1203; NOT the rest.yaml 1s-5min clamp) |
| `missing` | reserved node property (optional) | on `graph.resume` nodes; not-found behavior (§4-D3) |
| Types `Suspend`/`Resume`/`Suspensible` | convention only | UI colors; engine never reads types (verified: traversal reads only `skill`) |

### 3.2 graph.suspend (persist + terminate)

Node shape (no input/output mapping — R1):

```text
create node suspend
with type Suspend
with properties
purpose=Persist workflow state to the external state store
skill=graph.suspend
task=v1.redis.persist.model
ttl=2d
```

Behavior:
1. Validates `task` route exists (GraphTask precedent, GraphTask.java:68-74) and `ttl`
   parses; reads business cid from state machine `model.cid` — **error 400 if absent**
   (playground instances without an init-mapped model.cid fail loudly, §4-D6).
2. Builds the persistence envelope itself (no mapping):
   - `model`: deep-copied model namespace **minus reserved keys**
     (`model.flow/instance/trace/ttl/parent/root` excluded; `model.cid` excluded from the
     blob — it IS the key). Deep copy via `Utility.deepCopy`; the state machine map is
     live (`MultiLevelMap.getMap()` returns the underlying map), so the copy is mandatory.
   - `node`: the suspension point — the node that routed here (the `from` provenance,
     §3.5).
   - bookkeeping per §4-D2 (recommended: `nodeSeen` + `skillRun` snapshots).
3. Invokes the `task=` function **synchronously** (`po.eRequest` + Mono, the GraphTask
   pattern at GraphTask.java:142-164) and treats a non-2xx reply as a node error
   (`exception` property honored like any skill). **Deliberately NOT the fire-and-forget
   `ext:` external-state-machine contract** (TaskExecutor.java:1155-1183) — suspension is
   a durability promise; it must not report success before the store acks.
4. Default response staging (§4-D4): if state-machine `output.body` is still empty, stage
   `{"type":"suspended","cid":"<cid>"}` so the caller of the suspended run gets a
   meaningful 200 body instead of an empty 200 (verified: null output.body → empty 200,
   AsyncHttpResponse.handleContent:212-236). Designers override by staging `output.*`
   before the suspend node.
5. Returns `next` → traversal proceeds along its forward link(s) to `end` →
   `executionComplete` → normal flow response. No new termination mechanics.

### 3.3 graph.resume (retrieve + jump)

```text
create node resume
with type Resume
with properties
purpose=Restore workflow state from the external state store
skill=graph.resume
task=v1.redis.retrieve.model
```

Behavior:
1. Reads `model.cid`; invokes `task=` synchronously with `{cid}`.
2. **Found:** merges the persisted `model` blob into the state machine model namespace
   (fresh reserved keys — seeded by the new FlowInstance — are never overwritten; the blob
   contains none by construction). Restores bookkeeping per §4-D2 and marks the suspension
   node `nodeSeen`+`skillRun` (so a downstream `graph.join` counts it as a completed
   predecessor — GraphJoin.java:66-76 gates on exactly those maps). Replies with the new
   directive **`resume:<node>`**.
3. **Not found** (fresh transaction or TTL-expired): default replies `next` — the resume
   node's normal forward links ARE the fresh path, so first-run pass-through falls out of
   the existing NEXT directive with zero extra mechanics. Optional `missing=<node-alias>`
   property jumps to a designer-chosen handler instead (§4-D3).

### 3.4 Executor changes (GraphExecutor + GraphTraveler, kept identical)

1. **`resume:<alias>` directive** in `nextOrJump` (GraphExecutor.java:286-300): resolve
   the alias, do **not** execute it — mark it seen/run and `walkNext(alias)` traversing
   its forward links, **skipping the link whose target alias is `suspend`** (trivial
   name-based exclusion under R3). `:` is outside the alias charset
   (GraphProperties.validateName:53-70), so the directive cannot collide with a node name.
2. **Suspensible routing = a plain jump to the reserved alias `suspend` (R3).** A node
   with `suspend=true`, on first execution: after its skill completes with `next`, the
   executor jumps to `suspend` — the existing directive mechanism, no link classification
   needed. The drawn edge `<suspensible> → suspend` is still REQUIRED (CompileGraph §3.6)
   so the visual model stays truthful — the graph is the documentation. A plain edge into
   the `suspend` node (the concept's island example) remains an unconditional suspension
   with no property needed. `getSuspendNode()` resolves by alias, the root/end pattern
   (MiniGraph.getRootNode/getEndNode precedent).
3. **`from` provenance**: thread the walking-from alias through
   `walk/walkTo/executeSkill` and deliver it as skill header `from`. This is how
   `graph.suspend` learns the suspension point; it's also generically useful diagnostics.
   (Today's headers: in/type/node — GraphExecutor.java:277-280.)
4. **Reserved property additions**: `suspend` and `missing` join `RESERVED_PARAMETERS`
   (GraphLambdaFunction.java:149-151) — engine-routing configuration. `ttl` stays a plain
   node parameter (mandatory on the suspend node, validated by the skill; no default).
5. `suspend=true` runtime guard: reject on nodes whose skill is `graph.math`/`graph.js`
   (routing skills — suspension point must be deterministic), mirroring the concept.

### 3.5 The persistence-store contract (the pluggable seam)

Documented as a contract page (the registration-metadata-contract precedent — a spec page
developers implement against):

**Persist** — request to `task=` route of the suspend node:
- headers: `type=put`
- body: `{ "cid": "<business cid>", "node": "<suspension node alias>",
  "ttl": <seconds:int>, "model": {…}, "seen": {…}, "run": {…} }`
- reply: 2xx = durably stored; anything else = suspension fails (node error path).

**Retrieve** — request from the resume node:
- headers: `type=get`
- body: `{ "cid": "<business cid>" }`
- reply body: the persisted map (as stored) or null/404 for absent-or-expired.
- Consume semantics per §4-D1 (recommended default: atomic consume — Redis `GETDEL`).

Serialization is the store's concern; the reference Redis implementation stores the map as
MsgPack bytes (byte[] round-trips; known caveats documented: Integer/Long width, PoJos
already arrive as maps on the graph state machine since skill results transit the bus,
Date/Instant→ISO strings, null values dropped unless `serializer.null.transport=true` —
MsgPack.java:156-190, 260-274, 292-311). Stores without native TTL implement expiry
themselves (concept doc; contract page states it).

### 3.6 CompileGraph additions (structural only, opt-in graphs)

- Alias `suspend` ⇔ `skill=graph.suspend`, bidirectional (a node named `suspend` must
  carry the skill; a `graph.suspend` node must be named `suspend`) → reject on mismatch.
- `suspend=true` on a node whose skill is `graph.math`/`graph.js` → reject.
- `suspend=true` node with no drawn edge to the `suspend` node → reject (routing is by
  name, but the diagram must show the suspension path).
- `graph.suspend` node without `task`/`ttl`, `graph.resume` node without `task` → reject.
- (Runtime re-checks stay in the skills — CompileGraph is opt-in via
  `graph.model.automation`, so runtime remains the enforcement floor.)

### 3.7 Reference store module

New optional module **`extensions/minigraph-state-redis`** providing
`v1.redis.persist.model` + `v1.redis.retrieve.model` (Eric's route names; the v1.* dotted
convention is documented — code-conventions.md:57):
- **Imported by the playground/example application, never by the engine (R4)** — the
  engine stays store-free in every scope (verified: minigraph pom has zero store deps).
  The engine's own tests use the temp-file mock store (below); the same placement rule
  applies verbatim to the Rust repo (a separate crate imported by
  examples/minigraph-playground).
- **Temp-file mock store (engine test fixture + simplest contract reference):** a
  @PreLoad test function implementing the same put/get contract with one file per cid
  under `/tmp/suspend-resume` (MsgPack bytes; mtime+ttl expiry check on read;
  delete-on-read for consume parity with GETDEL). Doubles as the contract page's
  "smallest possible store" example for developers writing their own.
- `@OptionalService("minigraph.state.redis.enabled")` gating + a `RedisConfig`-style
  record (`redis.host/port/password/ssl/database/timeout.ms`, `${ENV_VAR:default}`) — the
  sync-over-async pattern (SyncOverAsyncAutoStart.java:44-46, RedisConfig.java:50-66).
- Lettuce with byte[] value codec; SETEX on put, GETDEL (or GET per D1) on get.
- Tests: embedded-redis (`com.github.codemonstur:embedded-redis:1.4.3`, arm64-safe, port
  16379 — RedisTestBase precedent), asserting TTL applied + consume semantics.
- `helpers/redis-standalone` already exists for live demos.

## 4. Decisions for the maintainer (with recommendations)

- **D1 — Consume-on-retrieve.** Recommend: reference Redis store uses atomic `GETDEL` —
  a resume consumes the record, so a duplicate resume (double manager click, retried
  message) cannot double-execute the continuation; a re-suspension later re-persists under
  the same cid. Trade-off stated in docs: a crash after consume but before completion
  loses the workflow (mitigation: the next suspension point re-persists; workflows needing
  stronger guarantees use a custom store with keep-until-ack semantics — the contract
  permits either).
- **D2 — Persisted envelope.** Concept says model + node. Recommend **also** persisting
  the `nodeSeen`/`skillRun` snapshots: without them, any `graph.join` whose predecessors
  completed *before* suspension can never satisfy its barrier after resume
  (GraphJoin counts predecessors via exactly those maps). Cheap (two string→boolean maps),
  engine-internal, invisible to the store. Regardless: `<node>.result` scratch does NOT
  survive suspension — **"model is the workflow's durable memory"** becomes the loudly
  documented design rule (map anything needed downstream into `model.*` before a
  suspension point).
- **D3 — Resume not-found.** Recommend: default `next` (fresh start falls out naturally);
  optional `missing=<node-alias>` jump for workflows where an absent record is an error
  (TTL-expired approval → jump to an "expired" node that stages a 410-style response).
  Engine can't distinguish "fresh" from "expired" — the designer declares it.
- **D4 — Suspended-run response.** Recommend: `graph.suspend` stages
  `{"type":"suspended","cid":…}` into `output.body` when empty (else the caller gets an
  empty 200 — verified). Designer-staged `output.*` always wins.
- **D5 — Concurrency constraint.** Recommend: **document** that a suspension point must be
  the sole active branch (no suspension between a fan-out and its join; suspend after the
  join instead) + a best-effort runtime WARN when the executor can see other unfinished
  seen-but-not-run nodes at suspension time. Static analysis of reachability is not worth
  the complexity for v1; in-flight branches are structurally unpersistable (they live as
  pending bus events).
- **D6 — Playground/dry-run.** Recommend: skills execute for real in the traveler (that's
  the traveler's existing semantics), so suspend/resume is fully testable in the
  Playground: set `model.cid` via the instantiate command's initial data mapping (already
  supports constant→model), run to suspension, run again to resume. `graph.suspend`
  errors clearly when `model.cid` is absent.
- **D7 — Rust lock-step (RATIFIED with placement refinement).** Graph JSON is
  engine-portable by commitment (Rust design K3: Java fixtures run verbatim), so a
  suspend-bearing tutorial graph makes this a cross-engine feature by construction. The
  Rust side is structurally ready (jump primitive exists, free-form types,
  executor+traveler pair). **Eric's ruling: the Redis store is its own crate, imported by
  `examples/minigraph-playground` — never bundled into the knowledge-graph engine crate;
  the engine's tests use the `/tmp/suspend-resume` temp-file mock store. Identical
  placement rule in both repos.** Java reference first, Rust arc immediately after
  (before any release that ships suspend-bearing tutorials); Redis crate choice
  (`redis-rs`/`fred`) is the Rust session's call. Note: its design record lists "session
  persistence across restarts" as out-of-scope — that line gets superseded by this arc.
- **D8 — cid security posture (documentation).** The business cid is a resume capability —
  whoever presents it continues the workflow. Docs state: resume-bearing endpoints should
  carry rest.yaml `authentication`; cids must be non-guessable (the engine's generated
  cids are UUIDs; edge-supplied cids are the caller's responsibility); the store may
  additionally scope keys. Also documented: business cid does NOT propagate across
  `graph.extension` today (GraphExtension.java:110 — fresh uuid, no correlation_id
  header), so resumable workflows are top-level graphs; extension-hop cid propagation is a
  possible follow-up, not part of this feature.

## 5. Implementation plan (phased; each phase independently green)

**P1 — Engine core** (`system/minigraph-playground-engine`, ~4 files + tests)
`from` provenance threading; suspend-link classification in walkNext; `suspend=true`
handling; `resume:<alias>` directive in nextOrJump (+ seen/run marking); RESERVED_PARAMETERS
additions; `graph.suspend`/`graph.resume` skills (GraphTask-derived; shared base or
composition — decide in code review); default response staging; loud no-cid error.
Tests: unit suite with the temp-file mock store (`/tmp/suspend-resume`, R4 — no Redis
anywhere in the engine) — suspend persists expected envelope (inspectable file per cid);
resume merges + jumps without re-execution; fresh-start pass-through; `missing=` jump;
join-after-resume (D2); alias⇔skill mismatch rejection; math/js suspend rejection;
multi-suspension (suspend→resume→suspend→resume) chain across separate runs.
Traveler: mirror every executor change (the two walkers stay semantically identical).

**P2 — CompileGraph + Playground surface**
§3.6 compile checks; Playground grammar already accepts arbitrary types/properties
(free-form — verified), so the surface work is `describe`/`show` output labeling and the
UI type-color note; `inspect state machine` unchanged (dev endpoint already dumps model).

**P3 — Reference Redis store** (`extensions/minigraph-state-redis`)
Module per §3.7; contract page `docs/guides/knowledge-graph/state-store-contract.md`
(persist/retrieve wire contract, consume semantics, TTL responsibility, serialization
caveats, security posture); embedded-redis test suite incl. TTL assert + GETDEL consume +
absent-key behavior.

**P4 — End-to-end + docs + governance**
Tutorial graph `tutorial-approval-workflow` (suspensible task → suspend → resume → join →
end) + FlowTest-style e2e: POST /api/graph/{id} → 200 `{type:suspended,cid}` → assert
Redis record + TTL → POST again with same X-Correlation-Id → assert continuation without
re-execution → completion body. Docs: knowledge-graph guide chapter (why-before-how:
workflow suspension story), skills-reference entries, reserved-names additions
(properties + skill routes), configuration-reference (module keys), concept-doc
corrections (§2), CHANGELOG. **Propose ADR** in `docs/arch-decisions/ADR.md` (durable
decision: suspension model — short-runs + external state, skill-encapsulated persistence,
skill-defines-behavior) — human-gated per protocol.

**P5 — Rust lock-step arc** (separate, delegated to the Rust session)
Mirror P1/P2 in `crates/knowledge-graph` (executor.rs + traveler.rs dual maintenance,
model.rs snapshot/rehydrate, compiler.rs checks, commands.rs surface); store-crate
decision (D7); shared tutorial fixture verbatim; interop check = the same graph JSON
suspends/resumes identically on both engines (normalized-output diff, the parity method).

Estimated Java-side scope: P1 ≈ the largest single piece (executor + two skills + tests);
P2-P3 moderate; P4 mostly writing. No changes to platform-core or event-script-engine are
required (the graph copy of `model` is already detached; reserved-key protection on
restore is by-construction since the blob excludes them).

## 6. VBDI note

This feature directly serves `vision-mercury-composable` ("workflow operation" for the
Active Knowledge Graph — business processes with human checkpoints as graph models). It
warrants a `(blueprint)` Open Thread — **proposed, human-gated**: e.g.
`- [ ] (blueprint) Workflow suspension for the Active Knowledge Graph — human-in-the-loop
checkpoints (approval, intervention, inbox) as first-class graph vocabulary → serves:
vision-mercury-composable`.
