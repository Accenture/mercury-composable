package com.accenture.minigraph.math;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Exact decimal arithmetic for the DECIMAL statement - the high-precision COMPUTE (RFC-0001).
 * <p>
 * A parallel evaluator: the double path in {@link Evaluator} is untouched. Number literals, substituted
 * numbers included, parse from their text straight into a {@link BigDecimal}, never through a double.
 * <ul>
 * <li>{@code + - *} are exact, with the result scale BigDecimal gives them (the larger operand scale for
 * {@code + -}, the sum of scales for {@code *}); a scale is never negative.</li>
 * <li>{@code /} never truncates: the exact quotient when it terminates, otherwise 34 significant digits
 * (decimal128) rounded HALF_EVEN; division by zero is an error naming the operator.</li>
 * <li>{@code **} and {@code pow} take a whole-number exponent within a bound; a fractional one is refused.</li>
 * <li>Rounding is always explicit: {@code round(x, scale, mode)}.</li>
 * <li>What cannot be exact - sqrt, log, exp, trigonometry, random() and the constants PI and E - is refused
 * by name at run time.</li>
 * </ul>
 * A result is a canonical string: plain notation, never scientific, the computed scale kept, and a zero of
 * any scale written "0".
 */
public final class DecimalEvaluator {
    // the exponent of ** and pow() is bounded so an expression cannot exhaust memory
    static final int MAX_EXPONENT = 999;
    private static final int MAX_SCALE = 1000;
    private static final int MAX_LITERAL_SCALE = 1000;
    private static final int MAX_LITERAL_PRECISION = 1000;
    private static final int MAX_RESULT_PRECISION = 10_000;
    private static final MathContext DIVISION = MathContext.DECIMAL128;
    private static final String MATH = "Math.";
    private static final Set<String> REFUSED_FUNCTIONS = Set.of(
            "sin", "cos", "tan", "asin", "acos", "atan", "sqrt", "log", "log10", "exp", "random");
    private static final Set<String> REFUSED_CONSTANTS = Set.of("PI", "E");
    private static final Set<String> FUNCTIONS = Set.of("abs", "floor", "ceil", "min", "max", "pow", "round");
    private static final String MODES = "HALF_UP, HALF_EVEN, HALF_DOWN, UP, DOWN, CEILING or FLOOR";

    private DecimalEvaluator() {
        // utility class
    }

    /**
     * Evaluate a DECIMAL statement's expression.
     *
     * @param expression the statement text after its variables were rendered into it
     * @return the result as a canonical decimal string
     */
    public static String evaluate(String expression) {
        return canonical(evaluateDecimal(expression));
    }

    /**
     * Evaluate a DECIMAL statement's expression.
     *
     * @param expression the statement text after its variables were rendered into it
     * @return the exact result, its scale never negative
     */
    public static BigDecimal evaluateDecimal(String expression) {
        var ast = new Parser(expression, true, true).parse();
        var value = eval(ast);
        return switch (value) {
            case DecimalValue(BigDecimal d) -> d;
            case StringValue(String s) -> {
                var d = NumericStrings.canonical(s);
                if (d == null) {
                    throw new IllegalArgumentException("Expected a number as the result of a DECIMAL statement, got " + value);
                }
                yield d;
            }
            case BooleanValue ignored ->
                    throw new IllegalArgumentException("Boolean result where a number was expected: " + value);
            case NumberValue ignored -> throw new IllegalStateException("A double in a DECIMAL statement");
        };
    }

    /**
     * The canonical string of a decimal: plain notation, never scientific, the computed scale kept, and a
     * zero of any scale written "0" (which is what the platform's JSON serializer writes for a zero).
     *
     * @param value a decimal
     * @return its canonical string
     */
    public static String canonical(BigDecimal value) {
        return value.signum() == 0? "0" : normalize(value).toPlainString();
    }

    /**
     * A number literal: bounded, and a negative scale (1e3) becomes scale 0 (1000).
     *
     * @param value the parsed literal
     * @return the literal as the evaluator uses it
     */
    static BigDecimal literal(BigDecimal value) {
        if (Math.abs(value.scale()) > MAX_LITERAL_SCALE || value.precision() > MAX_LITERAL_PRECISION) {
            throw new ArithmeticException("Number literal out of range");
        }
        return normalize(value);
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value.scale() < 0? value.setScale(0) : value;
    }

    private static BigDecimal checked(BigDecimal value, String context) {
        if (value.precision() > MAX_RESULT_PRECISION) {
            throw new IllegalArgumentException("Arithmetic result too large in '" + context + "'");
        }
        return normalize(value);
    }

    private static Value eval(Expr e) {
        return switch (e) {
            case Expr.DecimalLiteral(BigDecimal d) -> Value.decimal(d);
            case Expr.NumberLiteral ignored -> throw new IllegalStateException("A double literal in a DECIMAL statement");
            case Expr.StringLiteral(String s) -> Value.str(s);
            case Expr.BooleanLiteral(boolean b) -> Value.bool(b);
            case Expr.Variable v -> variable(v.name());
            case Expr.Unary u -> unary(u);
            case Expr.Binary b -> binary(b);
            case Expr.MemberAccess m -> member(m);
            case Expr.Call c -> call(c);
            case Expr.Conditional(Expr test, Expr consequent, Expr alternate) ->
                    eval(eval(test).asBoolean()? consequent : alternate);
        };
    }

    private static Value variable(String name) {
        if (REFUSED_CONSTANTS.contains(name)) {
            throw refusedConstant(name);
        }
        if (FUNCTIONS.contains(name) || REFUSED_FUNCTIONS.contains(name)) {
            throw new IllegalArgumentException("Identifier is a function, not a value: " + name);
        }
        throw new IllegalArgumentException("Unknown identifier: " + name);
    }

    private static Value member(Expr.MemberAccess m) {
        var name = calleeName(m);
        var bare = name.startsWith(MATH)? name.substring(MATH.length()) : name;
        if (REFUSED_CONSTANTS.contains(bare)) {
            throw refusedConstant(name);
        }
        if (FUNCTIONS.contains(bare) || REFUSED_FUNCTIONS.contains(bare)) {
            throw new IllegalArgumentException("Member is a function; call it with '()'.");
        }
        throw new IllegalArgumentException("Unknown member access");
    }

    private static IllegalArgumentException refusedConstant(String name) {
        return new IllegalArgumentException("'" + name + "' is not available in a DECIMAL statement: " +
                "it is not an exact decimal; use COMPUTE or a graph.task function");
    }

    private static Value unary(Expr.Unary u) {
        var operand = eval(u.right());
        return switch (u.op()) {
            case "+" -> Value.decimal(asDecimal(operand, "unary '+'"));
            case "-" -> Value.decimal(asDecimal(operand, "unary '-'").negate());
            case "!" -> Value.bool(!operand.asBoolean());
            default  -> throw new IllegalArgumentException("Unsupported unary operator: " + u.op());
        };
    }

    private static Value binary(Expr.Binary b) {
        var op = b.op();
        if ("&&".equals(op)) {
            return Value.bool(eval(b.left()).asBoolean() && eval(b.right()).asBoolean());
        }
        if ("||".equals(op)) {
            return Value.bool(eval(b.left()).asBoolean() || eval(b.right()).asBoolean());
        }
        var left = eval(b.left());
        var right = eval(b.right());
        return switch (op) {
            case "+"  -> Value.decimal(checked(asDecimal(left, op).add(asDecimal(right, op)), op));
            case "-"  -> Value.decimal(checked(asDecimal(left, op).subtract(asDecimal(right, op)), op));
            case "*"  -> Value.decimal(checked(asDecimal(left, op).multiply(asDecimal(right, op)), op));
            case "/"  -> Value.decimal(checked(divide(asDecimal(left, op), asDecimal(right, op), op), op));
            case "%"  -> Value.decimal(checked(remainder(asDecimal(left, op), asDecimal(right, op)), op));
            case "**" -> Value.decimal(checked(power(asDecimal(left, op), asDecimal(right, op), op), op));
            case "<", "<=", ">", ">=", "==", "!=" -> compare(op, left, right);
            default -> throw new IllegalArgumentException("Unsupported binary operator: " + op);
        };
    }

    private static BigDecimal divide(BigDecimal dividend, BigDecimal divisor, String context) {
        if (divisor.signum() == 0) {
            throw new IllegalArgumentException("Division by zero in '" + context + "'");
        }
        try {
            // the exact quotient when it terminates
            return dividend.divide(divisor);
        } catch (ArithmeticException e) {
            // non-terminating: 34 significant digits, HALF_EVEN
            return dividend.divide(divisor, DIVISION);
        }
    }

    private static BigDecimal remainder(BigDecimal dividend, BigDecimal divisor) {
        if (divisor.signum() == 0) {
            throw new IllegalArgumentException("Division by zero in '%'");
        }
        return dividend.remainder(divisor);
    }

    private static BigDecimal power(BigDecimal base, BigDecimal exponent, String context) {
        var n = wholeNumber(exponent, "exponent of '" + context + "'", MAX_EXPONENT);
        if (n >= 0) {
            return base.pow(n);
        }
        return divide(BigDecimal.ONE, base.pow(-n), context);
    }

    /** A number operand for arithmetic: a decimal, or a string that is a canonical number. */
    private static BigDecimal asDecimal(Value v, String context) {
        if (v instanceof DecimalValue(BigDecimal d)) {
            return d;
        }
        if (v instanceof StringValue(String s)) {
            var d = NumericStrings.canonical(s);
            if (d != null) {
                return d;
            }
        }
        if (v instanceof BooleanValue) {
            throw new IllegalArgumentException("Boolean operand in '" + context + "': " + v);
        }
        throw new IllegalArgumentException("Expected number in " + context + ", got " + v);
    }

    private static int wholeNumber(BigDecimal value, String what, int bound) {
        var whole = value.stripTrailingZeros();
        if (whole.scale() > 0) {
            throw new IllegalArgumentException("The " + what + " must be a whole number in a DECIMAL statement, got " +
                    canonical(value));
        }
        try {
            var n = whole.intValueExact();
            if (Math.abs(n) > bound) {
                throw new IllegalArgumentException("The " + what + " is limited to " + bound + ", got " + n);
            }
            return n;
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("The " + what + " is limited to " + bound + ", got " + canonical(value));
        }
    }

    private static Value compare(String op, Value left, Value right) {
        var numeric = compareNumbers(left, right);
        if (numeric != null) {
            return Value.bool(switch (op) {
                case "==" -> numeric == 0;
                case "!=" -> numeric != 0;
                default   -> NumericStrings.relation(op, numeric);
            });
        }
        var equality = "==".equals(op) || "!=".equals(op);
        if (left instanceof StringValue(String ls) && right instanceof StringValue(String rs)) {
            // two strings that are not both numbers compare as text, as they do in COMPUTE
            var cmp = ls.compareTo(rs);
            return Value.bool(switch (op) {
                case "==" -> cmp == 0;
                case "!=" -> cmp != 0;
                default   -> NumericStrings.relation(op, cmp);
            });
        }
        if (equality) {
            if (left instanceof BooleanValue(boolean lb) && right instanceof BooleanValue(boolean rb)) {
                return Value.bool("==".equals(op) == (lb == rb));
            }
            throw new IllegalArgumentException("Type mismatch for equality: " + left + " " + op + " " + right);
        }
        if (left instanceof BooleanValue || right instanceof BooleanValue) {
            throw new IllegalArgumentException("Boolean operand in '" + op + "': " +
                    (left instanceof BooleanValue? left : right));
        }
        throw new IllegalArgumentException("Expected number in " + op + ", got " +
                (NumericStrings.comparable(left) == null? left : right));
    }

    /** Two decimals, a decimal and a canonical string, or two canonical strings compare exactly. */
    private static Integer compareNumbers(Value left, Value right) {
        if (left instanceof DecimalValue(BigDecimal a) && right instanceof DecimalValue(BigDecimal b)) {
            return a.compareTo(b);
        }
        var a = NumericStrings.comparable(left);
        var b = NumericStrings.comparable(right);
        return a == null || b == null? null : a.compareTo(b);
    }

    private static Value call(Expr.Call c) {
        if (!(c.callee() instanceof Expr.Variable) && !(c.callee() instanceof Expr.MemberAccess)) {
            throw new IllegalArgumentException("Unknown function: expression");
        }
        var full = calleeName(c.callee());
        var name = full.startsWith(MATH)? full.substring(MATH.length()) : full;
        if (REFUSED_FUNCTIONS.contains(name)) {
            throw new IllegalArgumentException(name + "() is not available in a DECIMAL statement: " +
                    "the result cannot be exact; use COMPUTE or a graph.task function");
        }
        if (!FUNCTIONS.contains(name)) {
            // a misspelled or unsupported function is rejected by name, never a silent no-op
            throw new IllegalArgumentException("Unknown function: " + full);
        }
        var args = c.args();
        return switch (name) {
            case "round" -> round(args);
            case "min", "max" -> minMax(name, args);
            case "pow" -> {
                arity(name, args, 2);
                yield Value.decimal(checked(power(argument(name, args.get(0)), argument(name, args.get(1)), name), name));
            }
            default -> {
                arity(name, args, 1);
                var x = argument(name, args.getFirst());
                yield Value.decimal(switch (name) {
                    case "abs" -> x.abs();
                    case "floor" -> x.setScale(0, RoundingMode.FLOOR);
                    default -> x.setScale(0, RoundingMode.CEILING);
                });
            }
        };
    }

    private static void arity(String name, List<Expr> args, int expected) {
        if (args.size() != expected) {
            throw new IllegalArgumentException("Function " + name + " expects " + expected + " args, got " + args.size());
        }
    }

    private static BigDecimal argument(String function, Expr arg) {
        return asDecimal(eval(arg), "argument of " + function + "()");
    }

    private static Value minMax(String name, List<Expr> args) {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Function " + name + " needs at least one argument");
        }
        var values = new ArrayList<BigDecimal>(args.size());
        for (var arg : args) {
            values.add(argument(name, arg));
        }
        var best = values.getFirst();
        for (var value : values) {
            // the first of equal values wins, so the result is the same in every engine
            var cmp = value.compareTo(best);
            if ("min".equals(name)? cmp < 0 : cmp > 0) {
                best = value;
            }
        }
        return Value.decimal(best);
    }

    private static Value round(List<Expr> args) {
        if (args.size() != 3) {
            throw new IllegalArgumentException("round() takes three arguments in a DECIMAL statement, " +
                    "round(x, scale, mode), where mode is " + MODES + "; got " + args.size());
        }
        var x = argument("round", args.get(0));
        var scale = wholeNumber(argument("round", args.get(1)), "scale of round()", MAX_SCALE);
        var mode = roundingMode(args.get(2));
        return Value.decimal(normalize(x.setScale(scale, mode)));
    }

    private static RoundingMode roundingMode(Expr arg) {
        // the mode is a name, written bare (HALF_UP) or quoted ('HALF_UP')
        var name = switch (arg) {
            case Expr.Variable(String n) -> n;
            case Expr.StringLiteral(String s) -> s;
            default -> null;
        };
        if (name != null) {
            try {
                var mode = RoundingMode.valueOf(name.trim().toUpperCase(Locale.ROOT));
                if (mode != RoundingMode.UNNECESSARY) {
                    return mode;
                }
            } catch (IllegalArgumentException e) {
                // reported below
            }
        }
        throw new IllegalArgumentException("The mode of round() must be " + MODES);
    }

    private static String calleeName(Expr e) {
        if (e instanceof Expr.Variable(String name)) {
            return name;
        }
        if (e instanceof Expr.MemberAccess(Expr target, String property)) {
            return calleeName(target) + "." + property;
        }
        return "expression";
    }
}
