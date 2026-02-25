package com.accenture.minigraph.skills;

import com.accenture.minigraph.base.GraphLambdaFunction;
import com.accenture.minigraph.models.GraphInstance;
import org.platformlambda.core.annotations.KernelThreadRunner;
import org.platformlambda.core.annotations.PreLoad;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.platformlambda.core.graph.MiniGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.graalvm.polyglot.Context;

@KernelThreadRunner
@PreLoad(route = GraphJs.ROUTE, instances=50)
public class GraphJs extends GraphLambdaFunction {
    public static final String ROUTE = "graph.js";
    private static final Logger log = LoggerFactory.getLogger(GraphJs.class);
    private static final String IF_TAG = "if:";
    private static final String THEN_TAG = "then:";
    private static final String ELSE_TAG = "else:";

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (!EXECUTE.equals(headers.get(TYPE))) {
            throw new IllegalArgumentException("Type must be EXECUTE");
        }
        var in = headers.get(IN);
        var nodeName = headers.getOrDefault(NODE, "none");
        var graphInstance = getGraphInstance(in);
        var node = getNode(nodeName, graphInstance.graph);
        var properties = node.getProperties();
        if (!ROUTE.equals(properties.get(SKILL))) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have skill - "+ROUTE);
        }
        var mapping = properties.get(JS);
        var decision = properties.get(DECISION);
        var jsEntries = mapping instanceof List<?> entries? entries : Collections.emptyList();
        var decisionEntries = decision instanceof List<?> entries? entries : Collections.emptyList();
        if (jsEntries.isEmpty() && decisionEntries.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have 'js' or 'decision' entries");
        }
        return executeJs(nodeName, jsEntries, decisionEntries, graphInstance);
    }

    private String executeJs(String nodeName, List<?> jsEntries, List<?> decisionEntries, GraphInstance graphInstance) {
        try (Context context = Context.create(JS)) {
            // execute JavaScript statements
            for (Object entry : jsEntries) {
                var command = String.valueOf(entry);
                int sep = command.indexOf(MAP_TO);
                if (sep > 0) {
                    // lhs is result key and rhs is JavaScript statement
                    var lhs = command.substring(0, sep).trim();
                    var rhs = command.substring(sep + MAP_TO.length()).trim();
                    if (lhs.isEmpty() || rhs.isEmpty()) {
                        throw new IllegalArgumentException(NODE_NAME + nodeName + " has invalid js '"+command+"'");
                    }
                    var text = getJsWithParameters(rhs, graphInstance.stateMachine);
                    var result = context.eval(JS, text).as(Object.class);
                    graphInstance.stateMachine.setElement(nodeName + ".result." + lhs, result);
                } else {
                    throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have '->' in '"+command+"'");
                }
            }
            // evaluate decisions
            for (Object entry : decisionEntries) {
                var command = String.valueOf(entry).trim();
                var lines = util.split(command, "\n");
                var decision = evaluate(context, graphInstance, nodeName, lines);
                if (decision != null) {
                    return decision;
                }
            }
        }
        return NEXT;
    }

    private String evaluate(Context context, GraphInstance graphInstance, String nodeName, List<String> lines) {
        var ifStatement = getIfStatement(lines).trim();
        var thenStatement = getFirstWord(getThenStatement(lines).trim());
        var elseStatement = getFirstWord(getElseStatement(lines).trim());
        if (ifStatement.isEmpty() || thenStatement.isEmpty() || elseStatement.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have if:, then: or else:");
        }
        var text = getJsWithParameters(ifStatement, graphInstance.stateMachine);
        var result = String.valueOf(context.eval(JS, text).as(Object.class));
        return getNext(graphInstance.graph, isTrue(result)? thenStatement : elseStatement);
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

    private boolean isTrue(String text) {
        if ("true".equals(text) || "y".equals(text) || "t".equals(text)) return true;
        var number = text.contains(".")? util.str2float(text): util.str2long(text);
        return number >= 0;
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
