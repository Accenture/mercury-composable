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

package org.platformlambda.opentelemetry.support;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * An immutable {@link SpanData} reconstructed from Mercury's distributed-trace metrics.
 * <p>
 * Mercury has already produced the W3C-compatible trace ID, span ID and parent span ID during
 * execution (see {@code WorkerHandler}/{@code TraceInfo}), so this forwarder must emit a span that
 * carries those <em>exact</em> IDs - the standard OpenTelemetry {@code Tracer} API would mint new
 * ones and break the lineage. Building {@code SpanData} directly and handing it to an exporter is the
 * correct way to forward an already-completed span.
 */
public final class TraceMetricsSpanData implements SpanData {

    private static final Pattern TRACE_ID = Pattern.compile("[0-9a-f]{32}");
    private static final Pattern SPAN_ID = Pattern.compile("[0-9a-f]{16}");
    private static final String ZERO_TRACE = "00000000000000000000000000000000";
    private static final String ZERO_SPAN = "0000000000000000";
    private static final long NANOS_PER_MILLI = 1_000_000L;

    // metric keys (mirror WorkerHandler.getMetrics)
    private static final String TRACE = "trace";
    private static final String ANNOTATIONS = "annotations";
    private static final String ID = "id";
    private static final String SPAN = "span_id";
    private static final String PARENT_SPAN = "parent_span_id";
    private static final String SERVICE = "service";
    private static final String PATH = "path";
    private static final String FROM = "from";
    private static final String ORIGIN = "origin";
    private static final String START = "start";
    private static final String EXEC_TIME = "exec_time";
    private static final String ROUND_TRIP = "round_trip";
    private static final String STATUS = "status";
    private static final String SUCCESS = "success";
    private static final String EXCEPTION = "exception";
    private static final String HTTP_REQUEST = "http.request";

    private final String name;
    private final SpanKind kind;
    private final SpanContext spanContext;
    private final SpanContext parentContext;
    private final StatusData statusData;
    private final long startEpochNanos;
    private final long endEpochNanos;
    private final Attributes attributes;
    private final Resource resource;
    private final InstrumentationScopeInfo scope;

    private TraceMetricsSpanData(Map<String, Object> metrics, Map<String, Object> annotations,
                                 Resource resource, InstrumentationScopeInfo scope) {
        String traceId = str(metrics.get(ID));
        String parentSpanId = str(metrics.get(PARENT_SPAN));
        this.spanContext = SpanContext.create(
                traceId, str(metrics.get(SPAN)), TraceFlags.getSampled(), TraceState.getDefault());
        this.parentContext = validSpanId(parentSpanId)
                ? SpanContext.createFromRemoteParent(traceId, parentSpanId, TraceFlags.getSampled(), TraceState.getDefault())
                : SpanContext.getInvalid();
        this.startEpochNanos = epochNanos(str(metrics.get(START)));
        double execMs = toDouble(metrics.get(EXEC_TIME));
        this.endEpochNanos = startEpochNanos + (long) (execMs * NANOS_PER_MILLI);
        this.statusData = toBool(metrics.get(SUCCESS))
                ? StatusData.ok()
                : StatusData.create(StatusCode.ERROR, errorDescription(metrics));
        String service = str(metrics.get(SERVICE));
        String path = str(metrics.get(PATH));
        this.name = spanName(service, path);
        this.kind = HTTP_REQUEST.equals(str(metrics.get(FROM))) ? SpanKind.SERVER : SpanKind.INTERNAL;
        this.attributes = buildAttributes(metrics, annotations, service, path, execMs);
        this.resource = resource;
        this.scope = scope;
    }

    /**
     * Map one Mercury trace dataset ({@code {trace: {...}, annotations: {...}}}) to a span.
     *
     * @return the span, or {@code null} when the metrics lack a W3C-valid trace/span ID (cannot be
     *         represented as an OpenTelemetry span without forging IDs).
     */
    @SuppressWarnings("unchecked")
    public static SpanData map(Map<String, Object> dataset, Resource resource, InstrumentationScopeInfo scope) {
        if (dataset == null || !(dataset.get(TRACE) instanceof Map)) {
            return null;
        }
        Map<String, Object> metrics = (Map<String, Object>) dataset.get(TRACE);
        if (!validTraceId(str(metrics.get(ID))) || !validSpanId(str(metrics.get(SPAN)))) {
            return null;
        }
        Map<String, Object> annotations = dataset.get(ANNOTATIONS) instanceof Map
                ? (Map<String, Object>) dataset.get(ANNOTATIONS) : Map.of();
        return new TraceMetricsSpanData(metrics, annotations, resource, scope);
    }

    private static Attributes buildAttributes(Map<String, Object> metrics, Map<String, Object> annotations,
                                              String service, String path, double execMs) {
        AttributesBuilder ab = Attributes.builder();
        putStr(ab, "route", service);
        putStr(ab, FROM, metrics.get(FROM));
        putStr(ab, ORIGIN, metrics.get(ORIGIN));
        putStr(ab, PATH, path);
        if (metrics.get(STATUS) != null) {
            ab.put(STATUS, toLong(metrics.get(STATUS)));
        }
        ab.put("exec_time_ms", execMs);
        if (metrics.get(ROUND_TRIP) != null) {
            ab.put("round_trip_ms", toDouble(metrics.get(ROUND_TRIP)));
        }
        putStr(ab, EXCEPTION, metrics.get(EXCEPTION));
        for (Map.Entry<String, Object> e : annotations.entrySet()) {
            putStr(ab, "annotation." + e.getKey(), e.getValue());
        }
        return ab.build();
    }

    private static String errorDescription(Map<String, Object> m) {
        String ex = str(m.get(EXCEPTION));
        return ex != null ? ex : "status=" + m.get(STATUS);
    }

    private static String spanName(String service, String path) {
        if (service != null) {
            return service;
        }
        return path != null ? path : "task";
    }

    static boolean validTraceId(String id) {
        return id != null && TRACE_ID.matcher(id).matches() && !ZERO_TRACE.equals(id);
    }

    static boolean validSpanId(String id) {
        return id != null && SPAN_ID.matcher(id).matches() && !ZERO_SPAN.equals(id);
    }

    private static long epochNanos(String iso) {
        if (iso != null) {
            try {
                Instant t = Instant.parse(iso);
                return t.getEpochSecond() * 1_000_000_000L + t.getNano();
            } catch (DateTimeParseException ignore) {
                // fall through to "now"
            }
        }
        return System.currentTimeMillis() * NANOS_PER_MILLI;
    }

    private static void putStr(AttributesBuilder ab, String key, Object value) {
        if (value != null) {
            ab.put(key, String.valueOf(value));
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static double toDouble(Object o) {
        if (o == null) {
            return 0.0;
        }
        // Parse via the canonical string form rather than Number.doubleValue(). Mercury sends
        // exec_time / round_trip as float milliseconds (~3-decimal precision); widening a float
        // straight to double surfaces noise (e.g. 0.007 -> 0.007000000216066837). The string
        // round-trip preserves the value as authored.
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static long toLong(Object o) {
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return o == null ? 0L : Long.parseLong(String.valueOf(o));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static boolean toBool(Object o) {
        if (o instanceof Boolean b) {
            return b;
        }
        return o == null || Boolean.parseBoolean(String.valueOf(o));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SpanKind getKind() {
        return kind;
    }

    @Override
    public SpanContext getSpanContext() {
        return spanContext;
    }

    @Override
    public SpanContext getParentSpanContext() {
        return parentContext;
    }

    @Override
    public StatusData getStatus() {
        return statusData;
    }

    @Override
    public long getStartEpochNanos() {
        return startEpochNanos;
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    @Override
    public List<EventData> getEvents() {
        return Collections.emptyList();
    }

    @Override
    public List<LinkData> getLinks() {
        return Collections.emptyList();
    }

    @Override
    public long getEndEpochNanos() {
        return endEpochNanos;
    }

    @Override
    public boolean hasEnded() {
        return true;
    }

    @Override
    public int getTotalRecordedEvents() {
        return 0;
    }

    @Override
    public int getTotalRecordedLinks() {
        return 0;
    }

    @Override
    public int getTotalAttributeCount() {
        return attributes.size();
    }

    @Override
    @SuppressWarnings("deprecation")
    public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
        return InstrumentationLibraryInfo.create(scope.getName(), scope.getVersion());
    }

    @Override
    public InstrumentationScopeInfo getInstrumentationScopeInfo() {
        return scope;
    }

    @Override
    public Resource getResource() {
        return resource;
    }
}
