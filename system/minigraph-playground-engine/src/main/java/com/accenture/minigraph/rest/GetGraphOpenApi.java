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

import com.accenture.minigraph.contract.GraphContract;
import com.accenture.minigraph.contract.OpenApiDocument;
import com.accenture.minigraph.models.CompiledGraphs;
import com.accenture.minigraph.services.GraphCommandService;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.AppConfigReader;

import java.util.List;
import java.util.Map;

/**
 * The OpenAPI 3.0 document of a graph on demand (RFC-0007, WP1), dev-mode like the other Playground
 * services: {@code GET /api/openapi/{graph_id}} for a deployed graph and
 * {@code GET /api/openapi/session/{sessionId}} for a session's draft. The document is a YAML
 * attachment by default; {@code ?format=json} answers JSON inline; {@code ?view=contract} answers the
 * derived contract - the merged schemas with their evidence - as JSON, for the Playground's panel and
 * for an agent. The document is derived from the model each time and never stored.
 */
@OptionalService("app.env=dev")
@PreLoad(route = "get.graph.openapi", instances = 10)
public class GetGraphOpenApi implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {
    private static final String GRAPH_ID = "graph_id";
    private static final String SESSION_ID = "sessionId";
    private static final String FORMAT = "format";
    private static final String VIEW = "view";
    private static final String JSON = "json";
    private static final String YAML = "yaml";
    private static final String CONTRACT = "contract";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ALIAS = "alias";
    private static final String NODES = "nodes";
    private static final String PROPERTIES = "properties";

    @Override
    @SuppressWarnings("unchecked")
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance)
            throws AppException {
        var graphId = input.getPathParameter(GRAPH_ID);
        var sessionId = input.getPathParameter(SESSION_ID);
        Map<String, Object> model;
        String id;
        String version;
        if (graphId != null) {
            model = CompiledGraphs.getGraph(graphId);
            if (model == null) {
                throw new AppException(404, "Graph model '" + graphId + "' not found");
            }
            id = graphId;
            var set = CompiledGraphs.getGraphSet(graphId);
            version = set != null && !set.version().isEmpty() ? set.version() : appVersion();
        } else if (sessionId != null) {
            var draft = GraphCommandService.downloadGraph(sessionId);
            if (!(draft instanceof Map<?, ?> m)) {
                throw new AppException(404, "Session '" + sessionId + "' does not exist");
            }
            model = (Map<String, Object>) m;
            id = draftName(model);
            version = appVersion();
        } else {
            throw new IllegalArgumentException("Missing path parameter: graph_id or sessionId");
        }
        var contract = GraphContract.derive(id, model, CompiledGraphs::getGraph);
        var view = parameter(input, VIEW, "document");
        if (CONTRACT.equals(view)) {
            return new EventEnvelope().setHeader(CONTENT_TYPE, "application/json").setBody(contract.toMap());
        }
        if (!"document".equals(view)) {
            throw new IllegalArgumentException("Unknown view '" + view + "' - use document or contract");
        }
        var document = OpenApiDocument.of(contract, version, serverUrl(input));
        var format = parameter(input, FORMAT, YAML);
        if (JSON.equals(format)) {
            return new EventEnvelope().setHeader(CONTENT_TYPE, "application/json").setBody(document);
        }
        if (!YAML.equals(format)) {
            throw new IllegalArgumentException("Unknown format '" + format + "' - use yaml or json");
        }
        return new EventEnvelope()
                .setHeader(CONTENT_TYPE, "application/yaml; charset=utf-8")
                .setHeader("Content-Disposition", "attachment; filename=\"" + id + ".yaml\"")
                .setBody(OpenApiDocument.toYaml(document));
    }

    private static String parameter(AsyncHttpRequest input, String name, String defaultValue) {
        var value = input.getQueryParameter(name);
        return value == null || value.isBlank() ? defaultValue : value.trim().toLowerCase();
    }

    private static String appVersion() {
        return AppConfigReader.getInstance().getProperty("info.app.version", "1.0.0");
    }

    /** The server that answers, so a downloaded document points back at the engine that generated it. */
    private static String serverUrl(AsyncHttpRequest input) {
        var host = input.getHeader("host");
        if (host == null || host.isBlank()) {
            return null;
        }
        var scheme = input.isSecure() ? "https" : "http";
        return scheme + "://" + host.trim();
    }

    /** A draft is named after its root node's name, as the download and the export are; else "draft". */
    private static String draftName(Map<String, Object> model) {
        if (model.get(NODES) instanceof List<?> nodes) {
            for (var n : nodes) {
                if (n instanceof Map<?, ?> node && "root".equals(node.get(ALIAS))
                        && node.get(PROPERTIES) instanceof Map<?, ?> p && p.get("name") instanceof String name
                        && !name.isBlank()) {
                    return name.trim();
                }
            }
        }
        return "draft";
    }
}
