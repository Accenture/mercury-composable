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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Claims-fixture pin for claim id {@code math-expression-dialect}: the graph.math expression
 * dialect is EXACTLY what the skills reference documents - the operator set, the eighteen
 * built-in functions (each also reachable under the {@code Math} namespace), the two constants
 * {@code PI} and {@code E}, and nothing else. A fresh AI agent generates expressions from that
 * table alone, so the table and the engine must not drift apart in either direction.
 *
 * <p>The set-equality assertion fails when a function or constant is ADDED or REMOVED; the
 * behavioral checks tie each documented operator and arity to the evaluator, one cohesive group
 * per test method, and the negative checks pin that the documented "not in the dialect" forms
 * really are rejected.
 */
class ClaimMathExpressionDialectTest {

    private static final Set<String> FUNCTIONS = Set.of(
            "sin", "cos", "tan", "asin", "acos", "atan",
            "sqrt", "abs", "floor", "ceil", "round",
            "log", "log10", "exp",
            "min", "max", "pow", "random");

    private static final Set<String> CONSTANTS = Set.of("PI", "E");

    private final ExpressionEngine engine = new ExpressionEngine();

    @Test
    void theDialectIsExactlyTheDocumentedFunctionsAndConstants() {
        Map<String, Object> root = EvalContext.withDefaults().snapshot();
        var documented = new HashSet<>(FUNCTIONS);
        documented.addAll(CONSTANTS);
        documented.add("Math"); // the namespace object that mirrors every function and constant
        assertEquals(documented, root.keySet(),
                "the graph.math dialect must expose exactly the documented functions and constants - "
                        + "adding or removing one changes the documented contract "
                        + "(claims-registry: math-expression-dialect; update skills-reference.md#math-dialect, "
                        + "help graph-math.md and minigraph-commands.json together)");
        for (String name : FUNCTIONS) {
            assertInstanceOf(MathFunction.class, root.get(name), name + " must be a function");
        }
        for (String name : CONSTANTS) {
            assertInstanceOf(Double.class, root.get(name), name + " must be a numeric constant");
        }
        // every top-level function and constant is mirrored under Math.* - and nothing else is
        @SuppressWarnings("unchecked")
        var math = (Map<String, Object>) root.get("Math");
        var mirrored = new HashSet<>(FUNCTIONS);
        mirrored.addAll(CONSTANTS);
        assertEquals(mirrored, math.keySet(), "Math.* must mirror exactly the documented functions and constants");
    }

    @Test
    void oneArgumentFunctionsEvaluate() {
        assertEquals(0.0, engine.evalNumber("sin(0)"), 1e-12);
        assertEquals(1.0, engine.evalNumber("cos(0)"), 1e-12);
        assertEquals(0.0, engine.evalNumber("tan(0)"), 1e-12);
        assertEquals(Math.PI / 2, engine.evalNumber("asin(1)"), 1e-12);
        assertEquals(0.0, engine.evalNumber("acos(1)"), 1e-12);
        assertEquals(Math.PI / 4, engine.evalNumber("atan(1)"), 1e-12);
        assertEquals(4.0, engine.evalNumber("sqrt(16)"), 1e-12);
        assertEquals(2.5, engine.evalNumber("abs(-2.5)"), 1e-12);
        assertEquals(2.0, engine.evalNumber("floor(2.9)"), 1e-12);
        assertEquals(3.0, engine.evalNumber("ceil(2.1)"), 1e-12);
        assertEquals(3.0, engine.evalNumber("round(2.5)"), 1e-12);   // half up toward positive infinity
        assertEquals(-2.0, engine.evalNumber("round(-2.5)"), 1e-12);
        assertEquals(1.0, engine.evalNumber("log(E)"), 1e-12);      // natural logarithm
        assertEquals(3.0, engine.evalNumber("log10(1000)"), 1e-12);
        assertEquals(Math.E, engine.evalNumber("exp(1)"), 1e-12);
    }

    @Test
    void variadicTwoArgumentAndZeroArgumentFunctionsEvaluate() {
        assertEquals(1.0, engine.evalNumber("min(3, 1, 2)"), 1e-12);
        assertEquals(3.0, engine.evalNumber("max(3, 1, 2)"), 1e-12);
        assertEquals(1024.0, engine.evalNumber("pow(2, 10)"), 1e-12);
        double r = engine.evalNumber("random()");
        assertTrue(r >= 0.0 && r < 1.0, "random() is in [0, 1)");
    }

    @Test
    void constantsAndTheMathNamespaceEvaluate() {
        assertEquals(Math.PI, engine.evalNumber("PI"), 0.0);
        assertEquals(Math.E, engine.evalNumber("Math.E"), 0.0);
        assertEquals(8.0, engine.evalNumber("Math.pow(2, 3)"), 1e-12);
        assertEquals(1.0, engine.evalNumber("Math.sin(Math.PI / 2)"), 1e-12);
    }

    @Test
    void aWrongArityFailsByName() {
        // never a silent default
        var e1 = assertThrows(IllegalArgumentException.class, () -> engine.evalNumber("pow(2)"));
        assertTrue(e1.getMessage().contains("pow expects 2 args"), e1.getMessage());
        var e2 = assertThrows(IllegalArgumentException.class, () -> engine.evalNumber("sqrt(4, 9)"));
        assertTrue(e2.getMessage().contains("Expected 1 argument"), e2.getMessage());
        var e3 = assertThrows(IllegalArgumentException.class, () -> engine.evalNumber("random(1)"));
        assertTrue(e3.getMessage().contains("random expects 0 args"), e3.getMessage());
    }

    @Test
    void arithmeticOperatorsAreAccepted() {
        // exponent: right-associative, binds tighter than unary minus - the strict JS rule
        assertEquals(512.0, engine.evalNumber("2 ** 3 ** 2"), 1e-12);
        assertEquals(-4.0, engine.evalNumber("-(2 ** 2)"), 1e-12);
        assertThrows(ParseException.class, () -> engine.evalNumber("-2 ** 2"));
        // unary, multiplicative (incl. remainder) and additive
        assertEquals(-3.0, engine.evalNumber("-3"), 0.0);
        assertEquals(3.0, engine.evalNumber("+3"), 0.0);
        assertEquals(1.0, engine.evalNumber("7 % 3"), 1e-12);
        assertEquals(14.0, engine.evalNumber("2 + 3 * 4"), 1e-12);
        assertEquals(20.0, engine.evalNumber("(2 + 3) * 4"), 1e-12);
        assertEquals(2.5, engine.evalNumber("10 / 4"), 1e-12);
        // '+' concatenates when either side is a string
        assertEquals("id-7", engine.evaluateValue("'id-' + 7").asString());
    }

    @Test
    void comparisonLogicalAndTernaryOperatorsAreAccepted() {
        // relational on numbers and on two strings (lexical - ISO-8601 timestamps compare correctly)
        assertTrue(engine.evalBoolean("1 < 2 && 2 <= 2 && 3 > 2 && 3 >= 3"));
        assertTrue(engine.evalBoolean("'2026-03-02T01:00:01Z' > '2026-03-02T01:00:00Z'"));
        // equality is same-type only
        assertTrue(engine.evalBoolean("5 == 5.0 && 1 != 2 && 'a' == 'a' && true == true"));
        assertThrows(IllegalArgumentException.class, () -> engine.evalBoolean("'1' == 1"));
        // logical not / and / or (short-circuit) and the ternary
        assertTrue(engine.evalBoolean("!false && (false || true)"));
        assertEquals(1.0, engine.evalNumber("2 > 1 ? 1 : 0"), 0.0);
        assertEquals(0.0, engine.evalNumber("2 < 1 ? 1 : 0"), 0.0);
    }

    @Test
    void literalsAreAccepted() {
        // integer, decimal, leading-dot and exponent numbers; single- and double-quoted strings; booleans
        assertEquals(0.5, engine.evalNumber(".5"), 1e-12);
        assertEquals(1230.0, engine.evalNumber("1.23e3"), 1e-12);
        assertEquals("a\"b", engine.evaluateValue("'a\"b'").asString());
        assertEquals("a'b", engine.evaluateValue("\"a'b\"").asString());
        assertTrue(engine.evalBoolean("true"));
        assertFalse(engine.evalBoolean("false"));
    }

    @Test
    void theDocumentedExclusionsAreRejected() {
        // no bitwise or shift operators, no assignment, no user identifiers, no user-defined functions
        for (String expr : new String[] {"1 & 2", "1 | 2", "1 ^ 2", "~1", "1 << 2", "x = 1"}) {
            assertThrows(ParseException.class, () -> engine.evalNumber(expr), expr + " must not parse");
        }
        var e1 = assertThrows(IllegalArgumentException.class, () -> engine.evalNumber("total + 1"));
        assertTrue(e1.getMessage().startsWith("Unknown identifier: total"), e1.getMessage());
        var e2 = assertThrows(IllegalArgumentException.class, () -> engine.evalNumber("hypot(3, 4)"));
        assertEquals("Unknown function: hypot", e2.getMessage());
        var e3 = assertThrows(IllegalArgumentException.class, () -> engine.evalNumber("Math.hypot(3, 4)"));
        assertEquals("Unknown function: Math.hypot", e3.getMessage());
    }
}
