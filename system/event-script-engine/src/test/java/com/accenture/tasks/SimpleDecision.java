package com.accenture.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

@PreLoad(route="simple.decision", instances=10)
public class SimpleDecision implements TypedLambdaFunction<Map<String, Object>, Boolean> {

    private static final String DECISION = "decision";

    @Override
    public Boolean handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (input.containsKey(DECISION)) {
            return "true".equals(input.get(DECISION).toString());

        } else {
            throw new IllegalArgumentException("Missing decision");
        }
    }
}