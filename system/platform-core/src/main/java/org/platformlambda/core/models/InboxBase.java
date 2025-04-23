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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public abstract class InboxBase {
    private static final Logger log = LoggerFactory.getLogger(InboxBase.class);
    protected static final ExecutorService executor = Platform.getInstance().getVirtualThreadExecutor();
    protected static final ConcurrentMap<String, InboxBase> inboxes = new ConcurrentHashMap<>();
    protected static final String RPC = "rpc";
    protected static final String ANNOTATIONS = "annotations";
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final List<String> ZERO_TRACING_FILTER = List.of(ASYNC_HTTP_CLIENT);

    public final String cid = Utility.getInstance().getUuid();

    public static InboxBase getHolder(String inboxId) {
        return inboxes.get(inboxId);
    }

    public String getCorrelationId() {
        return cid;
    }

    public void close() {
        inboxes.remove(cid);
    }

    /**
     * To be overridden by various inbox implementations
     *
     * @param callback event
     * @return nothing
     */
    public Void handleEvent(EventEnvelope callback) {
        throw new IllegalArgumentException("Not implemented");
    }

    protected void recordRpcTrace(String traceId, String tracePath, String to, String from, String start,
                                  int status, Object error, float execTime, float roundTrip,
                                  Map<String, Object> annotations) {
        var service = trimOrigin(to);
        if (!ZERO_TRACING_FILTER.contains(service)) {
            try {
                Map<String, Object> payload = new HashMap<>();
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("origin", Platform.getInstance().getOrigin());
                metrics.put("id", traceId);
                metrics.put("service", service);
                if (from != null) {
                    metrics.put("from", trimOrigin(from));
                }
                metrics.put("exec_time", execTime);
                metrics.put("round_trip", roundTrip);
                metrics.put("start", start);
                metrics.put("path", tracePath);
                payload.put("trace", metrics);
                if (!annotations.isEmpty()) {
                    payload.put(ANNOTATIONS, annotations);
                }
                metrics.put("status", status);
                if (status >= 400) {
                    metrics.put("success", false);
                    // for data privacy, only shown error message from recognized standard error dataset format
                    metrics.put("exception", error instanceof String message? message : "***");
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

    private String trimOrigin(String route) {
        return route.contains("@")? route.substring(0, route.indexOf('@')) : route;
    }
}
