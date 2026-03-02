package com.accenture.minigraph;

import com.accenture.minigraph.math.EvalContext;
import com.accenture.minigraph.math.Evaluator;
import com.accenture.minigraph.math.ExpressionEngine;
import com.accenture.minigraph.math.ParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MathAndBooleanTest {

    @Test
    void boolean_literals_basic() {
        ExpressionEngine engine = new ExpressionEngine();
        assertTrue(engine.evalBoolean("true"));
        assertFalse(engine.evalBoolean("false"));
        assertTrue(engine.evalBoolean("!false")); // requested case
        assertFalse(engine.evalBoolean("!true"));
        assertTrue(engine.evalBoolean("!!true"));
        assertFalse(engine.evalBoolean("!!false"));
    }

    @Test
    void booleans_in_numeric_contexts() {
        ExpressionEngine engine = new ExpressionEngine();
        assertEquals(2.0, engine.evalNumber("true + 1"), 0.0);
        assertEquals(0.0, engine.evalNumber("false * 5"), 0.0);
        assertEquals(1.0, engine.evalNumber("true ? 1 : 0"), 0.0);
        assertEquals(0.0, engine.evalNumber("false ? 1 : 0"), 0.0);
    }

    @Test
    void logicals_with_booleans() {
        ExpressionEngine engine = new ExpressionEngine();
        assertTrue(engine.evalBoolean("true && true"));
        assertFalse(engine.evalBoolean("true && false"));
        assertTrue(engine.evalBoolean("false || true"));
        assertFalse(engine.evalBoolean("false || false"));
    }

    @Test
    void ternary_with_boolean_branches() {
        ExpressionEngine engine = new ExpressionEngine();
        assertTrue(engine.evalBoolean("(2 < 3) ? true : false"));
        assertFalse(engine.evalBoolean("(2 > 3) ? true : false"));
        assertTrue(engine.evalBoolean("true ? true : false"));
        assertFalse(engine.evalBoolean("false ? true : false"));
    }

    @Test
    void math_namespace_and_top_level_still_work() {
        ExpressionEngine engine = new ExpressionEngine();
        assertEquals(Math.PI, engine.evalNumber("Math.PI"), 1e-12);
        assertEquals(1.0, engine.evalNumber("sin(PI/2)"), 1e-12);
        assertEquals(1.0, engine.evalNumber("Math.sin(Math.PI/2)"), 1e-12);
    }


    @Test
    void arithmetic_precedence_and_associativity() {
        ExpressionEngine engine = new ExpressionEngine();

        // Multiplicative binds tighter than additive
        assertEquals(14.0, engine.evalNumber("2 + 3 * 4"), 1e-12);
        assertEquals(20.0, engine.evalNumber("(2 + 3) * 4"), 1e-12);

        // Exponentiation is right-associative
        assertEquals(Math.pow(2, Math.pow(3, 2)), engine.evalNumber("2 ** 3 ** 2"), 1e-12);

        // Mixed ops with parentheses
        assertEquals(0.56, engine.evalNumber("2 * (3 + 4) / 5 ** 2"), 1e-12);

        // Modulo
        assertEquals(1.0, engine.evalNumber("10 % 3"), 1e-12);
    }

    @Test
    void unary_plus_minus_and_parentheses() {
        ExpressionEngine engine = new ExpressionEngine();

        assertEquals(-5.0, engine.evalNumber("-(2 + 3)"), 1e-12);
        assertEquals(+5.0, engine.evalNumber("+(2 + 3)"), 1e-12);
        assertEquals(-2.0, engine.evalNumber("-2"), 1e-12);

        // JS strict rule: unary cannot be left of ** (should throw ParseException)
        assertThrows(ParseException.class, () -> engine.evalNumber("-2 ** 2"));

        // Parenthesized works
        assertEquals(-4.0, engine.evalNumber("-(2 ** 2)"), 1e-12);
    }

    @Test
    void variables_and_context_values() {
        EvalContext ctx = EvalContext.withDefaults()
                .defineVariable("x", 3.0)
                .defineVariable("rate", 0.15);

        assertEquals(10.5, Evaluator.evaluateNumber("x ** 2 + 10 * rate", ctx), 1e-12);

        // Override or add a custom constant
        ctx.define("K", 2.5);
        assertEquals(2.5 + 9.0, Evaluator.evaluateNumber("K + x*x", ctx), 1e-12);
    }

    @Test
    void built_in_functions_and_namespace() {
        ExpressionEngine engine = new ExpressionEngine();

        // Top-level functions
        assertEquals(2.0, engine.evalNumber("sin(PI/2) + 1"), 1e-12);
        assertEquals(7.0, engine.evalNumber("max(1, 9, 3, 7) - min(5, 2)"), 1e-12);
        assertEquals(8.0, engine.evalNumber("pow(2, 3)"), 1e-12);
        assertEquals(Math.log10(1000), engine.evalNumber("log10(1000)"), 1e-12);

        // Namespace Math.*
        assertEquals(2.0, engine.evalNumber("Math.sin(Math.PI / 2) + 1"), 1e-12);
        assertEquals(Math.E, engine.evalNumber("Math.E"), 1e-12);
        assertEquals(3.0, engine.evalNumber("Math.max(1, 2, 3)"), 1e-12);
    }

    @Test
    void random_is_in_range() {
        ExpressionEngine engine = new ExpressionEngine();
        // random() returns [0, 1) — we’ll just verify range and that calls are independent
        for (int i = 0; i < 10; i++) {
            double v = engine.evalNumber("random()");
            assertTrue(v >= 0.0 && v < 1.0, "random() not in [0,1): " + v);
        }
    }

    @Test
    void scientific_notation_and_decimals() {
        ExpressionEngine engine = new ExpressionEngine();

        assertEquals(1.23e3, engine.evalNumber("1.23e3"), 1e-12);
        assertEquals(0.001, engine.evalNumber("1e-3"), 1e-15);
        assertEquals(3.14, engine.evalNumber("3.14"), 1e-12);
        assertEquals(0.5, engine.evalNumber(".5"), 1e-12); // Lexer supports leading dot with digits
    }

    @Test
    void division_by_zero_behaves_like_java_double() {
        ExpressionEngine engine = new ExpressionEngine();

        assertEquals(Double.POSITIVE_INFINITY, engine.evalNumber("1 / 0.0"), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, engine.evalNumber("-1 / 0.0"), 0.0);

        double nan = engine.evalNumber("0 / 0.0");
        assertTrue(Double.isNaN(nan));
    }

    @Test
    void member_access_errors_and_unknowns() {
        ExpressionEngine engine = new ExpressionEngine();

        // Unknown identifier
        RuntimeException ex1 = assertThrows(RuntimeException.class, () -> engine.evalNumber("foo + 1"));
        assertTrue(ex1.getMessage().toLowerCase().contains("unknown identifier"));

        // Unknown member
        RuntimeException ex2 = assertThrows(RuntimeException.class, () -> engine.evalNumber("Math.nope(1)"));
        assertTrue(ex2.getMessage().toLowerCase().contains("non-function"));

        // Attempting to call a non-function
        RuntimeException ex3 = assertThrows(RuntimeException.class, () -> engine.evalNumber("PI(2)"));
        assertTrue(ex3.getMessage().toLowerCase().contains("non-function"));
    }

}