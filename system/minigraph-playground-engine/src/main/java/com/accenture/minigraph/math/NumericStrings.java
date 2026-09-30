package com.accenture.minigraph.math;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * The numeric-string comparison rule shared by both evaluators (RFC-0001).
 * <p>
 * Every value is rendered into the statement text before it is parsed, so the evaluator sees literals and
 * not sources: a substituted string is indistinguishable from one the author typed. A string that is a
 * canonical number therefore compares as a number, which makes '200' == '200', 200 == 200 and 200 == '200'
 * the same comparison. The comparison is exact: a number operand is taken as the decimal text it prints as,
 * and a string as the decimal it spells, so two distinct 20-digit ids never collapse into equal numbers.
 */
final class NumericStrings {
    // plain notation: an optional minus sign, digits without leading zeros, an optional fraction
    private static final Pattern CANONICAL = Pattern.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?");
    // bounds the work a hostile statement can ask of a comparison
    private static final int MAX_LENGTH = 1000;

    private NumericStrings() {
        // utility class
    }

    /**
     * @param text a string
     * @return the exact decimal it spells when it is a canonical number, otherwise null
     */
    static BigDecimal canonical(String text) {
        if (text == null || text.length() > MAX_LENGTH || !CANONICAL.matcher(text).matches()) {
            return null;
        }
        return new BigDecimal(text);
    }

    /**
     * @param v a value
     * @return its exact decimal when it can take part in a numeric comparison, otherwise null
     */
    static BigDecimal comparable(Value v) {
        return switch (v) {
            case DecimalValue(BigDecimal d) -> d;
            case NumberValue(double d) -> Double.isFinite(d)? BigDecimal.valueOf(d) : null;
            case StringValue(String s) -> canonical(s);
            case BooleanValue ignored -> null;
        };
    }

    /**
     * Compare two operands numerically when the rule applies: at least one is a string, and both are
     * numbers or canonical numbers. Number against number is left to the evaluator's own paths, and a string
     * that is not a canonical number, or a boolean, keeps today's behaviour.
     *
     * @param left the left operand
     * @param right the right operand
     * @return the comparison result (negative, zero or positive), or null when the rule does not apply
     */
    static Integer compare(Value left, Value right) {
        if (!(left instanceof StringValue) && !(right instanceof StringValue)) {
            return null;
        }
        var a = comparable(left);
        var b = comparable(right);
        return a == null || b == null? null : a.compareTo(b);
    }

    /**
     * @param operator one of &lt; &lt;= &gt; &gt;=
     * @param comparison the result of a comparison
     * @return the outcome of the operator
     */
    static boolean relation(String operator, int comparison) {
        return switch (operator) {
            case "<"  -> comparison < 0;
            case "<=" -> comparison <= 0;
            case ">"  -> comparison > 0;
            case ">=" -> comparison >= 0;
            default   -> throw new IllegalStateException("Not a relational operator: " + operator);
        };
    }
}
