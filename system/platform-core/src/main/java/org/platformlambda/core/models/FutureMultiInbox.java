/*

    Copyright 2018-2025 Accenture Technology

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

package org.platformlambda.core.models;

import org.platformlambda.core.services.DistributedTrace;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public class FutureMultiInbox extends InboxBase {
    private static final Logger log = LoggerFactory.getLogger(FutureMultiInbox.class);
    private final AtomicInteger total = new AtomicInteger(1);
    private final Map<String, InboxCorrelation> correlations = new HashMap<>();
    private final String start = Utility.getInstance().date2str(new Date());
    private final long begin = System.nanoTime();
    private final CompletableFuture<List<EventEnvelope>> future = new CompletableFuture<>();
    private final String traceId;
    private final String tracePath;
    private final String from;
    private final long timeout;
    private final boolean timeoutException;
    private final long timer;
    private final ConcurrentMap<String, EventEnvelope> replies = new ConcurrentHashMap<>();

    public FutureMultiInbox(int n, String from, String traceId, String tracePath, long timeout,
                           boolean timeoutException) {
        final Platform platform = Platform.getInstance();
        this.timeoutException = timeoutException;
        this.from = from == null? "unknown" : from;
        this.traceId = traceId;
        this.tracePath = tracePath;
        this.total.set(Math.max(1, n));
        this.timeout = Math.max(100, timeout);
        inboxes.put(cid, this);
        timer = platform.getVertx().setTimer(timeout, t -> abort(cid));
    }

    public void setCorrelation(String sequencedCid, InboxCorrelation correlation) {
        correlations.put(sequencedCid, correlation);
    }

    public Future<List<EventEnvelope>> getFuture() {
        return future;
    }

    private void abort(String inboxId) {
        FutureMultiInbox holder = (FutureMultiInbox) inboxes.get(inboxId);
        if (holder != null) {
            holder.close();
            if (timeoutException) {
                future.completeExceptionally(new TimeoutException("Timeout for " + holder.timeout + " ms"));
            } else {
                List<EventEnvelope> result = new ArrayList<>();
                for (Map.Entry<String, EventEnvelope> kv: replies.entrySet()) {
                    result.add(kv.getValue());
                }
                future.complete(result);
            }
        }
    }

    @Override
    public Void handleEvent(EventEnvelope callback) {
        String sequencedCid = callback.getCorrelationId();
        if (sequencedCid != null && sequencedCid.contains("-")) {
            saveResponse(sequencedCid, callback.setReplyTo(null));
        }
        return null;
    }

    private void saveResponse(String sequencedCid, EventEnvelope reply) {
        String inboxId = sequencedCid.substring(0, sequencedCid.lastIndexOf('-'));
        FutureMultiInbox holder = (FutureMultiInbox) inboxes.get(inboxId);
        if (holder != null) {
            float diff = (float) (System.nanoTime() - holder.begin) / EventEmitter.ONE_MILLISECOND;
            reply.setRoundTrip(diff);
            // remove some metadata that are not relevant for a RPC response
            reply.removeTag(RPC).setTo(null).setReplyTo(null).setTrace(null, null);
            Map<String, Object> annotations = new HashMap<>();
            // decode trace annotations from reply event
            Map<String, String> headers = reply.getHeaders();
            if (headers.containsKey(UNDERSCORE)) {
                int count = Utility.getInstance().str2int(headers.get(UNDERSCORE));
                for (int i=1; i <= count; i++) {
                    String kv = headers.get(UNDERSCORE+i);
                    if (kv != null) {
                        int eq = kv.indexOf('=');
                        if (eq > 0) {
                            annotations.put(kv.substring(0, eq), kv.substring(eq+1));
                        }
                    }
                }
                headers.remove(UNDERSCORE);
                for (int i=1; i <= count; i++) {
                    headers.remove(UNDERSCORE+i);
                }
            }
            InboxCorrelation correlation = holder.correlations.get(sequencedCid);
            if (correlation != null) {
                // restore original correlation ID
                replies.put(reply.getId(), reply.setCorrelationId(correlation.cid));
                if (holder.total.decrementAndGet() == 0) {
                    List<EventEnvelope> result = new ArrayList<>();
                    for (Map.Entry<String, EventEnvelope> kv : replies.entrySet()) {
                        result.add(kv.getValue());
                    }
                    holder.close();
                    Platform.getInstance().getVertx().cancelTimer(timer);
                    future.complete(result);
                }
                if (correlation.to != null && holder.traceId != null && holder.tracePath != null) {
                    try {
                        Map<String, Object> payload = new HashMap<>();
                        Map<String, Object> metrics = new HashMap<>();
                        metrics.put("origin", Platform.getInstance().getOrigin());
                        metrics.put("id", holder.traceId);
                        metrics.put("service", correlation.to);
                        metrics.put("from", holder.from);
                        metrics.put("exec_time", reply.getExecutionTime());
                        metrics.put("round_trip", reply.getRoundTrip());
                        metrics.put("start", start);
                        metrics.put("path", holder.tracePath);
                        payload.put("trace", metrics);
                        if (!annotations.isEmpty()) {
                            payload.put(ANNOTATIONS, annotations);
                        }
                        metrics.put("status", reply.getStatus());
                        if (reply.getStatus() >= 400) {
                            metrics.put("success", false);
                            metrics.put("exception", reply.getError());
                        } else {
                            metrics.put("success", true);
                        }
                        EventEnvelope dt = new EventEnvelope().setTo(DistributedTrace.DISTRIBUTED_TRACING);
                        EventEmitter.getInstance().send(dt.setBody(payload));
                    } catch (Exception e) {
                        log.error("Unable to send to {}", DistributedTrace.DISTRIBUTED_TRACING, e);
                    }
                }
            }
        }
    }
}
