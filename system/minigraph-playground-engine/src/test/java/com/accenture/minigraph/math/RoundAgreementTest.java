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

import com.accenture.services.plugins.arithmetic.RoundNumbers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The dialect's round(x) and the f:round(x) plugin are the same rule: half up, ties away from zero. They once
 * differed on a negative tie - the dialect used Math.round, so round(-2.5) was -2 while f:round(-2.5) was -3.
 */
class RoundAgreementTest {
    private final ExpressionEngine engine = new ExpressionEngine();

    @Test
    void theDialectAndThePluginRoundEveryTieTheSameWay() {
        var plugin = new RoundNumbers();
        for (var x : new double[]{-3.5, -2.5, -1.5, -0.5, 0.5, 1.5, 2.5, 3.5, -2.4, 2.4, -2.6, 2.6, 0.0}) {
            var expected = ((Number) plugin.calculate(x)).doubleValue();
            assertEquals(expected, engine.evalNumber("round(" + x + ")"), 1e-12, "round(" + x + ")");
        }
    }
}
