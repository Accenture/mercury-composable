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
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@PreLoad(route="v1.circuit.breaker", instances=20)
public class SimpleCircuitBreaker implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(SimpleCircuitBreaker.class);

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        Utility util = Utility.getInstance();
        int maxAttempts = util.str2int(input.getOrDefault("max_attempts", "1").toString());
        int attempt = util.str2int(input.getOrDefault("attempt", "0").toString());
        int status = util.str2int(input.getOrDefault("status", "200").toString());
        String message = input.getOrDefault("message", "unknown").toString();
        // retry when attempt < maxAttempts
        boolean retry = attempt < maxAttempts;
        int attempted = attempt + 1;
        Map<String, Object> result = new HashMap<>();
        result.put("decision", retry);
        result.put("attempt", attempted);
        result.put("status", status);
        result.put("message", message);
        if (retry) {
            log.info("Retry {}", attempted);
        } else {
            log.info("Abort");
        }
        return result;
    }
}
