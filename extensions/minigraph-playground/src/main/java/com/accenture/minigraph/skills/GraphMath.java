package com.accenture.minigraph.skills;

import com.accenture.minigraph.base.GraphLambdaFunction;
import com.accenture.minigraph.math.ExpressionEngine;
import com.accenture.minigraph.models.GraphInstance;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.models.EventEnvelope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@PreLoad(route = GraphMath.ROUTE, instances=200)
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
        var statements = node.getProperty(STATEMENT);
        var entries = statements instanceof List<?> list? list : Collections.emptyList();
        if (entries.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have 'statement' entries");
        }
        var execute = countExecuteStatements(nodeName, entries);
        if (execute > 0) {
            return executeStatements(nodeName, combine(nodeName, graphInstance.graph, entries), graphInstance);
        } else {
            return executeStatements(nodeName, entries, graphInstance);
        }
    }

    private List<String> combine(String nodeName, MiniGraph graph, List<?> entries) {
        List<String> merged = new ArrayList<>();
        for (Object entry : entries) {
            var line = String.valueOf(entry).trim().toLowerCase();
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
        var otherStatements = that.getProperty(STATEMENT);
        var otherEntries = otherStatements instanceof List<?> list? list : Collections.emptyList();
        if (countExecuteStatements(anotherNode, otherEntries) > 0) {
            throw new IllegalArgumentException(NODE_NAME + anotherNode + " contains nested EXECUTE statements");
        } else {
            for (Object more : otherEntries) {
                merged.add(String.valueOf(more));
            }
        }
    }

    private int countExecuteStatements(String nodeName, List<?> statements) {
        var execute = 0;
        var js = 0;
        var error = 0;
        for (Object entry : statements) {
            var line = String.valueOf(entry).trim().toLowerCase();
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

    private String executeStatements(String nodeName, List<?> entries, GraphInstance graphInstance) {
        for (Object entry : entries) {
            var line = String.valueOf(entry).trim();
            var colon = line.indexOf(':');
            var tag = line.substring(0, colon+1).toLowerCase();
            var command = line.substring(colon + 1).trim();
            if (IF_TAG.equals(tag)) {
                // evaluate decisions from an if-then-else multiline statement
                var lines = util.split(line, "\n");
                var decision = evaluate(graphInstance, nodeName, lines);
                if (decision != null) {
                    return decision;
                }
            }
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
                    return sb.toString();
                } else {
                    sb.append(line);
                }
            }
            if (lc.startsWith(IF_TAG)) {
                sb.append(line.substring(3));
                found = true;
            }
        }
        return sb.toString();
    }

    private String getThenStatement(List<String> lines) {
        boolean found = false;
        var sb = new StringBuilder();
        for (String line : lines) {
            var lc = line.toLowerCase().trim();
            if (found) {
                if (lc.startsWith(IF_TAG) || lc.startsWith(ELSE_TAG)) {
                    return sb.toString();
                } else {
                    sb.append(line);
                }
            }
            if (lc.startsWith(THEN_TAG)) {
                sb.append(line.substring(5));
                found = true;
            }
        }
        return sb.toString();
    }

    private String getElseStatement(List<String> lines) {
        boolean found = false;
        var sb = new StringBuilder();
        for (String line : lines) {
            var lc = line.toLowerCase().trim();
            if (found) {
                if (lc.startsWith(IF_TAG) || lc.startsWith(THEN_TAG)) {
                    return sb.toString();
                } else {
                    sb.append(line);
                }
            }
            if (lc.startsWith(ELSE_TAG)) {
                sb.append(line.substring(5));
                found = true;
            }
        }
        return sb.toString();
    }
}
