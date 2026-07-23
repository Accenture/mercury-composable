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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

@SimplePlugin
public class IsEmptyOperator implements PluginFunction {

    @Override
    public String getName() {
        return "isEmpty";
    }

    @Override
    public Object calculate(Object... input) {
        if (input == null || input.length != 1) {
            throw new IllegalArgumentException("One input is required to check if value is empty");
        }

        Object value = input[0];

        if (value == null) {
            throw new IllegalArgumentException("Input cannot be null to check if value is empty");
        }

        return switch (value) {
            case Collection<?> collection -> collection.isEmpty();
            case Map<?, ?> map -> map.isEmpty();
            case CharSequence text -> text.isEmpty();
            default -> {
                if (value.getClass().isArray()) {
                    yield Array.getLength(value) == 0;
                }

                throw new IllegalArgumentException(
                        "Unsupported input type to check if value is empty: " + value.getClass().getName());
            }
        };
    }
}
