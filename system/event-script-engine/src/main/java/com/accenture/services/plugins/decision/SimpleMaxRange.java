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
package com.accenture.services.plugins.decision;

import com.accenture.models.PluginFunction;
import com.accenture.models.SimplePlugin;
import com.accenture.util.RangeNotation;

/**
 * The upper bound of one range - 'f:maxRange(range)' - typed by the notation.
 * <p>
 * The range is written as the bands of 'rangeLookup' are: {@code min..max} runs from {@code min} inclusive up to
 * but excluding {@code max}, so the upper bound is the first value the range leaves out, and {@code min..} or
 * {@code ..max} leaves an end open. A whole number within the 64-bit range is returned as a long and any other
 * number as its exact decimal text ({@code 7.5} - the high-precision rule of DECIMAL; f:double converts it when a
 * floating-point output value is wanted); a text bound is returned as written, a quoted one without its quotes.
 * An open end has no bound: {@code 25000..} answers the optional second argument, or null when there is none, so the null-source
 * rule of a data mapping leaves the target unset.
 * <p>
 * A malformed range, a band given as a list, a range mixing kinds (a number and text, or quoted and unquoted
 * text), and an empty range (its lower bound not below its upper bound) are refused by name, exactly as 'rangeLookup'
 * refuses such a band in a table.
 */
@SimplePlugin
public class SimpleMaxRange implements PluginFunction {
    private static final String NAME = "maxRange";

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Read the upper bound of a range
     *
     * @param input - first argument is the range written as text,
     *              optional second argument is the default value when the upper end is open
     * @return the upper bound as a number or text, otherwise the default value or null when there is none
     */
    @Override
    public Object calculate(Object... input) {
        return RangeNotation.bound(input, false);
    }
}
