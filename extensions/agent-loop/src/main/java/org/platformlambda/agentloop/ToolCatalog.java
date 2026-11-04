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

import org.platformlambda.core.serializers.SimpleMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A loop node's tool catalog (RFC-0011, section 5): the rows the model may call, each mapping an API-safe name to
 * a composable-function route. The catalog is graph data, certified with the graph, so it is checked whole before
 * the first model turn: a name the API would refuse, a duplicate, a schema outside the vocabulary, an unknown
 * approval value, a field that is not a row's, or a route nothing serves. Any of them throws before a token is
 * spent - the loop's failure, unlike a name the model invents, which goes back to the model as an error result.
 */
public final class ToolCatalog {
    static final Pattern NAME = Pattern.compile("^[a-zA-Z0-9_-]{1,128}$");
    private static final String NAME_KEY = "name";
    private static final String DESCRIPTION = "description";
    private static final String INPUT_SCHEMA = "input_schema";
    private static final String ROUTE = "route";
    private static final String STRICT = "strict";
    private static final String APPROVAL = "approval";
    private static final String REQUIRED = "required";
    private static final List<String> FIELDS = List.of(NAME_KEY, DESCRIPTION, INPUT_SCHEMA, ROUTE, STRICT, APPROVAL);

    /**
     * One row of the catalog.
     *
     * @param name the name advertised to the model
     * @param description the description written on the graph
     * @param inputSchema the schema as written, advertised to the model
     * @param schema the compiled schema the loop validates against
     * @param route the composable-function route
     * @param strict grammar-constrained arguments, or null when the row does not say
     * @param approval true when the row is a high-risk write that waits for a person (mode auto)
     */
    public record Row(String name, String description, Map<String, Object> inputSchema, ToolSchema schema,
                      String route, Boolean strict, boolean approval) { }

    private final Map<String, Row> rows = new LinkedHashMap<>();

    private ToolCatalog() { }

    /**
     * Parse and check a catalog.
     *
     * @param tools the node's {@code tools} input: a list of rows, or the JSON text of one
     * @param reachable answers whether a route is served (local, an Event-over-HTTP target, or the mesh)
     * @return the catalog
     * @throws IllegalArgumentException naming the first problem
     */
    @SuppressWarnings("unchecked")
    public static ToolCatalog parse(Object tools, Predicate<String> reachable) {
        var list = asList(tools);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("tools is empty - a loop needs at least one tool " +
                    "(call llm.chat for a single model turn)");
        }
        var catalog = new ToolCatalog();
        for (int i = 0; i < list.size(); i++) {
            var where = "tools[" + i + "]";
            if (!(list.get(i) instanceof Map)) {
                throw new IllegalArgumentException(where + " must be a map");
            }
            var row = (Map<String, Object>) list.get(i);
            for (var key : new TreeSet<>(row.keySet())) {
                if (!FIELDS.contains(key)) {
                    throw new IllegalArgumentException(where + "." + key + " is not a field of a tool row - " +
                            "the fields are name, description, input_schema, route, strict and approval");
                }
            }
            var parsed = parseRow(where, row, reachable, catalog.rows.keySet());
            catalog.rows.put(parsed.name(), parsed);
        }
        return catalog;
    }

    @SuppressWarnings("unchecked")
    private static Row parseRow(String where, Map<String, Object> row, Predicate<String> reachable,
                                Set<String> taken) {
        if (!(row.get(NAME_KEY) instanceof String name) || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(where + ".name must match " + NAME.pattern());
        }
        if (taken.contains(name)) {
            throw new IllegalArgumentException(where + ".name '" + name + "' is not unique");
        }
        if (!(row.get(DESCRIPTION) instanceof String description) || description.isBlank()) {
            throw new IllegalArgumentException(where + ".description is missing - the graph writes what the " +
                    "model reads about a tool");
        }
        var schema = ToolSchema.compile(where + "." + INPUT_SCHEMA, row.get(INPUT_SCHEMA));
        if (!schema.types().equals(List.of(ToolSchema.OBJECT))) {
            throw new IllegalArgumentException(where + "." + INPUT_SCHEMA + ".type must be 'object'");
        }
        if (!(row.get(ROUTE) instanceof String route) || route.isBlank()) {
            throw new IllegalArgumentException(where + ".route is missing");
        }
        var target = route.trim();
        if (!reachable.test(target)) {
            throw new IllegalArgumentException(where + ".route '" + target + "' is not reachable - no local " +
                    "function, Event-over-HTTP target or mesh route serves it");
        }
        var strict = row.get(STRICT);
        if (strict != null && !(strict instanceof Boolean)) {
            throw new IllegalArgumentException(where + ".strict must be true or false");
        }
        var approval = row.get(APPROVAL);
        if (approval != null && !REQUIRED.equals(approval)) {
            throw new IllegalArgumentException(where + ".approval must be 'required' when set");
        }
        return new Row(name, description, (Map<String, Object>) row.get(INPUT_SCHEMA), schema, target,
                (Boolean) strict, approval != null);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object tools) {
        if (tools instanceof List<?> list) {
            return (List<Object>) list;
        }
        if (tools instanceof String text && !text.isBlank()) {
            Object parsed;
            try {
                parsed = SimpleMapper.getInstance().getMapper().readValue(text, List.class);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("tools is not a JSON list of tool rows");
            }
            if (parsed instanceof List<?> list) {
                return (List<Object>) list;
            }
        }
        throw new IllegalArgumentException(tools == null ? "tools is required - the loop's tool catalog" :
                "tools must be a list of tool rows, or its JSON text");
    }

    /**
     * Find a row by the name the model used.
     *
     * @param name the name
     * @return the row, or null when the catalog has none
     */
    public Row get(String name) {
        return name == null ? null : rows.get(name);
    }

    /**
     * The rows in catalog order.
     *
     * @return the rows
     */
    public List<Row> rows() {
        return new ArrayList<>(rows.values());
    }

    /**
     * The tools as the model sees them: name, description, input schema and strict. The route and the approval
     * mark stay in the catalog.
     *
     * @return the advertised tools, in catalog order
     */
    public List<Map<String, Object>> advertised() {
        var result = new ArrayList<Map<String, Object>>();
        for (var row : rows.values()) {
            var tool = new LinkedHashMap<String, Object>();
            tool.put(NAME_KEY, row.name());
            tool.put(DESCRIPTION, row.description());
            tool.put(INPUT_SCHEMA, row.inputSchema());
            if (row.strict() != null) {
                tool.put(STRICT, row.strict());
            }
            result.add(tool);
        }
        return result;
    }
}
