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
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The {@link SchemaType#AVRO} serde: wraps Confluent's {@link KafkaAvroSerializer}/{@link KafkaAvroDeserializer}.
 *
 * <p>On serialize, the supplied {@code Map} is converted to a {@link org.apache.avro.generic.GenericRecord}
 * built against the pre-registered schema (resolved by id) - see {@link AvroConversions} - so a field absent
 * from the {@code Map} takes its schema default. On decode the deserializer yields a {@code GenericRecord}
 * (generic, not specific - no generated classes) which is rendered back to a {@code Map}.</p>
 *
 * <p><b>Confinement.</b> The Confluent serdes are not thread-safe; one instance of this class is owned by a
 * single-flight {@link SchemaCodec.Encoder}/{@link SchemaCodec.Decoder}, so the cached serializers (per id)
 * and the lazily-built deserializer are only ever touched by one thread - no synchronization needed.</p>
 */
class AvroSchemaSerde implements SchemaSerde {

    private final SchemaRegistryClient client;
    private final String registryUrl;
    // one serializer per global schema id (use.schema.id is fixed at configure time); owner-confined,
    // lazily populated per id. There is only ever one deserializer, so it is built eagerly.
    private final ConcurrentMap<Integer, KafkaAvroSerializer> serializers = new ConcurrentHashMap<>();
    private final KafkaAvroDeserializer deserializer;

    // S2095: the deserializer (and the per-id serializers in newSerializer) are kept for the life of this
    // owner-confined serde - not method-local resources - so they are not closed here.
    @SuppressWarnings("java:S2095")
    AvroSchemaSerde(SchemaRegistryClient client, String registryUrl) {
        this.client = client;
        this.registryUrl = registryUrl;
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
        this.deserializer = new KafkaAvroDeserializer(client, cfg);
    }

    @Override
    public byte[] serialize(String topic, int schemaId, Object value) {
        Object avroRecord = AvroConversions.toAvro(value, avroSchemaById(schemaId).rawSchema());
        return serializers.computeIfAbsent(schemaId, this::newSerializer).serialize(topic, avroRecord);
    }

    @Override
    public Object decode(String topic, byte[] data) {
        // generic reader (default) -> GenericRecord; render it to a plain Map for the flow body.
        return AvroConversions.fromAvro(deserializer.deserialize(topic, data));
    }

    /** Resolve a pre-registered Avro schema by global id (cached by {@link FileCachedSchemaRegistryClient}). */
    private AvroSchema avroSchemaById(int schemaId) {
        try {
            ParsedSchema schema = client.getSchemaById(schemaId);
            if (schema instanceof AvroSchema avroSchema) {
                return avroSchema;
            }
            throw new IllegalStateException("schema id " + schemaId + " is " + schema.schemaType() + ", not AVRO");
        } catch (IOException | RestClientException e) {
            throw new IllegalStateException("Unable to resolve schema id " + schemaId + ": " + e.getMessage(), e);
        }
    }

    // S2095: the serializer is cached for the life of this owner-confined serde (not a method-local resource);
    // closing it here would tear down a still-in-use serializer.
    @SuppressWarnings("java:S2095")
    private KafkaAvroSerializer newSerializer(int schemaId) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
        cfg.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, false);
        cfg.put(AbstractKafkaSchemaSerDeConfig.USE_SCHEMA_ID, schemaId);
        // we serialize a pre-registered schema by id; the GenericRecord is built against that exact schema,
        // so do not enforce strict compatibility checks against a schema derived from the value.
        cfg.put(AbstractKafkaSchemaSerDeConfig.ID_COMPATIBILITY_STRICT, false);
        cfg.put(AbstractKafkaSchemaSerDeConfig.LATEST_COMPATIBILITY_STRICT, false);
        return new KafkaAvroSerializer(client, cfg);
    }
}
