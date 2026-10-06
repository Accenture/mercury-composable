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

package com.accenture.minigraph.common;

import com.accenture.automation.SimpleTypeMatchingConverter;
import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.util.MultiLevelMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The static checks of the deployment gate, as one method that CompileGraph, the graph packager and the
 * deployment of packaged graph sets share, so "passes the gate" means the same thing in every place (RFC-0005).
 * <p>
 * Every check reads the model alone - its structure, the root node's 'purpose', an 'end' node, the data mapping
 * syntax and the rules of GraphModelValidator - and none consults the functions of the target application. Therefore,
 * the gate runs as well when a graph set is packed as when an application starts.
 */
public final class GraphModelGate {
    private static final Logger log = LoggerFactory.getLogger(GraphModelGate.class);
    private static final SimpleTypeMatchingConverter converter = SimpleTypeMatchingConverter.getInstance();
    private static final String INPUT = "input";
    private static final String[] MAPPING_PROPERTIES = {"mapping", INPUT, "output", "for_each"};
    private static final String MAP_TO = "->";
    private static final String NODES = "nodes";
    private static final String ALIAS = "alias";
    private static final String ROOT = "root";
    private static final String PROPERTIES = "properties";
    private static final String PROPERTIES_SUFFIX = "].properties.";
    private static final String NODE_NAME = "node ";

    private GraphModelGate() {
        // utility class
    }

    /**
     * Validate a graph model the way the deployment gate does. The deprecated "simple type matching" syntax
     * (model.someKey:type) in the mapping, input, output and for_each properties is converted in place to the
     * equivalent simple plugin syntax (f:type(model.someKey)), so the model a caller registers afterward is the
     * converted one.
     *
     * @param graphId the graph id, for the log
     * @param model the graph model, converted in place
     * @return the imported graph
     * @throws IllegalArgumentException with the reason the model is rejected
     */
    public static MiniGraph validate(String graphId, Map<String, Object> model) {
        convertDataMappingEntries(graphId, model);
        // structural validation - throws IllegalArgumentException for a malformed graph
        var graph = new MiniGraph();
        graph.importGraph(model);
        // discovery contract: every deployable graph documents itself - the root
        // node's 'purpose' is what "list graphs" shows as living documentation
        if (!hasRootPurpose(model)) {
            throw new IllegalArgumentException("root node must define a non-empty 'purpose' property");
        }
        // every run must be able to complete - GraphExecutor trusts this at runtime
        if (graph.getEndNode() == null) {
            throw new IllegalArgumentException("graph must have an 'end' node");
        }
        GraphModelValidator.validate(graph);
        return graph;
    }

    /**
     * The file-name rule for a graph id: letters, digits, '_' and '-' only, so an id is always a safe file name.
     *
     * @param id a graph id
     * @return true when the id follows the rule
     */
    public static boolean isValidGraphId(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '-')) {
                return false;
            }
        }
        return true;
    }

    /**
     * The name the root node declares for its graph, as "export graph as" writes it.
     *
     * @param model a graph model
     * @return the root node's 'name' property, trimmed; empty when the root node has none
     */
    public static String declaredRootName(Map<String, Object> model) {
        if (model.get(NODES) instanceof List<?> nodes) {
            for (var n : nodes) {
                if (n instanceof Map<?, ?> node && ROOT.equals(node.get(ALIAS))) {
                    return node.get(PROPERTIES) instanceof Map<?, ?> properties && properties.get("name") != null ?
                            String.valueOf(properties.get("name")).trim() : "";
                }
            }
        }
        return "";
    }

    /**
     * A graph holds no null property: a map entry whose value is null is filtered out, at every depth, as the
     * engine's serializer does by default and as the configuration reader does when an application loads a deployed
     * graph. Every other value is kept as it is - an empty string ("key": "") is a value - and a list keeps its
     * elements in place.
     *
     * @param model a graph model
     * @return a copy of the model without its null properties
     */
    public static Map<String, Object> withoutNullProperties(Map<?, ?> model) {
        Map<String, Object> copy = new LinkedHashMap<>();
        model.forEach((k, v) -> {
            if (v != null) {
                copy.put(String.valueOf(k), withoutNullsIn(v));
            }
        });
        return copy;
    }

    private static Object withoutNullsIn(Object value) {
        if (value instanceof Map<?, ?> map) {
            return withoutNullProperties(map);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            list.forEach(v -> copy.add(withoutNullsIn(v)));
            return copy;
        }
        return value;
    }

    private static boolean hasRootPurpose(Map<String, Object> model) {
        if (model.get(NODES) instanceof List<?> nodes) {
            for (var n : nodes) {
                if (n instanceof Map<?, ?> node && ROOT.equals(node.get(ALIAS))) {
                    return node.get(PROPERTIES) instanceof Map<?, ?> properties
                            && properties.get("purpose") instanceof String purpose && !purpose.isBlank();
                }
            }
        }
        return false;
    }

    private static void convertDataMappingEntries(String graphId, Map<String, Object> model) {
        var mm = new MultiLevelMap(model);
        Object nodeList = model.get(NODES);
        if (nodeList instanceof List<?> nodes) {
            for (int i = 0; i < nodes.size(); i++) {
                for (String key : MAPPING_PROPERTIES) {
                    var path = NODES + "[" + i + PROPERTIES_SUFFIX + key;
                    if (mm.getElement(path) instanceof List<?> entries) {
                        mm.setElement(path, convertEntries(graphId, i, key, entries));
                    }
                }
            }
        }
    }

    private static List<String> convertEntries(String graphId, int nodeIndex, String property, List<?> entries) {
        List<String> converted = new ArrayList<>();
        for (Object o : entries) {
            var line = String.valueOf(o);
            if (line.contains(MAP_TO)) {
                var convertedLine = converter.convert(line);
                if (!convertedLine.equals(line)) {
                    log.warn("Deprecated syntax in graph {} node[{}].{} - '{}' converted to '{}'",
                            graphId, nodeIndex, property, line, convertedLine);
                }
                converted.add(convertedLine);
            } else if (INPUT.equals(property)) {
                // an 'input' entry without '->' is skill vocabulary, not a data mapping -
                // e.g. the fetcher's dictionary parameter names and feature flags
                converted.add(line);
            } else {
                // a mapping/for_each/output entry is always a data mapping: a line
                // without '->' is guaranteed to fail at runtime, so reject the graph
                // (the gate's promise is that a compiled graph is runnable)
                throw new IllegalArgumentException(NODE_NAME + "[" + nodeIndex + "]." + property +
                        " - missing '" + MAP_TO + "' in '" + line + "'");
            }
        }
        return converted;
    }
}
