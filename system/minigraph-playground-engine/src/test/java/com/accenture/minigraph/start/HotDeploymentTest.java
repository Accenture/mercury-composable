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
import com.accenture.minigraph.start.HotDeployment.Refused;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hot deploy and undeploy of graph sets (RFC-0017, WP1): a set deployed to the running engine registers through
 * the gate in one swap and is served at once; the folder's records restore it at startup and carry it to another
 * instance at its next lookup; a bundled id, another set's id, a set not named after its main graph and a graph
 * the gate refuses are each refused with nothing left behind. Each test configures the hot folder under its own
 * temporary folder and uses graph ids no other test uses.
 */
class HotDeploymentTest {
    private static final long TIMEOUT = 10000;
    private static String target;
    private final List<String> registered = new ArrayList<>();
    @TempDir
    Path tmp;
    private String hotLocation;

    @BeforeAll
    static void setup() {
        PlaygroundLoader.main(new String[0]);
        target = "http://localhost:" + AppConfigReader.getInstance().getProperty("rest.server.port");
    }

    @BeforeEach
    void configureTheHotFolder() {
        hotLocation = "file:" + tmp.resolve("deploy");
        // the startup sequence: configured before the manifests, the folder read after them
        HotDeployment.configure(true, true, hotLocation, 0);
        HotDeployment.reconcileAll();
        assertTrue(HotDeployment.isEnabled());
    }

    @AfterEach
    void forgetTheTestGraphs() {
        // the registry is shared by every test in this JVM
        registered.forEach(CompiledGraphs::removeGraph);
        HotDeployment.reset();
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> graphWithoutEnd(String id) {
        return SimpleMapper.getInstance().getMapper().readValue("""
                {"nodes": [{"alias": "root", "types": ["Root"],
                            "properties": {"purpose": "no end node", "name": "%s"}}],
                 "connections": []}
                """.formatted(id), Map.class);
    }

    private static byte[] pack(String setName, String version, Map<String, Map<String, Object>> graphs) {
        var fields = version == null? Map.<String, String>of() : Map.of("version", version);
        return GraphSet.pack(setName, fields, graphs);
    }

    private static Map<String, Map<String, Object>> graphs(String... idsAndAnswers) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (int i = 0; i < idsAndAnswers.length; i += 2) {
            result.put(idsAndAnswers[i], graph(idsAndAnswers[i], idsAndAnswers[i + 1]));
        }
        return result;
    }

    private HotDeployment.Deployment deploy(byte[] bytes, String... ids) throws Refused {
        registered.addAll(List.of(ids));
        return HotDeployment.deploy(bytes, null);
    }

    private Path folder() {
        return tmp.resolve("deploy");
    }

    private static EventEnvelope run(String graphId) throws Exception {
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/graph/" + graphId).setBody(Map.of())
                .setHeader("Content-Type", "application/json").setHeader("Accept", "application/json");
        var po = PostOffice.trackable("unit.test", String.format("%032x", graphId.hashCode()),
                "TEST /graph/" + graphId);
        return po.asyncRequest(new EventEnvelope().setTo("async.http.request").setBody(request), TIMEOUT)
                .await(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Another instance over the same folder: this instance forgets everything it registered from the folder, the
     * files stay, and the registry no longer holds the set's graphs.
     */
    private void becomeAnotherInstance(String... ids) {
        HotDeployment.configure(true, true, hotLocation, 0);
        for (var id : ids) {
            CompiledGraphs.removeGraph(id);
        }
    }

    private static List<String> filesIn(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    @Test
    void aSetDeploysInOneStepAndTheEndpointServesItsMainGraph() throws Exception {
        var bytes = pack("unit-hot-main", "1.0.0",
                graphs("unit-hot-main", "the main graph", "unit-hot-sub", "a subgraph"));
        var deployment = deploy(bytes, "unit-hot-main", "unit-hot-sub");
        assertTrue(deployment.changed());
        assertEquals("unit-hot-main", deployment.set());
        assertEquals("1.0.0", deployment.version());
        assertEquals(List.of("unit-hot-main", "unit-hot-sub"), deployment.graphs());
        assertNull(deployment.replaced());
        assertEquals(GraphSetLoader.sha256(bytes), deployment.sha256());
        for (var id : List.of("unit-hot-main", "unit-hot-sub")) {
            assertTrue(CompiledGraphs.graphExists(id), id);
            assertEquals(hotLocation, CompiledGraphs.getGraphLocation(id));
            assertEquals(new CompiledGraphs.DeployedSet("unit-hot-main", "1.0.0"), CompiledGraphs.getGraphSet(id));
        }
        // the main graph is public, every other graph of the set is private to it (D15)
        assertFalse(HotDeployment.isPrivateSubgraph("unit-hot-main"));
        assertTrue(HotDeployment.isPrivateSubgraph("unit-hot-sub"));
        assertFalse(HotDeployment.isPrivateSubgraph("tutorial-1"), "a bundled graph is never private");
        // the folder holds the package, one file per graph and the record - and nothing else
        assertEquals(List.of("unit-hot-main.deployed.yaml", "unit-hot-main.json", "unit-hot-main.pack",
                "unit-hot-sub.json"), filesIn(folder()));
        var recordText = Files.readString(folder().resolve("unit-hot-main.deployed.yaml"), UTF_8);
        assertTrue(recordText.contains("set: 'unit-hot-main'\n"), recordText);
        assertTrue(recordText.contains("sha256: '" + deployment.sha256() + "'\n"), recordText);
        assertTrue(recordText.contains("graphs:\n  - 'unit-hot-main'\n  - 'unit-hot-sub'\n"), recordText);
        assertTrue(recordText.contains("files:\n  - 'unit-hot-main.pack'\n  - 'unit-hot-main.json'\n"), recordText);
        assertTrue(CompiledGraphs.getDeployedLocations().contains(hotLocation));
        var response = run("unit-hot-main");
        assertEquals(200, response.getStatus(), String.valueOf(response.getBody()));
        assertEquals("the main graph", response.getBody());
        var listed = HotDeployment.list();
        assertEquals(1, listed.size());
        assertEquals("deployed", listed.getFirst().status());
        assertEquals(deployment.sha256(), listed.getFirst().sha256());
    }

    @Test
    void theSameBytesChangeNothingAndANewVersionReplacesTheSetAsAUnit() throws Exception {
        var v1 = pack("unit-hot-versioned", "1", graphs("unit-hot-versioned", "v1", "unit-hot-dropped", "dropped"));
        deploy(v1, "unit-hot-versioned", "unit-hot-dropped", "unit-hot-added");
        var again = HotDeployment.deploy(v1, GraphSetLoader.sha256(v1));
        assertFalse(again.changed(), "the same bytes are already deployed");
        assertEquals("1", again.version());
        var v2 = pack("unit-hot-versioned", "2", graphs("unit-hot-versioned", "v2", "unit-hot-added", "added"));
        var replaced = HotDeployment.deploy(v2, null);
        assertTrue(replaced.changed());
        assertEquals("2", replaced.version());
        assertNotNull(replaced.replaced());
        assertEquals("1", replaced.replaced().version());
        assertEquals(GraphSetLoader.sha256(v1), replaced.replaced().sha256());
        // the dropped graph left the registry and the folder; the added one is served
        assertFalse(CompiledGraphs.graphExists("unit-hot-dropped"));
        assertFalse(Files.exists(folder().resolve("unit-hot-dropped.json")));
        assertTrue(CompiledGraphs.graphExists("unit-hot-added"));
        assertEquals("2", CompiledGraphs.getGraphSet("unit-hot-versioned").version());
        assertEquals("v2", run("unit-hot-versioned").getBody());
        var recordText = Files.readString(folder().resolve("unit-hot-versioned.deployed.yaml"), UTF_8);
        assertTrue(recordText.contains("version: '2'\n"), recordText);
        assertFalse(recordText.contains("unit-hot-dropped"), recordText);
    }

    @Test
    void aSetIsNamedAfterItsMainGraph() {
        var noMain = pack("unit-hot-no-main", null, graphs("unit-hot-other", "x"));
        var refused = assertThrows(Refused.class, () -> HotDeployment.deploy(noMain, null));
        assertEquals(400, refused.getStatus());
        assertTrue(refused.getMessage().contains("named after its main graph"), refused.getMessage());
        assertFalse(CompiledGraphs.graphExists("unit-hot-other"));
        // graph_id, when the manifest has it, must agree with the set name
        var disagreeing = GraphSet.pack("unit-hot-entry", Map.of("graph_id", "unit-hot-entry-sub"),
                graphs("unit-hot-entry", "main", "unit-hot-entry-sub", "sub"));
        var refusedToo = assertThrows(Refused.class, () -> HotDeployment.deploy(disagreeing, null));
        assertEquals(400, refusedToo.getStatus());
        assertTrue(refusedToo.getMessage().contains("graph_id"), refusedToo.getMessage());
        assertFalse(CompiledGraphs.graphExists("unit-hot-entry"));
    }

    @Test
    void aBundledIdAndAnotherSetsIdAreRefusedWithTheOwnerNamed() throws Exception {
        // tutorial-1 is a bundled graph of the test manifest
        var shadowing = pack("unit-hot-shadow", null, graphs("unit-hot-shadow", "main", "tutorial-1", "a copy"));
        var refused = assertThrows(Refused.class, () -> HotDeployment.deploy(shadowing, null));
        assertEquals(409, refused.getStatus());
        assertTrue(refused.getMessage().contains("tutorial-1 is owned by classpath:/graph"), refused.getMessage());
        assertFalse(CompiledGraphs.graphExists("unit-hot-shadow"));
        assertEquals("classpath:/graph", CompiledGraphs.getGraphLocation("tutorial-1"), "the bundled copy stands");
        assertTrue(filesIn(folder()).isEmpty(), "nothing was written");
        // one hot set owns an id: a second set carrying it is refused, and the first keeps serving
        deploy(pack("unit-hot-first", "1", graphs("unit-hot-first", "first", "unit-hot-shared", "shared")),
                "unit-hot-first", "unit-hot-shared");
        var second = pack("unit-hot-second", "1", graphs("unit-hot-second", "second", "unit-hot-shared", "copy"));
        var refusedToo = assertThrows(Refused.class, () -> HotDeployment.deploy(second, null));
        assertEquals(409, refusedToo.getStatus());
        assertTrue(refusedToo.getMessage().contains("unit-hot-shared is owned by set unit-hot-first"),
                refusedToo.getMessage());
        assertFalse(CompiledGraphs.graphExists("unit-hot-second"));
        assertEquals("unit-hot-first", CompiledGraphs.getGraphSet("unit-hot-shared").name());
        assertEquals("first", run("unit-hot-first").getBody());
    }

    @Test
    void aRefusedUploadLeavesNoFileAndTheDigestIsChecked() throws Exception {
        // the packager would refuse this set (the gate runs when a set is packed), so it is built with the
        // canonical packager directly - what a deployment can meet
        var builder = CanonicalPackager.builder().manifest(GraphSet.SET, "unit-hot-half");
        builder.add("unit-hot-half.json", graph("unit-hot-half", "good"));
        builder.add("unit-hot-half-no-end.json", graphWithoutEnd("unit-hot-half-no-end"));
        var bytes = builder.build();
        var refused = assertThrows(Refused.class, () -> HotDeployment.deploy(bytes, null));
        assertEquals(400, refused.getStatus());
        assertTrue(refused.getMessage().contains("unit-hot-half-no-end"), refused.getMessage());
        assertFalse(CompiledGraphs.graphExists("unit-hot-half"), "all or none: the valid graph is not registered");
        assertTrue(filesIn(folder()).isEmpty(), "a refused upload leaves nothing, not even a staging folder");
        // a digest the caller expects must be the body's
        var good = pack("unit-hot-digest", null, graphs("unit-hot-digest", "x"));
        var mismatch = assertThrows(Refused.class, () -> HotDeployment.deploy(good, "00" .repeat(32)));
        assertEquals(400, mismatch.getStatus());
        assertTrue(mismatch.getMessage().contains("SHA-256 does not match"), mismatch.getMessage());
        assertFalse(CompiledGraphs.graphExists("unit-hot-digest"));
        var notASet = assertThrows(Refused.class, () -> HotDeployment.deploy("not a package".getBytes(UTF_8), null));
        assertEquals(400, notASet.getStatus());
        var empty = assertThrows(Refused.class, () -> HotDeployment.deploy(new byte[0], null));
        assertEquals(400, empty.getStatus());
    }

    @Test
    void undeployRemovesTheSetItsFilesAndItsRecord() throws Exception {
        deploy(pack("unit-hot-gone", "1", graphs("unit-hot-gone", "main", "unit-hot-gone-sub", "sub")),
                "unit-hot-gone", "unit-hot-gone-sub");
        assertEquals(200, run("unit-hot-gone").getStatus());
        var removed = HotDeployment.undeploy("unit-hot-gone");
        assertEquals(List.of("unit-hot-gone", "unit-hot-gone-sub"), removed.graphs().stream().sorted().toList());
        assertFalse(CompiledGraphs.graphExists("unit-hot-gone"));
        assertFalse(CompiledGraphs.graphExists("unit-hot-gone-sub"));
        assertTrue(filesIn(folder()).isEmpty(), "the record, the package and the graph files are gone");
        assertEquals(404, run("unit-hot-gone").getStatus());
        var again = assertThrows(Refused.class, () -> HotDeployment.undeploy("unit-hot-gone"));
        assertEquals(404, again.getStatus());
        // a bundled graph is never in the folder, so it answers 404 too
        var bundled = assertThrows(Refused.class, () -> HotDeployment.undeploy("tutorial-1"));
        assertEquals(404, bundled.getStatus());
        assertTrue(CompiledGraphs.graphExists("tutorial-1"));
        var invalid = assertThrows(Refused.class, () -> HotDeployment.undeploy("../escape"));
        assertEquals(400, invalid.getStatus());
    }

    @Test
    void theRecordsRestoreTheSetsAtStartupAndAStrayPackageIsIgnored() throws Exception {
        deploy(pack("unit-hot-restart", "3", graphs("unit-hot-restart", "back")), "unit-hot-restart");
        // a package someone copied in by hand has no record
        Files.write(folder().resolve("unit-hot-stray.pack"),
                pack("unit-hot-stray", null, graphs("unit-hot-stray", "x")));
        // a restart: the folder's files stay, the registry starts empty
        becomeAnotherInstance("unit-hot-restart");
        assertFalse(CompiledGraphs.getGraphLocation("unit-hot-restart") != null);
        HotDeployment.reconcileAll();
        assertTrue(CompiledGraphs.graphExists("unit-hot-restart"));
        assertEquals("3", CompiledGraphs.getGraphSet("unit-hot-restart").version());
        assertEquals("back", run("unit-hot-restart").getBody());
        assertFalse(CompiledGraphs.graphExists("unit-hot-stray"), "the records are the allowlist");
        var listed = HotDeployment.list();
        assertEquals(1, listed.size());
        assertEquals("unit-hot-restart", listed.getFirst().set());
        assertEquals("deployed", listed.getFirst().status());
    }

    @Test
    void anotherInstancesDeploymentReplacementAndUndeploymentAreSeenAtTheNextLookup() throws Exception {
        var v1 = pack("unit-hot-peer", "1", graphs("unit-hot-peer", "v1", "unit-hot-peer-sub", "sub"));
        deploy(v1, "unit-hot-peer", "unit-hot-peer-sub", "unit-hot-peer-new");
        // another instance over the same folder has never seen the set: its next lookup for the main graph loads it
        becomeAnotherInstance("unit-hot-peer", "unit-hot-peer-sub");
        assertNull(CompiledGraphs.getGraphLocation("unit-hot-peer"));
        assertNotNull(CompiledGraphs.getGraph("unit-hot-peer"), "an unknown id with a record of its name is loaded");
        assertTrue(CompiledGraphs.graphExists("unit-hot-peer-sub"), "the whole set came with it");
        assertEquals("v1", run("unit-hot-peer").getBody());
        // the first instance replaces the set: the record changed, so the peer's next lookup swaps the set
        var v2 = pack("unit-hot-peer", "2", graphs("unit-hot-peer", "v2", "unit-hot-peer-new", "new"));
        var rewritten = recordRewrittenBy(() -> HotDeployment.deploy(v2, null));
        assertTrue(rewritten);
        assertEquals("v2", run("unit-hot-peer").getBody());
        assertTrue(CompiledGraphs.graphExists("unit-hot-peer-new"));
        assertFalse(CompiledGraphs.graphExists("unit-hot-peer-sub"));
        // the first instance undeploys: the record is gone, so the peer drops the set at its next lookup
        Files.delete(folder().resolve("unit-hot-peer.deployed.yaml"));
        assertFalse(CompiledGraphs.graphExists("unit-hot-peer"));
        assertFalse(CompiledGraphs.graphExists("unit-hot-peer-new"));
        assertEquals(404, run("unit-hot-peer").getStatus());
    }

    /**
     * Run a deployment "on the first instance" - here the same JVM, so the registry already holds the result; what
     * the test checks is that the record on disk changed, which is all a peer ever reads.
     */
    private boolean recordRewrittenBy(Deployer deployer) throws Exception {
        var before = Files.readAllBytes(folder().resolve("unit-hot-peer.deployed.yaml"));
        deployer.run();
        var after = Files.readAllBytes(folder().resolve("unit-hot-peer.deployed.yaml"));
        return !java.util.Arrays.equals(before, after);
    }

    @FunctionalInterface
    private interface Deployer {
        void run() throws Exception;
    }

    @Test
    void aPackageThatNoLongerMatchesItsRecordIsUnregistered() throws Exception {
        deploy(pack("unit-hot-tampered", "1", graphs("unit-hot-tampered", "trusted")), "unit-hot-tampered");
        // the bytes under the record change without the record: not the bytes the record vouches for
        Files.write(folder().resolve("unit-hot-tampered.pack"),
                pack("unit-hot-tampered", "1", graphs("unit-hot-tampered", "tampered")));
        becomeAnotherInstance("unit-hot-tampered");
        assertNull(CompiledGraphs.getGraph("unit-hot-tampered"), "the record and the package disagree");
        var listed = HotDeployment.list();
        assertEquals(1, listed.size());
        assertEquals("not deployed - the package does not match its record", listed.getFirst().status());
        assertEquals(404, run("unit-hot-tampered").getStatus());
    }

    @Test
    void theSwitchesGovernDeployAndUndeploySeparately() throws Exception {
        var bytes = pack("unit-hot-switch", "1", graphs("unit-hot-switch", "x"));
        deploy(bytes, "unit-hot-switch");
        // deploy off, undeploy on: the set keeps serving and can still be removed, but nothing new enters
        HotDeployment.configure(false, true, hotLocation, 0);
        HotDeployment.reconcileAll();
        assertTrue(CompiledGraphs.graphExists("unit-hot-switch"));
        assertFalse(HotDeployment.canDeploy());
        assertTrue(HotDeployment.canUndeploy());
        var off = assertThrows(Refused.class, () -> HotDeployment.deploy(bytes, null));
        assertEquals(503, off.getStatus());
        HotDeployment.undeploy("unit-hot-switch");
        assertFalse(CompiledGraphs.graphExists("unit-hot-switch"));
        // both off: the folder is never read and both calls answer 503
        Files.write(folder().resolve("unit-hot-switch.pack"), bytes);
        HotDeployment.configure(false, false, hotLocation, 0);
        assertFalse(HotDeployment.isEnabled());
        HotDeployment.reconcileAll();
        assertFalse(CompiledGraphs.graphExists("unit-hot-switch"));
        assertEquals(503, assertThrows(Refused.class, () -> HotDeployment.deploy(bytes, null)).getStatus());
        assertEquals(503, assertThrows(Refused.class, () -> HotDeployment.undeploy("unit-hot-switch")).getStatus());
        assertTrue(HotDeployment.list().isEmpty());
    }

    @Test
    void theHotFolderAndAManifestsUnpackFolderRefuseEachOther() throws Exception {
        // a manifest whose 'unpack' lies inside the hot folder: its sets are skipped
        var packs = tmp.resolve("packs");
        Files.createDirectories(packs);
        Files.write(packs.resolve("unit-hot-unpack-set.pack"),
                pack("unit-hot-unpack-set", null, graphs("unit-hot-unpack-graph", "x")));
        var manifest = tmp.resolve("graphs.yaml");
        Files.writeString(manifest, "location: 'file:" + packs + "'\nsets:\n  - 'unit-hot-unpack-set'\nunpack: 'file:"
                + folder().resolve("inner") + "'\n");
        registered.add("unit-hot-unpack-graph");
        new CompileGraph().compileManifest("file:" + manifest);
        assertFalse(CompiledGraphs.graphExists("unit-hot-unpack-graph"), "an unpack folder inside the hot folder");
        // the hot folder inside a manifest's unpack folder: hot deployment turns itself off for the run
        var unpack = tmp.resolve("unpack");
        Files.writeString(manifest, "location: 'file:" + packs + "'\nsets:\n  - 'unit-hot-unpack-set'\nunpack: 'file:"
                + unpack + "'\n");
        HotDeployment.configure(false, false, hotLocation, 0);
        new CompileGraph().compileManifest("file:" + manifest);
        assertTrue(CompiledGraphs.graphExists("unit-hot-unpack-graph"));
        HotDeployment.configure(true, true, "file:" + unpack.resolve("hot"), 0);
        assertTrue(HotDeployment.isEnabled());
        HotDeployment.reconcileAll();
        assertFalse(HotDeployment.isEnabled(), "a hot folder inside an unpack folder is refused");
        // the Playground's temporary folder is refused at configuration
        HotDeployment.configure(true, true, "file:/tmp/graph/hot", 0);
        assertFalse(HotDeployment.isEnabled());
        assertFalse(Files.exists(Path.of("/tmp/graph/hot")));
        HotDeployment.configure(true, true, "classpath:/hot", 0);
        assertFalse(HotDeployment.isEnabled());
    }

    @Test
    void replaceGraphsSwapsRemovalsAndAdditionsAsOneUnit() {
        CompiledGraphs.addGraph("unit-hot-swap-old", graph("unit-hot-swap-old", "old"), "file:/swap");
        registered.addAll(List.of("unit-hot-swap-old", "unit-hot-swap-new"));
        CompiledGraphs.replaceGraphs(List.of("unit-hot-swap-old"),
                Map.of("unit-hot-swap-new", graph("unit-hot-swap-new", "new")), "file:/swap",
                new CompiledGraphs.DeployedSet("unit-hot-swap-new", "1"));
        assertNull(CompiledGraphs.getGraphLocation("unit-hot-swap-old"));
        assertEquals("file:/swap", CompiledGraphs.getGraphLocation("unit-hot-swap-new"));
        assertEquals(List.of("unit-hot-swap-new"), CompiledGraphs.getGraphsOfSet("unit-hot-swap-new", "file:/swap"));
        assertTrue(CompiledGraphs.getGraphsOfSet("unit-hot-swap-new", "file:/elsewhere").isEmpty());
    }
}
