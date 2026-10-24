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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

/**
 * Calendar-date arithmetic for the {@code addDays} and {@code daysBetween} simple plugins.
 * <p>
 * A date is an ISO-8601 calendar date written as {@code YYYY-MM-DD} - a plain day with no time and no time zone,
 * so the answer never depends on where the application runs. The text must be exactly that shape and a real day
 * of the proleptic Gregorian calendar ({@code 2028-02-29} is one, {@code 2027-02-29} is not); anything else, a
 * null included, is an error naming the plugin. A result is written the same way, so it must fall within the
 * years 0000 to 9999.
 */
public class DatePluginUtils {
    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final LocalDate MIN_DATE = LocalDate.of(0, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);
    /** The widest span between two writable dates; a larger number of days never lands on one. */
    private static final long MAX_DAYS = ChronoUnit.DAYS.between(MIN_DATE, MAX_DATE);

    private DatePluginUtils() {
        // utility class
    }

    /**
     * Checks the number of arguments of a date plugin.
     *
     * @param input the arguments
     * @param context the plugin name, for the error message
     */
    public static void expectTwo(Object[] input, String context) {
        if (input.length != 2) {
            throw new IllegalArgumentException("Expected two input values in " + context + " - actual=" +
                    input.length);
        }
    }

    /**
     * Reads a date argument.
     *
     * @param value a plugin argument
     * @param context the plugin name, for the error message
     * @return the calendar date it denotes
     * @throws IllegalArgumentException when it is not a date written as YYYY-MM-DD
     */
    public static LocalDate date(Object value, String context) {
        if (value instanceof String text && ISO_DATE.matcher(text).matches()) {
            try {
                return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Not a calendar date in " + context + " - got '" + text + "'");
            }
        }
        throw new IllegalArgumentException("Expected a date written as YYYY-MM-DD, with no time and no time zone, in " +
                context + " - got " + describe(value));
    }

    /**
     * Reads a number of days: a whole number, or a number or numeric string whose value is whole.
     *
     * @param value a plugin argument
     * @param context the plugin name, for the error message
     * @return the number of days, at most the span between 0000-01-01 and 9999-12-31 either way
     * @throws IllegalArgumentException when it is not a whole number, or too large to land on a writable date
     */
    public static long days(Object value, String context) {
        final BigDecimal number;
        try {
            number = DecimalPluginUtils.operand(value, context).stripTrailingZeros();
        } catch (IllegalArgumentException e) {
            throw daysError(value, context);
        }
        if (number.scale() > 0) {
            throw daysError(value, context);
        }
        if (number.abs().compareTo(BigDecimal.valueOf(MAX_DAYS)) > 0) {
            throw outOfRange(context);
        }
        return number.longValueExact();
    }

    /**
     * Writes a resulting date.
     *
     * @param date the date
     * @param context the plugin name, for the error message
     * @return the date written as YYYY-MM-DD
     * @throws IllegalArgumentException when the date falls outside the years 0000 to 9999
     */
    public static String text(LocalDate date, String context) {
        if (date.isBefore(MIN_DATE) || date.isAfter(MAX_DATE)) {
            throw outOfRange(context);
        }
        return date.toString();
    }

    /**
     * Counts the days between two dates, in either order.
     *
     * @param a one date
     * @param b the other date
     * @return the whole days from the earlier date (excluded) to the later date (included); 0 for the same date
     */
    public static int daysBetween(LocalDate a, LocalDate b) {
        return Math.toIntExact(Math.abs(ChronoUnit.DAYS.between(a, b)));
    }

    private static IllegalArgumentException daysError(Object value, String context) {
        return new IllegalArgumentException("The days of " + context + " must be a whole number - got " +
                describe(value));
    }

    private static String describe(Object value) {
        return value instanceof String text? "'" + text + "'" : String.valueOf(value);
    }

    private static IllegalArgumentException outOfRange(String context) {
        return new IllegalArgumentException("The date from " + context +
                " falls outside 0000-01-01 to 9999-12-31, which YYYY-MM-DD can write");
    }
}
