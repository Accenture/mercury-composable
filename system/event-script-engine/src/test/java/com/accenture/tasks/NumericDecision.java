package com.accenture.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.Utility;

import java.util.Map;

@PreLoad(route="numeric.decision", instances=10)
public class NumericDecision implements TypedLambdaFunction<Map<String, Object>, Integer> {

    private static final String DECISION = "decision";

    @Override
    public Integer handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (input.containsKey(DECISION)) {
            return Utility.getInstance().str2int(input.get(DECISION).toString());

        } else {
            throw new IllegalArgumentException("Missing decision");
        }
    }
}