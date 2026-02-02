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

import com.accenture.util.TypeConversionUtils;
import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class LongConversion implements PluginFunction {

    @Override
    public String getName() {
        return "long";
    }

    @Override
    public Object calculate(Object... input) {
        if (input.length == 0) {
            throw new IllegalArgumentException("Input is required for Long conversion");
        } else if (input.length == 1) {
            return TypeConversionUtils.convertLong(input[0]);
        }
        return Arrays.stream(input)
                .map(TypeConversionUtils::convertLong)
                .toList();
    }
}
