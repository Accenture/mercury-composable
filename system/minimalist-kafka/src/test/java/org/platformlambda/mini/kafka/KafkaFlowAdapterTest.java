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

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.Utility;

import org.platformlambda.core.util.W3cTrace;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Standalone end-to-end proof of the minimalist-kafka building blocks (no Redis, no REST):
 *
 * <pre>
 *   PostOffice -> simple.kafka.notification -> [Kafka topic] -> KafkaFlowAdapter -> kafka-sink-flow
 * </pre>
 *
 * <p>It also asserts that the trace-id stays continuous across the Kafka hop: the notification function
 * stamps a {@code traceparent} from its own span; the adapter parses it and chains the sink flow onto
 * that trace, so the sink task observes the same trace-id the caller started with.</p>
 */
// resource: the adapter, publisher and schema-registry client are process-wide singletons owned by
// KafkaRuntime for the app lifetime - closed once in shutdown(), never per test
@SuppressWarnings("resource")
class KafkaFlowAdapterTest {

    private static final String TOPIC = "mini-test-topic";
    // binding with per-entry 'trace.id.header' / 'correlation.id.header' overrides (legacy upstream)
    private static final String LEGACY_TOPIC = "legacy-test-topic";
    // The two Confluent-wire-format e2e cases: JSON Schema (JSON_TOPIC -> json-sink-flow -> JsonSinkTask)
    // and Avro (AVRO_TOPIC -> avro-sink-flow -> AvroSinkTask). Each has its own flow + sink so the two never
    // share mutable state; the codec's ability to decode both by embedded schema id is proven in SchemaCodecTest.
    private static final String JSON_TOPIC = "json-test-topic";
    private static final String AVRO_TOPIC = "avro-test-topic";
    // matches the topic-pattern binding 'events\.[a-z]{2}'; 2 partitions so an explicit-partition publish
    // (see topicPatternRoutesConcreteTopicMetadataThroughFlow) actually exercises partition selection.
    private static final String PATTERN_TOPIC = "events.de";
    // second-level routing binding ('flows' rule list + serializer: 'json' + task ttl)
    private static final String ROUTED_TOPIC = "routed-test-topic";
    private static final String JSON_SCHEMA =
            "{\"type\":\"object\",\"properties\":{\"hello\":{\"type\":\"string\"}},\"additionalProperties\":true}";
    private static final String AVRO_SCHEMA =
            "{\"type\":\"record\",\"name\":\"Greeting\",\"namespace\":\"test\","
            + "\"fields\":[{\"name\":\"hello\",\"type\":\"string\"}]}";
    private static final String TRACE_ID = "11112222333344445555666677778888";

    private static EmbeddedKafka kafka;
    private static EmbeddedSchemaRegistry registry;

    @BeforeAll
    static void boot() throws Exception {
        kafka = new EmbeddedKafka();
        KafkaTestSupport.createTopic(kafka.bootstrapServers(), TOPIC);
        KafkaTestSupport.createTopic(kafka.bootstrapServers(), LEGACY_TOPIC);
        KafkaTestSupport.createTopic(kafka.bootstrapServers(), JSON_TOPIC);
        KafkaTestSupport.createTopic(kafka.bootstrapServers(), AVRO_TOPIC);
        // created before AutoStart.main() starts the topic-pattern consumer's subscribe(Pattern), same as
        // the topics above - avoids a race against the adapter's first metadata fetch.
        KafkaTestSupport.createTopic(kafka.bootstrapServers(), PATTERN_TOPIC, 2);
        KafkaTestSupport.createTopic(kafka.bootstrapServers(), ROUTED_TOPIC);
        // self-contained, in-JVM Confluent-compatible registry on a random port. Set the exact config key
        // as a system property (ConfigReader consults system properties before the file, at get-time) so it
        // wins regardless of when AppConfigReader was first loaded by other tests. The id->schema cache is
        // in-memory (platform ManagedCache) and SchemaCodec clears it at startup.
        registry = new EmbeddedSchemaRegistry();
        System.setProperty("schema.registry.url", registry.baseUrl());
        // resolved by ${KAFKA_BOOTSTRAP_SERVERS:...} in the kafka-producer/consumer.properties templates
        System.setProperty("KAFKA_BOOTSTRAP_SERVERS", kafka.bootstrapServers());
        AutoStart.main(new String[0]);
        // KafkaFlowAutoStart (@MainApplication) runs after the engine is up: it sets the publisher and
        // starts the adapter. A non-null adapter is our readiness signal.
        // generous for busy CI executors - the poll returns as soon as the adapter is ready
        long deadline = System.currentTimeMillis() + 90000;
        while (KafkaRuntime.adapter() == null && System.currentTimeMillis() < deadline) {
            Utility.getInstance().sleep(100);
        }
        assertNotNull(KafkaRuntime.adapter(), "Kafka flow adapter should have started");
        assertNotNull(KafkaRuntime.schemaCodec(), "schema codec should have been built from schema.registry.url");
    }

    @AfterAll
    static void shutdown() {
        if (KafkaRuntime.adapter() != null) {
            KafkaRuntime.adapter().close();
        }
        if (KafkaRuntime.publisher() != null) {
            KafkaRuntime.publisher().close();
        }
        if (kafka != null) {
            kafka.close();
        }
        if (registry != null) {
            registry.close();
        }
        System.clearProperty("KAFKA_BOOTSTRAP_SERVERS");
        System.clearProperty("schema.registry.url");
    }

    @Test
    void notificationPublishesAndAdapterRoutesToFlowWithTraceContinuity() throws Exception {
        KafkaSinkTask.RECEIVED.clear();
        String cid = Utility.getInstance().getUuid();
        // Sending under TRACE_ID makes the notification function run within that trace, so the
        // traceparent it stamps carries TRACE_ID across the Kafka hop into the sink flow.
        PostOffice po = PostOffice.trackable("unit.test", TRACE_ID, "TEST /mini");
        po.send(new EventEnvelope().setTo("simple.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, TOPIC)
                .setHeader(KafkaHeaders.CORRELATION_ID, cid)
                .setBody("{\"hello\":\"kafka\"}".getBytes(StandardCharsets.UTF_8))
                .setTraceId(TRACE_ID).setTracePath("TEST /mini"));

        Map<String, Object> received = KafkaSinkTask.RECEIVED.poll(25, TimeUnit.SECONDS);
        assertNotNull(received, "the sink flow should receive the message routed by the adapter");
        assertEquals(cid, received.get("cid"), "correlation-id propagated as a Kafka header");
        assertEquals(cid, received.get("myCid"),
                "business cid surfaced to the task as model.cid / getMyCorrelationId() through the flow engine");
        assertEquals("{\"hello\":\"kafka\"}", received.get("body"), "body round-tripped through Kafka");
        assertEquals(TRACE_ID, received.get("traceId"), "trace-id stayed continuous across the Kafka hop");
    }

    @Test
    void notificationStampsTraceContextUnderBothNames() throws Exception {
        KafkaSinkTask.RECEIVED.clear();
        // kafka.traceparent.header=x-kafka-trace in this test suite: the notification stamps the SAME
        // W3C value under both the standard "traceparent" and the custom name, so the context survives
        // middleware that mishandles the standard header while compliant consumers keep working
        String cid = Utility.getInstance().getUuid();
        PostOffice po = PostOffice.trackable("unit.test", TRACE_ID, "TEST /dual/stamp");
        po.send(new EventEnvelope().setTo("simple.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, TOPIC)
                .setHeader(KafkaHeaders.CORRELATION_ID, cid)
                .setBody("{\"hello\":\"dual\"}".getBytes(StandardCharsets.UTF_8))
                .setTraceId(TRACE_ID).setTracePath("TEST /dual/stamp"));

        Map<String, Object> received = KafkaSinkTask.RECEIVED.poll(25, TimeUnit.SECONDS);
        assertNotNull(received, "the sink flow should receive the message");
        String standard = (String) received.get("traceparent");
        assertNotNull(standard, "the standard traceparent header is stamped");
        assertTrue(standard.contains(TRACE_ID), "the stamped traceparent continues the caller's trace");
        assertEquals(standard, received.get("customTraceparent"),
                "the same W3C value must be stamped under the custom kafka.traceparent.header name");
    }

    @Test
    void adapterAdoptsTraceContextFromCustomHeaderName() throws Exception {
        KafkaSinkTask.RECEIVED.clear();
        // inbound impedance matching: an upstream carries W3C trace context ONLY under the custom
        // name (legacy middleware stripped the standard header); the adapter falls back to it
        String customTraceId = "aaaa2222333344445555666677778888";
        Map<String, byte[]> recordHeaders = new HashMap<>();
        recordHeaders.put("x-kafka-trace",
                W3cTrace.format(customTraceId, "aaaabbbbccccdddd").getBytes(StandardCharsets.UTF_8));
        KafkaRuntime.publisher().publishSync(TOPIC, null, recordHeaders,
                "{\"hello\":\"custom\"}".getBytes(StandardCharsets.UTF_8), 10000);

        Map<String, Object> received = KafkaSinkTask.RECEIVED.poll(25, TimeUnit.SECONDS);
        assertNotNull(received, "the sink flow should receive the message");
        assertEquals(customTraceId, received.get("traceId"),
                "the custom traceparent name delivers the context when the standard header is absent");
    }

    @Test
    void standardTraceparentWinsOverCustomHeaderName() throws Exception {
        KafkaSinkTask.RECEIVED.clear();
        // the standards position: a well-formed standard traceparent means the upstream already
        // speaks W3C/OTel - a residual proprietary header alongside it is safely ignored
        String residualTraceId = "aaaa3333444455556666777788889999";
        String standardTraceId = "bbbb3333444455556666777788889999";
        Map<String, byte[]> recordHeaders = new HashMap<>();
        recordHeaders.put("x-kafka-trace",
                W3cTrace.format(residualTraceId, "aaaabbbbccccdddd").getBytes(StandardCharsets.UTF_8));
        recordHeaders.put(W3cTrace.TRACEPARENT,
                W3cTrace.format(standardTraceId, "ddddccccbbbbaaaa").getBytes(StandardCharsets.UTF_8));
        KafkaRuntime.publisher().publishSync(TOPIC, null, recordHeaders,
                "{\"hello\":\"standard\"}".getBytes(StandardCharsets.UTF_8), 10000);

        Map<String, Object> received = KafkaSinkTask.RECEIVED.poll(25, TimeUnit.SECONDS);
        assertNotNull(received, "the sink flow should receive the message");
        assertEquals(standardTraceId, received.get("traceId"),
                "the standard W3C traceparent wins; the custom name is a fallback only");
    }

    @Test
    void legacyHeaderOverridesCaptureTraceAndCorrelationId() throws Exception {
        KafkaSinkTask.RECEIVED.clear();
        // a legacy upstream publishes its own header names and no W3C traceparent; the binding's
        // 'trace.id.header' / 'correlation.id.header' overrides capture both
        String legacyCid = "legacy-cid-" + Utility.getInstance().getUuid();
        String legacyTraceId = "legacy-trace-0001";
        Map<String, byte[]> recordHeaders = new HashMap<>();
        recordHeaders.put("X-Correlation-ID", legacyCid.getBytes(StandardCharsets.UTF_8));
        recordHeaders.put("X-Legacy-Trace", legacyTraceId.getBytes(StandardCharsets.UTF_8));
        KafkaRuntime.publisher().publishSync(LEGACY_TOPIC, null, recordHeaders,
                "{\"hello\":\"legacy\"}".getBytes(StandardCharsets.UTF_8), 10000);

        Map<String, Object> received = KafkaSinkTask.RECEIVED.poll(25, TimeUnit.SECONDS);
        assertNotNull(received, "the sink flow should receive the legacy message");
        assertEquals(legacyCid, received.get("myCid"),
                "business cid captured from the per-binding 'correlation.id.header' override");
        assertEquals(legacyTraceId, received.get("traceId"),
                "trace-id captured from the per-binding 'trace.id.header' override (no traceparent sent)");
    }

    @Test
    void traceparentTakesPrecedenceOverLegacyTraceHeader() throws Exception {
        KafkaSinkTask.RECEIVED.clear();
        // when a well-formed W3C traceparent is present, it wins over the configured trace-id header
        String w3cTraceId = "99998888777766665555444433332222";
        Map<String, byte[]> recordHeaders = new HashMap<>();
        recordHeaders.put("X-Legacy-Trace", "should-not-win".getBytes(StandardCharsets.UTF_8));
        recordHeaders.put(W3cTrace.TRACEPARENT,
                W3cTrace.format(w3cTraceId, "aaaabbbbccccdddd").getBytes(StandardCharsets.UTF_8));
        KafkaRuntime.publisher().publishSync(LEGACY_TOPIC, null, recordHeaders,
                "{\"hello\":\"w3c\"}".getBytes(StandardCharsets.UTF_8), 10000);

        Map<String, Object> received = KafkaSinkTask.RECEIVED.poll(25, TimeUnit.SECONDS);
        assertNotNull(received, "the sink flow should receive the message");
        assertEquals(w3cTraceId, received.get("traceId"),
                "a well-formed traceparent takes precedence over the configured trace-id header");
    }

    @Test
    @SuppressWarnings("unchecked")
    void jsonSchemaFramedMessageDecodedIntoFlow() throws Exception {
        JsonSinkTask.RECEIVED.clear();
        // pre-register the JSON schema (governed artifact) under a subject; the producer names the subject
        String subject = JSON_TOPIC + "-value";
        KafkaRuntime.schemaCodec().client().register(subject, new JsonSchema(JSON_SCHEMA));
        String cid = Utility.getInstance().getUuid();

        // publish via simple.kafka.notification with subject (version defaults to latest): the producer
        // resolves the schema id, serializes into the Confluent wire format; the schema-enabled adapter
        // binding decodes it back to a Map for the flow.
        PostOffice po = PostOffice.trackable("unit.test", TRACE_ID, "TEST /json");
        po.send(new EventEnvelope().setTo("simple.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, JSON_TOPIC)
                .setHeader(KafkaHeaders.CORRELATION_ID, cid)
                .setHeader(KafkaHeaders.SUBJECT, subject)
                .setBody("{\"hello\":\"schema\"}".getBytes(StandardCharsets.UTF_8))
                .setTraceId(TRACE_ID).setTracePath("TEST /json"));

        Map<String, Object> received = JsonSinkTask.RECEIVED.poll(25, TimeUnit.SECONDS);
        assertNotNull(received, "the json sink flow should receive the decoded message");
        assertEquals(cid, received.get("cid"), "correlation-id propagated as a Kafka header");
        assertEquals(cid, received.get("myCid"),
                "business cid surfaced to the task as model.cid / getMyCorrelationId() through the flow engine");
        assertInstanceOf(Map.class, received.get("body"), "body decoded to a Map (not raw byte[])");
        assertEquals("schema", ((Map<String, Object>) received.get("body")).get("hello"),
                "JSON Schema message round-tripped: produced by id, decoded by the adapter");
        assertEquals(TRACE_ID, received.get("traceId"), "trace-id stayed continuous across the Kafka hop");
    }

    @Test
    @SuppressWarnings("unchecked")
    void avroFramedMessageDecodedIntoFlow() throws Exception {
        AvroSinkTask.RECEIVED.clear();
        // pre-register the Avro schema (governed artifact) under a subject; the producer names the subject
        String subject = AVRO_TOPIC + "-value";
        KafkaRuntime.schemaCodec().client().register(subject, new AvroSchema(AVRO_SCHEMA));
        String cid = Utility.getInstance().getUuid();

        // publish via simple.kafka.notification with subject (version defaults to latest): the body is
        // serialized into the Confluent Avro wire format; the schema-enabled adapter binding decodes it to a Map.
        PostOffice po = PostOffice.trackable("unit.test", TRACE_ID, "TEST /avro");
        po.send(new EventEnvelope().setTo("simple.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, AVRO_TOPIC)
                .setHeader(KafkaHeaders.CORRELATION_ID, cid)
                .setHeader(KafkaHeaders.SUBJECT, subject)
                .setBody("{\"hello\":\"avro\"}".getBytes(StandardCharsets.UTF_8))
                .setTraceId(TRACE_ID).setTracePath("TEST /avro"));

        Map<String, Object> received = AvroSinkTask.RECEIVED.poll(25, TimeUnit.SECONDS);
        assertNotNull(received, "the avro sink flow should receive the decoded Avro message");
        assertEquals(cid, received.get("cid"), "correlation-id propagated as a Kafka header");
        assertEquals(cid, received.get("myCid"),
                "business cid surfaced to the task as model.cid / getMyCorrelationId() through the flow engine");
        assertInstanceOf(Map.class, received.get("body"), "body decoded to a Map (not raw byte[])");
        assertEquals("avro", ((Map<String, Object>) received.get("body")).get("hello"),
                "Avro message round-tripped: produced by id, decoded by the adapter");
        assertEquals(TRACE_ID, received.get("traceId"), "trace-id stayed continuous across the Kafka hop");
    }

    @Test
    void topicPatternRoutesConcreteTopicMetadataThroughFlow() throws Exception {
        KafkaPatternSinkTask.RECEIVED.clear();
        String cid = Utility.getInstance().getUuid();

        // publish directly to the concrete matched topic (the adapter's topic-pattern binding subscribes
        // via subscribe(Pattern), not this literal name) and pin a partition so the record's own metadata
        // - not the binding's regex - is what the flow must recover.
        PostOffice po = PostOffice.trackable("unit.test", TRACE_ID, "TEST /pattern");
        po.send(new EventEnvelope().setTo("simple.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, PATTERN_TOPIC)
                .setHeader(KafkaHeaders.PARTITION, "1")
                .setHeader(KafkaHeaders.CORRELATION_ID, cid)
                .setBody("pattern-payload".getBytes(StandardCharsets.UTF_8))
                .setTraceId(TRACE_ID).setTracePath("TEST /pattern"));

        Map<String, Object> received = KafkaPatternSinkTask.RECEIVED.poll(25, TimeUnit.SECONDS);
        assertNotNull(received, "the topic-pattern binding should route the concrete matched topic");
        assertEquals(cid, received.get("cid"), "correlation-id propagated as a Kafka header");
        assertEquals("pattern-payload", received.get("body"), "body round-tripped through Kafka");
        assertEquals(PATTERN_TOPIC, received.get("topic"),
                "metadata.topic is the record's actual topic, not the binding's configured regex");
        assertEquals(1, received.get("partition"), "metadata.partition is the record's actual partition");
        assertNotNull(received.get("offset"), "metadata.offset is present");
    }

    @Test
    void routingRuleSelectsFlowByHeader() throws Exception {
        RoutedOrderSinkTask.RECEIVED.clear();
        String cid = Utility.getInstance().getUuid();
        Map<String, byte[]> recordHeaders = new HashMap<>();
        recordHeaders.put("type", "order".getBytes(StandardCharsets.UTF_8));
        recordHeaders.put("cid", cid.getBytes(StandardCharsets.UTF_8));
        recordHeaders.put(W3cTrace.TRACEPARENT,
                W3cTrace.format(TRACE_ID, "aaaabbbbccccdddd").getBytes(StandardCharsets.UTF_8));
        KafkaRuntime.publisher().publishSync(ROUTED_TOPIC, null, recordHeaders,
                "{\"hello\":\"routed\"}".getBytes(StandardCharsets.UTF_8), 10000);

        Map<String, Object> received = RoutedOrderSinkTask.RECEIVED.poll(25, TimeUnit.SECONDS);
        assertNotNull(received, "the exact-match rule should select the order flow");
        assertEquals("order", received.get("type"), "the routing key the rule matched on");
        assertEquals(cid, received.get("myCid"), "business cid surfaced as model.cid through the flow engine");
        assertEquals(TRACE_ID, received.get("traceId"), "trace-id stayed continuous across the Kafka hop");
        assertInstanceOf(Map.class, received.get("body"),
                "serializer 'json' delivered a decoded Map to the selected flow");
    }

    @Test
    void wildcardRuleSelectsFlowByHeader() throws Exception {
        RoutedOrderSinkTask.RECEIVED.clear();
        String cid = Utility.getInstance().getUuid();
        Map<String, byte[]> recordHeaders = new HashMap<>();
        recordHeaders.put("type", "bulk-7".getBytes(StandardCharsets.UTF_8));
        recordHeaders.put("cid", cid.getBytes(StandardCharsets.UTF_8));
        KafkaRuntime.publisher().publishSync(ROUTED_TOPIC, null, recordHeaders,
                "{\"hello\":\"bulk\"}".getBytes(StandardCharsets.UTF_8), 10000);

        Map<String, Object> received = RoutedOrderSinkTask.RECEIVED.poll(25, TimeUnit.SECONDS);
        assertNotNull(received, "the 'bulk-*' wildcard rule should select the order flow");
        assertEquals("bulk-7", received.get("type"), "the wildcard-matched routing key");
        assertEquals(cid, received.get("myCid"), "business cid surfaced as model.cid through the flow engine");
    }

    @Test
    @SuppressWarnings("unchecked")
    void bodyRuleRoutesToTaskTarget() throws Exception {
        RoutedTaskSink.RECEIVED.clear();
        String cid = Utility.getInstance().getUuid();
        Map<String, byte[]> recordHeaders = new HashMap<>();
        recordHeaders.put("cid", cid.getBytes(StandardCharsets.UTF_8));
        recordHeaders.put(W3cTrace.TRACEPARENT,
                W3cTrace.format(TRACE_ID, "aaaabbbbccccdddd").getBytes(StandardCharsets.UTF_8));
        KafkaRuntime.publisher().publishSync(ROUTED_TOPIC, null, recordHeaders,
                "{\"event\":{\"kind\":\"refund\"},\"amount\":10}".getBytes(StandardCharsets.UTF_8), 10000);

        Map<String, Object> received = RoutedTaskSink.RECEIVED.poll(25, TimeUnit.SECONDS);
        assertNotNull(received, "the input.body rule should route the record to the task:// target");
        Map<String, Object> body = (Map<String, Object>) assertInstanceOf(Map.class, received.get("body"),
                "the task received the decoded Map as its whole body (no dataset wrapper)");
        assertEquals("refund", ((Map<String, Object>) body.get("event")).get("kind"));
        assertEquals(cid, received.get("myCid"),
                "business cid injected as my_correlation_id via the engine-managed my_cid tag");
        assertEquals(TRACE_ID, received.get("traceId"), "trace-id stayed continuous into the direct task");
        Map<String, String> taskHeaders = (Map<String, String>) received.get("headers");
        assertEquals(cid, taskHeaders.get("cid"), "inbound record headers are copied verbatim to the task");
    }

    @Test
    void nonJsonRecordFallsThroughToDefaultTargetWithRawBytes() throws Exception {
        RoutedTaskSink.RECEIVED.clear();
        // best-effort serializer contract: an unparseable record keeps its raw byte[] body, every
        // input.body rule is a non-match, and the raw bytes simply pass to the default target
        KafkaRuntime.publisher().publishSync(ROUTED_TOPIC, null, Map.of(),
                "not-json-payload".getBytes(StandardCharsets.UTF_8), 10000);

        Map<String, Object> received = RoutedTaskSink.RECEIVED.poll(25, TimeUnit.SECONDS);
        assertNotNull(received, "the default rule should catch the unmatched record");
        byte[] body = assertInstanceOf(byte[].class, received.get("body"),
                "the raw byte[] passes to the default target unchanged");
        assertEquals("not-json-payload", new String(body, StandardCharsets.UTF_8));
    }

    @Test
    void rejectsUnknownTaskRouteAtStartup() {
        // a task:// target must exist in the platform registry when the adapter starts (functions
        // preload before the @MainApplication), the same fail-fast contract as an unknown flow
        ConfigReader reader = new ConfigReader();
        reader.load(Map.of("consumer", List.of(Map.of("topic", "orders",
                "flows", List.of("default -> task://no.such.route")))));
        RetryPolicy policy = new RetryPolicy(0, 0, null);
        Properties props = new Properties();
        assertThrows(IllegalArgumentException.class,
                () -> new KafkaFlowAdapter(props, reader, 1000, policy, null));
    }

}
