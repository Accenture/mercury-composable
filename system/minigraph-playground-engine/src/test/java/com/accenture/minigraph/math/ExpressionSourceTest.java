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

import com.accenture.minigraph.math.ExpressionSource.Kind;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A value from the data is one value where it lands in a graph.math expression, never more expression.
 */
class ExpressionSourceTest {
    private static final String X = "input.body.x";
    private static final String TIMES_TWO = "{input.body.x} * 2";
    private static final String EQUALS_X = "{input.body.x} == 'x'";
    private static final String DOUBLE_QUOTED = "\"{input.body.x}\" == 'x'";
    private static final String MEMBER = "Math.{input.body.x}(2)";
    private static final String BRANCH = "'a' == 'b' ? {input.body.x} : 0";
    private static final String ISO_DATE = "2025-07-15";

    private static String render(String expression, Kind kind, Object x) {
        var values = new HashMap<String, Object>();
        values.put(X, x);
        values.put("input.body.y", "2025-01-01");
        return ExpressionSource.render(expression, kind, values::get);
    }

    @Test
    void aTextValueThatSpellsOneNumberReadsAsWritten() {
        // every form the lexer reads as one number literal is rendered as it always was
        for (var value : List.of("100.25", "0", "1e3", "1E-5", "007", ".5", "+5", "-5", " 5 ",
                "true", "false", "null")) {
            assertEquals(value + " * 2", render(TIMES_TWO, Kind.NUMBER, value));
            assertEquals(value + " * 2", render(TIMES_TWO, Kind.DECIMAL, value));
        }
    }

    @Test
    void anyOtherTextInANumberExpressionFailsNamingTheSelector() {
        for (var value : List.of("2*3", "10-1", "(5)", ISO_DATE, "A4352", "5 5", "5.", "1,000", "0x10", "PI",
                "1/0", "max(1,2)", "3 > 2 ? 1 : 0", "1e", "--5", "Infinity", "")) {
            assertRefused(Kind.NUMBER, value);
            assertRefused(Kind.DECIMAL, value);
        }
    }

    private static void assertRefused(Kind kind, String value) {
        var e = assertThrows(IllegalArgumentException.class, () -> render(TIMES_TWO, kind, value));
        assertTrue(e.getMessage().startsWith("Not a number: input.body.x in '{input.body.x} * 2'"), e.getMessage());
    }

    @Test
    void theRefusalNeverCarriesTheValue() {
        var e = assertThrows(IllegalArgumentException.class, () -> render(TIMES_TWO, Kind.DECIMAL, ISO_DATE));
        assertFalse(e.getMessage().contains(ISO_DATE), e.getMessage());
    }

    @Test
    void aNumberLongerThanTheBoundIsRefused() {
        var digits = "9".repeat(1001);
        assertTrue(ExpressionSource.isOneNumber("9".repeat(1000)));
        assertFalse(ExpressionSource.isOneNumber(digits));
        assertThrows(IllegalArgumentException.class, () -> render(TIMES_TWO, Kind.NUMBER, digits));
    }

    @Test
    void aTextValueInABooleanExpressionIsAStringLiteral() {
        assertEquals("'2*3' == 'x'", render(EQUALS_X, Kind.BOOLEAN, "2*3"));
        assertEquals("'200' == 200", render("{input.body.x} == 200", Kind.BOOLEAN, "200"));
        // the quote and the backslash of a value stay inside its literal
        assertEquals("'O\\'Brien' == 'x'", render(EQUALS_X, Kind.BOOLEAN, "O'Brien"));
        assertEquals("'a\\\\b' == 'x'", render(EQUALS_X, Kind.BOOLEAN, "a\\b"));
    }

    @Test
    void aSelectorInsideQuotesTheAuthorWroteIsTheTextOfThatLiteral() {
        for (var kind : Kind.values()) {
            assertEquals("'2025-07-15' >= '2025-01-01' ? 100 : 0",
                    render("'{input.body.x}' >= '{input.body.y}' ? 100 : 0", kind, ISO_DATE));
            // a value cannot close the literal it lands in, whichever quote the author used
            assertEquals("'a\\' + \\'b' == 'x'", render("'{input.body.x}' == 'x'", kind, "a' + 'b"));
            assertEquals("\"a\\\" + \\\"b\" == 'x'", render(DOUBLE_QUOTED, kind, "a\" + \"b"));
            // the other quote character is text inside the literal
            assertEquals("\"it's\" == 'x'", render(DOUBLE_QUOTED, kind, "it's"));
            // an escaped quote in the author's literal does not close it
            assertEquals("'it\\'s 2*3' == 'x'", render("'it\\'s {input.body.x}' == 'x'", kind, "2*3"));
        }
    }

    @Test
    void aLiteralThatEndedBeforeTheSelectorDoesNotQuoteIt() {
        assertEquals("'a' == 'b' ? 5 : 0", render(BRANCH, Kind.DECIMAL, "5"));
        assertThrows(IllegalArgumentException.class,
                () -> render(BRANCH, Kind.DECIMAL, "5+5"));
    }

    @Test
    void aNumberOrABooleanIsItself() {
        assertEquals("3 * 2", render(TIMES_TWO, Kind.NUMBER, 3));
        assertEquals("-3 * 2", render(TIMES_TWO, Kind.DECIMAL, -3L));
        assertEquals("true * 2", render(TIMES_TWO, Kind.NUMBER, true));
        assertEquals("false == 'x'", render(EQUALS_X, Kind.BOOLEAN, false));
    }

    @Test
    void aDoubleRendersAsItsPlainDecimalTextInADecimalStatementOnly() {
        assertEquals("0.0005 * 2", render(TIMES_TWO, Kind.DECIMAL, 5.0E-4));
        assertEquals("100 * 2", render(TIMES_TWO, Kind.DECIMAL, 100.0f));
        assertEquals("5.0E-4 * 2", render(TIMES_TWO, Kind.NUMBER, 5.0E-4));
        var e = assertThrows(IllegalArgumentException.class, () -> render(TIMES_TWO, Kind.DECIMAL, Double.NaN));
        assertEquals("Not a finite number: NaN: input.body.x in '{input.body.x} * 2'", e.getMessage());
    }

    @Test
    void aTextValueNextToADotIsANameOrDigits() {
        assertEquals("Math.abs(2)", render(MEMBER, Kind.NUMBER, "abs"));
        assertEquals("3.5 * 2", render("{input.body.x}.5 * 2", Kind.NUMBER, "3"));
        var e = assertThrows(IllegalArgumentException.class,
                () -> render(MEMBER, Kind.BOOLEAN, "abs(1) + max"));
        assertTrue(e.getMessage().startsWith("Not a name: input.body.x in 'Math.{input.body.x}(2)'"),
                e.getMessage());
    }

    @Test
    void aTernaryIsAQuestionMarkOutsideAStringLiteral() {
        assertTrue(ExpressionSource.hasTernary("{model.a} > {model.b} ? 100 : 50"));
        assertTrue(ExpressionSource.hasTernary("({model.flag} ? 1 : 2) * 3"));
        assertTrue(ExpressionSource.hasTernary("'{model.a}' >= 'what?' ? 1 : 0"));
        assertFalse(ExpressionSource.hasTernary("{model.a} > {model.b}"));
        // a question mark the author wrote inside a literal is text, whichever quote and whatever it escapes
        assertFalse(ExpressionSource.hasTernary("{model.text} == 'what?'"));
        assertFalse(ExpressionSource.hasTernary("{model.text} == \"is it?\""));
        assertFalse(ExpressionSource.hasTernary("{model.text} == 'it\\'s what?'"));
        assertTrue(ExpressionSource.hasTernary("{model.text} == 'a\\\\' ? 1 : 0"));
    }

    @Test
    void anUnresolvedSelectorRendersNullAndOtherTextIsLeftAsWritten() {
        assertEquals("null * 2", render(TIMES_TWO, Kind.NUMBER, null));
        // a segment with a colon is not a selector
        assertEquals("{\"a\": 1} 5", render("{\"a\": 1} {input.body.x}", Kind.NUMBER, "5"));
        assertEquals("1 + 2", render("1 + 2", Kind.DECIMAL, "ignored"));
        assertEquals("[1,2] * 2", render(TIMES_TWO, Kind.NUMBER, List.of(1, 2)));
        assertEquals("{\"a\":1} * 2", render(TIMES_TWO, Kind.NUMBER, Map.of("a", 1)));
    }
}
