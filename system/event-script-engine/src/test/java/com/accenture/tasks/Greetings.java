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
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@PreLoad(route="greeting.test", instances=10, isPrivate = false)
public class Greetings implements TypedLambdaFunction<Map<String, Object>, Object> {

    private static final String USER = "user";
    private static final String GREETING = "greeting";
    private static final String MESSAGE = "message";
    private static final String TIME = "time";
    private static final String EXCEPTION = "exception";
    private static final String ORIGINAL = "original";
    private static final String TIMEOUT = "timeout";
    private static final String CUSTOM = "custom";
    private static final String DEMO = "demo";
    private static final String X_FLOW_ID = "x-flow-id";

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws InterruptedException {
        String exceptionTag = (String) input.get(EXCEPTION);
        if (exceptionTag != null) {
            if (TIMEOUT.equals(exceptionTag)) {
                Thread.sleep(2000);
            } else if (CUSTOM.equals(exceptionTag)) {
                return new EventEnvelope().setStatus(400).setBody(Map.of("error", "non-standard-format"));
            } else {
                // just testing throwing an exception through a Mono reactive response
                return Mono.create(emitter -> emitter.error(new AppException(403, "just a test")));
            }
        }
        if (input.containsKey(USER) && input.containsKey(GREETING)) {
            String greeting = input.get(GREETING).toString();
            String user = input.get(USER).toString();
            Map<String, Object> result = new HashMap<>();
            result.put(USER, user);
            result.put(GREETING, greeting);
            result.put(MESSAGE, "I got your greeting message - "+greeting);
            result.put(TIME, new Date());
            result.put(ORIGINAL, input);
            if (headers.containsKey(DEMO)) {
                result.put(DEMO+1, headers.get(DEMO));
            }
            if (headers.containsKey(USER)) {
                result.put(DEMO+2, headers.get(USER));
            }
            if (headers.containsKey(X_FLOW_ID)) {
                result.put(DEMO+3, headers.get(X_FLOW_ID));
            }
            return new EventEnvelope().setBody(result).setHeader(DEMO, "test-header").setStatus(201);

        } else {
            // the easiest way for error handling is just throwing an exception
            throw new IllegalArgumentException("Missing user or greeting");
        }
    }
}
