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

package com.accenture.services.plugins.generators;

import com.accenture.models.PluginFunction;
import com.accenture.models.SimplePlugin;
import com.accenture.util.TypeConversionUtils;

@SimplePlugin
public class Now implements PluginFunction {
    private static final String ISO = "iso";
    private static final String LOCAL = "local";
    private static final String MS = "ms";

    @Override
    public String getName() {
        return "now";
    }

    @Override
    public Object calculate(Object... input) {
        var command = input.length > 0 ? String.valueOf(input[0]) : ISO;
        if (LOCAL.equalsIgnoreCase(command)) {
            return TypeConversionUtils.getLocalTimestamp();
        } else if (MS.equalsIgnoreCase(command)) {
            return System.currentTimeMillis();
        } else if (ISO.equalsIgnoreCase(command)) {
            return TypeConversionUtils.getIsoTimestamp();
        } else {
            throw new IllegalArgumentException("Supported types are: iso, local, ms");
        }
    }
}
