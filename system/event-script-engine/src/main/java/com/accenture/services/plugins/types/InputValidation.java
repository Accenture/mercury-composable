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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SimplePlugin
public class InputValidation implements PluginFunction {
    private static final String EVALUATE = "evaluate";
    private static final Map<String, Class<?>> TYPES = Map.of(
            "String", String.class,
            "Integer", Integer.class, "Long", Long.class, "Float", Float.class,
            "Double", Double.class, "Boolean", Boolean.class,
            "Map", Map.class, "List", List.class);
    private static final String REQUIRED = "required";

    @Override
    public String getName() {
        return "validate";
    }

    /**
     * Validate a key-value with a rule.
     * Parameters are separated by a semicolon. See examples below:
     * <p>
     * f:validate(input.body.id, text(id; String))
     * f:validate(input.body.id, text(id; String; required))
     * f:validate(input.body.id, text(id; String; 2024-01-01; 2024-02-28))
     * f:validate(input.body.id, text(id; Integer))
     * f:validate(input.body.id, text(id; Integer; required))
     * f:validate(input.body.id, text(id; Integer; 1; 99))
     * f:validate(input.body.id, text(id; Long; 100; 9999))
     * Optional keyword "evaluate" can be appended to the end.
     * e.g. adding "evaluate keyword will return true or false instead of value or exception.
     * f:validate(input.body.id, text(id; String; evaluate))
     *
     * @param input expected 2 arguments where the second one is the validation rule
     * @return argument one if validation passes IllegalArgumentException will be thrown
     */
    @Override
    public Object calculate(Object... input) {
        if (input.length == 2 && input[1] instanceof String text) {
            var value = input[0];
            var rules = TypeConversionUtils.getRules(text);
            if (rules.size() < 2) {
                throw new IllegalArgumentException("Invalid validation rule. " +
                        "Syntax: text(id, type), text(id, type, required) or text(id, type, range-start, range-end)");
            }
            var fieldName = rules.getFirst();
            var type = rules.get(1);
            var clazz = TYPES.get(type);
            if (clazz == null) {
                throw new IllegalArgumentException("Validation type '" + type + "' is not supported. " +
                        "Use String, Integer, Long, Float, Double, Boolean, Map or List.");
            }
            if (rules.size() > 2 && REQUIRED.equalsIgnoreCase(rules.get(2)) && value == null) {
                throw new IllegalArgumentException(fieldName+" is required.");
            }
            var evaluate = EVALUATE.equalsIgnoreCase(rules.getLast());
            var filtered = filterRules(rules);
            return evaluateOrException(fieldName, value, clazz, filtered, evaluate);
        } else {
            throw new IllegalArgumentException("Validation syntax error. " +
                    "Expect 2 arguments where the second one is the validation rule.");
        }
    }

    private List<String> filterRules(List<String> rules) {
        var result = new ArrayList<String>();
        for (String rule : rules) {
            if (!REQUIRED.equalsIgnoreCase(rule) && !EVALUATE.equalsIgnoreCase(rule)) {
                result.add(rule);
            }
        }
        return result;
    }

    private Object evaluateOrException(String fieldName, Object value, Class<?> clazz, List<String> rules,
                                       boolean evaluate) {
        try {
            var outcome = validate(fieldName, value, clazz, rules);
            if (evaluate) {
                return true;
            } else {
                return outcome;
            }
        } catch (IllegalArgumentException e) {
            if (evaluate) {
                return false;
            } else {
                throw e;
            }
        }
    }

    private Object validate(String fieldName, Object value, Class<?> clazz, List<String> rules) {
        if (value != null && validClass(value, clazz)) {
            return rules.size() == 2? value : rangeCheck(fieldName, value, rules);
        } else {
            if (value == null && rules.size() > 2) {
                throw new IllegalArgumentException(fieldName+" is required.");
            }
            var actual = value == null? "null" : value.getClass().getSimpleName();
            throw new IllegalArgumentException("Expect "+fieldName+" as "+clazz.getSimpleName()+", Actual: "+actual);
        }
    }

    private Object rangeCheck(String fieldName, Object value, List<String> rules) {
        var rangeStart = rules.size() > 2? rules.get(2) : null;
        var rangeEnd = rules.size() > 3? rules.get(3) : null;
        if (value instanceof String str) {
            return compareStringRange(fieldName, str, rangeStart, rangeEnd);
        }
        if (value instanceof Integer number) {
            return compareIntegerRange(fieldName, number, rangeStart, rangeEnd);
        }
        if (value instanceof Long number) {
            return compareLongRange(fieldName, number, rangeStart, rangeEnd);
        }
        if (value instanceof Float number) {
            return compareFloatRange(fieldName, number, rangeStart, rangeEnd);
        }
        if (value instanceof Double number) {
            return compareDoubleRange(fieldName, number, rangeStart, rangeEnd);
        }
        return value;
    }

    private String compareStringRange(String fieldName, String value, String rangeStart, String rangeEnd) {
        if (!REQUIRED.equalsIgnoreCase(rangeStart)) {
            if (rangeStart != null && value.compareTo(rangeStart) < 0) {
                throw new IllegalArgumentException(fieldName + " (" + value + ") < " + rangeStart);
            }
            if (rangeEnd != null && value.compareTo(rangeEnd) > 0) {
                throw new IllegalArgumentException(fieldName + " (" + value + ") > " + rangeEnd);
            }
        }
        return value;
    }

    private Integer compareIntegerRange(String fieldName, Integer value, String rangeStart, String rangeEnd) {
        var start = TypeConversionUtils.str2int(rangeStart);
        var end = TypeConversionUtils.str2int(rangeEnd);
        if (rangeStart != null && value < start) {
            throw new IllegalArgumentException(fieldName+" ("+value+") < "+rangeStart);
        }
        if (rangeEnd != null && value > end) {
            throw new IllegalArgumentException(fieldName+" ("+value+") > "+rangeEnd);
        }
        return value;
    }

    private Long compareLongRange(String fieldName, Long value, String rangeStart, String rangeEnd) {
        var start = TypeConversionUtils.str2long(rangeStart);
        var end = TypeConversionUtils.str2long(rangeEnd);
        if (rangeStart != null && value < start) {
            throw new IllegalArgumentException(fieldName+" ("+value+") < "+rangeStart);
        }
        if (rangeEnd != null && value > end) {
            throw new IllegalArgumentException(fieldName+" ("+value+") > "+rangeEnd);
        }
        return value;
    }

    private Double compareDoubleRange(String fieldName, Double value, String rangeStart, String rangeEnd) {
        var start = TypeConversionUtils.str2double(rangeStart);
        var end = TypeConversionUtils.str2double(rangeEnd);
        if (rangeStart != null && value < start) {
            throw new IllegalArgumentException(fieldName+" ("+value+") < "+rangeStart);
        }
        if (rangeEnd != null && value > end) {
            throw new IllegalArgumentException(fieldName+" ("+value+") > "+rangeEnd);
        }
        return value;
    }

    private Float compareFloatRange(String fieldName, Float value, String rangeStart, String rangeEnd) {
        var start = TypeConversionUtils.str2float(rangeStart);
        var end = TypeConversionUtils.str2float(rangeEnd);
        if (rangeStart != null && value < start) {
            throw new IllegalArgumentException(fieldName+" ("+value+") < "+rangeStart);
        }
        if (rangeEnd != null && value > end) {
            throw new IllegalArgumentException(fieldName+" ("+value+") > "+rangeEnd);
        }
        return value;
    }

    private boolean validClass(Object value, Class<?> clazz) {
        if (clazz == Map.class && value instanceof Map) {
            return true;
        } else if (clazz == List.class && value instanceof List) {
            return true;
        } else if (clazz == Integer.class || clazz == Long.class) {
            return value instanceof Integer || value instanceof Long;
        } else if (clazz == Float.class || clazz == Double.class) {
            return value instanceof Float || value instanceof Double;
        } else {
            return value.getClass() == clazz;
        }
    }
}
