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

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ManagedCache;
import org.platformlambda.mini.kafka.schema.SchemaCodec;
import org.platformlambda.mini.kafka.schema.SchemaType;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the {@link SchemaCodec} (and its owner-confined {@link SchemaCodec.Encoder}/
 * {@link SchemaCodec.Decoder}) against the real Confluent JSON/Avro/Protobuf serdes and a self-contained,
 * in-JVM {@link EmbeddedSchemaRegistry} (no Kafka broker, no AutoStart, no network beyond loopback):
 * id-driven serialize, magic-id dispatch + decode to a Map, in-memory schema cache, and - critically -
 * decoding messages produced by stock Confluent serializers (interop with client projects).
 */
class SchemaCodecTest {

    private static final String TOPIC = "orders";
    private static final String JSON_SCHEMA =
            "{\"type\":\"object\",\"properties\":{\"hello\":{\"type\":\"string\"}},\"additionalProperties\":true}";
    private static final String AVRO_SCHEMA =
            "{\"type\":\"record\",\"name\":\"Greeting\",\"namespace\":\"test\","
            + "\"fields\":[{\"name\":\"hello\",\"type\":\"string\"}]}";
    private static final String PROTO_SCHEMA =
            "syntax = \"proto3\"; package test; message Greeting { string hello = 1; }";

    private static EmbeddedSchemaRegistry registry;
    private static SchemaCodec codec;
    private static SchemaCodec.Encoder encoder;
    private static SchemaCodec.Decoder decoder;

    @BeforeAll
    static void setup() throws Exception {
        registry = new EmbeddedSchemaRegistry();
        AppConfigReader config = AppConfigReader.getInstance();
        codec = SchemaCodec.fromConfig(config, registry.baseUrl());
        encoder = codec.newEncoder();
        decoder = codec.newDecoder();
    }

    @AfterAll
    static void teardown() {
        if (registry != null) {
            registry.close();
        }
    }

    private static boolean schemaCached(int id) {
        ManagedCache cache = ManagedCache.getInstance(SchemaCodec.CACHE_NAME);
        return cache != null && cache.exists(Integer.toString(id));
    }

    @Test
    void serializeByIdThenDecodeRoundTrips() throws Exception {
        int id = codec.client().register(TOPIC + "-value", new JsonSchema(JSON_SCHEMA));

        byte[] framed = encoder.serialize(TOPIC, SchemaType.JSON, id, Map.of("hello", "world"));
        assertTrue(SchemaCodec.isFramed(framed), "output is Confluent-framed (magic byte + id)");
        assertEquals(id, SchemaCodec.schemaId(framed), "the framed id matches the pre-registered schema");

        Object decoded = decoder.decode(TOPIC, framed);
        assertInstanceOf(Map.class, decoded);
        assertEquals("world", ((Map<?, ?>) decoded).get("hello"));

        assertTrue(schemaCached(id), "schema cached in memory by id");
    }

    @Test
    void decodesMessageFromStockConfluentSerializer() {
        // External-client stand-in: a stock KafkaJsonSchemaSerializer that auto-registers from the value.
        CachedSchemaRegistryClient srClient = new CachedSchemaRegistryClient(List.of(registry.baseUrl()),
                100, List.of(new JsonSchemaProvider()), Map.of());
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registry.baseUrl());
        cfg.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);
        try (KafkaJsonSchemaSerializer<Object> serializer = new KafkaJsonSchemaSerializer<>(srClient, cfg)) {
            byte[] framed = serializer.serialize(TOPIC, Map.of("hello", "external"));
            Object decoded = decoder.decode(TOPIC, framed);
            assertInstanceOf(Map.class, decoded);
            assertEquals("external", ((Map<?, ?>) decoded).get("hello"),
                    "minimalist-kafka decodes messages produced by a stock Confluent serializer");
        }
    }

    @Test
    void serializeAvroByIdThenDecodeRoundTrips() throws Exception {
        int id = codec.client().register(TOPIC + "-avro-value", new AvroSchema(AVRO_SCHEMA));

        byte[] framed = encoder.serialize(TOPIC, SchemaType.AVRO, id, Map.of("hello", "avro"));
        assertTrue(SchemaCodec.isFramed(framed), "output is Confluent-framed (magic byte + id)");
        assertEquals(id, SchemaCodec.schemaId(framed), "the framed id matches the pre-registered schema");

        Object decoded = decoder.decode(TOPIC, framed);
        assertInstanceOf(Map.class, decoded);
        assertEquals("avro", ((Map<?, ?>) decoded).get("hello"));

        assertTrue(schemaCached(id), "schema cached in memory by id");
    }

    @Test
    void decodesMessageFromStockConfluentAvroSerializer() {
        // External-client stand-in: a stock KafkaAvroSerializer that auto-registers from the GenericRecord.
        CachedSchemaRegistryClient srClient = new CachedSchemaRegistryClient(List.of(registry.baseUrl()),
                100, List.of(new AvroSchemaProvider()), Map.of());
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registry.baseUrl());
        cfg.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);
        GenericRecord greeting = new GenericData.Record(new Schema.Parser().parse(AVRO_SCHEMA));
        greeting.put("hello", "external-avro");
        try (KafkaAvroSerializer serializer = new KafkaAvroSerializer(srClient, cfg)) {
            byte[] framed = serializer.serialize(TOPIC, greeting);
            Object decoded = decoder.decode(TOPIC, framed);
            assertInstanceOf(Map.class, decoded);
            assertEquals("external-avro", ((Map<?, ?>) decoded).get("hello"),
                    "minimalist-kafka decodes Avro messages produced by a stock Confluent serializer");
        }
    }

    @Test
    void serializeProtobufByIdThenDecodeRoundTrips() throws Exception {
        int id = codec.client().register(TOPIC + "-proto-value", new ProtobufSchema(PROTO_SCHEMA));

        byte[] framed = encoder.serialize(TOPIC, SchemaType.PROTOBUF, id, Map.of("hello", "protobuf"));
        assertTrue(SchemaCodec.isFramed(framed), "output is Confluent-framed (magic byte + id)");
        assertEquals(id, SchemaCodec.schemaId(framed), "the framed id matches the pre-registered schema");

        Object decoded = decoder.decode(TOPIC, framed);
        assertInstanceOf(Map.class, decoded);
        assertEquals("protobuf", ((Map<?, ?>) decoded).get("hello"));

        assertTrue(schemaCached(id), "schema cached in memory by id");
    }

    @Test
    void decodesMessageFromStockConfluentProtobufSerializer() {
        // External-client stand-in: a stock KafkaProtobufSerializer that auto-registers from the message.
        CachedSchemaRegistryClient srClient = new CachedSchemaRegistryClient(List.of(registry.baseUrl()),
                100, List.of(new ProtobufSchemaProvider()), Map.of());
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registry.baseUrl());
        cfg.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);
        ProtobufSchema schema = new ProtobufSchema(PROTO_SCHEMA);
        DynamicMessage message = DynamicMessage.newBuilder(schema.toDescriptor())
                .setField(schema.toDescriptor().findFieldByName("hello"), "external-proto").build();
        try (KafkaProtobufSerializer<Message> serializer = new KafkaProtobufSerializer<>(srClient, cfg)) {
            byte[] framed = serializer.serialize(TOPIC, message);
            Object decoded = decoder.decode(TOPIC, framed);
            assertInstanceOf(Map.class, decoded);
            assertEquals("external-proto", ((Map<?, ?>) decoded).get("hello"),
                    "minimalist-kafka decodes Protobuf messages produced by a stock Confluent serializer");
        }
    }

    @Test
    void rejectsUnframedPayload() {
        byte[] unframed = "{\"hello\":\"x\"}".getBytes();
        assertThrows(IllegalArgumentException.class, () -> decoder.decode(TOPIC, unframed));
    }

    @Test
    void notFoundSchemaIdIsNotCached() {
        // Positive results only: a not-found id must throw and must NOT be remembered, so a schema
        // registered moments later is visible without waiting for a TTL to elapse or a pod restart.
        int unknownId = 987654;
        assertThrows(RestClientException.class, () -> codec.client().getSchemaById(unknownId));
        assertFalse(schemaCached(unknownId), "a not-found schema id is never cached");
    }

    @Test
    void encoderRecoversAfterSchemaAppears() throws Exception {
        // Reproduce the reported scenario at the producer level: encoding with a schema-id that is not
        // registered yet must fail WITHOUT poisoning the encoder, so once the id appears on the registry
        // (e.g. its <id>.json dropped into the registry) the same encoder succeeds - the miss was never cached.
        int existing = codec.client().register("recover-a-value", new JsonSchema(
                "{\"type\":\"object\",\"properties\":{\"recoverA\":{\"type\":\"string\"}},\"additionalProperties\":true}"));
        int futureId = existing + 1;   // EmbeddedSchemaRegistry assigns ids sequentially -> not present yet
        assertThrows(RuntimeException.class,
                () -> encoder.serialize(TOPIC, SchemaType.JSON, futureId, Map.of("hello", "x")),
                "encoding against an unregistered id fails");
        assertFalse(schemaCached(futureId), "the failed lookup is not cached");

        int assigned = codec.client().register("recover-b-value", new JsonSchema(
                "{\"type\":\"object\",\"properties\":{\"recoverB\":{\"type\":\"string\"}},\"additionalProperties\":true}"));
        assertEquals(futureId, assigned, "the next registration takes the id we probed");

        byte[] framed = encoder.serialize(TOPIC, SchemaType.JSON, futureId, Map.of("hello", "recovered"));
        assertEquals(futureId, SchemaCodec.schemaId(framed), "the same encoder now succeeds");
        assertEquals("recovered", ((Map<?, ?>) decoder.decode(TOPIC, framed)).get("hello"));
    }

    @Test
    void disabledWhenNoRegistryUrl() {
        assertNull(SchemaCodec.fromConfig(AppConfigReader.getInstance(), "  "),
                "blank registry url ⇒ schema features off");
    }

    @Test
    void serializeDecodeRestoreThreadContextClassLoader() throws Exception {
        // Guards the @KernelThreadRunner classloader fix: serde build/use pins the Confluent serde classloader
        // as the thread context classloader, then must restore the caller's - never leak SERDE_CLASSLOADER.
        int id = codec.client().register(TOPIC + "-tccl-value", new JsonSchema(JSON_SCHEMA));
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        ClassLoader sentinel = new URLClassLoader(new URL[0], original);
        try {
            thread.setContextClassLoader(sentinel);
            byte[] framed = encoder.serialize(TOPIC, SchemaType.JSON, id, Map.of("hello", "tccl"));
            assertEquals("tccl", ((Map<?, ?>) decoder.decode(TOPIC, framed)).get("hello"));
            assertSame(sentinel, thread.getContextClassLoader(),
                    "serialize/decode restore the caller's thread context classloader");
        } finally {
            thread.setContextClassLoader(original);
        }
    }
}
