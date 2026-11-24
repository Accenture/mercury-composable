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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;

/**
 * The compiled-graph registry: every graph model that is listed in a graph manifest
 * (graph.model.automation) AND passed the CompileGraph quality gate, or that a hot-deployed graph set
 * carried through the same gate (RFC-0017). Deployed execution is served exclusively from this
 * registry - a graph that failed the gate, or is not listed, answers 404 as if it does not exist
 * (ADR-0011).
 * <p>
 * Since 4.12.19 the manifest property may name several manifests (comma-separated, the
 * yaml.flow.automation convention), each carrying its own 'location'. The registry therefore
 * keeps the deployed locations in manifest order and, per graph, the location its compiled
 * copy came from. When two manifests list the same graph id the LATER manifest owns the id:
 * its copy replaces the earlier one, and if that copy is rejected the id is not executable -
 * the operator's latest intent wins, the way a later configuration source overrides an
 * earlier one.
 * <p>
 * The registry is one immutable snapshot behind an atomic reference. A read is a map lookup on the
 * current snapshot; a write builds the next snapshot and publishes it once, so a graph set that is
 * deployed, replaced or removed at run time appears or disappears as a unit and a run never sees half
 * of it. A refresh hook, installed by the hot-deployment loader when it is on, is consulted on every
 * lookup with the id and the location of the compiled copy (null for an unknown id); the hook may
 * reconcile the registry before the lookup is answered.
 */
public class CompiledGraphs {
    private static final String DEFAULT_LOCATION = "classpath:/graph";
    private static final AtomicReference<Snapshot> CURRENT = new AtomicReference<>(Snapshot.EMPTY);
    private static final AtomicReference<Refresh> REFRESH = new AtomicReference<>();
    // writers serialize their read-modify-write of the snapshot; readers never lock
    // (a ReentrantLock, not synchronized: a virtual thread must not pin its carrier)
    private static final ReentrantLock WRITE = new ReentrantLock();

    private CompiledGraphs() {}

    /**
     * The graph set a deployed graph came from (ADR-0027)
     *
     * @param name the set name, which is the package's file name without '.pack'
     * @param version the set's 'version' manifest field, empty when the set has none
     */
    public record DeployedSet(String name, String version) {
    }

    /**
     * A hook consulted on every lookup of a graph by id, before the lookup is answered.
     */
    @FunctionalInterface
    public interface Refresh {
        /**
         * Bring the registry up to date for a graph id, when the hook decides it should.
         *
         * @param graphId the id looked up
         * @param location the deployed location of the compiled copy, or null when the id is unknown
         */
        void check(String graphId, String location);
    }

    private record Entry(Map<String, Object> model, String location, DeployedSet set) {
    }

    private record Snapshot(Map<String, Entry> graphs, List<String> locations) {
        private static final Snapshot EMPTY = new Snapshot(Map.of(), List.of());

        private Snapshot withGraphs(Map<String, Entry> updated) {
            return new Snapshot(Map.copyOf(updated), locations);
        }
    }

    /**
     * Get a compiled graph model
     *
     * @param graphId of the deployed graph
     * @return the model, or null when the graph is not compiled (not listed or rejected)
     */
    public static Map<String, Object> getGraph(String graphId) {
        var entry = lookup(graphId);
        return entry == null? null : entry.model();
    }

    /**
     * Check if a graph is compiled and therefore executable
     *
     * @param graphId of the deployed graph
     * @return true when the compiled registry holds the graph
     */
    public static boolean graphExists(String graphId) {
        return lookup(graphId) != null;
    }

    private static Entry lookup(String graphId) {
        var entry = CURRENT.get().graphs().get(graphId);
        var refresh = REFRESH.get();
        if (refresh != null) {
            refresh.check(graphId, entry == null? null : entry.location());
            entry = CURRENT.get().graphs().get(graphId);
        }
        return entry;
    }

    /**
     * Register a compiled graph model
     *
     * @param graphId of the deployed graph
     * @param model the compiled graph model
     * @param location the deployed location (file:/ or classpath:/) the model was compiled from
     */
    public static void addGraph(String graphId, Map<String, Object> model, String location) {
        addGraph(graphId, model, location, null);
    }

    /**
     * Register a compiled graph model that came from a graph set
     *
     * @param graphId of the deployed graph
     * @param model the compiled graph model
     * @param location the deployed location (file:/ or classpath:/) the model was compiled from
     * @param set the graph set it came from, or null for a graph a manifest lists
     */
    public static void addGraph(String graphId, Map<String, Object> model, String location, DeployedSet set) {
        update(snapshot -> {
            var graphs = new HashMap<>(snapshot.graphs());
            graphs.put(graphId, new Entry(model, location, set));
            return snapshot.withGraphs(graphs);
        });
    }

    /**
     * Replace graphs in one swap (RFC-0017): the ids to remove leave and the models given register, and a
     * reader sees either the registry before or the registry after - never a mixture. A hot-deployed set
     * is deployed, replaced (its previous ids removed, its new graphs added) or undeployed (no models) this way.
     *
     * @param removeIds the graph ids to remove first
     * @param models the graph models to register, by id
     * @param location the deployed location the models were compiled from
     * @param set the graph set the models came from, or null
     */
    public static void replaceGraphs(Collection<String> removeIds, Map<String, Map<String, Object>> models,
                                     String location, DeployedSet set) {
        update(snapshot -> {
            var graphs = new HashMap<>(snapshot.graphs());
            removeIds.forEach(graphs::remove);
            models.forEach((id, model) -> graphs.put(id, new Entry(model, location, set)));
            return snapshot.withGraphs(graphs);
        });
    }

    /**
     * The graph set a compiled graph came from
     *
     * @param graphId of the deployed graph
     * @return the set, or null when a manifest lists the graph or the graph is not compiled
     */
    public static DeployedSet getGraphSet(String graphId) {
        var entry = CURRENT.get().graphs().get(graphId);
        return entry == null? null : entry.set();
    }

    /**
     * The graphs a set registered from a location holds in the registry
     *
     * @param setName the set's name
     * @param location the deployed location the set was compiled from
     * @return the graph ids, in no particular order
     */
    public static List<String> getGraphsOfSet(String setName, String location) {
        List<String> result = new ArrayList<>();
        for (var kv : CURRENT.get().graphs().entrySet()) {
            var set = kv.getValue().set();
            if (set != null && set.name().equals(setName) && location.equals(kv.getValue().location())) {
                result.add(kv.getKey());
            }
        }
        return result;
    }

    /**
     * Drop a compiled graph model - a later manifest takes ownership of the id
     *
     * @param graphId of the deployed graph
     */
    public static void removeGraph(String graphId) {
        update(snapshot -> {
            if (!snapshot.graphs().containsKey(graphId)) {
                return snapshot;
            }
            var graphs = new HashMap<>(snapshot.graphs());
            graphs.remove(graphId);
            return snapshot.withGraphs(graphs);
        });
    }

    /**
     * The deployed location a compiled graph came from
     *
     * @param graphId of the deployed graph
     * @return the location (file:/ or classpath:/), or null when the graph is not compiled
     */
    public static String getGraphLocation(String graphId) {
        var entry = CURRENT.get().graphs().get(graphId);
        return entry == null? null : entry.location();
    }

    /**
     * Append a manifest's deployed location (manifest order; a repeated location is kept once)
     *
     * @param location the deployed graph model folder (file:/ or classpath:/)
     */
    public static void addDeployedLocation(String location) {
        update(snapshot -> {
            if (snapshot.locations().contains(location)) {
                return snapshot;
            }
            var locations = new ArrayList<>(snapshot.locations());
            locations.add(location);
            return new Snapshot(snapshot.graphs(), List.copyOf(locations));
        });
    }

    /**
     * The primary deployed location - the first manifest's folder (the bundled one in the
     * common case), or the default when no manifest is configured
     *
     * @return the deployed graph model folder
     */
    public static String getDeployedLocation() {
        var locations = CURRENT.get().locations();
        return locations.isEmpty()? DEFAULT_LOCATION : locations.getFirst();
    }

    /**
     * Every deployed location in manifest order
     *
     * @return the deployed graph model folders (the default alone when no manifest is configured)
     */
    public static List<String> getDeployedLocations() {
        var locations = CURRENT.get().locations();
        return locations.isEmpty()? List.of(DEFAULT_LOCATION) : locations;
    }

    /**
     * Get the IDs of all compiled graphs
     *
     * @return list of graph IDs
     */
    public static List<String> getAllGraphs() {
        return new ArrayList<>(CURRENT.get().graphs().keySet());
    }

    /**
     * Install the refresh hook, or remove it with null. Reserved for the hot-deployment loader.
     *
     * @param refresh the hook consulted on every lookup by id
     */
    public static void setRefresh(Refresh refresh) {
        REFRESH.set(refresh);
    }

    private static void update(UnaryOperator<Snapshot> change) {
        WRITE.lock();
        try {
            CURRENT.set(change.apply(CURRENT.get()));
        } finally {
            WRITE.unlock();
        }
    }
}
