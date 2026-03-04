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

import com.accenture.minigraph.base.GraphLambdaFunction;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;

import java.util.Map;

@PreLoad(route = GraphJoin.ROUTE, instances=300)
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
