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

package org.platformlambda.automation.models;

import io.vertx.core.http.HttpServerRequest;
import org.platformlambda.core.models.EventEnvelope;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("java:S1104")
public class AsyncContextHolder {

    public final HttpServerRequest request;
    public long timeout;
    public long lastAccess;
    public String url;
    public String resHeaderId;
    public String accept;
    public String method;
    public String cidHeaderName;
    public String businessCorrelationId;
    // present when the response is a multi-shot event stream (x-event-stream)
    public EventStreamState eventStream;
    // the dedicated reply lane checked out for a streaming endpoint; volatile because
    // the housekeeper reads it from another thread; returned to the pool at context close
    public volatile String streamLane;
    // non-null when this context serves an Event-over-HTTP streaming relay (/api/event):
    // stream events render as the envelope-mode wire dialect in the requester's format
    public volatile EventEnvelope.Format envelopeStreamFormat;
    private final AtomicBoolean laneReleased = new AtomicBoolean(false);
    // request receipt - the start of the edge's round-trip span (wall clock for the
    // record's start time, monotonic clock for its duration)
    public final long startTime = System.currentTimeMillis();
    private final long startNanos = System.nanoTime();
    // the edge's trace context when the endpoint is traced: the round-trip span minted at
    // receipt (the first function's parent) and the inbound traceparent's span as its parent
    public volatile String traceId;
    public volatile String tracePath;
    public volatile String spanId;
    public volatile String parentSpanId;
    // a failure surfaced while producing the response (an edge error, the housekeeper's
    // timeout, an in-band stream failure) - reported on the round-trip record
    public volatile int errorStatus = 0;
    public volatile String error;
    // data segments the reply lane rendered for a streaming response - the lane annotates
    // its terminal record with this count ("frames")
    public final AtomicLong dataFrames = new AtomicLong(0);

    public AsyncContextHolder(HttpServerRequest request) {
        this.request = request;
        this.timeout = 30 * 1000L;
        this.touch();
    }

    /**
     * Bind the edge's trace context (a traced endpoint) to this request
     *
     * @param traceId of the request
     * @param tracePath METHOD /path
     * @param spanId the round-trip span minted at receipt
     * @param parentSpanId the inbound traceparent's span, if any
     */
    public void setTrace(String traceId, String tracePath, String spanId, String parentSpanId) {
        this.traceId = traceId;
        this.tracePath = tracePath;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
    }

    /**
     * Record a failure for the round-trip record (the first one wins)
     *
     * @param status HTTP-style status
     * @param message error text
     */
    public void markError(int status, String message) {
        if (errorStatus == 0) {
            this.errorStatus = status;
            this.error = message;
        }
    }

    /**
     * Elapsed time since receipt in milliseconds, to 3 decimal places
     *
     * @return elapsed ms
     */
    public double elapsedMs() {
        return Math.round((System.nanoTime() - startNanos) / 1_000.0) / 1_000.0;
    }

    public AsyncContextHolder setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    public AsyncContextHolder setUrl(String url) {
        this.url = url;
        return this;
    }

    public AsyncContextHolder setResHeaderId(String resHeaderId) {
        this.resHeaderId = resHeaderId;
        return this;
    }

    public AsyncContextHolder setMethod(String method) {
        this.method = method;
        return this;
    }

    public AsyncContextHolder setAccept(String accept) {
        this.accept = accept;
        return this;
    }

    public void setCorrelation(String cidHeaderName, String businessCorrelationId) {
        this.cidHeaderName = cidHeaderName;
        this.businessCorrelationId = businessCorrelationId;
    }

    public void touch() {
        this.lastAccess = System.currentTimeMillis();
    }

    /**
     * Claim the one-time right to return this context's reply lane to the pool.
     * The atomic claim lets a dynamic lane binding race safely with a concurrent
     * context close - whichever side runs second performs the release, exactly once.
     *
     * @return the lane to release, or null when there is none, or it was already claimed
     */
    public String claimLaneRelease() {
        return streamLane != null && laneReleased.compareAndSet(false, true) ? streamLane : null;
    }
}
