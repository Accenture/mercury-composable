# Streaming return route — cross-pod progressive rendering — design spec

**Status:** DESIGN RATIFIED, **E1 IMPLEMENTED** — drafted 2026-09-12 from Eric's direction; the
Q-series was answered by Eric the same day (decisions D1–D6, §8), then refined once more after the
driving use cases were articulated: **D7 removes the sequence number entirely — a Redis
List per cid replaces the per-seq keys** (supersedes D6's mechanism, keeps its
no-MAXLEN ruling) — and **D8 unifies the module: the one-shot path adopts the same list
mechanism**, so one message and a list of messages ride one store (`response:{cid}`
retires; `deliver` becomes a terminal post). E1 (coordinator + responder primitives with the
D8 acceptance gate, §9) landed the same day in `extensions/sync-over-async` (PR #369), and
**E2 followed** — the facade half (`StreamBridge`/`EventStreamSink`, §4.5) plus the
single-JVM end-to-end proof of both driving use cases behind real `stream: true` endpoints,
broker-free — and **E3 completed the dry-run**: two JVMs against `redis-standalone`, the
actual gap scenario with chaos checks, all green
([test report](../docs/test-reports/streaming-return-route-cross-pod.md)) — and **E4
delivered the blueprint payoff**: real Gemini tokens produced on one pod and rendered on
another through the rendezvous (test report, scenario 6). **The E-series is COMPLETE.**
The last open flag — the single final drain at edge idle expiry (§4.5, D4's nuance) — was
**CONFIRMED by Eric on 2026-09-12** after demonstrations at unit, single-JVM, and cross-pod
level. The design is fully ratified with no open flags.
**Blueprint:** serves `bp-agent-orchestration` (Q8 second half — graph-run streaming)
→ serves `vision-mercury-composable`.
**Repo scope:** `extensions/sync-over-async` only. Java-only like the extension itself
(no Rust lock-step; this is not an Event Script surface). No engine change.
**Related:** [http-response-streaming.md](http-response-streaming.md) and
[async-http-client-sse-streaming.md](async-http-client-sse-streaming.md) (the shipped
point-to-point feature), the [HTTP Response Streaming guide](../docs/guides/http-streaming.md),
the [Sync-over-Async guide](../docs/guides/sync-over-async.md).

---

## 1. Problem

Progressive rendering is shipped on both edges: the server side renders a multi-shot
`x-event-stream` sequence out a `stream: true` endpoint (SSE or chunked), and the client
side (`async.http.request`) consumes an SSE upstream progressively. Both ride the
caller's `reply_to` — a dedicated reply lane that is a route **on the pod holding the
HTTP connection**.

That makes the feature point-to-point. In a horizontally scaled deployment, the pod that
*produces* progressive events for a transaction is generally not the pod holding the
user's connection — and it has no route to another pod's reply lane. Discovering "which
pod holds the connection" is exactly the service-discovery problem the Kafka service
mesh solves, and sync-over-async exists as the deliberately lighter alternative to the
mesh. Event-over-HTTP cannot bridge it either: `/api/event` serves local routes only
(ratified 2026-08-30), and the sender would still need per-pod addressing for
dynamically scheduled pods.

Sync-over-async already solved this rendezvous for the **one-shot** case: the
originating pod registers a return route in Redis (`request:{cid}`), and any pod can
look it up and wake the right subscriber. This spec generalizes that return route from
*one response* to *a sequence of segments with a terminal signal*. By decision D3, the
segments ride **Redis only** — the producing server posts them straight into the return
route, so the streaming path has no broker dependency at all (the request leg reaches
the backend however the application likes). And by decision D8 the generalization folds
back onto the original: a one-shot response is the **degenerate stream** — a queue whose
first entry is terminal — so the module keeps one storage mechanism, not two.

## 2. Driving use cases (Eric, 2026-09-12)

**Event notification.** A UI application opens an SSE request and awaits
`text/event-stream` messages, then makes POST requests to various backend services.
The backends respond asynchronously and their messages arrive at different times —
**several producers post to the same cid** (the SSE session), and **message ordering
does not matter**. Either side may end the channel: the UI closes the SSE request, or a
backend posts the terminal event. The channel is legitimately quiet for long stretches —
idleness is normal here, not failure.

**Chat with an AI agent.** A UI application makes a POST request with progressive
rendering on. The request reaches a backend service talking point-to-point with an AI
agent that renders blocks of tokens and ends with an end-of-transmission signal.
**Strict ordering is required** — and the events come from a **single source** posting
to Redis sequentially, which is what makes ordering free (§5, contract 2).

These two shaped D7: with several uncoordinated producers, per-seq keys would collide
(or need a distributed counter), and with a single sequential producer, Redis's own
per-connection command ordering already preserves generation order — so the sequence
number is unnecessary in both cases, and the storage shape that serves both is a list.

## 3. Current state — the pieces already shipped

| Piece | Where | What it gives this design |
|-------|-------|---------------------------|
| `x-event-stream: data \| eof \| exception` contract | platform-core + REST automation | The last mile out the HTTP edge: SSE/chunked framing, ordered reply lanes (pool of 500, deterministic 503 back-pressure), idle timeout with in-band 408, keep-alive pings, slow-client buffer |
| `EventStreamWriter` (`first`/`write`/`close`/`fail`) | platform-core | The producer API the UI-pod forwarder uses verbatim — including `first(status, contentType, ttlSeconds)` to widen the idle allowance for a deliberately quiet notification channel |
| One-shot return route (`request:{cid}`, `response:{cid}`, per-pod channel `svc-return:{origin}`) | sync-over-async `ReturnRouteCoordinator` / `ReturnRouteStore` | The rendezvous mechanism: route registry as pod discovery, store-first + wake-up invariant, final-read recovery, bounded `PendingRequests` |
| Self-contained `cid` contract (`SyncRuntime.CID`) | sync-over-async (PR #364) | Transport-neutral correlation — and the streaming path keeps the module's footprint at platform-core + Lettuce, nothing more |

## 4. Design (ratified shape)

### 4.1 Shape in one sentence

Each segment is **stored first** by appending to a short-lived Redis List keyed by cid,
the **wake-up stays a bare cid** on the originating pod's existing return channel, and
the UI pod **drains the list destructively** — forwarding each entry into the shipped
`x-event-stream` edge — until a terminal entry.

Redis Pub/Sub remains a wake-up signal only (D1). It is fire-and-forget; in the
one-shot pattern a dropped notification is recoverable because the final read finds the
single response, but a payload dropped mid-stream would be a silently missing segment
with nothing left to re-read. Storing every segment keeps it readable until it is
drained or its TTL expires.

### 4.2 Redis keys

| Key | Type | Content | Lifetime |
|-----|------|---------|----------|
| `request:{cid}` | string (unchanged) | the originating pod's return channel | streaming rendezvous: `sync.stream.ttl.seconds` (session-scale — an SSE notification channel outlives a one-shot's route window); eagerly deleted on close |
| `queue:{cid}` | Redis List (new) | one entry per segment: compact JSON `{"type":"data\|eof\|exception","name":"<optional SSE event name>","body":...}` | `EXPIRE sync.stream.ttl.seconds`, refreshed on every `RPUSH`; a fully drained list ceases to exist on its own |
| `response:{cid}` | string | **retired (D8)** — the one-shot path posts its single terminal entry to `queue:{cid}` instead | replaced; the one-shot queue's TTL comes from the existing `sync.response.ttl.seconds` |

There is **no sequence number** (D7). Ordering, where required, comes from *how* the
producer posts (§5, contract 2), and reads are destructive pops, so the consumer keeps
no index. The `eof` (or `exception`) entry is simply the last entry the rendezvous will
deliver — "the end signal is also an event", stored like any other segment — and **any
producer may post it** (use case 1: a backend service may close the channel).

On MAXLEN: D6's no-cap ruling stands. Unlike the rejected append-only Stream, the list
is drained destructively, so it is near-empty in normal operation; it grows only while
the UI side is stalled, and that window is bounded by the TTL, not by a trim policy.

### 4.3 Coordinator additions (`ReturnRouteCoordinator`)

The existing class gains a streaming sibling for each one-shot member — and by D8 the
one-shot members keep their signatures while re-platforming onto the same queue
underneath (§4.7).

| One-shot (today) | Streaming (new) | Notes |
|------------------|-----------------|-------|
| `begin(cid)` → future in `PendingRequests` | `beginStream(cid, sink)` → entry in `PendingStreams` | `saveRoute` with the streaming TTL; sink = the forwarder callback |
| `deliver(cid, payload)` — SETEX then publish | responder posts each segment — `RPUSH queue:{cid}` + `EXPIRE`, then publish cid (§4.4) | store-first invariant preserved; orphan (route gone) returns `false` |
| `onResponseSignal` — GET + complete future | drain: `LPOP queue:{cid}` until empty, forwarding each entry; a terminal type completes the stream | destructive pop = no bookkeeping; a duplicate wake-up pops nothing; a dropped wake-up is healed by the next one popping everything queued |
| cleanup — DEL both keys | cleanup at the terminal entry — DEL route (+ any queue remnant) | eager cleanup; TTLs remain the crash safety net |

**Drains are serialized per cid.** Two wake-ups may arrive concurrently (several
producers, use case 1); a per-stream in-progress flag with a re-check ensures a single
drain loop forwards at a time, so forward order equals list order — which is what makes
the chat case's ordering hold end-to-end (producer connection order → list order →
serialized drain → the edge's ordered reply lane → SSE). One channel serves both
patterns: the signal handler checks `PendingStreams` first, then `PendingRequests`.
`PendingStreams` mirrors `PendingRequests` (bounded via `sync.max.pending.streams`,
atomic slot reservation, idempotent close). Drain execution stays off the Lettuce event
loop (the existing virtual-thread `signalWorkers` hand-off) because the pops and the
forward are blocking calls.

A race note: a producer's `RPUSH` may land just as another producer's terminal entry
completes the cleanup. Its own route lookup then finds nothing (`post` returns `false`,
it stops), and the recreated `queue:{cid}` remnant simply expires on its TTL.

### 4.4 Responder side — the segment producer API

Per D3 the producing server posts segments **directly to Redis** — no broker leg, no
consumer task. The extension ships a lightweight producer API (working name
`StreamResponder`) that a backend application constructs from the same discrete
`redis.*` parameters (`RedisConfig`), without the coordinator: the responder side needs
no subscriber, no return channel, and no `sync.over.async.enabled` switch — only the key
contract above.

```java
var responder = new StreamResponder(redisConfig);
responder.post(cid, "data", null, "Hello");        // RPUSH queue:{cid} + EXPIRE, then PUBLISH cid
responder.post(cid, "data", "tokens", "{...}");
boolean live = responder.post(cid, "eof", null, metadata);
// post returns false when the route is gone (orphan) - stop producing for that cid
```

The producer's whole contract is *post in the order you mean* — no sequence header to
stamp, no counter to manage, no per-cid state to hold (D7): the event producer stays as
straightforward as a plain send. Each `post` is store-first: append the segment, refresh
the queue TTL, GET the route, PUBLISH the cid. A `false` return means the rendezvous is
over — the UI disconnected,
timed out, crashed, or **another producer already closed the channel** — and the
producer should stop work for that cid. The producer is typically a unit of work (an
LLM token loop, a service posting a notification), which per the code/config boundary is
an in-function concern; a flow-task wrapper can be added later if a declarative use case
appears.

### 4.5 UI-pod facade — interceptor by design (D2)

The typical consumer is a UI application connected to a REST endpoint with SSE support.
The facade is an `@EventInterceptor` function addressed directly by a `stream: true`
endpoint — the same shape as every shipped streaming producer; there is no Event Script
composition on this path (D2). The facade:

- calls `beginStream(cid, sink)` where the sink wraps an `EventStreamWriter` bound to
  the request (`data` → `write`, `eof` → `close(metadata)`, `exception` → `fail`);
- for the chat case, initiates the backend work (whatever the application's request leg
  is); for the notification case, simply hands the session's cid back to the UI, which
  quotes it in its subsequent POSTs so backends know where to post;
- for a notification channel, widens the edge idle allowance with
  `first(status, contentType, ttlSeconds)` — long quiet stretches are normal there,
  and the channel ends by explicit close (either side), not by idling out;
- lets the edge's own machinery do the rest: per-lane ordering, disconnect handling,
  back-pressure.

On client disconnect or idle expiry the facade closes the `PendingStreams` entry and
deletes the route, so nothing leaks; the route's disappearance is what tells every
producer to stop. At idle expiry the facade performs **one final drain** before failing
in-band — the streaming analogue of the one-shot "final read before timeout"
cornerstone, so a dropped *final* notification still completes the render. (A single
last-chance read, not the periodic re-drain rejected in D4 — CONFIRMED by Eric 2026-09-12
after the unit, single-JVM, and cross-pod demonstrations.)

*Implemented in E2 as `StreamBridge` (the generic facade half) + `EventStreamSink` (the
segment→writer adapter).* `StreamBridge.open(coordinator, request, cid, idleSeconds)`
commits the SSE head with `idleSeconds` as the edge idle allowance and arms a watchdog
with the same allowance — one number drives both, so the bridge's expiry and the edge's
never disagree. Every drained segment re-arms the watchdog; the terminal segment disarms
it. Expiry = the single final drain, then (only if the stream did not complete) the
in-band 408 and `closeStream`. The watchdog is also what reclaims an abandoned/
disconnected client's stream — the edge drops late writes on its own, and the idle expiry
then releases the `PendingStreams` slot and the keys. Capacity rejection surfaces as a
real HTTP 503 (the head is not yet committed), mirroring the edge's reply-lane
back-pressure. An application interceptor is one `open(...)` call plus its own request
leg; a notification facade may use the returned writer to announce the session's cid
before any producer knows it.

### 4.6 Configuration keys (proposed)

| Key | Default (proposed) | Meaning |
|-----|--------------------|---------|
| `sync.stream.ttl.seconds` | `1800` | TTL of a streaming rendezvous's route key and of `queue:{cid}` (refreshed on every post) — the crash safety net, sized for session-scale SSE channels; eager deletes do the real cleanup |
| `sync.max.pending.streams` | `1000` | per-pod ceiling on concurrently open streams (the reply-lane pool of 500 is the natural upper bound per pod) |

The one-shot path's existing keys are untouched: `sync.response.ttl.seconds` now sets
the queue TTL for a one-shot `deliver`, and `sync.route.ttl.seconds` still governs its
route.

### 4.7 One mechanism for both — the one-shot path adopts the list (D8)

A one-shot response is the degenerate stream: a queue whose first entry is terminal. So
the one-shot path re-platforms onto the same store:

- `deliver(cid, payload)` keeps its signature and becomes, internally, a post of one
  terminal entry with the one-shot's own TTL (`sync.response.ttl.seconds`);
  `response:{cid}` and its SETEX/GET path retire.
- The signal handler always drains the queue; the only difference is the consumer — a
  one-shot cid's first entry completes the pending future, a stream's entries feed the
  sink until terminal.
- The one-shot "final read before timeout" and the streaming "final drain at idle
  expiry" become literally the same operation — one recovery cornerstone, two callers.
- Duplicate delivery becomes structurally impossible (the second drain pops nothing),
  where today it relies on `complete()` being idempotent.

One behavioral adjustment makes this correct — the **early-arrival path**. Today, a
response landing between `sync.prepare` and `sync.await` completes and removes the
pending future, and `awaitResponse` recovers by re-reading `response:{cid}` (GET is
non-destructive). Under destructive pops that Redis re-read disappears, so
`PendingRequests.complete()` completes the future **in place** and removal moves to the
paths that already remove on every other exit: `awaitResponse`'s `finally`, and the
`abort(cid)` the flow's exception handler already calls on the fail-fast path — together
exhaustive, so a completed-but-unawaited entry cannot leak. The existing await-by-cid
test pins the behavior.

**No backward-compatibility issue (Eric's ruling):** the Redis key shape changes, but
sync-over-async's use case is near real-time — rendezvous keys live for seconds and
nothing persists across versions, so there is no state to migrate. At worst, a rolling
upgrade re-times a handful of in-flight cross-version requests (408), indistinguishable
from ordinary timeout behavior.

*Two implementation notes from E1.* (1) The append and its TTL refresh execute as one
atomic server-side step (a two-command Lua `RPUSH`+`EXPIRE`), so a client crash between
them cannot leave a TTL-less queue key — every key the module creates ages out — and the
hot path spends one round-trip per post instead of two. (2) Destructive pops open one
narrow race the old non-destructive `GET` did not have: a wake-up landing exactly between
the await's timeout and its final drain pops the segment and completes the future in
place, leaving the drain empty. The timeout path therefore consults the future itself
after an empty final drain — with in-place completion, the future is then the only
remaining copy, and the response is still returned rather than mis-reported as a 408.

## 5. Contracts and invariants

1. **Store-first, notify-after (D1).** A segment is appended before its wake-up is
   published; the payload never rides Pub/Sub. Inherited verbatim from the one-shot
   design.
2. **Ordering is the producer's posting discipline (D7).** A producer that requires
   ordering posts sequentially over one connection — Redis executes each connection's
   commands in arrival order, so list order equals generation order (the chat case).
   Several producers on one cid interleave at entry granularity in arbitrary order —
   permitted by design (the notification case). No sequence number exists at this
   layer; an application is free to stamp one inside its own payload.
3. **The terminal signal is data, and anyone may send it.** `eof`/`exception` are
   ordinary stored entries; the first one drained completes the rendezvous, whichever
   producer posted it, and the route's deletion stops the rest.
4. **`cid` stays the module's self-contained key.** The streaming route references only
   `SyncRuntime.CID`; how the request leg carries it remains the application's concern.
5. **No new timeout machinery (D4).** The edge's idle allowance (widened per endpoint
   where quiet is normal) plus the key TTLs cover every abandonment case; cleanup is
   eager on completion and TTL-driven otherwise; drains are serialized per cid; no
   periodic sweeper.

## 6. Failure analysis

| Failure | Behavior |
|---------|----------|
| Wake-up notification dropped | The next wake-up's drain pops everything queued; a dropped *final* notification is caught by the facade's single final drain at idle expiry (§4.5) |
| UI pod dies mid-stream | The route key is Redis state, so it survives the pod until its TTL: posts inside that window are accepted into the void (stored + published to a subscriber-less channel), and every producer's `post` returns `false` — the orphan stop — once the route expires, within `sync.stream.ttl.seconds` of the crash. The queue remnant ages out on its own TTL. (Timing observed in E3, scenario 5.) |
| Producer dies mid-stream (chat case) | No terminal entry ever arrives; the edge idle timeout fails the render in-band with 408; keys age out on TTL |
| One producer closes while others are active (notification case) | The terminal entry completes the rendezvous and deletes the route; the other producers stop on their next `post` (orphan) |
| Producer outruns a slow UI client | The edge buffers up to its 1 MB slow-client bound; undelivered entries wait in the list under its TTL (destructive drains keep it near-empty otherwise) |
| Client disconnects | The edge drops late writes as no-ops; the facade closes the stream entry and deletes the route; producers stop on their next `post` |
| Pod at stream capacity | `beginStream` rejects deterministically, mirroring `sync.max.pending.requests` (and the edge already rejects at lane exhaustion with 503) |

## 7. Non-goals

- **Not a mesh replacement.** No service discovery, no cross-pod RPC — one rendezvous
  pattern, same as the one-shot route.
- **Not an event store.** The queue is a short-lived rendezvous buffer; durable history
  of progressive output, if an application needs it, is its own concern.
- **No fan-out (D5).** One cid, one waiting pod, one render — broadcasting one
  transaction's progress to several viewers is out of scope. (Several *producers*, one
  consumer — use case 1 — is in scope; the reverse is not.)
- **Not a change to the `x-stream-id` lane.** Object streams, file downloads and `Flux`
  relays are untouched; this rides the `x-event-stream` idiom only.
- **No broker on the streaming path (D3).** The return route adds no Kafka (or any
  event-system) requirement — consistent with the module's transport-neutral footprint
  (platform-core + Lettuce only).

## 8. Decisions (D-series)

D1–D6 resolve the original Q-series (Eric, 2026-09-12); D7 followed the same day from
the use-case discussion (§2).

- **D1 (from Q1) — Pub/Sub is wake-up only; payload never rides it.** Accepted as
  proposed. Store-first is the load-bearing invariant of the whole design.
- **D2 (from Q2) — No Event Script composition; the facade is an interceptor.** The
  typical use case is a UI application connecting to a REST endpoint with SSE support
  for event notification; the `stream: true` → interceptor shape (every shipped
  streaming producer's shape) is the design, not a fallback.
- **D3 (from Q3) — Kafka decoupled; Redis is the sole transport of the streaming
  return path.** The server producing a streaming response posts its events straight to
  Redis. This removes the broker leg and, with it, the partition-ordering contract and
  the at-least-once duplicate question of the first draft — fewer moving parts. The
  responder gains one requirement: Redis connectivity plus the producer API (§4.4).
- **D4 (from Q4) — No re-drain mechanism; Redis expiry serves the purpose.** TTL is
  the cleanup and the crash backstop. One retained nuance — CONFIRMED by Eric 2026-09-12: a
  *single* final drain at edge idle expiry (the one-shot "final read before timeout"
  pattern), which is a last-chance read on the failure path, not a sweeper.
- **D5 (from Q5) — No fan-out / broadcast.** Recorded as a non-goal.
- **D6 (from Q6) — No append structure to cap; MAXLEN and trim policy dissolve.**
  Originally resolved as "the sequence number is the retrieval index" with per-seq
  keys; **the mechanism half is superseded by D7** (the no-cap ruling stands — see
  §4.2 for why the list honors it).
- **D7 — No sequence number; a Redis List per cid replaces the per-seq keys (Eric,
  2026-09-12, from the use-case discussion).** The notification use case has several
  uncoordinated producers per cid — per-seq keys would collide or need a distributed
  counter — and the chat use case's single sequential producer gets ordering for free
  from Redis's per-connection command ordering. So nothing is stamped: `RPUSH` to
  `queue:{cid}`, destructive `LPOP` drains, ordering by posting discipline (§5,
  contract 2), and the channel may be closed by either side. Supersedes D6's mechanism.
- **D8 — The one-shot path adopts the list mechanism (Eric, 2026-09-12).** One store
  for one message and for a list of messages: `response:{cid}` retires, `deliver` posts
  a terminal entry, the recovery paths unify, and duplicate delivery becomes
  structurally impossible. Requires the complete-in-place adjustment in
  `PendingRequests` (§4.7). No backward-compatibility concern: the use case is near
  real-time, so rendezvous state is ephemeral and nothing persists across versions
  (Eric's ruling). The acceptance proof is the existing one-shot regression suite
  passing unchanged on the unified mechanism — the degenerate case shown to be truly
  degenerate.

## 9. Experiment plan (E-series)

- **E1 — Coordinator + responder primitives. ✅ DONE (2026-09-12).** `PendingStreams`,
  `beginStream`, the serialized drain loop, `StreamResponder.post`, cleanup + unit tests
  on the embedded Redis: in-order delivery for a sequential single producer, interleaved
  delivery from concurrent producers (order-free, per-producer subsequence order
  preserved), terminal entry from a *non-originating* producer closing the channel,
  orphan stop, missed-notification healing (suppress a publish, verify the next drain
  recovers), final drain at idle expiry, capacity rejection, duplicate wake-up
  idempotence — **plus the D8 acceptance gate: the entire existing one-shot regression
  suite (module and demo flows) passes unchanged on the unified mechanism**, with the
  early-arrival await-by-cid test proving the complete-in-place semantics. Landed in
  `extensions/sync-over-async` (`StreamResponder`, `StreamSegment`, `PendingStreams`;
  behavioral suite `StreamReturnRouteTest`, incl. a one-shot request completed by a
  stream producer's terminal post — the degenerate case shown degenerate); module suite
  73/73 green, demo flows green against the unified module.
- **E2 — Single-JVM end-to-end. ✅ DONE (2026-09-12).** Both use cases behind
  `stream: true` endpoints (no broker anywhere — the Kafka building blocks switched off
  for the run): a chat-style render (5 ordered tokens + `eof` metadata, exact order
  asserted end-to-end) and a notification channel (cid announced to the UI as the first
  SSE event, two posting services with their own connections, backend-side close, orphan
  stop for the remaining producer). Consumed progressively with the shipped SSE client
  (`async.http.request` with `Accept: text/event-stream` + reply_to), so the run
  exercises the full circle: facade interceptor → `StreamBridge` → Redis → serialized
  drain → reply lane → SSE edge → SSE consumer envelopes. Both idle-expiry endings
  demonstrated: the final drain RECOVERING a fully-lost close (every notification
  suppressed; the render still completes — D4's nuance made observable), and the in-band
  408 with rendezvous cleanup + producer orphan stop when nothing was queued. Landed in
  `extensions/sync-over-async` (`StreamBridge`, `EventStreamSink`; test app
  `ChatStreamFacade`/`NotificationStreamFacade`/`MockAiBackend` + `StreamingRestE2eTest`);
  module suite 77/77 green, demo flows 4/4. The consumer-side close is exercised through
  the idle-expiry path (a socket-level client disconnect is not scriptable through the
  in-JVM HTTP client; E3's cross-pod chaos checks cover the remaining physical cases).
- **E3 — Cross-pod dry-run. ✅ DONE (2026-09-12).** Two JVMs against `redis-standalone`:
  the UI request lands on pod A; producers run on pod B posting to Redis — the actual gap
  scenario. Five scenarios, all green on the first complete run — ordered chat tokens
  cross-pod (no sequence number anywhere), lost notification healed by the next drain,
  lost *close* recovered by the final drain at idle expiry (fired at idle + 1 ms),
  producer `kill -9` mid-stream → in-band 408, UI pod `kill -9` → producer orphan stop
  bounded by the route TTL (posts inside the window are accepted into the void — failure
  table §6 tightened accordingly). Permanent record:
  [streaming-return-route-cross-pod test report](../docs/test-reports/streaming-return-route-cross-pod.md);
  the runbook is reproducible via the demo's new `stream-ui` / `stream-producer` profiles
  (`examples/sync-over-async-demo`). No mechanism defects found.
- **E4 — Blueprint payoff. ✅ DONE (2026-09-12).** An LLM token stream through the
  bridge: pod B pulls the provider's own SSE stream (Gemini
  `streamGenerateContent?alt=sse`) through the platform's shipped SSE consumer, and a
  ~40-line bridge function forwards each relayed `x-event-stream` envelope into the
  rendezvous via `StreamResponder` - real tokens rendered in generation order on the
  other pod, terminal `done` carrying the provider's usage metadata; provider refusals
  ride the same path in-band (test report, scenario 6). Two findings: the platform-core
  HTTP client percent-encoded `:` in path segments (RFC 3986 pchar; broke Google-style
  `:verb` custom methods - fixed with a regression pin, own PR), and the producer
  contract applies to forwarders (the bridge must be a single-instance route, or relayed
  frames post out of order and a batch sequenced behind the terminal is discarded - the
  same reason the edge's reply lanes are single-instance). Graph-run attachment: this is
  the wrapper-side shape a `graph.task` node drives (E0's proven leg) - the node streams
  out-of-band while its graph edge stays request/response, zero engine change; driving it
  from inside a live graph run is the agent-orchestration thread's next experiment.

## 10. Relation to the blueprint

The agent-orchestration concept's E0 proved progressive token rendering out the engine's
SSE edge — on one pod. Its open question Q8 (second half) asks how a *graph run* streams
to a UI in a horizontally scaled deployment. This spec is that transport: the graph/LLM
wrapper posts progress events to the return route as it works, and whichever pod holds
the user's connection renders them. Governance properties are inherited, not invented —
the segments are ordinary short-lived Redis values, the rendezvous is the same registry
the one-shot pattern ships today, and the edge contract is the one already shipped and
interop-tested.
