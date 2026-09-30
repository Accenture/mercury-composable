package com.accenture.minigraph.math;

import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;

/** Sealed union for runtime values: number, decimal (DECIMAL statements only), boolean, string. */
public sealed interface Value permits NumberValue, DecimalValue, BooleanValue, StringValue {
    double asDouble();
    boolean asBoolean();
    String asString();

    static Value number(double d)   { return new NumberValue(d); }
    static Value decimal(BigDecimal d) { return new DecimalValue(d); }
    static Value bool(boolean b)    { return new BooleanValue(b); }
    static Value str(String s)      { return new StringValue(s); }
}

record NumberValue(double value) implements Value {

    @Override
    public double asDouble() {
        return value;
    }

    @Override
    public boolean asBoolean() {
        return value != 0.0 && !Double.isNaN(value);
    }

    @Override
    public String asString() {
        return String.valueOf(value);
    }

    @NonNull
    @Override
    public String toString() {
        return "Number(" + value + ")";
    }
}

record BooleanValue(boolean value) implements Value {

    @Override
    public double asDouble() {
        return value ? 1.0 : 0.0;
    }

    @Override
    public boolean asBoolean() {
        return value;
    }

    @Override
    public String asString() {
        return String.valueOf(value);
    }

    @NonNull
    @Override
    public String toString() {
        return "Boolean(" + value + ")";
    }
}

record StringValue(String value) implements Value {

    @Override
    public double asDouble() {
        // No implicit numeric coercion for strings in arithmetic contexts.
        throw new IllegalArgumentException("Cannot coerce string to number: \"" + value + "\"");
    }

    @Override
    public boolean asBoolean() {
        return value != null && !value.isEmpty();
    } // empty string is falsy

    @Override
    public String asString() {
        return String.valueOf(value);   // null value is returned as "null"
    }

    @NonNull
    @Override
    public String toString() {
        return "String(" + value + ")";
    }
}

/** An exact decimal - only a DECIMAL statement produces one. */
record DecimalValue(BigDecimal value) implements Value {

    @Override
    public double asDouble() {
        return value.doubleValue();
    }

    @Override
    public boolean asBoolean() {
        return value.signum() != 0;
    }

    @Override
    public String asString() {
        return DecimalEvaluator.canonical(value);
    }

    @NonNull
    @Override
    public String toString() {
        return "Decimal(" + DecimalEvaluator.canonical(value) + ")";
    }
}
