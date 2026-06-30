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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchemaUtils;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import org.platformlambda.core.serializers.SimpleMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The {@link SchemaType#JSON} serde: wraps Confluent's {@link KafkaJsonSchemaSerializer}/
 * {@link KafkaJsonSchemaDeserializer}.
 *
 * <p><b>Confinement.</b> The Confluent serdes are not thread-safe; one instance of this class is owned by a
 * single-flight {@link SchemaCodec.Encoder}/{@link SchemaCodec.Decoder}, so the cached serializers (per id)
 * and the lazily-built deserializer are only ever touched by one thread - no synchronization needed.</p>
 */
class JsonSchemaSerde implements SchemaSerde {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final SchemaRegistryClient client;
    private final String registryUrl;
    // one serializer per global schema id (use.schema.id is fixed at configure time); owner-confined
    private final ConcurrentMap<Integer, KafkaJsonSchemaSerializer<Object>> serializers = new ConcurrentHashMap<>();
    private KafkaJsonSchemaDeserializer<Object> deserializer;

    JsonSchemaSerde(SchemaRegistryClient client, String registryUrl) {
        this.client = client;
        this.registryUrl = registryUrl;
    }

    @Override
    public byte[] serialize(String topic, int schemaId, Object value) {
        // Envelope the value with its (pre-registered) schema, so the serializer uses that schema directly
        // instead of DERIVING one from the value's Map<String,Object> type - the derivation can't introspect
        // the Object-typed map values (noisy "Unable to process java.lang.Object" warnings) and is wasted
        // work. use.schema.id still pins the wire id and resolves via getSchemaById (no subject lookup).
        JsonNode node = JSON.valueToTree(value);
        Object enveloped = JsonSchemaUtils.envelope(jsonSchemaById(schemaId), node);
        return serializers.computeIfAbsent(schemaId, this::newSerializer).serialize(topic, enveloped);
    }

    @Override
    public Object decode(String topic, byte[] data) {
        return toMap(deserializer().deserialize(topic, data));
    }

    /** Resolve a pre-registered JSON schema by global id (cached by {@link FileCachedSchemaRegistryClient}). */
    private JsonSchema jsonSchemaById(int schemaId) {
        try {
            ParsedSchema schema = client.getSchemaById(schemaId);
            if (schema instanceof JsonSchema jsonSchema) {
                return jsonSchema;
            }
            throw new IllegalStateException("schema id " + schemaId + " is " + schema.schemaType() + ", not JSON");
        } catch (IOException | RestClientException e) {
            throw new IllegalStateException("Unable to resolve schema id " + schemaId + ": " + e.getMessage(), e);
        }
    }

    // S2095: the serializer is cached for the life of this owner-confined serde (not a method-local resource);
    // closing it here would tear down a still-in-use serializer.
    @SuppressWarnings("java:S2095")
    private KafkaJsonSchemaSerializer<Object> newSerializer(int schemaId) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
        cfg.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, false);
        cfg.put(AbstractKafkaSchemaSerDeConfig.USE_SCHEMA_ID, schemaId);
        // we serialize a pre-registered schema by id; do not enforce strict compatibility checks against
        // a schema derived from the value (the registered schema is authoritative).
        cfg.put(AbstractKafkaSchemaSerDeConfig.ID_COMPATIBILITY_STRICT, false);
        cfg.put(AbstractKafkaSchemaSerDeConfig.LATEST_COMPATIBILITY_STRICT, false);
        return new KafkaJsonSchemaSerializer<>(client, cfg);
    }

    // S2095: the deserializer is cached for the life of this owner-confined serde (see newSerializer).
    @SuppressWarnings("java:S2095")
    private KafkaJsonSchemaDeserializer<Object> deserializer() {
        if (deserializer == null) {
            Map<String, Object> cfg = new HashMap<>();
            cfg.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
            deserializer = new KafkaJsonSchemaDeserializer<>(client, cfg);
        }
        return deserializer;
    }

    /** Normalize the deserializer output to a {@code Map} for the flow dataset body. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Object decoded) {
        if (decoded instanceof Map) {
            return (Map<String, Object>) decoded;
        }
        // JSON Schema deserializer returns a Jackson JsonNode by default; render to JSON then parse to a Map.
        String json = decoded instanceof JsonNode node ? node.toString() : String.valueOf(decoded);
        return SimpleMapper.getInstance().getMapper().readValue(json, Map.class);
    }
}
