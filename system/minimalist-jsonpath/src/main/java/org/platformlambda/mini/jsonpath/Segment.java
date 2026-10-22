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
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * A child segment, which applies its selectors to each input node, or a descendant segment ('..'), which applies
 * them to each input node and to every node under it.
 *
 * @param descendant true for a descendant segment
 * @param selectors the selectors, applied in order
 */
record Segment(boolean descendant, List<Selector> selectors) {

    Segment {
        selectors = List.copyOf(selectors);
    }

    List<Node> apply(List<Node> input, Node root) {
        List<Node> out = new ArrayList<>();
        Consumer<Node> select = node -> {
            for (Selector selector : selectors) {
                selector.select(node, root, out);
            }
        };
        for (Node node : input) {
            if (descendant) {
                visitDescendants(node, select);
            } else {
                select.accept(node);
            }
        }
        return out;
    }

    /**
     * @return false only for a child segment with one name or one index selector, the one kind of segment a
     *         singular query is made of
     */
    boolean isNotSingular() {
        return descendant || selectors.size() != 1 ||
                (!(selectors.getFirst() instanceof Selector.Name) && !(selectors.getFirst() instanceof Selector.Index));
    }

    /*
     * RFC 9535 2.5.2.2: the input node and then its descendants, each node before its own descendants and the
     * elements of an array in order. The walk keeps its own stack, so a deep document cannot exhaust the thread's.
     */
    private static void visitDescendants(Node start, Consumer<Node> visitor) {
        Deque<Node> stack = new ArrayDeque<>();
        List<Node> children = new ArrayList<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            visitor.accept(node);
            children.clear();
            node.forEachChild(children::add);
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
    }
}
