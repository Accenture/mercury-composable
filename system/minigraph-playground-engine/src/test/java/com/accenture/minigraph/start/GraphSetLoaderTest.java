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
import org.junit.jupiter.api.io.TempDir;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.CanonicalPackager;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The graph-set loader (ADR-0027): a manifest's 'sets' are unpacked into its 'unpack' folder and deployed all or
 * none, right after the manifest's own graphs. Each test builds its packages from JSON in a temporary folder and
 * compiles its own manifest in the running test application, with graph ids no other test uses.
 */
class GraphSetLoaderTest {
    private static final long TIMEOUT = 10000;
    private static String target;
    private final List<String> registered = new ArrayList<>();

    @TempDir
    Path tmp;

    @BeforeAll
    static void setup() {
        PlaygroundLoader.main(new String[0]);
        target = "http://localhost:" + AppConfigReader.getInstance().getProperty("rest.server.port");
    }

    @AfterEach
    void forgetTheTestGraphs() {
        // the registry is shared by every test in this JVM
        registered.forEach(CompiledGraphs::removeGraph);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> graph(String id, String answer) throws IOException {
        return SimpleMapper.getInstance().getMapper().readValue("""
                {"nodes": [
                  {"alias": "root", "types": ["Root"], "properties": {"purpose": "a graph deployed from a set", "name": "%s"}},
                  {"alias": "end", "types": ["End"],
                   "properties": {"skill": "graph.data.mapper", "mapping": ["text(%s) -> output.body"]}}],
                 "connections": [{"source": "root", "target": "end", "relations": [{"type": "done", "properties": {}}]}]}
                """.formatted(id, answer), Map.class);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> graphWithoutEnd(String id) throws IOException {
        return SimpleMapper.getInstance().getMapper().readValue("""
                {"nodes": [{"alias": "root", "types": ["Root"], "properties": {"purpose": "no end node", "name": "%s"}}],
                 "connections": []}
                """.formatted(id), Map.class);
    }

    private static void pack(Path folder, String setName, String version, Map<String, Map<String, Object>> graphs)
            throws IOException {
        Files.createDirectories(folder);
        var fields = version == null ? Map.<String, String>of() : Map.of("version", version);
        Files.write(folder.resolve(setName + GraphSet.EXTENSION), GraphSet.pack(setName, fields, graphs));
    }

    // a package the packager would refuse, built with the canonical packager directly - what a deployment can meet
    private static void craft(Path folder, String setName, Map<String, Map<String, Object>> entries)
            throws IOException {
        Files.createDirectories(folder);
        var builder = CanonicalPackager.builder().manifest(GraphSet.SET, setName);
        entries.forEach(builder::add);
        Files.write(folder.resolve(setName + GraphSet.EXTENSION), builder.build());
    }

    private static String manifest(Path file, String location, List<String> graphs, List<String> sets,
                                   String unpack) throws IOException {
        var sb = new StringBuilder();
        if (!graphs.isEmpty()) {
            sb.append("graphs:\n");
            graphs.forEach(id -> sb.append("  - '").append(id).append("'\n"));
        }
        sb.append("location: '").append(location).append("'\n");
        if (!sets.isEmpty()) {
            sb.append("sets:\n");
            sets.forEach(name -> sb.append("  - '").append(name).append("'\n"));
        }
        if (unpack != null) {
            sb.append("unpack: '").append(unpack).append("'\n");
        }
        Files.writeString(file, sb.toString());
        return "file:" + file;
    }

    private static String file(Path folder) {
        return "file:" + folder;
    }

    private void compile(String manifest, String... ids) {
        registered.addAll(List.of(ids));
        new CompileGraph().compileManifest(manifest);
    }

    private static EventEnvelope run(String graphId) throws Exception {
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target).setUrl("/api/graph/" + graphId)
                .setBody(Map.of()).setHeader("Content-Type", "application/json").setHeader("Accept", "application/json");
        var po = PostOffice.trackable("unit.test", String.format("%032x", graphId.hashCode()), "TEST /graph/" + graphId);
        return po.asyncRequest(new EventEnvelope().setTo("async.http.request").setBody(request), TIMEOUT)
                .await(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Test
    void aSetDeploysItsGraphsAndTheEndpointServesThem() throws Exception {
        Map<String, Map<String, Object>> graphs = new LinkedHashMap<>();
        graphs.put("unit-test-set-one", graph("unit-test-set-one", "one from the set"));
        graphs.put("unit-test-set-two", graph("unit-test-set-two", "two from the set"));
        pack(tmp.resolve("packs"), "served-set", "1.0.0", graphs);
        var unpack = file(tmp.resolve("unpack"));
        compile(manifest(tmp.resolve("graphs.yaml"), file(tmp.resolve("packs")), List.of(), List.of("served-set"),
                unpack), "unit-test-set-one", "unit-test-set-two");
        for (var id : graphs.keySet()) {
            assertTrue(CompiledGraphs.graphExists(id), id);
            assertEquals(unpack, CompiledGraphs.getGraphLocation(id));
            assertEquals(new CompiledGraphs.DeployedSet("served-set", "1.0.0"), CompiledGraphs.getGraphSet(id));
            assertTrue(Files.isRegularFile(tmp.resolve("unpack/" + id + ".json")), id);
        }
        assertTrue(CompiledGraphs.getDeployedLocations().contains(unpack));
        var response = run("unit-test-set-one");
        assertEquals(200, response.getStatus(), String.valueOf(response.getBody()));
        assertEquals("one from the set", response.getBody());
        // the generated manifest records what deployed and the files the loader wrote
        var generated = Files.readString(tmp.resolve("unpack/graphs.yaml"), UTF_8);
        assertTrue(generated.contains("graphs:\n  - 'unit-test-set-one'\n  - 'unit-test-set-two'\n"), generated);
        assertTrue(generated.contains("location: '" + unpack + "'"), generated);
        assertTrue(generated.contains("generated:\n  'served-set':\n    - 'unit-test-set-one'\n"), generated);
        assertTrue(generated.contains("# set served-set: " + file(tmp.resolve("packs")) + "/served-set.pack, SHA-256 "),
                generated);
        assertTrue(generated.contains(", version=1.0.0 - deployed"), generated);
    }

    @Test
    void aSetWithAGraphTheGateRefusesRegistersNone() throws Exception {
        craft(tmp.resolve("packs"), "half-set", Map.of(
                "unit-test-set-good.json", graph("unit-test-set-good", "good"),
                "unit-test-set-no-end.json", graphWithoutEnd("unit-test-set-no-end")));
        compile(manifest(tmp.resolve("graphs.yaml"), file(tmp.resolve("packs")), List.of(), List.of("half-set"),
                file(tmp.resolve("unpack"))), "unit-test-set-good", "unit-test-set-no-end");
        assertFalse(CompiledGraphs.graphExists("unit-test-set-good"), "all or none: the valid graph is not registered");
        assertFalse(CompiledGraphs.graphExists("unit-test-set-no-end"));
        // the files stay for the operator to inspect, and the next start removes them
        assertTrue(Files.isRegularFile(tmp.resolve("unpack/unit-test-set-good.json")));
        var generated = Files.readString(tmp.resolve("unpack/graphs.yaml"), UTF_8);
        assertTrue(generated.contains("graphs: []\n"), generated);
        assertTrue(generated.contains("  'half-set':\n"), generated);
        assertTrue(generated.contains(" - rejected\n"), generated);
    }

    @Test
    void aDuplicateInvolvingASetIsWonByTheLaterCopy() throws Exception {
        var loose = tmp.resolve("loose");
        Files.createDirectories(loose);
        Files.writeString(loose.resolve("unit-test-set-dup.json"),
                SimpleMapper.getInstance().getMapper().writeValueAsString(graph("unit-test-set-dup", "loose")));
        // a loose graph, then a set holding the same id: the set's copy wins
        compile(manifest(tmp.resolve("m1.yaml"), file(loose), List.of("unit-test-set-dup"), List.of(), null),
                "unit-test-set-dup");
        assertEquals(file(loose), CompiledGraphs.getGraphLocation("unit-test-set-dup"));
        pack(tmp.resolve("packs"), "dup-set", "2", Map.of("unit-test-set-dup", graph("unit-test-set-dup", "set")));
        compile(manifest(tmp.resolve("m2.yaml"), file(tmp.resolve("packs")), List.of(), List.of("dup-set"),
                file(tmp.resolve("unpack"))));
        assertEquals(file(tmp.resolve("unpack")), CompiledGraphs.getGraphLocation("unit-test-set-dup"));
        assertEquals("dup-set", CompiledGraphs.getGraphSet("unit-test-set-dup").name());
        assertEquals("set", run("unit-test-set-dup").getBody());
        // a later loose copy wins over the set's, and the graph no longer belongs to a set
        compile(manifest(tmp.resolve("m3.yaml"), file(loose), List.of("unit-test-set-dup"), List.of(), null));
        assertEquals(file(loose), CompiledGraphs.getGraphLocation("unit-test-set-dup"));
        assertNull(CompiledGraphs.getGraphSet("unit-test-set-dup"));
        assertEquals("loose", run("unit-test-set-dup").getBody());
    }

    @Test
    void setsNeedAWritableUnpackFolderOfTheirOwn() throws Exception {
        pack(tmp.resolve("packs"), "folder-set", null, Map.of("unit-test-set-folder", graph("unit-test-set-folder", "x")));
        var loose = tmp.resolve("packs");
        Files.writeString(loose.resolve("unit-test-set-loose.json"),
                SimpleMapper.getInstance().getMapper().writeValueAsString(graph("unit-test-set-loose", "loose")));
        // no 'unpack': the sets are skipped and the manifest's own graphs still compile
        compile(manifest(tmp.resolve("m1.yaml"), file(loose), List.of("unit-test-set-loose"), List.of("folder-set"),
                null), "unit-test-set-loose", "unit-test-set-folder");
        assertTrue(CompiledGraphs.graphExists("unit-test-set-loose"));
        assertFalse(CompiledGraphs.graphExists("unit-test-set-folder"));
        // classpath: is refused - the loader writes there
        compile(manifest(tmp.resolve("m2.yaml"), file(loose), List.of(), List.of("folder-set"),
                "classpath:/unpacked"));
        assertFalse(CompiledGraphs.graphExists("unit-test-set-folder"));
        // the Playground's temporary folder, or a folder inside it, is refused - its housekeeping deletes files
        compile(manifest(tmp.resolve("m3.yaml"), file(loose), List.of(), List.of("folder-set"),
                "file:/tmp/graph/unpacked"));
        assertFalse(CompiledGraphs.graphExists("unit-test-set-folder"));
        assertFalse(Files.exists(Path.of("/tmp/graph/unpacked")));
        // one folder serves one manifest: the second manifest's sets are skipped
        compile(manifest(tmp.resolve("m4.yaml"), file(loose), List.of(), List.of("folder-set"),
                file(tmp.resolve("unpack"))));
        assertTrue(CompiledGraphs.graphExists("unit-test-set-folder"));
        CompiledGraphs.removeGraph("unit-test-set-folder");
        compile(manifest(tmp.resolve("m5.yaml"), file(loose), List.of(), List.of("folder-set"),
                file(tmp.resolve("unpack"))));
        assertFalse(CompiledGraphs.graphExists("unit-test-set-folder"));
    }

    @Test
    void theNextStartRemovesTheFilesOfAGraphTheSetNoLongerHolds() throws Exception {
        var unpack = tmp.resolve("unpack");
        Files.createDirectories(unpack);
        Files.writeString(unpack.resolve("notes.txt"), "the operator's own file");
        pack(tmp.resolve("packs"), "versioned-set", "1", Map.of(
                "unit-test-set-kept", graph("unit-test-set-kept", "kept"),
                "unit-test-set-dropped", graph("unit-test-set-dropped", "dropped")));
        var manifest = manifest(tmp.resolve("graphs.yaml"), file(tmp.resolve("packs")), List.of(),
                List.of("versioned-set"), file(unpack));
        compile(manifest, "unit-test-set-kept", "unit-test-set-dropped");
        assertTrue(Files.isRegularFile(unpack.resolve("unit-test-set-dropped.json")));
        // version 2 drops a graph; a new start unpacks it into the same folder
        pack(tmp.resolve("packs"), "versioned-set", "2", Map.of("unit-test-set-kept", graph("unit-test-set-kept", "v2")));
        GraphSetLoader.reset();
        compile(manifest);
        assertFalse(Files.exists(unpack.resolve("unit-test-set-dropped.json")), "the dropped graph's file is removed");
        assertTrue(Files.isRegularFile(unpack.resolve("unit-test-set-kept.json")));
        assertTrue(Files.isRegularFile(unpack.resolve("notes.txt")), "a file the loader did not write stays");
        assertEquals("2", CompiledGraphs.getGraphSet("unit-test-set-kept").version());
    }

    @Test
    void aCraftedEntryNameIsRefusedBeforeAnyFileIsWritten() throws Exception {
        craft(tmp.resolve("packs"), "crafted-set", Map.of("../escaped.json", graph("escaped", "x")));
        compile(manifest(tmp.resolve("graphs.yaml"), file(tmp.resolve("packs")), List.of(), List.of("crafted-set"),
                file(tmp.resolve("unpack/inner"))));
        assertFalse(Files.exists(tmp.resolve("unpack/escaped.json")));
        assertFalse(CompiledGraphs.graphExists("escaped"));
        var generated = Files.readString(tmp.resolve("unpack/inner/graphs.yaml"), UTF_8);
        assertTrue(generated.contains("# set crafted-set not deployed - its names break the set rules\n"), generated);
    }

    @Test
    void aMissingPackageIsNotDeployedAndTheOtherSetsAre() throws Exception {
        pack(tmp.resolve("packs"), "present-set", null, Map.of("unit-test-set-present", graph("unit-test-set-present", "x")));
        compile(manifest(tmp.resolve("graphs.yaml"), file(tmp.resolve("packs")), List.of(),
                List.of("absent-set", "present-set"), file(tmp.resolve("unpack"))), "unit-test-set-present");
        assertTrue(CompiledGraphs.graphExists("unit-test-set-present"));
        // a set without a version field deploys with an empty version
        assertEquals("", CompiledGraphs.getGraphSet("unit-test-set-present").version());
        var generated = Files.readString(tmp.resolve("unpack/graphs.yaml"), UTF_8);
        assertTrue(generated.contains("# set absent-set not deployed - " + file(tmp.resolve("packs"))
                + "/absent-set.pack not found\n"), generated);
    }

    @Test
    void aSetIsReadFromTheClasspathToo() throws Exception {
        // the manifest's location may be classpath:/ - the test classes folder is on the classpath
        var classes = Path.of(GraphSetLoaderTest.class.getResource("/graph").toURI()).getParent();
        pack(classes.resolve("graph-set-test"), "classpath-set", "3",
                Map.of("unit-test-set-classpath", graph("unit-test-set-classpath", "from the classpath")));
        compile(manifest(tmp.resolve("graphs.yaml"), "classpath:/graph-set-test", List.of(), List.of("classpath-set"),
                file(tmp.resolve("unpack"))), "unit-test-set-classpath");
        assertTrue(CompiledGraphs.graphExists("unit-test-set-classpath"));
        assertEquals("from the classpath", run("unit-test-set-classpath").getBody());
    }
}
