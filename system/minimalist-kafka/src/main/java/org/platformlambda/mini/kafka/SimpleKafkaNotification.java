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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Kafka Notification Function - a minimalist composable function that publishes a Post Office event to a
 * Kafka topic. It reads the {@code topic} header (required) and the optional {@code partition} header
 * for routing, forwards every other event header as a Kafka header (byte[]), and uses the event body as
 * the Kafka message body - byte[] verbatim, or a Map/List automatically serialized to JSON bytes (see
 * {@link #toBytes}, the outbound symmetry of the flow adapter's inbound {@code serializer: 'json'};
 * NON-schema topics only - the schema-registry path keeps its byte[] JSON-document contract).
 * It wraps the shared, thread-safe {@link KafkaRequestPublisher} singleton, and is drop-n-forget
 * because Kafka is asynchronous.
 *
 * <p><b>Trace propagation.</b> Rather than forwarding the caller's (now-stale) traceparent, it stamps a
 * fresh W3C {@code traceparent} built from this function's <i>own</i> current span. Therefore, the consuming side
 * adopts this span as the parent of the next hop - keeping the trace continuous across the Kafka boundary.</p>
 *
 * <p>byte[] is the wire form for headers and body so no custom Kafka serializer/deserializer is needed.
 * This is the minimalist building block; richer encodings (e.g. a Confluent Schema Registry) layer on
 * top, and the Map/List JSON convenience above is plain SimpleMapper - not a Kafka serde.</p>
 *
 * <p>It returns the {@code Mono<Void>} from the publisher: the platform-core worker subscribes to it,
 * deferring the function's completion until the broker acknowledges. A caller using {@code po.request}
 * (RPC) therefore learns whether the publishing succeeded - a publishing failure propagates back as an error -
 * while a {@code po.send} (async) caller simply doesn't observe it. The Mono is realized as {@code null}
 * (a {@code Void} body) on success.</p>
 *
 * <p><b>{@code @KernelThreadRunner}.</b> When the Schema Registry is used, this function builds Confluent
 * serializers, which use {@code synchronized} internally and are not thread-safe. Running on a kernel thread
 * (rather than a virtual thread) avoids pinning a virtual-thread carrier on those {@code synchronized}
 * sections, and each worker instance is single-flight. Therefore, each instance keeps its <b>own</b>
 * {@link SchemaCodec.Encoder} (in {@link #encoders}, keyed by instance), guaranteeing a Confluent serializer
 * is never touched by two threads at once.</p>
 *
 * <p><b>Keep the worker pool small.</b> Because {@code @KernelThreadRunner} puts each instance on a (scarce)
 * kernel thread rather than a virtual thread, keep {@code instances} low - {@code 5} is the default here.
 * Kafka publishing is fast and mostly waits on the broker ack, so a handful of single-flight workers sustain
 * high throughput while holding kernel-thread usage down. Raise it only if profiling shows the publishing path
 * is genuinely the bottleneck.</p>
 *
 * <p><b>Extension seam.</b> The publisher, schema codec, and outbound header names are resolved through
 * protected accessors, so a library that connects to an additional Kafka cluster (e.g. twin-kafka's
 * {@code secondary.kafka.notification}) can subclass this function and override only those accessors -
 * the routing, header propagation, trace stamping, and Confluent serialization logic is shared.</p>
 */
@KernelThreadRunner
@PreLoad(route = SimpleKafkaNotification.ROUTE, instances = 5)
public class SimpleKafkaNotification implements TypedLambdaFunction<Object, Mono<Void>> {

    /** The route this function registers under. */
    public static final String ROUTE = "simple.kafka.notification";

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
    // Configurable traceparent header name (default "traceparent"): when customized, the W3C trace
    // context is stamped under BOTH names, for an intermediary or downstream convention that does not
    // handle the standard header.
    private static final String TRACEPARENT_HEADER = AppConfigReader.getInstance()
            .getProperty("kafka.traceparent.header", W3cTrace.TRACEPARENT);

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

    /** The traceparent header name (default "traceparent", from kafka.traceparent.header). */
    protected String traceparentHeader() {
        return TRACEPARENT_HEADER;
    }

    /** The registry-url application property named in error messages (for accurate diagnostics). */
    protected String registryUrlKey() {
        return "schema.registry.url";
    }

    /** The producer opt-out property named in error messages - twin-kafka overrides with its secondary key. */
    protected String producerEnabledKey() {
        return KafkaClientConfig.PRODUCER_ENABLED;
    }

    /**
     * The target cluster's publisher, or a loud failure when that cluster's producer is switched off.
     *
     * <p>The function stays REGISTERED when the producer is disabled, so a flow that publishes anyway
     * fails with the setting that caused it rather than a "route not found" that sends the developer
     * hunting through flow YAML. Called at the publish site, after every input check, so a caller's
     * own mistake is never masked by this environment condition.</p>
     */
    private KafkaRequestPublisher requirePublisher() {
        KafkaRequestPublisher publisher = publisher();
        if (publisher == null) {
            throw new IllegalStateException("Kafka producer is disabled (" + producerEnabledKey()
                    + "=false); cannot publish");
        }
        return publisher;
    }

    // resource: the publisher is the process-wide shared singleton owned by KafkaRuntime,
    // not a resource this function opens - closing it here would tear it down for everyone
    @SuppressWarnings("resource")
    @Override
    public Mono<Void> handleEvent(Map<String, String> headers, Object body, int instance) {
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
            // when a custom traceparent header name is configured, stamp the same value under that
            // name too, for an intermediary or downstream convention that does not handle the standard
            String customTraceparent = traceparentHeader();
            if (!W3cTrace.TRACEPARENT.equalsIgnoreCase(customTraceparent)) {
                kafkaHeaders.put(customTraceparent, traceparent.getBytes(StandardCharsets.UTF_8));
            }
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
        // checked last: every caller-input error (missing topic, a non-byte[] schema document) is the
        // caller's own mistake and must surface ahead of an environment condition
        return requirePublisher().publish(topic, partition, kafkaHeaders, payload);
    }

    /**
     * Outbound symmetry with the flow adapter's inbound {@code serializer: 'json'} - NON-SCHEMA topics
     * only (the caller of {@link #encode} applies this on the raw path exclusively): a Map or List body
     * from the calling application is automatically serialized to JSON bytes with the default
     * SimpleMapper. byte[] passes through verbatim (the minimalist default) and null stays null (a
     * Kafka tombstone). Anything else is rejected loudly rather than published in a surprising shape.
     * Visible for testing.
     */
    static byte[] toBytes(Object body) {
        return switch (body) {
            case null -> null;
            case byte[] bytes -> bytes;
            case Map<?, ?> map -> SimpleMapper.getInstance().getMapper().writeValueAsBytes(map);
            case List<?> list -> SimpleMapper.getInstance().getMapper().writeValueAsBytes(list);
            default -> throw new IllegalArgumentException(
                    "body must be byte[], Map or List, got " + body.getClass().getSimpleName());
        };
    }

    /**
     * When a {@code subject} header is present, serialize the body into the Confluent wire format via this
     * instance's own {@link SchemaCodec.Encoder}: the {@code subject} + {@code version} (default
     * {@code latest}) are resolved to a global schema id and type, and the body is framed with that id -
     * the schema-path body contract stays byte[] (a JSON document), unchanged. Otherwise (a NON-schema
     * topic) the body is published as raw byte[], with a Map/List auto-serialized to JSON bytes first
     * (see {@link #toBytes}) - the JSON convenience applies to non-schema-registry topics only.
     */
    private byte[] encode(String topic, Map<String, String> headers, Object body, int instance) {
        String subject = headers.get(KafkaHeaders.SUBJECT);
        if (subject == null || subject.isBlank()) {
            return toBytes(body);
        }
        if (!(body instanceof byte[] document)) {
            throw new IllegalArgumentException("body must be byte[] (a JSON document) when '"
                    + KafkaHeaders.SUBJECT + "' is set, got "
                    + (body == null ? "null" : body.getClass().getSimpleName()));
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
        Object value = SimpleMapper.getInstance().getMapper().readValue(document, Object.class);
        SchemaCodec.Encoder encoder = encoders.computeIfAbsent(instance, i -> codec.newEncoder());
        return encoder.serialize(topic, resolved.type(), resolved.id(), value);
    }

    /** Build a W3C traceparent from this function's current trace context (null if tracing is off). */
    private static String currentTraceparent(PostOffice po) {
        TraceInfo trace = po.getTrace();
        return trace == null ? null : W3cTrace.format(po.getTraceId(), trace.spanId);
    }

    /**
     * Whether an event header is forwarded verbatim as a Kafka header.
     * Excludes:
     * 1. routing/encoding directives (topic/partition/subject/version)
     * 2. the inbound traceparent under the standard or configured name (replaced with this hop's own span)
     * 3. the correlation-id and configured trace-id headers (stamped explicitly from the resolved values)
     * 4. and the framework's read-only reserved headers (my_route / my_trace_id / my_trace_path / my_correlation_id)
     */
    private boolean isPropagatableHeader(String key) {
        return !KafkaHeaders.TOPIC.equals(key)
                && !KafkaHeaders.PARTITION.equals(key)
                && !KafkaHeaders.SUBJECT.equals(key)
                && !KafkaHeaders.VERSION.equals(key)
                && !W3cTrace.TRACEPARENT.equals(key)
                && !traceparentHeader().equals(key)
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
