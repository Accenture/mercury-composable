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

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.platformlambda.core.services.DistributedTrace;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public class AsyncInbox extends InboxBase {
    private static final Logger log = LoggerFactory.getLogger(AsyncInbox.class);
    private final String start = Utility.getInstance().date2str(new Date());
    private final long begin = System.nanoTime();
    private final String traceId;
    private final String tracePath;
    private final String from;
    private final String to;
    private final String originalCid;
    private final long timeout;
    private final long timer;
    private final boolean timeoutException;
    private final Future<EventEnvelope> future;
    private Promise<EventEnvelope> promise;

    public AsyncInbox(String to, EventEnvelope event, long timeout,
                      boolean timeoutException) {
        this.timeoutException = timeoutException;
        this.from = event.getFrom() == null? "unknown" : event.getFrom();
        this.to = to;
        this.traceId = event.getTraceId();
        this.tracePath = event.getTracePath();
        this.originalCid = event.getCorrelationId();
        this.timeout = Math.max(100, timeout);
        this.future = Future.future(p -> {
            promise = p;
            inboxes.put(cid, this);
        });
        this.timer = Platform.getInstance().getVertx().setTimer(timeout, t -> abort(cid));
    }

    public Future<EventEnvelope> getFuture() {
        return future;
    }

    private void abort(String inboxId) {
        AsyncInbox holder = (AsyncInbox) inboxes.get(inboxId);
        if (holder != null) {
            holder.close();
            String error = "Timeout for " + holder.timeout + " ms";
            executor.submit(() -> {
                if (timeoutException) {
                    holder.promise.fail(new TimeoutException(error));
                } else {
                    holder.promise.complete(new EventEnvelope().setStatus(408).setBody(error));
                }
            });
        }
    }

    @Override
    public Void handleEvent(EventEnvelope callback) {
        String inboxId = callback.getCorrelationId();
        if (inboxId != null) {
            if (originalCid != null) {
                callback.setCorrelationId(originalCid);
            }
            saveResponse(inboxId, callback.setReplyTo(null));
        }
        return null;
    }

    private void saveResponse(String inboxId, EventEnvelope reply) {
        AsyncInbox holder = (AsyncInbox) inboxes.get(inboxId);
        if (holder != null) {
            holder.close();
            Platform.getInstance().getVertx().cancelTimer(timer);
            float diff = (float) (System.nanoTime() - holder.begin) / EventEmitter.ONE_MILLISECOND;
            reply.setRoundTrip(diff);
            // remove some metadata that are not relevant for a RPC response
            reply.removeTag(RPC).setTo(null).setReplyTo(null).setTrace(null, null);
            var annotations = new HashMap<>(reply.getAnnotations());
            reply.clearAnnotations();
            executor.submit(() -> holder.promise.complete(reply));
            if (to != null && holder.traceId != null && holder.tracePath != null) {
                try {
                    Map<String, Object> payload = new HashMap<>();
                    Map<String, Object> metrics = new HashMap<>();
                    metrics.put("origin", Platform.getInstance().getOrigin());
                    metrics.put("id", holder.traceId);
                    metrics.put("service", to);
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
