# Streaming return route — cross-pod progressive rendering — design spec

**Status:** DRAFT — proposed 2026-09-12 from Eric's direction: when progressive events
arrive over Kafka (or a similar event system), Redis Pub/Sub should bridge them to the
pod serving the UI request, which forwards them until an end-of-transmission signal
(itself an event). No code yet; this paper captures the design, the open questions
(Q-series) and the staged experiment plan (E-series).
**Blueprint:** serves `bp-agent-orchestration` (Q8 second half — graph-run streaming)
→ serves `vision-mercury-composable`.
**Repo scope:** `extensions/sync-over-async` only. Java-only like the extension itself
(no Rust lock-step; this is not an Event Script surface). No engine change anticipated —
Q2 is the one place a small REST-automation seam could surface.
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

That makes the feature point-to-point. When the progressive events for one transaction
arrive over **Kafka**, any pod's consumer may receive them — and that pod has no route to
another pod's reply lane. Discovering "which pod holds the connection" is exactly the
service-discovery problem the Kafka service mesh solves, and sync-over-async exists as
the deliberately lighter alternative to the mesh. Event-over-HTTP cannot bridge it
either: `/api/event` serves local routes only (ratified 2026-08-30), and the sender
would still need per-pod addressing for dynamically scheduled pods.

Sync-over-async already solved this rendezvous for the **one-shot** case: the
originating pod registers a return route in Redis (`request:{cid}`), and any pod can
look it up and wake the right subscriber. This spec generalizes that return route from
*one response* to *a sequence of segments with a terminal signal*.

## 2. Current state — the pieces already shipped

| Piece | Where | What it gives this design |
|-------|-------|---------------------------|
| `x-event-stream: data \| eof \| exception` contract | platform-core + REST automation | The last mile out the HTTP edge: SSE/chunked framing, ordered reply lanes (pool of 500, deterministic 503 back-pressure), idle timeout with in-band 408, keep-alive pings, slow-client buffer |
| `EventStreamWriter` (`first`/`write`/`close`/`fail`) | platform-core | The producer API the UI-pod forwarder will use verbatim |
| SSE consumption in `async.http.request` | platform-core | Not on this path, but proves the relay idiom the forwarder mirrors |
| One-shot return route (`request:{cid}`, `response:{cid}`, per-pod channel `svc-return:{origin}`) | sync-over-async `ReturnRouteCoordinator` / `ReturnRouteStore` | The rendezvous mechanism: route registry as pod discovery, store-first + wake-up invariant, final-read recovery, bounded `PendingRequests` |
| Self-contained `cid` contract (`SyncRuntime.CID`) | sync-over-async (PR #364) | Transport-neutral correlation: the streaming route works the same whatever event system carried the segments |

## 3. Design

### 3.1 Shape in one sentence

Segments are **stored first** in a per-cid Redis Stream, the **wake-up stays a bare cid**
on the originating pod's existing return channel, and the UI pod **drains and forwards**
each new entry into the shipped `x-event-stream` edge until the terminal entry — the same
store-first/notify-after philosophy as the one-shot route, applied to a sequence.

Redis Pub/Sub remains a wake-up signal only. It is fire-and-forget; in the one-shot
pattern a dropped notification is recoverable because the final read finds the single
response, but a payload dropped mid-stream would be a silently missing segment with
nothing left to re-read. Putting segments in a Redis Stream keeps every segment
re-readable until the rendezvous completes.

### 3.2 Redis keys

| Key | Type | Content | Lifetime |
|-----|------|---------|----------|
| `request:{cid}` | string (unchanged) | the originating pod's return channel | `sync.route.ttl.seconds` — for a stream this must cover the whole render, not just the first response |
| `stream:{cid}` | Redis Stream (new) | one entry per segment (fields below) | `sync.stream.ttl.seconds`, set on first `XADD`; `XADD` with `MAXLEN ~ sync.stream.maxlen` caps growth |
| `response:{cid}` | string | one-shot only — unused on the streaming path | unchanged |

Segment entry fields:

| Field | Meaning |
|-------|---------|
| `type` | `data` \| `eof` \| `exception` — mirrors the `x-event-stream` marker one-to-one |
| `name` | optional SSE event name (maps to the edge's `event:` field) |
| `body` | the segment payload (text or compact JSON) |
| `seq` | optional producer sequence number (see Q3 — may become the explicit stream entry id) |

The `eof` (or `exception`) entry **is** the end-of-transmission signal — "the end signal
is also an event", stored like any other segment so it can never be lost ahead of the
data it terminates.

### 3.3 Coordinator additions (`ReturnRouteCoordinator`)

The existing class gains a streaming sibling for each one-shot member; nothing existing
changes behavior.

| One-shot (today) | Streaming (new) | Notes |
|------------------|-----------------|-------|
| `begin(cid)` → future in `PendingRequests` | `beginStream(cid, sink)` → entry in `PendingStreams` | same `saveRoute`; sink = the forwarder callback |
| `deliver(cid, payload)` — SETEX then publish | `deliverSegment(cid, type, name, body)` — `XADD` then publish cid | store-first invariant preserved; returns `false` for an orphan (route gone) |
| `onResponseSignal` — GET + complete future | drain: `XREAD` from the last-delivered entry id, forward each entry to the sink, stop at `eof`/`exception` | idempotent by entry id — a duplicate wake-up re-reads nothing |
| cleanup — DEL both keys | cleanup at terminal entry — DEL route + stream | eager cleanup; TTLs remain the crash safety net |

One channel serves both patterns: the signal handler checks `PendingStreams` first, then
`PendingRequests`. `PendingStreams` mirrors `PendingRequests` exactly — bounded with
atomic slot reservation (`sync.max.pending.streams`), idempotent close, keyed by cid —
plus one extra piece of state per entry: the last-delivered stream entry id, which makes
draining resumable and duplicate-safe.

Drain execution stays off the Lettuce event loop (the existing virtual-thread
`signalWorkers` hand-off), because `XREAD` and the forward are blocking calls.

### 3.4 Responder side — `soa.progress`

A new shipped task alongside `soa.reply`, wired identically in a reply flow: the Kafka
flow adapter consumes the progressive topic, seeds `model.cid` from the wire header, and
the flow maps `model.cid -> header.cid` into the task. The task reads the segment's
`type`/`name`/`body` from the event and calls `deliverSegment`. The producer emits the
terminal event through the same topic and task — no separate "end" task.

### 3.5 UI-pod facade — recommended first shape

Every shipped streaming producer is an `@EventInterceptor` function addressed directly
by a `stream: true` endpoint (`service: "v1.token.producer"`), while sync-over-async's
one-shot facade is an Event Script flow behind `http.flow.adapter`. Whether those two
compose — a flow task writing to the endpoint's reply lane — is an open platform
question (Q2). The first experiment therefore uses the proven shape:

- a `stream: true` endpoint addresses an interceptor facade function directly;
- the facade calls `beginStream(cid, sink)` where the sink wraps an `EventStreamWriter`
  bound to the request (`data` entry → `write`, `eof` → `close(metadata)`,
  `exception` → `fail`), then publishes the request event (directly, or by triggering
  the outbound flow);
- the edge's own machinery does the rest: ordering per lane, idle timeout, disconnect
  handling, back-pressure.

On client disconnect or idle expiry, the facade calls the streaming analogue of
`abort(cid)` so the `PendingStreams` entry never leaks — mirroring the one-shot
fail-fast contract.

### 3.6 Proposed configuration keys

| Key | Default (proposed) | Meaning |
|-----|--------------------|---------|
| `sync.stream.ttl.seconds` | `120` | TTL of `stream:{cid}` — the crash safety net for the whole render window |
| `sync.stream.maxlen` | `1000` | `XADD MAXLEN ~` cap per stream — bounds a producer that outruns a stalled consumer |
| `sync.max.pending.streams` | `1000` | per-pod ceiling on concurrently rendering streams (the reply-lane pool of 500 is the natural upper bound per pod) |

## 4. Contracts and invariants

1. **Store-first, notify-after.** A segment is `XADD`ed before its wake-up is published;
   the payload never rides Pub/Sub. Inherited verbatim from the one-shot design.
2. **Ordering is the producer's partition contract.** Stream entries render in insertion
   order. If segments of one cid may be consumed by different pods (multiple
   partitions), arrival order at Redis is not guaranteed to be generation order — so the
   progressive reply topic must be **keyed by cid** (one partition → one consumer →
   ordered appends). Same class of deployment contract as the existing end-to-end `cid`
   header rule; Q3 offers an enforcement option.
3. **The terminal signal is data, not signalling.** `eof`/`exception` are stream
   entries, so they can never arrive "before" the segments they terminate, and a missed
   notification still finds them on the next drain.
4. **`cid` stays the module's self-contained key.** The streaming route references only
   `SyncRuntime.CID`; the wire header remains the transport's configurable concern.
5. **No new timeout machinery.** The edge's idle allowance (each segment extends it;
   stall fails in-band with 408) plus the two TTLs cover every abandonment case.

## 5. Failure analysis

| Failure | Behavior |
|---------|----------|
| Wake-up notification dropped | Next segment's notification triggers the drain, which reads everything since the last-delivered id; a trailing drop is bounded by the edge idle timeout (in-band 408) |
| UI pod dies mid-stream | Route key disappears with the pod's rendezvous (or expires); `deliverSegment` returns `false` (orphan) and stops publishing; the stream ages out on its TTL |
| Consumer pod dies mid-stream | Kafka redelivers at-least-once to a rebalanced consumer → possible duplicate `XADD`s (see Q3) |
| Producer outruns a slow UI client | Edge buffers up to its 1 MB slow-client bound; the Redis stream is capped by `MAXLEN` (Q6 discusses trim policy) |
| `eof` never produced | Edge idle timeout fails the render in-band with 408; keys age out on TTL |
| Pod at stream capacity | `beginStream` rejects deterministically, mirroring `sync.max.pending.requests` (and the edge already rejects at lane exhaustion with 503) |

## 6. Non-goals

- **Not a mesh replacement.** No service discovery, no cross-pod RPC — one rendezvous
  pattern, same as the one-shot route.
- **Not an event store.** `stream:{cid}` is a short-lived rendezvous buffer; Kafka
  remains the system of record for the events themselves.
- **Not a change to the `x-stream-id` lane.** Object streams, file downloads and `Flux`
  relays are untouched; this rides the `x-event-stream` idiom only.
- **No fan-out.** One cid, one waiting pod, one render — multiple simultaneous viewers
  of one transaction are out of scope (Q5 records the door).

## 7. Open questions (Q-series)

- **Q1 — Payload over Pub/Sub is rejected; confirm.** The simpler alternative (publish
  each segment's payload on the pod channel) drops the store-first invariant and makes
  every dropped message a silent mid-stream gap. This spec proposes rejecting it;
  ratification makes it decision D1.
- **Q2 — Event Script composition.** Can a `stream: true` endpoint front
  `http.flow.adapter`, with a flow task writing to the endpoint's reply lane? If yes,
  the facade becomes a normal flow (preferred per the config-over-code convention); if
  not, the interceptor facade (§3.5) is the by-design shape, consistent with every
  shipped streaming producer. First experiment proceeds with the interceptor either way.
- **Q3 — Duplicate segments under Kafka redelivery.** Options: (a) accept duplicates
  (SSE consumers of LLM tokens generally cannot); (b) producer-assigned `seq` used as
  the **explicit stream entry id** (`{seq}-0`) — Redis rejects non-monotonic ids, giving
  dedupe *and* ordering enforcement in one mechanism, at the cost of requiring the
  producer to number its segments. (b) is the leading option.
- **Q4 — Idle catch-up.** Rely solely on notification-triggered drains plus the edge
  timeout, or add a slow periodic re-drain for streams with in-flight entries? Start
  without it; E3's chaos check decides.
- **Q5 — Multi-subscriber fan-out.** One route key holds one channel. Broadcasting one
  render to several pods/viewers would need a different registry shape — recorded as a
  door, proposed as out of scope.
- **Q6 — Trim policy.** `MAXLEN ~` trims oldest entries; for a render that must be
  complete-or-failed, trimming the head corrupts the stream — should hitting the cap
  instead fail the stream in-band (`exception` entry)? Leaning yes.

## 8. Experiment plan (E-series)

- **E1 — Coordinator primitives.** `PendingStreams`, `beginStream`/`deliverSegment`/
  drain/cleanup + unit tests on the embedded Redis: in-order delivery, terminal entry,
  orphan, missed-notification catch-up (suppress a publish, verify the next drain
  recovers), capacity rejection, duplicate wake-up idempotence.
- **E2 — Single-JVM end-to-end.** On the module's regression stack (embedded Kafka +
  Redis): `stream: true` endpoint → interceptor facade → Kafka → mock backend emitting
  N segments + `eof` → SSE out; verified with `curl -N` (and the lambda-example
  sse-client script).
- **E3 — Cross-pod dry-run.** Two facade JVMs against `kafka-standalone` +
  `redis-standalone`: the UI request lands on pod A, the reply partition on pod C — the
  actual gap scenario. Chaos checks: suppress one notification, kill the consumer pod
  mid-stream (redelivery/dedupe), kill the UI pod (orphan path).
- **E4 — Blueprint payoff.** An LLM/graph-run token stream through the bridge:
  the agent-orchestration progressive-rendering demo (E0 of that concept) re-run with
  the producer and the HTTP edge on different pods.

## 9. Relation to the blueprint

The agent-orchestration concept's E0 proved progressive token rendering out the engine's
SSE edge — on one pod. Its open question Q8 (second half) asks how a *graph run* streams
to a UI in a horizontally scaled deployment. This spec is that transport: the graph/LLM
wrapper publishes progress events to Kafka as it works, and the streaming return route
delivers them to whichever pod holds the user's connection. Governance properties are
inherited, not invented — the events are ordinary envelopes, the rendezvous is ordinary
Redis state, and the edge contract is the one already shipped and interop-tested.
