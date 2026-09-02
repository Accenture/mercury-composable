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

import com.accenture.minigraph.mock.CountingStepTask;
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
import org.platformlambda.core.util.MultiLevelMap;
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
        } finally {
            platform.release(outRoute);
        }
    }

    /**
     * A traversal ({@code run}) is asynchronous - the handler replies before the traveler streams
     * its output. The sync response must carry the WHOLE traversal, drained on the traveler's
     * terminal line (emitted last), not a raced sentinel that truncates it - and the dry-run edge
     * guarantees a business correlation ID exactly like the REST edge does.
     */
    @Test
    void syncRunDrainsWholeTraversalWithStructuredResult() throws Exception {
        var po = EventEmitter.getInstance();
        var sid = "ws-990007-2";
        var inRoute = "ws.990007.2.in";
        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", inRoute)));
        for (int i = 0; i < 50 && !GraphCommandService.hasSession(sid); i++) {
            Utility.getInstance().sleep(20);
        }
        assertTrue(GraphCommandService.hasSession(sid), "session must exist before a companion command");
        // build a runnable graph on this session
        syncCommand(po, sid, "create node root\nwith type Root");
        syncCommand(po, sid, "create node end");
        syncCommand(po, sid, """
                create node mapper
                with type mapper
                with properties
                skill=graph.data.mapper
                mapping[]=input.body.id -> output.body""");
        syncCommand(po, sid, "connect root to mapper with first");
        syncCommand(po, sid, "connect mapper to end with second");
        var instantiated = syncCommand(po, sid, "instantiate graph\ntext(hello world) -> input.body.id");
        assertEquals(Boolean.TRUE, instantiated.get("ok"), "instantiate -> ok:true: " + instantiated);
        // the instantiate command is the dry-run's edge: it guarantees a business
        // correlation ID like the REST edge does, with a reminder when auto-created
        var initOutput = ((List<?>) instantiated.get("output")).stream().map(String::valueOf).toList();
        assertTrue(initOutput.stream().anyMatch(
                        l -> l.startsWith("No business correlation ID given - this dry-run created model.cid = ")),
                "the dry-run edge must auto-create model.cid with a reminder: " + initOutput);
        // an explicitly mapped model.cid is honored without the reminder
        var withCid = syncCommand(po, sid,
                "instantiate graph\ntext(hello world) -> input.body.id\ntext(dry-run-77) -> model.cid");
        assertEquals(Boolean.TRUE, withCid.get("ok"), "instantiate with cid -> ok:true: " + withCid);
        var withCidOutput = ((List<?>) withCid.get("output")).stream().map(String::valueOf).toList();
        assertTrue(withCidOutput.stream().noneMatch(l -> l.contains("created model.cid")),
                "a supplied model.cid must be honored without the reminder: " + withCidOutput);

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
    }

    /**
     * A failing traversal (run before instantiate, fresh session) still returns
     * promptly with the uniform terminal - the drain never hangs to the timeout.
     */
    @Test
    void failedRunDrainsOnTerminalNotTimeout() throws Exception {
        var po = EventEmitter.getInstance();
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
    }

    /**
     * The playground's "run" command reuses the deployment gate's whole-graph rules
     * just before dispatching the traveler: draft authoring allows partial models,
     * but a runnable graph must honor the suspend/resume contract - here a suspend
     * node without a ttl is rejected pre-run with the same message CompileGraph
     * would log at deployment time, and the uniform terminal line keeps the sync
     * drain deterministic.
     */
    @Test
    void preRunCheckRejectsBrokenSuspendContract() throws Exception {
        var po = EventEmitter.getInstance();
        var inRoute = "ws.990011.3.in";
        var sid = "ws-990011-3";
        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", inRoute)));
        for (int i = 0; i < 50 && !GraphCommandService.hasSession(sid); i++) {
            Utility.getInstance().sleep(20);
        }
        // the root name doubles as the dry-run graph id; incidental here - this test pins the
        // pre-run check, and neither assertion below depends on the identity
        syncCommand(po, sid, "create node root\nwith type Root\nwith properties\nname=unit-test-prerun-check");
        syncCommand(po, sid, "create node end\nwith type End");
        syncCommand(po, sid,
                "create node suspend\nwith type Suspend\nwith properties\nskill=graph.suspend\ntask=v1.file.state.store");
        syncCommand(po, sid, "connect root to suspend with then");
        syncCommand(po, sid, "connect suspend to end with then");
        var instantiated = syncCommand(po, sid, "instantiate graph");
        assertEquals(Boolean.TRUE, instantiated.get("ok"), "instantiate must succeed: " + instantiated);
        var run = syncCommand(po, sid, "run");
        assertEquals(Boolean.FALSE, run.get("ok"), "run must be rejected pre-run: " + run);
        var output = ((List<?>) run.get("output")).stream().map(String::valueOf).toList();
        assertTrue(output.stream().anyMatch(l -> l.contains("Unable to run - node suspend does not have a 'ttl'")),
                "the gate's rule message must reach the author: " + output);
        assertTrue(output.stream().anyMatch("Graph traversal aborted"::equals),
                "pre-run rejection must still emit the uniform terminal: " + output);
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

            // 4) the fire-and-forget endpoint is RETIRED (2026-09-02): the bare
            //    companion URL answers 404 via REST automation (no route mapping)
            var retired = legacyCommand(po, sid, "session");
            assertEquals(404, retired.getStatus(),
                    "the retired async endpoint must answer 404: " + retired.getBody());
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
     * a genuinely missing model prints the not-found line alone and stays {@code ok:false}.
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

        // 2) a genuinely missing model prints the not-found line alone and stays an error
        var missed = syncCommand(po, sid, "import graph from no-such-graph-xyz");
        assertEquals(Boolean.FALSE, missed.get("ok"), "a genuine miss stays ok:false: " + missed);
        assertInstanceOf(String.class, missed.get("error"));
        assertTrue(((String) missed.get("error")).contains("not found"),
                "the not-found error is returned in-band for a genuine miss: " + missed);
    }

    /**
     * tutorial-13 in the dry-run lane: 'instantiate graph' loads the model through the
     * configuration system, so the {@code ${rest.server.port:8080}} reference in the task
     * node's 'host' resolves to the application's actual port (8090 under test - success
     * proves the 8080 default was NOT used), and the 'graph.task' input mapping stages
     * model.person_id for the {model.person_id} dynamic variable in the 'url'. Deployed
     * execution of the same model is covered in GraphTaskTest - the two lanes must
     * behave the same.
     */
    @Test
    void tutorial13DryRunResolvesEnvVarAndDynamicVariable() throws Exception {
        var po = EventEmitter.getInstance();
        var sid = "ws-990005-2";
        var inRoute = "ws.990005.2.in";

        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", inRoute)));
        for (int i = 0; i < 50 && !GraphCommandService.hasSession(sid); i++) {
            Utility.getInstance().sleep(20);
        }
        assertTrue(GraphCommandService.hasSession(sid), "session must exist before a companion command");

        // the tutorial model must come from the deployed classpath copy, not a stale export
        var temp = new File("/tmp/graph", "tutorial-13.json");
        if (temp.exists()) {
            assertTrue(temp.delete(), "stale temp copy must be removed for a deterministic import");
        }
        var imported = syncCommand(po, sid, "import graph from tutorial-13");
        assertEquals(Boolean.TRUE, imported.get("ok"), "import must succeed: " + imported);
        var instantiated = syncCommand(po, sid, "instantiate graph\nint(100) -> input.body.person_id");
        assertEquals(Boolean.TRUE, instantiated.get("ok"), "instantiate -> ok:true: " + instantiated);
        var ran = syncCommand(po, sid, "run");
        assertEquals(Boolean.TRUE, ran.get("ok"), "dry-run must succeed: " + ran);
        // the traversal's JSON payload arrives in 'result' (console narration is 'output')
        assertInstanceOf(List.class, ran.get("result"), "the graph output is returned in-band: " + ran);
        var results = (List<?>) ran.get("result");
        assertInstanceOf(Map.class, results.getFirst());
        @SuppressWarnings("unchecked")
        var mm = new MultiLevelMap((Map<String, Object>) results.getFirst());
        assertEquals("Peter", mm.getElement("output.body.profile.name"));
        assertEquals("100 World Blvd", mm.getElement("output.body.profile.address"));
    }

    /**
     * The dry-run twin of the generic exception context: the traveler stages
     * error.source/code/message/stack when a failed node routes to its exception=
     * handler, and 'inspect error' shows the staged context - the 'error' namespace
     * is a first-class state-machine citizen like 'model', so the inspect command
     * needs no special case.
     */
    @Test
    void dryRunStagesErrorContextAndInspectErrorShowsIt() throws Exception {
        var po = EventEmitter.getInstance();
        var sid = "ws-990012-1";
        var inRoute = "ws.990012.1.in";

        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", inRoute)));
        for (int i = 0; i < 50 && !GraphCommandService.hasSession(sid); i++) {
            Utility.getInstance().sleep(20);
        }
        assertTrue(GraphCommandService.hasSession(sid), "session must exist before a companion command");

        var temp = new File("/tmp/graph", "unit-test-error-context.json");
        if (temp.exists()) {
            assertTrue(temp.delete(), "stale temp copy must be removed for a deterministic import");
        }
        var imported = syncCommand(po, sid, "import graph from unit-test-error-context");
        assertEquals(Boolean.TRUE, imported.get("ok"), "import must succeed: " + imported);
        var instantiated = syncCommand(po, sid,
                "instantiate graph\ntext(task) -> input.body.mode\ntext(dry-err-1) -> model.cid");
        assertEquals(Boolean.TRUE, instantiated.get("ok"), "instantiate -> ok:true: " + instantiated);
        var ran = syncCommand(po, sid, "run");
        assertEquals(Boolean.TRUE, ran.get("ok"), "dry-run must succeed: " + ran);
        assertInstanceOf(List.class, ran.get("result"), "the graph output is returned in-band: " + ran);
        @SuppressWarnings("unchecked")
        var mm = new MultiLevelMap((Map<String, Object>) ((List<?>) ran.get("result")).getFirst());
        assertEquals("handled", mm.getElement("output.body.stage"));
        assertEquals("fail-task", mm.getElement("output.body.source"));
        assertEquals(400, mm.getElement("output.body.code"));
        // 'inspect error' returns the staged exception context - same mechanics as 'inspect model'
        var inspected = syncCommand(po, sid, "inspect error");
        assertEquals(Boolean.TRUE, inspected.get("ok"), "inspect error -> ok:true: " + inspected);
        assertInstanceOf(List.class, inspected.get("result"), "inspect payload is in-band: " + inspected);
        @SuppressWarnings("unchecked")
        var context = new MultiLevelMap((Map<String, Object>) ((List<?>) inspected.get("result")).getFirst());
        assertEquals("error", context.getElement("inspect"));
        assertEquals("fail-task", context.getElement("outcome.source"));
        assertEquals(400, context.getElement("outcome.code"));
        assertEquals("just a test", context.getElement("outcome.message"));
        assertNotNull(context.getElement("outcome.stack"), "error.stack must be staged when available");
    }

    /**
     * tutorial-12 in the dry-run lane: the generic retry handler recovers the fetcher,
     * and the walker resolves the virtual 'error' node - after the run,
     * 'inspect error' reports code=200 with the source kept and the failure details
     * removed (a stale 401 here misled the operator before this refinement).
     */
    @Test
    void successfulRetryResolvesErrorContextInDryRun() throws Exception {
        var po = EventEmitter.getInstance();
        var sid = "ws-990013-1";
        var inRoute = "ws.990013.1.in";

        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", inRoute)));
        for (int i = 0; i < 50 && !GraphCommandService.hasSession(sid); i++) {
            Utility.getInstance().sleep(20);
        }
        assertTrue(GraphCommandService.hasSession(sid), "session must exist before a companion command");

        var temp = new File("/tmp/graph", "tutorial-12.json");
        if (temp.exists()) {
            assertTrue(temp.delete(), "stale temp copy must be removed for a deterministic import");
        }
        var imported = syncCommand(po, sid, "import graph from tutorial-12");
        assertEquals(Boolean.TRUE, imported.get("ok"), "import must succeed: " + imported);
        var instantiated = syncCommand(po, sid,
                "instantiate graph\nint(100) -> input.body.person_id\nboolean(true) -> input.body.exception");
        assertEquals(Boolean.TRUE, instantiated.get("ok"), "instantiate -> ok:true: " + instantiated);
        var ran = syncCommand(po, sid, "run");
        assertEquals(Boolean.TRUE, ran.get("ok"), "dry-run must succeed: " + ran);
        // the generic handler retried the fetcher to success - the run's output is the profile
        assertInstanceOf(List.class, ran.get("result"), "the graph output is returned in-band: " + ran);
        @SuppressWarnings("unchecked")
        var mm = new MultiLevelMap((Map<String, Object>) ((List<?>) ran.get("result")).getFirst());
        assertEquals("Peter", mm.getElement("output.body.name"));
        // the virtual 'error' node reports the RECOVERY, not the stale failure
        var inspected = syncCommand(po, sid, "inspect error");
        assertEquals(Boolean.TRUE, inspected.get("ok"), "inspect error -> ok:true: " + inspected);
        @SuppressWarnings("unchecked")
        var context = new MultiLevelMap((Map<String, Object>) ((List<?>) inspected.get("result")).getFirst());
        assertEquals(200, context.getElement("outcome.code"));
        assertEquals("fetcher", context.getElement("outcome.source"));
        assertNull(context.getElement("outcome.message"), "the failure message must be removed on recovery");
        assertNull(context.getElement("outcome.stack"), "the failure stack must be removed on recovery");
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

        // the contract view of a deployed model: purpose, size, and the
        // input/output surface derived from the model's own mappings
        var contract = syncCommand(po, sid, "describe graph tutorial-3");
        assertEquals(Boolean.TRUE, contract.get("ok"), "describe graph {id} -> ok:true: " + contract);
        var contractText = String.valueOf(contract.get("output"));
        assertTrue(contractText.contains("Deployed graph model 'tutorial-3'"), "header: " + contractText);
        assertTrue(contractText.contains("  input.body.person_id\n"), "derived input surface (exact line): " + contractText);
        assertTrue(contractText.contains("  output.body.name\n"), "derived output surface (exact line): " + contractText);
    }

    /**
     * Findings #62/#63 (HTTPS drive pre-flight): the /sync contract gaps.
     * #62 - a synchronous companion RPC is a deliberate request: the 1-second
     * identical-command dedup guard (a WS double-submit protection) must NOT
     * silently swallow a repeat; the guard stays intact for the WS path.
     * #63 - a malformed command answered with a "Syntax: ..." usage hint did
     * nothing: the envelope must say ok:false with the hint as the error.
     */
    @Test
    void companionSyncContractGapsClosed() throws Exception {
        var po = EventEmitter.getInstance();
        var platform = Platform.getInstance();
        var sid = "ws-990005-2";
        var inRoute = "ws.990005.2.in";

        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", inRoute)));
        for (int i = 0; i < 50 && !GraphCommandService.hasSession(sid); i++) {
            Utility.getInstance().sleep(20);
        }
        assertTrue(GraphCommandService.hasSession(sid), "session must exist before a companion command");

        // #62 - the same command twice, back-to-back (well inside the 1s window):
        // both must execute and both envelopes must carry the echo + output
        var first = syncCommand(po, sid, "list nodes");
        var second = syncCommand(po, sid, "list nodes");
        assertEquals(Boolean.TRUE, first.get("ok"), "first repeat must execute: " + first);
        assertEquals(Boolean.TRUE, second.get("ok"), "second repeat must execute: " + second);
        assertTrue(String.valueOf(first.get("output")).contains("list nodes"),
                "first envelope must carry the echo: " + first);
        assertTrue(String.valueOf(second.get("output")).contains("list nodes"),
                "second envelope must carry the echo (not silently dropped): " + second);

        // #63 - a malformed command answered with the usage hint is a failed
        // command: ok:false, the hint in-band as the error
        var bad = syncCommand(po, sid, "connect a to b with type x");
        assertEquals(Boolean.FALSE, bad.get("ok"), "usage response must classify as failure: " + bad);
        assertInstanceOf(String.class, bad.get("error"));
        assertTrue(((String) bad.get("error")).startsWith("Syntax:"),
                "the usage hint must be the in-band error: " + bad);

        // the WS-path guard is untouched: two identical NON-direct commands within
        // the window - the second is dropped, so the console sees exactly one echo
        var sid2 = "ws-990006-2";
        var in2 = "ws.990006.2.in";
        var out2 = "ws.990006.2.out";
        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", in2)));
        for (int i = 0; i < 50 && !GraphCommandService.hasSession(sid2); i++) {
            Utility.getInstance().sleep(20);
        }
        List<Object> teed = new CopyOnWriteArrayList<>();
        LambdaFunction outTap = (hdr, body, inst) -> {
            teed.add(body);
            return null;
        };
        platform.registerPrivate(out2, outTap, 1);
        try {
            // dispatch via the 1-instance singleton (FIFO) so the pair is processed
            // sequentially - the deterministic path for observing the guard
            for (int i = 0; i < 2; i++) {
                po.send(new EventEnvelope().setTo(GraphCommandService.SINGLETON_COMMAND_HANDLER)
                        .setBody(Map.of("type", "command", "in", in2, "out", out2,
                                "message", "list nodes")));
            }
            Utility.getInstance().sleep(300);
            var echoes = teed.stream()
                    .filter(String.class::isInstance)
                    .map(Object::toString)
                    .filter(l -> l.contains("list nodes"))
                    .count();
            assertEquals(1, echoes,
                    "WS double-submit guard must still drop the duplicate: " + teed);
        } finally {
            platform.release(out2);
        }
    }

    private EventEnvelope legacyCommand(EventEmitter po, String sid, String command) throws Exception {
        // the RETIRED fire-and-forget URL - kept only to pin its 404
        var req = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/companion/{id}").setPathParameter("id", sid)
                .setHeader("Content-Type", "text/plain").setHeader("Accept", "application/json")
                .setBody(command);
        return po.request(new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(req), 10000).get();
    }


    /**
     * The dry-run twin of the field regression that surfaced on tutorial-14: a suspended dry-run
     * workflow must RESUME when the graph is re-instantiated with the same business correlation
     * ID. The store contract scopes records by graph + cid, so the dry-run instance must present
     * an identity that is STABLE across instantiations - here the root node's name - because a
     * per-instantiation handle writes the suspension under a key no later instantiation can ever
     * read, and every resume silently restarts fresh. The nameless twin is
     * {@link #unnamedDraftResumesAcrossInstantiations()}.
     */
    @Test
    void dryRunResumesAcrossInstantiations() throws Exception {
        var po = EventEmitter.getInstance();
        var sid = "ws-990014-2";
        var inRoute = "ws.990014.2.in";
        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", inRoute)));
        for (int i = 0; i < 50 && !GraphCommandService.hasSession(sid); i++) {
            Utility.getInstance().sleep(20);
        }
        assertTrue(GraphCommandService.hasSession(sid), "session must exist before a companion command");
        // the model must import from the deployed classpath copy (a stale temp copy from an
        // earlier manual run would shadow it)
        var temp = new File("/tmp/graph", "unit-test-suspend-1.json");
        if (temp.exists()) {
            assertTrue(temp.delete(), "stale temp copy removed");
        }
        var cid = "dry-run-scope-1";
        // consume-on-retrieve makes a leftover record indistinguishable from this test's own
        var stale = new File("/tmp/suspend-resume", "unit-test-suspend-1_" + cid);
        if (stale.exists()) {
            assertTrue(stale.delete(), "stale store record removed");
        }
        var imported = syncCommand(po, sid, "import graph from unit-test-suspend-1");
        assertEquals(Boolean.TRUE, imported.get("ok"), "import: " + imported);

        // run 1: fresh transaction suspends at the checkpoint and persists under graph + cid
        var instantiated = syncCommand(po, sid,
                "instantiate graph\ntext(" + cid + ") -> model.cid");
        assertEquals(Boolean.TRUE, instantiated.get("ok"), "instantiate: " + instantiated);
        var first = syncCommand(po, sid, "run");
        assertEquals(Boolean.TRUE, first.get("ok"), "run 1: " + first);
        assertEquals(1, CountingStepTask.getCount("one", cid), "step-1 executes on the fresh run");
        assertEquals(0, CountingStepTask.getCount("two", cid), "the suspension stops before step-2");
        // the KEY pin: the record must be scoped by the model's stable name, so a later
        // instantiation (or the production executor) can find it
        var storedRecord = new File("/tmp/suspend-resume", "unit-test-suspend-1_" + cid);
        assertTrue(storedRecord.exists(),
                "the suspension must persist under the model's stable identity, not an ephemeral "
                        + "per-instantiation handle");

        // run 2: a NEW instantiation with the same business cid must resume past the checkpoint
        var again = syncCommand(po, sid,
                "instantiate graph\ntext(" + cid + ") -> model.cid");
        assertEquals(Boolean.TRUE, again.get("ok"), "re-instantiate: " + again);
        var second = syncCommand(po, sid, "run");
        assertEquals(Boolean.TRUE, second.get("ok"), "run 2: " + second);
        assertEquals(1, CountingStepTask.getCount("one", cid), "the checkpoint must not re-execute");
        assertEquals(1, CountingStepTask.getCount("two", cid), "the continuation must run on resume");
        assertFalse(storedRecord.exists(), "the record is consumed on resume (at-most-once)");
    }

    /**
     * The nameless twin of {@link #dryRunResumesAcrossInstantiations()}: a draft sketched in the
     * playground has no root {@code name} yet, and must still suspend and resume - the dry-run
     * lane scopes such a draft under the stable constant {@code untitled}. This is the branch the
     * regression lived in: the identity only has to be STABLE across instantiations, so anything
     * per-instantiation (a UUID handle) writes a suspension no later run can read and silently
     * restarts fresh instead of resuming.
     */
    @Test
    void unnamedDraftResumesAcrossInstantiations() throws Exception {
        var po = EventEmitter.getInstance();
        var sid = "ws-990016-2";
        var inRoute = "ws.990016.2.in";
        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", inRoute)));
        for (int i = 0; i < 50 && !GraphCommandService.hasSession(sid); i++) {
            Utility.getInstance().sleep(20);
        }
        assertTrue(GraphCommandService.hasSession(sid), "session must exist before a companion command");
        var cid = "dry-run-untitled-1";
        // consume-on-retrieve makes a leftover record indistinguishable from this test's own
        var storedRecord = new File("/tmp/suspend-resume", "untitled_" + cid);
        if (storedRecord.exists()) {
            assertTrue(storedRecord.delete(), "stale store record removed");
        }
        // sketch a suspend/resume draft with NO name on the root - the edge-mode shape:
        // a drawn edge into the suspend node, plus the mandatory continuation edge
        for (var command : List.of(
                "create node root\nwith type Root",
                "create node end\nwith type End",
                """
                create node resume
                with type Resume
                with properties
                skill=graph.resume
                task=v1.file.state.store""",
                """
                create node step-1
                with type Suspensible
                with properties
                skill=graph.task
                task=v1.counting.step
                input[]=text(u-one) -> step
                input[]=model.cid -> cid
                output[]=result.count -> model.step1_count""",
                """
                create node suspend
                with type Suspend
                with properties
                skill=graph.suspend
                task=v1.file.state.store
                ttl=30s""",
                """
                create node step-2
                with type Task
                with properties
                skill=graph.task
                task=v1.counting.step
                input[]=text(u-two) -> step
                input[]=model.cid -> cid
                input[]=model.step1_count -> prior
                output[]=result -> output.body""",
                "connect root to resume with test",
                "connect resume to step-1 with test",
                "connect step-1 to suspend with checkpoint",
                "connect step-1 to step-2 with approved",
                "connect suspend to end with test",
                "connect step-2 to end with test")) {
            syncCommand(po, sid, command);
        }

        // run 1: the nameless draft suspends at the checkpoint
        var instantiated = syncCommand(po, sid, "instantiate graph\ntext(" + cid + ") -> model.cid");
        assertEquals(Boolean.TRUE, instantiated.get("ok"), "a nameless draft must instantiate: " + instantiated);
        var first = syncCommand(po, sid, "run");
        assertEquals(Boolean.TRUE, first.get("ok"), "run 1: " + first);
        assertEquals(1, CountingStepTask.getCount("u-one", cid), "step-1 executes on the fresh run");
        assertEquals(0, CountingStepTask.getCount("u-two", cid), "the suspension stops before step-2");
        assertTrue(storedRecord.exists(),
                "a nameless draft must persist under the stable 'untitled' scope, not a "
                        + "per-instantiation handle: " + storedRecord);

        // run 2: a NEW instantiation with the same business cid resumes past the checkpoint
        var again = syncCommand(po, sid, "instantiate graph\ntext(" + cid + ") -> model.cid");
        assertEquals(Boolean.TRUE, again.get("ok"), "re-instantiate: " + again);
        var second = syncCommand(po, sid, "run");
        assertEquals(Boolean.TRUE, second.get("ok"), "run 2: " + second);
        assertEquals(1, CountingStepTask.getCount("u-one", cid), "the checkpoint must not re-execute");
        assertEquals(1, CountingStepTask.getCount("u-two", cid), "the continuation must run on resume");
        assertFalse(storedRecord.exists(), "the record is consumed on resume (at-most-once)");
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
