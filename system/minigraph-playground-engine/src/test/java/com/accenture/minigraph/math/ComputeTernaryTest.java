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

package com.accenture.minigraph.math;

import com.accenture.minigraph.start.PlaygroundLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A ternary in a COMPUTE statement, end to end: the statement computes a number, the branch's value, as a
 * DECIMAL does. Before the rule a COMPUTE that carried a comparison was a boolean statement whatever else it
 * held, so {@code 2 > 3 ? 100 : 50} stored {@code true}, the truth of 50, without an error.
 */
class ComputeTernaryTest {
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final String TERNARY_GRAPH = "unit-test-compute-ternary";
    private static final String QUOTED_GRAPH = "unit-test-compute-ternary-quoted";
    private static final String BOOLEAN_GRAPH = "unit-test-compute-ternary-boolean";
    private static final String MESSAGE = "message";
    private static final String LARGER = "100.5";
    private static final String SMALLER = "99.5";
    private static final String LATER = "2025-07-15";
    private static final String EARLIER = "2025-01-01";
    private static final long TIMEOUT = 8000;
    private static String target;

    @BeforeAll
    static void beforeAll() {
        PlaygroundLoader.main(new String[0]);
        target = "http://localhost:" + AppConfigReader.getInstance().getProperty("rest.server.port");
    }

    @Test
    void aTernaryStoresItsBranchsValue() throws TimeoutException {
        var taken = body(runGraph(TERNARY_GRAPH, LARGER, SMALLER));
        assertEquals(100.0, number(taken.get("d")), 0.0);
        // a ternary inside arithmetic is a number too
        assertEquals(200.0, number(taken.get("e")), 0.0);
        var other = body(runGraph(TERNARY_GRAPH, SMALLER, LARGER));
        // the other branch's value, where the truth of 50 used to be stored
        assertEquals(50.0, number(other.get("d")), 0.0);
        assertEquals(100.0, number(other.get("e")), 0.0);
    }

    @Test
    void aComparisonWithoutATernaryIsStillABoolean() throws TimeoutException {
        assertEquals(true, body(runGraph(TERNARY_GRAPH, LARGER, SMALLER)).get("f"));
        assertEquals(false, body(runGraph(TERNARY_GRAPH, SMALLER, LARGER)).get("f"));
    }

    @Test
    void textThatIsNotANumberFailsByNameAsInADecimal() throws TimeoutException {
        var response = runGraph(TERNARY_GRAPH, LATER, EARLIER);
        assertEquals(400, response.getStatus());
        var message = String.valueOf(body(response).get(MESSAGE));
        assertTrue(message.startsWith(
                "Not a number: input.body.x in '{input.body.x} >= {input.body.y} ? 100 : 50'"), message);
    }

    @Test
    void insideQuotesTheAuthorWroteTheTestComparesText() throws TimeoutException {
        assertEquals(100.0, number(body(runGraph(QUOTED_GRAPH, LATER, EARLIER)).get("d")), 0.0);
        assertEquals(50.0, number(body(runGraph(QUOTED_GRAPH, EARLIER, LATER)).get("d")), 0.0);
    }

    @Test
    void aBooleanResultFailsByName() throws TimeoutException {
        var response = runGraph(BOOLEAN_GRAPH, LARGER, SMALLER);
        assertEquals(400, response.getStatus());
        var message = String.valueOf(body(response).get(MESSAGE));
        assertTrue(message.startsWith("Boolean result where a number was expected"), message);
    }

    private static double number(Object value) {
        return assertInstanceOf(Number.class, value, "a number, not the truth of one").doubleValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(EventEnvelope response) {
        return assertInstanceOf(Map.class, response.getBody());
    }

    private EventEnvelope runGraph(String graphId, String x, String y) throws TimeoutException {
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/graph/" + graphId)
                .setBody(Map.of("x", x, "y", y))
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json");
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request);
        var traceId = String.format("%032x", Math.abs((long) (graphId + x + y).hashCode()));
        var po = PostOffice.trackable("unit.test", traceId, "TEST /graph/" + graphId);
        return po.asyncRequest(event, TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
    }
}
