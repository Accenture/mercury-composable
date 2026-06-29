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
import org.platformlambda.core.util.Utility;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
class KafkaFlowAdapterTest {

    private static final String TOPIC = "mini-test-topic";
    private static final String SCHEMA_TOPIC = "schema-test-topic";
    private static final String AVRO_TOPIC = "avro-test-topic";
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
        KafkaTestSupport.createTopic(kafka.bootstrapServers(), SCHEMA_TOPIC);
        KafkaTestSupport.createTopic(kafka.bootstrapServers(), AVRO_TOPIC);
        // self-contained, in-JVM Confluent-compatible registry on a random port. Set the exact config key
        // as a system property (ConfigReader consults system properties before the file, at get-time) so it
        // wins regardless of when AppConfigReader was first loaded by other tests. Start the cache clean.
        registry = new EmbeddedSchemaRegistry();
        Utility.getInstance().cleanupDir(new java.io.File("/tmp/schema-registry-cache-test"));
        System.setProperty("schema.registry.url", registry.baseUrl());
        // resolved by ${KAFKA_BOOTSTRAP_SERVERS:...} in the kafka-producer/consumer.properties templates
        System.setProperty("KAFKA_BOOTSTRAP_SERVERS", kafka.bootstrapServers());
        AutoStart.main(new String[0]);
        // KafkaFlowAutoStart (@MainApplication) runs after the engine is up: it sets the publisher and
        // starts the adapter. A non-null adapter is our readiness signal.
        long deadline = System.currentTimeMillis() + 20000;
        while (KafkaRuntime.adapter() == null && System.currentTimeMillis() < deadline) {
            TimeUnit.MILLISECONDS.sleep(100);
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
        Utility.getInstance().cleanupDir(new java.io.File("/tmp/schema-registry-cache-test"));   // don't leave it behind
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
        assertEquals("{\"hello\":\"kafka\"}", received.get("body"), "body round-tripped through Kafka");
        assertEquals(TRACE_ID, received.get("traceId"), "trace-id stayed continuous across the Kafka hop");
    }

    @Test
    @SuppressWarnings("unchecked")
    void schemaFramedMessageDecodedIntoFlow() throws Exception {
        SchemaSinkTask.RECEIVED.clear();
        // pre-register the JSON schema (governed artifact); the producer publishes by its global id
        int schemaId = KafkaRuntime.schemaCodec().client().register(SCHEMA_TOPIC + "-value", new JsonSchema(JSON_SCHEMA));
        String cid = Utility.getInstance().getUuid();

        // publish via simple.kafka.notification with schema-id: the body is serialized into the Confluent
        // wire format; the schema-enabled adapter binding decodes it back to a Map for the flow.
        PostOffice po = PostOffice.trackable("unit.test", TRACE_ID, "TEST /schema");
        po.send(new EventEnvelope().setTo("simple.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, SCHEMA_TOPIC)
                .setHeader(KafkaHeaders.CORRELATION_ID, cid)
                .setHeader(KafkaHeaders.SCHEMA_ID, String.valueOf(schemaId))
                .setHeader(KafkaHeaders.SCHEMA_TYPE, "JSON")
                .setBody("{\"hello\":\"schema\"}".getBytes(StandardCharsets.UTF_8))
                .setTraceId(TRACE_ID).setTracePath("TEST /schema"));

        Map<String, Object> received = SchemaSinkTask.RECEIVED.poll(25, TimeUnit.SECONDS);
        assertNotNull(received, "the schema sink flow should receive the decoded message");
        assertEquals(cid, received.get("cid"), "correlation-id propagated as a Kafka header");
        assertInstanceOf(Map.class, received.get("body"), "body decoded to a Map (not raw byte[])");
        assertEquals("schema", ((Map<String, Object>) received.get("body")).get("hello"),
                "JSON Schema message round-tripped: produced by id, decoded by the adapter");
        assertEquals(TRACE_ID, received.get("traceId"), "trace-id stayed continuous across the Kafka hop");
    }

    @Test
    @SuppressWarnings("unchecked")
    void avroFramedMessageDecodedIntoFlow() throws Exception {
        SchemaSinkTask.RECEIVED.clear();
        // pre-register the Avro schema (governed artifact); the producer publishes by its global id
        int schemaId = KafkaRuntime.schemaCodec().client().register(AVRO_TOPIC + "-value", new AvroSchema(AVRO_SCHEMA));
        String cid = Utility.getInstance().getUuid();

        // publish via simple.kafka.notification with schema-type=AVRO: the body is serialized into the
        // Confluent Avro wire format; the schema-enabled adapter binding decodes it back to a Map.
        PostOffice po = PostOffice.trackable("unit.test", TRACE_ID, "TEST /avro");
        po.send(new EventEnvelope().setTo("simple.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, AVRO_TOPIC)
                .setHeader(KafkaHeaders.CORRELATION_ID, cid)
                .setHeader(KafkaHeaders.SCHEMA_ID, String.valueOf(schemaId))
                .setHeader(KafkaHeaders.SCHEMA_TYPE, "AVRO")
                .setBody("{\"hello\":\"avro\"}".getBytes(StandardCharsets.UTF_8))
                .setTraceId(TRACE_ID).setTracePath("TEST /avro"));

        Map<String, Object> received = SchemaSinkTask.RECEIVED.poll(25, TimeUnit.SECONDS);
        assertNotNull(received, "the schema sink flow should receive the decoded Avro message");
        assertEquals(cid, received.get("cid"), "correlation-id propagated as a Kafka header");
        assertInstanceOf(Map.class, received.get("body"), "body decoded to a Map (not raw byte[])");
        assertEquals("avro", ((Map<String, Object>) received.get("body")).get("hello"),
                "Avro message round-tripped: produced by id, decoded by the adapter");
        assertEquals(TRACE_ID, received.get("traceId"), "trace-id stayed continuous across the Kafka hop");
    }
}
