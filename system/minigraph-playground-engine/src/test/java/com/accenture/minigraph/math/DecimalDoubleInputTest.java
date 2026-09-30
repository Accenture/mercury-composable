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

package com.accenture.minigraph.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A double in a DECIMAL statement (RFC-0001): accepted through the shortest decimal text it prints as, at its
 * minimal scale. That is exact over the text received and only as exact as the computation that produced the
 * double - a limit the guide declares, so sending a number instead of a string is a conscious decision. This
 * suite pins both halves, so the documentation cannot drift from the behavior.
 */
class DecimalDoubleInputTest {

    @Test
    void aDoubleRendersAtItsMinimalScaleInPlainNotation() {
        assertEquals("0.0375", DecimalEvaluator.plainText(0.0375));
        assertEquals("0.0005", DecimalEvaluator.plainText(5.0E-4));
        assertEquals("0.0001", DecimalEvaluator.plainText(1.0E-4));
        assertEquals("0.0000001", DecimalEvaluator.plainText(1.0E-7));
        assertEquals("100", DecimalEvaluator.plainText(100.0));
        assertEquals("10000000000", DecimalEvaluator.plainText(1.0E10));
        assertEquals("1.005", DecimalEvaluator.plainText(1.005));
        assertEquals("-0.5", DecimalEvaluator.plainText(-0.5));
        assertEquals("0", DecimalEvaluator.plainText(0.0));
        assertEquals("0", DecimalEvaluator.plainText(-0.0));
    }

    @Test
    void aFloatRendersTheSameWay() {
        assertEquals("0.1", DecimalEvaluator.plainText(0.1f));
        assertEquals("100", DecimalEvaluator.plainText(100.0f));
        assertEquals("0.0375", DecimalEvaluator.plainText(0.0375f));
    }

    @Test
    void nanAndInfinityAreRejectedByName() {
        var e = assertThrows(IllegalArgumentException.class, () -> DecimalEvaluator.plainText(Double.NaN));
        assertTrue(e.getMessage().startsWith("Not a finite number"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> DecimalEvaluator.plainText(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> DecimalEvaluator.plainText(Float.NaN));
    }

    @Test
    void theDeclaredLimitAnArtifactIsOnlyAsExactAsTheComputationThatMadeIt() {
        // COMPUTE: 1.005 * 100 is a double, and it is not 100.5
        var artifact = new ExpressionEngine().evalNumber("1.005 * 100");
        assertEquals("100.49999999999999", DecimalEvaluator.plainText(artifact));
        // fed into DECIMAL it is rounded faithfully, and wrongly: the exact 100.5 rounds up to 101
        assertEquals("100", DecimalEvaluator.evaluate("round(" + DecimalEvaluator.plainText(artifact) + ", 0, HALF_UP)"));
        assertEquals("101", DecimalEvaluator.evaluate("round(1.005 * 100, 0, HALF_UP)"));
        // the same number as a typed string has no such problem
        assertEquals("101", DecimalEvaluator.evaluate("round('100.5', 0, HALF_UP)"));
    }

    @Test
    void theDeclaredLimitAJsonNumberLongerThanADoubleHoldsIsAlreadyRoundedByTheParser() {
        // the parser hands over 1.2345678901234568E16: the digits are gone before any statement runs
        var parsed = Double.parseDouble("12345678901234567.89");
        assertNotEquals("12345678901234567.89", DecimalEvaluator.plainText(parsed));
        // as a string it is exact
        assertEquals("12345678901234567.89", DecimalEvaluator.evaluate("'12345678901234567.89' * 1"));
    }
}
