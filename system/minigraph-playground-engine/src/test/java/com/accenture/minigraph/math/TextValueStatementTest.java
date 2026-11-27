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

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A text value from a request, read by a graph.math statement end to end: in a number expression it is one
 * number or the statement fails naming the selector, and it is never evaluated as part of the expression.
 * Before the rule a date written as ISO text computed as a subtraction (2025-07-15 gave 2003) and a later
 * date compared as the earlier one, both without an error.
 */
class TextValueStatementTest {
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final String DECIMAL_GRAPH = "unit-test-text-value-decimal";
    private static final String COMPUTE_GRAPH = "unit-test-text-value-compute";
    private static final String TERNARY_GRAPH = "unit-test-text-value-ternary";
    private static final String QUOTED_GRAPH = "unit-test-text-value-quoted";
    private static final String CONDITION_GRAPH = "unit-test-text-value-condition";
    private static final String NOT_A_NUMBER = "Not a number: input.body.x in '";
    private static final String RESULT = "d";
    private static final String MESSAGE = "message";
    private static final String LATER = "2025-07-15";
    private static final String EARLIER = "2025-01-01";
    private static final List<String> NOT_NUMBERS = List.of("2*3", "10-1", "(5)", LATER, "A4352", "5 5", "1,000",
            "PI", "max(1,2)", "3 > 2 ? 1 : 0");
    private static final long TIMEOUT = 8000;
    private static String target;

    @BeforeAll
    static void beforeAll() {
        PlaygroundLoader.main(new String[0]);
        target = "http://localhost:" + AppConfigReader.getInstance().getProperty("rest.server.port");
    }

    @Test
    void textThatIsNotANumberFailsADecimalStatementByName() throws TimeoutException {
        for (var value : NOT_NUMBERS) {
            assertNotANumber(runGraph(DECIMAL_GRAPH, value, value));
        }
    }

    @Test
    void textThatIsNotANumberFailsAComputeByName() throws TimeoutException {
        for (var value : NOT_NUMBERS) {
            assertNotANumber(runGraph(COMPUTE_GRAPH, value, value));
        }
    }

    @Test
    void theValueStaysOutOfTheError() throws TimeoutException {
        var message = String.valueOf(body(runGraph(DECIMAL_GRAPH, LATER, LATER)).get(MESSAGE));
        assertFalse(message.contains(LATER), message);
    }

    private static void assertNotANumber(EventEnvelope response) {
        assertEquals(400, response.getStatus());
        var message = String.valueOf(body(response).get(MESSAGE));
        assertTrue(message.startsWith(NOT_A_NUMBER + "{input.body.x} * 2'"), message);
    }

    @Test
    void textThatSpellsOneNumberComputesAsItAlwaysDid() throws TimeoutException {
        assertEquals("200.50", body(runGraph(DECIMAL_GRAPH, "100.25", "")).get(RESULT));
        assertEquals("2000", body(runGraph(DECIMAL_GRAPH, "1e3", "")).get(RESULT));
        assertEquals("14", body(runGraph(DECIMAL_GRAPH, "007", "")).get(RESULT));
        assertEquals("-10", body(runGraph(DECIMAL_GRAPH, "-5", "")).get(RESULT));
        assertEquals(200.5, ((Number) body(runGraph(COMPUTE_GRAPH, "100.25", "")).get(RESULT)).doubleValue(), 0.0);
        assertEquals(2000.0, ((Number) body(runGraph(COMPUTE_GRAPH, "1e3", "")).get(RESULT)).doubleValue(), 0.0);
    }

    @Test
    void aValueNeverTurnsAComputeIntoABoolean() throws TimeoutException {
        // the statement as written computes a number, so a value carrying '>' cannot make it a boolean
        var response = runGraph(COMPUTE_GRAPH, "3 > 2", "");
        assertEquals(400, response.getStatus());
        var number = body(runGraph(COMPUTE_GRAPH, "3", "")).get(RESULT);
        assertInstanceOf(Number.class, number);
    }

    @Test
    void twoDatesInADecimalTernaryAreNeverComparedAsArithmetic() throws TimeoutException {
        // read as arithmetic, 2025-07-15 is 2003 and 2025-01-01 is 2023: the later date compared as the earlier
        for (var dates : new String[][]{{LATER, EARLIER}, {EARLIER, LATER}, {LATER, LATER}}) {
            var response = runGraph(TERNARY_GRAPH, dates[0], dates[1]);
            assertEquals(400, response.getStatus());
            var message = String.valueOf(body(response).get(MESSAGE));
            assertTrue(message.startsWith(NOT_A_NUMBER), message);
        }
        // two numbers written as text compare as numbers
        assertEquals("100", body(runGraph(TERNARY_GRAPH, "100.5", "99.5")).get(RESULT));
        assertEquals("0", body(runGraph(TERNARY_GRAPH, "99.5", "100.5")).get(RESULT));
    }

    @Test
    void insideQuotesTheAuthorWroteTheDatesCompareAsText() throws TimeoutException {
        assertEquals("100", body(runGraph(QUOTED_GRAPH, LATER, EARLIER)).get(RESULT));
        assertEquals("0", body(runGraph(QUOTED_GRAPH, EARLIER, LATER)).get(RESULT));
        assertEquals("100", body(runGraph(QUOTED_GRAPH, LATER, LATER)).get(RESULT));
        // a quote in the value stays inside the literal
        assertEquals("100", body(runGraph(QUOTED_GRAPH, "z' >= 'a", EARLIER)).get(RESULT));
    }

    @Test
    void aConditionComparesTheSameValuesAsText() throws TimeoutException {
        for (var value : new String[]{"2*3", LATER, "A4352", "O'Brien", "a\\b"}) {
            assertEquals(true, body(runGraph(CONDITION_GRAPH, value, value)).get(RESULT), value);
        }
        assertEquals(false, body(runGraph(CONDITION_GRAPH, LATER, EARLIER)).get(RESULT));
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
