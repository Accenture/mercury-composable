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

@SimplePlugin
public class AddMonths implements PluginFunction {
    private static final String NAME = "addMonths";

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Add a number of calendar months to a date
     *
     * @param input - first argument is the date written as YYYY-MM-DD,
     *              second argument is the whole number of months, negative for an earlier date
     * @return the resulting date written as YYYY-MM-DD, with the same day of the month,
     *         or the last day of the resulting month when that month is shorter
     */
    @Override
    public Object calculate(Object... input) {
        DatePluginUtils.expectTwo(input, NAME);
        var date = DatePluginUtils.date(input[0], NAME);
        var months = DatePluginUtils.months(input[1], NAME);
        return DatePluginUtils.text(date.plusMonths(months), NAME);
    }
}
