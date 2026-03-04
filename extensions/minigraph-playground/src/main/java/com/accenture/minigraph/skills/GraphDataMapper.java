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

import java.util.List;
import java.util.Map;

@PreLoad(route = GraphDataMapper.ROUTE, instances=300)
public class GraphDataMapper extends GraphLambdaFunction {
    public static final String ROUTE = "graph.data.mapper";

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
