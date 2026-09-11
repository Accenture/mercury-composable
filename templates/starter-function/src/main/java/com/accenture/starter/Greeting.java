// Scaffolded from Mercury Composable's starter templates (https://github.com/Accenture/mercury-composable, Apache-2.0)

package com.accenture.starter;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * The starter's single composable function. It is addressed only by its route
 * name ("v1.greeting") - rest.yaml maps the /api/greeting endpoint to it, and
 * the same function could join an Event Script flow or a knowledge graph
 * without a code change.
 */
@PreLoad(route = "v1.greeting", instances = 10)
public class Greeting implements TypedLambdaFunction<AsyncHttpRequest, Map<String, Object>> {

    private static final String NAME = "name";

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        if (input.getUrl() == null) {
            throw new AppException(400, "The input does not appear to be an HTTP request. " +
                    "Please route the request through REST automation");
        }
        String name = input.getQueryParameter(NAME);
        if (name == null && input.getBody() instanceof Map<?, ?> map && map.get(NAME) instanceof String value) {
            name = value;
        }
        if (name == null || name.isBlank()) {
            throw new AppException(400, "Missing 'name' - send ?name=... or a JSON body {\"name\": \"...\"}");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("greeting", "Hello, " + name);
        result.put("served_by", "v1.greeting");
        result.put("instance", instance);
        return result;
    }
}
