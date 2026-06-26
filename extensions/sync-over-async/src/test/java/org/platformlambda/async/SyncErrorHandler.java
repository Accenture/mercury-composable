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

package org.platformlambda.async;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * Flow exception handler for {@code sync-to-async}. The flow maps {@code error.code -> status} and
 * {@code error.message -> message} into this task; the returned {@code status} is mapped back to the
 * HTTP status ({@code result.status -> output.status}). A 408 from {@code test.endpoint} therefore
 * surfaces as an HTTP 408.
 */
@PreLoad(route = "sync.error.handler", instances = 5)
public class SyncErrorHandler implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        Map<String, Object> error = new HashMap<>();
        error.put("type", "error");
        error.put("status", input.getOrDefault("status", 500));
        error.put("message", input.getOrDefault("message", "Internal error"));
        return error;
    }
}
