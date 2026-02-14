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
import com.accenture.util.TypeConversionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SimplePlugin
public class RemoveKey implements PluginFunction {

    /**
     * The first argument is a map or a list of maps.
     * The subsequent argument(s) must be the key(s) to be removed.
     *
     * @param input for one or keys to be removed from the content of the first argument
     * @return updated map or list from the first argument
     */
    @Override
    public Object calculate(Object... input) {
        if (input.length > 1) {
            if (input[0] instanceof Map) {
                return getMapWithDroppedKeys(input[0], input);
            }
            if (input[0] instanceof List<?> list) {
                var updated = new ArrayList<>();
                for (Object o: list) {
                    if (o instanceof Map) {
                        updated.add(getMapWithDroppedKeys(o, input));
                    } else {
                        updated.add(o);
                    }
                }
                return updated;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapWithDroppedKeys(Object first, Object[] input) {
        var map = (Map<String, Object>) first;
        var found = false;
        for (int i = 1; i < input.length; i++) {
            if (map.containsKey(String.valueOf(input[i]))) {
                found = true;
                break;
            }
        }
        if (found) {
            var copy = TypeConversionUtils.deepCopy(map);
            for (int i = 1; i < input.length; i++) {
                copy.remove(String.valueOf(input[i]));
            }
            return copy;
        } else {
            return map;
        }
    }
}
