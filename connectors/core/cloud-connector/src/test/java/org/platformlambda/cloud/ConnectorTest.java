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

package org.platformlambda.cloud;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.cloud.reporter.PresenceConnector;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.Kv;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.ServiceDiscovery;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.mock.TestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ConnectorTest extends TestBase {
    private static final Logger log = LoggerFactory.getLogger(ConnectorTest.class);

    private static final String CLOUD_CONNECTOR_HEALTH = "cloud.connector.health";

    private static final AtomicBoolean firstRun = new AtomicBoolean(true);

    @BeforeAll
    public static void waitForMockCloud() throws InterruptedException {
        if (firstRun.get()) {
            firstRun.set(false);
            final int WAIT = 20;
            final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
            Platform platform = Platform.getInstance();
            platform.waitForProvider(CLOUD_CONNECTOR_HEALTH, WAIT).onSuccess(bench::add);
            Boolean success = bench.poll(WAIT, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(success)) {
                log.info("Mock cloud ready");
            }
            waitForConnector();
        }
    }

    private static void waitForConnector() {
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
    void connectivityTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        String origin = "unit-test";
        Platform platform = Platform.getInstance();
        platform.waitForProvider("cloud.connector.health", 10);
        EventEmitter po = EventEmitter.getInstance();
        String URL = "https://127.0.0.1";
        String SERVICE_NAME = "CloudConnector";
        ConnectorConfig.setDisplayUrl(URL);
        ConnectorConfig.setServiceName(SERVICE_NAME);
        String url = ConnectorConfig.getDisplayUrl();
        String name = ConnectorConfig.getServiceName();
        Map<String, String> topicSubstitution = ConnectorConfig.getTopicSubstitution();
        assertEquals(URL, url);
        assertEquals(SERVICE_NAME, name);
        assertEquals("user.topic.one", topicSubstitution.get("multiplex.0001.0"));
        EventEnvelope req = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_REGISTRY)
                                    .setHeader("type", "join").setHeader("origin", origin)
                                    .setHeader("topic", "multiplex.0001-001");
        po.asyncRequest(req, 5000).onSuccess(bench::add);
        bench.poll(10, TimeUnit.SECONDS);
        req = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_REGISTRY)
                .setHeader("type", "join").setHeader("origin", platform.getOrigin())
                .setHeader("topic", "multiplex.0001-000");
        po.asyncRequest(req, 5000).onSuccess(bench::add);
        bench.poll(10, TimeUnit.SECONDS);
        req.setHeader("type", "add").setHeader("topic", "multiplex.0001-001");
        req = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_REGISTRY)
                    .setHeader("type", "add").setHeader("origin", origin).setHeader("route", "hello.world")
                    .setHeader("personality", "WEB");
        po.asyncRequest(req, 5000).onSuccess(bench::add);
        bench.poll(10, TimeUnit.SECONDS);
        Map<String, Object> routes = new HashMap<>();
        routes.put("hello.test", "WEB");
        routes.put("hello.demo", "WEB");
        routes.put("to.be.removed", "WEB");
        req = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_REGISTRY).setBody(routes)
                    .setHeader("type", "add").setHeader("origin", origin)
                    .setHeader("personality", "WEB");
        po.asyncRequest(req, 5000).onSuccess(bench::add);
        bench.poll(10, TimeUnit.SECONDS);
        po.broadcast("hello.world", "something");
        po.send("hello.demo@"+origin, "something else");
        req = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_REGISTRY).setBody(routes)
                .setHeader("type", "unregister").setHeader("origin", origin)
                .setHeader("route", "to.be.removed");
        po.asyncRequest(req, 5000).onSuccess(bench::add);
        bench.poll(10, TimeUnit.SECONDS);
        req = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_QUERY)
                .setHeader("type", "search")
                .setHeader("origin", origin).setHeader("route", "hello.world");
        po.asyncRequest(req, 5000).onSuccess(bench::add);
        EventEnvelope queryResult = bench.poll(10, TimeUnit.SECONDS);
        assert queryResult != null;
        assertInstanceOf(List.class, queryResult.getBody());
        List<String> list = (List<String>) queryResult.getBody();
        assertFalse(list.isEmpty());
        List<String> instances = (List<String>) queryResult.getBody();
        assertTrue(instances.contains("unit-test"));
        assertTrue(instances.contains(platform.getOrigin()));
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/info/routes", headers);
        MultiLevelMap multi = new MultiLevelMap((Map<String, Object>) response.getBody());
        Object nodes = multi.getElement("network.nodes");
        assertInstanceOf(List.class, nodes);
        String nodeList = nodes.toString();
        assertTrue(nodeList.contains("unit-test"));
        assertTrue(po.exists("hello.demo"));
        req = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_QUERY)
                .setBody(Collections.singletonList("hello.world")).setHeader("type", "find")
                .setHeader("route", "*");
        po.asyncRequest(req, 5000).onSuccess(bench::add);
        queryResult = bench.poll(10, TimeUnit.SECONDS);
        assert queryResult != null;
        assertEquals(true, queryResult.getBody());
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv("type", "leave"), new Kv("origin", origin));
    }

    @Test
    void checkTopicNameWithoutDot() {
        String name = "hello.world";
        ConnectorConfig.validateTopicName(name);
        String invalid = "helloworld";
        var ex = assertThrows(IllegalArgumentException.class, () -> ConnectorConfig.validateTopicName(invalid));
        assertEquals("Invalid route helloworld because it is missing dot separator(s). e.g. hello.world",
                ex.getMessage());
    }

    @Test
    void checkEmptyTopic() {
        var ex = assertThrows(IllegalArgumentException.class, () -> ConnectorConfig.validateTopicName(""));
        assertEquals("Invalid route name - use 0-9, a-z, A-Z, period, hyphen or underscore characters",
                ex.getMessage());
    }

    @Test
    void reservedExtension() {
        var ex = assertThrows(IllegalArgumentException.class, () ->
                ConnectorConfig.validateTopicName("hello.com"));
        assertEquals("Invalid route hello.com which is a reserved extension",
                ex.getMessage());
    }

    @Test
    void reservedName() {
        var ex = assertThrows(IllegalArgumentException.class, () ->
                ConnectorConfig.validateTopicName("Thumbs.db"));
        assertEquals("Invalid route Thumbs.db which is a reserved Windows filename",
                ex.getMessage());
    }

    @SuppressWarnings("unchecked")
    @Test
    void healthTest() throws InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        EventEnvelope response = httpGet("http://127.0.0.1:"+port, "/health", headers);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("UP", result.get("status"));
        assertEquals("cloud-connector", result.get("name"));
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals(200, multi.getElement("dependency[0].status_code"));
    }
}
