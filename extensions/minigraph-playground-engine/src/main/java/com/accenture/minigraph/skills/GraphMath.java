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
    private static final String MAPPING_TAG = "mapping:";
    private static final String COMPUTE_TAG = "compute:";
    private static final String EXECUTE_TAG = "execute:";
    private static final String IF_TAG = "if:";
    private static final String THEN_TAG = "then:";
    private static final String ELSE_TAG = "else:";

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
        var statements = getEntries(node.getProperty(STATEMENT));
        if (statements.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have 'statement' entries");
        }
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

    private int countExecuteStatements(String nodeName, List<String> statements) {
        var execute = 0;
        var js = 0;
        var error = 0;
        for (var entry : statements) {
            var line = entry.trim().toLowerCase();
            if (line.startsWith(IF_TAG) || line.startsWith(COMPUTE_TAG)) {
                js++;
            } else if (line.startsWith(EXECUTE_TAG)) {
                execute++;
            } else if (!line.startsWith(MAPPING_TAG)) {
                error++;
            }
        }
        if (js == 0) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have 'IF:' or 'COMPUTE:' statements");
        }
        if (error > 0) {
            throw new IllegalArgumentException(NODE_NAME + nodeName +
                    " must use 'IF:', 'COMPUTE:' or 'MAPPING:' statements");
        }
        return execute;
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
        int sep = command.indexOf(MAP_TO);
        if (sep > 0) {
            // lhs is result key and rhs is JavaScript statement
            var lhs = command.substring(0, sep).trim();
            var rhs = command.substring(sep + MAP_TO.length()).trim();
            if (lhs.isEmpty() || rhs.isEmpty()) {
                throw new IllegalArgumentException(NODE_NAME + nodeName + " has invalid statement '"+command+"'");
            }
            var text = getJsWithParameters(rhs, graphInstance.stateMachine, false);
            var result = engine.evalNumber(text);
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
        var text = getJsWithParameters(ifStatement, graphInstance.stateMachine, true);
        return getNext(graphInstance.graph, engine.evalBoolean(text)? thenStatement : elseStatement);
    }

    private String getNext(MiniGraph graph, String statement) {
        if (!NEXT.equalsIgnoreCase(statement)) {
            var nextNode = getNode(statement, graph);
            if (nextNode == null) {
                throw new IllegalArgumentException(NODE_NAME + statement + NOT_FOUND);
            } else {
                return statement;
            }
        }
        return null;
    }

    private String getFirstWord(String statement) {
        var space = statement.indexOf(' ');
        return space == -1? statement : statement.substring(0, space);
    }

    private String getIfStatement(List<String> lines) {
        boolean found = false;
        var sb = new StringBuilder();
        for (String line : lines) {
            var lc = line.toLowerCase().trim();
            if (found) {
                if (lc.startsWith(THEN_TAG) || lc.startsWith(ELSE_TAG)) {
                    break;
                } else {
                    sb.append(line).append(' ');
                }
            }
            if (lc.startsWith(IF_TAG)) {
                sb.append(line.substring(3)).append(' ');
                found = true;
            }
        }
        return sb.toString().trim();
    }

    private String getThenStatement(List<String> lines) {
        boolean found = false;
        var sb = new StringBuilder();
        for (String line : lines) {
            var lc = line.toLowerCase().trim();
            if (found) {
                if (lc.startsWith(IF_TAG) || lc.startsWith(ELSE_TAG)) {
                    break;
                } else {
                    sb.append(line).append(' ');
                }
            }
            if (lc.startsWith(THEN_TAG)) {
                sb.append(line.substring(5)).append(' ');
                found = true;
            }
        }
        return sb.toString().trim();
    }

    private String getElseStatement(List<String> lines) {
        boolean found = false;
        var sb = new StringBuilder();
        for (String line : lines) {
            var lc = line.toLowerCase().trim();
            if (found) {
                if (lc.startsWith(IF_TAG) || lc.startsWith(THEN_TAG)) {
                    break;
                } else {
                    sb.append(line).append(' ');
                }
            }
            if (lc.startsWith(ELSE_TAG)) {
                sb.append(line.substring(5)).append(' ');
                found = true;
            }
        }
        return sb.toString().trim();
    }
}
