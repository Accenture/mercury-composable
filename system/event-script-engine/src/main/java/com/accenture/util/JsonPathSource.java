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

package com.accenture.util;

import org.platformlambda.core.models.VarSegment;
import org.platformlambda.core.util.Utility;
import org.platformlambda.mini.jsonpath.JsonPath;
import org.platformlambda.mini.jsonpath.JsonPathException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A data-mapping source written as a JSONPath query ('$...'), checked against RFC 9535 before it runs (RFC-0010).
 * <p>
 * A source may carry references that the engine replaces with their values before the query is parsed: Event
 * Script's {model.key} and [model.i], and the knowledge graph's {namespace.key}. The values are not known when a
 * flow or a graph is compiled, so the check parses the source with a stand-in for each reference, chosen by where
 * the reference lands:
 * <ul>
 *   <li>{@link Site#NAME} after a dot or inside a member name - the name x;</li>
 *   <li>{@link Site#INDEX} inside a bracketed selector - the index 0;</li>
 *   <li>{@link Site#LITERAL} inside a string literal the author wrote - the letter x;</li>
 *   <li>{@link Site#FILTER} bare inside a filter expression - the string literal 'x'.</li>
 * </ul>
 * No value can make a query of a reference anywhere else (directly after the root '$', for example), so the
 * source is refused there. The check confirms the structure the author wrote; the value is checked where it lands
 * when the engine substitutes it.
 */
public final class JsonPathSource {
    private static final Utility util = Utility.getInstance();
    private static final String MODEL_NAMESPACE = "model.";
    private static final String JSON_PATH_ROOT = "$";
    private static final Pattern INTEGER = Pattern.compile("-?[0-9]+");
    private static final Pattern INDEXES = Pattern.compile("(\\[[0-9]+])*");
    private static final Pattern NUMBER = Pattern.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][-+]?[0-9]+)?");

    /** Which references the engine substitutes into a source before it reads it. */
    public enum Form {
        /** Event Script: {model.key} anywhere, and [model.i] as the whole content of a bracket */
        EVENT_SCRIPT,
        /** The knowledge graph: {namespace.key} anywhere */
        GRAPH,
        /** None: the source is read as written (a data dictionary's default value) */
        PLAIN
    }

    /** Where a reference lands in the query, which decides what may stand there. */
    public enum Site {
        /** After a dot, or inside a member name */
        NAME,
        /** Inside a bracketed selector, outside a filter */
        INDEX,
        /** Inside a string literal the author wrote */
        LITERAL,
        /** Bare inside a filter expression */
        FILTER
    }

    /**
     * A reference in a source.
     *
     * @param start the index of its first character in the source
     * @param end the index after its last character
     * @param site where it lands in the query
     * @param key the key it names
     * @param braced true for a {key}, false for Event Script's [model.i], whose span is the bracket's content
     * @param quote the enclosing string literal's quote character when the site is a literal, else 0
     */
    public record Reference(int start, int end, Site site, String key, boolean braced, char quote) { }

    private JsonPathSource() {
        // utility class
    }

    /**
     * @param source a data-mapping source
     * @return true when the source is a JSONPath query
     */
    public static boolean isJsonPath(String source) {
        return source.startsWith(JSON_PATH_ROOT);
    }

    /**
     * Refuse a JSONPath source that is not RFC 9535, with Mercury's hyphen rule (a hyphen in a dotted name).
     *
     * @param source the source as written, references included
     * @param form the references the engine substitutes into it
     * @throws JsonPathException naming the reason and the position in the source as written
     */
    public static void check(String source, Form form) {
        var references = references(source, form);
        var query = new StringBuilder();
        List<Integer> origin = new ArrayList<>();
        int last = 0;
        for (var reference : references) {
            copy(source, last, reference.start(), query, origin);
            for (char c : standIn(reference.site()).toCharArray()) {
                query.append(c);
                origin.add(reference.start());
            }
            last = reference.end();
        }
        copy(source, last, source.length(), query, origin);
        try {
            JsonPath.compileLenient(query.toString());
        } catch (JsonPathException e) {
            if (references.isEmpty()) {
                throw e;
            }
            // the stand-ins shift the text, so the position is restated in the source as written
            throw new JsonPathException(e.getReason(), source, sourcePosition(source, query, origin, e.getPosition()));
        }
    }

    /**
     * Find the references in a source and where each one lands.
     *
     * @param source the source as written
     * @param form the references the engine substitutes into it
     * @return the references in the order they appear
     * @throws JsonPathException when a reference stands where no value can make a query
     */
    public static List<Reference> references(String source, Form form) {
        var spans = switch (form) {
            case EVENT_SCRIPT -> eventScriptSpans(source);
            case GRAPH -> graphSpans(source);
            case PLAIN -> List.<VarSegment>of();
        };
        List<Reference> result = new ArrayList<>();
        var lexer = new Lexer();
        int next = 0;
        int i = 0;
        while (i < source.length()) {
            if (next < spans.size() && spans.get(next).start() == i) {
                var span = spans.get(next++);
                var site = lexer.site();
                if (site == null) {
                    throw new JsonPathException("a reference is not allowed here", source,
                            source.codePointCount(0, i));
                }
                result.add(new Reference(span.start(), span.end(), site, keyOf(source, span, form),
                        source.charAt(span.start()) == '{', lexer.quote));
                // the lexer reads the stand-in in place of the reference, so the context after it is the same
                for (char c : standIn(site).toCharArray()) {
                    lexer.accept(c);
                }
                i = span.end();
            } else {
                lexer.accept(source.charAt(i++));
            }
        }
        return result;
    }

    /**
     * Substitute each reference in a JSONPath source with its value, checked where the reference lands, so that a
     * value cannot change the query's structure (RFC-0010 WP4):
     * <ul>
     *   <li>in a name, the value is a key path - names joined by single dots, each optionally followed by [n]
     *       indexes ({@code items[0].name});</li>
     *   <li>in an index, an integer;</li>
     *   <li>inside a string literal the author wrote, its text, escaped for that literal (the quote, the backslash
     *       and the control characters);</li>
     *   <li>bare inside a filter, a number, a boolean or null as written, and text as a string literal - except
     *       that Event Script, which has always inserted text as written, keeps text that is already one literal
     *       (a number, true, false, null or a quoted string) as written.</li>
     * </ul>
     * A list or a map is never substituted: RFC 9535 has no array or object literal. Event Script's [model.i] keeps
     * its integer rendering, and a value renders as text the way each engine form always has.
     *
     * @param source the source as written
     * @param form the references the engine substitutes into it
     * @param values the value of a key, null when it has none
     * @return the query
     * @throws IllegalArgumentException naming the reference, the source and the reason
     */
    public static String substitute(String source, Form form, Function<String, Object> values) {
        var references = references(source, form);
        if (references.isEmpty()) {
            return source;
        }
        var query = new StringBuilder();
        int last = 0;
        for (var reference : references) {
            query.append(source, last, reference.start());
            query.append(render(source, reference, form, values.apply(reference.key())));
            last = reference.end();
        }
        return query.append(source.substring(last)).toString();
    }

    private static String render(String source, Reference reference, Form form, Object value) {
        if (value instanceof Map || value instanceof List) {
            throw invalid(source, reference, "a list or a map cannot be substituted into a JSONPath");
        }
        if (!reference.braced()) {
            // Event Script's [model.i] has always rendered as an integer
            return String.valueOf(util.str2int(textOf(form, value)));
        }
        var text = textOf(form, value);
        return switch (reference.site()) {
            case NAME -> {
                if (!isKeyPath(text)) {
                    throw invalid(source, reference, "'" + text + "' is not a key path");
                }
                yield text;
            }
            case INDEX -> {
                if (!INTEGER.matcher(text).matches()) {
                    throw invalid(source, reference, "'" + text + "' is not an integer index");
                }
                yield text;
            }
            case LITERAL -> escape(text, reference.quote());
            case FILTER -> filterValue(form, value);
        };
    }

    private static String filterValue(Form form, Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        var text = String.valueOf(value);
        return form == Form.EVENT_SCRIPT && isLiteral(text) ? text : "'" + escape(text, '\'') + "'";
    }

    /*
     * A value as text, the way each engine form has always rendered it: Event Script renders a string or a number
     * and anything else as null; the knowledge graph renders any value.
     */
    private static String textOf(Form form, Object value) {
        if (value == null || (form == Form.EVENT_SCRIPT && !(value instanceof String || value instanceof Number))) {
            return "null";
        }
        return String.valueOf(value);
    }

    private static IllegalArgumentException invalid(String source, Reference reference, String reason) {
        var written = reference.braced() ? source.substring(reference.start(), reference.end())
                                         : "[" + reference.key() + "]";
        return new IllegalArgumentException("Cannot substitute " + written + " into '" + source + "' - " + reason);
    }

    /**
     * Escape text for an RFC 9535 string literal written with the given quote: the quote, the backslash and the
     * control characters.
     *
     * @param text the text
     * @param quote the literal's quote character
     * @return the escaped text, without the quotes
     */
    public static String escape(String text, char quote) {
        var sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c == quote) {
                        sb.append('\\').append(c);
                    } else if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * @param text a value's text
     * @return true when it is a key path: names joined by single dots, each optionally followed by [n] indexes
     */
    public static boolean isKeyPath(String text) {
        if (text.isEmpty()) {
            return false;
        }
        for (var part : text.split("\\.", -1)) {
            int i = 0;
            while (i < part.length() && isNameChar(part.charAt(i))) {
                i++;
            }
            if (i == 0 || !INDEXES.matcher(part.substring(i)).matches()) {
                return false;
            }
        }
        return true;
    }

    /*
     * Text that is already exactly one RFC 9535 literal: a number, true, false, null, or a string in single or
     * double quotes whose own quote character appears inside only escaped.
     */
    private static boolean isLiteral(String text) {
        if (NUMBER.matcher(text).matches() || "true".equals(text) || "false".equals(text) || "null".equals(text)) {
            return true;
        }
        if (text.length() < 2 || (text.charAt(0) != '\'' && text.charAt(0) != '"')
                || text.charAt(text.length() - 1) != text.charAt(0)) {
            return false;
        }
        char quote = text.charAt(0);
        boolean escaped = false;
        for (int i = 1; i < text.length() - 1; i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == quote) {
                return false;
            }
        }
        return !escaped;
    }

    private static String keyOf(String source, VarSegment span, Form form) {
        if (source.charAt(span.start()) != '{') {
            return source.substring(span.start(), span.end()).trim();
        }
        var key = source.substring(span.start() + 1, span.end() - 1);
        // Event Script trims its key; the knowledge graph reads the key as written
        return form == Form.EVENT_SCRIPT ? key.trim() : key;
    }

    private static String standIn(Site site) {
        return switch (site) {
            case NAME, LITERAL -> "x";
            case INDEX -> "0";
            case FILTER -> "'x'";
        };
    }

    private static void copy(String source, int from, int to, StringBuilder query, List<Integer> origin) {
        for (int i = from; i < to; i++) {
            query.append(source.charAt(i));
            origin.add(i);
        }
    }

    private static int sourcePosition(String source, StringBuilder query, List<Integer> origin, int position) {
        if (position >= query.codePointCount(0, query.length())) {
            return source.codePointCount(0, source.length());
        }
        return source.codePointCount(0, origin.get(query.offsetByCodePoints(0, position)));
    }

    /*
     * Event Script renders {model.key} first and then scans the result for [model.i], pairing each '[' with the
     * first ']' after the previous pair - so a [model.i] inside a filter is not an index reference, and the parser
     * refuses it. The brackets are scanned here the same way, over the source with each brace reference masked.
     */
    private static List<VarSegment> eventScriptSpans(String source) {
        List<VarSegment> braces = new ArrayList<>();
        if (source.contains("{") && source.contains("}")) {
            for (var segment : util.extractSegments(source, "{", "}")) {
                if (isModelKey(source.substring(segment.start() + 1, segment.end() - 1).trim())) {
                    braces.add(segment);
                }
            }
        }
        var masked = new StringBuilder(source);
        for (var segment : braces) {
            for (int i = segment.start(); i < segment.end(); i++) {
                masked.setCharAt(i, 'x');
            }
        }
        List<VarSegment> indexes = new ArrayList<>();
        if (source.contains("[") && source.contains("]")) {
            int start = 0;
            int open = masked.indexOf("[", start);
            int close = masked.indexOf("]", start);
            while (open != -1 && close > open) {
                if (isModelKey(masked.substring(open + 1, close).trim())) {
                    indexes.add(new VarSegment(open + 1, close));
                }
                start = close + 1;
                open = masked.indexOf("[", start);
                close = masked.indexOf("]", start);
            }
        }
        List<VarSegment> result = new ArrayList<>(indexes);
        for (var segment : braces) {
            if (indexes.stream().noneMatch(index -> index.start() <= segment.start() && segment.end() <= index.end())) {
                result.add(segment);
            }
        }
        result.sort(Comparator.comparingInt(VarSegment::start));
        return result;
    }

    private static List<VarSegment> graphSpans(String source) {
        List<VarSegment> result = new ArrayList<>();
        for (var segment : util.extractSegments(source, "{", "}")) {
            if (isGraphKey(source.substring(segment.start() + 1, segment.end() - 1))) {
                result.add(segment);
            }
        }
        return result;
    }

    private static boolean isModelKey(String key) {
        return key.startsWith(MODEL_NAMESPACE) && !key.endsWith(".");
    }

    /**
     * The knowledge graph's rule for a {key} that is a reference: a key with a line break, a tab or a colon is
     * text (a JavaScript block or a JSON object), left as written.
     *
     * @param key the text between the braces
     * @return true when the engine resolves it
     */
    public static boolean isGraphKey(String key) {
        return !(key.contains("\r") || key.contains("\n") || key.contains("\t") || key.contains(":"));
    }

    /**
     * A knowledge-graph {reference} names a state-machine key or a constant, never a JSONPath query: JSONPath is
     * a data mapping's source, read where the source and its target are separated by '->'.
     *
     * @param key the text between the braces of a reference
     * @throws IllegalArgumentException when the key is a JSONPath query
     */
    public static void refuseJsonPathKey(String key) {
        if (isJsonPath(key.trim())) {
            throw new IllegalArgumentException("JSONPath is not supported in a reference - {" + key +
                    "}; map the value to a model variable first");
        }
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c >= 0x80;
    }

    private static final class Frame {
        private boolean filter;
        private int parens;
    }

    /** Just enough of RFC 9535's lexical structure to tell where a reference lands. */
    private static final class Lexer {
        private final Deque<Frame> frames = new ArrayDeque<>();
        private char quote;
        private boolean escaped;
        private char previous = ' ';

        private void accept(char c) {
            if (quote != 0) {
                literal(c);
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else {
                structure(c);
            }
            previous = c;
        }

        private void literal(char c) {
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == quote) {
                quote = 0;
            }
        }

        private void structure(char c) {
            var top = frames.peek();
            if (c == '[') {
                frames.push(new Frame());
            } else if (c == ']') {
                frames.poll();
            } else if (top != null) {
                switch (c) {
                    case '?' -> top.filter = true;
                    case '(' -> top.parens++;
                    case ')' -> top.parens--;
                    // a comma between selectors ends a filter selector; a comma between arguments does not
                    case ',' -> top.filter = top.filter && top.parens > 0;
                    default -> {
                        // any other character leaves the context as it is
                    }
                }
            }
        }

        private Site site() {
            if (quote != 0) {
                return Site.LITERAL;
            }
            var top = frames.peek();
            boolean filter = top != null && top.filter;
            if (previous == '.' || (isNameChar(previous) && (top == null || filter))) {
                return Site.NAME;
            }
            if (top == null) {
                return null;
            }
            return filter ? Site.FILTER : Site.INDEX;
        }
    }
}
