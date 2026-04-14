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

package com.accenture.minigraph.unit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GraphTests {
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final long TIMEOUT = 8000;
    private static String target;

    @BeforeAll
    static void beforeAll() {
        AutoStart.main(new String[0]);
        var config = AppConfigReader.getInstance();
        var port = config.getProperty("rest.server.port");
        target = "http://localhost:" + port;
    }

    @SuppressWarnings("unchecked")
    @Test
    void unitTest1HappyPath() throws ExecutionException, InterruptedException {
        var request = new AsyncHttpRequest();
        request.setMethod("POST").setTargetHost(target).setUrl("/api/graph/unit-test-1");
        request.setBody(Map.of("person_id", 100)).setHeader("Content-Type", "application/json");
        var po = PostOffice.trackable("unit.test", "1000", "TEST /api/graph/unit-test-1");
        var event = new EventEnvelope().setBody(request).setTo(ASYNC_HTTP_CLIENT);
        var response = po.request(event, TIMEOUT).get();
        assertInstanceOf(Map.class, response.getBody());
        assertEquals(200, response.getStatus());
        var map = (Map<String, Object>) response.getBody();
        assertEquals("Peter", map.get("name"));
        assertEquals("100 World Blvd", map.get("address"));
    }
}
