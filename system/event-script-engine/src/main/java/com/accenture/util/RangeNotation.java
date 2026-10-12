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
 * <p>
 * A bound is one of three kinds, and a range never mixes two of them. A canonical number ({@code 25000},
 * {@code 2500.50}, {@code -1}) is a NUMBER bound and compares exactly as a decimal. Text in single or double
 * quotes ({@code '0020'}, {@code "2026-11-15"}) is a QUOTED bound and compares as written, case-sensitively;
 * the quotes are how a zero-padded code or a case-sensitive band is kept as text, and a quoted bound may contain
 * the separator and the other quote character ({@code "O'Brien"}), never its own - the first closing quote ends the
 * bound, in either position. Anything else ({@code 16:00}, {@code h}) is a TEXT bound and compares as text without
 * case, and may not contain the separator. Text orders by Unicode code point, the order of the Rust engine's string
 * comparison ({@code String.compareTo} orders UTF-16 units, which disagree above U+FFFF). A range whose lower bound
 * is not below its upper bound ({@code 3..3}, {@code 5..1}) holds nothing - no value could ever match it - and is
 * refused; so is a band given as a list or a map, because a band is one range.
 * <p>
 * The three plugins read the notation through this class, so a range is refused by one of them exactly when the
 * others refuse it. It lives beside the other plugin helpers because a plugin class may reference only the classes
 * the loader's bytecode gate allows, and the plugin package itself is not one of them.
 */
public final class RangeNotation {
    /** How the bounds of a range, and of every band of a table, compare. */
    public enum Kind {
        /** canonical numbers, compared exactly as decimals */
        NUMBER,
        /** unquoted text, compared without case */
        TEXT,
        /** quoted text, compared as written */
        QUOTED
    }

    public static final String SEPARATOR = "..";
    private static final Pattern CANONICAL_NUMBER = Pattern.compile("-?(0|[1-9]\\d*)(\\.\\d+)?");
    private static final String NOTATION = " must be written as min..max, min.. or ..max (the upper bound is excluded) - got ";

    /** the range as written, trimmed */
    public final String text;
    /** the lower bound as written (a quoted bound without its quotes), or null when the lower end is open */
    public final String lower;
    /** the upper bound as written (a quoted bound without its quotes), or null when the upper end is open */
    public final String upper;
    /** the kind of the bounds - a range with two bounds has them of one kind */
    public final Kind kind;
    /** the lower bound as a decimal when the range is numeric and has one, else null */
    public final BigDecimal lowerNumber;
    /** the upper bound as a decimal when the range is numeric and has one, else null */
    public final BigDecimal upperNumber;

    private RangeNotation(String text, Bound lower, Bound upper, Kind kind) {
        this.text = text;
        this.lower = lower.value;
        this.upper = upper.value;
        this.kind = kind;
        this.lowerNumber = kind == Kind.NUMBER && !lower.open() ? lower.number() : null;
        this.upperNumber = kind == Kind.NUMBER && !upper.open() ? upper.number() : null;
    }

    /**
     * Parse and check one range
     *
     * @param text the range as written
     * @param subject the start of a refusal - "Range of 'gold'" for a band of a table, "Range" for a single range
     * @return the range
     * @throws IllegalArgumentException when the range is a list or a map, the notation is malformed, a quoted
     *                                  bound is empty, the bounds mix kinds, or the range is empty
     */
    public static RangeNotation parse(String text, String subject) {
        String range = text.trim();
        if (range.startsWith("[") || range.startsWith("{")) {
            throw new IllegalArgumentException(subject + " must be one range, not a list or a map - got '" + range + "'");
        }
        Bound lower = lowerBound(range, subject);
        Bound upper = upperBound(lower.rest, range, subject);
        if (lower.open() && upper.open()) {
            throw malformed(subject, range);
        }
        if (!lower.open() && !upper.open()) {
            checkBounds(range, subject, lower, upper);
        }
        return new RangeNotation(range, lower, upper, lower.open() ? upper.kind : lower.kind);
    }

    /** The lower bound - quoted up to its closing quote, else the text before the first separator - and the rest. */
    private static Bound lowerBound(String range, String subject) {
        if (range.isEmpty()) {
            throw malformed(subject, range);
        }
        char first = range.charAt(0);
        if (isQuote(first)) {
            int close = range.indexOf(first, 1);
            if (close < 0) {
                throw malformed(subject, range);
            }
            String rest = range.substring(close + 1).trim();
            if (!rest.startsWith(SEPARATOR)) {
                throw malformed(subject, range);
            }
            return Bound.quoted(range.substring(1, close), rest.substring(SEPARATOR.length()).trim(), subject, range);
        }
        int at = range.indexOf(SEPARATOR);
        if (at < 0) {
            throw malformed(subject, range);
        }
        String value = range.substring(0, at).trim();
        if (value.endsWith(">") || value.endsWith("=")) {
            throw malformed(subject, range);
        }
        return Bound.unquoted(value, range.substring(at + SEPARATOR.length()).trim());
    }

    /**
     * The upper bound from what follows the separator: quoted up to its closing quote, which must end the range, or
     * unquoted text with no separator.
     */
    private static Bound upperBound(String rest, String range, String subject) {
        if (rest.isEmpty()) {
            return Bound.unquoted("", "");
        }
        char first = rest.charAt(0);
        if (isQuote(first)) {
            // the first closing quote ends the bound, as it does for the lower bound, and nothing may follow it;
            // an unmatched quote (-1) fails the same test
            int close = rest.indexOf(first, 1);
            if (close != rest.length() - 1) {
                throw malformed(subject, range);
            }
            return Bound.quoted(rest.substring(1, close), "", subject, range);
        }
        if (rest.contains(SEPARATOR) || first == '.' || first == '<' || first == '=') {
            throw malformed(subject, range);
        }
        return Bound.unquoted(rest, "");
    }

    /** A range with two bounds has them of one kind, and its lower bound below its upper bound. */
    private static void checkBounds(String range, String subject, Bound lower, Bound upper) {
        if (lower.kind != upper.kind) {
            if (lower.kind == Kind.QUOTED || upper.kind == Kind.QUOTED) {
                throw new IllegalArgumentException(subject +
                        " mixes a quoted and an unquoted bound - quote both bounds or neither: '" + range + "'");
            }
            throw new IllegalArgumentException(subject + " mixes a numeric and a text bound - got '" + range +
                    "' (a bound with a leading zero is text; quote both bounds to compare them as text)");
        }
        int order = switch (lower.kind) {
            case NUMBER -> lower.number().compareTo(upper.number());
            case QUOTED -> compare(lower.value, upper.value);
            case TEXT -> compare(lower.value.toLowerCase(Locale.ROOT), upper.value.toLowerCase(Locale.ROOT));
        };
        if (order >= 0) {
            throw new IllegalArgumentException(subject +
                    " is empty - its lower bound is not below its upper bound: '" + range + "'");
        }
    }

    private static IllegalArgumentException malformed(String subject, String range) {
        return new IllegalArgumentException(subject + NOTATION + "'" + range + "'");
    }

    private static boolean isQuote(char c) {
        return c == '\'' || c == '"';
    }

    /**
     * @param bound one bound as written
     * @return true when it is a canonical number, which is how a numeric range table is recognized
     */
    public static boolean isNumeric(String bound) {
        return CANONICAL_NUMBER.matcher(bound).matches();
    }

    /**
     * Text order by Unicode code point - the order in which both engines compare bounds and values.
     * {@code String.compareTo} orders UTF-16 code units, which disagree with code points when a character above
     * U+FFFF (a surrogate pair) meets one between U+E000 and U+FFFF.
     *
     * @param a one text
     * @param b another
     * @return negative, zero or positive as a is below, equal to or above b
     */
    public static int compare(String a, String b) {
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

    /**
     * The typed lower or upper bound of one range - the body of minRange and maxRange
     *
     * @param input the plugin's arguments: the range as text, and an optional default for an open end
     * @param lowerEnd true for the lower bound, false for the upper bound
     * @return a number written without a decimal point, within the 64-bit range, as a long, any other number as
     *         its exact decimal text, a text bound as written (a quoted one without its quotes), the default (null
     *         when there is none) when that end of the range is open, or null when the range itself is null - the
     *         band a lookup did not choose, read back through a dynamic key, has no bounds, and the default
     *         belongs to an open end
     * @throws IllegalArgumentException when the arguments are not one or two, or the range is refused
     */
    public static Object bound(Object[] input, boolean lowerEnd) {
        if (input.length < 1 || input.length > 2) {
            throw new IllegalArgumentException("Expected one or two input values - actual=" + input.length);
        }
        if (input[0] == null) {
            // no range, no bounds: the null-source rule leaves the mapping target unset
            return null;
        }
        var range = parse(String.valueOf(input[0]), "Range");
        String bound = lowerEnd ? range.lower : range.upper;
        if (bound == null) {
            return input.length == 2 ? input[1] : null;
        }
        return range.kind == Kind.NUMBER ? number(bound) : bound;
    }

    /**
     * A number written without a decimal point, within the 64-bit range, is a long; any other number ({@code 1.0}
     * included) stays its exact decimal text - the high-precision rule of the DECIMAL statement, where a double
     * would round it.
     */
    private static Object number(String bound) {
        if (bound.indexOf('.') < 0) {
            try {
                return Long.parseLong(bound);
            } catch (NumberFormatException e) {
                // beyond the 64-bit range: the exact text, below
            }
        }
        return bound;
    }

    /** One bound while parsing: its value (null when open), its kind (null when open) and the unread rest. */
    private record Bound(String value, Kind kind, String rest) {
        static Bound unquoted(String value, String rest) {
            if (value.isEmpty()) {
                return new Bound(null, null, rest);
            }
            Kind kind = isNumeric(value) ? Kind.NUMBER : Kind.TEXT;
            return new Bound(value, kind, rest);
        }

        static Bound quoted(String value, String rest, String subject, String range) {
            if (value.isEmpty()) {
                throw new IllegalArgumentException(subject +
                        " has an empty quoted bound - an open end is written by omission: '" + range + "'");
            }
            return new Bound(value, Kind.QUOTED, rest);
        }

        boolean open() {
            return value == null;
        }

        BigDecimal number() {
            return new BigDecimal(value);
        }
    }
}
