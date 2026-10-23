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

package org.platformlambda.core.util;

import java.util.regex.Pattern;

/**
 * Helper for W3C Trace Context "traceparent" propagation across HTTP boundaries.
 * <p>
 * Format: {@code version-trace-id-parent-id-trace-flags}<br>
 * Example: {@code 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01}
 * <p>
 * The trace-id is 16 bytes (32 hex) and the parent-id - the caller's span - is 8 bytes (16 hex).
 * These match Mercury's auto-generated trace ID and span ID formats, so they propagate directly.
 */
public class W3cTrace {
    public static final String TRACEPARENT = "traceparent";
    private static final String VERSION = "00";
    private static final String SAMPLED = "01";
    private static final String SEPARATOR = "-";
    private static final Pattern TRACE_ID = Pattern.compile("[0-9a-f]{32}");
    private static final Pattern SPAN_ID = Pattern.compile("[0-9a-f]{16}");
    private static final String ZERO_TRACE = "00000000000000000000000000000000";
    private static final String ZERO_SPAN = "0000000000000000";

    private W3cTrace() { }

    /**
     * Build a W3C traceparent header value from a trace ID and the current span ID.
     *
     * @param traceId 32-char lowercase hex trace ID
     * @param spanId 16-char lowercase hex span ID (becomes the downstream's parent span)
     * @return the traceparent value, or null if either ID is not W3C-compatible
     */
    public static String format(String traceId, String spanId) {
        if (validTraceId(traceId) && validSpanId(spanId)) {
            return VERSION + SEPARATOR + traceId + SEPARATOR + spanId + SEPARATOR + SAMPLED;
        }
        return null;
    }

    /**
     * Parse a W3C traceparent header value.
     *
     * @param traceparent header value
     * @return a 2-element array of {trace-id, parent-span-id}, or an empty array if the value is invalid
     */
    public static String[] parse(String traceparent) {
        if (traceparent != null) {
            // trace-id and parent-id are always the 2nd and 3rd fields across all versions
            var parts = traceparent.trim().split(SEPARATOR);
            if (parts.length >= 4 && validTraceId(parts[1]) && validSpanId(parts[2])) {
                return new String[] { parts[1], parts[2] };
            }
        }
        return new String[0];
    }

    private static boolean validTraceId(String id) {
        return id != null && TRACE_ID.matcher(id).matches() && !ZERO_TRACE.equals(id);
    }

    private static boolean validSpanId(String id) {
        return id != null && SPAN_ID.matcher(id).matches() && !ZERO_SPAN.equals(id);
    }
}
