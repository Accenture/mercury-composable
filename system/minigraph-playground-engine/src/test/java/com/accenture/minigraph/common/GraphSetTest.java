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

package com.accenture.minigraph.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.platformlambda.core.serializers.CanonicalPackager;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.ConfigReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The rules of a graph set (RFC-0005) and the gate's checks they share with CompileGraph.
 */
class GraphSetTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> graph(String json) {
        return SimpleMapper.getInstance().getMapper().readValue(json, Map.class);
    }

    private static Map<String, Object> valid(String id) {
        return graph("""
                {"nodes": [
                  {"alias": "root", "types": ["Root"], "properties": {"purpose": "a valid graph", "name": "%s"}},
                  {"alias": "end", "types": ["End"],
                   "properties": {"skill": "graph.data.mapper", "mapping": ["text(ok) -> output.body"]}}],
                 "connections": [{"source": "root", "target": "end", "relations": [{"type": "done", "properties": {}}]}]}
                """.formatted(id));
    }

    @Test
    void theGraphIdRuleIsTheFileNameRule() {
        assertTrue(GraphModelGate.isValidGraphId("tutorial-1"));
        assertTrue(GraphModelGate.isValidGraphId("A_b-9"));
        assertFalse(GraphModelGate.isValidGraphId(null));
        assertFalse(GraphModelGate.isValidGraphId(""));
        assertFalse(GraphModelGate.isValidGraphId("a.b"));
        assertFalse(GraphModelGate.isValidGraphId("../a"));
        assertFalse(GraphModelGate.isValidGraphId("a b"));
    }

    @Test
    void theRootNameIsReadFromTheRootNode() {
        assertEquals("quote", GraphModelGate.declaredRootName(valid(" quote ")));
        assertEquals("", GraphModelGate.declaredRootName(graph("""
                {"nodes": [{"alias": "root", "types": ["Root"], "properties": {"purpose": "p"}}]}""")));
        assertEquals("", GraphModelGate.declaredRootName(graph("{\"connections\": []}")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void theGateConvertsDeprecatedSyntaxInPlace() {
        var model = valid("old");
        var end = (Map<String, Object>) ((List<?>) model.get("nodes")).get(1);
        var properties = (Map<String, Object>) end.get("properties");
        properties.put("mapping", new ArrayList<>(List.of("model.number:int -> output.body")));
        GraphModelGate.validate("old", model);
        assertEquals(List.of("f:int(model.number) -> output.body"), properties.get("mapping"));
    }

    @Test
    void packingRefusesWithEveryReason() {
        Map<String, Map<String, Object>> graphs = new TreeMap<>();
        graphs.put("good", valid("good"));
        graphs.put("bad.id", valid("bad.id"));
        graphs.put("renamed", valid("other"));
        graphs.put("no-end", graph("""
                {"nodes": [{"alias": "root", "types": ["Root"], "properties": {"purpose": "p"}}], "connections": []}"""));
        var fields = new LinkedHashMap<String, String>();
        fields.put("set", "x");
        fields.put("format", "y");
        fields.put("graph_id", "missing");
        var e = assertThrows(GraphSet.RefusedException.class, () -> GraphSet.pack("bad set", fields, graphs));
        assertEquals(List.of(
                "set name 'bad set' - use letters, digits, '_' and '-' only",
                "manifest field 'set' - it is written from the set name",
                "manifest field 'format' - it is written by the packager",
                "manifest field 'graph_id' - 'missing' is not a graph of the set",
                "bad.id: graph id - use letters, digits, '_' and '-' only",
                "no-end: graph must have an 'end' node",
                "renamed: the root node's name 'other' differs from the graph id"), e.getReasons());
        var empty = assertThrows(GraphSet.RefusedException.class, () -> GraphSet.pack("s", Map.of(), Map.of()));
        assertEquals(List.of("a set needs at least one graph"), empty.getReasons());
    }

    @Test
    void aSetPacksDeterministicallyAndReadsBack() throws IOException {
        Map<String, Map<String, Object>> graphs = new LinkedHashMap<>();
        graphs.put("b", valid("b"));
        graphs.put("a", valid("a"));
        var bytes = GraphSet.pack("pair", Map.of("version", "2", "graph_id", "a"), graphs);
        Map<String, Map<String, Object>> reversed = new LinkedHashMap<>();
        reversed.put("a", valid("a"));
        reversed.put("b", valid("b"));
        assertArrayEquals(bytes, GraphSet.pack("pair", new TreeMap<>(Map.of("graph_id", "a", "version", "2")), reversed));
        var contents = GraphSet.read(bytes);
        assertEquals(List.of("format", "format_version", "graph_id", "set", "version"),
                List.copyOf(contents.manifest().keySet()));
        assertEquals("pair", contents.manifest().get("set"));
        assertEquals(List.of("a", "b"), List.copyOf(contents.graphs().keySet()));
        assertArrayEquals(CanonicalPackager.encode(valid("a")), CanonicalPackager.encode(contents.graphs().get("a")));
    }

    @Test
    void readingRefusesWhatASetMustNotHold() throws IOException {
        var bytes = CanonicalPackager.builder()
                .manifest("graph_id", "absent")
                .add("../escape.json", valid("escape"))
                .add("notes.txt", Map.of("a", 1))
                .add("renamed.json", valid("other"))
                .add("binary.json", Map.of("nodes", List.of(Map.of("data", new byte[]{1, 2}))))
                .build();
        var e = assertThrows(GraphSet.RefusedException.class, () -> GraphSet.read(bytes));
        assertEquals(List.of(
                "entry '../escape.json' - expect <graph-id>.json, the id in letters, digits, '_' and '-'",
                "binary: binary data at 'nodes[0].data' - a graph model is JSON",
                "entry 'notes.txt' - expect <graph-id>.json, the id in letters, digits, '_' and '-'",
                "renamed: the root node's name 'other' differs from the graph id",
                "manifest field 'graph_id' - 'absent' is not a graph of the set"), e.getReasons());
        var empty = CanonicalPackager.builder().build();
        assertEquals(List.of("the package holds no graph"),
                assertThrows(GraphSet.RefusedException.class, () -> GraphSet.read(empty)).getReasons());
        var trailing = new byte[empty.length + 1];
        System.arraycopy(empty, 0, trailing, 0, empty.length);
        assertThrows(IOException.class, () -> GraphSet.read(trailing));
    }

    private static final String WITH_NULLS = """
            {"nodes": [
              {"alias": "root", "types": ["Root"],
               "properties": {"purpose": "p", "name": "n", "note": null, "empty": "", "flags": [true, null]}},
              {"alias": "end", "types": ["End"], "properties": {}}],
             "connections": [{"source": "root", "target": "end", "relations": [{"type": "done", "properties": {"x": null}}]}]}""";

    private static final String WITHOUT_NULLS = """
            {"nodes": [
              {"alias": "root", "types": ["Root"],
               "properties": {"purpose": "p", "name": "n", "empty": "", "flags": [true, null]}},
              {"alias": "end", "types": ["End"], "properties": {}}],
             "connections": [{"source": "root", "target": "end", "relations": [{"type": "done", "properties": {}}]}]}""";

    @Test
    @SuppressWarnings("unchecked")
    void aNullPropertyIsFilteredOutAndAnEmptyStringKept() throws IOException {
        var bytes = GraphSet.pack("n", Map.of(), Map.of("n", graph(WITH_NULLS)));
        // the same bytes as the graph written without its null properties
        assertArrayEquals(GraphSet.pack("n", Map.of(), Map.of("n", graph(WITHOUT_NULLS))), bytes);
        var read = GraphSet.read(bytes).graphs().get("n");
        var properties = (Map<String, Object>) ((Map<String, Object>) ((List<?>) read.get("nodes")).getFirst())
                .get("properties");
        assertFalse(properties.containsKey("note"));
        assertEquals("", properties.get("empty"));
        // a list keeps its elements in place
        assertEquals(List.of(true), ((List<?>) properties.get("flags")).subList(0, 1));
        assertEquals(2, ((List<?>) properties.get("flags")).size());
        var json = GraphSet.toJson(read);
        assertTrue(json.startsWith("{\n  \"connections\": [\n"), json);
        assertFalse(json.contains("\"note\""), json);
        assertFalse(json.contains("\"x\""), json);
        assertTrue(json.contains("\"empty\": \"\""), json);
        assertTrue(json.endsWith("}\n"));
        assertArrayEquals(CanonicalPackager.encode(read), CanonicalPackager.encode(graph(json)));
    }

    @Test
    void readingFiltersANullPropertyOut() throws IOException {
        var bytes = CanonicalPackager.builder().add("n.json", graph(WITH_NULLS)).build();
        assertArrayEquals(CanonicalPackager.encode(graph(WITHOUT_NULLS)),
                CanonicalPackager.encode(GraphSet.read(bytes).graphs().get("n")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void theDeploymentReadDropsANullPropertyToo(@TempDir Path dir) throws IOException {
        // the reference behavior the packager follows: CompileGraph's read drops "key": null and keeps "key": ""
        var file = Files.writeString(dir.resolve("n.json"), WITH_NULLS);
        var model = new ConfigReader("file:" + file).getMap();
        var root = (Map<String, Object>) ((List<?>) model.get("nodes")).getFirst();
        var properties = (Map<String, Object>) root.get("properties");
        assertFalse(properties.containsKey("note"));
        assertEquals("", properties.get("empty"));
        assertNotNull(GraphModelGate.validate("n", model));
    }
}
