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
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ManagedCache;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.util.common.ConfigBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
 * {@link ManagedCacheSchemaRegistryClient} (id→schema lookups, in-memory cached) and {@link #schemaTypeOf} - stay on
 * the singleton, while {@link #newEncoder()} / {@link #newDecoder()} mint <b>owner-confined</b> serde sets. A
 * producer keeps one {@link Encoder} per worker instance (an instance is single-flight); a consumer keeps one
 * {@link Decoder} for its single poll thread. So a given Confluent serializer/deserializer is only ever
 * touched by one thread at a time. JSON, Avro, and Protobuf are wired.</p>
 */
public class SchemaCodec {
    private static final Logger log = LoggerFactory.getLogger(SchemaCodec.class);

    private static final String REGISTRY_URL = "schema.registry.url";
    private static final String CACHE_TTL = "schema.registry.cache.ttl";
    private static final String DEFAULT_CACHE_TTL = "30m";
    /** Name of the platform {@link ManagedCache} that holds id→schema lookups (for monitoring/inspection). */
    public static final String CACHE_NAME = "schema.registry";
    private static final int IDENTITY_MAP_CAPACITY = 1000;
    private static final byte MAGIC_BYTE = 0x0;

    /**
     * The classloader that loaded the Confluent serde classes. Confluent's config validation resolves classes
     * (e.g. the default {@code context.name.strategy} → {@code NullContextNameStrategy}) via the <b>thread
     * context</b> classloader. When the producer runs on a {@code @KernelThreadRunner} pool thread - whose
     * context classloader is not the application's - that lookup fails, so a serde build/use is wrapped with
     * {@link #withSerdeClassLoader} to set this for the duration of the call and restore the previous one after.
     */
    private static final ClassLoader SERDE_CLASSLOADER = AbstractKafkaSchemaSerDeConfig.class.getClassLoader();

    private final SchemaRegistryClient client;
    private final String registryUrl;

    SchemaCodec(SchemaRegistryClient client, String registryUrl) {
        this.client = client;
        this.registryUrl = registryUrl;
    }

    /** Run {@code action} with the Confluent serde classloader as the thread context classloader. */
    private static <T> T withSerdeClassLoader(Supplier<T> action) {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(SERDE_CLASSLOADER);
            return action.get();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    /**
     * Build a codec from application config (schema features stay off when {@code schema.registry.url} is
     * unset - the library keeps its raw byte[] behavior).
     *
     * @param config the application configuration (read for {@code schema.registry.url} and the cache settings)
     * @return a codec, or {@code null} when {@code schema.registry.url} is unset/blank
     */
    public static SchemaCodec fromConfig(AppConfigReader config) {
        return fromConfig(config, config.getProperty(REGISTRY_URL));
    }

    /**
     * Build a codec for an explicit registry URL (used by tests).
     *
     * @param config      configuration source for the cache TTL setting
     * @param registryUrl the Schema Registry URL; a {@code null}/blank value disables schema features
     * @return a codec, or {@code null} when {@code registryUrl} is {@code null}/blank
     */
    public static SchemaCodec fromConfig(ConfigBase config, String registryUrl) {
        if (registryUrl == null || registryUrl.isBlank()) {
            return null;
        }
        long ttlMillis = 1000L * Utility.getInstance()
                .getDurationInSeconds(config.getProperty(CACHE_TTL, DEFAULT_CACHE_TTL));
        /*
         * The schema cache is rebuildable from the registry, so clear it at startup - a stale entry (e.g.
         * after the registry's schemas changed between runs) is never served. It re-populates on demand and
         * TTL-expires within the run. ManagedCache is a JVM-wide singleton by name, so an earlier run in the
         * same JVM (e.g. another test) could have left entries behind; clear() guarantees a clean start.
         */
        ManagedCache cache = ManagedCache.createCache(CACHE_NAME, ttlMillis);
        cache.clear();
        Map<String, Object> srConfig = new HashMap<>();
        srConfig.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
        /*
         * Cache positive results only. Pin Confluent's "missing" (negative) caches to 0 so a not-yet-created
         * schema id is never remembered as absent: a schema registered while the app is running becomes
         * visible on the next lookup, with no stale "not found" lingering until a TTL elapses or the pod is
         * restarted. (These default to 0 already; setting them makes the requirement explicit and durable.)
         */
        srConfig.put(SchemaRegistryClientConfig.MISSING_ID_CACHE_TTL_CONFIG, 0L);
        srConfig.put(SchemaRegistryClientConfig.MISSING_VERSION_CACHE_TTL_CONFIG, 0L);
        srConfig.put(SchemaRegistryClientConfig.MISSING_SCHEMA_CACHE_TTL_CONFIG, 0L);
        SchemaRegistryClient client = new ManagedCacheSchemaRegistryClient(List.of(registryUrl),
                IDENTITY_MAP_CAPACITY,
                List.of(new JsonSchemaProvider(), new AvroSchemaProvider(), new ProtobufSchemaProvider()),
                srConfig, cache);
        log.info("Schema codec ready (registry={}, cache={}, ttlMs={}, types={})",
                registryUrl, CACHE_NAME, ttlMillis, List.of(SchemaType.values()));
        return new SchemaCodec(client, registryUrl);
    }

    /**
     * @return a fresh, owner-confined serde set (one {@link SchemaSerde} per type) over the shared client
     */
    private Map<SchemaType, SchemaSerde> newSerdes() {
        Map<SchemaType, SchemaSerde> serdes = new EnumMap<>(SchemaType.class);
        serdes.put(SchemaType.JSON, new JsonSchemaSerde(client, registryUrl));
        serdes.put(SchemaType.AVRO, new AvroSchemaSerde(client, registryUrl));
        serdes.put(SchemaType.PROTOBUF, new ProtobufSchemaSerde(client, registryUrl));
        return serdes;
    }

    /**
     * @return a new {@link Encoder} for one producer owner (e.g. one {@code simple.kafka.notification} instance)
     */
    public Encoder newEncoder() {
        return withSerdeClassLoader(() -> new Encoder(newSerdes()));
    }

    /**
     * @return a new {@link Decoder} for one consumer owner (one
     *         {@link org.platformlambda.mini.kafka.KafkaFlowConsumer})
     */
    public Decoder newDecoder() {
        return withSerdeClassLoader(() -> new Decoder(this, newSerdes()));
    }

    /**
     * @param data the bytes to inspect (may be {@code null})
     * @return {@code true} if the bytes are Confluent-framed (magic byte + 4-byte id + payload)
     */
    public static boolean isFramed(byte[] data) {
        return data != null && data.length >= 5 && data[0] == MAGIC_BYTE;
    }

    /**
     * @param data Confluent-framed bytes (see {@link #isFramed})
     * @return the global schema id embedded in the frame
     */
    public static int schemaId(byte[] data) {
        return ByteBuffer.wrap(data, 1, 4).getInt();
    }

    /**
     * Look up the registered {@code schemaType} for a global id (shared, thread-safe, disk-cached).
     *
     * @param id the global schema id
     * @return the schema's {@link SchemaType}
     * @throws IllegalStateException if the id cannot be resolved against the registry
     */
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

    /**
     * Visible for tests.
     *
     * @return the shared (file-cached) registry client
     */
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
         *
         * @param topic    the destination topic (a serde may derive its subject from it)
         * @param type     the schema type that selects the serde
         * @param schemaId the pre-registered global schema id to frame with
         * @param value    the value to serialize (typically a {@code Map})
         * @return the Confluent-framed bytes
         * @throws UnsupportedOperationException if {@code type} has no registered serde
         */
        public byte[] serialize(String topic, SchemaType type, int schemaId, Object value) {
            return withSerdeClassLoader(() -> serde(serdes, type).serialize(topic, schemaId, value));
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
         * Read the embedded id, look up the registered {@code schemaType}, and dispatch to the matching serde.
         *
         * @param topic the source topic
         * @param data  the Confluent-framed bytes
         * @return the decoded value as a {@code Map}
         * @throws IllegalArgumentException      if {@code data} is not Confluent-framed
         * @throws UnsupportedOperationException if the embedded schema type has no registered serde
         */
        public Object decode(String topic, byte[] data) {
            if (!isFramed(data)) {
                throw new IllegalArgumentException("payload is not Confluent schema-framed (missing magic byte)");
            }
            return withSerdeClassLoader(() -> serde(serdes, codec.schemaTypeOf(schemaId(data))).decode(topic, data));
        }
    }
}
