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
                Thread.sleep(20);
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
            Thread.sleep(200);
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
