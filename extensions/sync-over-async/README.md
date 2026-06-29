# Sync-over-async (Redis-assisted Kafka request/reply)

A self-initializing extension that exposes **synchronous REST semantics over asynchronous Kafka
processing** across horizontally scaled pods. The pod that consumes the Kafka response is usually not
the pod holding the original HTTP connection, so Redis carries the cross-pod return route: the response
payload is stored in Redis (`SETEX`) and a per-pod Pub/Sub channel wakes the originating pod, keyed by a
correlation-id. Kafka remains the durable business transport; Redis Pub/Sub is only a low-latency
wake-up signal.

> **Status: prototype.** The cross-pod return-route engine (Phase 1) and the Kafka request/response
> legs (Phase 2) are implemented and tested against embedded Redis + Kafka. The synchronous REST facade
> that ties them together — and registers these pieces as Mercury functions — is the remaining MVP piece
> (Phase 3).

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

Two small classes bracket the asynchronous backend:

- **`KafkaRequestPublisher`** (outbound) — publishes the request to the request topic, keyed by
  correlation-id and carrying `cid` + `traceparent` headers. The send is confirmed (blocking with a
  timeout) so a publish failure surfaces immediately rather than as a silent timeout later.
- **`KafkaResponseConsumer`** (inbound "return adapter") — a single-threaded poll loop on the response
  topic that decodes each record and hands `(cid, payload)` to a `ResponseDelivery`, wired in production
  to `ReturnRouteCoordinator::deliver`. The Kafka consumer is not thread-safe, so it is owned by one
  daemon thread and stopped cleanly via `wakeup()`.

Both use `key=String, value=byte[]` to match the platform's Kafka connector. The full path —
`begin → publish → backend → response topic → deliver → Redis return route → awaitResponse`, with
`traceparent` preserved throughout — is proven end-to-end in `SyncOverAsyncKafkaIntegrationTest` against
a real embedded KRaft broker and `redis-server`.

## Building

This is an optional extension. Build it from source with `mvn clean install` (or publish it to your
organization artifactory). Unit tests run against an embedded `redis-server` binary (bundled for macOS
and Linux, arm64/amd64) — no Docker required.
