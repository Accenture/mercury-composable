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

package org.platformlambda.mini.jsonpath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A compiled JSONPath query (RFC 9535). A query is immutable and thread-safe: compile it once and evaluate it
 * against any number of documents, from any number of threads.
 * <p>
 * A document is a tree of {@link Map} (an object, whose keys are member names), {@link List} (an array),
 * {@link String}, {@link Number}, {@link Boolean} and null (a JSON null). A value of any other type is a leaf
 * that no selector descends into.
 * <p>
 * {@link #compile(String)} accepts RFC 9535 exactly. {@link #compileLenient(String)} adds Mercury's one declared
 * extension, a hyphen in a dot-notation member name, the way the Rust engine does. {@link #select(Object)} returns
 * the standard's nodelist; {@link #read(Object, Object)} shapes the result the way Mercury documents it.
 */
public final class JsonPath {
    private final String text;
    private final Query query;
    private final boolean singular;
    // the selectors of the leading segments that select one member or one element - Mercury's shaping rule reads them
    private final List<Selector> definitePrefix;

    private JsonPath(String text, Query query) {
        this.text = text;
        this.query = query;
        this.singular = query.isSingular();
        List<Selector> prefix = new ArrayList<>();
        for (Segment segment : query.segments()) {
            if (!segment.isSingular()) {
                break;
            }
            prefix.add(segment.selectors().getFirst());
        }
        this.definitePrefix = List.copyOf(prefix);
    }

    /**
     * Compile a query that follows RFC 9535 exactly.
     *
     * @param query the query, which starts with '$'
     * @return the compiled query
     * @throws JsonPathException when the query is not valid RFC 9535 JSONPath
     */
    public static JsonPath compile(String query) {
        if (query == null) {
            throw new JsonPathException("a query is required", "", 0);
        }
        return new JsonPath(query, new Parser(query).parseQuery());
    }

    /**
     * Compile a query with Mercury's one declared extension: a dot-notation member name may contain a hyphen
     * ({@code $.fetcher-ext.result}), because graph node names carry hyphens. As in the Rust engine, a query that
     * RFC 9535 refuses is retried once with each such name rewritten to bracket notation
     * ({@code $['fetcher-ext'].result}); a query that RFC 9535 accepts is never rewritten.
     *
     * @param query the query, which starts with '$'
     * @return the compiled query
     * @throws JsonPathException the strict parser's error, when the rewritten query is refused too
     */
    public static JsonPath compileLenient(String query) {
        try {
            return compile(query);
        } catch (JsonPathException strict) {
            String rewritten = HyphenNames.bracketize(query);
            if (rewritten.equals(query)) {
                throw strict;
            }
            try {
                return new JsonPath(query, new Parser(rewritten).parseQuery());
            } catch (JsonPathException e) {
                throw strict;
            }
        }
    }

    /**
     * @return true when the query is singular (RFC 9535 2.3.5.1): every segment selects one member name or one
     *         index, so it selects at most one node
     */
    public boolean isSingular() {
        return singular;
    }

    /**
     * Evaluate the query.
     *
     * @param document the root of the document
     * @return the values of the selected nodes, in the order RFC 9535 defines
     */
    public List<Object> select(Object document) {
        List<Node> nodes = evaluate(document);
        List<Object> values = new ArrayList<>(nodes.size());
        for (Node node : nodes) {
            values.add(node.value());
        }
        return values;
    }

    /**
     * Evaluate the query and report where each selected node is.
     *
     * @param document the root of the document
     * @return the normalized path of each selected node (RFC 9535 2.7), in the order of {@link #select(Object)}
     */
    public List<String> selectPaths(Object document) {
        List<Node> nodes = evaluate(document);
        List<String> paths = new ArrayList<>(nodes.size());
        for (Node node : nodes) {
            paths.add(node.normalizedPath());
        }
        return paths;
    }

    /**
     * Evaluate the query and shape the result the way Mercury documents it, by the kind of query and not by the
     * number of matches (the same rule as the Rust engine):
     * <ul>
     *   <li>a singular query yields the value of its node, or {@code notFound} when it selects none;</li>
     *   <li>any other query yields a list of values, one match being a one-element list and none an empty list,
     *       unless a member name selected before its first other segment is missing, which yields
     *       {@code notFound}.</li>
     * </ul>
     *
     * @param document the root of the document
     * @param notFound what to return when nothing is found
     * @return the value, the list of values, or {@code notFound}
     */
    public Object read(Object document, Object notFound) {
        if (singular) {
            List<Node> nodes = evaluate(document);
            return nodes.isEmpty() ? notFound : nodes.getFirst().value();
        }
        return missesAMemberName(document) ? notFound : select(document);
    }

    private List<Node> evaluate(Object document) {
        Node root = Node.root(document);
        return query.evaluate(root, root);
    }

    // a member name that is absent, or asked of a value that is not an object, before the first other segment;
    // an index that selects nothing does not count
    private boolean missesAMemberName(Object document) {
        Object current = document;
        for (Selector selector : definitePrefix) {
            if (selector instanceof Selector.Name(String name)) {
                if (!(current instanceof Map<?, ?> map) || !map.containsKey(name)) {
                    return true;
                }
                current = map.get(name);
            } else {
                current = Selector.Index.elementAt(current, ((Selector.Index) selector).index());
                if (current == Values.NOTHING) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * @return the query as written
     */
    @Override
    public String toString() {
        return text;
    }
}
