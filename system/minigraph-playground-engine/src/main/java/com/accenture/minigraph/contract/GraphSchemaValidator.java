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

package com.accenture.minigraph.contract;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

/**
 * The built-in input validator of a graph with a contract (RFC-0007, WP2).
 * <p>
 * The engine assumes this step at the root node of every run whose root carries a {@code schema}
 * property - nothing is written into the node - and invokes it the way a task node invokes a
 * composable function: the request body is {@code {body, header, schema}}, the state machine's
 * {@code input.body} and {@code input.header} beside the root's {@code schema}; 200 passes and 400
 * carries every violation in one message, which the walker turns into the run's abort status, the
 * root's {@code exception=} handler, or the console line of a dry run. The application property
 * {@code graph.schema.validator} names a substitute function with the same contract.
 */
@PreLoad(route = GraphSchemaValidator.ROUTE, instances = 100)
public class GraphSchemaValidator implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    public static final String ROUTE = "graph.schema.validator";
    private static final String SCHEMA = "schema";
    private static final String VALID = "valid";

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws AppException {
        if (input == null || !input.containsKey(SCHEMA)) {
            throw new AppException(400, "Invalid schema - schema: must be an object with body and/or header");
        }
        GraphSchema.Contract contract;
        try {
            contract = GraphSchema.compileContract(input.get(SCHEMA));
        } catch (IllegalArgumentException e) {
            throw new AppException(400, "Invalid schema - " + e.getMessage());
        }
        var violations = contract.check(input.get(GraphSchema.BODY), input.get(GraphSchema.HEADER));
        if (!violations.isEmpty()) {
            throw new AppException(400, GraphSchema.report(violations));
        }
        return Map.of(VALID, true);
    }
}
