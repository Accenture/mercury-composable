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

import com.accenture.minigraph.common.GraphModelValidator;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.util.ConfigReader;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The limit on the decimal places of a DECIMAL result (ADR-0034): a result with more decimal places than the
 * limit is rounded HALF_UP to it, one with fewer keeps its computed scale, and the 'precision' setting that
 * states the limit is a whole number from 0 to 1000, checked at deploy.
 */
class DecimalPrecisionTest {

    @Test
    void aLongerResultIsRoundedHalfUpToTheLimit() {
        assertEquals("0.333333", DecimalEvaluator.evaluate("1 / 3", 6));
        assertEquals("0.666667", DecimalEvaluator.evaluate("2 / 3", 6));
        assertEquals("-0.666667", DecimalEvaluator.evaluate("-2 / 3", 6), "half up is away from zero");
        assertEquals("0.13", DecimalEvaluator.evaluate("0.125", 2));
        assertEquals("-0.13", DecimalEvaluator.evaluate("-0.125", 2));
        assertEquals("3", DecimalEvaluator.evaluate("2.5", 0));
        assertEquals("1.000000", DecimalEvaluator.evaluate("0.9999999", 6), "a carry keeps the limit's scale");
    }

    @Test
    void aShorterResultKeepsItsComputedScale() {
        assertEquals("3.0", DecimalEvaluator.evaluate("1.5 * 2", 6), "the limit never pads");
        assertEquals("10.50", DecimalEvaluator.evaluate("round(10.5, 2, HALF_UP)", 6));
        assertEquals("100", DecimalEvaluator.evaluate("100", 2));
        assertEquals("0", DecimalEvaluator.evaluate("0.0000004", 6), "a value that rounds to zero is \"0\"");
        assertEquals("0", DecimalEvaluator.evaluate("1.50 - 1.50", 2));
    }

    @Test
    void theLimitAppliesToTheResultNotToTheArithmetic() {
        // the quotient inside the expression keeps its 34 digits, so 1 / 3 * 3 is 0.99...9 and rounds to one
        assertEquals("1.000000", DecimalEvaluator.evaluate("1 / 3 * 3", 6));
        // an explicit round() to more places than the limit is rounded again to the limit - the node's
        // precision is raised for a result that needs more
        assertEquals("0.333333", DecimalEvaluator.evaluate("round(1 / 3, 8, HALF_UP)", 6));
        assertEquals("0.33333333", DecimalEvaluator.evaluate("round(1 / 3, 8, HALF_UP)", 8));
        // the largest limit leaves every result as the unbounded evaluator gives it
        assertEquals(DecimalEvaluator.evaluate("1 / 3"),
                DecimalEvaluator.evaluate("1 / 3", DecimalEvaluator.MAX_SCALE));
        assertEquals(34, new BigDecimal(DecimalEvaluator.evaluate("1 / 3")).precision());
    }

    @Test
    void limitScaleRoundsOnlyBeyondTheLimit() {
        var kept = new BigDecimal("1.50");
        assertSame(kept, DecimalEvaluator.limitScale(kept, 6));
        assertEquals(new BigDecimal("1.50"), DecimalEvaluator.limitScale(kept, 2));
        assertEquals(new BigDecimal("1.5"), DecimalEvaluator.limitScale(kept, 1));
        assertEquals(new BigDecimal("2"), DecimalEvaluator.limitScale(kept, 0));
    }

    @Test
    void aPrecisionSettingIsAWholeNumberFromZeroToTheMaximumScale() {
        assertEquals(6, DecimalEvaluator.precision("6"));
        assertEquals(6, DecimalEvaluator.precision(6));
        assertEquals(6, DecimalEvaluator.precision(6L));
        assertEquals(2, DecimalEvaluator.precision(" 2 "));
        assertEquals(0, DecimalEvaluator.precision("0"));
        assertEquals(1000, DecimalEvaluator.precision(1000));
        for (var bad : new Object[]{"-1", "1001", -1, 1001L, "two", "2.5", 2.5, "", null, true}) {
            var e = assertThrows(IllegalArgumentException.class, () -> DecimalEvaluator.precision(bad),
                    String.valueOf(bad));
            assertTrue(e.getMessage().startsWith("must be a whole number from 0 to 1000, got '"), e.getMessage());
        }
    }

    @Test
    void theModelValidatorRejectsAMalformedPrecisionOnAMathNodeOnly() {
        var graph = importGraph("unit-test-decimal-precision-bad");
        var e = assertThrows(IllegalArgumentException.class, () -> GraphModelValidator.validate(graph));
        assertEquals("node price - precision must be a whole number from 0 to 1000, got 'two'", e.getMessage());
        // a well-formed one passes
        var price = graph.findNodeByAlias("price");
        price.getProperties().put("precision", "2");
        assertDoesNotThrow(() -> GraphModelValidator.validate(graph));
        // 'precision' on another skill is that skill's business
        price.getProperties().put("precision", "two");
        price.getProperties().put("skill", "graph.data.mapper");
        assertDoesNotThrow(() -> GraphModelValidator.validate(graph));
        // the fixture that runs end to end passes as written: precision as text, as a number and as zero
        assertDoesNotThrow(() -> GraphModelValidator.validate(importGraph("unit-test-decimal-precision")));
    }

    private static MiniGraph importGraph(String id) {
        var reader = new ConfigReader("classpath:/graph/" + id + ".json");
        var graph = new MiniGraph();
        graph.importGraph(reader.getMap());
        return graph;
    }
}
