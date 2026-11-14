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

package com.accenture.minigraph.demo;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

/**
 * The order-assistant demo's first tool: an order by its id (catalog row {@code order_lookup}).
 * <p>
 * A tool of the agent loop is an ordinary composable function. The loop calls the route the graph's
 * catalog maps the tool name to, with the model's validated arguments as the request body, and returns
 * the response to the model as the call's result. A thrown exception goes back to the model as that
 * call's error, with its status and message, so a 404 here is something the model reads and answers.
 */
@PreLoad(route = "demo.order.lookup", instances = 10)
public class OrderLookup implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws AppException {
        return OrderBook.order(OrderBook.orderId(input));
    }
}
