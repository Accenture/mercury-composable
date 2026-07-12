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

package org.platformlambda.cloud;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.platformlambda.cloud.services.ServiceRegistry;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.Kv;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.ServiceDiscovery;
import org.platformlambda.core.util.Utility;
import org.platformlambda.mock.TestBase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the service registry's member life-cycle and peer-synchronization paths that the
 * main connectivity test does not reach: life-cycle/presence-monitor subscriptions with their
 * notification fan-out, keep-alive handling for new and known peers, route-list exchange,
 * broadcast of local add/unregister, monitor-down handling, and the query variants.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServiceRegistryEdgeTest extends TestBase {

    private static final String TYPE = "type";
    private static final String ROUTE = "route";
    private static final String ORIGIN = "origin";
    private static final String TOPIC = "topic";
    private static final String WATCHER = "member.event.watcher";
    private static final BlockingQueue<Map<String, String>> events = new ArrayBlockingQueue<>(10);

    @org.junit.jupiter.api.BeforeAll
    static void waitForRegistry() throws InterruptedException {
        Platform platform = Platform.getInstance();
        // the mock cloud registers the service registry asynchronously after boot
        final BlockingQueue<Boolean> ready = new ArrayBlockingQueue<>(1);
        platform.waitForProvider("cloud.connector.health", 20).onSuccess(ready::add);
        assertEquals(Boolean.TRUE, ready.poll(20, TimeUnit.SECONDS));
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline
                && !platform.hasRoute(ServiceDiscovery.SERVICE_REGISTRY)) {
            Utility.getInstance().sleep(200);
        }
    }

    private void subscribeWatcher() {
        Platform platform = Platform.getInstance();
        if (!platform.hasRoute(WATCHER)) {
            LambdaFunction f = (headers, input, instance) -> {
                events.add(new HashMap<>(headers));
                return true;
            };
            platform.registerPrivate(WATCHER, f, 1);
        }
        EventEmitter po = EventEmitter.getInstance();
        po.send(ServiceDiscovery.SERVICE_REGISTRY,
                new Kv(TYPE, "subscribe_life_cycle"), new Kv(ROUTE, WATCHER));
        po.send(ServiceDiscovery.SERVICE_REGISTRY,
                new Kv(TYPE, "subscribe_pm_status"), new Kv(ROUTE, WATCHER));
        // subscribing twice is idempotent; a fully-qualified route (with @) is rejected quietly
        po.send(ServiceDiscovery.SERVICE_REGISTRY,
                new Kv(TYPE, "subscribe_life_cycle"), new Kv(ROUTE, WATCHER));
        po.send(ServiceDiscovery.SERVICE_REGISTRY,
                new Kv(TYPE, "subscribe_life_cycle"), new Kv(ROUTE, "invalid@origin"));
        // missing route header is a no-op
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv(TYPE, "subscribe_life_cycle"));
    }

    private Map<String, String> waitForEvent(String type) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            Map<String, String> event = events.poll(500, TimeUnit.MILLISECONDS);
            if (event != null && type.equals(event.get(TYPE))) {
                return event;
            }
        }
        return null;
    }

    @Order(1)
    @Test
    void memberLifeCycleNotifications() throws InterruptedException {
        subscribeWatcher();
        EventEmitter po = EventEmitter.getInstance();
        String peer = "edge-test-peer-1";
        // a peer joining with a route list (NAME header) notifies life-cycle subscribers
        Map<String, Object> routes = new HashMap<>();
        routes.put("edge.service.one", "APP");
        EventEnvelope addList = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_REGISTRY)
                .setBody(routes).setHeader(TYPE, "add").setHeader(ORIGIN, peer)
                .setHeader(TOPIC, "multiplex.0001-005").setHeader("name", "edge-app")
                .setHeader("exchange", "true");
        po.send(addList);
        Map<String, String> joined = waitForEvent("join");
        assertNotNull(joined, "life-cycle subscriber should see the peer join");
        assertEquals(peer, joined.get(ORIGIN));
        // the peer leaving notifies subscribers and clears its routes
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv(TYPE, "leave"), new Kv(ORIGIN, peer));
        Map<String, String> left = waitForEvent("leave");
        assertNotNull(left, "life-cycle subscriber should see the peer leave");
        assertEquals(peer, left.get(ORIGIN));
        assertTrue(ServiceRegistry.getInstances("edge.service.one").isEmpty());
    }

    @Order(2)
    @Test
    void keepAliveRefreshesKnownAndUnknownPeers() {
        EventEmitter po = EventEmitter.getInstance();
        Platform platform = Platform.getInstance();
        String peer = "edge-test-peer-2";
        // keep-alive from an unknown peer triggers a join re-broadcast
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv(TYPE, "keep-alive"),
                new Kv(ORIGIN, peer), new Kv(TOPIC, "multiplex.0001-006"),
                new Kv("name", "edge-app-2"), new Kv("version", "1.0.0"));
        // keep-alive from a known peer refreshes the last-seen timestamp
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv(TYPE, "keep-alive"),
                new Kv(ORIGIN, peer), new Kv(TOPIC, "multiplex.0001-006"),
                new Kv("name", "edge-app-2"), new Kv("version", "1.0.0"));
        // keep-alive for this node removes stalled peers
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv(TYPE, "keep-alive"),
                new Kv(ORIGIN, platform.getOrigin()), new Kv(TOPIC, "multiplex.0001-000"));
        // join with a monitor version hits the version-detected branch
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv(TYPE, "join"),
                new Kv(ORIGIN, platform.getOrigin()), new Kv(TOPIC, "multiplex.0001-000"),
                new Kv("version", "9.9.9"));
        // incomplete events are no-ops
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv(TYPE, "join"), new Kv(ORIGIN, peer));
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv(TYPE, "keep-alive"), new Kv(ORIGIN, peer));
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv(TYPE, "leave"));
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv(TYPE, "no-such-type"));
        // allow the async handlers to drain, then confirm the peer is known
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline
                && !ServiceRegistry.getAllOrigins().containsKey(peer)) {
            Utility.getInstance().sleep(200);
        }
        assertTrue(ServiceRegistry.getAllOrigins().containsKey(peer));
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv(TYPE, "leave"), new Kv(ORIGIN, peer));
    }

    @Order(3)
    @Test
    void localAddAndUnregisterBroadcastToPeers() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        Platform platform = Platform.getInstance();
        String myOrigin = platform.getOrigin();
        // a locally registered route (origin = me, no 'final' flag) is broadcast to peers
        EventEnvelope add = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_REGISTRY)
                .setHeader(TYPE, "add").setHeader(ORIGIN, myOrigin)
                .setHeader(ROUTE, "edge.local.route").setHeader("personality", "APP");
        po.asyncRequest(add, 5000).onSuccess(bench::add);
        assertNotNull(bench.poll(10, TimeUnit.SECONDS));
        assertTrue(ServiceRegistry.getInstances("edge.local.route").contains(myOrigin));
        // and unregistering it broadcasts the removal. The earlier add was broadcast through the
        // mock cloud, which loops it back as an add (final) event - on a slow runner that loopback
        // can land AFTER this unregister and momentarily re-add the route, so poll until the
        // trailing unregister (final) loopback settles the registry to the removed state.
        EventEnvelope remove = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_REGISTRY)
                .setHeader(TYPE, "unregister").setHeader(ORIGIN, myOrigin)
                .setHeader(ROUTE, "edge.local.route");
        po.asyncRequest(remove, 5000).onSuccess(bench::add);
        assertNotNull(bench.poll(10, TimeUnit.SECONDS));
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline
                && ServiceRegistry.getInstances("edge.local.route").contains(myOrigin)) {
            Utility.getInstance().sleep(200);
        }
        assertFalse(ServiceRegistry.getInstances("edge.local.route").contains(myOrigin));
        // destination checks
        assertTrue(ServiceRegistry.destinationExists(myOrigin));
        assertTrue(ServiceRegistry.destinationExists("monitor-1"));
        assertFalse(ServiceRegistry.destinationExists(null));
        assertFalse(ServiceRegistry.destinationExists("no-such-origin"));
        assertTrue(ServiceRegistry.getTopic("monitor-2").endsWith("-2"));
        assertTrue(ServiceRegistry.getInstances("no.such.route").isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Order(4)
    @Test
    void queryVariants() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        // info
        EventEnvelope info = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_QUERY)
                .setHeader(TYPE, "info");
        po.asyncRequest(info, 5000).onSuccess(bench::add);
        EventEnvelope infoResult = bench.poll(10, TimeUnit.SECONDS);
        assertNotNull(infoResult);
        assertInstanceOf(Map.class, infoResult.getBody());
        assertTrue(((Map<String, Object>) infoResult.getBody()).containsKey("origin"));
        // download
        EventEnvelope download = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_QUERY)
                .setHeader(TYPE, "download");
        po.asyncRequest(download, 5000).onSuccess(bench::add);
        EventEnvelope downloadResult = bench.poll(10, TimeUnit.SECONDS);
        assertNotNull(downloadResult);
        Map<String, Object> data = (Map<String, Object>) downloadResult.getBody();
        assertTrue(data.containsKey("routes"));
        assertTrue(data.containsKey("nodes"));
        // find for an unknown route
        EventEnvelope findMissing = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_QUERY)
                .setHeader(TYPE, "find").setHeader(ROUTE, "no.such.route");
        po.asyncRequest(findMissing, 5000).onSuccess(bench::add);
        EventEnvelope missing = bench.poll(10, TimeUnit.SECONDS);
        assertNotNull(missing);
        assertEquals(false, missing.getBody());
        // find "*" without a list body is false
        EventEnvelope findStar = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_QUERY)
                .setHeader(TYPE, "find").setHeader(ROUTE, "*").setBody("not-a-list");
        po.asyncRequest(findStar, 5000).onSuccess(bench::add);
        EventEnvelope star = bench.poll(10, TimeUnit.SECONDS);
        assertNotNull(star);
        assertEquals(false, star.getBody());
        // find "*" where one of the routes does not exist is false
        EventEnvelope findList = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_QUERY)
                .setHeader(TYPE, "find").setHeader(ROUTE, "*")
                .setBody(List.of("no.such.route"));
        po.asyncRequest(findList, 5000).onSuccess(bench::add);
        EventEnvelope listResult = bench.poll(10, TimeUnit.SECONDS);
        assertNotNull(listResult);
        assertEquals(false, listResult.getBody());
        // invalid usage is rejected
        EventEnvelope invalid = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_QUERY)
                .setHeader(TYPE, "nonsense");
        po.asyncRequest(invalid, 5000).onSuccess(bench::add);
        EventEnvelope error = bench.poll(10, TimeUnit.SECONDS);
        assertNotNull(error);
        assertEquals(400, error.getStatus());
        // unsubscribe both watcher subscriptions (and verify the no-op branches)
        po.send(ServiceDiscovery.SERVICE_REGISTRY,
                new Kv(TYPE, "unsubscribe_life_cycle"), new Kv(ROUTE, WATCHER));
        po.send(ServiceDiscovery.SERVICE_REGISTRY,
                new Kv(TYPE, "unsubscribe_pm_status"), new Kv(ROUTE, WATCHER));
        po.send(ServiceDiscovery.SERVICE_REGISTRY,
                new Kv(TYPE, "unsubscribe_life_cycle"), new Kv(ROUTE, "never.subscribed"));
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv(TYPE, "unsubscribe_pm_status"));
    }
}
