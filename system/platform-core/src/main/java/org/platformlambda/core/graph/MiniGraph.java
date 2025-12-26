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

package org.platformlambda.core.graph;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.models.SimpleConnection;
import org.platformlambda.core.models.SimpleRelationship;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MiniGraph {
    private static final Logger log = LoggerFactory.getLogger(MiniGraph.class);
    private static final String NODE_PREFIX = "nodes[";
    private static final String CONNECTION_PREFIX = "connections[";
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
     * Print the graph as a JSON string
     *
     * @return json text
     */
    public String toString() {
        var map = exportGraph();
        return SimpleMapper.getInstance().getMapper().writeValueAsString(map);
    }

    /**
     * Export graph as a map of key-values
     *
     * @return map
     */
    public Map<String, Object> exportGraph() {
        var allNodes = getNodes();
        var allConnections = getConnections();
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> nodeList = new ArrayList<>();
        result.put("nodes", nodeList);
        List<Map<String, Object>> connectionList = new ArrayList<>();
        result.put("connections", connectionList);
        Map<String, Map<String, Object>> nodeMap = new HashMap<>();
        for (var node: allNodes) {
            Map<String, Object> nEntry = new HashMap<>();
            nEntry.put("alias", node.getAlias());
            nEntry.put("types", new ArrayList<>(node.getTypes()));
            nEntry.put("properties", util.deepCopy(node.getProperties()));
            nodeMap.put(node.getAlias(), nEntry);
        }
        var nl = new ArrayList<>(nodeMap.keySet());
        Collections.sort(nl);
        for (var n : nl) {
            nodeList.add(nodeMap.get(n));
        }
        Map<String, Map<String, Object>> connectionMap = new HashMap<>();
        for (var conn: allConnections) {
            Map<String, Object> cEntry = new HashMap<>();
            cEntry.put("source", conn.getSource().getAlias());
            cEntry.put("target", conn.getTarget().getAlias());
            List<Map<String, Object>> relationList = new ArrayList<>();
            cEntry.put("relations", relationList);
            Map<String, Map<String, Object>> relationMap = new HashMap<>();
            for (var relation: conn.getRelations()) {
                Map<String, Object> inner = new HashMap<>();
                inner.put("type", relation.getType());
                inner.put("properties", util.deepCopy(relation.getProperties()));
                relationMap.put(relation.getType(), inner);
            }
            var rl = new ArrayList<>(relationMap.keySet());
            Collections.sort(rl);
            for (var r : rl) {
                relationList.add(relationMap.get(r));
            }
            connectionMap.put(getRelationshipPair(conn.getSource().getAlias(), conn.getTarget().getAlias()), cEntry);
        }
        var cl = new ArrayList<>(connectionMap.keySet());
        Collections.sort(cl);
        for (var c : cl) {
            connectionList.add(connectionMap.get(c));
        }
        return result;
    }

    /**
     * Import a map of key-values that represents a graph
     *
     * @param map of key-values
     */
    public void importGraph(Map<String, Object> map) {
        try {
            reset();
            importNodesAndConnections(map);
        } catch (IllegalArgumentException e) {
            reset();
            throw e;
        }
    }

    private void importNodesAndConnections(Map<String, Object> map) {
        var nodeList = map.get("nodes");
        var connectionList = map.get("connections");
        if (nodeList instanceof List<?> nl && connectionList instanceof List<?> cl) {
            MultiLevelMap mm = new MultiLevelMap(map);
            importNodes(mm, nl.size());
            importConnections(mm, cl.size());
        } else {
            throw new IllegalArgumentException("type of nodes and connections must be List of key-value maps");
        }
    }

    private void importNodes(MultiLevelMap mm, int len) {
        for (int i=0; i < len; i++) {
            var alias = mm.getElement(NODE_PREFIX+i+"].alias");
            if (alias == null) {
                throw new IllegalArgumentException("missing alias in node entry-"+(i+1));
            }
            var types = mm.getElement(NODE_PREFIX+i+"].types");
            if (types instanceof List<?> tl && !tl.isEmpty()) {
                var node = createNode(String.valueOf(alias), String.valueOf(tl.getFirst()));
                if (tl.size() > 1) {
                    for (int j=1; j < tl.size(); j++) {
                        node.addType(String.valueOf(tl.get(j)));
                    }
                }
                importNodeProperties(mm, node, i);
            } else {
                throw new IllegalArgumentException("invalid types in node entry-"+(i+1));
            }
        }
    }

    private void importNodeProperties(MultiLevelMap mm, SimpleNode node, int i) {
        var properties = mm.getElement(NODE_PREFIX+i+"].properties");
        if (properties instanceof Map<?, ?> propMap) {
            for (var entry : propMap.entrySet()) {
                node.addProperty(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
    }

    private void importConnections(MultiLevelMap mm, int len) {
        for (int i=0; i < len; i++) {
            var source = mm.getElement(CONNECTION_PREFIX+i+"].source");
            var target = mm.getElement(CONNECTION_PREFIX+i+"].target");
            if (source == null || target == null) {
                throw new IllegalArgumentException("invalid source/target alias in connection entry-"+(i+1));
            }
            var sourceAlias = String.valueOf(source);
            var targetAlias = String.valueOf(target);
            var conn = connect(sourceAlias, targetAlias);
            var relations = mm.getElement(CONNECTION_PREFIX+i+"].relations");
            if (relations instanceof List<?> list) {
                for (int j=0; j < list.size(); j++) {
                    importRelations(mm, conn, i, j);
                }
            }
        }
    }

    private void importRelations(MultiLevelMap mm, SimpleConnection conn, int i, int j) {
        var relationType = mm.getElement(CONNECTION_PREFIX+i+"].relations["+j+"].type");
        var properties = mm.getElement(CONNECTION_PREFIX+i+"].relations["+j+"].properties");
        if (relationType instanceof String rt) {
            var relation = conn.addRelation(rt);
            if (properties instanceof Map<?, ?> propMap) {
                for (var entry : propMap.entrySet()) {
                    relation.addProperty(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }
    }

    public boolean sameAs(MiniGraph that) {
        return sameNodes(that) && sameConnections(that);
    }

    private boolean sameNodes(MiniGraph that) {
        var nodes1 = this.getNodes();
        var nodes2 = that.getNodes();
        if (nodes1.size() != nodes2.size()) {
            return false;
        }
        for (var n1: nodes1) {
            var n2 = that.findNodeByAlias(n1.getAlias());
            if (n2 == null) {
                return false;
            }
            if (!n1.getTypes().equals(n2.getTypes())) {
                return false;
            }
            if (differentProperties(n1.getProperties(), n2.getProperties())) {
                return false;
            }
        }
        return true;
    }

    private boolean sameConnections(MiniGraph that) {
        var conn1 = this.getConnections();
        var conn2 = that.getConnections();
        if (conn1.size() != conn2.size()) {
            return false;
        }
        for (var c1: conn1) {
            var c2 = that.findConnection(c1.getSource().getAlias(), c1.getTarget().getAlias());
            if (c2 == null) {
                return false;
            }
            var relations1 = c1.getRelations();
            var relations2 = c2.getRelations();
            if (relations1.size() != relations2.size()) {
                return false;
            }
            for (var r1: relations1) {
                var r2 = c2.getRelation(r1.getType());
                if (r2 == null) {
                    return false;
                }
                if (differentProperties(r1.getProperties(), r2.getProperties())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean differentProperties(Map<String, Object> map1, Map<String, Object> map2) {
        var m1 = util.getFlatMap(map1);
        var m2 = util.getFlatMap(map2);
        if (m1.size() != m2.size()) {
            return true;
        }
        var k1 = new ArrayList<>(m1.keySet());
        var k2 = new ArrayList<>(m2.keySet());
        Collections.sort(k1);
        Collections.sort(k2);
        if (!k1.equals(k2)) {
            return true;
        }
        for (var k: k1) {
            var v1 = m1.get(k);
            var v2 = m2.get(k);
            if (!v1.equals(v2)) {
                return true;
            }
        }
        return false;
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
        log.debug("Created {} as {}, total={}", type, alias, count);
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
            var count = nodeCount.decrementAndGet();
            log.debug("Removed {}, total={}", alias, count);
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
        nodeCount.set(0);
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
    public SimpleConnection findConnection(String sourceAlias, String targetAlias) {
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
     * Find bidirectional connections between two nodes where a connection contains relationships if any.
     * Each relationship has type and optional properties.
     *
     * @param sourceAlias of a node
     * @param targetAlias of another node
     * @return 0 to 2 connections
     */
    public List<SimpleConnection> findBiDirectionalConnection(String sourceAlias, String targetAlias) {
        List<SimpleConnection> bothDirection = new ArrayList<>();
        var forward = findConnection(sourceAlias, targetAlias);
        if (forward != null) {
            bothDirection.add(forward);
        }
        var backward = findConnection(targetAlias, sourceAlias);
        if (backward != null) {
            bothDirection.add(backward);
        }
        return bothDirection;
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
