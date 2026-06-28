package org.platformlambda.helpers.registry.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.helpers.registry.store.SchemaStore;

import java.util.Map;

/**
 * Handles POST /subjects/{subject}/versions
 * Registers a new schema and returns its ID.
 */
@PreLoad(route = "schema.registry.register")
public class RegisterSchemaFunction implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    private final SchemaStore store = SchemaStore.getInstance();

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) throws Exception {
        String subject = headers.get("subject");
        if (subject == null || subject.isEmpty()) {
            throw new AppException(400, "Subject is required");
        }

        if (input == null) {
            throw new AppException(400, "Missing request body");
        }

        Object schemaObj = input.get("schema");
        if (!(schemaObj instanceof String)) {
            throw new AppException(400, "Missing or invalid schema string");
        }
        
        String schema = (String) schemaObj;
        String schemaType = (String) input.getOrDefault("schemaType", SchemaStore.AVRO_TYPE);

        int id = store.register(schema, schemaType);
        
        return Map.of("id", id);
    }
}
