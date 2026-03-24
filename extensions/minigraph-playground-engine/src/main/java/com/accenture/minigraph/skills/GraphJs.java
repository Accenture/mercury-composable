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
import com.accenture.minigraph.models.GraphInstance;
import org.graalvm.polyglot.Value;
import org.platformlambda.core.annotations.KernelThreadRunner;
import org.platformlambda.core.annotations.PreLoad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.platformlambda.core.graph.MiniGraph;

import org.graalvm.polyglot.Context;
import org.platformlambda.core.models.EventEnvelope;

/**
 * Since JavaScript uses Kernel resources, we must limit it to a small number less than 100
 */
@KernelThreadRunner
@PreLoad(route = GraphJs.ROUTE, instances=50)
public class GraphJs extends GraphLambdaFunction {
    public static final String ROUTE = "graph.js";

    public GraphJs() {
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
        try (Context context = Context.create(JS)) {
            if (forEach.isEmpty()) {
                return executeStatementBlock(context, nodeName, statements, graphInstance);
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
                    var result = executeStatementBlock(context, nodeName, statements, graphInstance);
                    if (!NEXT.equals(result)) {
                        return result;
                    }
                }
                return NEXT;
            }
        }
    }

    private String executeStatementBlock(Context context, String nodeName, List<String> statements,
                                         GraphInstance graphInstance) {
        var execute = countExecuteStatements(nodeName, statements);
        if (execute > 0) {
            return executeStatements(context, nodeName, combine(nodeName, graphInstance.graph, statements), graphInstance);
        } else {
            return executeStatements(context, nodeName, statements, graphInstance);
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

    private String executeStatements(Context context, String nodeName, List<String> entries, GraphInstance graphInstance) {
        for (var entry : entries) {
            var block = entry.trim();
            var colon = block.indexOf(':');
            var tag = block.substring(0, colon+1).toLowerCase();
            var command = block.substring(colon + 1).trim();
            if (IF_TAG.equals(tag)) {
                // evaluate decisions from an if-then-else multiline statement
                var lines = util.split(block, "\n");
                var decision = evaluate(context, graphInstance, nodeName, lines);
                if (decision != null) {
                    return decision;
                }
            }
            // guarantee that it is a single line
            command = command.replace('\n', ' ');
            if (COMPUTE_TAG.equals(tag)) {
                compute(context, command, nodeName, graphInstance);
            }
            if (MAPPING_TAG.equals(tag)) {
                handleDataMappingEntry(nodeName, command, graphInstance);
            }
        }
        return NEXT;
    }

    private void compute(Context context, String command, String nodeName, GraphInstance graphInstance) {
        int sep = command.lastIndexOf(MAP_TO);
        if (sep > 0) {
            // lhs is result key and rhs is JavaScript statement
            var lhs = command.substring(0, sep).trim();
            var rhs = command.substring(sep + MAP_TO.length()).trim();
            if (lhs.isEmpty() || rhs.isEmpty()) {
                throw new IllegalArgumentException(NODE_NAME + nodeName + " has invalid statement '"+command+"'");
            }
            var text = substituteVarIfAny(rhs, graphInstance.stateMachine);
            Object result = toJavaObject(context.eval(JS, text));
            graphInstance.stateMachine.setElement(nodeName + ".result." + lhs, result);
        } else {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have '->' in '"+command+"'");
        }
    }

    private Map<String, Object> toJavaMap(Value v) {
        if (v == null || v.isNull()) return Collections.emptyMap();
        // JSON Object or alike
        if (v.hasMembers()) {
            Map<String, Object> out = new HashMap<>();
            for (String key : v.getMemberKeys()) {
                Value member = v.getMember(key);
                out.put(key, toJavaObject(member));
            }
            return out;
        }

        throw new IllegalArgumentException("Value is not object-like (no members).");
    }

    private Object toJavaObject(Value v) {
        if (v == null) return null;
        // Arrays
        if (v.hasArrayElements()) {
            long n = v.getArraySize();
            List<Object> list = new ArrayList<>();
            for (long i = 0; i < n; i++) {
                list.add(toJavaObject(v.getArrayElement(i)));
            }
            return list;
        }
        // JSON object or alike
        if (v.hasMembers()) return toJavaMap(v);
        // Scalars
        if (v.isBoolean()) return v.asBoolean();
        if (v.isNumber())  return v.as(Number.class);
        if (v.isNull()) return null;
        // All other types will be converted to String
        return v.asString();
    }

    private String evaluate(Context context, GraphInstance graphInstance, String nodeName, List<String> lines) {
        var ifStatement = getIfStatement(lines).trim();
        var thenStatement = getFirstWord(getThenStatement(lines).trim());
        var elseStatement = getFirstWord(getElseStatement(lines).trim());
        if (ifStatement.isEmpty() || thenStatement.isEmpty() || elseStatement.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have if:, then: or else:");
        }
        if (thenStatement.equals(elseStatement)) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " then and else statements cannot be the same");
        }
        var text = substituteVarIfAny(ifStatement, graphInstance.stateMachine);
        Object result = toJavaObject(context.eval(JS, text));
        return getNext(graphInstance.graph, isTrue(result)? thenStatement : elseStatement);
    }

    private boolean isTrue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        } else {
            var text = value instanceof String v? v: String.valueOf(value);
            if ("true".equalsIgnoreCase(text) || "y".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text)) return true;
            var number = text.contains(".")? util.str2double(text): util.str2long(text);
            return number >= 0;
        }
    }
}
