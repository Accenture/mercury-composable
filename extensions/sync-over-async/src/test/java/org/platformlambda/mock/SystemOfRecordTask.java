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

package org.platformlambda.mock;

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
 * Composable mock System-of-Record: the first task of the {@code system-of-record} flow, reached when the
 * Kafka flow adapter routes a message from the request topic. It echoes the request and emits the response
 * <i>payload</i> plus a decision that tells the flow whether to publish a reply - it does <b>not</b> publish
 * itself. Publishing the reply to the response topic is the next task ({@code simple.kafka.notification}),
 * declared in the flow YAML, keeping this function a pure, side-effect-free processor.
 *
 * <p>It echoes back the current {@code traceId} so the test can confirm the trace-id stayed continuous
 * across the Kafka hops (reading the trace context via {@link PostOffice} is not a side effect). A
 * {@code no-reply} request returns the {@code DROP} decision so the flow drops it (no reply) to exercise
 * the facade's timeout path.</p>
 */
@PreLoad(route = "system.of.record", instances = 10)
public class SystemOfRecordTask implements TypedLambdaFunction<byte[], EventEnvelope> {

    // The flow's decision task routes a numeric decision to next[n-1]: 1 -> drop (no.op), 2 -> publish reply.
    private static final String DECISION = "decision";
    private static final String DROP = "1";
    private static final String REPLY = "2";

    @Override
    @SuppressWarnings("unchecked")
    public EventEnvelope handleEvent(Map<String, String> headers, byte[] input, int instance) {
        String cid = headers.get(KafkaHeaders.CORRELATION_ID);
        String requestJson = new String(input, StandardCharsets.UTF_8);
        Map<String, Object> request = SimpleMapper.getInstance().getMapper().readValue(requestJson, Map.class);
        if ("no-reply".equals(request.get("action"))) {
            return new EventEnvelope().setHeader(DECISION, DROP);   // simulate a backend that never answers
        }
        Map<String, Object> response = new HashMap<>();
        response.put("cid", cid);
        response.put("traceId", new PostOffice(headers, instance).getTraceId());   // continuous across Kafka hops
        response.put("echo", request);
        byte[] responseBody = SimpleMapper.getInstance().getMapper().writeValueAsBytes(response);
        return new EventEnvelope().setHeader(DECISION, REPLY)
                .setHeader(KafkaHeaders.CORRELATION_ID, cid).setBody(responseBody);
    }
}
