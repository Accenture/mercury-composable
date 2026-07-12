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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.cloud.services.ServiceRegistry;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.Kv;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.ServiceDiscovery;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.websocket.common.MultipartPayload;
import org.platformlambda.mock.TestBase;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the outbound event producer (EventEmitter.CLOUD_CONNECTOR) through its destination
 * resolution paths: round-robin by route, broadcast fan-out, segmented (multipart) sticky
 * destinations, fully-qualified targets and unknown destinations.
 */
class EventProducerTest extends TestBase {

    private static final String TO = MultipartPayload.TO;
    private static final String BROADCAST = MultipartPayload.BROADCAST;
    private static final String ID = MultipartPayload.ID;
    private static final String COUNT = MultipartPayload.COUNT;
    private static final String TOTAL = MultipartPayload.TOTAL;
    private static final String PEER_1 = "ep-peer-1";
    private static final String PEER_2 = "ep-peer-2";
    private static final String ROUTE = "ep.load.balanced";
    // the mock pub/sub decodes the published payload as a packed EventEnvelope
    private static final byte[] PAYLOAD = new EventEnvelope()
            .setTo("hello.world").setBody("test-payload").toBytes();

    @BeforeAll
    static void peers() throws InterruptedException {
        EventEmitter po = EventEmitter.getInstance();
        Platform platform = Platform.getInstance();
        // the mock cloud registers the service registry asynchronously after boot
        final BlockingQueue<Boolean> ready = new ArrayBlockingQueue<>(1);
        platform.waitForProvider("cloud.connector.health", 20).onSuccess(ready::add);
        assertEquals(Boolean.TRUE, ready.poll(20, TimeUnit.SECONDS));
        long registryDeadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < registryDeadline
                && !platform.hasRoute(ServiceDiscovery.SERVICE_REGISTRY)) {
            Utility.getInstance().sleep(200);
        }
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv("type", "join"),
                new Kv("origin", platform.getOrigin()), new Kv("topic", "multiplex.0001-000"));
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv("type", "join"),
                new Kv("origin", PEER_1), new Kv("topic", "multiplex.0001-007"));
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv("type", "join"),
                new Kv("origin", PEER_2), new Kv("topic", "multiplex.0001-008"));
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv("type", "add"),
                new Kv("origin", PEER_1), new Kv("route", ROUTE), new Kv("personality", "APP"));
        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv("type", "add"),
                new Kv("origin", PEER_2), new Kv("route", ROUTE), new Kv("personality", "APP"));
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline
                && ServiceRegistry.getInstances(ROUTE).size() < 2) {
            Utility.getInstance().sleep(200);
        }
        assertEquals(2, ServiceRegistry.getInstances(ROUTE).size());
    }

    private EventEnvelope produce(EventEnvelope event) throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter.getInstance().asyncRequest(event, 5000).onSuccess(bench::add);
        return bench.poll(10, TimeUnit.SECONDS);
    }

    @Test
    void roundRobinAcrossInstances() throws InterruptedException {
        // two sends exercise the load-balancer rotation over the two peers
        for (int i = 0; i < 2; i++) {
            EventEnvelope event = new EventEnvelope().setTo(EventEmitter.CLOUD_CONNECTOR)
                    .setHeader(TO, ROUTE).setBody(PAYLOAD);
            EventEnvelope response = produce(event);
            assertNotNull(response);
            assertEquals(true, response.getBody());
        }
    }

    @Test
    void broadcastReachesAllInstances() throws InterruptedException {
        EventEnvelope event = new EventEnvelope().setTo(EventEmitter.CLOUD_CONNECTOR)
                .setHeader(TO, ROUTE).setHeader(BROADCAST, "1").setBody(PAYLOAD);
        EventEnvelope response = produce(event);
        assertNotNull(response);
        assertEquals(true, response.getBody());
    }

    @Test
    void segmentedMessageSticksToOneDestination() throws InterruptedException {
        String id = "segment-demo-1";
        // first block resolves by route and caches the destination
        EventEnvelope first = new EventEnvelope().setTo(EventEmitter.CLOUD_CONNECTOR)
                .setHeader(TO, ROUTE).setHeader(ID, id)
                .setHeader(COUNT, "1").setHeader(TOTAL, "2").setBody(PAYLOAD);
        assertNotNull(produce(first));
        // the final block reuses the cached destination and clears it
        EventEnvelope last = new EventEnvelope().setTo(EventEmitter.CLOUD_CONNECTOR)
                .setHeader(TO, ROUTE).setHeader(ID, id)
                .setHeader(COUNT, "2").setHeader(TOTAL, "2").setBody(PAYLOAD);
        assertNotNull(produce(last));
    }

    @Test
    void fullyQualifiedAndUnknownTargets() throws InterruptedException {
        // known origin: direct delivery
        EventEnvelope direct = new EventEnvelope().setTo(EventEmitter.CLOUD_CONNECTOR)
                .setHeader(TO, "some.route@" + PEER_1).setBody(PAYLOAD);
        EventEnvelope response = produce(direct);
        assertNotNull(response);
        assertEquals(true, response.getBody());
        // unknown origin: quietly dropped
        EventEnvelope unknown = new EventEnvelope().setTo(EventEmitter.CLOUD_CONNECTOR)
                .setHeader(TO, "some.route@no-such-origin").setBody(PAYLOAD);
        assertNotNull(produce(unknown));
        // unknown route: no destinations
        EventEnvelope noRoute = new EventEnvelope().setTo(EventEmitter.CLOUD_CONNECTOR)
                .setHeader(TO, "no.such.route").setBody(PAYLOAD);
        assertNotNull(produce(noRoute));
        // local route resolves to this instance
        EventEnvelope local = new EventEnvelope().setTo(EventEmitter.CLOUD_CONNECTOR)
                .setHeader(TO, ServiceDiscovery.SERVICE_QUERY).setBody(PAYLOAD);
        assertNotNull(produce(local));
    }
}
