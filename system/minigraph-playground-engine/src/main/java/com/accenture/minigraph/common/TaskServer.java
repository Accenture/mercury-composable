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

import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.models.SimpleNode;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * A task's provider named on the node (RFC-0017, D16): {@code server.url} is the Event-over-HTTP endpoint of the
 * engine or language pack that hosts the function the node's {@code task} names, and {@code server.header[]}
 * adds transport headers to the call, {@code name: value}, such as a gateway's token.
 * <p>
 * A hot-deployed graph may need a custom composable function the engine does not hold, and the engine's
 * Event-over-HTTP map ({@code yaml.event.over.http}) is loaded once at startup - so the node says where its
 * function is, and the graph reaches a function the map has never heard of without a restart. The property wins
 * over a local function and over the map: explicit beats implicit. A URL without a path gets {@code /api/event};
 * a URL with a path is used as written, which is how an API gateway's own path reaches the target. The values
 * are read from the compiled model, so a {@code ${...}} reference resolved when the graph was registered, once
 * per instance, from its own environment.
 */
public final class TaskServer {
    public static final String URL = "server.url";
    public static final String HEADER = "server.header";
    private static final String SERVER = "server";
    private static final String URL_LEAF = "url";
    private static final String HEADER_LEAF = "header";
    private static final String DEFAULT_PATH = "/api/event";
    private final String endpoint;
    private final Map<String, String> headers;

    private TaskServer(String endpoint, Map<String, String> headers) {
        this.endpoint = endpoint;
        this.headers = headers;
    }

    /**
     * The provider a node names, or null when the node has no {@code server.url}.
     *
     * @param node the task-consuming node
     * @return the provider, or null
     * @throws IllegalArgumentException when the URL or a header entry is malformed - the gate refuses such a
     *                                  graph, so this is reached by a draft in a dry run only
     */
    public static TaskServer of(SimpleNode node) {
        var url = urlOf(node);
        if (url == null) {
            return null;
        }
        var problem = checkUrl(url);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        return new TaskServer(normalize(String.valueOf(url).trim()), headersOf(headerOf(node)));
    }

    /**
     * The node's {@code server.url}. A compiled model holds it nested - the config reader that reads a graph back
     * normalizes a dotted key into {@code server: {url: ...}} - while a draft in the Playground holds the flat key
     * the author typed; both are read.
     *
     * @param node the node
     * @return the value, or null
     */
    public static Object urlOf(SimpleNode node) {
        return nestedOrFlat(node.getProperty(SERVER), URL_LEAF, node.getProperty(URL));
    }

    /**
     * The node's {@code server.header[]}, nested or flat as {@link #urlOf(SimpleNode)} reads the URL.
     *
     * @param node the node
     * @return the value, or null
     */
    public static Object headerOf(SimpleNode node) {
        return nestedOrFlat(node.getProperty(SERVER), HEADER_LEAF, node.getProperty(HEADER));
    }

    /**
     * Whether the node declares a provider at all, in either form - for the gate's placement rule.
     *
     * @param node the node
     * @return true when 'server', 'server.url' or 'server.header' is present
     */
    public static boolean declaredOn(SimpleNode node) {
        return node.getProperty(SERVER) != null || node.getProperty(URL) != null || node.getProperty(HEADER) != null;
    }

    /**
     * The {@code server.url} of a node's property map as a model file holds it, nested or flat.
     *
     * @param properties the node's properties
     * @return the value, or null
     */
    public static Object urlOf(Map<?, ?> properties) {
        return nestedOrFlat(properties.get(SERVER), URL_LEAF, properties.get(URL));
    }

    private static Object nestedOrFlat(Object server, String leaf, Object flat) {
        if (server instanceof Map<?, ?> map && map.get(leaf) != null) {
            return map.get(leaf);
        }
        return flat;
    }

    /**
     * Whether a {@code server.url} value is an absolute http or https URL.
     *
     * @param url the property's value
     * @return null when it is, else the reason it is not
     */
    public static String checkUrl(Object url) {
        var text = url == null? "" : String.valueOf(url).trim();
        if (text.isEmpty()) {
            return "'" + URL + "' is empty - a ${...} reference may be unresolved in this environment";
        }
        try {
            var uri = new URI(text);
            var scheme = uri.getScheme() == null? "" : uri.getScheme().toLowerCase();
            if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null) {
                return "'" + URL + "' must be an absolute http or https URL, not '" + text + "'";
            }
            return null;
        } catch (URISyntaxException e) {
            return "'" + URL + "' is not a URL - " + e.getMessage();
        }
    }

    /**
     * Whether every {@code server.header[]} entry is {@code name: value}.
     *
     * @param value the property's value
     * @return null when it is, else the reason it is not
     */
    public static String checkHeaders(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List<?> entries)) {
            return "'" + HEADER + "' is a list of 'name: value' entries";
        }
        for (var entry : entries) {
            var text = String.valueOf(entry);
            var colon = text.indexOf(':');
            if (colon < 1 || text.substring(0, colon).isBlank()) {
                return "'" + HEADER + "' entry '" + text + "' is not 'name: value'";
            }
        }
        return null;
    }

    private static Map<String, String> headersOf(Object value) {
        var problem = checkHeaders(value);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        Map<String, String> result = new LinkedHashMap<>();
        if (value instanceof List<?> entries) {
            for (var entry : entries) {
                var text = String.valueOf(entry);
                var colon = text.indexOf(':');
                result.put(text.substring(0, colon).trim(), text.substring(colon + 1).trim());
            }
        }
        return result;
    }

    private static String normalize(String url) {
        var uri = URI.create(url);
        var path = uri.getRawPath();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            var base = url.endsWith("/")? url.substring(0, url.length() - 1) : url;
            return base + DEFAULT_PATH;
        }
        return url;
    }

    /**
     * @return the endpoint the call goes to, the default path supplied
     */
    public String endpoint() {
        return endpoint;
    }

    /**
     * @return the transport headers, possibly empty
     */
    public Map<String, String> headers() {
        return headers;
    }

    /**
     * Call the function at the provider, as a request-response, with a future that completes on the worker's
     * reactor (the shape the task and state skills chain their completion on).
     *
     * @param po the post office of the calling skill
     * @param event the request, addressed to the task route
     * @param ttl the deadline in milliseconds, which travels as X-TTL
     * @return the response
     */
    public CompletableFuture<EventEnvelope> eRequest(PostOffice po, EventEnvelope event, long ttl) {
        return po.eRequest(event, ttl, headers, endpoint, true);
    }

    /**
     * Call the function at the provider, as a request-response, with a blocking future.
     *
     * @param po the post office of the calling skill
     * @param event the request, addressed to the task route
     * @param ttl the deadline in milliseconds
     * @return the response
     */
    public Future<EventEnvelope> request(PostOffice po, EventEnvelope event, long ttl) {
        return po.request(event, ttl, headers, endpoint, true);
    }

    @Override
    public String toString() {
        return endpoint;
    }
}
