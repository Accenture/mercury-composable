/*

    Copyright 2018-2026 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package org.platformlambda.mini.jsonpath;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A recursive descent parser for the grammar of RFC 9535 (its appendix A), over the code points of the query.
 * It also applies the well-typedness rules of RFC 9535 2.4.3, so every query that compiles is one that the
 * evaluator can run. Nesting - parentheses, filters and function arguments - is limited to {@value #MAX_DEPTH}
 * levels, so no query text can exhaust the stack.
 */
final class Parser {
    static final int MAX_DEPTH = 64;
    // RFC 9535 2.1: an index or slice bound is an integer of the I-JSON range
    private static final long MAX_INTEGER = 9_007_199_254_740_991L;
    private static final String CLOSING_PARENTHESIS = "')'";
    private final String text;
    private final int[] cps;
    private int pos;
    private int depth;

    Parser(String text) {
        this.text = text;
        this.cps = text.codePoints().toArray();
    }

    /** jsonpath-query = root-identifier segments, and nothing after it. */
    Query parseQuery() {
        expect('$', "a query that begins with '$'");
        List<Segment> segments = parseSegments();
        if (pos < cps.length) {
            throw error("unexpected " + describe(cps[pos]));
        }
        return new Query(true, segments);
    }

    // ---- segments and selectors ----

    /** segments = *(S segment); blanks are consumed only when a segment follows them. */
    private List<Segment> parseSegments() {
        List<Segment> segments = new ArrayList<>();
        while (true) {
            int mark = pos;
            skipBlank();
            if (peek('[')) {
                segments.add(new Segment(false, parseBracketed()));
            } else if (peek('.')) {
                segments.add(parseDotSegment());
            } else {
                pos = mark;
                return segments;
            }
        }
    }

    private Segment parseDotSegment() {
        pos++;
        boolean descendant = peek('.');
        if (descendant) {
            pos++;
            if (peek('[')) {
                return new Segment(true, parseBracketed());
            }
        }
        if (peek('*')) {
            pos++;
            return new Segment(descendant, List.of(new Selector.Wildcard()));
        }
        return new Segment(descendant, List.of(new Selector.Name(parseShorthand())));
    }

    /** member-name-shorthand = name-first *name-char. */
    private String parseShorthand() {
        if (pos >= cps.length || !isNameFirst(cps[pos])) {
            throw error("expected a member name");
        }
        StringBuilder name = new StringBuilder();
        while (pos < cps.length && (isNameFirst(cps[pos]) || isDigit(cps[pos]))) {
            name.appendCodePoint(cps[pos++]);
        }
        return name.toString();
    }

    /** bracketed-selection = "[" S selector *(S "," S selector) S "]". */
    private List<Selector> parseBracketed() {
        pos++;
        List<Selector> selectors = new ArrayList<>();
        do {
            skipBlank();
            selectors.add(parseSelector());
            skipBlank();
        } while (consume(','));
        expect(']', "',' or ']'");
        return selectors;
    }

    private Selector parseSelector() {
        if (peek('\'') || peek('"')) {
            return new Selector.Name(parseString());
        }
        if (consume('*')) {
            return new Selector.Wildcard();
        }
        if (consume('?')) {
            skipBlank();
            return new Selector.Filter(parseLogical());
        }
        return parseIndexOrSlice();
    }

    /** index-selector = int; slice-selector = [start S] ":" S [end S] [":" [S step]]. */
    private Selector parseIndexOrSlice() {
        Long start = isIntegerStart() ? parseInteger() : null;
        skipBlank();
        if (!consume(':')) {
            if (start == null) {
                throw error("expected a selector");
            }
            return new Selector.Index(start);
        }
        skipBlank();
        Long end = isIntegerStart() ? parseInteger() : null;
        skipBlank();
        Long step = null;
        if (consume(':')) {
            skipBlank();
            step = isIntegerStart() ? parseInteger() : null;
        }
        return new Selector.Slice(start, end, step);
    }

    // ---- logical expressions ----

    /** logical-or-expr = logical-and-expr *(S "||" S logical-and-expr). */
    private Expr parseLogical() {
        enter();
        List<Expr> terms = new ArrayList<>();
        do {
            terms.add(parseAnd());
        } while (consumeOperator("||"));
        depth--;
        return terms.size() == 1 ? terms.getFirst() : new Expr.Or(terms);
    }

    /** logical-and-expr = basic-expr *(S "&&" S basic-expr). */
    private Expr parseAnd() {
        List<Expr> terms = new ArrayList<>();
        do {
            terms.add(parseBasic());
        } while (consumeOperator("&&"));
        return terms.size() == 1 ? terms.getFirst() : new Expr.And(terms);
    }

    /** basic-expr = paren-expr / comparison-expr / test-expr. */
    private Expr parseBasic() {
        if (consume('!')) {
            skipBlank();
            return new Expr.Not(peek('(') ? parseParen() : parseNegatedTest());
        }
        return peek('(') ? parseParen() : parseComparisonOrTest();
    }

    private Expr parseParen() {
        pos++;
        skipBlank();
        Expr expr = parseLogical();
        skipBlank();
        expect(')', CLOSING_PARENTHESIS);
        return expr;
    }

    /** After '!': test-expr only - a negation is never the left side of a comparison. */
    private Expr parseNegatedTest() {
        int start = pos;
        if (isQueryStart()) {
            return new Expr.Exists(parseFilterQuery());
        }
        if (isFunctionStart()) {
            return testOf(parseFunction(), start);
        }
        throw error("expected a query or a function");
    }

    private Expr parseComparisonOrTest() {
        int start = pos;
        if (isQueryStart()) {
            Query query = parseFilterQuery();
            Expr.Op op = consumeComparison();
            if (op == null) {
                return new Expr.Exists(query);
            }
            return new Expr.Comparison(singular(query, start), op, parseComparable());
        }
        if (isFunctionStart()) {
            FunctionCall call = parseFunction();
            Expr.Op op = consumeComparison();
            if (op == null) {
                return testOf(call, start);
            }
            return new Expr.Comparison(valueOf(call, start), op, parseComparable());
        }
        Operand literal = new Operand.Literal(parseLiteral());
        Expr.Op op = consumeComparison();
        if (op == null) {
            throw error("expected a comparison operator after a literal");
        }
        return new Expr.Comparison(literal, op, parseComparable());
    }

    /** comparable = literal / singular-query / function-expr (of a value type). */
    private Operand parseComparable() {
        int start = pos;
        if (isQueryStart()) {
            return singular(parseFilterQuery(), start);
        }
        if (isFunctionStart()) {
            return valueOf(parseFunction(), start);
        }
        return new Operand.Literal(parseLiteral());
    }

    private Operand singular(Query query, int start) {
        if (!query.isSingular()) {
            throw errorAt(start, "a comparison takes a singular query, which selects one name or index per segment");
        }
        return new Operand.SingularQuery(query);
    }

    private FunctionCall valueOf(FunctionCall call, int start) {
        if (call.type() != JsonFunction.Type.VALUE) {
            throw errorAt(start, call.name() + "() is a test, not a value to compare");
        }
        return call;
    }

    private Expr testOf(FunctionCall call, int start) {
        if (call.type() == JsonFunction.Type.VALUE) {
            throw errorAt(start, call.name() + "() yields a value; compare it to make a test");
        }
        return new Expr.Test(call);
    }

    private Expr.Op consumeComparison() {
        int mark = pos;
        skipBlank();
        for (Expr.Op op : Expr.Op.values()) {
            if (startsWith(op.symbol)) {
                pos += op.symbol.length();
                skipBlank();
                return op;
            }
        }
        pos = mark;
        return null;
    }

    private boolean isQueryStart() {
        return peek('@') || peek('$');
    }

    /** filter-query = rel-query / jsonpath-query. */
    private Query parseFilterQuery() {
        boolean absolute = cps[pos] == '$';
        pos++;
        return new Query(absolute, parseSegments());
    }

    // ---- functions ----

    private boolean isFunctionStart() {
        int i = pos;
        if (i >= cps.length || cps[i] < 'a' || cps[i] > 'z') {
            return false;
        }
        while (i < cps.length && isFunctionNameChar(cps[i])) {
            i++;
        }
        return i < cps.length && cps[i] == '(';
    }

    /** function-expr = function-name "(" S [function-argument *(S "," S function-argument)] S ")". */
    private FunctionCall parseFunction() {
        int start = pos;
        StringBuilder name = new StringBuilder();
        while (cps[pos] != '(') {
            name.appendCodePoint(cps[pos++]);
        }
        JsonFunction function = JsonFunction.of(name.toString());
        if (function == null) {
            throw errorAt(start, "unknown function " + name + "()");
        }
        pos++;
        enter();
        List<Argument> arguments = new ArrayList<>();
        skipBlank();
        if (!peek(')')) {
            do {
                skipBlank();
                arguments.add(parseArgument());
                skipBlank();
            } while (consume(','));
        }
        expect(')', CLOSING_PARENTHESIS);
        depth--;
        return bind(function, arguments, start);
    }

    /** function-argument = literal / filter-query / logical-expr / function-expr. */
    private Argument parseArgument() {
        int start = pos;
        Argument simple = parseSimpleArgument();
        if (simple != null && endsArgument()) {
            return simple;
        }
        pos = start;
        return new Argument.Logical(parseLogical());
    }

    private Argument parseSimpleArgument() {
        if (isQueryStart()) {
            return new Argument.Nodes(parseFilterQuery());
        }
        if (isFunctionStart()) {
            return new Argument.Call(parseFunction());
        }
        if (isLiteralStart()) {
            return new Argument.Value(parseLiteral());
        }
        return null;
    }

    private boolean endsArgument() {
        int mark = pos;
        skipBlank();
        boolean ends = peek(',') || peek(')');
        pos = mark;
        return ends;
    }

    // RFC 9535 2.4.3: each argument must be well-typed for its parameter
    private FunctionCall bind(JsonFunction function, List<Argument> arguments, int start) {
        List<JsonFunction.Type> parameters = function.parameters;
        if (arguments.size() != parameters.size()) {
            throw errorAt(start, function.functionName + "() takes " + parameters.size() + " argument" +
                    (parameters.size() == 1 ? "" : "s") + ", not " + arguments.size());
        }
        List<Object> bound = new ArrayList<>(arguments.size());
        for (int i = 0; i < arguments.size(); i++) {
            Argument argument = arguments.get(i);
            bound.add(parameters.get(i) == JsonFunction.Type.NODES ?
                    nodesArgument(argument, function, start) : valueArgument(argument, function, start));
        }
        return new FunctionCall(function, bound);
    }

    private Operand valueArgument(Argument argument, JsonFunction function, int start) {
        return switch (argument) {
            case Argument.Value(Object literal) -> new Operand.Literal(literal);
            case Argument.Nodes(Query query) when query.isSingular() -> new Operand.SingularQuery(query);
            case Argument.Call(FunctionCall call) when call.type() == JsonFunction.Type.VALUE -> call;
            default -> throw errorAt(start, function.functionName +
                    "() takes a value: a literal, a singular query or a function that yields a value");
        };
    }

    private Query nodesArgument(Argument argument, JsonFunction function, int start) {
        if (argument instanceof Argument.Nodes(Query query)) {
            return query;
        }
        throw errorAt(start, function.functionName + "() takes a query");
    }

    // ---- literals ----

    private boolean isLiteralStart() {
        return peek('\'') || peek('"') || peek('-') || (pos < cps.length && isDigit(cps[pos])) ||
                startsWith("true") || startsWith("false") || startsWith("null");
    }

    /** literal = number / string-literal / true / false / null. */
    private Object parseLiteral() {
        if (peek('\'') || peek('"')) {
            return parseString();
        }
        if (consumeWord("true")) {
            return Boolean.TRUE;
        }
        if (consumeWord("false")) {
            return Boolean.FALSE;
        }
        if (consumeWord("null")) {
            return null;
        }
        if (peek('-') || (pos < cps.length && isDigit(cps[pos]))) {
            return parseNumber();
        }
        throw error("expected a literal, a query or a function");
    }

    /** number = (int / "-0") [ frac ] [ exp ]. */
    private BigDecimal parseNumber() {
        StringBuilder number = new StringBuilder();
        if (consume('-')) {
            number.append('-');
        }
        appendIntegerDigits(number);
        if (consume('.')) {
            number.append('.');
            appendDigits(number, "a digit after the decimal point");
        }
        if (peek('e') || peek('E')) {
            number.append('e');
            pos++;
            if (peek('+') || peek('-')) {
                number.appendCodePoint(cps[pos++]);
            }
            appendDigits(number, "a digit in the exponent");
        }
        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException e) {
            throw error("the number is out of range");
        }
    }

    // an integer part: "0", or a digit 1-9 followed by digits
    private void appendIntegerDigits(StringBuilder number) {
        if (consume('0')) {
            number.append('0');
            if (pos < cps.length && isDigit(cps[pos])) {
                throw error("a number has no leading zero");
            }
        } else {
            appendDigits(number, "a digit");
        }
    }

    private void appendDigits(StringBuilder number, String what) {
        if (pos >= cps.length || !isDigit(cps[pos])) {
            throw error("expected " + what);
        }
        while (pos < cps.length && isDigit(cps[pos])) {
            number.appendCodePoint(cps[pos++]);
        }
    }

    private boolean isIntegerStart() {
        int i = peek('-') ? pos + 1 : pos;
        return i < cps.length && isDigit(cps[i]);
    }

    /** int = "0" / (["-"] DIGIT1 *DIGIT), within the I-JSON range. */
    private long parseInteger() {
        int start = pos;
        boolean negative = consume('-');
        if (consume('0')) {
            if (negative || (pos < cps.length && isDigit(cps[pos]))) {
                throw errorAt(start, "an index is 0 or an integer without a leading zero");
            }
            return 0;
        }
        StringBuilder digits = new StringBuilder();
        while (pos < cps.length && isDigit(cps[pos])) {
            digits.appendCodePoint(cps[pos++]);
        }
        if (digits.length() > 16 || Long.parseLong(digits.toString()) > MAX_INTEGER) {
            throw errorAt(start, "an index is out of the range -(2^53)+1 to (2^53)-1");
        }
        long value = Long.parseLong(digits.toString());
        return negative ? -value : value;
    }

    /** string-literal: single or double-quoted, with the escapes of RFC 9535 2.3.1.1. */
    private String parseString() {
        int quote = cps[pos++];
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= cps.length) {
                throw error("the string is not closed");
            }
            int c = cps[pos++];
            if (c == quote) {
                return sb.toString();
            }
            if (c == '\\') {
                appendEscape(sb, quote);
            } else if (c < 0x20 || (c >= Character.MIN_SURROGATE && c <= Character.MAX_SURROGATE)) {
                throw errorAt(pos - 1, "a string cannot hold " + describe(c) + " unescaped");
            } else {
                sb.appendCodePoint(c);
            }
        }
    }

    private void appendEscape(StringBuilder sb, int quote) {
        int c = pos < cps.length ? cps[pos++] : -1;
        switch (c) {
            case 'b' -> sb.append('\b');
            case 'f' -> sb.append('\f');
            case 'n' -> sb.append('\n');
            case 'r' -> sb.append('\r');
            case 't' -> sb.append('\t');
            case '/' -> sb.append('/');
            case '\\' -> sb.append('\\');
            case 'u' -> sb.appendCodePoint(parseUnicodeEscape());
            default -> {
                // a quote is escaped only inside a string of the same quote
                if (c != quote) {
                    throw errorAt(pos - 1, "invalid escape in a string");
                }
                sb.appendCodePoint(c);
            }
        }
    }

    // \\uXXXX, or a surrogate pair written as two such escapes; a lone surrogate is refused
    private int parseUnicodeEscape() {
        int start = pos;
        int unit = parseHex4();
        if (Character.isLowSurrogate((char) unit)) {
            throw errorAt(start, "a low surrogate must follow a high surrogate");
        }
        if (!Character.isHighSurrogate((char) unit)) {
            return unit;
        }
        if (!startsWith("\\u")) {
            throw errorAt(start, "a high surrogate must be followed by a low surrogate");
        }
        pos += 2;
        int low = parseHex4();
        if (!Character.isLowSurrogate((char) low)) {
            throw errorAt(start, "a high surrogate must be followed by a low surrogate");
        }
        return Character.toCodePoint((char) unit, (char) low);
    }

    private int parseHex4() {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int digit = pos < cps.length ? Character.digit(cps[pos], 16) : -1;
            if (digit < 0 || cps[pos] > 'f') {
                throw error("expected four hexadecimal digits");
            }
            value = value * 16 + digit;
            pos++;
        }
        return value;
    }

    // ---- characters ----

    private static boolean isNameFirst(int c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_' ||
                (c >= 0x80 && c <= 0xD7FF) || (c >= 0xE000 && c <= 0x10FFFF);
    }

    private static boolean isFunctionNameChar(int c) {
        return (c >= 'a' && c <= 'z') || c == '_' || isDigit(c);
    }

    private static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    // RFC 9535 2.1: blank space is space, horizontal tab, line feed and carriage return
    private static boolean isBlank(int c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private void skipBlank() {
        while (pos < cps.length && isBlank(cps[pos])) {
            pos++;
        }
    }

    private boolean peek(int c) {
        return pos < cps.length && cps[pos] == c;
    }

    private boolean consume(int c) {
        if (peek(c)) {
            pos++;
            return true;
        }
        return false;
    }

    private boolean consumeOperator(String op) {
        int mark = pos;
        skipBlank();
        if (startsWith(op)) {
            pos += op.length();
            skipBlank();
            return true;
        }
        pos = mark;
        return false;
    }

    private boolean consumeWord(String word) {
        if (startsWith(word)) {
            pos += word.length();
            return true;
        }
        return false;
    }

    private boolean startsWith(String s) {
        if (pos + s.length() > cps.length) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (cps[pos + i] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private void expect(int c, String what) {
        if (!consume(c)) {
            throw error("expected " + what);
        }
    }

    private void enter() {
        if (++depth > MAX_DEPTH) {
            throw error("the query nests deeper than " + MAX_DEPTH + " levels");
        }
    }

    private static String describe(int c) {
        return c >= 0x20 && c != 0x7F ? "'" + new String(Character.toChars(c)) + "'" : String.format("U+%04X", c);
    }

    private JsonPathException error(String reason) {
        return errorAt(pos, reason);
    }

    private JsonPathException errorAt(int position, String reason) {
        return new JsonPathException(reason, text, position);
    }

    /** A function argument before it is checked against its parameter's type. */
    private sealed interface Argument {
        record Value(Object literal) implements Argument { }
        record Nodes(Query query) implements Argument { }
        record Call(FunctionCall call) implements Argument { }
        record Logical(Expr expr) implements Argument { }
    }
}
