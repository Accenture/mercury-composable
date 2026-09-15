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

package com.accenture.cache.demo.functions;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>Layer 2 (Event Script) - the method-to-action mapper.</b> A deliberately tiny composable function: it
 * turns the HTTP method into a cache action so the single {@code l2-profile} flow can route GET / POST /
 * DELETE the same way the Layer 3 graph routes on its payload {@code action} field. This is the Layer 2
 * counterpart to "the action comes from the request" - here it is derived from {@code input.method}
 * rather than a body field, which is why the flow feeds it {@code input.method -> method}.
 *
 * <p>It returns the two values the {@code execution: decision} task consumes:
 * <ul>
 *   <li>{@code action} - the human-readable action string ({@code get} / {@code save} / {@code delete}),
 *       mapped to {@code model.action} for visibility in the flow state and trace;</li>
 *   <li>{@code decision} - the 1-based branch index the engine routes on (GET&rarr;1, POST&rarr;2,
 *       DELETE&rarr;3), mapped to the flow's {@code decision}.</li>
 * </ul>
 *
 * The {@link PostOffice} is built from the request headers so this task joins the request's trace, and
 * {@code updateContext} tags the app-context log with the layer and the derived action.
 */
@PreLoad(route = "v1.http.method.action", instances = 10)
public class MethodActionMapper implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    private static final String METHOD = "method";
    private static final String ACTION = "action";
    private static final String DECISION = "decision";

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws AppException {
        String method = input.get(METHOD) == null ? "" : String.valueOf(input.get(METHOD)).toUpperCase();
        final String action;
        final int decision;
        switch (method) {
            case "GET" -> { action = "get"; decision = 1; }
            case "POST" -> { action = "save"; decision = 2; }
            case "DELETE" -> { action = "delete"; decision = 3; }
            default -> throw new AppException(405, "Method not allowed: " + method);
        }
        PostOffice po = new PostOffice(headers, instance);
        po.updateContext("layer", "2");     // app-context logging: tag this request's log ...
        po.updateContext(ACTION, action);   // ... with its layer and the action derived from the method
        Map<String, Object> result = new HashMap<>();
        result.put(ACTION, action);
        result.put(DECISION, decision);
        return result;
    }
}
