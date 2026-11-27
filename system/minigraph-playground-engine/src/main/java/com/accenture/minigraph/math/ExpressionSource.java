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

package com.accenture.minigraph.math;

import com.accenture.util.JsonPathSource;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Renders the {selectors} of a graph.math expression into the text the evaluator parses. A statement's
 * expression is the graph author's and a value comes from the data, so each value is checked where it lands
 * and is always one value, never more expression:
 * <ul>
 * <li>inside a string literal the author wrote, it is the text of that literal, escaped;</li>
 * <li>next to a dot it is a name or digits;</li>
 * <li>anywhere else a number or a boolean is itself, and a text value is a string literal in a boolean
 * expression and one number in a number expression - text that does not spell a number fails there by name.</li>
 * </ul>
 * A text value that spells {@code true}, {@code false} or {@code null} keeps its meaning in a number
 * expression: the first two read as booleans, and the third is reported as an unresolved variable.
 */
public final class ExpressionSource {
    /** How the statement reads its expression, which decides how a text value is rendered. */
    public enum Kind {
        /** A COMPUTE or an IF without a comparison or boolean operator. */
        NUMBER,
        /** A DECIMAL statement: a number expression where a Double or Float renders as its plain decimal text. */
        DECIMAL,
        /** A CONDITION, or a COMPUTE or an IF with a comparison or boolean operator. */
        BOOLEAN
    }

    // bounds the work a value from the data can ask of the number test
    private static final int MAX_NUMBER_LENGTH = 1000;
    // one number literal as the dialect's lexer reads it, with an optional sign
    private static final Pattern ONE_NUMBER = Pattern.compile("[+-]?(\\d+(\\.\\d+)?|\\.\\d+)([eE][+-]?\\d+)?");
    private static final Pattern NAME_OR_DIGITS = Pattern.compile("\\w+");
    private static final Set<String> KEYWORDS = Set.of("true", "false", "null");
    private static final char OUTSIDE = 0;
    private static final Utility util = Utility.getInstance();

    private ExpressionSource() {
        // utility class
    }

    /**
     * Render the selectors of an expression.
     *
     * @param expression the statement's expression as the author wrote it
     * @param kind how the statement reads the expression
     * @param lookup resolves a selector's key to its value
     * @return the text for the evaluator
     * @throws IllegalArgumentException when a text value cannot be one value where it lands; the message
     *                                  names the selector and never carries the value
     */
    public static String render(String expression, Kind kind, Function<String, Object> lookup) {
        var sb = new StringBuilder();
        var start = 0;
        var quote = OUTSIDE;
        for (var segment : util.extractSegments(expression, "{", "}")) {
            var heading = expression.substring(start, segment.start());
            sb.append(heading);
            quote = quoteAfter(quote, heading);
            var key = expression.substring(segment.start() + 1, segment.end() - 1);
            if (JsonPathSource.isGraphKey(key)) {
                JsonPathSource.refuseJsonPathKey(key);
                var dot = heading.endsWith(".") ||
                        (expression.length() > segment.end() && expression.charAt(segment.end()) == '.');
                sb.append(rendered(new Selector(key, expression, kind), lookup.apply(key), quote, dot));
            } else {
                // a JavaScript block or a JSON object, left as written
                var written = expression.substring(segment.start(), segment.end());
                sb.append(written);
                quote = quoteAfter(quote, written);
            }
            start = segment.end();
        }
        return sb.append(expression.substring(start)).toString();
    }

    /**
     * @param text a text value
     * @return true when it spells one number literal of the dialect, with an optional sign
     */
    public static boolean isOneNumber(String text) {
        var trimmed = text.strip();
        return trimmed.length() <= MAX_NUMBER_LENGTH && ONE_NUMBER.matcher(trimmed).matches();
    }

    private static String rendered(Selector selector, Object value, char quote, boolean dot) {
        if (value == null) {
            return "null";
        }
        var text = textOf(selector, value);
        if (quote != OUTSIDE) {
            return escape(text, quote);
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Map || value instanceof List) {
            return text;
        }
        if (dot) {
            if (NAME_OR_DIGITS.matcher(text).matches()) {
                return text;
            }
            throw new IllegalArgumentException("Not a name: " + selector.key() + " in '" + selector.expression() +
                    "' - a text value next to a '.' must be a name or digits");
        }
        if (selector.kind() == Kind.BOOLEAN) {
            return "'" + escape(text, '\'') + "'";
        }
        if (KEYWORDS.contains(text.strip()) || isOneNumber(text)) {
            return text;
        }
        throw new IllegalArgumentException("Not a number: " + selector.key() + " in '" + selector.expression() +
                "' - a text value is read here as one number, never as part of the expression; " +
                "write it inside quotes to read it as text ('{" + selector.key() + "}')");
    }

    private static String textOf(Selector selector, Object value) {
        if (selector.kind() == Kind.DECIMAL && (value instanceof Double || value instanceof Float)) {
            try {
                return DecimalEvaluator.plainText((Number) value);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e.getMessage() + ": " + selector.key() + " in '" +
                        selector.expression() + "'");
            }
        }
        if (value instanceof Map || value instanceof List) {
            return SimpleMapper.getInstance().getCompactGson().toJson(value);
        }
        return String.valueOf(value);
    }

    /** The expression lexer reads a backslash as an escape, so it is escaped first, then the quote. */
    private static String escape(String text, char quote) {
        return text.replace("\\", "\\\\").replace(String.valueOf(quote), "\\" + quote);
    }

    /**
     * Follow the author's text with the lexer's rule for a string literal.
     *
     * @param quote the quote character of the literal that is open, or OUTSIDE
     * @param written a piece of the expression as written
     * @return the quote character of the literal open after the piece, or OUTSIDE
     */
    private static char quoteAfter(char quote, String written) {
        var open = quote;
        var i = 0;
        while (i < written.length()) {
            var c = written.charAt(i);
            if (open == OUTSIDE) {
                open = c == '\'' || c == '"'? c : OUTSIDE;
            } else if (c == '\\') {
                // the escaped character belongs to the literal
                i++;
            } else if (c == open) {
                open = OUTSIDE;
            }
            i++;
        }
        return open;
    }

    private record Selector(String key, String expression, Kind kind) { }
}
