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

package com.accenture.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@PreLoad(route="simple.exception.handler", instances=10)
public class SimpleExceptionHandler implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(SimpleExceptionHandler.class);

    private static final String TYPE = "type";
    private static final String ERROR = "error";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (input.containsKey(STATUS) && input.containsKey(MESSAGE)) {
            Object stack = input.get("stack");
            if (stack instanceof String text) {
                log.error("User defined exception handler got {}, rc={}, error={}, stack={}",
                        headers, input.get(STATUS), input.get(MESSAGE), text);
            } else {
                log.error("User defined exception handler got {}, rc={}, error={}",
                            headers, input.get(STATUS), input.get(MESSAGE));
            }
            Map<String, Object> error = new HashMap<>();
            error.put(STATUS, input.get(STATUS));
            error.put(MESSAGE, input.get(MESSAGE));
            error.put(TYPE, ERROR);
            return error;
        } else {
            return Collections.emptyMap();
        }
    }
}
