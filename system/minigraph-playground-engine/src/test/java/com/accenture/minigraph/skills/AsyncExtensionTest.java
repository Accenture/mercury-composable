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

import com.accenture.minigraph.mock.AsyncRecorder;
import com.accenture.minigraph.start.PlaygroundLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An asynchronous {@code graph.extension} (RFC-0011 section 8.1), end to end through the HTTP stack: the node starts
 * its target and the parent graph answers before the target ends. The target's work is {@code v1.async.recorder},
 * which records each run under the business correlation id the target inherited.
 */
class AsyncExtensionTest {
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final long TIMEOUT = 8000;
    private static final String ADVICE = "advice";
    private static final String NOT_STARTED = "not started";
    private static String target;

    @BeforeAll
    static void beforeAll() {
        PlaygroundLoader.main(new String[0]);
        target = "http://localhost:" + AppConfigReader.getInstance().getProperty("rest.server.port");
    }

    @Test
    void theParentAnswersBeforeItsTargetEnds() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var response = runGraph("unit-test-async-1", cid, Map.of("item", "a", "delay", 1500));
        assertEquals(202, response.getStatus(), String.valueOf(response.getBody()));
        assertEquals(ADVICE, body(response).getElement("type"));
        var runs = AsyncRecorder.runs(cid);
        assertEquals(0, runs.ended.get(), "the caller is answered before the target ends");
        assertTrue(runs.awaitEnded(1, TIMEOUT), "the target runs to its end after the parent answered");
        var run = runs.started.getFirst();
        assertEquals("a", run.get("item"));
        assertNull(run.get("index"), "a single delegation has no for_each position");
        assertEquals(1, runs.started.size());
    }

    @Test
    void aFanOutStartsOneRunPerElementWithinItsConcurrency() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var items = List.of("a", "b", "c", "d", "e");
        var response = runGraph("unit-test-async-2", cid, Map.of("items", items, "delay", 400));
        assertEquals(202, response.getStatus(), String.valueOf(response.getBody()));
        var runs = AsyncRecorder.runs(cid);
        assertEquals(0, runs.ended.get(), "the caller is answered before the runs end");
        assertTrue(runs.awaitEnded(items.size(), TIMEOUT), "every element runs");
        assertEquals(2, runs.maxActive.get(), "concurrency bounds the active runs, and the window stays full");
        Set<Object> seen = new HashSet<>();
        Set<Object> positions = new HashSet<>();
        for (var run : runs.started) {
            seen.add(run.get("item"));
            positions.add(run.get("index"));
        }
        assertEquals(Set.copyOf(items), seen);
        assertEquals(Set.of("0", "1", "2", "3", "4"), positions, "each run carries its position in the list");
    }

    @Test
    void aFlowTargetRunsUnderTheNodeTtl() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var response = runGraph("unit-test-async-5", cid, Map.of("item", "f", "delay", 300));
        assertEquals(202, response.getStatus(), String.valueOf(response.getBody()));
        var runs = AsyncRecorder.runs(cid);
        assertTrue(runs.awaitEnded(1, TIMEOUT));
        assertEquals("f", runs.started.getFirst().get("item"));
        assertEquals(20000, Utility.getInstance().str2int(String.valueOf(runs.started.getFirst().get("ttl"))),
                "the node's ttl is the target's own, not the parent's");
    }

    @Test
    void aTargetWithoutANodeTtlRunsUnderItsOwn() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var response = runGraph("unit-test-async-6", cid, Map.of("item", "g", "delay", 0));
        assertEquals(202, response.getStatus(), String.valueOf(response.getBody()));
        var runs = AsyncRecorder.runs(cid);
        assertTrue(runs.awaitEnded(1, TIMEOUT));
        assertEquals(45000, Utility.getInstance().str2int(String.valueOf(runs.started.getFirst().get("ttl"))),
                "the flow's own ttl, not the 30 seconds the parent runs under");
    }

    @Test
    void aGraphThatIsNotDeployedIsAFailureToStart() throws Exception {
        var response = runGraph("unit-test-async-3", Utility.getInstance().getUuid(), Map.of("item", "a"));
        var handled = body(response);
        assertEquals(NOT_STARTED, handled.getElement("stage"));
        assertEquals("start", handled.getElement("source"));
        assertEquals(404, handled.getElement("code"));
        assertEquals("node start - unit-test-async-missing does not exist", handled.getElement("message"));
    }

    @Test
    void aFlowThatDoesNotExistIsAFailureToStart() throws Exception {
        var response = runGraph("unit-test-async-4", Utility.getInstance().getUuid(), Map.of("item", "a"));
        var handled = body(response);
        assertEquals(NOT_STARTED, handled.getElement("stage"));
        assertEquals(404, handled.getElement("code"));
        assertEquals("node start - flow://no-such-flow does not exist", handled.getElement("message"));
    }

    @Test
    void anInputTheTargetRefusesIsAFailureToStart() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var response = runGraph("unit-test-async-1", cid, Map.of("item", 42));
        var handled = body(response);
        assertEquals(NOT_STARTED, handled.getElement("stage"));
        assertEquals(400, handled.getElement("code"));
        assertTrue(String.valueOf(handled.getElement("message")).contains("item"),
                "the validator names the field: " + handled.getElement("message"));
        assertFalse(AsyncRecorder.runs(cid).awaitStarted(1, 1000), "a refused input starts nothing");
    }

    @Test
    void oneRefusedElementStartsNoneOfTheFanOut() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var response = runGraph("unit-test-async-2", cid, Map.of("items", List.of("a", 7), "delay", 0));
        var handled = body(response);
        assertEquals(400, handled.getElement("code"));
        assertTrue(String.valueOf(handled.getElement("message")).startsWith("for_each element 1 - "),
                String.valueOf(handled.getElement("message")));
        assertFalse(AsyncRecorder.runs(cid).awaitStarted(1, 1000), "no element starts when one is refused");
    }

    @SuppressWarnings("unchecked")
    private static MultiLevelMap body(EventEnvelope response) {
        assertInstanceOf(Map.class, response.getBody(), String.valueOf(response.getBody()));
        return new MultiLevelMap((Map<String, Object>) response.getBody());
    }

    private static EventEnvelope runGraph(String graphId, String cid, Map<String, Object> body)
            throws TimeoutException {
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/graph/" + graphId).setBody(body)
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json")
                .setHeader("X-Correlation-Id", cid);
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request);
        var po = PostOffice.trackable("unit.test", Utility.getInstance().getUuid(), "TEST /graph/" + graphId);
        return po.asyncRequest(event, TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
    }
}
