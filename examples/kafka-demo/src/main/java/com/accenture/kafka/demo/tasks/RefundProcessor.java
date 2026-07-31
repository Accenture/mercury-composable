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

import java.util.HashMap;
import java.util.Map;

/**
 * The SPECIFIC TASK of the second-level routing demo - the {@code task://demo.refund.processor}
 * target, selected when the {@code input.body.event.kind(refund)} rule matches a {@code demo.orders}
 * record's payload (a body rule needs the Map that {@code serializer: 'json'} decoded).
 *
 * <p>A {@code task://} target invokes this function DIRECTLY - no flow, no data mapping. The adapter
 * copies all inbound Kafka record headers verbatim onto the input headers, passes the whole decoded
 * payload as the body, and injects the business correlation-id (readable via
 * {@code PostOffice.getMyCorrelationId()}) with full trace continuity. Returning normally
 * (status &lt; 400) lets the adapter commit the offset; throwing follows the same bounded-retry then
 * dead-letter path as a flow failure.</p>
 *
 * <p>Use a task for processing simple enough that a flow is overweight - record, count, notify. This
 * demo task just acknowledges the refund in the application log (watch the Java terminal); anything
 * needing orchestration - like publishing onward - belongs in a {@code flow://} target instead.</p>
 */
@PreLoad(route = "demo.refund.processor", instances = 10)
public class RefundProcessor implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(RefundProcessor.class);

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        PostOffice po = new PostOffice(headers, instance);
        log.info("Refund routed by rule input.body.event.kind(refund) (cid={}, traceId={}): {}",
                po.getMyCorrelationId(), po.getTraceId(), input);

        Map<String, Object> ack = new HashMap<>();
        ack.put("status", "refund recorded");
        ack.put("refund", input);
        ack.put("cid", po.getMyCorrelationId());
        return ack;
    }
}
