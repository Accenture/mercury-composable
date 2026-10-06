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

import com.accenture.minigraph.common.GraphSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.CanonicalPackager;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.AppConfigReader;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The graph-set endpoints of ADR-0027 (WP4), through the real router. POST /api/graph-set/pack answers the set
 * as a download - the same bytes the graph packager writes for the same graphs and fields - and refuses a
 * graph that the import validation or the deployment gate refuses; POST /api/graph-set/unpack answers the
 * manifest and the graphs of a package, whether its bytes arrive with a content length or as a stream, and
 * refuses bytes that are not a canonical package or a set that breaks a rule.
 */
class GraphSetEndpointTest {
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final String JSON = "application/json";
    private static final String OCTET_STREAM = "application/octet-stream";
    private static final String PACK = "/api/graph-set/pack";
    private static final String UNPACK = "/api/graph-set/unpack";
    private static final String SET = "unit-test-endpoint-set";
    private static final String GRAPH_A = "unit-test-endpoint-a";
    private static String target;

    @BeforeAll
    static void setup() {
        AutoStart.main(new String[0]);
        var config = AppConfigReader.getInstance();
        var port = config.getProperty("rest.server.port", config.getProperty("server.port", "8085"));
        target = "http://127.0.0.1:" + port;
    }

    @Test
    void packAnswersTheSetAsADownload() throws Exception {
        var model = model(GRAPH_A);
        var fields = Map.of("version", "1.0.0", "author", "the endpoint test");
        var response = post(PACK, JSON, Map.of("manifest", manifest(fields), "graphs", Map.of(GRAPH_A, model)), true);
        assertEquals(200, response.getStatus(), message(response));
        assertTrue(header(response, "content-type").startsWith(OCTET_STREAM), response.getHeaders().toString());
        assertEquals("attachment; filename=\"" + SET + GraphSet.EXTENSION + "\"", header(response, "content-disposition"));
        var bytes = assertInstanceOf(byte[].class, response.getBody());
        // the same bytes the graph packager writes for the same graphs and fields
        assertArrayEquals(GraphSet.pack(SET, fields, Map.of(GRAPH_A, model)), bytes);
        var contents = GraphSet.read(bytes);
        assertEquals(SET, contents.manifest().get(GraphSet.SET));
        assertEquals("1.0.0", contents.manifest().get("version"));
        assertEquals(Set.of(GRAPH_A), contents.graphs().keySet());
    }

    @Test
    void packRefusesAGraphTheGateRefuses() throws Exception {
        var noEnd = model("unit-test-endpoint-no-end");
        ((List<?>) noEnd.get("nodes")).remove(1);
        noEnd.remove("connections");
        var graphs = Map.of(GRAPH_A, model(GRAPH_A), "unit-test-endpoint-no-end", noEnd);
        var response = post(PACK, JSON, Map.of("manifest", manifest(Map.of()), "graphs", graphs), true);
        assertEquals(400, response.getStatus(), message(response));
        assertEquals("Set not packed - unit-test-endpoint-no-end: graph must have an 'end' node", message(response));
    }

    @Test
    void packRefusesAModelWithAForeignSection() throws Exception {
        var foreign = model("unit-test-endpoint-b");
        foreign.put("manifest", Map.of());
        var response = post(PACK, JSON, Map.of("manifest", manifest(Map.of()), "graphs", Map.of("unit-test-endpoint-b", foreign)), true);
        assertEquals(400, response.getStatus(), message(response));
        assertEquals("Set not packed - unit-test-endpoint-b: Unexpected top-level section(s): manifest" +
                " - a graph model has only 'nodes' and 'connections'", message(response));
    }

    @Test
    void packNeedsTheSetName() throws Exception {
        var response = post(PACK, JSON, Map.of("manifest", Map.of("version", "1"), "graphs", Map.of(GRAPH_A, model(GRAPH_A))), true);
        assertEquals(400, response.getStatus(), message(response));
        assertEquals("Set not packed - manifest field 'set' is required - it names the set and its file", message(response));
        var noGraphs = post(PACK, JSON, Map.of("manifest", manifest(Map.of())), true);
        assertEquals(400, noGraphs.getStatus(), message(noGraphs));
        assertEquals("Set not packed - 'graphs' is a JSON object keyed by graph id, each value a graph model", message(noGraphs));
    }

    @Test
    void unpackAnswersTheManifestAndTheGraphs() throws Exception {
        var bytes = GraphSet.pack(SET, Map.of("version", "2.0.0", GraphSet.GRAPH_ID, GRAPH_A), Map.of(GRAPH_A, model(GRAPH_A)));
        // with a content length the bytes arrive as one body; without one they arrive as a stream
        for (boolean withLength : new boolean[] {true, false}) {
            var response = post(UNPACK, OCTET_STREAM, bytes, withLength);
            assertEquals(200, response.getStatus(), message(response));
            assertTrue(header(response, "content-type").startsWith(JSON), response.getHeaders().toString());
            var body = assertInstanceOf(Map.class, response.getBody());
            var manifest = assertInstanceOf(Map.class, body.get("manifest"));
            assertEquals(SET, manifest.get(GraphSet.SET));
            assertEquals("2.0.0", manifest.get("version"));
            assertEquals(GRAPH_A, manifest.get(GraphSet.GRAPH_ID));
            assertEquals("mercury-package", manifest.get(CanonicalPackager.FORMAT_KEY));
            var graphs = assertInstanceOf(Map.class, body.get("graphs"));
            assertEquals(Set.of(GRAPH_A), graphs.keySet());
            var graph = assertInstanceOf(Map.class, graphs.get(GRAPH_A));
            assertEquals(2, assertInstanceOf(List.class, graph.get("nodes")).size(), "with length " + withLength);
        }
    }

    @Test
    void unpackRefusesWhatIsNotAGraphSet() throws Exception {
        var text = post(UNPACK, OCTET_STREAM, "not a package".getBytes(StandardCharsets.UTF_8), true);
        assertEquals(400, text.getStatus(), message(text));
        assertTrue(message(text).startsWith("Not a graph set - "), message(text));
        // a canonical package whose entry is not <graph-id>.json is refused before anything is built from its name
        var crafted = CanonicalPackager.builder().manifest(GraphSet.SET, SET).add("../escape.json", model("escape")).build();
        var entry = post(UNPACK, OCTET_STREAM, crafted, true);
        assertEquals(400, entry.getStatus(), message(entry));
        assertEquals("Not a graph set - entry '../escape.json' - expect <graph-id>.json, the id in letters, digits, '_' and '-'",
                message(entry));
        // the body is the package itself, not a JSON document
        var json = post(UNPACK, JSON, Map.of("manifest", Map.of()), true);
        assertEquals(400, json.getStatus(), message(json));
        assertEquals("The request body is the graph set (.pack) to read, sent as application/octet-stream", message(json));
    }

    private static Map<String, Object> manifest(Map<String, String> fields) {
        var manifest = new LinkedHashMap<String, Object>();
        manifest.put(GraphSet.SET, SET);
        manifest.putAll(fields);
        return manifest;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> model(String id) {
        return SimpleMapper.getInstance().getMapper().readValue("""
                {"nodes": [
                  {"alias": "root", "types": ["Root"],
                   "properties": {"purpose": "a graph packed by the endpoint", "name": "%s"}},
                  {"alias": "end", "types": ["End"],
                   "properties": {"skill": "graph.data.mapper", "mapping": ["text(packed) -> output.body"]}}],
                 "connections": [{"source": "root", "target": "end", "relations": [{"type": "done", "properties": {}}]}]}
                """.formatted(id), Map.class);
    }

    private static EventEnvelope post(String url, String contentType, Object body, boolean withLength) throws Exception {
        var req = new AsyncHttpRequest().setMethod("POST").setTargetHost(target).setUrl(url)
                .setHeader("Content-Type", contentType).setHeader("Accept", JSON).setBody(body);
        if (withLength && body instanceof byte[] bytes) {
            req.setContentLength(bytes.length);
        }
        return EventEmitter.getInstance().request(new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(req), 10000).get();
    }

    private static String header(EventEnvelope response, String name) {
        return response.getHeaders().entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(name)).map(Map.Entry::getValue).findFirst().orElse("");
    }

    private static String message(EventEnvelope response) {
        return String.valueOf(response.getBody() instanceof Map<?, ?> map && map.containsKey("message") ?
                map.get("message") : response.getBody());
    }
}
