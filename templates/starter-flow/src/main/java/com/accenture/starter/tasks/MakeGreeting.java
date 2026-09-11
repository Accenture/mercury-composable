// Scaffolded from Mercury Composable's starter templates (https://github.com/Accenture/mercury-composable, Apache-2.0)

package com.accenture.starter.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * Second task of greeting-flow: compose the response. It knows nothing about
 * the validator - the flow's state machine ('model.name') carries data
 * between the decoupled tasks.
 */
@PreLoad(route = "v1.make.greeting", instances = 10)
public class MakeGreeting implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    private static final String NAME = "name";

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        Map<String, Object> result = new HashMap<>();
        result.put("greeting", "Hello, " + input.get(NAME));
        result.put("served_by", "v1.make.greeting");
        return result;
    }
}
