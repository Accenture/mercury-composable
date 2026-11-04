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

package org.platformlambda.agentloop;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A tool's input schema (RFC-0011, section 5): a closed subset of JSON Schema, compiled from a catalog row's
 * {@code input_schema} before the first model turn, and used to validate each input the model fills in before
 * the tool's route is called.
 * <p>
 * The vocabulary is {@code type} (a name, or a list of names, from object, array, string, number, integer,
 * boolean and null), {@code properties}, {@code required}, {@code additionalProperties} (a boolean or a schema),
 * {@code items}, {@code enum}, {@code const}, {@code minimum}, {@code maximum}, {@code exclusiveMinimum},
 * {@code exclusiveMaximum} (numbers, as in JSON Schema), {@code minLength}, {@code maxLength}, {@code pattern},
 * {@code minItems} and {@code maxItems}; {@code title}, {@code description}, {@code default}, {@code examples}
 * and {@code format} are documentary and never validated. Any other keyword is refused, because a constraint
 * the loop silently ignores teaches that unflagged means safe; so is a keyword that cannot apply to any of the
 * declared types. This is the JSON Schema dialect the model is given, not the OpenAPI 3.0 subset of the graph
 * contract.
 * <p>
 * Types are strict JSON types: a numeric string is not a number, a boolean is not a number, and an integer is a
 * number with no fractional part (so {@code 1.0} is one). A {@code pattern} is searched, not anchored, with
 * Unicode-aware classes, and stays inside the regular-expression subset common to both engines. Lengths count
 * code points. The messages are pinned by the shared vector file {@code llm-run-vectors.json}.
 */
public final class ToolSchema {
    /** The number of violations one message carries; the rest are counted. */
    public static final int VIOLATION_CAP = 10;
    private static final String TYPE = "type";
    private static final String PROPERTIES = "properties";
    private static final String REQUIRED = "required";
    private static final String ADDITIONAL_PROPERTIES = "additionalProperties";
    private static final String ITEMS = "items";
    private static final String ENUM = "enum";
    private static final String CONST = "const";
    private static final String MINIMUM = "minimum";
    private static final String MAXIMUM = "maximum";
    private static final String EXCLUSIVE_MINIMUM = "exclusiveMinimum";
    private static final String EXCLUSIVE_MAXIMUM = "exclusiveMaximum";
    private static final String MIN_LENGTH = "minLength";
    private static final String MAX_LENGTH = "maxLength";
    private static final String PATTERN = "pattern";
    private static final String MIN_ITEMS = "minItems";
    private static final String MAX_ITEMS = "maxItems";
    static final String OBJECT = "object";
    private static final String ARRAY = "array";
    private static final String STRING = "string";
    private static final String NUMBER = "number";
    private static final String INTEGER = "integer";
    private static final String BOOLEAN = "boolean";
    private static final String NULL = "null";
    /** The vocabulary, in the order the error message lists it. */
    public static final List<String> KEYWORDS = List.of(TYPE, PROPERTIES, REQUIRED, ADDITIONAL_PROPERTIES, ITEMS,
            ENUM, CONST, MINIMUM, MAXIMUM, EXCLUSIVE_MINIMUM, EXCLUSIVE_MAXIMUM, MIN_LENGTH, MAX_LENGTH, PATTERN,
            MIN_ITEMS, MAX_ITEMS);
    /** Keywords that document and never validate. */
    public static final List<String> DOCUMENTARY = List.of("title", "description", "default", "examples", "format");
    private static final List<String> KNOWN_TYPES = List.of(OBJECT, ARRAY, STRING, NUMBER, INTEGER, BOOLEAN, NULL);
    /** The types a type-specific keyword applies to. */
    private static final Map<String, List<String>> APPLIES_TO = Map.ofEntries(
            Map.entry(PROPERTIES, List.of(OBJECT)), Map.entry(REQUIRED, List.of(OBJECT)),
            Map.entry(ADDITIONAL_PROPERTIES, List.of(OBJECT)), Map.entry(ITEMS, List.of(ARRAY)),
            Map.entry(MIN_ITEMS, List.of(ARRAY)), Map.entry(MAX_ITEMS, List.of(ARRAY)),
            Map.entry(MIN_LENGTH, List.of(STRING)), Map.entry(MAX_LENGTH, List.of(STRING)),
            Map.entry(PATTERN, List.of(STRING)), Map.entry(MINIMUM, List.of(NUMBER, INTEGER)),
            Map.entry(MAXIMUM, List.of(NUMBER, INTEGER)), Map.entry(EXCLUSIVE_MINIMUM, List.of(NUMBER, INTEGER)),
            Map.entry(EXCLUSIVE_MAXIMUM, List.of(NUMBER, INTEGER)));
    private static final String UNKNOWN_KEYWORD = ": unknown keyword '%s' - the vocabulary is " +
            String.join(", ", KEYWORDS.subList(0, KEYWORDS.size() - 1)) + " and " + KEYWORDS.getLast() + " (" +
            String.join(", ", DOCUMENTARY.subList(0, DOCUMENTARY.size() - 1)) + " and " + DOCUMENTARY.getLast() +
            " are documentary)";
    private static final String TYPE_NAMES = "object, array, string, number, integer, boolean or null";
    private static final String EXPECTED = ": expected ";
    private static final String MUST_BE = ": must be ";

    private final List<String> types;
    private final Map<String, ToolSchema> propertySchemas = new TreeMap<>();
    private final List<String> requiredNames = new ArrayList<>();
    private final boolean noAdditional;
    private final ToolSchema additional;
    private final ToolSchema itemSchema;
    private final List<Object> enumValues;
    private final boolean hasConst;
    private final Object constValue;
    private final BigDecimal lowerBound;
    private final BigDecimal upperBound;
    private final BigDecimal exclusiveLowerBound;
    private final BigDecimal exclusiveUpperBound;
    private final Integer minLength;
    private final Integer maxLength;
    private final String patternText;
    private final Pattern compiledPattern;
    private final Integer minItems;
    private final Integer maxItems;

    private ToolSchema(String where, Map<String, Object> schema) {
        checkKeywords(where, schema);
        this.types = compileType(where, schema.get(TYPE));
        checkApplicable(where, schema);
        compileProperties(where, schema.get(PROPERTIES));
        compileRequired(where, schema.get(REQUIRED));
        var extra = schema.get(ADDITIONAL_PROPERTIES);
        this.noAdditional = Boolean.FALSE.equals(extra);
        this.additional = extra == null || extra instanceof Boolean ? null :
                compile(where + "." + ADDITIONAL_PROPERTIES, extra);
        var items = schema.get(ITEMS);
        this.itemSchema = items == null ? null : compile(where + "." + ITEMS, items);
        this.enumValues = compileEnum(where, schema.get(ENUM));
        this.hasConst = schema.containsKey(CONST);
        this.constValue = schema.get(CONST);
        this.lowerBound = number(where, MINIMUM, schema.get(MINIMUM));
        this.upperBound = number(where, MAXIMUM, schema.get(MAXIMUM));
        this.exclusiveLowerBound = number(where, EXCLUSIVE_MINIMUM, schema.get(EXCLUSIVE_MINIMUM));
        this.exclusiveUpperBound = number(where, EXCLUSIVE_MAXIMUM, schema.get(EXCLUSIVE_MAXIMUM));
        this.minLength = count(where, MIN_LENGTH, schema.get(MIN_LENGTH));
        this.maxLength = count(where, MAX_LENGTH, schema.get(MAX_LENGTH));
        this.minItems = count(where, MIN_ITEMS, schema.get(MIN_ITEMS));
        this.maxItems = count(where, MAX_ITEMS, schema.get(MAX_ITEMS));
        this.patternText = patternText(where, schema.get(PATTERN));
        this.compiledPattern = patternText == null ? null : compilePattern(where, patternText);
    }

    /** Keys in name order, so both engines name the same first problem whatever order a map keeps. */
    private static void checkKeywords(String where, Map<String, Object> schema) {
        for (var key : new TreeSet<>(schema.keySet())) {
            if (!KEYWORDS.contains(key) && !DOCUMENTARY.contains(key)) {
                throw new IllegalArgumentException(where + String.format(UNKNOWN_KEYWORD, key));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void compileProperties(String where, Object props) {
        if (props == null) {
            return;
        }
        if (!(props instanceof Map)) {
            throw new IllegalArgumentException(where + "." + PROPERTIES + " must be a map of schemas");
        }
        for (var entry : new TreeMap<>((Map<String, Object>) props).entrySet()) {
            propertySchemas.put(entry.getKey(), compile(where + "." + PROPERTIES + "." + entry.getKey(),
                    entry.getValue()));
        }
    }

    private static String patternText(String where, Object pattern) {
        if (pattern == null || pattern instanceof String) {
            return (String) pattern;
        }
        throw new IllegalArgumentException(where + "." + PATTERN + " must be a regular expression");
    }

    /**
     * Compile a schema.
     *
     * @param where the schema's place, named in an error (e.g. {@code tools[0].input_schema})
     * @param schema the schema, a map
     * @return the compiled schema
     * @throws IllegalArgumentException naming the first problem
     */
    @SuppressWarnings("unchecked")
    public static ToolSchema compile(String where, Object schema) {
        if (schema instanceof Map) {
            return new ToolSchema(where, (Map<String, Object>) schema);
        }
        throw new IllegalArgumentException(where + " must be a JSON schema object");
    }

    /**
     * The declared types, empty when the schema declares none.
     *
     * @return the type names
     */
    public List<String> types() {
        return types;
    }

    private static List<String> compileType(String where, Object type) {
        if (type == null) {
            return List.of();
        }
        var result = typeNames(where, type instanceof List<?> list ? list : List.of(type));
        if (result.isEmpty()) {
            throw new IllegalArgumentException(where + "." + TYPE + " must be " + TYPE_NAMES + ", or a list of them");
        }
        return result;
    }

    private static List<String> typeNames(String where, List<?> names) {
        var result = new ArrayList<String>();
        for (var name : names) {
            if (!(name instanceof String text) || !KNOWN_TYPES.contains(text)) {
                throw new IllegalArgumentException(where + "." + TYPE + " must be " + TYPE_NAMES +
                        ", or a list of them");
            }
            if (result.contains(text)) {
                throw new IllegalArgumentException(where + "." + TYPE + " lists '" + text + "' twice");
            }
            result.add(text);
        }
        return List.copyOf(result);
    }

    /** A keyword that applies to none of the declared types is refused: it would never constrain anything. */
    private void checkApplicable(String where, Map<String, Object> schema) {
        if (types.isEmpty()) {
            return;
        }
        for (var key : new TreeSet<>(schema.keySet())) {
            var needs = APPLIES_TO.get(key);
            if (needs != null && needs.stream().noneMatch(types::contains)) {
                throw new IllegalArgumentException(where + "." + key + " applies to the type " +
                        String.join(" or ", needs) + ", which the schema does not declare");
            }
        }
    }

    private void compileRequired(String where, Object required) {
        if (required == null) {
            return;
        }
        if (!(required instanceof List<?> names)) {
            throw new IllegalArgumentException(where + "." + REQUIRED + " must be a list of property names");
        }
        for (var name : names) {
            if (!(name instanceof String text)) {
                throw new IllegalArgumentException(where + "." + REQUIRED + " must be a list of property names");
            }
            if (requiredNames.contains(text)) {
                throw new IllegalArgumentException(where + "." + REQUIRED + " lists '" + text + "' twice");
            }
            requiredNames.add(text);
        }
    }

    private static List<Object> compileEnum(String where, Object values) {
        if (values == null) {
            return List.of();
        }
        if (values instanceof List<?> list && !list.isEmpty()) {
            return new ArrayList<>(list);
        }
        throw new IllegalArgumentException(where + "." + ENUM + " must be a non-empty list");
    }

    private static BigDecimal number(String where, String key, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return toDecimal(n);
        }
        throw new IllegalArgumentException(where + "." + key + " must be a number");
    }

    private static Integer count(String where, String key, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            var d = toDecimal(n);
            if (isIntegral(d) && d.signum() >= 0 && d.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0) {
                return d.intValueExact();
            }
        }
        throw new IllegalArgumentException(where + "." + key + " must be a whole number >= 0");
    }

    private static Pattern compilePattern(String where, String text) {
        if (outsideCommonSubset(text)) {
            throw new IllegalArgumentException(where + "." + PATTERN + ": '" + text + "' uses lookaround, an " +
                    "atomic group, a backreference or a possessive quantifier - outside the regex subset common " +
                    "to both engines");
        }
        try {
            return Pattern.compile(text, Pattern.UNICODE_CHARACTER_CLASS);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException(where + "." + PATTERN + ": '" + text +
                    "' is not a valid regular expression");
        }
    }

    /**
     * Lookaround, atomic groups, backreferences and possessive quantifiers are Java-only constructs: the Rust
     * engine's regex crate has none of them, so a pattern that uses one is refused on both engines by this textual
     * check (escapes and character classes are skipped). The same rule as the graph contract's.
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

    private static boolean isBackreference(String p, int i) {
        if (i + 1 >= p.length()) {
            return false;
        }
        char next = p.charAt(i + 1);
        return (next >= '1' && next <= '9') || next == 'k';
    }

    private static boolean isJavaOnlyGroup(String p, int i) {
        if (!p.startsWith("(?", i)) {
            return false;
        }
        var rest = p.substring(i + 2);
        return rest.startsWith("=") || rest.startsWith("!") || rest.startsWith("<=") || rest.startsWith("<!")
                || rest.startsWith(">");
    }

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
     * Validate a value, collecting every violation in a fixed order: the type, then enum and const, then the
     * keywords of the value's type. An object reports its missing required properties in the order declared,
     * then its properties in name order.
     *
     * @param path the value's path, e.g. {@code input}
     * @param value the value
     * @param violations the collector
     */
    @SuppressWarnings("unchecked")
    public void validate(String path, Object value, List<String> violations) {
        var actual = kindOf(value);
        if (!types.isEmpty() && !matchesType(actual)) {
            violations.add(path + EXPECTED + typeText() + ", got " + actual);
            return;
        }
        if (!enumValues.isEmpty() && enumValues.stream().noneMatch(v -> jsonEquals(v, value))) {
            violations.add(path + ": must be one of " + Json.compact(enumValues));
        }
        if (hasConst && !jsonEquals(constValue, value)) {
            violations.add(path + MUST_BE + Json.compact(constValue));
        }
        switch (actual) {
            case INTEGER, NUMBER -> checkBounds(path, toDecimal((Number) value), violations);
            case STRING -> checkString(path, (String) value, violations);
            case ARRAY -> checkArray(path, (List<Object>) value, violations);
            case OBJECT -> checkObject(path, (Map<String, Object>) value, violations);
            default -> {
                // null and boolean have no keywords of their own
            }
        }
    }

    private boolean matchesType(String actual) {
        return types.contains(actual) || (INTEGER.equals(actual) && types.contains(NUMBER));
    }

    private String typeText() {
        if (types.size() == 1) {
            return types.getFirst();
        }
        return String.join(", ", types.subList(0, types.size() - 1)) + " or " + types.getLast();
    }

    private void checkBounds(String path, BigDecimal n, List<String> violations) {
        if (lowerBound != null && n.compareTo(lowerBound) < 0) {
            violations.add(path + MUST_BE + ">= " + render(lowerBound));
        }
        if (upperBound != null && n.compareTo(upperBound) > 0) {
            violations.add(path + MUST_BE + "<= " + render(upperBound));
        }
        if (exclusiveLowerBound != null && n.compareTo(exclusiveLowerBound) <= 0) {
            violations.add(path + MUST_BE + "> " + render(exclusiveLowerBound));
        }
        if (exclusiveUpperBound != null && n.compareTo(exclusiveUpperBound) >= 0) {
            violations.add(path + MUST_BE + "< " + render(exclusiveUpperBound));
        }
    }

    private void checkString(String path, String text, List<String> violations) {
        int length = text.codePointCount(0, text.length());
        if (minLength != null && length < minLength) {
            violations.add(path + ": must be at least " + minLength + " characters");
        }
        if (maxLength != null && length > maxLength) {
            violations.add(path + ": must be at most " + maxLength + " characters");
        }
        if (compiledPattern != null && !compiledPattern.matcher(text).find()) {
            violations.add(path + ": must match the pattern '" + patternText + "'");
        }
    }

    private void checkArray(String path, List<Object> list, List<String> violations) {
        if (minItems != null && list.size() < minItems) {
            violations.add(path + ": must have at least " + minItems + " items");
        }
        if (maxItems != null && list.size() > maxItems) {
            violations.add(path + ": must have at most " + maxItems + " items");
        }
        if (itemSchema != null) {
            for (int i = 0; i < list.size(); i++) {
                itemSchema.validate(path + "[" + i + "]", list.get(i), violations);
            }
        }
    }

    private void checkObject(String path, Map<String, Object> map, List<String> violations) {
        for (var name : requiredNames) {
            if (!map.containsKey(name)) {
                violations.add(path + "." + name + ": is required");
            }
        }
        var names = new TreeSet<>(map.keySet());
        for (var name : names) {
            var declared = propertySchemas.get(name);
            if (declared != null) {
                declared.validate(path + "." + name, map.get(name), violations);
            } else if (noAdditional) {
                violations.add(path + "." + name + ": is not a declared property");
            } else if (additional != null) {
                additional.validate(path + "." + name, map.get(name), violations);
            }
        }
    }

    /**
     * Render violations as one message, capped at {@link #VIOLATION_CAP}.
     *
     * @param violations the violations, at least one
     * @return the message
     */
    public static String report(List<String> violations) {
        if (violations.size() <= VIOLATION_CAP) {
            return String.join("; ", violations);
        }
        return String.join("; ", violations.subList(0, VIOLATION_CAP)) + "; and " +
                (violations.size() - VIOLATION_CAP) + " more";
    }

    // ------------------------------------------------------------------ JSON values

    /**
     * What a decoded JSON value is: null, boolean, integer (a number with no fractional part), number, string,
     * array or object.
     *
     * @param value a decoded JSON value
     * @return the name of what it is
     */
    static String kindOf(Object value) {
        return switch (value) {
            case null -> NULL;
            case Boolean ignored -> BOOLEAN;
            case Number n -> isIntegral(toDecimal(n)) ? INTEGER : NUMBER;
            case String ignored -> STRING;
            case List<?> ignored -> ARRAY;
            case Map<?, ?> ignored -> OBJECT;
            default -> value.getClass().getSimpleName();
        };
    }

    static BigDecimal toDecimal(Number n) {
        return n instanceof BigDecimal d ? d : new BigDecimal(n.toString());
    }

    static boolean isIntegral(BigDecimal n) {
        return n.stripTrailingZeros().scale() <= 0;
    }

    /** A number as both engines print it: no exponent, no trailing zeros, no decimal point when whole. */
    static String render(BigDecimal n) {
        var stripped = n.stripTrailingZeros();
        // a negative scale only widens to whole digits, so nothing is ever rounded
        return stripped.scale() < 0 ? stripped.setScale(0, RoundingMode.UNNECESSARY).toPlainString() :
                stripped.toPlainString();
    }

    /** JSON equality: numbers by value, lists in order, maps by keys and values. */
    static boolean jsonEquals(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        // a Boolean is not a Number in Java, so true never equals 1
        return switch (a) {
            case Number x when b instanceof Number y -> toDecimal(x).compareTo(toDecimal(y)) == 0;
            case List<?> x when b instanceof List<?> y -> listEquals(x, y);
            case Map<?, ?> x when b instanceof Map<?, ?> y -> mapEquals(x, y);
            default -> a.equals(b);
        };
    }

    private static boolean listEquals(List<?> x, List<?> y) {
        if (x.size() != y.size()) {
            return false;
        }
        for (int i = 0; i < x.size(); i++) {
            if (!jsonEquals(x.get(i), y.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean mapEquals(Map<?, ?> x, Map<?, ?> y) {
        if (!new HashSet<>(x.keySet()).equals(new HashSet<>(y.keySet()))) {
            return false;
        }
        for (var entry : x.entrySet()) {
            if (!jsonEquals(entry.getValue(), y.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }
}
