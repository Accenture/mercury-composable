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
import io.confluent.kafka.schemaregistry.SchemaProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.platformlambda.core.util.ManagedCache;
import org.platformlambda.core.util.Utility;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A {@link CachedSchemaRegistryClient} that fronts {@code getSchemaById} - the hot path used by both
 * serialization-by-id and deserialization - with platform-core's in-memory {@link ManagedCache}. A global
 * schema id is immutable in Confluent (ids are content-addressed), so a resolved {@link ParsedSchema} is
 * cached by id and re-served on later lookups, eliminating repeat registry round-trips. The cache is bounded
 * and TTL-expired by the platform housekeeper - nothing is written to disk.
 *
 * <p><b>Positive results only.</b> A not-found id is never cached (the registry lookup throws before the
 * cache write), so a schema registered while the app is running becomes visible on the very next lookup -
 * no stale "not found" lingers until a TTL elapses or the pod restarts. ({@link SchemaCodec} also pins
 * Confluent's own "missing" caches off, so no layer remembers an absent id.)</p>
 *
 * <p>The TTL comes from {@code application.properties} ({@code schema.registry.cache.ttl}); the cache is built
 * (and cleared at startup) by {@link SchemaCodec}. The Confluent serdes call {@code getSchemaById} through
 * this client, so they transparently benefit, for any schema type.</p>
 */
public class ManagedCacheSchemaRegistryClient extends CachedSchemaRegistryClient {

    /** The reserved version alias that resolves to a subject's current newest version. */
    public static final String LATEST = "latest";

    private final ManagedCache cache;
    private final ManagedCache versionCache;

    public ManagedCacheSchemaRegistryClient(List<String> urls, int identityMapCapacity,
                                            List<SchemaProvider> providers, Map<String, ?> configs,
                                            ManagedCache cache, ManagedCache versionCache) {
        super(urls, identityMapCapacity, providers, configs);
        this.cache = cache;
        this.versionCache = versionCache;
    }

    /**
     * Resolve a schema by global id, serving a cached {@link ParsedSchema} when present and otherwise fetching
     * from the registry and caching it. The id is content-addressed and immutable, so a cache hit is always
     * the correct schema.
     *
     * @param id the global schema id
     * @return the parsed schema for {@code id}
     * @throws IOException         if the registry round-trip fails
     * @throws RestClientException if the registry rejects the lookup
     */
    @Override
    public ParsedSchema getSchemaById(int id) throws IOException, RestClientException {
        String key = Integer.toString(id);
        if (cache.get(key) instanceof ParsedSchema cached) {
            return cached;
        }
        // Positive results only: a not-found id throws here, before the cache write below, so it is never cached.
        ParsedSchema schema = super.getSchemaById(id);
        cache.put(key, schema);
        return schema;
    }

    /**
     * Resolve a {@code (subject, version)} to a global schema id and its type. The id is then used by the
     * id-based serializer ({@code use.schema.id}) to frame the Confluent wire format - subject/version are a
     * producer-side convenience and never travel on the wire.
     *
     * <p><b>Cache, by mutability.</b> A pinned numeric version is immutable, so its resolution is cached in
     * {@code versionCache} (long TTL, bounded). A subject's {@code latest} is mutable (a new registration
     * moves it), so it shares the short-TTL id→schema {@code cache} (under a {@code "latest/"}-namespaced key
     * that cannot collide with the digit-only id keys) - a short TTL re-resolves a moved {@code latest}.</p>
     *
     * <p>The schema type is taken from the parsed schema (via {@link #getSchemaById(int)}), so it is
     * authoritative - never guessed from a possibly-absent {@code schemaType} - and that lookup also warms the
     * id→schema cache, so the subsequent serialize needs no extra round-trip.</p>
     *
     * @param subject the registered subject name
     * @param version {@code "latest"} (or null/blank) for the newest version, or a positive integer
     * @return the resolved global id and schema type
     * @throws IOException              if a registry round-trip fails
     * @throws RestClientException      if the registry rejects the lookup (e.g. subject/version not found)
     * @throws IllegalArgumentException if {@code version} is neither {@code "latest"} nor a positive integer
     */
    public ResolvedSchema resolve(String subject, String version) throws IOException, RestClientException {
        boolean latest = version == null || version.isBlank() || LATEST.equalsIgnoreCase(version.trim());
        int parsedVersion = latest ? 0 : parseVersion(version);
        // latest shares the short-TTL id cache (mutable); pinned numeric versions use the long-TTL version
        // cache (immutable). The "latest/" prefix keeps latest keys out of the id cache's digit-only key space.
        String key = latest ? LATEST + "/" + subject : subject + "/" + parsedVersion;
        ManagedCache nameCache = latest ? cache : versionCache;
        if (nameCache.get(key) instanceof ResolvedSchema cached) {
            return cached;
        }
        SchemaMetadata metadata = latest ? getLatestSchemaMetadata(subject) : getSchemaMetadata(subject, parsedVersion);
        int id = metadata.getId();
        // Authoritative type from the parsed schema; also warms the id→schema cache for the serialize step.
        ResolvedSchema resolved = new ResolvedSchema(id, SchemaType.from(getSchemaById(id).schemaType()));
        nameCache.put(key, resolved);
        return resolved;
    }

    private static int parseVersion(String version) {
        String v = version.trim();
        if (!Utility.getInstance().isDigits(v)) {
            throw new IllegalArgumentException("'version' must be '" + LATEST
                    + "' or a positive integer, got '" + version + "'");
        }
        int parsed = Integer.parseInt(v);
        if (parsed < 1) {
            throw new IllegalArgumentException("'version' must be >= 1, got '" + version + "'");
        }
        return parsed;
    }
}
