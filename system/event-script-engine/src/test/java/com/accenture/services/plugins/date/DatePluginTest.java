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

package com.accenture.services.plugins.date;

import com.accenture.models.PluginFunction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * addDays and daysBetween work on ISO-8601 calendar dates written as YYYY-MM-DD, with no time and no time zone:
 * addDays answers the date a whole number of days later (earlier for a negative number), and daysBetween the whole
 * days from the earlier date (excluded) to the later one (included), whatever the order of its arguments.
 */
class DatePluginTest {

    private final AddDays addDays = new AddDays();
    private final DaysBetween daysBetween = new DaysBetween();

    @Test
    void shouldUseAddDaysAndDaysBetweenAsPluginNames() {
        assertEquals("addDays", addDays.getName());
        assertEquals("daysBetween", daysBetween.getName());
    }

    @Test
    void shouldAddDaysAcrossMonthsYearsAndLeapDays() {
        assertEquals("2027-01-30", addDays.calculate("2027-01-15", 15));
        assertEquals("2027-01-15", addDays.calculate("2027-01-15", 0));
        assertEquals("2027-02-01", addDays.calculate("2027-01-31", 1));
        assertEquals("2028-01-01", addDays.calculate("2027-12-31", 1));
        // 2028 is a leap year and 2027 is not
        assertEquals("2028-02-29", addDays.calculate("2028-02-28", 1));
        assertEquals("2027-03-01", addDays.calculate("2027-02-28", 1));
        assertEquals("2029-01-15", addDays.calculate("2028-01-15", 366));
        // 2100 is not a leap year, 2000 was
        assertEquals("2100-03-01", addDays.calculate("2100-02-28", 1));
        assertEquals("2000-02-29", addDays.calculate("2000-02-28", 1));
    }

    @Test
    void shouldGoBackForANegativeNumberOfDays() {
        assertEquals("2027-01-15", addDays.calculate("2027-01-30", -15));
        assertEquals("2026-12-31", addDays.calculate("2027-01-01", -1));
        assertEquals("2028-02-29", addDays.calculate("2028-03-01", -1));
    }

    @Test
    void shouldReadTheDaysAsAnyWholeNumber() {
        // a long, numeric text and a whole double are all whole numbers
        assertEquals("2027-01-30", addDays.calculate("2027-01-15", 15L));
        assertEquals("2027-01-30", addDays.calculate("2027-01-15", "15"));
        assertEquals("2027-01-14", addDays.calculate("2027-01-15", "-1"));
        assertEquals("2027-01-30", addDays.calculate("2027-01-15", 15.0));
        assertEquals("2027-01-30", addDays.calculate("2027-01-15", new BigDecimal("15.00")));
        // the days of a daysBetween answer bring the earlier date to the later one
        var days = daysBetween.calculate("2027-04-10", "2027-05-11");
        assertEquals("2027-05-11", addDays.calculate("2027-04-10", days));
    }

    @Test
    void shouldReachTheEndsOfTheWritableYears() {
        assertEquals("9999-12-31", addDays.calculate("9999-12-30", 1));
        assertEquals("0000-01-01", addDays.calculate("0000-01-02", -1));
        // a year below 1000 is written with four digits
        assertEquals("0999-01-01", addDays.calculate("0998-12-31", 1));
        assertEquals("9999-12-31", addDays.calculate("0000-01-01", 3652424));
        assertEquals(3652424, daysBetween.calculate("0000-01-01", "9999-12-31"));
    }

    @Test
    void shouldCountTheDaysBetweenTwoDates() {
        assertEquals(31, daysBetween.calculate("2027-04-10", "2027-05-11"));
        assertEquals(0, daysBetween.calculate("2027-03-01", "2027-03-01"));
        assertEquals(1, daysBetween.calculate("2027-03-01", "2027-03-02"));
        assertEquals(1, daysBetween.calculate("2027-02-28", "2027-03-01"));
        assertEquals(2, daysBetween.calculate("2028-02-28", "2028-03-01"));
        assertEquals(365, daysBetween.calculate("2027-01-01", "2028-01-01"));
        assertEquals(366, daysBetween.calculate("2028-01-01", "2029-01-01"));
    }

    @Test
    void shouldCountTheSameDaysWhateverTheOrder() {
        assertEquals(31, daysBetween.calculate("2027-05-11", "2027-04-10"));
        assertEquals(1, daysBetween.calculate("2027-03-02", "2027-03-01"));
        assertEquals(daysBetween.calculate("2026-12-31", "2027-02-01"),
                daysBetween.calculate("2027-02-01", "2026-12-31"));
        // the answer is an integer, never negative
        assertInstanceOf(Integer.class, daysBetween.calculate("2027-05-11", "2027-04-10"));
    }

    @Test
    void shouldRefuseTheWrongNumberOfArguments() {
        assertEquals("Expected two input values in addDays - actual=1", failure(addDays, "2027-01-15").getMessage());
        assertEquals("Expected two input values in daysBetween - actual=3",
                failure(daysBetween, "2027-01-15", "2027-01-16", "2027-01-17").getMessage());
        assertStartsWith("Expected two input values in daysBetween - actual=0", failure(daysBetween));
    }

    @Test
    void shouldRefuseADateOfAnotherShape() {
        assertEquals("Expected a date written as YYYY-MM-DD, with no time and no time zone, in addDays"
                + " - got '2027-01-15T10:00:00'", failure(addDays, "2027-01-15T10:00:00", 1).getMessage());
        // a time zone, a single-digit month or day, another order, spaces and a date-like number are other shapes
        assertStartsWith("Expected a date written as YYYY-MM-DD", failure(daysBetween, "2027-01-15Z", "2027-01-16"));
        assertStartsWith("Expected a date written as YYYY-MM-DD", failure(daysBetween, "2027-1-5", "2027-01-16"));
        assertStartsWith("Expected a date written as YYYY-MM-DD", failure(daysBetween, "2027-01-16", "01/15/2027"));
        assertStartsWith("Expected a date written as YYYY-MM-DD", failure(addDays, " 2027-01-15", 1));
        assertStartsWith("Expected a date written as YYYY-MM-DD", failure(addDays, "+12027-01-15", 1));
        assertEquals("Expected a date written as YYYY-MM-DD, with no time and no time zone, in addDays - got 20270115",
                failure(addDays, 20270115, 1).getMessage());
        // an empty string is not a date either
        assertEquals("Expected a date written as YYYY-MM-DD, with no time and no time zone, in daysBetween - got ''",
                failure(daysBetween, "", "2027-01-16").getMessage());
    }

    @Test
    void shouldRefuseADayTheCalendarLacks() {
        assertEquals("Not a calendar date in addDays - got '2027-02-29'",
                failure(addDays, "2027-02-29", 1).getMessage());
        assertStartsWith("Not a calendar date in daysBetween", failure(daysBetween, "2027-04-31", "2027-05-01"));
        assertStartsWith("Not a calendar date in daysBetween", failure(daysBetween, "2027-01-01", "2027-13-01"));
        assertStartsWith("Not a calendar date in addDays", failure(addDays, "2027-00-10", 1));
        assertStartsWith("Not a calendar date in addDays", failure(addDays, "2100-02-29", 1));
        assertEquals("Not a calendar date in daysBetween - got '2027-02-30'",
                failure(daysBetween, "2027-03-01", "2027-02-30").getMessage());
    }

    @Test
    void shouldRefuseANullArgument() {
        assertStartsWith("Expected a date written as YYYY-MM-DD, with no time and no time zone, in addDays - got null",
                failure(addDays, null, 1));
        assertStartsWith("Expected a date written as YYYY-MM-DD", failure(daysBetween, "2027-01-15", null));
        assertEquals("The days of addDays must be a whole number - got null",
                failure(addDays, "2027-01-15", null).getMessage());
    }

    @Test
    void shouldRefuseDaysThatAreNotAWholeNumber() {
        assertEquals("The days of addDays must be a whole number - got 1.5",
                failure(addDays, "2027-01-15", 1.5).getMessage());
        assertEquals("The days of addDays must be a whole number - got 'fifteen'",
                failure(addDays, "2027-01-15", "fifteen").getMessage());
        assertStartsWith("The days of addDays must be a whole number", failure(addDays, "2027-01-15", "1.5"));
        assertStartsWith("The days of addDays must be a whole number", failure(addDays, "2027-01-15", true));
        assertStartsWith("The days of addDays must be a whole number", failure(addDays, "2027-01-15", List.of(1)));
        assertStartsWith("The days of addDays must be a whole number", failure(addDays, "2027-01-15", Double.NaN));
    }

    @Test
    void shouldRefuseAResultOutsideTheWritableYears() {
        assertEquals("The date from addDays falls outside 0000-01-01 to 9999-12-31, which YYYY-MM-DD can write",
                failure(addDays, "9999-12-31", 1).getMessage());
        assertStartsWith("The date from addDays falls outside", failure(addDays, "0000-01-01", -1));
        assertStartsWith("The date from addDays falls outside", failure(addDays, "2027-01-15", 3652425));
        assertStartsWith("The date from addDays falls outside", failure(addDays, "2027-01-15", Long.MIN_VALUE));
        assertStartsWith("The date from addDays falls outside",
                failure(addDays, "2027-01-15", "100000000000000000000"));
    }

    /** The plugin's refusal of the given arguments; the arguments are built before the lambda runs. */
    private static IllegalArgumentException failure(PluginFunction plugin, Object... input) {
        return assertThrows(IllegalArgumentException.class, () -> plugin.calculate(input));
    }

    private static void assertStartsWith(String prefix, Exception e) {
        assertTrue(e.getMessage().startsWith(prefix), e.getMessage());
    }
}
