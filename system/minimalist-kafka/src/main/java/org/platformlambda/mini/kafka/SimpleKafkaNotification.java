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
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.W3cTrace;
import org.platformlambda.mini.kafka.schema.ResolvedSchema;
import org.platformlambda.mini.kafka.schema.SchemaCodec;
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
 *
 * <p><b>Keep the worker pool small.</b> Because {@code @KernelThreadRunner} puts each instance on a (scarce)
 * kernel thread rather than a virtual thread, keep {@code instances} low - {@code 5} is the default here.
 * Kafka publishing is fast and mostly waits on the broker ack, so a handful of single-flight workers sustain
 * high throughput while holding kernel-thread usage down. Raise it only if profiling shows the publish path
 * is genuinely the bottleneck.</p>
 *
 * <p><b>Extension seam.</b> The publisher, schema codec, and outbound header names are resolved through
 * protected accessors, so a library that connects to an additional Kafka cluster (e.g. twin-kafka's
 * {@code secondary.kafka.notification}) can subclass this function and override only those accessors -
 * the routing, header propagation, trace stamping, and Confluent serialization logic is shared.</p>
 */
@KernelThreadRunner
@PreLoad(route = "simple.kafka.notification", instances = 5)
public class SimpleKafkaNotification implements TypedLambdaFunction<byte[], Mono<Void>> {

    // Read-only reserved headers injected by the framework; never forwarded to Kafka as raw headers.
    private static final String MY_ROUTE = "my_route";
    private static final String MY_TRACE_ID = "my_trace_id";
    private static final String MY_TRACE_PATH = "my_trace_path";
    private static final String MY_CORRELATION_ID = "my_correlation_id";
    // Configurable outbound business correlation-id header (default "cid").
    private static final String BUSINESS_CORRELATION_ID_HEADER = AppConfigReader.getInstance()
            .getProperty("kafka.correlation.id.header", KafkaHeaders.CORRELATION_ID);
    // Optional outbound trace-id header (unset by default): when configured, the current trace-id is
    // stamped under this name ALONGSIDE the W3C traceparent, for legacy downstream consumers that read a
    // proprietary trace-id header instead of parsing traceparent.
    private static final String TRACE_ID_HEADER = AppConfigReader.getInstance()
            .getProperty("kafka.trace.id.header");

    // one Encoder per worker instance (an instance is single-flight) -> owner-confined Confluent serializers.
    private final ConcurrentMap<Integer, SchemaCodec.Encoder> encoders = new ConcurrentHashMap<>();

    /** The publisher for the target cluster - twin-kafka overrides this for its secondary cluster. */
    protected KafkaRequestPublisher publisher() {
        return KafkaRuntime.publisher();
    }

    /** The schema codec for the target cluster's registry, or null when schema features are off. */
    protected SchemaCodec schemaCodec() {
        return KafkaRuntime.schemaCodec();
    }

    /** The outbound business correlation-id header name (default from kafka.correlation.id.header). */
    protected String correlationIdHeader() {
        return BUSINESS_CORRELATION_ID_HEADER;
    }

    /** The optional outbound trace-id header name (default from kafka.trace.id.header), or null. */
    protected String traceIdHeader() {
        return TRACE_ID_HEADER;
    }

    /** The registry-url application property named in error messages (for accurate diagnostics). */
    protected String registryUrlKey() {
        return "schema.registry.url";
    }

    // resource: the publisher is the process-wide shared singleton owned by KafkaRuntime,
    // not a resource this function opens - closing it here would tear it down for everyone
    @SuppressWarnings("resource")
    @Override
    public Mono<Void> handleEvent(Map<String, String> headers, byte[] body, int instance) {
        String topic = headers.get(KafkaHeaders.TOPIC);
        if (topic == null) {
            throw new IllegalArgumentException("Missing '" + KafkaHeaders.TOPIC + "' header");
        }
        Integer partition = parsePartition(headers.get(KafkaHeaders.PARTITION));
        Map<String, byte[]> kafkaHeaders = new HashMap<>();
        headers.forEach((key, value) -> {
            if (isPropagatableHeader(key)) {
                kafkaHeaders.put(key, value.getBytes(StandardCharsets.UTF_8));
            }
        });
        // propagate the business correlation-id under the configured header; an explicitly mapped value
        // wins over the flow's correlation-id (model.cid, carried as the my_correlation_id reserved header).
        String cidHeader = correlationIdHeader();
        String businessCorrelationId = headers.getOrDefault(cidHeader, headers.get(MY_CORRELATION_ID));
        if (businessCorrelationId != null) {
            kafkaHeaders.put(cidHeader, businessCorrelationId.getBytes(StandardCharsets.UTF_8));
        }
        String traceparent = currentTraceparent(new PostOffice(headers, instance));
        if (traceparent != null) {
            kafkaHeaders.put(W3cTrace.TRACEPARENT, traceparent.getBytes(StandardCharsets.UTF_8));
        }
        // when the trace-id header is configured, also stamp the trace-id under that name for legacy
        // downstream consumers; an explicitly mapped value wins over the flow's trace-id (my_trace_id).
        String traceHeader = traceIdHeader();
        if (traceHeader != null) {
            String traceId = headers.getOrDefault(traceHeader, headers.get(MY_TRACE_ID));
            if (traceId != null) {
                kafkaHeaders.put(traceHeader, traceId.getBytes(StandardCharsets.UTF_8));
            }
        }
        byte[] payload = encode(topic, headers, body, instance);
        return publisher().publish(topic, partition, kafkaHeaders, payload);
    }

    /**
     * When a {@code subject} header is present, serialize the body into the Confluent wire format via this
     * instance's own {@link SchemaCodec.Encoder}: the {@code subject} + {@code version} (default
     * {@code latest}) are resolved to a global schema id and type, and the body is framed with that id.
     * Otherwise the body is published as raw byte[] - the default minimalist behavior.
     */
    private byte[] encode(String topic, Map<String, String> headers, byte[] body, int instance) {
        String subject = headers.get(KafkaHeaders.SUBJECT);
        if (subject == null || subject.isBlank()) {
            return body;
        }
        SchemaCodec codec = schemaCodec();
        if (codec == null) {
            throw new IllegalStateException("'" + KafkaHeaders.SUBJECT + "' header set but '"
                    + registryUrlKey() + "' is not configured");
        }
        String version = headers.getOrDefault(KafkaHeaders.VERSION, KafkaHeaders.DEFAULT_VERSION);
        // Resolve subject+version -> global id + type (cached); throws IllegalState/IllegalArgument on failure.
        ResolvedSchema resolved = codec.resolve(subject, version);
        // The body is the structured value to encode (for JSON, a JSON document); parse it for the serializer.
        Object value = SimpleMapper.getInstance().getMapper().readValue(body, Object.class);
        SchemaCodec.Encoder encoder = encoders.computeIfAbsent(instance, i -> codec.newEncoder());
        return encoder.serialize(topic, resolved.type(), resolved.id(), value);
    }

    /** Build a W3C traceparent from this function's current trace context (null if tracing is off). */
    private static String currentTraceparent(PostOffice po) {
        TraceInfo trace = po.getTrace();
        return trace == null ? null : W3cTrace.format(po.getTraceId(), trace.spanId);
    }

    /**
     * Whether an event header is forwarded verbatim as a Kafka header. Excludes routing/encoding directives
     * (topic/partition/subject/version), the inbound traceparent (replaced with this hop's own span), the
     * correlation-id and configured trace-id headers (stamped explicitly from the resolved values), and the
     * framework's read-only reserved headers (my_route / my_trace_id / my_trace_path / my_correlation_id).
     */
    private boolean isPropagatableHeader(String key) {
        return !KafkaHeaders.TOPIC.equals(key)
                && !KafkaHeaders.PARTITION.equals(key)
                && !KafkaHeaders.SUBJECT.equals(key)
                && !KafkaHeaders.VERSION.equals(key)
                && !W3cTrace.TRACEPARENT.equals(key)
                && !correlationIdHeader().equals(key)
                && !key.equals(traceIdHeader())
                && !MY_ROUTE.equals(key)
                && !MY_TRACE_ID.equals(key)
                && !MY_TRACE_PATH.equals(key)
                && !MY_CORRELATION_ID.equals(key);
    }

    private static Integer parsePartition(String value) {
        return value == null ? null : Integer.valueOf(value.trim());
    }
}
