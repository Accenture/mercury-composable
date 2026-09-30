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
import org.platformlambda.core.serializers.MsgPack;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The DECIMAL statement end to end (RFC-0001): the high-precision COMPUTE. Decimal strings and whole numbers
 * go in, canonical decimal strings come out, a double is rejected by name, and COMPUTE keeps its meaning.
 */
class DecimalStatementTest {
    private static final Logger log = LoggerFactory.getLogger(DecimalStatementTest.class);
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
    void decimalStringsInCanonicalStringsOut() throws TimeoutException {
        var response = runGraph(Map.of("amount", "100.25", "rate", "0.0375", "qty", 3));
        assertEquals(200, response.getStatus());
        var body = new MultiLevelMap((Map<String, Object>) response.getBody());
        // 100.25 * 0.0375 is exact: the scales add (2 + 4 = 6)
        assertEquals("3.759375", body.getElement("fee"));
        assertEquals("3.76", body.getElement("rounded"));
        // a whole number is exact; 3 * 3.76 keeps the scale of the rounded amount
        assertEquals("11.28", body.getElement("total"));
        // a zero of any scale is "0"
        assertEquals("0", body.getElement("zero"));
        assertInstanceOf(String.class, body.getElement("fee"), "a decimal is never a JSON number");
        log.info("DECIMAL statements returned canonical strings");
    }

    @SuppressWarnings("unchecked")
    @Test
    void compareTheDecimalStringAsANumberInIf() throws TimeoutException {
        // 5000.00 * 0.0375 = 187.5000 -> '187.50'; as text '187.50' sorts before '99.5', as numbers it is larger
        var big = runGraph(Map.of("amount", "5000.00", "rate", "0.0375", "qty", 1));
        assertEquals(200, big.getStatus());
        var bigBody = new MultiLevelMap((Map<String, Object>) big.getBody());
        assertEquals("187.50", bigBody.getElement("rounded"));
        assertEquals("big", bigBody.getElement("size"));
        var small = runGraph(Map.of("amount", "100.25", "rate", "0.0375", "qty", 1));
        assertEquals("small", new MultiLevelMap((Map<String, Object>) small.getBody()).getElement("size"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void computeKeepsItsMeaning() throws TimeoutException {
        var response = runGraph(Map.of("amount", "100.25", "rate", "0.0375", "qty", 3));
        var body = new MultiLevelMap((Map<String, Object>) response.getBody());
        // COMPUTE still stores a double: a JSON number, not a string
        assertInstanceOf(Number.class, body.getElement("doubled"));
        assertEquals(6.0, ((Number) body.getElement("doubled")).doubleValue(), 0.0);
    }

    @Test
    void aDoubleIsRejectedByName() throws TimeoutException {
        // a JSON number arrives as a double: the decimal must travel as the string "0.0375"
        var response = runGraph(Map.of("amount", "100.25", "rate", 0.0375, "qty", 3));
        assertNotEquals(200, response.getStatus(), "a double must not silently drop the statement to floating point");
        var text = String.valueOf(response.getBody());
        assertTrue(text.contains("Inexact number"), text);
        assertTrue(text.contains("input.body.rate"), text);
        log.info("a double was rejected: {}", text);
    }

    @Test
    void aDecimalSurvivesTheSerializerThatSuspendAndResumeUse() throws IOException {
        // graph.suspend persists the model and graph.resume merges it back through the platform's MsgPack:
        // a BigDecimal would come back a String, and a small Long an Integer - which is why a decimal is stored
        // as its canonical string, the same before and after
        var model = new LinkedHashMap<String, Object>();
        model.put("fee", "3.759375");
        model.put("zero", "0");
        model.put("held", new BigDecimal("10.50"));
        model.put("count", 5L);
        var packer = new MsgPack();
        var restored = (Map<?, ?>) packer.unpack(packer.pack(Map.of("model", model)));
        var back = (Map<?, ?>) restored.get("model");
        assertEquals("3.759375", back.get("fee"));
        assertEquals("0", back.get("zero"));
        assertEquals("10.50", back.get("held"));
        assertInstanceOf(String.class, back.get("held"), "a BigDecimal does not survive as a BigDecimal");
        assertInstanceOf(Integer.class, back.get("count"));
        // the restored strings are the decimals the statement reads
        assertEquals("14.259375", DecimalEvaluator.evaluate("'3.759375' + '10.50'"));
        assertEquals("1", DecimalEvaluator.evaluate("'3.759375' > '10.50' ? 0 : 1"));
    }

    private EventEnvelope runGraph(Map<String, Object> body) throws TimeoutException {
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/graph/unit-test-decimal")
                .setBody(body)
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json");
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request);
        var po = PostOffice.trackable("unit.test",
                String.format("%032x", Math.abs(body.hashCode())), "TEST /graph/unit-test-decimal");
        var response = po.asyncRequest(event, TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
        if (response.hasError()) {
            log.warn("HTTP-{} - {}", response.getStatus(), response.getBody());
        }
        return response;
    }
}
