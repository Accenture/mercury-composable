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

package org.platformlambda.helpers.registry.services;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.helpers.registry.store.SchemaStore;

import java.util.Map;

/**
 * Faithful mock of Confluent's {@code POST /subjects/{subject}/versions} (register a schema).
 * <p>
 * The REST endpoint is wired directly to this function in rest.yaml (no flow), so the input is the
 * raw {@link AsyncHttpRequest}: the "subject" comes from the path and the schema from the JSON body.
 * Per the Confluent spec the body is
 * <pre>{ "schema": "&lt;escaped schema string&gt;", "schemaType": "AVRO|JSON|PROTOBUF" }</pre>
 * where {@code schema} is an opaque escaped string (the registry is schema-language-agnostic) and
 * {@code schemaType} defaults to AVRO. On success it returns {@code {"id": &lt;int&gt;}}; the id is global
 * and content-based, so registering identical content (even under another subject) returns the same id.
 */
@PreLoad(route = "schema.registry.register", instances = 10)
public class RegisterSchemaFunction implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {

    private static final String SCHEMA = "schema";
    private static final String SCHEMA_TYPE = "schemaType";

    private final SchemaStore store = SchemaStore.getInstance();

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        String subject = input.getPathParameter("subject");
        if (subject == null || subject.isEmpty()) {
            return ApiError.of(404, ApiError.SUBJECT_NOT_FOUND, "Subject not found");
        }
        if (!(input.getBody() instanceof Map)) {
            return ApiError.of(422, ApiError.INVALID_SCHEMA, "Invalid schema: missing request body");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) input.getBody();
        if (!(body.get(SCHEMA) instanceof String schema) || schema.isEmpty()) {
            return ApiError.of(422, ApiError.INVALID_SCHEMA, "Invalid schema: 'schema' string is required");
        }
        Object typeObj = body.getOrDefault(SCHEMA_TYPE, SchemaStore.AVRO_TYPE);
        String schemaType = typeObj == null ? SchemaStore.AVRO_TYPE : typeObj.toString();
        // Confluent rejects a malformed schema with error_code 42201. Avro and JSON Schema are JSON
        // documents, so a well-formedness check approximates that faithfully without bundling a parser
        // per schema language. Protobuf IDL is not JSON, so it is accepted as-is.
        if (!SchemaStore.PROTOBUF_TYPE.equalsIgnoreCase(schemaType) && malformedJson(schema)) {
            return ApiError.of(422, ApiError.INVALID_SCHEMA, "Invalid schema: not well-formed JSON");
        }
        int id = store.register(schema, schemaType);
        return new EventEnvelope().setStatus(200).setBody(Map.of("id", id));
    }

    private static boolean malformedJson(String text) {
        try {
            JsonParser.parseString(text);
            return false;
        } catch (JsonSyntaxException e) {
            return true;
        }
    }
}
