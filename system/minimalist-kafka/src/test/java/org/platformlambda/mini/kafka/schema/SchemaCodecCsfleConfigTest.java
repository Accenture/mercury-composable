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

package org.platformlambda.mini.kafka.schema;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientFactory;
import io.confluent.kafka.schemaregistry.client.rest.entities.Rule;
import io.confluent.kafka.schemaregistry.client.rest.entities.RuleKind;
import io.confluent.kafka.schemaregistry.client.rest.entities.RuleMode;
import io.confluent.kafka.schemaregistry.client.rest.entities.RuleSet;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.util.common.ConfigBase;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the {@code schema.registry.serde.*} config pass-through (design spec §5.3/§5.4, task #14) - both
 * the extraction from application config and its use by the real {@link SchemaCodec}/{@link JsonSchemaSerde}
 * produce/consume path, not just the raw Confluent API used by the standalone verification spike
 * ({@code CsfleLocalRoundTripTest}).
 *
 * <p>{@link SchemaCodec#fromConfig} always builds a {@link ManagedCacheSchemaRegistryClient} (a real
 * {@code CachedSchemaRegistryClient}, which opens an actual {@code RestService} - it does not honor Confluent's
 * test-only {@code mock://} scope convention, that detection lives in {@code SchemaRegistryClientFactory} /
 * {@code MockSchemaRegistry} instead). So the encrypt/decrypt round-trip here builds a {@code SchemaCodec} via
 * its package-private constructor over a {@link SchemaRegistryClientFactory}-built {@code MockSchemaRegistryClient}
 * (the mock:// scope), exercising the real {@code SchemaCodec.Encoder}/{@code Decoder}/{@code JsonSchemaSerde}
 * production classes - exactly the part task #14 changed - without needing a live registry or touching
 * {@code EmbeddedSchemaRegistry}.</p>
 */
class SchemaCodecCsfleConfigTest {

    private static final String TOPIC = "csfle-codec-topic";
    private static final String TAGGED_FIELD = "ssn";
    private static final String PII_TAG = "PII";
    private static final Utility util = Utility.getInstance();

    /** Minimal, fully-controlled {@link ConfigBase} backed by a flat key-value map (no dotted-key flattening). */
    private static final class MapConfig implements ConfigBase {
        private final Map<String, Object> map;
        MapConfig(Map<String, Object> map) { this.map = map; }
        @Override public Object get(String key) { return map.get(key); }
        @Override public Object get(String key, Object defaultValue, String... loop) { return map.getOrDefault(key, defaultValue); }
        @Override public String getProperty(String key) { Object v = map.get(key); return v == null ? null : String.valueOf(v); }
        @Override public String getProperty(String key, String defaultValue) { String v = getProperty(key); return v == null ? defaultValue : v; }
        @Override public boolean exists(String key) { return map.containsKey(key); }
        @Override public boolean isEmpty() { return map.isEmpty(); }
        @Override public boolean isBaseConfig() { return false; }
        @Override public Map<String, Object> getMap() { return map; }
        @Override public Map<String, Object> getCompositeKeyValues() { return map; }
    }

    @Test
    void extractSerdeConfigStripsPrefixAndIgnoresOtherKeys() {
        Map<String, Object> appConfig = new HashMap<>();
        appConfig.put("schema.registry.serde.encrypt.kek.name", "my-kek");
        appConfig.put("schema.registry.serde.secret", "my-secret");
        appConfig.put("schema.registry.cache.ttl", "30m");   // not a serde.* key - must be excluded
        appConfig.put("schema.registry.url", "http://localhost:8081"); // likewise excluded

        Map<String, Object> extracted = SchemaCodec.extractSerdeConfig(new MapConfig(appConfig));

        assertEquals(Map.of("encrypt.kek.name", "my-kek", "secret", "my-secret"), extracted);
    }

    @Test
    void extractSerdeConfigIsEmptyWhenNoneConfigured() {
        Map<String, Object> extracted = SchemaCodec.extractSerdeConfig(new MapConfig(new HashMap<>()));
        assertTrue(extracted.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void csfleConfigReachesEncoderAndDecoder() throws Exception {
        String mockUrl = "mock://" + util.getUuid();

        // extraSerdeConfig is for genuinely global, driver-level settings only (here, the local KMS driver's
        // "secret"). encrypt.kek.name/kms.type/kms.key.id are NOT set here: RuleContext.getParameter resolves
        // those strictly from the registered rule's own params (or the schema's Metadata) - never from this
        // executor-level config map - which is exactly what lets different subjects use different KEKs/KMS
        // vendors with zero code or config change here. See the Rule built below.
        Map<String, Object> extraSerdeConfig = new HashMap<>();
        extraSerdeConfig.put("secret", "spike-test-passphrase");

        SchemaRegistryClient mockClient = SchemaRegistryClientFactory.newClient(
                List.of(mockUrl), 100, List.of(new JsonSchemaProvider()), Map.of(), Map.of());
        SchemaCodec codec = new SchemaCodec(mockClient, mockUrl, extraSerdeConfig);

        String schemaString = "{\"type\":\"object\",\"properties\":{"
                + "\"hello\":{\"type\":\"string\"},"
                + "\"" + TAGGED_FIELD + "\":{\"type\":\"string\",\"confluent:tags\":[\"" + PII_TAG + "\"]}"
                + "},\"additionalProperties\":true}";
        Map<String, String> ruleParams = new HashMap<>();
        ruleParams.put("encrypt.kek.name", "csfle-codec-kek");
        ruleParams.put("encrypt.kms.type", "local-kms");
        ruleParams.put("encrypt.kms.key.id", "local-kms://csfle-codec-key");
        Rule encryptRule = new Rule("encryptPII", null, RuleKind.TRANSFORM, RuleMode.WRITEREAD,
                "ENCRYPT", Set.of(PII_TAG), ruleParams, null, null, "ERROR", false);
        JsonSchema ruledSchema = new JsonSchema(schemaString).copy(null, new RuleSet(List.of(), List.of(encryptRule)));
        int id = mockClient.register(TOPIC + "-value", ruledSchema);

        // SchemaCodec.Encoder.serialize -> JsonSchemaSerde.serialize, the real production path: it envelopes
        // with the registered (ruled) schema by id and merges extraSerdeConfig into the serializer's config.
        SchemaCodec.Encoder encoder = codec.newEncoder();
        Map<String, Object> value = Map.of("hello", "world", TAGGED_FIELD, "123-45-6789");
        byte[] framed = encoder.serialize(TOPIC, SchemaType.JSON, id, value);

        String wireText = new String(framed, StandardCharsets.ISO_8859_1);
        assertFalse(wireText.contains("123-45-6789"), "tagged field must be encrypted on the wire via SchemaCodec");
        assertTrue(wireText.contains("world"), "untagged field stays plaintext");

        SchemaCodec.Decoder decoder = codec.newDecoder();
        Map<String, Object> decoded = (Map<String, Object>) decoder.decode(TOPIC, framed);
        assertEquals("123-45-6789", decoded.get(TAGGED_FIELD), "SchemaCodec.Decoder decrypts back to the original");
        assertEquals("world", decoded.get("hello"));
    }

    @Test
    void noCsfleConfigLeavesSerdePlaintext() throws Exception {
        String mockUrl = "mock://" + util.getUuid();
        SchemaRegistryClient mockClient = SchemaRegistryClientFactory.newClient(
                List.of(mockUrl), 100, List.of(new JsonSchemaProvider()), Map.of(), Map.of());
        SchemaCodec codec = new SchemaCodec(mockClient, mockUrl, Map.of()); // no CSFLE pass-through configured

        String schemaString = "{\"type\":\"object\",\"properties\":{\"hello\":{\"type\":\"string\"}}}";
        int id = mockClient.register(TOPIC + "-plain-value", new JsonSchema(schemaString));

        byte[] framed = codec.newEncoder().serialize(TOPIC, SchemaType.JSON, id, Map.of("hello", "world"));
        String wireText = new String(framed, StandardCharsets.ISO_8859_1);
        assertTrue(wireText.contains("world"), "no CSFLE config -> plaintext produce, unchanged from before");
    }
}
