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

package com.accenture.minigraph.contract;

import com.accenture.minigraph.services.GraphCommandService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The OpenAPI document on demand (RFC-0007, WP1): {@code GET /api/openapi/{graph_id}} answers a YAML
 * attachment named after the graph, {@code ?format=json} the document inline, {@code ?view=contract}
 * the derived contract; an unknown graph answers 404; {@code GET /api/openapi/session/{sessionId}}
 * documents a session's draft and names it after its root node.
 */
class GraphOpenApiEndpointTest {
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
    @SuppressWarnings("unchecked")
    void deployedGraphDocumentInThreeViews() throws Exception {
        var po = EventEmitter.getInstance();
        // YAML attachment by default
        var yaml = get(po, "/api/openapi/tutorial-4", null);
        assertEquals(200, yaml.getStatus());
        assertTrue(String.valueOf(yaml.getHeader("content-type")).startsWith("application/yaml"), yaml.getHeaders().toString());
        assertEquals("attachment; filename=\"tutorial-4.yaml\"", yaml.getHeader("content-disposition"));
        var text = yaml.getBody() instanceof byte[] b ? new String(b) : String.valueOf(yaml.getBody());
        assertTrue(text.startsWith("openapi: 3.0.3\n"), text);
        Map<String, Object> parsed = new Yaml().load(text);
        assertEquals("tutorial-4", ((Map<String, Object>) parsed.get("info")).get("title"));
        assertEquals(List.of(Map.of("url", target)), parsed.get("servers"));
        var post = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) parsed.get("paths"))
                .get("/api/graph/tutorial-4")).get("post");
        var request = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) post
                .get("requestBody")).get("content")).get("application/json")).get("schema");
        var properties = (Map<String, Object>) request.get("properties");
        assertEquals("number", ((Map<String, Object>) properties.get("a")).get("type"), "a is an arithmetic operand");
        // JSON inline
        var json = get(po, "/api/openapi/tutorial-4", "format=json");
        assertEquals(200, json.getStatus());
        assertTrue(String.valueOf(json.getHeader("content-type")).startsWith("application/json"));
        assertInstanceOf(Map.class, json.getBody());
        assertEquals("3.0.3", ((Map<String, Object>) json.getBody()).get("openapi"));
        // the contract view
        var contract = get(po, "/api/openapi/tutorial-4", "view=contract");
        assertEquals(200, contract.getStatus());
        var view = (Map<String, Object>) contract.getBody();
        assertEquals("tutorial-4", view.get("graph"));
        var input = (Map<String, Object>) ((Map<String, Object>) view.get("input")).get("body");
        assertEquals(false, input.get("declared"));
        var paths = (List<Map<String, Object>>) input.get("paths");
        assertTrue(paths.stream().anyMatch(p -> "input.body.a".equals(p.get("path")) && "number".equals(p.get("type"))), paths.toString());
        // the unknowns
        assertEquals(404, get(po, "/api/openapi/no-such-graph", null).getStatus());
        assertEquals(400, get(po, "/api/openapi/tutorial-4", "format=xml").getStatus());
        assertEquals(400, get(po, "/api/openapi/tutorial-4", "view=nonsense").getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void draftDocumentNamedAfterTheRoot() throws Exception {
        var po = EventEmitter.getInstance();
        var sid = "ws-990077-5";
        var inRoute = "ws.990077.5.in";
        var outRoute = "ws.990077.5.out";
        // stand in for the session's WebSocket .out route: the console echo of every command goes there
        Platform.getInstance().registerPrivate(outRoute, (hdr, body, inst) -> null, 1);
        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "open", "in", inRoute, "out", outRoute)));
        for (int i = 0; i < 50 && !GraphCommandService.hasSession(sid); i++) {
            Utility.getInstance().sleep(20);
        }
        assertTrue(GraphCommandService.hasSession(sid));
        try {
            command(po, inRoute, "create node root\nwith type Root\nwith properties\nname=my-draft\npurpose=A draft");
            command(po, inRoute, "create node end\nwith type End\nwith properties\nskill=graph.data.mapper\nmapping[]=int(input.body.n) -> output.body.n");
            command(po, inRoute, "connect root to end with contains");
            Utility.getInstance().sleep(300);
            var json = get(po, "/api/openapi/session/" + sid, "format=json");
            assertEquals(200, json.getStatus(), String.valueOf(json.getBody()));
            var document = (Map<String, Object>) json.getBody();
            assertEquals("my-draft", ((Map<String, Object>) document.get("info")).get("title"),
                    "draft: " + GraphCommandService.downloadGraph(sid));
            assertTrue(((Map<String, Object>) document.get("paths")).containsKey("/api/graph/my-draft"));
            var yaml = get(po, "/api/openapi/session/" + sid, null);
            assertEquals("attachment; filename=\"my-draft.yaml\"", yaml.getHeader("content-disposition"));
            assertEquals(404, get(po, "/api/openapi/session/ws-000000-0", null).getStatus());
        } finally {
            po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE).setBody(Map.of("type", "close", "in", inRoute)));
            Utility.getInstance().sleep(100);
            Platform.getInstance().release(outRoute);
        }
    }

    private static void command(EventEmitter po, String inRoute, String text) {
        po.send(new EventEnvelope().setTo(GraphCommandService.ROUTE)
                .setBody(Map.of("type", "command", "in", inRoute, "out", inRoute.replace(".in", ".out"), "message", text)));
        Utility.getInstance().sleep(150);
    }

    private static EventEnvelope get(EventEmitter po, String url, String query) throws Exception {
        var req = new AsyncHttpRequest().setMethod("GET").setTargetHost(target).setUrl(url)
                .setHeader("Accept", "*/*");
        if (query != null) {
            var kv = query.split("=", 2);
            req.setQueryParameter(kv[0], kv[1]);
        }
        return po.request(new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(req), 10000).get();
    }
}
