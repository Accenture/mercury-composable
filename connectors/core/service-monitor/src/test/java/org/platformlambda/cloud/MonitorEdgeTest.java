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
import org.platformlambda.MainApp;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.Kv;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.Utility;
import org.platformlambda.mock.TestBase;
import org.platformlambda.ws.MonitorService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the presence monitor's coordination services beyond the main connectivity test:
 * the additional-info query, housekeeping of peer monitors, presence-event handling for
 * unknown members, and the topic controller's guarded request types.
 */
class MonitorEdgeTest extends TestBase {

    private static final String TYPE = "type";
    private static final String ORIGIN = "origin";
    private static final String ADDITIONAL_INFO = "additional.info";
    private static final long TIMEOUT = 10000;

    @BeforeAll
    static void waitForMember() {
        // the mock member connection is established asynchronously after boot
        for (int i = 0; i < 40 && MonitorService.getConnections().isEmpty(); i++) {
            Utility.getInstance().sleep(500);
        }
        assertFalse(MonitorService.getConnections().isEmpty(), "expect a mock member connection");
    }

    private EventEnvelope request(EventEnvelope event) throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter.getInstance().asyncRequest(event, TIMEOUT).onSuccess(bench::add);
        return bench.poll(TIMEOUT + 5000, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("unchecked")
    @Test
    void additionalInfoQuery() throws InterruptedException {
        EventEnvelope response = request(new EventEnvelope().setTo(ADDITIONAL_INFO)
                .setHeader(TYPE, "query"));
        assertNotNull(response);
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> info = (Map<String, Object>) response.getBody();
        assertTrue(info.containsKey("connections"));
        assertTrue(info.containsKey("monitors"));
        assertTrue(info.containsKey("topics"));
        assertTrue(info.containsKey("virtual_topics"));
        assertTrue(info.containsKey("total"));
        // invalid usage is rejected
        EventEnvelope invalid = request(new EventEnvelope().setTo(ADDITIONAL_INFO)
                .setHeader(TYPE, "nonsense"));
        assertNotNull(invalid);
        assertEquals(400, invalid.getStatus());
    }

    @Test
    void houseKeeperTracksPeerMonitors() throws InterruptedException {
        EventEmitter po = EventEmitter.getInstance();
        Platform platform = Platform.getInstance();
        String peerMonitor = "edge-monitor-1";
        // init for myself registers this monitor instance
        po.send(MainApp.PRESENCE_HOUSEKEEPER, new Kv(TYPE, "init"),
                new Kv(ORIGIN, platform.getOrigin()), new Kv("instance", "monitor-app-1"));
        // init for a peer triggers an alive broadcast instead
        po.send(MainApp.PRESENCE_HOUSEKEEPER, new Kv(TYPE, "init"), new Kv(ORIGIN, peerMonitor));
        // a peer keep-alive with the member list registers the peer and refreshes it
        List<String> members = new ArrayList<>(MonitorService.getConnections().keySet());
        EventEnvelope alive = new EventEnvelope().setTo(MainApp.PRESENCE_HOUSEKEEPER)
                .setBody(members)
                .setHeader(TYPE, MainApp.MONITOR_ALIVE).setHeader(ORIGIN, peerMonitor);
        assertNotNull(request(alive));
        // a second keep-alive exercises the refresh (already-known) branch
        assertNotNull(request(new EventEnvelope().setTo(MainApp.PRESENCE_HOUSEKEEPER)
                .setBody(members)
                .setHeader(TYPE, MainApp.MONITOR_ALIVE).setHeader(ORIGIN, peerMonitor)));
    }

    @Test
    void presenceHandlerEdgeCases() {
        EventEmitter po = EventEmitter.getInstance();
        // delete an unknown member connection is a quiet no-op
        po.send(MainApp.PRESENCE_HANDLER, new Kv(TYPE, "del"), new Kv(ORIGIN, "no-such-member"));
        // keep-alive for an unknown member is ignored
        po.send(MainApp.PRESENCE_HANDLER, new Kv(TYPE, "keep-alive"),
                new Kv(ORIGIN, "no-such-member"), new Kv("topic", "multiplex.0001-011"));
        // download request from a peer monitor
        po.send(MainApp.PRESENCE_HANDLER, new Kv(TYPE, "download"), new Kv(ORIGIN, "edge-monitor-2"));
        // allow the async handlers to drain
        Utility.getInstance().sleep(1000);
        assertFalse(MonitorService.getConnections().containsKey("no-such-member"));
    }

    @Test
    void topicControllerGuardedTypes() throws InterruptedException {
        EventEmitter po = EventEmitter.getInstance();
        // no type header returns false
        EventEnvelope noType = request(new EventEnvelope().setTo(MainApp.TOPIC_CONTROLLER));
        assertNotNull(noType);
        assertEquals(false, noType.getBody());
        // unknown type returns false
        EventEnvelope unknown = request(new EventEnvelope().setTo(MainApp.TOPIC_CONTROLLER)
                .setHeader(TYPE, "nonsense"));
        assertNotNull(unknown);
        assertEquals(false, unknown.getBody());
        // keep-alive for the current member refreshes its session
        String member = MonitorService.getConnections().keySet().iterator().next();
        po.send(MainApp.TOPIC_CONTROLLER, new Kv(TYPE, "keep-alive"), new Kv(ORIGIN, member));
        // release topic for an unknown origin is a guarded no-op
        po.send(MainApp.TOPIC_CONTROLLER, new Kv(TYPE, "release_topic"), new Kv(ORIGIN, "no-such-member"));
        Utility.getInstance().sleep(500);
        assertTrue(MonitorService.getConnections().containsKey(member));
    }
}
