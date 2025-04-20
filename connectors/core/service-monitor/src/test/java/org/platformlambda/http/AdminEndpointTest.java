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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class AdminEndpointTest extends TestBase {
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
    void infoEndpointTest() throws IOException, InterruptedException {
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
    void libEndpointTest() throws IOException, InterruptedException {
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
    void routeEndpointHasLocalRoutingTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/info/routes", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals("presence-monitor", multi.getElement("app.name"));
        assertInstanceOf(Map.class, multi.getElement("routing.public"));
        assertInstanceOf(Map.class, multi.getElement("routing.private"));
        Map<String, Object> publicRoutes = (Map<String, Object>) multi.getElement("routing.public");
        Map<String, Object> privateRoutes = (Map<String, Object>) multi.getElement("routing.private");
        assertTrue(publicRoutes.isEmpty());
        assertTrue(privateRoutes.containsKey("temporary.inbox"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void healthEndpointTest() throws IOException, InterruptedException {
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

    @Test
    void livenessEndpointTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "text/plain");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/livenessprobe", headers);
        assertInstanceOf(String.class, response.getBody());
        assertEquals("OK", response.getBody());
    }

    @SuppressWarnings("unchecked")
    @Test
    void envEndpointTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/env", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals("presence-monitor", multi.getElement("app.name"));
        assertInstanceOf(Map.class, result.get("env"));
    }
}
