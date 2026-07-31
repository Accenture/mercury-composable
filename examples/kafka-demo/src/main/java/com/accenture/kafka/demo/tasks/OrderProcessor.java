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

package com.accenture.kafka.demo.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * The task of {@code demo-order-flow} - the SPECIFIC FLOW of the second-level routing demo, selected
 * when a {@code demo.orders} record's {@code type} header matches the {@code order} (exact) or
 * {@code order-*} (wildcard) routing rule.
 *
 * <p>The binding's {@code serializer: 'json'} decoded the record before routing, so the input is a
 * {@code Map} - no byte[] handling here. The function returns a {@code Map} too: the flow maps it
 * straight into {@code simple.kafka.notification}, which auto-serializes a Map body to JSON bytes on
 * a non-schema topic (the outbound symmetry of {@code serializer: 'json'}).</p>
 *
 * <p>{@code header.type} is the routing key the rule matched on (mapped by the flow from the raw
 * record header), echoed in the response so the outbound message shows which rule fired.</p>
 */
@PreLoad(route = "demo.order.processor", instances = 10)
public class OrderProcessor implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(OrderProcessor.class);

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        PostOffice po = new PostOffice(headers, instance);
        String type = headers.get("type");
        log.info("Order event routed by rule type({}) (cid={}, traceId={}): {}",
                type, po.getMyCorrelationId(), po.getTraceId(), input);

        Map<String, Object> response = new HashMap<>();
        response.put("order", input);
        response.put("routedBy", "input.header.type(" + type + ")");
        response.put("processedBy", "demo-order-flow");
        // Instant is serialized as an ISO-8601 / RFC-3339 string by the built-in mapper (platform-core)
        response.put("processedAt", Instant.now());
        response.put("traceId", po.getTraceId());   // continuous across the Kafka hop
        return response;
    }
}
