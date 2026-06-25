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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.platformlambda.opentelemetry.mock.MockOtlpCollector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full-pipeline integration test. A traced RPC chain {@code unit.test -> fun.1 -> fun.2 -> fun.3}
 * makes the framework emit three linked spans; the <b>real</b> {@code OpenTelemetryForwarder}
 * (auto-registered at {@code distributed.trace.forwarder}) maps and forwards each to the composable
 * mock collector over OTLP automatically. The test asserts the spans arrived with the right trace ID
 * and parent-child lineage.
 */
class OtlpTracePipelineTest {

    @BeforeAll
    static void boot() throws InterruptedException {
        // The forwarder configures itself from application.properties (otel.exporter.otlp.endpoint
        // points at the in-process mock collector), so no manual bootstrap is needed here.
        TestBoot.start();
    }

    @Test
    void tracedRpcChainProducesLinkedSpans() throws InterruptedException {
        MockOtlpCollector.CAPTURED.clear();
        String traceId = Utility.getInstance().getUuid();   // 32-hex, W3C-compatible
        PostOffice po = new PostOffice("unit.test", traceId, "TEST /api/trace/chain");
        po.send("fun.1", Map.of("hello", "world"));

        // collect the three spans (fun.1 -> fun.2 -> fun.3), keyed by span name, ignoring other traces
        Map<String, Map<String, Object>> byName = new HashMap<>();
        long deadline = System.currentTimeMillis() + 10_000;
        while (byName.size() < 3 && System.currentTimeMillis() < deadline) {
            Map<String, Object> rec = MockOtlpCollector.CAPTURED.poll(
                    Math.max(1, deadline - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
            if (rec != null && traceId.equals(rec.get("wire.trace_id"))) {
                byName.put((String) rec.get("wire.span_name"), rec);
            }
        }

        assertEquals(Set.of("fun.1", "fun.2", "fun.3"), byName.keySet(),
                "the traced chain should forward exactly three spans to the OTLP mock");
        // every span belongs to the same trace we started
        byName.values().forEach(rec -> assertEquals(traceId, rec.get("wire.trace_id")));

        String span1 = (String) byName.get("fun.1").get("wire.span_id");
        String span2 = (String) byName.get("fun.2").get("wire.span_id");
        String span3 = (String) byName.get("fun.3").get("wire.span_id");
        assertNotEquals(span1, span2);
        assertNotEquals(span2, span3);

        // parent-child lineage: root(fun.1) <- fun.2 <- fun.3
        assertEquals(span1, byName.get("fun.2").get("wire.parent_span_id"), "fun.2's parent must be fun.1");
        assertEquals(span2, byName.get("fun.3").get("wire.parent_span_id"), "fun.3's parent must be fun.2");
        String rootParent = (String) byName.get("fun.1").get("wire.parent_span_id");
        assertTrue(rootParent == null || rootParent.isEmpty() || "0000000000000000".equals(rootParent),
                "fun.1 is the root span (no in-chain parent), got: " + rootParent);
    }
}
