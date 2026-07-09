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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientFactory;
import io.confluent.kafka.schemaregistry.client.rest.entities.Rule;
import io.confluent.kafka.schemaregistry.client.rest.entities.RuleKind;
import io.confluent.kafka.schemaregistry.client.rest.entities.RuleMode;
import io.confluent.kafka.schemaregistry.client.rest.entities.RuleSet;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.schemaregistry.json.JsonSchemaUtils;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verification spike for the CSFLE design spec (§7): proves that field encryption/decryption runs on
 * exactly the mechanism {@code SchemaCodec}/{@code JsonSchemaSerde} already use for produce/consume
 * ({@code use.schema.id} + envelope) - no framework rule/schema code, only the encryption executor + a KMS
 * driver on the classpath and CSFLE properties in the serde config.
 *
 * <p>Self-contained via Confluent's own {@code mock://} scope test utility - no HTTP server, no cloud KMS,
 * and no changes to {@code EmbeddedSchemaRegistry} or the standalone mock. A single {@code mock://} URL
 * backs both the {@link SchemaRegistryClient} (schema + ruleSet storage) and, transparently, the
 * {@code FieldEncryptionExecutor}'s {@code DekRegistryClient}: {@code EncryptionExecutor.configure()}
 * builds its DEK-registry client from the very same {@code schema.registry.url} value, and
 * {@code DekRegistryClientFactory} honors the identical {@code mock://<scope>} convention as
 * {@code SchemaRegistryClientFactory} (both Confluent test utilities, scope-keyed static in-memory maps).</p>
 *
 * <p>The PII tag is declared <b>inline</b> in the JSON schema text ({@code "confluent:tags": ["PII"]} -
 * {@code JsonSchema.TAGS}), so no separate {@code Metadata} object is needed; the {@code ENCRYPT} rule
 * selects fields by that same tag. The rule executor is picked up by Confluent's {@code ServiceLoader}
 * (enabled by default), so no {@code rule.executors} config is required either.</p>
 */
class CsfleLocalRoundTripTest {

    private static final String TOPIC = "csfle-topic";
    private static final String TAGGED_FIELD = "ssn";
    private static final String PII_TAG = "PII";
    private static final String KEK_NAME = "csfle-test-kek";
    private static final String KMS_TYPE = "local-kms";
    private static final String KMS_KEY_ID = "local-kms://csfle-test-key";
    private static final String SECRET = "spike-test-passphrase";

    @Test
    void encryptsTaggedFieldOnProduceAndDecryptsOnConsume() throws Exception {
        // Unique scope per run so repeated test executions never share mock:// state.
        String mockUrl = "mock://" + Utility.getInstance().getUuid();

        String schemaString = "{\"type\":\"object\",\"properties\":{"
                + "\"hello\":{\"type\":\"string\"},"
                + "\"" + TAGGED_FIELD + "\":{\"type\":\"string\",\"confluent:tags\":[\"" + PII_TAG + "\"]}"
                + "},\"additionalProperties\":true}";

        Map<String, String> ruleParams = new HashMap<>();
        ruleParams.put("encrypt.kek.name", KEK_NAME);
        ruleParams.put("encrypt.kms.type", KMS_TYPE);
        ruleParams.put("encrypt.kms.key.id", KMS_KEY_ID);
        Rule encryptRule = new Rule("encryptPII", null, RuleKind.TRANSFORM, RuleMode.WRITEREAD,
                "ENCRYPT", Set.of(PII_TAG), ruleParams, null, null, "ERROR", false);
        RuleSet ruleSet = new RuleSet(List.of(), List.of(encryptRule));
        JsonSchema ruledSchema = new JsonSchema(schemaString).copy(null, ruleSet);

        SchemaRegistryClient client = SchemaRegistryClientFactory.newClient(
                List.of(mockUrl), 100, List.of(new JsonSchemaProvider()), Map.of(), Map.of());
        int id = client.register(TOPIC + "-value", ruledSchema);

        // The exact config shape our own JsonSchemaSerde builds (use.schema.id, auto-register off), plus
        // the CSFLE-specific KMS secret. No rule.executors entry: ServiceLoader auto-discovers
        // FieldEncryptionExecutor from the jar's META-INF/services registration.
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, mockUrl);
        cfg.put(AbstractKafkaSchemaSerDeConfig.USE_SCHEMA_ID, id);
        cfg.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, false);
        cfg.put("secret", SECRET);

        Map<String, Object> value = Map.of("hello", "world", TAGGED_FIELD, "123-45-6789");
        // Envelope the value with the pre-registered (ruled) schema directly - exactly what our own
        // JsonSchemaSerde.serialize does - so the serializer uses that schema as-is instead of deriving one
        // from the raw Map (which would fail id.compatibility.strict against the inline-tagged schema).
        JsonNode node = new ObjectMapper().valueToTree(value);
        Object enveloped = JsonSchemaUtils.envelope(ruledSchema, node);
        byte[] framed;
        try (KafkaJsonSchemaSerializer<Object> serializer = new KafkaJsonSchemaSerializer<>(client, cfg)) {
            framed = serializer.serialize(TOPIC, enveloped);
        }

        // The tagged field's plaintext must not appear on the wire; the untagged field is unaffected.
        String wireText = new String(framed, StandardCharsets.ISO_8859_1);
        assertFalse(wireText.contains("123-45-6789"), "tagged field must be encrypted on the wire");
        assertTrue(wireText.contains("world"), "untagged field stays plaintext on the wire");

        Object decoded;
        try (KafkaJsonSchemaDeserializer<Object> deserializer = new KafkaJsonSchemaDeserializer<>(client, cfg)) {
            decoded = deserializer.deserialize(TOPIC, framed);
        }
        Map<String, Object> decrypted = toMap(decoded);
        assertEquals("123-45-6789", decrypted.get(TAGGED_FIELD), "tagged field decrypts back to the original");
        assertEquals("world", decrypted.get("hello"), "untagged field round-trips unchanged");
    }

    /** Mirrors JsonSchemaSerde's own normalization: the deserializer returns a Map or a Jackson JsonNode. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Object decoded) {
        if (decoded instanceof Map) {
            return (Map<String, Object>) decoded;
        }
        String json = decoded instanceof JsonNode node ? node.toString() : String.valueOf(decoded);
        return SimpleMapper.getInstance().getMapper().readValue(json, Map.class);
    }
}
