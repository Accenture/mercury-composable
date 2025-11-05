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

package org.platformlambda.core;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.models.SimpleNode;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GraphTest {

    @Test
    void nodeTest() {
        var graph = new MiniGraph();
        var nodeA = graph.createNode("A", "transaction");
        var nodeB = graph.createNode("B", "data");
        var nodeC = graph.createNode("C", "data");
        var nodeD = graph.createNode("D", "data");
        var nodeE = graph.createNode("E", "data");
        var r1 = graph.connect("A", "B");
        // connection between 2 nodes is unique. It will return the existing one.
        var r1a = graph.connect("A", "B");
        assertEquals(r1, r1a);
        var r2 = graph.connect("A", "C");
        compareRelation(graph);
        var r3 = graph.connect("C", "D");
        var r4 = graph.connect("D", "E");
        compareNodes(graph, nodeB, nodeC, nodeD, nodeE);
        var transactionNodes = graph.findNodesByType("transaction");
        assertTrue(transactionNodes.contains(nodeA));
        assertEquals(1, transactionNodes.size());
        var r01 = graph.findConnectionByAlias("A", "B");
        assertEquals(r1, r01);
        var r02 = graph.findConnectionByAlias("A", "C");
        assertEquals(r2, r02);
        var r03 = graph.findConnectionByAlias("C", "D");
        assertEquals(r3, r03);
        var r04 = graph.findConnectionByAlias("D", "E");
        assertEquals(r4, r04);
        nodeA.addType("hello");
        nodeA.addType("service");
        nodeA.addProperty("hello", "world");
        nodeA.addProperty("test", "message");
        var nodeAA = graph.findNodeById(nodeA.getId());
        assertEquals("world", nodeAA.getProperties().get("hello"));
        assertEquals("message", nodeAA.getProperties().get("test"));
        assertTrue(nodeAA.getTypes().contains("hello"));
        assertTrue(nodeAA.getTypes().contains("service"));
        nodeA.removeType("hello");
        nodeA.removeProperty("hello");
        assertNull(nodeAA.getProperties().get("hello"));
        assertFalse(nodeAA.getTypes().contains("hello"));
        var neighborsToA = graph.getNeighbors("A");
        var neighborsAliases = new ArrayList<String>();
        neighborsToA.forEach(n -> neighborsAliases.add(n.getAlias()));
        assertEquals(2, neighborsAliases.size());
        assertTrue(neighborsAliases.contains("B"));
        assertTrue(neighborsAliases.contains("C"));
        validatePaths1(graph);
        validateNodesThenRemoveConnection(graph);
        validatePaths2(graph);
        // add the connection again
        graph.connect("C", "D");
        // removing node-D will remove its forward and backward connections. node-E becomes an orphan.
        graph.removeNode("D");
        validatePath3(graph);
        graph.reset();
    }

    private void compareNodes(MiniGraph graph,SimpleNode nodeB, SimpleNode nodeC, SimpleNode nodeD, SimpleNode nodeE) {
        var dataNodes = graph.findNodesByType("data");
        assertEquals(4, dataNodes.size());
        assertTrue(dataNodes.contains(nodeB));
        assertTrue(dataNodes.contains(nodeC));
        assertTrue(dataNodes.contains(nodeD));
        assertTrue(dataNodes.contains(nodeE));
    }

    private void compareRelation(MiniGraph graph) {
        var r2 = graph.connect("A", "C");
        // create relation for a connection
        var r2relation = r2.addRelation("Demo");
        r2relation.addProperty("some", "relation");
        var r02relation = graph.findRelationByType("demo");
        assertEquals(1, r02relation.size());
        assertEquals(r2relation, r02relation.getFirst());
        var r2a = graph.findConnectionByAlias(r02relation.getFirst().getSourceAlias(), r02relation.getFirst().getTargetAlias());
        assertEquals(r2, r2a);
        var all = r2.getRelations();
        assertEquals(1, all.size());
        assertEquals(r02relation.getFirst(), all.getFirst());
    }

    private void validatePaths1(MiniGraph graph) {
        // [[A], [B, C], [D], [E]]
        var pathsFromA = graph.findPaths("A");
        assertEquals(4, pathsFromA.size());
        assertEquals(1, pathsFromA.getFirst().size());
        assertEquals(2, pathsFromA.get(1).size());
        assertEquals(1, pathsFromA.get(2).size());
        assertEquals(1, pathsFromA.get(3).size());
        assertEquals("A", pathsFromA.getFirst().getFirst());
        assertTrue(pathsFromA.get(1).contains("B"));
        assertTrue(pathsFromA.get(1).contains("C"));
        assertEquals("D", pathsFromA.get(2).getFirst());
        assertEquals("E", pathsFromA.get(3).getFirst());
    }

    private void validateNodesThenRemoveConnection(MiniGraph graph) {
        // get all nodes
        var nodes = graph.getNodes();
        assertEquals(5, nodes.size());
        assertTrue(nodes.contains(graph.findNodeByAlias("A")));
        assertTrue(nodes.contains(graph.findNodeByAlias("B")));
        assertTrue(nodes.contains(graph.findNodeByAlias("C")));
        assertTrue(nodes.contains(graph.findNodeByAlias("D")));
        assertTrue(nodes.contains(graph.findNodeByAlias("E")));
        var connections = graph.getConnections();
        assertEquals(4, connections.size());
        // now remove a connection
        graph.removeConnection("C", "D");
        var connections2 = graph.getConnections();
        assertEquals(3, connections2.size());
    }

    private void validatePaths2(MiniGraph graph) {
        // [[A], [B, C]] and [[E], [D]] are 2 disjointed graphs
        var pathsFromA2 = graph.findPaths("A");
        assertEquals(2, pathsFromA2.size());
        assertEquals(1, pathsFromA2.getFirst().size());
        assertEquals(2, pathsFromA2.get(1).size());
        assertEquals("A", pathsFromA2.getFirst().getFirst());
        assertTrue(pathsFromA2.get(1).contains("B"));
        assertTrue(pathsFromA2.get(1).contains("C"));
        // [[E], [D]]
        var pathsFromE = graph.findPaths("E");
        assertEquals(2, pathsFromE.size());
        assertEquals(1, pathsFromE.getFirst().size());
        assertEquals(1, pathsFromE.get(1).size());
        assertEquals("E", pathsFromE.getFirst().getFirst());
        assertEquals("D", pathsFromE.get(1).getFirst());
    }

    private void validatePath3(MiniGraph graph) {
        // [[E]]
        var pathsFromE2 = graph.findPaths("E");
        assertEquals(1, pathsFromE2.size());
        assertEquals(1, pathsFromE2.getFirst().size());
        assertEquals("E", pathsFromE2.getFirst().getFirst());
        // case-insensitive search
        var node1 = graph.findNodeByAlias("B");
        var node2 = graph.findNodeByAlias("b");
        assertEquals(node1, node2);
        assertNotNull(graph.findNodesByType("Transaction"));
        // find node by property
        var nodes = graph.findNodesByProperty("test", "message");
        assertEquals(1, nodes.size());
        assertEquals(Map.of("test", "message"), nodes.getFirst().getProperties());
        assertEquals(Set.of("transaction", "service"), nodes.getFirst().getTypes());
    }

    @Test
    void exceptionTest1() {
        var graph = new MiniGraph();
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                graph.createNode(null, "transaction"));
        assertEquals("alias must not be empty", ex1.getMessage());
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                graph.createNode("", "transaction"));
        assertEquals("alias must not be empty", ex2.getMessage());
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
                graph.createNode("test", null));
        assertEquals("type must not be empty", ex3.getMessage());
        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class, () ->
                graph.createNode("test", ""));
        assertEquals("type must not be empty", ex4.getMessage());
        graph.createNode("test", "transaction");
        IllegalArgumentException ex5 = assertThrows(IllegalArgumentException.class, () ->
                graph.createNode("test", "hello"));
        assertEquals("alias test already exists", ex5.getMessage());
        IllegalArgumentException ex6 = assertThrows(IllegalArgumentException.class, () ->
                graph.removeNode(null));
        assertEquals("alias must not be empty", ex6.getMessage());
        IllegalArgumentException ex7 = assertThrows(IllegalArgumentException.class, () ->
                graph.removeNode(""));
        assertEquals("alias must not be empty", ex7.getMessage());
    }

    @Test
    void exceptionTest2() {
        var graph = new MiniGraph();
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                graph.removeConnection(null, "B"));
        assertEquals("source alias cannot be null", ex1.getMessage());
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                graph.removeConnection("not found", "X"));
        assertEquals("source node does not exist", ex2.getMessage());
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
                graph.removeConnection("test", null));
        assertEquals("target alias cannot be null", ex3.getMessage());
        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class, () ->
                graph.removeConnection("A", "A"));
        assertEquals("source and target aliases cannot be the same", ex4.getMessage());
        graph.createNode("test", "transaction");
        IllegalArgumentException ex5 = assertThrows(IllegalArgumentException.class, () ->
                graph.removeConnection("test", "hello"));
        assertEquals("target node does not exist", ex5.getMessage());
    }

    @Test
    void exceptionTest3() {
        var graph = new MiniGraph();
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                graph.findNodeByAlias(null));
        assertEquals("alias cannot be null", ex1.getMessage());
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                graph.findNodeById(null));
        assertEquals("id cannot be null", ex2.getMessage());
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
                graph.findNodesByType(null));
        assertEquals("type cannot be empty", ex3.getMessage());
        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class, () ->
                graph.findNodesByProperty(null, "test"));
        assertEquals("key cannot be empty", ex4.getMessage());
    }

    @Test
    void exceptionTest4() {
        var graph = new MiniGraph();
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                graph.connect(null, "B"));
        assertEquals("source alias cannot be null", ex1.getMessage());
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                graph.connect("not found", "X"));
        assertEquals("source node does not exist", ex2.getMessage());
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
                graph.connect("test", null));
        assertEquals("target alias cannot be null", ex3.getMessage());
        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class, () ->
                graph.connect("A", "A"));
        assertEquals("source and target aliases cannot be the same", ex4.getMessage());
        graph.createNode("test", "transaction");
        IllegalArgumentException ex5 = assertThrows(IllegalArgumentException.class, () ->
                graph.connect("test", "hello"));
        assertEquals("target node does not exist", ex5.getMessage());
    }

    @Test
    void exceptionTest5() {
        var graph = new MiniGraph();
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                graph.findConnectionByAlias(null, "B"));
        assertEquals("source alias cannot be null", ex1.getMessage());
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                graph.findConnectionByAlias("not found", "X"));
        assertEquals("source node does not exist", ex2.getMessage());
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
                graph.findConnectionByAlias("test", null));
        assertEquals("target alias cannot be null", ex3.getMessage());
        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class, () ->
                graph.findConnectionByAlias("A", "A"));
        assertEquals("source and target aliases cannot be the same", ex4.getMessage());
        graph.createNode("test", "transaction");
        IllegalArgumentException ex5 = assertThrows(IllegalArgumentException.class, () ->
                graph.findConnectionByAlias("test", "hello"));
        assertEquals("target node does not exist", ex5.getMessage());
        IllegalArgumentException ex6 = assertThrows(IllegalArgumentException.class, () ->
                graph.getNeighbors(null));
        assertEquals("alias cannot be null", ex6.getMessage());
        IllegalArgumentException ex7 = assertThrows(IllegalArgumentException.class, () ->
                graph.getForwardLinks(null));
        assertEquals("alias cannot be null", ex7.getMessage());
        IllegalArgumentException ex8 = assertThrows(IllegalArgumentException.class, () ->
                graph.getBackwardLinks(null));
        assertEquals("alias cannot be null", ex8.getMessage());
        IllegalArgumentException ex9 = assertThrows(IllegalArgumentException.class, () ->
                graph.getNeighbors("not found"));
        assertEquals("node does not exist", ex9.getMessage());
        IllegalArgumentException ex10 = assertThrows(IllegalArgumentException.class, () ->
                graph.getForwardLinks("not found"));
        assertEquals("node does not exist", ex10.getMessage());
        IllegalArgumentException ex11 = assertThrows(IllegalArgumentException.class, () ->
                graph.getBackwardLinks("not found"));
        assertEquals("node does not exist", ex11.getMessage());
        IllegalArgumentException ex12 = assertThrows(IllegalArgumentException.class, () ->
                graph.findPaths("not found"));
        assertEquals("node does not exist", ex12.getMessage());
    }
}
