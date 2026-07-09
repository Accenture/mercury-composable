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
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import org.platformlambda.mini.kafka.KafkaClientConfig;
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
 * touched by one thread at a time. JSON and Avro are wired.</p>
 *
 * <p><b>Protobuf is deliberately not wired.</b> Confluent's {@code kafka-protobuf-provider} depends on the
 * discontinued {@code com.squareup.wire:wire-runtime-jvm} coordinate, which carries an unpatched
 * denial-of-service CVE (CVE-2026-45799 / GHSA-7xpr-hc2w-34m9) with no available fix — Wire's maintainers
 * will not patch that artifact; the fix only exists under the renamed {@code wire-runtime} coordinate, which
 * Confluent has not adopted as of {@code kafka-protobuf-provider:8.3.0}. Shipping it would fail our security
 * gate, so it is out of scope until Confluent moves or a specific field installation needs it and accepts the
 * risk (see {@link SchemaType#PROTOBUF}).</p>
 */
public class SchemaCodec {
    private static final Logger log = LoggerFactory.getLogger(SchemaCodec.class);

    private static final String REGISTRY_URL = "schema.registry.url";
    /**
     * Prefix for CSFLE (and any other Confluent serde) properties to pass through verbatim - each
     * {@code schema.registry.serde.<key>} in application config becomes {@code <key>} in both the
     * serializer and deserializer config maps of every {@link SchemaSerde}. Generic and driver-agnostic:
     * this is for the KMS <i>driver's</i> own global settings only - e.g. cloud credentials such as
     * {@code access.key.id}/{@code secret.access.key}/{@code profile} (AWS), or the local Tink driver's
     * {@code secret} - which flow through without any code change here. It is <b>not</b> where a key is
     * chosen: the KEK/KMS identity ({@code encrypt.kek.name}/{@code encrypt.kms.type}/
     * {@code encrypt.kms.key.id}) is per-subject and lives on the registered schema's {@code ENCRYPT} rule,
     * which Confluent resolves via {@code RuleContext.getParameter} - never from this serde config. Absent
     * entries ⇒ the serdes build exactly as before (plaintext).
     */
    private static final String SERDE_CONFIG_PREFIX = "schema.registry.serde.";
    private static final String CACHE_TTL = "schema.registry.cache.ttl";
    private static final String DEFAULT_CACHE_TTL = "30m";
    private static final String VERSION_CACHE_TTL = "schema.registry.version.cache.ttl";
    // A pinned subject+numeric-version is immutable, so it can be cached effectively forever; a long TTL
    // (plus a bounded item count) keeps it fresh enough while removing any unbounded-growth risk.
    private static final String DEFAULT_VERSION_CACHE_TTL = "10d";
    private static final long VERSION_CACHE_MAX_ITEMS = 3000L;
    /**
     * Name of the platform {@link ManagedCache} that holds id→schema lookups (for monitoring/inspection).
     * It also holds {@code subject+latest → (id,type)} resolutions (mutable, so the same short TTL applies),
     * under {@code "latest/"}-namespaced keys that cannot collide with the digit-only id keys.
     */
    public static final String CACHE_NAME = "schema.registry";
    /** ManagedCache holding {@code subject+numeric-version → (id,type)} resolutions (immutable, long TTL, bounded). */
    public static final String VERSION_CACHE_NAME = "schema.registry.version";
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
    private final Map<String, Object> extraSerdeConfig;

    SchemaCodec(SchemaRegistryClient client, String registryUrl) {
        this(client, registryUrl, Map.of());
    }

    SchemaCodec(SchemaRegistryClient client, String registryUrl, Map<String, Object> extraSerdeConfig) {
        this.client = client;
        this.registryUrl = registryUrl;
        this.extraSerdeConfig = extraSerdeConfig;
    }

    /**
     * Extract the {@code schema.registry.serde.*} pass-through properties (see {@link #SERDE_CONFIG_PREFIX}).
     * Package-private (rather than private) so it is directly unit-testable.
     *
     * @param config the application configuration to scan
     * @return output key (prefix stripped) → value; empty when none are configured
     */
    static Map<String, Object> extractSerdeConfig(ConfigBase config) {
        Map<String, Object> extra = new HashMap<>();
        config.getCompositeKeyValues().forEach((key, value) -> {
            if (key.startsWith(SERDE_CONFIG_PREFIX)) {
                extra.put(key.substring(SERDE_CONFIG_PREFIX.length()), value);
            }
        });
        return extra;
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
        // subject+numeric-version → (id,type): immutable, so a long TTL (default 10d), bounded to 3000 items.
        long versionTtlMillis = 1000L * Utility.getInstance()
                .getDurationInSeconds(config.getProperty(VERSION_CACHE_TTL, DEFAULT_VERSION_CACHE_TTL));
        ManagedCache versionCache = ManagedCache.createCache(VERSION_CACHE_NAME, versionTtlMillis, VERSION_CACHE_MAX_ITEMS);
        versionCache.clear();
        // (subject+latest resolutions share the short-TTL id cache above; no separate cache needed.)
        /*
         * Start from the schema-registry.properties template (verbatim pass-through), so any Confluent
         * client parameter - OAuth 2.0 bearer auth, basic auth, SSL, optional installation-specific
         * settings - reaches the registry REST client without a library change. Loading the template
         * also auto-registers any OAuth token endpoint URL on the JVM allow-list, which must happen
         * before the client below is constructed. The library's own contract keys are put after the
         * template, so they always win: the registry URL comes from application.properties (the feature
         * switch) and the negative caches stay pinned off.
         */
        Map<String, Object> srConfig = new HashMap<>(KafkaClientConfig.schemaRegistryProperties(config));
        Object bearerAuthSource = srConfig.get(SchemaRegistryClientConfig.BEARER_AUTH_CREDENTIALS_SOURCE);
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
                List.of(new JsonSchemaProvider(), new AvroSchemaProvider()),
                srConfig, cache, versionCache);
        Map<String, Object> extraSerdeConfig = extractSerdeConfig(config);
        log.info("Schema codec ready (registry={}, cache={}, ttlMs={}, types={}, csfle={}, auth={})",
                registryUrl, CACHE_NAME, ttlMillis, List.of(SchemaType.values()), !extraSerdeConfig.isEmpty(),
                bearerAuthSource == null ? "none" : bearerAuthSource);
        return new SchemaCodec(client, registryUrl, extraSerdeConfig);
    }

    /**
     * @return a fresh, owner-confined serde set (one {@link SchemaSerde} per type) over the shared client
     */
    private Map<SchemaType, SchemaSerde> newSerdes() {
        Map<SchemaType, SchemaSerde> serdes = new EnumMap<>(SchemaType.class);
        serdes.put(SchemaType.JSON, new JsonSchemaSerde(client, registryUrl, extraSerdeConfig));
        serdes.put(SchemaType.AVRO, new AvroSchemaSerde(client, registryUrl, extraSerdeConfig));
        // SchemaType.PROTOBUF is intentionally unregistered - see the class-level Javadoc above.
        // serde(...) below fails clearly (UnsupportedOperationException) rather than silently.
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

    /**
     * Resolve a {@code (subject, version)} to a global schema id + type (cached two-tier; see
     * {@link ManagedCacheSchemaRegistryClient#resolve}). This is the producer-side convenience that lets a
     * caller name a subject instead of knowing the global id.
     *
     * @param subject the registered subject name
     * @param version {@code "latest"} (or null/blank) for the newest version, or a positive integer
     * @return the resolved global id and schema type
     * @throws IllegalStateException    if the subject/version cannot be resolved against the registry
     * @throws IllegalArgumentException if {@code version} is neither {@code "latest"} nor a positive integer
     */
    public ResolvedSchema resolve(String subject, String version) {
        if (!(client instanceof ManagedCacheSchemaRegistryClient resolver)) {
            throw new IllegalStateException("subject/version resolution requires ManagedCacheSchemaRegistryClient");
        }
        try {
            return resolver.resolve(subject, version);
        } catch (IOException | RestClientException e) {
            throw new IllegalStateException("Unable to resolve subject '" + subject + "' version '"
                    + version + "': " + e.getMessage(), e);
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
