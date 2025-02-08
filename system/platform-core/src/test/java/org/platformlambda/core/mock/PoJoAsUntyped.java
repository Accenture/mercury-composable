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

package org.platformlambda.core.mock;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.models.PoJo;
import org.platformlambda.core.serializers.SimpleMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
@PreLoad(route = "input.pojo.untyped", inputPojoClass = PoJo.class)
public class PoJoAsUntyped implements LambdaFunction {
    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) throws Exception {
        var mapper = SimpleMapper.getInstance().getMapper();
        Map<String, Object> result = new HashMap<>();
        // prove that pojo is converted automatically using inputPojoClass in the annotation
        if (input instanceof PoJo) {
            result.put("map", mapper.readValue(input, Map.class));
        }
        // it also works if input is a list of pojo
        var list = new ArrayList<Map<String, Object>>();
        result.put("list", list);
        if (input instanceof List) {
            var itemList = (List<Object>) input;
            for (Object o: itemList) {
                if (o instanceof PoJo) {
                    list.add(mapper.readValue(o, Map.class));
                } else {
                    list.add(null);
                }
            }
        }
        return result;
    }
}
