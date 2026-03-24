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
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.annotations.ZeroTracing;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@ZeroTracing
@EventInterceptor
@PreLoad(route = GraphExecutor.ROUTE, instances=300)
public class GraphExecutor extends GraphLambdaFunction {
    public static final String ROUTE = "graph.executor";
    private static final Logger log = LoggerFactory.getLogger(GraphExecutor.class);
    private static final String DEFAULT_DEPLOY_DIR = "classpath:/graph";
    private static final String INSTANCE = "instance";
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
    public Void handleEvent(Map<String, String> headers, EventEnvelope event, int instance) {
        var po = PostOffice.trackable(headers, instance);
        var cid = event.getCorrelationId();
        if (cid != null) {
            if (cid.contains("@")) {
                handleSkillResponse(po, event);
            } else if (event.getReplyTo() != null) {
                executeGraph(po, headers, event);
            }
        }
        return null;
    }

    private void executeGraph(PostOffice po, Map<String, String> headers, EventEnvelope event) {
        try {
            var graphInstance = createInstance(headers, event.getReplyTo(), event.getCorrelationId());
            var flowInstanceId = headers.get(INSTANCE);
            var flowInstance = Flows.getFlowInstance(flowInstanceId);
            graphInstance.hasSeen.clear();
            beginTraversal(po, flowInstance, graphInstance);
        } catch (Exception e) {
            var rc = e instanceof AppException ex? ex.getStatus() : 400;
            var error = new EventEnvelope().setTo(event.getReplyTo()).setStatus(rc).setBody(e.getMessage())
                    .setCorrelationId(event.getCorrelationId());
            po.send(error);
        }
    }

    private GraphInstance createInstance(Map<String, String> headers, String replyTo, String cid) {
        var flowInstanceId = headers.get(INSTANCE);
        if (flowInstanceId == null) {
            throw new IllegalArgumentException("Missing instance ID in header");
        }
        var graphId = headers.get(GRAPH);
        if (graphId == null) {
            throw new IllegalArgumentException("Missing graph ID in header");
        }
        var flowInstance = Flows.getFlowInstance(flowInstanceId);
        if (flowInstance == null) {
            throw new IllegalArgumentException("Invalid flow instance " + flowInstanceId);
        }
        flowInstance.setEndFlowListeners(GraphHousekeeper.ROUTE);
        var map = getGraphModel(graphId);
        if (map.isEmpty()) {
            throw new IllegalArgumentException("Unable to load graph model '"+graphId+"' - missing or invalid");
        }
        GraphInstance graphInstance = new GraphInstance(graphId);
        graphInstance.setFlowInstanceId(flowInstanceId);
        graphInstance.setCorrelationId(cid);
        graphInstance.setReplyTo(replyTo);
        var graph = graphInstance.graph;
        graph.importGraph(map);
        graphInstances.put(flowInstanceId, graphInstance);
        return graphInstance;
    }

    @SuppressWarnings("unchecked")
    private void beginTraversal(PostOffice po, FlowInstance flowInstance, GraphInstance graphInstance) {
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
        walk(po, graphInstance, root);
    }

    private void handleSkillResponse(PostOffice po, EventEnvelope response) {
        var compositeId = response.getCorrelationId();
        var at = compositeId.indexOf('@');
        var flowInstanceId = compositeId.substring(0, at);
        var nodeName = compositeId.substring(at+1);
        var graphInstance = graphInstances.get(flowInstanceId);
        var flowInstance = Flows.getFlowInstance(flowInstanceId);
        if (graphInstance != null && flowInstance != null) {
            var stateMachine = graphInstance.stateMachine;
            if (response.hasError()) {
                handleErrorResponse(po, graphInstance, response);
                return;
            }
            var graph = graphInstance.graph;
            var node = graph.findNodeByAlias(nodeName);
            var skill = node.getProperty(SKILL);
            // Except "graph.join", mark node as seen.
            // The "graph.join" node itself will mark "hasSeen" only when all joining paths are done.
            if (skill != null && !skill.equals(GraphJoin.ROUTE)) {
                graphInstance.hasSeen.put(nodeName, true);
            }
            // Skill handler can also set status and error in its node properties instead of throwing exception
            var processStatus = stateMachine.getElement(nodeName + "." + STATUS);
            var resultError = stateMachine.getElement(nodeName + "." + ERROR);
            if (processStatus instanceof Integer rc && resultError != null) {
                var replyTo = graphInstance.getReplyTo();
                var cid = graphInstance.getCorrelationId();
                var error = new EventEnvelope().setTo(replyTo).setCorrelationId(cid).setBody(resultError).setStatus(rc);
                po.send(error);
                graphInstance.complete.set(true);
            } else if (!graphInstance.complete.get()) {
                var endNode = graph.getEndNode();
                if (endNode.getId().equals(node.getId())) {
                    executionComplete(po, graphInstance);
                } else {
                    var next = String.valueOf(response.getBody());
                    nextOrJump(po, graphInstance, node, next);
                }
            }
        }
    }

    private void walk(PostOffice po, GraphInstance graphInstance, SimpleNode node) {
        if (!graphInstance.complete.get()) {
            var nodeName = node.getAlias();
            String skill = node.getProperty(SKILL) != null ? String.valueOf(node.getProperty(SKILL)) : null;
            var seen = graphInstance.hasSeen.get(nodeName);
            if (seen == null) {
                if (skill == null) {
                    graphInstance.hasSeen.put(nodeName, true);
                }
                walkTo(po, skill, graphInstance, node);
            }
        }
    }

    private void walkTo(PostOffice po, String skill, GraphInstance graphInstance, SimpleNode node) {
        var graph = graphInstance.graph;
        var endNode = graph.getEndNode();
        if (endNode.getId().equals(node.getId())) {
            if (skill != null) {
                executeSkill(po, skill, graphInstance, node);
            } else {
                executionComplete(po, graphInstance);
            }
        } else {
            if (skill != null) {
                executeSkill(po, skill, graphInstance, node);
            } else {
                walkNext(po, graphInstance, node);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void executionComplete(PostOffice po, GraphInstance graphInstance) {
        var body = graphInstance.stateMachine.getElement(OUTPUT_BODY_NAMESPACE);
        var hdr = graphInstance.stateMachine.getElement(OUTPUT_HEADER_NAMESPACE);
        var headers = hdr instanceof Map ? (Map<String, Object>) hdr : new HashMap<String, Object>();
        var response = new EventEnvelope().setTo(graphInstance.getReplyTo())
                .setCorrelationId(graphInstance.getCorrelationId());
        for (Map.Entry<String, Object> kv : headers.entrySet()) {
            response.setHeader(kv.getKey(), kv.getValue());
        }
        po.send(response.setBody(body));
        graphInstance.complete.set(true);
    }

    private void executeSkill(PostOffice po, String skill, GraphInstance graphInstance, SimpleNode node) {
        if (po.exists(skill)) {
            var flowInstanceId = graphInstance.getFlowInstanceId();
            var nodeName = node.getAlias();
            var compositeId = flowInstanceId + "@" + nodeName;
            po.send(new EventEnvelope().setTo(skill).setHeader(IN, flowInstanceId)
                    .setHeader(LIVE, true).setHeader(TYPE, EXECUTE).setHeader(NODE, nodeName)
                    .setReplyTo(GraphExecutor.ROUTE).setCorrelationId(compositeId));
        } else {
            sendError(po, graphInstance, "Skill " + skill + " does not exist");
        }
    }

    private void nextOrJump(PostOffice po, GraphInstance graphInstance, SimpleNode node, String next) {
        if (!SINK.equals(next)) {
            var graph = graphInstance.graph;
            if (NEXT.equals(next)) {
                walkNext(po, graphInstance, node);
            } else {
                var nextNode = graph.findNodeByAlias(next);
                if (nextNode != null) {
                    walk(po, graphInstance, nextNode);
                } else {
                    sendError(po, graphInstance, "Next node '" + next + "' does not exist");
                }
            }
        }
    }

    private void walkNext(PostOffice po, GraphInstance graphInstance, SimpleNode node) {
        if (!graphInstance.complete.get()) {
            var graph = graphInstance.graph;
            var nodes = graph.getForwardLinks(node.getAlias());
            for (SimpleNode next : nodes) {
                walk(po, graphInstance, next);
            }
        }
    }

    private Map<String, Object> getGraphModel(String graphId) {
        // use config reader to resolve environment variables
        var reader = new ConfigReader(getNormalizedPath(deployedGraphLocation, graphId));
        return reader.getMap();
    }

    @SuppressWarnings("unchecked")
    private void handleErrorResponse(PostOffice po, GraphInstance graphInstance, EventEnvelope response) {
        var ex = response.getException();
        if (ex instanceof FetchException) {
            var stateMachine = graphInstance.stateMachine;
            var status = stateMachine.getElement(OUTPUT_NAMESPACE+STATUS);
            var rc = status instanceof Number number? number.intValue() : response.getStatus();
            var headers = stateMachine.getElement(OUTPUT_HEADER_NAMESPACE);
            var body = stateMachine.getElement(OUTPUT_BODY_NAMESPACE);
            var error = new EventEnvelope().setTo(graphInstance.getReplyTo()).setStatus(rc)
                    .setCorrelationId(graphInstance.getCorrelationId());
            error.setBody(body == null? response.getBody() : body);
            if (headers instanceof Map) {
                error.setHeaders((Map<String, String>) headers);
            }
            po.send(error);
        } else {
            var error = new EventEnvelope().setTo(graphInstance.getReplyTo())
                    .setCorrelationId(graphInstance.getCorrelationId())
                    .setBody(response.getBody()).setStatus(response.getStatus());
            po.send(error);
        }
        graphInstance.complete.set(true);
    }

    private void sendError(PostOffice po, GraphInstance graphInstance, String message) {
        var error = new EventEnvelope().setTo(graphInstance.getReplyTo())
                .setCorrelationId(graphInstance.getCorrelationId())
                .setBody(message).setStatus(400);
        po.send(error);
        graphInstance.complete.set(true);
    }
}
