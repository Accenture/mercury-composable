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

@PreLoad(route = GraphExtension.ROUTE, instances=300)
public class GraphExtension extends GraphLambdaFunction {
    public static final String ROUTE = "graph.extension";
    private static final String GRAPH_EXECUTOR = "graph-executor";

    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
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
        var graphId = properties.get(EXTENSION);
        if (graphId == null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have question ID");
        }
        var mapping = properties.get(MAPPING);
        if (mapping instanceof List<?> entries) {
            for (Object entry : entries) {
                fillFetcherApiParameters(nodeName, String.valueOf(entry), graphInstance);
            }
            var stateMachine = graphInstance.stateMachine;
            var parameters = stateMachine.getElement(nodeName + API_DOT);
            var flow = Flows.getFlow(GRAPH_EXECUTOR);
            if (flow == null) {
                throw new IllegalArgumentException("flow://"+ GRAPH_EXECUTOR +" not found");
            }
            var po = new PostOffice(headers, instance);
            var dataset = Map.of("body", parameters instanceof Map? parameters : Map.of(),
                                "header", Map.of(),
                                "path_parameter", Map.of("graph_id", graphId));
            return retrieveFromExtension(po, nodeName, graphInstance, dataset, flow.ttl);
        } else {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have 'mapping' entries");
        }
    }

    private Object retrieveFromExtension(PostOffice po, String nodeName, GraphInstance graphInstance,
                                         Map<String, Object> dataset, long ttl) {
        var stateMachine = graphInstance.stateMachine;
        var forward = new EventEnvelope();
        forward.setTo(EventScriptManager.SERVICE_NAME).setHeader(FLOW_ID, GRAPH_EXECUTOR);
        forward.setCorrelationId(util.getUuid()).setBody(dataset);
        return Mono.create(sink ->
            po.eRequest(forward, ttl, false).thenAccept(response -> {
                stateMachine.setElement(nodeName + RESULT_DOT + STATUS, response.getStatus());
                if (response.hasError()) {
                    stateMachine.setElement(nodeName + RESULT_DOT + ERROR, response.getError());
                } else {
                    stateMachine.setElement(nodeName + RESULT_DOT, response.getBody());
                }
            sink.success(NEXT);
        }));
    }
}
