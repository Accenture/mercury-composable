package com.accenture.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@PreLoad(route="begin.parallel.test", instances=10)
public class PrepareParallelTest implements TypedLambdaFunction<Map<String, Object>, Void> {
    private static final Logger log = LoggerFactory.getLogger(PrepareParallelTest.class);

    private static final String COUNT = "count";

    @Override
    public Void handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (input.containsKey(COUNT)) {
            int total = Math.max(1, Integer.parseInt(input.get(COUNT).toString()));
            log.info("Reset parallel counter to {}", total);
            ParallelTask.resetCounter(total);
        }
        return null;
    }
}
