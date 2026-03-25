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
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.annotations.ZeroTracing;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.system.PostOffice;

import java.util.Map;

@OptionalService("app.env=dev")
@ZeroTracing
@EventInterceptor
@PreLoad(route = GraphTraveler.ROUTE, instances=300)
public class GraphTraveler extends GraphLambdaFunction {
    public static final String ROUTE = "graph.traveler";

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
            var in = headers.get(IN);
            var graphInstance = getGraphInstance(in);
            graphInstance.setWsInstance(in);
            graphInstance.setCorrelationId(event.getCorrelationId());
            graphInstance.setReplyTo(event.getReplyTo());
            graphInstance.hasSeen.clear();
            graphInstance.complete.set(false);
            beginTraversal(po, graphInstance);
        } catch (Exception e) {
            var rc = e instanceof AppException ex? ex.getStatus() : 400;
            var error = new EventEnvelope().setTo(event.getReplyTo()).setStatus(rc).setBody(e.getMessage())
                                                .setCorrelationId(event.getCorrelationId());
            po.send(error);
        }
    }

    private void beginTraversal(PostOffice po, GraphInstance graphInstance) {
        var graph = graphInstance.graph;
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
        var wsInstance = compositeId.substring(0, at);
        var nodeName = compositeId.substring(at+1);
        var graphInstance = graphInstances.get(wsInstance);
        if (graphInstance != null) {
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
                sendError(po, graphInstance, "Graph traversal aborted");
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
            var out = graphInstance.getReplyTo();
            if (seen == null) {
                if (skill == null) {
                    graphInstance.hasSeen.put(nodeName, true);
                }
                po.send(new EventEnvelope().setTo(out).setBody("Walk to " + nodeName));
                walkTo(po, skill, graphInstance, node);
            } else {
                po.send(new EventEnvelope().setTo(out).setBody("I have seen '" + nodeName +"'"));
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

    private void executionComplete(PostOffice po, GraphInstance graphInstance) {
        var out = graphInstance.getReplyTo();
        var body = graphInstance.stateMachine.getElement(OUTPUT_BODY_NAMESPACE);
        var response = new EventEnvelope().setTo(out).setCorrelationId(graphInstance.getCorrelationId());
        po.send(response.setBody(body));
        graphInstance.complete.set(true);
        po.send(new EventEnvelope().setTo(out).setBody("Graph traversal completed"));
    }

    private void executeSkill(PostOffice po, String skill, GraphInstance graphInstance, SimpleNode node) {
        if (po.exists(skill)) {
            var wsInstanceId = graphInstance.getWsInstance();
            var nodeName = node.getAlias();
            var compositeId = wsInstanceId + "@" + nodeName;
            var out = graphInstance.getReplyTo();
            po.send(new EventEnvelope().setTo(out).setBody("Execute " + nodeName + " with skill " + skill));
            po.send(new EventEnvelope().setTo(skill).setHeader(IN, wsInstanceId)
                    .setHeader(LIVE, true).setHeader(TYPE, EXECUTE).setHeader(NODE, nodeName)
                    .setReplyTo(GraphTraveler.ROUTE).setCorrelationId(compositeId));
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

    private void handleErrorResponse(PostOffice po, GraphInstance graphInstance, EventEnvelope response) {
        var out = graphInstance.getReplyTo();
        var ex = response.getException();
        if (ex instanceof FetchException) {
            var stateMachine = graphInstance.stateMachine;
            var status = stateMachine.getElement(OUTPUT_NAMESPACE+STATUS);
            var rc = status instanceof Number number? number.intValue() : response.getStatus();
            var body = stateMachine.getElement(OUTPUT_BODY_NAMESPACE);
            var error = new EventEnvelope().setTo(out).setStatus(rc).setCorrelationId(graphInstance.getCorrelationId());
            error.setBody(body == null? response.getBody() : body);
            po.send(error);
        } else {
            var error = new EventEnvelope().setTo(out).setCorrelationId(graphInstance.getCorrelationId())
                                .setBody(response.getBody()).setStatus(response.getStatus());
            po.send(error);
        }
        sendError(po, graphInstance, "Graph traversal aborted");
    }

    private void sendError(PostOffice po, GraphInstance graphInstance, String message) {
        graphInstance.complete.set(true);
        var error = new EventEnvelope().setTo(graphInstance.getReplyTo())
                            .setCorrelationId(graphInstance.getCorrelationId()).setBody(message).setStatus(400);
        po.send(error);
    }
}
