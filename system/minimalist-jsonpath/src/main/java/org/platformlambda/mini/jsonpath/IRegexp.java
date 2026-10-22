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

import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * I-Regexp (RFC 9485), the regular expressions of match() and search(), checked against its grammar and translated
 * into a {@link Pattern}. The translation keeps I-Regexp's meaning where java.util.regex reads the same text
 * differently: '.' matches any character but a line feed or a carriage return (Java's '.' also excludes U+0085,
 * U+2028 and U+2029); '^' and '$' anchor the start and the very end of the string, as the compliance suite reads
 * them (Java's '$' would also match before a final line terminator); inside a character class, '&amp;' is ordinary.
 * Every literal outside [A-Za-z0-9] is written as a code point escape, so no character can turn into Java syntax.
 * Groups are non-capturing, and nesting is limited to {@value #MAX_DEPTH} levels.
 */
final class IRegexp {
    private static final int MAX_DEPTH = 64;
    private static final Set<String> CATEGORIES = Set.of(
            "L", "Lu", "Ll", "Lt", "Lm", "Lo", "M", "Mn", "Mc", "Me", "N", "Nd", "Nl", "No",
            "P", "Pc", "Pd", "Ps", "Pe", "Pi", "Pf", "Po", "Z", "Zs", "Zl", "Zp",
            "S", "Sm", "Sc", "Sk", "So", "C", "Cc", "Cf", "Cn", "Co");
    private static final Invalid INVALID = new Invalid();
    private final int[] cps;
    private final StringBuilder out = new StringBuilder();
    private int pos;
    private int depth;

    private IRegexp(String regex) {
        this.cps = regex.codePoints().toArray();
    }

    /**
     * @param regex an I-Regexp
     * @return the pattern, or null when the text is not a valid I-Regexp
     */
    static Pattern compile(String regex) {
        IRegexp parser = new IRegexp(regex);
        try {
            parser.regexp();
            if (parser.pos != parser.cps.length) {
                return null;
            }
            return Pattern.compile(parser.out.toString());
        } catch (Invalid | PatternSyntaxException e) {
            return null;
        }
    }

    /** i-regexp = branch *( "|" branch ). */
    private void regexp() {
        branch();
        while (peek('|')) {
            pos++;
            out.append('|');
            branch();
        }
    }

    /** branch = *piece. */
    private void branch() {
        while (pos < cps.length && cps[pos] != '|' && cps[pos] != ')') {
            atom();
            quantifier();
        }
    }

    /** atom = NormalChar / charClass / ( "(" i-regexp ")" ). */
    private void atom() {
        int c = cps[pos++];
        switch (c) {
            case '(' -> group();
            case '[' -> charClassExpr();
            case '.' -> out.append("[^\\n\\r]");
            case '^' -> out.append("\\A");
            case '$' -> out.append("\\z");
            case '\\' -> escape();
            default -> {
                if (!isNormalChar(c)) {
                    throw INVALID;
                }
                literal(c);
            }
        }
    }

    private void group() {
        if (++depth > MAX_DEPTH) {
            throw INVALID;
        }
        out.append("(?:");
        regexp();
        if (!peek(')')) {
            throw INVALID;
        }
        pos++;
        out.append(')');
        depth--;
    }

    /** quantifier = ( "*" / "+" / "?" ) / range-quantifier. */
    private void quantifier() {
        if (peek('*') || peek('+') || peek('?')) {
            out.appendCodePoint(cps[pos++]);
        } else if (peek('{')) {
            pos++;
            rangeQuantifier();
        }
    }

    /** range-quantifier = "{" QuantExact [ "," [ QuantExact ] ] "}". */
    private void rangeQuantifier() {
        long min = quantExact();
        out.append('{').append(min);
        if (peek(',')) {
            pos++;
            out.append(',');
            if (pos < cps.length && isDigit(cps[pos])) {
                long max = quantExact();
                if (max < min) {
                    throw INVALID;
                }
                out.append(max);
            }
        }
        if (!peek('}')) {
            throw INVALID;
        }
        pos++;
        out.append('}');
    }

    private long quantExact() {
        long value = 0;
        int start = pos;
        while (pos < cps.length && isDigit(cps[pos])) {
            value = value * 10 + (cps[pos++] - '0');
            if (value > Integer.MAX_VALUE) {
                throw INVALID;
            }
        }
        if (pos == start) {
            throw INVALID;
        }
        return value;
    }

    /** After '\': SingleCharEsc, catEsc ("\p{..}") or complEsc ("\P{..}"). */
    private void escape() {
        if (peek('p') || peek('P')) {
            category(cps[pos++]);
        } else {
            literal(singleCharEscape());
        }
    }

    /** SingleCharEsc, after the '\'. */
    private int singleCharEscape() {
        if (pos >= cps.length) {
            throw INVALID;
        }
        int c = cps[pos++];
        return switch (c) {
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case '(', ')', '*', '+', '-', '.', '?', '[', '\\', ']', '^', '{', '|', '}' -> c;
            default -> throw INVALID;
        };
    }

    /** IsCategory, after "\p" or "\P". */
    private void category(int kind) {
        if (!peek('{')) {
            throw INVALID;
        }
        pos++;
        StringBuilder name = new StringBuilder();
        while (pos < cps.length && cps[pos] != '}') {
            name.appendCodePoint(cps[pos++]);
        }
        if (!peek('}') || !CATEGORIES.contains(name.toString())) {
            throw INVALID;
        }
        pos++;
        out.append('\\').appendCodePoint(kind).append('{').append(name).append('}');
    }

    /** charClassExpr = "[" [ "^" ] ( "-" / CCE1 ) *CCE1 [ "-" ] "]", after the '['. */
    private void charClassExpr() {
        out.append('[');
        if (peek('^')) {
            pos++;
            out.append('^');
        }
        if (peek('-')) {
            pos++;
            literal('-');
        } else {
            classItem();
        }
        while (!peek(']')) {
            if (peek('-') && pos + 1 < cps.length && cps[pos + 1] == ']') {
                pos++;
                literal('-');
            } else {
                classItem();
            }
        }
        pos++;
        out.append(']');
    }

    /** CCE1 = ( CCchar [ "-" CCchar ] ) / charClassEsc. */
    private void classItem() {
        if (pos + 1 < cps.length && cps[pos] == '\\' && (cps[pos + 1] == 'p' || cps[pos + 1] == 'P')) {
            pos += 2;
            category(cps[pos - 1]);
            return;
        }
        int first = classChar();
        if (peek('-') && pos + 1 < cps.length && cps[pos + 1] != ']') {
            pos++;
            int last = classChar();
            if (last < first) {
                throw INVALID;
            }
            literal(first);
            out.append('-');
            literal(last);
        } else {
            literal(first);
        }
    }

    /** CCchar: any character but '-', '[', '\' and ']', or a SingleCharEsc. */
    private int classChar() {
        if (pos >= cps.length) {
            throw INVALID;
        }
        int c = cps[pos++];
        if (c == '\\') {
            return singleCharEscape();
        }
        if (c == '-' || c == '[' || c == ']' || isSurrogate(c)) {
            throw INVALID;
        }
        return c;
    }

    /** NormalChar: any character but . \ ? * + { } ( ) | [ ] and the surrogates. */
    private static boolean isNormalChar(int c) {
        return switch (c) {
            case '(', ')', '*', '+', '.', '?', '[', '\\', ']', '{', '|', '}' -> false;
            default -> !isSurrogate(c);
        };
    }

    private void literal(int c) {
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || isDigit(c)) {
            out.appendCodePoint(c);
        } else {
            out.append("\\x{").append(Integer.toHexString(c)).append('}');
        }
    }

    private boolean peek(int c) {
        return pos < cps.length && cps[pos] == c;
    }

    private static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isSurrogate(int c) {
        return c >= Character.MIN_SURROGATE && c <= Character.MAX_SURROGATE;
    }

    /** Not an I-Regexp; thrown without a stack trace, because it is control flow inside {@link #compile}. */
    private static final class Invalid extends RuntimeException {
        Invalid() {
            super("not an I-Regexp", null, false, false);
        }
    }
}
