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

package org.platformlambda.async;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.mini.kafka.KafkaHeaders;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Composable mock System-of-Record: the task of the {@code system-of-record} flow, reached when the
 * Kafka flow adapter routes a message from the request topic. It echoes the request and publishes the
 * reply to the response topic via {@code simple.kafka.notification} (drop-n-forget). It echoes back the
 * current {@code traceId} so the test can confirm the trace-id stayed continuous across the Kafka hops.
 * A {@code no-reply} request is dropped (no reply) to exercise the timeout path. Because this runs as a
 * flow task, its work shows up on the same telemetry trace.
 */
@PreLoad(route = "system.of.record", instances = 10)
public class SystemOfRecordTask implements TypedLambdaFunction<byte[], Map<String, Object>> {

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> handleEvent(Map<String, String> headers, byte[] input, int instance) {
        String cid = headers.get(KafkaHeaders.CORRELATION_ID);
        PostOffice po = new PostOffice(headers, instance);
        String requestJson = new String(input, StandardCharsets.UTF_8);
        Map<String, Object> request = SimpleMapper.getInstance().getMapper().readValue(requestJson, Map.class);
        if ("no-reply".equals(request.get("action"))) {
            return Map.of("status", "dropped");   // simulate a backend that never answers
        }
        Map<String, Object> response = new HashMap<>();
        response.put("cid", cid);
        response.put("traceId", po.getTraceId());   // the trace-id should be continuous across the Kafka hops
        response.put("echo", request);
        byte[] responseBody = SimpleMapper.getInstance().getMapper().writeValueAsBytes(response);

        // Drop-n-forget reply; the notification function stamps a fresh traceparent from its own span.
        po.send(new EventEnvelope().setTo("simple.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, SyncRuntime.RESPONSE_TOPIC)
                .setHeader(KafkaHeaders.CORRELATION_ID, cid)
                .setBody(responseBody));
        return Map.of("status", "processed");
    }
}
