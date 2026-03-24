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

package com.accenture.minigraph.skills;

import com.accenture.minigraph.common.GraphLambdaFunction;
import com.accenture.minigraph.math.ExpressionEngine;
import com.accenture.minigraph.models.GraphInstance;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.models.EventEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@PreLoad(route = GraphMath.ROUTE, instances=300)
public class GraphMath extends GraphLambdaFunction {
    public static final String ROUTE = "graph.math";
    private static final ExpressionEngine engine = new ExpressionEngine();

    public GraphMath() {
        // suppress JavaScript engine warning
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
    }

    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        if (!EXECUTE.equals(headers.get(TYPE))) {
            throw new IllegalArgumentException("Type must be EXECUTE");
        }
        var in = headers.get(IN);
        var nodeName = headers.getOrDefault(NODE, "none");
        var graphInstance = getGraphInstance(in);
        var node = getNode(nodeName, graphInstance.graph);
        if (!ROUTE.equals(node.getProperty(SKILL))) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have skill - "+ROUTE);
        }
        var stateMachine = graphInstance.stateMachine;
        var forEach = getEntries(node.getProperty(FOR_EACH));
        var statements = getEntries(node.getProperty(STATEMENT));
        if (statements.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have 'statement' entries");
        }
        if (forEach.isEmpty()) {
            return executeStatementBlock(nodeName, statements, graphInstance);
        } else {
            Map<String, List<?>> forEachMapping = getForEachMapping(nodeName, forEach, stateMachine);
            if (forEachMapping.isEmpty()) {
                throw new IllegalArgumentException(NODE_NAME + nodeName +
                        " - No data mapping resolved from 'for_each' entries. LHS must be a list.");
            }
            var size = getModelArraySize(forEachMapping);
            for (int i = 0; i < size; i++) {
                var x = getNextModelParamSet(forEachMapping, i);
                for (var kv : x.entrySet()) {
                    stateMachine.setElement(kv.getKey(), kv.getValue());
                }
                var result = executeStatementBlock(nodeName, statements, graphInstance);
                if (!NEXT.equals(result)) {
                    return result;
                }
            }
            return NEXT;
        }
    }

    private String executeStatementBlock(String nodeName, List<String> statements, GraphInstance graphInstance) {
        var execute = countExecuteStatements(nodeName, statements);
        if (execute > 0) {
            return executeStatements(nodeName, combine(nodeName, graphInstance.graph, statements), graphInstance);
        } else {
            return executeStatements(nodeName, statements, graphInstance);
        }
    }

    private List<String> combine(String nodeName, MiniGraph graph, List<String> entries) {
        List<String> merged = new ArrayList<>();
        for (var entry : entries) {
            var line = entry.trim().toLowerCase();
            if (line.startsWith(EXECUTE_TAG)) {
                mergeStatements(nodeName, line.substring(EXECUTE_TAG.length()).trim(), graph, merged);
            } else {
                merged.add(line);
            }
        }
        return merged;
    }

    private void mergeStatements(String nodeName, String anotherNode, MiniGraph graph, List<String> merged) {
        var that = graph.findNodeByAlias(anotherNode);
        if (that == null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName +
                    " is referring to non-existing node '" + anotherNode + "'");
        }
        if (!ROUTE.equals(that.getProperty(SKILL))) {
            throw new IllegalArgumentException(NODE_NAME + anotherNode + " does not have skill - "+ROUTE);
        }
        var otherStatements = getEntries(that.getProperty(STATEMENT));
        if (countExecuteStatements(anotherNode, otherStatements) > 0) {
            throw new IllegalArgumentException(NODE_NAME + anotherNode + " contains nested EXECUTE statements");
        } else {
            merged.addAll(otherStatements);
        }
    }

    private String executeStatements(String nodeName, List<String> entries, GraphInstance graphInstance) {
        for (var entry : entries) {
            var block = entry.trim();
            var colon = block.indexOf(':');
            var tag = block.substring(0, colon+1).toLowerCase();
            var command = block.substring(colon + 1).trim();
            if (IF_TAG.equals(tag)) {
                // evaluate decisions from an if-then-else multiline statement
                var lines = util.split(block, "\n");
                var decision = evaluate(graphInstance, nodeName, lines);
                if (decision != null) {
                    return decision;
                }
            }
            // guarantee that it is a single line
            command = command.replace('\n', ' ');
            if (COMPUTE_TAG.equals(tag)) {
                compute(command, nodeName, graphInstance);
            }
            if (MAPPING_TAG.equals(tag)) {
                handleDataMappingEntry(nodeName, command, graphInstance);
            }
        }
        return NEXT;
    }

    private void compute(String command, String nodeName, GraphInstance graphInstance) {
        int sep = command.lastIndexOf(MAP_TO);
        if (sep > 0) {
            // lhs is result key and rhs is JavaScript statement
            var lhs = command.substring(0, sep).trim();
            var rhs = command.substring(sep + MAP_TO.length()).trim();
            if (lhs.isEmpty() || rhs.isEmpty()) {
                throw new IllegalArgumentException(NODE_NAME + nodeName + " has invalid statement '"+command+"'");
            }
            var text = substituteVarIfAny(rhs, graphInstance.stateMachine);
            var result = hasBooleanOperator(text)? engine.evalBoolean(text) : engine.evalNumber(text);
            graphInstance.stateMachine.setElement(nodeName + ".result." + lhs, result);
        } else {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have '->' in '"+command+"'");
        }
    }

    private String evaluate(GraphInstance graphInstance, String nodeName, List<String> lines) {
        var ifStatement = getIfStatement(lines).trim();
        var thenStatement = getFirstWord(getThenStatement(lines).trim());
        var elseStatement = getFirstWord(getElseStatement(lines).trim());
        if (ifStatement.isEmpty() || thenStatement.isEmpty() || elseStatement.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have if:, then: or else:");
        }
        var text = substituteVarIfAny(ifStatement, graphInstance.stateMachine);
        return getNext(graphInstance.graph, engine.evalBoolean(text)? thenStatement : elseStatement);
    }
}
