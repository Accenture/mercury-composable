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

/**
 * A query: the root query of a compiled path, or a query inside a filter, which starts at the root ('$') or at the
 * node under test ('@').
 *
 * @param absolute true for '$', false for '@'
 * @param segments the segments, applied in order
 */
record Query(boolean absolute, List<Segment> segments) {

    Query {
        segments = List.copyOf(segments);
    }

    List<Node> evaluate(Node current, Node root) {
        List<Node> nodes = List.of(absolute ? root : current);
        for (Segment segment : segments) {
            if (nodes.isEmpty()) {
                break;
            }
            nodes = segment.apply(nodes, root);
        }
        return nodes;
    }

    boolean isSingular() {
        for (Segment segment : segments) {
            if (!segment.isSingular()) {
                return false;
            }
        }
        return true;
    }
}
