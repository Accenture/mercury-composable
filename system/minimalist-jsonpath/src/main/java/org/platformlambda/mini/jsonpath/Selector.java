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

import java.util.List;
import java.util.Map;

/**
 * A selector of RFC 9535 2.3: it selects zero or more children of one input node.
 */
sealed interface Selector permits Selector.Name, Selector.Wildcard, Selector.Index, Selector.Slice, Selector.Filter {

    /**
     * Append the nodes this selector selects from one input node.
     *
     * @param node the input node
     * @param root the root node, for an absolute query inside a filter
     * @param out the nodelist being built
     */
    void select(Node node, Node root, List<Node> out);

    /**
     * The member of an object with this name (RFC 9535 2.3.1).
     *
     * @param name the member name
     */
    record Name(String name) implements Selector {
        @Override
        public void select(Node node, Node root, List<Node> out) {
            if (node.value() instanceof Map<?, ?> map && map.containsKey(name)) {
                out.add(new Node(map.get(name), node, name));
            }
        }
    }

    /**
     * Every element of an array and every member value of an object (RFC 9535 2.3.2).
     */
    record Wildcard() implements Selector {
        @Override
        public void select(Node node, Node root, List<Node> out) {
            node.forEachChild(out::add);
        }
    }

    /**
     * One element of an array, counted from the end when negative (RFC 9535 2.3.3).
     *
     * @param index the index
     */
    record Index(long index) implements Selector {
        @Override
        public void select(Node node, Node root, List<Node> out) {
            if (node.value() instanceof List<?> list) {
                long at = index < 0 ? list.size() + index : index;
                if (at >= 0 && at < list.size()) {
                    out.add(new Node(list.get((int) at), node, (int) at));
                }
            }
        }

        /**
         * @return the element of a list at an index counted from the end when negative, or
         *         {@link Values#NOTHING} when the value is not a list or the index is out of range
         */
        static Object elementAt(Object value, long index) {
            if (value instanceof List<?> list) {
                long at = index < 0 ? list.size() + index : index;
                if (at >= 0 && at < list.size()) {
                    return list.get((int) at);
                }
            }
            return Values.NOTHING;
        }
    }

    /**
     * A range of elements of an array, start inclusive and end exclusive, in steps (RFC 9535 2.3.4).
     *
     * @param start the start, or null for the default
     * @param end the end, or null for the default
     * @param step the step, or null for 1
     */
    record Slice(Long start, Long end, Long step) implements Selector {
        @Override
        public void select(Node node, Node root, List<Node> out) {
            if (!(node.value() instanceof List<?> list)) {
                return;
            }
            long by = step == null ? 1 : step;
            if (by > 0) {
                forward(list, node, by, out);
            } else if (by < 0) {
                backward(list, node, -by, out);
            }
        }

        // RFC 9535 2.3.4.2.2, a positive step: the bounds default to 0 and the length
        private void forward(List<?> list, Node node, long by, List<Node> out) {
            long len = list.size();
            long lower = Math.clamp(normalize(start == null ? 0 : start, len), 0, len);
            long upper = Math.clamp(normalize(end == null ? len : end, len), 0, len);
            for (long i = lower; i < upper; i += by) {
                out.add(new Node(list.get((int) i), node, (int) i));
            }
        }

        // a negative step, of magnitude by: the bounds default to the last element and to before the first
        private void backward(List<?> list, Node node, long by, List<Node> out) {
            long len = list.size();
            long upper = Math.clamp(normalize(start == null ? len - 1 : start, len), -1, len - 1);
            long lower = Math.clamp(normalize(end == null ? -len - 1 : end, len), -1, len - 1);
            for (long i = upper; i > lower; i -= by) {
                out.add(new Node(list.get((int) i), node, (int) i));
            }
        }

        private static long normalize(long i, long len) {
            return i >= 0 ? i : len + i;
        }
    }

    /**
     * The children of a node for which a logical expression is true (RFC 9535 2.3.5).
     *
     * @param expr the expression, evaluated with each child as the current node
     */
    record Filter(Expr expr) implements Selector {
        @Override
        public void select(Node node, Node root, List<Node> out) {
            node.forEachChild(child -> {
                if (expr.test(child, root)) {
                    out.add(child);
                }
            });
        }
    }
}
