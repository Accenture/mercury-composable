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

package com.accenture.minigraph.services;

import com.accenture.minigraph.common.GraphLambdaFunction;
import com.accenture.minigraph.exception.FetchException;
import com.accenture.minigraph.models.GraphInstance;
import com.accenture.minigraph.skills.GraphJoin;
import com.accenture.models.FlowInstance;
import com.accenture.models.Flows;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@PreLoad(route = GraphExecutor.ROUTE, instances=300)
public class GraphExecutor extends GraphLambdaFunction {
    public static final String ROUTE = "graph.executor";
    private static final Logger log = LoggerFactory.getLogger(GraphExecutor.class);
    private static final String DEFAULT_DEPLOY_DIR = "classpath:/graph";
    private static final int STATUS_CONTINUE = 100;
    private static final String DONE = "done";
    private final String deployedGraphLocation;

    public GraphExecutor() {
        var config = AppConfigReader.getInstance();
        var deployLocation = config.getProperty("location.graph.deployed", DEFAULT_DEPLOY_DIR);
        if (deployLocation.startsWith(FILE_PREFIX) || deployLocation.startsWith(CLASSPATH_PREFIX)) {
            this.deployedGraphLocation = deployLocation;
        } else {
            log.error("location.graph.temp must start with file:/ or classpath:/. Fallback to {}", DEFAULT_DEPLOY_DIR);
            this.deployedGraphLocation = DEFAULT_DEPLOY_DIR;
        }
        log.info("Deployed graph model folder (location.graph.deployed) - {}", this.deployedGraphLocation);
    }

    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope event, int instance) throws InterruptedException {
        if (event.getReplyTo() != null && event.getCorrelationId() != null) {
            var po = PostOffice.trackable(headers, instance);
            var graphInstance = create(headers);
            try {
                var instanceId = headers.get("instance");
                var flowInstance = Flows.getFlowInstance(instanceId);
                traverseAndExecute(po, instanceId, flowInstance, graphInstance, event);
                if (graphInstance.complete.get()) {
                    // send task completion signal to Flow's TaskExecutor
                    return new EventEnvelope().setStatus(STATUS_CONTINUE).setBody(DONE);
                }
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                var rc = e instanceof AppException ex? ex.getStatus() : 400;
                if (rc != STATUS_CONTINUE || !DONE.equals(e.getMessage())) {
                    var error = new EventEnvelope().setTo(event.getReplyTo()).setStatus(rc).setBody(e.getMessage())
                            .setCorrelationId(event.getCorrelationId());
                    po.send(error);
                }
            }
        }
        throw new IllegalArgumentException("Graph instance does not resolve to an outcome");
    }

    private GraphInstance create(Map<String, String> headers) {
        var instanceId = headers.get("instance");
        if (instanceId == null) {
            throw new IllegalArgumentException("Missing instance ID in header");
        }
        var graphId = headers.get("graph");
        if (graphId == null) {
            throw new IllegalArgumentException("Missing graph ID in header");
        }
        var flowInstance = Flows.getFlowInstance(instanceId);
        if (flowInstance == null) {
            throw new IllegalArgumentException("Invalid flow instance " + instanceId);
        }
        flowInstance.setEndFlowListeners(GraphHousekeeper.ROUTE);
        var map = getGraphModel(graphId);
        if (map.isEmpty()) {
            throw new IllegalArgumentException("Unable to load graph model '"+graphId+"' - missing or invalid");
        }
        GraphInstance graphInstance = new GraphInstance(graphId);
        var graph = graphInstance.graph;
        graph.importGraph(map);
        graphInstances.put(instanceId, graphInstance);
        return graphInstance;
    }

    @SuppressWarnings("unchecked")
    private void traverseAndExecute(PostOffice po, String instanceId, FlowInstance flowInstance,
                                    GraphInstance graphInstance, EventEnvelope event)
            throws ExecutionException, InterruptedException {
        var stateMachine = graphInstance.stateMachine;
        var graph = graphInstance.graph;
        // make a copy of flow input and model to avoid accidentally changing the original values
        var inputCopy = util.deepCopy((Map<String, Object>) flowInstance.dataset.get(INPUT));
        var modelCopy = util.deepCopy((Map<String, Object>) flowInstance.dataset.get(MODEL));
        stateMachine.setElement(INPUT, inputCopy);
        stateMachine.setElement(MODEL, modelCopy);
        // map node properties to state machine
        initializeWithNodeProperties(graphInstance);
        var root = graph.getRootNode();
        if (root == null) {
            throw new IllegalArgumentException("Root node does not exist");
        }
        var end = graph.getEndNode();
        if (end == null) {
            throw new IllegalArgumentException("End node does not exist");
        }
        walk(po, instanceId, event, graphInstance, root, getModelTtl(graphInstance));
    }

    private void walk(PostOffice po, String in, EventEnvelope event,
                      GraphInstance graphInstance, SimpleNode node, long timeout)
            throws ExecutionException, InterruptedException {
        if (!graphInstance.complete.get()) {
            var nodeName = node.getAlias();
            var properties = node.getProperties();
            String skill = properties.containsKey(SKILL) ? String.valueOf(properties.get(SKILL)) : null;
            var seen = graphInstance.hasSeen.get(nodeName);
            if (seen == null) {
                if (!GraphJoin.ROUTE.equals(skill)) {
                    graphInstance.hasSeen.put(nodeName, true);
                }
                walkTo(po, in, event, skill, graphInstance, node, timeout);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void walkTo(PostOffice po, String in, EventEnvelope event, String skill, GraphInstance graphInstance,
                        SimpleNode node, long timeout) throws ExecutionException, InterruptedException {
        var graph = graphInstance.graph;
        var endNode = graph.getEndNode();
        if (endNode.getId().equals(node.getId())) {
            if (skill != null) {
                execute(po, skill, in, event, graphInstance, node, timeout);
            }
            var body = graphInstance.stateMachine.getElement(OUTPUT_BODY_NAMESPACE);
            var hdr = graphInstance.stateMachine.getElement(OUTPUT_HEADER_NAMESPACE);
            var headers = hdr instanceof Map? (Map<String, Object>) hdr : new HashMap<String, Object>();
            var response = new EventEnvelope().setTo(event.getReplyTo()).setCorrelationId(event.getCorrelationId());
            for (Map.Entry<String, Object> kv : headers.entrySet()) {
                response.setHeader(kv.getKey(), kv.getValue());
            }
            po.send(response.setBody(body));
            graphInstance.complete.set(true);
        } else {
            if (skill == null) {
                walkNext(po, in, event, graphInstance, node, timeout);
            } else {
                execute(po, skill, in, event, graphInstance, node, timeout);
            }
        }
    }

    private void execute(PostOffice po, String skill, String in, EventEnvelope event, GraphInstance graphInstance,
                         SimpleNode node, long timeout) throws ExecutionException, InterruptedException {
        if (po.exists(skill)) {
            var nodeName = node.getAlias();
            var response = po.request(new EventEnvelope().setTo(skill).setHeader(IN, in).setHeader(LIVE, true)
                                        .setHeader(TYPE, EXECUTE).setHeader(NODE, nodeName), timeout).get();
            // check processing status
            var stateMachine = graphInstance.stateMachine;
            if (response.hasError()) {
                handleException(po, stateMachine, event, response);
                // tell executor that response has been sent
                graphInstance.complete.set(true);
                throw new AppException(STATUS_CONTINUE, DONE);
            }
            // if skill handler does not throw exception, it can also set status and error in its node properties
            var processStatus = stateMachine.getElement(nodeName + "." + STATUS);
            var resultError = stateMachine.getElement(nodeName + "." + ERROR);
            if (processStatus instanceof Integer rc && resultError != null) {
                var error = new EventEnvelope().setTo(event.getReplyTo()).setCorrelationId(event.getCorrelationId())
                        .setBody(resultError).setStatus(rc);
                po.send(error);
                // tell executor that response has been sent
                graphInstance.complete.set(true);
                throw new AppException(STATUS_CONTINUE, DONE);
            }
            if (!graphInstance.complete.get()) {
                var graph = graphInstance.graph;
                var endNode = graph.getEndNode();
                if (!endNode.getId().equals(node.getId())) {
                    var next = String.valueOf(response.getBody());
                    nextOrJump(po, in, event, graphInstance, node, next, timeout);
                }
            }
        } else {
            throw new IllegalArgumentException("Skill " + skill + " does not exist");
        }
    }

    @SuppressWarnings("unchecked")
    private void handleException(PostOffice po, MultiLevelMap stateMachine, EventEnvelope event, EventEnvelope response) {
        var ex = response.getException();
        if (ex instanceof FetchException) {
            var status = stateMachine.getElement(OUTPUT_NAMESPACE+STATUS);
            var rc = status instanceof Number number? number.intValue() : response.getStatus();
            var headers = stateMachine.getElement(OUTPUT_HEADER_NAMESPACE);
            var body = stateMachine.getElement(OUTPUT_BODY_NAMESPACE);
            var error = new EventEnvelope().setTo(event.getReplyTo()).setStatus(rc)
                    .setCorrelationId(event.getCorrelationId());
            error.setBody(body == null? response.getBody() : body);
            if (headers instanceof Map) {
                error.setHeaders((Map<String, String>) headers);
            }
            po.send(error);
        } else {
            var error = new EventEnvelope().setTo(event.getReplyTo()).setCorrelationId(event.getCorrelationId())
                    .setBody(response.getBody()).setStatus(response.getStatus());
            po.send(error);
        }
    }

    private void nextOrJump(PostOffice po, String in, EventEnvelope event, GraphInstance graphInstance, SimpleNode node,
                            String next, long timeout) throws ExecutionException, InterruptedException {
        if (!SINK.equals(next)) {
            var graph = graphInstance.graph;
            if (NEXT.equals(next)) {
                walkNext(po, in, event, graphInstance, node, timeout);
            } else {
                var nextNode = graph.findNodeByAlias(next);
                if (nextNode != null) {
                    walk(po, in, event, graphInstance, nextNode, timeout);
                } else {
                    throw new IllegalArgumentException("Next node '" + next + "' does not exist");
                }
            }
        }
    }

    private void walkNext(PostOffice po, String in, EventEnvelope event, GraphInstance graphInstance,
                          SimpleNode node, long timeout)
            throws ExecutionException, InterruptedException {
        if (!graphInstance.complete.get()) {
            var graph = graphInstance.graph;
            var nodes = graph.getForwardLinks(node.getAlias());
            for (SimpleNode next : nodes) {
                walk(po, in, event, graphInstance, next, timeout);
            }
        }
    }

    private Map<String, Object> getGraphModel(String graphId) {
        // use config reader to resolve environment variables
        var reader = new ConfigReader(getNormalizedPath(deployedGraphLocation, graphId));
        return reader.getMap();
    }
}
