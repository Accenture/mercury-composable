/*

    Copyright 2018-2025 Accenture Technology

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

package org.platformlambda.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.platformlambda.cloud.reporter.PresenceConnector;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.mock.TestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class AdminEndpointTest extends TestBase {
    private static final Logger log = LoggerFactory.getLogger(AdminEndpointTest.class);

    private static final String CLOUD_CONNECTOR_HEALTH = "cloud.connector.health";

    private static final AtomicBoolean firstRun = new AtomicBoolean(true);

    @BeforeEach
    public void waitForMockCloud() throws InterruptedException {
        if (firstRun.get()) {
            firstRun.set(false);
            final int WAIT = 20;
            final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
            Platform platform = Platform.getInstance();
            platform.waitForProvider(CLOUD_CONNECTOR_HEALTH, WAIT).onSuccess(bench::add);
            Boolean success = bench.poll(WAIT, TimeUnit.SECONDS);
            assert success != null;
            if (success) {
                log.info("Mock cloud ready");
            }
            waitForConnector();
        }
    }

    private void waitForConnector() {
        boolean ready = false;
        PresenceConnector connector = PresenceConnector.getInstance();
        for (int i=0; i < 20; i++) {
            if (connector.isConnected() && connector.isReady()) {
                ready = true;
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (ready) {
            log.info("Cloud connection ready");
        } else {
            log.error("Cloud connection not ready");
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void infoEndpointTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/info", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals("presence-monitor", multi.getElement("app.name"));
        assertEquals("RESOURCES", multi.getElement("personality"));
        String origin = Platform.getInstance().getOrigin();
        assertEquals(origin, multi.getElement("origin"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void remoteInfoEndpointTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("x-app-instance", "does-not-exist");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/info", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(404, result.get("status"));
        assertEquals("does-not-exist is not reachable", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void libEndpointTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/info/lib", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals("presence-monitor", multi.getElement("app.name"));
        assertTrue(result.containsKey("library"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void remoteLibEndpointTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("x-app-instance", "does-not-exist");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/info/lib", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(404, result.get("status"));
        assertEquals("does-not-exist is not reachable", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void routeEndpointNotAvailableTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/info/routes", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals("presence-monitor", multi.getElement("app.name"));
        assertEquals(Collections.emptyMap(), multi.getElement("routing"));
        assertEquals("Routing table is not visible from a presence monitor",
                multi.getElement("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void remoteRouteEndpointTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("x-app-instance", "does-not-exist");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/info/routes", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(404, result.get("status"));
        assertEquals("does-not-exist is not reachable", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void healthEndpointTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/health", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("UP", result.get("status"));
        MultiLevelMap map = new MultiLevelMap(result);
        assertEquals("mock-cloud", map.getElement("dependency[0].service"));
        log.info("health report: {}", result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void remoteHealthEndpointTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("x-app-instance", "does-not-exist");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/health", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(404, result.get("status"));
        assertEquals("does-not-exist is not reachable", result.get("message"));
    }

    @Test
    public void livenessEndpointTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "text/plain");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/livenessprobe", headers);
        assertInstanceOf(String.class, response.getBody());
        assertEquals("OK", response.getBody());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void remoteLivenessEndpointTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("x-app-instance", "does-not-exist");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/livenessprobe", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(404, result.get("status"));
        assertEquals("does-not-exist is not reachable", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void envEndpointTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/env", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals("presence-monitor", multi.getElement("app.name"));
        assertInstanceOf(Map.class, result.get("env"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void remoteEnvEndpointTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("x-app-instance", "does-not-exist");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/env", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(404, result.get("status"));
        assertEquals("does-not-exist is not reachable", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shutdownUsingGetWillFail() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/shutdown/now", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(404, result.get("status"));
        assertEquals("Resource not found", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void suspendUsingGetWillFail() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/suspend/now", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(404, result.get("status"));
        assertEquals("Resource not found", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void resumeUsingGetWillFail() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/resume/now", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(404, result.get("status"));
        assertEquals("Resource not found", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shutdownWithoutAppInstanceWillFail() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        EventEnvelope response = httpPost("http://127.0.0.1:"+port, "/shutdown", headers, new HashMap<>());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(400, result.get("status"));
        assertEquals("Missing X-App-Instance in request header", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shutdownWithIncorrectAppInstanceWillFail() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("x-app-instance", "does-not-exist");
        EventEnvelope response = httpPost("http://127.0.0.1:"+port, "/shutdown", headers, new HashMap<>());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(404, result.get("status"));
        assertEquals("does-not-exist is not reachable", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void suspendWithoutAppInstanceWillFail() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        EventEnvelope response = httpPost("http://127.0.0.1:"+port, "/suspend/now", headers, new HashMap<>());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(400, result.get("status"));
        assertEquals("Missing X-App-Instance in request header", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void suspendWithIncorrectAppInstanceWillFail() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("x-app-instance", "does-not-exist");
        EventEnvelope response = httpPost("http://127.0.0.1:"+port, "/suspend/now", headers, new HashMap<>());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(404, result.get("status"));
        assertEquals("does-not-exist is not reachable", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void resumeWithoutAppInstanceWillFail() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        EventEnvelope response = httpPost("http://127.0.0.1:"+port, "/resume/now", headers, new HashMap<>());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(400, result.get("status"));
        assertEquals("Missing X-App-Instance in request header", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void resumeWithIncorrectAppInstanceWillFail() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("x-app-instance", "does-not-exist");
        EventEnvelope response = httpPost("http://127.0.0.1:"+port, "/resume/now", headers, new HashMap<>());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(404, result.get("status"));
        assertEquals("does-not-exist is not reachable", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void suspendAppInstanceOK() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-App-Instance", Platform.getInstance().getOrigin());
        EventEnvelope response = httpPost("http://127.0.0.1:"+port, "/suspend/now", headers, new HashMap<>());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(200, result.get("status"));
        assertEquals("suspend", result.get("type"));
        assertEquals("/suspend/now", result.get("path"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void resumeAppInstanceOK() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-App-Instance", Platform.getInstance().getOrigin());
        EventEnvelope response = httpPost("http://127.0.0.1:"+port, "/resume/now", headers, new HashMap<>());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(200, result.get("status"));
        assertEquals("resume", result.get("type"));
        assertEquals("/resume/now", result.get("path"));
    }
}
