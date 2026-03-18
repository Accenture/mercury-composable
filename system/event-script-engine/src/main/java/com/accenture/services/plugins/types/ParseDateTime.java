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

package com.accenture.services.plugins.types;

import com.accenture.models.PluginFunction;
import com.accenture.models.SimplePlugin;
import com.accenture.util.TypeConversionUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@SimplePlugin
public class ParseDateTime implements PluginFunction {
    private static final String ISO = "iso";
    private static final String LOCAL = "local";
    private static final String MS = "ms";

    @Override
    public String getName() {
        return "parseDateTime";
    }

    @Override
    public Object calculate(Object... input) {
        if (input.length == 2 && input[1] instanceof String text) {
            var value = String.valueOf(input[0]);
            var rules = TypeConversionUtils.getRules(text);
            if (rules.isEmpty()) {
                throw new IllegalArgumentException("Invalid validation rule. Syntax: text(pattern, iso | local | ms)");
            }
            var dateFormatter = DateTimeFormatter.ofPattern(rules.getFirst());
            Instant instant = LocalDateTime.parse(value, dateFormatter).atZone(ZoneId.systemDefault()).toInstant();
            var date = Date.from(instant);
            var type = rules.size() > 1? rules.get(1) : ISO;
            if (LOCAL.equalsIgnoreCase(type)) {
                return TypeConversionUtils.getLocalTimestamp(date.getTime());
            } else if (MS.equalsIgnoreCase(type)) {
                return date.getTime();
            } else if (ISO.equalsIgnoreCase(type)) {
                return TypeConversionUtils.getIsoTimestamp(date);
            } else {
                throw new IllegalArgumentException("Supported types are: iso, local, ms");
            }
        } else {
            throw new IllegalArgumentException("Syntax error. Syntax: text(pattern, iso | local | ms)." +
                    "e.g. f:parseDateTime(input.body.datetime, text(MM/dd/yyyy HH:mm:ss; ms))");
        }
    }
}
