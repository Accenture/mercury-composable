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

package com.accenture.minigraph.contract;

import org.platformlambda.core.serializers.SimpleMapper;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal OpenAPI 3.0 document of one graph (RFC-0007), derived from its {@link GraphContract}: the
 * graph's one endpoint {@code POST /api/graph/{graph-id}}, its request body and header parameters, the
 * {@code 200} response with its body and headers, every status the model stages with
 * {@code int(N) -> output.status}, and the engine's error shape. The document is a derived artifact,
 * never stored; the Rust engine produces the same map from the same model, and the shared vector file
 * pins both.
 */
public final class OpenApiDocument {
    private static final String ERROR_REF = "#/components/schemas/Error";
    private static final String APPLICATION_JSON = "application/json";
    private static final String DESCRIPTION = "description";
    private static final String SCHEMA = "schema";
    private static final String CONTENT = "content";
    private static final String TYPE = "type";
    private static final String REQUIRED = "required";
    private static final String STRING = "string";

    private OpenApiDocument() {}

    /**
     * Build the document.
     *
     * @param contract the graph's contract
     * @param version the API version (the deployed set's version, else the application's)
     * @param serverUrl the base URL of the engine that answers, e.g. {@code http://127.0.0.1:8085}; null to omit
     * @return the OpenAPI 3.0 document as an ordered map
     */
    public static Map<String, Object> of(GraphContract contract, String version, String serverUrl) {
        var id = contract.getGraphId();
        var purpose = contract.getPurpose();
        var document = new LinkedHashMap<String, Object>();
        document.put("openapi", "3.0.3");
        var info = new LinkedHashMap<String, Object>();
        info.put("title", id);
        info.put("version", version == null || version.isBlank() ? "1.0.0" : version);
        if (purpose != null) {
            info.put(DESCRIPTION, purpose);
        }
        document.put("info", info);
        if (serverUrl != null && !serverUrl.isBlank()) {
            document.put("servers", List.of(Map.of("url", serverUrl)));
        }
        var operation = new LinkedHashMap<String, Object>();
        operation.put("operationId", id);
        operation.put("summary", purpose == null ? "Run the graph " + id : purpose);
        var parameters = headerParameters(contract);
        if (!parameters.isEmpty()) {
            operation.put("parameters", parameters);
        }
        var requestSchema = contract.schema(GraphContract.INPUT_BODY);
        if (!requestSchema.isEmpty()) {
            var requestBody = new LinkedHashMap<String, Object>();
            requestBody.put(REQUIRED, true);
            requestBody.put(CONTENT, jsonContent(requestSchema));
            operation.put("requestBody", requestBody);
        }
        operation.put("responses", responses(contract));
        var path = new LinkedHashMap<String, Object>();
        path.put("post", operation);
        var paths = new LinkedHashMap<String, Object>();
        paths.put("/api/graph/" + id, path);
        document.put("paths", paths);
        document.put("components", Map.of("schemas", Map.of("Error", errorSchema())));
        return document;
    }

    private static List<Map<String, Object>> headerParameters(GraphContract contract) {
        var parameters = new ArrayList<Map<String, Object>>();
        var root = contract.root(GraphContract.INPUT_HEADER);
        for (var header : root.children.values()) {
            var parameter = new LinkedHashMap<String, Object>();
            parameter.put("name", header.name);
            parameter.put("in", "header");
            if (header.required) {
                parameter.put(REQUIRED, true);
            }
            var schema = GraphContract.schemaOf(header);
            var description = schema.remove(DESCRIPTION);
            if (description != null) {
                parameter.put(DESCRIPTION, description);
            }
            schema.putIfAbsent(TYPE, STRING);
            parameter.put(SCHEMA, schema);
            parameters.add(parameter);
        }
        return parameters;
    }

    private static Map<String, Object> responses(GraphContract contract) {
        var responses = new LinkedHashMap<String, Object>();
        var ok = new LinkedHashMap<String, Object>();
        ok.put(DESCRIPTION, "The graph's output.body");
        var headers = responseHeaders(contract);
        if (!headers.isEmpty()) {
            ok.put("headers", headers);
        }
        ok.put(CONTENT, jsonContent(contract.schema(GraphContract.OUTPUT_BODY)));
        responses.put("200", ok);
        for (var code : contract.getStatusCodes()) {
            if (code == 200) {
                continue;
            }
            var staged = new LinkedHashMap<String, Object>();
            staged.put(DESCRIPTION, "Staged by the graph (int(" + code + ") -> output.status)");
            var schema = new LinkedHashMap<String, Object>();
            schema.put(TYPE, "object");
            staged.put(CONTENT, jsonContent(schema));
            responses.put(String.valueOf(code), staged);
        }
        var error = new LinkedHashMap<String, Object>();
        error.put(DESCRIPTION, "Error");
        var ref = new LinkedHashMap<String, Object>();
        ref.put("$ref", ERROR_REF);
        error.put(CONTENT, jsonContent(ref));
        responses.put("default", error);
        return responses;
    }

    private static Map<String, Object> responseHeaders(GraphContract contract) {
        var headers = new LinkedHashMap<String, Object>();
        var root = contract.root(GraphContract.OUTPUT_HEADER);
        for (var header : root.children.values()) {
            var entry = new LinkedHashMap<String, Object>();
            var schema = GraphContract.schemaOf(header);
            var description = schema.remove(DESCRIPTION);
            if (description != null) {
                entry.put(DESCRIPTION, description);
            }
            schema.putIfAbsent(TYPE, STRING);
            entry.put(SCHEMA, schema);
            headers.put(header.name, entry);
        }
        return headers;
    }

    private static Map<String, Object> jsonContent(Map<String, Object> schema) {
        var media = new LinkedHashMap<String, Object>();
        media.put(SCHEMA, schema);
        var content = new LinkedHashMap<String, Object>();
        content.put(APPLICATION_JSON, media);
        return content;
    }

    private static Map<String, Object> errorSchema() {
        var properties = new LinkedHashMap<String, Object>();
        properties.put(TYPE, Map.of(TYPE, STRING));
        properties.put("status", Map.of(TYPE, "integer"));
        properties.put("message", Map.of(TYPE, STRING));
        var schema = new LinkedHashMap<String, Object>();
        schema.put(TYPE, "object");
        schema.put("properties", properties);
        schema.put(REQUIRED, List.of(TYPE, "status", "message"));
        return schema;
    }

    /** The document as YAML, block style, keys in document order. */
    public static String toYaml(Map<String, Object> document) {
        var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setPrettyFlow(true);
        return new Yaml(options).dump(document);
    }

    /** The document as JSON, keys in document order. */
    public static String toJson(Map<String, Object> document) {
        return SimpleMapper.getInstance().getMapper().writeValueAsString(document);
    }
}
