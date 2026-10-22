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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A node of the queried document: its value, and where it is, so that a nodelist can be reported as normalized paths.
 * <p>
 * The record is public because it appears in the signatures of public methods: those of the records nested in the
 * package's evaluation interfaces ({@code Expr}, {@code Selector} and {@code Operand}), which are public as every type
 * declared in an interface is. No public method of {@link JsonPath} takes or returns a node.
 *
 * @param value the value, null for a JSON null
 * @param parent the parent node, null for the root
 * @param key the member name (a String) or the array index (an Integer) under the parent, null for the root
 */
public record Node(Object value, Node parent, Object key) {

    static Node root(Object document) {
        return new Node(document, null, null);
    }

    /**
     * Visit the children of this node: the elements of an array in order, the members of an object in the
     * map's iteration order. A value of any other type has no children.
     */
    void forEachChild(Consumer<Node> visitor) {
        if (value instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                visitor.accept(new Node(list.get(i), this, i));
            }
        } else if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                visitor.accept(new Node(entry.getValue(), this, String.valueOf(entry.getKey())));
            }
        }
    }

    /**
     * @return the normalized path of RFC 9535 2.7, for example {@code $['store']['book'][0]}
     */
    String normalizedPath() {
        Deque<Object> keys = new ArrayDeque<>();
        for (Node node = this; node.parent != null; node = node.parent) {
            keys.push(node.key);
        }
        StringBuilder sb = new StringBuilder("$");
        for (Object k : keys) {
            if (k instanceof Integer index) {
                sb.append('[').append(index).append(']');
            } else {
                sb.append("['");
                appendEscaped(sb, (String) k);
                sb.append("']");
            }
        }
        return sb.toString();
    }

    // RFC 9535 2.7: the escapes a normalized path uses, with a lower case hexadecimal escape for other controls
    private static void appendEscaped(StringBuilder sb, String name) {
        name.codePoints().forEach(c -> {
            switch (c) {
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\'' -> sb.append("\\'");
                case '\\' -> sb.append("\\\\");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", c));
                    } else {
                        sb.appendCodePoint(c);
                    }
                }
            }
        });
    }
}
