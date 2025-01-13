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

import java.util.Map;

@PreLoad(route="decision.case", instances=10)
public class DecisionCase implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    private static final String EXCEPTION = "exception";
    private static final String BREAK = "break";
    private static final String CONTINUE = "continue";
    private static final String INCREMENT = "increment";
    private static final String JUMP = "jump";

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) throws AppException {
        Utility util = Utility.getInstance();
        if (input.containsKey(EXCEPTION)) {
            throw new AppException(400, String.valueOf(input.get(EXCEPTION)));
        }
        int n = util.str2int(String.valueOf(input.get("n")));
        if (input.containsKey(INCREMENT)) {
            n++;
            input.put("n", n);
        }
        if (input.containsKey(CONTINUE)) {
            int skip = util.str2int(String.valueOf(input.get(CONTINUE)));
            if (n == skip) {
                input.put("continue", true);
            }
        }
        // 'break' or 'jump' from unit test will set different condition (model.quit or model.jump)
        // Either of the condition would result in a "break" action.
        if (input.containsKey(BREAK)) {
            int breakAt = util.str2int(String.valueOf(input.get(BREAK)));
            if (n == breakAt) {
                input.put("quit", true);
            }
        }
        if (input.containsKey(JUMP)) {
            int skip = util.str2int(String.valueOf(input.get(JUMP)));
            if (n == skip) {
                input.put("jump", true);
            }
        }
        return input;
    }
}
