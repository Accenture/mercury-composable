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

import com.accenture.minigraph.mock.CountingStepTask;
import com.accenture.minigraph.start.PlaygroundLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests of the generic exception context: when a failed node routes to its
 * exception= handler, the walker stages error.source, error.code, error.message and
 * error.stack - so ONE island-anchored handler can serve every node without naming any
 * failing node in its data mapping.
 */
class GraphErrorContextTest {
    private static final Logger log = LoggerFactory.getLogger(GraphErrorContextTest.class);
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
    void genericHandlerServesAFailingComposableFunction() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var response = runGraph("unit-test-error-context", cid, Map.of("mode", "task"));
        assertEquals(200, response.getStatus());
        var body = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("handled", body.getElement("stage"));
        // the handler reads the generic context - it never names the failing node
        assertEquals("fail-task", body.getElement("source"));
        assertEquals(400, body.getElement("code"));
        assertEquals("just a test", body.getElement("message"));
        // the thrown AppException carries a stack trace - staged as error.stack
        assertNotNull(body.getElement("stack"), "error.stack must be staged when available");
        assertTrue(String.valueOf(body.getElement("stack")).contains("AppException"),
                "unexpected stack: " + body.getElement("stack"));
        // an exception handler node connects onward like any node
        assertEquals(1, body.getElement("relay_count"));
        assertEquals(1, CountingStepTask.getCount("relay", cid));
        log.info("generic handler served the failing composable function for cid {}", cid);
    }

    @SuppressWarnings("unchecked")
    @Test
    void genericHandlerServesAFailingHttpCall() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var response = runGraph("unit-test-error-context", cid, Map.of("mode", "http"));
        assertEquals(200, response.getStatus());
        var body = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("handled", body.getElement("stage"));
        // the SAME handler node serves a different failing node - error.source tells them apart
        assertEquals("fetch-profile", body.getElement("source"));
        assertEquals(401, body.getElement("code"));
        assertTrue(String.valueOf(body.getElement("message")).contains("simulated exception"),
                "unexpected message: " + body.getElement("message"));
        assertEquals(1, CountingStepTask.getCount("relay", cid));
        log.info("generic handler served the failing HTTP call for cid {}", cid);
    }

    @Test
    void reservedErrorAliasIsRejectedAtTheGate() throws TimeoutException {
        // a node aliased 'error' would shadow the exception-context namespace - the
        // deployment gate rejects the graph so it answers 404 as if nonexistent
        var response = runGraph("unit-test-error-alias", Utility.getInstance().getUuid(),
                                Map.of("start", true));
        assertEquals(404, response.getStatus());
        assertTrue(String.valueOf(response.getBody()).contains("not found"),
                "unexpected error response: " + response.getBody());
        log.info("reserved 'error' alias rejected at the gate");
    }

    private EventEnvelope runGraph(String graphId, String cid, Map<String, Object> body) throws TimeoutException {
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/graph/" + graphId)
                .setBody(body)
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json")
                .setHeader("X-Correlation-Id", cid);
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request);
        var po = PostOffice.trackable("unit.test", util32HexTraceId(graphId + cid), "TEST /graph/" + graphId);
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
