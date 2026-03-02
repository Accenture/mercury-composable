package com.accenture.minigraph.math;

/**
 * Convenience facade for parsing/evaluating expressions.
 */
public final class ExpressionEngine {

    private final EvalContext ctx;

    public ExpressionEngine() { this(EvalContext.withDefaults()); }
    public ExpressionEngine(EvalContext ctx) { this.ctx = ctx; }

    /** Evaluate to a number; booleans are coerced to 0/1 if produced. */
    public double evalNumber(String expr) {
        return Evaluator.evaluateNumber(expr, ctx);
    }

    /** Evaluate to a boolean; numbers are coerced with JS-like truthiness if produced. */
    public boolean evalBoolean(String expr) {
        return Evaluator.evaluateBoolean(expr, ctx);
    }

    public EvalContext context() { return ctx; }
}
