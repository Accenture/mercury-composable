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

package com.accenture.automation;

import com.accenture.services.plugins.arithmetic.*;
import com.accenture.services.plugins.logical.GreaterThanOperator;
import com.accenture.services.plugins.logical.LessThanOperator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Numeric promotion for the simple-plugin family (generalized from
 * whole-number-only): any floating-point argument promotes the whole
 * computation to Double, decided over ALL arguments so the result is
 * order-independent; all-integral inputs keep exact Long arithmetic —
 * including integer division, exactly as before the generalization.
 */
class SimplePluginNumberPromotionTest {

    @Test
    void allIntegralInputsKeepExactLongArithmetic() {
        assertEquals(11L, new AddNumbers().calculate(6, "2", 3));
        assertEquals(1L, new SubtractNumbers().calculate(6, 2, 3));
        assertEquals(25L, new MultiplyNumbers().calculate(5, 5));
        // integer division is preserved for whole-number inputs
        assertEquals(1L, new DivideNumbers().calculate(6, 4));
        assertEquals(1L, new ModulusNumbers().calculate(7, 3));
        assertEquals(7L, new IncrementNumbers().calculate(6));
        assertEquals(5L, new DecrementNumbers().calculate(6));
    }

    @Test
    void anyFloatingArgumentPromotesToDouble() {
        assertEquals(8.5d, new AddNumbers().calculate(6, 2.5));
        // order-independent: promotion is decided over all args, not fold order
        assertEquals(8.5d, new AddNumbers().calculate(2.5, 6));
        assertEquals(6.5d, new AddNumbers().calculate(1, "2.5", 3));
        assertEquals(10.0d, new MultiplyNumbers().calculate(2.5, 4));
        assertEquals(1.5d, new DivideNumbers().calculate(6.0, 4));
        assertEquals(1.5d, new ModulusNumbers().calculate(7.5, 2));
        assertEquals(3.5d, new IncrementNumbers().calculate(2.5));
        assertEquals(1.5d, new DecrementNumbers().calculate(2.5));
    }

    @Test
    void comparisonsFollowTheSamePromotion() {
        // both whole => exact long comparison
        assertEquals(true, new GreaterThanOperator().calculate(5, 3));
        assertEquals(false, new LessThanOperator().calculate(5, 3));
        // mixed types compare as double
        assertEquals(true, new GreaterThanOperator().calculate(2.5, 2));
        assertEquals(true, new LessThanOperator().calculate("2.5", 3));
    }

    @Test
    void roundIsHalfUpOnTheDecimalRepresentation() {
        assertEquals(2.35d, new RoundNumbers().calculate(2.345, 2));
        // the classic trap: a naive multiply-round-divide yields 1.0
        assertEquals(1.01d, new RoundNumbers().calculate(1.005, 2));
        assertEquals(3.0d, new RoundNumbers().calculate(2.5));
        // ties round away from zero
        assertEquals(-3.0d, new RoundNumbers().calculate(-2.5));
        assertEquals(3.142d, new RoundNumbers().calculate(3.14159, 3));
        assertEquals(2.35d, new RoundNumbers().calculate("2.345", 2));
        // a whole number is already exact and passes through unchanged
        assertEquals(5L, new RoundNumbers().calculate(5, 2));
        var round = new RoundNumbers();
        assertThrows(IllegalArgumentException.class, () -> round.calculate(1.5, 1.5));
        assertThrows(IllegalArgumentException.class, () -> round.calculate(1.5, -1));
    }

    @Test
    void errorsStayErrors() {
        var divide = new DivideNumbers();
        var add = new AddNumbers();
        // a floating-point zero divisor is still division by zero
        assertThrows(IllegalArgumentException.class, () -> divide.calculate(6, 0.0));
        assertThrows(IllegalArgumentException.class, () -> divide.calculate(6, 0));
        // non-numeric text is still rejected
        assertThrows(IllegalArgumentException.class, () -> add.calculate(1, "not-a-number"));
    }
}
