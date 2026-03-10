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
import com.accenture.minigraph.models.GraphInstance;
import com.accenture.minigraph.skills.GraphJoin;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.system.PostOffice;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@PreLoad(route = GraphTraveler.ROUTE, instances=100)
public class GraphTraveler extends GraphLambdaFunction {
    public static final String ROUTE = "graph.playground.traveler";

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope input, int instance) throws Exception {
        var po = new PostOffice(headers, instance);
        var in = headers.get(IN);
        var out = headers.get(OUT);
        var graphInstance = getGraphInstance(in);
        var graph = graphInstance.graph;
        var root = graph.getRootNode();
        if (root == null) {
            throw new IllegalArgumentException("Root node does not exist");
        }
        var end = graph.getEndNode();
        if (end == null) {
            throw new IllegalArgumentException("End node does not exist");
        }
        graphInstance.hasSeen.clear();
        walk(po, in, out, graphInstance, root, getModelTtl(graphInstance));
        return null;
    }

    private void walk(PostOffice po, String in, String out, GraphInstance graphInstance, SimpleNode node, long timeout)
            throws ExecutionException, InterruptedException {
        var nodeName = node.getAlias();
        var properties = node.getProperties();
        String skill = properties.containsKey(SKILL)? String.valueOf(properties.get(SKILL)) : null;
        var seen = graphInstance.hasSeen.get(nodeName);
        if (seen == null) {
            if (!GraphJoin.ROUTE.equals(skill)) {
                graphInstance.hasSeen.put(nodeName, true);
            }
            po.send(new EventEnvelope().setTo(out).setBody("Walk to " + nodeName));
            walkTo(po, in, out, skill, graphInstance, node, timeout);
        } else {
            po.send(new EventEnvelope().setTo(out).setBody("I have seen '" + nodeName +"'"));
        }
    }

    private void walkTo(PostOffice po, String in, String out, String skill, GraphInstance graphInstance,
                        SimpleNode node, long timeout) throws ExecutionException, InterruptedException {
        var graph = graphInstance.graph;
        var endNode = graph.getEndNode();
        if (endNode.getId().equals(node.getId())) {
            if (skill != null) {
                execute(po, skill, in, out, graphInstance, node, timeout);
            }
            po.send(new EventEnvelope().setTo(out).setBody("Graph traversal completed"));
        } else {
            if (skill == null) {
                walkNext(po, in, out, graphInstance, node, timeout);
            } else {
                execute(po, skill, in, out, graphInstance, node, timeout);
            }
        }
    }

    private void execute(PostOffice po, String skill, String in, String out, GraphInstance graphInstance,
                         SimpleNode node, long timeout) throws ExecutionException, InterruptedException {
        if (po.exists(skill)) {
            var nodeName = node.getAlias();
            po.send(new EventEnvelope().setTo(out).setBody("Execute " + nodeName + " with skill " + skill));
            var response = po.request(new EventEnvelope().setTo(skill).setHeader(IN, in)
                    .setHeader(TYPE, EXECUTE).setHeader(NODE, nodeName), timeout).get();
            if (response.hasError()) {
                po.send(new EventEnvelope().setTo(out)
                        .setStatus(response.getStatus()).setBody(response.getBody()));
                throw new IllegalArgumentException("Graph traversal aborted");
            } else {
                var graph = graphInstance.graph;
                var endNode = graph.getEndNode();
                if (!endNode.getId().equals(node.getId())) {
                    var next = String.valueOf(response.getBody());
                    nextOrJump(po, in, out, graphInstance, node, next, timeout);
                }
            }
        } else {
            throw new IllegalArgumentException("Skill " + skill + " does not exist");
        }
    }

    private void nextOrJump(PostOffice po, String in, String out, GraphInstance graphInstance, SimpleNode node,
                            String next, long timeout) throws ExecutionException, InterruptedException {
        if (!SINK.equals(next)) {
            var graph = graphInstance.graph;
            if (NEXT.equals(next)) {
                walkNext(po, in, out, graphInstance, node, timeout);
            } else {
                var nextNode = graph.findNodeByAlias(next);
                if (nextNode != null) {
                    walk(po, in, out, graphInstance, nextNode, timeout);
                } else {
                    po.send(new EventEnvelope().setTo(out).setBody("Next node '" + next + "' does not exist"));
                }
            }
        }
    }

    private void walkNext(PostOffice po, String in, String out, GraphInstance graphInstance,
                          SimpleNode node, long timeout)
            throws ExecutionException, InterruptedException {
        var graph = graphInstance.graph;
        var nodes = graph.getForwardLinks(node.getAlias());
        for (SimpleNode next : nodes) {
            walk(po, in, out, graphInstance, next, timeout);
        }
    }
}
