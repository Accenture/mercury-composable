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
import com.accenture.minigraph.models.CompiledGraphs;
import com.accenture.minigraph.models.GraphInstance;
import com.accenture.minigraph.models.Visits;
import com.accenture.minigraph.skills.GraphJoin;
import com.accenture.models.FlowInstance;
import com.accenture.models.Flows;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.annotations.ZeroTracing;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
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
    private static final String INSTANCE = "instance";
    private final boolean isDevEnv;

    public GraphExecutor() {
        var config = AppConfigReader.getInstance();
        this.isDevEnv = "dev".equals(config.getProperty("app.env", "dev"));
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
        // The span that triggered this graph (stamped by the event-script task executor) is the
        // parent span for the graph's first node, establishing OTel lineage into the graph.
        var parentSpanId = event.getSpanId();
        try {
            var graphInstance = createInstance(headers, event.getReplyTo(), event.getCorrelationId());
            var flowInstanceId = headers.get(INSTANCE);
            var flowInstance = Flows.getFlowInstance(flowInstanceId);
            beginTraversal(po, flowInstance, graphInstance, parentSpanId);
        } catch (Exception e) {
            var rc = e instanceof AppException ex? ex.getStatus() : 400;
            var error = new EventEnvelope().setTo(event.getReplyTo()).setStatus(rc).setBody(e.getMessage())
                    .setCorrelationId(event.getCorrelationId()).setSpanId(parentSpanId);
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
    private void beginTraversal(PostOffice po, FlowInstance flowInstance, GraphInstance graphInstance, String parentSpanId) {
        var stateMachine = graphInstance.stateMachine;
        var graph = graphInstance.graph;
        // make a copy of flow input and model to avoid accidentally changing the original values
        var inputCopy = util.deepCopy((Map<String, Object>) flowInstance.dataset.get(INPUT));
        var modelCopy = util.deepCopy((Map<String, Object>) flowInstance.dataset.get(MODEL));
        stateMachine.setElement(INPUT, inputCopy);
        stateMachine.setElement(MODEL, modelCopy);
        // map node properties to state machine
        initializeWithNodeProperties(graphInstance);
        // a compiled model is guaranteed to have root and end nodes (the CompileGraph
        // quality gate is the only door to deployed execution) - no per-request
        // structural re-validation; the dry-run walker keeps its own checks because
        // playground drafts never pass the gate
        walk(po, graphInstance, graph.getRootNode(), null, parentSpanId);
    }

    private void handleSkillResponse(PostOffice po, EventEnvelope response) {
        var compositeId = response.getCorrelationId();
        var at = compositeId.indexOf('@');
        var flowInstanceId = compositeId.substring(0, at);
        var nodeName = compositeId.substring(at+1);
        var graphInstance = graphInstances.get(flowInstanceId);
        var flowInstance = Flows.getFlowInstance(flowInstanceId);
        if (graphInstance != null && flowInstance != null) {
            // The completed node's own span (stamped on its reply by WorkerHandler) is the parent
            // span for whatever node this callback dispatches next.
            var parentSpanId = response.getSpanId();
            var stateMachine = graphInstance.stateMachine;
            var target = stateMachine.getElement(nodeName + "." + TARGET);
            // Unrecoverable error from the node itself
            if (response.hasError()) {
                if (target != null) {
                    var eMap = getErrorMap(stateMachine.getElement(OUTPUT_BODY), target);
                    stateMachine.setElement(OUTPUT_BODY, eMap);
                }
                handleErrorResponse(po, graphInstance, response, parentSpanId);
                return;
            }
            var graph = graphInstance.graph;
            var node = graph.findNodeByAlias(nodeName);
            checkFrequency(po, graphInstance, nodeName, parentSpanId);
            // Skill handler can also set status and error in its node properties instead of throwing exception
            var processStatus = stateMachine.getElement(nodeName + "." + STATUS);
            var resultError = stateMachine.getElement(nodeName + "." + ERROR);
            // Mark the skill complete only when it did NOT fail (status + error set,
            // e.g. an exception-routed fetcher): a join barrier consults skillRun,
            // so a failed branch must not satisfy the barrier while it retries.
            // GraphTraveler keeps identical semantics.
            if (!(processStatus instanceof Integer && resultError != null)) {
                graphInstance.skillRun.put(nodeName, true);
            }
            // Skill handler would set status and error in its node properties
            // e.g. the HTTP response status code to the API fetcher >= 400
            var errorHandler = node.getProperty(EXCEPTION);
            if (processStatus instanceof Integer rc && resultError != null && errorHandler == null) {
                var errorMap = getErrorMap(resultError, target);
                var replyTo = graphInstance.getReplyTo();
                var cid = graphInstance.getCorrelationId();
                var error = new EventEnvelope().setTo(replyTo).setCorrelationId(cid).setBody(errorMap)
                        .setStatus(rc).setSpanId(parentSpanId);
                po.send(error);
                graphInstance.complete.set(true);
            } else if (!graphInstance.complete.get()) {
                if (processStatus instanceof Integer && resultError != null) {
                    // the node failed and routed to its exception= handler ('next' is the
                    // handler's alias): stage the generic exception context so one handler
                    // can serve any node. GraphTraveler keeps identical semantics.
                    stageErrorContext(stateMachine, nodeName);
                }
                var next = String.valueOf(response.getBody());
                decideNext(po, node, next, graphInstance, parentSpanId);
            }
        }
    }

    private void checkFrequency(PostOffice po, GraphInstance graphInstance, String nodeName, String parentSpanId) {
        var frequency = graphInstance.hits.getOrDefault(nodeName, new Visits());
        var now = System.currentTimeMillis();
        var last = frequency.lastVisit.get();
        if (now - last > getLoopInterval()) {
            frequency.lastVisit.set(now);
            frequency.hits.set(0);
        }
        var total = frequency.hits.incrementAndGet();
        graphInstance.hits.put(nodeName, frequency);
        if (total > getHighFrequency()) {
            log.error("Looping detected - {} hits in {} ms for {} in {}",
                    total, now - last, nodeName, graphInstance.graphId);
            var response = new EventEnvelope().setBody("Node " + nodeName + " executed too frequently").setStatus(400);
            handleErrorResponse(po, graphInstance, response, parentSpanId);
        }
    }

    private void decideNext(PostOffice po, SimpleNode node, String next, GraphInstance graphInstance, String parentSpanId) {
        var graph = graphInstance.graph;
        var endNode = graph.getEndNode();
        if (endNode.getId().equals(node.getId())) {
            executionComplete(po, graphInstance, parentSpanId);
        } else {
            nextOrJump(po, graphInstance, node, next, parentSpanId);
        }
    }

    private void walk(PostOffice po, GraphInstance graphInstance, SimpleNode node, String from, String parentSpanId) {
        if (!graphInstance.complete.get()) {
            var nodeName = node.getAlias();
            String skill = node.getProperty(SKILL) != null ? String.valueOf(node.getProperty(SKILL)) : null;
            // atomic mark-and-test: concurrent branches converging on the same
            // non-join node must not dispatch it twice (a join always evaluates -
            // its barrier logic owns the dedup)
            var isJoin = GraphJoin.ROUTE.equals(skill);
            var seen = graphInstance.nodeSeen.putIfAbsent(nodeName, true) != null;
            if (isJoin || !seen) {
                walkTo(po, skill, graphInstance, node, from, parentSpanId);
            }
        }
    }

    private void walkTo(PostOffice po, String skill, GraphInstance graphInstance, SimpleNode node,
                        String from, String parentSpanId) {
        var graph = graphInstance.graph;
        var endNode = graph.getEndNode();
        if (endNode.getId().equals(node.getId())) {
            if (skill != null) {
                executeSkill(po, skill, graphInstance, node, from, parentSpanId);
            } else {
                executionComplete(po, graphInstance, parentSpanId);
            }
        } else {
            if (skill != null) {
                executeSkill(po, skill, graphInstance, node, from, parentSpanId);
            } else if (hasSuspendEdge(graphInstance.graph, node)) {
                walkToSuspendNode(po, graphInstance, node, parentSpanId);
            } else {
                walkNext(po, graphInstance, node, parentSpanId, false);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void executionComplete(PostOffice po, GraphInstance graphInstance, String parentSpanId) {
        var body = graphInstance.stateMachine.getElement(OUTPUT_BODY);
        var hdr = graphInstance.stateMachine.getElement(OUTPUT_HEADER_NAMESPACE);
        var headers = hdr instanceof Map ? (Map<String, Object>) hdr : new HashMap<String, Object>();
        var response = new EventEnvelope().setTo(graphInstance.getReplyTo())
                .setCorrelationId(graphInstance.getCorrelationId()).setSpanId(parentSpanId);
        // a graph may stage its own HTTP status declaratively, e.g. 'int(404) -> output.status'
        // in a rejection node - the surrounding flow's 'status -> output.status' mapping then
        // carries it to the caller
        var status = graphInstance.stateMachine.getElement(OUTPUT_NAMESPACE + STATUS);
        if (status != null && util.isDigits(String.valueOf(status))) {
            response.setStatus(util.str2int(String.valueOf(status)));
        }
        for (Map.Entry<String, Object> kv : headers.entrySet()) {
            response.setHeader(kv.getKey(), kv.getValue());
        }
        po.send(response.setBody(body));
        graphInstance.complete.set(true);
    }

    private void executeSkill(PostOffice po, String skill, GraphInstance graphInstance, SimpleNode node,
                              String from, String parentSpanId) {
        if (po.exists(skill)) {
            var flowInstanceId = graphInstance.getFlowInstanceId();
            var nodeName = node.getAlias();
            var compositeId = flowInstanceId + "@" + nodeName;
            var event = new EventEnvelope().setTo(skill).setHeader(IN, flowInstanceId)
                    .setHeader(TYPE, EXECUTE).setHeader(NODE, nodeName)
                    .setReplyTo(GraphExecutor.ROUTE).setCorrelationId(compositeId).setSpanId(parentSpanId);
            if (from != null) {
                event.setHeader(FROM, from);
            }
            // The walker is an event interceptor, so the business correlation-id is not
            // auto-propagated by PostOffice. Stamp it from the graph's own model.cid so
            // every skill (and its downstream calls) sees the business id in the
            // my_correlation_id and application log context.
            if (graphInstance.stateMachine.getElement(MODEL_CID) instanceof String businessCid
                    && !businessCid.isBlank()) {
                event.addTag(EventEmitter.BUSINESS_CID_TAG, businessCid.trim());
            }
            po.send(event);
        } else {
            sendError(po, graphInstance, "Skill " + skill + " does not exist", parentSpanId);
        }
    }

    private void nextOrJump(PostOffice po, GraphInstance graphInstance, SimpleNode node, String next, String parentSpanId) {
        if (!SINK.equals(next)) {
            var graph = graphInstance.graph;
            if (next.startsWith(RESUME_PREFIX)) {
                resumeTraversal(po, graphInstance, next.substring(RESUME_PREFIX.length()), parentSpanId);
            } else if (NEXT.equals(next)) {
                if (hasSuspendEdge(graphInstance.graph, node)) {
                    walkToSuspendNode(po, graphInstance, node, parentSpanId);
                } else {
                    walkNext(po, graphInstance, node, parentSpanId, false);
                }
            } else {
                var nextNode = graph.findNodeByAlias(next);
                if (nextNode != null) {
                    walk(po, graphInstance, nextNode, node.getAlias(), parentSpanId);
                } else {
                    sendError(po, graphInstance, "Next node '" + next + "' does not exist", parentSpanId);
                }
            }
        }
    }

    private void walkToSuspendNode(PostOffice po, GraphInstance graphInstance, SimpleNode node, String parentSpanId) {
        // the quality gate already rejected a routing-skill drawn edge to 'suspend', a
        // missing 'suspend' node and a mis-skilled one - a compiled model needs no
        // re-check (GraphTraveler keeps these guards: playground drafts never pass the gate)
        walk(po, graphInstance, graphInstance.graph.findNodeByAlias(SUSPEND), node.getAlias(), parentSpanId);
    }

    private void resumeTraversal(PostOffice po, GraphInstance graphInstance, String alias, String parentSpanId) {
        var resumedNode = graphInstance.graph.findNodeByAlias(alias);
        if (resumedNode == null) {
            sendError(po, graphInstance, "Resumed node '" + alias + "' does not exist", parentSpanId);
        } else if (isJumpModeCheckpoint(graphInstance.graph, alias)) {
            // the decision jumped to the checkpoint: re-execute it against the new
            // request input - its forward links are outcome alternatives, not branches
            // (clear the marks restored from the suspension record so the walk dispatches)
            graphInstance.nodeSeen.remove(alias);
            graphInstance.skillRun.remove(alias);
            walk(po, graphInstance, resumedNode, null, parentSpanId);
        } else {
            // the suspension point (drawn checkpoint edge) already ran before
            // suspension - do not re-execute it; continue along its other forward links
            graphInstance.nodeSeen.put(alias, true);
            graphInstance.skillRun.put(alias, true);
            walkNext(po, graphInstance, resumedNode, parentSpanId, true);
        }
    }

    private void walkNext(PostOffice po, GraphInstance graphInstance, SimpleNode node,
                          String parentSpanId, boolean afterResume) {
        if (!graphInstance.complete.get()) {
            var graph = graphInstance.graph;
            var nodes = graph.getForwardLinks(node.getAlias());
            var deadEnd = true;
            for (SimpleNode next : nodes) {
                // a resumed traversal continues along the normal path, never back into suspension
                if (afterResume && SUSPEND.equals(next.getAlias())) {
                    continue;
                }
                deadEnd = false;
                walk(po, graphInstance, next, node.getAlias(), parentSpanId);
            }
            if (afterResume && deadEnd) {
                sendError(po, graphInstance, "Resumed node '" + node.getAlias() +
                        "' has no forward path to continue", parentSpanId);
            }
        }
    }

    private Map<String, Object> getGraphModel(String graphId) {
        if (graphId.startsWith("tutorial") && !isDevEnv) {
            throw new IllegalArgumentException("tutorial graph models not allowed");
        }
        // deployed graph execution is served exclusively from the compiled registry:
        // a model is executable only when it is listed in the graph manifest and
        // passed the CompileGraph quality gate (the CompileFlows precedent) - a
        // failed or unlisted graph answers 404 as if it does not exist
        var compiled = CompiledGraphs.getGraph(graphId);
        if (compiled == null) {
            throw new AppException(404, graphId + " not found");
        }
        return util.deepCopy(compiled);
    }

    private void handleErrorResponse(PostOffice po, GraphInstance graphInstance, EventEnvelope response, String parentSpanId) {
        var error = new EventEnvelope().setTo(graphInstance.getReplyTo())
                                        .setCorrelationId(graphInstance.getCorrelationId())
                                        .setBody(response.getBody()).setStatus(response.getStatus())
                                        .setSpanId(parentSpanId);
        po.send(error);
        graphInstance.complete.set(true);
    }

    private void sendError(PostOffice po, GraphInstance graphInstance, String message, String parentSpanId) {
        var error = new EventEnvelope().setTo(graphInstance.getReplyTo())
                            .setCorrelationId(graphInstance.getCorrelationId()).setBody(message).setStatus(400)
                            .setSpanId(parentSpanId);
        po.send(error);
        graphInstance.complete.set(true);
    }
}
