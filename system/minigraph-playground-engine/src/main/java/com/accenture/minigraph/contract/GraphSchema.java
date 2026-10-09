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

package com.accenture.minigraph.contract;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The graph contract's schema vocabulary (RFC-0007, WP2): a closed subset of OpenAPI 3.0 keywords, compiled
 * from the {@code schema} property of a root or end node and, for the root, validated against a run's input.
 * <p>
 * The vocabulary is {@code type} (object, array, string, number, integer, boolean), {@code properties},
 * {@code required}, {@code items}, {@code enum}, {@code minimum}, {@code maximum}, {@code exclusiveMinimum},
 * {@code exclusiveMaximum} (booleans, as in OpenAPI 3.0), {@code minLength}, {@code maxLength}, {@code pattern},
 * {@code minItems}, {@code maxItems}, {@code nullable} and {@code additionalProperties}; {@code title},
 * {@code description}, {@code example} and {@code format} are documentary and never validated. Any other
 * keyword is refused by {@link #compile}, because a constraint the validator silently ignores teaches that
 * unflagged means safe. A keyword that cannot apply to the declared type is refused for the same reason.
 * <p>
 * Types are strict JSON types: a numeric string is not a number, a boolean is not a number, and an integer
 * is a number with no fractional part (so {@code 1.0} is one). A value the grammar stored as text - the
 * Playground writes every scalar property as text - is accepted where the schema expects a number or a
 * boolean ({@code minimum=0}, {@code nullable=true}), and an {@code enum} entry is read as the declared type.
 * A header is text: the header part's property schemas take the types string, number, integer and boolean,
 * and a number or boolean header validates the parsed text. A {@code pattern} is searched, not anchored, with
 * Unicode-aware character classes, and must stay inside the regular-expression subset common to both engines
 * (no lookaround, atomic group, backreference or possessive quantifier). Character counts are code points.
 * <p>
 * Every violation is collected - the names are the paths of the state machine ({@code input.body.items[2].sku},
 * {@code input.header.X-Api-Key}), a header name as declared - and {@link #report} renders them as one message
 * capped at {@link #VIOLATION_CAP}. The messages are pinned by the shared vector file
 * {@code graph-schema-vectors.json}, byte-identical in the Rust repository.
 */
public final class GraphSchema {
    /** The number of violations one message carries; the rest are counted. */
    public static final int VIOLATION_CAP = 10;
    public static final String BODY = "body";
    public static final String HEADER = "header";
    private static final String TYPE = "type";
    private static final String PROPERTIES = "properties";
    private static final String REQUIRED = "required";
    private static final String ITEMS = "items";
    private static final String ENUM = "enum";
    private static final String MINIMUM = "minimum";
    private static final String MAXIMUM = "maximum";
    private static final String EXCLUSIVE_MINIMUM = "exclusiveMinimum";
    private static final String EXCLUSIVE_MAXIMUM = "exclusiveMaximum";
    private static final String MIN_LENGTH = "minLength";
    private static final String MAX_LENGTH = "maxLength";
    private static final String PATTERN = "pattern";
    private static final String MIN_ITEMS = "minItems";
    private static final String MAX_ITEMS = "maxItems";
    private static final String NULLABLE = "nullable";
    private static final String ADDITIONAL_PROPERTIES = "additionalProperties";
    private static final String OBJECT = "object";
    private static final String ARRAY = "array";
    private static final String STRING = "string";
    private static final String NUMBER = "number";
    private static final String INTEGER = "integer";
    private static final String BOOLEAN = "boolean";
    private static final String NULL = "null";
    /** The vocabulary, in the order the error message lists it. */
    public static final List<String> KEYWORDS = List.of(TYPE, PROPERTIES, REQUIRED, ITEMS, ENUM, MINIMUM, MAXIMUM,
            EXCLUSIVE_MINIMUM, EXCLUSIVE_MAXIMUM, MIN_LENGTH, MAX_LENGTH, PATTERN, MIN_ITEMS, MAX_ITEMS, NULLABLE,
            ADDITIONAL_PROPERTIES);
    /** Keywords that document and never validate. */
    public static final List<String> DOCUMENTARY = List.of("title", "description", "example", "format");
    private static final List<String> TYPES = List.of(OBJECT, ARRAY, STRING, NUMBER, INTEGER, BOOLEAN);
    private static final List<String> HEADER_TYPES = List.of(STRING, NUMBER, INTEGER, BOOLEAN);
    private static final List<String> OBJECT_KEYWORDS = List.of(PROPERTIES, REQUIRED, ADDITIONAL_PROPERTIES);
    private static final List<String> ARRAY_KEYWORDS = List.of(ITEMS, MIN_ITEMS, MAX_ITEMS);
    private static final List<String> STRING_KEYWORDS = List.of(MIN_LENGTH, MAX_LENGTH, PATTERN);
    private static final List<String> NUMBER_KEYWORDS = List.of(MINIMUM, MAXIMUM, EXCLUSIVE_MINIMUM, EXCLUSIVE_MAXIMUM);
    /** The header part itself takes only these (plus the documentary keywords). */
    private static final List<String> HEADER_PART_KEYWORDS = List.of(TYPE, PROPERTIES, REQUIRED);
    /** A header property never takes these: a header is one text value. */
    private static final List<String> NOT_FOR_A_HEADER = List.of(PROPERTIES, REQUIRED, ITEMS, MIN_ITEMS, MAX_ITEMS,
            ADDITIONAL_PROPERTIES, NULLABLE);
    private static final String UNKNOWN_KEYWORD = ": unknown keyword '%s' - the vocabulary is " + String.join(", ",
            KEYWORDS.subList(0, KEYWORDS.size() - 1)) + " and " + KEYWORDS.getLast() + " (" + String.join(", ",
            DOCUMENTARY.subList(0, DOCUMENTARY.size() - 1)) + " and " + DOCUMENTARY.getLast() + " are documentary)";
    private static final String TYPE_NAMES = "object, array, string, number, integer or boolean";
    private static final String HEADER_TYPE_NAMES = "string, number, integer or boolean";

    private final String type;
    private final Map<String, GraphSchema> properties = new TreeMap<>();
    private final List<String> required = new ArrayList<>();
    private final GraphSchema items;
    private final List<Object> enumValues;
    private final BigDecimal minimum;
    private final BigDecimal maximum;
    private final boolean exclusiveMinimum;
    private final boolean exclusiveMaximum;
    private final Integer minLength;
    private final Integer maxLength;
    private final String patternText;
    private final Pattern pattern;
    private final Integer minItems;
    private final Integer maxItems;
    private final boolean nullable;
    private final boolean noAdditional;
    private final GraphSchema additional;

    /**
     * The two parts of a node's {@code schema} property, compiled.
     *
     * @param body the body part, or null when the node declares none
     * @param header the header part, or null when the node declares none
     */
    public record Contract(GraphSchema body, GraphSchema header) {
        /**
         * Validate a run's input against the declaration.
         *
         * @param body the state machine's input.body (null when absent)
         * @param header the state machine's input.header (null when absent)
         * @return every violation, in order; empty when the input passes
         */
        public List<String> check(Object body, Object header) {
            List<String> violations = new ArrayList<>();
            if (this.body != null) {
                this.body.validate("input." + BODY, body, violations);
            }
            if (this.header != null) {
                this.header.validateHeaders("input." + HEADER, header instanceof Map<?, ?> map ? map : Map.of(),
                        violations);
            }
            return violations;
        }
    }

    /**
     * Compile a node's {@code schema} property: an object with a {@code body} part and/or a {@code header} part.
     *
     * @param schema the property's value
     * @return the compiled parts
     * @throws IllegalArgumentException with the reason the declaration is refused, prefixed by its location
     */
    public static Contract compileContract(Object schema) {
        if (!(schema instanceof Map<?, ?> parts)) {
            throw new IllegalArgumentException("schema: must be an object with body and/or header");
        }
        for (var key : parts.keySet()) {
            var part = String.valueOf(key);
            if (!BODY.equals(part) && !HEADER.equals(part)) {
                throw new IllegalArgumentException("schema: unknown part '" + part + "' - use body or header");
            }
        }
        var body = parts.containsKey(BODY) ? compile("schema." + BODY, parts.get(BODY)) : null;
        var header = parts.containsKey(HEADER) ? compileHeaderPart("schema." + HEADER, parts.get(HEADER)) : null;
        return new Contract(body, header);
    }

    /**
     * Compile a schema object.
     *
     * @param where the location for error messages, e.g. {@code schema.body}
     * @param schema the schema object
     * @return the compiled schema
     * @throws IllegalArgumentException with the reason the schema is refused, prefixed by its location
     */
    public static GraphSchema compile(String where, Object schema) {
        return new GraphSchema(where, schema, Mode.BODY);
    }

    /**
     * Compile the header part: an object schema whose properties are header names.
     *
     * @param where the location for error messages, e.g. {@code schema.header}
     * @param schema the header part
     * @return the compiled part
     * @throws IllegalArgumentException with the reason the part is refused, prefixed by its location
     */
    public static GraphSchema compileHeaderPart(String where, Object schema) {
        return new GraphSchema(where, schema, Mode.HEADER_PART);
    }

    /**
     * Render the violations of one run as the validator's message.
     *
     * @param violations every violation found
     * @return {@code Input validation failed - a; b; ...}, capped at {@link #VIOLATION_CAP} with the rest counted
     */
    public static String report(List<String> violations) {
        var shown = violations.size() > VIOLATION_CAP ? violations.subList(0, VIOLATION_CAP) : violations;
        var sb = new StringBuilder("Input validation failed - ").append(String.join("; ", shown));
        if (violations.size() > VIOLATION_CAP) {
            sb.append("; and ").append(violations.size() - VIOLATION_CAP).append(" more");
        }
        return sb.toString();
    }

    private enum Mode { BODY, HEADER_PART, HEADER_PROPERTY }

    private GraphSchema(String where, Object schema, Mode mode) {
        if (!(schema instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(where + ": must be an object");
        }
        for (var key : map.keySet()) {
            var keyword = String.valueOf(key);
            if (!KEYWORDS.contains(keyword) && !DOCUMENTARY.contains(keyword)) {
                throw new IllegalArgumentException(where + String.format(UNKNOWN_KEYWORD, keyword));
            }
            if (mode == Mode.HEADER_PART && !HEADER_PART_KEYWORDS.contains(keyword) && !DOCUMENTARY.contains(keyword)) {
                throw new IllegalArgumentException(where + ": '" + keyword + "' does not apply to the header part");
            }
            if (mode == Mode.HEADER_PROPERTY && NOT_FOR_A_HEADER.contains(keyword)) {
                throw new IllegalArgumentException(where + ": '" + keyword + "' does not apply to a header");
            }
        }
        this.type = compileType(where, map, mode);
        if (type != null) {
            for (var key : map.keySet()) {
                var keyword = String.valueOf(key);
                if (!appliesTo(keyword, type)) {
                    throw new IllegalArgumentException(where + ": '" + keyword + "' does not apply to type " + type);
                }
            }
        }
        if (map.containsKey(PROPERTIES)) {
            if (!(map.get(PROPERTIES) instanceof Map<?, ?> props)) {
                throw new IllegalArgumentException(where + "." + PROPERTIES + ": must be an object of schemas");
            }
            var childMode = mode == Mode.HEADER_PART ? Mode.HEADER_PROPERTY : Mode.BODY;
            for (var kv : props.entrySet()) {
                var name = String.valueOf(kv.getKey());
                properties.put(name, new GraphSchema(where + "." + PROPERTIES + "." + name, kv.getValue(), childMode));
            }
        }
        if (map.containsKey(REQUIRED)) {
            if (!(map.get(REQUIRED) instanceof List<?> names)) {
                throw new IllegalArgumentException(where + "." + REQUIRED + ": must be a list of property names");
            }
            for (var name : names) {
                if (!(name instanceof String text) || text.isBlank()) {
                    throw new IllegalArgumentException(where + "." + REQUIRED + ": must be a list of property names");
                }
                required.add(text);
            }
        }
        this.items = map.containsKey(ITEMS) ? new GraphSchema(where + "." + ITEMS, map.get(ITEMS), Mode.BODY) : null;
        this.enumValues = map.containsKey(ENUM) ? compileEnum(where, map.get(ENUM), type) : null;
        this.minimum = map.containsKey(MINIMUM) ? number(where, MINIMUM, map.get(MINIMUM)) : null;
        this.maximum = map.containsKey(MAXIMUM) ? number(where, MAXIMUM, map.get(MAXIMUM)) : null;
        this.exclusiveMinimum = map.containsKey(EXCLUSIVE_MINIMUM) && flag(where, EXCLUSIVE_MINIMUM, map.get(EXCLUSIVE_MINIMUM));
        this.exclusiveMaximum = map.containsKey(EXCLUSIVE_MAXIMUM) && flag(where, EXCLUSIVE_MAXIMUM, map.get(EXCLUSIVE_MAXIMUM));
        if (map.containsKey(EXCLUSIVE_MINIMUM) && minimum == null) {
            throw new IllegalArgumentException(where + "." + EXCLUSIVE_MINIMUM + ": needs " + MINIMUM);
        }
        if (map.containsKey(EXCLUSIVE_MAXIMUM) && maximum == null) {
            throw new IllegalArgumentException(where + "." + EXCLUSIVE_MAXIMUM + ": needs " + MAXIMUM);
        }
        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(where + ": " + MINIMUM + " is greater than " + MAXIMUM);
        }
        this.minLength = map.containsKey(MIN_LENGTH) ? count(where, MIN_LENGTH, map.get(MIN_LENGTH)) : null;
        this.maxLength = map.containsKey(MAX_LENGTH) ? count(where, MAX_LENGTH, map.get(MAX_LENGTH)) : null;
        if (minLength != null && maxLength != null && minLength > maxLength) {
            throw new IllegalArgumentException(where + ": " + MIN_LENGTH + " is greater than " + MAX_LENGTH);
        }
        this.minItems = map.containsKey(MIN_ITEMS) ? count(where, MIN_ITEMS, map.get(MIN_ITEMS)) : null;
        this.maxItems = map.containsKey(MAX_ITEMS) ? count(where, MAX_ITEMS, map.get(MAX_ITEMS)) : null;
        if (minItems != null && maxItems != null && minItems > maxItems) {
            throw new IllegalArgumentException(where + ": " + MIN_ITEMS + " is greater than " + MAX_ITEMS);
        }
        if (map.containsKey(PATTERN)) {
            if (!(map.get(PATTERN) instanceof String text)) {
                throw new IllegalArgumentException(where + "." + PATTERN + ": must be text");
            }
            this.patternText = text;
            this.pattern = compilePattern(where, text);
        } else {
            this.patternText = null;
            this.pattern = null;
        }
        this.nullable = map.containsKey(NULLABLE) && flag(where, NULLABLE, map.get(NULLABLE));
        var extras = map.get(ADDITIONAL_PROPERTIES);
        if (extras == null) {
            this.noAdditional = false;
            this.additional = null;
        } else if (extras instanceof Map<?, ?>) {
            this.noAdditional = false;
            this.additional = new GraphSchema(where + "." + ADDITIONAL_PROPERTIES, extras, Mode.BODY);
        } else if (isFlag(extras)) {
            this.noAdditional = !flag(where, ADDITIONAL_PROPERTIES, extras);
            this.additional = null;
        } else {
            throw new IllegalArgumentException(where + "." + ADDITIONAL_PROPERTIES + ": must be true, false or a schema");
        }
    }

    private static String compileType(String where, Map<?, ?> map, Mode mode) {
        if (!map.containsKey(TYPE)) {
            return null;
        }
        var names = mode == Mode.HEADER_PROPERTY ? HEADER_TYPE_NAMES : TYPE_NAMES;
        if (!(map.get(TYPE) instanceof String text)) {
            throw new IllegalArgumentException(where + "." + TYPE + ": must be a type name - " + names);
        }
        if (mode == Mode.HEADER_PART) {
            if (!OBJECT.equals(text)) {
                throw new IllegalArgumentException(where + "." + TYPE + ": must be object");
            }
            return text;
        }
        if (mode == Mode.HEADER_PROPERTY && !HEADER_TYPES.contains(text)) {
            throw new IllegalArgumentException(where + "." + TYPE + ": unknown type '" + text +
                    "' - a header is text: use " + names);
        }
        if (!TYPES.contains(text)) {
            throw new IllegalArgumentException(where + "." + TYPE + ": unknown type '" + text + "' - use " + names);
        }
        return text;
    }

    private static boolean appliesTo(String keyword, String type) {
        if (OBJECT_KEYWORDS.contains(keyword)) {
            return OBJECT.equals(type);
        }
        if (ARRAY_KEYWORDS.contains(keyword)) {
            return ARRAY.equals(type);
        }
        if (STRING_KEYWORDS.contains(keyword)) {
            return STRING.equals(type);
        }
        if (NUMBER_KEYWORDS.contains(keyword)) {
            return NUMBER.equals(type) || INTEGER.equals(type);
        }
        return true;
    }

    private static List<Object> compileEnum(String where, Object value, String type) {
        if (!(value instanceof List<?> entries) || entries.isEmpty()) {
            throw new IllegalArgumentException(where + "." + ENUM + ": must be a non-empty list");
        }
        List<Object> result = new ArrayList<>();
        for (var entry : entries) {
            if (NUMBER.equals(type) || INTEGER.equals(type)) {
                var n = toNumber(entry);
                if (n == null || (INTEGER.equals(type) && !isIntegral(n))) {
                    throw new IllegalArgumentException(where + "." + ENUM + ": '" + entry + "' is not " +
                            (INTEGER.equals(type) ? "an integer" : "a number"));
                }
                result.add(n);
            } else if (BOOLEAN.equals(type)) {
                if (!isFlag(entry)) {
                    throw new IllegalArgumentException(where + "." + ENUM + ": '" + entry + "' is not a boolean");
                }
                result.add(Boolean.parseBoolean(String.valueOf(entry)));
            } else if (STRING.equals(type)) {
                result.add(String.valueOf(entry));
            } else {
                result.add(entry);
            }
        }
        return result;
    }

    private static BigDecimal number(String where, String keyword, Object value) {
        var n = toNumber(value);
        if (n == null) {
            throw new IllegalArgumentException(where + "." + keyword + ": must be a number");
        }
        return n;
    }

    private static int count(String where, String keyword, Object value) {
        var n = toNumber(value);
        if (n == null || !isIntegral(n) || n.signum() < 0 || n.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException(where + "." + keyword + ": must be a non-negative integer");
        }
        return n.intValue();
    }

    private static boolean isFlag(Object value) {
        return value instanceof Boolean || "true".equals(value) || "false".equals(value);
    }

    private static boolean flag(String where, String keyword, Object value) {
        if (!isFlag(value)) {
            throw new IllegalArgumentException(where + "." + keyword + ": must be true or false");
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * A number or numeric text as an exact decimal; null for anything else, a boolean included.
     * A non-finite floating-point value is not a number.
     */
    static BigDecimal toNumber(Object value) {
        if (value instanceof BigDecimal d) {
            return d;
        }
        if (value instanceof Double || value instanceof Float) {
            var d = ((Number) value).doubleValue();
            return Double.isFinite(d) ? new BigDecimal(Double.toString(d)) : null;
        }
        if (value instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        if (value instanceof String text) {
            try {
                var t = text.trim();
                return t.isEmpty() ? null : new BigDecimal(t);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    static boolean isIntegral(BigDecimal n) {
        return n.stripTrailingZeros().scale() <= 0;
    }

    private static Pattern compilePattern(String where, String text) {
        if (outsideCommonSubset(text)) {
            throw new IllegalArgumentException(where + "." + PATTERN + ": '" + text + "' uses lookaround, an atomic " +
                    "group, a backreference or a possessive quantifier - outside the regex subset common to both engines");
        }
        try {
            return Pattern.compile(text, Pattern.UNICODE_CHARACTER_CLASS);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException(where + "." + PATTERN + ": '" + text + "' is not a valid regular expression");
        }
    }

    /**
     * Lookaround, atomic groups, backreferences and possessive quantifiers are Java-only constructs: the Rust
     * engine's regex crate has none of them, so a pattern that uses one is refused on both engines by this
     * textual check (escapes and character classes are skipped).
     */
    static boolean outsideCommonSubset(String p) {
        boolean inClass = false;
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            if (c == '\\') {
                if (i + 1 < p.length()) {
                    char next = p.charAt(i + 1);
                    if (!inClass && ((next >= '1' && next <= '9') || next == 'k')) {
                        return true;
                    }
                    i++;
                }
                continue;
            }
            if (inClass) {
                if (c == ']') {
                    inClass = false;
                }
                continue;
            }
            if (c == '[') {
                inClass = true;
            } else if (c == '(' && p.startsWith("(?", i)) {
                var rest = p.substring(i + 2);
                if (rest.startsWith("=") || rest.startsWith("!") || rest.startsWith("<=") || rest.startsWith("<!")
                        || rest.startsWith(">")) {
                    return true;
                }
            } else if (c == '+' && i > 0) {
                char previous = p.charAt(i - 1);
                if ((previous == '*' || previous == '+' || previous == '?' || previous == '}') && !escaped(p, i - 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean escaped(String p, int index) {
        int backslashes = 0;
        for (int i = index - 1; i >= 0 && p.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return backslashes % 2 == 1;
    }

    // ------------------------------------------------------------------ validation

    /**
     * Validate a value against this schema, collecting every violation.
     *
     * @param path the value's path, e.g. {@code input.body}
     * @param value the value (null when absent)
     * @param violations the collector
     */
    public void validate(String path, Object value, List<String> violations) {
        if (value == null) {
            if (!nullable && type != null) {
                violations.add(path + ": expected " + type + ", got " + NULL);
            }
            return;
        }
        var actual = kindOf(value);
        if (type != null && !matches(type, value, actual)) {
            violations.add(path + ": expected " + type + ", got " + actual);
            return;
        }
        if (enumValues != null && !inEnum(value, actual)) {
            violations.add(path + ": must be one of " + renderEnum());
        }
        switch (actual) {
            case NUMBER -> checkBounds(path, toNumber(value), violations);
            case STRING -> checkText(path, value instanceof String text ? text : String.valueOf(value), violations);
            case ARRAY -> checkArray(path, (List<?>) value, violations);
            case OBJECT -> checkObject(path, (Map<?, ?>) value, violations);
            default -> { }
        }
    }

    /**
     * Validate a run's headers against this header part: the required names must be present and each declared
     * header's text must satisfy its schema. Names match case-insensitively.
     *
     * @param path the headers' path, {@code input.header}
     * @param headers the header map
     * @param violations the collector
     */
    public void validateHeaders(String path, Map<?, ?> headers, List<String> violations) {
        for (var name : required) {
            if (lookup(headers, name) == null) {
                violations.add(path + "." + name + ": required");
            }
        }
        for (var kv : properties.entrySet()) {
            var text = lookup(headers, kv.getKey());
            if (text != null) {
                kv.getValue().validateHeader(path + "." + kv.getKey(), text, violations);
            }
        }
    }

    private static String lookup(Map<?, ?> headers, String name) {
        for (var kv : headers.entrySet()) {
            if (name.equalsIgnoreCase(String.valueOf(kv.getKey())) && kv.getValue() != null) {
                return String.valueOf(kv.getValue());
            }
        }
        return null;
    }

    private void validateHeader(String path, String text, List<String> violations) {
        if (NUMBER.equals(type) || INTEGER.equals(type)) {
            var n = toNumber(text);
            if (n == null || (INTEGER.equals(type) && !isIntegral(n))) {
                violations.add(path + ": expected " + type);
                return;
            }
            if (enumValues != null && !inEnum(n, NUMBER)) {
                violations.add(path + ": must be one of " + renderEnum());
            }
            checkBounds(path, n, violations);
        } else if (BOOLEAN.equals(type)) {
            if (!text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false")) {
                violations.add(path + ": expected " + type);
                return;
            }
            if (enumValues != null && !inEnum(Boolean.parseBoolean(text.toLowerCase()), BOOLEAN)) {
                violations.add(path + ": must be one of " + renderEnum());
            }
        } else {
            if (enumValues != null && !inEnum(text, STRING)) {
                violations.add(path + ": must be one of " + renderEnum());
            }
            checkText(path, text, violations);
        }
    }

    private static String kindOf(Object value) {
        if (value instanceof Map) {
            return OBJECT;
        }
        if (value instanceof List) {
            return ARRAY;
        }
        if (value instanceof Boolean) {
            return BOOLEAN;
        }
        if (value instanceof Number) {
            return NUMBER;
        }
        return STRING;
    }

    private static boolean matches(String type, Object value, String actual) {
        if (INTEGER.equals(type)) {
            if (!NUMBER.equals(actual)) {
                return false;
            }
            var n = toNumber(value);
            return n != null && isIntegral(n);
        }
        return type.equals(actual);
    }

    private boolean inEnum(Object value, String actual) {
        for (var entry : enumValues) {
            if (sameValue(entry, value, actual)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameValue(Object entry, Object value, String actual) {
        return switch (actual) {
            case NUMBER -> {
                var a = toNumber(entry);
                var b = toNumber(value);
                yield a != null && b != null && !(entry instanceof String) && a.compareTo(b) == 0;
            }
            case STRING -> entry instanceof String text && text.equals(value);
            case BOOLEAN -> entry instanceof Boolean flag && flag.equals(value);
            default -> entry != null && entry.equals(value);
        };
    }

    private String renderEnum() {
        List<String> names = new ArrayList<>();
        for (var entry : enumValues) {
            names.add(entry instanceof BigDecimal n ? renderNumber(n) : String.valueOf(entry));
        }
        return String.join(", ", names);
    }

    /** A number rendered as both engines render it: plain notation, no trailing zeros. */
    static String renderNumber(BigDecimal n) {
        return n.stripTrailingZeros().toPlainString();
    }

    private void checkBounds(String path, BigDecimal n, List<String> violations) {
        if (n == null) {
            return;
        }
        if (minimum != null) {
            var cmp = n.compareTo(minimum);
            if (exclusiveMinimum ? cmp <= 0 : cmp < 0) {
                violations.add(path + ": must be " + (exclusiveMinimum ? "more than " : "at least ") + renderNumber(minimum));
            }
        }
        if (maximum != null) {
            var cmp = n.compareTo(maximum);
            if (exclusiveMaximum ? cmp >= 0 : cmp > 0) {
                violations.add(path + ": must be " + (exclusiveMaximum ? "less than " : "at most ") + renderNumber(maximum));
            }
        }
    }

    private void checkText(String path, String text, List<String> violations) {
        var length = text.codePointCount(0, text.length());
        if (minLength != null && length < minLength) {
            violations.add(path + ": must be at least " + plural(minLength, "character"));
        }
        if (maxLength != null && length > maxLength) {
            violations.add(path + ": must be at most " + plural(maxLength, "character"));
        }
        if (pattern != null && !pattern.matcher(text).find()) {
            violations.add(path + ": does not match pattern " + patternText);
        }
    }

    private void checkArray(String path, List<?> list, List<String> violations) {
        if (minItems != null && list.size() < minItems) {
            violations.add(path + ": must have at least " + plural(minItems, "item"));
        }
        if (maxItems != null && list.size() > maxItems) {
            violations.add(path + ": must have at most " + plural(maxItems, "item"));
        }
        if (items != null) {
            for (int i = 0; i < list.size(); i++) {
                items.validate(path + "[" + i + "]", list.get(i), violations);
            }
        }
    }

    private void checkObject(String path, Map<?, ?> map, List<String> violations) {
        for (var name : required) {
            if (!map.containsKey(name)) {
                violations.add(path + "." + name + ": required");
            }
        }
        for (var kv : properties.entrySet()) {
            if (map.containsKey(kv.getKey())) {
                kv.getValue().validate(path + "." + kv.getKey(), map.get(kv.getKey()), violations);
            }
        }
        if (noAdditional || additional != null) {
            var extras = new TreeSet<String>();
            for (var key : map.keySet()) {
                var name = String.valueOf(key);
                if (!properties.containsKey(name)) {
                    extras.add(name);
                }
            }
            for (var name : extras) {
                if (noAdditional) {
                    violations.add(path + "." + name + ": not allowed");
                } else {
                    additional.validate(path + "." + name, map.get(name), violations);
                }
            }
        }
    }

    private static String plural(int n, String noun) {
        return n + " " + noun + (n == 1 ? "" : "s");
    }
}
