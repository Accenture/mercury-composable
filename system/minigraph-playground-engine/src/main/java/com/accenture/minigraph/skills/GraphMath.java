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
import com.accenture.minigraph.math.DecimalEvaluator;
import com.accenture.minigraph.math.ExpressionEngine;
import com.accenture.minigraph.math.ExpressionSource;
import com.accenture.minigraph.models.GraphInstance;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@PreLoad(route = GraphMath.ROUTE, instances=300)
public class GraphMath extends GraphLambdaFunction {
    public static final String ROUTE = "graph.math";
    /** The node property that limits the decimal places of this node's DECIMAL results. */
    public static final String PRECISION = "precision";
    /** The application property that limits the decimal places of every DECIMAL result without a node limit. */
    public static final String PRECISION_CONFIG = "big.decimal.precision";
    /** The limit when neither the node nor the application sets one. */
    public static final int DEFAULT_PRECISION = 6;
    private static final String RESULT = ".result.";
    private static final ExpressionEngine engine = new ExpressionEngine();
    private static final AtomicInteger defaultPrecision = new AtomicInteger(-1);

    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        if (!EXECUTE.equals(headers.get(TYPE))) {
            throw new IllegalArgumentException("Type must be EXECUTE");
        }
        var po = PostOffice.trackable(headers, instance);
        var nodeName = headers.getOrDefault(NODE, "none");
        po.annotateTrace(NODE, nodeName);
        var in = headers.get(IN);
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
        var result = executeNode(nodeName, graphInstance, forEach, statements);
        stateMachine.setElement(nodeName+DOT_DECISION, result);
        var delay = stateMachine.getElement(nodeName+DOT_DELAY);
        if (delay instanceof Long ms) {
            return Mono.create(sink ->
                    Platform.getInstance().getVertx().setTimer(ms, t -> sink.success(result)));
        } else {
            return result;
        }
    }

    private String executeNode(String nodeName, GraphInstance graphInstance,
                               List<String> forEach, List<String> statements) {
        var execute = countExecuteStatements(nodeName, statements, true);
        var merged = execute > 0? combine(ROUTE, nodeName, graphInstance.graph, statements) : statements;
        if (forEach.isEmpty()) {
            return executeStatements(nodeName, merged, graphInstance);
        } else {
            List<List<String>> blocks = splitBlocks(merged);
            var result1 = executeStatements(nodeName, blocks.getFirst(), graphInstance);
            if (!NEXT.equals(result1)) {
                return result1;
            }
            var result2 = executeForEach(nodeName, forEach, blocks.get(1), graphInstance);
            if (!NEXT.equals(result2)) {
                return result2;
            }
            return executeStatements(nodeName, blocks.getLast(), graphInstance);
        }
    }

    private String executeForEach(String nodeName, List<String> forEach, List<String> statements,
                                  GraphInstance graphInstance) {
        if (!statements.isEmpty()) {
            var stateMachine = graphInstance.stateMachine;
            Map<String, List<Object>> forEachMapping = getForEachMapping(nodeName, forEach, stateMachine);
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
                var result = executeStatements(nodeName, statements, graphInstance);
                if (!NEXT.equals(result)) {
                    return result;
                }
            }
        }
        return NEXT;
    }

    private String executeStatements(String nodeName, List<String> entries, GraphInstance graphInstance) {
        String jump = null;
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
            processCommands(tag, command, nodeName, graphInstance);
            var next = getNext(tag, command, graphInstance.stateMachine);
            if (next != null) {
                jump = next;
            }
        }
        return jump == null? NEXT : jump;
    }

    private void processCommands(String tag, String command, String nodeName, GraphInstance graphInstance) {
        // guarantee that it is a single line for all commands except IF-THEN-ELSE
        command = command.replace('\n', ' ');
        if (COMPUTE_TAG.equals(tag)) {
            compute(command, nodeName, graphInstance);
        }
        if (CONDITION_TAG.equals(tag)) {
            condition(command, nodeName, graphInstance);
        }
        if (DECIMAL_TAG.equals(tag)) {
            decimal(command, nodeName, graphInstance);
        }
        if (MAPPING_TAG.equals(tag)) {
            handleDataMappingEntry(nodeName, command, graphInstance);
        }
        if (RESET_TAG.equals(tag)) {
            // dynamic variables resolve in every statement command - a generic error
            // handler resets the failing node via 'RESET: {error.source}, error-handler'
            resetNodes(substituteVarIfAny(command, graphInstance.stateMachine), graphInstance);
        }
        if (DELAY_TAG.equals(tag)) {
            // e.g. a computed backoff: 'DELAY: {model.backoff}'
            var delay = util.str2long(substituteVarIfAny(command, graphInstance.stateMachine));
            if (delay >= 0) {
                graphInstance.stateMachine.setElement(nodeName + DOT_DELAY, delay);
            }
        }
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
            assertVariablesResolved(rhs, graphInstance.stateMachine);
            // a statement with a ternary computes a number, the branch's value, as a DECIMAL does; without one
            // the statement as written decides whether it computes a boolean, never a value rendered into it
            var logical = hasBooleanOperator(rhs) && !ExpressionSource.hasTernary(rhs);
            var text = render(rhs, logical? ExpressionSource.Kind.BOOLEAN : ExpressionSource.Kind.NUMBER,
                    graphInstance.stateMachine);
            final Object result;
            try {
                result = logical? engine.evalBoolean(text) : engine.evalNumber(text);
            } catch (RuntimeException e) {
                throw nameOffendingSelectors(e, rhs, graphInstance.stateMachine);
            }
            graphInstance.stateMachine.setElement(nodeName + RESULT + lhs, result);
        } else {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have '->' in '"+command+"'");
        }
    }

    /**
     * DECIMAL: var -> expression - the high-precision COMPUTE (RFC-0001). The expression is evaluated with
     * exact decimal arithmetic and the result is stored at {node}.result.{var} as a canonical decimal string,
     * which is what survives 'graph.suspend', 'graph.resume' and every event hop (a BigDecimal would come back
     * a String). COMPUTE keeps its meaning, so a graph that never says DECIMAL is unchanged.
     * <p>
     * The statement computes a number, so a text value renders as the one number it spells and any other text
     * fails by name ({@link ExpressionSource}): a value from the data is never read as part of the expression.
     * A Double or Float value is accepted through the shortest decimal text it prints as, at its minimal scale:
     * once a value is text its Java type is gone, so the conversion happens as the value is rendered
     * (5.0E-4 renders 0.0005, not the 0.00050 its own text would parse as). That is exact over the
     * text received and only as exact as the computation that produced the double - the guide declares it,
     * so sending a number instead of a string is a conscious decision.
     * <p>
     * The result's decimal places are limited by the node's 'precision' property, else by the application's
     * big.decimal.precision (default 6): a result with more is rounded HALF_UP to the limit, one with fewer
     * keeps its computed scale. The limit applies to each statement's result, never to the arithmetic inside
     * the expression, and an EXECUTE'd module takes the calling node's limit, as its results land on the caller.
     *
     * @param command the statement after the tag
     * @param nodeName the node's alias
     * @param graphInstance the graph instance
     */
    private void decimal(String command, String nodeName, GraphInstance graphInstance) {
        int sep = command.lastIndexOf(MAP_TO);
        if (sep > 0) {
            var lhs = command.substring(0, sep).trim();
            var rhs = command.substring(sep + MAP_TO.length()).trim();
            if (lhs.isEmpty() || rhs.isEmpty()) {
                throw new IllegalArgumentException(NODE_NAME + nodeName + " has invalid statement '"+command+"'");
            }
            var stateMachine = graphInstance.stateMachine;
            var precision = decimalPrecision(nodeName, graphInstance);
            assertVariablesResolved(rhs, stateMachine);
            var text = render(rhs, ExpressionSource.Kind.DECIMAL, stateMachine);
            final String result;
            try {
                result = DecimalEvaluator.evaluate(text, precision);
            } catch (RuntimeException e) {
                throw nameOffendingSelectors(e, rhs, stateMachine);
            }
            stateMachine.setElement(nodeName + RESULT + lhs, result);
        } else {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have '->' in '"+command+"'");
        }
    }

    /**
     * The decimal places a DECIMAL result of this node may carry: the node's 'precision' property when it has
     * one, else the application's default.
     *
     * @param nodeName the node's alias
     * @param graphInstance the graph instance
     * @return a whole number from 0 to DecimalEvaluator.MAX_SCALE
     */
    private int decimalPrecision(String nodeName, GraphInstance graphInstance) {
        var value = getNode(nodeName, graphInstance.graph).getProperty(PRECISION);
        if (value == null) {
            return getDefaultPrecision();
        }
        try {
            return DecimalEvaluator.precision(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " - " + PRECISION + " " + e.getMessage());
        }
    }

    private static int getDefaultPrecision() {
        var precision = defaultPrecision.get();
        if (precision < 0) {
            var configured = AppConfigReader.getInstance().getProperty(PRECISION_CONFIG,
                    String.valueOf(DEFAULT_PRECISION));
            try {
                precision = DecimalEvaluator.precision(configured);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(PRECISION_CONFIG + " " + e.getMessage());
            }
            defaultPrecision.set(precision);
        }
        return precision;
    }

    private String render(String expression, ExpressionSource.Kind kind, MultiLevelMap stateMachine) {
        return ExpressionSource.render(expression, kind, key -> helper.getLhsOrConstant(key, stateMachine));
    }

    /**
     * CONDITION: var -> expression - the explicit boolean statement. The expression is evaluated as a
     * boolean whatever operators it carries (a bare {model.flag} included) and the result is stored as a
     * Boolean at {node}.result.{var}. Therefore, a decision value is declared as one instead of being inferred
     * from the operators of a COMPUTE.
     *
     * @param command the statement after the tag
     * @param nodeName the node's alias
     * @param graphInstance the graph instance
     */
    private void condition(String command, String nodeName, GraphInstance graphInstance) {
        int sep = command.lastIndexOf(MAP_TO);
        if (sep > 0) {
            var lhs = command.substring(0, sep).trim();
            var rhs = command.substring(sep + MAP_TO.length()).trim();
            if (lhs.isEmpty() || rhs.isEmpty()) {
                throw new IllegalArgumentException(NODE_NAME + nodeName + " has invalid statement '"+command+"'");
            }
            assertVariablesResolved(rhs, graphInstance.stateMachine);
            var text = render(rhs, ExpressionSource.Kind.BOOLEAN, graphInstance.stateMachine);
            final boolean result;
            try {
                result = engine.evalBoolean(text);
            } catch (RuntimeException e) {
                throw nameOffendingSelectors(e, rhs, graphInstance.stateMachine);
            }
            graphInstance.stateMachine.setElement(nodeName + RESULT + lhs, result);
        } else {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have '->' in '"+command+"'");
        }
    }

    private String evaluate(GraphInstance graphInstance, String nodeName, List<String> lines) {
        var stateMachine = graphInstance.stateMachine;
        var ifStatement = getIfStatement(lines).trim();
        // jump targets resolve dynamic variables like every other statement position,
        // e.g. 'THEN: {error.source}' jumps back to the failing node
        var thenStatement = getFirstWord(substituteVarIfAny(getThenStatement(lines).trim(), stateMachine));
        var elseStatement = getFirstWord(substituteVarIfAny(getElseStatement(lines).trim(), stateMachine));
        if (ifStatement.isEmpty() || thenStatement.isEmpty() || elseStatement.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have if:, then: or else:");
        }
        assertVariablesResolved(ifStatement, stateMachine);
        var text = render(ifStatement, hasBooleanOperator(ifStatement)? ExpressionSource.Kind.BOOLEAN
                : ExpressionSource.Kind.NUMBER, stateMachine);
        final boolean decision;
        try {
            decision = engine.evalBoolean(text);
        } catch (RuntimeException e) {
            throw nameOffendingSelectors(e, ifStatement, stateMachine);
        }
        return getNext(graphInstance.graph, decision? thenStatement : elseStatement);
    }
}
