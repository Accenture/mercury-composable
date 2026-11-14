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

package com.accenture.minigraph.tests;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * A stand-in for the route {@code diagram.render}, which the MCP binding of a language pack serves in
 * production by calling a Mermaid MCP server (the diagram-assistant graph's one tool). Here the same
 * route name resolves to this mock - the route-decoupling contract that makes an MCP-bound tool testable
 * by configuration. It answers the shape the real server answers: the title, the code, {@code valid},
 * and the rendered image when valid, or an error when the code does not start like a Mermaid diagram.
 */
@PreLoad(route = "diagram.render", instances = 10)
public class MockDiagramRender implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final String[] HEADERS = {"flowchart", "graph", "sequenceDiagram", "classDiagram", "stateDiagram"};

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws AppException {
        Object code = input.get("diagramCode");
        if (!(code instanceof String text) || text.isBlank()) {
            throw new AppException(400, "diagramCode is required");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("title", input.getOrDefault("title", ""));
        result.put("diagramCode", text);
        boolean valid = false;
        for (String header : HEADERS) {
            if (text.stripLeading().startsWith(header)) {
                valid = true;
                break;
            }
        }
        result.put("valid", valid);
        if (valid) {
            result.put("rawSVG", "<svg xmlns=\"http://www.w3.org/2000/svg\"><text>mock render</text></svg>");
        } else {
            result.put("error", "Parse error on line 1: the diagram must start with its type");
        }
        return result;
    }
}
