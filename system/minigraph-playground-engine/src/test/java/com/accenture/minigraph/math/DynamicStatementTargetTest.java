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
import org.platformlambda.core.util.MultiLevelMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every statement command resolves {dynamic variables}: RESET: and NEXT: are pinned
 * end-to-end by tutorial-12's generic error handler (RESET:/NEXT: {error.source});
 * this suite pins the remaining positions - a THEN: jump target and a DELAY: value
 * resolved from the model.
 */
class DynamicStatementTargetTest {
    private static final Logger log = LoggerFactory.getLogger(DynamicStatementTargetTest.class);
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
    void thenTargetAndDelayResolveDynamicVariables() throws TimeoutException {
        // the gate jumps to THEN: {model.hop} with 'DELAY: {model.backoff}'
        var response = runGraph(Map.of("hop", "stage-b"));
        assertEquals(200, response.getStatus());
        var body = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("dynamic", body.getElement("route_taken"));
        assertEquals(15, ((Number) body.getElement("applied_delay")).intValue(),
                "DELAY: {model.backoff} must resolve to the staged value");
        log.info("THEN: and DELAY: dynamic variables resolved");
    }

    @SuppressWarnings("unchecked")
    @Test
    void unmatchedConditionTakesTheLiteralAlternative() throws TimeoutException {
        var response = runGraph(Map.of("hop", "nowhere"));
        assertEquals(200, response.getStatus());
        var body = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("static", body.getElement("route_taken"));
        log.info("literal ELSE: target still routes normally");
    }

    private EventEnvelope runGraph(Map<String, Object> body) throws TimeoutException {
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/graph/unit-test-dynamic-jump")
                .setBody(body)
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json");
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request);
        var po = PostOffice.trackable("unit.test",
                String.format("%032x", Math.abs(body.hashCode())), "TEST /graph/unit-test-dynamic-jump");
        var response = po.asyncRequest(event, TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
        if (response.hasError()) {
            log.warn("HTTP-{} - {}", response.getStatus(), response.getBody());
        }
        return response;
    }
}
