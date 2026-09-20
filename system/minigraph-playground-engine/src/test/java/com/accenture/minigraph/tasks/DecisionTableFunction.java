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

package com.accenture.minigraph.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;

import java.util.List;
import java.util.Map;

/**
 * A generic decision-table lookup: the TABLE arrives in the request, nothing about it is compiled
 * into the function. 'table.keys' lists the rule names in priority order and each rule name is a
 * property holding that rule's member keys - the shape a skill-less DecisionTable node carries in a
 * graph (unit-test-task-9): keys=[ "a", "b" ], a=[ "CA", "TX" ], b=[ "NY" ], each value a JSON array
 * written as text that SimpleMapper reconstructs here. A product owner certifies the data ON the
 * graph and a new table ships as a new graph version, not as code.
 */
@PreLoad(route = "v1.decision.table", instances = 10)
public class DecisionTableFunction implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
                                            throws AppException {
        if (!(input.get("table") instanceof Map<?, ?> table)) {
            throw new AppException(400, "Missing decision table");
        }
        var key = String.valueOf(input.get("key"));
        for (var rule : asList(table.get("keys"))) {
            if (asList(table.get(String.valueOf(rule))).contains(key)) {
                return Map.of("rule", String.valueOf(rule), "key", key);
            }
        }
        throw new AppException(404, "No rule for " + key);
    }

    /**
     * A node property authored as 'keys=[ "a", "b" ]' arrives as JSON text - SimpleMapper reconstructs
     * the list; a 'keys[]=a' list property, or a dataset from f:json, arrives as a list already.
     */
    private static List<?> asList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        if (value instanceof String text && text.trim().startsWith("[")) {
            return SimpleMapper.getInstance().getMapper().readValue(text, List.class);
        }
        return List.of();
    }
}
