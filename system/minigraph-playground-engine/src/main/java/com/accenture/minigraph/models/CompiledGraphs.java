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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The compiled-graph registry: every graph model that is listed in a graph manifest
 * (graph.model.automation) AND passed the CompileGraph quality gate. Deployed execution is
 * served exclusively from this registry - a graph that failed the gate, or is not listed,
 * answers 404 as if it does not exist (ADR-0011).
 * <p>
 * Since 4.12.19 the manifest property may name several manifests (comma-separated, the
 * yaml.flow.automation convention), each carrying its own 'location'. The registry therefore
 * keeps the deployed locations in manifest order and, per graph, the location its compiled
 * copy came from. When two manifests list the same graph id the LATER manifest owns the id:
 * its copy replaces the earlier one, and if that copy is rejected the id is not executable -
 * the operator's latest intent wins, the way a later configuration source overrides an
 * earlier one.
 */
public class CompiledGraphs {
    private static final String DEFAULT_LOCATION = "classpath:/graph";
    private static final ConcurrentMap<String, Map<String, Object>> COMPILED_GRAPHS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> GRAPH_LOCATIONS = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<String> DEPLOYED_LOCATIONS = new CopyOnWriteArrayList<>();

    private CompiledGraphs() {}

    /**
     * Get a compiled graph model
     *
     * @param graphId of the deployed graph
     * @return the model, or null when the graph is not compiled (not listed or rejected)
     */
    public static Map<String, Object> getGraph(String graphId) {
        return COMPILED_GRAPHS.get(graphId);
    }

    /**
     * Check if a graph is compiled and therefore executable
     *
     * @param graphId of the deployed graph
     * @return true when the compiled registry holds the graph
     */
    public static boolean graphExists(String graphId) {
        return COMPILED_GRAPHS.containsKey(graphId);
    }

    /**
     * Register a compiled graph model from the default location
     *
     * @param graphId of the deployed graph
     * @param model the compiled graph model
     */
    public static void addGraph(String graphId, Map<String, Object> model) {
        addGraph(graphId, model, DEFAULT_LOCATION);
    }

    /**
     * Register a compiled graph model
     *
     * @param graphId of the deployed graph
     * @param model the compiled graph model
     * @param location the deployed location (file:/ or classpath:/) the model was compiled from
     */
    public static void addGraph(String graphId, Map<String, Object> model, String location) {
        COMPILED_GRAPHS.put(graphId, model);
        GRAPH_LOCATIONS.put(graphId, location);
    }

    /**
     * Drop a compiled graph model - a later manifest takes ownership of the id
     *
     * @param graphId of the deployed graph
     * @return the removed model, or null when the graph was not compiled
     */
    public static Map<String, Object> removeGraph(String graphId) {
        GRAPH_LOCATIONS.remove(graphId);
        return COMPILED_GRAPHS.remove(graphId);
    }

    /**
     * The deployed location a compiled graph came from
     *
     * @param graphId of the deployed graph
     * @return the location (file:/ or classpath:/), or null when the graph is not compiled
     */
    public static String getGraphLocation(String graphId) {
        return GRAPH_LOCATIONS.get(graphId);
    }

    /**
     * Replace the deployed locations with a single one (the single-manifest form)
     *
     * @param location the deployed graph model folder (file:/ or classpath:/)
     */
    public static void setDeployedLocation(String location) {
        DEPLOYED_LOCATIONS.clear();
        DEPLOYED_LOCATIONS.add(location);
    }

    /**
     * Append a manifest's deployed location (manifest order; a repeated location is kept once)
     *
     * @param location the deployed graph model folder (file:/ or classpath:/)
     */
    public static void addDeployedLocation(String location) {
        DEPLOYED_LOCATIONS.addIfAbsent(location);
    }

    /**
     * The primary deployed location - the first manifest's folder (the bundled one in the
     * common case), or the default when no manifest is configured
     *
     * @return the deployed graph model folder
     */
    public static String getDeployedLocation() {
        return DEPLOYED_LOCATIONS.isEmpty()? DEFAULT_LOCATION : DEPLOYED_LOCATIONS.getFirst();
    }

    /**
     * Every deployed location in manifest order
     *
     * @return the deployed graph model folders (the default alone when no manifest is configured)
     */
    public static List<String> getDeployedLocations() {
        return DEPLOYED_LOCATIONS.isEmpty()? List.of(DEFAULT_LOCATION) : List.copyOf(DEPLOYED_LOCATIONS);
    }

    /**
     * Get the IDs of all compiled graphs
     *
     * @return list of graph IDs
     */
    public static List<String> getAllGraphs() {
        return new ArrayList<>(COMPILED_GRAPHS.keySet());
    }
}
