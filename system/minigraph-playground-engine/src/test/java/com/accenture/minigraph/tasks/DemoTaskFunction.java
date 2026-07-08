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

package com.accenture.minigraph.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * A composable function for graph.task unit tests.
 * It echoes the request body and the "hello" request header, doubles the "amount" field
 * and returns a response header. When the request body contains "exception", it throws
 * an AppException to exercise the error path.
 */
@PreLoad(route = "v1.demo.task", instances = 10)
public class DemoTaskFunction implements TypedLambdaFunction<Map<String, Object>, Object> {

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
                                throws AppException {
        if (input.containsKey("exception")) {
            throw new AppException(400, "just a test");
        }
        var result = new HashMap<String, Object>();
        result.put("received", input);
        if (headers.containsKey("hello")) {
            result.put("hello_header", headers.get("hello"));
        }
        if (input.get("amount") instanceof Number n) {
            result.put("doubled", n.doubleValue() * 2);
        }
        return new EventEnvelope().setBody(result).setHeader("x-task", "demo");
    }
}
