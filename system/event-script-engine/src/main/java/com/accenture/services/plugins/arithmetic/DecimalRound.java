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
 * f:decimalRound(x, scale, mode) - rounding is always explicit: mode is text(HALF_UP), HALF_EVEN, HALF_DOWN, UP, DOWN, CEILING or FLOOR,
 * and the result has exactly that scale. The one- and two-argument forms are refused; f:round is the half-up double companion.
 */
@SimplePlugin
public class DecimalRound implements PluginFunction {

    @Override
    public Object calculate(Object... input) {
        if (input.length != 3) {
            throw new IllegalArgumentException("decimalRound takes three arguments, decimalRound(x, scale, mode), " +
                    "got " + input.length);
        }
        var x = DecimalPluginUtils.operand(input[0], "decimalRound");
        var scale = DecimalPluginUtils.operand(input[1], "decimalRound");
        return DecimalPluginUtils.canonical(DecimalPluginUtils.round(x, scale, input[2]));
    }
}
