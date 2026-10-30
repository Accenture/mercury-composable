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
 * A {@link CachedSchemaRegistryClient} that resolves a {@code (subject, version)} to a global schema id for the
 * producer, caching a pinned numeric version in platform-core's in-memory {@link ManagedCache}.
 *
 * <p><b>What Confluent caches, and what this class adds.</b> {@link CachedSchemaRegistryClient} keeps every schema
 * it fetched by id for the life of the client - no expiry, bounded by the identity-map capacity - and that cache
 * answers {@code getSchemaById} and the subject-keyed lookups its serializers and deserializers make. A global id is
 * immutable (ids are content-addressed), so a hit is always the right schema. It also caches a subject's latest
 * version for {@code latest.cache.ttl.sec} (60 seconds by default). It does not cache
 * {@code getSchemaMetadata(subject, version)}, so a pinned version would cost a registry request on every publish;
 * that resolution is the one this class caches. Nothing is written to disk.</p>
 *
 * <p><b>Positive results only.</b> A failed lookup is never cached here (it throws before the cache write), and
 * {@link SchemaCodec} pins Confluent's "missing" caches off, so a schema registered while the app is running
 * becomes visible on the very next lookup - no stale "not found" lingers until a TTL elapses or the pod
 * restarts.</p>
 *
 * <p>The version cache is built (and cleared at startup) by {@link SchemaCodec}, with the TTL
 * {@code schema.registry.version.cache.ttl}.</p>
 */
public class ManagedCacheSchemaRegistryClient extends CachedSchemaRegistryClient {

    /** The reserved version alias that resolves to a subject's current newest version. */
    public static final String LATEST = "latest";

    private final ManagedCache versionCache;

    public ManagedCacheSchemaRegistryClient(List<String> urls, int identityMapCapacity,
                                            List<SchemaProvider> providers, Map<String, ?> configs,
                                            ManagedCache versionCache) {
        super(urls, identityMapCapacity, providers, configs);
        this.versionCache = versionCache;
    }

    /**
     * Resolve a {@code (subject, version)} to a global schema id and its type. The id is then used by the
     * id-based serializer ({@code use.schema.id}) to frame the Confluent wire format - subject/version are a
     * producer-side convenience and never travel on the wire.
     *
     * <p><b>Cache, by mutability.</b> A subject's {@code latest} is mutable (a new registration moves it), so it is
     * resolved through {@code getLatestSchemaMetadata}, which Confluent caches for {@code latest.cache.ttl.sec}: a
     * moved {@code latest} is picked up within that TTL. A pinned numeric version is immutable, so its resolution
     * is cached in {@code versionCache} (long TTL, bounded).</p>
     *
     * <p>The schema type is taken from the parsed schema (via {@code getSchemaById}), so it is authoritative -
     * never guessed from a possibly-absent {@code schemaType} - and that lookup also fills Confluent's id cache,
     * so the subsequent serialize needs no extra round-trip.</p>
     *
     * @param subject the registered subject name
     * @param version {@code "latest"} (or null/blank) for the newest version, or a positive integer
     * @return the resolved global id and schema type
     * @throws IOException              if a registry round-trip fails
     * @throws RestClientException      if the registry rejects the lookup (e.g. subject/version not found)
     * @throws IllegalArgumentException if {@code version} is neither {@code "latest"} nor a positive integer
     */
    public ResolvedSchema resolve(String subject, String version) throws IOException, RestClientException {
        if (version == null || version.isBlank() || LATEST.equalsIgnoreCase(version.trim())) {
            return typed(getLatestSchemaMetadata(subject));
        }
        int pinned = parseVersion(version);
        String key = subject + "/" + pinned;
        if (versionCache.get(key) instanceof ResolvedSchema cached) {
            return cached;
        }
        ResolvedSchema resolved = typed(getSchemaMetadata(subject, pinned));
        versionCache.put(key, resolved);
        return resolved;
    }

    /**
     * @param metadata the subject version the registry answered with
     * @return its global id with the authoritative type of the schema registered under it
     */
    private ResolvedSchema typed(SchemaMetadata metadata) throws IOException, RestClientException {
        int id = metadata.getId();
        return new ResolvedSchema(id, SchemaType.from(getSchemaById(id).schemaType()));
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
