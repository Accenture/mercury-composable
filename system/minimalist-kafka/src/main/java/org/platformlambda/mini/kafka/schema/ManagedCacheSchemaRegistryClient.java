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
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.platformlambda.core.util.ManagedCache;

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

    private final ManagedCache cache;

    public ManagedCacheSchemaRegistryClient(List<String> urls, int identityMapCapacity,
                                            List<SchemaProvider> providers, Map<String, ?> configs,
                                            ManagedCache cache) {
        super(urls, identityMapCapacity, providers, configs);
        this.cache = cache;
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
}
