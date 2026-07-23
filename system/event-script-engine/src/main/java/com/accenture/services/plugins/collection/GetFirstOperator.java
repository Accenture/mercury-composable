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

package com.accenture.services.plugins.collection;

import com.accenture.models.PluginFunction;
import com.accenture.models.SimplePlugin;

import java.util.List;

@SimplePlugin
public class GetFirstOperator implements PluginFunction {

    @Override
    public String getName() {
        return "getFirst";
    }

    @Override
    public Object calculate(Object... input) {
        if (input == null || input.length != 1) {
            throw new IllegalArgumentException("One input is required to get first item from list");
        }

        Object value = input[0];

        if (value == null) {
            throw new IllegalArgumentException("Input cannot be null to get first item from list");
        }

        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("Input must be a list to get first item");
        }

        if (list.isEmpty()) {
            throw new IllegalArgumentException("Input cannot be empty to get first item from list");
        }

        return list.getFirst();
    }
}
