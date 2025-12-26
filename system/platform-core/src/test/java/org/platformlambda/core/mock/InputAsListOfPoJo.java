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

package org.platformlambda.core.mock;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.PoJo;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PreLoad(route = "input.list.of.pojo.java", inputPojoClass = PoJo.class)
public class InputAsListOfPoJo implements TypedLambdaFunction<List<PoJo>, Object> {
    @Override
    public Object handleEvent(Map<String, String> headers, List<PoJo> input, int instance) throws Exception {
        List<String> names = new ArrayList<>();
        // prove that the list of pojo is correctly deserialized
        for (PoJo o: input) {
            if (o != null) {
                names.add(o.getName());
            } else {
                names.add("null");
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("names", names);
        return result;
    }
}
