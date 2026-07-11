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

package com.accenture.examples.tests;

import com.accenture.examples.common.TestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class RestEndpointTest extends TestBase {

    private EventEnvelope httpGet(String path, String demoHeader)
            throws InterruptedException, ExecutionException {
        PostOffice po = new PostOffice("unit.test", Utility.getInstance().getUuid(), "GET " + path);
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setHeader("accept", "application/json");
        if (demoHeader != null) {
            req.setHeader("x-demo-header", demoHeader);
        }
        req.setUrl(path);
        req.setTargetHost("http://127.0.0.1:" + springPort);
        EventEnvelope request = new EventEnvelope().setTo(HTTP_REQUEST).setBody(req);
        return po.request(request, RPC_TIMEOUT).get();
    }

    @SuppressWarnings("unchecked")
    @Test
    void helloWorldEchoesHeadersThroughTheService() throws InterruptedException, ExecutionException {
        EventEnvelope response = httpGet("/api/hello/world", "hello-123");
        assertEquals(200, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        // the endpoint forwards the incoming HTTP headers to hello.world, which echoes them
        // under its own 'body' key - hence body.body from the endpoint's wrapper
        assertEquals(200, map.getElement("status"));
        assertEquals("hello-123", map.getElement("body.body.x-demo-header"));
        assertNotNull(map.getElement("body.origin"));
        assertNotNull(map.getElement("execution_time"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void helloConcurrentReturnsAllParallelResults() throws InterruptedException, ExecutionException {
        EventEnvelope response = httpGet("/api/hello/concurrent", "parallel-456");
        assertEquals(200, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> results = (Map<String, Object>) response.getBody();
        // the endpoint fans out 10 parallel events to hello.world and aggregates the futures
        assertEquals(10, results.size());
        MultiLevelMap map = new MultiLevelMap(results);
        for (int i = 1; i <= 10; i++) {
            assertEquals("parallel-456", map.getElement("result-" + i + ".body.body.x-demo-header"));
        }
    }

    @Test
    void demoServletSaysHello() throws InterruptedException, ExecutionException {
        EventEnvelope response = httpGet("/demo", null);
        assertEquals(200, response.getStatus());
        assertEquals("hello world from servlet!", response.getBody());
    }

    /**
     * The Event-over-HTTP peer (lambda-example) and the hello.pojo service are not deployed in a
     * unit test, so all three pojo endpoints exercise their fail-fast error paths.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/api/pojo/http/1", "/api/pojo2/http/1", "/api/pojo/mesh/1"})
    void pojoEndpointsFailFastWithoutTheRemotePeer(String path)
            throws InterruptedException, ExecutionException {
        EventEnvelope response = httpGet(path, null);
        assertTrue(response.getStatus() >= 400, "expect an error status, got " + response.getStatus());
    }
}
