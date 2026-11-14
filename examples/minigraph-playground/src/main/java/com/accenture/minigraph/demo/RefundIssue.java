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
 * The order-assistant demo's one write: issue a refund (catalog row {@code issue_refund}, marked
 * {@code write: true}).
 * <p>
 * The mark on the catalog row, not anything in this code, is what makes the loop hold the call for a
 * person: in the node's default mode a marked write waits for an approval, and the graph suspends until
 * a supervisor decides. This function runs only after that decision, with the arguments the supervisor
 * saw. It refunds an order once; a second attempt answers 409, which the model reads as the call's error.
 */
@PreLoad(route = "demo.refund.issue", instances = 10)
public class RefundIssue implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws AppException {
        String orderId = OrderBook.orderId(input);
        Object amount = input.get("amount");
        if (!(amount instanceof Number number)) {
            throw new AppException(400, "The amount must be a number");
        }
        Object reason = input.get("reason");
        return OrderBook.refund(orderId, number.doubleValue(), reason == null ? null : String.valueOf(reason));
    }
}
