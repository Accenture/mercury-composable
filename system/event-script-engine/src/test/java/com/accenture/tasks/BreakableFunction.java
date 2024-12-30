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
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.Utility;

import java.util.HashMap;
import java.util.Map;

@PreLoad(route="breakable.function", instances=20)
public class BreakableFunction implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) throws AppException {
        Utility util = Utility.getInstance();
        int accept = util.str2int(input.getOrDefault("accept", "0").toString());
        int attempt = util.str2int(input.getOrDefault("attempt", "0").toString());
        if (attempt == accept) {
            Map<String, Object> result = new HashMap<>();
            result.put("attempt", attempt);
            result.put("message", "Task completed successfully");
            return result;
        }
        throw new AppException(400, "Just a demo exception for circuit breaker to handle");
    }
}
