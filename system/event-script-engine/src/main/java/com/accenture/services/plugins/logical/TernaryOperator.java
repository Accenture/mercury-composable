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

package com.accenture.services.plugins.logical;

import com.accenture.utils.TypeConversionUtils;
import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class TernaryOperator implements PluginFunction {

    @Override
    public String getName() {
        return "ternary";
    }

    @Override
    public Object calculate(Object... input) {
        if (input.length != 3) {
            throw new IllegalArgumentException("Three parts are required for Ternary Operation - got: " + Arrays.toString(input));
        }
        boolean condition = TypeConversionUtils.convertBoolean(input[0]);
        return condition? input[1] : input[2];
    }
}
