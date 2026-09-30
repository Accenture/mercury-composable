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

package com.accenture.services.plugins.arithmetic;

import com.accenture.models.PluginFunction;
import com.accenture.models.SimplePlugin;
import com.accenture.util.DecimalPluginUtils;

/**
 * f:decimalSubtract(a, b, ...) - a minus the remaining arguments, exactly, as a canonical decimal string.
 */
@SimplePlugin
public class DecimalSubtract implements PluginFunction {

    @Override
    public Object calculate(Object... input) {
        if (input.length < 2) {
            throw new IllegalArgumentException("Expected at least two numbers to subtract");
        }
        var total = DecimalPluginUtils.operand(input[0], "decimalSubtract");
        for (int i = 1; i < input.length; i++) {
            total = DecimalPluginUtils.subtract(total, DecimalPluginUtils.operand(input[i], "decimalSubtract"));
        }
        return DecimalPluginUtils.canonical(total);
    }
}
