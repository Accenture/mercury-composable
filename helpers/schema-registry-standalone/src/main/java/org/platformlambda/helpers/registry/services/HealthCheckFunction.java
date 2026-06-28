package org.platformlambda.helpers.registry.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Collections;
import java.util.Map;

/**
 * Health check endpoint for the Schema Registry mock.
 * Returns an empty JSON object which satisfies Confluent client startup checks.
 */
@PreLoad(route = "schema.registry.health")
public class HealthCheckFunction implements TypedLambdaFunction<EventEnvelope, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        return Collections.emptyMap();
    }
}
