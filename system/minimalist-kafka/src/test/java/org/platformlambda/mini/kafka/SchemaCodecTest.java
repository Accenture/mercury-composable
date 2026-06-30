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
import org.platformlambda.core.util.Utility;
import org.platformlambda.mini.kafka.schema.SchemaCodec;
import org.platformlambda.mini.kafka.schema.SchemaType;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the {@link SchemaCodec} (and its owner-confined {@link SchemaCodec.Encoder}/
 * {@link SchemaCodec.Decoder}) against the real Confluent JSON/Avro/Protobuf serdes and a self-contained,
 * in-JVM {@link EmbeddedSchemaRegistry} (no Kafka broker, no AutoStart, no network beyond loopback):
 * id-driven serialize, magic-id dispatch + decode to a Map, on-disk schema cache, and - critically -
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
    private static File cacheDir;

    @BeforeAll
    static void setup() throws Exception {
        registry = new EmbeddedSchemaRegistry();
        AppConfigReader config = AppConfigReader.getInstance();
        cacheDir = new File(config.getProperty("schema.registry.cache.dir", "/tmp/schema-registry-cache-test"));
        Utility.getInstance().cleanupDir(cacheDir);   // transient /tmp cache: start clean
        codec = SchemaCodec.fromConfig(config, registry.baseUrl());
        encoder = codec.newEncoder();
        decoder = codec.newDecoder();
    }

    @AfterAll
    static void teardown() {
        if (registry != null) {
            registry.close();
        }
        Utility.getInstance().cleanupDir(cacheDir);   // don't leave the transient schema cache behind
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

        assertTrue(new File(cacheDir, id + ".json").exists(), "schema cached to disk by id");
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

        assertTrue(new File(cacheDir, id + ".json").exists(), "schema cached to disk by id");
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

        assertTrue(new File(cacheDir, id + ".json").exists(), "schema cached to disk by id");
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
    void disabledWhenNoRegistryUrl() {
        assertNull(SchemaCodec.fromConfig(AppConfigReader.getInstance(), "  "),
                "blank registry url ⇒ schema features off");
    }
}
