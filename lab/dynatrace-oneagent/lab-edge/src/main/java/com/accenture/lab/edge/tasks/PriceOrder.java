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
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Prices an order in a Mono that completes 200 ms later on a Reactor thread, so the experiment can check that a
 * deferred result's hop has the expected duration.
 */
@PreLoad(route = "lab.price", instances = 10)
public class PriceOrder implements TypedLambdaFunction<Map<String, Object>, Mono<Map<String, Object>>> {
    private static final Logger log = LoggerFactory.getLogger(PriceOrder.class);
    private static final long DELAY_MS = 200;

    @Override
    public Mono<Map<String, Object>> handleEvent(Map<String, String> headers, Map<String, Object> input,
                                                 int instance) {
        log.info("Pricing order, the result follows in {} ms", DELAY_MS);
        var order = new HashMap<>(input);
        var quantity = input.get("quantity") instanceof Number n ? n.intValue() : 1;
        order.put("price", quantity * 12.5);
        return Mono.<Map<String, Object>>just(order).delayElement(Duration.ofMillis(DELAY_MS));
    }
}
