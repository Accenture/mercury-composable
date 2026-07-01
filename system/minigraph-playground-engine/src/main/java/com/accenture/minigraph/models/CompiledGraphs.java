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

package com.accenture.minigraph.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory registry of graph models that have been validated and converted at startup by CompileGraph.
 * <p>
 * This mirrors the role of {@code com.accenture.models.Flows} in event-script-engine: a graph model
 * registered here has already been structurally validated (via MiniGraph.importGraph) and had its data
 * mapping entries converted from the deprecated "simple type matching" syntax to "simple plugin" syntax.
 * GraphExecutor consults this registry first and falls back to lazy, per-request loading for any
 * graph ID that was not declared in the graph manifest.
 */
public class CompiledGraphs {
    private static final ConcurrentMap<String, Map<String, Object>> compiledGraphs = new ConcurrentHashMap<>();

    private CompiledGraphs() {}

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param graphId of a compiled graph model
     * @return the compiled graph model, or null if not compiled at startup
     */
    public static Map<String, Object> getGraph(String graphId) {
        return compiledGraphs.get(graphId);
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param graphId of a graph model
     * @return true if the graph model was compiled at startup
     */
    public static boolean graphExists(String graphId) {
        return compiledGraphs.containsKey(graphId);
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param graphId of a graph model
     * @param model the validated and converted graph model
     */
    public static void addGraph(String graphId, Map<String, Object> model) {
        compiledGraphs.put(graphId, model);
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return all compiled graph IDs
     */
    public static List<String> getAllGraphs() {
        return new ArrayList<>(compiledGraphs.keySet());
    }
}
