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

package com.accenture.demo.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@PreLoad(route="v1.hello.exception", instances=10)
public class HelloException implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(HelloException.class);

    private static final String TYPE = "type";
    private static final String ERROR = "error";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private static final String STACK = "stack";

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (input.containsKey(STACK)) {
            // set stack trace as a map. It will be printed as pretty JSON when log.format=json
            var map = Utility.getInstance().stackTraceToMap(String.valueOf(input.get(STACK)));
            log.info("{}", map);
        }
        if (input.containsKey(STATUS) && input.containsKey(MESSAGE)) {
            log.info("User defined exception handler - status={} error={}", input.get(STATUS), input.get(MESSAGE));
            Map<String, Object> error = new HashMap<>();
            error.put(TYPE, ERROR);
            error.put(STATUS, input.get(STATUS));
            error.put(MESSAGE, input.get(MESSAGE));
            return error;
        } else {
            return Collections.emptyMap();
        }
    }
}
