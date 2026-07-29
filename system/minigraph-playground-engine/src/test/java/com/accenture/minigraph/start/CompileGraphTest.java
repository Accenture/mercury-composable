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

import com.accenture.minigraph.models.CompiledGraphs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
        // a graph ID that is not listed in graphs.yaml must not be compiled -
        // GraphExecutor falls back to lazy loading for it
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
    void invalidSuspendResumeGraphsAreRejectedAtCompileTime() {
        // err1 graph.suspend node not named 'suspend'; err2 suspend=true on graph.math;
        // err3 suspensible node without a suspend node; err4 suspend node without ttl;
        // err5 suspensible node without a drawn edge to 'suspend'; err6 resume 'missing'
        // target that does not exist - all six are listed in graphs.yaml and must be
        // rejected by the static checks (the runtime guards remain the enforcement floor)
        for (var id : List.of("unit-test-suspend-err1", "unit-test-suspend-err2", "unit-test-suspend-err3",
                              "unit-test-suspend-err4", "unit-test-suspend-err5", "unit-test-suspend-err6")) {
            assertFalse(CompiledGraphs.graphExists(id), id + " must be rejected at compile time");
        }
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
