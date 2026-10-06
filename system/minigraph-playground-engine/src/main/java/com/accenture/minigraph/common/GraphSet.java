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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.platformlambda.core.serializers.CanonicalPackager;
import org.platformlambda.core.util.ConfigReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A graph set: graph models delivered together as one canonical package (RFC-0005; the package format is ADR-0026).
 * <p>
 * One package is one set, and its file is {@code <set>.pack}. Each graph is one entry named {@code <graph-id>.json},
 * whose id follows the file-name rule and, when the root node declares a 'name', equals it. The manifest holds the
 * packager's 'format' and 'format_version', the set name in 'set', and caller fields as text - for example 'version',
 * 'description' or 'author', and the optional 'graph_id' naming the set's entry-point graph. Nothing is taken from
 * the environment or the clock, so the same graphs and fields always give the same bytes.
 * <p>
 * A graph holds no null property: "key": null is filtered out when a set is packed or read, as the engine's
 * serializer does by default ({@link GraphModelGate#withoutNullProperties(Map)}). An empty string is a value and is
 * kept.
 * <p>
 * The rules live here so the graph packager, the deployment of packaged sets and the Playground apply the same ones.
 */
public final class GraphSet {
    public static final String EXTENSION = ".pack";
    public static final String SET = "set";
    public static final String GRAPH_ID = "graph_id";
    private static final String JSON_EXT = ".json";
    private static final String ID_RULE = "use letters, digits, '_' and '-' only";
    private static final Gson JSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private GraphSet() {
        // utility class
    }

    /**
     * The content of a graph set that passed the read checks.
     *
     * @param manifest the manifest fields, in the order the package holds them
     * @param graphs the graph models keyed by graph id, in the order the package holds them
     */
    public record Contents(Map<String, String> manifest, Map<String, Map<String, Object>> graphs) {
    }

    /**
     * A set that breaks a rule, with every reason found.
     */
    public static final class RefusedException extends IllegalArgumentException {
        private final transient List<String> reasons;

        public RefusedException(List<String> reasons) {
            super(String.join("; ", reasons));
            this.reasons = List.copyOf(reasons);
        }

        /**
         * @return each reason the set is refused, one per broken rule
         */
        public List<String> getReasons() {
            return reasons;
        }
    }

    /**
     * Pack graph models into a set. Every rule is checked before anything is packed - the set name and each graph id
     * against the file-name rule, each root 'name' against its graph id, the 'graph_id' field against the graphs.
     * Each model must pass the deployment gate's checks, and the set is refused with every reason when any rule fails.
     * A null property is filtered out first: a graph holds none.
     * <p>
     * The gate checks a copy read the way the gate reads a deployed model, normalized and with its ${...} references
     * resolved, while the model is packed as written: a reference belongs to the environment the set is deployed to,
     * and resolving it here would make the bytes depend on the machine that packs them.
     *
     * @param setName the set name, which names the file
     * @param fields caller manifest fields as text; 'set' is written from the set name
     * @param graphs the graph models keyed by graph id
     * @return the package bytes
     * @throws RefusedException naming every broken rule
     * @throws IOException when a value cannot be written
     */
    public static byte[] pack(String setName, Map<String, String> fields, Map<String, Map<String, Object>> graphs)
            throws IOException {
        List<String> reasons = new ArrayList<>();
        if (!GraphModelGate.isValidGraphId(setName)) {
            reasons.add("set name '" + setName + "' - " + ID_RULE);
        }
        for (var key : fields.keySet()) {
            if (key == null || key.isEmpty()) {
                reasons.add("a manifest field needs a name");
            } else if (SET.equals(key)) {
                reasons.add("manifest field '" + SET + "' - it is written from the set name");
            } else if (CanonicalPackager.FORMAT_KEY.equals(key) || CanonicalPackager.FORMAT_VERSION_KEY.equals(key)) {
                reasons.add("manifest field '" + key + "' - it is written by the packager");
            }
        }
        if (graphs.isEmpty()) {
            reasons.add("a set needs at least one graph");
        }
        var entryPoint = fields.get(GRAPH_ID);
        if (entryPoint != null && !graphs.containsKey(entryPoint)) {
            reasons.add("manifest field '" + GRAPH_ID + "' - '" + entryPoint + "' is not a graph of the set");
        }
        // a graph holds no null property: "key": null is filtered out before the checks and the pack
        Map<String, Map<String, Object>> models = new TreeMap<>();
        graphs.forEach((id, model) -> models.put(id, GraphModelGate.withoutNullProperties(model)));
        for (var graph : models.entrySet()) {
            var reason = check(graph.getKey(), graph.getValue());
            if (reason != null) {
                reasons.add(graph.getKey() + ": " + reason);
            }
        }
        if (!reasons.isEmpty()) {
            throw new RefusedException(reasons);
        }
        var builder = CanonicalPackager.builder();
        fields.forEach(builder::manifest);
        builder.manifest(SET, setName);
        models.forEach((id, model) -> builder.add(id + JSON_EXT, model));
        return builder.build();
    }

    /**
     * Read a set: the strict canonical read, then the entry names, the root names and the 'graph_id' field are
     * checked before anything is built from them, so a crafted entry name never becomes a path.
     *
     * @param bytes a package
     * @return the manifest and the graph models
     * @throws IOException when the bytes are not a canonical package
     * @throws RefusedException naming every broken rule
     */
    public static Contents read(byte[] bytes) throws IOException {
        var pkg = CanonicalPackager.unpack(bytes);
        List<String> reasons = new ArrayList<>();
        Map<String, Map<String, Object>> graphs = new LinkedHashMap<>();
        for (var entry : pkg.maps().entrySet()) {
            var name = entry.getKey();
            var id = name.endsWith(JSON_EXT) ? name.substring(0, name.length() - JSON_EXT.length()) : "";
            if (!GraphModelGate.isValidGraphId(id)) {
                reasons.add("entry '" + name + "' - expect <graph-id>.json, the id in letters, digits, '_' and '-'");
                continue;
            }
            var declared = GraphModelGate.declaredRootName(entry.getValue());
            if (!declared.isEmpty() && !declared.equals(id)) {
                reasons.add(id + ": " + rootNameDiffers(declared));
            }
            var binary = findBinary(entry.getValue(), "");
            if (binary != null) {
                reasons.add(id + ": binary data at '" + binary + "' - a graph model is JSON");
            }
            graphs.put(id, GraphModelGate.withoutNullProperties(entry.getValue()));
        }
        if (pkg.maps().isEmpty()) {
            reasons.add("the package holds no graph");
        }
        var entryPoint = pkg.manifest().get(GRAPH_ID);
        if (entryPoint != null && !graphs.containsKey(entryPoint)) {
            reasons.add("manifest field '" + GRAPH_ID + "' - '" + entryPoint + "' is not a graph of the set");
        }
        if (!reasons.isEmpty()) {
            throw new RefusedException(reasons);
        }
        return new Contents(pkg.manifest(), graphs);
    }

    /**
     * Readable JSON for a graph model taken out of a set: the canonical key order the package holds and a two-space
     * indent, so two versions of a set diff cleanly and the file packs to the same bytes again.
     *
     * @param model a graph model from {@link #read(byte[])}
     * @return the JSON text, ending with a new line
     */
    public static String toJson(Map<String, Object> model) {
        return JSON.toJson(model) + "\n";
    }

    private static String check(String graphId, Map<String, Object> model) {
        if (!GraphModelGate.isValidGraphId(graphId)) {
            return "graph id - " + ID_RULE;
        }
        var declared = GraphModelGate.declaredRootName(model);
        if (!declared.isEmpty() && !declared.equals(graphId)) {
            return rootNameDiffers(declared);
        }
        try {
            var asDeployed = new ConfigReader().load(copyOf(model)).getMap();
            GraphModelGate.validate(graphId, asDeployed);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private static String rootNameDiffers(String declared) {
        return "the root node's name '" + declared + "' differs from the graph id";
    }

    private static Map<String, Object> copyOf(Map<?, ?> map) {
        Map<String, Object> copy = new LinkedHashMap<>();
        map.forEach((k, v) -> copy.put(String.valueOf(k), copyOfValue(v)));
        return copy;
    }

    private static Object copyOfValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copyOf(map);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            list.forEach(v -> copy.add(copyOfValue(v)));
            return copy;
        }
        return value;
    }

    private static String findBinary(Object value, String path) {
        if (value instanceof byte[]) {
            return path;
        }
        if (value instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                var found = findBinary(entry.getValue(), path.isEmpty() ? String.valueOf(entry.getKey()) :
                        path + "." + entry.getKey());
                if (found != null) {
                    return found;
                }
            }
        }
        if (value instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                var found = findBinary(list.get(i), path + "[" + i + "]");
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
