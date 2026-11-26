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

import com.accenture.minigraph.mock.IterationOutcome;
import com.accenture.minigraph.start.PlaygroundLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * One failing element fails a synchronous {@code for_each} fan-out, end to end through the HTTP stack, for the three
 * skills that fan out ({@code graph.extension}, {@code graph.task}, {@code graph.api.fetcher}) and for a fetcher's
 * dictionary items, which are called one after the other.
 * <p>
 * The node carries the status and the error of the first failing element in list order, whatever the other elements
 * answered and whenever they ended; an element that does not answer within the node's deadline is a 408; the output
 * mappings are skipped; no batch starts after a batch with a failure; and the aggregated result follows the source
 * list. Each rule is repeated, because the fork-join returns its responses in no particular order and a run could
 * pass by luck.
 */
class ForEachFailureTest {
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final long TIMEOUT = 20000;
    private static final int REPEATS = 8;
    private static final String EXTENSION = "unit-test-foreach-1";
    private static final String EXTENSION_HANDLED = "unit-test-foreach-2";
    private static final String TASK = "unit-test-foreach-3";
    private static final String TASK_HANDLED = "unit-test-foreach-4";
    private static final String FETCHER = "unit-test-foreach-5";
    private static final String FETCHER_HANDLED = "unit-test-foreach-6";
    private static final String ITEMS = "items";
    private static final String FAIL_ON = "fail_on";
    private static final String MESSAGE = "message";
    private static final String RESULTS = "results";
    private static final String PARTIAL = "partial";
    private static final String CODE = "code";
    private static final String STAGE = "stage";
    private static final String HANDLED = "handled";
    private static final String PERSON_ID = "person_id";
    private static final String NOT_FOUND = "Account zzz not found";
    private static final String ACCOUNT_DETAILS = "account-details";
    private static final String ACCOUNT_ID = "account_id";
    private static final String ERROR_TARGET = "target";
    private static final String FAILED_FOR_A = "upstream failure for a";
    private static final String TIMED_OUT = "Timeout for 2000 ms";
    private static final String CONTINUED = "continued";
    private static final String SKIPPED = "the output mappings are skipped";
    private static String target;

    @BeforeAll
    static void beforeAll() {
        PlaygroundLoader.main(new String[0]);
        target = "http://localhost:" + AppConfigReader.getInstance().getProperty("rest.server.port");
    }

    @Test
    void aFailingElementFailsTheNodeWithItsOwnStatus() throws Exception {
        // one batch of three: the element in the middle fails, the other two succeed
        for (var graph : List.of(EXTENSION, TASK)) {
            for (int i = 0; i < REPEATS; i++) {
                var response = runGraph(graph, Map.of(ITEMS, List.of("a", "b", "c"), FAIL_ON, "b"));
                assertEquals(500, response.getStatus(), graph + " " + response.getBody());
                var body = body(response);
                assertEquals("upstream failure for b", body.getElement(MESSAGE));
                assertNull(body.getElement(RESULTS), SKIPPED);
                assertNull(body.getElement(STAGE), "the run stops at the failed node");
            }
        }
    }

    @Test
    void theOrderTheElementsEndInPlaysNoPart() throws Exception {
        // the failing element is the slowest of its batch, then the fastest
        for (var graph : List.of(EXTENSION, TASK)) {
            for (var delays : List.of(Map.of("fail_delay", 200), Map.of("delay", 200))) {
                var request = new HashMap<String, Object>(delays);
                request.put(ITEMS, List.of("a", "b", "c"));
                request.put(FAIL_ON, "b");
                request.put("fail_status", 503);
                for (int i = 0; i < REPEATS / 2; i++) {
                    var response = runGraph(graph, request);
                    assertEquals(503, response.getStatus(), graph + " " + delays + " " + response.getBody());
                }
            }
        }
    }

    @Test
    void theHandlerReceivesTheFailingElementsStatus() throws Exception {
        for (var graph : List.of(EXTENSION_HANDLED, TASK_HANDLED)) {
            for (int i = 0; i < REPEATS; i++) {
                var response = runGraph(graph, Map.of(ITEMS, List.of("a", "b", "c"), FAIL_ON, "b"));
                assertEquals(200, response.getStatus(), graph + " " + response.getBody());
                var body = body(response);
                assertEquals(HANDLED, body.getElement(STAGE));
                assertEquals("fan-out", body.getElement("source"));
                assertEquals(500, body.getElement(CODE), graph + " " + response.getBody());
                assertNull(body.getElement(RESULTS), SKIPPED);
                // the node holds what succeeded, in list order - the failed element contributes nothing
                assertEquals(List.of(Map.of("item", "a"), Map.of("item", "c")), body.getElement(PARTIAL));
            }
        }
        // a subgraph's error is its error body, a function's is its message
        var fromSubgraph = body(runGraph(EXTENSION_HANDLED, Map.of(ITEMS, List.of("a", "b"), FAIL_ON, "a")));
        assertEquals(FAILED_FOR_A, fromSubgraph.getElement(MESSAGE + "." + MESSAGE));
        var fromFunction = body(runGraph(TASK_HANDLED, Map.of(ITEMS, List.of("a", "b"), FAIL_ON, "a")));
        assertEquals(FAILED_FOR_A, fromFunction.getElement(MESSAGE));
    }

    @Test
    void noBatchStartsAfterABatchWithAFailure() throws Exception {
        // four elements, three at a time: the first fails, so the fourth never starts
        var elements = List.of("a", "b", "c", "d");
        for (var graph : List.of(EXTENSION, TASK)) {
            var cid = Utility.getInstance().getUuid();
            var response = runGraph(graph, cid, Map.of(ITEMS, elements, FAIL_ON, "a"));
            assertEquals(500, response.getStatus(), graph + " " + response.getBody());
            assertEquals(FAILED_FOR_A, body(response).getElement(MESSAGE));
            assertEquals(Set.of("a", "b", "c"), IterationOutcome.started(cid), graph);
        }
        for (var graph : List.of(EXTENSION_HANDLED, TASK_HANDLED)) {
            var body = body(runGraph(graph, Map.of(ITEMS, elements, FAIL_ON, "a")));
            assertEquals(500, body.getElement(CODE), graph);
            assertEquals(List.of(Map.of("item", "b"), Map.of("item", "c")), body.getElement(PARTIAL), graph);
        }
        // the last element fails alone in its batch: the three before it ran, and the node still fails
        for (var graph : List.of(EXTENSION, TASK)) {
            var cid = Utility.getInstance().getUuid();
            var response = runGraph(graph, cid, Map.of(ITEMS, elements, FAIL_ON, "d"));
            assertEquals(500, response.getStatus(), graph + " " + response.getBody());
            assertEquals(Set.copyOf(elements), IterationOutcome.started(cid), graph);
        }
    }

    @Test
    void anElementThatDoesNotAnswerIsATimeout() throws Exception {
        // the node waits 2 seconds; element b takes 4, a and c answer at once
        var request = Map.<String, Object>of(ITEMS, List.of("a", "b", "c"), FAIL_ON, "b", "fail_delay", 4000);
        for (var graph : List.of("unit-test-foreach-7", "unit-test-foreach-8")) {
            var response = runGraph(graph, request);
            assertEquals(408, response.getStatus(), graph + " " + response.getBody());
            assertEquals(TIMED_OUT, body(response).getElement(MESSAGE));
        }
        var caught = body(runGraph("unit-test-foreach-9", request));
        assertEquals(HANDLED, caught.getElement(STAGE));
        assertEquals(408, caught.getElement(CODE));
        assertEquals(TIMED_OUT, caught.getElement(MESSAGE));
        assertEquals(List.of(Map.of("item", "a"), Map.of("item", "c")), caught.getElement(PARTIAL));
    }

    @Test
    void theResultFollowsTheSourceList() throws Exception {
        // nine elements in three batches, then the fetcher's five accounts in two
        var elements = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i");
        var expected = elements.stream().map(item -> Map.of("item", item)).toList();
        var accounts = List.of("a101", "b202", "c303", "d400", "e500");
        for (int i = 0; i < REPEATS / 2; i++) {
            for (var graph : List.of(EXTENSION, TASK)) {
                var response = runGraph(graph, Map.of(ITEMS, elements));
                assertEquals(200, response.getStatus(), graph + " " + response.getBody());
                assertEquals(expected, body(response).getElement(RESULTS), graph);
                assertEquals(CONTINUED, body(response).getElement(STAGE));
            }
            var fetched = body(runGraph(FETCHER, Map.of(PERSON_ID, 100, ITEMS, accounts)));
            assertEquals(accounts, fetched.getElement(RESULTS));
        }
    }

    @Test
    void aFailingElementFailsAFetcherFanOut() throws Exception {
        // account 'zzz' does not exist: the endpoint answers 400 for it
        var middle = Map.<String, Object>of(PERSON_ID, 100, ITEMS, List.of("a101", "zzz", "b202"));
        var first = Map.<String, Object>of(PERSON_ID, 100, ITEMS, List.of("zzz", "a101", "b202", "c303"));
        for (int i = 0; i < REPEATS; i++) {
            var response = runGraph(FETCHER, i % 2 == 0? middle : first);
            assertEquals(400, response.getStatus(), String.valueOf(response.getBody()));
            var body = body(response);
            assertEquals(NOT_FOUND, body.getElement(MESSAGE));
            assertEquals(ACCOUNT_DETAILS, body.getElement(ERROR_TARGET));
            assertNull(body.getElement(RESULTS), SKIPPED);
        }
        for (int i = 0; i < REPEATS; i++) {
            var body = body(runGraph(FETCHER_HANDLED, i % 2 == 0? middle : first));
            assertEquals(400, body.getElement(CODE), String.valueOf(body.getMap()));
            assertEquals(NOT_FOUND, body.getElement(MESSAGE));
            assertNull(body.getElement(RESULTS), SKIPPED);
            // in list order, the failed element maps nothing, and account c303 of the second batch never started
            assertEquals(List.of("a101", "b202"), body.getElement(PARTIAL + "." + ACCOUNT_ID));
        }
    }

    @Test
    void theFirstFailingDictionaryItemFailsTheFetcher() throws Exception {
        // no for_each: two dictionary items served by two providers, called one after the other
        var request = Map.<String, Object>of(PERSON_ID, 100, ACCOUNT_ID, "zzz");
        // the failing item is the first of the two, then the last
        for (var graph : List.of("unit-test-foreach-10", "unit-test-foreach-12")) {
            var response = runGraph(graph, request);
            assertEquals(400, response.getStatus(), graph + " " + response.getBody());
            var body = body(response);
            assertEquals(NOT_FOUND, body.getElement(MESSAGE));
            assertEquals(ACCOUNT_DETAILS, body.getElement(ERROR_TARGET), "the target is the item that failed");
            assertNull(body.getElement("name"), SKIPPED);
        }
        var caught = body(runGraph("unit-test-foreach-11", request));
        assertEquals(HANDLED, caught.getElement(STAGE));
        assertEquals(400, caught.getElement(CODE));
        assertEquals(NOT_FOUND, caught.getElement(MESSAGE));
        assertNull(caught.getElement("name"), SKIPPED);
        assertNull(caught.getElement(PARTIAL + ".person_name"), "no item is called after the one that failed");
        // nothing fails: both items answer and the mappings run
        var found = body(runGraph("unit-test-foreach-10", Map.of(PERSON_ID, 100, ACCOUNT_ID, "a101")));
        assertEquals("a101", found.getElement(ACCOUNT_ID));
        assertEquals("Peter", found.getElement("name"));
        assertEquals(CONTINUED, found.getElement(STAGE));
    }

    @SuppressWarnings("unchecked")
    private static MultiLevelMap body(EventEnvelope response) {
        assertInstanceOf(Map.class, response.getBody(), String.valueOf(response.getBody()));
        return new MultiLevelMap((Map<String, Object>) response.getBody());
    }

    private static EventEnvelope runGraph(String graphId, Map<String, Object> body) throws TimeoutException {
        return runGraph(graphId, Utility.getInstance().getUuid(), body);
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
        var response = po.asyncRequest(event, TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
        assertNotEquals(404, response.getStatus(), graphId + " must be deployed");
        return response;
    }
}
