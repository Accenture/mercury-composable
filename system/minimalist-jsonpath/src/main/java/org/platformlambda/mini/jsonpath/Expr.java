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
 * A logical expression of a filter selector (RFC 9535 2.3.5.1).
 */
sealed interface Expr permits Expr.Or, Expr.And, Expr.Not, Expr.Exists, Expr.Test, Expr.Comparison {

    /**
     * @param current the node under test ('@')
     * @param root the root node ('$')
     * @return the value of the expression
     */
    boolean test(Node current, Node root);

    /**
     * @param terms two or more expressions, any of which must be true
     */
    record Or(List<Expr> terms) implements Expr {
        public Or {
            terms = List.copyOf(terms);
        }

        @Override
        public boolean test(Node current, Node root) {
            for (Expr term : terms) {
                if (term.test(current, root)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * @param terms two or more expressions, all of which must be true
     */
    record And(List<Expr> terms) implements Expr {
        public And {
            terms = List.copyOf(terms);
        }

        @Override
        public boolean test(Node current, Node root) {
            for (Expr term : terms) {
                if (!term.test(current, root)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * @param expr the expression negated by '!'
     */
    record Not(Expr expr) implements Expr {
        @Override
        public boolean test(Node current, Node root) {
            return !expr.test(current, root);
        }
    }

    /**
     * An existence test: true when the query selects at least one node.
     *
     * @param query the query
     */
    record Exists(Query query) implements Expr {
        @Override
        public boolean test(Node current, Node root) {
            return !query.evaluate(current, root).isEmpty();
        }
    }

    /**
     * A function whose result is a logical value, used as a test.
     *
     * @param call the function call
     */
    record Test(FunctionCall call) implements Expr {
        @Override
        public boolean test(Node current, Node root) {
            return call.test(current, root);
        }
    }

    /**
     * A comparison of two values (RFC 9535 2.3.5.2.2).
     *
     * @param left the left operand
     * @param op the operator
     * @param right the right operand
     */
    record Comparison(Operand left, Op op, Operand right) implements Expr {
        @Override
        public boolean test(Node current, Node root) {
            return op.apply(left.value(current, root), right.value(current, root));
        }
    }

    /**
     * The comparison operators. Equality is defined for every pair of values; order only for two numbers or two
     * strings, so '&lt;' is false for any other pair, and '&lt;=' is true for them only when they are equal.
     */
    enum Op {
        EQ("=="), NE("!="), LE("<="), GE(">="), LT("<"), GT(">");

        final String symbol;

        Op(String symbol) {
            this.symbol = symbol;
        }

        boolean apply(Object a, Object b) {
            return switch (this) {
                case EQ -> Values.equal(a, b);
                case NE -> !Values.equal(a, b);
                case LT -> Values.less(a, b);
                case GT -> Values.less(b, a);
                case LE -> Values.less(a, b) || Values.equal(a, b);
                case GE -> Values.less(b, a) || Values.equal(a, b);
            };
        }
    }
}
