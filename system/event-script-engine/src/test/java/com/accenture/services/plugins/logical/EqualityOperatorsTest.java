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

package com.accenture.services.plugins.logical;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code eq} and {@code ne} operators compare exactly two values
 * (consistent with gt/lt), with optional {@code ignoreCase} and/or
 * {@code ignoreType} modifiers in argument positions 3 and 4.
 * {@code ignoreType} compares the {@code String.valueOf} text forms for a
 * relaxed comparison of numbers and booleans; {@code ignoreCase} compares
 * strings case-insensitively; together they compare text forms
 * case-insensitively.
 */
class EqualityOperatorsTest {
    private final EqualsOperator eq = new EqualsOperator();
    private final NotEqualsOperator ne = new NotEqualsOperator();

    @Test
    void strictComparisonWithoutModifiers() {
        assertEquals(true, eq.calculate(5, 5));
        assertEquals(true, eq.calculate("hello", "hello"));
        assertEquals(false, eq.calculate("Hello", "HELLO"));
        // without 'ignoreType', types matter
        assertEquals(false, eq.calculate("123", 123));
        assertEquals(false, eq.calculate(5, 5L));
        assertEquals(true, eq.calculate(null, null));
        assertEquals(false, eq.calculate(null, "x"));
        // ne is the exact complement
        assertEquals(false, ne.calculate(5, 5));
        assertEquals(true, ne.calculate("Hello", "HELLO"));
        assertEquals(true, ne.calculate("123", 123));
    }

    @Test
    void ignoreCaseComparesStringsCaseInsensitively() {
        assertEquals(true, eq.calculate("Hello", "HELLO", "ignoreCase"));
        assertEquals(false, eq.calculate("Hello", "World", "ignoreCase"));
        assertEquals(false, ne.calculate("Hello", "HELLO", "ignoreCase"));
        // non-string values are unaffected: types still matter
        assertEquals(true, eq.calculate(5, 5, "ignoreCase"));
        assertEquals(false, eq.calculate("123", 123, "ignoreCase"));
    }

    @Test
    void ignoreTypeComparesTextForms() {
        assertEquals(true, eq.calculate("123", 123, "ignoreType"));
        assertEquals(true, eq.calculate("123.456", 123.456, "ignoreType"));
        assertEquals(true, eq.calculate("true", true, "ignoreType"));
        assertEquals(true, eq.calculate(5, 5L, "ignoreType"));
        assertEquals(false, eq.calculate("124", 123, "ignoreType"));
        // 'ignoreType' alone stays case-sensitive
        assertEquals(false, eq.calculate("TRUE", true, "ignoreType"));
        assertEquals(true, ne.calculate("TRUE", true, "ignoreType"));
    }

    @Test
    void bothModifiersCombineInEitherOrder() {
        assertEquals(true, eq.calculate("TRUE", true, "ignoreCase", "ignoreType"));
        assertEquals(true, eq.calculate("TRUE", true, "ignoreType", "ignoreCase"));
        assertEquals(false, ne.calculate("TRUE", true, "ignoreType", "ignoreCase"));
        assertEquals(false, eq.calculate("FALSE", true, "ignoreCase", "ignoreType"));
    }

    @Test
    void invalidArgumentsAreRejected() {
        var tooFewEq = assertThrows(IllegalArgumentException.class, () -> eq.calculate(1));
        assertEquals("Input is required to check for equality", tooFewEq.getMessage());
        var tooFewNe = assertThrows(IllegalArgumentException.class, () -> ne.calculate(1));
        assertEquals("Input is required to check for inequality", tooFewNe.getMessage());
        var unknown = assertThrows(IllegalArgumentException.class,
                () -> eq.calculate(1, 2, "bogus"));
        assertEquals("Unknown modifier 'bogus' - only 'ignoreCase' and 'ignoreType' are supported",
                unknown.getMessage());
        // a third plain value is no longer a chained comparison
        var chained = assertThrows(IllegalArgumentException.class,
                () -> eq.calculate(5, 5, 5));
        assertTrue(chained.getMessage().startsWith("Unknown modifier '5'"));
        var tooMany = assertThrows(IllegalArgumentException.class,
                () -> ne.calculate(1, 2, "ignoreCase", "ignoreType", "ignoreCase"));
        assertEquals("Expected two values plus optional 'ignoreCase' and/or 'ignoreType' modifiers",
                tooMany.getMessage());
    }
}
