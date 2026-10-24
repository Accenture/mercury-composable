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
 * The date a number of days later - 'f:addDays(date, days)'.
 * <p>
 * The date is an ISO-8601 calendar date written as {@code YYYY-MM-DD}, with no time and no time zone, and so is
 * the result: {@code f:addDays(text(2027-01-15), int(15))} is {@code 2027-01-30}. The days are a whole number - a
 * negative one gives an earlier date - written as a number or as numeric text ({@code 15}, {@code "15"}; {@code 15.0}
 * is whole too). Months and years are calendar ones, so 2028-02-28 plus 1 day is 2028-02-29.
 * <p>
 * A date of another shape (a date-time, a time zone, a single-digit month), a day the calendar lacks
 * ({@code 2027-02-29}), a fractional or non-numeric number of days, a null argument, and a result outside the years
 * 0000 to 9999 are refused by name.
 */
@SimplePlugin
public class AddDays implements PluginFunction {
    private static final String NAME = "addDays";

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Add a number of days to a date
     *
     * @param input - first argument is the date written as YYYY-MM-DD,
     *              second argument is the whole number of days, negative for an earlier date
     * @return the resulting date written as YYYY-MM-DD
     */
    @Override
    public Object calculate(Object... input) {
        DatePluginUtils.expectTwo(input, NAME);
        var date = DatePluginUtils.date(input[0], NAME);
        var days = DatePluginUtils.days(input[1], NAME);
        return DatePluginUtils.text(date.plusDays(days), NAME);
    }
}
