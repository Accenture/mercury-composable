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

import com.accenture.minigraph.services.GraphCommandService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the synchronous AI-companion endpoint (ADR-0008): it returns the command
 * outcome in-band ({@code ok}/{@code output}/{@code error}/{@code result}) instead of a
 * fire-and-forget acknowledgement, and tees the same output to the session's WebSocket
 * {@code .out} route so a human watches live.
 */
class CompanionSyncTest {
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static String target;

    @BeforeAll
    static void setup() {
        AutoStart.main(new String[0]);
        var config = AppConfigReader.getInstance();
        var port = config.getProperty("rest.server.port", config.getProperty("server.port", "8085"));
        target = "http://127.0.0.1:" + port;
    }

    @Test
    void syncCompanionReturnsOutcomeInBandAndTees() throws Exception {
        var po = EventEmitter.getInstance();
        var platform = Platform.getInstance();
        var sid = "ws-990001-2";
        var inRoute = "ws.990001.2.in";
        var outRoute = "ws.990001.2.out";

        // create the session (mimic the WebSocket "open" event)
        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", inRoute)));
        boolean ready = false;
        for (int i = 0; i < 50 && !ready; i++) {
            if (GraphCommandService.hasSession(sid)) {
                ready = true;
            } else {
                Utility.getInstance().sleep(20);
            }
        }
        assertTrue(GraphCommandService.hasSession(sid), "session must exist before a companion command");

        // stand in for the session's WebSocket .out route, to prove the tee (live human view)
        List<Object> teed = new CopyOnWriteArrayList<>();
        LambdaFunction outTap = (hdr, body, inst) -> {
            teed.add(body);
            return null;
        };
        platform.registerPrivate(outRoute, outTap, 1);
        try {
            // 1) an invalid command -> ok:false, error present in-band (the blind spot, closed)
            var bad = syncCommand(po, sid, "flibbertigibbet not a command");
            assertEquals(Boolean.FALSE, bad.get("ok"), "invalid command -> ok:false");
            assertInstanceOf(String.class, bad.get("error"), "error text returned in-band, not WS-only");

            // 2) a valid command -> ok:true, error null, output populated
            var good = syncCommand(po, sid, "create node root\nwith type Root");
            assertEquals(Boolean.TRUE, good.get("ok"), "valid command -> ok:true");
            assertNull(good.get("error"), "no error on success");
            assertInstanceOf(List.class, good.get("output"));
            assertFalse(((List<?>) good.get("output")).isEmpty(), "console output returned in-band");

            // 3) the tee: the same output also reached the session's WebSocket .out route
            Utility.getInstance().sleep(200);
            var teedText = teed.stream()
                    .filter(String.class::isInstance)
                    .map(Object::toString)
                    .reduce("", (a, b) -> a + "\n" + b);
            assertTrue(teedText.contains("node root created"),
                    "sync output must be teed to the session's WS .out for the live human view: " + teed);

            // 4) a traversal (`run`) is asynchronous - the handler replies before the
            //    traveler streams its output. The sync response must carry the WHOLE
            //    traversal, drained on the traveler's terminal line (emitted last), not a
            //    raced sentinel that truncates it. Build a runnable graph and run it.
            syncCommand(po, sid, "create node end");
            syncCommand(po, sid, "create node mapper\nwith type mapper\nwith properties\n"
                    + "skill=graph.data.mapper\nmapping[]=input.body.id -> output.body");
            syncCommand(po, sid, "connect root to mapper with first");
            syncCommand(po, sid, "connect mapper to end with second");
            var instantiated = syncCommand(po, sid, "instantiate graph\ntext(hello world) -> input.body.id");
            assertEquals(Boolean.TRUE, instantiated.get("ok"), "instantiate -> ok:true: " + instantiated);

            var ran = syncCommand(po, sid, "run");
            assertEquals(Boolean.TRUE, ran.get("ok"), "sync run -> ok:true: " + ran);
            var runOutput = ((List<?>) ran.get("output")).stream().map(String::valueOf).toList();
            assertTrue(runOutput.stream().anyMatch("Walk to root"::equals),
                    "sync run captures the traversal start: " + runOutput);
            assertTrue(runOutput.stream().anyMatch(l -> l.startsWith("Executed mapper with skill graph.data.mapper")),
                    "sync run captures mid-traversal skill execution: " + runOutput);
            assertTrue(runOutput.stream().anyMatch(l -> l.startsWith("Graph traversal completed in")),
                    "sync run must capture the traversal terminal (drain waited for it): " + runOutput);
            assertNotNull(ran.get("result"), "sync run returns the output.body as structured result: " + ran);
            assertTrue(String.valueOf(ran.get("result")).contains("hello world"),
                    "structured result carries the run's output.body: " + ran.get("result"));

            // 5) a failing traversal (run before instantiate, fresh session) still returns
            //    promptly with the uniform terminal - the drain never hangs to the timeout.
            var badIn = "ws.990009.2.in";
            var badId = "ws-990009-2";
            po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                    .setBody(Map.of("type", "open", "in", badIn)));
            for (int i = 0; i < 50 && !GraphCommandService.hasSession(badId); i++) {
                Utility.getInstance().sleep(20);
            }
            long started = System.currentTimeMillis();
            var badRun = syncCommand(po, badId, "run");
            assertTrue(System.currentTimeMillis() - started < 10000,
                    "a failed run must drain on the terminal, not the safety timeout");
            assertEquals(Boolean.FALSE, badRun.get("ok"), "run with no instance -> ok:false: " + badRun);
            var badRunOutput = ((List<?>) badRun.get("output")).stream().map(String::valueOf).toList();
            assertTrue(badRunOutput.stream().anyMatch("Graph traversal aborted"::equals),
                    "every run ends with a terminal, even on early failure: " + badRunOutput);
        } finally {
            platform.release(outRoute);
        }
    }

    /**
     * A companion is an <b>assistant to</b> a session, not a WebSocket session of its own —
     * so both companion endpoints limit the {@code session} command to the read-only status
     * query: the topology subcommands (subscribe/unsubscribe/reset) are rejected before
     * dispatch. Executed on the sync path they would durably register the per-request
     * {@code companion.sync.<uuid>} capture route as a subscriber.
     */
    @Test
    void companionEndpointsLimitSessionCommandToReadOnly() throws Exception {
        var po = EventEmitter.getInstance();
        var platform = Platform.getInstance();
        var sid = "ws-990002-2";
        var inRoute = "ws.990002.2.in";
        var outRoute = "ws.990002.2.out";

        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", inRoute)));
        for (int i = 0; i < 50 && !GraphCommandService.hasSession(sid); i++) {
            Utility.getInstance().sleep(20);
        }
        assertTrue(GraphCommandService.hasSession(sid), "session must exist before a companion command");

        List<Object> teed = new CopyOnWriteArrayList<>();
        LambdaFunction outTap = (hdr, body, inst) -> {
            teed.add(body);
            return null;
        };
        platform.registerPrivate(outRoute, outTap, 1);
        try {
            // 1) every topology-mutating form is rejected in-band on the sync endpoint
            for (var command : List.of("session subscribe ws-990001-2", "session unsubscribe", "session reset")) {
                var refused = syncCommand(po, sid, command);
                assertEquals(Boolean.FALSE, refused.get("ok"), "rejected: " + refused);
                assertInstanceOf(String.class, refused.get("error"));
                assertTrue(((String) refused.get("error")).contains("not available on the companion endpoint"),
                        "refusal reason returned in-band: " + refused);
            }

            // 2) nothing was registered: this session's read-only status query still works
            //    and shows no subscription; no capture route appears anywhere
            var status = syncCommand(po, sid, "session");
            assertEquals(Boolean.TRUE, status.get("ok"), "read-only 'session' stays allowed: " + status);
            var statusText = String.valueOf(status.get("output"));
            assertFalse(statusText.contains("subscribed to"),
                    "the rejected subscribe must not mark this session as subscribed: " + status);
            assertFalse(statusText.contains("companion.sync"),
                    "no capture-route subscriber may be registered: " + status);

            // 3) the refusal is also teed to the session's WS console for the human
            Utility.getInstance().sleep(200);
            assertTrue(teed.stream().filter(String.class::isInstance).map(Object::toString)
                            .anyMatch(l -> l.contains("not available on the companion endpoint")),
                    "refusal must be visible on the live console: " + teed);

            // 4) the fire-and-forget endpoint enforces the same restriction with a 400,
            //    while its read-only 'session' status query still dispatches (accepted)
            var refusedLegacy = legacyCommand(po, sid, "session subscribe ws-990001-2");
            assertEquals(400, refusedLegacy.getStatus(), "legacy endpoint refuses with 400");
            assertTrue(String.valueOf(refusedLegacy.getBody()).contains("not available on the companion endpoint"),
                    "legacy refusal carries the reason: " + refusedLegacy.getBody());
            var statusLegacy = legacyCommand(po, sid, "session");
            assertEquals(200, statusLegacy.getStatus(),
                    "read-only 'session' stays allowed on the legacy endpoint: " + statusLegacy.getBody());
        } finally {
            platform.release(outRoute);
        }
    }

    /**
     * The {@code ok} flag is derived from the console lines with <b>whole-output</b>
     * context: {@code import graph from {deployed}} legitimately prints
     * "Graph model not found in /tmp/..." before falling back to the deployed
     * classpath copy — a benign line that must not mark the command failed. It is
     * forgiven only when the same output also carries the fallback's success marker;
     * a genuine miss prints the not-found line alone and stays {@code ok:false}.
     */
    @Test
    void companionSyncImportFallbackReportsOk() throws Exception {
        var po = EventEmitter.getInstance();
        var sid = "ws-990003-2";
        var inRoute = "ws.990003.2.in";

        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", inRoute)));
        for (int i = 0; i < 50 && !GraphCommandService.hasSession(sid); i++) {
            Utility.getInstance().sleep(20);
        }
        assertTrue(GraphCommandService.hasSession(sid), "session must exist before a companion command");

        // guarantee the fallback path: the graph must exist ONLY as a deployed classpath copy
        // (tutorial-113 ships in classpath:/graph and no test exports it, but a stale temp
        // copy from an earlier manual run would short-circuit the fallback)
        var temp = new File("/tmp/graph", "tutorial-113.json");
        if (temp.exists()) {
            assertTrue(temp.delete(), "stale temp copy must be removed to exercise the fallback");
        }

        // 1) deployed-only graph: the benign not-found line is forgiven -> ok:true
        var imported = syncCommand(po, sid, "import graph from tutorial-113");
        var lines = ((List<?>) imported.get("output")).stream().map(String::valueOf).toList();
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("Graph model not found in")),
                "the fallback prints the benign not-found line first: " + lines);
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("Found deployed graph model")),
                "the deployed copy must be found for this test to be meaningful: " + lines);
        assertEquals(Boolean.TRUE, imported.get("ok"),
                "the benign fallback must not be classified an error: " + imported);
        assertNull(imported.get("error"), "no error on a successful fallback import: " + imported);

        // 2) a genuine miss prints the not-found line alone and stays an error
        var missed = syncCommand(po, sid, "import graph from no-such-graph-xyz");
        assertEquals(Boolean.FALSE, missed.get("ok"), "a genuine miss stays ok:false: " + missed);
        assertInstanceOf(String.class, missed.get("error"));
        assertTrue(((String) missed.get("error")).contains("not found"),
                "the genuine miss carries the not-found error in-band: " + missed);
    }

    /**
     * Discovery commands (read-only): "list graphs" enumerates the deployable
     * graph models (compiled registry + deployed folder) with each root's
     * "purpose", and "list flows" the Event Script flows - so an agent can
     * find extension={graph-id} / extension=flow://{flow-id} targets without
     * an out-of-band brief.
     */
    @Test
    void discoveryCommandsListDeployedGraphsAndFlows() throws Exception {
        var po = EventEmitter.getInstance();
        var sid = "ws-990004-2";
        var inRoute = "ws.990004.2.in";

        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", inRoute)));
        for (int i = 0; i < 50 && !GraphCommandService.hasSession(sid); i++) {
            Utility.getInstance().sleep(20);
        }
        assertTrue(GraphCommandService.hasSession(sid), "session must exist before a companion command");

        var graphs = syncCommand(po, sid, "list graphs");
        assertEquals(Boolean.TRUE, graphs.get("ok"), "list graphs -> ok:true: " + graphs);
        var graphText = String.valueOf(graphs.get("output"));
        assertTrue(graphText.contains("extension={graph-id} targets"), "graphs header: " + graphText);
        assertTrue(graphText.contains("tutorial-1"), "deployed tutorial-1 expected: " + graphText);
        assertTrue(graphText.contains("unit-test-join-chain"), "manifest fixture expected: " + graphText);

        var flows = syncCommand(po, sid, "list flows");
        assertEquals(Boolean.TRUE, flows.get("ok"), "list flows -> ok:true: " + flows);
        var flowText = String.valueOf(flows.get("output"));
        assertTrue(flowText.contains("extension=flow://{flow-id} targets"), "flows header: " + flowText);
        assertTrue(flowText.contains("graph-executor"), "the engine's own flow must be listed: " + flowText);
    }

    private EventEnvelope legacyCommand(EventEmitter po, String sid, String command) throws Exception {
        var req = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/companion/{id}").setPathParameter("id", sid)
                .setHeader("Content-Type", "text/plain").setHeader("Accept", "application/json")
                .setBody(command);
        return po.request(new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(req), 10000).get();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> syncCommand(EventEmitter po, String sid, String command) throws Exception {
        var req = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/companion/{id}/sync").setPathParameter("id", sid)
                .setHeader("Content-Type", "text/plain").setHeader("Accept", "application/json")
                .setBody(command);
        var resp = po.request(new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(req), 10000).get();
        assertEquals(200, resp.getStatus(), "sync endpoint returns 200 with the outcome in the body");
        assertInstanceOf(Map.class, resp.getBody());
        return (Map<String, Object>) resp.getBody();
    }
}
