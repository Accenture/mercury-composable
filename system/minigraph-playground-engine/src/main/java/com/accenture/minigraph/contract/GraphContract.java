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

import org.platformlambda.core.serializers.SimpleMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * The contract of a graph model (RFC-0007): what a caller sends and what the graph answers, derived
 * from the model and merged with what the model declares.
 * <p>
 * <b>Discovery</b> works in three tiers over every node's properties: (1) the path scan that
 * {@code describe graph} has always done - every {@code input.*} and {@code output.*} token, with
 * nesting and the {@code []}, {@code [*]} and {@code [0]} array markers; (2) direct evidence - a typed
 * constant or wrapper ({@code int(...)}, {@code text(...)}), a plugin with a known result
 * ({@code f:now}, {@code f:listOfMap}), a {@code for_each} source, a {@code graph.math} result
 * ({@code COMPUTE} a number, {@code CONDITION} a boolean, {@code DECIMAL} a string) and an arithmetic
 * or ordered comparison operand; (3) one hop of propagation through a variable - a model variable or a
 * node result that was typed by (2), or a {@code graph.extension} target's declared output - into the
 * output path it feeds. Each path records the nodes that reference it.
 * <p>
 * <b>Declaration</b> is the optional {@code schema} property of the root node (the request:
 * {@code schema.body} for {@code input.body}, {@code schema.header} for {@code input.header}) and of the
 * end node (the response: {@code output.body} and {@code output.header}), each part a schema object in
 * the OpenAPI 3.0 dialect. The declaration wins over discovery; a discovered path the declaration lacks
 * is kept untyped and flagged; a declared path the model never references is kept and flagged. Header
 * names are case-insensitive. Query parameters are not part of the graph API (one endpoint for every
 * graph), so they are neither declared nor derived.
 * <p>
 * The same derivation runs in the Rust engine; the shared vector file pins both.
 */
public final class GraphContract {
    public static final String INPUT_BODY = "input.body";
    public static final String INPUT_HEADER = "input.header";
    public static final String OUTPUT_BODY = "output.body";
    public static final String OUTPUT_HEADER = "output.header";
    public static final String OUTPUT_STATUS = "output.status";
    private static final String[] NAMESPACES = {INPUT_BODY, INPUT_HEADER, OUTPUT_BODY, OUTPUT_HEADER};
    private static final String ROOT = "root";
    private static final String END = "end";
    private static final String ALIAS = "alias";
    private static final String NODES = "nodes";
    private static final String PROPERTIES = "properties";
    private static final String SCHEMA = "schema";
    private static final String BODY = "body";
    private static final String HEADER = "header";
    private static final String TYPE = "type";
    private static final String ITEMS = "items";
    private static final String REQUIRED = "required";
    private static final String DESCRIPTION = "description";
    private static final String STRING = "string";
    private static final String NUMBER = "number";
    private static final String INTEGER = "integer";
    private static final String BOOLEAN = "boolean";
    private static final String OBJECT = "object";
    private static final String ARRAY = "array";
    private static final String MAP_TO = "->";
    private static final String MODEL_PREFIX = "model.";
    private static final String RESULT = "result";
    private static final String INPUT = "input";
    private static final String OUTPUT = "output";
    private static final String PURPOSE = "purpose";
    private static final String FOR_EACH = "for_each";
    private static final String MAPPING = "MAPPING";
    private static final String COMPUTE = "COMPUTE";
    private static final String CONDITION = "CONDITION";
    private static final String DECIMAL = "DECIMAL";
    private static final String INT = "int";
    private static final String LONG = "long";
    private static final String FLOAT = "float";
    private static final String DOUBLE = "double";
    private static final String TEXT = "text";
    private static final String DEFAULT_VALUE = "defaultValue";
    private static final String[] MAPPING_PROPERTIES = {"mapping", INPUT, OUTPUT, FOR_EACH, "statement"};
    /** The simple plugins whose result type is known from the plugin alone. */
    private static final Map<String, String> PLUGIN_TYPES = Map.ofEntries(
            Map.entry(INT, INTEGER), Map.entry(LONG, INTEGER), Map.entry("length", INTEGER),
            Map.entry("decimalCompare", INTEGER),
            Map.entry(FLOAT, NUMBER), Map.entry(DOUBLE, NUMBER), Map.entry("add", NUMBER),
            Map.entry("subtract", NUMBER), Map.entry("multiply", NUMBER), Map.entry("div", NUMBER),
            Map.entry("mod", NUMBER), Map.entry("increment", NUMBER), Map.entry("decrement", NUMBER),
            Map.entry(BOOLEAN, BOOLEAN), Map.entry("eq", BOOLEAN), Map.entry("ne", BOOLEAN),
            Map.entry("gt", BOOLEAN), Map.entry("lt", BOOLEAN), Map.entry("and", BOOLEAN),
            Map.entry("or", BOOLEAN), Map.entry("not", BOOLEAN), Map.entry("isNull", BOOLEAN),
            Map.entry("notNull", BOOLEAN),
            Map.entry(TEXT, STRING), Map.entry("concat", STRING), Map.entry("substring", STRING),
            Map.entry("now", STRING), Map.entry("uuid", STRING), Map.entry("dateTime", STRING),
            Map.entry("b64", STRING), Map.entry("lookup", STRING), Map.entry("decimalAdd", STRING),
            Map.entry("decimalSubtract", STRING), Map.entry("decimalMultiply", STRING),
            Map.entry("decimalDiv", STRING), Map.entry("decimalMod", STRING), Map.entry("decimalRound", STRING),
            Map.entry("listOfMap", ARRAY), Map.entry("updateListOfMap", ARRAY));

    /** One path of a namespace tree: a property, a header, or the element of an array. */
    static final class Node {
        String name;
        String type;
        boolean array;
        boolean declared;
        boolean discovered;
        boolean required;
        final TreeSet<String> usedBy = new TreeSet<>();
        /** The declared keys of this path other than the structural ones, verbatim. */
        final Map<String, Object> fragment = new LinkedHashMap<>();
        /** For a declared array, the declared keys of its element other than the structural ones. */
        final Map<String, Object> itemFragment = new LinkedHashMap<>();
        final TreeMap<String, Node> children = new TreeMap<>();

        Node child(String key, boolean caseInsensitive) {
            var k = caseInsensitive ? key.toLowerCase(Locale.ROOT) : key;
            return children.computeIfAbsent(k, ignored -> {
                var n = new Node();
                n.name = key;
                return n;
            });
        }
    }

    private final String graphId;
    private final String purpose;
    private final Map<String, Node> roots = new LinkedHashMap<>();
    private final Map<String, Boolean> declared = new LinkedHashMap<>();
    private final TreeSet<Integer> statusCodes = new TreeSet<>();
    private final List<String> issues = new ArrayList<>();
    /** Variables typed by direct evidence: model.x, {node}.result.y, {node}.y. */
    private final Map<String, String> variables = new LinkedHashMap<>();

    private GraphContract(String graphId, String purpose) {
        this.graphId = graphId;
        this.purpose = purpose;
        for (var ns : NAMESPACES) {
            var root = new Node();
            root.name = ns;
            roots.put(ns, root);
            declared.put(ns, false);
        }
    }

    /**
     * Derive the contract of a graph model.
     *
     * @param graphId the graph id (the deployed id, or a draft's name)
     * @param model the graph model (nodes and connections)
     * @param otherModels resolves another deployed model by id, for a graph.extension target; may return null
     * @return the contract
     */
    public static GraphContract derive(String graphId, Map<String, Object> model,
                                       Function<String, Map<String, Object>> otherModels) {
        var contract = new GraphContract(graphId, rootPurpose(model));
        contract.discover(model, otherModels == null ? id -> null : otherModels);
        contract.declare(model);
        contract.reconcile();
        return contract;
    }

    public String getGraphId() {
        return graphId;
    }

    public String getPurpose() {
        return purpose;
    }

    public SortedSet<Integer> getStatusCodes() {
        return statusCodes;
    }

    public boolean isDeclared(String namespace) {
        return Boolean.TRUE.equals(declared.get(namespace));
    }

    /** True when the namespace has any path, discovered or declared. */
    public boolean has(String namespace) {
        var root = roots.get(namespace);
        return root != null && (root.discovered || root.declared || !root.children.isEmpty());
    }

    Node root(String namespace) {
        return roots.get(namespace);
    }

    public List<String> getIssues() {
        return issues;
    }

    // ---------------------------------------------------------------- discovery

    private void discover(Map<String, Object> model, Function<String, Map<String, Object>> otherModels) {
        var nodes = nodeList(model);
        // tier 1: the path scan over every node's properties, in the JSON text form both engines scan
        for (var node : nodes) {
            var alias = String.valueOf(node.get(ALIAS));
            var properties = node.get(PROPERTIES);
            if (properties instanceof Map<?, ?> map) {
                var text = propertiesAsText(map);
                for (var ns : NAMESPACES) {
                    for (var token : collectPathTokens(text, ns)) {
                        recordPath(ns, token, alias);
                    }
                }
            }
        }
        // tier 2: direct evidence, then tier 3: one hop through a variable (two ordered passes)
        var entries = mappingEntries(nodes);
        for (var e : entries) {
            directEvidence(e.alias, e.property, e.text);
        }
        for (var node : nodes) {
            extensionTarget(node, otherModels);
        }
        for (var e : entries) {
            variableFromInput(e.alias, e.text);
        }
        for (var e : entries) {
            outputFromVariable(e.alias, e.text);
        }
    }

    private record Entry(String alias, String property, String text) {}

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> nodeList(Map<String, Object> model) {
        var result = new ArrayList<Map<String, Object>>();
        if (model != null && model.get(NODES) instanceof List<?> list) {
            for (var n : list) {
                if (n instanceof Map<?, ?> m) {
                    result.add((Map<String, Object>) m);
                }
            }
        }
        return result;
    }

    private static List<Entry> mappingEntries(List<Map<String, Object>> nodes) {
        var result = new ArrayList<Entry>();
        for (var node : nodes) {
            var alias = String.valueOf(node.get(ALIAS));
            if (node.get(PROPERTIES) instanceof Map<?, ?> properties) {
                for (var property : MAPPING_PROPERTIES) {
                    addEntries(result, alias, property, properties.get(property));
                }
            }
        }
        return result;
    }

    /** A mapping property holds one line or a list of lines; anything else carries no mapping. */
    private static void addEntries(List<Entry> result, String alias, String property, Object value) {
        if (value instanceof String s) {
            result.add(new Entry(alias, property, s));
        } else if (value instanceof List<?> list) {
            for (var item : list) {
                if (item instanceof String s) {
                    result.add(new Entry(alias, property, s));
                }
            }
        }
    }

    private static String rootPurpose(Map<String, Object> model) {
        if (nodeProperties(model, ROOT).get(PURPOSE) instanceof String s && !s.isBlank()) {
            return s.trim();
        }
        return null;
    }

    /** The properties of the node with the alias; empty when the model has no such node. */
    private static Map<?, ?> nodeProperties(Map<String, Object> model, String alias) {
        for (var node : nodeList(model)) {
            if (alias.equals(node.get(ALIAS)) && node.get(PROPERTIES) instanceof Map<?, ?> p) {
                return p;
            }
        }
        return Map.of();
    }

    /** The JSON text of a node's properties - the shape the Rust engine scans too. */
    static String propertiesAsText(Object properties) {
        try {
            return SimpleMapper.getInstance().getMapper().writeValueAsString(properties);
        } catch (Exception e) {
            return String.valueOf(properties);
        }
    }

    /** Collect the dotted-path tokens starting with the prefix from free text. */
    public static SortedSet<String> collectPathTokens(String text, String prefix) {
        var found = new TreeSet<String>();
        int start = 0;
        while (start != -1) {
            start = nextPathToken(text, prefix, start, found);
        }
        return found;
    }

    private static int nextPathToken(String text, String prefix, int start, SortedSet<String> found) {
        int begin = text.indexOf(prefix, start);
        if (begin == -1) {
            return -1;
        }
        // a mid-word match is part of a longer identifier, not a path token - except the JSONPath
        // form "$.input.body...", whose dot follows the root symbol
        if (begin > 0 && isWordChar(text.charAt(begin - 1))
                && !(text.charAt(begin - 1) == '.' && begin > 1 && text.charAt(begin - 2) == '$')) {
            return begin + prefix.length();
        }
        int end = begin + prefix.length();
        // a namespace prefix must end the token or be followed by a separator (input.body vs
        // input.bodyish); a prefix that ends with the separator itself ("input.") continues
        if (!prefix.endsWith(".") && end < text.length()
                && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '_')) {
            return end;
        }
        while (end < text.length() && isTokenChar(text.charAt(end))) {
            end++;
        }
        var token = trimToken(text.substring(begin, end));
        if (token.length() >= prefix.length()) {
            found.add(token);
        }
        return end;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.';
    }

    private static boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-' || c == '[' || c == ']' || c == '*';
    }

    /**
     * Strip trailing separators, then every trailing ']' the token's own '[' do not balance (an
     * enclosing list's closing bracket absorbed from an unquoted serialization form).
     */
    private static String trimToken(String token) {
        var result = token;
        while (!result.isEmpty() && (result.endsWith(".") || result.endsWith("-") || result.endsWith("["))) {
            result = result.substring(0, result.length() - 1);
        }
        while (result.endsWith("]") && count(result, ']') > count(result, '[')) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static int count(String text, char c) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }

    /** Record a discovered path token of a namespace, with the node that references it. */
    private void recordPath(String namespace, String token, String alias) {
        var node = locate(namespace, token, true);
        if (node != null) {
            node.discovered = true;
            if (alias != null) {
                node.usedBy.add(alias);
            }
        }
    }

    /**
     * Find or create the node of a path token such as {@code input.body.items[*].sku}; the namespace
     * itself names the root. Returns null when the token is not of the namespace.
     */
    private Node locate(String namespace, String token, boolean create) {
        var root = roots.get(namespace);
        if (token.equals(namespace)) {
            return root;
        }
        if (!token.startsWith(namespace + ".")) {
            return null;
        }
        var headers = isHeaderNamespace(namespace);
        var node = root;
        for (var segment : token.substring(namespace.length() + 1).split("\\.")) {
            var bracket = segment.indexOf('[');
            var key = bracket >= 0 ? segment.substring(0, bracket) : segment;
            if (!key.isEmpty()) {
                node = step(node, key, headers, create);
                if (node == null) {
                    return null;
                }
                if (bracket >= 0) {
                    node.array = true;
                }
            }
        }
        return node;
    }

    /** The child of a node for one path segment; null when it does not exist and may not be created. */
    private static Node step(Node node, String key, boolean headers, boolean create) {
        if (!create && !node.children.containsKey(headers ? key.toLowerCase(Locale.ROOT) : key)) {
            return null;
        }
        return node.child(key, headers);
    }

    private static boolean isHeaderNamespace(String namespace) {
        return INPUT_HEADER.equals(namespace) || OUTPUT_HEADER.equals(namespace);
    }

    private static String tagOf(String text) {
        var colon = text.indexOf(':');
        if (colon > 0) {
            var tag = text.substring(0, colon).trim();
            if (tag.equals(MAPPING) || tag.equals(COMPUTE) || tag.equals(CONDITION) || tag.equals(DECIMAL)
                    || tag.equals("IF") || tag.equals("RESET") || tag.equals("NEXT") || tag.equals("DELAY")
                    || tag.equals("THEN") || tag.equals("ELSE")) {
                return tag;
            }
        }
        return null;
    }

    private static String afterTag(String text) {
        var tag = tagOf(text);
        return tag == null ? text.trim() : text.substring(text.indexOf(':') + 1).trim();
    }

    /** The two sides of a mapping line ({@code lhs -> rhs}); null for a line that is not a mapping. */
    private static String[] mappingSides(String text) {
        var tag = tagOf(text);
        if (tag != null && !MAPPING.equals(tag)) {
            return null;
        }
        var body = afterTag(text);
        var sep = body.lastIndexOf(MAP_TO);
        if (sep <= 0) {
            return null;
        }
        return new String[]{body.substring(0, sep).trim(), body.substring(sep + MAP_TO.length()).trim()};
    }

    /** Tier 2: a typed constant, wrapper, plugin, for_each source or graph.math statement. */
    private void directEvidence(String alias, String property, String text) {
        var tag = tagOf(text);
        if (COMPUTE.equals(tag) || CONDITION.equals(tag) || DECIMAL.equals(tag)) {
            mathStatement(alias, tag, afterTag(text));
            return;
        }
        if ("IF".equals(tag)) {
            comparisonOperands(afterTag(text).split("\n")[0]);
            return;
        }
        var sides = mappingSides(text);
        if (sides != null) {
            mappingEvidence(property, sides[0], sides[1]);
        }
    }

    /** A for_each source is an array; a status constant is a response code; a typed source types its target. */
    private void mappingEvidence(String property, String lhs, String rhs) {
        if (FOR_EACH.equals(property)) {
            forEachSource(lhs);
            return;
        }
        var type = typeOf(lhs);
        if (rhs.equals(OUTPUT_STATUS)) {
            var code = integerConstant(lhs);
            if (code != null) {
                statusCodes.add(code);
            }
            return;
        }
        if (type != null) {
            assign(rhs, type);
        }
    }

    private void forEachSource(String lhs) {
        for (var ns : new String[]{INPUT_BODY, OUTPUT_BODY}) {
            var node = locate(ns, lhs, false);
            if (node != null && node != roots.get(ns)) {
                node.array = true;
            }
        }
    }

    /** COMPUTE a number, CONDITION a boolean, DECIMAL a string, at {alias}.result.{var}. */
    private void mathStatement(String alias, String tag, String body) {
        var sep = body.indexOf(MAP_TO);
        if (sep <= 0) {
            return;
        }
        var variable = body.substring(0, sep).trim();
        var expression = body.substring(sep + MAP_TO.length()).trim();
        var type = switch (tag) {
            case COMPUTE -> NUMBER;
            case CONDITION -> BOOLEAN;
            default -> STRING;
        };
        if (!variable.isEmpty()) {
            variables.put(alias + "." + RESULT + "." + variable, type);
        }
        if (COMPUTE.equals(tag) && hasArithmetic(expression)) {
            // the operands of arithmetic are numbers: a boolean is never a number (4.12.18)
            numericOperands(expression);
        } else if (CONDITION.equals(tag)) {
            comparisonOperands(expression);
        }
    }

    /** Every input path an expression references is a number. */
    private void numericOperands(String expression) {
        for (var ns : new String[]{INPUT_BODY, INPUT_HEADER}) {
            for (var token : collectPathTokens(expression, ns)) {
                typeInputPath(token, NUMBER);
            }
        }
    }

    private static boolean hasArithmetic(String expression) {
        var depth = 0;
        for (int i = 0; i < expression.length(); i++) {
            var c = expression.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0 && (c == '+' || c == '-' || c == '*' || c == '/' || c == '%')) {
                return true;
            }
        }
        return false;
    }

    /** The operands of an ordered comparison (&lt;, &gt;, &lt;=, &gt;=) are numbers. */
    private void comparisonOperands(String expression) {
        for (var clause : expression.split("&&|\\|\\|")) {
            if (clause.contains("==") || clause.contains("!=")) {
                continue;
            }
            if (clause.contains("<") || clause.contains(">")) {
                numericOperands(clause);
            }
        }
    }

    /** An array result marks the path as an array (OpenAPI needs its items); any other type is the leaf type. */
    private static void setType(Node node, String type) {
        if (ARRAY.equals(type)) {
            node.array = true;
        } else {
            node.type = type;
        }
    }

    /** Give a target - an output path or a variable - a type found on the source side. */
    private void assign(String rhs, String type) {
        for (var ns : new String[]{OUTPUT_BODY, OUTPUT_HEADER}) {
            var node = locate(ns, rhs, false);
            if (node != null) {
                if (node.type == null && (node != roots.get(ns) || node.children.isEmpty())) {
                    setType(node, type);
                }
                return;
            }
        }
        if (rhs.startsWith(MODEL_PREFIX) || rhs.contains("." + RESULT + ".") || isNodeVariable(rhs)) {
            variables.putIfAbsent(rhs, type);
        }
    }

    private static boolean isNodeVariable(String selector) {
        return !selector.startsWith("input.") && !selector.startsWith("output.") && !selector.startsWith(".")
                && selector.contains(".") && !selector.contains("(");
    }

    /** Tier 3, first hop: a variable fed by a typed input path takes its type. */
    private void variableFromInput(String alias, String text) {
        var sides = mappingSides(text);
        if (sides == null) {
            return;
        }
        var lhs = sides[0];
        var rhs = sides[1];
        var type = inputType(lhs);
        if (type != null && (rhs.startsWith(MODEL_PREFIX) || isNodeVariable(rhs))) {
            variables.putIfAbsent(qualified(alias, rhs), type);
        }
    }

    /** A node-relative selector such as {@code result.total} is {@code {alias}.result.total}. */
    private static String qualified(String alias, String selector) {
        return selector.startsWith(RESULT) && (selector.length() == RESULT.length() || selector.charAt(RESULT.length()) == '.')
                ? alias + "." + selector : selector;
    }

    /** Tier 3, second hop: an output path fed by a typed variable or a typed input takes the type. */
    private void outputFromVariable(String alias, String text) {
        var sides = mappingSides(text);
        if (sides == null) {
            return;
        }
        var lhs = sides[0];
        var rhs = sides[1];
        var type = variables.get(lhs);
        if (type == null) {
            type = variables.get(qualified(alias, lhs));
        }
        if (type == null) {
            type = inputType(lhs);
        }
        if (type != null) {
            for (var ns : new String[]{OUTPUT_BODY, OUTPUT_HEADER}) {
                var node = locate(ns, rhs, false);
                if (node != null && node != roots.get(ns) && node.type == null) {
                    setType(node, type);
                }
            }
        }
    }

    private String inputType(String selector) {
        for (var ns : new String[]{INPUT_BODY, INPUT_HEADER}) {
            var node = locate(ns, selector, false);
            if (node != null && node != roots.get(ns)) {
                return node.type;
            }
        }
        return null;
    }

    /** A graph.extension target's declared output types the node's result. */
    private void extensionTarget(Map<String, Object> node, Function<String, Map<String, Object>> otherModels) {
        if (!(node.get(PROPERTIES) instanceof Map<?, ?> p) || !"graph.extension".equals(p.get("skill"))) {
            return;
        }
        var alias = String.valueOf(node.get(ALIAS));
        var target = p.get("extension");
        if (!(target instanceof String id) || id.contains("://") || id.isBlank()) {
            return;
        }
        var model = otherModels.apply(id.trim());
        if (model == null) {
            return;
        }
        var end = nodeProperties(model, END);
        if (end.get(SCHEMA) instanceof Map<?, ?> schema && schema.get(BODY) instanceof Map<?, ?> body
                && body.get(PROPERTIES) instanceof Map<?, ?> properties) {
            for (var kv : properties.entrySet()) {
                if (kv.getValue() instanceof Map<?, ?> property && property.get(TYPE) instanceof String type) {
                    variables.putIfAbsent(alias + "." + RESULT + "." + kv.getKey(), type);
                }
            }
        }
    }

    /**
     * The type a mapping source carries: a constant wrapper, a plugin with a known result, or a
     * wrapper around an input path (which types the path as well).
     */
    private String typeOf(String lhs) {
        var open = lhs.indexOf('(');
        if (open <= 0 || !lhs.endsWith(")")) {
            return null;
        }
        var name = lhs.substring(0, open).trim();
        var args = splitArguments(lhs.substring(open + 1, lhs.length() - 1));
        var wrapper = wrapperType(name);
        if (wrapper != null) {
            typeSingleArgument(args, wrapper);
            return wrapper;
        }
        return name.startsWith("f:") ? pluginType(name.substring(2), args) : null;
    }

    /** The constant wrappers of a mapping source, which the plugins of the same names mirror. */
    private static String wrapperType(String name) {
        return switch (name) {
            case INT, LONG -> INTEGER;
            case FLOAT, DOUBLE -> NUMBER;
            case BOOLEAN -> BOOLEAN;
            case TEXT -> STRING;
            default -> null;
        };
    }

    /** A wrapper around one input path types the path as well. */
    private void typeSingleArgument(List<String> args, String type) {
        if (args.size() == 1) {
            typeInputPath(args.getFirst(), type);
        }
    }

    private String pluginType(String plugin, List<String> args) {
        if (DEFAULT_VALUE.equals(plugin) || "ternary".equals(plugin)) {
            return secondArgumentType(plugin, args);
        }
        var type = PLUGIN_TYPES.get(plugin);
        if (type != null && wrapperType(plugin) != null) {
            typeSingleArgument(args, type);
        }
        return type;
    }

    /**
     * {@code f:defaultValue(path, fallback)} and {@code f:ternary(condition, a, b)} carry the type of their
     * second argument; a default value types the path it stands in for.
     */
    private String secondArgumentType(String plugin, List<String> args) {
        if (args.size() < 2) {
            return null;
        }
        var type = typeOf(args.get(1));
        if (type != null && DEFAULT_VALUE.equals(plugin)) {
            typeInputPath(args.getFirst(), type);
        }
        return type;
    }

    private void typeInputPath(String argument, String type) {
        var selector = argument.trim();
        for (var ns : new String[]{INPUT_BODY, INPUT_HEADER}) {
            var node = locate(ns, selector, false);
            if (node != null && node != roots.get(ns) && node.type == null) {
                setType(node, type);
            }
        }
    }

    private static Integer integerConstant(String lhs) {
        if ((lhs.startsWith("int(") || lhs.startsWith("long(")) && lhs.endsWith(")")) {
            var digits = lhs.substring(lhs.indexOf('(') + 1, lhs.length() - 1).trim();
            if (!digits.isEmpty() && digits.chars().allMatch(Character::isDigit)) {
                return Integer.parseInt(digits);
            }
        }
        return null;
    }

    private static List<String> splitArguments(String text) {
        var result = new ArrayList<String>();
        var depth = 0;
        var start = 0;
        for (int i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                result.add(text.substring(start, i).trim());
                start = i + 1;
            }
        }
        var last = text.substring(start).trim();
        if (!last.isEmpty() || !result.isEmpty()) {
            result.add(last);
        }
        return result;
    }

    // ---------------------------------------------------------------- declaration

    private void declare(Map<String, Object> model) {
        var root = nodeProperties(model, ROOT);
        var end = nodeProperties(model, END);
        declarePart(root, BODY, INPUT_BODY);
        declarePart(root, HEADER, INPUT_HEADER);
        declarePart(end, BODY, OUTPUT_BODY);
        declarePart(end, HEADER, OUTPUT_HEADER);
    }

    private void declarePart(Map<?, ?> properties, String part, String namespace) {
        if (!(properties.get(SCHEMA) instanceof Map<?, ?> schema)
                || !(schema.get(part) instanceof Map<?, ?> declaredSchema)) {
            return;
        }
        declared.put(namespace, true);
        var root = roots.get(namespace);
        root.declared = true;
        overlay(root, declaredSchema, isHeaderNamespace(namespace));
    }

    /** Overlay a declared schema object on a node: the declaration wins. */
    private void overlay(Node node, Map<?, ?> schema, boolean headers) {
        node.declared = true;
        var type = schema.get(TYPE) instanceof String t ? t : null;
        if (ARRAY.equals(type)) {
            overlayArray(node, schema, headers);
            return;
        }
        if (type != null && !OBJECT.equals(type)) {
            setType(node, type);
        }
        if (schema.get(PROPERTIES) instanceof Map<?, ?> properties) {
            overlayProperties(node, properties, schema.get(REQUIRED), headers);
        }
        keepFragment(schema, node.fragment);
    }

    /** A declared array: its element's type, properties and fragment land on the path itself. */
    private void overlayArray(Node node, Map<?, ?> schema, boolean headers) {
        node.array = true;
        if (schema.get(ITEMS) instanceof Map<?, ?> items) {
            var itemType = items.get(TYPE) instanceof String t ? t : null;
            if (itemType != null && !OBJECT.equals(itemType)) {
                node.type = itemType;
            }
            if (items.get(PROPERTIES) instanceof Map<?, ?> properties) {
                overlayProperties(node, properties, items.get(REQUIRED), headers);
            }
            keepFragment(items, node.itemFragment);
        }
        keepFragment(schema, node.fragment);
    }

    private void overlayProperties(Node node, Map<?, ?> properties, Object required, boolean headers) {
        for (var kv : properties.entrySet()) {
            if (kv.getValue() instanceof Map<?, ?> propertySchema) {
                var child = node.child(String.valueOf(kv.getKey()), headers);
                child.name = String.valueOf(kv.getKey());
                overlay(child, propertySchema, headers);
            }
        }
        if (required instanceof List<?> list) {
            for (var name : list) {
                var key = String.valueOf(name);
                var child = node.children.get(headers ? key.toLowerCase(Locale.ROOT) : key);
                if (child != null) {
                    child.required = true;
                }
            }
        }
    }

    private static void keepFragment(Map<?, ?> schema, Map<String, Object> fragment) {
        for (var kv : schema.entrySet()) {
            var key = String.valueOf(kv.getKey());
            if (!key.equals(TYPE) && !key.equals(PROPERTIES) && !key.equals(ITEMS) && !key.equals(REQUIRED)) {
                fragment.put(key, kv.getValue());
            }
        }
    }

    // ---------------------------------------------------------------- reconcile

    /** Flag the gaps between declaration and discovery; a namespace without a declaration flags nothing. */
    private void reconcile() {
        for (var ns : NAMESPACES) {
            if (!isDeclared(ns)) {
                continue;
            }
            walk(roots.get(ns), ns, (path, node) -> {
                if (node.declared && !node.discovered && node.children.isEmpty()) {
                    issues.add(path + " is declared but never referenced by the model");
                } else if (node.discovered && !node.declared) {
                    issues.add(path + " is referenced by " + String.join(", ", node.usedBy) + " but not declared");
                }
            });
        }
    }

    interface Visitor {
        void visit(String path, Node node);
    }

    static void walk(Node node, String path, Visitor visitor) {
        for (var kv : node.children.entrySet()) {
            var child = kv.getValue();
            var childPath = path + "." + child.name + (child.array ? "[]" : "");
            visitor.visit(childPath, child);
            walk(child, childPath, visitor);
        }
    }

    // ---------------------------------------------------------------- views

    /** The OpenAPI schema object of a namespace (empty when nothing is referenced or declared). */
    public Map<String, Object> schema(String namespace) {
        var root = roots.get(namespace);
        if (!has(namespace)) {
            return new LinkedHashMap<>();
        }
        return schemaOf(root);
    }

    static Map<String, Object> schemaOf(Node node) {
        var schema = new LinkedHashMap<String, Object>();
        if (node.array) {
            schema.put(TYPE, ARRAY);
            var items = new LinkedHashMap<String, Object>();
            elementSchema(node, items, node.itemFragment, false);
            schema.put(ITEMS, items);
            if (!node.declared && !node.usedBy.isEmpty()) {
                schema.put(DESCRIPTION, "Referenced by " + String.join(", ", node.usedBy));
            }
            schema.putAll(node.fragment);
            return schema;
        }
        elementSchema(node, schema, node.fragment, true);
        return schema;
    }

    private static void elementSchema(Node node, Map<String, Object> schema, Map<String, Object> fragment,
                                      boolean describe) {
        if (!node.children.isEmpty()) {
            schema.put(TYPE, OBJECT);
            var properties = new LinkedHashMap<String, Object>();
            var required = new ArrayList<String>();
            for (var child : node.children.values()) {
                properties.put(child.name, schemaOf(child));
                if (child.required) {
                    required.add(child.name);
                }
            }
            schema.put(PROPERTIES, properties);
            if (!required.isEmpty()) {
                schema.put(REQUIRED, required);
            }
        } else if (node.type != null) {
            schema.put(TYPE, node.type);
        }
        if (describe && !node.declared && !node.usedBy.isEmpty() && node.children.isEmpty()) {
            schema.put(DESCRIPTION, "Referenced by " + String.join(", ", node.usedBy));
        }
        schema.putAll(fragment);
    }

    /** The contract view: the merged schemas with their evidence, for the Playground and for agents. */
    public Map<String, Object> toMap() {
        var view = new LinkedHashMap<String, Object>();
        view.put("graph", graphId);
        if (purpose != null) {
            view.put(PURPOSE, purpose);
        }
        var input = new LinkedHashMap<String, Object>();
        input.put(BODY, namespaceView(INPUT_BODY));
        input.put(HEADER, namespaceView(INPUT_HEADER));
        view.put(INPUT, input);
        var output = new LinkedHashMap<String, Object>();
        output.put(BODY, namespaceView(OUTPUT_BODY));
        output.put(HEADER, namespaceView(OUTPUT_HEADER));
        output.put("status", new ArrayList<>(statusCodes));
        view.put(OUTPUT, output);
        view.put("issues", new ArrayList<>(issues));
        return view;
    }

    private Map<String, Object> namespaceView(String namespace) {
        var view = new LinkedHashMap<String, Object>();
        view.put("declared", isDeclared(namespace));
        var schema = schema(namespace);
        if (!schema.isEmpty()) {
            view.put(SCHEMA, schema);
        }
        var paths = new ArrayList<Map<String, Object>>();
        walk(roots.get(namespace), namespace, (path, node) -> {
            var entry = new LinkedHashMap<String, Object>();
            entry.put("path", path);
            if (node.array) {
                entry.put(TYPE, ARRAY);
            } else if (node.type != null) {
                entry.put(TYPE, node.type);
            } else if (!node.children.isEmpty()) {
                entry.put(TYPE, OBJECT);
            }
            entry.put("origin", originOf(node));
            if (node.required) {
                entry.put(REQUIRED, true);
            }
            if (!node.usedBy.isEmpty()) {
                entry.put("usedBy", new ArrayList<>(node.usedBy));
            }
            paths.add(entry);
        });
        view.put("paths", paths);
        return view;
    }

    private static String originOf(Node node) {
        if (node.declared && node.discovered) {
            return "both";
        }
        return node.declared ? "declared" : "discovered";
    }

    /** The lines of {@code describe graph}: the surfaces with their types, then the declaration. */
    public String describe() {
        var sb = new StringBuilder();
        sb.append("Input surface:\n");
        var inputs = surfaceLines(INPUT_BODY);
        inputs.addAll(surfaceLines(INPUT_HEADER));
        if (inputs.isEmpty()) {
            sb.append("  (none referenced)\n");
        }
        inputs.forEach(line -> sb.append("  ").append(line).append('\n'));
        sb.append("Output surface:\n");
        var outputs = surfaceLines(OUTPUT_BODY);
        outputs.addAll(surfaceLines(OUTPUT_HEADER));
        if (!statusCodes.isEmpty()) {
            var codes = new ArrayList<String>();
            statusCodes.forEach(c -> codes.add(String.valueOf(c)));
            outputs.add(OUTPUT_STATUS + " " + String.join(", ", codes));
        }
        if (outputs.isEmpty()) {
            sb.append("  (none referenced)\n");
        }
        outputs.forEach(line -> sb.append("  ").append(line).append('\n'));
        var parts = new ArrayList<String>();
        if (isDeclared(INPUT_BODY) || isDeclared(INPUT_HEADER)) {
            parts.add(INPUT);
        }
        if (isDeclared(OUTPUT_BODY) || isDeclared(OUTPUT_HEADER)) {
            parts.add(OUTPUT);
        }
        sb.append("Declared schema: ").append(parts.isEmpty() ? "none" : String.join(", ", parts)).append('\n');
        return sb.toString();
    }

    private List<String> surfaceLines(String namespace) {
        var lines = new ArrayList<String>();
        var root = roots.get(namespace);
        if (root.discovered) {
            lines.add(root.type == null ? namespace : namespace + " (" + root.type + ")");
        }
        walk(root, namespace, (path, node) -> {
            if (!node.children.isEmpty()) {
                return;
            }
            var line = new StringBuilder(path);
            if (node.type != null) {
                line.append(" (").append(node.type).append(')');
            }
            if (node.declared && !node.discovered) {
                line.append(" [declared]");
            }
            lines.add(line.toString());
        });
        return lines;
    }

    /** Unmodifiable: the issues found when reconciling the declaration with the model. */
    public List<String> issues() {
        return Collections.unmodifiableList(issues);
    }
}
