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
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.util.common.ConfigBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridges the minimalist {@code byte[]} Kafka transport to the Confluent Schema Registry wire format
 * ({@code [magic 0x00][4-byte global id][payload]}) using Confluent's <b>own</b> serializers as a library -
 * no reinvented codec.
 *
 * <p><b>Produce</b> ({@link Encoder}) is driven by a caller-supplied global {@code schemaId} (schemas are
 * pre-registered; the producer never computes a subject or registers), so it is subject-naming-strategy
 * agnostic and a topic can carry many record types. <b>Consume</b> ({@link Decoder}) reads the embedded id,
 * looks up the registered schema's {@code schemaType}, and dispatches to the matching deserializer.</p>
 *
 * <p><b>Thread model.</b> The Confluent serializers/deserializers are <b>not thread-safe</b>, so this codec is
 * a <b>factory</b>, not a shared (de)serializer holder: the shared, thread-safe parts - the
 * {@link FileCachedSchemaRegistryClient} (id→schema lookups, disk-cached) and {@link #schemaTypeOf} - stay on
 * the singleton, while {@link #newEncoder()} / {@link #newDecoder()} mint <b>owner-confined</b> serde sets. A
 * producer keeps one {@link Encoder} per worker instance (an instance is single-flight); a consumer keeps one
 * {@link Decoder} for its single poll thread. So a given Confluent serializer/deserializer is only ever
 * touched by one thread at a time. JSON, Avro, and Protobuf are wired.</p>
 */
public class SchemaCodec {
    private static final Logger log = LoggerFactory.getLogger(SchemaCodec.class);

    private static final String REGISTRY_URL = "schema.registry.url";
    private static final String CACHE_DIR = "schema.registry.cache.dir";
    private static final String CACHE_TTL = "schema.registry.cache.ttl";
    private static final String DEFAULT_CACHE_DIR = "/tmp/schema-registry-cache";
    private static final String DEFAULT_CACHE_TTL = "24h";
    private static final int IDENTITY_MAP_CAPACITY = 1000;
    private static final byte MAGIC_BYTE = 0x0;

    private final SchemaRegistryClient client;
    private final String registryUrl;

    SchemaCodec(SchemaRegistryClient client, String registryUrl) {
        this.client = client;
        this.registryUrl = registryUrl;
    }

    /**
     * Build a codec from application config, or return {@code null} when {@code schema.registry.url} is not
     * set (schema features simply stay off - the library keeps its raw byte[] behavior).
     */
    public static SchemaCodec fromConfig(AppConfigReader config) {
        return fromConfig(config, config.getProperty(REGISTRY_URL));
    }

    /** Build a codec for an explicit registry URL (used by tests); {@code null}/blank URL ⇒ {@code null}. */
    public static SchemaCodec fromConfig(ConfigBase config, String registryUrl) {
        if (registryUrl == null || registryUrl.isBlank()) {
            return null;
        }
        File cacheDir = new File(config.getProperty(CACHE_DIR, DEFAULT_CACHE_DIR));
        long ttlMillis = 1000L * Utility.getInstance()
                .getDurationInSeconds(config.getProperty(CACHE_TTL, DEFAULT_CACHE_TTL));
        /*
         * The schema cache is rebuildable from the registry, so clear it at startup - a stale entry (e.g.
         * after the registry's schemas changed between runs) is never served. It re-populates on demand and
         * TTL-expires within the run. (Unlike the registry's data store, a cache has no durable mode.)
         */
        Utility.getInstance().cleanupDir(cacheDir);
        Map<String, Object> srConfig = new HashMap<>();
        srConfig.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
        SchemaRegistryClient client = new FileCachedSchemaRegistryClient(List.of(registryUrl),
                IDENTITY_MAP_CAPACITY,
                List.of(new JsonSchemaProvider(), new AvroSchemaProvider(), new ProtobufSchemaProvider()),
                srConfig, cacheDir, ttlMillis);
        log.info("Schema codec ready (registry={}, cache={}, ttlMs={}, types={})",
                registryUrl, cacheDir, ttlMillis, List.of(SchemaType.values()));
        return new SchemaCodec(client, registryUrl);
    }

    /** A fresh, owner-confined serde set (one {@link SchemaSerde} per type) over the shared registry client. */
    private Map<SchemaType, SchemaSerde> newSerdes() {
        Map<SchemaType, SchemaSerde> serdes = new EnumMap<>(SchemaType.class);
        serdes.put(SchemaType.JSON, new JsonSchemaSerde(client, registryUrl));
        serdes.put(SchemaType.AVRO, new AvroSchemaSerde(client, registryUrl));
        serdes.put(SchemaType.PROTOBUF, new ProtobufSchemaSerde(client, registryUrl));
        return serdes;
    }

    /** Mint an {@link Encoder} for one producer owner (e.g. one {@code simple.kafka.notification} instance). */
    public Encoder newEncoder() {
        return new Encoder(newSerdes());
    }

    /** Mint a {@link Decoder} for one consumer owner (one {@link org.platformlambda.mini.kafka.KafkaFlowConsumer}). */
    public Decoder newDecoder() {
        return new Decoder(this, newSerdes());
    }

    /** True if the bytes are Confluent-framed (magic byte + 4-byte id + payload). */
    public static boolean isFramed(byte[] data) {
        return data != null && data.length >= 5 && data[0] == MAGIC_BYTE;
    }

    /** The global schema id embedded in framed bytes. */
    public static int schemaId(byte[] data) {
        return ByteBuffer.wrap(data, 1, 4).getInt();
    }

    /** Look up the registered {@code schemaType} for a global id (shared, thread-safe, disk-cached). */
    SchemaType schemaTypeOf(int id) {
        try {
            ParsedSchema schema = client.getSchemaById(id);
            return SchemaType.from(schema.schemaType());
        } catch (IOException | RestClientException e) {
            throw new IllegalStateException("Unable to resolve schema id " + id + ": " + e.getMessage(), e);
        }
    }

    private static SchemaSerde serde(Map<SchemaType, SchemaSerde> serdes, SchemaType type) {
        SchemaSerde serde = serdes.get(type);
        if (serde == null) {
            throw new UnsupportedOperationException("schema-type " + type + " is not yet supported");
        }
        return serde;
    }

    /** Visible for tests: the shared registry client (file-cached). */
    public SchemaRegistryClient client() {
        return client;
    }

    /**
     * Owner-confined producer codec: serialize a value into the Confluent wire format for a known global
     * schema id. Not thread-safe by itself - hold one per single-flight owner (see {@link SchemaCodec}).
     */
    public static final class Encoder {
        private final Map<SchemaType, SchemaSerde> serdes;

        Encoder(Map<SchemaType, SchemaSerde> serdes) {
            this.serdes = serdes;
        }

        /**
         * Serialize using the serde for {@code type}. The schema is fetched by id and NOT registered
         * (auto-register off), so no subject/strategy is involved.
         */
        public byte[] serialize(String topic, SchemaType type, int schemaId, Object value) {
            return serde(serdes, type).serialize(topic, schemaId, value);
        }
    }

    /**
     * Owner-confined consumer codec: decode Confluent-framed bytes by their embedded id. Not thread-safe by
     * itself - hold one per single-threaded owner (see {@link SchemaCodec}).
     */
    public static final class Decoder {
        private final SchemaCodec codec;
        private final Map<SchemaType, SchemaSerde> serdes;

        Decoder(SchemaCodec codec, Map<SchemaType, SchemaSerde> serdes) {
            this.codec = codec;
            this.serdes = serdes;
        }

        /**
         * Read the embedded id, look up the registered {@code schemaType}, and dispatch to the matching serde,
         * returning a {@code Map}. Throws for unframed bytes or a schema type not yet supported.
         */
        public Object decode(String topic, byte[] data) {
            if (!isFramed(data)) {
                throw new IllegalArgumentException("payload is not Confluent schema-framed (missing magic byte)");
            }
            return serde(serdes, codec.schemaTypeOf(schemaId(data))).decode(topic, data);
        }
    }
}
