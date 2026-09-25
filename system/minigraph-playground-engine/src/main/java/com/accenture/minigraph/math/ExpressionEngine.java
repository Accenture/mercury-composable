package com.accenture.minigraph.math;

/** Convenience facade for parsing/evaluating expressions. */
public final class ExpressionEngine {

    private final EvalContext ctx;

    public ExpressionEngine() { this(EvalContext.withDefaults()); }
    public ExpressionEngine(EvalContext ctx) { this.ctx = ctx; }

    /** Evaluate to a number; a boolean result is rejected (a boolean is not a number in this dialect). */
    public double evalNumber(String expr) {
        return Evaluator.evaluateNumber(expr, ctx);
    }

    /** Evaluate to a boolean; numbers are coerced with JS-like truthiness if produced. */
    public boolean evalBoolean(String expr) {
        return Evaluator.evaluateBoolean(expr, ctx);
    }

    public Value evaluateValue(String expr) {
        return Evaluator.evaluateValue(expr, ctx);
    }

    public EvalContext context() { return ctx; }
}