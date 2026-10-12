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

import static org.junit.jupiter.api.Assertions.*;

/**
 * minRange and maxRange read one range in the notation of rangeLookup's bands and answer its bound typed by the
 * notation: a long for a number written without a decimal point, the exact decimal text for any other number, text
 * as written for a text or quoted bound, the optional default (or null) for an open end; null for a null range.
 */
class RangeBoundsPluginTest {

    private final SimpleMinRange min = new SimpleMinRange();
    private final SimpleMaxRange max = new SimpleMaxRange();

    @Test
    void shouldUseMinRangeAndMaxRangeAsPluginNames() {
        assertEquals("minRange", min.getName());
        assertEquals("maxRange", max.getName());
    }

    @Test
    void shouldAnswerAWholeNumberAsALongAndADecimalAsItsExactText() {
        // a whole number is a long
        assertEquals(10000L, min.calculate("10000..25000"));
        assertEquals(25000L, max.calculate("10000..25000"));
        assertEquals(-5L, min.calculate("-5..5"));
        assertEquals(0L, max.calculate("-1..0"));
        // any other number is its exact decimal text - the DECIMAL convention; f:double converts it when wanted
        assertEquals("2500.50", min.calculate("2500.50..10000"));
        assertEquals("0.1", max.calculate("..0.1"));
        assertEquals("2.0", max.calculate("1.0..2.0"));
        // a whole number beyond the 64-bit range is its exact text too
        assertEquals("100000000000000000000", min.calculate("100000000000000000000.."));
        // spaces around the bounds are not part of them
        assertEquals(10L, min.calculate(" 10 .. 20 "));
        assertEquals(20L, max.calculate(" 10 .. 20 "));
    }

    @Test
    void shouldAnswerATextBoundAsWritten() {
        assertEquals("00:00", min.calculate("00:00..16:00"));
        assertEquals("16:00", max.calculate("00:00..16:00"));
        assertEquals("2026-11-15", min.calculate("2026-11-15..2027-01-01"));
        assertEquals("2027-01-01", max.calculate("2026-11-15..2027-01-01"));
        // the case is kept - only the lookup's comparison ignores it
        assertEquals("A", min.calculate("A..H"));
        assertEquals("H", max.calculate("A..H"));
    }

    @Test
    void shouldAnswerAQuotedBoundWithoutItsQuotes() {
        // quotes keep a zero-padded code as text, and either quote style works
        assertEquals("0020", min.calculate("'0020'..'0030'"));
        assertEquals("0030", max.calculate("\"0020\"..\"0030\""));
        assertEquals("2026-11-15", min.calculate("\"2026-11-15\"..\"2027-01-01\""));
        // quoted bounds compare as written, so 'A' sits below 'h'
        assertEquals("h", max.calculate("'A'..'h'"));
        // a quoted bound may contain the separator, and a quoted number is text
        assertEquals("a..b", min.calculate("'a..b'..'c'"));
        assertEquals("1000", min.calculate("'1000'.."));
        assertEquals("m", max.calculate("..'m'", "none"));
        // the other quote character is ordinary text inside a quoted bound, in either position - the YAML convention
        assertEquals("this is a 'nice' drawing", min.calculate("\"this is a 'nice' drawing\"..\"zebra\""));
        assertEquals("this is a \"nice\" drawing", max.calculate("'a'..'this is a \"nice\" drawing'"));
        assertEquals("O'Brien", min.calculate("\"O'Brien\"..\"O'Neil\""));
    }

    @Test
    void shouldAnswerNullForANullRangeNeverTheDefault() {
        // the band a lookup did not choose, read back through a dynamic key, has no bounds; the default belongs to an
        // open end, so an unknown tier never reads as "from 0" - the null-source rule leaves the target unset
        assertNull(min.calculate((Object) null));
        assertNull(max.calculate((Object) null));
        assertNull(min.calculate(null, 0));
        assertNull(max.calculate(null, "none"));
    }

    @Test
    void shouldOrderTextByCodePoint() {
        // U+FF10 (a fullwidth zero) sits below U+1F600 (an emoji) by code point, as the Rust engine orders text;
        // String.compareTo orders UTF-16 units, which would call this range empty
        assertEquals("\uFF10", min.calculate("'\uFF10'..'\uD83D\uDE00'"));
        assertEquals("\uD83D\uDE00", max.calculate("\uFF10..\uD83D\uDE00"));
        assertStartsWith("Range is empty", failure(min, "'\uD83D\uDE00'..'\uFF10'"));
    }

    @Test
    void shouldAnswerTheDefaultOrNullForAnOpenEnd() {
        assertNull(min.calculate("..2500"));
        assertNull(max.calculate("25000.."));
        assertEquals(0, min.calculate("..2500", 0));
        assertEquals("none", max.calculate("25000..", "none"));
        // the other end is still a bound
        assertEquals(2500L, max.calculate("..2500"));
        assertEquals(25000L, min.calculate("25000..", 0));
        // the default is ignored when the bound is written
        assertEquals(1L, min.calculate("1..2", 9));
        assertEquals("h", max.calculate("a..h", "none"));
    }

    @Test
    void shouldRefuseTheWrongNumberOfArgumentsAndANonRange() {
        assertStartsWith("Expected one or two input values - actual=0", failure(min));
        assertStartsWith("Expected one or two input values - actual=3", failure(max, "1..2", 0, 0));
        assertEquals("Range must be written as min..max, min.. or ..max (the upper bound is excluded) - got '1-2'",
                failure(max, "1-2").getMessage());
        assertStartsWith("Range must be written as", failure(min, ".."));
        assertStartsWith("Range must be written as", failure(min, "1..<2"));
        // a number is not a range
        assertStartsWith("Range must be written as", failure(min, 5));
    }

    @Test
    void shouldRefuseAListOrAMapAndASecondSeparator() {
        // a band is one range, never a list or a map - as a value or as its text
        assertEquals("Range must be one range, not a list or a map - got '[10000..25000]'",
                failure(min, java.util.List.of("10000..25000")).getMessage());
        assertStartsWith("Range must be one range, not a list or a map", failure(max, java.util.Map.of("a", "1..2")));
        assertStartsWith("Range must be one range, not a list or a map", failure(min, "[a..h]"));
        // an unquoted bound never contains the separator
        assertStartsWith("Range must be written as", failure(min, "a..b..c"));
        assertStartsWith("Range must be written as", failure(max, "1...2"));
    }

    @Test
    void shouldRefuseBadQuoting() {
        // a quote must be closed and end the bound; an open end is written by omission; quoting is all or nothing
        assertStartsWith("Range must be written as", failure(min, "'a..b"));
        assertStartsWith("Range must be written as", failure(max, "'a' x..'b'"));
        // a quoted bound never holds its own quote character, in either position - the other style does that
        assertStartsWith("Range must be written as", failure(min, "'a'b'..'z'"));
        assertStartsWith("Range must be written as", failure(max, "'a'..'b'c'"));
        assertStartsWith("Range must be written as", failure(max, "'a'..'b' x'"));
        assertEquals("Range has an empty quoted bound - an open end is written by omission: '''..'m''",
                failure(min, "''..'m'").getMessage());
        assertEquals("Range mixes a quoted and an unquoted bound - quote both bounds or neither: ''a'..b'",
                failure(min, "'a'..b").getMessage());
    }

    @Test
    void shouldRefuseAMixedOrEmptyRange() {
        assertStartsWith("Range mixes a numeric and a text bound - got '1..x'", failure(min, "1..x"));
        assertEquals("Range mixes a numeric and a text bound - got '00501..10000'"
                + " (a bound with a leading zero is text; quote both bounds to compare them as text)",
                failure(max, "00501..10000").getMessage());
        // an empty range holds nothing, so neither bound is meaningful
        assertEquals("Range is empty - its lower bound is not below its upper bound: '3 .. 3'",
                failure(max, "3 .. 3").getMessage());
        assertStartsWith("Range is empty", failure(min, "5..1"));
        assertStartsWith("Range is empty", failure(max, "h..a"));
        // quoted bounds compare as written: 'a' is above 'B'
        assertStartsWith("Range is empty", failure(max, "'a'..'B'"));
    }

    /** The plugin's refusal of the given arguments; the arguments are built before the lambda runs. */
    private static IllegalArgumentException failure(com.accenture.models.PluginFunction plugin, Object... input) {
        return assertThrows(IllegalArgumentException.class, () -> plugin.calculate(input));
    }

    private static void assertStartsWith(String prefix, Exception e) {
        assertTrue(e.getMessage().startsWith(prefix), e.getMessage());
    }
}
