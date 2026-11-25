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

package com.accenture.minigraph.start;

import com.accenture.minigraph.common.GraphSet;
import com.accenture.minigraph.models.CompiledGraphs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The REST surface of hot deployment (RFC-0017, WP2): a set deployed over POST /api/graph-set/deploy is served at
 * its main graph at once while its subgraphs stay private at the graph and OpenAPI endpoints and to another set's
 * extension; the deploy answer carries the advisory; a refusal answers its status; the list and the undeploy
 * entries do what they say. The test application turns both switches on in a folder of the tests' own.
 */
class HotDeployEndpointTest {
    private static final long TIMEOUT = 10000;
    private static final Path FOLDER = Path.of("/tmp/hot-deploy-unit-test");
    private static String target;
    private final List<String> deployed = new ArrayList<>();

    @BeforeAll
    static void setup() throws IOException {
        PlaygroundLoader.main(new String[0]);
        target = "http://localhost:" + AppConfigReader.getInstance().getProperty("rest.server.port");
        // the folder is the application's own, so a previous run's leftovers are removed and the folder re-read
        Files.createDirectories(FOLDER);
        try (Stream<Path> files = Files.list(FOLDER)) {
            for (var file : files.toList()) {
                Files.deleteIfExists(file);
            }
        }
        HotDeployment.reset();
        HotDeployment.reconcileAll();
        assertTrue(HotDeployment.canDeploy() && HotDeployment.canUndeploy(), "the test application turns both on");
    }

    @AfterEach
    void undeployTheTestSets() {
        for (var set : deployed) {
            try {
                HotDeployment.undeploy(set);
            } catch (HotDeployment.Refused e) {
                CompiledGraphs.getGraphsOfSet(set, HotDeployment.location()).forEach(CompiledGraphs::removeGraph);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> graph(String id, String answer) {
        return SimpleMapper.getInstance().getMapper().readValue("""
                {"nodes": [
                  {"alias": "root", "types": ["Root"],
                   "properties": {"purpose": "a hot-deployed graph", "name": "%s"}},
                  {"alias": "end", "types": ["End"],
                   "properties": {"skill": "graph.data.mapper", "mapping": ["text(%s) -> output.body"]}}],
                 "connections": [{"source": "root", "target": "end",
                                  "relations": [{"type": "done", "properties": {}}]}]}
                """.formatted(id, answer), Map.class);
    }

    /**
     * A main graph whose one node extends another graph and returns what it answered
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> extending(String id, String extension) {
        return SimpleMapper.getInstance().getMapper().readValue("""
                {"nodes": [
                  {"alias": "root", "types": ["Root"],
                   "properties": {"purpose": "a graph that extends another", "name": "%s"}},
                  {"alias": "delegate", "types": ["Extension"],
                   "properties": {"skill": "graph.extension", "extension": "%s",
                                  "input": ["input.body -> *"], "output": ["result -> output.body"]}},
                  {"alias": "end", "types": ["End"], "properties": {}}],
                 "connections": [
                  {"source": "root", "target": "delegate", "relations": [{"type": "next", "properties": {}}]},
                  {"source": "delegate", "target": "end", "relations": [{"type": "done", "properties": {}}]}]}
                """.formatted(id, extension), Map.class);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static byte[] pack(String setName, String version, Map<String, Map<String, Object>> graphs) {
        return GraphSet.pack(setName, Map.of("version", version), graphs);
    }

    private static Map<String, Map<String, Object>> graphs(Map<String, Object>... models) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (var model : models) {
            var nodes = (List<Map<String, Object>>) model.get("nodes");
            var root = nodes.stream().filter(n -> "root".equals(n.get("alias"))).findFirst().orElseThrow();
            result.put(String.valueOf(((Map<String, Object>) root.get("properties")).get("name")), model);
        }
        return result;
    }

    private static EventEnvelope http(String method, String path, Object body, Map<String, String> headers)
            throws Exception {
        var request = new AsyncHttpRequest().setMethod(method).setTargetHost(target).setUrl(path)
                .setHeader("Accept", "application/json");
        if (body instanceof byte[] bytes) {
            request.setBody(bytes).setHeader("Content-Type", "application/octet-stream");
        } else if (body != null) {
            request.setBody(body).setHeader("Content-Type", "application/json");
        }
        headers.forEach(request::setHeader);
        var po = PostOffice.trackable("unit.test", String.format("%032x", path.hashCode()), "TEST " + method + " " + path);
        return po.asyncRequest(new EventEnvelope().setTo("async.http.request").setBody(request), TIMEOUT)
                .await(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    private static MultiLevelMap body(EventEnvelope response) {
        assertInstanceOf(Map.class, response.getBody(), String.valueOf(response.getBody()));
        return new MultiLevelMap((Map<String, Object>) response.getBody());
    }

    private EventEnvelope deploy(String setName, byte[] bytes, Map<String, String> headers) throws Exception {
        deployed.add(setName);
        return http("POST", "/api/graph-set/deploy", bytes, headers);
    }

    @Test
    void aDeployedSetServesItsMainGraphAndHidesItsSubgraphs() throws Exception {
        var bytes = pack("unit-rest-main", "1.0", graphs(graph("unit-rest-main", "main"), graph("unit-rest-sub", "sub")));
        var digest = sha256(bytes);
        var response = deploy("unit-rest-main", bytes, Map.of("X-Graph-Set-SHA256", digest));
        assertEquals(200, response.getStatus(), String.valueOf(response.getBody()));
        var answer = body(response);
        assertEquals("unit-rest-main", answer.getElement("set"));
        assertEquals("1.0", answer.getElement("version"));
        assertEquals(digest, answer.getElement("sha256"));
        assertEquals(true, answer.getElement("changed"));
        assertEquals(List.of("unit-rest-main", "unit-rest-sub"), answer.getElement("graphs"));
        assertEquals(List.of(), answer.getElement("advisory.routes"));
        assertEquals(List.of(), answer.getElement("advisory.extensions"));
        // the main graph is served, the subgraph is not - at the graph endpoint and at the OpenAPI endpoint
        var main = http("POST", "/api/graph/unit-rest-main", Map.of(), Map.of());
        assertEquals(200, main.getStatus(), String.valueOf(main.getBody()));
        assertEquals("main", main.getBody());
        assertEquals(404, http("POST", "/api/graph/unit-rest-sub", Map.of(), Map.of()).getStatus());
        assertEquals(200, http("GET", "/api/openapi/unit-rest-main", null, Map.of()).getStatus());
        assertEquals(404, http("GET", "/api/openapi/unit-rest-sub", null, Map.of()).getStatus());
        // the same bytes again: nothing changes
        var again = deploy("unit-rest-main", bytes, Map.of());
        assertEquals(200, again.getStatus());
        assertEquals(false, body(again).getElement("changed"));
    }

    @Test
    void aSubgraphIsReachedFromItsOwnSetAndRefusedToAnotherSet() throws Exception {
        var owner = pack("unit-rest-owner", "1", graphs(extending("unit-rest-owner", "unit-rest-owner-sub"),
                graph("unit-rest-owner-sub", "from the subgraph")));
        assertEquals(200, deploy("unit-rest-owner", owner, Map.of()).getStatus());
        var served = http("POST", "/api/graph/unit-rest-owner", Map.of(), Map.of());
        assertEquals(200, served.getStatus(), String.valueOf(served.getBody()));
        assertEquals("from the subgraph", served.getBody());
        // another set extends the private subgraph: the advisory says so at deploy time, the run meets 404
        var intruder = pack("unit-rest-intruder", "1", graphs(extending("unit-rest-intruder", "unit-rest-owner-sub")));
        var response = deploy("unit-rest-intruder", intruder, Map.of());
        assertEquals(200, response.getStatus(), String.valueOf(response.getBody()));
        var extensions = body(response).getElement("advisory.extensions");
        assertInstanceOf(List.class, extensions);
        var row = new MultiLevelMap((Map<String, Object>) ((List<?>) extensions).getFirst());
        assertEquals("unit-rest-owner-sub", row.getElement("target"));
        assertEquals("private", row.getElement("status"));
        var refused = http("POST", "/api/graph/unit-rest-intruder", Map.of(), Map.of());
        assertNotEquals(200, refused.getStatus());
        assertTrue(String.valueOf(refused.getBody()).contains("not found"), String.valueOf(refused.getBody()));
    }

    @Test
    void refusalsAnswerTheirStatus() throws Exception {
        var bytes = pack("unit-rest-refused", "1", graphs(graph("unit-rest-refused", "x")));
        var mismatch = deploy("unit-rest-refused", bytes, Map.of("X-Graph-Set-SHA256", "00".repeat(32)));
        assertEquals(400, mismatch.getStatus());
        assertTrue(String.valueOf(mismatch.getBody()).contains("SHA-256 does not match"), String.valueOf(mismatch.getBody()));
        assertFalse(CompiledGraphs.graphExists("unit-rest-refused"));
        var shadowing = pack("unit-rest-shadow", "1", graphs(graph("unit-rest-shadow", "x"), graph("tutorial-1", "copy")));
        var conflict = deploy("unit-rest-shadow", shadowing, Map.of());
        assertEquals(409, conflict.getStatus());
        assertTrue(String.valueOf(conflict.getBody()).contains("tutorial-1 is owned by classpath:/graph"),
                String.valueOf(conflict.getBody()));
        var notASet = deploy("unit-rest-not-a-set", "hello".getBytes(), Map.of());
        assertEquals(400, notASet.getStatus());
        var unnamed = deploy("unit-rest-unnamed", pack("unit-rest-unnamed", "1", graphs(graph("unit-rest-other", "x"))), Map.of());
        assertEquals(400, unnamed.getStatus());
        assertTrue(String.valueOf(unnamed.getBody()).contains("named after its main graph"), String.valueOf(unnamed.getBody()));
    }

    @Test
    void theListAndTheUndeployEntries() throws Exception {
        assertEquals(200, deploy("unit-rest-listed", pack("unit-rest-listed", "7", graphs(graph("unit-rest-listed", "x"))),
                Map.of()).getStatus());
        var listed = http("GET", "/api/graph-set/deployed", null, Map.of());
        assertEquals(200, listed.getStatus(), String.valueOf(listed.getBody()));
        var answer = body(listed);
        assertEquals(HotDeployment.location(), answer.getElement("location"));
        var sets = (List<Map<String, Object>>) answer.getElement("sets");
        var row = sets.stream().filter(s -> "unit-rest-listed".equals(s.get("set"))).findFirst().orElseThrow();
        assertEquals("7", row.get("version"));
        assertEquals("deployed", row.get("status"));
        var removed = http("POST", "/api/graph-set/undeploy/unit-rest-listed", null, Map.of());
        assertEquals(200, removed.getStatus(), String.valueOf(removed.getBody()));
        assertEquals(true, body(removed).getElement("undeployed"));
        assertEquals(List.of("unit-rest-listed"), body(removed).getElement("graphs"));
        assertEquals(404, http("POST", "/api/graph/unit-rest-listed", Map.of(), Map.of()).getStatus());
        assertEquals(404, http("POST", "/api/graph-set/undeploy/unit-rest-listed", null, Map.of()).getStatus());
        assertEquals(404, http("POST", "/api/graph-set/undeploy/tutorial-1", null, Map.of()).getStatus());
    }
}
