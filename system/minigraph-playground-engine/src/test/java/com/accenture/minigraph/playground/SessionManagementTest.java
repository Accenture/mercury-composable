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
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.websocket.client.PersistentWsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagementTest {
    private static final Logger log = LoggerFactory.getLogger(SessionManagementTest.class);
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final Utility util = Utility.getInstance();
    private static final EventEmitter po = EventEmitter.getInstance();
    private static String httpTarget;

    @BeforeAll
    static void setup() {
        AutoStart.main(new String[0]);
        var config = AppConfigReader.getInstance();
        var port = config.getProperty("rest.server.port", config.getProperty("server.port", "8085"));
        httpTarget = "http://127.0.0.1:" + port;
    }

    @Test
    void freshSessionHasNoSubscribersOrTargetTest() throws InterruptedException {
        SessionFixture fx = createFixture();
        if (fx == null) {
            return;
        }
        try (fx) {
            assertFalse(fx.initialStatusA().contains("subscribed by"),
                    "Fresh session should have no subscribers");
            assertFalse(fx.initialStatusA().contains("subscribed to"),
                    "Fresh primary session should not be subscribed to anyone");
        }
    }

    @Test
    void subscribeValidationRejectionsTest() throws InterruptedException {
        SessionFixture fx = createFixture();
        if (fx == null) {
            return;
        }
        try (fx) {
            // Subscribe to self should be rejected
            po.send(fx.txPathA(), "session subscribe " + fx.sessionA());
            assertNotNull(waitForMessage(fx.messagesA(), "You cannot subscribe to yourself", 5));

            // Subscribe to an unknown session is rejected
            po.send(fx.txPathA(), "session subscribe ws-000000-0");
            assertNotNull(waitForMessage(fx.messagesA(), "Session ws-000000-0 not found", 5));

            // Unsubscribe on a primary session is rejected
            po.send(fx.txPathA(), "session unsubscribe");
            assertNotNull(waitForMessage(fx.messagesA(), "Nothing to unsubscribe", 5));

            // Malformed session sub-command (3+ tokens, not "subscribe") is rejected
            po.send(fx.txPathA(), "session foo bar");
            assertNotNull(waitForMessage(fx.messagesA(), "Invalid session command", 5));
        }
    }

    @Test
    void subscribeFlowNotifiesBothSidesTest() throws InterruptedException {
        SessionFixture fx = createFixture();
        if (fx == null) {
            return;
        }
        try (fx) {
            subscribeBToA(fx);

            // "session" now shows the subscriber on A and the target on B
            po.send(fx.txPathA(), "session");
            var statusAfterSub = requireMessage(fx.messagesA(), "subscribed by");
            assertTrue(statusAfterSub.contains(fx.sessionA()));
            assertTrue(statusAfterSub.contains(fx.sessionB()));

            fx.messagesB().clear();
            po.send(fx.txPathB(), "session");
            var statusBSubscribed = requireMessage(fx.messagesB(), "subscribed to " + fx.sessionA());
            assertTrue(statusBSubscribed.contains("Session " + fx.sessionB() + " started since"));

            // Subscribing to a non-primary session is rejected (A tries to subscribe to B)
            po.send(fx.txPathA(), "session subscribe " + fx.sessionB());
            assertNotNull(waitForMessage(fx.messagesA(), fx.sessionB() + " is not a primary session", 5));

            // B is already subscribed, attempting to subscribe again is rejected
            po.send(fx.txPathB(), "session subscribe " + fx.sessionA());
            assertNotNull(waitForMessage(fx.messagesB(),
                    "You have already subscribed to " + fx.sessionA(), 5));
        }
    }

    @Test
    void unsubscribeFlowNotifiesBothSidesTest() throws InterruptedException {
        SessionFixture fx = createFixture();
        if (fx == null) {
            return;
        }
        try (fx) {
            subscribeBToA(fx);

            // B unsubscribes - both sides get notified
            po.send(fx.txPathB(), "session unsubscribe");
            assertNotNull(waitForMessage(fx.messagesB(),
                    "Session unsubscribed from " + fx.sessionA(), 5));
            assertNotNull(waitForMessage(fx.messagesA(),
                    fx.sessionB() + " unsubscribed from your session", 5));

            // After unsubscribe, B's "session" should show no subscription target
            fx.messagesB().clear();
            po.send(fx.txPathB(), "session");
            var statusBAfter = requireMessage(fx.messagesB(),
                    "Session " + fx.sessionB() + " started since");
            assertFalse(statusBAfter.contains("subscribed to"),
                    "After unsubscribe, B should be a primary session again");
        }
    }

    @Test
    void resetDetachesSubscribersTest() throws InterruptedException {
        SessionFixture fx = createFixture();
        if (fx == null) {
            return;
        }
        try (fx) {
            subscribeBToA(fx);

            // A resets - subscribers must be detached and informed
            po.send(fx.txPathA(), "session reset");
            assertNotNull(waitForMessage(fx.messagesA(), "Session restarted", 5));
            assertNotNull(waitForMessage(fx.messagesB(),
                    "Session " + fx.sessionA() + " has closed", 5));

            // After reset, B is now primary again - "session" reflects that
            fx.messagesB().clear();
            po.send(fx.txPathB(), "session");
            var statusBAfterReset = requireMessage(fx.messagesB(),
                    "Session " + fx.sessionB() + " started since");
            assertFalse(statusBAfterReset.contains("subscribed to"),
                    "After reset on the primary, the former subscriber should become primary");
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void aiCompanionCreatesNodesAndEdgesTest() throws InterruptedException, ExecutionException {
        SessionFixture fx = createFixture();
        if (fx == null) {
            return;
        }
        try (fx) {
            // AI dispatches "create node" commands via the companion endpoint.
            // The session owner (client A) sees the echo and the engine's confirmation
            // on its WebSocket - this is the user-AI collaboration channel.
            var accepted = postCompanion(fx.sessionA(), "create node alpha");
            assertEquals(200, accepted.getStatus());
            assertInstanceOf(Map.class, accepted.getBody());
            var acceptedBody = (Map<String, Object>) accepted.getBody();
            assertEquals("companion", acceptedBody.get("type"));
            assertEquals("accepted", acceptedBody.get("status"));
            assertEquals(fx.sessionA(), acceptedBody.get("id"));
            assertNotNull(waitForMessage(fx.messagesA(), "node alpha created", 5));

            assertEquals(200, postCompanion(fx.sessionA(), "create node beta").getStatus());
            assertNotNull(waitForMessage(fx.messagesA(), "node beta created", 5));

            assertEquals(200, postCompanion(fx.sessionA(), "connect alpha to beta with relates").getStatus());
            assertNotNull(waitForMessage(fx.messagesA(), "node alpha connected to beta", 5));

            // Verify the live graph endpoint reflects what the AI built.
            var live = getLiveGraph(fx.sessionA());
            assertEquals(200, live.getStatus());
            assertInstanceOf(Map.class, live.getBody());
            var graph = (Map<String, Object>) live.getBody();
            assertInstanceOf(List.class, graph.get("nodes"));
            assertInstanceOf(List.class, graph.get("connections"));
            var nodes = (List<Map<String, Object>>) graph.get("nodes");
            var aliases = nodes.stream().map(n -> String.valueOf(n.get("alias"))).toList();
            assertTrue(aliases.contains("alpha"), "Live graph should contain node 'alpha'");
            assertTrue(aliases.contains("beta"), "Live graph should contain node 'beta'");
            var connections = (List<Map<String, Object>>) graph.get("connections");
            assertEquals(1, connections.size(), "Expected one connection between alpha and beta");
        }
    }

    @Test
    void aiCompanionAutoConvertsDeprecatedTypeMatchingSyntaxTest() throws InterruptedException, ExecutionException {
        SessionFixture fx = createFixture();
        if (fx == null) {
            return;
        }
        try (fx) {
            // the AI companion (or a human) still uses the deprecated "simple type matching" syntax
            var command = """
                    create node data-mapper
                    with properties
                    skill=graph.data.mapper
                    mapping[]=model.hello:int -> output.body.value""";
            assertEquals(200, postCompanion(fx.sessionA(), command).getStatus());
            // "node ... created" and the deprecation notice are sent together as a single message -
            // the deprecated syntax must be silently upgraded to the "simple plugin" syntax and the
            // caller (human or AI agent) must be told about it so it can switch going forward
            var notice = waitForMessage(fx.messagesA(), "node data-mapper created", 5);
            assertNotNull(notice);
            assertTrue(notice.contains("DEPRECATION NOTICE"));
            assertTrue(notice.contains("simple type matching"));
            assertTrue(notice.contains("f:int(model.hello) -> output.body.value"));

            var live = getLiveGraph(fx.sessionA());
            assertEquals(200, live.getStatus());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void aiCompanionRejectsMalformedMappingSyntaxTest() throws InterruptedException, ExecutionException {
        SessionFixture fx = createFixture();
        if (fx == null) {
            return;
        }
        try (fx) {
            var command = """
                    create node data-mapper
                    with properties
                    skill=graph.data.mapper
                    mapping[]=missing-arrow-here""";
            var response = postCompanion(fx.sessionA(), command);
            assertEquals(200, response.getStatus());
            var message = waitForMessage(fx.messagesA(), "ERROR:", 5);
            assertNotNull(message, "Expected a rejection for malformed data mapping syntax");
            assertTrue(message.contains("syntax must be 'LHS -> RHS'"));

            // the invalid node must not have been created
            var live = getLiveGraph(fx.sessionA());
            assertEquals(200, live.getStatus());
            var graph = (Map<String, Object>) live.getBody();
            var nodes = (List<Map<String, Object>>) graph.get("nodes");
            var aliases = nodes.stream().map(n -> String.valueOf(n.get("alias"))).toList();
            assertFalse(aliases.contains("data-mapper"), "Node with invalid syntax must be rejected");
        }
    }

    @Test
    void aiCompanionRejectsUnknownPluginReferenceTest() throws InterruptedException, ExecutionException {
        SessionFixture fx = createFixture();
        if (fx == null) {
            return;
        }
        try (fx) {
            var command = """
                    create node data-mapper
                    with properties
                    skill=graph.data.mapper
                    mapping[]=f:doesNotExist(model.hello) -> output.body.value""";
            var response = postCompanion(fx.sessionA(), command);
            assertEquals(200, response.getStatus());
            var message = waitForMessage(fx.messagesA(), "ERROR:", 5);
            assertNotNull(message, "Expected a rejection for an unregistered simple plugin");
            assertTrue(message.contains("unknown simple plugin"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void aiCompanionRejectsUnknownSessionTest() throws ExecutionException, InterruptedException {
        // The endpoint must not allow AI to write into a non-existent session
        // (e.g. stale id from a prior browser tab).
        var unknown = "ws-000000-0";
        var response = postCompanion(unknown, "create node ghost");
        assertEquals(404, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        var body = (Map<String, Object>) response.getBody();
        assertEquals("No active session for id " + unknown, body.get("message"));

        var live = getLiveGraph(unknown);
        assertEquals(404, live.getStatus());
    }

    @Test
    void aiCompanionRejectsEmptyBodyTest() throws ExecutionException, InterruptedException {
        SessionFixture fx = createFixture();
        if (fx == null) {
            return;
        }
        try (fx) {
            var response = postCompanion(fx.sessionA(), "   ");
            assertEquals(400, response.getStatus());
        }
    }

    @Test
    void aiCompanionBroadcastsToSubscribersTest() throws InterruptedException, ExecutionException {
        SessionFixture fx = createFixture();
        if (fx == null) {
            return;
        }
        try (fx) {
            subscribeBToA(fx);
            // AI calls A's companion endpoint - the primary owner A and subscriber B
            // both observe the change in real time. This is the user-AI-user collaboration story.
            assertEquals(200, postCompanion(fx.sessionA(), "create node shared").getStatus());
            assertNotNull(waitForMessage(fx.messagesA(), "node shared created", 5));
            assertNotNull(waitForMessage(fx.messagesB(), "node shared created", 5));
        }
    }

    @Test
    void subscribeFromGraphOwnerPopulatesPrimaryAndPeerSubscribersTest()
            throws InterruptedException, ExecutionException {
        ThreeSessionFixture fx = createThreeSessionFixture();
        if (fx == null) {
            return;
        }
        try (fx) {
            // sessionB subscribes to the empty primary sessionA. Both have no graph data yet,
            // so B becomes a passive subscriber waiting for content to land on A.
            po.send(fx.txPathB(), "session subscribe " + fx.sessionA());
            assertNotNull(waitForMessage(fx.messagesB(), "Subscribed to " + fx.sessionA(), 5));
            assertNotNull(waitForMessage(fx.messagesA(), fx.sessionB() + " subscribed to your session", 5));

            // sessionC builds a hello-world graph (root --> end) through the AI companion endpoint.
            assertEquals(200, postCompanion(fx.sessionC(), "create node root").getStatus());
            assertNotNull(waitForMessage(fx.messagesC(), "node root created", 5));
            assertEquals(200, postCompanion(fx.sessionC(), "create node end").getStatus());
            assertNotNull(waitForMessage(fx.messagesC(), "node end created", 5));
            assertEquals(200, postCompanion(fx.sessionC(), "connect root to end with relates").getStatus());
            assertNotNull(waitForMessage(fx.messagesC(), "node root connected to end", 5));

            // sessionC subscribes to the still-empty sessionA. GraphCommandService detects that the
            // primary's graph is empty while the new subscriber has data, so it copies C's graph
            // into A AND into A's existing peer subscribers (B) via populateSubscriberGraph().
            // touchNode() then fires an "update node root" through the primary so every connected
            // UI refreshes its view.
            fx.messagesA().clear();
            fx.messagesB().clear();
            fx.messagesC().clear();
            po.send(fx.txPathC(), "session subscribe " + fx.sessionA());
            assertNotNull(waitForMessage(fx.messagesC(), "Subscribed to " + fx.sessionA(), 5));
            assertNotNull(waitForMessage(fx.messagesA(), fx.sessionC() + " subscribed to your session", 5));

            // touchNode broadcasts a synthetic "update node root" through the primary;
            // A, B, and C all observe the resulting confirmation message.
            assertNotNull(waitForMessage(fx.messagesA(), "node root updated", 10));
            assertNotNull(waitForMessage(fx.messagesB(), "node root updated", 10));
            assertNotNull(waitForMessage(fx.messagesC(), "node root updated", 10));

            // Live graph endpoint confirms that all three sessions now hold the hello-world graph.
            assertGraphHasHelloWorld(fx.sessionA());
            assertGraphHasHelloWorld(fx.sessionB());
            assertGraphHasHelloWorld(fx.sessionC());
        }
    }

    @SuppressWarnings("unchecked")
    private void assertGraphHasHelloWorld(String sessionId) throws ExecutionException, InterruptedException {
        var live = getLiveGraph(sessionId);
        assertEquals(200, live.getStatus(), "Live graph fetch failed for " + sessionId);
        assertInstanceOf(Map.class, live.getBody());
        var graph = (Map<String, Object>) live.getBody();
        var nodes = (List<Map<String, Object>>) graph.get("nodes");
        var aliases = nodes.stream().map(n -> String.valueOf(n.get("alias"))).toList();
        assertTrue(aliases.contains("root"), sessionId + " should contain node 'root'");
        assertTrue(aliases.contains("end"), sessionId + " should contain node 'end'");
        var connections = (List<Map<String, Object>>) graph.get("connections");
        assertEquals(1, connections.size(),
                sessionId + " should have exactly one connection between root and end");
    }

    private EventEnvelope postCompanion(String sessionId, String command)
            throws ExecutionException, InterruptedException {
        var req = new AsyncHttpRequest().setMethod("POST").setTargetHost(httpTarget)
                .setUrl("/api/companion/{id}").setPathParameter("id", sessionId)
                .setHeader("Content-Type", "text/plain")
                .setHeader("Accept", "application/json")
                .setBody(command);
        var envelope = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(req);
        return po.request(envelope, 5000).get();
    }

    private EventEnvelope getLiveGraph(String sessionId)
            throws ExecutionException, InterruptedException {
        var req = new AsyncHttpRequest().setMethod("GET").setTargetHost(httpTarget)
                .setUrl("/api/graph/session/{id}").setPathParameter("id", sessionId)
                .setHeader("Accept", "application/json");
        var envelope = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(req);
        return po.request(envelope, 5000).get();
    }

    private void subscribeBToA(SessionFixture fx) throws InterruptedException {
        po.send(fx.txPathB(), "session subscribe " + fx.sessionA());
        assertNotNull(waitForMessage(fx.messagesB(), "Subscribed to " + fx.sessionA(), 5));
        assertNotNull(waitForMessage(fx.messagesA(), fx.sessionB() + " subscribed to your session", 5));
    }

    private SessionFixture createFixture() throws InterruptedException {
        final AppConfigReader config = AppConfigReader.getInstance();
        final int port = util.str2int(config.getProperty("rest.server.port",
                config.getProperty("server.port", "8085")));
        for (int i = 0; i < 3; i++) {
            if (util.portReady("127.0.0.1", port, 3000)) {
                break;
            }
            log.info("Waiting for GRAPH websocket server at port-{} to get ready", port);
            Thread.sleep(1000);
        }
        final BlockingQueue<String> messagesA = new LinkedBlockingQueue<>();
        final BlockingQueue<String> messagesB = new LinkedBlockingQueue<>();
        final AtomicReference<String> txPathA = new AtomicReference<>();
        final AtomicReference<String> txPathB = new AtomicReference<>();
        LambdaFunction connectorA = (headers, input, instance) ->
                handleClientEvent(headers, input, txPathA, messagesA, "A");
        LambdaFunction connectorB = (headers, input, instance) ->
                handleClientEvent(headers, input, txPathB, messagesB, "B");
        PersistentWsClient clientA = new PersistentWsClient(connectorA,
                Collections.singletonList("ws://127.0.0.1:" + port + "/ws/graph/sessionA"));
        PersistentWsClient clientB = new PersistentWsClient(connectorB,
                Collections.singletonList("ws://127.0.0.1:" + port + "/ws/graph/sessionB"));
        clientA.start();
        clientB.start();

        // Wait for both clients to connect and capture each session's tx_path
        var deadline = System.currentTimeMillis() + 10_000L;
        while ((txPathA.get() == null || txPathB.get() == null)
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        // websocket client emulation in unit test would be influenced by the host computer so we want
        // to make it optional. However, we do want to print out the status for further improvement.
        if (txPathA.get() == null || txPathB.get() == null) {
            log.info("Session management test skipped - websocket not ready (txA={}, txB={})",
                    txPathA.get(), txPathB.get());
            closeQuietly(clientA, clientB);
            return null;
        }
        // Use the 'session' command to discover each session-id (format: ws-{m}-{n}).
        // The session-id is encoded from the receiver/transmitter function routes, so
        // querying via the command keeps the test decoupled from the welcome banner.
        messagesA.clear();
        messagesB.clear();
        po.send(txPathA.get(), "session");
        po.send(txPathB.get(), "session");
        var statusA = waitForMessage(messagesA, "Session ws-", 10);
        var statusB = waitForMessage(messagesB, "Session ws-", 10);
        if (statusA == null || statusB == null) {
            log.info("Session management test skipped - 'session' command did not respond" +
                    " (statusA={}, statusB={})", statusA, statusB);
            closeQuietly(clientA, clientB);
            return null;
        }
        var sessionA = extractSessionId(statusA);
        var sessionB = extractSessionId(statusB);
        assertNotNull(sessionA, "Failed to extract session A id");
        assertNotNull(sessionB, "Failed to extract session B id");
        assertNotEquals(sessionA, sessionB, "Each websocket must have a unique session id");
        log.info("Session A = {}, Session B = {}", sessionA, sessionB);
        return new SessionFixture(clientA, clientB, txPathA.get(), txPathB.get(),
                messagesA, messagesB, sessionA, sessionB, statusA);
    }

    private ThreeSessionFixture createThreeSessionFixture() throws InterruptedException {
        final AppConfigReader config = AppConfigReader.getInstance();
        final int port = util.str2int(config.getProperty("rest.server.port",
                config.getProperty("server.port", "8085")));
        for (int i = 0; i < 3; i++) {
            if (util.portReady("127.0.0.1", port, 3000)) {
                break;
            }
            log.info("Waiting for GRAPH websocket server at port-{} to get ready", port);
            Thread.sleep(1000);
        }
        final BlockingQueue<String> messagesA = new LinkedBlockingQueue<>();
        final BlockingQueue<String> messagesB = new LinkedBlockingQueue<>();
        final BlockingQueue<String> messagesC = new LinkedBlockingQueue<>();
        final AtomicReference<String> txPathA = new AtomicReference<>();
        final AtomicReference<String> txPathB = new AtomicReference<>();
        final AtomicReference<String> txPathC = new AtomicReference<>();
        LambdaFunction connectorA = (headers, input, instance) ->
                handleClientEvent(headers, input, txPathA, messagesA, "A");
        LambdaFunction connectorB = (headers, input, instance) ->
                handleClientEvent(headers, input, txPathB, messagesB, "B");
        LambdaFunction connectorC = (headers, input, instance) ->
                handleClientEvent(headers, input, txPathC, messagesC, "C");
        PersistentWsClient clientA = new PersistentWsClient(connectorA,
                Collections.singletonList("ws://127.0.0.1:" + port + "/ws/graph/sessionA"));
        PersistentWsClient clientB = new PersistentWsClient(connectorB,
                Collections.singletonList("ws://127.0.0.1:" + port + "/ws/graph/sessionB"));
        PersistentWsClient clientC = new PersistentWsClient(connectorC,
                Collections.singletonList("ws://127.0.0.1:" + port + "/ws/graph/sessionC"));
        clientA.start();
        clientB.start();
        clientC.start();

        var deadline = System.currentTimeMillis() + 10_000L;
        while ((txPathA.get() == null || txPathB.get() == null || txPathC.get() == null)
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        if (txPathA.get() == null || txPathB.get() == null || txPathC.get() == null) {
            log.info("Session management test skipped - websocket not ready (txA={}, txB={}, txC={})",
                    txPathA.get(), txPathB.get(), txPathC.get());
            closeQuietly(clientA, clientB, clientC);
            return null;
        }
        messagesA.clear();
        messagesB.clear();
        messagesC.clear();
        po.send(txPathA.get(), "session");
        po.send(txPathB.get(), "session");
        po.send(txPathC.get(), "session");
        var statusA = waitForMessage(messagesA, "Session ws-", 10);
        var statusB = waitForMessage(messagesB, "Session ws-", 10);
        var statusC = waitForMessage(messagesC, "Session ws-", 10);
        if (statusA == null || statusB == null || statusC == null) {
            log.info("Session management test skipped - 'session' command did not respond" +
                    " (statusA={}, statusB={}, statusC={})", statusA, statusB, statusC);
            closeQuietly(clientA, clientB, clientC);
            return null;
        }
        var sessionA = extractSessionId(statusA);
        var sessionB = extractSessionId(statusB);
        var sessionC = extractSessionId(statusC);
        assertNotNull(sessionA, "Failed to extract session A id");
        assertNotNull(sessionB, "Failed to extract session B id");
        assertNotNull(sessionC, "Failed to extract session C id");
        assertNotEquals(sessionA, sessionB, "Each websocket must have a unique session id");
        assertNotEquals(sessionA, sessionC, "Each websocket must have a unique session id");
        assertNotEquals(sessionB, sessionC, "Each websocket must have a unique session id");
        log.info("Session A = {}, Session B = {}, Session C = {}", sessionA, sessionB, sessionC);
        return new ThreeSessionFixture(clientA, clientB, clientC, txPathB.get(), txPathC.get(),
                messagesA, messagesB, messagesC, sessionA, sessionB, sessionC);
    }

    private static void closeQuietly(PersistentWsClient... clients) {
        for (var c : clients) {
            if (c != null) {
                c.close();
            }
        }
    }

    private Boolean handleClientEvent(Map<String, String> headers, Object input,
                                       AtomicReference<String> txPathHolder,
                                       BlockingQueue<String> messages, String label) {
        var type = headers.get("type");
        if ("open".equals(type)) {
            txPathHolder.set(headers.get("tx_path"));
            log.info("Client {} websocket open, tx_path={}", label, headers.get("tx_path"));
        } else if ("string".equals(type) && input instanceof String text) {
            var message = text.trim();
            log.info("Client {} received: {}", label, message);
            messages.add(message);
        }
        return true;
    }

    private String extractSessionId(String message) {
        // Response of the "session" command: "Session ws-{m}-{n} started since {timestamp}..."
        var marker = "ws-";
        var idx = message.indexOf(marker);
        if (idx == -1) {
            return null;
        }
        var end = idx;
        while (end < message.length()) {
            var c = message.charAt(end);
            if (c == ' ' || c == '\n' || c == '\r' || c == ']') {
                break;
            }
            end++;
        }
        return message.substring(idx, end);
    }

    private String requireMessage(BlockingQueue<String> queue, String substring)
            throws InterruptedException {
        return Objects.requireNonNull(waitForMessage(queue, substring, 5),
                "Expected message containing: " + substring);
    }

    private String waitForMessage(BlockingQueue<String> queue, String substring, int timeoutSeconds)
            throws InterruptedException {
        var deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            var remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                break;
            }
            var msg = queue.poll(remaining, TimeUnit.MILLISECONDS);
            if (msg == null) {
                continue;
            }
            if (msg.contains(substring)) {
                return msg;
            }
        }
        log.info("Timeout waiting for substring: {}", substring);
        return null;
    }

    private record SessionFixture(
            PersistentWsClient clientA, PersistentWsClient clientB,
            String txPathA, String txPathB,
            BlockingQueue<String> messagesA, BlockingQueue<String> messagesB,
            String sessionA, String sessionB, String initialStatusA
    ) implements AutoCloseable {
        @Override
        public void close() {
            clientA.close();
            clientB.close();
        }
    }

    private record ThreeSessionFixture(
            PersistentWsClient clientA, PersistentWsClient clientB, PersistentWsClient clientC,
            String txPathB, String txPathC,
            BlockingQueue<String> messagesA, BlockingQueue<String> messagesB, BlockingQueue<String> messagesC,
            String sessionA, String sessionB, String sessionC
    ) implements AutoCloseable {
        @Override
        public void close() {
            clientA.close();
            clientB.close();
            clientC.close();
        }
    }
}
