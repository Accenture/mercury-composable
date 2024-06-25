package com.accenture.tasks;

import com.accenture.models.PoJo;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

@PreLoad(route="sequential.two, sequential.three", instances=10)
public class SequentialTwo implements TypedLambdaFunction<PoJo, PoJo> {

    @Override
    public PoJo handleEvent(Map<String, String> headers, PoJo input, int instance) {
        return input;
    }
}
