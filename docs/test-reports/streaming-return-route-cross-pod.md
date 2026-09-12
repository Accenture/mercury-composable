---
title: Test Report — Streaming Return Route, cross-pod dry-run
summary: Permanent record of the E3 experiment - two JVMs against a standalone Redis,
  the UI request landing on one pod and producers posting from another, with chaos
  checks for lost notifications, a producer killed mid-stream, and a killed UI pod.
layer: reference
audience: [developer, architect]
keywords: [streaming, sse, redis, return route, cross-pod, chaos, test report]
---

# Test Report — Streaming Return Route, cross-pod dry-run

*Live two-pod validation of the
[streaming return route](https://github.com/Accenture/mercury-composable/blob/main/draft-design-specs/streaming-return-route.md)
(experiment E3), conducted 2026-09-12 (local time) on a single developer machine with three
JVM processes and `curl` as the client. This report is a permanent record in the tradition of
the [progressive-rendering interop report](progressive-rendering-interop.md): what was run,
the evidence, and the observations the round produced.*

## The scenario under test

The streaming return route exists for exactly one gap: in a horizontally scaled deployment,
the pod that *produces* progressive events is generally not the pod holding the user's HTTP
connection. E3 makes that literal — **two separate JVMs**:

| Process | Role | Port | Notes |
|---------|------|------|-------|
| `redis-standalone` | the rendezvous | 6379 | the only shared infrastructure — **no broker anywhere** (design D3); both pods run with the Kafka building blocks switched off |
| `sync-over-async-demo`, profile `stream-ui` (**pod A**) | holds the SSE connection | 8600 | return-route coordinator + `StreamBridge` facade; `sync.stream.ttl.seconds=30` for this run so the TTL-bounded endings complete quickly |
| `sync-over-async-demo`, profile `stream-producer` (**pod B**) | posts the events | 8601 | `StreamResponder` only — **no coordinator**, no subscriber, no `sync.over.async.enabled` |

The client opens `GET /api/notifications` on pod A (`stream: true`, SSE); the facade
announces the session's correlation-id as the first SSE event; the client then POSTs to
pod B's `/api/produce`, quoting that cid — the notification use case's exact shape. Pod B's
chaos mode (`"mode": "lost"`) stores a segment while suppressing its wake-up, simulating a
lost Pub/Sub notification. The build under test is the unified list mechanism
(D7/D8 + `StreamBridge`, PRs #369/#370, pre-release).

## Scenario 1 — ordered tokens across pods (the chat shape)

Pod B posts five tokens and `eof` sequentially over one connection; pod A renders them out
its SSE edge. **No sequence number exists anywhere in the pipeline** — order is carried by
posting discipline → Redis list order → serialized drain → the edge's ordered reply lane:

```text
14:59:04.656  event: cid
14:59:04.656  data: 61ea9747fb934af2b821322d5ef786d0
14:59:07.409  data: Streaming
14:59:07.409  data:  across
14:59:07.409  data:  pods
14:59:07.410  data:  by
14:59:07.411  data:  design
14:59:07.411  event: done
14:59:07.411  data: {"tokens":5}
```

Producer's view: `{"mode":"chat","live":true,"posted":6}`. **Exact order preserved,
terminal metadata delivered, rendezvous closed.** ✅

## Scenario 2 — lost notification, healed by the next drain

Three notifications from pod B: `orders` (normal), `billing` (**wake-up suppressed**),
`payments` (normal). Mid-run check after the suppressed post: `billing` is stored in Redis
but **absent from the render** — then the `payments` wake-up's drain delivers both, in list
order, and a backend-side `close` ends the channel ("the end signal is also an event"):

```text
14:59:30.835  event: orders
14:59:30.835  data: order 42 shipped
              (billing posted with its wake-up suppressed - nothing renders)
14:59:34.082  event: billing
14:59:34.082  data: invoice 7 ready
14:59:34.082  event: payments
14:59:34.082  data: refund 9 done
14:59:35.191  event: done
```

**A dropped signal costs latency, never data.** ✅

## Scenario 3 — lost *close*, recovered by the final drain at idle expiry

The worst case design D4's nuance exists for: a data segment **and the terminal `eof`** are
stored with every wake-up suppressed. Nothing wakes pod A. At the channel's idle allowance
(6s, set per request via `x-stream-idle-seconds`) the facade watchdog performs its **single
final drain** — and the render completes successfully:

```text
14:59:54.769  event: cid
              (data + eof stored at ~14:59:57, all wake-ups suppressed)
15:00:00.770  event: orders
15:00:00.770  data: the last update
15:00:00.771  event: done
15:00:00.771  data: {"recovered":true}
```

The drain fired at **idle + 1 ms** (channel opened 14:59:54.769, drain at 15:00:00.770).
**The one-shot "final read before timeout" cornerstone, working as a stream, across pods.** ✅

## Scenario 4 — producer killed mid-stream (`kill -9`)

Pod B posts two tokens ("stall" mode) and is then killed. No terminal can ever arrive; the
render must fail **in-band** at idle expiry, exactly like the failure table says:

```text
15:00:22.841  event: cid
15:00:25.402  data: first
15:00:25.403  data: second
              (pod B kill -9)
15:00:31.415  event: error
15:00:31.415  data: {"status":408,"message":"Stream idle timeout","type":"error"}
```

408 at 6.01s after the last segment (the watchdog re-armed by the activity, then firing).
The rendezvous keys were deleted by the close; the queue remnant needs no sweeper. ✅

## Scenario 5 — UI pod killed (`kill -9`) → producer orphan stop, TTL-bounded

Pod A is killed with a channel open (route TTL 30s in this run). The route key is Redis
state, so it does **not** vanish with the pod — a post inside the TTL window is accepted
into the void (stored, published to a channel nobody subscribes to), and the producer stops
as soon as the route expires:

```text
15:01:05  post right after the kill: {"mode":"notify","live":true}    <- TTL-bounded void window
15:01:48  post after the 30s TTL:    {"mode":"notify","live":false}   <- orphan stop
```

**Bounded, deterministic stop with no liveness machinery** — the TTL is the crash backstop
doing exactly its job (contract 5). The spec's failure table has been tightened to state the
timing explicitly: the producer stops *within the route TTL* of a UI-pod crash, not
instantaneously, and the abandoned queue remnant ages out on its own TTL. ✅

## Scenario 6 — real LLM tokens across pods (experiment E4, the blueprint payoff)

The reason this transport exists (agent-orchestration Q8, second half): an LLM's token stream
produced on one pod, rendered on another. Same topology, one more composition — **pod B pulls the
provider's own SSE stream through the platform's shipped SSE consumer** (`async.http.request` with
`Accept: text/event-stream` + a reply route) **and a bridge function forwards each relayed
`x-event-stream` envelope into the rendezvous** via `StreamResponder`. No SDK, no new
infrastructure: the request leg is one event, and the bridge is ~40 lines of forwarding.

```text
  pod A (SSE render)      Redis       pod B: async.http.request ──SSE──► Gemini
    event: cid ────────────────────────► POST /api/produce {cid, mode: "llm", prompt}
    ◄── token batches ◄── queue:{cid} ◄── demo.llm.bridge (reply route, instances=1)
    event: done {usage}
```

Live run (Gemini `streamGenerateContent?alt=sse`, `gemini-flash-latest` resolving to
`gemini-3.8-flash`, developer key from the environment):

```text
15:57:56.709  event: cid
15:57:56.709  data: 4b6345d72a73466c990e93c6265169df
15:58:01.108  data: They decouple components, allowing individual services to scale
              independently based on demand. Additionally, asynchronous communication
15:58:01.109  data:  prevents bottlenecks by letting producers and consumers process
              tasks at their own pace.
15:58:01.113  event: done
15:58:01.114  data: {"provider":"gemini","model":"gemini-3.8-flash","finishReason":"STOP",
              "promptTokenCount":14,"candidatesTokenCount":32,"totalTokenCount":46}
```

Real tokens, in generation order, terminal `done` carrying the provider's usage metadata — and the
provider's early refusals (a 404 during the round) rode the same path in-band as `event: error`. ✅

This is also the wrapper-side shape a `graph.task` node drives (E0 proved the live
`graph.task → wrapper` leg): the node streams progress out-of-band through the rendezvous while its
own graph edge stays plain request/response — graph-run streaming with **zero engine change**.

**Two findings from this round:**

1. **platform-core: the HTTP client percent-encoded `:` in URI path segments** (form encoding via
   `URLEncoder`), which Google-style custom methods (`…/models/<model>:streamGenerateContent` —
   every Google Cloud `:verb` API) reject with 404. RFC 3986 allows `:` in a path segment (pchar);
   fixed in `Utility.encodeUriSegments` with a regression pin (its own PR).
2. **The producer contract bites forwarders too:** the bridge initially ran 50 function instances,
   so relayed frames were posted concurrently — and a token batch sequenced *behind* the terminal
   in the queue is discarded by design (the first live run rendered a mid-sentence answer).
   `instances = 1` restores post-in-order (design D7) — the same reason the HTTP edge's reply
   lanes are single-instance routes. A high-fanout application would mint one temporary route per
   stream.

(Demo note: current flash aliases resolve to *thinking* models — the demo disables the thinking
budget and bounds `maxOutputTokens`, or a short generation spends its whole budget on reasoning
and returns `MAX_TOKENS` with no visible text.)

## Observations and round notes

- **No mechanism defects found.** All five scenarios behaved per the ratified design on the
  first complete run.
- **Sizing note for operators:** `sync.stream.ttl.seconds` bounds two things at once — how
  long a crashed UI pod's rendezvous accepts posts into the void, and how long a stalled
  consumer's queue survives. The 1800s default favors long-lived quiet notification
  channels; deployments with chatty producers may prefer a smaller value (this run used 30s
  to make the bound observable).
- **One demo-side fix during the round** (not a mechanism issue): a function addressed
  *directly* by a rest.yaml entry receives the whole `AsyncHttpRequest` object as its input —
  the JSON body is inside it — unlike a flow task, whose input mapping extracts
  `input.body`. The demo's producer endpoint now unwraps accordingly
  (`new AsyncHttpRequest(request.getBody())`), and the
  [REST automation guide](../guides/rest-automation/index.md) now states the contract
  explicitly for interceptor/untyped functions (gap closed in the same round, confirmed by
  the maintainer).
- The demo's `stream-ui` / `stream-producer` profiles are permanent: the runbook in the
  [sync-over-async-demo README](https://github.com/Accenture/mercury-composable/tree/main/examples/sync-over-async-demo)
  reproduces this report end-to-end with three terminals and `curl`.

## What remains

Nothing — scenario 6 completed the E-series (E4, the blueprint payoff). The transport half of
agent-orchestration Q8's second question is delivered: a graph/LLM wrapper posts its token stream
to the return route as it works, and whichever pod holds the user's connection renders it. Driving
this bridge from an LLM node *inside* a live graph run is the agent-orchestration thread's next
experiment, on the foundation recorded here.
