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

package org.platformlambda.opentelemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.Test;
import org.platformlambda.opentelemetry.support.TraceMetricsSpanData;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test casts the dataset's Object value to Map (erasure -> unchecked) and intentionally exercises the
// deprecated-but-mandatory SpanData.getInstrumentationLibraryInfo(); both are expected in this test.
@SuppressWarnings({"unchecked", "deprecation"})
class TraceMetricsSpanDataTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID = "00f067aa0ba902b7";
    private static final String PARENT_SPAN_ID = "a3ce929d0e0e4736";
    private static final Resource RESOURCE = Resource.getDefault();
    private static final InstrumentationScopeInfo SCOPE = InstrumentationScopeInfo.create("test");

    private Map<String, Object> dataset(boolean success) {
        Map<String, Object> trace = new HashMap<>();
        trace.put("id", TRACE_ID);
        trace.put("span_id", SPAN_ID);
        trace.put("parent_span_id", PARENT_SPAN_ID);
        trace.put("service", "hello.world");
        trace.put("path", "/api/hello");
        trace.put("from", "http.request");
        trace.put("origin", "node-1");
        trace.put("start", "2026-06-24T10:00:00Z");
        trace.put("exec_time", 12.5);
        trace.put("status", 200);
        trace.put("success", success);
        if (!success) {
            trace.put("exception", "boom");
        }
        Map<String, Object> ds = new HashMap<>();
        ds.put("trace", trace);
        ds.put("annotations", Map.of("user", "alice"));
        return ds;
    }

    @Test
    void mapsIdsTimingAndAttributes() {
        SpanData span = TraceMetricsSpanData.map(dataset(true), RESOURCE, SCOPE);
        assertNotNull(span);
        // exact W3C IDs preserved (the whole point of a forwarder)
        assertEquals(TRACE_ID, span.getTraceId());
        assertEquals(SPAN_ID, span.getSpanId());
        assertTrue(span.getParentSpanContext().isValid());
        assertEquals(PARENT_SPAN_ID, span.getParentSpanContext().getSpanId());
        // name + kind
        assertEquals("hello.world", span.getName());
        assertEquals(SpanKind.SERVER, span.getKind());
        // duration = exec_time (12.5 ms) in nanos
        assertEquals(12_500_000L, span.getEndEpochNanos() - span.getStartEpochNanos());
        // status
        assertEquals(StatusCode.OK, span.getStatus().getStatusCode());
        // attributes
        assertEquals("/api/hello", span.getAttributes().get(AttributeKey.stringKey("path")));
        assertEquals("hello.world", span.getAttributes().get(AttributeKey.stringKey("route")));
        assertEquals(Long.valueOf(200), span.getAttributes().get(AttributeKey.longKey("status")));
        assertEquals("alice", span.getAttributes().get(AttributeKey.stringKey("annotation.user")));
        assertTrue(span.hasEnded());
        assertEquals(RESOURCE, span.getResource());
    }

    @Test
    void failedTraceBecomesErrorStatus() {
        SpanData span = TraceMetricsSpanData.map(dataset(false), RESOURCE, SCOPE);
        assertNotNull(span);
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
        assertEquals("boom", span.getStatus().getDescription());
    }

    @Test
    void invalidOrMissingIdsYieldNull() {
        // not a 32-hex trace id
        Map<String, Object> bad = dataset(true);
        ((Map<String, Object>) bad.get("trace")).put("id", "not-a-valid-trace-id");
        assertNull(TraceMetricsSpanData.map(bad, RESOURCE, SCOPE));

        // missing span id
        Map<String, Object> noSpan = dataset(true);
        ((Map<String, Object>) noSpan.get("trace")).remove("span_id");
        assertNull(TraceMetricsSpanData.map(noSpan, RESOURCE, SCOPE));

        // no trace block at all
        assertNull(TraceMetricsSpanData.map(Map.of("annotations", Map.of()), RESOURCE, SCOPE));
    }

    @Test
    void rootSpanHasNoParent() {
        Map<String, Object> ds = dataset(true);
        ((Map<String, Object>) ds.get("trace")).remove("parent_span_id");
        SpanData span = TraceMetricsSpanData.map(ds, RESOURCE, SCOPE);
        assertNotNull(span);
        assertFalse(span.getParentSpanContext().isValid());
    }

    @Test
    void floatExecTimeKeepsThreeDecimalPrecision() {
        // Mercury sends exec_time / round_trip as float ms; widening a float straight to double
        // would surface noise (0.007 -> 0.007000000216066837). The mapper must preserve 0.007.
        Map<String, Object> ds = dataset(true);
        ((Map<String, Object>) ds.get("trace")).put("exec_time", 0.007f);
        SpanData span = TraceMetricsSpanData.map(ds, RESOURCE, SCOPE);
        Double execMs = span.getAttributes().get(AttributeKey.doubleKey("exec_time_ms"));
        assertEquals(0.007, execMs);
        assertEquals("0.007", String.valueOf(execMs));
    }

    @Test
    void handlesStringTypedAndOptionalMetrics() {
        // serialization may deliver numbers/booleans as strings; round_trip is optional
        Map<String, Object> ds = dataset(true);
        Map<String, Object> trace = (Map<String, Object>) ds.get("trace");
        trace.put("exec_time", "2.5");     // string double
        trace.put("status", "200");        // string int
        trace.put("success", "true");      // string boolean
        trace.put("round_trip", 3.75);     // optional metric
        SpanData span = TraceMetricsSpanData.map(ds, RESOURCE, SCOPE);
        assertNotNull(span);
        assertEquals(2_500_000L, span.getEndEpochNanos() - span.getStartEpochNanos());
        assertEquals(Long.valueOf(200), span.getAttributes().get(AttributeKey.longKey("status")));
        assertEquals(3.75, span.getAttributes().get(AttributeKey.doubleKey("round_trip_ms")));
        assertEquals(StatusCode.OK, span.getStatus().getStatusCode());
    }

    @Test
    void invalidNumbersFallBackToDefaults() {
        Map<String, Object> ds = dataset(true);
        Map<String, Object> trace = (Map<String, Object>) ds.get("trace");
        trace.put("exec_time", "not-a-number");
        trace.put("status", "xyz");
        SpanData span = TraceMetricsSpanData.map(ds, RESOURCE, SCOPE);
        assertNotNull(span);
        assertEquals(0L, span.getEndEpochNanos() - span.getStartEpochNanos());
        assertEquals(Long.valueOf(0), span.getAttributes().get(AttributeKey.longKey("status")));
    }

    @Test
    void errorWithoutExceptionUsesStatusDescription() {
        Map<String, Object> ds = dataset(true);
        Map<String, Object> trace = (Map<String, Object>) ds.get("trace");
        trace.put("success", false);
        trace.put("status", 500);          // no exception present
        SpanData span = TraceMetricsSpanData.map(ds, RESOURCE, SCOPE);
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
        assertEquals("status=500", span.getStatus().getDescription());
    }

    @Test
    void spanNameFallsBackToPathThenTask() {
        Map<String, Object> ds = dataset(true);
        Map<String, Object> trace = (Map<String, Object>) ds.get("trace");
        trace.remove("service");           // no service -> name = path
        assertEquals("/api/hello", TraceMetricsSpanData.map(ds, RESOURCE, SCOPE).getName());
        trace.remove("path");              // neither -> "task"
        assertEquals("task", TraceMetricsSpanData.map(ds, RESOURCE, SCOPE).getName());
    }

    @Test
    void allZeroIdsAreRejected() {
        Map<String, Object> ds = dataset(true);
        ((Map<String, Object>) ds.get("trace")).put("id", "00000000000000000000000000000000");
        assertNull(TraceMetricsSpanData.map(ds, RESOURCE, SCOPE));
    }

    @Test
    void exposesInstrumentationScopeAndLibraryInfo() {
        SpanData span = TraceMetricsSpanData.map(dataset(true), RESOURCE, SCOPE);
        assertEquals(SCOPE, span.getInstrumentationScopeInfo());
        assertEquals(SCOPE.getName(), span.getInstrumentationLibraryInfo().getName());
    }
}
