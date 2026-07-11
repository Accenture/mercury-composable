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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

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
        createTopics(clusterB.bootstrapServers(), "bridge.mirror", "reverse.source", "schema.mirror");
        // registry on the SECONDARY side only (asymmetric topology)
        registryB = new EmbeddedSchemaRegistry();
        System.setProperty("SECONDARY_SCHEMA_REGISTRY_URL", registryB.baseUrl());
        // point the producer/consumer templates at the two embedded brokers
        System.setProperty("KAFKA_BOOTSTRAP_SERVERS", clusterA.bootstrapServers());
        System.setProperty("SECONDARY_KAFKA_BOOTSTRAP_SERVERS", clusterB.bootstrapServers());
        AutoStart.main(new String[0]);
        // both @MainApplication entry points must complete: primary + secondary adapters ready
        long deadline = System.currentTimeMillis() + 20000;
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
        System.clearProperty("SECONDARY_SCHEMA_REGISTRY_URL");
    }

    private static void createTopics(String bootstrapServers, String... topics) throws Exception {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("client.id", "twin-admin");
        try (AdminClient admin = AdminClient.create(props)) {
            admin.createTopics(List.of(topics).stream()
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
                "business correlation-id survived both Kafka hops (model.cid -> header.cid on the bridge)");
        assertEquals(TRACE_A, received.get("traceId"),
                "trace-id stayed continuous across cluster A, the bridge flow, and cluster B");
    }

    @Test
    void bridgesFromSecondaryToPrimaryWithTraceAndCidContinuity() throws Exception {
        BridgeSinkA.RECEIVED.clear();
        String cid = "b-to-a-" + Utility.getInstance().getUuid();
        PostOffice po = PostOffice.trackable("unit.test", TRACE_B, "TEST /bridge/b2a");
        po.send(new EventEnvelope().setTo("secondary.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, "reverse.source")
                .setHeader(KafkaHeaders.CORRELATION_ID, cid)
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
}
