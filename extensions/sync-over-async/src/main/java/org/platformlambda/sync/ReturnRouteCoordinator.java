/*

    Copyright 2018-2026 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package org.platformlambda.sync;

import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.platformlambda.redis.RedisBackend;
import org.platformlambda.redis.StandaloneRedisBackend;
import org.platformlambda.support.SyncOverAsyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * The cross-pod return-route engine for one pod (identified by its Mercury origin-id). One storage
 * mechanism - a per-cid Redis List of {@link StreamSegment segments}, drained destructively - serves two
 * rendezvous patterns: a <b>one-shot</b> response is simply the degenerate stream whose first entry is
 * terminal.
 * <ul>
 *   <li>{@link #begin} / {@link #awaitResponse} / {@link #deliver} - the one-shot pattern: register a
 *       pending request, block for the response (with a <b>final drain</b> on timeout so a missed Pub/Sub
 *       notification still resolves), and deliver = post one terminal segment then wake the originator;</li>
 *   <li>{@link #beginStream} / {@link #closeStream} / {@link #finalDrain} - the streaming pattern: register
 *       a sink, and each wake-up drains the queue into it - serialized per cid, so forward order equals
 *       list order - until a terminal segment ({@code eof}/{@code exception}) completes the stream. Any
 *       producer may post the terminal entry; the route's deletion then stops the rest (orphan);</li>
 *   <li>one subscription on this pod's return channel serves both: a wake-up carrying a cid is checked
 *       against the open streams first, then the pending one-shot requests.</li>
 * </ul>
 * Pub/Sub callbacks run on the Lettuce event loop, so all draining is dispatched to virtual threads to
 * avoid stalling/deadlocking that loop (the pops and the sink forward are blocking calls).
 */
public class ReturnRouteCoordinator implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ReturnRouteCoordinator.class);

    private final RedisBackend<String> backend;
    private final boolean ownsBackend;
    private final RedisClusterCommands<String, String> commands;
    private final SyncOverAsyncConfig config;
    private final String returnChannel;
    private final PendingRequests pending;
    private final PendingStreams streams;
    private final ReturnRouteStore store;
    private final ExecutorService signalWorkers = Executors.newVirtualThreadPerTaskExecutor();
    private StatefulRedisPubSubConnection<String, String> subscription;

    /**
     * @param backend the standalone-or-cluster Redis seam; its lifecycle is owned by the caller (typically
     *                {@code SyncRuntime}), so this coordinator uses it but does not close it.
     */
    public ReturnRouteCoordinator(RedisBackend<String> backend, String originId, SyncOverAsyncConfig config) {
        this(backend, false, originId, config);
    }

    /**
     * Convenience for a standalone {@link RedisClient} (single-node callers and tests): wraps it in a
     * non-owning backend that this coordinator closes on {@link #close()}, leaving the shared client alone.
     */
    public ReturnRouteCoordinator(RedisClient client, String originId, SyncOverAsyncConfig config) {
        this(new StandaloneRedisBackend<>(client, StringCodec.UTF8, false), true, originId, config);
    }

    private ReturnRouteCoordinator(RedisBackend<String> backend, boolean ownsBackend, String originId,
                                   SyncOverAsyncConfig config) {
        this.backend = backend;
        this.ownsBackend = ownsBackend;
        this.commands = backend.commands();
        this.config = config;
        this.returnChannel = config.returnChannelPrefix() + ":" + originId;
        this.pending = new PendingRequests(config.maxPendingRequests());
        this.streams = new PendingStreams(config.maxPendingStreams());
        this.store = new ReturnRouteStore(commands);
    }

    /** Subscribe to this pod's return channel. Call once at startup. */
    public void start() {
        if (subscription != null) {
            throw new IllegalStateException("Return-route coordinator already started");
        }
        subscription = backend.openPubSub();
        subscription.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, String businessCorrelationId) {
                // off the event loop: the drain below is a sequence of blocking sync commands
                signalWorkers.submit(() -> onWakeUp(businessCorrelationId));
            }
        });
        subscription.sync().subscribe(returnChannel);
        log.info("Return-route subscriber listening on {}", returnChannel);
    }

    /** Originating pod: register the pending request and publish its return route. */
    public CompletableFuture<String> begin(String businessCorrelationId) {
        CompletableFuture<String> future = pending.register(businessCorrelationId);
        store.saveRoute(businessCorrelationId, returnChannel, config.routeTtlSeconds());
        return future;
    }

    /**
     * Originating pod, await-by-cid: used when {@code begin} and the await run as separate flow tasks, so the
     * caller no longer holds the future returned by {@link #begin}. Looks the future up by correlation-id and
     * blocks on it. An early response (arriving between the two tasks) is covered by the lookup itself:
     * completion is <b>in place</b>, so the completed future is still registered until this await collects it.
     */
    public String awaitResponse(String businessCorrelationId, long timeoutMillis)
            throws InterruptedException, TimeoutException {
        CompletableFuture<String> future = pending.get(businessCorrelationId);
        if (future != null) {
            return awaitResponse(businessCorrelationId, future, timeoutMillis);
        }
        // no registration to be found (defensive) - a final drain may still recover a stored response
        String stored = popResponseBody(businessCorrelationId);
        if (stored != null) {
            store.cleanup(businessCorrelationId);
            return stored;
        }
        throw new IllegalStateException("No pending request for " + businessCorrelationId);
    }

    /**
     * Originating pod: block on the supplied future for the response. On timeout, do one final drain of the
     * rendezvous queue so a dropped Pub/Sub notification still resolves the request before giving up - the
     * same recovery cornerstone a stream applies at edge idle expiry ({@link #finalDrain}).
     *
     * @return the response payload
     * @throws TimeoutException if no response arrived (and none is queued) within {@code timeoutMillis}
     */
    public String awaitResponse(String businessCorrelationId, CompletableFuture<String> future, long timeoutMillis)
            throws InterruptedException, TimeoutException {
        try {
            String response = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            store.cleanup(businessCorrelationId);   // rendezvous done; free the keys now instead of on TTL
            return response;
        } catch (TimeoutException timeout) {
            String late = popResponseBody(businessCorrelationId);
            if (late != null) {
                log.debug("Recovered response for {} via final drain (missed notification)", businessCorrelationId);
                store.cleanup(businessCorrelationId);
                return late;
            }
            // a wake-up may have raced this catch block: it pops the segment and completes the future in
            // place, leaving our drain empty - the future, not Redis, then holds the only copy
            String raced = future.getNow(null);
            if (raced != null) {
                store.cleanup(businessCorrelationId);
                return raced;
            }
            throw timeout;
        } catch (ExecutionException e) {
            throw new IllegalStateException("Pending request failed: " + businessCorrelationId, e.getCause());
        } finally {
            // the sole removal point together with abort(): completion is in place, so the entry (and its
            // capacity slot) is released here on every exit path, including InterruptedException
            pending.cancel(businessCorrelationId);
        }
    }

    /**
     * Cancel a pending request without waiting for a response. Called by the flow's exception handler when
     * the publish step fails (fail-fast path): {@code sync.await} never runs in that case, so the entry
     * registered by {@code begin} would otherwise leak until the pod restarts. Safe to call if the entry
     * has already been removed — cancel is a no-op in that case.
     */
    public void abort(String businessCorrelationId) {
        pending.cancel(businessCorrelationId);
    }

    /**
     * Responder side (any pod): deliver a one-shot response = post one terminal segment (store-first,
     * with the one-shot TTL) then wake the originating pod. Structurally the same act as a stream
     * producer's closing post - the degenerate stream.
     *
     * @return {@code true} if a route existed and a notification was published; {@code false} for an orphan
     *         (route expired or unknown correlation-id - the segment is still queued under its TTL).
     */
    public boolean deliver(String businessCorrelationId, String responsePayload) {
        if (responsePayload == null) {
            throw new IllegalArgumentException("responsePayload must not be null");
        }
        StreamSegment terminal = StreamSegment.of(StreamSegment.EOF, null, responsePayload);
        store.appendSegment(businessCorrelationId, terminal.toJson(), config.responseTtlSeconds());
        String channel = store.getRoute(businessCorrelationId);
        if (channel == null) {
            log.debug("Orphan response for {} - no return route", businessCorrelationId);
            return false;
        }
        commands.publish(channel, businessCorrelationId);
        return true;
    }

    /**
     * Originating pod: open a streaming rendezvous. Registers the sink and publishes the return route with
     * the session-scale streaming TTL, so producers (via {@code StreamResponder}) can discover this pod.
     * The sink receives every drained segment <b>including the terminal one</b> ({@code eof}/{@code exception})
     * in list order; after the terminal segment the stream is closed and its keys deleted. If the sink
     * throws, the stream is closed the same way (the consumer is gone), and producers stop on their next post.
     *
     * @throws IllegalStateException if the pod is at stream capacity or the correlation-id is already open
     */
    public void beginStream(String businessCorrelationId, Consumer<StreamSegment> sink) {
        streams.register(businessCorrelationId, sink);
        store.saveRoute(businessCorrelationId, returnChannel, config.streamTtlSeconds());
    }

    /**
     * Originating pod: close a stream from the consumer side - client disconnect or edge idle expiry.
     * Removes the entry and deletes the Redis keys eagerly; the route's disappearance is what tells every
     * producer to stop. Idempotent (a stream already completed by its terminal segment is a no-op).
     */
    public void closeStream(String businessCorrelationId) {
        streams.remove(businessCorrelationId);
        store.cleanup(businessCorrelationId);
    }

    /**
     * Originating pod: one last-chance drain of an open stream, for the edge idle-expiry path - the
     * streaming analogue of the one-shot final read before timeout, and the same recovery cornerstone:
     * if the final notification was dropped, the queued segments (terminal included) are still delivered
     * before the caller fails the render in-band. This is a single drain, not a periodic sweeper (D4).
     *
     * @return {@code true} if the drain completed the stream (a terminal segment was delivered)
     */
    public boolean finalDrain(String businessCorrelationId) {
        return drainStream(businessCorrelationId);
    }

    /** A wake-up is a bare cid; one channel serves both patterns - streams first, then one-shot requests. */
    private void onWakeUp(String businessCorrelationId) {
        if (streams.contains(businessCorrelationId)) {
            drainStream(businessCorrelationId);
            return;
        }
        String payload = popResponseBody(businessCorrelationId);
        if (payload != null) {
            // completes in place; the entry stays until awaitResponse (or abort) removes it, so an
            // await-by-cid arriving after this wake-up still finds the response (early-arrival path)
            pending.complete(businessCorrelationId, payload);
        }
    }

    /**
     * Pop the first queued segment of a one-shot rendezvous and return its body. The one-shot producer
     * contract is a single terminal entry, so the first entry <em>is</em> the response; a malformed
     * entry is discarded (logged) rather than delivered.
     */
    private String popResponseBody(String businessCorrelationId) {
        String json = store.popSegment(businessCorrelationId);
        if (json == null) {
            return null;
        }
        try {
            return StreamSegment.fromJson(json).body();
        } catch (IllegalArgumentException e) {
            log.warn("Discarding malformed segment for {}", businessCorrelationId, e);
            return null;
        }
    }

    /**
     * Drain the stream's queue into its sink until empty or terminal. Serialized per cid by the entry's
     * drain flag: concurrent wake-ups (several producers) collapse onto one forwarding loop, so forward
     * order equals list order. The flag is held through terminal cleanup, so no segment can be forwarded
     * after the sink saw the terminal one; and after releasing, the queue is re-checked once - a producer
     * that appended during the hold (its wake-up bounced off the flag) is not left stranded.
     *
     * @return {@code true} if this call completed the stream (terminal segment delivered, or sink failure)
     */
    private boolean drainStream(String businessCorrelationId) {
        for (;;) {
            PendingStreams.StreamEntry entry = streams.get(businessCorrelationId);
            if (entry == null || !entry.tryAcquireDrain()) {
                return false;   // stream closed, or another drain is active (it re-checks before exiting)
            }
            try {
                if (forwardQueuedSegments(businessCorrelationId, entry)) {
                    // still under the drain hold: no segment can be forwarded after the terminal one
                    closeStream(businessCorrelationId);
                    return true;
                }
            } finally {
                entry.releaseDrain();
            }
            if (store.queueLength(businessCorrelationId) == 0) {
                return false;
            }
            // lost-wakeup guard: a segment landed while this drain was finishing - loop and re-acquire
        }
    }

    /**
     * Forward queued segments to the sink in list order until the queue is empty or a terminal condition
     * is reached. Runs entirely under the caller's drain hold.
     *
     * @return {@code true} on a terminal condition (terminal segment delivered, or sink failure)
     */
    private boolean forwardQueuedSegments(String businessCorrelationId, PendingStreams.StreamEntry entry) {
        String json;
        while ((json = store.popSegment(businessCorrelationId)) != null) {
            StreamSegment segment;
            try {
                segment = StreamSegment.fromJson(json);
                entry.sink().accept(segment);
            } catch (RuntimeException failure) {
                log.warn("Closing stream {} - segment could not be delivered", businessCorrelationId, failure);
                return true;
            }
            if (segment.isTerminal()) {
                return true;
            }
        }
        return false;
    }

    public int pendingCount() {
        return pending.size();
    }

    /** @return the number of open streaming rendezvous on this pod. */
    public int activeStreams() {
        return streams.size();
    }

    @Override
    public void close() {
        if (subscription != null) {
            subscription.close();
        }
        // let in-flight signal handlers finish their blocking drains before forcing the connection shut
        signalWorkers.shutdown();
        try {
            if (!signalWorkers.awaitTermination(5, TimeUnit.SECONDS)) {
                signalWorkers.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            signalWorkers.shutdownNow();
        }
        // close only a backend this coordinator created (the RedisClient convenience path); a backend passed
        // in is owned and closed by the caller (SyncRuntime), after this returns
        if (ownsBackend) {
            backend.close();
        }
    }
}
