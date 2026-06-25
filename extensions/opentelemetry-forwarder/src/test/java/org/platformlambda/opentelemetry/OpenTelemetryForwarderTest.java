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

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.Test;
import org.platformlambda.opentelemetry.support.OtelForwarderContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenTelemetryForwarderTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID = "00f067aa0ba902b7";

    private Map<String, Object> dataset() {
        Map<String, Object> trace = new HashMap<>();
        trace.put("id", TRACE_ID);
        trace.put("span_id", SPAN_ID);
        trace.put("service", "graph.executor");
        trace.put("start", "2026-06-24T10:00:00Z");
        trace.put("exec_time", 5.0);
        trace.put("success", true);
        Map<String, Object> ds = new HashMap<>();
        ds.put("trace", trace);
        return ds;
    }

    @Test
    void noArgConstructorConfiguresFromProperties() {
        // the production entry point that @PreLoad invokes reflectively; reads test application.properties
        OpenTelemetryForwarder forwarder = new OpenTelemetryForwarder();
        assertNotNull(forwarder);
        forwarder.handleEvent(Map.of(), "non-map-is-a-no-op", 1);
    }

    @Test
    void contextReportsEnabledFlag() {
        assertTrue(new OtelForwarderContext(true, InMemorySpanExporter.create(), "x").isEnabled());
        assertFalse(new OtelForwarderContext(false, null, "x").isEnabled());
    }

    @Test
    @SuppressWarnings("unchecked")
    void forwardIsSafeNoOpWhenUnmappableOrNoExporter() {
        InMemorySpanExporter mem = InMemorySpanExporter.create();
        // a dataset that cannot map to a span (no span_id) must export nothing
        Map<String, Object> noSpan = dataset();
        ((Map<String, Object>) noSpan.get("trace")).remove("span_id");
        new OtelForwarderContext(true, mem, "x").forward(noSpan);
        assertTrue(mem.getFinishedSpanItems().isEmpty());
        // enabled but no exporter configured -> no-op, no NPE
        new OtelForwarderContext(true, null, "x").forward(dataset());
    }

    @Test
    void exportsSpanThroughTheForwarderFunction() {
        InMemorySpanExporter mem = InMemorySpanExporter.create();
        OpenTelemetryForwarder forwarder = new OpenTelemetryForwarder(new OtelForwarderContext(true, mem, "unit-test"));

        forwarder.handleEvent(Map.of(), dataset(), 1);

        List<SpanData> spans = mem.getFinishedSpanItems();
        assertEquals(1, spans.size());
        assertEquals(TRACE_ID, spans.getFirst().getTraceId());
        assertEquals(SPAN_ID, spans.getFirst().getSpanId());
        assertEquals("graph.executor", spans.getFirst().getName());
        assertEquals("unit-test", spans.getFirst().getResource().getAttributes().get(
                io.opentelemetry.api.common.AttributeKey.stringKey("service.name")));
    }

    @Test
    void disabledForwarderExportsNothing() {
        InMemorySpanExporter mem = InMemorySpanExporter.create();
        OpenTelemetryForwarder forwarder = new OpenTelemetryForwarder(new OtelForwarderContext(false, mem, "unit-test"));

        forwarder.handleEvent(Map.of(), dataset(), 1);

        assertTrue(mem.getFinishedSpanItems().isEmpty());
    }

    @Test
    void nonMapInputIsIgnored() {
        InMemorySpanExporter mem = InMemorySpanExporter.create();
        OpenTelemetryForwarder forwarder = new OpenTelemetryForwarder(new OtelForwarderContext(true, mem, "unit-test"));

        forwarder.handleEvent(Map.of(), "not-a-map", 1);

        assertTrue(mem.getFinishedSpanItems().isEmpty());
    }
}
