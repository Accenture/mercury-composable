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

package com.accenture.minigraph.playground;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.AppConfigReader;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class RestEndpointTest {
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static String target;

    @BeforeAll
    static void setup() {
        AutoStart.main(new String[0]);
        var config = AppConfigReader.getInstance();
        var port = config.getProperty("rest.server.port");
        target = "http://localhost:" + port;
    }

    @SuppressWarnings("unchecked")
    @Test
    void testRestEndpoint() throws ExecutionException, InterruptedException {
        var invalid = new AsyncHttpRequest().setMethod("GET").setTargetHost(target).setUrl("/api/ws/{id}");
        invalid.setHeader("Accept", "application/json").setPathParameter("id", "nothing");
        var invalidReq = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(invalid);
        var po = EventEmitter.getInstance();
        var invalidResponse = po.request(invalidReq, 5000).get();
        assertEquals(400, invalidResponse.getStatus());
        assertInstanceOf(Map.class, invalidResponse.getBody());
        Map<String, Object> invalidBody = (Map<String, Object>) invalidResponse.getBody();
        assertEquals("Path parameter must be graph or json", invalidBody.get("message"));
        // load JSON playground home page
        var jsonPage = new AsyncHttpRequest().setMethod("GET").setTargetHost(target).setUrl("/api/ws/{id}");
        jsonPage.setHeader("Accept", "text/html").setPathParameter("id", "json");
        var jsonReq = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(jsonPage);
        var jsonHomePage = po.request(jsonReq, 5000).get();
        assertEquals(200, jsonHomePage.getStatus());
        assertInstanceOf(String.class, jsonHomePage.getBody());
        var jsonPayload = (String) jsonHomePage.getBody();
        assertTrue(jsonPayload.trim().endsWith("</html>"));
        // load MiniGraph playground home page
        var graphPage = new AsyncHttpRequest().setMethod("GET").setTargetHost(target).setUrl("/api/ws/{id}");
        graphPage.setHeader("Accept", "text/html").setPathParameter("id", "graph");
        var graphReq = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(graphPage);
        var graphHomePage = po.request(graphReq, 5000).get();
        assertEquals(200, graphHomePage.getStatus());
        assertInstanceOf(String.class, graphHomePage.getBody());
        var graphPayload = (String) graphHomePage.getBody();
        assertTrue(graphPayload.trim().endsWith("</html>"));
        // load MiniGraph UI home page
        var uiPage = new AsyncHttpRequest().setMethod("GET").setTargetHost(target).setUrl("/index.html");
        uiPage.setHeader("Accept", "text/html");
        var uiReq = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(uiPage);
        var uiHomePage = po.request(uiReq, 5000).get();
        assertEquals(200, uiHomePage.getStatus());
        assertInstanceOf(String.class, uiHomePage.getBody());
        var uiPayload = (String) uiHomePage.getBody();
        assertTrue(uiPayload.trim().endsWith("</html>"));
    }
}
