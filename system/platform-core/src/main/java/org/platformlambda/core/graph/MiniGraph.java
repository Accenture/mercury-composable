/*

    Copyright 2018-2025 Accenture Technology

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

package org.platformlambda.core.graph;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.models.SimpleConnection;
import org.platformlambda.core.models.SimpleRelationship;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MiniGraph {
    private static final Logger log = LoggerFactory.getLogger(MiniGraph.class);
    private static final Utility util = Utility.getInstance();
    private final String graphId = util.getUuid();
    private final ConcurrentMap<String, SimpleNode> nodesByAlias = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SimpleNode> nodesById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SimpleConnection> connections = new ConcurrentHashMap<>();
    private final MutableGraph<String> graph = GraphBuilder.directed().allowsSelfLoops(false).build();
    private final AtomicInteger nodeCount = new AtomicInteger();
    private final int maxNodes;

    /**
     * Create a mini-graph instance with default maximum of 500 nodes
     * <p>
     * Mini-graph is an in-memory minimalist graph designed to handle a small number of nodes very efficiently.
     */
    public MiniGraph() {
        this(500);
    }

    /**
     * Create a mini-graph instance and set the max number of nodes.
     * <p>
     * Mini-graph is an in-memory minimalist graph designed to handle a small number of nodes.
     * Therefore, please be conservative when setting the max number of nodes to avoid running out of memory.
     * Please note that performance decreases when the number of nodes increases.
     *
     * @param maxNodes to be set
     */
    public MiniGraph(int maxNodes) {
        this.maxNodes = maxNodes;
    }

    /**
     * Obtain the unique ID of this mini-graph
     * <p>
     * A typical use case of mini-graph is to provide a memory based property graph for processing across
     * multiple tasks in an event flow instance. Therefore, when you retain reference of a mini-graph in
     * a state machine of an event flow instance, memory of the mini-graph will be released by the JVM's
     * GC process.
     * <p>
     * Important: if you retain a number of mini-graphs outside the context of an event flow instance
     * and forget to "de-reference" them, the consumed memory will not be released, thus resulting in
     * memory leak.
     *
     * @return unique ID of the mini-graph
     */
    public String getId() {
        return graphId;
    }

    /**
     * Create a node with alias and type
     *
     * @param alias of the node
     * @param type of the node
     * @return simple node
     */
    public SimpleNode createNode(String alias, String type) {
        if (alias == null || alias.isEmpty()) {
            throw new IllegalArgumentException("alias must not be empty");
        }
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("type must not be empty");
        }
        if (nodesByAlias.containsKey(alias)) {
            throw new IllegalArgumentException("alias "+alias+" already exists");
        }
        if (nodeCount.get() > maxNodes) {
            throw new IllegalArgumentException("max number of nodes is "+maxNodes);
        }
        int count = nodeCount.incrementAndGet();
        var aliasLower = alias.toLowerCase();
        var cid = util.getUuid();
        graph.addNode(cid);
        var node = new SimpleNode(cid, alias, type);
        nodesByAlias.put(aliasLower, node);
        nodesById.put(cid, node);
        log.debug("Created {} node-{} as {}", type, count, alias);
        return node;
    }

    /**
     * Remove a node with a given alias
     *
     * @param alias of the node
     */
    public void removeNode(String alias) {
        if (alias == null || alias.isEmpty()) {
            throw new IllegalArgumentException("alias must not be empty");
        }
        var node = findNodeByAlias(alias);
        if (node != null) {
            removeConnectionsFromNode(alias, true);
            removeConnectionsFromNode(alias, false);
            graph.removeNode(node.getId());
            nodesById.remove(node.getId());
            nodesByAlias.remove(alias);
            log.debug("Removed {}", alias);
        }
    }

    /**
     * Remove connections from a node
     *
     * @param alias of the node
     * @param forward or backward search
     */
    private void removeConnectionsFromNode(String alias, boolean forward) {
        var text = forward? "forward" : "backward";
        var nodes = forward? getForwardLinks(alias) : getBackwardLinks(alias);
        if (!nodes.isEmpty()) {
            log.debug("Removing {} {} connection{}", nodes.size(), text, nodes.size() == 1? "":"s");
            for (var n: nodes) {
                if (forward) {
                    removeConnection(alias, n.getAlias());
                } else {
                    removeConnection(n.getAlias(), alias);
                }
            }
        }
    }

    /**
     * List all nodes
     *
     * @return all nodes
     */
    public List<SimpleNode> getNodes() {
        return new ArrayList<>(nodesById.values());
    }

    /**
     * List all connections
     *
     * @return all relationships
     */
    public List<SimpleConnection> getConnections() {
        return new ArrayList<>(connections.values());
    }

    /**
     * Clear the mini-graph instance and de-reference any node and connection objects associated with it.
     */
    public void reset() {
        var connectionList = getConnections();
        for (var c: connectionList) {
            removeConnection(c.getSource().getAlias(), c.getTarget().getAlias());
        }
        var nodes = getNodes();
        for (var n: nodes) {
            removeNode(n.getAlias());
        }
    }

    /**
     * Remove a connection between two nodes
     *
     * @param sourceAlias of a node
     * @param targetAlias of another node
     */
    public void removeConnection(String sourceAlias, String targetAlias) {
        if (sourceAlias == null) {
            throw new IllegalArgumentException("source alias cannot be null");
        }
        if (targetAlias == null) {
            throw new IllegalArgumentException("target alias cannot be null");
        }
        if (sourceAlias.equalsIgnoreCase(targetAlias)) {
            throw new IllegalArgumentException("source and target aliases cannot be the same");
        }
        SimpleNode source = findNodeByAlias(sourceAlias);
        if (source == null) {
            throw new IllegalArgumentException("source node does not exist");
        }
        SimpleNode target = findNodeByAlias(targetAlias);
        if (target == null) {
            throw new IllegalArgumentException("target node does not exist");
        }
        var key = getRelationshipPair(source.getId(), target.getId());
        if (connections.containsKey(key)) {
            graph.removeEdge(source.getId(), target.getId());
            connections.remove(key);
            log.debug("Removed connection {} to {}", source.getAlias(), target.getAlias());
        }
    }

    /**
     * Find a node by alias
     *
     * @param alias of a node
     * @return a node if found. null if not found.
     */
    public SimpleNode findNodeByAlias(String alias) {
        if (alias == null) {
            throw new IllegalArgumentException("alias cannot be null");
        }
        return nodesByAlias.get(alias.toLowerCase());
    }

    /**
     * Find a node by ID
     *
     * @param id of a node
     * @return a node if found. null if not found.
     */
    public SimpleNode findNodeById(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        return nodesById.get(id);
    }

    /**
     * Find a list of nodes by type
     *
     * @param type of the nodes
     * @return a list of nodes
     */
    public List<SimpleNode> findNodesByType(String type) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("type cannot be empty");
        }
        List<SimpleNode> result = new ArrayList<>();
        for (var node: nodesById.values()) {
            for (var t: node.getTypes()) {
                if (t.equalsIgnoreCase(type)) {
                    result.add(node);
                }
            }
        }
        return result;
    }

    public List<SimpleRelationship> findRelationByType(String type) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("type cannot be empty");
        }
        List<SimpleRelationship> result = new ArrayList<>();
        for (var conn: connections.values()) {
            var relation = conn.getRelation(type);
            if (relation != null) {
                result.add(relation);
            }
        }
        return result;
    }

    /**
     * Find a list of nodes by key-value
     *
     * @param key of a property
     * @param value of a property
     * @return a list of nodes
     */
    public List<SimpleNode> findNodesByProperty(String key, Object value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be empty");
        }
        List<SimpleNode> result = new ArrayList<>();
        for (var node: nodesById.values()) {
            var properties = node.getProperties();
            for (var entry: properties.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key) && entry.getValue().equals(value)) {
                    result.add(node);
                }
            }
        }
        return result;
    }

    /**
     * Create a connection between two nodes
     *
     * @param sourceAlias of a node
     * @param targetAlias of another node
     * @return a connection
     */
    public SimpleConnection connect(String sourceAlias, String targetAlias) {
        if (sourceAlias == null) {
            throw new IllegalArgumentException("source alias cannot be null");
        }
        if (targetAlias == null) {
            throw new IllegalArgumentException("target alias cannot be null");
        }
        if (sourceAlias.equalsIgnoreCase(targetAlias)) {
            throw new IllegalArgumentException("source and target aliases cannot be the same");
        }
        SimpleNode source = findNodeByAlias(sourceAlias);
        if (source == null) {
            throw new IllegalArgumentException("source node does not exist");
        }
        SimpleNode target = findNodeByAlias(targetAlias);
        if (target == null) {
            throw new IllegalArgumentException("target node does not exist");
        }
        var key = getRelationshipPair(source.getId(), target.getId());
        if (connections.containsKey(key)) {
            return connections.get(key);
        }
        var cid = util.getUuid();
        SimpleConnection connection = new SimpleConnection(cid, source, target);
        connections.put(key, connection);
        graph.putEdge(source.getId(), target.getId());
        log.debug("Created connection {} to {}", sourceAlias, targetAlias);
        return connection;
    }

    /**
     * Find a connection between two nodes where a connection contains relationships if any.
     * Each relationship has type and optional properties.
     *
     * @param sourceAlias of a node
     * @param targetAlias of another node
     * @return a connection
     */
    public SimpleConnection findConnectionByAlias(String sourceAlias, String targetAlias) {
        if (sourceAlias == null) {
            throw new IllegalArgumentException("source alias cannot be null");
        }
        if (targetAlias == null) {
            throw new IllegalArgumentException("target alias cannot be null");
        }
        if (sourceAlias.equalsIgnoreCase(targetAlias)) {
            throw new IllegalArgumentException("source and target aliases cannot be the same");
        }
        SimpleNode source = findNodeByAlias(sourceAlias);
        if (source == null) {
            throw new IllegalArgumentException("source node does not exist");
        }
        SimpleNode target = findNodeByAlias(targetAlias);
        if (target == null) {
            throw new IllegalArgumentException("target node does not exist");
        }
        return connections.get(getRelationshipPair(source.getId(), target.getId()));
    }

    /**
     * Construct a composite key for a connection
     *
     * @param sourceId of a node
     * @param targetId of another node
     * @return composite key
     */
    private String getRelationshipPair(String sourceId, String targetId) {
        return sourceId + ":" + targetId;
    }

    /**
     * Get a list of nodes connected with a node regardless of direction
     *
     * @param alias of a node
     * @return neighbor nodes
     */
    public List<SimpleNode> getNeighbors(String alias) {
        SimpleNode node = findNodeByAlias(alias);
        if (node == null) {
            throw new IllegalArgumentException("node does not exist");
        }
        List<SimpleNode> result = new ArrayList<>();
        Set<String> nodes = graph.adjacentNodes(node.getId());
        for (var n: nodes) {
            SimpleNode neighbor = findNodeById(n);
            result.add(neighbor);
        }
        return result;
    }

    /**
     * Get a list of nodes connected with a node using forward links
     *
     * @param alias of a node
     * @return neighbor nodes
     */
    public List<SimpleNode> getForwardLinks(String alias) {
        SimpleNode node = findNodeByAlias(alias);
        if (node == null) {
            throw new IllegalArgumentException("node does not exist");
        }
        List<SimpleNode> result = new ArrayList<>();
        Set<String> nodes = graph.successors(node.getId());
        for (var n: nodes) {
            SimpleNode neighbor = findNodeById(n);
            result.add(neighbor);
        }
        return result;
    }

    /**
     * Get a list of nodes connected with a node using backward links
     *
     * @param alias of a node
     * @return neighbor nodes
     */
    public List<SimpleNode> getBackwardLinks(String alias) {
        SimpleNode node = findNodeByAlias(alias);
        if (node == null) {
            throw new IllegalArgumentException("node does not exist");
        }
        List<SimpleNode> result = new ArrayList<>();
        Set<String> nodes = graph.predecessors(node.getId());
        for (var n: nodes) {
            SimpleNode neighbor = findNodeById(n);
            result.add(neighbor);
        }
        return result;
    }

    /**
     * Discover the paths originated from a node
     *
     * @param alias of a node
     * @return list of paths
     */
    public List<List<String>> findPaths(String alias) {
        SimpleNode node = findNodeByAlias(alias);
        if (node == null) {
            throw new IllegalArgumentException("node does not exist");
        }
        List<List<String>> result = new ArrayList<>();
        Map<Integer, List<String>> levelList = new HashMap<>();
        Map<String, Integer> distances = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        for (String id : graph.nodes()) {
            distances.put(id, -1);
        }
        distances.put(node.getId(), 0);
        queue.add(node.getId());
        while (!queue.isEmpty()) {
            String u = queue.poll();
            for (String v : graph.adjacentNodes(u)) {
                // only process it when it is not visited before
                if (distances.get(v) == -1) {
                    distances.put(v, distances.get(u) + 1);
                    queue.add(v);
                }
            }
        }
        for (var entry: distances.entrySet()) {
            var level = entry.getValue();
            var id = entry.getKey();
            var nodeAlias = findNodeById(id).getAlias();
            List<String> list = levelList.containsKey(level)? levelList.get(level) : new ArrayList<>();
            list.add(nodeAlias);
            levelList.put(level, list);
        }
        var levels = new ArrayList<>(levelList.keySet());
        Collections.sort(levels);
        for (var n: levels) {
            // skip orphans
            if (n != -1) {
                result.add(levelList.get(n));
            }
        }
        return result;
    }
}
