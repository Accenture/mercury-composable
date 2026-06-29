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

import java.util.HashMap;
import java.util.Map;

/**
 * Avro variant of {@link SystemOfRecord}, the task of the {@code system-of-record-avro} flow on the backend
 * pod (bound to the {@code avro-topic-1} request topic).
 *
 * <p>Because that binding sets {@code schema.enabled}, the Kafka flow adapter decodes the Confluent Avro
 * value and hands this task a {@code Map}. Unlike the JSON Schema path - whose {@code additionalProperties:
 * true} schema tolerates the open, nested response that {@link SystemOfRecord#process} builds - an <b>Avro
 * record is closed-shape</b>: every field of {@code SyncDemoMessage} (action, status, processedBy, traceId)
 * is declared, and only those. So this task builds a <b>flat</b> reply matching the record exactly; the flow
 * then re-encodes it with schema id 2 and publishes it to {@code avro-topic-2}.</p>
 */
@PreLoad(route = "system.of.record.avro", instances = 50)
public class SystemOfRecordAvro implements TypedLambdaFunction<Map<String, Object>, EventEnvelope> {
    private static final Logger log = LoggerFactory.getLogger(SystemOfRecordAvro.class);

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        String cid = headers.get(KafkaHeaders.CORRELATION_ID);
        PostOffice po = new PostOffice(headers, instance);
        log.info("Processing Avro request (cid={}): {}", cid, input);

        // Flat reply matching the Avro SyncDemoMessage record exactly (no extra/nested fields).
        Map<String, Object> response = new HashMap<>();
        response.put("action", input.getOrDefault("action", ""));
        response.put("status", "processed");
        response.put("processedBy", "system-of-record");
        response.put("traceId", po.getTraceId());   // continuous across the Kafka hops

        // simple.kafka.notification takes a byte[] body (JSON) and re-encodes it with the schema id; the
        // byte[]/JSON/Avro paths share that contract.
        byte[] payload = SimpleMapper.getInstance().getMapper().writeValueAsBytes(response);
        EventEnvelope result = new EventEnvelope().setBody(payload);
        if (cid != null) {
            result.setHeader(KafkaHeaders.CORRELATION_ID, cid);
        }
        return result;
    }
}
