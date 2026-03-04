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

package com.accenture.minigraph.tasks;

import com.accenture.minigraph.base.GraphLambdaFunction;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@PreLoad(route = GraphHousekeeper.ROUTE, instances=20)
public class GraphHousekeeper extends GraphLambdaFunction {
    public static final String ROUTE = "graph.housekeeper";
    private static final Logger log = LoggerFactory.getLogger(GraphHousekeeper.class);
    private static final String INSTANCE_ID = "instance_id";
    private static final String TYPE = "type";
    private static final String END = "end";

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        if (headers.containsKey(INSTANCE_ID) && END.equals(headers.get(TYPE))) {
            var instanceId = headers.get(INSTANCE_ID);
            var graphInstance = graphInstances.get(instanceId);
            if (graphInstance != null) {
                graphInstances.remove(instanceId);
                log.debug("Graph instance {} for model '{}' cleared", instanceId, graphInstance.graphId);
            }
        }
        return null;
    }
}
