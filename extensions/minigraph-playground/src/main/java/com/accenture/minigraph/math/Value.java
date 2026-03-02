package com.accenture.minigraph.math;

/**
 * A simple tagged union for numbers and booleans.
 * - Numbers are doubles.
 * - Booleans are true/false.
 * <p>
 * Conversions:
 * - asDouble(): booleans map to 1.0 (true) / 0.0 (false).
 * - asBoolean(): numbers use JS-like truthiness: 0 and NaN are false; others true.
 */
public final class Value {
    private final Double number;
    private final Boolean bool;

    private Value(Double number, Boolean bool) {
        this.number = number;
        this.bool = bool;
    }

    public static Value number(double d) {
        return new Value(d, null);
    }

    public static Value bool(boolean b) {
        return new Value(null, b);
    }

    public boolean isNumber() { return number != null; }
    public boolean isBoolean() { return bool != null; }

    public double asDouble() {
        if (number != null) return number;
        // boolean → number
        return bool ? 1.0 : 0.0;
    }

    public boolean asBoolean() {
        if (bool != null) return bool;
        // JS-like truthiness: 0 and NaN are falsy.
        return number != 0.0 && !Double.isNaN(number);
    }

    @Override
    public String toString() {
        return isNumber() ? ("Number(" + number + ")") : ("Boolean(" + bool + ")");
    }
}
