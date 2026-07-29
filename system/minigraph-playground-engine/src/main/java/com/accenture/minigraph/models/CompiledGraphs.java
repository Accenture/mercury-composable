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
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory registry of graph models that have been validated and converted at startup by CompileGraph.
 * <p>
 * This mirrors the role of {@code com.accenture.models.Flows} in event-script-engine: a graph model
 * registered here has already been structurally validated (via MiniGraph.importGraph) and had its data
 * mapping entries converted from the deprecated "simple type matching" syntax to "simple plugin" syntax.
 * GraphExecutor serves deployed graph execution EXCLUSIVELY from this registry: a deployed graph
 * model is executable only when it is listed in the graph manifest (graph.model.automation) AND
 * passed the CompileGraph quality gate. A graph ID that is not here answers HTTP-404 as if the
 * model does not exist - the CompileFlows precedent, where an invalid flow never becomes
 * executable. There is no lazy loading of deployed models. (The playground's dry-run workspace
 * is a separate surface and is not affected.)
 */
public class CompiledGraphs {
    private static final ConcurrentMap<String, Map<String, Object>> COMPILED_GRAPHS = new ConcurrentHashMap<>();
    private static final AtomicReference<String> DEPLOYED_LOCATION = new AtomicReference<>("classpath:/graph");

    private CompiledGraphs() {}

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param graphId of a compiled graph model
     * @return the compiled graph model, or null if not compiled at startup
     */
    public static Map<String, Object> getGraph(String graphId) {
        return COMPILED_GRAPHS.get(graphId);
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param graphId of a graph model
     * @return true if the graph model was compiled at startup
     */
    public static boolean graphExists(String graphId) {
        return COMPILED_GRAPHS.containsKey(graphId);
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param graphId of a graph model
     * @param model the validated and converted graph model
     */
    public static void addGraph(String graphId, Map<String, Object> model) {
        COMPILED_GRAPHS.put(graphId, model);
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     * <p>
     * Set by CompileGraph from the graph manifest's 'location' entry
     * (default classpath:/graph - the CompileFlows convention).
     *
     * @param location of the deployed graph models (file:/ or classpath:/)
     */
    public static void setDeployedLocation(String location) {
        DEPLOYED_LOCATION.set(location);
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return the deployed graph model location
     */
    public static String getDeployedLocation() {
        return DEPLOYED_LOCATION.get();
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return all compiled graph IDs
     */
    public static List<String> getAllGraphs() {
        return new ArrayList<>(COMPILED_GRAPHS.keySet());
    }
}
