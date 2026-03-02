package com.accenture.minigraph.math;

import java.util.ArrayList;
import java.util.List;

public final class Evaluator {

    public static double evaluateNumber(String expression, EvalContext ctx) {
        Value v = evaluateValue(expression, ctx);
        return v.asDouble();
    }

    public static boolean evaluateBoolean(String expression, EvalContext ctx) {
        Value v = evaluateValue(expression, ctx);
        return v.asBoolean();
    }

    private static Value evaluateValue(String expression, EvalContext ctx) {
        Parser parser = new Parser(expression); // strict JS '**' rule
        Expr ast = parser.parse();
        return eval(ast, ctx);
    }

    private static Value eval(Expr e, EvalContext ctx) {
        if (e instanceof Expr.NumberLiteral en) {
            return Value.number(en.value);
        }
        if (e instanceof Expr.BooleanLiteral eb) {
            return Value.bool(eb.value);
        }
        if (e instanceof Expr.Variable ev) {
            return evalVar(ev, ctx);
        }
        if (e instanceof Expr.Unary u) {
            return evalUnary(u, ctx);
        }
        if (e instanceof Expr.Binary b) {
            return evalBinary(b, ctx);
        }
        if (e instanceof Expr.MemberAccess m) {
            return evalMemberAccess(m, ctx);
        }
        if (e instanceof Expr.Call c) {
            return evalCall(c, ctx);
        }
        if (e instanceof Expr.Conditional q) {
            boolean test = eval(q.test, ctx).asBoolean();
            return test ? eval(q.consequent, ctx) : eval(q.alternate, ctx);
        }
        throw new IllegalStateException("Unknown expression node: " + e.getClass());
    }

    private static Value evalVar(Expr.Variable ev, EvalContext ctx) throws IllegalArgumentException {
        String name = ev.name;
        Object v = ctx.lookup(name);
        return switch (v) {
            case null -> throw new IllegalArgumentException("Unknown identifier: " + name);
            case Double vd -> Value.number(vd);
            case MathFunction ignored ->
                    throw new IllegalArgumentException("Identifier is a function, not a value: " + name);
            default -> throw new IllegalArgumentException("Unsupported variable type for: " + name);
        };
    }

    private static Value evalUnary(Expr.Unary u, EvalContext ctx) {
        Value r = eval(u.right, ctx);
        return switch (u.op) {
            case "+" -> Value.number(+r.asDouble());
            case "-" -> Value.number(-r.asDouble());
            case "!" -> Value.bool(!r.asBoolean());
            default -> throw new IllegalArgumentException("Unsupported unary operator: " + u.op);
        };
    }

    private static Value evalCall(Expr.Call c, EvalContext ctx) {
        Object fnObj;
        if (c.callee instanceof Expr.MemberAccess) {
            fnObj = resolveObject(c.callee, ctx);
        } else if (c.callee instanceof Expr.Variable cc) {
            String name = cc.name;
            fnObj = ctx.lookup(name);
        } else {
            // Calling a computed non-function (e.g., (1+2)(3))
            fnObj = null;
        }
        if (!(fnObj instanceof MathFunction fn)) {
            throw new IllegalArgumentException("Attempting to call a non-function");
        }

        List<Double> argVals = new ArrayList<>(c.args.size());
        for (Expr arg : c.args) {
            argVals.add(eval(arg, ctx).asDouble());
        }
        double[] arr = new double[argVals.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = argVals.get(i);
        return Value.number(fn.apply(arr));
    }

    private static Value evalBinary(Expr.Binary b, EvalContext ctx) throws IllegalArgumentException {
        if ("&&".equals(b.op)) {
            Value l = eval(b.left, ctx);
            if (!l.asBoolean()) return Value.bool(false);
            Value r = eval(b.right, ctx);
            return Value.bool(r.asBoolean());
        }
        if ("||".equals(b.op)) {
            Value l = eval(b.left, ctx);
            if (l.asBoolean()) return Value.bool(true);
            Value r = eval(b.right, ctx);
            return Value.bool(r.asBoolean());
        }
        Value lv = eval(b.left, ctx);
        Value rv = eval(b.right, ctx);
        double l = lv.asDouble();
        double r = rv.asDouble();
        return switch (b.op) {
            case "+" -> Value.number(l + r);
            case "-" -> Value.number(l - r);
            case "*" -> Value.number(l * r);
            case "/" -> Value.number(l / r);
            case "%" -> Value.number(l % r);
            case "**" -> Value.number(Math.pow(l, r));
            case "<" -> Value.bool(l < r);
            case "<=" -> Value.bool(l <= r);
            case ">" -> Value.bool(l > r);
            case ">=" -> Value.bool(l >= r);
            case "==" -> {
                if (lv.isBoolean() && rv.isBoolean()) yield Value.bool(lv.asBoolean() == rv.asBoolean());
                yield Value.bool(l == r);
            }
            case "!=" -> {
                if (lv.isBoolean() && rv.isBoolean()) yield Value.bool(lv.asBoolean() != rv.asBoolean());
                yield Value.bool(l != r);
            }
            default -> throw new IllegalArgumentException("Unsupported binary operator: " + b.op);
        };
    }

    private static Value evalMemberAccess(Expr.MemberAccess m, EvalContext ctx) {
        Object obj = resolveObject(m, ctx);
        if (obj instanceof Double d) return Value.number(d);
        if (obj instanceof MathFunction) throw new IllegalArgumentException("Member is a function; call it with '()'.");
        if (obj == null) throw new IllegalArgumentException("Unknown member access");
        throw new IllegalArgumentException("Unsupported member type in member access");
    }

    /**
     * Resolve an expression that denotes an object in the context (namespace, member, function, or constant).
     * Supports chained member access: e.g., "Math.max", MyNs.subNs.value
     */
    private static Object resolveObject(Expr e, EvalContext ctx) {
        if (e instanceof Expr.Variable ev) {
            String name = ev.name;
            return ctx.lookup(name); // may be Map (namespace), Double, or MathFunction
        }
        if (e instanceof Expr.MemberAccess ma) {
            Object target = resolveObject(ma.target, ctx);
            return ctx.member(target, ma.property); // null if target not a namespace or unknown property
        }
        // Not a resolvable object expression
        return null;
    }
}
