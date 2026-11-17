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

package com.accenture.minigraph.tests;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A scripted stand-in for the routes of a language pack's built-in agent tools (RFC-0013), so the
 * site-notes, document-ingest and document-query demo graphs run in the test scope without a pack,
 * a network or a graph database.
 * <p>
 * In production each route resolves to {@code extensions/agent-tools} of mercury-python or mercury-nodejs
 * through declarative Event-over-HTTP; here the same route names resolve to this one function, which
 * keeps the tools' result shapes and refusals: {@code web.crawl} and {@code web.fetch} answer a small
 * documentation site of two pages for one allowed host and refuse any other host by the allowlist rule;
 * {@code file.write} records the writes it is asked for (a second {@code create} of the same path is a 409);
 * {@code file.extract} reads the synthetic corpus of this application; {@code graph.ingest},
 * {@code graph.schema} and {@code graph.query} keep an in-memory graph - nodes by key with their label,
 * properties and source - and answer a query with the nodes of the first label it names.
 */
@PreLoad(route = "web.crawl, web.fetch, file.write, file.extract, graph.ingest, graph.schema, graph.query", instances = 10)
public class MockAgentTools implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    /** The documentation site the fake allowlist names. */
    public static final String SITE = "https://docs.example.test";
    public static final String SECTION = SITE + "/guides/configuration/";
    /** The writes {@code file.write} was asked for: workspace path to content. */
    public static final ConcurrentMap<String, String> WRITES = new ConcurrentHashMap<>();
    /** The in-memory graph: node key to {label, properties}. */
    public static final ConcurrentMap<String, Map<String, Object>> NODES = new ConcurrentHashMap<>();
    public static final List<Map<String, Object>> RELATIONSHIPS = new CopyOnWriteArrayList<>();
    /** Every call, as route names in order. */
    public static final List<String> CALLS = new CopyOnWriteArrayList<>();
    private static final Path CORPUS = Path.of("corpus");
    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,63}$");
    private static final Pattern LABEL_IN_QUERY = Pattern.compile(":`?([A-Za-z][A-Za-z0-9_]*)`?");
    private static final Pattern WRITE_CLAUSE = Pattern.compile("\\b(CREATE|MERGE|SET|DELETE|REMOVE|DROP|LOAD\\s+CSV)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final String PATH = "path";
    private static final String TEXT = "text";
    private static final String TITLE = "title";
    private static final String URL = "url";
    private static final String TRUNCATED = "truncated";
    private static final String PROPERTIES = "properties";
    private static final String LABEL = "label";
    private static final String SOURCE = "source";
    private static final String KEY = "key";

    /** Forget every write, node and relationship - a test's clean slate. */
    public static void reset() {
        WRITES.clear();
        NODES.clear();
        RELATIONSHIPS.clear();
        CALLS.clear();
    }

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws AppException {
        String route = headers.get("my_route");
        CALLS.add(route);
        return switch (route) {
            case "web.crawl" -> crawl(input);
            case "web.fetch" -> fetch(input);
            case "file.write" -> write(input);
            case "file.extract" -> extract(input);
            case "graph.ingest" -> ingest(input);
            case "graph.schema" -> schema();
            case "graph.query" -> query(input);
            default -> throw new AppException(404, "Route " + route + " not found");
        };
    }

    private static String allowedUrl(Map<String, Object> input) throws AppException {
        String url = text(input, URL);
        URI uri = URI.create(url.trim());
        if (uri.getHost() == null || !URI.create(SITE).getHost().equals(uri.getHost())) {
            throw new AppException(400, (uri.getHost() == null ? url : uri.getHost())
                    + " is not one of the sites this agent may reach (tools.web.allow)");
        }
        return url.trim();
    }

    private static Map<String, Object> page(String url, String title, String text) {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put(URL, url);
        page.put(TITLE, title);
        page.put(TEXT, text);
        page.put("bytes", text.length() * 3);
        return page;
    }

    private Map<String, Object> crawl(Map<String, Object> input) throws AppException {
        String url = allowedUrl(input);
        List<Map<String, Object>> pages = List.of(
                page(url, "Configuration", "The configuration keys of the platform: application.name names the "
                        + "application, rest.server.port the port, and log.format the log presentation (text, json or compact). "
                        + "A value may carry ${ENV_VAR:default} references, resolved at load."),
                page(url + "overrides/", "Runtime overrides", "Any key can be overridden at run time with -Dkey=value, "
                        + "and a -D value is substituted like a file value, so a credential travels as -Dkey=${SECRET}."));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pages", pages);
        result.put("visited", pages.size());
        result.put(TRUNCATED, false);
        return result;
    }

    private Map<String, Object> fetch(Map<String, Object> input) throws AppException {
        String url = allowedUrl(input);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(URL, url);
        result.put("status", 200);
        result.put("content_type", "text/html");
        result.put(TITLE, "Configuration");
        result.put(TEXT, "The configuration keys of the platform, one page.");
        result.put("bytes", 512);
        result.put(TRUNCATED, false);
        return result;
    }

    private Map<String, Object> write(Map<String, Object> input) throws AppException {
        String path = text(input, PATH);
        String content = text(input, "content");
        String mode = input.get("mode") == null ? "create" : String.valueOf(input.get("mode"));
        if (path.startsWith("/") || path.contains("..")) {
            throw new AppException(400, "path is outside the workspace");
        }
        if ("create".equals(mode) && WRITES.containsKey(path)) {
            throw new AppException(409, path + " exists - mode overwrite replaces it, append extends it");
        }
        WRITES.merge(path, content, (old, added) -> "append".equals(mode) ? old + added : added);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(PATH, path);
        result.put("bytes", content.getBytes().length);
        result.put("mode", mode);
        return result;
    }

    private Map<String, Object> extract(Map<String, Object> input) throws AppException {
        String path = text(input, PATH);
        if (path.startsWith("/") || path.contains("..")) {
            throw new AppException(400, "path is outside the workspace");
        }
        Path file = CORPUS.resolve(path);
        if (!Files.isRegularFile(file)) {
            throw new AppException(404, "file not found: " + path);
        }
        try {
            String content = Files.readString(file).strip();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put(PATH, path);
            result.put("type", "text");
            result.put("pages", 1);
            result.put(TEXT, "--- page 1 ---\n" + content);
            result.put(TRUNCATED, false);
            return result;
        } catch (IOException e) {
            throw new AppException(400, path + " could not be read (" + e.getClass().getSimpleName() + ")");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> ingest(Map<String, Object> input) throws AppException {
        List<Map<String, Object>> nodes = input.get("nodes") instanceof List<?> list ?
                (List<Map<String, Object>>) list : List.of();
        List<Map<String, Object>> relationships = input.get("relationships") instanceof List<?> list ?
                (List<Map<String, Object>>) list : List.of();
        if (nodes.isEmpty() && relationships.isEmpty()) {
            throw new AppException(400, "nothing to ingest - give nodes, relationships or both");
        }
        Object source = input.get(SOURCE);
        for (Map<String, Object> node : nodes) {
            String label = String.valueOf(node.get(LABEL));
            if (!IDENTIFIER.matcher(label).matches()) {
                throw new AppException(400, "label must be an identifier - letters, digits and underscore, starting with a letter");
            }
            Map<String, Object> properties = new LinkedHashMap<>();
            if (node.get(PROPERTIES) instanceof Map<?, ?> given) {
                given.forEach((k, v) -> properties.put(String.valueOf(k), v));
            }
            properties.put(KEY, String.valueOf(node.get(KEY)));
            if (source != null) {
                properties.put(SOURCE, source);
            }
            Map<String, Object> stored = new LinkedHashMap<>();
            stored.put(LABEL, label);
            stored.put(PROPERTIES, properties);
            NODES.put(String.valueOf(node.get(KEY)), stored);
        }
        for (Map<String, Object> relationship : relationships) {
            for (String end : List.of("from", "to")) {
                if (!NODES.containsKey(String.valueOf(relationship.get(end)))) {
                    throw new AppException(400, "relationship endpoints not found in the graph or in this call: "
                            + relationship.get(end));
                }
            }
            Map<String, Object> stored = new LinkedHashMap<>(relationship);
            if (source != null) {
                stored.put(SOURCE, source);
            }
            RELATIONSHIPS.add(stored);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", nodes.size());
        result.put("relationships", relationships.size());
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> schema() {
        Map<String, Map<String, Object>> labels = new TreeMap<>();
        for (Map<String, Object> node : NODES.values()) {
            String label = String.valueOf(node.get(LABEL));
            Map<String, Object> entry = labels.computeIfAbsent(label, l -> {
                Map<String, Object> created = new LinkedHashMap<>();
                created.put(LABEL, l);
                created.put("count", 0);
                created.put(PROPERTIES, new ArrayList<Map<String, Object>>());
                return created;
            });
            entry.put("count", (Integer) entry.get("count") + 1);
            List<Map<String, Object>> properties = (List<Map<String, Object>>) entry.get(PROPERTIES);
            for (String name : ((Map<String, Object>) node.get(PROPERTIES)).keySet()) {
                if (properties.stream().noneMatch(p -> name.equals(p.get("name")))) {
                    properties.add(Map.of("name", name, "types", List.of("String")));
                }
            }
        }
        Map<String, Map<String, Object>> kinds = new TreeMap<>();
        for (Map<String, Object> relationship : RELATIONSHIPS) {
            String type = String.valueOf(relationship.get("type"));
            Map<String, Object> entry = kinds.computeIfAbsent(type, t -> {
                Map<String, Object> created = new LinkedHashMap<>();
                created.put("type", t);
                created.put("count", 0);
                created.put("from", new ArrayList<String>());
                created.put("to", new ArrayList<String>());
                created.put(PROPERTIES, new ArrayList<Map<String, Object>>());
                return created;
            });
            entry.put("count", (Integer) entry.get("count") + 1);
            addLabel((List<String>) entry.get("from"), String.valueOf(relationship.get("from")));
            addLabel((List<String>) entry.get("to"), String.valueOf(relationship.get("to")));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", new ArrayList<>(labels.values()));
        result.put("relationship_types", new ArrayList<>(kinds.values()));
        return result;
    }

    private static void addLabel(List<String> labels, String key) {
        Map<String, Object> node = NODES.get(key);
        if (node != null) {
            String label = String.valueOf(node.get(LABEL));
            if (!labels.contains(label)) {
                labels.add(label);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> query(Map<String, Object> input) throws AppException {
        String query = text(input, "query");
        Matcher clause = WRITE_CLAUSE.matcher(query);
        if (clause.find()) {
            throw new AppException(400, "query carries " + clause.group(1).toUpperCase() + ", a write clause - graph_query is read-only; "
                    + "graph_write or graph_ingest writes");
        }
        Matcher label = LABEL_IN_QUERY.matcher(query);
        List<List<Object>> rows = new ArrayList<>();
        if (label.find()) {
            String wanted = label.group(1);
            new TreeMap<>(NODES).forEach((key, node) -> {
                if (wanted.equals(node.get(LABEL))) {
                    Map<String, Object> rendered = new LinkedHashMap<>();
                    rendered.put("id", "4:test:" + key);
                    rendered.put("labels", List.of(wanted));
                    rendered.put(PROPERTIES, node.get(PROPERTIES));
                    rows.add(List.of(rendered));
                }
            });
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", List.of("n"));
        result.put("rows", rows);
        result.put(TRUNCATED, false);
        return result;
    }

    private static String text(Map<String, Object> input, String name) throws AppException {
        Object value = input.get(name);
        if (!(value instanceof String s) || s.isBlank()) {
            throw new AppException(400, name + " is required");
        }
        return s;
    }
}
