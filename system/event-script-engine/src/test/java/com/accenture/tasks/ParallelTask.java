package com.accenture.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@PreLoad(route="parallel.one, parallel.two", instances=10)
public class ParallelTask implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(ParallelTask.class);

    private static final String DECISION = "decision";
    public static AtomicInteger counter = new AtomicInteger(1);
    public static BlockingQueue<Map<String, Object>> bench = new ArrayBlockingQueue<>(2);

    public static void resetCounter(int count) {
        ParallelTask.counter = new AtomicInteger(count);
        ParallelTask.bench = new ArrayBlockingQueue<>(count);
    }

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        int remaining = counter.decrementAndGet();
        bench.offer(input);
        log.info("Remaining parallel tasks = {}, input = {}", remaining, input);
        Map<String, Object> result = new HashMap<>();
        result.put(DECISION, remaining <= 0);
        result.putAll(input);
        return result;
    }
}
