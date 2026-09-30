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

package com.accenture.services.plugins.arithmetic;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The decimal plugins compute exactly and answer a canonical decimal string. The shared conformance vectors run
 * in the minigraph-playground-engine module; this class pins the operand types a flow can hand a plugin.
 */
class DecimalPluginsTest {

    @Test
    void wholeNumbersAndDecimalTypesAreExactOperands() {
        assertEquals("3", new DecimalAdd().calculate(1, 2L));
        assertEquals("3", new DecimalAdd().calculate((short) 1, BigInteger.TWO));
        assertEquals("3.30", new DecimalAdd().calculate(new BigDecimal("1.10"), "2.2"));
    }

    @Test
    void aDoubleOrFloatIsTakenThroughItsShortestDecimalText() {
        assertEquals("0.3", new DecimalAdd().calculate(0.1d, 0.2d));
        assertEquals("0.0005", new DecimalAdd().calculate(5.0E-4d, 0));
        assertEquals("100", new DecimalAdd().calculate(100.0d, 0));
        assertEquals("0.1", new DecimalAdd().calculate(0.1f, 0));
    }

    @Test
    void nonFiniteDoublesAreRefused() {
        var nan = assertThrows(IllegalArgumentException.class, () -> new DecimalAdd().calculate(Double.NaN, 1));
        assertTrue(nan.getMessage().contains("Not a finite number"));
        assertThrows(IllegalArgumentException.class, () -> new DecimalAdd().calculate(Double.POSITIVE_INFINITY, 1));
    }

    @Test
    void compareAnswersMinusOneZeroOrOne() {
        assertEquals(-1L, new DecimalCompare().calculate("1", "2"));
        assertEquals(0L, new DecimalCompare().calculate("2.0", 2));
        assertEquals(1L, new DecimalCompare().calculate(3, "2.999"));
    }

    @Test
    void theResultIsAStringSoItSurvivesSuspendAndResume() {
        assertInstanceOf(String.class, new DecimalMultiply().calculate("1.5", "2"));
        assertInstanceOf(String.class, new DecimalDiv().calculate(1, 3));
    }

    @Test
    void anOversizedResultIsRefused() {
        var big = BigInteger.TEN.pow(6000);
        var e = assertThrows(IllegalArgumentException.class, () -> new DecimalMultiply().calculate(big, big));
        assertTrue(e.getMessage().contains("too large"));
    }
}
