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

package com.accenture.automation;

import org.platformlambda.core.util.Utility;

import java.util.Set;

/**
 * Converts the deprecated "simple type matching" syntax (model.someKey:type) found in
 * event flow YAML data mappings into the equivalent "simple plugin" syntax (f:type(model.someKey)).
 * <p>
 * CompileFlows applies this converter to every input/output data mapping entry at compile time so
 * that TaskExecutor only has to deal with the simple plugin syntax at runtime. This keeps backward
 * compatibility with existing event flow configuration files while allowing the runtime engine to
 * be simplified.
 * <p>
 * When a colon type qualifier is not recognized or is malformed (e.g. unbalanced brackets), the
 * entry is returned unchanged so that the legacy type matching resolution in DataMappingHelper
 * continues to handle it.
 * <p>
 * Intentional singleton
 */
@SuppressWarnings("java:S6548")
public class SimpleTypeMatchingConverter {
    private static final SimpleTypeMatchingConverter INSTANCE = new SimpleTypeMatchingConverter();
    private static final Utility util = Utility.getInstance();
    private static final String MODEL_NAMESPACE = "model.";
    private static final String MAP_TO = "->";
    private static final String SIMPLE_PLUGIN_PREFIX = "f:";
    private static final String TEXT_TYPE = "text(";
    private static final String NEGATE = "!";
    private static final String AND_TYPE = "and(";
    private static final String OR_TYPE = "or(";
    private static final String CONCAT_TYPE = "concat(";
    private static final String SUBSTRING_TYPE = "substring(";
    private static final String BOOLEAN_TYPE = "boolean";
    private static final String NULL_TOKEN = "null";
    private static final String TRUE_TOKEN = "true";
    private static final String CLOSE_BRACKET = ")";
    private static final Set<String> SIMPLE_TYPES = Set.of(
            "text", "binary", "int", "long", "float", "double", BOOLEAN_TYPE, "uuid", "length", "b64");

    private SimpleTypeMatchingConverter() {
        // singleton
    }

    public static SimpleTypeMatchingConverter getInstance() {
        return INSTANCE;
    }

    /**
     * Convert a single "LHS -> RHS" data mapping entry.
     *
     * @param entry a normalized 2-part data mapping entry
     * @return the converted entry, or the original entry if there is nothing to convert
     */
    public String convert(String entry) {
        int sep = entry.lastIndexOf(MAP_TO);
        if (sep < 0) {
            return entry;
        }
        String lhs = entry.substring(0, sep).trim();
        String rhs = entry.substring(sep + MAP_TO.length()).trim();
        int lhsColon = typeColonIndex(lhs);
        if (lhsColon > 0) {
            String key = lhs.substring(0, lhsColon).trim();
            String plugin = toPluginCall(key, lhs.substring(lhsColon + 1).trim());
            return plugin != null? plugin + " -> " + rhs : entry;
        }
        int rhsColon = typeColonIndex(rhs);
        if (rhsColon > 0) {
            String key = rhs.substring(0, rhsColon).trim();
            String plugin = toPluginCall(lhs, rhs.substring(rhsColon + 1).trim());
            return plugin != null? plugin + " -> " + key : entry;
        }
        return entry;
    }

    private int typeColonIndex(String token) {
        return token.startsWith(MODEL_NAMESPACE)? token.indexOf(':') : -1;
    }

    private String toPluginCall(String source, String qualifier) {
        if (SIMPLE_TYPES.contains(qualifier)) {
            return SIMPLE_PLUGIN_PREFIX + qualifier + "(" + source + ")";
        }
        if (NEGATE.equals(qualifier)) {
            return SIMPLE_PLUGIN_PREFIX + "not(" + source + ")";
        }
        if (qualifier.startsWith(AND_TYPE) && qualifier.endsWith(CLOSE_BRACKET)) {
            var inner = qualifier.substring(AND_TYPE.length(), qualifier.length() - 1).trim();
            return SIMPLE_PLUGIN_PREFIX + "and(" + source + ", " + inner + ")";
        }
        if (qualifier.startsWith(OR_TYPE) && qualifier.endsWith(CLOSE_BRACKET)) {
            var inner = qualifier.substring(OR_TYPE.length(), qualifier.length() - 1).trim();
            return SIMPLE_PLUGIN_PREFIX + "or(" + source + ", " + inner + ")";
        }
        if (qualifier.startsWith(CONCAT_TYPE) && qualifier.endsWith(CLOSE_BRACKET)) {
            var inner = qualifier.substring(CONCAT_TYPE.length(), qualifier.length() - 1).trim();
            return SIMPLE_PLUGIN_PREFIX + CONCAT_TYPE + source + ", " + inner + ")";
        }
        if (qualifier.startsWith(SUBSTRING_TYPE) && qualifier.endsWith(CLOSE_BRACKET)) {
            var inner = qualifier.substring(SUBSTRING_TYPE.length(), qualifier.length() - 1);
            return convertSubstring(source, inner);
        }
        if (isBooleanValueMatch(qualifier)) {
            return convertBooleanValueMatch(source, qualifier);
        }
        // unrecognized or malformed qualifier - leave unconverted for legacy resolution
        return null;
    }

    private String convertSubstring(String source, String inner) {
        StringBuilder args = new StringBuilder(source);
        for (String part: util.split(inner, ",")) {
            var v = part.trim();
            args.append(", ").append(v.startsWith(MODEL_NAMESPACE)? v : "int(" + v + ")");
        }
        return SIMPLE_PLUGIN_PREFIX + SUBSTRING_TYPE + args + ")";
    }

    private boolean isBooleanValueMatch(String qualifier) {
        if (!qualifier.startsWith(BOOLEAN_TYPE) || !qualifier.endsWith(CLOSE_BRACKET)) {
            return false;
        }
        return qualifier.substring(BOOLEAN_TYPE.length()).trim().startsWith("(");
    }

    private String convertBooleanValueMatch(String source, String qualifier) {
        int open = qualifier.indexOf('(');
        String inner = qualifier.substring(open + 1, qualifier.length() - 1).trim();
        int eq = inner.indexOf('=');
        String token = (eq < 0? inner : inner.substring(0, eq)).trim();
        String flag = eq < 0? null : inner.substring(eq + 1).trim();
        boolean conditionTrue = flag == null || TRUE_TOKEN.equalsIgnoreCase(flag);
        if (NULL_TOKEN.equals(token)) {
            return SIMPLE_PLUGIN_PREFIX + (conditionTrue? "isNull(" : "notNull(") + source + ")";
        }
        // string-coerce a model variable so that value matching behaves the same regardless of its actual type
        String compared = source.startsWith(MODEL_NAMESPACE)? source + ":text" : source;
        return SIMPLE_PLUGIN_PREFIX + (conditionTrue? "eq(" : "ne(") + compared + ", " + TEXT_TYPE + token + "))";
    }
}
