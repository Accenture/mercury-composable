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

package org.platformlambda.opentelemetry.mock;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

/**
 * First task of the Level-2 demo flow ({@code otel-trace-test}). In Event Script the engine
 * orchestrates task-to-task routing, so the function only computes and returns - no PostOffice relay.
 */
@PreLoad(route = "task.1", instances = 5)
public class Task1 implements TypedLambdaFunction<Map<String, Object>, Object> {

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        return Map.of("step", "task.1");
    }
}
