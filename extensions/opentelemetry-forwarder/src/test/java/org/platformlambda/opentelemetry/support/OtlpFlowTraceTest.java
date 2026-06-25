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

import com.accenture.adapters.FlowExecutor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.Utility;
import org.platformlambda.opentelemetry.mock.MockOtlpCollector;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Level-2 (Event Script) validation. A two-task flow ({@code task.1 -> task.2}) is driven through
 * {@link FlowExecutor}; the engine produces a Level-1-style task-span chain <b>plus</b> one synthetic
 * {@code task.executor} flow-summary span. The real forwarder maps and exports all of them to the
 * composable mock automatically, and the RPC round-trip record (which carries no {@code span_id}) is
 * gracefully skipped by the mapper - this test confirms the forwarder handles the flow summary.
 */
class OtlpFlowTraceTest {

    @BeforeAll
    static void boot() throws InterruptedException {
        // The forwarder configures itself from application.properties (otel.exporter.otlp.endpoint
        // points at the in-process mock collector), so no manual bootstrap is needed here.
        TestBoot.start();
    }

    @Test
    @SuppressWarnings("unchecked")
    void eventScriptFlowForwardsTaskSpansAndFlowSummary() throws Exception {
        MockOtlpCollector.CAPTURED.clear();
        Utility util = Utility.getInstance();
        String traceId = util.getUuid();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("body", Map.of("hello", "world"));

        FlowExecutor.getInstance()
                .request("unit.test", traceId, "TEST /api/flow/chain", "otel-trace-test",
                        dataset, util.getUuid(), 8000)
                .get(8000, TimeUnit.MILLISECONDS);

        // collect this trace's spans by name: task.1, task.2, and the synthetic task.executor summary
        Map<String, Map<String, Object>> byName = new HashMap<>();
        long deadline = System.currentTimeMillis() + 10_000;
        while (!(byName.containsKey("task.1") && byName.containsKey("task.2") && byName.containsKey("task.executor"))
                && System.currentTimeMillis() < deadline) {
            Map<String, Object> rec = MockOtlpCollector.CAPTURED.poll(
                    Math.max(1, deadline - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
            if (rec != null && traceId.equals(rec.get("wire.trace_id"))) {
                byName.put((String) rec.get("wire.span_name"), rec);
            }
        }

        // Level-2 rides on Level-1: the two task spans, exactly like a Level-1 chain ...
        assertTrue(byName.containsKey("task.1"), "missing task.1 span");
        assertTrue(byName.containsKey("task.2"), "missing task.2 span");
        // ... plus the one synthetic flow summary the forwarder must also handle
        assertTrue(byName.containsKey("task.executor"), "missing the synthetic flow-summary span");

        byName.values().forEach(rec -> assertEquals(traceId, rec.get("wire.trace_id")));
        // task chain lineage is identical to Level-1: task.2 chains to task.1
        assertEquals(byName.get("task.1").get("wire.span_id"), byName.get("task.2").get("wire.parent_span_id"),
                "task.2 must chain to task.1");
        // the flow summary is annotated with the flow id (so it is unmistakably this flow's summary)
        Map<String, String> summaryAttrs = (Map<String, String>) byName.get("task.executor").get("wire.attributes");
        assertEquals("otel-trace-test", summaryAttrs.get("annotation.flow"),
                "the flow summary must carry the flow id annotation");
    }
}
