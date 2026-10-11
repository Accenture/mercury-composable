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
 * The lower bound of one range - 'f:minRange(range)' - typed by the notation.
 * <p>
 * The range is written as the bands of 'rangeLookup' are: {@code min..max} runs from {@code min} inclusive up to
 * but excluding {@code max}, and {@code min..} or {@code ..max} leaves an end open. A numeric bound is returned as
 * a number - a whole number within the 64-bit range as a long, any other number as a double - and a text bound as
 * text, exactly as written. An open end has no bound: {@code ..2500} answers the optional second argument, or
 * null when there is none, so the null-source rule of a data mapping leaves the target unset.
 * <p>
 * A malformed range, a range that mixes a numeric and a text bound, and an empty range (its lower bound not below
 * its upper bound) are refused by name, exactly as 'rangeLookup' refuses such a band in a table.
 */
@SimplePlugin
public class SimpleMinRange implements PluginFunction {
    private static final String NAME = "minRange";

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Read the lower bound of a range
     *
     * @param input - first argument is the range written as text,
     *              optional second argument is the default value when the lower end is open
     * @return the lower bound as a number or text, otherwise the default value or null when there is none
     */
    @Override
    public Object calculate(Object... input) {
        return RangeNotation.bound(input, true);
    }
}
