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

package com.accenture.minigraph.mock;

import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * A demo composable function for tutorial-13 (the graph.task skill).
 * It composes a greeting from the "name" field, doubles the "amount" field
 * and echoes the "x-app" request header.
 */
@OptionalService("app.env=dev")
@PreLoad(route = "v1.hello.task", instances = 50)
public class HelloTask implements TypedLambdaFunction<Map<String, Object>, Object> {

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        var result = new HashMap<String, Object>();
        result.put("greeting", "Hello, " + input.getOrDefault("name", "stranger"));
        if (input.get("amount") instanceof Number n) {
            result.put("doubled", n.doubleValue() * 2);
        }
        if (headers.containsKey("x-app")) {
            result.put("app", headers.get("x-app"));
        }
        return result;
    }
}
