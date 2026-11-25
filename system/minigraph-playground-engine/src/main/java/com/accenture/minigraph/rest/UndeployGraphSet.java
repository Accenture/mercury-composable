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

package com.accenture.minigraph.rest;

import com.accenture.minigraph.start.HotDeployment;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * POST /api/graph-set/undeploy/{graph_id} - undeploy a hot-deployed graph set by its main graph's id, which is the
 * set's name (RFC-0017). The set's graphs leave the registry in one swap, the record is deleted first and then the
 * files it named; every other instance over the same folder drops the set at its next lookup. A set the folder does
 * not hold answers 404 - a bundled graph or set name too, since the folder never holds a record for one.
 */
@OptionalService(HotDeployment.UNDEPLOY_KEY)
@PreLoad(route = "undeploy.graph.set", instances = 10)
public class UndeployGraphSet implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        var graphId = input.getPathParameter("graph_id");
        if (graphId == null || graphId.isBlank()) {
            throw new IllegalArgumentException("Missing path parameter: graph_id");
        }
        final HotDeployment.Deployment removed;
        try {
            removed = HotDeployment.undeploy(graphId.trim());
        } catch (HotDeployment.Refused e) {
            throw new AppException(e.getStatus(), e.getMessage());
        }
        PostOffice.trackable(headers, instance).annotateTrace("set", removed.set());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("set", removed.set());
        result.put("version", removed.version());
        result.put("sha256", removed.sha256());
        result.put("graphs", removed.graphs());
        result.put("undeployed", true);
        return new EventEnvelope().setHeader("Content-Type", "application/json").setBody(result);
    }
}
