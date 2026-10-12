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
import com.accenture.util.DecimalPluginUtils;
import com.accenture.util.RangeNotation;
import com.accenture.util.SimplePluginUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Range lookup of a decision table - 'f:rangeLookup(table, value)' returns the name of the first
 * band whose range contains the value.
 * <p>
 * The first argument is a map in the shape of a decision table:
 * <pre>
 * keys=[ band1, band2, ... ]
 * band1=min..max
 * band2=min..
 * band3=..max
 * </pre>
 * The 'keys' field lists the bands in priority order (the first band that contains the value wins)
 * and each band field is ONE range written as text: {@code min..max} runs from {@code min}
 * inclusive up to but EXCLUDING {@code max}, and an omitted end is open - so adjacent bands
 * ({@code 2500..10000}, {@code 10000..25000}) tile a scale with no gap and no overlap, and a band
 * that ends at a value is written with the next value as its upper bound. A bound is a canonical
 * number ({@code 25000}, {@code 2500.50}, {@code -1}), text in single or double quotes
 * ({@code '0020'}, {@code "2026-11-15"}) or unquoted text ({@code 16:00}, {@code h}), and the
 * table decides how values compare: numeric bounds compare the value exactly as a decimal (it must
 * be a number or a numeric string), quoted bounds compare it as written and case-sensitively -
 * the quotes keep a zero-padded code or a case-sensitive band as text - and unquoted text bounds
 * compare it as text without case, which is how ISO-8601 dates and times and alphabetic bands
 * work. Text compares as written, so a value must be shaped like the bounds (zero-padded, the same
 * parts, one UTC offset and one precision throughout), and text orders by Unicode code point. The
 * value must be a number or text: a boolean, a list, a map or bytes are refused by name on every
 * table, because a band is a scale and none of them sits on one. A table that mixes kinds is
 * refused. As with 'lookup', the 'keys' field may be a list or a JSON list written as text, and the
 * table itself may be JSON text; a band, though, is one range and never a list.
 * <p>
 * Every band is checked when the table is read, whatever the value (a null value included): a
 * malformed range, a band given as a list, a band mixing kinds, and an empty band - its lower bound
 * not below its upper bound ({@code 3..3}), which no value could ever match - are refused by name,
 * so a table that could never answer one of its bands never reaches a run silently. The same
 * notation is read by 'minRange' and 'maxRange' through {@link RangeNotation}.
 * <p>
 * The result is the matching band name; when no band contains the value, or the value is null,
 * the optional third argument is returned as the default (typically a text(...) constant), or null
 * when there is none. A non-numeric value against numeric bands fails by name, as 'graph.math' does.
 */
@SimplePlugin
public class SimpleRangeLookup implements PluginFunction {
    private static final String NAME = "rangeLookup";

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Resolve the band of a range table that contains a value
     *
     * @param input - first argument is the range table (a map, or a JSON object as text),
     *              second argument is the value to place,
     *              optional third argument is the default value when no band contains the value
     * @return the matching band name, otherwise the default value or null when there is none
     */
    @Override
    public Object calculate(Object... input) {
        if (input.length < 2 || input.length > 3) {
            throw new IllegalArgumentException("Expected two or three input values - actual=" + input.length);
        }
        Object defaultValue = input.length == 3 ? input[2] : null;
        var table = SimplePluginUtils.normalizeDecisionTable(input[0]);
        var keys = table.get("keys");
        if (keys == null) {
            throw new IllegalArgumentException("Missing keys in input");
        }
        // every band is parsed and checked before any value is placed, so a bad band is refused whatever the value
        List<Band> bands = parseBands(SimplePluginUtils.normalizeDecisionTableEntry(keys), table);
        RangeNotation.Kind kind = tableKind(bands);
        if (input[1] == null) {
            return defaultValue;
        }
        checkValue(input[1]);
        String band = switch (kind) {
            case NUMBER -> resolveNumeric(bands, DecimalPluginUtils.operand(input[1], NAME));
            case QUOTED -> resolveText(bands, String.valueOf(input[1]), false);
            case TEXT -> resolveText(bands, String.valueOf(input[1]), true);
        };
        return band == null ? defaultValue : band;
    }

    private List<Band> parseBands(List<String> keyList, Map<?, ?> table) {
        List<Band> bands = new ArrayList<>();
        for (String key : keyList) {
            var entry = table.get(key);
            if (entry == null) {
                throw new IllegalArgumentException("Missing key in input: " + key);
            }
            bands.add(new Band(key, RangeNotation.parse(String.valueOf(entry), "Range of '" + key + "'")));
        }
        return bands;
    }

    /** The kind every band shares; a table with no bands reads as text and matches nothing. */
    private RangeNotation.Kind tableKind(List<Band> bands) {
        RangeNotation.Kind kind = null;
        for (Band band : bands) {
            var k = band.range.kind;
            if (kind == null) {
                kind = k;
            } else if (k != kind) {
                if (k == RangeNotation.Kind.QUOTED || kind == RangeNotation.Kind.QUOTED) {
                    throw new IllegalArgumentException(
                            "Range table mixes quoted and unquoted bounds - quote every bound or none");
                }
                throw new IllegalArgumentException(
                        "Range table mixes numeric and text bounds - a table is one or the other");
            }
        }
        return kind == null ? RangeNotation.Kind.TEXT : kind;
    }

    private String resolveNumeric(List<Band> bands, BigDecimal value) {
        for (Band band : bands) {
            var r = band.range;
            if (contains(r.lowerNumber == null ? null : r.lowerNumber.compareTo(value),
                         r.upperNumber == null ? null : value.compareTo(r.upperNumber))) {
                return band.key;
            }
        }
        return null;
    }

    private String resolveText(List<Band> bands, String value, boolean ignoreCase) {
        String v = ignoreCase ? value.toLowerCase(Locale.ROOT) : value;
        for (Band band : bands) {
            String lower = fold(band.range.lower, ignoreCase);
            String upper = fold(band.range.upper, ignoreCase);
            if (contains(lower == null ? null : RangeNotation.compare(lower, v),
                         upper == null ? null : RangeNotation.compare(v, upper))) {
                return band.key;
            }
        }
        return null;
    }

    /** A band is a scale of numbers or text, so the value must be one of them; anything else is refused by name. */
    private static void checkValue(Object value) {
        if (value instanceof Number || value instanceof String) {
            return;
        }
        String kind = "an object";
        if (value instanceof Boolean) {
            kind = "a boolean";
        } else if (value instanceof List) {
            kind = "a list";
        } else if (value instanceof Map) {
            kind = "a map";
        } else if (value instanceof byte[]) {
            kind = "bytes";
        }
        throw new IllegalArgumentException(NAME + " value must be a number or text - got " + kind);
    }

    /** A bound folded for the comparison - null for an open end. */
    private static String fold(String bound, boolean ignoreCase) {
        return bound == null || !ignoreCase ? bound : bound.toLowerCase(Locale.ROOT);
    }

    /**
     * lowerToValue = compare(lower, value) or null for an open end; valueToUpper = compare(value, upper) or null.
     * The lower bound belongs to the band, the upper bound does not.
     */
    private static boolean contains(Integer lowerToValue, Integer valueToUpper) {
        boolean atOrAboveLower = lowerToValue == null || lowerToValue <= 0;
        boolean belowUpper = valueToUpper == null || valueToUpper < 0;
        return atOrAboveLower && belowUpper;
    }

    /** A band: its name and its range, checked when the table was read. */
    private record Band(String key, RangeNotation range) { }
}
