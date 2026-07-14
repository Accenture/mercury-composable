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

package org.platformlambda.twin.kafka;

import io.confluent.kafka.schemaregistry.json.JsonSchema;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.platformlambda.mini.kafka.KafkaHeaders;
import org.platformlambda.mini.kafka.KafkaRuntime;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dual-cluster end-to-end proof of the twin-kafka bridge topology - two independent embedded KRaft
 * brokers in one JVM (cluster A = primary on 19092, cluster B = secondary on 18092):
 *
 * <pre>
 *   A-to-B:  simple.kafka.notification -> [A: bridge.source] -> primary adapter -> bridge-a-to-b flow
 *            -> secondary.kafka.notification -> [B: bridge.mirror] -> secondary adapter -> sink-b
 *   B-to-A:  secondary.kafka.notification -> [B: reverse.source] -> secondary adapter -> bridge-b-to-a flow
 *            -> simple.kafka.notification -> [A: reverse.mirror] -> primary adapter -> sink-a
 * </pre>
 *
 * <p>The Schema Registry topology is deliberately ASYMMETRIC - none on the primary (as with an on-prem
 * Apache Kafka), an embedded Confluent-compatible registry on the secondary (as with a cloud Confluent
 * cluster) - proving each cluster's registry is optional and independent.</p>
 */
// resource: the adapters, publishers and registry client are process-wide singletons owned by the two
// runtimes for the app lifetime - closed once in shutdown(), never per test
@SuppressWarnings("resource")
class TwinKafkaBridgeTest {

    private static final String TRACE_A = "aaaa1111bbbb2222cccc3333dddd4444";
    private static final String TRACE_B = "eeee5555ffff6666aaaa7777bbbb8888";
    private static final String JSON_SCHEMA =
            "{\"type\":\"object\",\"properties\":{\"hello\":{\"type\":\"string\"}},\"additionalProperties\":true}";

    private static EmbeddedKafka clusterA;
    private static EmbeddedKafka clusterB;
    private static EmbeddedSchemaRegistry registryB;

    @BeforeAll
    static void boot() throws Exception {
        clusterA = new EmbeddedKafka(19092, 19093, "/tmp/twin-kafka-a");
        clusterB = new EmbeddedKafka(18092, 18093, "/tmp/twin-kafka-b");
        createTopics(clusterA.bootstrapServers(), "bridge.source", "reverse.mirror");
        createTopics(clusterB.bootstrapServers(), "bridge.mirror", "reverse.source", "schema.mirror",
                "header.probe", "poison.source", "poison.dlq");
        // registry on the SECONDARY side only (asymmetric topology)
        registryB = new EmbeddedSchemaRegistry();
        // Inject via the EXACT config key, not the ${SECONDARY_SCHEMA_REGISTRY_URL} reference in
        // application.properties: AppConfigReader resolves ${VAR} references ONCE when the singleton
        // first loads, and another test class in this JVM may already have initialized it before this
        // @BeforeAll runs (surefire's default run order is filesystem-dependent, so class order varies
        // by CI environment - a field Jenkins build froze the reference to blank and the secondary
        // adapter failed with "'secondary.schema.registry.url' is not configured" while GitHub CI
        // passed). ConfigReader.get() checks System.getProperty(<exact key>) live on every read, so
        // this override is immune to initialization order.
        System.setProperty("secondary.schema.registry.url", registryB.baseUrl());
        // point the producer/consumer templates at the two embedded brokers - safe as ${VAR} references
        // because the templates are loaded fresh by each adapter/publisher AFTER this point
        System.setProperty("KAFKA_BOOTSTRAP_SERVERS", clusterA.bootstrapServers());
        System.setProperty("SECONDARY_KAFKA_BOOTSTRAP_SERVERS", clusterB.bootstrapServers());
        AutoStart.main(new String[0]);
        // both @MainApplication entry points must complete: primary + secondary adapters ready.
        // The deadline is deliberately generous: this boot brings up TWO embedded KRaft brokers,
        // a schema registry, and the full application - the poll returns the moment both adapters
        // are ready, so a large budget costs nothing on a fast machine. If this deadline ever
        // expires, check the log for an AppStarter "Unable to start" ERROR before blaming a slow
        // executor: a crashed @MainApplication never registers its adapter, and no deadline
        // extension can fix that.
        long deadline = System.currentTimeMillis() + 90000;
        while ((KafkaRuntime.adapter() == null || SecondaryKafkaRuntime.adapter() == null)
                && System.currentTimeMillis() < deadline) {
            Utility.getInstance().sleep(100);
        }
        assertNotNull(KafkaRuntime.adapter(), "primary Kafka flow adapter should have started");
        assertNotNull(SecondaryKafkaRuntime.adapter(), "secondary Kafka flow adapter should have started");
    }

    @AfterAll
    static void shutdown() {
        if (SecondaryKafkaRuntime.adapter() != null) {
            SecondaryKafkaRuntime.adapter().close();
        }
        if (SecondaryKafkaRuntime.publisher() != null) {
            SecondaryKafkaRuntime.publisher().close();
        }
        if (KafkaRuntime.adapter() != null) {
            KafkaRuntime.adapter().close();
        }
        if (KafkaRuntime.publisher() != null) {
            KafkaRuntime.publisher().close();
        }
        if (clusterA != null) {
            clusterA.close();
        }
        if (clusterB != null) {
            clusterB.close();
        }
        if (registryB != null) {
            registryB.close();
        }
        System.clearProperty("KAFKA_BOOTSTRAP_SERVERS");
        System.clearProperty("SECONDARY_KAFKA_BOOTSTRAP_SERVERS");
        System.clearProperty("secondary.schema.registry.url");
    }

    private static void createTopics(String bootstrapServers, String... topics) throws Exception {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("client.id", "twin-admin");
        try (AdminClient admin = AdminClient.create(props)) {
            admin.createTopics(Stream.of(topics)
                    .map(t -> new NewTopic(t, 1, (short) 1)).toList()).all().get();
        }
    }

    @Test
    void bridgesFromPrimaryToSecondaryWithTraceAndCidContinuity() throws Exception {
        BridgeSinkB.RECEIVED.clear();
        String cid = "a-to-b-" + Utility.getInstance().getUuid();
        PostOffice po = PostOffice.trackable("unit.test", TRACE_A, "TEST /bridge/a2b");
        po.send(new EventEnvelope().setTo("simple.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, "bridge.source")
                .setHeader(KafkaHeaders.CORRELATION_ID, cid)
                .setBody("{\"hello\":\"from cluster A\"}".getBytes(StandardCharsets.UTF_8))
                .setTraceId(TRACE_A).setTracePath("TEST /bridge/a2b"));

        Map<String, Object> received = BridgeSinkB.RECEIVED.poll(30, TimeUnit.SECONDS);
        assertNotNull(received, "the message should cross both clusters into sink B");
        assertEquals("{\"hello\":\"from cluster A\"}", received.get("body"),
                "body bridged intact from cluster A to cluster B");
        assertEquals(cid, received.get("myCid"),
                "business correlation-id survived both Kafka hops");
        assertEquals(TRACE_A, received.get("traceId"),
                "trace-id stayed continuous across cluster A, the bridge flow, and cluster B");
        ConsumerRecord<String, byte[]> rec = pollOne(clusterB.bootstrapServers(),
                "bridge.mirror", "bridge-mirror-wire-" + Utility.getInstance().getUuid());
        assertNotNull(rec, "the bridged rec should be visible on cluster B");
        assertEquals(cid, headerValue(rec, "X-Secondary-Cid"),
                "the bridge stamps the configured secondary correlation-id header");
        assertEquals(TRACE_A, headerValue(rec, "X-Secondary-Trace"),
                "the bridge stamps the configured secondary trace-id header");
        assertNull(headerValue(rec, "cid"),
                "the bridge flow must not leak the default correlation-id header name");
    }

    @Test
    void bridgesFromSecondaryToPrimaryWithTraceAndCidContinuity() throws Exception {
        BridgeSinkA.RECEIVED.clear();
        String cid = "b-to-a-" + Utility.getInstance().getUuid();
        PostOffice po = PostOffice.trackable("unit.test", TRACE_B, "TEST /bridge/b2a");
        po.send(new EventEnvelope().setTo("secondary.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, "reverse.source")
                .setHeader("X-Secondary-Cid", cid)
                .setBody("{\"hello\":\"from cluster B\"}".getBytes(StandardCharsets.UTF_8))
                .setTraceId(TRACE_B).setTracePath("TEST /bridge/b2a"));

        Map<String, Object> received = BridgeSinkA.RECEIVED.poll(30, TimeUnit.SECONDS);
        assertNotNull(received, "the message should cross both clusters into sink A");
        assertEquals("{\"hello\":\"from cluster B\"}", received.get("body"),
                "body bridged intact from cluster B to cluster A");
        assertEquals(cid, received.get("myCid"),
                "business correlation-id survived both Kafka hops in the reverse direction");
        assertEquals(TRACE_B, received.get("traceId"),
                "trace-id stayed continuous across cluster B, the bridge flow, and cluster A");
    }

    @Test
    void schemaRegistryIsPerClusterAndOptional() throws Exception {
        SchemaSinkB.RECEIVED.clear();
        // the asymmetric topology: no registry on the primary, an independent one on the secondary
        assertNull(KafkaRuntime.schemaCodec(), "primary cluster has no Schema Registry (Apache Kafka style)");
        assertNotNull(SecondaryKafkaRuntime.schemaCodec(), "secondary cluster has its own registry (Confluent style)");
        // subject-driven Confluent serialization against the SECONDARY registry, decoded by the
        // schema-enabled secondary binding back into a Map for the flow
        String subject = "schema.mirror-value";
        SecondaryKafkaRuntime.schemaCodec().client().register(subject, new JsonSchema(JSON_SCHEMA));
        PostOffice po = PostOffice.trackable("unit.test", TRACE_B, "TEST /schema/b");
        po.send(new EventEnvelope().setTo("secondary.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, "schema.mirror")
                .setHeader(KafkaHeaders.SUBJECT, subject)
                .setBody("{\"hello\":\"confluent wire format\"}".getBytes(StandardCharsets.UTF_8))
                .setTraceId(TRACE_B).setTracePath("TEST /schema/b"));

        Map<String, Object> received = SchemaSinkB.RECEIVED.poll(30, TimeUnit.SECONDS);
        assertNotNull(received, "the schema-enabled binding should decode and route the message");
        assertEquals("confluent wire format", received.get("hello"),
                "value round-tripped through the secondary registry's Confluent wire format");
    }

    @Test
    void primaryRejectsSubjectPublishWithoutRegistry() throws Exception {
        // asymmetry enforced with a clear diagnostic: the PRIMARY has no registry, so a subject-driven
        // publish through simple.kafka.notification must fail fast naming the primary's config key
        PostOffice po = PostOffice.trackable("unit.test", TRACE_A, "TEST /schema/a");
        EventEnvelope request = new EventEnvelope().setTo("simple.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, "bridge.source")
                .setHeader(KafkaHeaders.SUBJECT, "no-registry-value")
                .setBody("{\"hello\":\"x\"}".getBytes(StandardCharsets.UTF_8));
        EventEnvelope response = po.request(request, 10000).get();
        assertNotNull(response);
        assertTrue(String.valueOf(response.getError()).contains("schema.registry.url"),
                "the failure names the primary registry key, got: " + response.getError());
    }

    /**
     * The secondary notification must stamp the SECONDARY header-name overrides
     * (secondary.kafka.correlation.id.header / secondary.kafka.trace.id.header from the test
     * application.properties) on cluster-B records - observed with a raw consumer on the wire.
     */
    @Test
    void secondaryOutboundHeaderOverrides() {
        String cid = "override-" + Utility.getInstance().getUuid();
        PostOffice po = PostOffice.trackable("unit.test", TRACE_B, "TEST /secondary/headers");
        po.send(new EventEnvelope().setTo("secondary.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, "header.probe")
                .setHeader("X-Secondary-Cid", cid)
                .setBody("{\"hello\":\"header probe\"}".getBytes(StandardCharsets.UTF_8))
                .setTraceId(TRACE_B).setTracePath("TEST /secondary/headers"));
        ConsumerRecord<String, byte[]> rec = pollOne(clusterB.bootstrapServers(),
                "header.probe", "header-probe-group");
        assertNotNull(rec, "the probe rec should arrive on cluster B");
        assertEquals(cid, headerValue(rec, "X-Secondary-Cid"),
                "business correlation-id stamped under the SECONDARY override name");
        assertEquals(TRACE_B, headerValue(rec, "X-Secondary-Trace"),
                "trace-id stamped under the SECONDARY override name");
        assertTrue(String.valueOf(headerValue(rec, "traceparent")).contains(TRACE_B),
                "W3C traceparent is stamped alongside the legacy trace-id header");
        assertNull(headerValue(rec, "cid"),
                "the global default name is not used when the secondary override is set");
    }

    /**
     * Dead letters from a SECONDARY binding must land on the SECONDARY cluster: the poison flow
     * always fails, and after exhausted retries the record appears on the binding's dlq-topic on
     * cluster B (RetryPolicy carries the secondary publisher).
     */
    @Test
    void secondaryDeadLettersLandOnSecondaryCluster() {
        PostOffice po = PostOffice.trackable("unit.test", TRACE_B, "TEST /secondary/dlq");
        po.send(new EventEnvelope().setTo("secondary.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, "poison.source")
                .setBody("{\"hello\":\"boom\"}".getBytes(StandardCharsets.UTF_8))
                .setTraceId(TRACE_B).setTracePath("TEST /secondary/dlq"));
        // 3 retries at 500ms backoff, then the dead-letter write to the secondary cluster
        ConsumerRecord<String, byte[]> rec = pollOne(clusterB.bootstrapServers(),
                "poison.dlq", "dlq-probe-group");
        assertNotNull(rec, "the exhausted rec should land on the secondary cluster's DLQ topic");
        assertEquals("{\"hello\":\"boom\"}", new String(rec.value(), StandardCharsets.UTF_8),
                "the dead letter carries the original payload");
    }

    /** Raw wire-level consumer: poll one record from a topic (up to 60s). */
    private static ConsumerRecord<String, byte[]> pollOne(String bootstrapServers, String topic,
                                                          String groupId) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", groupId);
        props.put("auto.offset.reset", "earliest");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + 60000;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofSeconds(2));
                var it = records.iterator();
                if (it.hasNext()) {
                    return it.next();
                }
            }
        }
        return null;
    }

    /** Read a rec header as UTF-8 text (null when absent). */
    private static String headerValue(ConsumerRecord<String, byte[]> rec, String name) {
        var header = rec.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
