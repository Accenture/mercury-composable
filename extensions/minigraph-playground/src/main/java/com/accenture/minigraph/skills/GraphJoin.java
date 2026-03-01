package com.accenture.minigraph.skills;

import com.accenture.minigraph.base.GraphLambdaFunction;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;

import java.util.Map;

@PreLoad(route = GraphJoin.ROUTE, instances=100)
public class GraphJoin extends GraphLambdaFunction {
    public static final String ROUTE = "graph.join";

    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        var in = headers.get(IN);
        var nodeName = headers.getOrDefault(NODE, "none");
        var graphInstance = getGraphInstance(in);
        var graph = graphInstance.graph;
        var node = getNode(nodeName, graph);
        var properties = node.getProperties();
        if (!ROUTE.equals(properties.get(SKILL))) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have skill - "+ROUTE);
        }
        var connected = graph.getBackwardLinks(nodeName);
        var count = 0;
        for (var from : connected) {
            if (graphInstance.hasSeen.containsKey(from.getAlias())) {
                count++;
            }
        }
        // successful "join" when all the upstream nodes have been seen
        if (count == connected.size()) {
            graphInstance.hasSeen.put(nodeName, true);
            return NEXT;
        } else {
            return SINK;
        }
    }
}
