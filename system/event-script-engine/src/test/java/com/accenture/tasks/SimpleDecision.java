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

import java.util.Map;

@PreLoad(route="simple.decision", instances=10)
public class SimpleDecision implements TypedLambdaFunction<Map<String, Object>, Boolean> {

    private static final String DECISION = "decision";

    @Override
    public Boolean handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (input.containsKey(DECISION)) {
            return "true".equals(input.get(DECISION).toString());

        } else {
            throw new IllegalArgumentException("Missing decision");
        }
    }
}