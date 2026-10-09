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

    private static final String EXPECTED = ": expected ";
    private static final String MUST_BE_ONE_OF = ": must be one of ";
    private static final String MUST_BE = ": must be ";

    private final String typeName;
    private final Map<String, GraphSchema> propertySchemas = new TreeMap<>();
    private final List<String> requiredNames = new ArrayList<>();
    private final GraphSchema itemSchema;
    private final List<Object> enumValues;
    private final BigDecimal lowerBound;
    private final BigDecimal upperBound;
    private final boolean exclusiveMinimum;
    private final boolean exclusiveMaximum;
    private final Integer minLength;
    private final Integer maxLength;
    private final String patternText;
    private final Pattern compiledPattern;
    private final Integer minItems;
    private final Integer maxItems;
    private final boolean allowsNull;
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

    /**
     * Compile one schema object. The checks run in a fixed order, so a schema with several faults is
     * refused for the same one on both engines: the keywords, the type, the keywords' applicability to
     * the type, then each keyword's value in vocabulary order.
     */
    private GraphSchema(String where, Object schema, Mode mode) {
        if (!(schema instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(where + ": must be an object");
        }
        checkKeywords(where, map, mode);
        this.typeName = compileType(where, map, mode);
        checkApplicability(where, map, typeName);
        compileProperties(where, map, mode);
        compileRequired(where, map);
        this.itemSchema = map.containsKey(ITEMS) ? new GraphSchema(where + "." + ITEMS, map.get(ITEMS), Mode.BODY) : null;
        this.enumValues = map.containsKey(ENUM) ? compileEnum(where, map.get(ENUM), typeName) : null;
        this.lowerBound = optionalNumber(where, MINIMUM, map);
        this.upperBound = optionalNumber(where, MAXIMUM, map);
        this.exclusiveMinimum = optionalFlag(where, EXCLUSIVE_MINIMUM, map);
        this.exclusiveMaximum = optionalFlag(where, EXCLUSIVE_MAXIMUM, map);
        checkBoundsDeclaration(where, map);
        this.minLength = optionalCount(where, MIN_LENGTH, map);
        this.maxLength = optionalCount(where, MAX_LENGTH, map);
        checkOrder(where, MIN_LENGTH, minLength, MAX_LENGTH, maxLength);
        this.minItems = optionalCount(where, MIN_ITEMS, map);
        this.maxItems = optionalCount(where, MAX_ITEMS, map);
        checkOrder(where, MIN_ITEMS, minItems, MAX_ITEMS, maxItems);
        this.patternText = optionalPatternText(where, map);
        this.compiledPattern = patternText == null ? null : compilePattern(where, patternText);
        this.allowsNull = optionalFlag(where, NULLABLE, map);
        var extras = map.get(ADDITIONAL_PROPERTIES);
        this.additional = extras instanceof Map<?, ?>
                ? new GraphSchema(where + "." + ADDITIONAL_PROPERTIES, extras, Mode.BODY) : null;
        this.noAdditional = additionalRefused(where, extras);
    }

    /** Every key is a keyword of the vocabulary, and one the part or the header takes. */
    private static void checkKeywords(String where, Map<?, ?> map, Mode mode) {
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
    }

    /** With a declared type, every keyword must apply to it. */
    private static void checkApplicability(String where, Map<?, ?> map, String type) {
        if (type == null) {
            return;
        }
        for (var key : map.keySet()) {
            var keyword = String.valueOf(key);
            if (!appliesTo(keyword, type)) {
                throw new IllegalArgumentException(where + ": '" + keyword + "' does not apply to type " + type);
            }
        }
    }

    private void compileProperties(String where, Map<?, ?> map, Mode mode) {
        if (!map.containsKey(PROPERTIES)) {
            return;
        }
        if (!(map.get(PROPERTIES) instanceof Map<?, ?> props)) {
            throw new IllegalArgumentException(where + "." + PROPERTIES + ": must be an object of schemas");
        }
        var childMode = mode == Mode.HEADER_PART ? Mode.HEADER_PROPERTY : Mode.BODY;
        for (var kv : props.entrySet()) {
            var name = String.valueOf(kv.getKey());
            propertySchemas.put(name, new GraphSchema(where + "." + PROPERTIES + "." + name, kv.getValue(), childMode));
        }
    }

    private void compileRequired(String where, Map<?, ?> map) {
        if (!map.containsKey(REQUIRED)) {
            return;
        }
        if (!(map.get(REQUIRED) instanceof List<?> names)) {
            throw new IllegalArgumentException(where + "." + REQUIRED + ": must be a list of property names");
        }
        for (var name : names) {
            if (!(name instanceof String text) || text.isBlank()) {
                throw new IllegalArgumentException(where + "." + REQUIRED + ": must be a list of property names");
            }
            requiredNames.add(text);
        }
    }

    /** An exclusive flag needs its bound, and the lower bound may not exceed the upper. */
    private void checkBoundsDeclaration(String where, Map<?, ?> map) {
        if (map.containsKey(EXCLUSIVE_MINIMUM) && lowerBound == null) {
            throw new IllegalArgumentException(where + "." + EXCLUSIVE_MINIMUM + ": needs " + MINIMUM);
        }
        if (map.containsKey(EXCLUSIVE_MAXIMUM) && upperBound == null) {
            throw new IllegalArgumentException(where + "." + EXCLUSIVE_MAXIMUM + ": needs " + MAXIMUM);
        }
        if (lowerBound != null && upperBound != null && lowerBound.compareTo(upperBound) > 0) {
            throw new IllegalArgumentException(where + ": " + MINIMUM + " is greater than " + MAXIMUM);
        }
    }

    private static void checkOrder(String where, String minKeyword, Integer min, String maxKeyword, Integer max) {
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException(where + ": " + minKeyword + " is greater than " + maxKeyword);
        }
    }

    private static BigDecimal optionalNumber(String where, String keyword, Map<?, ?> map) {
        return map.containsKey(keyword) ? number(where, keyword, map.get(keyword)) : null;
    }

    private static Integer optionalCount(String where, String keyword, Map<?, ?> map) {
        return map.containsKey(keyword) ? count(where, keyword, map.get(keyword)) : null;
    }

    private static boolean optionalFlag(String where, String keyword, Map<?, ?> map) {
        return map.containsKey(keyword) && flag(where, keyword, map.get(keyword));
    }

    private static String optionalPatternText(String where, Map<?, ?> map) {
        if (!map.containsKey(PATTERN)) {
            return null;
        }
        if (!(map.get(PATTERN) instanceof String text)) {
            throw new IllegalArgumentException(where + "." + PATTERN + ": must be text");
        }
        return text;
    }

    /** {@code additionalProperties: false} refuses extras; true, absent or a schema does not. */
    private static boolean additionalRefused(String where, Object extras) {
        if (extras == null || extras instanceof Map<?, ?>) {
            return false;
        }
        if (isFlag(extras)) {
            return !flag(where, ADDITIONAL_PROPERTIES, extras);
        }
        throw new IllegalArgumentException(where + "." + ADDITIONAL_PROPERTIES + ": must be true, false or a schema");
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
            result.add(enumEntry(where, entry, type));
        }
        return result;
    }

    /** An enum entry read as the declared type; an untyped schema keeps the entry as written. */
    private static Object enumEntry(String where, Object entry, String type) {
        if (type == null) {
            return entry;
        }
        return switch (type) {
            case NUMBER, INTEGER -> {
                var n = toNumber(entry);
                if (n == null || (INTEGER.equals(type) && !isIntegral(n))) {
                    throw new IllegalArgumentException(where + "." + ENUM + ": '" + entry + "' is not " +
                            (INTEGER.equals(type) ? "an integer" : "a number"));
                }
                yield n;
            }
            case BOOLEAN -> {
                if (!isFlag(entry)) {
                    throw new IllegalArgumentException(where + "." + ENUM + ": '" + entry + "' is not a boolean");
                }
                yield Boolean.parseBoolean(String.valueOf(entry));
            }
            case STRING -> String.valueOf(entry);
            default -> entry;
        };
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
        int i = 0;
        while (i < p.length()) {
            char c = p.charAt(i);
            if (c == '\\') {
                if (!inClass && isBackreference(p, i)) {
                    return true;
                }
                // the escape and the character it escapes
                i += 2;
            } else {
                if (inClass) {
                    inClass = c != ']';
                } else if (c == '[') {
                    inClass = true;
                } else if (isJavaOnlyGroup(p, i) || isPossessive(p, i)) {
                    return true;
                }
                i++;
            }
        }
        return false;
    }

    /** The backslash at {@code i} starts {@code \1}..{@code \9} or {@code \k<name>}. */
    private static boolean isBackreference(String p, int i) {
        if (i + 1 >= p.length()) {
            return false;
        }
        char next = p.charAt(i + 1);
        return (next >= '1' && next <= '9') || next == 'k';
    }

    /** The group opening at {@code i} is a lookaround or an atomic group. */
    private static boolean isJavaOnlyGroup(String p, int i) {
        if (!p.startsWith("(?", i)) {
            return false;
        }
        var rest = p.substring(i + 2);
        return rest.startsWith("=") || rest.startsWith("!") || rest.startsWith("<=") || rest.startsWith("<!")
                || rest.startsWith(">");
    }

    /** The {@code +} at {@code i} follows an unescaped quantifier, making it possessive. */
    private static boolean isPossessive(String p, int i) {
        if (p.charAt(i) != '+' || i == 0) {
            return false;
        }
        char previous = p.charAt(i - 1);
        return (previous == '*' || previous == '+' || previous == '?' || previous == '}') && !escaped(p, i - 1);
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
            if (!allowsNull && typeName != null) {
                violations.add(path + EXPECTED + typeName + ", got " + NULL);
            }
            return;
        }
        var actual = kindOf(value);
        if (typeName != null && !matches(typeName, value, actual)) {
            violations.add(path + EXPECTED + typeName + ", got " + actual);
            return;
        }
        checkEnum(path, value, actual, violations);
        switch (actual) {
            case NUMBER -> checkBounds(path, toNumber(value), violations);
            case STRING -> checkText(path, value instanceof String text ? text : String.valueOf(value), violations);
            case ARRAY -> checkArray(path, (List<?>) value, violations);
            case OBJECT -> checkObject(path, (Map<?, ?>) value, violations);
            default -> {
                // a boolean has no constraint beyond its type and enum
            }
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
        for (var name : requiredNames) {
            if (lookup(headers, name) == null) {
                violations.add(path + "." + name + ": required");
            }
        }
        for (var kv : propertySchemas.entrySet()) {
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

    /** A header is text: a number or boolean type validates the parsed text. */
    private void validateHeader(String path, String text, List<String> violations) {
        if (NUMBER.equals(typeName) || INTEGER.equals(typeName)) {
            validateNumericHeader(path, text, violations);
        } else if (BOOLEAN.equals(typeName)) {
            validateBooleanHeader(path, text, violations);
        } else {
            checkEnum(path, text, STRING, violations);
            checkText(path, text, violations);
        }
    }

    private void validateNumericHeader(String path, String text, List<String> violations) {
        var n = toNumber(text);
        if (n == null || (INTEGER.equals(typeName) && !isIntegral(n))) {
            violations.add(path + EXPECTED + typeName);
            return;
        }
        checkEnum(path, n, NUMBER, violations);
        checkBounds(path, n, violations);
    }

    private void validateBooleanHeader(String path, String text, List<String> violations) {
        if (!text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false")) {
            violations.add(path + EXPECTED + typeName);
            return;
        }
        checkEnum(path, Boolean.parseBoolean(text.toLowerCase()), BOOLEAN, violations);
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

    /** With an {@code enum}, the value must be one of its entries, compared as the actual kind. */
    private void checkEnum(String path, Object value, String actual, List<String> violations) {
        if (enumValues != null && outsideEnum(value, actual)) {
            violations.add(path + MUST_BE_ONE_OF + renderEnum());
        }
    }

    private boolean outsideEnum(Object value, String actual) {
        for (var entry : enumValues) {
            if (sameValue(entry, value, actual)) {
                return false;
            }
        }
        return true;
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
        if (lowerBound != null) {
            var cmp = n.compareTo(lowerBound);
            if (exclusiveMinimum ? cmp <= 0 : cmp < 0) {
                violations.add(path + MUST_BE + (exclusiveMinimum ? "more than " : "at least ") + renderNumber(lowerBound));
            }
        }
        if (upperBound != null) {
            var cmp = n.compareTo(upperBound);
            if (exclusiveMaximum ? cmp >= 0 : cmp > 0) {
                violations.add(path + MUST_BE + (exclusiveMaximum ? "less than " : "at most ") + renderNumber(upperBound));
            }
        }
    }

    private void checkText(String path, String text, List<String> violations) {
        var length = text.codePointCount(0, text.length());
        if (minLength != null && length < minLength) {
            violations.add(path + MUST_BE + "at least " + plural(minLength, "character"));
        }
        if (maxLength != null && length > maxLength) {
            violations.add(path + MUST_BE + "at most " + plural(maxLength, "character"));
        }
        if (compiledPattern != null && !compiledPattern.matcher(text).find()) {
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
        if (itemSchema != null) {
            for (int i = 0; i < list.size(); i++) {
                itemSchema.validate(path + "[" + i + "]", list.get(i), violations);
            }
        }
    }

    private void checkObject(String path, Map<?, ?> map, List<String> violations) {
        for (var name : requiredNames) {
            if (!map.containsKey(name)) {
                violations.add(path + "." + name + ": required");
            }
        }
        for (var kv : propertySchemas.entrySet()) {
            if (map.containsKey(kv.getKey())) {
                kv.getValue().validate(path + "." + kv.getKey(), map.get(kv.getKey()), violations);
            }
        }
        if (noAdditional || additional != null) {
            checkExtras(path, map, violations);
        }
    }

    /** The keys the schema does not declare: refused, or validated against the extras' schema. */
    private void checkExtras(String path, Map<?, ?> map, List<String> violations) {
        var extras = new TreeSet<String>();
        for (var key : map.keySet()) {
            var name = String.valueOf(key);
            if (!propertySchemas.containsKey(name)) {
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

    private static String plural(int n, String noun) {
        return n + " " + noun + (n == 1 ? "" : "s");
    }
}
