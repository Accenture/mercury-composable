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

import com.accenture.minigraph.common.GraphModelGate;
import com.accenture.minigraph.common.GraphSet;
import com.accenture.minigraph.services.GraphCommandService;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * POST /api/graph/pack - pack graph models into a graph set on the engine (ADR-0027), so the Playground's
 * "Package graphs" panel never carries a packager of its own. The body is a JSON object,
 * {"manifest": {"set": "<name>", ...}, "graphs": {"<graph-id>": <model>, ...}}: the manifest field 'set'
 * names the set and its file, every other manifest field is caller text ('format' and 'format_version' are
 * written by the packager), and each graph is a model as a file holds it. Every model passes the import
 * validation and then the deployment gate's checks, as the graph packager's pack command does, and the set
 * is refused with every reason when any rule fails. The answer is the package as a download named
 * <set>.pack - the same bytes for the same graphs and fields, wherever they are packed.
 */
@OptionalService("app.env=dev")
@PreLoad(route = "pack.graph.set", instances = 10)
public class PackGraphSet implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {
    private static final String MANIFEST = "manifest";
    private static final String GRAPHS = "graphs";
    private static final String NOT_PACKED = "Set not packed - ";

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        if (!(input.getBody() instanceof Map<?, ?> body)) {
            throw new IllegalArgumentException(NOT_PACKED + "the request body is a JSON object with 'manifest' and 'graphs'");
        }
        var fields = manifestFields(body.get(MANIFEST));
        var setName = fields.remove(GraphSet.SET);
        if (setName == null || setName.isBlank()) {
            throw new IllegalArgumentException(NOT_PACKED + "manifest field 'set' is required - it names the set and its file");
        }
        var graphs = graphModels(body.get(GRAPHS));
        final byte[] bytes;
        try {
            bytes = GraphSet.pack(setName, fields, graphs);
        } catch (GraphSet.RefusedException | IOException e) {
            throw new IllegalArgumentException(NOT_PACKED + e.getMessage());
        }
        return new EventEnvelope()
                .setHeader("Content-Type", "application/octet-stream")
                .setHeader("Content-Disposition", "attachment; filename=\"" + setName + GraphSet.EXTENSION + "\"")
                .setBody(bytes);
    }

    private static Map<String, String> manifestFields(Object manifest) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (manifest == null) {
            return fields;
        }
        if (!(manifest instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(NOT_PACKED + "'manifest' is a JSON object of text fields");
        }
        for (var entry : map.entrySet()) {
            var key = String.valueOf(entry.getKey());
            var value = entry.getValue();
            if (value == null || value instanceof Map || value instanceof List) {
                throw new IllegalArgumentException(NOT_PACKED + "manifest field '" + key + "' - a value is text");
            }
            fields.put(key, String.valueOf(value));
        }
        return fields;
    }

    private static Map<String, Map<String, Object>> graphModels(Object graphs) {
        if (!(graphs instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(NOT_PACKED +
                    "'graphs' is a JSON object keyed by graph id, each value a graph model");
        }
        List<String> reasons = new ArrayList<>();
        Map<String, Map<String, Object>> models = new TreeMap<>();
        for (var entry : map.entrySet()) {
            var id = String.valueOf(entry.getKey());
            var problem = GraphCommandService.validateGraphModel(entry.getValue());
            if (problem != null) {
                reasons.add(id + ": " + problem);
            } else if (entry.getValue() instanceof Map<?, ?> model) {
                models.put(id, GraphModelGate.withoutNullProperties(model));
            }
        }
        if (!reasons.isEmpty()) {
            throw new IllegalArgumentException(NOT_PACKED + String.join("; ", reasons));
        }
        return models;
    }
}
