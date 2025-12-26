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

package com.accenture.services.plugins.logical;

import com.accenture.utils.SimplePluginUtils;
import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

@SimplePlugin
public class LessThanOperator implements PluginFunction {

    @Override
    public String getName() {
        return "lt";
    }

    @Override
    public Object calculate(Object... input) {
        if (input.length != 2) {
            throw new IllegalArgumentException("Input is required to compare using 'Less Than'");
        }
        Long first = SimplePluginUtils.promoteNumber(input[0]);
        Long second = SimplePluginUtils.promoteNumber(input[1]);
        return first < second;
    }
}
