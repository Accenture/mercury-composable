package com.accenture.minigraph.math;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class EvalContext {
    private final Map<String, Object> root = new HashMap<>();

    public static EvalContext withDefaults() {
        EvalContext ctx = new EvalContext();

        // Math namespace (populated as we define)
        ctx.root.put("Math", new HashMap<>());

        // Constants
        ctx.define("PI", Math.PI);
        ctx.define("E",  Math.E);

        // Functions (top-level mirrored into Math.*)
        ctx.define("sin",  a -> Math.sin(req1(a)));
        ctx.define("cos",  a -> Math.cos(req1(a)));
        ctx.define("tan",  a -> Math.tan(req1(a)));
        ctx.define("asin", a -> Math.asin(req1(a)));
        ctx.define("acos", a -> Math.acos(req1(a)));
        ctx.define("atan", a -> Math.atan(req1(a)));
        ctx.define("sqrt", a -> Math.sqrt(req1(a)));
        ctx.define("abs",  a -> Math.abs(req1(a)));
        ctx.define("floor",a -> Math.floor(req1(a)));
        ctx.define("ceil", a -> Math.ceil(req1(a)));
        ctx.define("round",a -> (double)Math.round(req1(a)));
        ctx.define("log",  a -> Math.log(req1(a)));
        ctx.define("log10",a -> Math.log10(req1(a)));
        ctx.define("exp",  a -> Math.exp(req1(a)));
        ctx.define("min",  EvalContext::min);
        ctx.define("max",  EvalContext::max);
        ctx.define("pow",  a -> Math.pow(req2(a, "pow"), a[1]));
        ctx.define("random", a -> { reqN(a, 0, "random"); return Math.random(); });

        return ctx;
    }

    public EvalContext define(String name, double value) {
        root.put(name, value);
        mirrorIntoMath(name, value);
        return this;
    }

    public EvalContext define(String name, MathFunction fn) {
        root.put(name, fn);
        mirrorIntoMath(name, fn);
        return this;
    }

    /** Numbers only; for strings use defineString. */
    public EvalContext defineVariable(String name, double value) {
        root.put(name, value);
        return this;
    }

    /** Define string variable. */
    public EvalContext defineString(String name, String value) {
        root.put(name, value);
        return this;
    }

    public Object lookup(String name) {
        return root.get(name);
    }

    @SuppressWarnings("unchecked")
    public Object member(Object target, String property) {
        if (!(target instanceof Map)) return null;
        return ((Map<String, Object>) target).get(property);
    }

    public Map<String, Object> snapshot() {
        return Collections.unmodifiableMap(root);
    }

    private void mirrorIntoMath(String name, Object v) {
        Object math = root.get("Math");
        if (math instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) math;
            m.put(name, v);
        }
    }

    // Helpers
    private static double req1(double[] a) {
        if (a.length != 1) throw new IllegalArgumentException("Expected 1 argument");
        return a[0];
    }
    private static double req2(double[] a, String fn) {
        reqN(a, 2, fn);
        return a[0];
    }
    private static void reqN(double[] a, int n, String fn) {
        if (a.length != n) throw new IllegalArgumentException("Function " + fn + " expects " + n + " args, got " + a.length);
    }
    private static double min(double... a) {
        if (a.length == 0) return Double.POSITIVE_INFINITY;
        double m = a[0];
        for (int i = 1; i < a.length; i++) m = Math.min(m, a[i]);
        return m;
    }
    private static double max(double... a) {
        if (a.length == 0) return Double.NEGATIVE_INFINITY;
        double m = a[0];
        for (int i = 1; i < a.length; i++) m = Math.max(m, a[i]);
        return m;
    }
}
