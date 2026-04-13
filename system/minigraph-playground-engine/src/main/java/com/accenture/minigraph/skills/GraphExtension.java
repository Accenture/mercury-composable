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

import com.accenture.automation.EventScriptManager;
import com.accenture.minigraph.common.GraphLambdaFunction;
import com.accenture.minigraph.models.GraphInstance;
import com.accenture.models.Flows;
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

@PreLoad(route = GraphExtension.ROUTE, instances=300)
public class GraphExtension extends GraphLambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(GraphExtension.class);
    public static final String ROUTE = "graph.extension";
    private static final String GRAPH_EXECUTOR = "graph-executor";

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
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have skill - "+ROUTE);
        }
        if ("true".equals(headers.get(LIVE))) {
            stateMachine.setElement(nodeName + "." + LIVE, true);
        }
        var graphId = node.getProperty(EXTENSION) instanceof String id? id : null;
        if (graphId == null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have graph ID");
        }
        // reset result to ensure execution is idempotent
        stateMachine.removeElement(nodeName + "." + RESULT);
        stateMachine.removeElement(nodeName + "." + HEADER);
        var forEach = getEntries(node.getProperty(FOR_EACH));
        if (forEach.isEmpty()) {
            return callExtension(po, node, graphInstance, graphId);
        }
        Map<String, List<?>> forEachMapping = getForEachMapping(nodeName, forEach, stateMachine);
        // iterative API requests with an array of parameters
        if (forEachMapping.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName +
                    " - No data mapping resolved from 'for_each' entries. LHS must be a list.");
        }
        var mapping = getEntries(node.getProperty(INPUT));
        var size = getModelArraySize(forEachMapping);
        for (int i = 0; i < size; i++) {
            var x = getNextModelParamSet(forEachMapping, i);
            for (var kv : x.entrySet()) {
                stateMachine.setElement(kv.getKey(), kv.getValue());
            }
            for (var entry : mapping) {
                fillFetcherApiParameters(nodeName, entry, graphInstance, true);
            }
        }
        callExtensionWithForkJoin(po, graphInstance, node, graphId, size);
        return NEXT;
    }

    @SuppressWarnings("unchecked")
    private void callExtensionWithForkJoin(PostOffice po, GraphInstance graphInstance,
                                             SimpleNode node, String extension, int size)
                                                throws ExecutionException, InterruptedException {
        var nodeName = node.getAlias();
        var givenConcurrency = util.str2int(String.valueOf(node.getProperty(CONCURRENCY)));
        var concurrency = Math.clamp(givenConcurrency < 0 ? 3 : givenConcurrency, 1, 30);
        var timeout = getModelTtl(graphInstance);
        var stateMachine = graphInstance.stateMachine;
        var apiParams = (Map<String, List<Object>>) stateMachine.getElement(nodeName + EACH, new HashMap<>());
        Deque<EventEnvelope> stack = new ArrayDeque<>();
        // release one set of parameters from the array for each data dictionary item
        for (int i = 0; i < size; i++) {
            var parameters = new HashMap<String, Object>();
            for (var kv : apiParams.entrySet()) {
                var key = kv.getKey();
                var value = kv.getValue().get(i);
                parameters.put(key, value);
            }
            var forward = new EventEnvelope().setTo(EventScriptManager.SERVICE_NAME).setCorrelationId(util.getUuid());
            if (extension.startsWith(FLOW_PROTOCOL)) {
                var flowId = extension.substring(FLOW_PROTOCOL.length());
                var flow = Flows.getFlow(flowId);
                if (flow == null) {
                    throw new IllegalArgumentException(NODE_NAME + nodeName + " -  "+extension+" does not exist");
                }
                forward.setHeader(FLOW_ID, flowId);
                forward.setBody(Map.of(BODY, parameters, HEADER, Map.of(), TTL, timeout));
            } else {
                var dataset = Map.of(BODY, parameters, HEADER, Map.of(), TTL, timeout,
                                    PATH_PARAMETER, Map.of(GRAPH_ID, extension));
                forward.setHeader(FLOW_ID, GRAPH_EXECUTOR);
                forward.setBody(dataset);
            }
            stack.add(forward);
        }
        runConcurrentRequests(po, graphInstance, node, extension, concurrency, stack, timeout);
        var outputMapping = getEntries(node.getProperty(OUTPUT));
        performFetcherOutputMapping(nodeName, stateMachine, outputMapping);
        // clear temporary dataset
        stateMachine.removeElement(nodeName + FETCH);
        stateMachine.removeElement(nodeName + EACH);
    }

    private void runConcurrentRequests(PostOffice po, GraphInstance graphInstance, SimpleNode node,
                                       String extension, int concurrency, Deque<EventEnvelope> stack, long timeout)
                                        throws ExecutionException, InterruptedException {
        po.annotateTrace(EXTENSION, extension);
        var nameLoaded = false;
        var parameterNames = new HashSet<String>();
        var nodeName = node.getAlias();
        var stateMachine = graphInstance.stateMachine;
        List<EventEnvelope> batch = new ArrayList<>();
        var n = concurrency;
        while (!stack.isEmpty()) {
            n--;
            batch.add(stack.pop());
            if (stack.isEmpty() || n == 0) {
                n = concurrency;
                var body = batch.getFirst().getBody() instanceof Map<?, ?> map? map : Map.of();
                if (!nameLoaded) {
                    nameLoaded = true;
                    var names = body.get(BODY) instanceof Map<?, ?> map? map.keySet() : Set.of();
                    names.forEach(name -> parameterNames.add(String.valueOf(name)));
                    po.annotateTrace(FOR_EACH, String.valueOf(parameterNames));
                }
                log.info("Call extension {}, for each {}, parallel={}, ttl={}", extension,
                        parameterNames, batch.size(), timeout);
                stateMachine.setElement(nodeName + "." + TARGET, extension);
                doForkJoin(po, nodeName, stateMachine, batch, timeout);
                batch.clear();
            }
        }
    }

    private void doForkJoin(PostOffice po, String nodeName, MultiLevelMap stateMachine,
                            List<EventEnvelope> batch, long timeout)
                            throws ExecutionException, InterruptedException {
        var responses = po.request(batch, timeout, false).get();
        for (var response : responses) {
            stateMachine.setElement(nodeName + "." + STATUS, response.getStatus());
            if (!response.getHeaders().isEmpty()) {
                stateMachine.setElement(nodeName + "." + HEADER + "[]", response.getHeaders());
            }
            if (response.hasError()) {
                stateMachine.setElement(nodeName + "." + ERROR, response.getError());
                throw new IllegalArgumentException(String.valueOf(response.getError()));
            } else {
                stateMachine.setElement(nodeName + "." + RESULT + "[]", response.getBody());
            }
        }
    }

    private Object callExtension(PostOffice po, SimpleNode node, GraphInstance graphInstance, String graphId) {
        var nodeName = node.getAlias();
        var mapping = getEntries(node.getProperty(INPUT));
        if (mapping.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have 'input' entries");
        }
        for (var entry : mapping) {
            fillFetcherApiParameters(nodeName, entry, graphInstance, false);
        }
        var timeout = getModelTtl(graphInstance);
        return retrieveFromExtension(po, node, graphInstance, graphId, timeout);
    }

    private Object retrieveFromExtension(PostOffice po, SimpleNode node, GraphInstance graphInstance,
                                         String extension, long ttl) {
        log.info("Call extension {}, ttl={}", extension, ttl);
        po.annotateTrace(EXTENSION, extension);
        var nodeName = node.getAlias();
        var stateMachine = graphInstance.stateMachine;
        stateMachine.setElement(nodeName + "." + TARGET, extension);
        // construct dataset
        var parameters = stateMachine.getElement(nodeName + FETCH, new HashMap<>());
        var forward = new EventEnvelope();
        forward.setTo(EventScriptManager.SERVICE_NAME).setCorrelationId(util.getUuid());
        if (extension.startsWith(FLOW_PROTOCOL)) {
            var flowId = extension.substring(FLOW_PROTOCOL.length());
            var flow = Flows.getFlow(flowId);
            if (flow == null) {
                throw new IllegalArgumentException(NODE_NAME + nodeName + " -  "+extension+" does not exist");
            }
            var dataset = Map.of(BODY, parameters instanceof Map? parameters : Map.of(), HEADER, Map.of(), TTL, ttl);
            forward.setHeader(FLOW_ID, flowId).setBody(dataset);
        } else {
            var dataset = Map.of(BODY, parameters instanceof Map? parameters : Map.of(), HEADER, Map.of(), TTL, ttl,
                                PATH_PARAMETER, Map.of(GRAPH_ID, extension));
            forward.setHeader(FLOW_ID, GRAPH_EXECUTOR).setBody(dataset);
        }
        // clean up working area
        stateMachine.removeElement(nodeName + FETCH);
        return Mono.create(sink ->
            po.eRequest(forward, ttl, false).thenAccept(response -> {
                stateMachine.setElement(nodeName + "." + STATUS, response.getStatus());
                if (!response.getHeaders().isEmpty()) {
                    stateMachine.setElement(nodeName + "." + HEADER, response.getHeaders());
                }
                if (response.hasError()) {
                    stateMachine.setElement(nodeName + "." + ERROR, response.getError());
                    sink.error(new IllegalArgumentException(String.valueOf(response.getError())));
                } else {
                    stateMachine.setElement(nodeName + "." + RESULT, response.getBody());
                    var outputMapping = getEntries(node.getProperty(OUTPUT));
                    performFetcherOutputMapping(nodeName, stateMachine, outputMapping);
                    sink.success(NEXT);
                }
        }));
    }
}
