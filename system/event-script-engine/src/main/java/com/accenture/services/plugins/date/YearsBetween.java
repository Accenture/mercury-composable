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
public class YearsBetween implements PluginFunction {
    private static final String NAME = "yearsBetween";

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Count the whole calendar years between two dates
     *
     * @param input - two dates written as YYYY-MM-DD, in either order
     * @return the whole number of years completed from the earlier date to the later date,
     *         a year being complete when the later date reaches the earlier date's month and day
     */
    @Override
    public Object calculate(Object... input) {
        DatePluginUtils.expectTwo(input, NAME);
        var from = DatePluginUtils.date(input[0], NAME);
        var to = DatePluginUtils.date(input[1], NAME);
        return DatePluginUtils.yearsBetween(from, to);
    }
}
