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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.opentelemetry.mock.MockOtlpCollector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the real OTLP/HTTP exporter against the composable mock collector (a Mercury function behind
 * {@code rest.yaml}, booted via {@code AutoStart}), verifying the endpoint is reached and the backend
 * credential header actually arrives.
 */
class OtlpComposableExportTest {

    private static int port;

    @BeforeAll
    static void boot() throws InterruptedException {
        TestBoot.start();
        port = Utility.getInstance().str2int(AppConfigReader.getInstance().getProperty("server.port", "8299"));
    }

    private SpanData sampleSpan() {
        Map<String, Object> trace = new HashMap<>();
        trace.put("id", "4bf92f3577b34da6a3ce929d0e0e4736");
        trace.put("span_id", "00f067aa0ba902b7");
        trace.put("parent_span_id", "a3ce929d0e0e4736");
        trace.put("service", "hello.world");
        trace.put("start", "2026-06-24T10:00:00Z");
        trace.put("exec_time", 1.0);
        trace.put("success", true);
        Map<String, Object> ds = new HashMap<>();
        ds.put("trace", trace);
        Resource resource = Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), "mercury-otel-demo"));
        return TraceMetricsSpanData.map(ds, resource, InstrumentationScopeInfo.create("test"));
    }

    @Test
    void exporterReachesBothBackendPaths() throws Exception {
        // the mock collector is wired to both ingest paths in rest.yaml
        assertCollectorReceives("/api/v2/otlp/v1/traces");  // Dynatrace-style
        assertCollectorReceives("/v2/trace/otlp");          // Splunk Observability-style
    }

    private void assertCollectorReceives(String path) throws Exception {
        MockOtlpCollector.CAPTURED.clear();
        String endpoint = "http://127.0.0.1:" + port + path;
        try (SpanExporter exporter = OtelForwarderContext.buildExporter(
                endpoint, 5000, Map.of("Authorization", "Api-Token dt0c01.SECRET"))) {
            CompletableResultCode rc = exporter.export(List.of(sampleSpan()));
            rc.join(10, TimeUnit.SECONDS);

            Map<String, Object> captured = MockOtlpCollector.CAPTURED.poll(10, TimeUnit.SECONDS);
            assertNotNull(captured, "the composable mock collector should have received POST " + path);
            assertEquals("POST", captured.get("method"));
            assertEquals(path, captured.get("path"));
            assertEquals("Api-Token dt0c01.SECRET", captured.get("authorization"),
                    "the backend credential header must reach the collector");
            // round-trip proof: the IDs we mapped survived OTLP protobuf serialization over the wire
            assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", captured.get("wire.trace_id"));
            assertEquals("00f067aa0ba902b7", captured.get("wire.span_id"));
            assertEquals("a3ce929d0e0e4736", captured.get("wire.parent_span_id"));
            assertEquals("hello.world", captured.get("wire.span_name"));
            assertEquals("mercury-otel-demo", captured.get("wire.service.name"));
        }
    }

    @Test
    void gzipCompressionIsAppliedOnTheWire() throws Exception {
        // otel.exporter.otlp.compression=gzip must make the exporter gzip the request body. The mock's
        // HTTP layer doesn't surface a compressed body to the function (getBody() is null for a gzip
        // request), so we prove the knob by the Content-Encoding header the exporter set on the wire -
        // a real collector inflates that body natively.
        MockOtlpCollector.CAPTURED.clear();
        String endpoint = "http://127.0.0.1:" + port + "/api/v2/otlp/v1/traces";
        try (SpanExporter exporter = OtelForwarderContext.buildExporter(
                endpoint, 5000, 5000, "gzip", Map.of("Authorization", "Api-Token dt0c01.SECRET"))) {
            CompletableResultCode rc = exporter.export(List.of(sampleSpan()));
            rc.join(10, TimeUnit.SECONDS);
            assertTrue(rc.isSuccess(), "gzip-compressed OTLP export should be accepted (HTTP 200)");

            Map<String, Object> captured = MockOtlpCollector.CAPTURED.poll(10, TimeUnit.SECONDS);
            assertNotNull(captured, "the collector should have received the gzip-compressed POST");
            assertEquals("gzip", captured.get("content-encoding"),
                    "the compression setting must gzip the request body on the wire");
            assertEquals("Api-Token dt0c01.SECRET", captured.get("authorization"),
                    "credential headers still reach the collector when compression is on");
        }
    }

    @Test
    void parsesCredentialHeaders() {
        // value may itself contain '=' (e.g. base64) - split only on the first '='
        Map<String, String> h = OtelForwarderContext.parseHeaders("Authorization=Api-Token dt0c01.ABC=,X-SF-Token=xyz");
        assertEquals("Api-Token dt0c01.ABC=", h.get("Authorization"));
        assertEquals("xyz", h.get("X-SF-Token"));
        assertTrue(OtelForwarderContext.parseHeaders(null).isEmpty());
        assertTrue(OtelForwarderContext.parseHeaders("  ").isEmpty());
    }
}
