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
 * A string that is a canonical number compares as a number in COMPUTE, IF and CONDITION (RFC-0001), so
 * '200' == '200', 200 == 200 and 200 == '200' are the same comparison. A string that is not a canonical
 * number keeps today's behaviour.
 */
class NumericStringComparisonTest {
    private final ExpressionEngine engine = new ExpressionEngine();

    private void isTrue(String expression) {
        assertEquals(true, engine.evalBoolean(expression), expression);
    }

    private void isFalse(String expression) {
        assertEquals(false, engine.evalBoolean(expression), expression);
    }

    @Test
    void theThreeSpellingsOfOneComparisonAreTheSame() {
        isTrue("'200' == '200'");
        isTrue("200 == 200");
        isTrue("200 == '200'");
        isTrue("'200' == 200");
        isFalse("'200' != 200");
        isFalse("200 != '200'");
    }

    @Test
    void numericStringsOrderAsNumbersNotAsText() {
        isFalse("'9.5' > '10.25'");
        isTrue("'9.5' < '10.25'");
        isFalse("9.5 > '10.25'");
        isFalse("'9.5' > 10.25");
        isTrue("'10.25' >= 10.25");
        isTrue("'-5' < 0");
        isTrue("'-0' == 0");
    }

    @Test
    void equalNumbersWrittenDifferentlyAreEqual() {
        isTrue("'1.0' == '1'");
        isTrue("'200' == '200.0'");
        isTrue("200 == '200.00'");
        isTrue("'0.30000000000000004' == 0.1 + 0.2");
        isFalse("'0.3' == 0.1 + 0.2");
    }

    @Test
    void theComparisonIsExactSoDistinctLongIdsStayDistinct() {
        isFalse("'12345678901234567890' == '12345678901234567891'");
        isTrue("'12345678901234567890' == '12345678901234567890'");
        isTrue("'12345678901234567890' < '12345678901234567891'");
    }

    @Test
    void aStringThatIsNotACanonicalNumberKeepsTodaysBehaviour() {
        // leading zeros, a plus sign, an exponent: text
        isFalse("'007' == '7'");
        isFalse("'+5' == '5'");
        isFalse("'1e3' == '1000'");
        isTrue("'abc' == 'abc'");
        isTrue("'abc' < 'abd'");
        // a numeric string against text compares as two strings
        isFalse("'9.5' > 'abc'");
        isFalse("'200' == 'abc'");
    }

    @Test
    void aNumberAgainstTextIsStillAnError() {
        var e = assertThrows(IllegalArgumentException.class, () -> engine.evalBoolean("200 == 'abc'"));
        assertTrue(e.getMessage().startsWith("Type mismatch for equality"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> engine.evalBoolean("200 > 'abc'"));
        assertThrows(IllegalArgumentException.class, () -> engine.evalBoolean("200 == '007'"));
    }

    @Test
    void aBooleanIsStillNotANumber() {
        assertThrows(IllegalArgumentException.class, () -> engine.evalBoolean("true == 'true'"));
        assertThrows(IllegalArgumentException.class, () -> engine.evalBoolean("true == '1'"));
    }

    @Test
    void numberAgainstNumberIsUntouched() {
        isTrue("200 == 200");
        isTrue("1.0 == 1");
        isFalse("0.1 + 0.2 == 0.3");
        isTrue("0.1 + 0.2 > 0.3");
    }
}
