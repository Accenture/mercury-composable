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

package com.accenture.services.plugins.types;

import com.accenture.models.PluginFunction;
import com.accenture.models.SimplePlugin;
import org.platformlambda.core.util.MultiLevelMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@SimplePlugin
public class ListOfMap implements PluginFunction {
    private static final String DATA = "d";

    /**
     * Convert map of lists to list of maps
     * <p>
     * e.g. FROM this data structure:
     * {
     *   "hello": {
     *     "world": [
     *       1,
     *       2,
     *       3
     *     ],
     *     "test": [
     *       "a",
     *       "b",
     *       "c"
     *     ]
     *   }
     * }
     * <p>
     * TO this data structure:
     *   [
     *     {
     *       "world": 1,
     *       "test": "a"
     *     },
     *     {
     *       "world": 2,
     *       "test": "b"
     *     },
     *     {
     *       "world": 3,
     *       "test": "c"
     *     }
     *   ]
     *
     * @param input data structure containing a map of lists
     * @return re-arranged data structure containing list of maps or empty list if not resolved
     */
    @Override
    public Object calculate(Object... input) {
        System.out.println(input[0]);
        if (input.length == 1 && input[0] instanceof Map<?, ?> data) {
            var map = findMapOfLists(data);
            if (!map.isEmpty()) {
                return normalize(map);
            }
        }
        return Collections.emptyList();
    }

    private Object normalize(Map<?, ?> map) {
        var target = new MultiLevelMap();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof List<?> items) {
                for (int i=0; i < items.size(); i++) {
                    target.setElement(DATA+"["+i+"]." + entry.getKey(), items.get(i));
                }
            }
        }
        return target.getMap().isEmpty()? Collections.emptyList() : target.getElement(DATA);
    }

    private Map<?, ?> findMapOfLists(Map<?, ?> map) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof List) {
                return map;
            }
            if (entry.getValue() instanceof Map<?, ?> inner) {
                return findMapOfLists(inner);
            }
        }
        return Collections.emptyMap();
    }
}
