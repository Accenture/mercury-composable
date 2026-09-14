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
 * registers a future before publishing the request and blocks on it; the return-channel subscriber (or
 * the final-drain fallback) completes it when the response arrives.
 * <p>
 * Completion is race-safe and idempotent: whichever of the subscriber thread and the timeout path wins,
 * the future is completed exactly once - duplicate or late responses are no-ops. {@link #complete}
 * deliberately completes the future <b>in place</b> without removing the entry: the response segment is
 * destructively popped from Redis before completion, so the completed future is the only remaining copy,
 * and the await-by-cid path (begin and await as separate flow tasks) must still find it by lookup.
 * Removal belongs to the paths that end every request's life - {@code awaitResponse}'s finally block and
 * the exception handler's {@code abort} - which are together exhaustive, so a completed-but-unawaited
 * entry cannot leak. Growth is bounded by {@code maxPending} to protect a pod under load; the cap is
 * reserved atomically (increment-then-check), so concurrent {@code register} calls cannot oversubscribe it.
 */
public class PendingRequests {

    private final ConcurrentMap<String, CompletableFuture<String>> pending = new ConcurrentHashMap<>();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final int maxPending;

    public PendingRequests(int maxPending) {
        this.maxPending = maxPending;
    }

    /**
     * Register a pending request. Call before publishing the request to the async backend.
     *
     * @throws IllegalStateException if the pod is at capacity or the correlation-id is already in flight
     */
    public CompletableFuture<String> register(String businessCorrelationId) {
        // reserve the slot atomically before the map write so two threads can't both pass the cap check
        if (inFlight.incrementAndGet() > maxPending) {
            inFlight.decrementAndGet();
            throw new IllegalStateException("Too many pending requests (max " + maxPending + ")");
        }
        CompletableFuture<String> future = new CompletableFuture<>();
        if (pending.putIfAbsent(businessCorrelationId, future) != null) {
            inFlight.decrementAndGet();
            throw new IllegalStateException("Duplicate correlation-id in flight: " + businessCorrelationId);
        }
        return future;
    }

    /**
     * Complete the waiting future for this correlation-id <b>in place</b> - the entry stays registered
     * (holding its capacity slot) until the awaiting or aborting path removes it, so an await-by-cid that
     * runs after the response arrived still finds the completed future. Idempotent: returns {@code false}
     * (a no-op) if no request is pending, or it was already completed - i.e. an orphan, duplicate, or
     * timed-out response.
     */
    public boolean complete(String businessCorrelationId, String response) {
        CompletableFuture<String> future = pending.get(businessCorrelationId);
        return future != null && future.complete(response);
    }

    /** Drop a pending request (e.g. on timeout) without completing it. */
    public void cancel(String businessCorrelationId) {
        remove(businessCorrelationId);
    }

    /** Remove the entry and release its reserved slot, exactly once. */
    private void remove(String businessCorrelationId) {
        if (pending.remove(businessCorrelationId) != null) {
            inFlight.decrementAndGet();
        }
    }

    public boolean isPending(String businessCorrelationId) {
        return pending.containsKey(businessCorrelationId);
    }

    /**
     * Look up the pending future for this correlation-id without removing it. Returns {@code null} if no
     * request is in flight - e.g. the response already arrived and completed (and removed) the entry.
     * Used by the await-by-cid path when {@code begin} and {@code awaitResponse} run as separate flow tasks.
     */
    public CompletableFuture<String> get(String businessCorrelationId) {
        return pending.get(businessCorrelationId);
    }

    public int size() {
        return pending.size();
    }
}
