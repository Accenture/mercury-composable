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
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ManagedCache;
import org.platformlambda.mini.kafka.schema.ResolvedSchema;
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
 * {@link SchemaCodec.Decoder}) against the real Confluent JSON/Avro serdes and a self-contained,
 * in-JVM {@link EmbeddedSchemaRegistry} (no Kafka broker, no AutoStart, no network beyond loopback):
 * id-driven serialize, magic-id dispatch + decode to a Map, in-memory schema cache, and - critically -
 * decoding messages produced by stock Confluent serializers (interop with client projects). Also guards
 * that {@link SchemaType#PROTOBUF} - recognized but deliberately unwired, see its Javadoc - fails clearly.
 */
class SchemaCodecTest {

    private static final String TOPIC = "orders";
    private static final String JSON_SCHEMA =
            "{\"type\":\"object\",\"properties\":{\"hello\":{\"type\":\"string\"}},\"additionalProperties\":true}";
    private static final String AVRO_SCHEMA =
            "{\"type\":\"record\",\"name\":\"Greeting\",\"namespace\":\"test\","
            + "\"fields\":[{\"name\":\"hello\",\"type\":\"string\"}]}";

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
    void resolveSubjectVersionToId() throws Exception {
        String subject = "resolve-demo-value";
        int id = codec.client().register(subject, new JsonSchema(JSON_SCHEMA));

        ResolvedSchema byLatest = codec.resolve(subject, "latest");
        assertEquals(id, byLatest.id(), "latest resolves to the registered global id");
        assertEquals(SchemaType.JSON, byLatest.type(), "type derived authoritatively from the parsed schema");

        assertEquals(id, codec.resolve(subject, null).id(), "null/blank version means latest");
        assertEquals(id, codec.resolve(subject, "1").id(), "pinned version 1 resolves to the same id");

        // resolution is cached: numeric in the (long-TTL) version cache, latest in the (short-TTL) id cache
        assertTrue(ManagedCache.getInstance(SchemaCodec.VERSION_CACHE_NAME).exists(subject + "/1"),
                "numeric version resolution cached");
        assertTrue(ManagedCache.getInstance(SchemaCodec.CACHE_NAME).exists("latest/" + subject),
                "latest resolution cached in the id cache under a namespaced key");
    }

    @Test
    void resolveRejectsBadVersionAndUnknownSubject() throws Exception {
        String subject = "resolve-errors-value";
        codec.client().register(subject, new JsonSchema(JSON_SCHEMA));
        // a non-numeric, non-"latest" version is rejected up front
        assertThrows(IllegalArgumentException.class, () -> codec.resolve(subject, "v2"),
                "version must be 'latest' or a positive integer");
        // an unknown subject cannot be resolved
        assertThrows(IllegalStateException.class, () -> codec.resolve("no-such-subject", "latest"),
                "unknown subject fails to resolve");
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
    void protobufIsRecognizedButNotSupported() throws Exception {
        // PROTOBUF is a real SchemaType (parses fine, dispatch reaches the serde lookup) but has no
        // registered serde - see SchemaCodec's class Javadoc and SchemaType.PROTOBUF's Javadoc for why
        // (Confluent's kafka-protobuf-provider depends on the unpatched, discontinued wire-runtime-jvm).
        // It must fail clearly (UnsupportedOperationException), never silently or with an NPE.
        int id = codec.client().register(TOPIC + "-proto-value", new JsonSchema(JSON_SCHEMA));
        var protoPayload = Map.of("hello", "protobuf");
        assertThrows(UnsupportedOperationException.class,
                () -> encoder.serialize(TOPIC, SchemaType.PROTOBUF, id, protoPayload),
                "Protobuf is recognized but deliberately unwired - must fail clearly, not silently");
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
        var missPayload = Map.of("hello", "x");
        assertThrows(RuntimeException.class,
                () -> encoder.serialize(TOPIC, SchemaType.JSON, futureId, missPayload),
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
        try (URLClassLoader sentinel = new URLClassLoader(new URL[0], original)) {
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
