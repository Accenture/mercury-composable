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

package com.accenture.util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.stream.Stream;

public class SimplePluginUtils {

    private SimplePluginUtils() {}

    public static void divideByZeroCheck(Object... input) {
        boolean anyZero = Arrays.stream(input)
                .map(SimplePluginUtils::promoteNumber)
                .anyMatch(n -> n.doubleValue() == 0.0d);
        if (anyZero) {
            throw new IllegalArgumentException("Dividing the input: " + Arrays.toString(input) + " would cause Division By Zero");
        }
    }

    public static Stream<Number> promoteInput(Object... input) {
        return Arrays.stream(input).map(SimplePluginUtils::promoteNumber);
    }

    /**
     * Java-style numeric promotion: whole numbers and whole-number strings
     * promote to Long, floating-point values and decimal strings to Double
     * (generalized from whole-number-only); anything else is an error.
     */
    public static Number promoteNumber(Object o) {
        return switch (o) {
            case Short s -> s.longValue();
            case Integer i -> i.longValue();
            case Long l -> l;
            case Float f -> f.doubleValue();
            case Double d -> d;
            case String s -> parseNumber(s);
            default -> throw new IllegalArgumentException("Cannot convert the object to a number: " + o);
        };
    }

    private static Number parseNumber(String s) {
        var text = s.trim();
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            try {
                return Double.valueOf(text);
            } catch (NumberFormatException e2) {
                throw new IllegalArgumentException("Cannot convert the object to a number: " + s);
            }
        }
    }

    /**
     * Two-value equality for the {@code eq} and {@code ne} operators.
     * Arguments 3 and 4 are optional modifiers, each {@code ignoreCase}
     * or {@code ignoreType}:
     * <ul>
     * <li>{@code ignoreCase} compares two strings case-insensitively; for
     *     non-string values it has no effect (types still matter).</li>
     * <li>{@code ignoreType} compares the {@code String.valueOf} text forms,
     *     allowing the relaxed comparison of numbers and booleans -
     *     "123" == 123, "123.456" == 123.456 and "true" == true.</li>
     * <li>Both together compare the text forms case-insensitively.</li>
     * </ul>
     *
     * @param input two values, optionally followed by one or two modifiers
     * @return true when the two values are equal under the given modifiers
     */
    public static boolean equalsWithModifiers(Object... input) {
        if (input.length > 4) {
            throw new IllegalArgumentException(
                    "Expected two values plus optional 'ignoreCase' and/or 'ignoreType' modifiers");
        }
        boolean ignoreCase = false;
        boolean ignoreType = false;
        for (int i = 2; i < input.length; i++) {
            String modifier = String.valueOf(input[i]);
            switch (modifier) {
                case "ignoreCase" -> ignoreCase = true;
                case "ignoreType" -> ignoreType = true;
                default -> throw new IllegalArgumentException(
                        "Unknown modifier '" + modifier + "' - only 'ignoreCase' and 'ignoreType' are supported");
            }
        }
        Object a = input[0];
        Object b = input[1];
        if (ignoreType) {
            String first = String.valueOf(a);
            String second = String.valueOf(b);
            return ignoreCase ? first.equalsIgnoreCase(second) : first.equals(second);
        }
        if (ignoreCase && a instanceof String first && b instanceof String second) {
            return first.equalsIgnoreCase(second);
        }
        return Objects.equals(a, b);
    }

    /**
     * Reduce with numeric promotion decided over ALL args first (so the
     * result is order-independent): any floating-point arg promotes the
     * whole computation to double; all-integral inputs keep exact long
     * arithmetic - including integer division, exactly as before this
     * generalization.
     */
    public static Object reduceNumbers(LongBinaryOperator longOp, DoubleBinaryOperator doubleOp, Object... input) {
        List<Number> numbers = promoteInput(input).toList();
        boolean floating = numbers.stream().anyMatch(Double.class::isInstance);
        if (floating) {
            double total = numbers.getFirst().doubleValue();
            for (int i = 1; i < numbers.size(); i++) {
                total = doubleOp.applyAsDouble(total, numbers.get(i).doubleValue());
            }
            return total;
        } else {
            long total = numbers.getFirst().longValue();
            for (int i = 1; i < numbers.size(); i++) {
                total = longOp.applyAsLong(total, numbers.get(i).longValue());
            }
            return total;
        }
    }
}
