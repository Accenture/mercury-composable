package com.accenture.minigraph.math;

import java.math.BigDecimal;
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
        Parser parser = new Parser(expression, true /* strict JS '**' rule */);
        Expr ast = parser.parse();
        return eval(ast, ctx);
    }

    private static Value eval(Expr e, EvalContext ctx) {
        return switch (e) {
            case Expr.NumberLiteral(double en)    -> Value.number(en);
            case Expr.StringLiteral(String es)    -> Value.str(es);
            case Expr.BooleanLiteral(boolean eb)  -> Value.bool(eb);
            case Expr.Variable ev        -> evalVar(ev, ctx);
            case Expr.Unary u            -> evalUnary(u, ctx);
            case Expr.Binary b           -> evalBinary(b, ctx);
            case Expr.MemberAccess m     -> evalMemberAccess(m, ctx);
            case Expr.Call c             -> evalCall(c, ctx);
            case Expr.Conditional(Expr test, Expr consequent, Expr alternate)
                    -> eval(test, ctx).asBoolean() ? eval(consequent, ctx) : eval(alternate, ctx);
        };
    }

    private static Value evalVar(Expr.Variable ev, EvalContext ctx) {
        String name = ev.name();
        Object v = ctx.lookup(name);
        return switch (v) {
            case null -> throw new IllegalArgumentException("Unknown identifier: " + name);
            case Double vd -> Value.number(vd);
            case String vs -> Value.str(vs);
            case MathFunction ignored -> throw new IllegalArgumentException("Identifier is a function, not a value: " + name);
            default -> throw new IllegalArgumentException("Unsupported variable type for: " + name);
        };
    }

    private static Value evalUnary(Expr.Unary u, EvalContext ctx) {
        Value r = eval(u.right(), ctx);
        return switch (u.op()) {
            case "+" -> Value.number(+asNumber(r, "unary '+'"));
            case "-" -> Value.number(-asNumber(r, "unary '-'"));
            case "!" -> Value.bool(!r.asBoolean());
            default  -> throw new IllegalArgumentException("Unsupported unary operator: " + u.op());
        };
    }

    private static Value evalCall(Expr.Call c, EvalContext ctx) {
        Object fnObj;
        if (c.callee() instanceof Expr.MemberAccess || c.callee() instanceof Expr.Variable) {
            fnObj = resolveObject(c.callee(), ctx);
        } else {
            fnObj = null;
        }
        if (!(fnObj instanceof MathFunction fn)) {
            throw new IllegalArgumentException("Attempting to call a non-function");
        }
        List<Double> argVals = new ArrayList<>(c.args().size());
        for (Expr arg : c.args()) {
            argVals.add(eval(arg, ctx).asDouble());
        }
        double[] arr = new double[argVals.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = argVals.get(i);
        return Value.number(fn.apply(arr));
    }

    private static Value evalBinary(Expr.Binary b, EvalContext ctx) {
        // Short-circuit boolean ops
        if ("&&".equals(b.op())) {
            Value l = eval(b.left(), ctx);
            if (!l.asBoolean()) return Value.bool(false);
            return Value.bool(eval(b.right(), ctx).asBoolean());
        }
        if ("||".equals(b.op())) {
            Value l = eval(b.left(), ctx);
            if (l.asBoolean()) return Value.bool(true);
            return Value.bool(eval(b.right(), ctx).asBoolean());
        }

        // Evaluate operands once for non-short-circuit ops
        Value lv = eval(b.left(), ctx);
        Value rv = eval(b.right(), ctx);

        // String concatenation if either side is string
        if ("+".equals(b.op()) && (lv instanceof StringValue || rv instanceof StringValue)) {
            return Value.str(asString(lv) + asString(rv));
        }

        // Numeric arithmetic
        switch (b.op()) {
            case "+"  : return Value.number(asNumber(lv, "+") + asNumber(rv, "+"));
            case "-"  : return Value.number(asNumber(lv, "-") - asNumber(rv, "-"));
            case "*"  : return Value.number(asNumber(lv, "*") * asNumber(rv, "*"));
            case "/"  : return Value.number(asNumber(lv, "/") / asNumber(rv, "/"));
            case "%"  : return Value.number(asNumber(lv, "%") % asNumber(rv, "%"));
            case "**" : return Value.number(Math.pow(asNumber(lv, "**"), asNumber(rv, "**")));
            default: /* next */
        }

        // Relational / equality with string and number support
        switch (b.op()) {
            case "<", "<=", ">", ">=" -> {
                if (lv instanceof StringValue(String ls) && rv instanceof StringValue(String rs)) {
                    int cmp = ls.compareTo(rs);
                    return switch (b.op()) {
                        case "<"  -> Value.bool(cmp <  0);
                        case "<=" -> Value.bool(cmp <= 0);
                        case ">"  -> Value.bool(cmp >  0);
                        case ">=" -> Value.bool(cmp >= 0);
                        default   -> throw new IllegalStateException();
                    };
                } else {
                    double l = asNumber(lv, b.op());
                    double r = asNumber(rv, b.op());
                    return switch (b.op()) {
                        case "<"  -> Value.bool(l <  r);
                        case "<=" -> Value.bool(l <= r);
                        case ">"  -> Value.bool(l >  r);
                        case ">=" -> Value.bool(l >= r);
                        default   -> throw new IllegalStateException();
                    };
                }
            }
            case "==", "!=" -> {
                return evalBinaryEquality(lv, rv, b);
            }
            default -> throw new IllegalArgumentException("Unsupported binary operator: " + b.op());
        }
    }

    private static Value evalBinaryEquality(Value lv, Value rv, Expr.Binary b) {
        boolean eq;
        switch (lv) {
            case StringValue ls when rv instanceof StringValue(String rs) -> eq = ls.value().equals(rs);
            case BooleanValue lb when rv instanceof BooleanValue(boolean rb) -> eq = lb.value() == rb;
            case NumberValue ln when rv instanceof NumberValue(double rn) -> eq = ln.value() == rn; // NaN == NaN -> false
            default -> throw new IllegalArgumentException("Type mismatch for equality: " + lv + " " + b.op() + " " + rv);
        }
        return Value.bool("==".equals(b.op()) == eq);
    }

    private static Value evalMemberAccess(Expr.MemberAccess m, EvalContext ctx) {
        Object obj = resolveObject(m, ctx);
        if (obj instanceof Double d) return Value.number(d);
        if (obj instanceof MathFunction) throw new IllegalArgumentException("Member is a function; call it with '()'.");
        if (obj == null) throw new IllegalArgumentException("Unknown member access");
        throw new IllegalArgumentException("Unsupported member type in member access");
    }

    private static Object resolveObject(Expr e, EvalContext ctx) {
        if (e instanceof Expr.Variable(String name)) {
            return ctx.lookup(name);
        }
        if (e instanceof Expr.MemberAccess(Expr target1, String property)) {
            Object target = resolveObject(target1, ctx);
            return ctx.member(target, property);
        }
        return null;
    }

    private static double asNumber(Value v, String context) {
        if (v instanceof NumberValue(double nv))  return nv;
        if (v instanceof BooleanValue) return v.asDouble(); // allow bool→number for arithmetic
        throw new IllegalArgumentException("Expected number in " + context + ", got " + v);
    }

    private static String asString(Value v) {
        if (v instanceof StringValue(String sv))  return sv;
        if (v instanceof NumberValue(double nv))  return numberToString(nv);
        if (v instanceof BooleanValue(boolean bv)) return Boolean.toString(bv);
        return v.toString();
    }

    private static String numberToString(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) return Double.toString(d);
        // Render like JS: integral doubles as integer text, otherwise minimal decimal text.
        return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
    }
}
