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

import com.accenture.minigraph.services.GraphCommandService;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

@OptionalService("app.env=dev")
@PreLoad(route = "get.live.graph", instances = 10)
public class GetLiveGraph implements TypedLambdaFunction<AsyncHttpRequest, Object> {

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        var id = input.getPathParameter("id");
        if (id == null) {
            throw new IllegalArgumentException("Missing path parameter: id");
        }
        var graph = GraphCommandService.downloadGraph(id);
        if (graph == null) {
            throw new AppException(404, "No active session for id " + id);
        }
        return new EventEnvelope().setHeader("Content-Type", "application/json").setBody(graph);
    }
}