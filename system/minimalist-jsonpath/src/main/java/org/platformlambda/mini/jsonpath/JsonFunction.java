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
 * The function extensions of RFC 9535 2.4, with their declared types. There is no registry: the standard defines
 * five functions and the engine supports exactly those.
 */
enum JsonFunction {
    LENGTH("length", Type.VALUE, Type.VALUE),
    COUNT("count", Type.VALUE, Type.NODES),
    MATCH("match", Type.LOGICAL, Type.VALUE, Type.VALUE),
    SEARCH("search", Type.LOGICAL, Type.VALUE, Type.VALUE),
    VALUE("value", Type.VALUE, Type.NODES);

    /**
     * The types of RFC 9535 2.4.1: a value or Nothing, a logical value, or a nodelist.
     */
    enum Type { VALUE, LOGICAL, NODES }

    final String functionName;
    final Type result;
    final List<Type> parameters;

    JsonFunction(String functionName, Type result, Type... parameters) {
        this.functionName = functionName;
        this.result = result;
        this.parameters = List.of(parameters);
    }

    /**
     * @param name a function name as written
     * @return the function, or null when the standard defines none of that name
     */
    static JsonFunction of(String name) {
        for (JsonFunction function : values()) {
            if (function.functionName.equals(name)) {
                return function;
            }
        }
        return null;
    }
}
