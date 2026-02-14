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

import java.util.Collections;
import java.util.List;

@SimplePlugin
public class UpdateListOfMap implements PluginFunction {

    /**
     * Merge additional JSON-Path result list into a list of Maps from a prior ListOfMap result
     * <p>
     * Merge this data structure:
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
     * with:
     *  {
     *     "more": [
     *       "X",
     *       "Y",
     *       "Z"
     *     ]
     *   }
     * becomes:
     *   [
     *     {
     *       "world": 1,
     *       "test": "a",
     *       "more": "X"
     *     },
     *     {
     *       "world": 2,
     *       "test": "b",
     *       "more": "Y"
     *     },
     *     {
     *       "world": 3,
     *       "test": "c",
     *       "more": "Z"
     *     }
     *   ]
     *
     * @param input where the first argument is a list of maps from a result of ListOfMap plugin
     *              and subsequent arguments are map of lists from JSON-Path search
     * @return merged list of maps
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object calculate(Object... input) {
        if (input.length > 1 && TypeConversionUtils.validateFirstArgument(input[0])) {
            var toBeMerged = TypeConversionUtils.prepareMerge(input);
            if (!toBeMerged.isEmpty()) {
                var first = (List<Object>) input[0];
                return TypeConversionUtils.merge(first, toBeMerged);
            }
        }
        return Collections.emptyList();
    }
}
