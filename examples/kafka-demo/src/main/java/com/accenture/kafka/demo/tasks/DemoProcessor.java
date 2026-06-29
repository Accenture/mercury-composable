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
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TraceInfo;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.mini.kafka.KafkaHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * The single task of {@code kafka-demo-flow}, reached when the Kafka flow adapter routes a message from the
 * {@code demo.inbound} topic. It is a self-contained function: it reads the inbound text, wraps it with a
 * little processing metadata (who processed it, when, and the trace-id), and returns that as the result.
 * The flow then publishes the result to {@code demo.outbound} via {@code simple.kafka.notification} - this
 * function does not publish itself (that is orchestration, expressed in the flow YAML).
 *
 * <p>The echoed {@code traceId} lets you confirm the trace stayed continuous across the Kafka hop, which
 * the telemetry log also shows as the end-to-end span path.</p>
 */
@PreLoad(route = "demo.processor", instances = 10)
public class DemoProcessor implements TypedLambdaFunction<byte[], EventEnvelope> {
    private static final Logger log = LoggerFactory.getLogger(DemoProcessor.class);

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, byte[] input, int instance) {
        String cid = headers.get(KafkaHeaders.CORRELATION_ID);
        String received = new String(input, StandardCharsets.UTF_8);
        PostOffice po = new PostOffice(headers, instance);
        // incoming span = the upstream span carried in the inbound traceparent (null if none was sent)
        TraceInfo trace = po.getTrace();
        String incomingSpan = trace != null ? trace.parentSpanId : null;
        log.info("Received from demo.inbound (cid={}, traceId={}, incoming span={}): {}",
                cid, po.getTraceId(), incomingSpan, received);

        Map<String, Object> response = new HashMap<>();
        response.put("received", received);
        response.put("processedBy", "kafka-demo");
        // Instant is serialized as an ISO-8601 / RFC-3339 string by the built-in mapper (platform-core)
        response.put("processedAt", Instant.now());
        response.put("traceId", po.getTraceId());   // continuous across the Kafka hop
        byte[] payload = SimpleMapper.getInstance().getMapper().writeValueAsBytes(response);

        EventEnvelope result = new EventEnvelope().setBody(payload);
        if (cid != null) {
            result.setHeader(KafkaHeaders.CORRELATION_ID, cid);
        }
        return result;
    }
}
