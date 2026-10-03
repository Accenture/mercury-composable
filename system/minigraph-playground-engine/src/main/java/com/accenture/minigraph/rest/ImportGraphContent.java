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

/**
 * POST /api/graph/import/{id} - import a graph model from a file into a Playground session's draft.
 * The Playground's "Import Graph" button and a file dropped on the graph view call it; the model is
 * validated (nodes mandatory, connections optional, no other section) and then travels like a
 * command, so every member of a collaborative session receives the draft.
 */
@OptionalService("app.env=dev")
@PreLoad(route = "import.graph.content", instances = 10)
public class ImportGraphContent implements TypedLambdaFunction<AsyncHttpRequest, Object> {

    @SuppressWarnings("unchecked")
    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        var id = input.getPathParameter("id");
        if (id == null) {
            throw new IllegalArgumentException("Missing path parameter: id");
        }
        var body = input.getBody();
        var problem = GraphCommandService.validateGraphModel(body);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        if (!GraphCommandService.importContent(id, (Map<String, Object>) body)) {
            throw new AppException(404, "No active session for id " + id);
        }
        return new EventEnvelope().setHeader("Content-Type", "application/json")
                .setBody(Map.of("message", "Graph model imported as draft", "type", "import"));
    }
}
