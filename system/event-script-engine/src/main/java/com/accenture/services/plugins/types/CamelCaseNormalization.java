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
import com.accenture.util.KeyNormalizationUtils;

/**
 * f:camelCase(mapOrList) - normalize every key of a map (recursively, including maps
 * inside lists) to camelCase. For legacy payloads whose key formats vary per source
 * (MyExampleKey, My_Example_key, my_example_Key all become myExampleKey). Values are
 * never touched. Algorithm and edge cases: {@link KeyNormalizationUtils}.
 */
@SimplePlugin
public class CamelCaseNormalization implements PluginFunction {

    @Override
    public String getName() {
        return "camelCase";
    }

    @Override
    public Object calculate(Object... input) {
        return KeyNormalizationUtils.normalize(input, getName(), KeyNormalizationUtils::toCamelCase);
    }
}
