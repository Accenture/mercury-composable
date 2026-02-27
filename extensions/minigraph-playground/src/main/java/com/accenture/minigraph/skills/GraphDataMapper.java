package com.accenture.minigraph.skills;

import com.accenture.minigraph.base.GraphLambdaFunction;
import org.platformlambda.core.annotations.PreLoad;

import java.util.List;
import java.util.Map;

@PreLoad(route = GraphDataMapper.ROUTE, instances=200)
public class GraphDataMapper extends GraphLambdaFunction {
    public static final String ROUTE = "graph.data.mapper";

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (!EXECUTE.equals(headers.get(TYPE))) {
            throw new IllegalArgumentException("Type must be EXECUTE");
        }
        var in = headers.get(IN);
        var nodeName = headers.getOrDefault(NODE, "none");
        var graphInstance = getGraphInstance(in);
        var node = getNode(nodeName, graphInstance.graph);
        var properties = node.getProperties();
        if (!ROUTE.equals(properties.get(SKILL))) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have skill - "+ROUTE);
        }
        var mapping = properties.get(MAPPING);
        if (mapping instanceof List<?> entries) {
            for (Object entry : entries) {
                handleDataMappingEntry(nodeName, String.valueOf(entry), graphInstance);
            }
        } else {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have 'mapping' entries");
        }
        return NEXT;
    }
}
