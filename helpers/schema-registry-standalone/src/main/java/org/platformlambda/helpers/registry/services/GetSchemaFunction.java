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
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.Utility;
import org.platformlambda.helpers.registry.store.SchemaStore;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles GET /schemas/ids/{id}
 * Retrieves a schema string and type by its integer ID.
 */
@PreLoad(route = "schema.registry.get")
public class GetSchemaFunction implements TypedLambdaFunction<EventEnvelope, Map<String, Object>> {

    private final SchemaStore store = SchemaStore.getInstance();
    private final Utility util = Utility.getInstance();

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, EventEnvelope input, int instance) throws Exception {
        String idStr = headers.get("id");
        if (idStr == null || idStr.isEmpty()) {
            throw new AppException(400, "Schema ID is required");
        }

        int id = util.str2int(idStr);
        if (id == -1 && !idStr.equals("-1")) {
             throw new AppException(400, "Invalid schema ID");
        }

        SchemaStore.SchemaEntry entry = store.get(id);
        if (entry == null) {
            throw new AppException(404, "Schema not found");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("schema", entry.schema());
        
        // Avro is default, omit schemaType for it to perfectly mimic Confluent behavior,
        // otherwise include it.
        if (!SchemaStore.AVRO_TYPE.equals(entry.schemaType())) {
            response.put("schemaType", entry.schemaType());
        }

        return response;
    }
}
