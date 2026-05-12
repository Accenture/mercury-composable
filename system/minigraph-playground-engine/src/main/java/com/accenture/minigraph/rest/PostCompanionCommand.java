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
import org.platformlambda.core.system.PostOffice;

import java.util.Map;

@OptionalService("app.env=dev")
@PreLoad(route = "post.companion.command", instances = 10)
public class PostCompanionCommand implements TypedLambdaFunction<AsyncHttpRequest, Object> {

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        var id = input.getPathParameter("id");
        if (id == null) {
            throw new IllegalArgumentException("Missing path parameter: id");
        }
        if (!(input.getBody() instanceof String raw)) {
            throw new IllegalArgumentException("Body must be a non-empty text/plain command");
        }
        var command = raw.trim();
        if (command.isEmpty()) {
            throw new IllegalArgumentException("Body must be a non-empty text/plain command");
        }
        if (!GraphCommandService.hasSession(id)) {
            throw new AppException(404, "No active session for id " + id);
        }
        // Inverse of GraphUserInterface's public session id mapping.
        var route = id.replace('-', '.');
        var inRoute = route + ".in";
        var outRoute = route + ".out";
        var po = new PostOffice(headers, instance);
        po.send(new EventEnvelope()
                .setTo(GraphCommandService.ROUTE)
                .setBody(Map.of(
                        "type", "command",
                        "in", inRoute,
                        "out", outRoute,
                        "message", command)));
        return new EventEnvelope()
                .setHeader("Content-Type", "application/json")
                .setBody(Map.of(
                        "type", "companion",
                        "status", "accepted",
                        "id", id,
                        "message", "Command dispatched to graph.command.service. " +
                                "Output streams to the WebSocket console for this session."));
    }
}
