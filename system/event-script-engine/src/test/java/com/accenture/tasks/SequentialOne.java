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

import com.accenture.models.PoJo;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import java.util.Map;

@PreLoad(route="sequential.one", instances=10)
public class SequentialOne implements TypedLambdaFunction<Map<String, Object>, PoJo> {
    private static final String USER = "user";
    private static final String SEQ = "sequence";

    @Override
    public PoJo handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (input.containsKey(USER) && input.containsKey(SEQ)) {
            return new PoJo(input.get(USER).toString(), Integer.parseInt(input.get(SEQ).toString()));

        } else {
            throw new IllegalArgumentException("Missing user or sequence");
        }
    }
}
