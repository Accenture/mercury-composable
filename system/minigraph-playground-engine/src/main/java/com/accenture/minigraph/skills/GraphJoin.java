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

import com.accenture.minigraph.common.GraphLambdaFunction;
import com.accenture.minigraph.models.GraphInstance;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;

import java.util.Map;

@PreLoad(route = GraphJoin.ROUTE, instances=300)
public class GraphJoin extends GraphLambdaFunction {
    public static final String ROUTE = "graph.join";

    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        var po = PostOffice.trackable(headers, instance);
        var nodeName = headers.getOrDefault(NODE, "none");
        po.annotateTrace(NODE, nodeName);
        var in = headers.get(IN);
        var graphInstance = getGraphInstance(in);
        var graph = graphInstance.graph;
        var node = getNode(nodeName, graph);
        if (!ROUTE.equals(node.getProperty(SKILL))) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have skill - "+ROUTE);
        }
        var connected = graph.getBackwardLinks(nodeName);
        var count = 0;
        for (var from : connected) {
            if (nodeCompleted(from.getAlias(), graphInstance)) {
                count++;
            }
        }
        // successful "join" when all the upstream nodes have been seen
        if (count == connected.size()) {
            graphInstance.nodeSeen.put(nodeName, true);
            return NEXT;
        } else {
            graphInstance.nodeSeen.put(nodeName, false);
            return SINK;
        }
    }

    private boolean nodeCompleted(String predecessor, GraphInstance graphInstance) {
        var graph = graphInstance.graph;
        var node = graph.findNodeByAlias(predecessor);
        var skill = node.getProperty(SKILL);
        if (skill == null) {
            return graphInstance.nodeSeen.containsKey(predecessor);
        }
        // A chained upstream JOIN is complete only when it actually FIRED: its
        // skill runs (and is marked in skillRun) on every arriving branch,
        // including evaluations that sink - the outcome it records in nodeSeen
        // is the truth.
        if (ROUTE.equals(skill)) {
            return Boolean.TRUE.equals(graphInstance.nodeSeen.get(predecessor));
        }
        return graphInstance.skillRun.containsKey(predecessor);
    }
}
