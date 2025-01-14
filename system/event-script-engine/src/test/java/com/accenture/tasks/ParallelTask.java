/*

    Copyright 2018-2025 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package com.accenture.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@PreLoad(route="parallel.task", instances=10)
public class ParallelTask implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(ParallelTask.class);

    private static final String DECISION = "decision";
    public static final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) throws InterruptedException {
        int n = counter.incrementAndGet();
        boolean done = n == 2;
        log.info("Processing {}, counter={}", input, n);
        Map<String, Object> result = new HashMap<>();
        result.put(DECISION, done);
        result.putAll(input);
        // handle the racing condition for parallel tasking
        if (done) {
            log.info("I am the second task. I would like to sleep briefly so the first task can complete");
            Thread.sleep(200);
            log.info("I have waked up");
        } else {
            log.info("I am the first task");
        }
        return result;
    }
}
