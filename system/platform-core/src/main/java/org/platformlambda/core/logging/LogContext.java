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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    /**
     * Every spelling a developer is refused: each reserved {@code $token} name AND its snake_case
     * form ({@code traceId} and {@code trace_id}, {@code parentSpanId} and {@code parent_span_id}, …).
     * <p>
     * Both are needed because the two vocabularies diverged. {@link #RESERVED_KEYS} holds the TOKEN
     * names, which are also what a template's right-hand side must name; the shipped templates now
     * emit snake_case output keys to match the distributed-trace block. Guarding only the token
     * spelling would let {@code updateContext("trace_id", …)} through to overwrite the real trace id
     * under the very name the default template publishes.
     * <p>
     * This is a fast, legible error for the two spellings that actually occur. It is not the
     * guarantee: an output key is the operator's free choice and no set can enumerate it, so
     * {@code LogContextConfig.render} also lets the template win over any developer key. The set
     * catches the likely mistake; the ordering makes shadowing impossible.
     */
    public static final Set<String> PROTECTED_KEYS = RESERVED_KEYS.stream()
            .flatMap(key -> Stream.of(key, toSnakeCase(key)))
            .collect(Collectors.toUnmodifiableSet());

    /** camelCase token name to its snake_case output form; single-word names are returned unchanged. */
    private static String toSnakeCase(String camel) {
        StringBuilder sb = new StringBuilder();
        for (char c : camel.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private final TraceInfo trace;
    private final String cid;
    // key order is not preserved; log aggregators (Dynatrace, Splunk, ...) reorder keys on display anyway
    private final ConcurrentMap<String, Object> customKeys = new ConcurrentHashMap<>();

    public LogContext(TraceInfo trace, String cid) {
        this.trace = trace;
        this.cid = cid;
    }

    /**
     * Is this key one a developer may not set? Both the {@code $token} spelling and its snake_case
     * output form are refused - see {@link #PROTECTED_KEYS}.
     *
     * @param key the candidate custom key
     * @return true when the key is protected in either spelling
     */
    public static boolean isReservedKey(String key) {
        return PROTECTED_KEYS.contains(key);
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
