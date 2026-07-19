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
import com.accenture.util.SimplePluginUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * f:round(number[, decimal_places]) - half-up rounding (ties away from zero)
 * applied to the number's SHORTEST decimal representation
 * (BigDecimal.valueOf), so binary floating-point representation error does
 * not leak into the rounding decision: 1.005 rounds to 1.01 at 2 places,
 * where a naive multiply-round-divide would give 1.0. A whole-number input
 * is already exact and passes through unchanged. Companion to the numeric
 * promotion of the arithmetic family - use it to tame floating-point
 * precision artifacts after decimal arithmetic.
 */
@SimplePlugin
public class RoundNumbers implements PluginFunction {

    @Override
    public String getName() {
        return "round";
    }

    @Override
    public Object calculate(Object... input) {
        if (input.length < 1 || input.length > 2) {
            throw new IllegalArgumentException("Expected a number and optional decimal places to round");
        }
        int decimalPlaces = 0;
        if (input.length == 2) {
            if (SimplePluginUtils.promoteNumber(input[1]) instanceof Long dp && dp >= 0) {
                decimalPlaces = dp.intValue();
            } else {
                throw new IllegalArgumentException("Decimal places for round must be a whole number >= 0");
            }
        }
        Number n = SimplePluginUtils.promoteNumber(input[0]);
        if (n instanceof Long whole) {
            // a whole number is already exact - rounding never changes it
            return whole;
        }
        return BigDecimal.valueOf(n.doubleValue()).setScale(decimalPlaces, RoundingMode.HALF_UP).doubleValue();
    }
}
