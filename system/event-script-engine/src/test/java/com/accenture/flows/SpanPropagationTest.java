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

package com.accenture.flows;

import com.accenture.adapters.FlowExecutor;
import com.accenture.setup.TestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.Utility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates OpenTelemetry span-id / parent-span-id propagation across the Event Script engine.
 * <p>
 * A function is registered at the "distributed.trace.forwarder" extension point so every telemetry
 * event produced by {@code Telemetry} is captured here. Flows are driven through {@link FlowExecutor}
 * with a unique traceId so the captured spans can be correlated deterministically.
 */
class SpanPropagationTest extends TestBase {

    private static final String FORWARDER = "distributed.trace.forwarder";
    private static final String TRACE = "trace";
    private static final String ANNOTATIONS = "annotations";
    private static final String ID = "id";
    private static final String SERVICE = "service";
    private static final String SPAN_ID = "span_id";
    private static final String PARENT_SPAN_ID = "parent_span_id";
    private static final String FLOW = "flow";
    private static final String TASK_EXECUTOR = "task.executor";
    private static final String BODY = "body";
    private static final String ORIGINATOR = "unit.test";
    private static final long FLOW_TIMEOUT = 8000;
    // a 16-char lowercase hex value with no dashes, per W3C Trace Context
    private static final String SPAN_FORMAT = "[0-9a-f]{16}";

    // traceId -> captured telemetry datasets (each holds "trace" metrics and optional "annotations")
    private static final ConcurrentMap<String, List<Map<String, Object>>> CAPTURED = new ConcurrentHashMap<>();

    @BeforeAll
    static void registerTraceForwarder() {
        Platform platform = Platform.getInstance();
        if (!platform.hasRoute(FORWARDER)) {
            LambdaFunction forwarder = (headers, body, instance) -> {
                if (body instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataset = (Map<String, Object>) body;
                    if (dataset.get(TRACE) instanceof Map<?, ?> metrics) {
                        Object traceId = metrics.get(ID);
                        if (traceId != null) {
                            CAPTURED.computeIfAbsent(traceId.toString(), k -> new CopyOnWriteArrayList<>())
                                    .add(dataset);
                        }
                    }
                }
                return null;
            };
            platform.registerPrivate(FORWARDER, forwarder, 1);
        }
    }

    @Test
    void sequentialFlowProducesLinearSpanChain()
            throws ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> dataset = new HashMap<>();
        dataset.put(BODY, new HashMap<>());
        dataset.put("path_parameter", Map.of("user", "alice"));
        dataset.put("query", Map.of("seq", 100));
        dataset.put("header", Map.of("accept", "application/json"));
        List<Span> spans = runFlow("sequential-test", dataset);
        assertSpanInvariants(spans);
        // exclude the synthetic flow summary - it is asserted separately
        List<Span> taskSpans = spans.stream().filter(s -> !TASK_EXECUTOR.equals(s.service)).toList();
        // the first task is the only root and starts the chain
        List<Span> roots = taskSpans.stream().filter(s -> s.parentSpanId == null).toList();
        assertEquals(1, roots.size(), "a sequential flow must have exactly one root task span");
        assertEquals("sequential.one", roots.getFirst().service);
        // a linear chain: no span is the parent of more than one task (no branching)
        Map<String, Long> childrenPerParent = new HashMap<>();
        for (Span s : taskSpans) {
            if (s.parentSpanId != null) {
                childrenPerParent.merge(s.parentSpanId, 1L, Long::sum);
            }
        }
        childrenPerParent.forEach((parent, count) ->
                assertEquals(1L, count, "sequential chain must not branch at span " + parent));
        // every task except the root is chained, so the chain visits every task span
        assertEquals(taskSpans.size() - 1, childrenPerParent.size(),
                "the chain must connect every sequential task span");
    }

    @Test
    void parallelForkSiblingsShareParentSpan()
            throws ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> dataset = new HashMap<>();
        dataset.put(BODY, new HashMap<>());
        List<Span> spans = runFlow("parallel-test", dataset);
        assertSpanInvariants(spans);
        // the two parallel branches both run the "parallel.task" function
        List<Span> forks = spans.stream().filter(s -> "parallel.task".equals(s.service)).toList();
        assertEquals(2, forks.size(), "parallel-test forks two tasks");
        assertNotNull(forks.getFirst().parentSpanId);
        // KEY assertion: concurrently dispatched siblings share the SAME parent span.
        // The per-task TaskReference anchor guarantees this even though each callback
        // is processed on its own virtual thread.
        assertEquals(forks.get(0).parentSpanId, forks.get(1).parentSpanId,
                "parallel sibling tasks must share the same parent span");
        // and that shared parent is the span of the fork originator
        Span fork = findSpan(spans, "begin.parallel.test");
        assertEquals(fork.spanId, forks.getFirst().parentSpanId,
                "the shared parent span must be the fork originator's span");
    }

    @Test
    void decisionFlowChainsToSelectedBranch()
            throws ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> dataset = new HashMap<>();
        dataset.put(BODY, new HashMap<>());
        dataset.put("query", Map.of("decision", 2));
        List<Span> spans = runFlow("numeric-decision-test", dataset);
        assertSpanInvariants(spans);
        Span decision = findSpan(spans, "numeric.decision");
        Span branch = findSpan(spans, "decision.case");
        assertNull(decision.parentSpanId, "the decision task is the flow root");
        assertEquals(decision.spanId, branch.parentSpanId,
                "the selected branch must chain to the decision task's span");
    }

    @Test
    void subflowInheritsParentFlowSpan()
            throws ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> dataset = new HashMap<>();
        dataset.put(BODY, new HashMap<>());
        dataset.put("path_parameter", Map.of("user", "test"));
        dataset.put("header", Map.of("accept", "application/json"));
        List<Span> spans = runFlow("parent-greetings", dataset);
        assertSpanInvariants(spans);
        // each flow emits a "task.executor" summary; distinguish them by the flow annotation
        List<Span> summaries = spans.stream().filter(s -> TASK_EXECUTOR.equals(s.service)).toList();
        Span parentSummary = summaries.stream().filter(s -> "parent-greetings".equals(s.flow))
                .findFirst().orElseThrow(() -> new AssertionError("missing parent flow summary"));
        Span childSummary = summaries.stream().filter(s -> "daughter-greetings".equals(s.flow))
                .findFirst().orElseThrow(() -> new AssertionError("missing sub-flow summary"));
        // the parent flow is the root (driven internally with no inbound span)
        assertNull(parentSummary.parentSpanId);
        // the sub-flow's parent span must be a span produced by the parent flow (cross-flow lineage)
        assertNotNull(childSummary.parentSpanId);
        Set<String> allSpanIds = new HashSet<>();
        spans.forEach(s -> allSpanIds.add(s.spanId));
        assertTrue(allSpanIds.contains(childSummary.parentSpanId),
                "the sub-flow's parent span must exist within the same trace");
        // specifically, it is the parent flow's first task (the no.op that dispatched the sub-flow)
        Span dispatcher = spans.stream()
                .filter(s -> "no.op".equals(s.service) && s.parentSpanId == null)
                .findFirst().orElseThrow(() -> new AssertionError("missing parent flow dispatcher task"));
        assertEquals(dispatcher.spanId, childSummary.parentSpanId,
                "the sub-flow must chain to the parent flow task that dispatched it");
    }

    // ----- helpers -----

    private List<Span> runFlow(String flowId, Map<String, Object> dataset)
            throws ExecutionException, InterruptedException, TimeoutException {
        Utility util = Utility.getInstance();
        String traceId = util.getUuid();
        CAPTURED.remove(traceId);
        FlowExecutor.getInstance()
                .request(ORIGINATOR, traceId, "TEST /" + flowId, flowId, dataset, util.getUuid(), FLOW_TIMEOUT)
                .get(FLOW_TIMEOUT, TimeUnit.MILLISECONDS);
        return toSpans(awaitStableCapture(traceId));
    }

    /**
     * Telemetry is forwarded asynchronously, so wait until the captured-span count
     * for a trace stops growing before asserting against it.
     */
    private List<Map<String, Object>> awaitStableCapture(String traceId) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + FLOW_TIMEOUT;
        int lastSize = -1;
        long stableSince = System.currentTimeMillis();
        while (System.currentTimeMillis() < deadline) {
            int size = CAPTURED.getOrDefault(traceId, List.of()).size();
            if (size > 0 && size == lastSize && System.currentTimeMillis() - stableSince >= 300) {
                break;
            }
            if (size != lastSize) {
                lastSize = size;
                stableSince = System.currentTimeMillis();
            }
            //noinspection BusyWait
            Thread.sleep(50);
        }
        List<Map<String, Object>> captured = CAPTURED.get(traceId);
        assertNotNull(captured, "no telemetry captured for trace " + traceId);
        return new ArrayList<>(captured);
    }

    @SuppressWarnings("unchecked")
    private List<Span> toSpans(List<Map<String, Object>> datasets) {
        List<Span> spans = new ArrayList<>();
        for (Map<String, Object> dataset : datasets) {
            Map<String, Object> metrics = (Map<String, Object>) dataset.get(TRACE);
            // Skip RPC round-trip client traces (identified by "round_trip"): they are the caller's
            // own measurement of a call and carry no span_id, so they are not nodes in the span tree.
            if (metrics.get(SPAN_ID) == null) {
                continue;
            }
            Map<String, Object> annotations = dataset.get(ANNOTATIONS) instanceof Map
                    ? (Map<String, Object>) dataset.get(ANNOTATIONS) : Map.of();
            spans.add(new Span(str(metrics.get(SERVICE)), str(metrics.get(SPAN_ID)),
                    str(metrics.get(PARENT_SPAN_ID)), str(annotations.get(FLOW))));
        }
        return spans;
    }

    /**
     * Invariants that must hold for every captured span regardless of flow topology.
     */
    private void assertSpanInvariants(List<Span> spans) {
        assertFalse(spans.isEmpty(), "expected captured spans");
        Set<String> spanIds = new HashSet<>();
        for (Span s : spans) {
            assertNotNull(s.spanId, "missing span_id for " + s.service);
            assertTrue(s.spanId.matches(SPAN_FORMAT), "span_id is not 16-char hex: " + s.spanId);
            assertTrue(spanIds.add(s.spanId), "duplicate span_id " + s.spanId + " for " + s.service);
            if (s.parentSpanId != null) {
                assertTrue(s.parentSpanId.matches(SPAN_FORMAT),
                        "parent_span_id is not 16-char hex: " + s.parentSpanId);
                assertNotEquals(s.spanId, s.parentSpanId, "a span cannot be its own parent: " + s.service);
            }
        }
        // every non-root span must point to a parent that exists within the same trace
        for (Span s : spans) {
            if (s.parentSpanId != null) {
                assertTrue(spanIds.contains(s.parentSpanId),
                        "dangling parent_span_id " + s.parentSpanId + " for " + s.service);
            }
        }
    }

    private Span findSpan(List<Span> spans, String service) {
        return spans.stream().filter(s -> service.equals(s.service)).findFirst()
                .orElseThrow(() -> new AssertionError("no span captured for service " + service));
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private record Span(String service, String spanId, String parentSpanId, String flow) { }
}
