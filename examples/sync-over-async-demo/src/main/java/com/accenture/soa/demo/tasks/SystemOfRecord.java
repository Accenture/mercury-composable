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

package com.accenture.soa.demo.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
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
 * The backend business logic - the only application-specific function in this example. It is the task of
 * the {@code system-of-record} flow, reached (on the <b>backend</b> pod) when the Kafka flow adapter routes
 * a message from the {@code soa.request} topic.
 *
 * <p>It echoes the request with a little processing metadata and returns the result; the flow then
 * publishes it to the {@code soa.response} topic via {@code simple.kafka.notification}. The correlation-id
 * is carried through so the facade can match the reply to the awaiting request, and the {@code traceId}
 * is echoed to show the trace stayed continuous across the Kafka hops.</p>
 */
@PreLoad(route = "system.of.record", instances = 50)
public class SystemOfRecord implements TypedLambdaFunction<byte[], EventEnvelope> {
    private static final Logger log = LoggerFactory.getLogger(SystemOfRecord.class);

    @Override
    @SuppressWarnings("unchecked")
    public EventEnvelope handleEvent(Map<String, String> headers, byte[] input, int instance) {
        String cid = headers.get(KafkaHeaders.CORRELATION_ID);
        PostOffice po = new PostOffice(headers, instance);
        String requestJson = new String(input, StandardCharsets.UTF_8);
        Map<String, Object> request = SimpleMapper.getInstance().getMapper().readValue(requestJson, Map.class);
        log.info("Processing request (cid={}): {}", cid, request);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "processed");
        response.put("processedBy", "system-of-record");
        response.put("processedAt", Instant.now());
        response.put("traceId", po.getTraceId());   // continuous across the Kafka hops
        response.put("request", request);
        byte[] payload = SimpleMapper.getInstance().getMapper().writeValueAsBytes(response);

        EventEnvelope result = new EventEnvelope().setBody(payload);
        if (cid != null) {
            result.setHeader(KafkaHeaders.CORRELATION_ID, cid);
        }
        return result;
    }
}
