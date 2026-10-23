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

package com.accenture.lab.edge.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/** The flow's last task: the answer to the HTTP caller. */
@PreLoad(route = "lab.receipt", instances = 10)
public class OrderReceipt implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(OrderReceipt.class);

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        log.info("Order accepted and published to lab.orders");
        var receipt = new HashMap<>(input);
        receipt.put("status", "accepted");
        return receipt;
    }
}
