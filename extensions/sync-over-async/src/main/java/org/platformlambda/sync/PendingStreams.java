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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Per-pod registry of open streaming rendezvous, keyed by correlation-id - the streaming sibling of
 * {@link PendingRequests}. Each entry holds the consumer's sink (the forwarder that writes segments to
 * the HTTP edge) and the per-stream drain flag that keeps drains <b>serialized per cid</b>: several
 * producers may wake the pod concurrently, but only one drain loop forwards at a time, so forward order
 * equals list order.
 * <p>
 * Growth is bounded by {@code maxPending} with the same atomic slot reservation as
 * {@link PendingRequests}; {@link #remove} is idempotent and releases the slot exactly once.
 */
public class PendingStreams {

    /** One open stream: the segment sink plus the drain-serialization flag. */
    public static final class StreamEntry {
        private final Consumer<StreamSegment> sink;
        private final AtomicBoolean draining = new AtomicBoolean(false);

        private StreamEntry(Consumer<StreamSegment> sink) {
            this.sink = sink;
        }

        public Consumer<StreamSegment> sink() {
            return sink;
        }

        /** @return true if this caller now owns the (single) drain loop for the stream */
        boolean tryAcquireDrain() {
            return draining.compareAndSet(false, true);
        }

        void releaseDrain() {
            draining.set(false);
        }
    }

    private final ConcurrentMap<String, StreamEntry> streams = new ConcurrentHashMap<>();
    private final AtomicInteger open = new AtomicInteger();
    private final int maxPending;

    public PendingStreams(int maxPending) {
        this.maxPending = maxPending;
    }

    /**
     * Register an open stream. Call before any producer can post (i.e. before handing out the cid).
     *
     * @throws IllegalStateException if the pod is at capacity or the correlation-id is already open
     */
    public StreamEntry register(String businessCorrelationId, Consumer<StreamSegment> sink) {
        // reserve the slot atomically before the map write so two threads can't both pass the cap check
        if (open.incrementAndGet() > maxPending) {
            open.decrementAndGet();
            throw new IllegalStateException("Too many open streams (max " + maxPending + ")");
        }
        StreamEntry entry = new StreamEntry(sink);
        if (streams.putIfAbsent(businessCorrelationId, entry) != null) {
            open.decrementAndGet();
            throw new IllegalStateException("Duplicate correlation-id in flight: " + businessCorrelationId);
        }
        return entry;
    }

    /** @return the open stream entry, or {@code null} if the stream completed or was never begun. */
    public StreamEntry get(String businessCorrelationId) {
        return streams.get(businessCorrelationId);
    }

    public boolean contains(String businessCorrelationId) {
        return streams.containsKey(businessCorrelationId);
    }

    /** Close the entry and release its reserved slot, exactly once (idempotent). */
    public void remove(String businessCorrelationId) {
        if (streams.remove(businessCorrelationId) != null) {
            open.decrementAndGet();
        }
    }

    public int size() {
        return streams.size();
    }
}
