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

package com.accenture.minigraph.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.AppConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@PreLoad(route = "graph.exception.handler")
public class GraphExceptionHandler implements TypedLambdaFunction<Map<String, Object>, Object> {
    private static final Logger log = LoggerFactory.getLogger(GraphExceptionHandler.class);
    private static final String TYPE = "type";
    private static final String TASK = "task";
    private static final String STACK = "stack";
    private static final String ERROR = "error";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private final boolean isDevEnv;

    public GraphExceptionHandler() {
        var config = AppConfigReader.getInstance();
        this.isDevEnv = "dev".equals(config.getProperty("app.env", "dev"));
    }

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (input.containsKey(STATUS) && input.containsKey(MESSAGE)) {
            // log error only in the dev environment
            if (isDevEnv) printError(input);
            if (input.get(MESSAGE) instanceof Map<?,?> map) {
                // remove stack, use original content and status code
                var result = new HashMap<>();
                map.forEach((key, value) -> {
                    if (!STACK.equals(key)) {
                        result.put(key, value);
                    }
                });
                result.put(STATUS, input.get(STATUS));
                return result;
            } else {
                // standard error format
                Map<String, Object> error = new HashMap<>();
                error.put(STATUS, input.get(STATUS));
                error.put(MESSAGE, input.get(MESSAGE));
                error.put(TYPE, ERROR);
                return error;
            }
        } else {
            return Collections.emptyMap();
        }
    }

    private void printError(Map<String, Object> input) {
        Object stack = input.get(STACK);
        Object task = input.get(TASK);
        if (stack instanceof String text) {
            log.error("User defined exception handler received from {}, rc={}, error={}, stack={}",
                    task instanceof String name? name : "previous task",
                    input.get(STATUS), input.get(MESSAGE), text);
        } else {
            log.error("User defined exception handler received from {}, rc={}, error={}",
                    task instanceof String name? name : "previous task",
                    input.get(STATUS), input.get(MESSAGE));
        }
    }
}
