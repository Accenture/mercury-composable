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

import java.util.List;
import java.util.Map;

@SimplePlugin
public class ListOfMap implements PluginFunction {

    /**
     * Re-arrange list elements into a list of maps
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
     * {
     *   "hello": [
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
     * }
     *
     * @param input data structure containing list of items
     * @return re-arranged data structure containing list of maps
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object calculate(Object... input) {
        if (input.length != 2) {
            throw new IllegalArgumentException("Input must have two arguments");
        }
        if (input[0] instanceof Map && input[1] instanceof String) {
            var map = (Map<String, Object>) input[0];
            var key = String.valueOf(input[1]);
            return normalize(new MultiLevelMap(map), key).getMap();
        } else {
            return input[0];
        }
    }

    private MultiLevelMap normalize(MultiLevelMap mm, String key) {
        var target = new MultiLevelMap();
        var map = mm.getElement(key);
        if (map instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof List<?> items) {
                    for (int i=0; i<items.size(); i++) {
                        target.setElement(key+"["+i+"]." + entry.getKey(), items.get(i));
                    }
                }
            }
        }
        return target.getMap().isEmpty()? mm : target;
    }
}
