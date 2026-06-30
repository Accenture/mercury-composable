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

package org.platformlambda.core.logging;

import org.platformlambda.core.models.TraceInfo;
import org.platformlambda.core.util.Utility;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-request holder for application log context.
 * <p>
 * It carries a reference to the live {@link TraceInfo} (created on the worker thread), the
 * correlation-id captured from the incoming event, and a map of developer-supplied custom
 * key-values. It is created and destroyed in lockstep with the worker's trace bracket and
 * is registered in {@link LogContextManager} keyed by the worker thread id.
 * <p>
 * This deliberately avoids the ThreadLocal / Log4j MDC pattern, which is an anti-pattern for
 * a virtual-thread runtime.
 */
public class LogContext {
    /**
     * Reserved keys (the logical names) that a developer cannot override via
     * {@code PostOffice.updateContext}. The output key name in app-log-context.yaml is the
     * operator's choice; this set governs the developer API only.
     */
    public static final Set<String> RESERVED_KEYS =
            Set.of("cid", "traceId", "tracePath", "spanId", "parentSpanId", "service", "utc");

    private final TraceInfo trace;
    private final String cid;
    // key order is not preserved; log aggregators (Dynatrace, Splunk, ...) reorder keys on display anyway
    private final ConcurrentMap<String, Object> customKeys = new ConcurrentHashMap<>();

    public LogContext(TraceInfo trace, String cid) {
        this.trace = trace;
        this.cid = cid;
    }

    public static boolean isReservedKey(String key) {
        return RESERVED_KEYS.contains(key);
    }

    /**
     * Add (or remove, when value is null) a developer-supplied context key-value.
     *
     * @param key custom key (must not be a reserved key)
     * @param value associated value; null removes the key
     */
    public void put(String key, Object value) {
        if (value == null) {
            customKeys.remove(key);
        } else {
            customKeys.put(key, value);
        }
    }

    public Map<String, Object> getCustomKeys() {
        return customKeys;
    }

    /**
     * Resolve a reserved token to its live value.
     *
     * @param token one of the reserved tokens (cid, traceId, tracePath, spanId, parentSpanId, service, utc)
     * @param logTimeMillis the log event time, used for the per-line utc timestamp
     * @return resolved value, or null if absent (caller omits null keys from the output)
     */
    public Object token(String token, long logTimeMillis) {
        return switch (token) {
            case "cid" -> cid;
            case "traceId" -> trace.id;
            case "tracePath" -> trace.path;
            case "spanId" -> trace.spanId;
            case "parentSpanId" -> trace.parentSpanId;
            case "service" -> trace.route;
            case "utc" -> Utility.getInstance().date2str(new Date(logTimeMillis), true);
            default -> null;
        };
    }
}
