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

package com.accenture.minigraph.packager;

import com.accenture.minigraph.common.GraphSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.platformlambda.core.serializers.CanonicalPackager;
import org.platformlambda.core.serializers.SimpleMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class GraphPackagerTest {
    private static final int TUTORIALS = 14;

    @TempDir
    Path tmp;

    private record Result(int code, String out, String err) {
    }

    private static Result run(String... args) {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        int code = new GraphPackager(new PrintStream(out, true, UTF_8), new PrintStream(err, true, UTF_8)).run(args);
        return new Result(code, out.toString(UTF_8), err.toString(UTF_8));
    }

    /**
     * Copy the engine's tutorial graphs, which every application can deploy, into a folder.
     */
    private static Path tutorials(Path folder) throws IOException {
        Files.createDirectories(folder);
        for (int i = 1; i <= TUTORIALS; i++) {
            var name = "tutorial-" + i + ".json";
            try (var in = GraphPackagerTest.class.getResourceAsStream("/graph/" + name)) {
                assertNotNull(in, name + " is on the classpath");
                Files.write(folder.resolve(name), in.readAllBytes());
            }
        }
        return folder;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(Path file) throws IOException {
        return SimpleMapper.getInstance().getMapper().readValue(Files.readString(file, UTF_8), Map.class);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static Path write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        return Files.writeString(file, text, UTF_8);
    }

    @Test
    void packsTheTutorialsAndEveryEntryEqualsItsFile() throws Exception {
        var graphs = tutorials(tmp.resolve("graphs"));
        var out = tmp.resolve("out");
        var r = run("pack", "--set", "tutorials", "--manifest", "version=1.0.0", "--out", out.toString(),
                graphs.toString());
        assertEquals(0, r.code(), r.err());
        var bytes = Files.readAllBytes(out.resolve("tutorials.pack"));
        assertTrue(r.out().contains("Packed 14 graphs into " + out.resolve("tutorials.pack")), r.out());
        assertTrue(r.out().contains("SHA-256 " + sha256(bytes)), r.out());
        var contents = GraphSet.read(bytes);
        assertEquals("mercury-package", contents.manifest().get("format"));
        assertEquals("tutorials", contents.manifest().get("set"));
        assertEquals("1.0.0", contents.manifest().get("version"));
        assertEquals(TUTORIALS, contents.graphs().size());
        for (var graph : contents.graphs().entrySet()) {
            var original = parse(graphs.resolve(graph.getKey() + ".json"));
            assertArrayEquals(CanonicalPackager.encode(original), CanonicalPackager.encode(graph.getValue()),
                    graph.getKey());
        }
    }

    @Test
    void theSameGraphsAndFieldsGiveTheSameBytes() throws Exception {
        var first = tmp.resolve("first");
        run("pack", "--set", "s", "--manifest", "version=1", "--manifest", "author=team", "--out", first.toString(),
                tutorials(tmp.resolve("a")).toString());
        // the same graphs from another folder, named one by one in reverse order, and the fields in reverse order
        List<String> args = new ArrayList<>(List.of("pack", "--set", "s", "--manifest", "author=team",
                "--manifest", "version=1", "--out", tmp.resolve("second").toString()));
        var copy = tutorials(tmp.resolve("b"));
        for (int i = TUTORIALS; i >= 1; i--) {
            args.add(copy.resolve("tutorial-" + i + ".json").toString());
        }
        assertEquals(0, run(args.toArray(String[]::new)).code());
        assertArrayEquals(Files.readAllBytes(first.resolve("s.pack")),
                Files.readAllBytes(tmp.resolve("second/s.pack")));
    }

    @Test
    void unpackWritesReadableJsonThatPacksToTheSameBytes() throws Exception {
        var out = tmp.resolve("out");
        run("pack", "--set", "tutorials", "--manifest", "version=1.0.0", "--out", out.toString(),
                tutorials(tmp.resolve("graphs")).toString());
        var unpacked = tmp.resolve("unpacked");
        var r = run("unpack", out.resolve("tutorials.pack").toString(), "--out", unpacked.toString());
        assertEquals(0, r.code(), r.err());
        assertTrue(r.out().contains("Unpacked 14 graphs from"), r.out());
        // the canonical key order and a two-space indent, so two versions of a set diff cleanly
        var text = Files.readString(unpacked.resolve("tutorial-1.json"), UTF_8);
        assertTrue(text.startsWith("{\n  \"connections\": [\n    {\n      \"relations\": ["), text);
        assertTrue(text.endsWith("}\n"));
        var repacked = tmp.resolve("repacked");
        assertEquals(0, run("pack", "--set", "tutorials", "--manifest", "version=1.0.0", "--out", repacked.toString(),
                unpacked.toString()).code());
        assertArrayEquals(Files.readAllBytes(out.resolve("tutorials.pack")),
                Files.readAllBytes(repacked.resolve("tutorials.pack")));
    }

    @Test
    void aSetIsRefusedWithEveryReasonTheGateGives() throws Exception {
        var graphs = tutorials(tmp.resolve("graphs"));
        write(graphs.resolve("no-end.json"), """
                {"nodes": [{"alias": "root", "types": ["Root"],
                            "properties": {"purpose": "a graph that cannot complete", "name": "no-end"}}],
                 "connections": []}""");
        write(graphs.resolve("no-purpose.json"), """
                {"nodes": [{"alias": "root", "types": ["Root"], "properties": {"name": "no-purpose"}},
                           {"alias": "end", "types": ["End"], "properties": {}}],
                 "connections": [{"source": "root", "target": "end", "relations": [{"type": "done", "properties": {}}]}]}""");
        var out = tmp.resolve("out");
        var r = run("pack", "--set", "mixed", "--out", out.toString(), graphs.toString());
        assertEquals(1, r.code());
        assertTrue(r.err().contains("no-end: graph must have an 'end' node"), r.err());
        assertTrue(r.err().contains("no-purpose: root node must define a non-empty 'purpose' property"), r.err());
        assertFalse(Files.exists(out.resolve("mixed.pack")));
    }

    @Test
    void namesThatBreakTheRulesAreRefused() throws Exception {
        var graphs = tutorials(tmp.resolve("graphs"));
        Files.copy(graphs.resolve("tutorial-1.json"), graphs.resolve("bad.name.json"));
        Files.copy(graphs.resolve("tutorial-1.json"), graphs.resolve("renamed.json"));
        var r = run("pack", "--set", "bad set", "--out", tmp.resolve("out").toString(), graphs.toString());
        assertEquals(1, r.code());
        assertTrue(r.err().contains("set name 'bad set' - use letters, digits, '_' and '-' only"), r.err());
        assertTrue(r.err().contains("bad.name: graph id - use letters, digits, '_' and '-' only"), r.err());
        assertTrue(r.err().contains("renamed: the root node's name 'tutorial-1' differs from the graph id"), r.err());
    }

    @Test
    void manifestFieldsFollowTheSetRules() throws Exception {
        var graphs = tutorials(tmp.resolve("graphs")).toString();
        var out = tmp.resolve("out").toString();
        assertEquals(0, run("pack", "--set", "entry", "--manifest", "graph_id=tutorial-1", "--out", out, graphs).code());
        assertEquals("tutorial-1", GraphSet.read(Files.readAllBytes(tmp.resolve("out/entry.pack")))
                .manifest().get("graph_id"));
        var r = run("pack", "--set", "s", "--manifest", "graph_id=nope", "--out", out, graphs);
        assertEquals(1, r.code());
        assertTrue(r.err().contains("manifest field 'graph_id' - 'nope' is not a graph of the set"), r.err());
        r = run("pack", "--set", "s", "--manifest", "format=x", "--manifest", "set=y", "--out", out, graphs);
        assertEquals(1, r.code());
        assertTrue(r.err().contains("manifest field 'format' - it is written by the packager"), r.err());
        assertTrue(r.err().contains("manifest field 'set' - it is written from the set name"), r.err());
        r = run("pack", "--set", "s", "--manifest", "novalue", "--out", out, graphs);
        assertEquals(1, r.code());
        assertTrue(r.err().contains("--manifest takes key=value, not 'novalue'"), r.err());
        r = run("pack", "--set", "s", "--manifest", "k=1", "--manifest", "k=2", "--out", out, graphs);
        assertEquals(1, r.code());
        assertTrue(r.err().contains("The manifest field 'k' is given twice"), r.err());
    }

    @Test
    void packsExactlyWhatADeploymentManifestLists() throws Exception {
        var graphs = tutorials(tmp.resolve("graphs"));
        var out = tmp.resolve("out");
        var manifest = write(tmp.resolve("deploy/graphs.yaml"),
                "graphs:\n  - 'tutorial-1'\n  - 'tutorial-2'\nlocation: 'file:" + graphs + "'\n");
        var r = run("pack", "--set", "two", "--from-manifest", manifest.toString(), "--out", out.toString());
        assertEquals(0, r.code(), r.err());
        assertEquals(List.of("tutorial-1", "tutorial-2"),
                List.copyOf(GraphSet.read(Files.readAllBytes(out.resolve("two.pack"))).graphs().keySet()));
        // a folder written without file: resolves against the manifest's own folder
        var relative = write(tmp.resolve("graphs.yaml"), "graphs:\n  - 'tutorial-3'\nlocation: 'graphs'\n");
        assertEquals(0, run("pack", "--set", "three", "--from-manifest", relative.toString(),
                "--out", out.toString()).code());
        // a classpath location is inside an application, so the pipeline is told to pass the folder
        var classpath = write(tmp.resolve("cp/graphs.yaml"), "graphs:\n  - 'tutorial-1'\nlocation: 'classpath:/graph'\n");
        r = run("pack", "--set", "cp", "--from-manifest", classpath.toString(), "--out", out.toString());
        assertEquals(1, r.code());
        assertTrue(r.err().contains("inside an application - pass the folder that holds the graphs instead"), r.err());
        // the default location is the classpath too
        var noLocation = write(tmp.resolve("none/graphs.yaml"), "graphs:\n  - 'tutorial-1'\n");
        assertEquals(1, run("pack", "--set", "none", "--from-manifest", noLocation.toString(),
                "--out", out.toString()).code());
        // a listed id is checked before it becomes a path
        var crafted = write(tmp.resolve("crafted/graphs.yaml"),
                "graphs:\n  - '../graphs/tutorial-1'\nlocation: 'file:" + graphs + "'\n");
        r = run("pack", "--set", "crafted", "--from-manifest", crafted.toString(), "--out", out.toString());
        assertEquals(1, r.code());
        assertTrue(r.err().contains("graph id '../graphs/tutorial-1'"), r.err());
        r = run("pack", "--set", "x", "--from-manifest", manifest.toString(), graphs.toString());
        assertEquals(1, r.code());
        assertTrue(r.err().contains("Give the graphs or --from-manifest, not both"), r.err());
    }

    @Test
    void unpackRefusesACraftedEntryNameBeforeWritingAnything() throws Exception {
        var model = parse(tutorials(tmp.resolve("graphs")).resolve("tutorial-1.json"));
        var crafted = tmp.resolve("crafted.pack");
        Files.write(crafted, CanonicalPackager.builder().manifest("set", "crafted").add("../evil.json", model).build());
        var target = tmp.resolve("target/out");
        var r = run("unpack", crafted.toString(), "--out", target.toString());
        assertEquals(1, r.code());
        assertTrue(r.err().contains("entry '../evil.json' - expect <graph-id>.json"), r.err());
        assertFalse(Files.exists(tmp.resolve("target/evil.json")));
        assertFalse(Files.exists(target));
    }

    @Test
    void aPackageThatIsNotCanonicalIsAFormatError() throws Exception {
        var out = tmp.resolve("out");
        run("pack", "--set", "tutorials", "--out", out.toString(), tutorials(tmp.resolve("graphs")).toString());
        var bytes = Files.readAllBytes(out.resolve("tutorials.pack"));
        var extended = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, extended, 0, bytes.length);
        var corrupt = Files.write(tmp.resolve("corrupt.pack"), extended);
        var r = run("unpack", corrupt.toString(), "--out", tmp.resolve("x").toString());
        assertEquals(2, r.code());
        assertTrue(r.err().startsWith("Error: "), r.err());
        assertEquals(2, run("inspect", corrupt.toString()).code());
    }

    @Test
    @SuppressWarnings("unchecked")
    void inspectReportsTheSetForPipelines() throws Exception {
        var out = tmp.resolve("out");
        run("pack", "--set", "tutorials", "--manifest", "version=1.0.0", "--out", out.toString(),
                tutorials(tmp.resolve("graphs")).toString());
        var file = out.resolve("tutorials.pack");
        var bytes = Files.readAllBytes(file);
        var r = run("inspect", file.toString(), "--json");
        assertEquals(0, r.code(), r.err());
        // standard output holds the report alone: the engine's log goes to standard error
        Map<String, Object> report = SimpleMapper.getInstance().getMapper().readValue(r.out(), Map.class);
        assertEquals(sha256(bytes), report.get("sha256"));
        assertEquals(bytes.length, ((Number) report.get("size")).intValue());
        assertEquals("tutorials", ((Map<String, Object>) report.get("manifest")).get("set"));
        var graphs = (List<Map<String, Object>>) report.get("graphs");
        assertEquals(TUTORIALS, graphs.size());
        assertEquals("tutorial-1", graphs.getFirst().get("id"));
        assertEquals(2, ((Number) graphs.getFirst().get("nodes")).intValue());
        assertEquals(1, ((Number) graphs.getFirst().get("connections")).intValue());
        r = run("inspect", file.toString());
        assertEquals(0, r.code(), r.err());
        assertTrue(r.out().contains("SHA-256   " + sha256(bytes)), r.out());
        assertTrue(r.out().contains("  tutorial-1   2 nodes, 1 connection"), r.out());
        assertTrue(r.out().contains("  version         1.0.0"), r.out());
    }

    @Test
    void aMissingFileIsAnIoError() throws Exception {
        var missing = tmp.resolve("missing").toString();
        assertEquals(2, run("pack", "--set", "s", missing).code());
        assertEquals(2, run("pack", "--set", "s", "--from-manifest", missing).code());
        assertEquals(2, run("unpack", missing, "--out", tmp.toString()).code());
        var r = run("inspect", missing);
        assertEquals(2, r.code());
        assertTrue(r.err().contains("no such file"), r.err());
        var notJson = write(tmp.resolve("broken/broken.json"), "{ not json");
        r = run("pack", "--set", "s", notJson.toString());
        assertEquals(2, r.code());
        assertTrue(r.err().contains("is not a JSON graph model"), r.err());
    }

    @Test
    void usageErrorsAreRefusedWithTheUsage() {
        assertEquals(1, run().code());
        var r = run("nope");
        assertEquals(1, r.code());
        assertTrue(r.err().contains("Unknown command 'nope'") && r.err().contains("Usage:"), r.err());
        assertTrue(run("pack", "x.json").err().contains("pack needs --set <name>"));
        assertTrue(run("unpack", "x.pack").err().contains("unpack needs --out <dir>"));
        assertTrue(run("inspect", "x.pack", "--verbose").err().contains("Unknown option '--verbose'"));
        r = run("--help");
        assertEquals(0, r.code());
        assertTrue(r.out().contains("graph-packager inspect <file.pack> [--json]"), r.out());
    }

    @Test
    void aGraphJsNodeIsCheckedWithoutGraalVm() throws Exception {
        // the gate reads the graph.js statements but never runs a script, so the packager leaves GraalVM out
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.graalvm.polyglot.Context"));
        var graphs = write(tmp.resolve("js/js-graph.json"), """
                {"nodes": [
                  {"alias": "root", "types": ["Root"],
                   "properties": {"purpose": "a graph.js node, packed without GraalVM", "name": "js-graph"}},
                  {"alias": "calc", "types": ["JavaScript"],
                   "properties": {"skill": "graph.js", "statement": ["COMPUTE: sum -> 3 + 5"]}},
                  {"alias": "end", "types": ["End"],
                   "properties": {"skill": "graph.data.mapper", "mapping": ["calc.result.sum -> output.body.sum"]}}],
                 "connections": [
                  {"source": "root", "target": "calc", "relations": [{"type": "next", "properties": {}}]},
                  {"source": "calc", "target": "end", "relations": [{"type": "next", "properties": {}}]}]}""")
                .getParent();
        var r = run("pack", "--set", "js", "--out", tmp.resolve("out").toString(), graphs.toString());
        assertEquals(0, r.code(), r.err());
    }

    @Test
    void aNullPropertyIsFilteredOutWhenPacked() throws Exception {
        // a graph holds no null property: "key": null is filtered out, and an empty string is a value
        write(tmp.resolve("a/nulls.json"), """
                {"nodes": [
                  {"alias": "root", "types": ["Root"],
                   "properties": {"purpose": "null properties", "name": "nulls", "note": null, "empty": ""}},
                  {"alias": "end", "types": ["End"], "properties": {}}],
                 "connections": [{"source": "root", "target": "end",
                                  "relations": [{"type": "done", "properties": {"x": null}}]}]}""");
        write(tmp.resolve("b/nulls.json"), """
                {"nodes": [
                  {"alias": "root", "types": ["Root"],
                   "properties": {"purpose": "null properties", "name": "nulls", "empty": ""}},
                  {"alias": "end", "types": ["End"], "properties": {}}],
                 "connections": [{"source": "root", "target": "end",
                                  "relations": [{"type": "done", "properties": {}}]}]}""");
        assertEquals(0, run("pack", "--set", "s", "--out", tmp.resolve("out-a").toString(),
                tmp.resolve("a").toString()).code());
        assertEquals(0, run("pack", "--set", "s", "--out", tmp.resolve("out-b").toString(),
                tmp.resolve("b").toString()).code());
        assertArrayEquals(Files.readAllBytes(tmp.resolve("out-b/s.pack")), Files.readAllBytes(tmp.resolve("out-a/s.pack")));
        assertEquals(0, run("unpack", tmp.resolve("out-a/s.pack").toString(), "--out",
                tmp.resolve("unpacked").toString()).code());
        var text = Files.readString(tmp.resolve("unpacked/nulls.json"), UTF_8);
        assertFalse(text.contains(": null") || text.contains("\"note\"") || text.contains("\"x\""), text);
        assertTrue(text.contains("\"empty\": \"\""), text);
    }

    @Test
    void aReferenceIsCheckedResolvedButPackedAsWritten() throws Exception {
        // the gate reads a deployed model with its ${...} references resolved; the package keeps them,
        // because they belong to the environment the set is deployed to
        var graphs = write(tmp.resolve("ref/ref-graph.json"), """
                {"nodes": [
                  {"alias": "root", "types": ["Root"],
                   "properties": {"purpose": "${GRAPH_PACKAGER_TEST_PURPOSE:resolved where it is deployed}",
                                  "name": "ref-graph"}},
                  {"alias": "end", "types": ["End"], "properties": {}}],
                 "connections": [{"source": "root", "target": "end", "relations": [{"type": "done", "properties": {}}]}]}""")
                .getParent();
        var out = tmp.resolve("out");
        assertEquals(0, run("pack", "--set", "ref", "--out", out.toString(), graphs.toString()).code());
        var model = GraphSet.read(Files.readAllBytes(out.resolve("ref.pack"))).graphs().get("ref-graph");
        assertTrue(GraphSet.toJson(model).contains("${GRAPH_PACKAGER_TEST_PURPOSE:resolved where it is deployed}"));
    }
}
