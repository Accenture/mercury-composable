package com.accenture.tasks;

import com.accenture.models.PoJo;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

@PreLoad(route="sequential.one", instances=10)
public class SequentialOne implements TypedLambdaFunction<Map<String, Object>, PoJo> {

    private static final String USER = "user";
    private static final String SEQ = "sequence";

    @Override
    public PoJo handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (input.containsKey(USER) && input.containsKey(SEQ)) {
            return new PoJo(input.get(USER).toString(), Integer.parseInt(input.get(SEQ).toString()));

        } else {
            throw new IllegalArgumentException("Missing user or sequence");
        }
    }
}
