# Streaming return route — cross-pod progressive rendering — design spec

**Status:** DESIGN RATIFIED — drafted 2026-09-12 from Eric's direction; the Q-series was
answered by Eric the same day (decisions D1–D6, §8), then refined once more after the
driving use cases were articulated: **D7 removes the sequence number entirely — a Redis
List per cid replaces the per-seq keys** (supersedes D6's mechanism, keeps its
no-MAXLEN ruling). Next gate: experiment E1.
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
the backend however the application likes).

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
| `response:{cid}` | string | one-shot only — unused on the streaming path | unchanged |

There is **no sequence number** (D7). Ordering, where required, comes from *how* the
producer posts (§5, contract 2), and reads are destructive pops, so the consumer keeps
no index. The `eof` (or `exception`) entry is simply the last entry the rendezvous will
deliver — "the end signal is also an event", stored like any other segment — and **any
producer may post it** (use case 1: a backend service may close the channel).

On MAXLEN: D6's no-cap ruling stands. Unlike the rejected append-only Stream, the list
is drained destructively, so it is near-empty in normal operation; it grows only while
the UI side is stalled, and that window is bounded by the TTL, not by a trim policy.

### 4.3 Coordinator additions (`ReturnRouteCoordinator`)

The existing class gains a streaming sibling for each one-shot member; nothing existing
changes behavior.

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
last-chance read, not the periodic re-drain rejected in D4 — flagged for Eric's
confirmation.)

### 4.6 Configuration keys (proposed)

| Key | Default (proposed) | Meaning |
|-----|--------------------|---------|
| `sync.stream.ttl.seconds` | `1800` | TTL of a streaming rendezvous's route key and of `queue:{cid}` (refreshed on every post) — the crash safety net, sized for session-scale SSE channels; eager deletes do the real cleanup |
| `sync.max.pending.streams` | `1000` | per-pod ceiling on concurrently open streams (the reply-lane pool of 500 is the natural upper bound per pod) |

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
| UI pod dies mid-stream | Route key gone with the pod (or expired); every producer's next `post` returns `false` and it stops; the queue remnant ages out on TTL |
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
  the cleanup and the crash backstop. The spec retains one nuance for confirmation: a
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

## 9. Experiment plan (E-series)

- **E1 — Coordinator + responder primitives.** `PendingStreams`, `beginStream`, the
  serialized drain loop, `StreamResponder.post`, cleanup + unit tests on the embedded
  Redis: in-order delivery for a sequential single producer, interleaved delivery from
  concurrent producers (order-free), terminal entry from a *non-originating* producer
  closing the channel, orphan stop, missed-notification healing (suppress a publish,
  verify the next drain recovers), final drain at idle expiry, capacity rejection,
  duplicate wake-up idempotence.
- **E2 — Single-JVM end-to-end.** Both use cases behind `stream: true` endpoints (no
  broker anywhere): a chat-style render (N ordered segments + `eof`, verified in exact
  order with `curl -N`) and a notification channel (several posting services, UI-side
  and backend-side close).
- **E3 — Cross-pod dry-run.** Two JVMs against `redis-standalone`: the UI request lands
  on pod A; producers run on pod B posting to Redis — the actual gap scenario. Chaos
  checks: suppress one notification, kill the producer mid-stream (idle 408), kill the
  UI pod (orphan stop on the producer).
- **E4 — Blueprint payoff.** An LLM/graph-run token stream through the bridge: the
  agent-orchestration wrapper posts token segments via the responder API while the HTTP
  edge renders on a different pod (the E0 demo, made horizontal).

## 10. Relation to the blueprint

The agent-orchestration concept's E0 proved progressive token rendering out the engine's
SSE edge — on one pod. Its open question Q8 (second half) asks how a *graph run* streams
to a UI in a horizontally scaled deployment. This spec is that transport: the graph/LLM
wrapper posts progress events to the return route as it works, and whichever pod holds
the user's connection renders them. Governance properties are inherited, not invented —
the segments are ordinary short-lived Redis values, the rendezvous is the same registry
the one-shot pattern ships today, and the edge contract is the one already shipped and
interop-tested.
