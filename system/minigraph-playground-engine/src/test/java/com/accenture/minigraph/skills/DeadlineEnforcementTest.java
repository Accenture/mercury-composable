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

package com.accenture.minigraph.skills;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import com.accenture.minigraph.start.PlaygroundLoader;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deployed-lane proof of the two deadline-alignment fixes:
 * <ol>
 * <li>graph.api.fetcher stamps x-ttl from its effective deadline (node ttl, else
 *     model.ttl), so the HTTP client's wire-level read timeout tracks the graph-side
 *     deadline instead of running disabled - the mock MDM endpoint echoes the header
 *     it observed on the wire.</li>
 * <li>graph.js arms a GraalVM context interrupt at the same effective deadline, so an
 *     endless script is killed instead of pinning a kernel thread forever.</li>
 * </ol>
 */
class DeadlineEnforcementTest {
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final long TIMEOUT = 10000;
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
    void apiFetcherAlignsWireTtlWithNodeDeadline() throws TimeoutException {
        var response = runGraph("unit-test-ttl-wire", Map.of("person_id", 100));
        assertEquals(200, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        var body = (Map<String, Object>) response.getBody();
        // the node declares ttl 7s; the fetcher must stamp x-ttl (milliseconds) so the
        // wire read timeout is the deadline + the client's one-second grace - the mock
        // endpoint echoes the header exactly as observed on the wire
        assertEquals("7000", body.get("observed_ttl"),
                "the api.fetcher must stamp x-ttl from its effective deadline: " + body);
    }

    @Test
    void endlessJsScriptIsInterruptedAtNodeDeadline() throws TimeoutException {
        long started = System.currentTimeMillis();
        var response = runGraph("unit-test-js-deadline", Map.of());
        long elapsed = System.currentTimeMillis() - started;
        // the endless script must die at the node's 2s deadline (GraalVM context
        // interrupt), well before the flow-level timer would abort the whole run
        assertEquals(408, response.getStatus(),
                "an interrupted script must surface as a 408: " + response.getBody());
        assertTrue(String.valueOf(response.getBody()).contains("exceeded the 2000 ms execution deadline"),
                "the error must name the script deadline: " + response.getBody());
        assertTrue(elapsed < 8000,
                "the interrupt must fire at the 2s node deadline, not a later flow timer (elapsed "
                        + elapsed + " ms)");
    }

    @Test
    void jsScriptDefaultDeadlineIsFiveSeconds() throws TimeoutException {
        long started = System.currentTimeMillis();
        var response = runGraph("unit-test-js-default-deadline", Map.of());
        long elapsed = System.currentTimeMillis() - started;
        // without a node ttl, graph.js does NOT inherit model.ttl: scripts are meant for
        // very simple computation or IF-THEN-ELSE, so the default deadline is a tight 5s
        assertEquals(408, response.getStatus(),
                "an interrupted script must surface as a 408: " + response.getBody());
        assertTrue(String.valueOf(response.getBody()).contains("exceeded the 5000 ms execution deadline"),
                "the default script deadline must be 5000 ms: " + response.getBody());
        assertTrue(elapsed >= 5000 && elapsed < 9000,
                "the default deadline must fire at ~5s (elapsed " + elapsed + " ms)");
    }

    private EventEnvelope runGraph(String graphId, Map<String, Object> body) throws TimeoutException {
        var request = new AsyncHttpRequest().setMethod(body.isEmpty()? "GET" : "POST").setTargetHost(target);
        if (!body.isEmpty()) {
            request.setBody(body).setHeader("Content-Type", "application/json");
        }
        request.setHeader("Accept", "application/json");
        request.setUrl("/api/graph/" + graphId);
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request);
        var po = PostOffice.trackable("unit.test", String.format("%032x", Math.abs(graphId.hashCode())),
                "TEST /graph/" + graphId);
        return po.asyncRequest(event, TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
    }
}
