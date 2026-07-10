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
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * The graph.task skill invokes a composable function (a TypedLambdaFunction registered with
 * the PreLoad annotation) through its route name, given by the "task" property of the node.
 * <p>
 * Input data mapping follows the Event Script syntax:
 * RHS '*' maps the LHS value as the whole request body, 'header.{name}' sets a request header,
 * and any other RHS is a composite key path in the request body.
 * <p>
 * The function's response is stored as the node's "result", "status" and "header" properties
 * and the optional output data mapping copies them to the 'model.' or 'output.' namespace.
 */
@PreLoad(route = GraphTask.ROUTE, instances = 300)
public class GraphTask extends GraphLambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(GraphTask.class);
    public static final String ROUTE = "graph.task";
    private static final String WHOLE_BODY = "*";

    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance)
                                throws ExecutionException, InterruptedException {
        if (!EXECUTE.equals(headers.get(TYPE))) {
            throw new IllegalArgumentException("Type must be EXECUTE");
        }
        var po = PostOffice.trackable(headers, instance);
        var nodeName = headers.getOrDefault(NODE, "none");
        po.annotateTrace(NODE, nodeName);
        var in = headers.get(IN);
        var graphInstance = getGraphInstance(in);
        var stateMachine = graphInstance.stateMachine;
        var node = getNode(nodeName, graphInstance.graph);
        if (!ROUTE.equals(node.getProperty(SKILL))) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have skill - " + ROUTE);
        }
        var route = node.getProperty(TASK) instanceof String value && !value.isBlank()? value.trim() : null;
        if (route == null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have a 'task' route");
        }
        if (!po.exists(route)) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " - task '" + route + "' does not exist");
        }
        // reset result to ensure execution is idempotent
        stateMachine.removeElement(nodeName + "." + RESULT);
        stateMachine.removeElement(nodeName + "." + HEADER);
        stateMachine.removeElement(nodeName + "." + STATUS);
        stateMachine.removeElement(nodeName + "." + ERROR);
        var timeout = getModelTtl(graphInstance);
        var mapping = getEntries(node.getProperty(INPUT));
        var forEach = getEntries(node.getProperty(FOR_EACH));
        if (forEach.isEmpty()) {
            var request = buildTaskRequest(nodeName, route, mapping, stateMachine);
            return invokeTask(po, node, graphInstance, route, request, timeout);
        }
        Map<String, List<Object>> forEachMapping = getForEachMapping(nodeName, forEach, stateMachine);
        // iterative task requests with an array of parameters
        if (forEachMapping.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName +
                    " - No data mapping resolved from 'for_each' entries. LHS must be a list.");
        }
        callTaskWithForkJoin(po, graphInstance, node, route, forEachMapping, mapping, timeout);
        return nextPath(stateMachine, node);
    }

    private EventEnvelope buildTaskRequest(String nodeName, String route,
                                           List<String> mapping, MultiLevelMap stateMachine) {
        var request = new EventEnvelope().setTo(route).setCorrelationId(util.getUuid());
        Object body = new HashMap<String, Object>();
        for (var entry : mapping) {
            int sep = entry.lastIndexOf(MAP_TO);
            if (sep <= 0) {
                throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have '->' in '" + entry + "'");
            }
            var lhs = substituteVarIfAny(entry.substring(0, sep).trim(), stateMachine);
            var rhs = entry.substring(sep + MAP_TO.length()).trim();
            var value = helper.getLhsOrConstant(lhs, stateMachine);
            body = stageTaskParameter(nodeName, request, rhs, value, body);
        }
        return request.setBody(body);
    }

    @SuppressWarnings("unchecked")
    private Object stageTaskParameter(String nodeName, EventEnvelope request, String rhs, Object value, Object body) {
        if (WHOLE_BODY.equals(rhs)) {
            // The whole-body target maps the LHS value as the entire request body.
            // A map is deep copied so that later entries can merge into it without
            // touching the graph's state machine.
            return value instanceof Map? util.deepCopy((Map<String, Object>) value) : value;
        }
        if (rhs.startsWith(HEADER_NAMESPACE)) {
            var key = rhs.substring(HEADER_NAMESPACE.length()).trim();
            if (value != null && !key.isEmpty()) {
                request.setHeader(key, String.valueOf(value));
            }
            return body;
        }
        if (body instanceof Map) {
            var map = new MultiLevelMap((Map<String, Object>) body);
            if (value != null) {
                map.setElement(rhs, value);
            } else {
                map.removeElement(rhs);
            }
            return map.getMap();
        }
        throw new IllegalArgumentException(NODE_NAME + nodeName + " - cannot map '" + rhs +
                "' because '*' was mapped with a non-map value");
    }

    private Object invokeTask(PostOffice po, SimpleNode node, GraphInstance graphInstance,
                              String route, EventEnvelope request, long ttl) {
        log.info("Call task {}, ttl={}", route, ttl);
        po.annotateTrace(TASK, route);
        var nodeName = node.getAlias();
        var stateMachine = graphInstance.stateMachine;
        stateMachine.setElement(nodeName + "." + TARGET, route);
        return Mono.create(sink ->
            po.eRequest(request, ttl, false).thenAccept(response -> {
                stateMachine.setElement(nodeName + "." + STATUS, response.getStatus());
                if (!response.getHeaders().isEmpty()) {
                    stateMachine.setElement(nodeName + "." + HEADER, response.getHeaders());
                }
                if (response.hasError()) {
                    sink.success(setError(stateMachine, node, response));
                } else {
                    stateMachine.setElement(nodeName + "." + RESULT, response.getBody());
                    var outputMapping = getEntries(node.getProperty(OUTPUT));
                    performFetcherOutputMapping(nodeName, stateMachine, outputMapping);
                    sink.success(NEXT);
                }
        }));
    }

    private void callTaskWithForkJoin(PostOffice po, GraphInstance graphInstance, SimpleNode node, String route,
                                      Map<String, List<Object>> forEachMapping, List<String> mapping, long timeout)
                                        throws ExecutionException, InterruptedException {
        var nodeName = node.getAlias();
        var stateMachine = graphInstance.stateMachine;
        var givenConcurrency = util.str2int(String.valueOf(node.getProperty(CONCURRENCY)));
        var concurrency = Math.clamp(givenConcurrency < 0 ? 3 : givenConcurrency, 1, 30);
        var size = getModelArraySize(forEachMapping);
        Deque<EventEnvelope> stack = new ArrayDeque<>();
        // release one set of parameters from the array for each request
        for (int i = 0; i < size; i++) {
            var paramSet = getNextModelParamSet(forEachMapping, i);
            for (var kv : paramSet.entrySet()) {
                stateMachine.setElement(kv.getKey(), kv.getValue());
            }
            stack.add(buildTaskRequest(nodeName, route, mapping, stateMachine));
        }
        log.info("Call task {}, for each {}, parallel={}, ttl={}",
                route, forEachMapping.keySet(), concurrency, timeout);
        po.annotateTrace(TASK, route);
        po.annotateTrace(FOR_EACH, String.valueOf(forEachMapping.keySet()));
        stateMachine.setElement(nodeName + "." + TARGET, route);
        List<EventEnvelope> batch = new ArrayList<>();
        var n = concurrency;
        while (!stack.isEmpty()) {
            n--;
            batch.add(stack.pop());
            if (stack.isEmpty() || n == 0) {
                n = concurrency;
                doForkJoin(po, node, stateMachine, batch, timeout);
                batch.clear();
            }
        }
        var outputMapping = getEntries(node.getProperty(OUTPUT));
        performFetcherOutputMapping(nodeName, stateMachine, outputMapping);
    }

    private void doForkJoin(PostOffice po, SimpleNode node, MultiLevelMap stateMachine,
                            List<EventEnvelope> batch, long timeout)
                            throws ExecutionException, InterruptedException {
        var nodeName = node.getAlias();
        var responses = po.request(batch, timeout, false).get();
        for (var response : responses) {
            stateMachine.setElement(nodeName + "." + STATUS, response.getStatus());
            if (!response.getHeaders().isEmpty()) {
                stateMachine.setElement(nodeName + "." + HEADER + "[]", response.getHeaders());
            }
            if (response.hasError()) {
                setError(stateMachine, node, response);
            } else {
                stateMachine.setElement(nodeName + "." + RESULT + "[]", response.getBody());
            }
        }
    }

    private String setError(MultiLevelMap stateMachine, SimpleNode node, EventEnvelope response) {
        var nodeName = node.getAlias();
        stateMachine.setElement(nodeName + "." + ERROR, response.getError());
        var errorHandler = node.getProperty(EXCEPTION);
        if (errorHandler == null) {
            stateMachine.setElement(OUTPUT_BODY, response.getBody());
            stateMachine.setElement(OUTPUT_NAMESPACE + HEADER, response.getHeaders());
            stateMachine.setElement(OUTPUT_NAMESPACE + STATUS, response.getStatus());
            return NEXT;
        } else {
            return String.valueOf(errorHandler);
        }
    }

    private String nextPath(MultiLevelMap stateMachine, SimpleNode task) {
        var nodeName = task.getAlias();
        var processStatus = stateMachine.getElement(nodeName + "." + STATUS);
        var resultError = stateMachine.getElement(nodeName + "." + ERROR);
        var errorHandler = task.getProperty(EXCEPTION);
        if (processStatus instanceof Integer && resultError != null && errorHandler != null) {
            return String.valueOf(errorHandler);
        } else {
            return NEXT;
        }
    }
}
