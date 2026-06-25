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

package com.accenture.minigraph.tutorials;

import com.accenture.minigraph.start.PlaygroundLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates OpenTelemetry span-id / parent-span-id propagation through the {@code graph.executor}.
 * <p>
 * The graph executor is the graph-layer analog of the Event Script task executor: it dispatches
 * graph node "skills" and must stamp the correct parent span on each so a distributed trace can
 * reconstruct node lineage. A function is registered at the "distributed.trace.forwarder"
 * extension point to capture telemetry; graphs are driven over HTTP with a unique traceId so the
 * captured spans can be correlated deterministically (the traceId crosses the HTTP boundary via
 * the X-Trace-Id header).
 */
class GraphSpanPropagationTest {

    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final String FORWARDER = "distributed.trace.forwarder";
    private static final String GRAPH_EXECUTOR = "graph.executor";
    private static final String TASK_EXECUTOR = "task.executor";
    private static final String HTTP_ADAPTER = "http.flow.adapter";
    private static final String HTTP_RESPONSE = "async.http.response";
    private static final String TRACE = "trace";
    private static final String ANNOTATIONS = "annotations";
    private static final String ID = "id";
    private static final String SERVICE = "service";
    private static final String SPAN_ID = "span_id";
    private static final String PARENT_SPAN_ID = "parent_span_id";
    private static final String FROM = "from";
    private static final String NODE = "node";
    private static final long TIMEOUT = 8000;
    private static final String SPAN_FORMAT = "[0-9a-f]{16}";

    private static String target;

    // traceId -> captured telemetry datasets (each holds "trace" metrics and optional "annotations")
    private static final ConcurrentMap<String, List<Map<String, Object>>> CAPTURED = new ConcurrentHashMap<>();

    @BeforeAll
    static void setup() {
        PlaygroundLoader.main(new String[0]);
        var config = AppConfigReader.getInstance();
        target = "http://localhost:" + config.getProperty("rest.server.port");
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
    void parallelGraphNodesShareParentSpan() throws TimeoutException {
        // tutorial-3 forks from the root node to "dictionary" and "fetcher" (parallel branches)
        List<Span> spans = runGraph(3, Map.of("person_id", 100));
        assertSpanInvariants(spans);
        List<Span> nodes = graphNodeSpans(spans);
        assertEquals(2, nodes.size(), "tutorial-3 dispatches two graph nodes");
        // THE FIX: every node dispatched by graph.executor carries a parent span
        nodes.forEach(n -> assertNotNull(n.parentSpanId,
                "graph node " + n.service + " must carry a parent span"));
        // parallel siblings share the same parent (concurrency-correct, like an event-script fork)
        assertEquals(nodes.get(0).parentSpanId, nodes.get(1).parentSpanId,
                "parallel graph nodes must share the same parent span");
        // the shared parent is the span that entered the graph (the HTTP flow adapter)
        Span adapter = findFirst(spans, HTTP_ADAPTER, null);
        assertEquals(adapter.spanId, nodes.getFirst().parentSpanId,
                "parallel nodes must chain from the graph's entry span");
        // the graph's final response chains back to one of the graph nodes
        Span graphResponse = findFirst(spans, HTTP_RESPONSE, TASK_EXECUTOR);
        assertTrue(spanIds(nodes).contains(graphResponse.parentSpanId),
                "the graph response must chain to a graph node");
    }

    @Test
    void downstreamHttpServiceChainsToCallerSpanViaTraceparent() throws TimeoutException {
        // tutorial-3's "fetcher" makes an outbound HTTP call to the mock MDM endpoint.
        // The W3C "traceparent" header must carry the fetcher's span across the HTTP boundary
        // so the downstream server span chains back to it (platform-core inject + extract).
        List<Span> spans = runGraph(3, Map.of("person_id", 100));
        assertSpanInvariants(spans);
        Span fetcher = findFirst(spans, "graph.api.fetcher", GRAPH_EXECUTOR);
        Span downstream = findFirst(spans, "mock.mdm.profile", null);
        assertEquals(fetcher.spanId, downstream.parentSpanId,
                "the downstream HTTP service span must chain to the fetcher via W3C traceparent");
    }

    @Test
    void sequentialGraphNodesChain() throws TimeoutException {
        // tutorial-4 is a linear decision graph: decision -> less-than -> end
        List<Span> spans = runGraph(4, Map.of("a", 100, "b", 200));
        assertSpanInvariants(spans);
        List<Span> nodes = graphNodeSpans(spans);
        assertEquals(3, nodes.size(), "tutorial-4 dispatches three graph nodes");
        nodes.forEach(n -> assertNotNull(n.parentSpanId,
                "graph node " + n.service + " must carry a parent span"));
        Span adapter = findFirst(spans, HTTP_ADAPTER, null);
        Set<String> nodeIds = spanIds(nodes);
        // exactly one node roots at the graph entry; the remaining nodes chain node-to-node
        long rootedAtEntry = nodes.stream().filter(n -> adapter.spanId.equals(n.parentSpanId)).count();
        assertEquals(1, rootedAtEntry, "exactly one graph node should chain from the graph entry span");
        long chainedToNode = nodes.stream().filter(n -> nodeIds.contains(n.parentSpanId)).count();
        assertEquals(2, chainedToNode, "the remaining graph nodes must chain node-to-node");
        // a linear chain does not branch: no node span is the parent of more than one node
        Map<String, Long> childrenPerParent = new HashMap<>();
        for (Span n : nodes) {
            if (nodeIds.contains(n.parentSpanId)) {
                childrenPerParent.merge(n.parentSpanId, 1L, Long::sum);
            }
        }
        childrenPerParent.forEach((parent, count) ->
                assertEquals(1L, count, "the node chain must not branch at span " + parent));
        // the final response chains to the last graph node
        Span graphResponse = findFirst(spans, HTTP_RESPONSE, TASK_EXECUTOR);
        assertTrue(nodeIds.contains(graphResponse.parentSpanId),
                "the graph response must chain to a graph node");
    }

    // ----- helpers -----

    private List<Span> runGraph(int chapter, Map<String, Object> input) throws TimeoutException {
        String traceId = Utility.getInstance().getUuid();
        CAPTURED.remove(traceId);
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setBody(input).setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json").setUrl("/api/graph/tutorial-" + chapter);
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request);
        var po = PostOffice.trackable("unit.test", traceId, "TEST /span/tutorial-" + chapter);
        po.asyncRequest(event, TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
        return toSpans(awaitStableCapture(traceId));
    }

    private List<Map<String, Object>> awaitStableCapture(String traceId) {
        final long deadline = System.currentTimeMillis() + TIMEOUT;
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
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
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
            // Skip RPC round-trip client traces (they carry "round_trip" and no span_id);
            // they are the caller's own measurement, not a node in the span tree.
            if (metrics.get(SPAN_ID) == null) {
                continue;
            }
            Map<String, Object> annotations = dataset.get(ANNOTATIONS) instanceof Map
                    ? (Map<String, Object>) dataset.get(ANNOTATIONS) : Map.of();
            spans.add(new Span(str(metrics.get(SERVICE)), str(metrics.get(SPAN_ID)),
                    str(metrics.get(PARENT_SPAN_ID)), str(metrics.get(FROM)), str(annotations.get(NODE))));
        }
        return spans;
    }

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
        // every present parent must resolve to a span within the same trace
        for (Span s : spans) {
            if (s.parentSpanId != null) {
                assertTrue(spanIds.contains(s.parentSpanId),
                        "dangling parent_span_id " + s.parentSpanId + " for " + s.service);
            }
        }
    }

    private List<Span> graphNodeSpans(List<Span> spans) {
        return spans.stream().filter(s -> GRAPH_EXECUTOR.equals(s.from)).toList();
    }

    private Span findFirst(List<Span> spans, String service, String from) {
        return spans.stream()
                .filter(s -> service.equals(s.service) && (from == null || from.equals(s.from)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no span captured for service " + service
                        + (from == null ? "" : " from " + from)));
    }

    private Set<String> spanIds(List<Span> spans) {
        Set<String> ids = new HashSet<>();
        spans.forEach(s -> ids.add(s.spanId));
        return ids;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private record Span(String service, String spanId, String parentSpanId, String from, String node) { }
}
