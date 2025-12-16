/*

    Copyright 2018-2025 Accenture Technology

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

package com.accenture.services.plugins.generators;

import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@SimplePlugin
public class DateGenerator implements PluginFunction {

    @Override
    public String getName() {
        return "dateTime";
    }

    @Override
    public Object calculate(Object... input) {
        DateTimeFormatter dateFormatter = (input.length == 1)?
                DateTimeFormatter.ofPattern(String.valueOf(input[0])) : DateTimeFormatter.ISO_DATE_TIME;
        ZoneId zone = (input.length == 2)? ZoneId.of(String.valueOf(input[1])) : ZoneId.systemDefault();
        return ZonedDateTime.now(zone).format(dateFormatter);
    }
}
