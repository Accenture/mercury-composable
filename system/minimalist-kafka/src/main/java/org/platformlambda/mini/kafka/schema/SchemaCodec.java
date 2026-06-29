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
 * <p><b>Produce</b> ({@link #serialize}) is driven by a caller-supplied global {@code schemaId} (schemas are
 * pre-registered; the producer never computes a subject or registers), so it is subject-naming-strategy
 * agnostic and a topic can carry many record types. <b>Consume</b> ({@link #decode}) reads the embedded id,
 * looks up the registered schema's {@code schemaType}, and dispatches to the matching deserializer.</p>
 *
 * <p>Each {@link SchemaType} is handled by a {@link SchemaSerde} kept in {@link #serdes}; this codec just
 * picks one (by the {@code schema-type} header on produce, by the registered type on consume). JSON and Avro
 * are wired; Protobuf throws clearly until its phase. The shared {@link SchemaRegistryClient} is a
 * {@link FileCachedSchemaRegistryClient}, so schema lookups by id are cached on disk (TTL-bounded).</p>
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
    private final Map<SchemaType, SchemaSerde> serdes;

    SchemaCodec(SchemaRegistryClient client, Map<SchemaType, SchemaSerde> serdes) {
        this.client = client;
        this.serdes = serdes;
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
        // The schema cache is rebuildable from the registry, so clear it at startup - a stale entry (e.g.
        // after the registry's schemas changed between runs) is never served. It re-populates on demand and
        // TTL-expires within the run. (Unlike the registry's data store, a cache has no durable mode.)
        Utility.getInstance().cleanupDir(cacheDir);
        Map<String, Object> srConfig = new HashMap<>();
        srConfig.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
        SchemaRegistryClient client = new FileCachedSchemaRegistryClient(List.of(registryUrl),
                IDENTITY_MAP_CAPACITY, List.of(new JsonSchemaProvider(), new AvroSchemaProvider()),
                srConfig, cacheDir, ttlMillis);
        Map<SchemaType, SchemaSerde> serdes = new EnumMap<>(SchemaType.class);
        serdes.put(SchemaType.JSON, new JsonSchemaSerde(client, registryUrl));
        serdes.put(SchemaType.AVRO, new AvroSchemaSerde(client, registryUrl));
        log.info("Schema codec ready (registry={}, cache={}, ttlMs={}, types={})",
                registryUrl, cacheDir, ttlMillis, serdes.keySet());
        return new SchemaCodec(client, serdes);
    }

    /** True if the bytes are Confluent-framed (magic byte + 4-byte id + payload). */
    public static boolean isFramed(byte[] data) {
        return data != null && data.length >= 5 && data[0] == MAGIC_BYTE;
    }

    /** The global schema id embedded in framed bytes. */
    public static int schemaId(byte[] data) {
        return ByteBuffer.wrap(data, 1, 4).getInt();
    }

    /**
     * Serialize a value into the Confluent wire format for a known global schema id, dispatching to the
     * serde for {@code type}. The schema is fetched from the registry by id and NOT registered
     * (auto-register off), so no subject/strategy is involved.
     */
    public byte[] serialize(String topic, SchemaType type, int schemaId, Object value) {
        return serde(type).serialize(topic, schemaId, value);
    }

    /**
     * Decode Confluent-framed bytes: read the embedded id, look up the registered {@code schemaType}, and
     * dispatch to the matching serde, returning a {@code Map}. Throws for unframed bytes or a schema type
     * not yet supported.
     */
    public Object decode(String topic, byte[] data) {
        if (!isFramed(data)) {
            throw new IllegalArgumentException("payload is not Confluent schema-framed (missing magic byte)");
        }
        return serde(schemaTypeOf(schemaId(data))).decode(topic, data);
    }

    private SchemaSerde serde(SchemaType type) {
        SchemaSerde serde = serdes.get(type);
        if (serde == null) {
            throw new UnsupportedOperationException("schema-type " + type + " is not yet supported");
        }
        return serde;
    }

    private SchemaType schemaTypeOf(int id) {
        try {
            ParsedSchema schema = client.getSchemaById(id);
            return SchemaType.from(schema.schemaType());
        } catch (IOException | RestClientException e) {
            throw new IllegalStateException("Unable to resolve schema id " + id + ": " + e.getMessage(), e);
        }
    }

    /** Visible for tests: the shared registry client (file-cached). */
    public SchemaRegistryClient client() {
        return client;
    }
}
