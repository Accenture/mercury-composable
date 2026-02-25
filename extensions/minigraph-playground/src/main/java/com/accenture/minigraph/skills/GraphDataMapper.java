package com.accenture.minigraph.skills;

import com.accenture.minigraph.base.GraphLambdaFunction;
import com.accenture.minigraph.models.GraphInstance;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.graph.MiniGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

@PreLoad(route = GraphDataMapper.ROUTE, instances=200)
public class GraphDataMapper extends GraphLambdaFunction {
    public static final String ROUTE = "graph.data.mapper";
    private static final Set<String> RESERVED_PARAMETERS = Set.of("skill", "mapping", "js", "decision", "question");
    private static final Logger log = LoggerFactory.getLogger(GraphDataMapper.class);

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

    private void handleDataMappingEntry(String nodeName, String command, GraphInstance graphInstance) {
        int sep = command.indexOf(MAP_TO);
        if (sep > 0) {
            var stateMachine = graphInstance.stateMachine;
            var lhs = command.substring(0, sep).trim();
            var rhs = command.substring(sep + MAP_TO.length()).trim();
            var value = helper.getLhsOrConstant(lhs, stateMachine);
            if (!validRhs(rhs, graphInstance.graph)) {
                throw new IllegalArgumentException(NODE_NAME + nodeName + " has invalid mapping '"+command+"'");
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
        } else {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have '->' in '"+command+"'");
        }
    }

    private boolean validRhs(String rhs, MiniGraph graph) {
        if (rhs.startsWith(OUTPUT_ARRAY) || rhs.startsWith(OUTPUT_NAMESPACE) || rhs.startsWith(MODEL_NAMESPACE)) {
            return true;
        }
        if (rhs.startsWith(".") || !rhs.contains(".")) {
            return false;
        }
        var parts = util.split(rhs, ".[]");
        if (parts.size() < 2) {
            return false;
        }
        var node = graph.findNodeByAlias(parts.getFirst());
        return node != null && !RESERVED_PARAMETERS.contains(parts.get(1));
    }
}
