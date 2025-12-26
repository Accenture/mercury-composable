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

package org.platformlambda.core.models;

import org.platformlambda.core.services.Telemetry;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final String SKIP_RPC_TRACING = "skip.rpc.tracing";
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final Set<String> skipTracing = new HashSet<>();
    private static final AtomicBoolean firstRun = new AtomicBoolean(true);

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

    protected void recordTrace(InboxMetadata md) {
        var service = trimOrigin(md.to);
        if (!getSkipTracing().contains(service)) {
            try {
                Map<String, Object> payload = new HashMap<>();
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("origin", Platform.getInstance().getOrigin());
                metrics.put("id", md.traceId);
                metrics.put("service", service);
                if (md.from != null) {
                    metrics.put("from", trimOrigin(md.from));
                }
                metrics.put("exec_time", md.execTime);
                metrics.put("round_trip", md.roundTrip);
                metrics.put("start", md.start);
                metrics.put("path", md.tracePath);
                payload.put("trace", metrics);
                if (!md.annotations.isEmpty()) {
                    payload.put(ANNOTATIONS, md.annotations);
                }
                metrics.put("status", md.status);
                if (md.status >= 400) {
                    metrics.put("success", false);
                    // for data privacy, only shown error message from recognized standard error dataset format
                    metrics.put("exception", md.error instanceof String message? message : "***");
                } else {
                    metrics.put("success", true);
                }
                EventEnvelope dt = new EventEnvelope().setTo(Telemetry.DISTRIBUTED_TRACING);
                EventEmitter.getInstance().send(dt.setBody(payload));
            } catch (Exception e) {
                log.error("Unable to send to {}", Telemetry.DISTRIBUTED_TRACING, e);
            }
        }
    }

    private Set<String> getSkipTracing() {
        if (firstRun.get()) {
            firstRun.set(false);
            var config = AppConfigReader.getInstance();
            var value = config.getProperty(SKIP_RPC_TRACING, ASYNC_HTTP_CLIENT);
            List<String> skipList = Utility.getInstance().split(value, ", ");
            skipTracing.addAll(skipList);
        }
        return skipTracing;
    }

    private String trimOrigin(String route) {
        return route.contains("@")? route.substring(0, route.indexOf('@')) : route;
    }

    protected static class InboxMetadata {
        String traceId;
        String tracePath;
        String to;
        String from;
        String start;
        int status;
        Object error;
        float execTime;
        float roundTrip;
        Map<String, Object> annotations;
    }
}
