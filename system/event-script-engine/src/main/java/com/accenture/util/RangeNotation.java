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
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * One range written as text - the notation shared by rangeLookup, minRange and maxRange: {@code min..max} runs
 * from {@code min} inclusive up to but EXCLUDING {@code max}, and {@code min..} or {@code ..max} leaves an end open.
 * A bound is numeric when it is a canonical number ({@code 25000}, {@code 2500.50}, {@code -1}) and text otherwise
 * ({@code 16:00}, {@code 2026-11-15}, {@code h}); a range never mixes the two. A range whose lower bound is not
 * below its upper bound ({@code 3..3}, {@code 5..1}) holds nothing - no value could ever match it - and is refused.
 * <p>
 * The three plugins read the notation through this class, so a range is refused by one of them exactly when the
 * others refuse it: a malformed range, a range mixing a numeric and a text bound, and an empty range fail by name.
 * It lives beside the other plugin helpers because a plugin class may reference only the packages the loader's
 * bytecode gate allows, and the plugin package itself is not one of them.
 */
public final class RangeNotation {
    public static final String SEPARATOR = "..";
    private static final Pattern CANONICAL_NUMBER = Pattern.compile("-?(0|[1-9]\\d*)(\\.\\d+)?");
    private static final String NOTATION = " must be written as min..max, min.. or ..max (the upper bound is excluded) - got ";

    /** the range as written, trimmed */
    public final String text;
    /** the lower bound as written, or null when the lower end is open */
    public final String lower;
    /** the upper bound as written, or null when the upper end is open */
    public final String upper;
    /** true when the bounds are numbers, false when they are text (a range with two bounds has them of one kind) */
    public final boolean numeric;

    private RangeNotation(String text, String lower, String upper, boolean numeric) {
        this.text = text;
        this.lower = lower;
        this.upper = upper;
        this.numeric = numeric;
    }

    /**
     * Parse and check one range
     *
     * @param text the range as written
     * @param subject the start of a refusal - "Range of 'gold'" for a band of a table, "Range" for a single range
     * @return the range
     * @throws IllegalArgumentException when the notation is malformed, the bounds mix a number and text,
     *                                  or the range is empty
     */
    public static RangeNotation parse(String text, String subject) {
        String range = text.trim();
        int at = range.indexOf(SEPARATOR);
        if (at < 0) {
            throw malformed(subject, range);
        }
        String lower = range.substring(0, at).trim();
        String upper = range.substring(at + SEPARATOR.length()).trim();
        if ((lower.isEmpty() && upper.isEmpty()) || hasMarker(lower, upper)) {
            throw malformed(subject, range);
        }
        boolean lowerNumeric = !lower.isEmpty() && isNumeric(lower);
        boolean upperNumeric = !upper.isEmpty() && isNumeric(upper);
        if (!lower.isEmpty() && !upper.isEmpty()) {
            checkBounds(range, subject, lower, upper, lowerNumeric, upperNumeric);
        }
        return new RangeNotation(range, open(lower), open(upper), lowerNumeric || upperNumeric);
    }

    private static IllegalArgumentException malformed(String subject, String range) {
        return new IllegalArgumentException(subject + NOTATION + "'" + range + "'");
    }

    /** The inclusive or exclusive markers of a first draft ({@code 1..<2}, {@code 1..=2}) are refused, never read. */
    private static boolean hasMarker(String lower, String upper) {
        return upper.startsWith("<") || upper.startsWith("=") || lower.endsWith(">") || lower.endsWith("=");
    }

    /** A range with two bounds has them of one kind, and its lower bound below its upper bound. */
    private static void checkBounds(String range, String subject, String lower, String upper,
                                    boolean lowerNumeric, boolean upperNumeric) {
        if (lowerNumeric != upperNumeric) {
            throw new IllegalArgumentException(subject + " mixes a numeric and a text bound - got '" + range + "'");
        }
        int order = lowerNumeric ? new BigDecimal(lower).compareTo(new BigDecimal(upper))
                : lower.toLowerCase(Locale.ROOT).compareTo(upper.toLowerCase(Locale.ROOT));
        if (order >= 0) {
            throw new IllegalArgumentException(subject +
                    " is empty - its lower bound is not below its upper bound: '" + range + "'");
        }
    }

    /** An empty bound is an open end, kept as null. */
    private static String open(String bound) {
        return bound.isEmpty() ? null : bound;
    }

    /**
     * @param bound one bound as written
     * @return true when it is a canonical number, which is how a numeric range table is recognized
     */
    public static boolean isNumeric(String bound) {
        return CANONICAL_NUMBER.matcher(bound).matches();
    }

    /**
     * The typed lower or upper bound of one range - the body of minRange and maxRange
     *
     * @param input the plugin's arguments: the range as text, and an optional default for an open end
     * @param lowerEnd true for the lower bound, false for the upper bound
     * @return a number for a numeric bound (a whole number within the 64-bit range as a long, any other number
     *         as a double), the text of a text bound as written, or the default (null when there is none)
     *         when that end of the range is open
     * @throws IllegalArgumentException when the arguments are not one or two, or the range is refused
     */
    public static Object bound(Object[] input, boolean lowerEnd) {
        if (input.length < 1 || input.length > 2) {
            throw new IllegalArgumentException("Expected one or two input values - actual=" + input.length);
        }
        if (input[0] == null) {
            throw new IllegalArgumentException("Range" + NOTATION + "null");
        }
        var range = parse(String.valueOf(input[0]), "Range");
        String bound = lowerEnd ? range.lower : range.upper;
        if (bound == null) {
            return input.length == 2 ? input[1] : null;
        }
        return range.numeric ? number(bound) : bound;
    }

    /** A whole number within the 64-bit range is a long; any other number is a double. */
    private static Object number(String bound) {
        if (bound.indexOf('.') < 0) {
            try {
                return Long.parseLong(bound);
            } catch (NumberFormatException e) {
                // beyond the 64-bit range: a double, below
            }
        }
        return Double.parseDouble(bound);
    }
}
