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
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Claims-fixture pin for claim id {@code export-allows-orphans}: the Playground
 * "export graph as ..." command does NOT reject orphan (unconnected, non-root) nodes -
 * there is no orphan rule in GraphCommandService.handleExportCommand or
 * MiniGraph.exportGraph. Export is a draft-authoring operation; whole-graph validation
 * (including connectivity) is the CompileGraph deployment gate's job, applied at
 * deploy time.
 *
 * <p>This test fails if an orphan rule is ever added to the export path (the export
 * would be rejected) or if exportGraph starts silently dropping unconnected nodes
 * (the orphan alias would be missing from the exported file).
 */
class ClaimExportAllowsOrphansTest {
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
    void exportAcceptsGraphWithOrphanNodeAndKeepsItInTheFile() throws Exception {
        var po = EventEmitter.getInstance();
        var file = new File("/tmp/graph/claim-orphan-export.json");
        if (file.exists()) {
            assertTrue(file.delete(), "stale test file must be removable");
        }
        try {
            var sid = openCompanionSession("990051");
            syncCommand(po, sid, "create node root\nwith type Root");
            // a non-root node with NO connection to anything: a true orphan
            syncCommand(po, sid, "create node island-orphan");
            var exported = syncCommand(po, sid, "export graph as claim-orphan-export");
            assertEquals(Boolean.TRUE, exported.get("ok"),
                    "export with an orphan node must succeed: " + exported);
            var out = ((List<?>) exported.get("output")).stream().map(String::valueOf).toList();
            assertTrue(out.stream().anyMatch(l -> l.startsWith("Graph exported to ")),
                    "the orphan node must not block the export: " + out);
            assertTrue(file.exists(), "export must create the file");
            var text = Utility.getInstance().file2str(file);
            assertTrue(text.contains("island-orphan"),
                    "the orphan node must be present in the exported model, not silently dropped");
        } finally {
            if (file.exists() && !file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    private String openCompanionSession(String seq) {
        var po = EventEmitter.getInstance();
        var sid = "ws-" + seq + "-2";
        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", "ws." + seq + ".2.in")));
        for (int i = 0; i < 50 && !GraphCommandService.hasSession(sid); i++) {
            Utility.getInstance().sleep(20);
        }
        assertTrue(GraphCommandService.hasSession(sid), "session must exist before a companion command");
        return sid;
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
