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

import java.util.List;
import java.util.Map;

@PreLoad(route = "wildcard.conversion", inputPojoClass = PoJo.class)
public class WildcardConversion implements TypedLambdaFunction<List<PoJo>, List<PoJo>> {

    @Override
    public List<PoJo> handleEvent(Map<String, String> headers, List<PoJo> input, int instance) throws Exception {
        assert input != null;
        assert input.getFirst() != null;
        return input;
    }
}
