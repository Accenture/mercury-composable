package com.accenture.minigraph.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionEngineFullTest {

    @Test
    void arithmetic_precedence_and_exponent_right_assoc() {
        ExpressionEngine engine = new ExpressionEngine();
        assertEquals(14.0, engine.evalNumber("2 + 3 * 4"), 1e-12);
        assertEquals(20.0, engine.evalNumber("(2 + 3) * 4"), 1e-12);
        assertEquals(Math.pow(2, Math.pow(3, 2)), engine.evalNumber("2 ** 3 ** 2"), 1e-12);
        assertEquals(0.56, engine.evalNumber("2 * (3 + 4) / 5 ** 2"), 1e-12);
        assertEquals(1.0, engine.evalNumber("10 % 3"), 1e-12);
    }

    @Test
    void strict_js_rule_unary_cannot_be_left_of_exponent() {
        // Using try-catch instead of Assertions.assertThrows to be compliant with SonarQube check
        var hasError = false;
        try {
            // -2 ** 2 should be a parse error (JS behavior)
            new ExpressionEngine().evalNumber("-2 ** 2");
        } catch (ParseException e) {
            hasError = true;
            assertTrue(e.getMessage().contains("Unary expression"));
        }
        assertTrue(hasError);
        // Parenthesized is OK
        assertEquals(-4.0, new ExpressionEngine().evalNumber("-(2 ** 2)"), 1e-12);
    }

    @Test
    void booleans_and_short_circuit() {
        EvalContext ctx = EvalContext.withDefaults()
                .define("boom", a -> { throw new RuntimeException("boom"); });

        assertFalse(new ExpressionEngine(ctx).evalBoolean("0 && boom()")); // short-circuit false
        assertTrue(new ExpressionEngine(ctx).evalBoolean("1 || boom()"));  // short-circuit true

        ExpressionEngine engine = new ExpressionEngine();
        assertTrue(engine.evalBoolean("0 || 1 && 1"));     // short-circuit happens early without parenthesis
        assertFalse(engine.evalBoolean("(0 || 1) && 0"));  // (true) && false -> false
    }

    @Test
    void truthiness_for_numbers_and_strings() {
        ExpressionEngine engine = new ExpressionEngine();
        assertTrue(engine.evalBoolean("3.14"));
        assertFalse(engine.evalBoolean("0"));
        assertTrue(engine.evalBoolean("'x'"));
        assertFalse(engine.evalBoolean("''"));
    }

    @Test
    void ternary_returns_branch_value_and_precedence() {
        ExpressionEngine engine = new ExpressionEngine();
        assertEquals(42.0, engine.evalNumber("true ? 42 : 7"), 0.0);
        assertEquals(7.0, engine.evalNumber("false ? 42 : 7"), 0.0);
        assertEquals(2.0, engine.evalNumber("0 || 1 ? 2 : 3"), 0.0); // (0 || 1) ? 2 : 3 -> 2
        assertEquals(3.0, engine.evalNumber("0 || 0 ? 2 : 3"), 0.0);
    }

    @Test
    void string_literals_and_comparisons() {
        ExpressionEngine engine = new ExpressionEngine();
        assertTrue(engine.evalBoolean("('XYZ' > 'ABC' == true)"));
        // ISO-8601 timestamp comparison
        assertTrue(engine.evalBoolean("'2026-03-02T01:00:01.000Z' > '2026-03-02T01:00:00.001Z' == true"));
        assertTrue(engine.evalBoolean("'abc' < 'abd'"));
        assertTrue(engine.evalBoolean("'a' == 'a'"));
        assertFalse(engine.evalBoolean("'a' != 'a'"));
    }

    @Test
    void string_concatenation_and_mixed_types() {
        ExpressionEngine engine = new ExpressionEngine();
        // pure strings
        assertTrue(engine.evalBoolean("('a' + 'b') == 'ab'"));
        // string + number
        assertTrue(engine.evalBoolean("('answer=' + (1 + 2)) == 'answer=3'"));
        // number + string
        assertTrue(engine.evalBoolean("((1 + 2) + ' apples') == '3 apples'"));
        // string + boolean
        assertTrue(engine.evalBoolean("('x' + true) == 'xtrue'"));
        // boolean + string
        assertTrue(engine.evalBoolean("(false + 'y') == 'falsey'"));
        // Combine multiple
        assertTrue(engine.evalBoolean("('A' + 1 + true + 'Z') == 'A1trueZ'"));
    }

    @Test
    void variables_and_context_values_numbers_and_strings() {
        EvalContext ctx = EvalContext.withDefaults()
                .defineVariable("x", 3.0)
                .defineVariable("rate", 0.15)
                .defineString("greet", "Hello");

        ExpressionEngine engine = new ExpressionEngine(ctx);
        assertEquals(10.5, engine.evalNumber("x ** 2 + 10 * rate"), 1e-12); // 9 + 1.5
        assertTrue(engine.evalBoolean("(greet + ', world') == 'Hello, world'"));
    }

    @Test
    void math_functions_and_namespace() {
        ExpressionEngine engine = new ExpressionEngine();
        assertEquals(1.0, engine.evalNumber("sin(PI/2)"), 1e-12);
        assertEquals(1.0, engine.evalNumber("Math.sin(Math.PI/2)"), 1e-12);
        assertEquals(8.0, engine.evalNumber("pow(2,3)"), 1e-12);
    }

    @Test
    void numbers_lexing_edge_cases() {
        ExpressionEngine engine = new ExpressionEngine();
        assertEquals(0.5, engine.evalNumber(".5"), 1e-12);
        assertEquals(123.0, engine.evalNumber(".123e3"), 1e-9);
        assertEquals(1.23e3, engine.evalNumber("1.23e3"), 1e-12);
    }

    @Test
    void division_by_zero_and_nan() {
        ExpressionEngine engine = new ExpressionEngine();
        assertEquals(Double.POSITIVE_INFINITY, engine.evalNumber("1 / 0.0"), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, engine.evalNumber("-1 / 0.0"), 0.0);
        // NaN truthiness
        assertTrue(engine.evalBoolean("!(0/0)"));
        assertFalse(engine.evalBoolean("(0/0) == (0/0)")); // NaN == NaN -> false
    }

    @Test
    void simple_math_and_logical_operation() {
        ExpressionEngine engine = new ExpressionEngine();
        var n = engine.evalNumber("(1 + 2 + 3 + 4 + 5 + 6 + 7) * 100 / 8");
        assertEquals(350.0, n, 1e-12);
        // 175 >= 174
        assertTrue(engine.evalBoolean("(1 + 2 + 3 + 4 + 5 + 6 - 7) * 100 / 8 >= 150 + 24"));
        // 350 > 345
        assertTrue(engine.evalBoolean("(1 + 2 + 3 + 4 + 5 + 6 + 7) * 100 / 8 > 150 * 2.3"));
        // 350 <= 360
        assertTrue(engine.evalBoolean("(1 + 2 + 3 + 4 + 5 + 6 + 7) * 100 / 8 < 150 * 2.4"));
    }

    @Test
    void error_cases_unknowns_and_misuse() {
        ExpressionEngine engine = new ExpressionEngine();

        // unknown identifier
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> engine.evalNumber("foo + 1"));
        assertTrue(e1.getMessage().toLowerCase().contains("unknown identifier"));

        // calling a non-function
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> engine.evalNumber("PI(2)"));
        assertTrue(e2.getMessage().toLowerCase().contains("non-function"));

        // type mismatch in equality
        assertThrows(IllegalArgumentException.class, () -> engine.evalBoolean("'1' == 1"));
    }
}