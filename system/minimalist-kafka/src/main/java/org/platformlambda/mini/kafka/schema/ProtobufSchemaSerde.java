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

import com.google.protobuf.Message;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The {@link SchemaType#PROTOBUF} serde: wraps Confluent's {@link KafkaProtobufSerializer}/
 * {@link KafkaProtobufDeserializer}.
 *
 * <p>On serialize, the supplied {@code Map} is converted to a {@link com.google.protobuf.DynamicMessage}
 * built against the pre-registered schema's descriptor (resolved by id) - see {@link ProtobufConversions} -
 * so no generated classes are needed. On decode the deserializer yields a {@code DynamicMessage} which is
 * rendered back to a {@code Map}. (Confluent's protobuf wire format adds a message-index header after the
 * id; the serde handles that framing - this codec only reads the leading magic byte + global id.)</p>
 *
 * <p><b>Confinement.</b> The Confluent serdes are not thread-safe; one instance of this class is owned by a
 * single-flight {@link SchemaCodec.Encoder}/{@link SchemaCodec.Decoder}, so the cached serializers (per id)
 * and the lazily-built deserializer are only ever touched by one thread - no synchronization needed.</p>
 */
class ProtobufSchemaSerde implements SchemaSerde {

    private final SchemaRegistryClient client;
    private final String registryUrl;
    // one serializer per global schema id (use.schema.id is fixed at configure time); owner-confined
    private final ConcurrentMap<Integer, KafkaProtobufSerializer<Message>> serializers = new ConcurrentHashMap<>();
    private KafkaProtobufDeserializer<Message> deserializer;

    ProtobufSchemaSerde(SchemaRegistryClient client, String registryUrl) {
        this.client = client;
        this.registryUrl = registryUrl;
    }

    @Override
    public byte[] serialize(String topic, int schemaId, Object value) {
        Message message = ProtobufConversions.toMessage(value, protobufSchemaById(schemaId).toDescriptor());
        return serializers.computeIfAbsent(schemaId, this::newSerializer).serialize(topic, message);
    }

    @Override
    public Object decode(String topic, byte[] data) {
        // generic reader -> DynamicMessage; render it to a plain Map for the flow body.
        return ProtobufConversions.fromMessage(deserializer().deserialize(topic, data));
    }

    /** Resolve a pre-registered Protobuf schema by global id (cached by {@link FileCachedSchemaRegistryClient}). */
    private ProtobufSchema protobufSchemaById(int schemaId) {
        try {
            ParsedSchema schema = client.getSchemaById(schemaId);
            if (schema instanceof ProtobufSchema protobufSchema) {
                return protobufSchema;
            }
            throw new IllegalStateException("schema id " + schemaId + " is " + schema.schemaType() + ", not PROTOBUF");
        } catch (IOException | RestClientException e) {
            throw new IllegalStateException("Unable to resolve schema id " + schemaId + ": " + e.getMessage(), e);
        }
    }

    // S2095: the serializer is cached for the life of this owner-confined serde (not a method-local resource);
    // closing it here would tear down a still-in-use serializer.
    @SuppressWarnings("java:S2095")
    private KafkaProtobufSerializer<Message> newSerializer(int schemaId) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
        cfg.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, false);
        cfg.put(AbstractKafkaSchemaSerDeConfig.USE_SCHEMA_ID, schemaId);
        // we serialize a pre-registered schema by id; the DynamicMessage is built against that exact schema,
        // so do not enforce strict compatibility checks against a schema derived from the value.
        cfg.put(AbstractKafkaSchemaSerDeConfig.ID_COMPATIBILITY_STRICT, false);
        cfg.put(AbstractKafkaSchemaSerDeConfig.LATEST_COMPATIBILITY_STRICT, false);
        return new KafkaProtobufSerializer<>(client, cfg);
    }

    // S2095: the deserializer is cached for the life of this owner-confined serde (see newSerializer).
    @SuppressWarnings("java:S2095")
    private KafkaProtobufDeserializer<Message> deserializer() {
        if (deserializer == null) {
            Map<String, Object> cfg = new HashMap<>();
            cfg.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
            // no specific.protobuf.value.type -> deserialize to a generic DynamicMessage
            deserializer = new KafkaProtobufDeserializer<>(client, cfg);
        }
        return deserializer;
    }
}
