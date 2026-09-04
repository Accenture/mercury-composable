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

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientFactory;
import io.confluent.kafka.schemaregistry.client.rest.entities.Rule;
import io.confluent.kafka.schemaregistry.client.rest.entities.RuleKind;
import io.confluent.kafka.schemaregistry.client.rest.entities.RuleMode;
import io.confluent.kafka.schemaregistry.client.rest.entities.RuleSet;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.util.common.ConfigBase;
import org.platformlambda.mini.kafka.OAuthUrlAllowList;

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

    private String savedAllowList;

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
        @Override public Map<String, Object> getCompositeKeyValues() { return getMap(); }
    }

    @Test
    void extractSerdeConfigStripsPrefixAndIgnoresOtherKeys() {
        Map<String, Object> appConfig = new HashMap<>();
        // genuine driver-level keys only: an AWS credential + the local KMS driver's secret. NOT
        // encrypt.kek.name/kms.type/kms.key.id - those are per-subject rule params resolved from the
        // registered schema, never from this serde pass-through (see csfleConfigReachesEncoderAndDecoder).
        appConfig.put("schema.registry.serde.access.key.id", "AKIA-example");
        appConfig.put("schema.registry.serde.secret", "my-secret");
        appConfig.put("schema.registry.cache.ttl", "30m");   // not a serde.* key - must be excluded
        appConfig.put("schema.registry.url", "http://localhost:8081"); // likewise excluded

        Map<String, Object> extracted = SchemaCodec.extractSerdeConfig(new MapConfig(appConfig));

        assertEquals(Map.of("access.key.id", "AKIA-example", "secret", "my-secret"), extracted);
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
    @SuppressWarnings("unchecked")
    void avroCsfleConfigReachesEncoderAndDecoder() throws Exception {
        // Avro mirror of csfleConfigReachesEncoderAndDecoder: Avro has different mechanics (the input Map is
        // converted to a GenericRecord against the registered schema before serialize), so CSFLE is proven
        // end-to-end for Avro too, not just assumed symmetric with JSON. Same delegation model: the ENCRYPT
        // rule + its KEK/KMS identity live on the registered schema; only the local KMS driver's global
        // "secret" comes through the schema.registry.serde.* pass-through.
        String mockUrl = "mock://" + util.getUuid();

        Map<String, Object> extraSerdeConfig = new HashMap<>();
        extraSerdeConfig.put("secret", "spike-test-passphrase");

        SchemaRegistryClient mockClient = SchemaRegistryClientFactory.newClient(
                List.of(mockUrl), 100, List.of(new AvroSchemaProvider()), Map.of(), Map.of());
        SchemaCodec codec = new SchemaCodec(mockClient, mockUrl, extraSerdeConfig);

        // the tagged field carries confluent:tags inline in the Avro schema; the ENCRYPT rule selects by tag
        String schemaString = "{\"type\":\"record\",\"name\":\"Customer\",\"namespace\":\"test\",\"fields\":["
                + "{\"name\":\"hello\",\"type\":\"string\"},"
                + "{\"name\":\"" + TAGGED_FIELD + "\",\"type\":\"string\",\"confluent:tags\":[\"" + PII_TAG + "\"]}"
                + "]}";
        Map<String, String> ruleParams = new HashMap<>();
        ruleParams.put("encrypt.kek.name", "csfle-avro-kek");
        ruleParams.put("encrypt.kms.type", "local-kms");
        ruleParams.put("encrypt.kms.key.id", "local-kms://csfle-avro-key");
        Rule encryptRule = new Rule("encryptPII", null, RuleKind.TRANSFORM, RuleMode.WRITEREAD,
                "ENCRYPT", Set.of(PII_TAG), ruleParams, null, null, "ERROR", false);
        ParsedSchema ruledSchema = new AvroSchema(schemaString).copy(null, new RuleSet(List.of(), List.of(encryptRule)));
        int id = mockClient.register(TOPIC + "-avro-value", ruledSchema);

        SchemaCodec.Encoder encoder = codec.newEncoder();
        Map<String, Object> value = Map.of("hello", "world", TAGGED_FIELD, "123-45-6789");
        byte[] framed = encoder.serialize(TOPIC, SchemaType.AVRO, id, value);

        String wireText = new String(framed, StandardCharsets.ISO_8859_1);
        assertFalse(wireText.contains("123-45-6789"), "tagged Avro field must be encrypted on the wire via SchemaCodec");

        SchemaCodec.Decoder decoder = codec.newDecoder();
        Map<String, Object> decoded = (Map<String, Object>) decoder.decode(TOPIC, framed);
        assertEquals("123-45-6789", decoded.get(TAGGED_FIELD), "SchemaCodec.Decoder decrypts the Avro field back");
        assertEquals("world", decoded.get("hello"));
    }

    /**
     * The inherit-template fixture carries an OAuth token endpoint (required by Confluent's
     * OauthCredentialProvider), and loading it auto-registers that URL on a JVM-wide system property.
     * Save and restore it so these tests leave no global state behind, as SchemaRegistryOAuthTest does.
     */
    @BeforeEach
    void saveAllowList() {
        savedAllowList = System.getProperty(OAuthUrlAllowList.ALLOWED_URLS_PROPERTY);
    }

    @AfterEach
    void restoreAllowList() {
        if (savedAllowList == null) {
            System.clearProperty(OAuthUrlAllowList.ALLOWED_URLS_PROPERTY);
        } else {
            System.setProperty(OAuthUrlAllowList.ALLOWED_URLS_PROPERTY, savedAllowList);
        }
    }

    @Test
    void serdeInheritsRegistryClientTemplate() {
        // A serde's rule executors build their own registry clients from this config map alone -
        // Confluent's CSFLE EncryptionExecutor.configure() creates a DekRegistryClient from it - so a
        // map seeded empty leaves those clients unauthenticated and every DEK lookup 401s, even though
        // the codec's own client authenticates against the same registry. The credentials the template
        // already resolved must therefore reach the serdes too.
        Map<String, Object> appConfig = new HashMap<>();
        appConfig.put("schema.registry.properties", "classpath:/schema-registry-serde-inherit-test.properties");
        // no calls are made against this URL; only the config composition is under test
        SchemaCodec codec = SchemaCodec.fromConfig(new MapConfig(appConfig), "http://127.0.0.1:1");

        Map<String, Object> serdeConfig = codec.serdeConfig();
        assertEquals("OAUTHBEARER", serdeConfig.get("bearer.auth.credentials.source"));
        assertEquals("inherit-client", serdeConfig.get("bearer.auth.client.id"));
        assertEquals("inherit-secret", serdeConfig.get("bearer.auth.client.secret"));
        assertEquals("lsrc-test", serdeConfig.get("bearer.auth.logical.cluster"));
        // the library's own pins belong to the registry REST client, not to the serdes: inheriting the
        // template (not the post-processed client config) keeps them out
        assertFalse(serdeConfig.containsKey("schema.registry.url"),
                "serde config carries only what the application authored");
        assertFalse(serdeConfig.containsKey("missing.id.cache.ttl.sec"),
                "the library's negative-cache pins stay on the registry client");
    }

    @Test
    void serdeOverridesWinOverInheritedTemplate() {
        Map<String, Object> appConfig = new HashMap<>();
        appConfig.put("schema.registry.properties", "classpath:/schema-registry-serde-inherit-test.properties");
        // an explicit serde.* entry must still take precedence, so a serde-only identity stays expressible
        appConfig.put("schema.registry.serde.bearer.auth.client.id", "serde-specific-client");
        appConfig.put("schema.registry.serde.secret", "local-kms-passphrase");
        SchemaCodec codec = SchemaCodec.fromConfig(new MapConfig(appConfig), "http://127.0.0.1:1");

        Map<String, Object> serdeConfig = codec.serdeConfig();
        assertEquals("serde-specific-client", serdeConfig.get("bearer.auth.client.id"), "serde.* overrides the template");
        assertEquals("local-kms-passphrase", serdeConfig.get("secret"), "serde-only keys still pass through");
        assertEquals("inherit-secret", serdeConfig.get("bearer.auth.client.secret"),
                "keys the serde does not override are still inherited");
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
