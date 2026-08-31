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

    public AsyncContextHolder(HttpServerRequest request) {
        this.request = request;
        this.timeout = 30 * 1000L;
        this.touch();
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
