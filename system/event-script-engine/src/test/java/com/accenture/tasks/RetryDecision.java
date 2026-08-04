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

package com.accenture.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * The retry counter of the budgeted-retry test flow (retry-subflow-test): each invocation counts
 * one failed sub-flow attempt and decides whether the remaining budget allows another try. The
 * flow's decision task routes true back to the sub-flow task and false to the give-up branch.
 */
@PreLoad(route = "retry.decision", instances = 10)
public class RetryDecision implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        int attempts = input.get("attempts") instanceof Number n ? n.intValue() : 0;
        int failed = attempts + 1;
        Map<String, Object> result = new HashMap<>();
        result.put("attempts", failed);
        result.put("retry", failed < 3);
        if (input.get("status") instanceof Number status) {
            result.put("lastStatus", status.intValue());
        }
        return result;
    }
}
