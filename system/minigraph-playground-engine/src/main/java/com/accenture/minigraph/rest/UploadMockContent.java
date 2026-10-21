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
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.MultiLevelMap;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

@OptionalService("app.env=dev")
@PreLoad(route = "upload.mock.content", instances = 10)
public class UploadMockContent implements TypedLambdaFunction<AsyncHttpRequest, Object> {

    private static final String NAMESPACE = "namespace";
    private static final String BODY = "body";
    private static final String HEADER = "header";
    private static final String MODEL = "model";

    /**
     * Load mock data into the session's graph instance. The {@code namespace} query parameter selects
     * the target: {@code body} (the default) takes a JSON map or list as {@code input.body};
     * {@code header} takes a JSON object of text values as {@code input.header}, processed as a real
     * request's headers are (see GraphCommandService.loadMockHeaders), so a dry run can supply the
     * headers a graph reads; {@code model} takes a JSON object whose keys name model variables, for the
     * state a request's edge would have set. Each namespace travels like a command to every member of a
     * collaborative session.
     */
    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        var id = input.getPathParameter("id");
        if (id == null) {
            throw new IllegalArgumentException("Missing path parameter: id");
        }
        var namespace = input.getQueryParameter(NAMESPACE);
        if (namespace == null || namespace.isBlank()) {
            namespace = BODY;
        }
        var body = input.getBody();
        if (HEADER.equals(namespace)) {
            checkHeaders(body);
        } else if (MODEL.equals(namespace)) {
            checkModelVariables(body);
        } else if (BODY.equals(namespace)) {
            if (!(body instanceof Map || body instanceof List)) {
                throw new IllegalArgumentException("Input is not a valid JSON payload that represents a Map or List");
            }
        } else {
            throw new IllegalArgumentException("Unknown mock namespace '" + namespace +
                    "' - use body, header or model");
        }
        if (GraphCommandService.uploadContent(id, body, namespace)) {
            return new EventEnvelope().setHeader("Content-Type", "application/json")
                    .setBody(Map.of("message", "Content uploaded", "type", "upload", NAMESPACE, namespace));
        } else {
            throw new IllegalArgumentException("Session "+id+" is expired or invalid");
        }
    }

    /**
     * Mock headers are a JSON object of text values whose names are distinct without case, because a
     * request's header names are case-insensitive and the engine stores them in lower case.
     */
    private static void checkHeaders(Object body) {
        if (!(body instanceof Map<?, ?> map) || !map.values().stream().allMatch(String.class::isInstance)) {
            throw new IllegalArgumentException("Mock headers must be a JSON object with text values");
        }
        var names = new HashSet<String>();
        for (var name : map.keySet()) {
            var lower = String.valueOf(name).toLowerCase();
            if (!names.add(lower)) {
                throw new IllegalArgumentException("Duplicate mock header '" + lower +
                        "' - header names are case-insensitive");
            }
        }
    }

    /**
     * Mock model variables are a JSON object: each key is a model path without the {@code model.} prefix,
     * in the dot-bracket syntax of the state machine, and each value is not null. An empty segment is
     * refused because the syntax check drops it, and a name of dots alone would replace the whole model.
     */
    private static void checkModelVariables(Object body) {
        if (!(body instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Mock model variables must be a JSON object");
        }
        var syntax = new MultiLevelMap();
        for (var entry : map.entrySet()) {
            var name = String.valueOf(entry.getKey());
            if (name.isBlank() || name.startsWith(".") || name.endsWith(".") || name.contains("..")) {
                throw new IllegalArgumentException("Invalid mock model variable '" + name + "' - empty path segment");
            }
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("Mock model variable '" + name + "' has no value");
            }
            try {
                syntax.validateCompositePathSyntax(MODEL + "." + name);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid mock model variable '" + name + "' - " + e.getMessage());
            }
        }
    }
}
