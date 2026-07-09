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

package com.accenture.minigraph.tasks;

import com.accenture.minigraph.start.PlaygroundLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests of the graph.task skill - a graph node that invokes a composable
 * function (TypedLambdaFunction registered with PreLoad) through its route name.
 */
class GraphTaskTest {
    private static final Logger log = LoggerFactory.getLogger(GraphTaskTest.class);
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final long TIMEOUT = 8000;
    private static String target;

    @BeforeAll
    static void beforeAll() {
        PlaygroundLoader.main(new String[0]);
        var config = AppConfigReader.getInstance();
        var port = config.getProperty("rest.server.port");
        target = "http://localhost:" + port;
    }

    @SuppressWarnings("unchecked")
    @Test
    void wholeBodyMergeAndHeaders() throws TimeoutException {
        var response = runGraph("unit-test-task-1", Map.of("hello", "world", "amount", 5),
                                Map.of("x-demo", "sunshine"));
        assertEquals(200, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        var mm = new MultiLevelMap((Map<String, Object>) response.getBody());
        // 'input.body -> *' seeds the whole body and 'int(100) -> amount' merges over it
        assertEquals("world", mm.getElement("received.hello"));
        assertEquals(100, mm.getElement("received.amount"));
        // 'input.header.x-demo -> header.hello' becomes a request header of the function
        assertEquals("sunshine", mm.getElement("hello_header"));
        assertEquals(200.0, mm.getElement("doubled"));
        // the function's response header is mapped to the graph output header
        assertEquals("demo", response.getHeader("x-task"));
        log.info("graph.task whole-body merge and header mapping work");
    }

    @SuppressWarnings("unchecked")
    @Test
    void fieldMappingToPoJoFunction() throws TimeoutException {
        var response = runGraph("unit-test-task-2", Map.of("name", "apple", "amount", 7), Map.of());
        assertEquals(200, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        var mm = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("apple", mm.getElement("name"));
        assertEquals(14, mm.getElement("total"));
        log.info("graph.task field mapping to a PoJo function works");
    }

    @SuppressWarnings("unchecked")
    @Test
    void forEachForkJoin() throws TimeoutException {
        var response = runGraph("unit-test-task-3", Map.of("items", List.of(1, 2, 3)), Map.of());
        assertEquals(200, response.getStatus());
        assertInstanceOf(List.class, response.getBody());
        var results = (List<Map<String, Object>>) response.getBody();
        assertEquals(3, results.size());
        var doubled = results.stream().map(m -> m.get("doubled")).toList();
        assertTrue(doubled.containsAll(List.of(2.0, 4.0, 6.0)));
        log.info("graph.task for_each fork-join works");
    }

    @SuppressWarnings("unchecked")
    @Test
    void exceptionHandlerNode() throws TimeoutException {
        var response = runGraph("unit-test-task-4", Map.of("hello", "world"), Map.of());
        assertEquals(200, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        var mm = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("recovered", mm.getElement("message"));
        assertEquals(400, mm.getElement("status"));
        log.info("graph.task exception handler routing works");
    }

    @Test
    void missingTaskRouteFailsFast() throws TimeoutException {
        var response = runGraph("unit-test-task-5", Map.of("hello", "world"), Map.of());
        assertNotEquals(200, response.getStatus());
        assertTrue(String.valueOf(response.getBody()).contains("does not exist"),
                "unexpected error response: " + response.getBody());
        log.info("graph.task fails fast for a missing route");
    }

    @Test
    void helpFileFollowsNamingConvention() {
        // 'describe skill graph.task' resolves the file name by replacing dots with hyphens
        assertNotNull(GraphTaskTest.class.getResourceAsStream("/help/help graph-task.md"),
                "help file for graph.task is missing");
    }

    private EventEnvelope runGraph(String graphId, Map<String, Object> body, Map<String, String> headers)
                                    throws TimeoutException {
        var request = new AsyncHttpRequest().setMethod(body.isEmpty()? "GET" : "POST").setTargetHost(target);
        if (!body.isEmpty()) {
            request.setBody(body).setHeader("Content-Type", "application/json");
        }
        request.setHeader("Accept", "application/json");
        headers.forEach(request::setHeader);
        request.setUrl("/api/graph/" + graphId);
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request);
        var po = PostOffice.trackable("unit.test", util32HexTraceId(graphId), "TEST /graph/" + graphId);
        var response = po.asyncRequest(event, TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
        if (response.hasError()) {
            log.warn("HTTP-{} - {}", response.getStatus(), response.getBody());
        }
        return response;
    }

    private String util32HexTraceId(String seed) {
        return String.format("%032x", Math.abs(seed.hashCode()));
    }
}
