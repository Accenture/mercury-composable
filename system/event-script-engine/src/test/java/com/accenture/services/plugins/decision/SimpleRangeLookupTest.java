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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SimpleRangeLookupTest {

    private final SimpleRangeLookup plugin = new SimpleRangeLookup();

    // a credit card loyalty tier by annual spend: half-open money bands tile the number line
    private static final Map<String, Object> LOYALTY_TIERS = Map.of(
            "purpose", "a property the lookup ignores",
            "keys", "[ \"platinum\", \"gold\", \"silver\", \"standard\" ]",
            "platinum", "25000..",
            "gold", "10000..25000",
            "silver", "2500..10000",
            "standard", "..2500");

    // a credit score band: the upper bound of each band is the lower bound of the next; the top band is open
    private static final Map<String, Object> SCORE_BANDS = Map.of(
            "keys", List.of("excellent", "very_good", "good", "fair", "poor"),
            "excellent", "800..",
            "very_good", "740..800",
            "good", "670..740",
            "fair", "580..670",
            "poor", "300..580");

    // a same-day processing cut-off: ISO-8601 times compare as text
    private static final Map<String, Object> CUT_OFF = Map.of(
            "keys", List.of("same_day", "next_day"),
            "same_day", "00:00..16:00",
            "next_day", "16:00..");

    @Test
    void shouldUseRangeLookupAsPluginName() {
        assertEquals("rangeLookup", plugin.getName());
    }

    @Test
    void shouldPlaceANumberInTheFirstBandThatContainsIt() {
        assertEquals("platinum", plugin.calculate(LOYALTY_TIERS, 25000));
        assertEquals("gold", plugin.calculate(LOYALTY_TIERS, "24999.99"));
        assertEquals("gold", plugin.calculate(LOYALTY_TIERS, 10000));
        assertEquals("silver", plugin.calculate(LOYALTY_TIERS, 9999.99));
        assertEquals("standard", plugin.calculate(LOYALTY_TIERS, 0));
        assertEquals("standard", plugin.calculate(LOYALTY_TIERS, "-50"));
        // the lower bound belongs to the band, the upper bound to the next band
        assertEquals("excellent", plugin.calculate(SCORE_BANDS, 850));
        assertEquals("excellent", plugin.calculate(SCORE_BANDS, 800));
        assertEquals("very_good", plugin.calculate(SCORE_BANDS, 799L));
        assertEquals("good", plugin.calculate(SCORE_BANDS, 739.5));
        assertEquals("poor", plugin.calculate(SCORE_BANDS, "300"));
    }

    @Test
    void shouldAnswerTheDefaultOnAMissOrANullValue() {
        assertNull(plugin.calculate(SCORE_BANDS, 299));
        assertEquals("unknown", plugin.calculate(SCORE_BANDS, "299.99", "unknown"));
        assertEquals("unknown", plugin.calculate(SCORE_BANDS, null, "unknown"));
        assertNull(plugin.calculate(SCORE_BANDS, null));
        // the default is ignored on a hit
        assertEquals("good", plugin.calculate(SCORE_BANDS, 700, "unknown"));
    }

    @Test
    void shouldCompareTextBandsCaseInsensitively() {
        assertEquals("same_day", plugin.calculate(CUT_OFF, "15:59:59"));
        assertEquals("next_day", plugin.calculate(CUT_OFF, "16:00"));
        assertEquals("next_day", plugin.calculate(CUT_OFF, "23:59"));
        var initials = Map.of("keys", List.of("branch-a", "branch-b"), "branch-a", "a..h", "branch-b", "h..");
        assertEquals("branch-a", plugin.calculate(initials, "Garcia"));
        assertEquals("branch-b", plugin.calculate(initials, "HARRIS"));
        var promo = Map.of("keys", List.of("holiday_bonus"), "holiday_bonus", "2026-11-15..2027-01-01");
        assertEquals("holiday_bonus", plugin.calculate(promo, "2026-12-31T23:59:59Z"));
        assertEquals("regular", plugin.calculate(promo, "2027-01-01", "regular"));
    }

    @Test
    void shouldAcceptTheWholeTableAsJsonText() {
        var json = "{\"keys\": [\"low\", \"high\"], \"low\": \"..100\", \"high\": \"100..\"}";
        assertEquals("low", plugin.calculate(json, 99.5));
        assertEquals("high", plugin.calculate(json, "100"));
    }

    @Test
    void shouldFailByNameOnABadTableOrValue() {
        assertTrue(failure(SCORE_BANDS).getMessage().startsWith("Expected two or three input values"));
        assertEquals("Missing keys in input", failure(Map.of("a", "1..2"), 1).getMessage());
        assertEquals("Missing key in input: b", failure(Map.of("keys", List.of("a", "b"), "a", "1..2"), 1).getMessage());
        assertStartsWith("Range of 'a' must be written as", failure(oneBand("1-2"), 1));
        assertStartsWith("Range of 'a' must be written as", failure(oneBand(".."), 1));
        // the exclusive-upper notation of a first draft is refused rather than silently accepted
        assertStartsWith("Range of 'a' must be written as", failure(oneBand("1..<2"), 1));
        // a band whose bounds are equal holds nothing
        assertStartsWith("Range of 'a' is empty", failure(oneBand("2..2"), 2));
        assertStartsWith("Range of 'a' is empty", failure(oneBand("5..1"), 3));
        var mixed = Map.of("keys", List.of("a", "b"), "a", "1..2", "b", "x..y");
        assertStartsWith("Range table mixes numeric and text bounds", failure(mixed, 1));
        // numeric bands, non-numeric value: fails by name, as graph.math does
        assertTrue(failure(SCORE_BANDS, "abc").getMessage().contains("Expected a decimal number in rangeLookup"));
        assertStartsWith("Boolean operand in rangeLookup", failure(SCORE_BANDS, true));
    }

    @Test
    void shouldCheckEveryBandWhenTheTableIsRead() {
        // a band that could never match - its bounds equal - is refused whatever the value, even when an earlier
        // band would have answered and even when the value is null; so is a band that mixes a number and text
        var table = Map.of("keys", List.of("a", "b"), "a", "1..2", "b", "3 .. 3");
        assertStartsWith("Range of 'b' is empty - its lower bound is not below its upper bound: '3 .. 3'",
                failure(table, 1));
        assertStartsWith("Range of 'b' is empty", failure(table, null));
        assertStartsWith("Range of 'a' mixes a numeric and a text bound - got '1..x'", failure(oneBand("1..x"), 1));
        var mixedTable = Map.of("keys", List.of("a", "b"), "a", "1..2", "b", "x..y");
        assertStartsWith("Range table mixes numeric and text bounds", failure(mixedTable, null));
    }

    @Test
    void shouldCompareQuotedBoundsAsWritten() {
        // quotes make a table case-sensitive: a lowercase m sorts above the uppercase M
        var initials = Map.of("keys", List.of("a", "b"), "a", "'A'..'M'", "b", "'M'..");
        assertEquals("b", plugin.calculate(initials, "m"));
        assertEquals("a", plugin.calculate(initials, "B"));
        // and keep a zero-padded code as text, where 0000..1000 would mix a text and a numeric bound
        var codes = Map.of("keys", List.of("legacy", "current"), "legacy", "'0000'..'1000'", "current", "'1000'..");
        assertEquals("legacy", plugin.calculate(codes, "0450"));
        assertEquals("current", plugin.calculate(codes, "1000"));
        assertStartsWith("Range table mixes quoted and unquoted bounds",
                failure(Map.of("keys", List.of("a", "b"), "a", "'x'..'y'", "b", "1..2"), 1));
        assertStartsWith("Range of 'a' mixes a quoted and an unquoted bound", failure(oneBand("'x'..y"), 1));
    }

    @Test
    void shouldRefuseABandThatIsAListOrHoldsASecondSeparator() {
        // a key[]= list or a JSON list written as text is not a range
        assertStartsWith("Range of 'a' must be one range, not a list or a map",
                failure(Map.of("keys", List.of("a"), "a", List.of("1..2")), 1));
        assertStartsWith("Range of 'a' must be one range, not a list or a map", failure(oneBand("[ \"a..h\" ]"), "h"));
        assertStartsWith("Range of 'a' must be written as", failure(oneBand("a..b..c"), "b"));
        assertStartsWith("Range of 'a' must be written as", failure(oneBand("1...2"), 1));
    }

    /** A table with one band named 'a'. */
    private static Map<String, Object> oneBand(String range) {
        return Map.of("keys", List.of("a"), "a", range);
    }

    /** The plugin's refusal of the given arguments; the arguments are built before the lambda runs. */
    private IllegalArgumentException failure(Object... input) {
        return assertThrows(IllegalArgumentException.class, () -> plugin.calculate(input));
    }

    private static void assertStartsWith(String prefix, Exception e) {
        assertTrue(e.getMessage().startsWith(prefix), e.getMessage());
    }
}
