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
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.platformlambda.support.SyncOverAsyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The cross-pod return-route engine for one pod (identified by its Mercury origin-id). It:
 * <ul>
 *   <li>{@link #begin} - registers a pending request and publishes its return route to Redis;</li>
 *   <li>subscribes to this pod's return channel and, on a wake-up, completes the waiting future;</li>
 *   <li>{@link #awaitResponse} - blocks for the response, with a <b>final Redis read</b> on timeout so a
 *       missed Pub/Sub notification still resolves;</li>
 *   <li>{@link #deliver} - the responder side (any pod): stores the response, then wakes the originator.</li>
 * </ul>
 * Pub/Sub callbacks run on the Lettuce event loop, so the blocking response read is dispatched to a
 * virtual thread to avoid stalling/deadlocking that loop.
 */
public class ReturnRouteCoordinator implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ReturnRouteCoordinator.class);

    private final RedisClient client;
    private final SyncOverAsyncConfig config;
    private final String returnChannel;
    private final PendingRequests pending;
    private final StatefulRedisConnection<String, String> commandConnection;
    private final ReturnRouteStore store;
    private final ExecutorService signalWorkers = Executors.newVirtualThreadPerTaskExecutor();
    private StatefulRedisPubSubConnection<String, String> subscription;

    public ReturnRouteCoordinator(RedisClient client, String originId, SyncOverAsyncConfig config) {
        this.client = client;
        this.config = config;
        this.returnChannel = config.returnChannelPrefix() + ":" + originId;
        this.pending = new PendingRequests(config.maxPendingRequests());
        this.commandConnection = client.connect();
        this.store = new ReturnRouteStore(commandConnection);
    }

    /** Subscribe to this pod's return channel. Call once at startup. */
    public void start() {
        subscription = client.connectPubSub();
        subscription.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, String correlationId) {
                // off the event loop: the response read below is a blocking sync command
                signalWorkers.submit(() -> onResponseSignal(correlationId));
            }
        });
        subscription.sync().subscribe(returnChannel);
        log.info("Return-route subscriber listening on {}", returnChannel);
    }

    /** Originating pod: register the pending request and publish its return route. */
    public CompletableFuture<String> begin(String correlationId) {
        CompletableFuture<String> future = pending.register(correlationId);
        store.saveRoute(correlationId, returnChannel, config.routeTtlSeconds());
        return future;
    }

    /**
     * Originating pod: block for the response. On timeout, do one final Redis read so a dropped Pub/Sub
     * notification still resolves the request before giving up.
     *
     * @return the response payload
     * @throws TimeoutException if no response arrived (and none is in Redis) within {@code timeoutMillis}
     */
    public String awaitResponse(String correlationId, CompletableFuture<String> future, long timeoutMillis)
            throws InterruptedException, TimeoutException {
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeout) {
            String late = store.getResponse(correlationId);
            pending.cancel(correlationId);
            if (late != null) {
                log.debug("Recovered response for {} via final read (missed notification)", correlationId);
                return late;
            }
            throw timeout;
        } catch (ExecutionException e) {
            pending.cancel(correlationId);
            throw new IllegalStateException("Pending request failed: " + correlationId, e.getCause());
        }
    }

    /**
     * Responder side (any pod): store the response payload (source of truth) then wake the originating pod.
     *
     * @return {@code true} if a route existed and a notification was published; {@code false} for an orphan
     *         (route expired or unknown correlation-id - the response is still stored under its TTL).
     */
    public boolean deliver(String correlationId, String responsePayload) {
        store.saveResponse(correlationId, responsePayload, config.responseTtlSeconds());
        String channel = store.getRoute(correlationId);
        if (channel == null) {
            log.debug("Orphan response for {} - no return route", correlationId);
            return false;
        }
        commandConnection.sync().publish(channel, correlationId);
        return true;
    }

    private void onResponseSignal(String correlationId) {
        String payload = store.getResponse(correlationId);
        if (payload != null) {
            pending.complete(correlationId, payload);
        }
    }

    public int pendingCount() {
        return pending.size();
    }

    @Override
    public void close() {
        if (subscription != null) {
            subscription.close();
        }
        signalWorkers.shutdownNow();
        commandConnection.close();
    }
}
