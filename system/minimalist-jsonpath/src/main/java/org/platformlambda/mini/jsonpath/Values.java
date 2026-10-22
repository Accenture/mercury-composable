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

package org.platformlambda.mini.jsonpath;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Equality and order of JSON values, as RFC 9535 2.3.5.2.2 defines them.
 */
final class Values {
    /**
     * The absence of a value: what an empty nodelist yields as a comparable, and what a function returns when it
     * has no result. It differs from a JSON null, which is a value.
     */
    static final Object NOTHING = new Object() {
        @Override
        public String toString() {
            return "Nothing";
        }
    };

    private Values() { }

    /**
     * Two values are equal when they are:
     * <ul>
     *   <li>both Nothing, or both null</li>
     *   <li>numbers of the same mathematical value</li>
     *   <li>equal strings, or equal booleans</li>
     *   <li>arrays of pairwise equal elements</li>
     *   <li>objects with the same member names and equal member values</li>
     * </ul>
     * Nothing equals only Nothing.
     */
    static boolean isEqual(Object a, Object b) {
        if (a == NOTHING || b == NOTHING || a == null || b == null) {
            return a == b;
        }
        return switch (a) {
            case Number x when b instanceof Number y -> {
                OptionalInt order = compareNumbers(x, y);
                yield order.isPresent() && order.getAsInt() == 0;
            }
            case List<?> x when b instanceof List<?> y -> listsEqual(x, y);
            case Map<?, ?> x when b instanceof Map<?, ?> y -> mapsEqual(x, y);
            default -> a.equals(b);
        };
    }

    /**
     * @return true when both are numbers, or both strings, and the first is the smaller; strings are ordered by
     *         their Unicode scalar values
     */
    static boolean isLess(Object a, Object b) {
        if (a instanceof Number x && b instanceof Number y) {
            OptionalInt order = compareNumbers(x, y);
            return order.isPresent() && order.getAsInt() < 0;
        }
        if (a instanceof String x && b instanceof String y) {
            return compareCodePoints(x, y) < 0;
        }
        return false;
    }

    // String.compareTo orders UTF-16 units, which disagrees with code point order above U+FFFF
    static int compareCodePoints(String a, String b) {
        int i = 0;
        int j = 0;
        while (i < a.length() && j < b.length()) {
            int x = a.codePointAt(i);
            int y = b.codePointAt(j);
            if (x != y) {
                return Integer.compare(x, y);
            }
            i += Character.charCount(x);
            j += Character.charCount(y);
        }
        return Integer.compare(a.length() - i, b.length() - j);
    }

    // empty when either number is not finite, so it neither equals nor orders against anything
    private static OptionalInt compareNumbers(Number a, Number b) {
        if (isIntegral(a) && isIntegral(b)) {
            return OptionalInt.of(Long.compare(a.longValue(), b.longValue()));
        }
        BigDecimal x = toDecimal(a);
        BigDecimal y = toDecimal(b);
        return x == null || y == null ? OptionalInt.empty() : OptionalInt.of(x.compareTo(y));
    }

    private static boolean isIntegral(Number n) {
        return n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte;
    }

    // a Double converts through its shortest decimal text, as Mercury's DECIMAL statement does
    private static BigDecimal toDecimal(Number n) {
        return switch (n) {
            case BigDecimal d -> d;
            case BigInteger i -> new BigDecimal(i);
            case Double d -> Double.isFinite(d) ? BigDecimal.valueOf(d) : null;
            case Float f -> Float.isFinite(f) ? new BigDecimal(Float.toString(f)) : null;
            default -> isIntegral(n) ? BigDecimal.valueOf(n.longValue()) : parse(n.toString());
        };
    }

    private static BigDecimal parse(String text) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean listsEqual(List<?> a, List<?> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!isEqual(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean mapsEqual(Map<?, ?> a, Map<?, ?> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (Map.Entry<?, ?> entry : a.entrySet()) {
            if (!b.containsKey(entry.getKey()) || !isEqual(entry.getValue(), b.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }
}
