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
        var e = assertThrows(IllegalArgumentException.class, () -> plugin.calculate(SCORE_BANDS));
        assertTrue(e.getMessage().startsWith("Expected two or three input values"));
        e = assertThrows(IllegalArgumentException.class, () -> plugin.calculate(Map.of("a", "1..2"), 1));
        assertEquals("Missing keys in input", e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> plugin.calculate(Map.of("keys", List.of("a", "b"), "a", "1..2"), 1));
        assertEquals("Missing key in input: b", e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> plugin.calculate(Map.of("keys", List.of("a"), "a", "1-2"), 1));
        assertTrue(e.getMessage().startsWith("Range of 'a' must be written as"), e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> plugin.calculate(Map.of("keys", List.of("a"), "a", ".."), 1));
        assertTrue(e.getMessage().startsWith("Range of 'a' must be written as"), e.getMessage());
        // the exclusive-upper notation of a first draft is refused rather than silently accepted
        e = assertThrows(IllegalArgumentException.class, () -> plugin.calculate(Map.of("keys", List.of("a"), "a", "1..<2"), 1));
        assertTrue(e.getMessage().startsWith("Range of 'a' must be written as"), e.getMessage());
        // a band whose bounds are equal holds nothing
        e = assertThrows(IllegalArgumentException.class, () -> plugin.calculate(Map.of("keys", List.of("a"), "a", "2..2"), 2));
        assertTrue(e.getMessage().startsWith("Range of 'a' is empty"), e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> plugin.calculate(Map.of("keys", List.of("a"), "a", "5..1"), 3));
        assertTrue(e.getMessage().startsWith("Range of 'a' is empty"), e.getMessage());
        e = assertThrows(IllegalArgumentException.class,
                () -> plugin.calculate(Map.of("keys", List.of("a", "b"), "a", "1..2", "b", "x..y"), 1));
        assertTrue(e.getMessage().startsWith("Range table mixes numeric and text bounds"), e.getMessage());
        // numeric bands, non-numeric value: fails by name, as graph.math does
        e = assertThrows(IllegalArgumentException.class, () -> plugin.calculate(SCORE_BANDS, "abc"));
        assertTrue(e.getMessage().contains("Expected a decimal number in rangeLookup"), e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> plugin.calculate(SCORE_BANDS, true));
        assertTrue(e.getMessage().startsWith("Boolean operand in rangeLookup"), e.getMessage());
    }
}
