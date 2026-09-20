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
import com.accenture.util.SimplePluginUtils;

import java.util.List;
import java.util.Map;

/**
 * Simple lookup of a decision table - 'f:lookup(table, value)' returns the name of the rule
 * that lists the value.
 * <p>
 * The first argument is a map of key-values in the following data structure:
 * <pre>
 * keys=[ rule1, rule2, ... ]
 * rule1=[ value11, value12, ... ]
 * rule2=[ value21, value22, ... ]
 * </pre>
 * The 'keys' field lists the rule names in priority order and each rule field lists the values
 * that select the rule; a value can be a number or a string. The 'keys' field and each rule field
 * may be given as a list or as a JSON list written as text (the shape of a node property authored as
 * keys=[ "a", "b" ]), and the table itself may be given as JSON text - SimplePluginUtils converts them
 * (a plugin class must stay inside the loader's allowlist, so the serializer is reached only there).
 * <p>
 * Values are compared as text, case-insensitively. The result is the matching rule name or null
 * when no rule lists the value, so a mapping can supply a default with f:defaultValue.
 */
@SimplePlugin
public class SimpleLookup implements PluginFunction {

    @Override
    public String getName() {
        return "lookup";
    }

    /**
     * Resolve the rule of a decision table that lists a value
     *
     * @param input - first argument is the decision table (a map, or a JSON object as text),
     *              second argument is the value to match
     * @return the matching rule name, or null when no rule lists the value
     */
    @Override
    public Object calculate(Object... input) {
        if (input.length != 2) {
            throw new IllegalArgumentException("Expected two input values - actual=" + input.length);
        }
        var table = SimplePluginUtils.normalizeDecisionTable(input[0]);
        var keys = table.get("keys");
        if (keys == null) {
            throw new IllegalArgumentException("Missing keys in input");
        }
        return resolveKey(SimplePluginUtils.normalizeDecisionTableEntry(keys), table, String.valueOf(input[1]));
    }

    private String resolveKey(List<String> keyList, Map<?, ?> table, String value) {
        for (String key : keyList) {
            var entry = table.get(key);
            if (entry == null) {
                throw new IllegalArgumentException("Missing key in input: " + key);
            }
            var valueList = SimplePluginUtils.normalizeDecisionTableEntry(entry);
            for (String v : valueList) {
                if (v.equalsIgnoreCase(value)) {
                    return key;
                }
            }
        }
        return null;
    }
}
