package com.accenture.minigraph.skills;

import com.accenture.automation.EventScriptManager;
import com.accenture.minigraph.base.GraphLambdaFunction;
import com.accenture.minigraph.models.GraphInstance;
import com.accenture.models.Flows;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@PreLoad(route = GraphApiFetcher.ROUTE, instances=200)
public class GraphApiFetcher extends GraphLambdaFunction {
    public static final String ROUTE = "graph.api.fetcher";
    private static final String DATA_DICTIONARY = "data-by-dictionary";

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
        var questionId = properties.get(QUESTION);
        if (questionId == null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have question ID");
        }
        var mapping = properties.get(MAPPING);
        if (mapping instanceof List<?> entries) {
            for (Object entry : entries) {
                handleDataMappingEntry(nodeName, String.valueOf(entry), graphInstance);
            }
            var stateMachine = graphInstance.stateMachine;
            var parameters = stateMachine.getElement(nodeName + API_DOT);
            var flow = Flows.getFlow(DATA_DICTIONARY);
            if (flow == null) {
                throw new IllegalArgumentException("flow://"+DATA_DICTIONARY+" not found");
            }
            var po = new PostOffice(headers, instance);
            var dataset = Map.of("body", parameters instanceof Map? parameters : Map.of(),
                                "header", Map.of(),
                                "path_parameter", Map.of("question_id", questionId));
            return retrieveFromDataDictionary(po, nodeName, graphInstance, dataset, flow.ttl);
        } else {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have 'mapping' entries");
        }
    }

    private Object retrieveFromDataDictionary(PostOffice po, String nodeName, GraphInstance graphInstance,
                                              Map<String, Object> dataset, long ttl) {

        System.out.println("tracing----->"+po.getTraceId());

        var stateMachine = graphInstance.stateMachine;
        var forward = new EventEnvelope();
        forward.setTo(EventScriptManager.SERVICE_NAME).setHeader(FLOW_ID, DATA_DICTIONARY);
        forward.setCorrelationId(util.getUuid()).setBody(dataset);
        return Mono.create(sink ->
            po.eRequest(forward, ttl, false).thenAccept(response -> {
                graphInstance.safety.lock();
                try {
                    stateMachine.setElement(nodeName + ".status", response.getStatus());
                    if (response.hasError()) {
                        stateMachine.setElement(nodeName + RESULT_DOT + "error", response.getError());
                    } else {
                        stateMachine.setElement(nodeName + RESULT_DOT, response.getBody());
                    }
                } finally {
                    graphInstance.safety.unlock();
                }
            sink.success(NEXT);
        }));
    }

    private void handleDataMappingEntry(String nodeName, String command, GraphInstance graphInstance) {
        int sep = command.indexOf(MAP_TO);
        if (sep > 0) {
            var stateMachine = graphInstance.stateMachine;
            var lhs = command.substring(0, sep).trim();
            var rhs = command.substring(sep + MAP_TO.length()).trim();
            var value = helper.getLhsOrConstant(lhs, stateMachine);
            var target = rhs.startsWith(MODEL_NAMESPACE)? rhs : nodeName + API_DOT + rhs;
            graphInstance.safety.lock();
            try {
                if (value != null) {
                    stateMachine.setElement(target, value);
                } else {
                    if (rhs.endsWith("]") && rhs.contains("[")) {
                        stateMachine.setElement(rhs, null);
                    } else {
                        stateMachine.removeElement(rhs);
                    }
                }
            } finally {
                graphInstance.safety.unlock();
            }
        } else {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have '->' in '"+command+"'");
        }
    }
}
