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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-pod registry of in-flight synchronous requests, keyed by correlation-id. The REST handler
 * registers a future before publishing to Kafka and blocks on it; the return-channel subscriber (or
 * the final-read fallback) completes it when the response arrives.
 * <p>
 * Completion is race-safe and idempotent: whichever of the subscriber thread and the timeout path wins,
 * the future is completed exactly once and the entry removed - duplicate or late responses are no-ops.
 * Growth is bounded by {@code maxPending} to protect a pod under load; the cap is reserved atomically
 * (increment-then-check), so concurrent {@code register} calls cannot oversubscribe it.
 */
public class PendingRequests {

    private final ConcurrentMap<String, CompletableFuture<String>> pending = new ConcurrentHashMap<>();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final int maxPending;

    public PendingRequests(int maxPending) {
        this.maxPending = maxPending;
    }

    /**
     * Register a pending request. Call before publishing the Kafka request.
     *
     * @throws IllegalStateException if the pod is at capacity or the correlation-id is already in flight
     */
    public CompletableFuture<String> register(String correlationId) {
        // reserve the slot atomically before the map write so two threads can't both pass the cap check
        if (inFlight.incrementAndGet() > maxPending) {
            inFlight.decrementAndGet();
            throw new IllegalStateException("Too many pending requests (max " + maxPending + ")");
        }
        CompletableFuture<String> future = new CompletableFuture<>();
        if (pending.putIfAbsent(correlationId, future) != null) {
            inFlight.decrementAndGet();
            throw new IllegalStateException("Duplicate correlation-id in flight: " + correlationId);
        }
        return future;
    }

    /**
     * Complete the waiting future for this correlation-id. Idempotent: returns {@code false} (a no-op)
     * if no request is pending - i.e. an orphan, duplicate, or already-completed/timed-out response.
     */
    public boolean complete(String correlationId, String response) {
        CompletableFuture<String> future = remove(correlationId);
        return future != null && future.complete(response);
    }

    /** Drop a pending request (e.g. on timeout) without completing it. */
    public void cancel(String correlationId) {
        remove(correlationId);
    }

    /** Remove the entry and release its reserved slot, exactly once. */
    private CompletableFuture<String> remove(String correlationId) {
        CompletableFuture<String> future = pending.remove(correlationId);
        if (future != null) {
            inFlight.decrementAndGet();
        }
        return future;
    }

    public boolean isPending(String correlationId) {
        return pending.containsKey(correlationId);
    }

    public int size() {
        return pending.size();
    }
}
