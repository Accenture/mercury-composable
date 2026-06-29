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

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.Utility;
import org.platformlambda.helpers.registry.store.SchemaStore;

import java.util.HashMap;
import java.util.Map;

/**
 * Faithful mock of Confluent's {@code GET /schemas/ids/{id}} (fetch a schema by its global id).
 * <p>
 * The REST endpoint is wired directly to this function in rest.yaml (no flow), so the input is the
 * raw {@link AsyncHttpRequest} and the "id" comes from the path. The response is
 * {@code {"schema": "&lt;escaped schema string&gt;"}}; {@code schemaType} is included only for non-AVRO
 * schemas (Confluent omits it for AVRO). An unknown id returns HTTP 404 with error_code 40403.
 */
@PreLoad(route = "schema.registry.get", instances = 10)
public class GetSchemaFunction implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {

    private final SchemaStore store = SchemaStore.getInstance();
    private final Utility util = Utility.getInstance();

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        String idStr = input.getPathParameter("id");
        if (idStr == null || !util.isDigits(idStr)) {
            return ApiError.of(404, ApiError.SCHEMA_NOT_FOUND, "Schema not found");
        }
        int id = util.str2int(idStr);
        SchemaStore.SchemaEntry entry = store.get(id);
        if (entry == null) {
            return ApiError.of(404, ApiError.SCHEMA_NOT_FOUND, "Schema " + id + " not found");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("schema", entry.schema());
        // Confluent omits schemaType for AVRO (the default) and includes it otherwise.
        if (!SchemaStore.AVRO_TYPE.equals(entry.schemaType())) {
            response.put("schemaType", entry.schemaType());
        }
        return new EventEnvelope().setStatus(200).setBody(response);
    }
}
