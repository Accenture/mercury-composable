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

import com.accenture.minigraph.common.GraphModelValidator;
import com.accenture.minigraph.models.CompiledGraphs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.MultiLevelMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompileGraphTest {

    @BeforeAll
    static void setup() {
        PlaygroundLoader.main(new String[0]);
    }

    @Test
    void manifestListedGraphsAreCompiled() {
        assertTrue(CompiledGraphs.graphExists("hellojs"));
        assertTrue(CompiledGraphs.graphExists("tutorial-1"));
        // a graph ID that is not listed in graphs.yaml is not compiled and
        // therefore not executable - GraphExecutor answers 404 for it
        assertFalse(CompiledGraphs.graphExists("tutorial-99"));
    }

    @Test
    void validSuspendResumeGraphsAreCompiled() {
        assertTrue(CompiledGraphs.graphExists("unit-test-suspend-1"));
        assertTrue(CompiledGraphs.graphExists("unit-test-suspend-2"));
        assertTrue(CompiledGraphs.graphExists("unit-test-suspend-3"));
        assertTrue(CompiledGraphs.graphExists("unit-test-suspend-4"));
    }

    @Test
    void invalidManifestGraphsAreNotCompiled() {
        // every deliberately invalid manifest graph must fail the gate; deployed
        // execution is served exclusively from the compiled registry, so a rejected
        // graph answers 404 as if it does not exist (CompileFlows parity) -
        // err1-err7 break the suspend/resume contract, unit-test-no-end has no
        // 'end' node (a run could never complete)
        for (var id : List.of("unit-test-suspend-err1", "unit-test-suspend-err2", "unit-test-suspend-err3",
                              "unit-test-suspend-err4", "unit-test-suspend-err5", "unit-test-suspend-err6",
                              "unit-test-suspend-err7", "unit-test-no-end")) {
            assertFalse(CompiledGraphs.graphExists(id), id + " must be rejected by the quality gate");
        }
    }

    @Test
    void manifestLocationDefaultsToClasspathGraph() {
        // the engine's test manifest declares no 'location' - the CompileFlows-style
        // default applies (the playground example app's manifest sets it explicitly)
        assertEquals("classpath:/graph", CompiledGraphs.getDeployedLocation());
    }

    @Test
    void staticValidatorRejectsEveryInvalidSuspendResumeShape() {
        // direct coverage of every static rule, independent of the manifest:
        // err1 graph.suspend node not named 'suspend'; err2 suspend=true on graph.math;
        // err3 suspensible node without a suspend node; err4 suspend node without ttl;
        // err5 suspensible node without a drawn edge to 'suspend'; err6 suspend node
        // without an outgoing connection; err7 suspension point without a
        // continuation edge (a resumed run could not continue)
        for (var id : List.of("unit-test-suspend-err1", "unit-test-suspend-err2", "unit-test-suspend-err3",
                              "unit-test-suspend-err4", "unit-test-suspend-err5", "unit-test-suspend-err6",
                              "unit-test-suspend-err7")) {
            var reader = new ConfigReader("classpath:/graph/" + id + ".json");
            var graph = new MiniGraph();
            graph.importGraph(reader.getMap());
            assertThrows(IllegalArgumentException.class, () -> GraphModelValidator.validateSuspendResume(graph),
                    id + " must fail the static validator");
        }
    }

    @Test
    void nodeTtlPlacementAndMetadataImmutabilityAreValidated() {
        // valid: a graph.task node may declare a child-call deadline (ttl in the suspend grammar)
        var ok = importGraph("unit-test-ttl-ok");
        GraphModelValidator.validate(ok);
        // err1: ttl on a skill without child-call deadline semantics (graph.math);
        // err2: malformed duration on a deadline skill;
        // err3: a data mapping writing to reserved model metadata (model.ttl)
        for (var id : List.of("unit-test-ttl-err1", "unit-test-ttl-err2", "unit-test-ttl-err3")) {
            var graph = importGraph(id);
            assertThrows(IllegalArgumentException.class, () -> GraphModelValidator.validate(graph),
                    id + " must fail the static validator");
        }
    }

    private MiniGraph importGraph(String id) {
        var reader = new ConfigReader("classpath:/graph/" + id + ".json");
        var graph = new MiniGraph();
        graph.importGraph(reader.getMap());
        return graph;
    }

    @Test
    void deprecatedTypeMatchingSyntaxIsConvertedAtCompileTime() {
        Map<String, Object> model = CompiledGraphs.getGraph("hellojs");
        assertNotNull(model);
        var mm = new MultiLevelMap(model);
        Object nodes = model.get("nodes");
        assertInstanceOf(List.class, nodes);
        boolean found = false;
        for (int i = 0; i < ((List<?>) nodes).size(); i++) {
            var mapping = mm.getElement("nodes[" + i + "].properties.mapping");
            if (mapping instanceof List<?> entries) {
                for (var entry : entries) {
                    var line = String.valueOf(entry);
                    // the deprecated colon syntax must be gone
                    assertFalse(line.contains("model.number:int"), "colon syntax should be converted: " + line);
                    if (line.equals("f:int(model.number) -> hello.xyz")) {
                        found = true;
                    }
                }
            }
        }
        assertTrue(found, "expected converted mapping entry not found");
    }
}
