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

import com.accenture.util.SimplePluginUtils;
import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

@SimplePlugin
public class GreaterThanOperator implements PluginFunction {

    @Override
    public String getName() {
        return "gt";
    }

    @Override
    public Object calculate(Object... input) {
        if (input.length != 2) {
            throw new IllegalArgumentException("Input is required to compare using 'Greater Than'");
        }
        Number first = SimplePluginUtils.promoteNumber(input[0]);
        Number second = SimplePluginUtils.promoteNumber(input[1]);
        // both whole => exact long comparison; otherwise numeric promotion to double
        if (first instanceof Long a && second instanceof Long b) {
            return a > b;
        }
        return first.doubleValue() > second.doubleValue();
    }
}
