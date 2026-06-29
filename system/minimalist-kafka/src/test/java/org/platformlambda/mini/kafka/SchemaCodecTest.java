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

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the {@link SchemaCodec} against the real Confluent JSON Schema serdes and a self-contained,
 * in-JVM {@link EmbeddedSchemaRegistry} (no Kafka broker, no AutoStart, no network beyond loopback):
 * id-driven serialize, magic-id dispatch + decode to a Map, on-disk schema cache, and - critically -
 * decoding messages produced by a stock {@code KafkaJsonSchemaSerializer} (interop with client projects).
 */
class SchemaCodecTest {

    private static final String TOPIC = "orders";
    private static final String JSON_SCHEMA =
            "{\"type\":\"object\",\"properties\":{\"hello\":{\"type\":\"string\"}},\"additionalProperties\":true}";

    private static EmbeddedSchemaRegistry registry;
    private static SchemaCodec codec;
    private static File cacheDir;

    @BeforeAll
    static void setup() throws Exception {
        registry = new EmbeddedSchemaRegistry();
        AppConfigReader config = AppConfigReader.getInstance();
        cacheDir = new File(config.getProperty("schema.registry.cache.dir", "/tmp/schema-registry-cache-test"));
        Utility.getInstance().cleanupDir(cacheDir);   // transient /tmp cache: start clean
        codec = SchemaCodec.fromConfig(config, registry.baseUrl());
    }

    @AfterAll
    static void teardown() {
        if (registry != null) {
            registry.close();
        }
    }

    @Test
    void serializeByIdThenDecodeRoundTrips() throws Exception {
        int id = codec.client().register(TOPIC + "-value", new JsonSchema(JSON_SCHEMA));

        byte[] framed = codec.serialize(TOPIC, SchemaType.JSON, id, Map.of("hello", "world"));
        assertTrue(SchemaCodec.isFramed(framed), "output is Confluent-framed (magic byte + id)");
        assertEquals(id, SchemaCodec.schemaId(framed), "the framed id matches the pre-registered schema");

        Object decoded = codec.decode(TOPIC, framed);
        assertInstanceOf(Map.class, decoded);
        assertEquals("world", ((Map<?, ?>) decoded).get("hello"));

        assertTrue(new File(cacheDir, id + ".json").exists(), "schema cached to disk by id");
    }

    @Test
    void decodesMessageFromStockConfluentSerializer() throws Exception {
        // External-client stand-in: a stock KafkaJsonSchemaSerializer that auto-registers from the value.
        CachedSchemaRegistryClient srClient = new CachedSchemaRegistryClient(List.of(registry.baseUrl()),
                100, List.of(new JsonSchemaProvider()), Map.of());
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registry.baseUrl());
        cfg.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);
        try (KafkaJsonSchemaSerializer<Object> serializer = new KafkaJsonSchemaSerializer<>(srClient, cfg)) {
            byte[] framed = serializer.serialize(TOPIC, Map.of("hello", "external"));
            Object decoded = codec.decode(TOPIC, framed);
            assertInstanceOf(Map.class, decoded);
            assertEquals("external", ((Map<?, ?>) decoded).get("hello"),
                    "minimalist-kafka decodes messages produced by a stock Confluent serializer");
        }
    }

    @Test
    void rejectsUnframedPayload() {
        assertThrows(IllegalArgumentException.class, () -> codec.decode(TOPIC, "{\"hello\":\"x\"}".getBytes()));
    }

    @Test
    void rejectsUnsupportedSchemaTypeOnSerialize() {
        assertThrows(UnsupportedOperationException.class,
                () -> codec.serialize(TOPIC, SchemaType.AVRO, 1, Map.of("a", "b")));
    }

    @Test
    void disabledWhenNoRegistryUrl() {
        assertNull(SchemaCodec.fromConfig(AppConfigReader.getInstance(), "  "),
                "blank registry url ⇒ schema features off");
    }
}
