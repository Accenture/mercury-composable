// Scaffolded from Mercury Composable's starter templates (https://github.com/Accenture/mercury-composable, Apache-2.0)

package com.accenture.starter.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

/**
 * First task of greeting-flow: reject a request without a name. The flow's
 * input data mapping delivers 'input.body.name' as the "name" key; business
 * logic stays here, never in the flow YAML.
 */
@PreLoad(route = "v1.validate.request", instances = 10)
public class ValidateRequest implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    private static final String NAME = "name";

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (!(input.get(NAME) instanceof String name) || name.isBlank()) {
            throw new AppException(400, "Missing 'name' - send a JSON body {\"name\": \"...\"}");
        }
        return Map.of(NAME, name.trim());
    }
}
