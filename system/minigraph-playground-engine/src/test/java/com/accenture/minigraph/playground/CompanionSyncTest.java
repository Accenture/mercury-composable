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
