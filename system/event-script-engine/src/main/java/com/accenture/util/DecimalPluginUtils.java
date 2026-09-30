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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Exact decimal arithmetic for the {@code decimal*} simple plugins (RFC-0001, ADR-0025): the same rules as the
 * DECIMAL statement of graph.math, so a flow and a graph compute the same answer, and the shared conformance
 * vectors hold in both.
 * <p>
 * A result is a canonical string - plain notation, never scientific, the computed scale kept, and a zero of any
 * scale written "0" - which is what survives a serialization boundary unchanged. An operand is a whole number, a
 * Double or Float (taken through the shortest decimal text it prints as), a BigDecimal or BigInteger, or a string
 * that is a canonical number; anything else is an error naming it.
 */
public class DecimalPluginUtils {
    private static final Pattern CANONICAL = Pattern.compile("-?(0|[1-9]\\d*)(\\.\\d+)?");
    private static final int MAX_LENGTH = 1000;
    private static final int MAX_SCALE = 1000;
    private static final int MAX_RESULT_PRECISION = 10_000;
    private static final MathContext DIVISION = MathContext.DECIMAL128;
    private static final String MODES = "HALF_UP, HALF_EVEN, HALF_DOWN, UP, DOWN, CEILING or FLOOR";

    private DecimalPluginUtils() {
        // utility class
    }

    /**
     * @param value a decimal
     * @return its canonical string
     */
    public static String canonical(BigDecimal value) {
        return value.signum() == 0? "0" : normalize(value).toPlainString();
    }

    /**
     * Reads an operand as an exact decimal.
     *
     * @param value a plugin argument
     * @param context the plugin name, for the error message
     * @return the decimal it denotes
     * @throws IllegalArgumentException when it is not a number
     */
    public static BigDecimal operand(Object value, String context) {
        return switch (value) {
            case null -> throw new IllegalArgumentException("Cannot convert null to a decimal in " + context);
            case Short s -> BigDecimal.valueOf(s.longValue());
            case Integer i -> BigDecimal.valueOf(i.longValue());
            case Long l -> BigDecimal.valueOf(l);
            case BigInteger b -> new BigDecimal(b);
            case BigDecimal d -> normalize(d);
            case Float f -> shortest(f.toString(), value);
            case Double d -> shortest(d.toString(), value);
            case String s -> fromText(s, context);
            case Boolean b -> throw new IllegalArgumentException("Boolean operand in " + context + ": " + b);
            default -> throw new IllegalArgumentException("Cannot convert the object to a decimal in " + context +
                    ": " + value);
        };
    }

    private static BigDecimal shortest(String text, Object original) {
        try {
            // Double.toString and Float.toString print the shortest text that identifies the number
            return normalize(new BigDecimal(text).stripTrailingZeros());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a finite number: " + original);
        }
    }

    private static BigDecimal fromText(String text, String context) {
        if (text.length() > MAX_LENGTH || !CANONICAL.matcher(text).matches()) {
            throw new IllegalArgumentException("Expected a decimal number in " + context + ", got '" + text + "'");
        }
        return new BigDecimal(text);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return checked(a.add(b), "add");
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return checked(a.subtract(b), "subtract");
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return checked(a.multiply(b), "multiply");
    }

    /**
     * Never truncates: the exact quotient when it terminates, otherwise 34 significant digits, HALF_EVEN.
     *
     * @param dividend the dividend
     * @param divisor the divisor
     * @return the quotient
     * @throws IllegalArgumentException on division by zero
     */
    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        if (divisor.signum() == 0) {
            throw new IllegalArgumentException("Division by zero in 'decimalDiv'");
        }
        try {
            return checked(dividend.divide(divisor, MathContext.UNLIMITED), "decimalDiv");
        } catch (ArithmeticException e) {
            return checked(dividend.divide(divisor, DIVISION), "decimalDiv");
        }
    }

    public static BigDecimal remainder(BigDecimal dividend, BigDecimal divisor) {
        if (divisor.signum() == 0) {
            throw new IllegalArgumentException("Division by zero in 'decimalMod'");
        }
        return checked(dividend.remainder(divisor), "decimalMod");
    }

    /**
     * Rounding is always explicit: the scale and the mode are both required.
     *
     * @param x the value
     * @param scale a whole number of decimal places, 0 to 1000
     * @param mode one of HALF_UP, HALF_EVEN, HALF_DOWN, UP, DOWN, CEILING or FLOOR
     * @return the value at exactly that scale (a scale is never negative)
     */
    public static BigDecimal round(BigDecimal x, BigDecimal scale, Object mode) {
        var whole = scale.stripTrailingZeros();
        final int places;
        try {
            places = whole.scale() > 0? -1 : whole.intValueExact();
        } catch (ArithmeticException e) {
            throw scaleError(scale);
        }
        if (places < 0 || places > MAX_SCALE) {
            throw scaleError(scale);
        }
        return normalize(x.setScale(places, roundingMode(mode)));
    }

    private static IllegalArgumentException scaleError(BigDecimal scale) {
        return new IllegalArgumentException("The scale of decimalRound must be a whole number from 0 to " +
                MAX_SCALE + ", got " + canonical(scale));
    }

    private static RoundingMode roundingMode(Object mode) {
        if (mode instanceof String name) {
            try {
                var found = RoundingMode.valueOf(name.trim().toUpperCase(Locale.ROOT));
                if (found != RoundingMode.UNNECESSARY) {
                    return found;
                }
            } catch (IllegalArgumentException e) {
                // reported below
            }
        }
        throw new IllegalArgumentException("The mode of decimalRound must be " + MODES);
    }

    private static BigDecimal checked(BigDecimal value, String context) {
        if (value.precision() > MAX_RESULT_PRECISION) {
            throw new IllegalArgumentException("Arithmetic result too large in '" + context + "'");
        }
        return normalize(value);
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value.scale() < 0? value.setScale(0, RoundingMode.UNNECESSARY) : value;
    }
}
