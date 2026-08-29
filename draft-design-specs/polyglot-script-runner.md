# Polyglot function execution — design investigation & implementation plan

> Status: RATIFIED 2026-08-22 — D0–D8 accepted by Eric (D7 resolved: non-blocking async
> design and libs). Two in-flight scope refinements ratified the same day:
> (1) wrappers include the **minimalist utilities** — Configuration Management, Logging,
> Telemetry — in engine-consistent style, so developers don't invent different ways to do
> foundational things; (2) configuration follows the engines' **`resources/` location
> convention** and the **`-Dkey=value` runtime override syntax** (Java JVM / Rust port
> parity).
> Executed same day: both wrapper repos (Accenture/mercury-python, Accenture/mercury-nodejs
> — D2 answered by repurposing them) rebooted on `feature/polyglot-event-over-http`;
> python 30/30 and node 31/31 tests green incl. the shared golden envelope vectors;
> cross-wrapper interop proven both directions; composable-example's declarative flow
> executed the python function unchanged. Remaining: P2 graph.task guard (both engines),
> P4 docs/demo/interop-report + ADR-0016, P5 publishing.
> This file is gitignored (draft-design-specs/); the durable record lives in memory/ and
> the coming ADR.

---

## 1. Intent

Polyglot development lets developers use the best language per use case. Python and
Node.js excel at rapid development but lack the composable foundation, and porting the
full foundation to them would destroy exactly the lightness that makes them good for
prototyping. Goal: python/node functions participate in Event Script flows and MiniGraph
tasks — input data mapping delivers the request, output data mapping returns the result
or error.

**Vision trace:** serves `vision-mercury-composable` ("Event Script + custom skills remain
the escape hatch"; polyglot functions widen who can contribute behavior). Proposed
Blueprint entry: `(blueprint) polyglot function execution` — human-gated (D0).

## 2. The design pivot (Eric, mid-investigation)

The v1 concept was a composable function that spawns python/node as child processes over
stdin/stdout. Eric redirected with two insights, both confirmed by the code sweep:

1. **Non-blocking:** the engine's HTTP client path (async.http.request machinery /
   Event-over-HTTP relay) is interceptor-based on the event loop — an in-flight polyglot
   call holds **zero threads** in the JVM. The stdio design needed
   `@KernelThreadRunner` + a kernel thread per in-flight script (pipe I/O and
   `Process.waitFor` pin virtual-thread carriers on JDK 21 — the BdbElasticStore lesson).
2. **No subprocess, no stability surface:** if the polyglot code runs as its own
   long-lived service, the JVM never parents interpreters — no zombie reaping, no
   process-tree kills, no pipe-deadlock engineering, no Sonar command-injection hotspots.
   A polyglot peer fails as an HTTP peer, handled by the timeout/retry/exception
   plumbing flows and graphs already have.

**Direction: wrap the existing "Event over HTTP" protocol for python and node.js.**

## 3. What already exists (evidence, 2026-08-22 sweep)

| # | Capability | Evidence | Consequence |
|---|-----------|----------|-------------|
| E1 | Event API endpoint | `event.api.service` built-in; `POST /api/event` auto-merged into rest.yaml as a default system endpoint | Callee side of the protocol is free on every engine app |
| E2 | **Declarative target maps** | `yaml.event.over.http` → event-over-http.yaml (`route` → `target` URL + optional per-route security headers); resolved inside EventEmitter at all three dispatch paths; `x-event-api` recursion guard; config-substituted URLs (`${peer.host}`) | A flow/graph/function addresses `my.python.function` **as if local**; zero caller code |
| E3 | Zero-code flow demo SHIPPED | composable-example → lambda-example: flow task `process: 'hello.declarative'` where the route exists only on the peer | Event Script reach is proven, not theoretical |
| E4 | Language-neutral wire format, documented to implement from | docs/guides/event-envelope-wire-format.md — every field (id, to, cid, trace_id/trace_path/span_id, status, headers, body, exec_time, tags, annotations, stack, obj_type, exception), encoding rules, format detection; MsgPack "standard" format default | A wrapper can be written from this one page — its stated design goal |
| E5 | Cross-language interop already proven engine-to-engine | docs/test-reports/event-over-http-interop.md (Java↔Rust); Rust platform-core carries post_office.rs / event_api.rs / app_starter.rs support | The protocol the wrappers ride is versioned, tested, and maintained by BOTH first-class engines — unlike the old bespoke language-pack websocket protocol |
| E6 | Trace continuity | trace_id/trace_path/span_id ride IN the envelope; `/api/event` is a visible traced span (v4.10.1) | Cross-language spans + log correlation for free if the wrapper echoes trace fields |
| E7 | Security | Per-route `headers:` (e.g. Authorization) caller-side; auth service attachable to `/api/event` callee-side; target functions must be PUBLIC (`isPrivate=false`) else 403 | Enterprise posture exists; wrappers mirror the public/private concept |
| E8 | Timeout semantics | Caller RPC timeout bounds the wait; callee's `/api/event` rest.yaml timeout (default 60s) bounds execution — same "two deadlines" shape as the async.http.request x-ttl lesson | Document the interplay; long-running python needs the callee entry raised |

**The ONE engine-side gap (G1):** `graph.task` guards route existence
(`GraphTask.java:74 po.exists(route)`), and `EventEmitter.routeExists()` checks
origin + local registry + cloud routes — **not** `eventHttpTargets`. So a graph task
naming a declaratively-mapped foreign route is rejected today ("task 'x' does not
exist"). The Rust twin has the identical guard (knowledge-graph skills.rs:362).
Flows are unaffected (no pre-check — that is why E3 works).

Fix options:
- **Surgical (recommended):** graph.task accepts a route that exists locally, in the
  cloud, OR in the event-over-http map (`EventEmitter.getEventHttpTarget(route) != null`
  is already `public`; add a PostOffice passthrough). Java + Rust lock-step, pinned by a
  foreign-route graph e2e against a stub peer.
- Global: teach `routeExists()` about the map. Rejected by default — `po.exists()` has
  many callers (actuators, optional-dependency probes); changing its meaning globally has
  blast radius the surgical fix avoids. (Note: `graph.suspend`'s store-task guard keeps
  the local-only check — state stores over HTTP is not this feature.)

## 4. Option space

- **A. stdio subprocess per invocation** (v1 concept — full design preserved in git
  history of this draft). Unique wins: single-artifact deployment (script rides in the
  app jar; no second service, port, or supervisor) and no network/auth surface. Costs:
  the JVM owns process lifecycle (zombies, tree-kills, pipe deadlocks — real field-bug
  surface), kernel-thread-per-execution on JDK 21, spawn latency per call, Sonar
  command-injection hotspots, a new framework module on both engines. **Shelved — not
  built now; revisit only if the field demands embedded single-artifact scripting.**
- **B. Persistent worker processes over framed stdio** — a mini language server; state
  leakage; protocol + lifecycle burden. Rejected.
- **C. Full composable SDK per language** — rejected by the stated rationale (weight).
- **D. Embedded interpreters in-JVM (GraalPy/GraalJS)** — heavyweight, no Rust analog,
  and the in-runtime code-execution pattern the graph.js phase-out retires. Rejected.
- **E. Pre-forked warm pool** — a performance lever for A only; moot while A is shelved.
- **F. WASM** — real sandbox, but python/node→WASM toolchains defeat "plain python for
  rapid prototyping". Rejected for now.
- **G. Event-over-HTTP language wrappers (Eric's direction) — RECOMMENDED.** Thin
  python/node packages implement the already-documented envelope + `/api/event` host;
  engines need (almost) nothing.
- **H. No framework artifact, document a DIY pattern** — the codec/host/registry is
  exactly what should be written once, correctly. Rejected.

### A vs G, honestly

| Axis | A (stdio subprocess) | G (Event-over-HTTP wrapper) |
|---|---|---|
| Engine change | New module both engines | **Zero** except the graph.task guard (G1) |
| JVM stability surface | Process lifecycle owned by the JVM | **None** — peer fails as an HTTP peer |
| JVM threading | Kernel thread per in-flight script | **Zero threads** (event-loop interceptor path) |
| Interpreter startup | Per call (30–100ms+, imports repeated) | Once — heavy libs (pandas, ML) load one time |
| Envelope semantics / trace | Bolted on via env vars | **Native** — trace/cid/status ride the envelope |
| Bidirectional (python calls engine functions) | No | Yes — the wrapper client is the same POST |
| Deployment | Single artifact (script in the jar) | Second service to run (k8s pod / dev terminal) |
| Dev loop | Drop a .py in resources | `pip install` + one-liner dev runner (see 5.4) |
| Protocol maintenance | New stdio contract to keep parity on | Rides the Java↔Rust-tested wire format |

G wins on every axis except single-artifact deployment. That niche is real but narrow —
and it is exactly where the old rationale ("don't own process stability") bites hardest.

## 5. Recommended design (G)

### 5.1 Architecture principle — the scope fence

**Polyglot services contribute FUNCTIONS; orchestration stays in the engines.** The
wrapper is deliberately NOT a composable foundation: no event bus, no flows, no graphs,
no local orchestration. It is four small parts per language:

1. **Envelope codec** — the standard MsgPack wire format (E4 is the contract).
2. **HTTP host** — `POST /api/event`: decode → dispatch → encode reply (or 202 for
   drop-n-forget). Honors `x-event-format: compact` reply-in-kind like the engines.
3. **Route registry + handler API** — mirrors the Mercury vocabulary so knowledge
   transfers across languages (see 5.2).
4. **Thin client (optional but recommended)** — `request(...)` to call engine functions
   (or peer polyglot services) over the same protocol: bidirectional polyglot.

Plus a **dev runner CLI** for the prototyping loop. That's the whole package — a few
hundred lines per language, versioned by protocol compatibility, releasable on its own
cadence (pip/npm), never coupled to engine releases.

This keeps the architectural invariants intact: functions stay decoupled (route names +
envelopes — `functions-decoupled-routes` extends across languages verbatim), and the
same caution as `kafka-mesh-opt-in` applies in docs: use polyglot services where another
language genuinely earns its place, not to scatter one app across processes.

### 5.2 Handler API sketch (vocabulary mirror)

```python
# python — mercury wrapper package (final name per D2)
from mercury import preload, AppException, platform

@preload(route="my.python.function", instances=10)   # instances = concurrency limit
def handle_event(headers: dict, body):               # TypedLambdaFunction's (headers, body)
    if "text" not in body:
        raise AppException(400, "missing 'text'")
    return {"result": body["text"].upper()}          # return value -> envelope body

platform.run(port=8086)                              # or: mercury serve app.py --port 8086
```

```javascript
// node — same shape
const { preload, AppException, platform } = require('mercury');
preload('my.node.function', { instances: 10 }, async (headers, body) => {
  return { result: body.text.toUpperCase() };
});
platform.run({ port: 8087 });
```

Semantics mapping (all existing engine behavior, documented per language):
- **Addressing:** caller-side event-over-http.yaml maps the route to the service URL
  (config-substituted per environment). Flows/graphs/functions call the route as local.
- **Errors:** `AppException(status, message)` → envelope status/error → the engine's
  normal error path (flow exception handler; graph `error.source/code/message/stack`).
- **Trace:** wrapper reads trace_id/trace_path from the envelope, exposes them to the
  handler (context var), stamps its logs, echoes them on replies and onward calls —
  cross-language spans and one log aggregation (`conv-telemetry-presentation-parity`
  spirit).
- **Timeouts:** caller RPC timeout + callee endpoint timeout (E8); document raising the
  wrapper's per-route execution deadline for long python work.
- **Public/private:** registry mirrors `isPrivate` (default public — these services
  exist to be called; private = internal helper routes).
- **Serialization:** MsgPack standard format; document the numeric gotchas
  (`conv-serialization-gotchas` analog — int/long/float mapping rules are in the wire
  format's encoding-rules section).

### 5.3 What changes in the engine repos

1. **graph.task foreign-route acceptance (G1)** — Java + Rust lock-step, the only code
   change. Pins: graph e2e calling a mapped route against a stub peer; rejection message
   unchanged for genuinely unknown routes.
2. **Docs:** new guide "Polyglot functions" (this repo + Rust repo) — when to use,
   wrapper contract, declarative map recipe, auth, timeouts, trace, deployment patterns
   (k8s DNS, dev loop); cross-links from event-over-http.md and the AI guides. The wire
   format page is already the implementable contract — link, don't duplicate.
3. **Demo:** examples-level — a python service (a few lines) + a flow task + a graph.task
   node calling it; doubles as the manual regression procedure (kafka-demo README style).
4. **Interop report:** extend the event-over-http-interop procedure with Java↔python /
   Java↔node columns (reference-signature procedure exists).

### 5.4 The dev loop (protecting the rapid-prototyping goal)

The honest cost of G vs A is the second process. Mitigations, in order of value:
`pip install mercury-…` + `mercury serve app.py --port 8086` one-liner; a
`polyglot-demo` example with docker-compose and bare-terminal variants; config
substitution so the same flow YAML runs dev/prod. (helpers/-style embedding of a python
runtime is explicitly NOT planned — that would re-enter the subprocess business.)

## 6. Implementation plan

- **P0 — ratify** D0–D8; freeze wrapper package names.
- **P1 — python wrapper (reference implementation).** Codec from the wire-format page,
  host, registry/decorator, AppException, trace plumbing, thin client, dev CLI, unit
  tests + a live interop gate against a running Java engine app (and Rust — same
  procedure). Home per D2.
- **P2 — graph.task guard (G1), Java + Rust lock-step**, with pins both engines.
- **P3 — node wrapper**, same contract, interop-gated. (Ordering per D3; note this can
  BE the "fresh node.js re-port" the docs promise — light by design — D3.)
- **P4 — docs + demo + interop-report extension** on both engine repos; CHANGELOG;
  propose **ADR-0016** (polyglot functions over Event over HTTP) — human-gated.
- **P5 — publishing** (PyPI/npm ownership, cadence, protocol-compat versioning) — Eric
  gates; wrappers declare "implements standard wire format" compatibility, not engine
  versions.

## 7. Risks

- **The language-pack ghost** (old Mercury python/node packs rotted): mitigated —
  wrappers ride a protocol two first-class engines already keep honest (E5), scope is
  fenced tiny, and the interop gate runs per wrapper release. Ownership/cadence is the
  real question → D6.
- **Long-running scripts vs 60s endpoint default** — documentation + per-route config.
- **New ecosystems** (PyPI/npm publishing, supply-chain posture, minimal dependencies —
  msgpack + one HTTP lib) → D7.
- **Scope creep toward a full SDK** — the fence in 5.1 is the defense; anything beyond
  the four parts needs its own ruling.

## 8. Decisions requested (Eric)

- **D0**: Ratify the Blueprint entry `(blueprint) polyglot function execution — Event
  over HTTP wrappers` → serves vision-mercury-composable.
- **D1**: Confirm Option G; option A (stdio subprocess) shelved, not built (its niche —
  single-artifact embedded scripting — revisited only on field demand).
- **D2**: Wrapper homes + names — new repos (e.g. Accenture/mercury-python,
  Accenture/mercury-nodejs) vs incubate in-repo first; package names (pip/npm).
- **D3**: Sequencing — python first (reference), node second? And: is the node wrapper
  the sanctioned "fresh node.js re-port" answer?
- **D4**: Ratify the scope fence (5.1): codec + host + registry + thin client + dev CLI,
  nothing more.
- **D5**: Approve the surgical graph.task guard change (G1) on both engines (vs leaving
  graphs without polyglot reach or changing global exists() semantics).
- **D6**: Publishing ownership + release gate (interop report green per wrapper release).
- **D7**: Wrapper HTTP stacks (python: aiohttp vs ASGI-neutral; node: stdlib http vs
  fastify) — can defer to P1 implementation review.
- **D8**: API vocabulary mirror (preload/route/handle_event/AppException/instances) —
  recommend yes for cross-language familiarity.
