/*

    Copyright 2018-2026 Accenture Technology

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

package com.accenture.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * The test cron's demo-flow job's flow task, registered when the application starts. A job that fires before the test
 * class that reads it has run must still find it, or the run is spent and the state resolver refuses the next one.
 */
@PreLoad(route = "hello.flow")
public class HelloFlow implements TypedLambdaFunction<Map<String, Object>, Void> {
    private static final Logger log = LoggerFactory.getLogger(HelloFlow.class);
    /** The runs received, each as its body and headers. The jobs keep firing, so a full queue drops the newest. */
    public static final BlockingQueue<Map<String, Object>> RECEIVED = new ArrayBlockingQueue<>(2);

    @Override
    public Void handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        log.info("hello.flow got {} {}", headers, input);
        if (!RECEIVED.offer(Map.of("body", input, "headers", headers))) {
            log.debug("Run dropped - the queue holds {} runs already", RECEIVED.size());
        }
        return null;
    }
}
