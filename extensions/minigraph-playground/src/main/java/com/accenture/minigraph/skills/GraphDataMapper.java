package com.accenture.minigraph.skills;

import com.accenture.minigraph.base.GraphLambdaFunction;
import com.accenture.minigraph.models.GraphInstance;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.graph.MiniGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@PreLoad(route = GraphDataMapper.ROUTE, instances=200)
public class GraphDataMapper extends GraphLambdaFunction {
    public static final String ROUTE = "graph.data.mapper";
    private static final Logger log = LoggerFactory.getLogger(GraphDataMapper.class);

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (!EXECUTE.equals(headers.get(TYPE))) {
            throw new IllegalArgumentException("Type must be EXECUTE");
        }
        var in = headers.get(IN);
        var nodeName = headers.getOrDefault(NODE, "none");
        var graphInstance = graphInstances.get(in);
        if (graphInstance == null) {
            throw new IllegalArgumentException("Graph instance " + in + NOT_FOUND);
        }
        var node = graphInstance.graph.findNodeByAlias(nodeName);
        if (node == null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + NOT_FOUND);
        }
        var properties = node.getProperties();
        if (!ROUTE.equals(properties.get(SKILL))) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have skill - "+ROUTE);
        }
        var mapping = properties.get("mapping");
        if (mapping instanceof List<?> entries) {
            for (Object entry : entries) {
                handleDataMappingEntry(nodeName, String.valueOf(entry), graphInstance);
            }
        } else {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have data mapping entries");
        }
        return null;
    }

    private void handleDataMappingEntry(String nodeName, String command, GraphInstance graphInstance) {
        int sep = command.indexOf(MAP_TO);
        if (sep > 0) {
            var graph = graphInstance.graph;
            var stateMachine = graphInstance.stateMachine;
            var lhs = command.substring(0, sep).trim();
            var rhs = command.substring(sep + MAP_TO.length()).trim();
            var value = helper.getLhsOrConstant(lhs, stateMachine);
            if (!validRhs(rhs, graph)) {
                throw new IllegalArgumentException(NODE_NAME + nodeName + " Invalid mapping '"+command+"'");
            }
            if (value != null) {
                stateMachine.setElement(rhs, value);
            } else {
                if (rhs.endsWith("]") && rhs.contains("[")) {
                    stateMachine.setElement(rhs, null);
                } else {
                    stateMachine.removeElement(rhs);
                }
            }
        }
    }

    private boolean validRhs(String rhs, MiniGraph graph) {
        if (rhs.startsWith(OUTPUT_ARRAY) || rhs.startsWith(OUTPUT_NAMESPACE) || rhs.startsWith(MODEL_NAMESPACE)) {
            return true;
        }
        if (rhs.startsWith(".") || !rhs.contains(".")) {
            return false;
        }
        var alias = rhs.substring(0, rhs.indexOf("."));
        var node = graph.findNodeByAlias(alias);
        return node != null;
    }
}
