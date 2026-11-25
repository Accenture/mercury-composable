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
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/graph-set/deployed - the hot-deployed graph sets as the folder records them (RFC-0017), each reconciled
 * first: set, version, sha256, deployed_at, graphs, and the status on this instance - "deployed", or
 * "not deployed - " and the reason. The dashboard's read, and an operator's with curl.
 */
@OptionalService(HotDeployment.DEPLOY_KEY + "," + HotDeployment.UNDEPLOY_KEY)
@PreLoad(route = "list.deployed.sets", instances = 10)
public class ListDeployedGraphSets implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        List<Map<String, Object>> sets = new ArrayList<>();
        for (var status : HotDeployment.list()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("set", status.set());
            row.put("version", status.version());
            row.put("sha256", status.sha256());
            row.put("deployed_at", status.deployedAt());
            row.put("graphs", status.graphs());
            row.put("status", status.status());
            sets.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("location", HotDeployment.location());
        result.put("sets", sets);
        return new EventEnvelope().setHeader("Content-Type", "application/json").setBody(result);
    }
}
