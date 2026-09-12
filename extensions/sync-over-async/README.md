# Sync-over-async (Redis-assisted request/reply and streaming for async backends)

A self-initializing extension that exposes **synchronous REST semantics over asynchronous
processing** across horizontally scaled pods. The pod that produces the response is usually not
the pod holding the original HTTP connection, so Redis carries the cross-pod return route: every
segment is appended to a short-lived per-correlation-id **rendezvous queue** (a Redis List,
`RPUSH` before any signal) and a per-pod Pub/Sub channel wakes the originating pod, which drains
the queue destructively (`LPOP`). One mechanism serves two patterns — a **one-shot** response is
the degenerate stream (a queue whose first entry is terminal), and a **streaming** rendezvous
carries progressive events until an `eof`/`exception` entry. The application's async transport
(e.g. Kafka) remains the durable business leg; Redis Pub/Sub is only a low-latency wake-up signal.

> **Status: shipped, opt-in.** The feature is **off by default**; enable it with
> `sync.over.async.enabled=true` plus the `redis.*` connection settings. The
> [Sync-over-Async guide](../../docs/guides/sync-over-async.md) is the canonical documentation
> (pattern, configuration keys, reliability design), and
> [`examples/sync-over-async-demo`](../../examples/sync-over-async-demo) is a runnable multi-pod
> demo with captured telemetry.

## What ships in this module

The module contains the Redis return-route engine, the three composable tasks that form the
synchronous facade, and the streaming producer API. It is **transport-neutral by composition**: its
compile footprint is platform-core + Lettuce only; the Kafka legs of the reference wiring come from
`system/minimalist-kafka`, which the application declares itself (here it is a test-scope dependency
used by the end-to-end regression):

- **`ReturnRouteCoordinator`** — the per-pod engine. One-shot: `begin` registers the return route,
  `awaitResponse` blocks with a final drain before timeout (a missed notification cannot lose
  the request), and `deliver` posts one terminal segment (`RPUSH` + TTL, data before signal) then
  publishes the Pub/Sub wake-up to the originating pod's channel. Streaming: `beginStream`
  registers a segment sink, each wake-up drains the queue into it — serialized per cid, so forward
  order equals list order — until a terminal entry, `finalDrain` is the last-chance drain at edge
  idle expiry, and `closeStream` is the consumer-side close (client disconnect).
- **`ReturnRouteStore`** — Redis key storage (`request:{cid}` return route, `queue:{cid}`
  rendezvous queue with an atomically refreshed TTL as the crash safety net) over a single shared
  [Lettuce](https://lettuce.io/) connection.
- **`StreamResponder`** — the segment-producer API a backend service constructs from plain
  `redis.*` parameters (no coordinator, no subscriber): `post(cid, type, name, body)` appends
  store-first and wakes the consuming pod; `false` means the rendezvous is over — stop producing.
  There is **no sequence number**: ordering, where required, is the posting discipline (Redis
  executes each connection's commands in arrival order), and any producer may post the terminal
  entry.
- **`StreamBridge` / `EventStreamSink`** — the generic half of a UI-pod streaming facade: an
  `@EventInterceptor` addressed by a `stream: true` endpoint calls `StreamBridge.open(coordinator,
  request, cid, idleSeconds)` and the bridge does the rest — commits the SSE head, forwards each
  drained segment into the request's reply lane (`data` → SSE event, `eof` → the terminal `done`
  event, `exception` → the in-band `error` event), and owns the idle watchdog whose expiry performs
  one final drain (a dropped final notification still completes the render) before failing in-band
  and closing the rendezvous. Stream capacity rejects with a proper HTTP 503 before the head is
  committed.
- **`PendingRequests` / `PendingStreams`** — race-safe in-flight registries: each waiting request
  completes exactly once — in place, so the await-by-cid path survives destructive pops — and each
  stream drains on a single loop at a time, under atomically enforced per-pod ceilings
  (`sync.max.pending.requests`, `sync.max.pending.streams`).
- **Composable tasks `sync.prepare`, `sync.await` and `soa.reply`** (`SyncPrepareTask`,
  `SyncAwaitTask`, `SoaReplyTask`) — the building blocks an application wires into its own Event
  Script flow. The reference wiring lives in this module's test resources (`rest.yaml`,
  `flows/sync-to-async.yml`, `flows/soa-reply.yml`, `kafka-flow-adapter.yaml`) and in the demo app.
- **`SyncOverAsyncAutoStart` / `SyncRuntime`** — self-initialization gated by
  `@OptionalService("sync.over.async.enabled")`, so nothing loads (and no Redis connection is
  opened) unless the feature is switched on. (`StreamResponder` needs neither — the producer side
  has no switch.)

## Threading model & virtual-thread safety

Mercury runs functions on **virtual threads**. The Redis client ([Lettuce](https://lettuce.io/)) is
accessed through its blocking **sync** API (`connection.sync()`), so a reasonable reviewer will ask
whether a blocking call pins its carrier (platform) thread and defeats the purpose of virtual threads.

It does not, and the reasoning is:

- Lettuce performs the actual socket I/O on **Netty event-loop (platform) threads**. The caller only
  *awaits* the reply on a `CountDownLatch` inside Lettuce's `AsyncCommand`.
- That latch is built on `AbstractQueuedSynchronizer` / `LockSupport.park`, which **Java 21 made
  virtual-thread-aware**: the virtual thread **unmounts** from its carrier for the duration of the
  round-trip instead of holding it. Pinning would only happen if the thread blocked while holding a
  `synchronized` monitor (or inside a native frame) — which this await path does not.
- Therefore blocking sync on a virtual thread is the **recommended idiom** here: simpler than the
  reactive API, with the same throughput. A single shared connection multiplexes commands from many
  virtual threads, so no connection pool is required.

The one place a blocking call **must not** run is the Lettuce Pub/Sub callback, which executes on the
Netty event loop — a sync command there would self-deadlock. `ReturnRouteCoordinator` deliberately
dispatches the post-notification Redis read to a virtual-thread executor for exactly this reason.

### Evidence

This is not just an argument — it is measured by
[`VirtualThreadPinningTest`](src/test/java/org/platformlambda/sync/VirtualThreadPinningTest.java). The
test drives 100 virtual threads through the real `ReturnRouteStore` round-trips while a JFR
`RecordingStream` listens for `jdk.VirtualThreadPinned` events (the same signal behind
`-Djdk.tracePinnedThreads`), with the default 20&nbsp;ms threshold removed so even a sub-millisecond pin
against embedded Redis is caught.

One subtlety is documented in the test so it isn't misread: the JVM itself pins a carrier **once** while
it lazily bootstraps `invokedynamic` call sites — string concatenation in particular, where
`StringConcatFactory` generates a hidden class inside a VM frame (`pinnedReason = "Native or VM frame on
stack"`). These pins are one-time, sub-millisecond, JDK-build-dependent, and unrelated to I/O blocking,
so depending on JVM warm-up state a handful may appear. The test therefore (1) **warms up** the workload
before measuring, and (2) fails only on pins whose stack actually runs through Lettuce (`io.lettuce.*`),
reporting any residual JVM-internal pins rather than hiding them. It asserts **zero Lettuce-attributable
pins** and is verified green on JDK 21 and JDK 26.

## Request/response legs (Kafka)

The reference wiring's Kafka legs are the reusable building blocks of `system/minimalist-kafka` — a
test-scope dependency here, exercised by the end-to-end regression; an application that wants them
declares the library itself (the extension does not impose a transport):

- **Outbound** — `simple.kafka.notification` publishes the request to the request topic with `cid`
  + `traceparent` headers. It returns a `Mono` that completes on broker acknowledgment, so inside
  an Event Script flow a publish failure fails the task and routes to the flow's exception handler
  (fail-fast) instead of surfacing as a silent timeout later.
- **Inbound** — the Kafka Flow Adapter binds the response topic to a flow (see
  `src/test/resources/kafka-flow-adapter.yaml`); that flow hands `(cid, payload)` to `soa.reply`,
  which calls `ReturnRouteCoordinator::deliver`. Consumption is at-least-once with
  commit-after-process, and the adapter re-parents the flow onto the inbound `traceparent` so the
  distributed trace stays continuous across both Kafka hops.

The full path — `sync.prepare → simple.kafka.notification → backend → response topic → soa.reply →
Redis return route → sync.await`, with one trace id preserved throughout — is proven end-to-end by
[`RestFlowMvpTest`](src/test/java/org/platformlambda/async/RestFlowMvpTest.java) against a real
embedded KRaft broker and `redis-server`.

## Building

This is an optional extension. Build it from source with `mvn clean install` (or publish it to your
organization artifactory). Unit tests run against an embedded `redis-server` binary (bundled for macOS
and Linux, arm64/amd64) — no Docker required.
