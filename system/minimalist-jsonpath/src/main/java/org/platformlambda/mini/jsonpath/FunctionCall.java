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
import java.util.regex.Pattern;

/**
 * A call of one of the five standard functions, with arguments already checked against the function's declared
 * types: an {@link Operand} for a value parameter, a {@link Query} for a nodelist parameter.
 */
final class FunctionCall implements Operand {
    private final JsonFunction function;
    private final List<Object> arguments;
    // match() and search() with a literal pattern compile it once; an invalid pattern stays null
    private final boolean literalPattern;
    private final Pattern pattern;

    FunctionCall(JsonFunction function, List<Object> arguments) {
        this.function = function;
        this.arguments = List.copyOf(arguments);
        boolean regex = function == JsonFunction.MATCH || function == JsonFunction.SEARCH;
        if (regex && arguments.get(1) instanceof Operand.Literal(Object literal)) {
            this.literalPattern = true;
            this.pattern = literal instanceof String expression ? IRegexp.compile(expression) : null;
        } else {
            this.literalPattern = false;
            this.pattern = null;
        }
    }

    JsonFunction.Type type() {
        return function.result;
    }

    String name() {
        return function.functionName;
    }

    @Override
    public Object value(Node current, Node root) {
        return switch (function) {
            case LENGTH -> length(valueOf(0, current, root));
            case COUNT -> Long.valueOf(nodesOf(current, root).size());
            case VALUE -> {
                List<Node> nodes = nodesOf(current, root);
                yield nodes.size() == 1 ? nodes.getFirst().value() : Values.NOTHING;
            }
            case MATCH, SEARCH -> test(current, root);
        };
    }

    /**
     * @return the logical result of match() or search(): false unless both arguments are strings and the second is
     *         a valid I-Regexp (RFC 9485) that matches the whole first string, or some substring of it
     */
    boolean test(Node current, Node root) {
        if (!(valueOf(0, current, root) instanceof String text) ||
                !(valueOf(1, current, root) instanceof String expression)) {
            return false;
        }
        Pattern p = literalPattern ? pattern : IRegexp.compile(expression);
        if (p == null) {
            return false;
        }
        return function == JsonFunction.MATCH ? p.matcher(text).matches() : p.matcher(text).find();
    }

    private Object valueOf(int index, Node current, Node root) {
        return ((Operand) arguments.get(index)).value(current, root);
    }

    private List<Node> nodesOf(Node current, Node root) {
        return ((Query) arguments.getFirst()).evaluate(current, root);
    }

    // the number of Unicode scalar values of a string, of elements of an array or of members of an object
    private static Object length(Object value) {
        return switch (value) {
            case String s -> Long.valueOf(s.codePointCount(0, s.length()));
            case List<?> list -> Long.valueOf(list.size());
            case Map<?, ?> map -> Long.valueOf(map.size());
            case null, default -> Values.NOTHING;
        };
    }
}
