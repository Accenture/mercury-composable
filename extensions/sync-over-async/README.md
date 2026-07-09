# Sync-over-async (Redis-assisted Kafka request/reply)

A self-initializing extension that exposes **synchronous REST semantics over asynchronous Kafka
processing** across horizontally scaled pods. The pod that consumes the Kafka response is usually not
the pod holding the original HTTP connection, so Redis carries the cross-pod return route: the response
payload is stored in Redis (`SETEX`) and a per-pod Pub/Sub channel wakes the originating pod, keyed by a
correlation-id. Kafka remains the durable business transport; Redis Pub/Sub is only a low-latency
wake-up signal.

> **Status: shipped, opt-in.** The feature is **off by default**; enable it with
> `sync.over.async.enabled=true` plus the `redis.*` connection settings. The
> [Sync-over-Async guide](../../docs/guides/sync-over-async.md) is the canonical documentation
> (pattern, configuration keys, reliability design), and
> [`examples/sync-over-async-demo`](../../examples/sync-over-async-demo) is a runnable multi-pod
> demo with captured telemetry.

## What ships in this module

The module contains the Redis return-route engine and the three composable tasks that form the
synchronous facade. The Kafka legs come from `system/minimalist-kafka`, which this module depends on:

- **`ReturnRouteCoordinator`** — the per-pod engine: `begin` registers the return route,
  `awaitResponse` blocks with a final Redis read before timeout (a missed notification cannot lose
  the request), and `deliver` stores the response (`SETEX`, data before signal) then publishes the
  Pub/Sub wake-up to the originating pod's channel.
- **`ReturnRouteStore`** — Redis key storage (`request:{cid}` return route, `response:{cid}`
  payload, short TTLs as the crash safety net) over a single shared [Lettuce](https://lettuce.io/)
  connection.
- **`PendingRequests`** — race-safe in-flight registry: each waiting request completes exactly once
  (duplicate and orphan deliveries are no-ops) under an atomically enforced per-pod ceiling
  (`sync.max.pending.requests`).
- **Composable tasks `sync.prepare`, `sync.await` and `soa.reply`** (`SyncPrepareTask`,
  `SyncAwaitTask`, `SoaReplyTask`) — the building blocks an application wires into its own Event
  Script flow. The reference wiring lives in this module's test resources (`rest.yaml`,
  `flows/sync-to-async.yml`, `flows/soa-reply.yml`, `kafka-flow-adapter.yaml`) and in the demo app.
- **`SyncOverAsyncAutoStart` / `SyncRuntime`** — self-initialization gated by
  `@OptionalService("sync.over.async.enabled")`, so nothing loads (and no Redis connection is
  opened) unless the feature is switched on.

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

The Kafka legs are the reusable building blocks of `system/minimalist-kafka` (this module's direct
dependency), not classes of this module:

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
