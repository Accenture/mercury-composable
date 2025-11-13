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
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.Utility;
import java.util.HashMap;
import java.util.Map;

@PreLoad(route = "exception.simulator", instances=10)
public class ExceptionSimulator implements TypedLambdaFunction<Map<String, Object>, Object> {
    private static final String EXCEPTION = "exception";
    private static final String ACCEPT = "accept";
    private static final String ATTEMPT = "attempt";

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws AppException {
        // throw exception as requested
        if (headers.containsKey(EXCEPTION)) {
            int code = Utility.getInstance().str2int(headers.get(EXCEPTION));
            throw new AppException(code == -1? 400 : code, "Simulated Exception");
        }
        // throw exception when accept != attempt
        if (input.containsKey(ACCEPT)) {
            Utility util = Utility.getInstance();
            int accept = util.str2int(input.get(ACCEPT).toString());
            int attempt = util.str2int(input.getOrDefault(ATTEMPT, 0).toString());
            if (attempt == accept) {
                Map<String, Object> result = new HashMap<>();
                result.put("attempt", attempt);
                result.put("message", "Task completed successfully");
                return result;
            } else {
                throw new IllegalArgumentException("Demo Exception");
            }
        }
        // just echo input and headers when there is no need to throw exception
        return new EventEnvelope().setBody(input).setHeaders(headers);
    }
}
