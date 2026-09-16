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
    void reservedErrorAliasFailsTheGate() {
        // the generic exception context is staged under the top-level 'error' namespace
        // (error.source/code/message/stack - see 'inspect error'), and 'error' has always
        // been in MiniGraph's reserved alias list: node creation itself rejects it, so
        // the gate rejection is inherited - the model cannot even be imported
        assertFalse(CompiledGraphs.graphExists("unit-test-error-alias"),
                "a graph with a node aliased 'error' must be rejected by the quality gate");
        var ex = assertThrows(IllegalArgumentException.class, () -> importGraph("unit-test-error-alias"),
                "the graph model itself must reject the reserved alias");
        assertTrue(ex.getMessage().contains("reserved name"),
                "the error must teach the reservation: " + ex.getMessage());
        // the valid fixtures of the same feature family pass the gate
        assertTrue(CompiledGraphs.graphExists("unit-test-error-context"));
        assertTrue(CompiledGraphs.graphExists("unit-test-orchestrator"));
        assertTrue(CompiledGraphs.graphExists("unit-test-sub-suspend"));
    }

    @Test
    void manifestLocationDefaultsToClasspathGraph() {
        // the engine's test manifest declares no 'location' - the CompileFlows-style
        // default applies (the playground example app's manifest sets it explicitly)
        assertEquals("classpath:/graph", CompiledGraphs.getDeployedLocation());
    }

    @Test
    void staticValidatorRejectsEveryInvalidSuspendResumeShape() {
        // Direct coverage of every static rule, independent of the manifest. The fixtures cover
        // err1 a graph.suspend node not named 'suspend', err2 the suspend marker on graph.math,
        // err3 a suspensible node without a suspend node, err4 a suspend node without ttl,
        // err5 a suspensible node without a drawn edge to 'suspend', err6 a suspend node
        // without an outgoing connection, and err7 a suspension point without a
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
        // The err fixtures cover err1 a ttl on a skill without child-call deadline semantics
        // (graph.math), err2 a malformed duration on a deadline skill, err3 a data mapping
        // writing to reserved model metadata (model.ttl), and err4 the same metadata write
        // embedded as a MAPPING line inside a graph.math statement
        for (var id : List.of("unit-test-ttl-err1", "unit-test-ttl-err2", "unit-test-ttl-err3",
                              "unit-test-ttl-err4")) {
            var graph = importGraph(id);
            assertThrows(IllegalArgumentException.class, () -> GraphModelValidator.validate(graph),
                    id + " must fail the static validator");
        }
    }

    @Test
    void aTaskRouteAndItsSkillMustAgree() {
        // The pairing is bidirectional: a 'task' route is called ONLY by graph.task, graph.suspend
        // and graph.resume (the latter two are supersets whose task names the state-store function),
        // and each of those three requires one. Either half broken leaves an inert node - the graph
        // traverses it, nothing executes, and nothing is reported. That cost a debugging round in
        // the E0 round, which is why this is a hard error rather than a gate warning.
        //
        // err1 a task with no skill at all, err2 a task claimed by a skill that never reads one
        // (graph.math), err3 graph.task with no task route - the same defect from the other side.
        // The message is asserted, not just the throw: a malformed fixture would throw for an
        // unrelated reason and still satisfy assertThrows.
        var expected = Map.of("unit-test-task-skill-err1", "but no 'skill'",
                              "unit-test-task-skill-err2", "does not call a task",
                              "unit-test-task-skill-err3", "but has no 'task'");
        for (var entry : expected.entrySet()) {
            var graph = importGraph(entry.getKey());
            var ex = assertThrows(IllegalArgumentException.class,
                    () -> GraphModelValidator.validate(graph),
                    entry.getKey() + " must fail the static validator");
            assertTrue(ex.getMessage().contains(entry.getValue()),
                    entry.getKey() + " must fail on the task/skill pairing, not incidentally - got: "
                            + ex.getMessage());
        }
    }

    @Test
    void descriptorNodesMayCarryInputOutputWithoutASkill() {
        // Guards the narrowing: 'input'/'output' are NOT the signal. A Provider node uses 'input'
        // for its HTTP request shape and a Dictionary node uses 'output' for its projection, and
        // neither carries a skill by design - an earlier draft keyed on those properties and
        // rejected these valid tutorial models.
        var graph = importGraph("tutorial-113");
        assertDoesNotThrow(() -> GraphModelValidator.validate(graph),
                "Provider/Dictionary nodes carry input/output with no skill and must stay valid");
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
