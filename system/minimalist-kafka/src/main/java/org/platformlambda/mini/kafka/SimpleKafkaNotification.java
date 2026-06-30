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

import org.platformlambda.core.annotations.KernelThreadRunner;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TraceInfo;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.util.W3cTrace;
import org.platformlambda.mini.kafka.schema.SchemaCodec;
import org.platformlambda.mini.kafka.schema.SchemaType;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
 *
 * <p>It returns the {@code Mono<Void>} from the publisher: the platform-core worker subscribes to it,
 * deferring the function's completion until the broker acknowledges. A caller using {@code po.request}
 * (RPC) therefore learns whether the publish succeeded - a publish failure propagates back as an error -
 * while a {@code po.send} (async) caller simply doesn't observe it. The Mono is realized as {@code null}
 * (a {@code Void} body) on success.</p>
 *
 * <p><b>{@code @KernelThreadRunner}.</b> When the Schema Registry is used, this function builds Confluent
 * serializers, which use {@code synchronized} internally and are not thread-safe. Running on a kernel thread
 * (rather than a virtual thread) avoids pinning a virtual-thread carrier on those {@code synchronized}
 * sections, and each worker instance is single-flight - so each instance keeps its <b>own</b>
 * {@link SchemaCodec.Encoder} (in {@link #encoders}, keyed by instance), guaranteeing a Confluent serializer
 * is never touched by two threads at once.</p>
 */
@KernelThreadRunner
@PreLoad(route = "simple.kafka.notification", instances = 10)
public class SimpleKafkaNotification implements TypedLambdaFunction<byte[], Mono<Void>> {

    // one Encoder per worker instance (an instance is single-flight) -> owner-confined Confluent serializers.
    private final ConcurrentMap<Integer, SchemaCodec.Encoder> encoders = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handleEvent(Map<String, String> headers, byte[] body, int instance) {
        String topic = headers.get(KafkaHeaders.TOPIC);
        if (topic == null) {
            throw new IllegalArgumentException("Missing '" + KafkaHeaders.TOPIC + "' header");
        }
        Integer partition = parsePartition(headers.get(KafkaHeaders.PARTITION));
        Map<String, byte[]> kafkaHeaders = new HashMap<>();
        headers.forEach((key, value) -> {
            // topic/partition/schema-* are routing/encoding directives; the inbound traceparent is replaced below.
            if (!KafkaHeaders.TOPIC.equals(key)
                    && !KafkaHeaders.PARTITION.equals(key)
                    && !KafkaHeaders.SCHEMA_ID.equals(key)
                    && !KafkaHeaders.SCHEMA_TYPE.equals(key)
                    && !W3cTrace.TRACEPARENT.equals(key)) {
                kafkaHeaders.put(key, value.getBytes(StandardCharsets.UTF_8));
            }
        });
        String traceparent = currentTraceparent(new PostOffice(headers, instance));
        if (traceparent != null) {
            kafkaHeaders.put(W3cTrace.TRACEPARENT, traceparent.getBytes(StandardCharsets.UTF_8));
        }
        byte[] payload = encode(topic, headers, body, instance);
        return KafkaRuntime.publisher().publish(topic, partition, kafkaHeaders, payload);
    }

    /**
     * When a {@code schema-id} header is present, serialize the body into the Confluent wire format via this
     * instance's own {@link SchemaCodec.Encoder} (the schema is pre-registered; identified by id). Otherwise
     * the body is published as raw byte[] - the default minimalist behavior.
     */
    private byte[] encode(String topic, Map<String, String> headers, byte[] body, int instance) {
        String schemaId = headers.get(KafkaHeaders.SCHEMA_ID);
        if (schemaId == null) {
            return body;
        }
        SchemaCodec codec = KafkaRuntime.schemaCodec();
        if (codec == null) {
            throw new IllegalStateException("'" + KafkaHeaders.SCHEMA_ID + "' header set but "
                    + "'schema.registry.url' is not configured");
        }
        if (!Utility.getInstance().isDigits(schemaId.trim())) {
            throw new IllegalArgumentException("'" + KafkaHeaders.SCHEMA_ID + "' must be an integer, got '"
                    + schemaId + "'");
        }
        SchemaType type = SchemaType.from(headers.get(KafkaHeaders.SCHEMA_TYPE));
        // The body is the structured value to encode (for JSON, a JSON document); parse it for the serializer.
        Object value = SimpleMapper.getInstance().getMapper().readValue(body, Object.class);
        SchemaCodec.Encoder encoder = encoders.computeIfAbsent(instance, i -> codec.newEncoder());
        return encoder.serialize(topic, type, Integer.parseInt(schemaId.trim()), value);
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
