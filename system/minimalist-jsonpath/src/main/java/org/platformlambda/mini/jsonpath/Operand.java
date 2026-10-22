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
 * Something that yields one value or {@link Values#NOTHING}: either side of a comparison, or a value argument of
 * a function (RFC 9535 2.3.5.1 and 2.4.1).
 */
sealed interface Operand permits Operand.Literal, Operand.SingularQuery, FunctionCall {

    /**
     * @param current the node under test ('@')
     * @param root the root node ('$')
     * @return the value, null for a JSON null, or {@link Values#NOTHING}
     */
    Object value(Node current, Node root);

    /**
     * A string, number, true, false or null written in the query.
     *
     * @param literal the value: a String, a BigDecimal, a Boolean or null
     */
    record Literal(Object literal) implements Operand {
        @Override
        public Object value(Node current, Node root) {
            return literal;
        }
    }

    /**
     * A singular query: the value of the node it selects, or Nothing when it selects none.
     *
     * @param query the query
     */
    record SingularQuery(Query query) implements Operand {
        @Override
        public Object value(Node current, Node root) {
            List<Node> nodes = query.evaluate(current, root);
            return nodes.isEmpty() ? Values.NOTHING : nodes.getFirst().value();
        }
    }
}
