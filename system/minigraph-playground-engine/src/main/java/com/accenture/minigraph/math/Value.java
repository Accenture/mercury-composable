package com.accenture.minigraph.math;

import org.jspecify.annotations.NonNull;

/** Sealed union for runtime values: number, boolean, string. */
public sealed interface Value permits NumberValue, BooleanValue, StringValue {
    double asDouble();
    boolean asBoolean();

    static Value number(double d)   { return new NumberValue(d); }
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

    @NonNull
    @Override
    public String toString() {
        return "String(" + value + ")";
    }
}
