# Streaming return route — cross-pod progressive rendering — design spec

**Status:** DESIGN RATIFIED — drafted 2026-09-12 from Eric's direction; the Q-series was
answered by Eric the same day and is folded in below as decisions D1–D6 (§7). The
ratified design is **simpler than the first draft**: Redis is the sole transport of the
streaming return path (no broker leg), and segments are individually keyed values indexed
by sequence number (no Redis Stream). Next gate: experiment E1.
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

## 2. Current state — the pieces already shipped

| Piece | Where | What it gives this design |
|-------|-------|---------------------------|
| `x-event-stream: data \| eof \| exception` contract | platform-core + REST automation | The last mile out the HTTP edge: SSE/chunked framing, ordered reply lanes (pool of 500, deterministic 503 back-pressure), idle timeout with in-band 408, keep-alive pings, slow-client buffer |
| `EventStreamWriter` (`first`/`write`/`close`/`fail`) | platform-core | The producer API the UI-pod forwarder uses verbatim |
| One-shot return route (`request:{cid}`, `response:{cid}`, per-pod channel `svc-return:{origin}`) | sync-over-async `ReturnRouteCoordinator` / `ReturnRouteStore` | The rendezvous mechanism: route registry as pod discovery, store-first + wake-up invariant, final-read recovery, bounded `PendingRequests` |
| Self-contained `cid` contract (`SyncRuntime.CID`) | sync-over-async (PR #364) | Transport-neutral correlation — and the streaming path keeps the module's footprint at platform-core + Lettuce, nothing more |

## 3. Design (ratified shape)

### 3.1 Shape in one sentence

Each segment is **stored first** as its own short-lived Redis value keyed by cid and
sequence number, the **wake-up stays a bare cid** on the originating pod's existing
return channel, and the UI pod **drains by consecutive sequence** — forwarding each
segment into the shipped `x-event-stream` edge — until the terminal segment.

Redis Pub/Sub remains a wake-up signal only (D1). It is fire-and-forget; in the
one-shot pattern a dropped notification is recoverable because the final read finds the
single response, but a payload dropped mid-stream would be a silently missing segment
with nothing left to re-read. Storing every segment keeps it re-readable until the
rendezvous completes or its TTL expires.

### 3.2 Redis keys

| Key | Type | Content | Lifetime |
|-----|------|---------|----------|
| `request:{cid}` | string (unchanged) | the originating pod's return channel | `sync.route.ttl.seconds` — for a stream this must cover the whole render, not just the first response |
| `segment:{cid}:{seq}` | string (new; one per segment) | compact JSON `{"type":"data\|eof\|exception","name":"<optional SSE event name>","body":...}` | `SETEX sync.stream.ttl.seconds` per key — the crash safety net (D4) |
| `response:{cid}` | string | one-shot only — unused on the streaming path | unchanged |

The sequence number is the retrieval index (D6): the producer assigns **consecutive
integers from 1**, and the terminal `eof` (or `exception`) segment is simply the last
sequence — "the end signal is also an event", stored like any other segment so it can
never outrun the data it terminates. There is no array or stream structure in Redis, so
there is nothing to cap or trim; per-key TTL bounds every segment's lifetime.

### 3.3 Coordinator additions (`ReturnRouteCoordinator`)

The existing class gains a streaming sibling for each one-shot member; nothing existing
changes behavior.

| One-shot (today) | Streaming (new) | Notes |
|------------------|-----------------|-------|
| `begin(cid)` → future in `PendingRequests` | `beginStream(cid, sink)` → entry in `PendingStreams` | same `saveRoute`; sink = the forwarder callback |
| `deliver(cid, payload)` — SETEX then publish | responder posts each segment — SETEX `segment:{cid}:{seq}` then publish cid (§3.4) | store-first invariant preserved; orphan (route gone) returns `false` |
| `onResponseSignal` — GET + complete future | drain: `GET segment:{cid}:{nextSeq}`, forward, increment, repeat until a miss; a terminal type completes the stream | idempotent — a duplicate or late wake-up re-reads nothing; a dropped wake-up is healed by the next one |
| cleanup — DEL both keys | cleanup at the terminal segment — DEL route + `segment:{cid}:1..n` | eager cleanup; TTLs remain the crash safety net |

One channel serves both patterns: the signal handler checks `PendingStreams` first, then
`PendingRequests`. `PendingStreams` mirrors `PendingRequests` (bounded with atomic slot
reservation via `sync.max.pending.streams`, idempotent close, keyed by cid) plus one
extra piece of state per entry: `nextSeq`, which makes draining resumable, in-order by
construction, and duplicate-safe. Drain execution stays off the Lettuce event loop (the
existing virtual-thread `signalWorkers` hand-off), because the reads and the forward are
blocking calls.

### 3.4 Responder side — the segment producer API

Per D3 the producing server posts segments **directly to Redis** — no broker leg, no
consumer task. The extension ships a lightweight producer API (working name
`StreamResponder`) that a backend application constructs from the same discrete
`redis.*` parameters (`RedisConfig`), without the coordinator: the responder side needs
no subscriber, no return channel, and no `sync.over.async.enabled` switch — only the key
contract above.

```java
var responder = new StreamResponder(redisConfig, ttlSeconds);
responder.post(cid, 1, "data", null, "Hello");      // SETEX segment:{cid}:1, then PUBLISH cid
responder.post(cid, 2, "data", "tokens", "{...}");
boolean live = responder.post(cid, 3, "eof", null, metadata);
// post returns false when the route is gone (orphan) - stop producing
```

Each `post` is store-first: SETEX the segment, GET the route, PUBLISH the cid. A `false`
return means the originating pod is gone (disconnect, timeout, crash) — the producer
should stop work for that cid. The producer is typically a unit of work (an LLM token
loop, a long-running computation reporting progress), which per the code/config boundary
is an in-function concern; a flow-task wrapper can be added later if a declarative use
case appears.

### 3.5 UI-pod facade — interceptor by design (D2)

The typical consumer is a UI application connected to a REST endpoint with SSE support.
The facade is an `@EventInterceptor` function addressed directly by a `stream: true`
endpoint — the same shape as every shipped streaming producer; there is no Event Script
composition on this path (D2). The facade:

- calls `beginStream(cid, sink)` where the sink wraps an `EventStreamWriter` bound to
  the request (`data` → `write`, `eof` → `close(metadata)`, `exception` → `fail`);
- initiates the backend work (publishes the request event, calls the backend — whatever
  the application's request leg is);
- lets the edge's own machinery do the rest: per-lane ordering, idle timeout,
  disconnect handling, back-pressure.

On client disconnect or idle expiry the facade closes the `PendingStreams` entry (the
streaming analogue of `abort(cid)`) so nothing leaks; the route key's disappearance is
what tells the producer to stop. At idle expiry the facade performs **one final drain**
before failing in-band — the streaming analogue of the one-shot "final read before
timeout" cornerstone, so a dropped *final* notification still completes the render.
(This is a single last-chance read, not the periodic re-drain rejected in D4 — flagged
for Eric's confirmation.)

### 3.6 Configuration keys (proposed)

| Key | Default (proposed) | Meaning |
|-----|--------------------|---------|
| `sync.stream.ttl.seconds` | `120` | `SETEX` TTL of every `segment:{cid}:{seq}` key — the crash safety net for the render window |
| `sync.max.pending.streams` | `1000` | per-pod ceiling on concurrently rendering streams (the reply-lane pool of 500 is the natural upper bound per pod) |

(The first draft's `sync.stream.maxlen` is gone — D6 removed the stream structure it
would have capped.)

## 4. Contracts and invariants

1. **Store-first, notify-after (D1).** A segment is written before its wake-up is
   published; the payload never rides Pub/Sub. Inherited verbatim from the one-shot
   design.
2. **One producer per cid, consecutive sequence from 1 (D3 + D6).** With no broker leg
   there is no partition or redelivery concern; the drain loop delivers strictly in
   sequence by construction. A gap (producer crashed mid-stream) stalls the drain and
   ends as an idle-timeout 408 — never out-of-order delivery.
3. **The terminal signal is data, not signalling.** `eof`/`exception` are stored
   segments occupying the last sequence, so the terminal can never arrive "before" the
   segments it ends.
4. **`cid` stays the module's self-contained key.** The streaming route references only
   `SyncRuntime.CID`; how the request leg carries it remains the application's concern.
5. **No new timeout machinery (D4).** The edge's idle allowance (each segment extends
   it; a stall fails in-band with 408) plus the key TTLs cover every abandonment case;
   cleanup is eager on completion and TTL-driven otherwise. No periodic sweeper.

## 5. Failure analysis

| Failure | Behavior |
|---------|----------|
| Wake-up notification dropped | The next segment's notification drains everything from `nextSeq`; a dropped *final* notification is caught by the facade's single final drain at idle expiry (§3.5) |
| UI pod dies mid-stream | Route key gone with the pod (or expired); the producer's next `post` returns `false` and it stops; orphan segment keys age out on TTL |
| Producer dies mid-stream | No terminal segment ever arrives; the edge idle timeout fails the render in-band with 408; keys age out on TTL |
| Producer outruns a slow UI client | The edge buffers up to its 1 MB slow-client bound; undelivered segments wait in Redis under their TTLs (per-key expiry bounds total footprint — D6/D4) |
| Client disconnects | The edge drops late writes as no-ops; the facade closes the stream entry; the route disappears and the producer stops on its next `post` |
| Pod at stream capacity | `beginStream` rejects deterministically, mirroring `sync.max.pending.requests` (and the edge already rejects at lane exhaustion with 503) |

## 6. Non-goals

- **Not a mesh replacement.** No service discovery, no cross-pod RPC — one rendezvous
  pattern, same as the one-shot route.
- **Not an event store.** Segment keys are a short-lived rendezvous buffer; durable
  history of progressive output, if an application needs it, is its own concern.
- **No fan-out (D5).** One cid, one waiting pod, one render — broadcasting one
  transaction's progress to several viewers is out of scope.
- **Not a change to the `x-stream-id` lane.** Object streams, file downloads and `Flux`
  relays are untouched; this rides the `x-event-stream` idiom only.
- **No broker on the streaming path (D3).** The return route adds no Kafka (or any
  event-system) requirement — consistent with the module's transport-neutral footprint
  (platform-core + Lettuce only).

## 7. Decisions (D-series) — the Q-series as answered by Eric, 2026-09-12

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
  responder gains one requirement: Redis connectivity plus the producer API (§3.4).
- **D4 (from Q4) — No re-drain mechanism; Redis expiry serves the purpose.** TTL is
  the cleanup and the crash backstop. The spec retains one nuance for confirmation: a
  *single* final drain at edge idle expiry (the one-shot "final read before timeout"
  pattern), which is a last-chance read on the failure path, not a sweeper.
- **D5 (from Q5) — No fan-out / broadcast.** Recorded as a non-goal.
- **D6 (from Q6) — The sequence number is the index; per-seq keys replace the Redis
  Stream.** Each event carries a sequence number, so each segment is retrieved by key
  (`segment:{cid}:{seq}`) — nothing appends to an array in Redis, so `MAXLEN` and the
  trim-policy question dissolve. This supersedes the first draft's `stream:{cid}`
  XADD/XREAD design.

## 8. Experiment plan (E-series)

- **E1 — Coordinator + responder primitives.** `PendingStreams`, `beginStream`, the
  drain loop, `StreamResponder.post`, cleanup + unit tests on the embedded Redis:
  in-order delivery by sequence, terminal segment, orphan stop, missed-notification
  healing (suppress a publish, verify the next drain recovers), final drain at idle
  expiry, capacity rejection, duplicate wake-up idempotence.
- **E2 — Single-JVM end-to-end.** `stream: true` endpoint → interceptor facade → a
  responder posting N segments + `eof` (no broker anywhere) → SSE out; verified with
  `curl -N` (and the lambda-example sse-client script).
- **E3 — Cross-pod dry-run.** Two JVMs against `redis-standalone`: the UI request lands
  on pod A; the responder runs on pod B posting to Redis — the actual gap scenario.
  Chaos checks: suppress one notification, kill the producer mid-stream (idle 408), kill
  the UI pod (orphan stop on the producer).
- **E4 — Blueprint payoff.** An LLM/graph-run token stream through the bridge: the
  agent-orchestration wrapper posts token segments via the responder API while the HTTP
  edge renders on a different pod (the E0 demo, made horizontal).

## 9. Relation to the blueprint

The agent-orchestration concept's E0 proved progressive token rendering out the engine's
SSE edge — on one pod. Its open question Q8 (second half) asks how a *graph run* streams
to a UI in a horizontally scaled deployment. This spec is that transport: the graph/LLM
wrapper posts progress events to the return route as it works, and whichever pod holds
the user's connection renders them. Governance properties are inherited, not invented —
the segments are ordinary short-lived Redis values, the rendezvous is the same registry
the one-shot pattern ships today, and the edge contract is the one already shipped and
interop-tested.
