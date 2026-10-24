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

package com.accenture.services.plugins.date;

import com.accenture.models.PluginFunction;
import com.accenture.models.SimplePlugin;
import com.accenture.util.DatePluginUtils;

/**
 * The whole days between two dates - 'f:daysBetween(from, to)'.
 * <p>
 * Both dates are ISO-8601 calendar dates written as {@code YYYY-MM-DD}, with no time and no time zone. The count
 * excludes the earlier date and includes the later one, so the same date twice gives 0, two consecutive days give 1,
 * and {@code 2027-04-10} to {@code 2027-05-11} gives 31. The order of the arguments does not matter: the answer is
 * the same either way, never negative, and an integer.
 * <p>
 * A date of another shape (a date-time, a time zone, a single-digit month), a day the calendar lacks
 * ({@code 2027-02-29}) and a null argument are refused by name.
 */
@SimplePlugin
public class DaysBetween implements PluginFunction {
    private static final String NAME = "daysBetween";

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Count the days between two dates
     *
     * @param input - two dates written as YYYY-MM-DD, in either order
     * @return the whole number of days from the earlier date (excluded) to the later date (included)
     */
    @Override
    public Object calculate(Object... input) {
        DatePluginUtils.expectTwo(input, NAME);
        var from = DatePluginUtils.date(input[0], NAME);
        var to = DatePluginUtils.date(input[1], NAME);
        return DatePluginUtils.daysBetween(from, to);
    }
}
