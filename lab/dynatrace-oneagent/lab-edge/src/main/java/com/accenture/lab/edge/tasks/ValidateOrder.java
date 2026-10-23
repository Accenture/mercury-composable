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
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Validates an order and looks up its customer through an RPC to lab.lookup - a send from inside a function,
 * outside Event Script, so the experiment covers the PostOffice's own send path.
 */
@PreLoad(route = "lab.validate", instances = 10)
public class ValidateOrder implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(ValidateOrder.class);
    private static final String ITEM = "item";
    private static final String QUANTITY = "quantity";
    private static final String CUSTOMER = "customer";

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws Exception {
        if (!(input.get(ITEM) instanceof String item) || item.isEmpty()) {
            throw new AppException(400, "Missing " + ITEM);
        }
        if (!(input.get(QUANTITY) instanceof Number quantity) || quantity.intValue() < 1) {
            throw new AppException(400, QUANTITY + " must be a positive number");
        }
        log.info("Validating order of {} x {}", quantity, item);
        var po = new PostOffice(headers, instance);
        var request = new EventEnvelope().setTo("lab.lookup")
                .setBody(Map.of(CUSTOMER, input.getOrDefault(CUSTOMER, "guest")));
        var response = po.request(request, 5000).get();
        var order = new HashMap<>(input);
        order.put(CUSTOMER, response.getBody());
        return order;
    }
}
