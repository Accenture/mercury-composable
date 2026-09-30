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

import java.math.BigDecimal;

/**
 * f:decimalMod(a, b) - the exact remainder as a canonical decimal string. Division by zero is an error.
 */
@SimplePlugin
public class DecimalMod implements PluginFunction {

    @Override
    public Object calculate(Object... input) {
        if (input.length != 2) {
            throw new IllegalArgumentException("Expected two numbers for decimalMod");
        }
        return DecimalPluginUtils.canonical(DecimalPluginUtils.remainder(
                DecimalPluginUtils.operand(input[0], "decimalMod"), DecimalPluginUtils.operand(input[1], "decimalMod")));
    }
}
