package com.accenture.minigraph.services;

import com.accenture.minigraph.base.GraphLambdaFunction;
import com.accenture.minigraph.models.GraphInstance;
import com.accenture.minigraph.skills.GraphJoin;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.system.PostOffice;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@PreLoad(route = GraphTraveler.ROUTE, instances=10)
public class GraphTraveler extends GraphLambdaFunction {
    public static final String ROUTE = "graph.playground.traveler";
    @Override
    public Void handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) throws Exception {
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
        var stateMachine = graphInstance.stateMachine;
        var ttl = util.str2int(String.valueOf(stateMachine.getElement("model.ttl", "10")));
        var timeout = Math.max(5, ttl) * 1000L;
        walk(po, in, out, graphInstance, root, timeout);
        return null;
    }

    private void walk(PostOffice po, String in, String out, GraphInstance graphInstance, SimpleNode node, long timeout)
            throws ExecutionException, InterruptedException {
        var nodeName = node.getAlias();
        var properties = node.getProperties();
        var skill = properties.get(SKILL);
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

    private void walkTo(PostOffice po, String in, String out, Object skill, GraphInstance graphInstance,
                        SimpleNode node, long timeout) throws ExecutionException, InterruptedException {
        var graph = graphInstance.graph;
        var endNode = graph.getEndNode();
        if (endNode.getId().equals(node.getId())) {
            po.send(new EventEnvelope().setTo(out).setBody("Graph traversal completed"));
        } else {
            if (skill == null) {
                walkNext(po, in, out, graphInstance, node, timeout);
            } else {
                // execute the node
                var skillRoute = String.valueOf(skill);
                if (po.exists(skillRoute)) {
                    execute(po, skillRoute, in, out, graphInstance, node, timeout);
                } else {
                    throw new IllegalArgumentException("Skill " + skill + " does not exist");
                }
            }
        }
    }

    private void execute(PostOffice po, String skillRoute, String in, String out, GraphInstance graphInstance,
                         SimpleNode node, long timeout) throws ExecutionException, InterruptedException {
        var nodeName = node.getAlias();
        po.send(new EventEnvelope().setTo(out).setBody("Execute " + nodeName + " with skill " + skillRoute));
        var response = po.request(new EventEnvelope().setTo(skillRoute).setHeader(IN, in)
                .setHeader(TYPE, EXECUTE).setHeader(NODE, nodeName), timeout).get();
        if (response.hasError()) {
            po.send(new EventEnvelope().setTo(out)
                    .setStatus(response.getStatus()).setBody(response.getBody()));
        } else {
            var next = String.valueOf(response.getBody());
            nextOrJump(po, in, out, graphInstance, node, next, timeout);
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
                    throw new IllegalArgumentException("Next node '" + next + "' does not exist");
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
