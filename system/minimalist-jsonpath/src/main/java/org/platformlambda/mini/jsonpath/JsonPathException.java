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

/**
 * A query that is not valid JSONPath: the message names the reason, the position (a code point index into the
 * query) and the query itself, for example {@code unknown function foo() at position 4 of '$[?foo(@)]'}.
 * <p>
 * It is an {@link IllegalArgumentException}, so a caller that already treats a bad argument as a client error
 * handles a bad query on the same path.
 */
public class JsonPathException extends IllegalArgumentException {
    private final int position;

    /**
     * @param reason what is wrong
     * @param query the query as written
     * @param position the code point index at which the problem was found
     */
    public JsonPathException(String reason, String query, int position) {
        super(reason + " at position " + position + " of '" + query + "'");
        this.position = position;
    }

    /**
     * @return the code point index at which the problem was found
     */
    public int getPosition() {
        return position;
    }
}
