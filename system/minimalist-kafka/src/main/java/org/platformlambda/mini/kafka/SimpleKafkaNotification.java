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

package org.platformlambda.mini.kafka;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TraceInfo;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.W3cTrace;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Notification Function - a minimalist composable function that publishes a Post Office event to a
 * Kafka topic. It reads the {@code topic} header (required) and the optional {@code partition} header
 * for routing, forwards every other event header as a Kafka header (byte[]), and uses the event body
 * (byte[]) as the Kafka message body. It wraps the shared, thread-safe {@link KafkaRequestPublisher}
 * singleton, and is drop-n-forget because Kafka is asynchronous.
 *
 * <p><b>Trace propagation.</b> Rather than forwarding the caller's (now-stale) traceparent, it stamps a
 * fresh W3C {@code traceparent} built from this function's <i>own</i> current span, so the consuming side
 * adopts this span as the parent of the next hop - keeping the trace continuous across the Kafka boundary.</p>
 *
 * <p>byte[] is used for headers and body so no custom serializer/deserializer is needed. This is the
 * minimalist building block; richer encodings (e.g. a Confluent Schema Registry) layer on top.</p>
 */
@PreLoad(route = "simple.kafka.notification", instances = 10)
public class SimpleKafkaNotification implements TypedLambdaFunction<byte[], Void> {

    @Override
    public Void handleEvent(Map<String, String> headers, byte[] body, int instance) {
        String topic = headers.get(KafkaHeaders.TOPIC);
        if (topic == null) {
            throw new IllegalArgumentException("Missing '" + KafkaHeaders.TOPIC + "' header");
        }
        Integer partition = parsePartition(headers.get(KafkaHeaders.PARTITION));
        Map<String, byte[]> kafkaHeaders = new HashMap<>();
        headers.forEach((key, value) -> {
            // topic/partition are routing directives; the inbound traceparent is replaced below.
            if (!KafkaHeaders.TOPIC.equals(key)
                    && !KafkaHeaders.PARTITION.equals(key)
                    && !W3cTrace.TRACEPARENT.equals(key)) {
                kafkaHeaders.put(key, value.getBytes(StandardCharsets.UTF_8));
            }
        });
        String traceparent = currentTraceparent(new PostOffice(headers, instance));
        if (traceparent != null) {
            kafkaHeaders.put(W3cTrace.TRACEPARENT, traceparent.getBytes(StandardCharsets.UTF_8));
        }
        KafkaRuntime.publisher().publish(topic, partition, kafkaHeaders, body);
        return null;
    }

    /** Build a W3C traceparent from this function's current trace context (null if tracing is off). */
    private static String currentTraceparent(PostOffice po) {
        TraceInfo trace = po.getTrace();
        return trace == null ? null : W3cTrace.traceparent(po.getTraceId(), trace.spanId);
    }

    private static Integer parsePartition(String value) {
        return value == null ? null : Integer.valueOf(value.trim());
    }
}
