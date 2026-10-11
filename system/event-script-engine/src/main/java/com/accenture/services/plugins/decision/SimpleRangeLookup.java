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
 * that ends at a value is written with the next value as its upper bound. The table decides how
 * values compare: when every bound is a number ({@code 25000}, {@code 2500.50}, {@code -1}), the
 * bands are numeric and the value must be a number or a numeric string, compared exactly as a
 * decimal; when the bounds are text, values compare as text, case-insensitively, which is how
 * ISO-8601 dates and times ({@code 2026-11-15..2027-01-01}, {@code 08:00..16:00}) and alphabetic
 * bands ({@code a..h}) work. A table that mixes numeric and text bounds is refused. As with 'lookup',
 * the 'keys' field may be a list or a JSON list written as text, and the table itself may be JSON
 * text.
 * <p>
 * Every band is checked when the table is read, whatever the value (a null value included): a
 * malformed range, a band mixing a numeric and a text bound, and an empty band - its lower bound
 * not below its upper bound ({@code 3..3}), which no value could ever match - are refused by name,
 * so a table that could never answer one of its bands never reaches a run silently. The same
 * notation is read by 'minRange' and 'maxRange' through {@link RangeNotation}.
 * <p>
 * The result is the matching band name; when no band contains the value, or the value is null,
 * the optional third argument is returned as the default (typically a text(...) constant), or null
 * when there is none. A non-numeric value against numeric bands fails by name, as graph.math does.
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
        boolean numeric = isNumericTable(bands);
        if (input[1] == null) {
            return defaultValue;
        }
        String band = numeric ? resolveNumeric(bands, DecimalPluginUtils.operand(input[1], NAME))
                              : resolveText(bands, String.valueOf(input[1]));
        return band == null ? defaultValue : band;
    }

    private List<Band> parseBands(List<String> keyList, Map<?, ?> table) {
        List<Band> bands = new ArrayList<>();
        for (String key : keyList) {
            var entry = table.get(key);
            if (entry == null) {
                throw new IllegalArgumentException("Missing key in input: " + key);
            }
            var range = RangeNotation.parse(String.valueOf(entry), "Range of '" + key + "'");
            bands.add(new Band(key, range.lower, range.upper, range.numeric));
        }
        return bands;
    }

    private boolean isNumericTable(List<Band> bands) {
        int numeric = 0;
        int text = 0;
        for (Band band : bands) {
            if (band.numeric) {
                numeric++;
            } else {
                text++;
            }
        }
        if (numeric > 0 && text > 0) {
            throw new IllegalArgumentException("Range table mixes numeric and text bounds - a table is one or the other");
        }
        return numeric > 0;
    }

    private String resolveNumeric(List<Band> bands, BigDecimal value) {
        for (Band band : bands) {
            BigDecimal lower = band.lower == null ? null : new BigDecimal(band.lower);
            BigDecimal upper = band.upper == null ? null : new BigDecimal(band.upper);
            if (contains(lower == null ? null : lower.compareTo(value), upper == null ? null : value.compareTo(upper))) {
                return band.key;
            }
        }
        return null;
    }

    private String resolveText(List<Band> bands, String value) {
        String v = value.toLowerCase(Locale.ROOT);
        for (Band band : bands) {
            String lower = band.lower == null ? null : band.lower.toLowerCase(Locale.ROOT);
            String upper = band.upper == null ? null : band.upper.toLowerCase(Locale.ROOT);
            if (contains(lower == null ? null : lower.compareTo(v), upper == null ? null : v.compareTo(upper))) {
                return band.key;
            }
        }
        return null;
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

    /** A band: its name, its bounds as written (null for an open end) and whether they are numeric. */
    private record Band(String key, String lower, String upper, boolean numeric) { }
}
