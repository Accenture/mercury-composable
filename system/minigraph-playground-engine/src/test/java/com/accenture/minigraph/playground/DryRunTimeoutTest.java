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
 * The dry-run watcher: a traversal launched by the playground 'run' command now has a
 * run-level deadline (model.ttl, seedable at the instantiate edge) mirroring the deployed
 * lane's flow timer. A hung or overlong dry-run ends with the canonical failure terminal
 * instead of silence - so the console always sees an end-of-transmission line and the
 * synchronous companion drain classifies the outcome correctly instead of returning a
 * silently truncated ok:true.
 */
class DryRunTimeoutTest {
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
    void hungDryRunAbortsAtModelTtl() throws Exception {
        var po = EventEmitter.getInstance();
        var sid = "ws-990031-2";
        var inRoute = "ws.990031.2.in";
        openSession(po, sid, inRoute);
        // a graph.task node whose function outlives the run deadline: the node's own
        // child-call ttl (8s) is deliberately LONGER than model.ttl (1.5s), so only the
        // run-level watcher can end this traversal
        syncCommand(po, sid, "create node root\nwith type Root");
        syncCommand(po, sid, "create node end");
        syncCommand(po, sid, """
                create node slow
                with type task
                with properties
                skill=graph.task
                task=v1.slow.task
                ttl=8s""");
        syncCommand(po, sid, "connect root to slow with first");
        syncCommand(po, sid, "connect slow to end with second");
        var instantiated = syncCommand(po, sid, "instantiate graph\nlong(1500) -> model.ttl");
        assertEquals(Boolean.TRUE, instantiated.get("ok"), "instantiate -> ok:true: " + instantiated);
        long started = System.currentTimeMillis();
        var ran = syncCommand(po, sid, "run");
        long elapsed = System.currentTimeMillis() - started;
        // the watcher's terminal - not the companion's 30s safety net - must end the drain
        assertTrue(elapsed < 10000,
                "the drain must end on the watcher's terminal, not the safety timeout (elapsed "
                        + elapsed + " ms)");
        assertEquals(Boolean.FALSE, ran.get("ok"), "a timed-out dry-run is a failure: " + ran);
        var output = ((List<?>) ran.get("output")).stream().map(String::valueOf).toList();
        assertTrue(output.stream().anyMatch("Graph traversal timed out after 1500 ms"::equals),
                "the watcher must report the model.ttl deadline: " + output);
        assertTrue(output.stream().anyMatch("Graph traversal aborted"::equals),
                "a timed-out run must end with the canonical failure terminal: " + output);
    }

    @Test
    void completedRunDoesNotFireLateWatcher() throws Exception {
        var po = EventEmitter.getInstance();
        var platform = Platform.getInstance();
        var sid = "ws-990032-2";
        var inRoute = "ws.990032.2.in";
        var outRoute = "ws.990032.2.out";
        openSession(po, sid, inRoute);
        List<Object> teed = new CopyOnWriteArrayList<>();
        LambdaFunction outTap = (hdr, body, inst) -> {
            teed.add(body);
            return null;
        };
        platform.registerPrivate(outRoute, outTap, 1);
        try {
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
            var instantiated = syncCommand(po, sid,
                    "instantiate graph\ntext(hello) -> input.body.id\nlong(3000) -> model.ttl");
            assertEquals(Boolean.TRUE, instantiated.get("ok"), "instantiate -> ok:true: " + instantiated);
            // dispatch the run with the TAPPED route as the traversal's reply route, so a
            // stale watcher's late lines would land where this test can see them (through
            // the sync endpoint, the reply route is a per-call capture route that is
            // released after the drain - late lines would vanish and prove nothing)
            po.send(new EventEnvelope().setTo(GraphCommandService.SINGLETON_COMMAND_HANDLER)
                    .setBody(Map.of("type", "command", "in", inRoute, "out", outRoute,
                            "message", "run", "direct", true)));
            boolean completed = false;
            for (int i = 0; i < 100 && !completed; i++) {
                completed = teed.stream().filter(String.class::isInstance).map(Object::toString)
                        .anyMatch(l -> l.startsWith("Graph traversal completed in"));
                if (!completed) {
                    Utility.getInstance().sleep(50);
                }
            }
            assertTrue(completed, "the fast run must complete on the console: " + teed);
            // sleep past the 3s deadline: a canceled watcher must stay silent - a stale
            // firing would send a spurious timeout/abort line to this console after success
            Utility.getInstance().sleep(3800);
            var late = teed.stream().filter(String.class::isInstance).map(Object::toString)
                    .filter(l -> l.startsWith("Graph traversal timed out") || l.equals("Graph traversal aborted"))
                    .toList();
            assertTrue(late.isEmpty(),
                    "a completed run must cancel its watcher - no late terminal allowed: " + late);
        } finally {
            platform.release(outRoute);
        }
    }

    private void openSession(EventEmitter po, String sid, String inRoute) {
        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", inRoute)));
        for (int i = 0; i < 50 && !GraphCommandService.hasSession(sid); i++) {
            Utility.getInstance().sleep(20);
        }
        assertTrue(GraphCommandService.hasSession(sid), "session must exist before a companion command");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> syncCommand(EventEmitter po, String sid, String command) throws Exception {
        var req = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/companion/{id}/sync").setPathParameter("id", sid)
                .setHeader("Content-Type", "text/plain").setHeader("Accept", "application/json")
                .setBody(command);
        var resp = po.request(new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(req), 40000).get();
        assertEquals(200, resp.getStatus(), "sync endpoint returns 200 with the outcome in the body");
        assertInstanceOf(Map.class, resp.getBody());
        return (Map<String, Object>) resp.getBody();
    }
}
